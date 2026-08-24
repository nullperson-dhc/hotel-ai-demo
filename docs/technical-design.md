# 酒店预订与入住系统技术设计

## 1. 设计目标与范围

本文依据 [需求说明](requirements.md) 设计可运行的单酒店 MVP，只描述架构与实现约束，不包含业务代码。

技术栈固定为：

- 后端：Java 17、Spring Boot 3、Spring Web、Spring Data JPA、Spring Validation、Spring Security、H2。
- 前端：React、TypeScript、Vite、Ant Design。
- 通信：JSON over HTTP，REST API。
- 部署：MVP 为一个前端静态站点和一个后端进程；H2 使用文件模式持久化。

核心质量目标是库存不超卖、订单与库存原子一致、订单状态不可非法迁移、顾客信息不泄露。

## 2. 系统架构

### 2.1 总体架构

```text
┌─────────────────────────────────────────────────────────┐
│ React SPA                                               │
│ 顾客端：房型查询 / 创建预订 / 订单查询                  │
│ 前台端：登录 / 订单查询 / 办理入住                      │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTPS + JSON
┌───────────────────────▼─────────────────────────────────┐
│ Spring Boot 模块化单体                                  │
│                                                         │
│ Web/API 层                                              │
│  参数校验、认证授权、DTO、统一响应、异常映射            │
│                         │                               │
│ Application 层                                         │
│  查询编排、预订事务、入住事务、幂等处理                 │
│                         │                               │
│ Domain 层                                               │
│  Hotel / RoomType / Inventory / BookingOrder / Staff   │
│  日期、金额、库存、订单状态规则                         │
│                         │                               │
│ Infrastructure 层                                       │
│  JPA Repository、条件更新、H2、Spring Security、时钟    │
└───────────────────────┬─────────────────────────────────┘
                        │ JDBC / transaction
┌───────────────────────▼─────────────────────────────────┐
│ H2 文件数据库                                           │
│ 酒店、房型、日历库存、订单、员工                        │
└─────────────────────────────────────────────────────────┘
```

### 2.2 架构决策

| 决策 | 选择 | 原因 |
| --- | --- | --- |
| 应用形态 | 模块化单体 | MVP 业务量小，库存与订单需要本地数据库事务，不引入分布式一致性 |
| API 风格 | REST + JSON | 与 React 集成直接，接口边界清楚 |
| 持久化 | JPA + H2 文件模式 | 满足指定技术栈，重启后保留 Demo 数据 |
| 后台认证 | Spring Security 服务端会话 | 单体应用简单可靠；Cookie 设置 HttpOnly、SameSite=Lax |
| CSRF | 后台写接口启用 CSRF Token | 会话 Cookie 认证下防止跨站写操作 |
| 库存并发 | 事务 + 条件更新 | 以数据库原子谓词 `available_stock >= roomCount` 防止负库存 |
| 订单并发 | 乐观锁 + 状态条件更新 | 防止重复/并发办理入住 |
| 时间来源 | 注入 `Clock`，酒店时区 Asia/Shanghai | 业务日期一致且可测试，不直接散落调用系统时间 |
| 金额 | `BigDecimal`，数据库 `DECIMAL(12,2)` | 避免浮点误差 |

H2 适合 Demo 和自动化测试，不视为生产部署方案。生产化时需要迁移到支持相同事务语义和条件更新的关系数据库，并重新执行并发与性能验证。

## 3. 项目目录结构

```text
hotel-ai-demo/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/example/hotel/
│       │   │   ├── HotelApplication.java
│       │   │   ├── common/
│       │   │   │   ├── api/          # 通用响应与错误结构
│       │   │   │   ├── exception/    # 异常类型及全局映射
│       │   │   │   ├── security/     # Security 配置与认证主体
│       │   │   │   └── time/         # Clock 与酒店业务日期
│       │   │   ├── catalog/
│       │   │   │   ├── web/          # 酒店/可订房型 API
│       │   │   │   ├── application/  # 可订房型查询服务
│       │   │   │   ├── domain/       # Hotel、RoomType
│       │   │   │   └── repository/
│       │   │   ├── inventory/
│       │   │   │   ├── domain/       # RoomInventory
│       │   │   │   └── repository/   # 区间查询、条件扣减
│       │   │   ├── booking/
│       │   │   │   ├── web/          # 顾客/前台订单 API
│       │   │   │   ├── application/  # 创建、查询、入住用例
│       │   │   │   ├── domain/       # BookingOrder、OrderStatus
│       │   │   │   └── repository/
│       │   │   └── staff/
│       │   │       ├── web/          # 登录、退出、会话 API
│       │   │       ├── domain/       # StaffUser
│       │   │       └── repository/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── schema.sql         # 可选：显式建表基线
│       │       └── data.sql           # Demo 酒店、房型、库存、账号
│       └── test/
│           └── java/com/example/hotel/
│               ├── booking/           # 预订与入住事务测试
│               ├── inventory/         # 并发库存测试
│               └── api/               # API、认证与错误契约测试
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── app/                        # 路由、布局、全局配置
│       ├── api/                        # HTTP 客户端、类型、错误解析
│       ├── features/
│       │   ├── availability/           # 可订房型查询
│       │   ├── booking/                # 预订与结果
│       │   ├── guest-order/            # 顾客查单
│       │   ├── staff-auth/             # 登录与会话
│       │   └── staff-order/            # 前台查单与入住
│       ├── components/                 # 通用展示组件
│       ├── pages/
│       ├── types/
│       └── main.tsx
└── docs/
    ├── requirements.md
    ├── technical-design.md
    ├── database-design.md
    └── api-design.md
```

