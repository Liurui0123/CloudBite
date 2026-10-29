package com.cloudbite.order.dto;

import java.util.List;

public record CreateOrderRequest(List<Item> items, String address, String remark) {
    public record Item(Long dishId, Integer quantity) {
    }
}