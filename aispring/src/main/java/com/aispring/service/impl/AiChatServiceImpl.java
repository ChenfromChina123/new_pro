package com.aispring.service.impl;

import com.aispring.service.AiChatService;
import com.aispring.repository.ChatRecordRepository;
import com.aispring.entity.ChatRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    
    @Value("${ai.doubao.model:}")
    private String doubaoModel;
    
    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;
    
    @Value("${ai.deepseek.api-url:}")
    private String deepseekApiUrl;
    
    private ChatClient doubaoChatClient;
    private StreamingChatClient doubaoStreamingChatClient;
    
    private ChatClient deepseekChatClient;
    private StreamingChatClient deepseekStreamingChatClient;
    
    // 上下文最大消息数
    private static final int MAX_CONTEXT_MESSAGES = 10;
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int DOUBAO_MAX_RETRIES = 2;
    
    private final OkHttpClient doubaoHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build();
    
    private final OkHttpClient doubaoStreamingHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    public AiChatServiceImpl(ObjectProvider<ChatClient> chatClientProvider,
                             ObjectProvider<StreamingChatClient> streamingChatClientProvider,
                             ChatRecordRepository chatRecordRepository,
                             @Value("${ai.doubao.api-key:}") String doubaoApiKey,
                             @Value("${ai.doubao.api-url:}") String doubaoApiUrl,
                             @Value("${ai.doubao.model:}") String doubaoModel,
                             @Value("${ai.deepseek.api-key:}") String deepseekApiKey,
                             @Value("${ai.deepseek.api-url:}") String deepseekApiUrl) {
        this.chatClientProvider = chatClientProvider;
        this.streamingChatClientProvider = streamingChatClientProvider;
        this.chatRecordRepository = chatRecordRepository;
        this.doubaoApiKey = doubaoApiKey;
        this.doubaoApiUrl = doubaoApiUrl;
        this.doubaoModel = doubaoModel;
        this.deepseekApiKey = deepseekApiKey;
        this.deepseekApiUrl = deepseekApiUrl;

        // Initialize Doubao Client
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
        
        // Initialize DeepSeek Client
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty() && deepseekApiUrl != null && !deepseekApiUrl.isEmpty()) {
            try {
                OpenAiApi deepseekApi = new OpenAiApi(deepseekApiUrl, deepseekApiKey);
                OpenAiChatClient client = new OpenAiChatClient(deepseekApi);
                this.deepseekChatClient = client;
                this.deepseekStreamingChatClient = client;
                System.out.println("DeepSeek AI client initialized successfully with URL: " + deepseekApiUrl);
            } catch (Exception e) {
                System.err.println("Failed to initialize DeepSeek AI client: " + e.getMessage());
            }
        }
    }


    

    
    @Override
    public SseEmitter askStream(String prompt, String sessionId, String model, String userId) {
        // 创建SSE发射器，设置超时时间为3分钟
        SseEmitter emitter = new SseEmitter(180_000L);
        
        if ("doubao".equals(model)) {
            new Thread(() -> {
                try {
                    streamDoubaoWithRetry(emitter, prompt, sessionId, userId);
                } catch (Exception e) {
                    handleError(emitter, e);
                }
            }).start();
            return emitter;
        }
        
        // 确定使用的客户端和模型名称
        StreamingChatClient clientToUse = null;
        String actualModel = model;
        
        if ("deepseek".equals(model) || "deepseek-chat".equals(model)) {
            if (deepseekStreamingChatClient != null) {
                clientToUse = deepseekStreamingChatClient;
            } else {
                clientToUse = streamingChatClientProvider.getIfAvailable();
            }
            actualModel = "deepseek-chat";
        } else {
            clientToUse = streamingChatClientProvider.getIfAvailable();
            if (model == null || model.isEmpty()) {
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

    /**
     * 构建包含历史上下文的 Prompt，用于 Spring AI 的 ChatClient 调用。
     */
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

    /**
     * 统一处理流式接口异常：记录日志并向前端发送可展示的错误消息，然后结束流。
     */
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
                return callDoubaoWithRetry(prompt, sessionId, userId);
            } else if ("deepseek".equals(model) || "deepseek-chat".equals(model)) {
                if (deepseekChatClient != null) {
                    clientToUse = deepseekChatClient;
                } else {
                    clientToUse = chatClientProvider.getIfAvailable();
                }
                actualModel = "deepseek-chat";
            } else {
                clientToUse = chatClientProvider.getIfAvailable();
                if (model == null || model.isEmpty()) {
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
            logger.info("Sending request to AI. Model: {}, Prompt length: {}", actualModel, prompt.length());
            
            ChatResponse response = finalClient.call(promptObj);
            String content = response.getResult().getOutput().getContent();
            logger.info("AI Response received. Length: {}", (content != null ? content.length() : 0));
            return content;
        } catch (Exception e) {
            logger.error("AI Chat Error in ask(): ", e);
            return fallbackAnswer(prompt);
        }
    }

    /**
     * 当外部AI不可用时的兜底回复（避免前端空白并保证流程可结束）。
     */
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
    
    /**
     * 将数据库中的对话历史与当前问题合并为 OpenAI 兼容的 messages 数组。
     */
    private List<Map<String, String>> buildOpenAiMessages(String promptText, String sessionId, String userId) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        if (sessionId != null && !sessionId.isEmpty()) {
            List<ChatRecord> history = chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
            int start = Math.max(0, history.size() - MAX_CONTEXT_MESSAGES);
            List<ChatRecord> recentHistory = history.subList(start, history.size());
            
            for (ChatRecord record : recentHistory) {
                if (record.getSenderType() == 1) {
                    messages.add(Map.of("role", "user", "content", record.getContent()));
                } else if (record.getSenderType() == 2) {
                    messages.add(Map.of("role", "assistant", "content", record.getContent()));
                }
            }
        }
        
        messages.add(Map.of("role", "user", "content", promptText));
        return messages;
    }
    
    /**
     * 生成豆包 OpenAI 兼容接口的 `chat/completions` 目标 URL。
     * 支持 baseUrl 既可能是根域名（自动补 `/v1`），也可能已经包含 `/v1` 或 `/api/v3`。
     */
    private String buildDoubaoChatCompletionsUrl() {
        String base = doubaoApiUrl == null ? "" : doubaoApiUrl.trim();
        if (base.isEmpty()) {
            throw new IllegalStateException("豆包API地址未配置（DOUBAO_BASEURL）");
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.contains("/api/v3") || base.endsWith("/v1")) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }
    
    /**
     * 校验豆包配置，并返回实际调用的模型名称（通常为 Endpoint ID）。
     */
    private String resolveDoubaoActualModel() {
        if (doubaoApiKey == null || doubaoApiKey.trim().isEmpty()) {
            throw new IllegalStateException("豆包API密钥未配置（DOUBAO_API_KEY）");
        }
        String m = doubaoModel == null ? "" : doubaoModel.trim();
        if (m.isEmpty()) {
            throw new IllegalStateException("豆包模型未配置（DOUBAO_MODEL，通常为 Endpoint ID）");
        }
        return m;
    }
    
    /**
     * 以流式方式调用豆包 OpenAI 兼容接口，并将增量内容转发为 SSE chunk。
     */
    private void streamDoubaoWithRetry(SseEmitter emitter, String prompt, String sessionId, String userId) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= DOUBAO_MAX_RETRIES; attempt++) {
            try {
                streamDoubaoOnce(emitter, prompt, sessionId, userId);
                return;
            } catch (Exception e) {
                last = e;
                boolean canRetry = (e instanceof IOException) || (e.getCause() instanceof IOException);
                if (attempt < DOUBAO_MAX_RETRIES && canRetry) {
                    Thread.sleep(300L * attempt);
                    continue;
                }
                throw e;
            }
        }
        if (last != null) {
            throw last;
        }
    }
    
    /**
     * 执行一次豆包流式请求。
     */
    private void streamDoubaoOnce(SseEmitter emitter, String prompt, String sessionId, String userId) throws Exception {
        String url = buildDoubaoChatCompletionsUrl();
        String actualModel = resolveDoubaoActualModel();
        
        List<Map<String, String>> messages = buildOpenAiMessages(prompt, sessionId, userId);
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "model", actualModel,
                "messages", messages,
                "stream", true,
                "temperature", 0.7,
                "max_tokens", maxTokens
        ));
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + doubaoApiKey.trim())
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(requestJson, JSON))
                .build();
        
        long start = System.currentTimeMillis();
        try (Response response = doubaoStreamingHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IllegalStateException("豆包请求失败，HTTP " + response.code() + (body.isEmpty() ? "" : (": " + body)));
            }
            
            if (response.body() == null) {
                throw new IllegalStateException("豆包响应为空");
            }
            
            BufferedSource source = response.body().source();
            String line;
            while ((line = source.readUtf8Line()) != null) {
                if (line.isBlank()) continue;
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                String chunk = extractStreamDeltaContent(data);
                if (chunk != null && !chunk.isEmpty()) {
                    String json = objectMapper.writeValueAsString(Map.of("content", chunk));
                    emitter.send(SseEmitter.event().data(json));
                }
            }
            
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } finally {
            long cost = System.currentTimeMillis() - start;
            logger.info("Doubao stream finished. costMs={}, sessionId={}, userId={}", cost, sessionId, userId);
        }
    }
    
    /**
     * 解析 OpenAI 流式返回的单条 data JSON，提取增量文本内容。
     */
    private String extractStreamDeltaContent(String dataJson) throws Exception {
        JsonNode root = objectMapper.readTree(dataJson);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode delta = choices.get(0).path("delta");
        if (delta.isMissingNode()) {
            return null;
        }
        JsonNode content = delta.get("content");
        return content != null && !content.isNull() ? content.asText() : null;
    }
    
    /**
     * 以非流式方式调用豆包接口（带一次重试），返回完整文本内容。
     */
    private String callDoubaoWithRetry(String prompt, String sessionId, String userId) {
        Exception last = null;
        for (int attempt = 1; attempt <= DOUBAO_MAX_RETRIES; attempt++) {
            try {
                return callDoubaoOnce(prompt, sessionId, userId);
            } catch (Exception e) {
                last = e;
                boolean canRetry = (e instanceof IOException) || (e.getCause() instanceof IOException);
                if (attempt < DOUBAO_MAX_RETRIES && canRetry) {
                    try {
                        Thread.sleep(300L * attempt);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                break;
            }
        }
        System.err.println("Doubao call failed: " + (last != null ? last.getMessage() : "unknown"));
        if (last != null) {
            last.printStackTrace();
        }
        return fallbackAnswer(prompt);
    }
    
    /**
     * 执行一次豆包非流式请求。
     */
    private String callDoubaoOnce(String prompt, String sessionId, String userId) throws Exception {
        String url = buildDoubaoChatCompletionsUrl();
        String actualModel = resolveDoubaoActualModel();
        
        List<Map<String, String>> messages = buildOpenAiMessages(prompt, sessionId, userId);
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "model", actualModel,
                "messages", messages,
                "stream", false,
                "temperature", 0.7,
                "max_tokens", maxTokens
        ));
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + doubaoApiKey.trim())
                .post(RequestBody.create(requestJson, JSON))
                .build();
        
        long start = System.currentTimeMillis();
        try (Response response = doubaoHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IllegalStateException("豆包请求失败，HTTP " + response.code() + (body.isEmpty() ? "" : (": " + body)));
            }
            if (response.body() == null) {
                throw new IllegalStateException("豆包响应为空");
            }
            String body = response.body().string();
            return extractNonStreamContent(body);
        } finally {
            long cost = System.currentTimeMillis() - start;
            logger.info("Doubao call finished. costMs={}, sessionId={}, userId={}", cost, sessionId, userId);
        }
    }
    
    /**
     * 解析非流式响应 JSON，提取 choices[0].message.content。
     */
    private String extractNonStreamContent(String bodyJson) throws Exception {
        JsonNode root = objectMapper.readTree(bodyJson);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode msg = choices.get(0).path("message");
        JsonNode content = msg.get("content");
        return content != null && !content.isNull() ? content.asText() : "";
    }
}
