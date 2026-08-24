---
name: react-development
description: 使用 React、TypeScript、Vite 和 Ant Design 开发酒店 Demo 前端，包括页面、API 集成、表单、状态处理和构建验证。适用于 frontend/ 下的前端实现；不用于纯后端任务。
---

# React Development Skill

## Purpose

用于酒店 Demo 系统的 React 前端开发。

目标：

- 页面简洁
- 功能完整
- 易于现场演示
- 不追求复杂视觉设计

---

## Recommended Stack

- React
- TypeScript
- Vite
- Ant Design
- Axios
- React Router

---

## Page Structure

最小功能页面：

1. Home
2. BookingPage
3. OrderQueryPage
4. CheckInPage

可选：

5. OrderDetailPage

---

## Project Structure

src/

├── api
├── components
├── pages
├── types
├── router
├── utils
├── App.tsx
└── main.tsx

---

## API Layer

所有 HTTP 请求集中放到 api 目录。

禁止页面直接散落 axios 调用。

例如：

api/booking.ts

export const createBooking = ...

export const getBooking = ...

export const checkIn = ...

---

## TypeScript

所有核心请求响应定义 Type。

例如：

export interface BookingRequest

export interface BookingResponse

禁止大量使用：

any

---

## Booking Page

页面至少包含：

- 入住日期
- 离店日期
- 房型选择
- 房间数量
- 入住人姓名
- 手机号
- 提交预订

预订成功后展示：

- 订单号
- 房型
- 入住日期
- 离店日期
- 总金额
- 状态

---

## Order Query Page

支持：

按订单号查询。

查询结果至少展示：

- 订单号
- 酒店
- 房型
- 入住人
- 入住日期
- 离店日期
- 房间数
- 总金额
- 状态

查询失败应展示明确错误信息。

---

## Check In Page

输入：

订单号

执行：

办理入住

成功后展示：

入住成功

和最新订单状态。

重复入住时：

展示后端业务错误。

---

## UI Principles

使用 Ant Design：

- Card
- Form
- Input
- DatePicker
- Select
- Button
- Table
- Descriptions
- Result
- message

保持 UI 简洁。

避免为了展示效果引入复杂状态管理框架。

MVP 不需要 Redux。

---

## Loading State

所有接口调用必须有 loading 状态。

避免用户重复点击。

例如：

<Button loading={loading}>

---

## Error Handling

统一处理后端响应。

业务异常直接展示：

message.error(errorMessage)

禁止只在 console.error 中记录错误。

---

## Date Rule

前端应禁止：

checkOutDate <= checkInDate

但后端仍必须再次校验。

前端校验只用于提升用户体验，不能代替后端校验。

---

## Component Rule

页面内重复 UI 超过两处时抽取组件。

例如：

OrderStatusTag

OrderDetailCard

---

## Demo Data

不要在前端写死库存结果。

库存、订单状态、金额等业务数据必须来自后端。

---

## Definition of Done

页面完成必须满足：

1. 能正常访问
2. API 调用成功
3. loading 状态完整
4. 错误状态可见
5. TypeScript 无明显类型错误
6. npm build 成功
7. 可以完成完整业务闭环
