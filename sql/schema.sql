-- 多租户电商 SaaS：共享库共享表，所有业务表带 tenant_id
CREATE DATABASE IF NOT EXISTS saas_mall DEFAULT CHARSET utf8mb4;
USE saas_mall;

-- 租户表（不隔离）
CREATE TABLE tenant (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(64) NOT NULL COMMENT '租户名称',
    admin_user  VARCHAR(64) NOT NULL COMMENT '管理员账号',
    admin_pwd   VARCHAR(128) NOT NULL COMMENT '管理员密码',
    status      TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '租户表';

-- 用户表（隔离）
CREATE TABLE sys_user (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id  BIGINT NOT NULL,
    username   VARCHAR(64) NOT NULL,
    password   VARCHAR(128) NOT NULL,
    role       VARCHAR(32) DEFAULT 'operator' COMMENT 'admin/operator',
    KEY idx_tenant (tenant_id)
) COMMENT '业务用户';

-- 商品（隔离）
CREATE TABLE product (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id  BIGINT NOT NULL,
    name       VARCHAR(128) NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    stock      INT NOT NULL DEFAULT 0,
    status     TINYINT DEFAULT 1 COMMENT '1上架 0下架',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tenant (tenant_id)
) COMMENT '商品';

-- 订单（隔离）
CREATE TABLE orders (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id  BIGINT NOT NULL,
    order_no   VARCHAR(64) NOT NULL,
    user_id    BIGINT NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    status     TINYINT DEFAULT 0 COMMENT '0排队中 1待支付 2已支付 3已取消',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tenant (tenant_id)
) COMMENT '订单';

CREATE TABLE order_item (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id  BIGINT NOT NULL,
    order_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    qty        INT NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    KEY idx_tenant (tenant_id)
) COMMENT '订单明细';

-- 秒杀活动（隔离）：秒杀价 + Redis 配额
CREATE TABLE seckill_activity (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    seckill_price DECIMAL(10,2) NOT NULL,
    total_stock   INT NOT NULL COMMENT 'Redis 预扣总数',
    start_time    DATETIME NOT NULL,
    end_time      DATETIME NOT NULL,
    status        TINYINT DEFAULT 1 COMMENT '1进行中 0结束',
    KEY idx_tenant (tenant_id)
) COMMENT '秒杀活动';

-- 演示秒杀：A店键盘 199 元限量 10 台，B店卫衣 99 元限量 5 件
INSERT INTO seckill_activity (tenant_id, product_id, seckill_price, total_stock, start_time, end_time) VALUES
(1, 1, 199.00, 10, '2026-01-01 00:00:00', '2030-01-01 00:00:00'),
(2, 3, 99.00, 5, '2026-01-01 00:00:00', '2030-01-01 00:00:00');

-- 演示数据：两个租户
INSERT INTO tenant (name, admin_user, admin_pwd) VALUES
('店铺A', 'adminA', '123456'),
('店铺B', 'adminB', '123456');

INSERT INTO sys_user (tenant_id, username, password, role) VALUES
(1, 'adminA', '123456', 'admin'),
(2, 'adminB', '123456', 'admin');

INSERT INTO product (tenant_id, name, price, stock) VALUES
(1, 'A店-赛博机械键盘', 599.00, 100),
(1, 'A店-霓虹鼠标垫',   99.00,  500),
(2, 'B店-蒸汽波卫衣',   299.00, 200);
