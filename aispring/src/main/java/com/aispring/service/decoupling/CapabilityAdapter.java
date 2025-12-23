package com.aispring.service.decoupling;

import com.aispring.entity.agent.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 能力适配器 - 将抽象能力映射到具体实现
 * 
 * 核心思想：模型只能提议（Propose），系统决定是否执行（Decide）
 */
@Component
@Slf4j
public class CapabilityAdapter {

    public enum Capability {
        READ_FILE,
        WRITE_FILE,
        EXECUTE_COMMAND,
        APPLY_DIFF,
        RUN_TEST
    }

    private final Map<Capability, Function<Map<String, Object>, ToolResult>> adapters = new HashMap<>();

    /**
     * 注册能力适配器
     */
    public void register(Capability capability, Function<Map<String, Object>, ToolResult> adapter) {
        adapters.put(capability, adapter);
        log.info("Registered capability adapter: {}", capability);
    }

    /**
     * 执行工具提议
     */
    public ToolResult execute(Capability capability, Map<String, Object> params, String decisionId) {
        Function<Map<String, Object>, ToolResult> adapter = adapters.get(capability);
        
        if (adapter == null) {
            log.warn("Capability {} not registered", capability);
            return ToolResult.builder()
                    .decisionId(decisionId)
                    .exitCode(1)
                    .stderr("Capability " + capability + " not registered")
                    .build();
        }

        try {
            return adapter.apply(params);
        } catch (Exception e) {
            log.error("Error executing capability {}: {}", capability, e.getMessage(), e);
            return ToolResult.builder()
                    .decisionId(decisionId)
                    .exitCode(1)
                    .stderr(e.getMessage())
                    .build();
        }
    }

    /**
     * 检查能力是否已注册
     */
    public boolean isRegistered(Capability capability) {
        return adapters.containsKey(capability);
    }
}

