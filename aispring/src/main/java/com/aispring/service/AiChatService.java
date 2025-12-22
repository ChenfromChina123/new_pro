package com.aispring.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI聊天服务接口
 * 对应Python: app.py中的AI聊天相关功能
 */
public interface AiChatService {
    
    /**
     * AI流式问答
     * @param prompt 提示词
     * @param sessionId 会话ID
     * @param model 模型名称
     * @param userId 用户ID
     * @return SSE发射器，用于流式响应
     */
    SseEmitter askStream(String prompt, String sessionId, String model, String userId);
    
    /**
     * AI非流式问答
     * @param prompt 提示词
     * @param sessionId 会话ID
     * @param model 模型名称
     * @param userId 用户ID
     * @return 非流式响应
     */
    String ask(String prompt, String sessionId, String model, String userId);

    /**
     * AI Agent流式问答 (支持自定义System Prompt和任务链上下文)
     */
    SseEmitter askAgentStream(String prompt, String sessionId, String model, String userId, String systemPromptTemplate, java.util.List<java.util.Map<String, Object>> tasks, java.util.function.Consumer<String> onResponse);

}
