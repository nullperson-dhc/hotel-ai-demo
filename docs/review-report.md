# 酒店预订与入住系统代码审查报告

## 1. 审查结论

当前实现的主要业务链路清晰，Controller 未直接返回 Entity，预订创建与逐日库存扣减处于同一事务，库存扣减使用数据库条件更新，现有实现未发现可导致库存负数或部分扣减提交的路径。

本次发现 **2 个 P1、5 个 P2，未发现 P0/P3 问题**。其中并发办理入住的错误语义不符合需求与技术方案，建议作为最高优先级修复。员工写接口 CSRF 校验已被明确排除在本期范围外，因此本报告不将其列为问题。

## 2. 问题清单

### [P1] 并发办理入住未使用状态条件更新，竞争失败返回 500

File:
`backend/src/main/java/com/example/hotel/service/StaffBookingService.java`

Location:
`checkIn()`，第 55-77 行

Problem:
方法先读取订单，再在内存中判断状态并修改受 `@Version` 保护的实体。两个并发事务可以同时读取 `BOOKED`，同时通过状态机和日期校验；其中一个事务提交后，另一个在事务提交阶段触发乐观锁异常。当前统一异常处理未将该异常转换为业务冲突，因此失败请求会返回 `500 INTERNAL_ERROR`，而不是技术方案要求的状态条件更新结果及 `409 ORDER_STATUS_CONFLICT`。

Impact:
违反 FR-06、BR-05 及“重复或并发办理同一订单入住最多一个成功”的错误契约；调用方会把可识别的业务竞争误判成系统故障。

Recommendation:
在 Repository 中实现 `where order_no = ? and status = BOOKED`（必要时同时校验 version）的原子条件更新，并检查影响行数；更新数为 0 时重新读取并映射为不存在或 `ORDER_STATUS_CONFLICT`。补充两个独立事务并发入住同一订单的集成测试。

### [P1] 手工登录流程绕过 Spring Security 会话固定防护

File:
`backend/src/main/java/com/example/hotel/controller/SessionController.java`

Location:
`login()`，第 32-47 行

Problem:
Controller 直接调用 `AuthenticationManager`，随后把 `SecurityContext` 写入当前已有 Session，但没有更换 Session ID。该流程没有经过标准认证过滤器的 `SessionAuthenticationStrategy`，技术方案和 API 设计要求的“登录成功后更新会话标识”未落实。

Impact:
攻击者若能在登录前固定受害者的 Session ID，登录后可能继续利用同一会话标识，形成会话固定风险。

Recommendation:
登录成功时显式调用 `request.changeSessionId()`，或将认证交给配置了 `SessionFixationProtectionStrategy` 的标准 Spring Security 登录流程。增加断言登录前后 Session ID 不同的 MockMvc 测试。

### [P2] Session Controller 直接依赖 Repository，破坏分层边界

File:
`backend/src/main/java/com/example/hotel/controller/SessionController.java`

Location:
类依赖及 `response()`，第 19-24、61-76 行

Problem:
Web 层直接注入并查询 `StaffUserRepository`。这与技术方案明确规定的“Web 层不直接操作 Repository”相冲突，也把会话 DTO 组装和员工查询逻辑留在 Controller。

Impact:
认证用例难以复用和独立测试，后续增加员工状态、权限或审计规则时容易在 Web 层继续堆积业务逻辑。

Recommendation:
引入会话/认证应用服务，由 Controller 仅处理 HTTP 输入输出和认证上下文；Repository 访问及员工展示信息映射放入应用服务。

### [P2] 多类请求参数异常会被统一映射为 500

File:
`backend/src/main/java/com/example/hotel/exception/GlobalExceptionHandler.java`

Location:
异常处理器，第 21-41 行

Problem:
当前只处理请求体 Bean Validation 和 JSON 解析。缺少查询参数缺失、类型转换失败、方法参数约束失败等常见异常的 400 映射。例如缺少 `availability` 日期参数、`roomCount=abc`、分页参数越界等可能进入兜底 `Exception` 处理器并返回 `500 INTERNAL_ERROR`。

Impact:
合法的客户端输入错误被报告为服务端故障，不符合统一错误契约，也会污染错误监控。

