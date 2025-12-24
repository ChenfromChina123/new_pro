package com.aispring.entity.session;

import com.aispring.entity.agent.AgentStatus;
import com.aispring.entity.agent.DecisionEnvelope;
import com.aispring.entity.agent.TaskState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 会话状态数据库实体
 * 用于 Redis 状态的持久化备份
 *
 * @author AISpring Team
 * @since 2025-12-24
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "session_states")
public class SessionStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_status", nullable = false)
    private AgentStatus agentStatus;

    @Column(name = "current_loop_id", length = 64)
    private String currentLoopId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stream_state", columnDefinition = "json")
    private StreamState streamState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "task_state", columnDefinition = "json")
    private TaskState taskState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "last_decision", columnDefinition = "json")
    private DecisionEnvelope lastDecision;

    @Column(name = "last_checkpoint_id", length = 64)
    private String lastCheckpointId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        lastActiveAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        lastActiveAt = Instant.now();
    }

    /**
     * 从 SessionState POJO 转换
     */
    public static SessionStateEntity fromPojo(SessionState pojo) {
        if (pojo == null) return null;
        return SessionStateEntity.builder()
                .sessionId(pojo.getSessionId())
                .userId(pojo.getUserId())
                .agentStatus(pojo.getStatus())
                .currentLoopId(pojo.getCurrentLoopId())
                .streamState(pojo.getStreamState())
                .taskState(pojo.getTaskState())
                .lastDecision(pojo.getLastDecision())
                .lastCheckpointId(pojo.getLastCheckpointId())
                .updatedAt(pojo.getUpdatedAt())
                .lastActiveAt(pojo.getUpdatedAt())
                .build();
    }

    /**
     * 转换为 SessionState POJO
     */
    public SessionState toPojo() {
        return SessionState.builder()
                .sessionId(this.sessionId)
                .userId(this.userId)
                .status(this.agentStatus)
                .currentLoopId(this.currentLoopId)
                .streamState(this.streamState)
                .taskState(this.taskState)
                .lastDecision(this.lastDecision)
                .lastCheckpointId(this.lastCheckpointId)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
