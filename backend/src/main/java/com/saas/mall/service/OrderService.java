package com.saas.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.Order;
import com.saas.mall.entity.OrderItem;
import com.saas.mall.entity.Product;
import com.saas.mall.mapper.OrderItemMapper;
import com.saas.mall.mapper.OrderMapper;
import com.saas.mall.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderService(ProductMapper productMapper, OrderItemMapper orderItemMapper) {
        this.productMapper = productMapper;
        this.orderItemMapper = orderItemMapper;
    }

    /** 下单：productId -> qty */
    @Transactional
    public Order create(Long userId, Map<Long, Integer> items) {
        Order order = new Order();
        order.setTenantId(TenantContext.get());
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setStatus(1);

        BigDecimal amount = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Product product = productMapper.selectById(entry.getKey()); // 拦截器自动限定本租户
            if (product == null) {
                throw new RuntimeException("商品不存在: " + entry.getKey());
            }
            if (product.getStock() < entry.getValue()) {
                throw new RuntimeException("库存不足: " + product.getName());
            }
            product.setStock(product.getStock() - entry.getValue());
            productMapper.updateById(product);

            OrderItem item = new OrderItem();
            item.setTenantId(TenantContext.get());
            item.setProductId(product.getId());
            item.setQty(entry.getValue());
            item.setPrice(product.getPrice());
            amount = amount.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            orderItemMapper.insert(item);
        }
        order.setAmount(amount);
        save(order);
        return order;
    }

    public List<OrderItem> items(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
