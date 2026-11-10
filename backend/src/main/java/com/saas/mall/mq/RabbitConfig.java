package com.saas.mall.mq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String SECKILL_QUEUE = "seckill.order.queue";

    @Bean
    public Queue seckillQueue() {
        // 声明为持久化队列，broker 重启不丢消息
        return new Queue(SECKILL_QUEUE, true);
    }
}
