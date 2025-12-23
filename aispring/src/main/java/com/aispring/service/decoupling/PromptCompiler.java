package com.aispring.service.decoupling;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词编译器 - 将配置编译为最终自然语言
 * 
 * 核心思想：Prompt = 可丢弃的产物（View）
 *          Identity / Task / Constraint = 系统状态（State）
 */
@Component
public class PromptCompiler {

    @Data
    @Builder
    public static class PromptConfig {
        private IdentityType identity;
        private TaskRole role;
        private String objective;
        @Builder.Default
        private List<String> constraints = new ArrayList<>();
        @Builder.Default
        private List<BehavioralBias> bias = new ArrayList<>();
        private String context;
    }

    public enum IdentityType {
        IDE_ENGINEER("你是一位专业的 IDE 嵌入式软件工程师"),
        TERMINAL_ASSISTANT("你是一个运行在专用文件夹系统中的智能终端助手"),
        CODE_REVIEWER("你是一位经验丰富的代码审查专家");

        private final String template;

        IdentityType(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }

    public enum TaskRole {
        REFACTORER("你的任务是重构代码，提高可读性和可维护性"),
        DEBUGGER("你的任务是定位和修复代码中的错误"),
        PLANNER("你的任务是分析用户意图，生成结构化的任务流水线"),
        EXECUTOR("你是自主工程执行 Agent，目标是完成当前任务"),
        CHATTER("你是 AI 终端助手，回答用户问题并提供工程建议");

        private final String template;

        TaskRole(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }

    public enum BehavioralBias {
        MINIMAL_CHANGE("优先进行最小化修改，保持现有行为不变"),
        AVOID_SPECULATION("避免推测，只基于确定的事实进行决策"),
        SAFE_FIRST("安全第一，所有操作都需要经过验证"),
        FAST_EXECUTION("优先快速执行，在保证正确性的前提下提高效率");

        private final String template;

        BehavioralBias(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }

    /**
     * 将配置编译为最终 Prompt
     */
    public String render(PromptConfig config) {
        StringBuilder sb = new StringBuilder();

        // 1. Identity
        if (config.getIdentity() != null) {
            sb.append("# 角色\n");
            sb.append(config.getIdentity().getTemplate()).append("\n");
        }

        // 2. Role
        if (config.getRole() != null) {
            sb.append("\n# 任务\n");
            sb.append(config.getRole().getTemplate()).append("\n");
        }

        // 3. Objective
        if (config.getObjective() != null && !config.getObjective().isEmpty()) {
            sb.append("\n# 目标\n");
            sb.append(config.getObjective()).append("\n");
        }

        // 4. Constraints
        if (config.getConstraints() != null && !config.getConstraints().isEmpty()) {
            sb.append("\n# 约束\n");
            for (String constraint : config.getConstraints()) {
                sb.append("- ").append(constraint).append("\n");
            }
        }

        // 5. Behavioral Bias
        if (config.getBias() != null && !config.getBias().isEmpty()) {
            sb.append("\n# 行为偏好\n");
            for (BehavioralBias bias : config.getBias()) {
                sb.append("- ").append(bias.getTemplate()).append("\n");
            }
        }

        // 6. Context
        if (config.getContext() != null && !config.getContext().isEmpty()) {
            sb.append("\n# 上下文\n");
            sb.append(config.getContext());
        }

        return sb.toString();
    }
}

