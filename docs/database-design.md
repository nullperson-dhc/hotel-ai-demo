# 酒店预订与入住系统数据库设计

## 1. 设计原则

- 数据库为 H2 文件模式，DDL 兼容 H2 2.x。
- 主键使用 `BIGINT` 自增；对外只暴露业务订单号，不暴露数据库主键。
- 库存按“房型 + 营业日期”建模，离店日不占库存。
- 金额使用 `DECIMAL(12,2)`，日期使用 `DATE`，时间点使用带时区语义的 `TIMESTAMP WITH TIME ZONE`。
- 枚举以字符串保存，避免枚举序号变化破坏历史数据。
- 所有业务表至少记录创建或更新时间；并发修改实体带 `version`。
- 订单保留酒店、房型、单价快照，历史订单不依赖当前资料。

## 2. 实体关系

```text
HOTEL 1 ─────────── N ROOM_TYPE
                         │
                         ├──── 1 ─── N ROOM_INVENTORY
                         │
HOTEL 1 ─────────── N BOOKING_ORDER N ─── 1 ROOM_TYPE
                         │
                         └──── N ─── 0..1 STAFF_USER（办理人）

STAFF_USER 独立用于后台认证
```

顾客不是独立账户，姓名和手机号作为订单信息保存。MVP 不创建 `guest` 表。

## 3. 表设计

### 3.1 `hotel` 酒店

| 字段 | 类型 | 空 | 约束/说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | 否 | 主键，自增 |
| `name` | VARCHAR(100) | 否 | 酒店名称 |
| `address` | VARCHAR(255) | 否 | 地址 |
| `status` | VARCHAR(20) | 否 | `ACTIVE` / `INACTIVE` |
| `created_at` | TIMESTAMP WITH TIME ZONE | 否 | 创建时间 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | 否 | 更新时间 |
| `version` | BIGINT | 否 | 乐观锁版本，默认 0 |

约束：`status IN ('ACTIVE', 'INACTIVE')`。

### 3.2 `room_type` 房型

| 字段 | 类型 | 空 | 约束/说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | 否 | 主键，自增 |
| `hotel_id` | BIGINT | 否 | 外键到 `hotel.id` |
| `name` | VARCHAR(100) | 否 | 房型名称 |
| `bed_type` | VARCHAR(50) | 否 | 床型展示文本 |
| `capacity` | INT | 否 | 单间建议入住人数，`> 0` |
| `description` | VARCHAR(1000) | 是 | 房型描述 |
| `base_price` | DECIMAL(12,2) | 否 | 每晚基础价，`>= 0` |
| `status` | VARCHAR(20) | 否 | `ACTIVE` / `INACTIVE` |
| `created_at` | TIMESTAMP WITH TIME ZONE | 否 | 创建时间 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | 否 | 更新时间 |
| `version` | BIGINT | 否 | 乐观锁版本，默认 0 |

约束与索引：

- 外键 `hotel_id -> hotel(id)`。
- `CHECK (capacity > 0)`、`CHECK (base_price >= 0)`。
- `CHECK (status IN ('ACTIVE', 'INACTIVE'))`。
- 普通索引 `idx_room_type_hotel_status(hotel_id, status)`。
- 同一酒店房型名称建议唯一：`uk_room_type_hotel_name(hotel_id, name)`。

`room_type` 不设置静态 `stock` 字段。

### 3.3 `room_inventory` 日历库存

| 字段 | 类型 | 空 | 约束/说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | 否 | 主键，自增 |
| `room_type_id` | BIGINT | 否 | 外键到 `room_type.id` |
| `biz_date` | DATE | 否 | 酒店营业日期 |
| `total_stock` | INT | 否 | 当日总库存，`>= 0` |
| `available_stock` | INT | 否 | 当日可用库存，范围 `[0, total_stock]` |
| `version` | BIGINT | 否 | 版本号，默认 0；条件扣减时递增 |
| `created_at` | TIMESTAMP WITH TIME ZONE | 否 | 创建时间 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | 否 | 更新时间 |

约束与索引：

- 外键 `room_type_id -> room_type(id)`。
- 唯一约束 `uk_inventory_room_date(room_type_id, biz_date)`，保证每天一行。
- `CHECK (total_stock >= 0)`。
- `CHECK (available_stock >= 0 AND available_stock <= total_stock)`。
- 唯一约束已覆盖按房型和日期范围查询的左前缀需求；可订查询如有需要再评估 `biz_date` 辅助索引。

库存记录需由 Demo 初始化数据预先生成。查询区间缺行视为不可售；预订流程不得临时自动创建缺失库存行。

### 3.4 `staff_user` 前台账号

