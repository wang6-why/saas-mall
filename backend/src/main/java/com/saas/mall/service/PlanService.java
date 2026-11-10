package com.saas.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.mall.entity.Plan;
import com.saas.mall.entity.Product;
import com.saas.mall.entity.Tenant;
import com.saas.mall.mapper.PlanMapper;
import com.saas.mall.mapper.ProductMapper;
import com.saas.mall.mapper.TenantMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 套餐与配额：企业 SaaS 的收费抓手。
 * 所有能力开关（商品上限、秒杀是否可用、套餐是否过期）都从这里统一判定。
 */
@Service
public class PlanService {

    private final TenantMapper tenantMapper;
    private final PlanMapper planMapper;
    private final ProductMapper productMapper;

    public PlanService(TenantMapper tenantMapper, PlanMapper planMapper, ProductMapper productMapper) {
        this.tenantMapper = tenantMapper;
        this.planMapper = planMapper;
        this.productMapper = productMapper;
    }

    public List<Plan> listPlans() {
        return planMapper.selectList(null);
    }

    public Plan currentPlan(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getPlanId() == null) {
            return null;
        }
        return planMapper.selectById(tenant.getPlanId());
    }

    /** 套餐是否有效（未过期）；未设置到期时间视为长期有效 */
    public boolean planValid(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) return false;
        return tenant.getExpireTime() == null || tenant.getExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 商品配额校验：返回 null 表示通过，否则返回错误提示
     * 企业场景要点：配额校验必须放在写入前，且 count 也走租户拦截器
     */
    public String checkProductQuota(Long tenantId) {
        if (!planValid(tenantId)) {
            return "套餐已到期，请续费后继续使用";
        }
        Plan plan = currentPlan(tenantId);
        if (plan == null || plan.getMaxProducts() == null || plan.getMaxProducts() < 0) {
            return null; // 不限
        }
        Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>());
        if (count >= plan.getMaxProducts()) {
            return "当前套餐商品上限 " + plan.getMaxProducts() + " 个，请升级套餐";
        }
        return null;
    }

    /** 秒杀功能是否对当前租户开放 */
    public boolean seckillEnabled(Long tenantId) {
        Plan plan = currentPlan(tenantId);
        return planValid(tenantId) && plan != null && Integer.valueOf(1).equals(plan.getSeckillEnabled());
    }

    /** 套餐使用概览，前端展示用 */
    public Map<String, Object> usage(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        Plan plan = currentPlan(tenantId);
        Long used = productMapper.selectCount(new LambdaQueryWrapper<Product>());
        Map<String, Object> m = new HashMap<>();
        m.put("planName", plan == null ? "-" : plan.getName());
        m.put("planPrice", plan == null ? 0 : plan.getPrice());
        m.put("maxProducts", plan == null ? -1 : plan.getMaxProducts());
        m.put("usedProducts", used);
        m.put("seckillEnabled", seckillEnabled(tenantId));
        m.put("expireTime", tenant == null ? null : tenant.getExpireTime());
        m.put("valid", planValid(tenantId));
        return m;
    }

    /** 续费/升级（真实系统对接支付，这里是签约后直接生效） */
    public void changePlan(Long tenantId, Long planId, Integer months) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("套餐不存在");
        }
        Tenant tenant = tenantMapper.selectById(tenantId);
        LocalDateTime base = tenant.getExpireTime() != null && tenant.getExpireTime().isAfter(LocalDateTime.now())
                ? tenant.getExpireTime() : LocalDateTime.now();
        tenant.setPlanId(planId);
        tenant.setExpireTime(base.plusMonths(months == null || months <= 0 ? 1 : months));
        tenantMapper.updateById(tenant);
    }
}
