package com.cloudbite.order.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cloudbite.mq")
public class MqProperties {
    private String namesrv = "127.0.0.1:9876";
    private String topic = "cloudbite-order";
    private String producerGroup = "cloudbite-producer";
    private String consumerGroup = "cloudbite-close-consumer";
    /** 1=1s 2=5s 3=10s 4=30s 5=1m ... 16=30m */
    private int closeDelayLevel = 16;
}