package com.saas.mall.seckill;

/**
 * 秒杀状态（存 Redis，前端轮询）
 */
public class SeckillResult {
    public static final int QUEUING = 0;    // 排队中
    public static final int SUCCESS = 1;    // 下单成功
    public static final int SOLD_OUT = -2;  // 售罄
    public static final int DUPLICATED = -1;// 重复下单
    public static final int FAILED = -3;    // MQ 消费失败
}
