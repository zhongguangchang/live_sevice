# 生活服务网

一个面向「上门服务 / 到店服务」的预约平台，用户在线选时段下单，平台派单给师傅，
师傅上门服务、用户评价，全流程在管理端可见可管。

项目由黑马程序员的**苍穹外卖**教学项目改造而来：外卖的「菜品 - 套餐 - 购物车 - 订单」
语义全部替换为「服务项目 - 服务套餐 - 服务清单 - 服务预约」，并新增了
师傅、排期、评价、服务区域等真实业务里必须有的模块。

---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.7.3 · MyBatis · PageHelper · Knife4j · Apache POI · **JDK 17** |
| 存储 | MySQL（`life_service` 库，16 张表）· Redis（防超卖、缓存、限流、分布式锁）· RabbitMQ（延迟队列） |
| 管理端 | Vue 2 · TypeScript · Element UI · ECharts |
| 小程序端 | uni-app（**尚未改造**，见文末「未完成」） |

## 二、目录结构

```
D:\campus\
├─ live_service\                       后端（Maven 多模块）
│   ├─ live-common\    通用：常量、异常、JWT 工具、结果封装、缓存工具
│   ├─ live-pojo\      实体 / DTO / VO
│   ├─ live-server\    启动类、Controller、Service、Mapper、MQ、定时任务、切面
│   ├─ db\             建表 SQL + 演示数据 SQL
│   ├─ docs\           设计文档（数据库 / 模块与业务 / 环境与启动）
│   └─ tools\          smoke-test.ps1 一键冒烟测试
├─ 前端源码\...\project-sky-admin-vue-ts\   管理端（11 个页面）
└─ 微信小程序源代码\                         小程序（未改造）
```

## 三、快速开始

### 1. 环境要求

| 组件 | 地址 | 账号 / 密码 |
|---|---|---|
| JDK | `D:\jdk17\jdk-17.0.2`（必须是 17） | — |
| MySQL | `127.0.0.1:3306` | `root` / `123456` |
| Redis | `192.168.229.129:6379` | `123456`，db `10` |
| RabbitMQ | `192.168.229.129:5672` | `admin` / `123456`，vhost `/` |

> ⚠ **启动顺序**：先开虚拟机、确保 Redis 在跑。项目用了 Redisson，
> Redis 不通时后端**直接启动失败**（报 `Unable to connect to Redis server`）。

### 2. 初始化数据库（首次）

```powershell
cd D:\campus\live_service\db
mysql -uroot -p123456 < life_service.sql          # 建库建表
mysql -uroot -p123456 < life_service_demo_data.sql # 演示数据（会清空业务表后重填）
```

### 3. 启动后端

```powershell
cd D:\campus\live_service\live-server
$env:JAVA_HOME='D:\jdk17\jdk-17.0.2'
mvn spring-boot:run       # 端口 8081，接口文档 http://localhost:8081/doc.html
```

### 4. 启动管理端

```powershell
cd "D:\campus\前端源码\生活服务网前端源码\project-sky-admin-vue-ts"
npm run serve             # 端口 8888，账号 admin / 123456
```

## 四、数据库（16 张表，7 个域）

| 域 | 表 |
|---|---|
| 用户与权限 | `employee` `user` |
| 服务商品 | `category` `service_item` `service_spec` `service_package` `service_package_item` |
| 服务人员 | `provider` `provider_skill` |
| 排期时段 | `slot` ★核心 |
| 交易 | `shopping_cart` `service_order` `service_order_item` `address_book` |
| 评价 | `review` |
| 服务区域 | `service_area` |

`slot`（排期）是整个库存模型的载体：**一个师傅 + 一天 + 一个时间段 = 一个可预约名额**。
防超卖防的就是它的名额，而不是商品数量。详见 [01-数据库设计.md](live_service/docs/01-数据库设计.md)。

## 五、模块与接口

