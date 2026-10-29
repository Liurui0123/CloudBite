package com.cloudbite.menu.service;

import com.cloudbite.common.BizException;
import com.cloudbite.menu.cache.MenuCache;
import com.cloudbite.menu.dto.DishRequest;
import com.cloudbite.menu.dto.StockItem;
import com.cloudbite.menu.entity.Category;
import com.cloudbite.menu.entity.Dish;
import com.cloudbite.menu.lock.DistributedLock;
import com.cloudbite.menu.repo.CategoryRepository;
import com.cloudbite.menu.repo.DishRepository;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class MenuService {

    private final DishRepository dishRepo;
    private final CategoryRepository categoryRepo;
    private final MenuCache cache;
    private final DistributedLock lock;
    private final StringRedisTemplate redis;
    private final TransactionTemplate transactionTemplate;

    public MenuService(DishRepository dishRepo, CategoryRepository categoryRepo,
                       MenuCache cache, DistributedLock lock, StringRedisTemplate redis,
                       TransactionTemplate transactionTemplate) {
        this.dishRepo = dishRepo;
        this.categoryRepo = categoryRepo;
        this.cache = cache;
        this.lock = lock;
        this.redis = redis;
        this.transactionTemplate = transactionTemplate;
    }

    // ---------- 查询（带缓存） ----------

    public List<Category> categories() {
        List<Category> cached = cache.getCategories();
        if (cached != null) {
            return cached;
        }
        List<Category> list = categoryRepo.findAllByOrderBySortAsc();
        cache.putCategories(list);
        return list;
    }

    public List<Dish> dishes(Long categoryId, String keyword) {
        List<Dish> cached = cache.getDishes(categoryId, keyword);
        if (cached != null) {
            return cached;
        }
        List<Dish> list;
        if (keyword != null && !keyword.isBlank()) {
            list = dishRepo.findByNameContainingAndStatus(keyword, "ON_SALE");
        } else if (categoryId != null) {
            list = dishRepo.findByCategoryIdAndStatusOrderBySalesDesc(categoryId, "ON_SALE");
        } else {
            list = dishRepo.findByStatusOrderBySalesDesc("ON_SALE");
        }
        cache.putDishes(categoryId, keyword, list);
        return list;
    }

    public Dish dish(Long id) {
        return dishRepo.findById(id).orElseThrow(() -> new BizException(404, "菜品不存在"));
    }

    public List<Dish> allDishes() {
        return dishRepo.findAll();
    }

    @Transactional
    public Dish create(DishRequest req) {
        Dish d = new Dish();
        apply(d, req);
        Dish saved = dishRepo.save(d);
        cache.bumpVersion();
        return saved;
    }

    @Transactional
    public Dish update(Long id, DishRequest req) {
        Dish d = dish(id);
        apply(d, req);
        Dish saved = dishRepo.save(d);
        cache.bumpVersion();
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        dishRepo.deleteById(id);
        cache.bumpVersion();
    }

    @Transactional
    public void toggle(Long id) {
        Dish d = dish(id);
        d.setStatus("ON_SALE".equals(d.getStatus()) ? "OFF_SALE" : "ON_SALE");
        dishRepo.save(d);
        cache.bumpVersion();
    }

    // ---------- 分类管理 ----------

    @Transactional
    public Category createCategory(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BizException("分类名称不能为空");
        }
        category.setId(null);
        if (category.getSort() == null) {
            category.setSort(99);
        }
        Category saved = categoryRepo.save(category);
        cache.bumpVersion();
        return saved;
    }

    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category exist = categoryRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "分类不存在"));
        if (category.getName() != null) {
            exist.setName(category.getName());
        }
        if (category.getSort() != null) {
            exist.setSort(category.getSort());
        }
        if (category.getIcon() != null) {
            exist.setIcon(category.getIcon());
        }
        Category saved = categoryRepo.save(exist);
        cache.bumpVersion();
        return saved;
    }

    @Transactional
    public void deleteCategory(Long id) {
        long used = dishRepo.countByCategoryId(id);
        if (used > 0) {
            throw new BizException("该分类下还有 " + used + " 道菜，不能删除");
        }
        categoryRepo.deleteById(id);
        cache.bumpVersion();
    }

    /**
     * 扣库存：先抢 Redisson 锁（事务外），再开事务做「校验 + 扣减」。
     * 顺序不能反：锁在事务内会读到旧快照，导致超卖。
     */
    public void deduct(List<StockItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<StockItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(StockItem::dishId));

        List<RLock> locks = new ArrayList<>();
        try {
            for (StockItem item : sorted) {
                RLock rl = lock.lock("lock:stock:" + item.dishId());
                if (!lock.tryLock(rl, 3)) {
                    throw new BizException("系统繁忙，请稍后重试");
                }
                locks.add(rl);
            }
            transactionTemplate.executeWithoutResult(status -> {
                for (StockItem item : sorted) {
                    Dish dish = dishRepo.findById(item.dishId())
                            .orElseThrow(() -> new BizException(404, "菜品不存在: " + item.dishId()));
                    if (dish.getStock() < item.quantity()) {
                        throw new BizException("「" + dish.getName() + "」库存不足，仅剩 " + dish.getStock() + " 份");
                    }
                }
                for (StockItem item : sorted) {
                    Dish dish = dishRepo.findById(item.dishId()).orElseThrow();
                    dish.setStock(dish.getStock() - item.quantity());
                    dish.setSales(dish.getSales() + item.quantity());
                    dishRepo.save(dish);
                }
            });
            for (StockItem item : sorted) {
                redis.opsForZSet().incrementScore(MenuCache.HOT_KEY, String.valueOf(item.dishId()), item.quantity());
            }
            cache.bumpVersion();
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                lock.unlock(locks.get(i));
            }
        }
    }

    /**
     * 回滚库存（订单超时未支付时由 order 服务通过 Feign 调用）
     */
    @Transactional
    public void release(List<StockItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (StockItem item : items) {
            Dish dish = dishRepo.findById(item.dishId()).orElse(null);
            if (dish == null) {
                continue;
            }
            dish.setStock(dish.getStock() + item.quantity());
            dish.setSales(Math.max(0, (dish.getSales() == null ? 0 : dish.getSales()) - item.quantity()));
            dishRepo.save(dish);
            redis.opsForZSet().incrementScore(MenuCache.HOT_KEY, String.valueOf(item.dishId()), -item.quantity());
        }
        cache.bumpVersion();
    }

    /** 实时热销榜（Redis ZSET） */
    public List<Map<String, Object>> hot(int top) {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(MenuCache.HOT_KEY, 0, top - 1);
        List<Map<String, Object>> res = new ArrayList<>();
        if (tuples == null) {
            return res;
        }
        for (var t : tuples) {
            Long dishId = Long.valueOf(t.getValue());
            Dish d = dishRepo.findById(dishId).orElse(null);
            if (d == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dishId", dishId);
            m.put("name", d.getName());
            m.put("emoji", d.getEmoji());
            m.put("price", d.getPrice());
            m.put("sold", t.getScore() == null ? 0 : t.getScore().intValue());
            res.add(m);
        }
        return res;
    }

    private void apply(Dish d, DishRequest req) {
        d.setName(req.name());
        d.setDescription(req.description());
        d.setPrice(req.price());
        d.setCategoryId(req.categoryId());
        d.setStock(req.stock() == null ? 100 : req.stock());
        d.setStatus(req.status() == null ? "ON_SALE" : req.status());
        d.setEmoji(req.emoji());
        d.setTag(req.tag());
        d.setImageUrl(req.imageUrl());
    }

    // ---------- 种子数据 ----------

    @Transactional
    public void seed() {
        if (categoryRepo.count() > 0) {
            return;
        }
        Category hot = newCategory("热菜", 1, "🔥");
        Category cold = newCategory("凉菜", 2, "🥗");
        Category staple = newCategory("主食", 3, "🍚");
        Category drink = newCategory("饮品", 4, "🥤");
        Category combo = newCategory("套餐", 5, "🍱");

        seedDish("宫保鸡丁", "花生鸡丁，微辣下饭", 32, hot, "🍗", "招牌", 88);
        seedDish("红烧肉", "肥而不腻，入口即化", 48, hot, "🥩", "热卖", 65);
        seedDish("青椒肉丝", "家常小炒，清爽解腻", 28, hot, "🌶️", null, 54);
        seedDish("番茄炒蛋", "酸甜可口，老少皆宜", 22, hot, "🍅", null, 73);
        seedDish("酸辣土豆丝", "酸辣开胃，脆爽", 18, hot, "🥔", null, 41);
        seedDish("凉拌黄瓜", "蒜香爽脆", 12, cold, "🥒", null, 30);
        seedDish("口水鸡", "麻辣鲜香", 36, cold, "🐔", "新品", 22);
        seedDish("米饭", "东北大米", 3, staple, "🍚", null, 200);
        seedDish("蛋炒饭", "粒粒分明", 16, staple, "🍳", null, 60);
        seedDish("手工水饺", "猪肉白菜馅", 24, staple, "🥟", null, 45);
        seedDish("冰可乐", "冰爽解辣", 6, drink, "🥤", null, 120);
        seedDish("酸梅汤", "古法熬制", 10, drink, "🧋", "推荐", 80);
        seedDish("双人套餐A", "宫保鸡丁 + 米饭 + 可乐", 45, combo, "🍱", "超值", 40);
        seedDish("单人套餐B", "番茄炒蛋 + 米饭 + 酸梅汤", 32, combo, "🍱", null, 35);
        cache.bumpVersion();
    }

    private Category newCategory(String name, int sort, String icon) {
        Category c = new Category();
        c.setName(name);
        c.setSort(sort);
        c.setIcon(icon);
        return categoryRepo.save(c);
    }

    private void seedDish(String name, String desc, int price, Category c, String emoji, String tag, int sales) {
        Dish d = new Dish();
        d.setName(name);
        d.setDescription(desc);
        d.setPrice(BigDecimal.valueOf(price));
        d.setCategoryId(c.getId());
        d.setStock(100);
        d.setEmoji(emoji);
        d.setTag(tag);
        d.setSales(sales);
        d.setStatus("ON_SALE");
        dishRepo.save(d);
    }
}
