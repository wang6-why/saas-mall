package com.saas.mall.controller;

import com.saas.mall.common.R;
import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.SeckillActivity;
import com.saas.mall.mapper.SeckillActivityMapper;
import com.saas.mall.seckill.SeckillResult;
import com.saas.mall.seckill.SeckillService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;
    private final SeckillActivityMapper activityMapper;

    public SeckillController(SeckillService seckillService, SeckillActivityMapper activityMapper) {
        this.seckillService = seckillService;
        this.activityMapper = activityMapper;
    }

    @GetMapping("/list")
    public R<List<SeckillActivity>> list() {
        return R.ok(activityMapper.selectList(null));
    }

    /** 发起秒杀：立即返回排队状态，不阻塞 */
    @PostMapping("/{activityId}/buy")
    public R<Integer> buy(@PathVariable Long activityId) {
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
