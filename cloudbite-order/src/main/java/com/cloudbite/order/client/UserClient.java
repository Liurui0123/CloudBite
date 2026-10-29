package com.cloudbite.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "cloudbite-user", path = "/api/user")
public interface UserClient {

    @GetMapping("/list")
    Map<String, Object> list();
}