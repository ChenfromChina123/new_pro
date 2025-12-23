package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具执行结果
 * 注意：由于全局 Jackson 配置使用 SNAKE_CASE，字段会自动转换为下划线命名
 * JSON 序列化：decision_id, exit_code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    private String decisionId;  // JSON: decision_id
    private int exitCode;       // JSON: exit_code
    private String stdout;
    private String stderr;
    private List<String> artifacts;
}
