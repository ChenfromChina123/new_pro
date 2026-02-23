package com.aispring.repository;

import com.aispring.entity.UserWordProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户单词进度仓库接口
 */
@Repository
public interface UserWordProgressRepository extends JpaRepository<UserWordProgress, Long> {
    
    /**
     * 根据用户ID和单词ID查找进度
     */
    Optional<UserWordProgress> findByUserIdAndWordId(Long userId, Integer wordId);
    
    /**
     * 根据用户ID查找所有进度
     */
    List<UserWordProgress> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    /**
     * 根据用户ID查找需要复习的单词
     */
    @Query("SELECT p FROM UserWordProgress p WHERE p.userId = :userId AND p.nextReviewDate <= :now ORDER BY p.nextReviewDate")
    List<UserWordProgress> findDueForReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM UserWordProgress p JOIN p.word w WHERE p.userId = :userId AND w.vocabularyListId = :listId ORDER BY p.updatedAt DESC")
    List<UserWordProgress> findByUserIdAndVocabularyListId(@Param("userId") Long userId, @Param("listId") Integer listId);
    
    /**
     * 根据用户ID查找困难单词
     */
    List<UserWordProgress> findByUserIdAndIsDifficultTrue(Long userId);
    
    /**
     * 根据用户ID和掌握程度查找
     */
    List<UserWordProgress> findByUserIdAndMasteryLevel(Long userId, Integer masteryLevel);
    
    /**
     * 统计用户学习的单词数
     */
    long countByUserId(Long userId);
    
    /**
     * 统计用户掌握的单词数（掌握程度>=4）
     */
    @Query("SELECT COUNT(p) FROM UserWordProgress p WHERE p.userId = :userId AND p.masteryLevel >= 4")
    long countMasteredWords(@Param("userId") Long userId);
}

