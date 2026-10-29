package com.cloudbite.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "cloudbite-menu", path = "/api/menu")
public interface MenuClient {

    @GetMapping("/public/dishes/{id}")
    Map<String, Object> dish(@PathVariable("id") Long id);

    @PostMapping("/internal/deduct")
    Map<String, Object> deduct(@RequestBody List<Map<String, Object>> items);

    @PostMapping("/internal/release")
    Map<String, Object> release(@RequestBody List<Map<String, Object>> items);
}