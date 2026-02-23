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
    
    @Builder.Default
    @Column(name = "mastery_level", nullable = false)
    private Integer masteryLevel = 0;  // 0-5 表示掌握程度
    
    @Column(name = "last_reviewed")
    private LocalDateTime lastReviewed;
    
    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;
    
    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;
    
    @Builder.Default
    @Column(name = "is_difficult", nullable = false)
    private Boolean isDifficult = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
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

