# Java Code Review Skill

## Purpose

用于对 Java / Spring Boot 项目进行独立代码审查。

Reviewer 默认假设：

代码可能存在问题。

不要因为测试通过就认为代码正确。

---

## Review Scope

重点检查：

1. 架构
2. 分层
3. 业务正确性
4. 事务
5. 并发
6. 数据一致性
7. 状态机
8. 异常处理
9. 性能
10. 可维护性
11. 安全
12. 测试覆盖

---

## Severity

### P0

严重故障：

- 数据损坏
- 大面积不可用
- 明确资金损失
- 严重安全问题

### P1

高风险：

- 库存超卖
- 事务失效
- 状态错误
- 核心业务逻辑错误

### P2

中风险：

- 性能问题
- 可维护性差
- 重复代码
- 异常处理不完整

### P3

低风险：

- 命名
- 格式
- 小范围优化建议

---

## Architecture Review

检查：

Controller 是否只负责 HTTP 层。

Service 是否承担业务逻辑。

Repository 是否只负责数据访问。

禁止：

Controller -> Repository

直接跨层操作。

---

## Transaction Review

重点检查：

库存扣减
+
订单创建

是否处于同一事务。

检查：

@Transactional 是否作用在 public 方法。

是否存在 self-invocation 导致事务失效。

是否错误 catch Exception 后不抛出，导致事务无法回滚。

---

## Concurrency Review

库存代码重点检查：

是否存在：

select stock
if stock > 0
update stock

这种无并发保护逻辑。

推荐：

条件更新
或
乐观锁
或
数据库锁

需要明确说明为什么不会超卖。

---

## State Review

订单状态必须经过合法流转。

检查：

是否随意：

order.setStatus(...)

应优先通过领域方法：

order.checkIn()

order.checkOut()

并在内部检查合法状态。

---

## Exception Review

检查：

是否有统一异常处理。

是否存在：

throw new RuntimeException("error")

到处散落。

业务异常是否有明确：

code
message

---

## Entity Review

检查：

Entity 是否直接作为 API DTO。

是否存在双向关联导致 JSON 无限递归。

金额是否 BigDecimal。

日期是否 LocalDate / LocalDateTime。

---

## Null Review

检查：

Optional 使用是否合理。

避免：

optional.get()

没有判断。

避免不必要的 null。

---

## Performance Review

重点关注：

- N+1 Query
- 全表扫描
- 循环内数据库查询
- 无索引查询
- 大量 save 单条写入
- 不必要的数据加载

---

## Validation Review

检查：

是否只依赖前端校验。

后端必须再次校验：

- 日期
- roomCount
- 状态
- 库存

---

## Security Review

Demo 至少检查：

- 不打印敏感信息
- 不返回异常堆栈
- SQL 是否参数化
- 输入是否经过基本校验

---

## Test Review

检查核心逻辑是否覆盖：

- 正常路径
- 库存不足
- 多日库存
- 非法日期
- 非法状态
- 并发库存

---

## Output Format

每个问题使用：

### [P1] Inventory update may oversell

File:
BookingService.java

Location:
createBooking()

Problem:
当前逻辑先查询库存再更新，两个并发请求都可能读取到相同库存。

Impact:
可能超卖。

Recommendation:
使用条件更新或 @Version 乐观锁，并检查 affected rows。