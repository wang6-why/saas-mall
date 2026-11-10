package com.saas.mall.seckill;

import com.saas.mall.mq.RabbitConfig;
import lombok.Data;

import java.io.Serializable;

/** 秒杀下单消息体 */
@Data
public class SeckillMessage implements Serializable {
    private Long tenantId;
    private Long userId;
    private Long activityId;

    public SeckillMessage(Long tenantId, Long userId, Long activityId) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.activityId = activityId;
    }
}
