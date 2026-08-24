我没有把 AI Coding 理解成让大模型一次性生成整个项目，而是按照真实研发流程拆成 Requirement、Architect、Developer、Tester、Reviewer 五类 Agent。
Requirement Agent 负责将产品的一句话需求结构化；Architect Agent 基于需求进行领域、数据库和 API 设计；Developer Agent 严格依据设计编码；Tester Agent 从需求视角独立生成和执行测试；Reviewer Agent 从架构、事务、并发和代码质量角度 Review。
Agent 之间通过项目中的 Markdown 文档交接，而不是依赖大模型上下文，因此整个研发过程是可追踪、可复现的。
同时把酒店日历库存、Spring Boot 开发规范、测试规范等沉淀成 Skill，让不同 Agent 可以复用。这样 Agent 是工作角色，Skill 是标准化能力，最终组成一个完整的 AI Coding 软件研发流程。
# 酒店预订与入住 Demo

一个可运行的前后端酒店系统，包含日期库存查询、房型查询、预订、顾客查单、前台登录和办理入住。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Data JPA、H2、COLA StateMachine
- 前端：React、TypeScript、Vite、Ant Design

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址为 `http://localhost:8080`，首次启动会初始化一间酒店、两种房型以及未来 180 天库存。

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址为 `http://localhost:5173`，Vite 会将 `/api` 代理到后端。

演示前台账号：

- 用户名：`frontdesk`
- 密码：`Hotel@123`

## 构建与测试

```bash
cd backend && mvn test
cd frontend && npm run build
```

H2 使用文件模式，运行数据保存在 `backend/data/`，该目录不会提交到 Git。