模块之间只通过应用服务或公开领域接口协作，Web 层不直接操作 Repository，实体不直接作为 API DTO 返回。

## 4. 核心领域模型

### 4.1 聚合与职责

| 模型 | 类型 | 核心职责 |
| --- | --- | --- |
| `Hotel` | 实体 | 酒店名称、地址、启停状态；不持有库存 |
| `RoomType` | 实体 | 物理房型、床型、容量、基础价、启停状态 |
| `RoomInventory` | 实体 | 某房型某营业日的总库存和可用库存；执行并发安全扣减 |
| `BookingOrder` | 聚合根 | 订单号、顾客、入住区间、数量、金额快照、状态迁移 |
| `StaffUser` | 实体 | 前台账号、密码摘要、状态和角色 |

`Guest` 在 MVP 中作为 `BookingOrder` 内的值对象（姓名、手机号），不建立独立顾客账户或顾客表，避免无需求依据的会员建模。

### 4.2 关键不变量

- `Hotel` 与 `RoomType` 必须为启用状态才可查询和预订。
- `checkOutDate > checkInDate >= hotelToday`。
- `nightCount = DAYS.between(checkInDate, checkOutDate)`。
- `roomCount >= 1`，房型容量只描述单间容量；MVP 未采集入住人数，因此不据此限制房间数。
- `unitPriceSnapshot >= 0`，`totalAmount = unitPriceSnapshot × nightCount × roomCount`。
- `0 <= availableStock <= totalStock`。
- 库存占用区间为 `[checkInDate, checkOutDate)`。
- 订单创建后，订单号、日期、房间数和业务快照不可变。
- MVP 唯一可执行状态迁移为 `BOOKED -> CHECKED_IN`。

### 4.3 值对象与枚举

- `StayPeriod(checkInDate, checkOutDate)`：校验日期、生成占用营业日序列、计算晚数。
- `GuestInfo(name, phone)`：统一去空格和中国大陆手机号校验。
- `Money(amount)` 或受约束的 `BigDecimal`：两位小数、非负、统一舍入规则 `HALF_UP`。
- `HotelStatus`、`RoomTypeStatus`、`StaffStatus`：`ACTIVE`、`INACTIVE`。
- `OrderStatus`：`BOOKED`、`CHECKED_IN`、预留 `CHECKED_OUT`、`CANCELLED`。

## 5. 关键应用流程

### 5.1 查询可订房型

1. 校验日期与房间数。
2. 查询启用酒店下启用房型。
3. 聚合 `[入住日, 离店日)` 每天库存；只保留库存记录完整且最小可用库存不少于房间数的房型。
4. 使用当前房型基础价计算预计总价。
5. 返回只读 DTO；该结果不锁定库存，创建预订时必须重新校验。

### 5.2 创建预订

1. 在进入事务前完成请求格式校验；在事务内重新校验酒店、房型与业务日期。
2. 检查 `Idempotency-Key`：同一键且请求摘要相同则返回原订单；摘要不同则报冲突。
3. 生成占用营业日列表，确认库存记录完整。
4. 按营业日升序逐行执行带库存下限条件的原子扣减。
5. 任一行更新数不是 1，抛出库存不足异常并回滚此前扣减。
6. 使用房型当前信息生成订单快照、金额和 BOOKED 状态，保存订单。
7. 提交后返回订单 DTO；若客户端未收到响应，可使用相同幂等键安全重试。

### 5.3 办理入住