Recommendation:
补充 `MissingServletRequestParameterException`、`MethodArgumentTypeMismatchException`、`HandlerMethodValidationException`（或当前 Spring 版本实际抛出的约束异常）映射，统一返回 `400 VALIDATION_ERROR` 或 `INVALID_STAY_PERIOD`，并增加 Controller 层契约测试。

### [P2] 可订房型查询对每个房型单独查询库存

File:
`backend/src/main/java/com/example/hotel/service/AvailabilityService.java`

Location:
`find()`，第 40-67 行

Problem:
先查询全部有效房型，再在 Stream 的 `filter` 中逐房型查询日期库存，形成典型的 `1 + N` 查询。目标上限 100 个房型时，一次可订查询最多产生约 101 次 SQL。

Impact:
数据库往返随房型数线性增长，可能影响 FR-01 的 P95 小于 2 秒目标；并发查询时会放大数据库压力。

Recommendation:
使用一次聚合查询按房型统计区间库存记录数和最小可用库存，或一次批量加载所有房型的区间库存后在内存分组过滤。补充查询次数或目标规模性能测试。

### [P2] 同一幂等键并发首提的失败请求返回内部错误

File:
`backend/src/main/java/com/example/hotel/service/BookingService.java`

Location:
`create()`，第 40-112 行

Problem:
幂等处理采用“先查键、再扣库存、最后插入订单”。相同键的两个并发首请求都可能查询为空并执行扣减；数据库唯一约束能使其中一个事务回滚，因此不会重复扣库存，但唯一键冲突没有转换为读取已创建订单并返回幂等结果，失败请求会落入 `500 INTERNAL_ERROR`。

Impact:
数据一致性能够保持，但并发重试的 API 语义不稳定，客户端可能继续盲目重试，违反技术方案中同幂等键安全重试的目标。

Recommendation:
将幂等键建模为可抢占的独立记录，或捕获唯一约束冲突后在新事务中读取已提交订单：摘要一致返回原订单，摘要不同返回 `IDEMPOTENCY_CONFLICT`。增加同一幂等键双线程首提测试。

### [P2] API、认证与并发入住测试覆盖明显不足

File:
`backend/src/test/java/com/example/hotel/HotelAcceptanceTest.java`

Location:
当前后端测试集

Problem:
现有 10 个测试主要直接调用 Service，未覆盖 HTTP 参数绑定、统一错误结构、登录失败、未认证访问、退出会话、会话固定防护、Request ID，以及两个独立事务并发办理入住。因而上述 HTTP 500 和会话问题无法被现有绿色测试发现。

Impact:
测试通过不能证明 FR-04、FR-05、FR-06 的接口与安全契约成立，回归风险较高。

Recommendation:
按技术方案补充 MockMvc API 测试，并使用独立线程和独立事务覆盖并发入住；至少断言 HTTP 状态、错误码、traceId、认证前后 Session ID 和会话失效行为。

## 3. 已确认的正向结论

- Controller、DTO 与 JPA Entity 已分离，没有直接序列化实体。
- 创建预订的库存扣减与订单保存处于同一公共 `@Transactional` 方法内，异常会回滚此前日期的扣减。
- 库存扣减使用 `availableStock >= count` 的数据库原子条件更新，能够防止负库存和普通并发超卖。
- 金额使用 `BigDecimal`，订单保存酒店、房型、床型、单价与总额快照。
- 顾客查单同时匹配订单号和完整手机号，响应只返回脱敏手机号。
- 状态机只配置 `BOOKED -> CHECKED_IN`，重复串行入住已能返回冲突。
- 全局异常响应不会向客户端直接暴露堆栈或 SQL，服务端日志通过 traceId 关联。

## 4. 修复优先级建议

1. 先修复并发入住条件更新与冲突映射，并增加并发测试。
2. 修复登录会话固定防护并补齐认证契约测试。
3. 完善请求参数异常映射和同幂等键并发语义。
4. 消除可订查询 `1 + N`，在目标数据量下执行性能验证。
5. 调整 Session Controller 分层，逐步补齐 API 自动化覆盖。
