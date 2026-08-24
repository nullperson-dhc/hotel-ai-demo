# 酒店预订与入住系统 API 设计

## 1. API 约定

### 1.1 基础约定

- 基础路径：`/api/v1`。
- 请求和响应：`application/json; charset=utf-8`。
- 日期：ISO 8601 `YYYY-MM-DD`，按酒店时区 Asia/Shanghai 解释。
- 时间点：ISO 8601，响应携带偏移量，例如 `2026-08-24T14:30:00+08:00`。
- 金额：JSON 字符串且固定两位小数，例如 `"1299.00"`，避免 JavaScript 浮点损失。
- ID：数据库主键在 MVP 可作为资源标识返回给查询结果，但订单只使用 `orderNo` 对外定位。
- 所有响应返回 `X-Request-Id`；客户端可传入合法的 `X-Request-Id` 供链路追踪。
- 后台接口使用服务端会话 Cookie；客户端请求需启用 credentials。
- 未特别说明的列表为空时返回空数组，不用 404。

### 1.2 成功响应

单资源直接返回资源 DTO，不额外包装 `code: 0`。创建成功使用 201，其余成功使用 200；退出登录无响应体时使用 204。

### 1.3 错误响应

```json
{
  "code": "INVALID_STAY_PERIOD",
  "message": "离店日期必须晚于入住日期",
  "fieldErrors": [
    {
      "field": "checkOutDate",
      "message": "必须晚于入住日期"
    }
  ],
  "traceId": "01J60ABCDEF1234567890XYZ"
}
```

字段说明：

| 字段 | 必有 | 说明 |
| --- | --- | --- |
| `code` | 是 | 稳定、可供前端分支处理的错误码 |
| `message` | 是 | 安全、面向用户的中文提示 |
| `fieldErrors` | 否 | 字段校验失败明细，不返回被拒绝的敏感值 |
| `traceId` | 是 | 服务端排查标识，与 `X-Request-Id` 一致 |

## 2. 公共 DTO

### 2.1 `OrderDetail`

```json
{
  "orderNo": "H20260824A1B2C3D4",
  "hotelName": "示例酒店",
  "roomTypeName": "豪华大床房",
  "bedType": "1张1.8米大床",
  "guestName": "张三",
  "guestPhoneMasked": "138****8000",
  "checkInDate": "2026-08-24",
  "checkOutDate": "2026-08-27",
  "roomCount": 1,
  "nightCount": 3,
  "unitPrice": "399.00",
  "totalAmount": "1197.00",
  "status": "BOOKED",
  "createdAt": "2026-08-20T10:00:00+08:00",
  "checkedInAt": null
}
```

公开顾客接口与前台接口可复用此安全 DTO；前台如需显示办理人，可在后台详情增加 `checkedInByName`，不返回账号内部 ID 或密码信息。

### 2.2 订单状态展示

| API 值 | 中文展示 | 顾客/前台可见 |
| --- | --- | --- |
| `BOOKED` | 已预订 | 是 |
| `CHECKED_IN` | 已入住 | 是 |
| `CHECKED_OUT` | 已离店 | 预留 |
| `CANCELLED` | 已取消 | 预留 |

前端依据枚举映射展示文字，不以中文文本作为业务判断条件。

## 3. 顾客公开接口

### 3.1 查询可订房型

`GET /api/v1/availability`

查询参数：

| 参数 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `checkInDate` | date | 是 | 不早于酒店当前日期 |
| `checkOutDate` | date | 是 | 晚于入住日期 |
| `roomCount` | integer | 是 | `>= 1` |

请求示例：

```http
GET /api/v1/availability?checkInDate=2026-08-24&checkOutDate=2026-08-27&roomCount=1
```

成功响应 `200 OK`：

```json
{
  "hotel": {
    "id": 1,
    "name": "示例酒店",
    "address": "上海市示例路1号"
  },
  "checkInDate": "2026-08-24",
  "checkOutDate": "2026-08-27",
  "nightCount": 3,
  "roomCount": 1,
  "roomTypes": [
    {
      "id": 101,
      "name": "豪华大床房",
      "bedType": "1张1.8米大床",
      "capacity": 2,
      "description": "含独立卫浴",
      "unitPrice": "399.00",
      "estimatedTotalAmount": "1197.00"
    }
  ]
}
```

说明：

- 仅返回整个区间库存都足够的房型。
- 空结果仍返回 200，`roomTypes: []`。
- 该接口结果不锁定库存，价格和库存以创建预订时再次校验为准。

错误：`400 INVALID_STAY_PERIOD`、`400 VALIDATION_ERROR`。

### 3.2 创建预订

`POST /api/v1/bookings`

请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `Idempotency-Key` | 是 | 1 至 64 字符，建议 UUID；相同业务重试必须沿用 |
| `Content-Type` | 是 | `application/json` |