1. 验证前台会话和 CSRF Token。
2. 按订单号执行状态条件更新：仅当状态为 BOOKED 且版本匹配时更新。
3. 更新前校验酒店当前日期满足 `checkInDate <= today < checkOutDate`。
4. 成功后写入 `checkedInAt` 和 `checkedInBy`；不改变库存。
5. 更新数为 0 时重新读取订单，将原因映射为不存在、非法状态或并发冲突。

## 6. 酒店日历库存模型

库存以 `(room_type_id, biz_date)` 唯一标识一行，不能把库存放在 `RoomType.stock`。

例如订单入住 2026-08-24、离店 2026-08-27、预订 2 间，扣减：

| 营业日 | 是否扣减 | 数量 |
| --- | --- | --- |
| 2026-08-24 | 是 | 2 |
| 2026-08-25 | 是 | 2 |
| 2026-08-26 | 是 | 2 |
| 2026-08-27 | 否 | 0 |

MVP 保存 `total_stock` 与 `available_stock`，不额外拆分锁定、已售库存，因为预订提交即成功且没有待支付锁定期。`version` 用于管理修改冲突和审计；预订扣减的最终安全边界仍是数据库条件更新。

可订查询必须要求区间库存记录数等于晚数；缺少任一天记录时视为不可订，不能把缺失记录视作无限库存或零值后自动补建。

## 7. 订单状态机

```text
                （本期实现）
        ┌─────────────────────────┐
        │                         ▼
     BOOKED                  CHECKED_IN
        │                         │
        │ 预留、MVP 无入口         │ 预留、MVP 无入口
        ▼                         ▼
    CANCELLED                 CHECKED_OUT
```

| 当前状态 | 动作 | 目标状态 | MVP | 额外条件 |
| --- | --- | --- | --- | --- |
| BOOKED | 办理入住 | CHECKED_IN | 是 | `checkInDate <= today < checkOutDate` |
| BOOKED | 取消 | CANCELLED | 否 | 后续版本定义库存返还规则 |
| CHECKED_IN | 办理离店 | CHECKED_OUT | 否 | 后续版本定义 |

其他迁移一律非法。状态变更由 `BookingOrder` 业务行为或专用条件更新完成，不开放通用 `setStatus`。

## 8. 事务设计

| 用例 | 事务属性 | 事务内操作 | 失败结果 |
| --- | --- | --- | --- |
| 查询可订房型 | 只读事务 | 房型及区间库存查询 | 无数据变化 |
| 创建预订 | 单个读写事务 | 幂等校验、房型校验、逐日扣库存、创建订单 | 任一步失败全部回滚 |
| 顾客/前台查单 | 只读事务 | 精确查询或分页列表 | 无数据变化 |
| 办理入住 | 单个读写事务 | 日期/状态校验、条件状态更新、操作人记录 | 更新失败不产生部分状态 |

- 创建预订采用默认 `READ_COMMITTED`，正确性不依赖“先查后改”的隔离效果，而依赖条件更新的原子谓词。
- 事务方法放在 Application Service 公共方法边界，避免同类内部调用绕过代理。
- 库存不足、状态冲突等业务异常必须触发回滚；异常分类不得被捕获后静默提交。
- 不在数据库事务中执行网络调用。MVP 没有支付或通知等外部副作用。
- 测试使用独立线程和独立事务验证并发，不用单事务测试模拟竞争。

## 9. 防止库存超卖方案

### 9.1 条件扣减

每个占用日执行等价 SQL：

```sql
UPDATE room_inventory
SET available_stock = available_stock - :roomCount,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE room_type_id = :roomTypeId
  AND biz_date = :bizDate
  AND available_stock >= :roomCount;
```

- 返回 1：该日扣减成功。
- 返回 0：库存行不存在或已不足，抛出异常回滚整个订单事务。
- 多日期按日期升序更新，降低未来多房型扩展时的死锁概率。
- 数据库同时用 `CHECK (available_stock >= 0)` 作为最后防线。

### 9.2 为什么不能只使用普通查询

“查询库存充足，再无条件更新”存在检查与更新之间的竞争窗口，两个请求可能同时看到最后一间房。即使 JPA 实体带 `@Version`，若上层错误重试整笔业务也可能产生复杂行为。因此本方案用数据库条件更新直接表达“仅在库存仍充足时扣减”。

### 9.3 必测并发场景

- 库存 1，两请求同时各订 1：恰好一单成功。
- 连续多日中只有一天库存 1，两请求竞争：最多一单成功，失败事务对其他日期的扣减全部回滚。
- 同一幂等键同时提交：最多创建一张订单且只扣减一次。

## 10. 认证、安全与隐私

