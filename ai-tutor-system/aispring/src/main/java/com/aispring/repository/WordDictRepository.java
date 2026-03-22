package com.aispring.repository;

import com.aispring.entity.WordDict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 单词词典仓储接口
 * 支持从 ECDICT 导入的 300万+ 词库查询
 */
@Repository
public interface WordDictRepository extends JpaRepository<WordDict, Long> {

    Optional<WordDict> findByWord(String word);

    Optional<WordDict> findByWordIgnoreCase(String word);

    List<WordDict> findByLevelTagsContaining(String tag);

    Page<WordDict> findByLevelTagsContaining(String tag, Pageable pageable);

    @Query("SELECT w FROM WordDict w WHERE w.word LIKE %:keyword% OR w.translation LIKE %:keyword%")
    Page<WordDict> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT w FROM WordDict w WHERE w.levelTags LIKE %:level% ORDER BY RAND() LIMIT :limit")
    List<WordDict> findRandomWordsByLevel(@Param("level") String level, @Param("limit") int limit);

    @Query("SELECT w FROM WordDict w ORDER BY RAND() LIMIT :limit")
    List<WordDict> findRandomWords(@Param("limit") int limit);

    boolean existsByWord(String word);

    long countByLevelTagsContaining(String tag);
}
