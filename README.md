我没有把 AI Coding 理解成让大模型一次性生成整个项目，而是按照真实研发流程拆成 Requirement、Architect、Developer、Tester、Reviewer 五类 Agent。
Requirement Agent 负责将产品的一句话需求结构化；Architect Agent 基于需求进行领域、数据库和 API 设计；Developer Agent 严格依据设计编码；Tester Agent 从需求视角独立生成和执行测试；Reviewer Agent 从架构、事务、并发和代码质量角度 Review。
Agent 之间通过项目中的 Markdown 文档交接，而不是依赖大模型上下文，因此整个研发过程是可追踪、可复现的。
同时把酒店日历库存、Spring Boot 开发规范、测试规范等沉淀成 Skill，让不同 Agent 可以复用。这样 Agent 是工作角色，Skill 是标准化能力，最终组成一个完整的 AI Coding 软件研发流程。