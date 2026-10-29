package com.cloudbite.user.dto;

public record LoginResponse(String token, Long userId, String username, String nickname, String role) {
}