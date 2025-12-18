package com.aispring.service;

import com.aispring.entity.ChatRecord;
import com.aispring.entity.User;
import com.aispring.repository.ChatRecordRepository;
import com.aispring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天记录服务
 * 对应Python: chat_records.py
 */
@Service
@RequiredArgsConstructor
public class ChatRecordService {
    
    private final ChatRecordRepository chatRecordRepository;
    private final UserRepository userRepository;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 创建聊天记录
     */
    @Transactional
    public ChatRecord createChatRecord(String content, Integer senderType, String userId, 
                                      String sessionId, String aiModel, String status) {
        // 如果没有会话ID，生成新的
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        
        // 获取该会话中最新的消息顺序号
        Integer lastMessageOrder = chatRecordRepository
            .findMaxMessageOrderBySessionIdAndUserId(sessionId, userId);
        int messageOrder = (lastMessageOrder == null ? 0 : lastMessageOrder) + 1;
        
        // 创建聊天记录
        ChatRecord chatRecord = ChatRecord.builder()
            .sessionId(sessionId)
            .userId(userId)
            .messageOrder(messageOrder)
            .senderType(senderType)
            .content(content)
            .aiModel(aiModel)
            .status(status != null ? status : "completed")
            .sendTime(LocalDateTime.now())
            .build();
        
        return chatRecordRepository.save(chatRecord);
    }
    
    /**
     * 获取用户的所有会话列表
     */
    public List<Map<String, Object>> getUserSessions(String userId) {
        List<Object[]> results = chatRecordRepository.findSessionInfoByUserId(userId);
        
        return results.stream().map(row -> {
            Map<String, Object> session = new HashMap<>();
            session.put("session_id", row[0]);
            Object t1 = row[1];
            String tm1 = "";
            if (t1 != null) {
                LocalDateTime dt1 = (t1 instanceof LocalDateTime) ? (LocalDateTime) t1
                        : (t1 instanceof java.sql.Timestamp) ? ((java.sql.Timestamp) t1).toLocalDateTime() : null;
                tm1 = dt1 != null ? dt1.format(FORMATTER) : "";
            }
            session.put("last_message_time", tm1);
            
            // 限制会话标题长度为50个字符
            String lastMessage = row[2] != null ? row[2].toString() : "";
            if (lastMessage.length() > 50) {
                lastMessage = lastMessage.substring(0, 50) + "...";
            }
            session.put("last_message", lastMessage);
            
            return session;
        }).collect(Collectors.toList());
    }
    
    /**
     * 获取特定会话的所有消息
     */
    public List<ChatRecord> getSessionMessages(String userId, String sessionId) {
        return chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
    }
    
    /**
     * 删除特定会话
     */
    @Transactional
    public void deleteSession(String userId, String sessionId) {
        chatRecordRepository.deleteByUserIdAndSessionId(userId, sessionId);
    }
    
    /**
     * 创建新会话
     */
    public String createNewSession() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 管理员：获取所有用户的会话列表
     */
    public List<Map<String, Object>> getAllSessions(Integer skip, Integer limit, String userIdFilter) {
        List<Object[]> results;
        
        if (userIdFilter != null && !userIdFilter.isEmpty()) {
            results = chatRecordRepository.findSessionsInfoByUserId(userIdFilter);
        } else {
            results = chatRecordRepository.findAllSessionsInfo();
        }
        
        // 应用分页
        int start = skip != null ? skip : 0;
        int end = limit != null ? Math.min(start + limit, results.size()) : results.size();
        
        return results.subList(start, Math.min(end, results.size())).stream().map(row -> {
            Map<String, Object> session = new HashMap<>();
            session.put("session_id", row[0]);
            session.put("user_id", row[1]);
            Object t2 = row[2];
            String tm2 = "";
            if (t2 != null) {
                LocalDateTime dt2 = (t2 instanceof LocalDateTime) ? (LocalDateTime) t2
                        : (t2 instanceof java.sql.Timestamp) ? ((java.sql.Timestamp) t2).toLocalDateTime() : null;
                tm2 = dt2 != null ? dt2.format(FORMATTER) : "";
            }
            session.put("last_message_time", tm2);
            session.put("message_count", row[3]);
            
            // 限制会话标题长度为50个字符
            String lastMessage = row[4] != null ? row[4].toString() : "";
            if (lastMessage.length() > 50) {
                lastMessage = lastMessage.substring(0, 50) + "...";
            }
            session.put("last_message", lastMessage);
            
            // 获取用户名
            String userId = (String) row[1];
            if (userId != null) {
                userRepository.findById(Long.parseLong(userId))
                    .ifPresent(user -> session.put("username", user.getUsername()));
            }
            
            return session;
        }).collect(Collectors.toList());
    }
    
    /**
     * 管理员：获取指定用户特定会话的消息
     */
    public List<Map<String, Object>> getUserSessionMessages(String userId, String sessionId) {
        List<ChatRecord> messages = chatRecordRepository
            .findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
        
        // 获取用户名
        String username = null;
        Optional<User> user = userRepository.findById(Long.parseLong(userId));
        if (user.isPresent()) {
            username = user.get().getUsername();
        }
        
        final String finalUsername = username;
        return messages.stream().map(msg -> {
            Map<String, Object> result = msg.toMap();
            result.put("username", finalUsername);
            return result;
        }).collect(Collectors.toList());
    }
    
    /**
     * 管理员：删除指定用户的会话
     */
    @Transactional
    public int deleteUserSession(String userId, String sessionId) {
        List<ChatRecord> records = chatRecordRepository
            .findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
        int count = records.size();
        chatRecordRepository.deleteByUserIdAndSessionId(userId, sessionId);
        return count;
    }
    
    /**
     * 管理员：获取聊天统计信息
     */
    public Map<String, Object> getChatStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_messages", chatRecordRepository.countTotalMessages());
        stats.put("total_sessions", chatRecordRepository.countTotalSessions());
        stats.put("active_users", chatRecordRepository.countActiveUsers());
        stats.put("today_messages", chatRecordRepository.countTodayMessages());
        return stats;
    }
}