| 字段 | 类型 | 空 | 约束/说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | 否 | 主键，自增 |
| `username` | VARCHAR(64) | 否 | 登录名，唯一 |
| `password_hash` | VARCHAR(100) | 否 | BCrypt 摘要 |
| `display_name` | VARCHAR(100) | 否 | 操作人展示名 |
| `role` | VARCHAR(20) | 否 | MVP 固定 `STAFF` |
| `status` | VARCHAR(20) | 否 | `ACTIVE` / `INACTIVE` |
| `created_at` | TIMESTAMP WITH TIME ZONE | 否 | 创建时间 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | 否 | 更新时间 |
| `version` | BIGINT | 否 | 乐观锁版本，默认 0 |

约束与索引：

- 唯一约束 `uk_staff_username(username)`。
- `CHECK (role IN ('STAFF'))`。
- `CHECK (status IN ('ACTIVE', 'INACTIVE'))`。

### 3.5 `booking_order` 预订订单

| 字段 | 类型 | 空 | 约束/说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | 否 | 主键，自增 |
| `order_no` | VARCHAR(32) | 否 | 对外订单号，全局唯一且不可变 |
| `hotel_id` | BIGINT | 否 | 酒店引用 |
| `room_type_id` | BIGINT | 否 | 房型引用 |
| `hotel_name_snapshot` | VARCHAR(100) | 否 | 酒店名称快照 |
| `room_type_name_snapshot` | VARCHAR(100) | 否 | 房型名称快照 |
| `bed_type_snapshot` | VARCHAR(50) | 否 | 床型快照 |
| `unit_price_snapshot` | DECIMAL(12,2) | 否 | 每房每晚单价快照 |
| `guest_name` | VARCHAR(100) | 否 | 入住人姓名 |
| `guest_phone` | VARCHAR(20) | 否 | 完整手机号，仅服务端受控使用 |
| `check_in_date` | DATE | 否 | 计划入住日 |
| `check_out_date` | DATE | 否 | 计划离店日，不占库存 |
| `room_count` | INT | 否 | 房间数，`> 0` |
| `night_count` | INT | 否 | 晚数，`> 0` |
| `total_amount` | DECIMAL(12,2) | 否 | 总金额，`>= 0` |
| `status` | VARCHAR(20) | 否 | 订单状态 |
| `idempotency_key` | VARCHAR(64) | 否 | 创建预订幂等键 |
| `request_hash` | VARCHAR(64) | 否 | 规范化请求 SHA-256 摘要 |
| `checked_in_at` | TIMESTAMP WITH TIME ZONE | 是 | 实际办理入住时间 |
| `checked_in_by` | BIGINT | 是 | 办理人，外键到 `staff_user.id` |
| `created_at` | TIMESTAMP WITH TIME ZONE | 否 | 创建时间 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | 否 | 更新时间 |
| `version` | BIGINT | 否 | 乐观锁版本，默认 0 |

约束与索引：

- 唯一约束 `uk_booking_order_no(order_no)`。
- 唯一约束 `uk_booking_idempotency_key(idempotency_key)`。
- 外键 `hotel_id -> hotel(id)`、`room_type_id -> room_type(id)`、`checked_in_by -> staff_user(id)`。
- `CHECK (check_out_date > check_in_date)`。
- `CHECK (room_count > 0 AND night_count > 0)`。
- `CHECK (unit_price_snapshot >= 0 AND total_amount >= 0)`。
- `CHECK (status IN ('BOOKED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'))`。
- 索引 `idx_booking_guest_phone_created(guest_phone, created_at)`，支持前台手机号查单并倒序排序。
- 索引 `idx_booking_hotel_stay(hotel_id, check_in_date, check_out_date)`，为后续按入住日期运营查询预留；若 MVP 未使用可暂缓创建。

订单快照字段创建后不更新。数据库无法简洁表达金额乘法与 `night_count` 日期差在所有方言下的一致约束，因此由领域层校验，并用集成测试保证。

## 4. 推荐 DDL

以下为逻辑 DDL 基线；实际建表脚本应与 JPA 映射保持一致。

