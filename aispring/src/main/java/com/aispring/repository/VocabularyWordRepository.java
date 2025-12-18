package com.aispring.repository;

import com.aispring.entity.VocabularyWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 单词仓库接口
 */
@Repository
public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, Integer> {
    
    /**
     * 根据单词表ID查找所有单词
     */
    List<VocabularyWord> findByVocabularyListIdOrderByCreatedAtDesc(Integer vocabularyListId);
    
    /**
     * 根据单词和单词表ID查找
     */
    Optional<VocabularyWord> findByWordAndVocabularyListId(String word, Integer vocabularyListId);
    
    /**
     * 根据语言和单词查找
     */
    List<VocabularyWord> findByLanguageAndWordContainingIgnoreCase(String language, String word);
    
    /**
     * 统计单词表中的单词数量
     */
    long countByVocabularyListId(Integer vocabularyListId);
    
    /**
     * 批量删除单词表的所有单词
     */
    void deleteByVocabularyListId(Integer vocabularyListId);
    
    /**
     * 根据ID列表查找单词
     */
    List<VocabularyWord> findByIdIn(List<Integer> ids);
}

