package com.cloudbite.user.config;

import com.cloudbite.common.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Bean
    public JwtUtil jwtUtil(@Value("${cloudbite.jwt.secret}") String secret) {
        return new JwtUtil(secret, 86400);
    }
}