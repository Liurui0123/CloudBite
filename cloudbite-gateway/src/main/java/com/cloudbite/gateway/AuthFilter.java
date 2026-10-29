package com.cloudbite.gateway;

import com.cloudbite.common.AuthHeaders;
import com.cloudbite.common.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关统一鉴权：
 * 1) 校验 JWT（SSE 的 EventSource 不能带 header，支持 ?token=）
 * 2) 注入 X-User-Id / X-User-Name / X-User-Role 给下游服务
 * 3) 管理端接口要求 ADMIN 角色（普通用户返回 403）
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login", "/api/user/register", "/api/menu/public",
            "/uploads",
            "/doc.html", "/v3/api-docs", "/webjars", "/swagger-resources", "/favicon.ico"
    );

    private static final List<String> ADMIN_PREFIX = List.of(
            "/api/order/admin", "/api/user/list", "/api/monitor"
    );

    private final JwtUtil jwtUtil;

    public AuthFilter(@Value("${cloudbite.jwt.secret}") String secret) {
        this.jwtUtil = new JwtUtil(secret, 86400);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (HttpMethod.OPTIONS.equals(method)
                || WHITE_LIST.stream().anyMatch(path::startsWith)
                || path.endsWith("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            String q = exchange.getRequest().getQueryParams().getFirst("token");
            if (q != null && !q.isBlank()) {
                auth = "Bearer " + q;
            }
        }
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(auth.substring(7));
        } catch (Exception e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, 401, "登录已过期，请重新登录");
        }

        String role = String.valueOf(claims.get("role"));
        if (needAdmin(path, method) && !"ADMIN".equals(role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, 403, "没有权限，请使用管理员账号登录");
        }

        ServerWebExchange mutated = exchange.mutate().request(r -> r.headers(h -> {
            h.set(AuthHeaders.USER_ID, claims.getSubject());
            h.set(AuthHeaders.USER_NAME, String.valueOf(claims.get("username")));
            h.set(AuthHeaders.ROLE, role);
        })).build();
        return chain.filter(mutated);
    }

    private boolean needAdmin(String path, HttpMethod method) {
        if (ADMIN_PREFIX.stream().anyMatch(path::startsWith)) {
            return true;
        }
        boolean menuWrite = path.startsWith("/api/menu/dishes")
                || path.startsWith("/api/menu/categories");
        return menuWrite && !HttpMethod.GET.equals(method);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        byte[] body = ("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