请求体：

```json
{
  "roomTypeId": 101,
  "checkInDate": "2026-08-24",
  "checkOutDate": "2026-08-27",
  "roomCount": 1,
  "guestName": "张三",
  "guestPhone": "13800138000"
}
```

字段规则：

| 字段 | 规则 |
| --- | --- |
| `roomTypeId` | 必填、正整数 |
| `checkInDate` | 必填、不早于当前酒店日期 |
| `checkOutDate` | 必填、晚于入住日期 |
| `roomCount` | 必填、整数、`>= 1` |
| `guestName` | 必填，去首尾空格后 1 至 100 字符 |
| `guestPhone` | 必填，中国大陆 11 位手机号格式 `^1[3-9]\d{9}$` |

首次成功响应 `201 Created`，响应头 `Location: /api/v1/bookings/{orderNo}`，响应体为 `OrderDetail`。

相同 `Idempotency-Key` 且请求规范化内容一致时返回原订单：

- 原首次请求已确认创建成功：返回 `200 OK`。
- 不再次扣减库存，不创建新订单。

错误：

| HTTP | 错误码 | 场景 |
| ---: | --- | --- |
| 400 | `VALIDATION_ERROR` | 字段格式错误或缺少幂等键 |
| 400 | `INVALID_STAY_PERIOD` | 日期不符合规则 |
| 404 | `ROOM_TYPE_NOT_FOUND` | 房型不存在；停用房型也按不可用处理 |
| 409 | `INVENTORY_INSUFFICIENT` | 任一天库存不足，整单未创建 |
| 409 | `IDEMPOTENCY_CONFLICT` | 同一幂等键对应不同请求内容 |

### 3.3 顾客查询订单

为避免把手机号放入 URL、浏览器历史和访问日志，使用只读语义的 POST 查询端点：

`POST /api/v1/bookings/query`

请求体：

```json
{
  "orderNo": "H20260824A1B2C3D4",
  "guestPhone": "13800138000"
}
```

成功响应 `200 OK`：`OrderDetail`，其中只包含 `guestPhoneMasked`。

订单号不存在或手机号不匹配统一返回：

