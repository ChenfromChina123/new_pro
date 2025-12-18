package com.aispring.service.impl;

import com.aispring.service.AiChatService;
import com.aispring.repository.ChatRecordRepository;
import com.aispring.entity.ChatRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI聊天服务实现类
 * 对应Python: app.py中的AI聊天相关功能
 */
@Service
public class AiChatServiceImpl implements AiChatService {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectProvider<StreamingChatClient> streamingChatClientProvider;
    private final ChatRecordRepository chatRecordRepository;
    
    @Value("${ai.max-tokens:4096}")
    private Integer maxTokens;
    
    @Value("${ai.doubao.api-key:}")
    private String doubaoApiKey;
    
    @Value("${ai.doubao.api-url:}")
    private String doubaoApiUrl;
    
    private ChatClient doubaoChatClient;
    private StreamingChatClient doubaoStreamingChatClient;
    
    // 上下文最大消息数
    private static final int MAX_CONTEXT_MESSAGES = 10;
    
    public AiChatServiceImpl(ObjectProvider<ChatClient> chatClientProvider,
                             ObjectProvider<StreamingChatClient> streamingChatClientProvider,
                             ChatRecordRepository chatRecordRepository,
                             @Value("${ai.doubao.api-key:}") String doubaoApiKey,
                             @Value("${ai.doubao.api-url:}") String doubaoApiUrl) {
        this.chatClientProvider = chatClientProvider;
        this.streamingChatClientProvider = streamingChatClientProvider;
        this.chatRecordRepository = chatRecordRepository;
        this.doubaoApiKey = doubaoApiKey;
        this.doubaoApiUrl = doubaoApiUrl;

        if (doubaoApiKey != null && !doubaoApiKey.isEmpty() && doubaoApiUrl != null && !doubaoApiUrl.isEmpty()) {
            try {
                OpenAiApi doubaoApi = new OpenAiApi(doubaoApiUrl, doubaoApiKey);
                OpenAiChatClient client = new OpenAiChatClient(doubaoApi);
                this.doubaoChatClient = client;
                this.doubaoStreamingChatClient = client;
                System.out.println("Doubao AI client initialized successfully with URL: " + doubaoApiUrl);
            } catch (Exception e) {
                System.err.println("Failed to initialize Doubao AI client: " + e.getMessage());
            }
        }
    }


    

    
    @Override
    public SseEmitter askStream(String prompt, String sessionId, String model, String userId) {
        // 创建SSE发射器，设置超时时间为3分钟
        SseEmitter emitter = new SseEmitter(180_000L);
        
        // 确定使用的客户端和模型名称
        StreamingChatClient clientToUse = null;
        String actualModel = model;
        
        if ("doubao".equals(model)) {
            if (doubaoStreamingChatClient != null) {
                clientToUse = doubaoStreamingChatClient;
                // 豆包模型名称保持原样，或者如果需要特定Endpoint ID，需用户在前端或配置中指定
                // 这里假设"doubao"是有效的或者是经过代理映射的
            } else {
                // 如果豆包客户端未初始化，尝试使用默认客户端（DeepSeek）
                System.err.println("Doubao client not initialized, falling back to default.");
                clientToUse = streamingChatClientProvider.getIfAvailable();
            }
        } else {
            clientToUse = streamingChatClientProvider.getIfAvailable();
            if ("deepseek".equals(model) || model == null || model.isEmpty()) {
                actualModel = "deepseek-chat";
            }
        }
        
        if (actualModel == null || actualModel.isEmpty()) {
            actualModel = "deepseek-chat";
        }
        
        // 配置AI模型选项
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(actualModel)
                .withTemperature(0.7f)
                .withMaxTokens(maxTokens)
                .build();
        
        // 构建包含上下文的Prompt
        Prompt promptObj = buildPrompt(prompt, sessionId, userId, options);
        
        final StreamingChatClient finalClient = clientToUse;
        
        // 异步处理流式响应
        new Thread(() -> {
            try {
                if (finalClient == null) {
                    String content = fallbackAnswer(prompt);
                    String json = objectMapper.writeValueAsString(Map.of("content", content));
                    emitter.send(SseEmitter.event().data(json));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                    return;
                }
                finalClient.stream(promptObj)
                    .doOnNext(chatResponse -> {
                        try {
                            // 获取AI响应内容
                            String content = chatResponse.getResult().getOutput().getContent();
                            if (content != null && !content.isEmpty()) {
                                // 发送SSE事件
                                String json = objectMapper.writeValueAsString(Map.of("content", content));
                                emitter.send(SseEmitter.event().data(json));
                            }
                        } catch (Exception e) {
                            handleError(emitter, e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            // 发送结束信号
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            emitter.complete();
                        } catch (Exception e) {
                            handleError(emitter, e);
                        }
                    })
                    .doOnError(e -> handleError(emitter, e))
                    .subscribe();
            } catch (Exception e) {
                handleError(emitter, e);
            }
        }).start();
        
        return emitter;
    }

    private Prompt buildPrompt(String promptText, String sessionId, String userId, OpenAiChatOptions options) {
        List<Message> messages = new ArrayList<>();
        
        // 获取历史消息
        if (sessionId != null && !sessionId.isEmpty()) {
            List<ChatRecord> history = chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
            
            // 截取最近的N条消息
            int start = Math.max(0, history.size() - MAX_CONTEXT_MESSAGES);
            List<ChatRecord> recentHistory = history.subList(start, history.size());
            
            for (ChatRecord record : recentHistory) {
                if (record.getSenderType() == 1) { // User
                    messages.add(new UserMessage(record.getContent()));
                } else if (record.getSenderType() == 2) { // AI
                    messages.add(new AssistantMessage(record.getContent()));
                }
            }
        }
        
        // 添加当前用户消息
        messages.add(new UserMessage(promptText));
        
        return new Prompt(messages, options);
    }

    private void handleError(SseEmitter emitter, Throwable e) {
        // 记录错误日志
        System.err.println("AI Chat Error: " + e.getMessage());
        e.printStackTrace();
        
        try {
            String errorMsg = "AI服务暂时不可用: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
            String json = objectMapper.writeValueAsString(Map.of("content", errorMsg));
            emitter.send(SseEmitter.event().data(json));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception ex) {
            // 发送错误消息失败（可能是连接已断开），仅记录日志，避免触发"Cannot render error page"
            System.err.println("Failed to send error response to client: " + ex.getMessage());
            // 不再调用 completeWithError，防止二次报错
            // emitter.completeWithError(ex); 
        }
    }
    
    @Override
    public String ask(String prompt, String sessionId, String model, String userId) {
        try {
            ChatClient clientToUse = null;
            String actualModel = model;
            
            if ("doubao".equals(model)) {
                if (doubaoChatClient != null) {
                    clientToUse = doubaoChatClient;
                } else {
                    clientToUse = chatClientProvider.getIfAvailable();
                }
            } else {
                clientToUse = chatClientProvider.getIfAvailable();
                if ("deepseek".equals(model) || model == null || model.isEmpty()) {
                    actualModel = "deepseek-chat";
                }
            }
            
            if (clientToUse == null) {
                return fallbackAnswer(prompt);
            }
            
            if (actualModel == null || actualModel.isEmpty()) {
                actualModel = "deepseek-chat";
            }

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(actualModel)
                    .withTemperature(0.7f)
                    .withMaxTokens(maxTokens)
                    .build();
            
            Prompt promptObj = buildPrompt(prompt, sessionId, userId, options);
            
            final ChatClient finalClient = clientToUse;
            System.out.println("Sending request to AI. Model: " + actualModel + ", Prompt length: " + prompt.length());
            
            ChatResponse response = finalClient.call(promptObj);
            String content = response.getResult().getOutput().getContent();
            System.out.println("AI Response received. Length: " + (content != null ? content.length() : 0));
            return content;
        } catch (Exception e) {
            System.err.println("AI Chat Error in ask(): " + e.getMessage());
            e.printStackTrace();
            return fallbackAnswer(prompt);
        }
    }

    private String fallbackAnswer(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "";
        }
        String p = prompt.replace("？", "?").replace("等于多少", "").replace("是多少", "");
        String norm = p.replace("加", "+").replace("减", "-").replace("乘", "*").replace("除以", "/").replace("除", "/");
        try {
            String expr = norm.replaceAll("[^0-9+\\-*/.]", "");
            if (expr.matches("\\s*\\d+(?:\\.\\d+)?\\s*[+\\-*/]\\s*\\d+(?:\\.\\d+)?\\s*")) {
                String[] parts;
                char op;
                if (expr.contains("+")) { parts = expr.split("\\+", 2); op = '+'; }
                else if (expr.contains("-")) { parts = expr.split("-", 2); op = '-'; }
                else if (expr.contains("*")) { parts = expr.split("\\*", 2); op = '*'; }
                else { parts = expr.split("/", 2); op = '/'; }
                double a = Double.parseDouble(parts[0].trim());
                double b = Double.parseDouble(parts[1].trim());
                double r = switch (op) { case '+' -> a + b; case '-' -> a - b; case '*' -> a * b; default -> b == 0 ? Double.NaN : a / b; };
                String rr = (Math.floor(r) == r) ? String.valueOf((long) r) : String.valueOf(r);
                return rr;
            }
        } catch (Exception ignore) {}
        return "抱歉，AI服务暂不可用。";
    }
}
