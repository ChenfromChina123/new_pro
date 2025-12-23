package com.aispring.entity.session;

import com.aispring.entity.agent.AgentStatus;
import com.aispring.entity.agent.DecisionEnvelope;
import com.aispring.entity.agent.TaskState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 会话状态实体
 * 存储在 Redis 中，用于 Agent 循环的状态管理
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * Agent 状态
     */
    @Builder.Default
    private AgentStatus status = AgentStatus.IDLE;
    
    /**
     * 当前 Agent 循环ID（用于中断）
     */
    private String currentLoopId;
    
    /**
     * 流式状态
     */
    private StreamState streamState;
    
    /**
     * 任务流水线状态（可选）
     */
    private TaskState taskState;
    
    /**
     * 最后一次决策
     */
    private DecisionEnvelope lastDecision;
    
    /**
     * 最后一个检查点ID
     */
    private String lastCheckpointId;
    
    /**
     * 创建时间
     */
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    /**
     * 更新时间
     */
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    /**
     * 创建新的空闲状态
     */
    public static SessionState newIdle(String sessionId, Long userId) {
        return SessionState.builder()
                .sessionId(sessionId)
                .userId(userId)
                .status(AgentStatus.IDLE)
                .streamState(StreamState.idle())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * 更新时间戳
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }
}

