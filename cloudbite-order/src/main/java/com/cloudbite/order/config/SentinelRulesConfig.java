package com.cloudbite.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 下单接口限流：QPS 阈值从 Nacos 配置中心读取，修改后无需重启即时生效。
 * Nacos 配置（dataId=cloudbite-order.yaml）：
 *   cloudbite.sentinel.order-qps: 5
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentinelRulesConfig implements ApplicationListener<RefreshScopeRefreshedEvent> {

    private final Environment environment;

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        log.info("检测到 Nacos 配置刷新，重新加载限流规则");
        reload();
    }

    private void reload() {
        double qps = Double.parseDouble(
                environment.getProperty("cloudbite.sentinel.order-qps", "5"));
        FlowRule rule = new FlowRule();
        rule.setResource("createOrder");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        List<FlowRule> rules = new ArrayList<>();
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
        log.info("Sentinel 限流规则已加载：createOrder QPS={}", qps);
    }
}