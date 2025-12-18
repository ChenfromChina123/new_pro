package com.aispring.service;

import com.aispring.entity.*;
import com.aispring.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 词汇学习服务
 * 对应Python: language_learning.py中的词汇相关功能
 */
@Service
@RequiredArgsConstructor
public class VocabularyService {
    
    private final VocabularyListRepository vocabularyListRepository;
    private final VocabularyWordRepository vocabularyWordRepository;
    private final UserWordProgressRepository userWordProgressRepository;
    private final PublicVocabularyWordRepository publicVocabularyWordRepository;
    private final UserLearningRecordRepository userLearningRecordRepository;
    
    /**
     * 创建单词表
     */
    @Transactional
    public VocabularyList createVocabularyList(Long userId, String name, String description, String language) {
        VocabularyList list = VocabularyList.builder()
            .name(name)
            .description(description)
            .language(language != null ? language : "en")
            .isPreset(false)
            .isPublic(false)
            .createdBy(userId)
            .build();
        
        return vocabularyListRepository.save(list);
    }
    
    /**
     * 获取用户的单词表列表
     */
    public List<VocabularyList> getUserVocabularyLists(Long userId) {
        return vocabularyListRepository.findByUserIdOrPublic(userId);
    }
    
    /**
     * 添加单词到单词表
     */
    @Transactional
    public VocabularyWord addWordToList(Integer vocabularyListId, String word, String definition, 
                                        String partOfSpeech, String example, String language) {
        VocabularyWord vocabularyWord = VocabularyWord.builder()
            .vocabularyListId(vocabularyListId)
            .word(word)
            .definition(definition)
            .partOfSpeech(partOfSpeech)
            .example(example)
            .language(language != null ? language : "en")
            .build();
        
        return vocabularyWordRepository.save(vocabularyWord);
    }
    
    /**
     * 获取单词表中的所有单词
     */
    public List<VocabularyWord> getWordsInList(Integer vocabularyListId) {
        return vocabularyWordRepository.findByVocabularyListIdOrderByCreatedAtDesc(vocabularyListId);
    }
    
    /**
     * 删除单词表
     */
    @Transactional
    public void deleteVocabularyList(Integer listId) {
        vocabularyListRepository.deleteById(listId);
    }
    
    /**
     * 删除单词
     */
    @Transactional
    public void deleteWord(Integer wordId) {
        vocabularyWordRepository.deleteById(wordId);
    }
    
    /**
     * 更新用户单词进度
     */
    @Transactional
    public UserWordProgress updateUserWordProgress(Long userId, Integer wordId, Integer masteryLevel, Boolean isDifficult) {
        Optional<UserWordProgress> existingProgress = userWordProgressRepository.findByUserIdAndWordId(userId, wordId);
        
        UserWordProgress progress;
        if (existingProgress.isPresent()) {
            progress = existingProgress.get();
            if (masteryLevel != null) {
                progress.setMasteryLevel(masteryLevel);
            }
            if (isDifficult != null) {
                progress.setIsDifficult(isDifficult);
            }
            progress.setLastReviewed(LocalDateTime.now());
            progress.setReviewCount(progress.getReviewCount() + 1);
            
            // 计算下次复习时间（简单的间隔重复算法）
            int daysToAdd = calculateNextReviewDays(progress.getMasteryLevel());
            progress.setNextReviewDate(LocalDateTime.now().plusDays(daysToAdd));
        } else {
            progress = UserWordProgress.builder()
                .userId(userId)
                .wordId(wordId)
                .masteryLevel(masteryLevel != null ? masteryLevel : 0)
                .isDifficult(isDifficult != null ? isDifficult : false)
                .lastReviewed(LocalDateTime.now())
                .reviewCount(1)
                .nextReviewDate(LocalDateTime.now().plusDays(1))
                .build();
        }
        
        return userWordProgressRepository.save(progress);
    }
    
    /**
     * 计算下次复习天数（间隔重复算法）
     */
    private int calculateNextReviewDays(int masteryLevel) {
        return switch (masteryLevel) {
            case 0 -> 1;   // 不熟悉，1天后复习
            case 1 -> 3;   // 稍微熟悉，3天后复习
            case 2 -> 7;   // 比较熟悉，7天后复习
            case 3 -> 14;  // 熟悉，14天后复习
            case 4 -> 30;  // 很熟悉，30天后复习
            case 5 -> 90;  // 完全掌握，90天后复习
            default -> 1;
        };
    }
    
    /**
     * 获取用户需要复习的单词
     */
    public List<UserWordProgress> getUserReviewWords(Long userId) {
        return userWordProgressRepository.findDueForReview(userId, LocalDateTime.now());
    }
    
    /**
     * 获取用户的学习统计
     */
    public LearningStats getUserLearningStats(Long userId) {
        long totalWords = userWordProgressRepository.countByUserId(userId);
        long masteredWords = userWordProgressRepository.countMasteredWords(userId);
        long totalDuration = userLearningRecordRepository.getTotalDuration(userId);
        long todayDuration = userLearningRecordRepository.getTodayDuration(userId);
        
        return new LearningStats(totalWords, masteredWords, totalDuration, todayDuration);
    }
    
    /**
     * 学习统计数据类
     */
    public record LearningStats(long totalWords, long masteredWords, long totalDuration, long todayDuration) {}
    
    /**
     * 记录学习活动
     */
    @Transactional
    public void recordLearningActivity(Long userId, String activityType, String activityDetails, Integer duration) {
        UserLearningRecord record = UserLearningRecord.builder()
            .userId(userId)
            .activityType(activityType)
            .activityDetails(activityDetails)
            .duration(duration)
            .build();
        
        userLearningRecordRepository.save(record);
    }
    
    /**
     * 从公共词库查找单词
     */
    public Optional<PublicVocabularyWord> findPublicWord(String word, String language) {
        return publicVocabularyWordRepository.findByWordAndLanguage(word, language);
    }
    
    /**
     * 搜索公共词库
     */
    public List<PublicVocabularyWord> searchPublicWords(String keyword, String language) {
        return publicVocabularyWordRepository.searchByKeyword(language, keyword);
    }
}

