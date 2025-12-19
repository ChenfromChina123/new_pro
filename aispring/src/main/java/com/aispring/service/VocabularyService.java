package com.aispring.service;

import com.aispring.entity.*;
import com.aispring.exception.CustomException;
import com.aispring.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final AiChatService aiChatService;
    private final GeneratedArticleRepository generatedArticleRepository;
    private final ArticleUsedWordRepository articleUsedWordRepository;
    private final ObjectMapper objectMapper;
    
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
        List<VocabularyList> lists = vocabularyListRepository.findByUserIdOrPublic(userId);
        // 填充单词数量
        for (VocabularyList list : lists) {
            long count = vocabularyWordRepository.countByVocabularyListId(list.getId());
            list.setWordCount(count);
        }
        return lists;
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

    public List<UserWordProgress> getUserProgressForList(Long userId, Integer listId) {
        return userWordProgressRepository.findByUserIdAndVocabularyListId(userId, listId);
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

    public PublicSearchResult searchPublicWordsPaged(String keyword, String language, Integer page, Integer size) {
        int safePage = page == null ? 1 : Math.max(1, page);
        int safeSize = size == null ? 50 : Math.min(Math.max(1, size), 200);

        String kw = keyword == null ? "" : keyword.trim();
        String lang = (language == null || language.isBlank()) ? "en" : language.trim();

        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        Page<PublicVocabularyWord> result;
        if (kw.isEmpty()) {
            result = publicVocabularyWordRepository.findByLanguageOrderByUsageCountDesc(lang, pageable);
        } else {
            result = publicVocabularyWordRepository.searchByKeywordPaged(lang, kw, pageable);
        }

        return new PublicSearchResult(result.getContent(), result.getTotalElements(), safePage, safeSize);
    }

    public record PublicSearchResult(List<PublicVocabularyWord> words, long total, int page, int size) {}
    
    /**
     * 生成文章主题建议
     */
    public List<String> generateArticleTopics(List<String> words, String language) {
        String wordsStr = String.join(", ", words);
        String prompt = String.format(
            "I have a list of %s words: [%s]. Please suggest 6 short article topics (titles) that could incorporate these words. " +
            "Each topic must be less than 10 characters long. \n" +
            "Return ONLY a JSON array of strings, e.g. [\"Topic 1\", \"Topic 2\"]. Do not include markdown formatting.",
            language, wordsStr
        );
        
        String response = aiChatService.ask(prompt, null, "deepseek-chat", "system");
        if (response == null) return new ArrayList<>();
        
        // Clean response (remove markdown code blocks if any)
        String json = response.replaceAll("```json", "").replaceAll("```", "").trim();
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>(){});
        } catch (Exception e) {
            System.err.println("Failed to parse topics JSON: " + e.getMessage());
            // Fallback to splitting if JSON parsing fails
            return Arrays.stream(response.split("[,，\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("[")) // Remove JSON brackets if split
                .filter(s -> !s.endsWith("]"))
                .limit(6)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 生成并保存文章
     */
    @Transactional
    public GeneratedArticle generateAndSaveArticle(Long userId, Integer listId, List<Integer> wordIds, 
                                                  String topic, String difficulty, String length) {
        // 1. 获取单词信息
        List<VocabularyWord> words = vocabularyWordRepository.findByIdIn(wordIds);
        String vocabularyList = words.stream()
            .map(VocabularyWord::getWord)
            .collect(Collectors.joining(", "));
            
        // 2. 调用AI生成文章
        String prompt = String.format(
            "Write a short article (Difficulty: %s, Length: %s) about '%s'. " +
            "You MUST use the following words in the article: [%s]. " +
            "Please wrap the used words in double asterisks like **word** so they can be highlighted. " +
            "Output only the article content. Do not include 'Title:' or markdown code blocks.",
            difficulty, length, topic, vocabularyList
        );
        
        String content = aiChatService.ask(prompt, null, "deepseek-chat", String.valueOf(userId));
        
        // Clean content
        if (content != null) {
            content = content.replaceAll("```markdown", "").replaceAll("```", "").trim();
            if (content.startsWith("Title:")) {
                content = content.substring(content.indexOf("\n") + 1).trim();
            }
        }
        
        // 3. 保存文章
        GeneratedArticle article = GeneratedArticle.builder()
            .userId(userId)
            .vocabularyListId(listId)
            .topic(topic)
            .difficultyLevel(difficulty)
            .articleLength(length)
            .originalText(content)
            .createdAt(LocalDateTime.now())
            .build();
            
        try {
            article.setUsedWordIds(objectMapper.writeValueAsString(wordIds));
        } catch (Exception e) {
            article.setUsedWordIds("[]");
        }
        
        article = generatedArticleRepository.save(article);
        
        // 4. 保存使用的单词记录
        for (VocabularyWord word : words) {
            ArticleUsedWord usedWord = ArticleUsedWord.builder()
                .articleId(article.getId())
                .wordId(word.getId())
                .wordText(word.getWord())
                .occurrenceCount(1) // 简化处理，默认为1，实际可以通过统计content中出现的次数来更新
                .build();
            articleUsedWordRepository.save(usedWord);
        }
        
        return article;
    }
    
    /**
     * 获取用户生成的文章列表
     */
    public List<GeneratedArticle> getUserGeneratedArticles(Long userId) {
        return generatedArticleRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 获取文章详情
     */
    public GeneratedArticle getGeneratedArticle(Integer articleId) {
        GeneratedArticle article = generatedArticleRepository.findById(articleId)
            .orElseThrow(() -> new CustomException("文章不存在"));
            
        // 填充使用的单词信息（如果需要详细信息）
        List<ArticleUsedWord> usedWords = articleUsedWordRepository.findByArticleId(articleId);
        article.setUsedWords(usedWords);
        
        return article;
    }
}
