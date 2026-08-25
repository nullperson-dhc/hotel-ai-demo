# Hotel AI Demo 项目说明

## 提交内容 Checklist

- [x] 完整的可运行代码
  - 后端：`backend/`
  - 前端：`frontend/`
- [x] 与 AI 沟通的原始会话记录
  - `docs/ai-chat-log.md`
- [x] 使用的 Skills
  - 位于 `.agents/skills/` 目录
  - Codex 在研发流程各阶段实际加载并使用了对应 Skills
- [x] 使用的 Agents
  - 主要由 Codex 分别扮演以下五类 Agent：
    - Requirement Agent
    - Architect Agent
    - Developer Agent
    - Tester Agent
    - Reviewer Agent
  - Agent 定义位于 `agents/` 目录
- [x] 过程文档
  - 需求方案
  - 技术方案
  - 数据库与 API 设计
  - 测试用例与测试报告
  - 相关文档位于 `docs/` 目录
- [x] 产品效果
  - 产品截图或录屏位于 `test-photo/` 目录
- [x] 实现思路与技术亮点总结

## 实现思路

我没有把 AI Coding 简单理解为让大模型一次性生成整个项目，而是参照真实的软件研发流程，将项目拆分为五类 Agent：

```text
Requirement → Architect → Developer → Tester → Reviewer
```

各 Agent 的职责如下：

| Agent | 主要职责 |
| --- | --- |
| Requirement Agent | 将产品的一句话需求转化为结构化、可验收的需求文档 |
| Architect Agent | 基于需求完成领域模型、系统架构、数据库和 API 设计 |
| Developer Agent | 严格依据需求与技术设计完成前后端代码实现 |
| Tester Agent | 从需求视角独立设计测试用例、执行测试并输出测试报告 |
| Reviewer Agent | 从架构、事务、并发、安全和代码质量等角度进行独立 Review |

Agent 之间通过项目中的 Markdown 文档进行交接，而不是依赖大模型的临时上下文。因此，整个研发过程具备以下特点：

- 可追踪
- 可复现
- 可审查
- 可持续迭代

同时，我将酒店日历库存、Spring Boot 开发规范、React 开发规范和 Java 代码审查规则等能力沉淀为 Skills，使不同 Agent 能够在相应阶段重复加载和复用。

> Agent 是研发流程中的工作角色，Skill 是可复用的标准化能力。两者结合，构成一套完整的 AI Coding 软件研发流程。

## 技术亮点

本项目的技术亮点主要集中在业务一致性、工程规范和 AI 多角色协作三个方面。

### 1. 完整的酒店业务闭环

项目覆盖酒店 MVP 的核心流程：

- 可订房型查询
- 多晚预订与库存扣减
- 顾客订单查询
- 前台登录和订单检索
- 办理入住
- 重复入住防护

前后端可以独立运行，并已完成主体流程验收。

### 2. 按日期管理酒店库存

库存不是简单存放在房型上的单一数字，而是按照以下维度建立库存记录：

```text
房型 + 营业日期
```

入住区间采用 `[入住日, 离店日)`，支持连续多晚库存判断，并保证离店日不占用库存。

### 3. 防止并发超卖

库存扣减使用数据库原子条件更新：

```sql
UPDATE room_inventory
SET available_stock = available_stock - :count
WHERE room_type_id = :roomTypeId
  AND biz_date = :date
  AND available_stock >= :count;
```

库存校验与扣减在同一条 SQL 中完成，避免“先查询、再扣减”产生的并发竞争窗口。

同时，多日库存扣减与订单创建处于同一事务中，任意一天库存不足都会整体回滚。

### 4. 请求幂等设计

创建预订要求携带 `Idempotency-Key`：

- 相同键、相同请求返回原订单
- 相同键、不同请求返回幂等冲突
- 数据库唯一约束防止重复订单
- 网络中断后可以使用原键安全重试

### 5. 状态机管理订单流转

项目使用 COLA 状态机定义订单状态迁移：

```text
BOOKED → CHECKED_IN
```

通过比较 `fireEvent` 前后的状态判断事件是否真正产生迁移，能够通用地识别重复入住和非法状态操作，避免在 Service 中重复硬编码目标状态。

### 6. 数据库与领域双重并发保护

- 库存使用条件更新防止超卖
- 库存与订单实体包含 `@Version`
- 订单状态通过领域方法和状态机控制
- 数据库为订单号、幂等键和库存日期等字段设置唯一约束

业务规则、事务和数据库约束共同构成多层保护。

### 7. 安全与隐私处理

- 前台使用 Spring Security Session 认证
- 密码使用 BCrypt 保存
- 后台接口限制 `ROLE_STAFF`
- 顾客查单必须同时匹配订单号和手机号
- API 只返回脱敏手机号
- DTO 与 JPA Entity 完全分离
- 错误响应不暴露 SQL、堆栈和内部结构
- 每个请求具有 `X-Request-Id` 和 `traceId`

### 8. 统一的 API 错误契约

业务异常使用统一结构：

```json
{
  "code": "INVENTORY_INSUFFICIENT",
  "message": "所选日期库存不足",
  "traceId": "..."
}
```

错误码保持稳定，便于前端分支处理和后端日志排查。

### 9. 可测试的时间设计

业务代码通过注入 `Clock` 获取酒店当前时间，并固定使用 `Asia/Shanghai` 时区。

这使入住日、离店日和过期判断可以稳定测试，避免测试直接依赖机器系统时间。

### 10. Maven 与自动化代码格式管理

后端使用 Maven Wrapper 固定 Maven 版本，并通过 Spotless 管理 Java 格式：

```bash
./mvnw spotless:apply
./mvnw verify
```

这样可以在 IntelliJ IDEA、命令行和 CI 环境中获得一致的构建与代码格式结果。

### 11. 核心场景自动化覆盖

后端自动化测试覆盖：

- 正常预订
- 多日库存扣减
- 库存不足整体回滚
- 非法入住日期
- 顾客隐私查单
- 幂等重试
- 重复办理入住
- 提前及过期入住
- 并发抢订最后一间房
- 状态机合法迁移

当前后端测试为 10/10 通过，前端 TypeScript 编译和生产构建也已通过。

### 12. AI 多角色工程流程

项目按照独立 Agent 角色推进：

```text
Requirement → Architect → Developer → Tester → Reviewer
```

每个阶段都有明确产物，包括需求、技术设计、数据库设计、API 设计、测试用例、测试报告和代码审查报告。项目不仅生成了可运行代码，也保留了完整、可追溯的软件工程过程。
