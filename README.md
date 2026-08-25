# 酒店预订与入住 Demo

一个可运行的前后端酒店系统，包含日期库存查询、房型查询、预订、顾客查单、前台登录和办理入住。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Data JPA、H2、COLA StateMachine
- 前端：React、TypeScript、Vite、Ant Design

## 启动后端

```bash
cd backend
./mvnw spring-boot:run
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
cd backend && ./mvnw verify
cd frontend && npm run build
```

H2 使用文件模式，运行数据保存在 `backend/data/`，该目录不会提交到 Git。
