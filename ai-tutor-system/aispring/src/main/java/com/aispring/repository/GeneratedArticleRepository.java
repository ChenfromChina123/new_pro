package com.aispring.repository;

import com.aispring.entity.GeneratedArticle;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
