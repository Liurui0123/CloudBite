package com.cloudbite.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 订单侧 Redis 用法：
 * 1) 看板统计结果缓存 10 秒，避免每次刷新都全表聚合；
 * 2) 下单幂等键（同一用户 3 秒内重复提交直接拒绝）。
 */
@Component
public class OrderCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, Object> getStats() {
        try {
            String json = redis.opsForValue().get("order:stats");
            if (json == null) {
                return null;
            }
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void putStats(Map<String, Object> stats) {
        try {
            redis.opsForValue().set("order:stats", mapper.writeValueAsString(stats), Duration.ofSeconds(10));
        } catch (Exception ignored) {
        }
    }

    /** 返回 true 表示是重复提交 */
    public boolean isDuplicate(Long userId) {
        Boolean ok = redis.opsForValue().setIfAbsent("order:submit:" + userId, "1", Duration.ofSeconds(3));
        return Boolean.FALSE.equals(ok);
    }

    public void evictStats() {
        redis.delete("order:stats");
    }

    /** 下单失败时释放幂等键，允许用户立即重试 */
    public void releaseSubmit(Long userId) {
        redis.delete("order:submit:" + userId);
    }
}