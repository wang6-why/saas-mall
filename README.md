# SaaS Mall — 多租户电商 SaaS 平台

Spring Boot 2.7 + MyBatis-Plus + Redis + RabbitMQ + Vue3 + MySQL。共享库共享表方案，MyBatis-Plus `TenantLineInnerInterceptor` 行级租户隔离，业务代码零侵入；内置高并发秒杀链路（Lua 预扣 + MQ 异步落库 + 乐观锁防超卖）。

## 架构

```
┌──────────────┐   HTTP    ┌───────────────────────────────────────┐
│  Vue3 前端    │ ────────→ │  Spring Boot (8081)                   │
│  (9002)      │           │  JwtInterceptor → ThreadLocal tenantId │
└──────────────┘           │  TenantLineInnerInterceptor            │
       │ 轮询              │  自动拼接 WHERE tenant_id = ?           │
       │                   └───────┬───────────────┬───────────────┘
       │  GET /seckill/{id}/status │               │ MQ 异步下单
       └───────────────────────────┤               ↓
                                   ↓        ┌──────────────┐
                       ┌──────────────┐     │ RabbitMQ      │
                       │ Redis 7      │←───→│ seckill.queue │
                       │ Lua 预扣库存  │     └──────┬───────┘
                       │ 排队状态      │            │ Consumer
                       └──────────────┘            ↓
                                          MySQL (乐观锁扣库存 + 落订单)
```

## 核心亮点

### 1. 行级租户隔离（零侵入）
- 登录 JWT 携带 tenantId → `JwtInterceptor` 写入 ThreadLocal → `TenantLineInnerInterceptor` 给所有 SQL 自动追加 `tenant_id = ?`，查询/更新/删除全覆盖，单条操作（`removeById`）也防越权。
- `TenantContext.clear()` 在 `afterCompletion` 清理，防止线程池复用串租户；MQ 消费线程同样显式 set/clear。
- `tenant` 平台级表在 `SaasTenantLineHandler#ignoreTable` 豁免。

### 2. 秒杀链路（压得住并发）
```
用户点击 → Lua 脚本原子判断[库存>0 且 未重复]并预扣 Redis 配额
        → 发 RabbitMQ 消息，接口立即返回"排队中"（不阻塞）
        → Consumer 落库：UPDATE product SET stock=stock-1 WHERE id=? AND stock>=1 乐观锁
        → 订单状态写 Redis，前端每 1.2s 轮询 → 排队中 → 成功/售罄/失败
        → 消费失败：回滚 Redis 配额 + 移除防重标记，用户可重试
```
三道防线防超卖：Redis Lua 原子预扣 → DB 乐观锁兜底 → Set 防同一用户重复抢。

## 启动

```bash
# 1. 基础设施（MySQL 自动执行 schema.sql，含演示数据）
docker compose up -d

# 2. 后端
cd backend && mvn spring-boot:run    # 8081

# 3. 前端
cd frontend && npm install && npm run dev   # http://localhost:9002
```

## 演示账号

| 租户 | 账号 | 密码 | 秒杀活动 |
|------|------|------|----------|
| 店铺A | adminA | 123456 | 键盘 199 元 限量10 |
| 店铺B | adminB | 123456 | 卫衣 99 元 限量5 |

两个账号登录后商品/订单/秒杀活动完全隔离。用 JMeter 对 `POST /seckill/{id}/buy` 压测即可演示：只有前 N 个请求成功，其余返回"售罄"，订单数不超配额。

## 面试话术清单

1. **为什么共享表**：成本低、运维简单；大客户可平滑迁独立库——替换 Handler 为数据源路由即可。
2. **为什么 Lua**：查库存+防重+扣减三步必须是原子的，Redis 单线程执行 Lua 天然串行。
3. **为什么 MQ**：把落库从秒杀峰值剥离，削峰填谷；接口只做准入判定，响应 ms 级。
4. **会不会超卖**：Redis 预扣挡住 99% 流量，DB 乐观锁兜底极端场景；消费失败回滚 Redis 配额保证不"吞库存"。
5. **ThreadLocal 坑**：Tomcat 线程池复用，不清会串租户；MQ 消费线程是独立线程，同样要手动 set。

## 目录

```
sql/schema.sql            建表 + 演示数据（含秒杀活动）
docker-compose.yml        mysql + redis + rabbitmq 一键起
backend/                  Spring Boot（config / seckill / mq / entity / mapper / service / controller）
frontend/                 Vue3 + Vite（赛博朋克 UI，秒杀页含倒计时与轮询）
```