| 模块 | 接口前缀 | 说明 |
|---|---|---|
| 认证权限 | `/admin/employee` `/user/user` | 两套独立 JWT（`token` / `authentication`） |
| 服务商品 | `/admin/serviceItem` `/admin/servicePackage` `/admin/category` `/user/service` `/user/category` | 服务项目（含规格加价）、套餐、分类 |
| 服务人员 | `/admin/provider` | 师傅档案、技能、接单状态、评分 |
| 排期时段 | `/admin/slot` `/user/slot` | 批量生成排期、开关时段、缓存预热、库存对账 |
| 订单交易 | `/admin/serviceOrder` `/user/serviceOrder` | 下单、支付、派单、接单、开始、完成、取消、核销 |
| 评价 | `/admin/review` `/user/review` | 三维打分、回写师傅与服务评分、商家回复 |
| 地址与区域 | `/user/addressBook` `/admin/serviceArea` | 上门地址、开通区域校验 |
| 统计报表 | `/admin/report` | 经营概览、营业额、用户、订单、销量 Top10、Excel 导出 |
| 基础设施 | `/admin/shop` `/admin/common/upload` | 营业状态、图片上传、WebSocket 推送 |

完整清单见 [02-模块与业务设计.md](live_service/docs/02-模块与业务设计.md)。

## 六、核心业务流程

```
浏览分类/服务 → 选师傅时段(slot) → 加服务清单 → 提交订单
   │  Redis Lua 原子预扣名额
   ▼
待付款 ──15 分钟未支付──▶ MQ 延迟消息 → 自动取消 + 回补名额
   │ 支付（MySQL CAS 落账）
   ▼
待接单 ──5 分钟无人接单──▶ MQ 延迟消息 → 自动转派（最多 3 次，超出转人工）
   │ 接单 → 开始服务 → 完成
   ▼
待评价 ──7 天未评价──▶ MQ 延迟消息 → 自动结单
   │ 评价
   ▼
已完成（回写师傅评分、好评率、服务销量）
```

## 七、技术亮点

| 亮点 | 实现要点 |
|---|---|
| **Redis 原子预扣防超卖** | 「校验库存 + 扣减 + 记录用户」写在一个 Lua 脚本里，靠 Redis 单线程保证原子性。实测 10 并发抢 1 个名额：1 单成功、9 单提示约满 |
| **Redis + MySQL 双层库存** | Redis 挡并发、MySQL 用 `where booked_count < total_stock` 的 CAS 落账兜底，两者靠定时对账保持一致（以 MySQL 为权威） |
| **MQ 延迟队列四场景** | TTL + 死信队列实现未支付取消 / 未接单转派 / 未评价结单，加一条消息级 TTL 的服务前提醒。实测 5 分钟精确触发转派 |
| **幂等与消息可靠性** | 所有状态流转走 `where id=? and status=?` 的 CAS；生产者确认 + 手动 ACK + 消费者幂等；事务提交后再发消息 |
| **缓存三兄弟** | 空值缓存防穿透、SETNX 互斥重建防击穿、随机 TTL 防雪崩；写操作统一失效 |
| **接口限流** | `@RateLimit` 注解 + AOP，Redis INCR 计数器，按用户维度 |
| **分布式锁** | Redisson `RLock` 保证定时任务在多实例下只有一个实例执行 |
| **实时推送** | WebSocket 推送来单提醒与服务前提醒，消息格式统一在 `AdminNotifier` 拼装 |

## 八、测试

```powershell
cd D:\campus\live_service
pwsh -File .\tools\smoke-test.ps1 -SkipSlow   # 约 10 秒
pwsh -File .\tools\smoke-test.ps1             # 完整版，含 5 分钟延迟队列实测
```

覆盖 156 项检查：管理端 / 用户端全部只读接口、增删改、鉴权与越权、
完整下单闭环、取消与名额回补、并发防超卖、MySQL CAS 兜底、
MQ 延迟队列与真实转派、接口限流、缓存失效、统计报表。

## 九、未完成

| 项 | 说明 |
|---|---|
| 小程序端 | 首页分类、服务详情、选时段下单、订单进度、评价。后端接口已就绪，只差前端页面 |
| 师傅端接口 | 目前「接单 / 拒单 / 开始 / 完成」放在管理端；独立师傅端需要单独登录方式与接口分组 |
| 微信登录 / 支付 | `WeChatPayUtil`、`WeChatProperties` 已保留，接真实小程序时打通授权与支付回调 |
| 报表按师傅维度 | 现在只有服务维度 Top10，还没有「师傅接单量 / 好评率排行」 |
