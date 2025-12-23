package com.aispring.entity.approval;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户批准设置实体
 * 配置哪些工具需要用户批准
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Entity
@Table(name = "user_approval_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserApprovalSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID（唯一）
     */
    @Column(nullable = false, unique = true)
    private Long userId;
    
    /**
     * 自动批准危险工具（删除文件、执行命令等）
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean autoApproveDangerousTools = false;
    
    /**
     * 自动批准读取文件
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean autoApproveReadFile = true;
    
    /**
     * 自动批准文件编辑（edit_file, write_file, rewrite_file）
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean autoApproveFileEdits = false;
    
    /**
     * 自动批准 MCP 工具（第三方工具）
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean autoApproveMcpTools = false;
    
    /**
     * 工具执行后显示 Lint 错误
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean includeToolLintErrors = true;
    
    /**
     * 每个会话保留的最大检查点数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxCheckpointsPerSession = 50;
    
    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    /**
     * 更新时间
     */
    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    /**
     * 在持久化前更新时间戳
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

