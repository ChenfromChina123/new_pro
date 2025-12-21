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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            "我有以下 %s 单词列表：[%s]。请建议 6 个简短的中文文章标题，要求这些标题能涵盖这些单词。 " +
            "每个标题不得超过 8 个汉字。 " +
            "仅返回一个 JSON 字符串数组，例如：[\"主题1\", \"主题2\"]。不要包含任何 Markdown 格式或代码块标记。",
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
        
        String finalTopic = normalizeTopic(topic);
        if (finalTopic == null) {
            finalTopic = generateAutoTopic(words, userId);
        }
            
        // 2. 调用AI生成文章
        int targetWords = lengthToTargetWords(length);
        String prompt = String.format(
            "请写一篇关于“%s”的英文文章（约 %d 词，难度： %s）。 " +
            "文章中必须包含以下单词：[%s]。 " +
            "请用双星号（如 **word**）包裹这些使用的单词，以便突出显示。 " +
            "仅输出文章正文内容，分 2-4 个段落。不要包含“Title:”字样，也不要包含 Markdown 代码块标记。",
            finalTopic, targetWords, difficulty, vocabularyList
        );
        
        String content = aiChatService.ask(prompt, null, "deepseek-chat", String.valueOf(userId));
        
        // Clean content
        if (content != null) {
            content = content.replaceAll("```markdown", "").replaceAll("```", "").trim();
            if (content.startsWith("Title:")) {
                content = content.substring(content.indexOf("\n") + 1).trim();
            }
        }

        String translated = translateArticleToChinese(content, userId);
        
        // 3. 保存文章
        GeneratedArticle article = GeneratedArticle.builder()
            .userId(userId)
            .vocabularyListId(listId)
            .topic(finalTopic)
            .difficultyLevel(difficulty)
            .articleLength(length)
            .originalText(content)
            .translatedText(translated)
            .createdAt(LocalDateTime.now())
            .build();
            
        try {
            article.setUsedWordIds(objectMapper.writeValueAsString(wordIds));
        } catch (Exception e) {
            article.setUsedWordIds("[]");
        }
        
        article = generatedArticleRepository.save(article);
        
        // 4. 保存使用的单词记录
        List<ArticleUsedWord> usedWords = new ArrayList<>();
        for (VocabularyWord word : words) {
            int occurrences = countOccurrencesIgnoreCase(content, word.getWord());
            ArticleUsedWord usedWord = ArticleUsedWord.builder()
                .articleId(article.getId())
                .wordId(word.getId())
                .wordText(word.getWord())
                .occurrenceCount(occurrences)
                .word(word)
                .build();
            usedWords.add(articleUsedWordRepository.save(usedWord));
        }
        article.setUsedWords(usedWords);
        
        return article;
    }

    private int countOccurrencesIgnoreCase(String text, String word) {
        if (text == null || text.isBlank() || word == null || word.isBlank()) return 0;
        String w = word.trim();
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(w) + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count += 1;
        }
        return count;
    }

    /**
     * 将文章长度枚举映射为目标词数
     */
    private int lengthToTargetWords(String length) {
        if (length == null) return 400;
        return switch (length.trim()) {
            case "Short" -> 200;
            case "Long" -> 700;
            default -> 400;
        };
    }

    /**
     * 规范化主题：空字符串视为无主题
     */
    private String normalizeTopic(String topic) {
        if (topic == null) return null;
        String t = topic.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 在用户未填写主题时自动生成一个中文主题
     */
    private String generateAutoTopic(List<VocabularyWord> words, Long userId) {
        String wordsStr = words.stream()
            .map(VocabularyWord::getWord)
            .limit(20)
            .collect(Collectors.joining(", "));
        String prompt = String.format(
            "根据这些英文单词：[%s]，提供一个简短的中文文章标题（不超过 8 个汉字）。 " +
            "仅返回标题文本。不要添加引号或任何 Markdown 格式。",
            wordsStr
        );
        String response = aiChatService.ask(prompt, null, "deepseek-chat", String.valueOf(userId));
        if (response == null) return "学习文章";
        String cleaned = response.replaceAll("```", "").trim();
        cleaned = cleaned.replaceAll("^\"|\"$", "");
        return cleaned.isBlank() ? "学习文章" : cleaned;
    }

    /**
     * 将英文文章翻译为中文，并尽量保持段落结构一致
     */
    private String translateArticleToChinese(String content, Long userId) {
        if (content == null || content.isBlank()) return null;
        String prompt = "将以下英文文章翻译成中文。 " +
            "保持段落数量和空行与原文完全一致。 " +
            "不要包含任何 Markdown 格式。不要包含 ** 标记，保持单词为普通文本。 " +
            "仅返回中文翻译文本。\n\n" +
            content;
        String response = aiChatService.ask(prompt, null, "deepseek-chat", String.valueOf(userId));
        if (response == null) return null;
        String cleaned = response.replaceAll("```markdown", "").replaceAll("```", "").trim();
        cleaned = cleaned.replace("**", "");
        return cleaned.isBlank() ? null : cleaned;
    }
    
    /**
     * 获取用户生成的文章列表
     */
    @Transactional(readOnly = true)
    public List<GeneratedArticle> getUserGeneratedArticles(Long userId) {
        return generatedArticleRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 获取文章详情
     */
    @Transactional(readOnly = true)
    public GeneratedArticle getGeneratedArticle(Integer articleId) {
        GeneratedArticle article = generatedArticleRepository.findById(articleId)
            .orElseThrow(() -> new CustomException("文章不存在"));
            
        // 填充使用的单词信息（如果需要详细信息）
        List<ArticleUsedWord> usedWords = articleUsedWordRepository.findByArticleIdWithWord(articleId);
        article.setUsedWords(usedWords);
        
        return article;
    }

    public byte[] renderPdfFromHtml(String html) {
        if (html == null || html.isBlank()) {
            throw new CustomException("PDF内容为空");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Class<?> builderClass = Class.forName("com.openhtmltopdf.pdfboxout.PdfRendererBuilder");
            Object builder = builderClass.getDeclaredConstructor().newInstance();

            invokeMethod(builder, builderClass, "useFastMode");
            invokeMethod(builder, builderClass, "withHtmlContent", String.class, String.class, html, null);
            registerPdfFonts(builder, builderClass);
            invokeMethod(builder, builderClass, "toStream", OutputStream.class, outputStream);
            invokeMethod(builder, builderClass, "run");

            return outputStream.toByteArray();
        } catch (ClassNotFoundException e) {
            throw new CustomException("生成PDF失败: 缺少PDF渲染组件(openhtmltopdf-pdfbox)，请联系管理员处理");
        } catch (Exception e) {
            throw new CustomException("生成PDF失败: " + e.getMessage());
        }
    }

    private void registerPdfFonts(Object builder, Class<?> builderClass) {
        // 优先使用 SimHei (黑体)，因为它是标准的 ttf 文件，兼容性最好
        tryUseFont(builder, builderClass, "C:/Windows/Fonts/simhei.ttf", "SimHei");
        // 备选方案
        tryUseFont(builder, builderClass, "C:/Windows/Fonts/msyh.ttc", "Microsoft YaHei");
        tryUseFont(builder, builderClass, "C:/Windows/Fonts/simsun.ttc", "SimSun");
        tryUseFont(builder, builderClass, "C:/Windows/Fonts/arialuni.ttf", "Arial Unicode MS");
    }

    private void tryUseFont(Object builder, Class<?> builderClass, String fontPath, String fontFamily) {
        try {
            File fontFile = new File(fontPath);
            if (!fontFile.exists() || !fontFile.isFile()) {
                System.out.println("PDF Font not found: " + fontPath);
                return;
            }
            // openhtmltopdf 对 ttc 的支持可能有限，如果是 ttc，尝试直接加载
            invokeMethod(builder, builderClass, "useFont", File.class, String.class, fontFile, fontFamily);
            System.out.println("Successfully registered PDF font: " + fontFamily + " from " + fontPath);
        } catch (Exception e) {
            System.err.println("Failed to register PDF font " + fontFamily + ": " + e.getMessage());
        }
    }

    private void invokeMethod(Object target, Class<?> targetClass, String methodName, Class<?> p1, Object a1) throws Exception {
        Method m = targetClass.getMethod(methodName, p1);
        m.invoke(target, a1);
    }

    private void invokeMethod(Object target, Class<?> targetClass, String methodName, Class<?> p1, Class<?> p2, Object a1, Object a2) throws Exception {
        Method m = targetClass.getMethod(methodName, p1, p2);
        m.invoke(target, a1, a2);
    }

    private void invokeMethod(Object target, Class<?> targetClass, String methodName) throws Exception {
        Method m = targetClass.getMethod(methodName);
        m.invoke(target);
    }
}
