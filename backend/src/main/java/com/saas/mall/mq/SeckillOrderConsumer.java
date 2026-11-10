package com.saas.mall.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.Order;
import com.saas.mall.entity.Product;
import com.saas.mall.entity.SeckillActivity;
import com.saas.mall.mapper.ProductMapper;
import com.saas.mall.mapper.SeckillActivityMapper;
import com.saas.mall.seckill.SeckillMessage;
import com.saas.mall.seckill.SeckillResult;
import com.saas.mall.seckill.SeckillService;
import com.saas.mall.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SeckillOrderConsumer {

    private final ProductMapper productMapper;
    private final SeckillActivityMapper activityMapper;
    private final OrderService orderService;
    private final StringRedisTemplate redis;

    public SeckillOrderConsumer(ProductMapper productMapper, SeckillActivityMapper activityMapper,
                                OrderService orderService, StringRedisTemplate redis) {
        this.productMapper = productMapper;
        this.activityMapper = activityMapper;
        this.orderService = orderService;
        this.redis = redis;
    }

    @RabbitListener(queues = RabbitConfig.SECKILL_QUEUE)
    public void onMessage(SeckillMessage msg) {
        TenantContext.set(msg.getTenantId()); // 消费线程也要有租户上下文
        String statusKey = "seckill:status:" + msg.getTenantId() + ":" + msg.getUserId() + ":" + msg.getActivityId();
        try {
            SeckillActivity activity = activityMapper.selectById(msg.getActivityId());
            // 数据库乐观扣库存：只有 stock >= 1 才更新成功，防止超卖兜底
            int updated = productMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>()
                            .setSql("stock = stock - 1")
                            .eq(Product::getId, activity.getProductId())
                            .ge(Product::getStock, 1));
            if (updated == 0) {
                rollbackRedis(msg, statusKey);
                redis.opsForValue().set(statusKey, String.valueOf(SeckillResult.FAILED));
                return;
            }

            Order order = new Order();
            order.setTenantId(msg.getTenantId());
            order.setUserId(msg.getUserId());
            order.setOrderNo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + ThreadLocalRandom.current().nextInt(1000, 9999));
            order.setAmount(activity.getSeckillPrice());
            order.setStatus(1); // 待支付
            orderService.save(order);

            redis.opsForValue().set(statusKey, String.valueOf(SeckillResult.SUCCESS));
        } catch (Exception e) {
            rollbackRedis(msg, statusKey);
            redis.opsForValue().set(statusKey, String.valueOf(SeckillResult.FAILED));
        } finally {
            TenantContext.clear();
        }
    }

    /** 消费失败把 Redis 配额还回去，允许用户重试 */
    private void rollbackRedis(SeckillMessage msg, String statusKey) {
        redis.opsForValue().increment("seckill:stock:" + msg.getTenantId() + ":" + msg.getActivityId());
        redis.opsForSet().remove("seckill:bought:" + msg.getTenantId() + ":" + msg.getActivityId(),
                String.valueOf(msg.getUserId()));
    }
}
