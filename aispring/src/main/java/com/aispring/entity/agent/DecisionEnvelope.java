package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 决策信封
 * 包含 AI 的工具调用决策信息
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEnvelope implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 决策ID（唯一标识）
     */
    private String decisionId;
    
    /**
     * 决策类型（TASK_COMPLETE, TOOL_CALL 等）
     */
    private String type;
    
    /**
     * 动作/操作（工具名称或其他操作）
     */
    private String action;
    
    /**
     * 工具名称（兼容字段）
     */
    private String toolName;
    
    /**
     * 工具参数
     */
    private Map<String, Object> params;
    
    /**
     * 决策原因/说明
     */
    private String reasoning;
    
    /**
     * 是否需要用户批准
     */
    @Builder.Default
    private boolean requiresApproval = false;
    
    /**
     * 决策期望（可选）
     */
    private DecisionExpectation expectation;
}
