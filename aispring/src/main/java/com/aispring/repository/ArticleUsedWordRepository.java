package com.aispring.repository;

import com.aispring.entity.ArticleUsedWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文章使用单词关联仓库接口
 */
@Repository
public interface ArticleUsedWordRepository extends JpaRepository<ArticleUsedWord, Integer> {
    
    /**
     * 根据文章ID查找所有使用的单词
     */
    List<ArticleUsedWord> findByArticleId(Integer articleId);
    
    /**
     * 根据单词ID查找所有使用该单词的文章
     */
    List<ArticleUsedWord> findByWordId(Integer wordId);
    
    /**
     * 删除文章的所有单词关联
     */
    void deleteByArticleId(Integer articleId);
}

