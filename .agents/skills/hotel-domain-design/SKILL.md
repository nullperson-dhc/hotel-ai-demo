---
name: hotel-domain-design
description: 用于酒店预订系统的领域建模、库存、订单状态及入住离店等核心业务规则设计。适用于酒店领域分析与业务建模；不用于具体框架代码实现。
---

# Hotel Domain Design Skill

## Purpose

用于酒店预订系统的领域建模与核心业务规则设计。

适用于：
- 酒店商品建模
- 房型建模
- 日历库存
- 酒店预订
- 订单状态
- 入住/离店流程

本 Skill 负责酒店领域规则，不负责具体框架代码实现。

---

## Core Domain Model

推荐核心模型：

- Hotel
- RoomType
- RoomInventory
- BookingOrder
- Guest

可根据业务复杂度扩展：

- Product
- SKU
- RatePlan
- OrderNight
- InventoryPool

---

## Hotel

Hotel 表示酒店主体。

核心属性：

- id
- name
- address
- status

Hotel 本身不直接维护房间库存。

---

## RoomType

RoomType 表示物理房型。

例如：

- 豪华大床房
- 豪华双床房
- 行政套房

核心属性：

- id
- hotelId
- name
- bedType
- capacity
- description
- basePrice
- status

禁止直接在 RoomType 上使用一个静态 stock 字段表示酒店库存。

错误示例：

RoomType.stock = 10

酒店库存必须考虑日期维度。

---

## Calendar Inventory

酒店库存必须按照：

RoomType + bizDate

进行建模。

推荐模型：

RoomInventory

字段：

- id
- roomTypeId
- bizDate
- totalStock
- availableStock
- lockedStock
- soldStock
- version

最小 Demo 可以只保留：

- roomTypeId
- bizDate
- availableStock
- version

---

## Stay Date Rule

入住日期：

2026-08-24

离店日期：

2026-08-27

实际占用库存：

- 2026-08-24
- 2026-08-25
- 2026-08-26

离店日期不占用库存。

入住晚数：

nightCount = checkOutDate - checkInDate

要求：

checkOutDate > checkInDate

---

## Booking Rule

创建预订前必须：

1. 校验酒店/房型有效
2. 校验入住日期
3. 计算入住日期区间
4. 查询区间内每天库存
5. 确保每天库存都满足 roomCount
6. 扣减每天库存
7. 创建订单

任意一天库存不足，整个预订失败。

不允许部分日期扣减成功、部分失败。

---

## Inventory Consistency

库存扣减必须考虑并发。

推荐：

数据库事务
+
条件更新
或
乐观锁 version

示例原则：

available_stock >= roomCount

只有满足条件才允许扣减。

禁止：

先查询库存
再无条件 update

否则存在并发超卖风险。

---

## Booking Order

BookingOrder 表示一次酒店预订。

推荐字段：

- id
- orderNo
- hotelId
- roomTypeId
- guestName
- guestPhone
- checkInDate
- checkOutDate
- roomCount
- nightCount
- totalAmount
- status
- createdAt
- updatedAt

订单应保存关键业务快照，避免后续酒店或房型信息变化影响历史订单。

可保存：

- hotelNameSnapshot
- roomTypeNameSnapshot
- unitPriceSnapshot

---

## Order Status

推荐订单状态：

- BOOKED
- CHECKED_IN
- CHECKED_OUT
- CANCELLED

合法状态迁移：

BOOKED -> CHECKED_IN
BOOKED -> CANCELLED
CHECKED_IN -> CHECKED_OUT

禁止：

CHECKED_OUT -> BOOKED
CANCELLED -> CHECKED_IN
CHECKED_IN -> BOOKED

状态修改必须通过业务方法，而不是任意 setStatus。

---

## Check In Rule

办理入住前必须：

1. 查询订单
2. 校验订单存在
3. 校验订单状态为 BOOKED
4. 校验当前业务规则允许入住
5. 更新为 CHECKED_IN

重复入住必须拒绝。

---

## Check Out Rule

只有：

CHECKED_IN

状态允许办理离店。

成功后：

CHECKED_IN -> CHECKED_OUT

---

## Product / SKU

如果需要扩展商品体系：

Hotel
-> RoomType
-> Product
-> SKU

RoomType：

描述物理房型。

Product：

描述商家实际销售的商品。

SKU：

描述具体售卖规格，例如：

- 双早 + 可取消
- 无早 + 可取消
- 双早 + 不可取消

日期价格与库存不建议通过每天新建 SKU 实现。

动态属性应进入 Calendar 模型。

---

## Design Principles

优先保证：

1. 领域含义清晰
2. 日期库存正确
3. 状态流转明确
4. 并发不超卖
5. 历史订单可追溯

MVP 阶段避免过度设计。

若需求只需要：

预订
订单查询
办理入住

则不强制引入：

- 支付
- 会员
- 营销
- 优惠券
- 复杂 RatePlan
- 多酒店集团
- PMS 对接
