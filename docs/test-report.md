# 酒店预订与入住系统测试报告

## 1. 测试结论

测试阶段结论：**通过（存在非阻断风险）**。

主体预订闭环、库存事务回滚、订单隐私查询、入住日期边界、重复办理入住冲突和并发抢最后库存均通过。上一轮发现的 BUG-001 已修复并通过回归。经范围确认，员工写接口 CSRF 校验本期不实现，不计为缺陷。

## 2. 测试环境

- 日期：2026-08-25
- Java：运行环境 JDK 21，Maven 编译目标 Java 17
- 后端：Spring Boot 3.5.5、H2 内存数据库
- 前端：React、TypeScript、Vite 7.3.6
- 浏览器：主体流程已在 Chromium 内核浏览器人工体验

## 3. 执行结果

### 3.1 后端自动化测试

命令：

```bash
cd backend
MAVEN_USER_HOME=/tmp/hotel-wrapper-home ./mvnw -Dmaven.repo.local=/tmp/hotel-m2 verify
```

结果：**通过**（`BUILD SUCCESS`）。

- 测试总数：10
- 通过：10
- 失败：0
- 错误：0
- 跳过：0
- Spotless：39 个 Java 文件格式检查通过
- Maven：打包成功

已通过的重要场景：

- 正常预订、金额计算、三晚库存扣减及离店日不扣库存
- 库存不足时不创建订单且库存不变化
- 入住时间非法
- 连续多晚中间一天无库存时全部回滚
- 正确订单查询、错误手机号与不存在订单使用相同错误码
- 正常办理入住及入住不重复扣库存
- 重复办理入住返回 409 `ORDER_STATUS_CONFLICT`，状态与库存保持不变
- 提前入住和过期入住拒绝
- 两个并发请求抢最后一间房时仅一个成功，最终库存为 0
- 相同幂等键重试不重复创建订单
- COLA 状态机基础迁移

### 3.2 前端构建测试

命令：

```bash
cd frontend
npm run build
```

结果：**通过**。TypeScript 编译和 Vite 生产构建成功。

非阻断警告：主 JavaScript 包约 1.14 MB，超过 Vite 默认 500 KB 提示阈值，后续可通过路由懒加载和拆包优化。

### 3.3 人工端到端测试

已体验通过：可用房查询、创建预订、顾客查单、前台登录、前台查单、首次办理入住。

未完成：Edge 兼容性、手机屏幕响应式专项、10 万订单规模性能和 P95 指标测试。

## 4. Bug 回归

### BUG-001 重复办理入住未返回状态冲突

- 严重程度：高
- 当前状态：**已修复，回归通过**
- 复现步骤：
  1. 创建入住日为当天、状态为 `BOOKED` 的订单。
  2. 已登录前台第一次调用办理入住，订单变为 `CHECKED_IN`。
  3. 对同一订单再次调用办理入住。
- 预期结果：第二次返回 HTTP 409、错误码 `ORDER_STATUS_CONFLICT`；状态仍为 `CHECKED_IN`，无重复副作用。
- 本轮实际结果：第二次调用返回 `ORDER_STATUS_CONFLICT`；状态保持 `CHECKED_IN`，未产生重复库存副作用。
- 相关文件：
  - `backend/src/main/java/com/example/hotel/service/StaffBookingService.java`
  - `backend/src/main/java/com/example/hotel/config/OrderStateMachineConfig.java`
  - `backend/src/test/java/com/example/hotel/HotelAcceptanceTest.java`

修复验证：服务以事件触发前状态与状态机返回状态是否相等判断未发生迁移；自动化用例 `HotelAcceptanceTest.checkInSucceedsOnceAndRepeatIsRejected` 已通过。

## 5. 风险与建议

- BUG-001 已关闭；本轮完整后端回归通过。
- 员工写接口 CSRF 校验已确认为本期非目标，不影响本次测试结论；后续若用于生产环境，建议重新纳入安全基线。
- HTTP Controller 层的校验、认证、退出会话、错误结构和 Request ID 尚缺自动化覆盖，建议下一轮补充 MockMvc 集成测试。
- 性能指标尚未实测，本报告不能确认 NFR 性能验收通过。
