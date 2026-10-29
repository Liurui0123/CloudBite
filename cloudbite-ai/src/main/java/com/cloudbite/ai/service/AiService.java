package com.cloudbite.ai.service;

import com.cloudbite.ai.client.MenuClient;
import com.cloudbite.ai.client.OrderClient;
import com.cloudbite.ai.entity.AiLog;
import com.cloudbite.ai.repo.AiLogRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AiService {
    private final DeepSeekClient llm;
    private final MenuClient menuClient;
    private final OrderClient orderClient;
    private final AiLogRepository logRepo;

    public AiService(DeepSeekClient llm, MenuClient menuClient, OrderClient orderClient, AiLogRepository logRepo) {
        this.llm = llm;
        this.menuClient = menuClient;
        this.orderClient = orderClient;
        this.logRepo = logRepo;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> dishes() {
        try {
            Map<String, Object> resp = menuClient.dishes();
            Object data = resp.get("data");
            if (data instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    public Map<String, Object> recommend(Long userId, String preference) {
        List<Map<String, Object>> all = dishes();
        List<Map<String, Object>> picked = new ArrayList<>(all.stream()
                .sorted((a, b) -> Integer.compare(num(b.get("sales")), num(a.get("sales"))))
                .limit(3)
                .toList());

        String answer = null;
        if (llm.enabled() && !all.isEmpty()) {
            StringBuilder menuText = new StringBuilder();
            for (Map<String, Object> d : all) {
                menuText.append("- ").append(d.get("name"))
                        .append("，￥").append(d.get("price"))
                        .append("，标签：").append(d.get("tag") == null ? "无" : d.get("tag"))
                        .append("，销量：").append(d.get("sales")).append("\n");
            }
            String system = "你是「CloudBite 云餐」的智能点餐助手。根据菜单和用户偏好，推荐 3 道菜，"
                    + "用中文回答，格式：每道菜一行，先写菜名，再写一句话推荐理由（不超过 30 字）。不要输出其它内容。";
            String user = "用户偏好：" + (preference == null || preference.isBlank() ? "无特别偏好，按人气推荐" : preference)
                    + "\n\n今日菜单：\n" + menuText;
            answer = llm.chat(system, user);
            saveLog(userId, "RECOMMEND", preference, answer);
        }
        if (answer == null) {
            answer = picked.stream()
                    .map(d -> d.get("name") + "：" + (d.get("tag") == null ? "人气之选，销量 " + d.get("sales") : d.get("tag")))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("暂无推荐");
        } else {
            // 让返回的菜品卡片与大模型实际提到的菜名对齐
            List<Map<String, Object>> mentioned = new ArrayList<>();
            for (Map<String, Object> d : all) {
                if (answer.contains(String.valueOf(d.get("name")))) {
                    mentioned.add(d);
                }
            }
            if (mentioned.size() >= 2) {
                picked = mentioned.subList(0, Math.min(3, mentioned.size()));
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", answer);
        res.put("dishes", picked);
        res.put("aiEnabled", llm.enabled());
        return res;
    }

    public Map<String, Object> chat(Long userId, String message, String orderNo) {
        String orderContext = "";
        if (orderNo != null && !orderNo.isBlank()) {
            try {
                Map<String, Object> resp = orderClient.detail(orderNo);
                Object data = resp.get("data");
                if (data instanceof Map<?, ?> m) {
                    orderContext = "该订单状态：" + m.get("status")
                            + "，金额：￥" + m.get("totalAmount")
                            + "，下单时间：" + m.get("createdAt");
                }
            } catch (Exception ignored) {
            }
        }
        String answer = null;
        if (llm.enabled()) {
            String system = "你是「CloudBite 云餐」的在线客服，语气亲切简洁，回答不超过 120 字。"
                    + "订单状态含义：PENDING_PAYMENT 待支付、PAID 已支付、COOKING 制作中、COMPLETED 已完成、CANCELLED 已取消。"
                    + "如果用户问订单进度且提供了订单信息，就据此回答；否则提示用户提供订单号。";
            answer = llm.chat(system, "用户问题：" + message + (orderContext.isBlank() ? "" : "\n\n" + orderContext));
            saveLog(userId, "CHAT", message, answer);
        }
        if (answer == null) {
            answer = orderContext.isBlank()
                    ? "您好，我是 CloudBite 的智能客服。可以提供订单号，我帮您查询进度～"
                    : "您的订单 " + orderNo + " " + orderContext.replace("该订单状态：", "当前状态是 ") + "。";
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", answer);
        res.put("aiEnabled", llm.enabled());
        return res;
    }

    public Map<String, Object> describeDish(Long userId, String dishName, String ingredients) {
        String answer = null;
        if (llm.enabled()) {
            String system = "你是餐厅文案策划，写 60 字以内的菜品推荐文案，要有食欲感，只输出文案本身。";
            answer = llm.chat(system, "菜名：" + dishName + "；主要食材：" + (ingredients == null ? "未知" : ingredients));
            saveLog(userId, "DESCRIBE", dishName, answer);
        }
        if (answer == null) {
            answer = dishName + "，选用" + (ingredients == null ? "新鲜食材" : ingredients) + "精心烹制，口感层次丰富，值得一试。";
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", answer);
        res.put("aiEnabled", llm.enabled());
        return res;
    }

    private void saveLog(Long userId, String type, String prompt, String answer) {
        try {
            AiLog log = new AiLog();
            log.setUserId(userId);
            log.setType(type);
            log.setPrompt(prompt == null ? "" : prompt.substring(0, Math.min(prompt.length(), 480)));
            log.setAnswer(answer == null ? "" : answer.substring(0, Math.min(answer.length(), 3900)));
            logRepo.save(log);
        } catch (Exception ignored) {
        }
    }

    private int num(Object o) {
        if (o == null) return 0;
        try {
            return new BigDecimal(String.valueOf(o)).intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}