```http
404 Not Found
```

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "未找到匹配订单",
  "traceId": "01J60ABCDEF1234567890XYZ"
}
```

不得通过状态码、错误码、消息或响应时序主动区分“订单不存在”和“手机号不匹配”。该接口不得写入订单或库存。

## 4. 前台认证接口

### 4.1 获取会话与 CSRF Token

`GET /api/v1/staff/session`

未登录也可调用，用于前端初始化 CSRF Token。响应通过 Cookie 提供服务端会话/CSRF 所需信息，并返回：

```json
{
  "authenticated": false,
  "staff": null,
  "csrf": {
    "headerName": "X-CSRF-TOKEN",
    "token": "opaque-token"
  }
}
```

已登录时：

```json
{
  "authenticated": true,
  "staff": {
    "displayName": "前台员工"
  },
  "csrf": {
    "headerName": "X-CSRF-TOKEN",
    "token": "opaque-token"
  }
}
```

### 4.2 登录

`POST /api/v1/staff/session`

需要携带 CSRF Token。

请求：

```json
{
  "username": "frontdesk",
  "password": "********"
}
```

成功响应 `200 OK`：

```json
{
  "authenticated": true,
  "staff": {
    "displayName": "前台员工"
  }
}
```

账号不存在、密码错误、账号停用均返回相同的 `401 INVALID_CREDENTIALS`，不得说明具体原因。登录成功后应更新会话标识以防会话固定攻击。

### 4.3 退出

`DELETE /api/v1/staff/session`

需要登录及 CSRF Token。成功返回 `204 No Content`，服务端失效会话并清除 Cookie。

## 5. 前台订单接口

以下接口均要求有效前台会话；未认证返回 `401 UNAUTHORIZED`。写接口还要求有效 CSRF Token。

### 5.1 前台查询订单

`GET /api/v1/staff/bookings`

查询参数二选一且必须恰好提供一个：

| 参数 | 类型 | 规则 |
| --- | --- | --- |
| `orderNo` | string | 完整订单号，精确查询 |
| `guestPhone` | string | 完整手机号，精确查询 |
| `page` | integer | 手机号查询可用，默认 0，`>= 0` |
| `size` | integer | 默认 20，范围 1 至 100 |

订单号查询成功仍使用统一分页结构，以简化前端：

```json
{
  "items": [
    {
      "orderNo": "H20260824A1B2C3D4",
      "hotelName": "示例酒店",
      "roomTypeName": "豪华大床房",
      "bedType": "1张1.8米大床",
      "guestName": "张三",
      "guestPhoneMasked": "138****8000",
      "checkInDate": "2026-08-24",
      "checkOutDate": "2026-08-27",
      "roomCount": 1,
      "nightCount": 3,
      "unitPrice": "399.00",
      "totalAmount": "1197.00",
      "status": "BOOKED",
      "createdAt": "2026-08-20T10:00:00+08:00",
      "checkedInAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

- 手机号结果按 `createdAt DESC`，相同时间按 `orderNo DESC` 保证稳定顺序。
- 无结果返回 200 和空 `items`。
- 同时提供或都未提供 `orderNo`、`guestPhone`，返回 `400 VALIDATION_ERROR`。
- 即使是前台列表，手机号默认仍脱敏；核验使用前台线下流程，不在 MVP 扩大隐私展示范围。

### 5.2 前台订单详情

`GET /api/v1/staff/bookings/{orderNo}`

成功返回 `200 OrderDetail`。不存在返回 `404 ORDER_NOT_FOUND`。

详情响应可增加：

```json
{
  "checkedInByName": "前台员工"
}
```

此片段表示可选扩展字段，不是独立响应体。

### 5.3 办理入住

`POST /api/v1/staff/bookings/{orderNo}/check-in`

要求有效会话及 CSRF Token，无请求体。服务端以酒店当前日期判断，不接受客户端传入办理日期或目标状态。

成功响应 `200 OK`：更新后的 `OrderDetail`，`status = CHECKED_IN` 且 `checkedInAt` 非空。

错误：

| HTTP | 错误码 | 场景 |
| ---: | --- | --- |
| 401 | `UNAUTHORIZED` | 未登录或会话失效 |
| 403 | `FORBIDDEN` | 无权限或 CSRF 无效 |
| 404 | `ORDER_NOT_FOUND` | 订单不存在 |
| 409 | `CHECK_IN_TOO_EARLY` | 当前酒店日期早于入住日 |
| 409 | `CHECK_IN_EXPIRED` | 当前酒店日期达到或晚于离店日 |
| 409 | `ORDER_STATUS_CONFLICT` | 订单不是 BOOKED，或并发状态已变化 |

重复调用不重复修改数据。已是 CHECKED_IN 时按需求视为冲突，而不是返回伪成功。

## 6. HTTP 状态码与错误码总表

| HTTP | 错误码 | 含义 |
| ---: | --- | --- |
| 400 | `VALIDATION_ERROR` | 请求格式或字段校验失败 |
| 400 | `MALFORMED_JSON` | JSON 无法解析 |
| 400 | `INVALID_STAY_PERIOD` | 入住/离店日期非法 |
| 401 | `UNAUTHORIZED` | 未认证或会话失效 |
| 401 | `INVALID_CREDENTIALS` | 登录凭据无效，统一提示 |
| 403 | `FORBIDDEN` | 无权限或 CSRF 校验失败 |
| 404 | `ROOM_TYPE_NOT_FOUND` | 房型不存在或不可预订 |
| 404 | `ORDER_NOT_FOUND` | 订单未找到；公开查单也用于手机号不匹配 |
| 409 | `INVENTORY_INSUFFICIENT` | 入住区间库存不足 |
| 409 | `IDEMPOTENCY_CONFLICT` | 幂等键与原请求不一致 |
| 409 | `ORDER_STATUS_CONFLICT` | 订单状态不允许当前操作 |
| 409 | `CHECK_IN_TOO_EARLY` | 尚未到入住日期 |
| 409 | `CHECK_IN_EXPIRED` | 已到或超过离店日期 |
| 429 | `TOO_MANY_REQUESTS` | 登录或公开查单触发限流（生产安全增强） |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误 |

## 7. 接口与需求追踪

| 需求 | API |
| --- | --- |
| FR-01 查询可订房型 | `GET /availability` |
| FR-02 创建预订 | `POST /bookings` |
| FR-03 顾客查询订单 | `POST /bookings/query` |
| FR-04 前台登录/退出 | `GET/POST/DELETE /staff/session` |
| FR-05 前台查询订单 | `GET /staff/bookings`、`GET /staff/bookings/{orderNo}` |
| FR-06 办理入住 | `POST /staff/bookings/{orderNo}/check-in` |

## 8. API 实现与测试约束

- Controller 只接受请求 DTO 并调用应用服务，不直接访问 JPA Repository。
- 所有 DTO 都执行服务端校验；跨字段日期规则由类级校验器或应用服务校验。
- JPA 实体不得直接序列化为响应。
- 创建预订测试必须验证响应中断后的同幂等键重试只产生一张订单。
- 顾客查单测试必须验证错误手机号与不存在订单得到相同 HTTP 状态和错误结构。
- 入住接口测试必须覆盖边界：入住日前一天、入住日、离店日前一天、离店日。
- 认证测试覆盖登录失败统一提示、未登录访问、退出后会话失效和 CSRF 拒绝。
- 并发测试必须验证库存为 1 时两个独立事务最多一个预订成功。
