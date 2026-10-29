package com.cloudbite.order.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    public static final String TAG_CREATED = "ORDER_CREATED";
    public static final String TAG_DELAY_CLOSE = "ORDER_DELAY_CLOSE";

    private final DefaultMQProducer orderProducer;
    private final MqProperties props;

    public void sendOrderCreated(String orderNo) {
        send(orderNo, TAG_CREATED, 0);
    }

    public void sendDelayClose(String orderNo) {
        send(orderNo, TAG_DELAY_CLOSE, props.getCloseDelayLevel());
    }

    private void send(String orderNo, String tag, int delayLevel) {
        Message msg = new Message(props.getTopic(), tag, orderNo.getBytes(StandardCharsets.UTF_8));
        if (delayLevel > 0) {
            msg.setDelayTimeLevel(delayLevel);
        }
        try {
            SendResult result = orderProducer.send(msg);
            log.info("[MQ] 消息已发送 orderNo={} tag={} delayLevel={} msgId={}",
                    orderNo, tag, delayLevel, result.getMsgId());
        } catch (Exception e) {
            log.error("[MQ] 消息发送失败 orderNo=" + orderNo + " tag=" + tag, e);
        }
    }
}