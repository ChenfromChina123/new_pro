package com.aispring.controller;

import com.aispring.entity.*;
import com.aispring.service.VocabularyService;
import com.aispring.dto.response.MessageResponse;
import com.aispring.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 词汇学习控制器
 * 对应Python: language_learning.py中的词汇相关端点
 */
@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {
    
    private final VocabularyService vocabularyService;
    
    // DTO类
    @Data
    public static class CreateVocabularyListRequest {
        @NotBlank(message = "名称不能为空")
        private String name;
        private String description;
        private String language = "en";
    }
    
    @Data
    public static class AddWordRequest {
        @NotBlank(message = "单词不能为空")
        private String word;
        private String definition;
        private String partOfSpeech;
        private String example;
        private String language = "en";
    }
    
    @Data
    public static class UpdateProgressRequest {
        @NotNull(message = "单词ID不能为空")
        private Integer wordId;
        private Integer masteryLevel;
        private Boolean isDifficult;
    }
    
    @Data
    public static class RecordActivityRequest {
        @NotBlank(message = "活动类型不能为空")
        private String activityType;
        private String activityDetails;
        private Integer duration;
    }
    
    /**
     * 创建单词表
     * Python: POST /api/vocabulary/lists
     */
    @PostMapping("/lists")
    public ResponseEntity<VocabularyList> createVocabularyList(
            @Valid @RequestBody CreateVocabularyListRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        VocabularyList list = vocabularyService.createVocabularyList(
            userId, request.getName(), request.getDescription(), request.getLanguage());
        
        return ResponseEntity.ok(list);
    }
    
    /**
     * 获取用户的单词表列表
     * Python: GET /api/vocabulary/lists
     */
    @GetMapping("/lists")
    public ResponseEntity<Map<String, Object>> getVocabularyLists(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        List<VocabularyList> lists = vocabularyService.getUserVocabularyLists(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("lists", lists);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 添加单词到单词表
     * Python: POST /api/vocabulary/lists/{listId}/words
     */
    @PostMapping("/lists/{listId}/words")
    public ResponseEntity<VocabularyWord> addWord(
            @PathVariable Integer listId,
            @Valid @RequestBody AddWordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        VocabularyWord word = vocabularyService.addWordToList(
            listId, request.getWord(), request.getDefinition(), 
            request.getPartOfSpeech(), request.getExample(), request.getLanguage());
        
        return ResponseEntity.ok(word);
    }
    
    /**
     * 获取单词表中的所有单词
     * Python: GET /api/vocabulary/lists/{listId}/words
     */
    @GetMapping("/lists/{listId}/words")
    public ResponseEntity<Map<String, Object>> getWords(
            @PathVariable Integer listId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        List<VocabularyWord> words = vocabularyService.getWordsInList(listId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("words", words);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除单词表
     * Python: DELETE /api/vocabulary/lists/{listId}
     */
    @DeleteMapping("/lists/{listId}")
    public ResponseEntity<MessageResponse> deleteList(
            @PathVariable Integer listId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        vocabularyService.deleteVocabularyList(listId);
        
        return ResponseEntity.ok(
            MessageResponse.builder()
                .message("单词表已删除")
                .build()
        );
    }
    
    /**
     * 删除单词
     * Python: DELETE /api/vocabulary/words/{wordId}
     */
    @DeleteMapping("/words/{wordId}")
    public ResponseEntity<MessageResponse> deleteWord(
            @PathVariable Integer wordId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        vocabularyService.deleteWord(wordId);
        
        return ResponseEntity.ok(
            MessageResponse.builder()
                .message("单词已删除")
                .build()
        );
    }
    
    /**
     * 更新单词学习进度
     * Python: POST /api/vocabulary/progress
     */
    @PostMapping("/progress")
    public ResponseEntity<UserWordProgress> updateProgress(
            @Valid @RequestBody UpdateProgressRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        UserWordProgress progress = vocabularyService.updateUserWordProgress(
            userId, request.getWordId(), request.getMasteryLevel(), request.getIsDifficult());
        
        return ResponseEntity.ok(progress);
    }
    
    /**
     * 获取需要复习的单词
     * Python: GET /api/vocabulary/review
     */
    @GetMapping("/review")
    public ResponseEntity<Map<String, Object>> getReviewWords(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        List<UserWordProgress> words = vocabularyService.getUserReviewWords(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("words", words);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取学习统计
     * Python: GET /api/vocabulary/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<VocabularyService.LearningStats> getStats(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        VocabularyService.LearningStats stats = vocabularyService.getUserLearningStats(userId);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 记录学习活动
     * Python: POST /api/vocabulary/activity
     */
    @PostMapping("/activity")
    public ResponseEntity<MessageResponse> recordActivity(
            @Valid @RequestBody RecordActivityRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        vocabularyService.recordLearningActivity(
            userId, request.getActivityType(), request.getActivityDetails(), request.getDuration());
        
        return ResponseEntity.ok(
            MessageResponse.builder()
                .message("学习活动已记录")
                .build()
        );
    }
    
    /**
     * 搜索公共词库
     * Python: GET /api/vocabulary/public/search
     */
    @GetMapping("/public/search")
    public ResponseEntity<Map<String, Object>> searchPublicWords(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "en") String language,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        String kw = (keyword != null && !keyword.isEmpty()) ? keyword : (q != null ? q : "");
        List<PublicVocabularyWord> words = vocabularyService.searchPublicWords(kw, language);
        
        Map<String, Object> response = new HashMap<>();
        response.put("words", words);
        
        return ResponseEntity.ok(response);
    }
}

