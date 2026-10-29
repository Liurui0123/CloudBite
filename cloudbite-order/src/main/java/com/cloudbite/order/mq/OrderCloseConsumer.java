package com.cloudbite.order.mq;

import com.cloudbite.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 延迟关单消费者：到期检查订单状态，未支付则取消并回滚库存。
 * 处理失败返回 RECONSUME_LATER 由 MQ 重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseConsumer implements SmartLifecycle {

    private final OrderService orderService;
    private final MqProperties props;
    private DefaultMQPushConsumer consumer;
    private volatile boolean running = false;

    @Override
    public void start() {
        try {
            consumer = new DefaultMQPushConsumer(props.getConsumerGroup());
            consumer.setNamesrvAddr(props.getNamesrv());
            consumer.subscribe(props.getTopic(), OrderMessageProducer.TAG_DELAY_CLOSE);
            consumer.setConsumeThreadMin(2);
            consumer.setConsumeThreadMax(4);
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                for (MessageExt msg : msgs) {
                    String orderNo = new String(msg.getBody(), StandardCharsets.UTF_8);
                    try {
                        boolean closed = orderService.closeIfUnpaid(orderNo);
                        log.info("[MQ] 处理延迟关单 orderNo={} reconsumeTimes={} 结果={}",
                                orderNo, msg.getReconsumeTimes(), closed ? "已关单并回滚库存" : "订单已支付/已处理，跳过");
                    } catch (Exception e) {
                        log.error("[MQ] 关单失败，稍后重试 orderNo=" + orderNo, e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            running = true;
            log.info("RocketMQ Consumer 已启动：group={}, topic={}", props.getConsumerGroup(), props.getTopic());
        } catch (Exception e) {
            log.error("RocketMQ Consumer 启动失败: {}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (consumer != null) {
            consumer.shutdown();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}