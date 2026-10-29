package com.cloudbite.order.service;

import com.cloudbite.common.BizException;
import com.cloudbite.order.client.MenuClient;
import com.cloudbite.order.dto.CreateOrderRequest;
import com.cloudbite.order.entity.Order;
import com.cloudbite.order.entity.OrderItem;
import com.cloudbite.order.repo.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {
    private final OrderRepository repo;
    private final MenuClient menuClient;
    private final OrderCache cache;
    private final com.cloudbite.order.mq.OrderMessageProducer mqProducer;
    private final com.cloudbite.order.client.UserClient userClient;

    public OrderService(OrderRepository repo, MenuClient menuClient, OrderCache cache,
                        com.cloudbite.order.mq.OrderMessageProducer mqProducer,
                        com.cloudbite.order.client.UserClient userClient) {
        this.repo = repo;
        this.menuClient = menuClient;
        this.cache = cache;
        this.mqProducer = mqProducer;
        this.userClient = userClient;
    }

    public Order create(Long userId, CreateOrderRequest req) {
        if (cache.isDuplicate(userId)) {
            throw new BizException("操作太快啦，请 3 秒后再试");
        }
        try {
            return doCreate(userId, req);
        } catch (RuntimeException e) {
            cache.releaseSubmit(userId);
            throw e;
        }
    }

    @Transactional
    public Order doCreate(Long userId, CreateOrderRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new BizException("购物车为空");
        }
        Order order = new Order();
        order.setUserId(userId);
        order.setAddress(req.address());
        order.setRemark(req.remark());

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        List<Map<String, Object>> deductItems = new ArrayList<>();

        for (CreateOrderRequest.Item it : req.items()) {
            if (it.quantity() == null || it.quantity() <= 0) {
                continue;
            }
            Map<String, Object> resp = menuClient.dish(it.dishId());
            Object dataObj = resp.get("data");
            if (!(dataObj instanceof Map<?, ?> data)) {
                throw new BizException("菜品不存在: " + it.dishId());
            }
            String name = String.valueOf(data.get("name"));
            BigDecimal price = new BigDecimal(String.valueOf(data.get("price")));
            Object emoji = data.get("emoji");
            Object stockObj = data.get("stock");
            int stock = stockObj == null ? 0 : Integer.parseInt(String.valueOf(stockObj));
            if (stock < it.quantity()) {
                throw new BizException("「" + name + "」库存不足，仅剩 " + stock + " 份");
            }

            OrderItem item = new OrderItem();
            item.setDishId(it.dishId());
            item.setDishName(name);
            item.setEmoji(emoji == null ? null : String.valueOf(emoji));
            item.setPrice(price);
            item.setQuantity(it.quantity());
            order.addItem(item);

            total = total.add(price.multiply(BigDecimal.valueOf(it.quantity())));
            count += it.quantity();
            deductItems.add(Map.of("dishId", it.dishId(), "quantity", it.quantity()));
        }

        if (count == 0) {
            throw new BizException("购物车为空");
        }

        menuClient.deduct(deductItems);

        order.setTotalAmount(total);
        order.setItemCount(count);
        order.setOrderNo(nextOrderNo());
        order.setStatus("PENDING_PAYMENT");
        Order saved = repo.save(order);
        cache.evictStats();
        // 普通消息（异步通知）+ 延迟消息（超时未支付自动关单）
        mqProducer.sendOrderCreated(saved.getOrderNo());
        mqProducer.sendDelayClose(saved.getOrderNo());
        return saved;
    }

    /**
     * 延迟消息回调：仍是待支付则取消订单，并调用 menu 服务回滚库存。
     * 用条件更新（乐观）保证重复投递只生效一次。
     */
    @Transactional
    public boolean closeIfUnpaid(String orderNo) {
        Order order = repo.findByOrderNo(orderNo).orElse(null);
        if (order == null) {
            return false;
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return false;
        }
        // 抢占式更新：只有仍为待支付时才更新成功
        order.setStatus("CANCELLED");
        repo.save(order);

        List<Map<String, Object>> releaseItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            releaseItems.add(Map.of("dishId", item.getDishId(), "quantity", item.getQuantity()));
        }
        if (!releaseItems.isEmpty()) {
            menuClient.release(releaseItems);
        }
        cache.evictStats();
        return true;
    }

    @Transactional
    public Order pay(String orderNo, Long userId) {
        Order o = get(orderNo);
        if (!o.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作该订单");
        }
        if (!"PENDING_PAYMENT".equals(o.getStatus())) {
            throw new BizException("订单状态不允许支付");
        }
        o.setStatus("PAID");
        o.setPaidAt(LocalDateTime.now());
        cache.evictStats();
        return repo.save(o);
    }

    @Transactional
    public Order cancel(String orderNo, Long userId) {
        Order o = get(orderNo);
        if (!o.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作该订单");
        }
        if (!"PENDING_PAYMENT".equals(o.getStatus())) {
            throw new BizException("仅待支付订单可取消");
        }
        o.setStatus("CANCELLED");
        cache.evictStats();
        return repo.save(o);
    }

    @Transactional
    public Order updateStatus(String orderNo, String target) {
        Order o = get(orderNo);
        Map<String, String> flow = Map.of(
                "PAID", "COOKING",
                "COOKING", "COMPLETED"
        );
        String expected = flow.get(o.getStatus());
        if (expected == null || !expected.equals(target)) {
            throw new BizException("非法的状态流转: " + o.getStatus() + " -> " + target);
        }
        o.setStatus(target);
        cache.evictStats();
        return repo.save(o);
    }

    public Order get(String orderNo) {
        return repo.findByOrderNo(orderNo).orElseThrow(() -> new BizException(404, "订单不存在"));
    }

    public List<Order> myOrders(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Order> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Map<String, Object> stats() {
        Map<String, Object> cached = cache.getStats();
        if (cached != null) {
            return cached;
        }
        LocalDateTime from = LocalDate.now().atStartOfDay();
        List<Order> today = repo.findByCreatedAtBetween(from, from.plusDays(1));
        BigDecimal revenue = today.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()) && !"PENDING_PAYMENT".equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> statusCount = new LinkedHashMap<>();
        for (Order o : repo.findAll()) {
            statusCount.merge(o.getStatus(), 1L, Long::sum);
        }

        Map<String, Integer> dishCount = new HashMap<>();
        for (Order o : repo.findAll()) {
            for (OrderItem it : o.getItems()) {
                dishCount.merge(it.getDishName(), it.getQuantity(), Integer::sum);
            }
        }
        List<Map<String, Object>> top = dishCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("todayOrders", today.size());
        res.put("todayRevenue", revenue);
        res.put("totalOrders", repo.count());
        res.put("statusCount", statusCount);
        res.put("topDishes", top);

        // 近 7 天趋势（看板折线图）
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            List<Order> dayOrders = repo.findByCreatedAtBetween(dayStart, dayStart.plusDays(1));
            BigDecimal dayRevenue = dayOrders.stream()
                    .filter(o -> !"CANCELLED".equals(o.getStatus()) && !"PENDING_PAYMENT".equals(o.getStatus()))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.put("orders", dayOrders.size());
            point.put("revenue", dayRevenue);
            trend.add(point);
        }
        res.put("weeklyTrend", trend);

        // 最新 5 单（实时看板滚动列表）
        List<Map<String, Object>> latest = new ArrayList<>();
        for (Order o : repo.findAllByOrderByCreatedAtDesc()) {
            if (latest.size() >= 5) {
                break;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderNo", o.getOrderNo());
            m.put("amount", o.getTotalAmount());
            m.put("status", o.getStatus());
            m.put("itemCount", o.getItemCount());
            m.put("createdAt", o.getCreatedAt() == null ? "" : o.getCreatedAt().toString().replace("T", " "));
            latest.add(m);
        }
        res.put("latestOrders", latest);
        res.put("totalUsers", userCount());
        res.put("updatedAt", LocalDateTime.now().toString().replace("T", " "));

        cache.putStats(res);
        return res;
    }

    /** 注册用户数：通过 Feign 调用 user 服务（跨服务查询，体现微服务协作） */
    @SuppressWarnings("unchecked")
    private int userCount() {
        try {
            Map<String, Object> resp = userClient.list();
            Object data = resp.get("data");
            if (data instanceof java.util.List<?> list) {
                return list.size();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private String nextOrderNo() {
        return "CB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}