- 后台账号密码使用 BCrypt 摘要；种子密码只用于本地 Demo 配置，不写入日志。
- 登录成功建立服务端会话，Cookie 使用 `HttpOnly`、`SameSite=Lax`；生产 HTTPS 环境增加 `Secure`。
- 顾客公开查单必须同时匹配订单号和完整手机号；不存在与不匹配统一返回 404 和同一错误码。
- 顾客响应中的手机号由服务端脱敏，前端不接收完整号码。
- 前台接口要求 `ROLE_STAFF`；写接口验证 CSRF Token。
- DTO 白名单映射，禁止序列化 JPA 实体、密码摘要或内部版本字段。
- 日志记录追踪号、错误码、订单号的安全形式，不记录密码和完整手机号。
- CORS 仅允许配置的前端来源并允许凭证，不使用通配来源。

## 11. 异常处理方案

### 11.1 分层原则

- Web 层：处理 JSON 解析、Bean Validation 和认证授权错误。
- Application/Domain 层：抛出语义明确的业务异常，不依赖 HTTP 状态码。
- Repository 层：数据库约束或锁冲突转换为应用可识别异常；不向客户端泄露 SQL。
- `@RestControllerAdvice`：统一转换为 API 错误契约，详见 [API 设计](api-design.md)。

### 11.2 异常映射

| 异常类别 | HTTP | 示例错误码 |
| --- | ---: | --- |
| 参数/业务日期非法 | 400 | `VALIDATION_ERROR`、`INVALID_STAY_PERIOD` |
| 未认证/会话失效 | 401 | `UNAUTHORIZED` |
| 无后台权限/CSRF 失败 | 403 | `FORBIDDEN` |
| 资源不存在 | 404 | `ROOM_TYPE_NOT_FOUND`、`ORDER_NOT_FOUND` |
| 库存不足 | 409 | `INVENTORY_INSUFFICIENT` |
| 非法订单状态/并发冲突 | 409 | `ORDER_STATUS_CONFLICT` |
| 幂等键复用但请求不同 | 409 | `IDEMPOTENCY_CONFLICT` |
| 未预期错误 | 500 | `INTERNAL_ERROR` |

500 响应只返回通用信息和 `traceId`。服务端记录含堆栈的结构化日志，以 `traceId` 关联排查。

## 12. 前端设计

### 12.1 路由

| 路由 | 页面 | 访问 |
| --- | --- | --- |
| `/` | 可订房型查询与结果 | 公开 |
| `/booking/new` | 预订填写 | 公开 |
| `/booking/result/:orderNo` | 预订成功摘要 | 当前导航态；不单凭订单号拉取隐私数据 |
| `/orders/query` | 顾客订单查询 | 公开 |
| `/staff/login` | 前台登录 | 公开 |
| `/staff/orders` | 前台订单查询 | 前台会话 |
| `/staff/orders/:orderNo` | 前台订单详情与入住 | 前台会话 |

### 12.2 状态管理

- 服务端数据使用 feature 内查询 hooks 管理请求、加载、成功、空结果和失败状态。
- 预订表单状态由 Ant Design Form 管理；前端校验用于体验，服务端校验为最终准则。
- 不把完整手机号、密码、会话标识持久化到 Local Storage。
- HTTP 客户端统一携带 Cookie、CSRF Token 和 `X-Request-Id`，统一解析错误结构。
- 创建预订时为一次用户提交生成 UUID 形式 `Idempotency-Key`；网络失败后的明确重试沿用原键，用户修改请求后生成新键。

## 13. 可观测性与测试边界

- 每个请求生成或透传 `X-Request-Id`，响应返回同一标识。
- 记录接口、耗时、结果码和安全化业务标识；健康检查不输出敏感配置。
- 单元测试覆盖日期、金额、手机号、状态机。
- Repository 集成测试覆盖区间库存查询和条件扣减。
- 服务集成测试覆盖原子回滚、幂等和并发超卖。
- API 测试覆盖 DTO、错误码、查单隐私、认证、CSRF。
- 前端测试覆盖表单校验、空态/错误态、登录失效和入住按钮条件。

## 14. 设计约束与后续演进

- 当前没有取消和离店业务，不实现库存返还与 CHECKED_OUT 入口。
- 当前基础价是固定每晚价，不引入 RatePlan、每日价格表或促销引擎。
- 当前无实体房号，不引入排房模型。
- 若后续支持待支付订单，应引入 `lockedStock`、库存锁定过期机制和支付状态，不能直接复用当前“创建即售出”的语义。
- 若后续改为多实例或生产数据库，需重新验证会话共享、幂等唯一约束、事务隔离和条件更新 SQL 方言。
