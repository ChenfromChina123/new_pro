package com.aispring.entity.checkpoint;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 聊天检查点实体
 * 用于实现"时间旅行"功能，记录文件快照和用户修改
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Entity
@Table(name = "chat_checkpoints", indexes = {
    @Index(name = "idx_session_user", columnList = "sessionId,userId"),
    @Index(name = "idx_message_order", columnList = "sessionId,messageOrder"),
    @Index(name = "idx_checkpoint_type", columnList = "checkpointType"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCheckpoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 检查点唯一标识（UUID）
     */
    @Column(nullable = false, unique = true, length = 64)
    private String checkpointId;
    
    /**
     * 会话ID
     */
    @Column(nullable = false, length = 100)
    private String sessionId;
    
    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 检查点类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckpointType checkpointType;
    
    /**
     * 消息顺序（关联到 chat_records 的 message_order）
     */
    @Column(nullable = false)
    private Integer messageOrder;
    
    /**
     * 文件快照（JSON 格式）
     * 结构: {"path": {"fileContent": "...", "diffAreas": [...]}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, FileSnapshot> fileSnapshots;
    
    /**
     * 用户修改快照（区分 AI 修改和用户修改）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, FileSnapshot> userModifications;
    
    /**
     * 检查点描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    /**
     * 文件快照内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileSnapshot {
        /**
         * 文件内容
         */
        private String fileContent;
        
        /**
         * Diff 区域（用于显示修改）
         */
        private java.util.List<DiffArea> diffAreas;
    }
    
    /**
     * Diff 区域内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffArea {
        /**
         * 起始行
         */
        private Integer startLine;
        
        /**
         * 结束行
         */
        private Integer endLine;
        
        /**
         * 修改类型（INSERT, DELETE, REPLACE）
         */
        private String changeType;
        
        /**
         * 原始内容
         */
        private String originalContent;
        
        /**
         * 新内容
         */
        private String newContent;
    }
}