```sql
CREATE TABLE hotel (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE room_type (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    bed_type VARCHAR(50) NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    description VARCHAR(1000),
    base_price DECIMAL(12,2) NOT NULL CHECK (base_price >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_room_type_hotel FOREIGN KEY (hotel_id) REFERENCES hotel(id),
    CONSTRAINT uk_room_type_hotel_name UNIQUE (hotel_id, name)
);

CREATE INDEX idx_room_type_hotel_status ON room_type(hotel_id, status);

CREATE TABLE room_inventory (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    room_type_id BIGINT NOT NULL,
    biz_date DATE NOT NULL,
    total_stock INT NOT NULL CHECK (total_stock >= 0),
    available_stock INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_inventory_available
        CHECK (available_stock >= 0 AND available_stock <= total_stock),
    CONSTRAINT fk_inventory_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id),
    CONSTRAINT uk_inventory_room_date UNIQUE (room_type_id, biz_date)
);

CREATE TABLE staff_user (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STAFF')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_staff_username UNIQUE (username)
);

CREATE TABLE booking_order (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    hotel_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    hotel_name_snapshot VARCHAR(100) NOT NULL,
    room_type_name_snapshot VARCHAR(100) NOT NULL,
    bed_type_snapshot VARCHAR(50) NOT NULL,
    unit_price_snapshot DECIMAL(12,2) NOT NULL CHECK (unit_price_snapshot >= 0),
    guest_name VARCHAR(100) NOT NULL,
    guest_phone VARCHAR(20) NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    room_count INT NOT NULL CHECK (room_count > 0),
    night_count INT NOT NULL CHECK (night_count > 0),
    total_amount DECIMAL(12,2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('BOOKED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    idempotency_key VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    checked_in_at TIMESTAMP WITH TIME ZONE,
    checked_in_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_booking_stay_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT uk_booking_order_no UNIQUE (order_no),
    CONSTRAINT uk_booking_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_booking_hotel FOREIGN KEY (hotel_id) REFERENCES hotel(id),
    CONSTRAINT fk_booking_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id),
    CONSTRAINT fk_booking_checked_in_by FOREIGN KEY (checked_in_by) REFERENCES staff_user(id)
);

CREATE INDEX idx_booking_guest_phone_created
    ON booking_order(guest_phone, created_at DESC);
```

## 5. 查询与更新设计

### 5.1 可订房型判断

逻辑条件：

- `room_type` 与所属 `hotel` 均为 `ACTIVE`。
- `room_inventory.biz_date >= checkInDate AND biz_date < checkOutDate`。
- 每个房型命中的库存行数等于 `nightCount`。
- `MIN(available_stock) >= roomCount`。

可用聚合查询一次筛选，也可在目标数据量下批量加载后由应用层组合；禁止逐房型逐日期产生 N+1 查询。

### 5.2 原子库存扣减

```sql
UPDATE room_inventory
SET available_stock = available_stock - ?,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE room_type_id = ?
  AND biz_date = ?
  AND available_stock >= ?;
```

应用层要求每个营业日更新行数均为 1，否则抛异常回滚整个事务。`CHECK` 约束为额外防线，不替代条件谓词。

### 5.3 条件办理入住

日期规则先在事务内基于持久化订单和注入时钟校验，再执行等价更新：

```sql
UPDATE booking_order
SET status = 'CHECKED_IN',
    checked_in_at = ?,
    checked_in_by = ?,
    version = version + 1,
    updated_at = ?
WHERE order_no = ?
  AND status = 'BOOKED'
  AND version = ?;
```

更新行数为 0 表示状态或版本已变化，返回冲突，不重复产生入住副作用。

## 6. 幂等设计

- 创建预订要求客户端提供 `Idempotency-Key`，长度 1 至 64，建议 UUID。
- 服务端对规范化后的 `roomTypeId + dates + roomCount + normalized guest data` 计算 SHA-256 `request_hash`。
- 首次请求在同一预订事务中保存幂等键和订单。
- 相同键、相同摘要：返回既有订单，不再扣库存。
- 相同键、不同摘要：返回 `IDEMPOTENCY_CONFLICT`。
- 两个相同键并发插入时，由唯一约束裁决；败方事务回滚后读取已提交订单并按上述规则响应。

该设计只保证创建预订接口的安全重试，不把幂等键扩展为通用永久请求日志。

## 7. 数据保留、脱敏与初始化

- 数据库保存完整手机号用于严格匹配；公开 API 仅返回服务端脱敏值，例如 `138****8000`。
- 密码只保存 BCrypt 摘要。
- Demo 初始化一间启用酒店、若干启用房型、覆盖演示日期范围的日历库存和一个前台账号。
- 初始化脚本应可重复执行或仅在空库执行，不能在应用每次启动时重置已有订单和库存。
- 不在源代码或正式文档中存放真实顾客数据和生产密码。

## 8. JPA 映射约束

- `EnumType.STRING` 映射所有状态枚举。
- `LocalDate` 映射营业日期；`OffsetDateTime` 或统一 UTC 的 `Instant` 映射时间点。
- `BigDecimal` 明确 `precision = 12, scale = 2`。
- `@Version` 映射 `version`，但库存扣减使用显式修改查询并同步递增版本。
- 默认关联使用 LAZY；API DTO 在事务内显式投影或组装，避免 Open Session in View 和序列化懒加载。
- `spring.jpa.open-in-view=false`。
- Demo 开发可用校验模式验证映射；数据库基线由显式 DDL 管理，不依赖生产环境自动 `create-drop`。
