package com.aispring.service.impl;

import com.aispring.service.AiChatService;
import com.aispring.repository.ChatRecordRepository;
import com.aispring.repository.AnonymousChatRecordRepository;
import com.aispring.entity.ChatRecord;
import com.aispring.entity.AnonymousChatRecord;
import com.aispring.entity.ChatSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import jakarta.annotation.PreDestroy;

/**
 * AI聊天服务实现类
 * 对应Python: app.py中的AI聊天相关功能
 */
@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectProvider<StreamingChatClient> streamingChatClientProvider;
    private final ChatRecordRepository chatRecordRepository;
    private final AnonymousChatRecordRepository anonymousChatRecordRepository;
    private final com.aispring.service.ChatRecordService chatRecordService; // 注入 ChatRecordService
    private final OkHttpClient okHttpClient;
    
    @Value("${ai.max-tokens:4096}")
    private Integer maxTokens;
    
    @Value("${ai.doubao.api-key:}")
    private String doubaoApiKey;
    
    @Value("${ai.doubao.api-url:}")
    private String doubaoApiUrl;
    
    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;
    
    @Value("${ai.deepseek.api-url:}")
    private String deepseekApiUrl;

    @Value("${ai.context.max-history-messages:30}")
    private Integer maxHistoryMessages;

    @Value("${ai.context.max-history-chars:20000}")
    private Integer maxHistoryChars;

    @Value("${ai.context.max-tool-result-chars:8000}")
    private Integer maxToolResultChars;

    @Value("${ai.context.max-saved-chars:200000}")
    private Integer maxSavedChars;

    @Value("${ai.context.max-saved-reasoning-chars:200000}")
    private Integer maxSavedReasoningChars;
    
    private ChatClient doubaoChatClient;
    private StreamingChatClient doubaoStreamingChatClient;
    
    private ChatClient deepseekChatClient;
    private StreamingChatClient deepseekStreamingChatClient;

    private static final AtomicInteger CHAT_THREAD_SEQ = new AtomicInteger(1);
    private static final AtomicInteger BG_THREAD_SEQ = new AtomicInteger(1);

    private final ExecutorService chatExecutor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r);
        t.setName("ai-chat-" + CHAT_THREAD_SEQ.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r);
        t.setName("ai-bg-" + BG_THREAD_SEQ.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdownExecutors() {
        chatExecutor.shutdownNow();
        backgroundExecutor.shutdownNow();
    }

    private String safePreview(String s, int maxChars) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars);
    }

    private void appendWithLimit(StringBuilder sb, String part, int maxChars) {
        if (part == null || part.isEmpty()) return;
        int remain = maxChars - sb.length();
        if (remain <= 0) return;
        if (part.length() <= remain) sb.append(part);
        else sb.append(part, 0, remain);
    }

    private String truncateToMax(String s, int maxChars) {
        if (s == null) return null;
        if (maxChars <= 0) return "";
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars);
    }
    
    public AiChatServiceImpl(ObjectProvider<ChatClient> chatClientProvider,
                             ObjectProvider<StreamingChatClient> streamingChatClientProvider,
                             ChatRecordRepository chatRecordRepository,
                             AnonymousChatRecordRepository anonymousChatRecordRepository,
                             com.aispring.service.ChatRecordService chatRecordService, // 添加到构造函数
                             @Value("${ai.doubao.api-key:}") String doubaoApiKey,
                             @Value("${ai.doubao.api-url:}") String doubaoApiUrl,
                             @Value("${ai.deepseek.api-key:}") String deepseekApiKey,
                             @Value("${ai.deepseek.api-url:}") String deepseekApiUrl) {
        this.chatClientProvider = chatClientProvider;
        this.streamingChatClientProvider = streamingChatClientProvider;
        this.chatRecordRepository = chatRecordRepository;
        this.anonymousChatRecordRepository = anonymousChatRecordRepository;
        this.chatRecordService = chatRecordService; // 初始化
        
        this.doubaoApiKey = doubaoApiKey;
        this.doubaoApiUrl = doubaoApiUrl;
        this.deepseekApiKey = deepseekApiKey;
        this.deepseekApiUrl = deepseekApiUrl;
        
        // Initialize OkHttpClient with custom timeouts and unsafe SSL
        this.okHttpClient = createUnsafeOkHttpClient();

        log.info("Initializing Doubao AI client");
        
        if (doubaoApiKey != null && !doubaoApiKey.isEmpty() && doubaoApiUrl != null && !doubaoApiUrl.isEmpty()) {
            try {
                // Doubao requires /api/v3 appended to base URL for OpenAiApi
                String doubaoBaseUrl = doubaoApiUrl;
                if (!doubaoBaseUrl.endsWith("/api/v3") && !doubaoBaseUrl.endsWith("/api/v3/")) {
                    if (doubaoBaseUrl.endsWith("/")) {
                        doubaoBaseUrl += "api/v3";
                    } else {
                        doubaoBaseUrl += "/api/v3";
                    }
                }
                OpenAiApi doubaoApi = new OpenAiApi(doubaoBaseUrl, doubaoApiKey);
                
                OpenAiChatOptions doubaoOptions = OpenAiChatOptions.builder()
                        .withModel("doubao-pro-32k") // 设置豆包模型名称
                        .withTemperature(0.7f)
                        .withMaxTokens(maxTokens)
                        .build();
                
                OpenAiChatClient client = new OpenAiChatClient(doubaoApi, doubaoOptions);
                this.doubaoChatClient = client;
                this.doubaoStreamingChatClient = client;
                log.info("Doubao AI client initialized successfully");
            } catch (Exception e) {
                log.warn("Failed to initialize Doubao AI client: {}", e.getMessage());
            }
        } else {
            log.info("Doubao client initialization skipped (missing api-key or api-url)");
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


    
    /**
     * 统一发送聊天响应（SSE）- 优化流畅度
     */
    private void sendChatResponse(SseEmitter emitter, String content, String reasoningContent) {
        try {
            Map<String, String> resultMap = new HashMap<>();
            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                resultMap.put("reasoning_content", reasoningContent);
            }
            if (content != null && !content.isEmpty()) {
                resultMap.put("content", content);
            }
            if (!resultMap.isEmpty()) {
                String json = objectMapper.writeValueAsString(resultMap);
                // 立即发送，不缓冲
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .data(json)
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("message");
                emitter.send(event);
            }
        } catch (Exception e) {
            handleError(emitter, e);
            // Stop generation if error occurred
            throw new RuntimeException("Stop chat generation", e);
        }
    }

    @Override
    public SseEmitter askStream(String prompt, String sessionId, String model, Long userId, String ipAddress) {
        return askStreamInternal(prompt, sessionId, model, userId, ipAddress);
    }

    @Override
    public SseEmitter askStream(String prompt, String sessionId, String model, Long userId) {
        return askStreamInternal(prompt, sessionId, model, userId, null);
    }

    /**
     * 普通流式问答核心实现
     */
    private SseEmitter askStreamInternal(String initialPrompt, String sessionId, String model, Long userId, String ipAddress) {
        // 创建SSE发射器，设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300_000L);
        
        log.info("=== askStreamInternal Called ===");
        log.info("Model: {}, SessionId: {}, UserId: {}, IP: {}", model, sessionId, userId, ipAddress);
        log.info("Prompt: {} chars, preview={}", initialPrompt == null ? 0 : initialPrompt.length(), safePreview(initialPrompt, 200));
        
        // 提前生成会话ID（针对匿名用户）
        final String finalSessionId = (sessionId == null || sessionId.isEmpty())
                ? java.util.UUID.randomUUID().toString().replace("-", "")
                : sessionId;

        chatExecutor.execute(() -> {
            try {
                log.info("=== Chat Thread Started ===");
                
                StringBuilder fullReasoning = new StringBuilder();
                // 执行对话并获取完整回复（内部已处理 SSE 发送）
                String fullContent = performBlockingChat(initialPrompt, finalSessionId, model, userId, null, emitter, ipAddress, fullReasoning);
                
                // 异步保存聊天记录
                if (userId != null) {
                    // 已登录用户逻辑保持不变
                } else {
                    // 匿名用户，保存到 anonymous_chat_records
                    String finalIp = (ipAddress == null || ipAddress.isEmpty()) ? "unknown" : ipAddress;

                    // 保存用户消息
                    AnonymousChatRecord userRecord = AnonymousChatRecord.builder()
                        .sessionId(finalSessionId)
                        .ipAddress(finalIp)
                        .role("user")
                        .content(initialPrompt)
                        .model(model)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                    anonymousChatRecordRepository.save(userRecord);
                    
                    // 保存AI消息
                    AnonymousChatRecord aiRecord = AnonymousChatRecord.builder()
                        .sessionId(finalSessionId)
                        .ipAddress(finalIp)
                        .role("assistant")
                        .content(truncateToMax(fullContent, maxSavedChars))
                        .reasoningContent(truncateToMax(fullReasoning.toString(), maxSavedReasoningChars))
                        .model(model)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                    anonymousChatRecordRepository.save(aiRecord);
                }

                // 发送完成事件
                log.info("对话完成，发送 [DONE] 事件 - sessionId={}", finalSessionId);
                try {
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("发送完成事件失败 - sessionId={}", sessionId, e);
                }
                
            } catch (Exception e) {
                // log.error("对话异常: sessionId={}", sessionId, e); // Removed to avoid duplicate logging, handled in handleError
                handleError(emitter, e);
            }
        });
        
        return emitter;
    }

    private String performBlockingChat(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter, String ipAddress, StringBuilder fullReasoning) throws IOException {
        StringBuilder fullContent = new StringBuilder();
        
        // 检查是否为推理模型
        boolean isReasoner = "deepseek-reasoner".equals(model) || "doubao-reasoner".equals(model);
        
        if (isReasoner) {
             performBlockingOkHttpChat(prompt, sessionId, model, userId, systemPrompt, emitter, fullContent, ipAddress, fullReasoning);
        } else {
             performBlockingSpringAiChat(prompt, sessionId, model, userId, systemPrompt, emitter, fullContent, ipAddress, fullReasoning);
        }
        
        return fullContent.toString();
    }
    
    private void performBlockingSpringAiChat(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent, String ipAddress, StringBuilder fullReasoning) {
        // Determine client
        StreamingChatClient clientToUse = streamingChatClientProvider.getIfAvailable();
        if ("doubao".equals(model) && doubaoStreamingChatClient != null) clientToUse = doubaoStreamingChatClient;
        if (("deepseek".equals(model) || "deepseek-chat".equals(model)) && deepseekStreamingChatClient != null) clientToUse = deepseekStreamingChatClient;
        
        String actualModel = (model == null || model.isEmpty()) ? "deepseek-chat" : model;
        if ("deepseek".equals(actualModel)) actualModel = "deepseek-chat";
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(actualModel)
                .withTemperature(0.7f)
                .withMaxTokens(maxTokens)
                .build();
        
        Prompt promptObj = buildPrompt(prompt, sessionId, userId, ipAddress, options, systemPrompt);
        
        if (clientToUse == null) {
             String content = fallbackAnswer(prompt);
             sendChatResponse(emitter, content, null);
             fullContent.append(content);
             return;
        }

        generateTitleAndSuggestionsAsync(prompt, sessionId, userId, emitter);

        clientToUse.stream(promptObj)
            .doOnNext(chatResponse -> {
                String content = chatResponse.getResult().getOutput().getContent();
                if (content != null && !content.isEmpty()) {
                    sendChatResponse(emitter, content, null);
                    appendWithLimit(fullContent, content, maxSavedChars);
                }
            })
            .doOnError(e -> {
                throw new RuntimeException(e);
            })
            .blockLast();
    }

    private void performBlockingOkHttpChat(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent, String ipAddress, StringBuilder fullReasoning) throws IOException {
         if (userId != null) {
             generateTitleAndSuggestionsAsync(prompt, sessionId, userId, emitter);
         }
         
         String apiKey = "";
         String apiUrl = "";
         String requestModel = "";
         boolean isDoubao = false;

         if ("deepseek-reasoner".equals(model)) {
             apiKey = deepseekApiKey;
             apiUrl = deepseekApiUrl + "/v1/chat/completions";
             requestModel = "deepseek-reasoner";
         } else if ("doubao-reasoner".equals(model)) {
             apiKey = doubaoApiKey;
             apiUrl = doubaoApiUrl + "/api/v3/chat/completions";
             if (doubaoApiUrl.endsWith("/chat/completions")) apiUrl = doubaoApiUrl;
             else if (doubaoApiUrl.endsWith("/")) apiUrl = doubaoApiUrl + "api/v3/chat/completions";
             requestModel = "doubao-seed-1-6-251015";
             isDoubao = true;
         }

         List<Map<String, String>> messages = new ArrayList<>();
         if (systemPrompt != null && !systemPrompt.isEmpty()) {
             Map<String, String> sysMsg = new HashMap<>();
             sysMsg.put("role", "system");
             sysMsg.put("content", systemPrompt);
             messages.add(sysMsg);
         }
         
        if (sessionId != null && !sessionId.isEmpty()) {
            int budget = maxHistoryChars == null ? 0 : Math.max(0, maxHistoryChars);
            List<Map<String, String>> reversedIncluded = new ArrayList<>();

            if (userId != null) {
                List<ChatRecord> history = chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderDesc(
                    userId,
                    sessionId,
                    PageRequest.of(0, maxHistoryMessages == null ? 0 : Math.max(0, maxHistoryMessages))
                );
                for (ChatRecord record : history) {
                    if (budget <= 0) break;
                    String role = (record.getSenderType() == 1 || record.getSenderType() == 3) ? "user" : "assistant";
                    String content = record.getContent();
                    if (record.getSenderType() != null && record.getSenderType() == 3) {
                        content = truncateToMax(content, maxToolResultChars);
                    }
                    if (content == null || content.isEmpty()) continue;
                    budget -= content.length();
                    Map<String, String> msg = new HashMap<>();
                    msg.put("role", role);
                    msg.put("content", content);
                    reversedIncluded.add(msg);
                }
            } else {
                List<AnonymousChatRecord> history = (ipAddress == null || ipAddress.isEmpty())
                    ? anonymousChatRecordRepository.findBySessionIdOrderByCreatedAtDesc(
                        sessionId,
                        PageRequest.of(0, maxHistoryMessages == null ? 0 : Math.max(0, maxHistoryMessages))
                    )
                    : anonymousChatRecordRepository.findBySessionIdAndIpAddressOrderByCreatedAtDesc(
                        sessionId,
                        ipAddress,
                        PageRequest.of(0, maxHistoryMessages == null ? 0 : Math.max(0, maxHistoryMessages))
                    );
                for (AnonymousChatRecord record : history) {
                    if (budget <= 0) break;
                    String role = "user".equalsIgnoreCase(record.getRole()) ? "user" : "assistant";
                    String content = record.getContent();
                    if (content == null || content.isEmpty()) continue;
                    budget -= content.length();
                    Map<String, String> msg = new HashMap<>();
                    msg.put("role", role);
                    msg.put("content", content);
                    reversedIncluded.add(msg);
                }
            }

            Collections.reverse(reversedIncluded);
            messages.addAll(reversedIncluded);
        }
         
         Map<String, String> currentMsg = new HashMap<>();
         currentMsg.put("role", "user");
         currentMsg.put("content", prompt);
         messages.add(currentMsg);

         Map<String, Object> payload = new HashMap<>();
         payload.put("model", requestModel);
         payload.put("messages", messages);
         payload.put("stream", true);
         payload.put("temperature", 0.6);
         payload.put("max_tokens", maxTokens);
         if (isDoubao) payload.put("thinking", Map.of("type", "enabled"));

         String jsonPayload = objectMapper.writeValueAsString(payload);
         RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json; charset=utf-8"));
         Request request = new Request.Builder()
                 .url(apiUrl)
                 .addHeader("Authorization", "Bearer " + apiKey)
                 .post(body)
                 .build();

         try (Response response = okHttpClient.newCall(request).execute()) {
             if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
             
             if (response.body() == null) throw new IOException("Response body is null");
             
                    InputStream is = response.body().byteStream();
                    // 使用更小的缓冲区，减少延迟
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8), 8192);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;
                            try {
                                JsonNode root = objectMapper.readTree(data);
                                JsonNode choices = root.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    String reasoningContent = delta.path("reasoning_content").asText("");
                                    String content = delta.path("content").asText("");
                                    
                                    // 立即发送，不等待累积
                                    if (!reasoningContent.isEmpty() || !content.isEmpty()) {
                                        sendChatResponse(emitter, content, reasoningContent);
                                        appendWithLimit(fullContent, content, maxSavedChars);
                                        if (!reasoningContent.isEmpty()) {
                                            appendWithLimit(fullReasoning, reasoningContent, maxSavedReasoningChars);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("Parse SSE error in performBlockingChat: {}", e.getMessage());
                            }
                        }
                    }
         }
    }


    /**
     * 异步生成会话标题和建议问题
     */
    private void generateTitleAndSuggestionsAsync(String userPrompt, String sessionId, Long userId, SseEmitter emitter) {
        backgroundExecutor.execute(() -> {
            try {
                if (deepseekChatClient == null) return;

                // 如果sessionId为空，无法保存标题和建议，直接返回
                if (sessionId == null || sessionId.isEmpty()) {
                    return;
                }

                // 检查是否需要生成标题
                boolean needTitle = true;
                if (sessionId != null && !sessionId.isEmpty()) {
                    Optional<ChatSession> sessionOpt = chatRecordService.getChatSession(sessionId);
                    if (sessionOpt.isPresent() && sessionOpt.get().getTitle() != null && 
                        !"新对话".equals(sessionOpt.get().getTitle()) && !sessionOpt.get().getTitle().isEmpty()) {
                        needTitle = false;
                    }
                }

                String systemPrompt = "你是一个中文助手，需要基于【当前用户询问】（最重要）以及【历史用户询问】（仅供参考）生成结果。\n" +
                        "仅输出 JSON，不要输出任何额外文字（包括 Markdown/代码块）。\n" +
                        "请生成 3 个“用户视角”的下一步追问（用户对助手说的话），要求：\n" +
                        "1) 每个都是完整问题，优先更具体、更可执行；\n" +
                        "2) 不要以 AI 口吻表达（如“我可以为你…/我还能…”），不要自称“AI/助手”；\n" +
                        "3) 不要复述历史问题，不要照抄历史原句；\n" +
                        "4) 每个问题 8~25 个汉字，末尾使用“？”。\n";
                if (needTitle) {
                    systemPrompt += "由于这是会话的第一条消息，请同时生成一个简短的标题（不超过15个字）。\n";
                }
                systemPrompt += "请严格按照以下 JSON 格式返回，不要包含任何其他文字：\n" +
                        "{\n" +
                        (needTitle ? "  \"title\": \"标题内容\",\n" : "") +
                        "  \"suggestions\": [\"问题1\", \"问题2\", \"问题3\"]\n" +
                        "}";

                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withModel("deepseek-chat")
                        .withTemperature(0.3f)
                        .build();

                String userPromptWithHistory = buildTitleAndSuggestionsUserPrompt(userPrompt, sessionId, userId);
                List<Message> messages = List.of(
                        new org.springframework.ai.chat.messages.SystemMessage(systemPrompt),
                        new UserMessage(userPromptWithHistory)
                );

                ChatResponse response = deepseekChatClient.call(new Prompt(messages, options));
                String content = response.getResult().getOutput().getContent();

                // 解析 JSON
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}");
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                    JsonNode root = objectMapper.readTree(jsonStr);
                    
                    String title = needTitle ? root.path("title").asText() : null;
                    JsonNode suggestionsNode = root.path("suggestions");
                    List<String> suggestionsList = new ArrayList<>();
                    if (suggestionsNode.isArray()) {
                        for (JsonNode node : suggestionsNode) {
                            suggestionsList.add(node.asText());
                        }
                    }

                    LinkedHashSet<String> normalized = new LinkedHashSet<>();
                    for (String s : suggestionsList) {
                        if (s == null) continue;
                        String t = s.trim();
                        if (t.isEmpty()) continue;
                        t = t.replaceAll("^\\s*[0-9]+[\\.、\\)]\\s*", "");
                        t = t.replaceAll("^\\s*[-•]\\s*", "");
                        if (!t.endsWith("？") && !t.endsWith("?")) t = t + "？";
                        normalized.add(t);
                        if (normalized.size() >= 3) break;
                    }
                    suggestionsList = new ArrayList<>(normalized);
                    while (suggestionsList.size() < 3) {
                        if (suggestionsList.size() == 0) suggestionsList.add("我下一步应该先做什么？");
                        else if (suggestionsList.size() == 1) suggestionsList.add("你能给我一个可执行的步骤清单吗？");
                        else suggestionsList.add("有哪些常见坑需要我提前避免？");
                    }

                    String suggestionsJson = objectMapper.writeValueAsString(suggestionsList);

                    // 保存到数据库
                    chatRecordService.updateSessionTitleAndSuggestions(sessionId, title, suggestionsJson, userId);

                    // 发送 SSE 事件（如果有 emitter）
                    if (emitter != null) {
                        Map<String, Object> sseData = new HashMap<>();
                        sseData.put("type", "session_update");
                        sseData.put("session_id", sessionId); // 始终包含当前会话ID
                        if (title != null) sseData.put("title", title);
                        sseData.put("suggestions", suggestionsList);
                        try {
                            emitter.send(SseEmitter.event().name("session_update").data(objectMapper.writeValueAsString(sseData)));
                        } catch (Exception ex) {
                            // 忽略发送失败
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error generating title and suggestions: " + e.getMessage());
            }
        });
    }

    /**
     * 构建用于“标题+引导问题”生成的用户输入：以当前询问为主，历史询问仅作参考，并限制长度。
     */
    private String buildTitleAndSuggestionsUserPrompt(String userPrompt, String sessionId, Long userId) {
        final int maxHistoryQuestions = 6;
        final int maxEachQuestionChars = 180;
        final int maxHistoryTotalChars = 1200;
        
        String current = userPrompt == null ? "" : userPrompt.trim();
        if (current.isEmpty()) current = "(空)";
        String currentForCompare = current.replaceAll("\\s+", " ").trim();
        
        if (sessionId == null || sessionId.isEmpty()) {
            return "【当前用户询问（最重要）】\n" + current + "\n";
        }

        List<ChatRecord> history;
        if (userId != null) {
            history = chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
        } else {
            history = chatRecordRepository.findBySessionIdOrderByMessageOrderAsc(sessionId);
        }
        
        if (history == null || history.isEmpty()) {
            return "【当前用户询问（最重要）】\n" + current + "\n";
        }

        List<String> userQuestions = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatRecord record = history.get(i);
            if (record == null) continue;
            if (record.getSenderType() != 1) continue;
            String q = record.getContent();
            if (q == null) continue;
            q = q.trim();
            if (q.isEmpty()) continue;
            if (q.length() > maxEachQuestionChars) q = q.substring(0, maxEachQuestionChars) + "...";
            String qForCompare = q.replaceAll("\\s+", " ").trim();
            if (qForCompare.equals(currentForCompare)) continue;
            userQuestions.add(q);
            if (userQuestions.size() >= maxHistoryQuestions) break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【当前用户询问（最重要）】\n").append(current).append("\n");
        if (!userQuestions.isEmpty()) {
            sb.append("\n【历史用户询问（仅供参考，已截断）】\n");
            int appended = 0;
            for (int i = userQuestions.size() - 1; i >= 0; i--) {
                String q = userQuestions.get(i);
                int nextLen = q.length() + 3;
                if (appended + nextLen > maxHistoryTotalChars) break;
                sb.append("- ").append(q).append("\n");
                appended += nextLen;
            }
        }
        return sb.toString();
    }

    private Prompt buildPrompt(String promptText, String sessionId, Long userId, String ipAddress, OpenAiChatOptions options, String systemPrompt) {
        List<Message> messages = new ArrayList<>();
        
        // Add System Prompt if exists
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            log.debug("添加系统提示词到消息列表: length={}", systemPrompt.length());
            messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        }

        // 获取历史消息
        if (sessionId != null && !sessionId.isEmpty()) {
            int budget = maxHistoryChars == null ? 0 : Math.max(0, maxHistoryChars);
            if (userId != null) {
                // 已登录用户：查询 ChatRecord
                List<ChatRecord> history = chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderDesc(
                    userId,
                    sessionId,
                    PageRequest.of(0, maxHistoryMessages == null ? 0 : Math.max(0, maxHistoryMessages))
                );
                List<Message> reversedIncluded = new ArrayList<>();
                for (ChatRecord record : history) {
                    if (budget <= 0) break;
                    String content = record.getContent();
                    if (record.getSenderType() != null && record.getSenderType() == 3) {
                        String toolResultContent = truncateToMax(content, maxToolResultChars);
                        if (record.getExitCode() != null && record.getExitCode() != 0) {
                            String stderr = record.getStderr();
                            if (stderr != null && !stderr.isEmpty()) {
                                toolResultContent = toolResultContent + "\n错误信息: " + truncateToMax(stderr, maxToolResultChars);
                            }
                        }
                        content = toolResultContent;
                    }
                    if (content == null || content.isEmpty()) continue;
                    budget -= content.length();
                    if (record.getSenderType() != null && record.getSenderType() == 2) {
                        reversedIncluded.add(new AssistantMessage(content));
                    } else {
                        reversedIncluded.add(new UserMessage(content));
                    }
                }
                Collections.reverse(reversedIncluded);
                messages.addAll(reversedIncluded);
            } else {
                // 匿名用户：查询 AnonymousChatRecord
                List<AnonymousChatRecord> history = (ipAddress == null || ipAddress.isEmpty())
                    ? anonymousChatRecordRepository.findBySessionIdOrderByCreatedAtDesc(
                        sessionId,
                        PageRequest.of(0, maxHistoryMessages == null ? 0 : Math.max(0, maxHistoryMessages))
                    )
                    : anonymousChatRecordRepository.findBySessionIdAndIpAddressOrderByCreatedAtDesc(
                        sessionId,
                        ipAddress,
                        PageRequest.of(0, maxHistoryMessages == null ? 0 : Math.max(0, maxHistoryMessages))
                    );
                List<Message> reversedIncluded = new ArrayList<>();
                for (AnonymousChatRecord record : history) {
                    if (budget <= 0) break;
                    String content = record.getContent();
                    if (content == null || content.isEmpty()) continue;
                    budget -= content.length();
                    if ("assistant".equalsIgnoreCase(record.getRole())) {
                        reversedIncluded.add(new AssistantMessage(content));
                    } else {
                        reversedIncluded.add(new UserMessage(content));
                    }
                }
                Collections.reverse(reversedIncluded);
                messages.addAll(reversedIncluded);
            }
        }
        
        // 添加当前用户消息
        messages.add(new UserMessage(promptText));
        
        return new Prompt(messages, options);
    }

    private void handleError(SseEmitter emitter, Throwable e) {
        // Unwrap RuntimeException if it's ours
        if (e instanceof RuntimeException && "Stop chat generation".equals(e.getMessage()) && e.getCause() != null) {
            e = e.getCause();
        }

        // Check for client disconnection or timeout
        String msg = e.getMessage();
        if (e instanceof AsyncRequestNotUsableException ||
            (msg != null && (msg.contains("SocketTimeoutException") || msg.contains("Broken pipe") || msg.contains("connection was aborted")))) {
            log.warn("Client disconnected or timed out during chat: {}", msg);
            return;
        }

        // 记录错误日志
        log.error("AI Chat Error: ", e);
        
        try {
            String errorMsg = "AI服务暂时不可用: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
            String json = objectMapper.writeValueAsString(Map.of("content", errorMsg));
            emitter.send(SseEmitter.event().data(json));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception ex) {
            // 发送错误消息失败（可能是连接已断开），仅记录日志
            log.warn("Failed to send error response to client: {}", ex.getMessage());
        }
    }
    
    @Override
    public String ask(String prompt, String sessionId, String model, Long userId, String systemPrompt) {
        try {
            // 异步生成标题（仅限第一条消息）和建议问题（每条消息）
            generateTitleAndSuggestionsAsync(prompt, sessionId, userId, null);

            ChatClient clientToUse = null;
            String actualModel = model;
            
            if ("doubao".equals(model)) {
                if (doubaoChatClient != null) {
                    clientToUse = doubaoChatClient;
                } else {
                    clientToUse = chatClientProvider.getIfAvailable();
                }
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
            
            Prompt promptObj = buildPrompt(prompt, sessionId, userId, null, options, systemPrompt);
            
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

    private OkHttpClient createUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            
            builder.connectTimeout(60, TimeUnit.SECONDS)
                   .writeTimeout(60, TimeUnit.SECONDS)
                   .readTimeout(180, TimeUnit.SECONDS);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
