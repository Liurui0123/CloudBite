package com.cloudbite.menu.cache;

import com.cloudbite.menu.entity.Category;
import com.cloudbite.menu.entity.Dish;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 菜单缓存：采用「版本号」策略做批量失效，避免 SCAN 删除。
 * key 里带版本号，写操作时 INCR 版本号，旧 key 自然过期。
 */
@Component
public class MenuCache {
    private static final String VERSION_KEY = "menu:version";
    public static final String HOT_KEY = "menu:hot";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public MenuCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long version() {
        String v = redis.opsForValue().get(VERSION_KEY);
        return v == null ? 0 : Long.parseLong(v);
    }

    public void bumpVersion() {
        redis.opsForValue().increment(VERSION_KEY);
    }

    public List<Category> getCategories() {
        return readList("menu:cat:" + version(), Category.class);
    }

    public void putCategories(List<Category> list) {
        write("menu:cat:" + version(), list, Duration.ofMinutes(10));
    }

    public List<Dish> getDishes(Long categoryId, String keyword) {
        return readList(dishKey(categoryId, keyword), Dish.class);
    }

    public void putDishes(Long categoryId, String keyword, List<Dish> list) {
        write(dishKey(categoryId, keyword), list, Duration.ofMinutes(2));
    }

    private String dishKey(Long categoryId, String keyword) {
        return "menu:dishes:" + version() + ":" + (categoryId == null ? 0 : categoryId) + ":" + (keyword == null ? "" : keyword);
    }

    private <T> List<T> readList(String key, Class<T> elem) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, elem);
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception ignored) {
        }
    }
}