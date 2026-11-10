package com.saas.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.mall.common.R;
import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.Product;
import com.saas.mall.service.PlanService;
import com.saas.mall.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;
    private final PlanService planService;

    public ProductController(ProductService productService, PlanService planService) {
        this.productService = productService;
        this.planService = planService;
    }

    @GetMapping("/list")
    public R<List<Product>> list(@RequestParam(required = false) String keyword) {
        // 拦截器自动追加 tenant_id 条件，业务代码完全无感知
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(Product::getName, keyword);
        }
        return R.ok(productService.list(qw));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Product product) {
        // 套餐配额校验：企业 SaaS 的收费边界，超限直接拒绝
        String deny = planService.checkProductQuota(TenantContext.get());
        if (deny != null) {
            return R.fail(deny);
        }
        product.setTenantId(TenantContext.get());
        return R.ok(productService.save(product));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Product product) {
        product.setTenantId(TenantContext.get());
        return R.ok(productService.updateById(product));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        // 删除范围也被 tenant_id 条件约束，跨租户删除不可能发生
        return R.ok(productService.removeById(id));
    }
}
