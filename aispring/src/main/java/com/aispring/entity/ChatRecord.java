package com.aispring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天记录实体类
 * 对应Python: ChatRecord模型
 */
@Entity
@Table(name = "chat_records",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_session_id", columnList = "session_id"),
        @Index(name = "idx_user_session", columnList = "user_id, session_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRecord {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;
    
    @Column(name = "message_order", nullable = false)
    private Integer messageOrder;
    
    @Column(name = "sender_type", nullable = false)
    private Integer senderType;  // 1: user, 2: AI
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "reasoning_content", columnDefinition = "TEXT")
    private String reasoningContent;  // AI 深度思考内容
    
    @Column(name = "ai_model", length = 50)
    private String aiModel;  // deepseek, doubao, etc.
    
    @Column(name = "status", length = 20, nullable = false)
    private String status;  // pending, completed, failed, cancelled
    
    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime;
    
    /**
     * 转换为Map用于API响应
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("user_id", userId);
        map.put("session_id", sessionId);
        map.put("message_order", messageOrder);
        map.put("sender_type", senderType);
        map.put("content", content);
        map.put("reasoning_content", reasoningContent);
        map.put("ai_model", aiModel);
        map.put("status", status);
        map.put("send_time", sendTime != null ? sendTime.format(FORMATTER) : null);
        return map;
    }
}

