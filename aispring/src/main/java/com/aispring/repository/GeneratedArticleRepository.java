package com.aispring.repository;

import com.aispring.entity.GeneratedArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI生成文章仓库接口
 */
@Repository
public interface GeneratedArticleRepository extends JpaRepository<GeneratedArticle, Integer> {
    
    /**
     * 根据用户ID查找文章
     */
    List<GeneratedArticle> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据用户ID和单词表ID查找文章
     */
    List<GeneratedArticle> findByUserIdAndVocabularyListIdOrderByCreatedAtDesc(Long userId, Integer vocabularyListId);
    
    /**
     * 根据难度级别查找文章
     */
    List<GeneratedArticle> findByUserIdAndDifficultyLevelOrderByCreatedAtDesc(Long userId, String difficultyLevel);
    
    /**
     * 统计用户生成的文章数量
     */
    long countByUserId(Long userId);
    
    /**
     * 获取用户最近的N篇文章
     */
    List<GeneratedArticle> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}

