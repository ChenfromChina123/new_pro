package com.aispring.service.decoupling;

import com.aispring.service.decoupling.CapabilityAdapter.Capability;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 调用策略 - 决定是否执行工具提议
 * 
 * 核心思想：模型永远没有"执行权"，只有系统可以决定
 */
@Component
@Data
public class InvocationPolicy {

    private boolean modelCanPropose = true;
    private boolean hostCanExecute = true;
    private List<Capability> whitelist = new ArrayList<>();
    private List<Capability> blacklist = new ArrayList<>();

    /**
     * 判断是否可以执行
     * @return (canExecute, reason)
     */
    public PolicyResult canExecute(Capability capability) {
        // 检查黑名单
        if (blacklist.contains(capability)) {
            return PolicyResult.denied("Capability " + capability + " is blacklisted");
        }

        // 检查白名单（如果设置了白名单）
        if (!whitelist.isEmpty() && !whitelist.contains(capability)) {
            return PolicyResult.denied("Capability " + capability + " not in whitelist");
        }

        // 检查执行权限
        if (!hostCanExecute) {
            return PolicyResult.denied("Host execution is disabled");
        }

        return PolicyResult.allowed();
    }

    @Data
    public static class PolicyResult {
        private boolean allowed;
        private String reason;

        public static PolicyResult allowed() {
            PolicyResult result = new PolicyResult();
            result.allowed = true;
            result.reason = "OK";
            return result;
        }

        public static PolicyResult denied(String reason) {
            PolicyResult result = new PolicyResult();
            result.allowed = false;
            result.reason = reason;
            return result;
        }
    }
}

