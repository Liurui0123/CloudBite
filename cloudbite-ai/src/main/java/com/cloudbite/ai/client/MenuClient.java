package com.cloudbite.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "cloudbite-menu", path = "/api/menu")
public interface MenuClient {
    @GetMapping("/public/dishes")
    Map<String, Object> dishes();
}