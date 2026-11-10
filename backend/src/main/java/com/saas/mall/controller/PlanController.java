package com.saas.mall.controller;

import com.saas.mall.common.R;
import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.Plan;
import com.saas.mall.service.PlanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/list")
    public R<List<Plan>> list() {
        return R.ok(planService.listPlans());
    }

    /** 当前租户套餐使用情况 */
    @GetMapping("/usage")
    public R<Map<String, Object>> usage() {
        return R.ok(planService.usage(TenantContext.get()));
    }

    /** 升级/续费：body {planId, months} */
    @PostMapping("/subscribe")
    public R<Boolean> subscribe(@RequestBody Map<String, Object> body) {
        Long planId = Long.valueOf(String.valueOf(body.get("planId")));
        Integer months = body.get("months") == null ? 1 : Integer.valueOf(String.valueOf(body.get("months")));
        planService.changePlan(TenantContext.get(), planId, months);
        return R.ok(true);
    }
}
