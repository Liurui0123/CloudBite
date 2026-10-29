package com.cloudbite.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 20, message = "用户名长度 3-20") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 30, message = "密码至少 6 位") String password,
        String nickname,
        String phone) {
}