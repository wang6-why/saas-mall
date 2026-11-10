package com.saas.mall.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.saas.mall.common.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.Arrays;
import java.util.List;

/**
 * 租户行级隔离处理器：自动为 SQL 追加 tenant_id = ?
 * tenant 表为平台级表，不追加条件。
 */
public class SaasTenantLineHandler implements TenantLineHandler {

    /** 平台级（不隔离）表 */
    private static final List<String> IGNORE_TABLES = Arrays.asList("tenant");

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            // 未登录场景理论上不会走到这里（JWT 拦截器先行）；防御性返回 NULL
            return new NullValue();
        }
        return new LongValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }
}
