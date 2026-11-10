package com.saas.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.mall.common.R;
import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.Order;
import com.saas.mall.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 下单，body: {"productId":1,"qty":2} */
    @PostMapping
    public R<Order> create(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        Integer qty = Integer.valueOf(String.valueOf(body.get("qty")));
        return R.ok(orderService.create(TenantContext.get(), Map.of(productId, qty)));
    }

    @GetMapping("/list")
    public R<List<Order>> list() {
        return R.ok(orderService.list(new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreatedAt)));
    }

    @GetMapping("/{id}/items")
    public R<List<Map<String, Object>>> items(@PathVariable Long id) {
        List<Map<String, Object>> items = orderService.items(id).stream()
                .map(item -> Map.<String, Object>of(
                        "productId", item.getProductId(),
                        "qty", item.getQty(),
                        "price", item.getPrice()))
                .collect(Collectors.toList());
        return R.ok(items);
    }

    @PutMapping("/{id}/pay")
    public R<Boolean> pay(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return R.fail("订单不存在");
        }
        order.setStatus(2);
        return R.ok(orderService.updateById(order));
    }
}
