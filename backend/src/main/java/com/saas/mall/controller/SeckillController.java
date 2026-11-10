package com.saas.mall.controller;

import com.saas.mall.common.R;
import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.SeckillActivity;
import com.saas.mall.mapper.SeckillActivityMapper;
import com.saas.mall.seckill.SeckillResult;
import com.saas.mall.seckill.SeckillService;
import com.saas.mall.service.PlanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;
    private final SeckillActivityMapper activityMapper;
    private final PlanService planService;

    public SeckillController(SeckillService seckillService, SeckillActivityMapper activityMapper,
                            PlanService planService) {
        this.seckillService = seckillService;
        this.activityMapper = activityMapper;
        this.planService = planService;
    }

    @GetMapping("/list")
    public R<List<SeckillActivity>> list() {
        // 秒杀是付费能力：免费版返回空，前端据此引导升级
        if (!planService.seckillEnabled(TenantContext.get())) {
            return R.ok(java.util.Collections.emptyList());
        }
        return R.ok(activityMapper.selectList(null));
    }

    /** 发起秒杀：立即返回排队状态，不阻塞 */
    @PostMapping("/{activityId}/buy")
    public R<Integer> buy(@PathVariable Long activityId) {
        if (!planService.seckillEnabled(TenantContext.get())) {
            return R.fail("当前套餐未开放秒杀功能，请升级到专业版");
        }
        return R.ok(seckillService.doSeckill(TenantContext.get(), TenantContext.get(), activityId));
    }

    /** 轮询下单结果 */
    @GetMapping("/{activityId}/status")
    public R<Integer> status(@PathVariable Long activityId) {
        return R.ok(seckillService.status(TenantContext.get(), TenantContext.get(), activityId));
    }

    /** 剩余库存（读 Redis） */
    @GetMapping("/{activityId}/stock")
    public R<Integer> stock(@PathVariable Long activityId) {
        return R.ok(seckillService.stock(TenantContext.get(), TenantContext.get(), activityId));
    }
}
