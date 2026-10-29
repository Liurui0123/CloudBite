package com.cloudbite.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MQProducerConfig {

    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer orderProducer(MqProperties props) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(props.getProducerGroup());
        producer.setNamesrvAddr(props.getNamesrv());
        producer.setSendMsgTimeout(5000);
        producer.setRetryTimesWhenSendFailed(2);
        producer.start();
        log.info("RocketMQ Producer 已启动：group={}, namesrv={}", props.getProducerGroup(), props.getNamesrv());
        return producer;
    }
}