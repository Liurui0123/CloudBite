package com.cloudbite.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.cloudbite.common.ApiResult;
import com.cloudbite.common.AuthHeaders;
import com.cloudbite.order.dto.CreateOrderRequest;
import com.cloudbite.order.entity.Order;
import com.cloudbite.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @SentinelResource(value = "createOrder", blockHandler = "createBlocked")
    public ApiResult<Order> create(@RequestHeader(AuthHeaders.USER_ID) Long userId,
                                   @RequestBody CreateOrderRequest req) {
        return ApiResult.ok(service.create(userId, req));
    }

    /** 触发限流时的兜底（方法签名需与原方法一致 + 末尾多一个 BlockException） */
    public ApiResult<Order> createBlocked(Long userId, CreateOrderRequest req, BlockException ex) {
        return ApiResult.fail(429, "当前下单人数过多，请稍后再试");
    }

    @PostMapping("/{orderNo}/pay")
    public ApiResult<Order> pay(@RequestHeader(AuthHeaders.USER_ID) Long userId,
                                @PathVariable String orderNo) {
        return ApiResult.ok(service.pay(orderNo, userId));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResult<Order> cancel(@RequestHeader(AuthHeaders.USER_ID) Long userId,
                                   @PathVariable String orderNo) {
        return ApiResult.ok(service.cancel(orderNo, userId));
    }

    @GetMapping("/my")
    public ApiResult<List<Order>> my(@RequestHeader(AuthHeaders.USER_ID) Long userId) {
        return ApiResult.ok(service.myOrders(userId));
    }

    @GetMapping("/{orderNo}")
    public ApiResult<Order> detail(@PathVariable String orderNo) {
        return ApiResult.ok(service.get(orderNo));
    }

    @GetMapping("/admin/list")
    public ApiResult<List<Order>> all() {
        return ApiResult.ok(service.all());
    }

    @PostMapping("/admin/{orderNo}/status/{target}")
    public ApiResult<Order> updateStatus(@PathVariable String orderNo, @PathVariable String target) {
        return ApiResult.ok(service.updateStatus(orderNo, target));
    }

    @GetMapping("/admin/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(service.stats());
    }
}