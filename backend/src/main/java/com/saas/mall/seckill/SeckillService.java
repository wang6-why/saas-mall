package com.saas.mall.seckill;

import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.SeckillActivity;
import com.saas.mall.mapper.SeckillActivityMapper;
import com.saas.mall.mq.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final StringRedisTemplate redis;
    private final RabbitTemplate rabbit;

    private static final DefaultRedisScript<Long> LUA = new DefaultRedisScript<>(
            // KEYS[1]=stock, KEYS[2]=bought set, ARGV[1]=userId
            "local stock = tonumber(redis.call('get', KEYS[1]) or '0') " +
            "if stock <= 0 then return 0 end " +
            "if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then return -1 end " +
            "redis.call('decr', KEYS[1]) " +
            "redis.call('sadd', KEYS[2], ARGV[1]) " +
            "return 1", Long.class);

    public SeckillService(SeckillActivityMapper activityMapper, StringRedisTemplate redis, RabbitTemplate rabbit) {
        this.activityMapper = activityMapper;
        this.redis = redis;
        this.rabbit = rabbit;
    }

    private String stockKey(Long tenantId, Long activityId) {
        return "seckill:stock:" + tenantId + ":" + activityId;
    }

    private String boughtKey(Long tenantId, Long activityId) {
        return "seckill:bought:" + tenantId + ":" + activityId;
    }

    private String statusKey(Long tenantId, Long userId, Long activityId) {
        return "seckill:status:" + tenantId + ":" + userId + ":" + activityId;
    }

    /** 活动首次被请求时，把配额预热进 Redis（幂等） */
    public void preload(SeckillActivity activity) {
        String key = stockKey(activity.getTenantId(), activity.getId());
        if (Boolean.FALSE.equals(redis.hasKey(key))) {
            redis.opsForValue().set(key, String.valueOf(activity.getTotalStock()));
        }
    }

    /**
     * 秒杀入口：Lua 原子预扣 -> 发 MQ 异步落库，返回排队状态
     */
    public int doSeckill(Long tenantId, Long userId, Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1
                || LocalDateTime.now().isBefore(activity.getStartTime())
                || LocalDateTime.now().isAfter(activity.getEndTime())) {
            return SeckillResult.SOLD_OUT;
        }
        preload(activity);

        Long ret = redis.execute(LUA, Arrays.asList(stockKey(tenantId, activityId), boughtKey(tenantId, activityId)),
                String.valueOf(userId));
        if (ret == null || ret == 0) {
            return SeckillResult.SOLD_OUT;
        }
        if (ret == -1) {
            return SeckillResult.DUPLICATED;
        }

        // 排队状态写入 Redis，前端轮询该 key
        redis.opsForValue().set(statusKey(tenantId, userId, activityId), String.valueOf(SeckillResult.QUEUING));
        // MQ 异步下单，解耦削峰
        rabbit.convertAndSend(RabbitConfig.SECKILL_QUEUE, new SeckillMessage(tenantId, userId, activityId));
        return SeckillResult.QUEUING;
    }

    /** 查询某用户在某活动的下单状态 */
    public int status(Long tenantId, Long userId, Long activityId) {
        String v = redis.opsForValue().get(statusKey(tenantId, userId, activityId));
        return v == null ? SeckillResult.FAILED : Integer.parseInt(v);
    }

    /** 查询 Redis 中的剩余配额 */
    public int stock(Long tenantId, Long activityId) {
        String v = redis.opsForValue().get(stockKey(tenantId, activityId));
        return v == null ? -1 : Integer.parseInt(v);
    }
}
