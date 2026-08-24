---
name: springboot-development
description: 使用 Java 17、Spring Boot 3、Spring Data JPA 等技术开发酒店项目后端，包括 REST API、分层、事务、校验、异常处理和库存一致性。适用于 backend/ 下的后端实现；不用于纯前端任务。
---

# Spring Boot Development Skill

## Purpose

用于基于 Java 17 + Spring Boot 3 开发规范、清晰、可运行的后端服务。

适用于：

- REST API
- Service
- Repository
- Transaction
- Validation
- Exception Handling
- JPA
- H2 / MySQL

---

## Recommended Stack

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Jakarta Validation
- H2 Database
- Lombok 可选
- Maven

---

## Package Structure

推荐：

com.example.hotel

├── controller
├── service
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── domain
├── exception
├── config
└── HotelApplication

禁止把所有代码放在同一个 package。

---

## Layer Responsibility

### Controller

负责：

- HTTP 参数接收
- 参数校验
- 调用 Service
- 返回 Response

禁止：

- 写复杂业务逻辑
- 直接操作 Repository
- 自己开启事务

---

### Service

负责：

- 核心业务逻辑
- 领域校验
- 事务控制
- 状态流转
- 多 Repository 协同

核心业务事务放在 Service 层。

---

### Repository

负责：

- 数据访问
- 查询
- 持久化

禁止在 Repository 中混入业务状态判断。

---

## DTO Rule

请求使用 Request DTO。

响应使用 Response DTO。

不要直接把 Entity 当成 API 入参或出参。

例如：

CreateBookingRequest

BookingResponse

---

## Validation

使用 Jakarta Validation。

例如：

@NotNull
@NotBlank
@Positive
@FutureOrPresent

Controller 参数使用：

@Valid

复杂业务校验放 Service。

例如：

checkOutDate > checkInDate

不能只依赖注解完成。

---

## Transaction

涉及：

- 扣库存
- 创建订单

必须使用同一个事务。

例如：

@Transactional
public BookingResponse createBooking(...)

事务边界应尽可能放在 Service 层业务方法。

避免：

Controller 使用 @Transactional。

---

## Exception Handling

定义统一业务异常：

BusinessException

建议包含：

- errorCode
- message

统一使用：

@RestControllerAdvice

处理：

- BusinessException
- MethodArgumentNotValidException
- EntityNotFoundException
- Exception

禁止把异常堆栈直接暴露给前端。

---

## API Response

建议统一结构：

{
"code": "SUCCESS",
"message": "success",
"data": {}
}

错误示例：

{
"code": "INVENTORY_NOT_ENOUGH",
"message": "库存不足",
"data": null
}

---

## Entity

Entity 只负责持久化映射。

字段明确：

- @Id
- @GeneratedValue
- @Column

日期推荐：

LocalDate

时间推荐：

LocalDateTime

金额推荐：

BigDecimal

禁止：

double price

---

## Money

金额必须使用：

BigDecimal

禁止使用：

float
double

涉及金额计算时明确 scale 和 rounding mode。

---

## Inventory Update

库存扣减避免：

find -> set -> save

作为唯一并发保护手段。

推荐：

条件更新 SQL：

UPDATE room_inventory
SET available_stock = available_stock - :count
WHERE room_type_id = :roomTypeId
AND biz_date = :bizDate
AND available_stock >= :count

返回更新行数。

更新行数 != 1 时：

抛出库存不足异常。

或者使用：

@Version

实现乐观锁。

---

## Logging

重要业务节点记录日志：

- 创建订单
- 扣减库存失败
- 办理入住
- 状态流转失败

日志中不要输出敏感信息。

---

## Initialization

Demo 项目应提供初始化数据。

推荐：

data.sql

或 CommandLineRunner。

应至少初始化：

- 1 个酒店
- 2 个房型
- 未来若干天库存

保证项目启动后即可演示。

---

## Build Requirement

后端项目必须使用 Maven 管理，保留可被 IntelliJ IDEA 直接导入的 `pom.xml`；项目提供 Maven Wrapper 时优先使用 `./mvnw`，避免依赖开发机的 Maven 版本。

Java 源码必须保持标准换行和缩进，不得把 package、import、类、字段或方法压缩在同一行。使用 Maven Spotless 作为唯一格式化基准：

```bash
./mvnw spotless:apply
./mvnw spotless:check
```

新增或修改 Java 代码后必须先格式化，再执行测试和构建。IDE 中的格式化规则不得与 Maven Spotless 冲突。

开发完成必须至少执行：

```bash
./mvnw test
./mvnw verify
```

确保格式检查、测试和项目构建全部成功。

---

## Definition of Done

一个后端功能完成必须满足：

1. API 可调用
2. 参数校验完整
3. Service 有业务校验
4. 事务边界正确
5. 异常统一
6. Entity 不直接暴露
7. 单元测试或集成测试覆盖核心路径
8. Maven Wrapper 可用，IDEA 可正常导入 `pom.xml`
9. Spotless 格式检查通过，Java 源码缩进可读
10. Maven 测试与构建成功
