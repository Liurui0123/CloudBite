package com.cloudbite.menu.dto;

import java.math.BigDecimal;

public record DishRequest(
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        Integer stock,
        String status,
        String emoji,
        String tag,
        String imageUrl) {
}