# SaaS Mall — 多租户电商 SaaS 平台

Spring Boot 2.7 + MyBatis-Plus + Vue3 + MySQL。共享库共享表方案，MyBatis-Plus `TenantLineInnerInterceptor` 实现行级租户隔离，业务代码零侵入。

## 架构

```
┌─────────────┐      ┌──────────────────────────────┐
│  Vue3 前端   │ HTTP │  Spring Boot 后端             │
│  (9002)     │ ───→ │  JwtInterceptor → ThreadLocal │
└─────────────┘      │  TenantLineInnerInterceptor   │
                     │  自动拼接 WHERE tenant_id = ?  │
                     └──────────────┬───────────────┘
                                    ↓
                              MySQL (saas_mall)
                        tenant / sys_user / product / orders
```

**租户隔离流程**：登录返回 JWT（含 tenantId）→ 每次请求 JwtInterceptor 解析 token 写入 `TenantContext`（ThreadLocal）→ MyBatis-Plus 租户拦截器自动为所有 SQL 追加 `tenant_id = ?`，查询/更新/删除/新增全覆盖，业务层无感知。

- `tenant` 表为平台级表，在 `SaasTenantLineHandler#ignoreTable` 中豁免。

## 启动

```bash
# 1. 初始化数据库（MySQL 5.7+）
mysql -uroot -p < sql/schema.sql

# 2. 启动后端（默认端口 8081，改连接串见 backend/src/main/resources/application.yml）
cd backend && mvn spring-boot:run

# 3. 启动前端
cd frontend && npm install && npm run dev   # http://localhost:9002
```

## 演示账号（schema.sql 预置）

| 租户 | 账号 | 密码 | 预置商品 |
|------|------|------|----------|
| 店铺A | adminA | 123456 | 赛博机械键盘、霓虹鼠标垫 |
| 店铺B | adminB | 123456 | 蒸汽波卫衣 |

分别用两个账号登录，可看到商品和订单完全隔离；注册新租户自动创建独立空间。

## 面试可讲的技术点

1. **行级隔离 vs 独立 Schema**：共享表成本低、运维简单，通过拦截器强制隔离；大客户可平滑迁独立库（Handler 换数据源路由即可）。
2. **ThreadLocal 生命周期**：`afterCompletion` 清理，防止线程池复用导致租户串数据。
3. **防越权**：`removeById` 等单条操作也被拦截器附加 tenant_id 条件，即使传了别家商品 ID 也删不到。
4. **事务一致性**：下单扣库存 + 生成订单在同一事务，`selectById` 自动限定本租户商品。

## 目录

```
sql/schema.sql       建表 + 演示数据
backend/             Spring Boot（config / entity / mapper / service / controller）
frontend/            Vue3 + Vite（赛博朋克 UI）
```
