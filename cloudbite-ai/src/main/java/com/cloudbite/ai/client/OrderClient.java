package com.cloudbite.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "cloudbite-order", path = "/api/order")
public interface OrderClient {
    @GetMapping("/{orderNo}")
    Map<String, Object> detail(@PathVariable("orderNo") String orderNo);
}