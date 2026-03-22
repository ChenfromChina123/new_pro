package com.aispring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户单词进度实体类
 * 对应Python: UserWordProgress模型
 */
@Entity
@Table(name = "user_word_progress",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_word_id", columnList = "word_id"),
        @Index(name = "idx_user_word", columnList = "user_id, word_id", unique = true)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWordProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "word_id", nullable = false)
    private Integer wordId;
    
    /**
     * 状态: 0=未学, 1=学习中, 2=已掌握, 3=易错
     */
    @Builder.Default
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Builder.Default
    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0;

    @Column(name = "next_review_time")
    private LocalDateTime nextReviewTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 兼容原有的旧字段
    @Column(name = "mastery_level")
    private Integer masteryLevel;

    @Column(name = "is_difficult")
    private Boolean isDifficult;

    @Column(name = "last_reviewed")
    private LocalDateTime lastReviewed;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "word_id", insertable = false, updatable = false)
    private VocabularyWord word;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

