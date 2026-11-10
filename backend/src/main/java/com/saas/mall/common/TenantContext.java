package com.saas.mall.common;

/**
 * 租户上下文：登录后由 JwtInterceptor 解析 token 写入 tenantId，请求结束清理。
 * MyBatis-Plus TenantLineHandler 从这里读取 tenant_id。
 */
public class TenantContext {
    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();

    public static void set(Long tenantId) { TENANT.set(tenantId); }

    public static Long get() { return TENANT.get(); }

    public static void clear() { TENANT.remove(); }
}
