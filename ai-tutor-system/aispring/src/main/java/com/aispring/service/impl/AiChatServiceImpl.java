package com.aispring.service.impl;

import com.aispring.service.AiChatService;
import com.aispring.service.TokenUsageAuditService;
import com.aispring.repository.ChatRecordRepository;
import com.aispring.repository.AnonymousChatRecordRepository;
import com.aispring.entity.ChatRecord;
import com.aispring.util.SensitiveDataMasker;
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
import com.aispring.entity.WordDict;
import com.aispring.repository.WordDictRepository;
import com.aispring.entity.UserWordProgress;
import com.aispring.repository.UserWordProgressRepository;
import org.springframework.data.domain.Page;
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
import java.util.stream.Collectors;
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
    private final TokenUsageAuditService tokenUsageAuditService;
    private final com.aispring.service.SearchService searchService;
    private final OkHttpClient okHttpClient;
    private final WordDictRepository wordDictRepository;
    private final UserWordProgressRepository userWordProgressRepository;

    @Value("${ai.max-tokens:4096}")
    private Integer maxTokens;

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
                             com.aispring.service.ChatRecordService chatRecordService,
                             TokenUsageAuditService tokenUsageAuditService,
                             com.aispring.service.SearchService searchService,
                             WordDictRepository wordDictRepository,
                             UserWordProgressRepository userWordProgressRepository,
                             @Value("${ai.deepseek.api-key:}") String deepseekApiKey,
                             @Value("${ai.deepseek.api-url:}") String deepseekApiUrl) {
        this.chatClientProvider = chatClientProvider;
        this.streamingChatClientProvider = streamingChatClientProvider;
        this.chatRecordRepository = chatRecordRepository;
        this.anonymousChatRecordRepository = anonymousChatRecordRepository;
        this.chatRecordService = chatRecordService;
        this.tokenUsageAuditService = tokenUsageAuditService;
        this.searchService = searchService;
        this.wordDictRepository = wordDictRepository;
        this.userWordProgressRepository = userWordProgressRepository;
        this.deepseekApiKey = deepseekApiKey;
        this.deepseekApiUrl = deepseekApiUrl;

        // Initialize OkHttpClient with custom timeouts and unsafe SSL
        this.okHttpClient = createUnsafeOkHttpClient();

        log.info("Initializing DeepSeek AI client with Aliyun Bailian API");

        // Initialize DeepSeek Client (Aliyun Bailian)
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty() && deepseekApiUrl != null && !deepseekApiUrl.isEmpty()) {
            try {
                // Aliyun Bailian uses compatible OpenAI mode
                OpenAiApi deepseekApi = new OpenAiApi(deepseekApiUrl, deepseekApiKey);
                OpenAiChatOptions deepseekOptions = OpenAiChatOptions.builder()
                        .withModel("deepseek-v3.2")
                        .withTemperature(0.7f)
                        .withMaxTokens(maxTokens)
                        .build();

                OpenAiChatClient client = new OpenAiChatClient(deepseekApi, deepseekOptions);
                this.deepseekChatClient = client;
                this.deepseekStreamingChatClient = client;
                log.info("DeepSeek AI client initialized successfully with Aliyun Bailian API: " + deepseekApiUrl);
            } catch (Exception e) {
                log.error("Failed to initialize DeepSeek AI client: {}", e.getMessage());
            }
        } else {
            log.info("DeepSeek client initialization skipped (missing api-key or api-url)");
        }
    }



    /**
     * 统一发送聊天响应（SSE）- 优化流畅度
     */
    private void sendChatResponse(SseEmitter emitter, String content, String reasoningContent) {
        Map<String, String> resultMap = new HashMap<>();
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            resultMap.put("reasoning_content", reasoningContent);
        }
        if (content != null && !content.isEmpty()) {
            resultMap.put("content", content);
        }
        if (!resultMap.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(resultMap);
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .data(json)
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("message");
                emitter.send(event);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to send chat response", e);
            }
        }
    }

    @Override
    public SseEmitter askStream(String prompt, String sessionId, String model, Long userId, String ipAddress, String systemPrompt) {
        return askStreamInternal(prompt, sessionId, model, userId, ipAddress, systemPrompt);
    }

    /**
     * 普通流式问答核心实现
     */
    private SseEmitter askStreamInternal(String initialPrompt, String sessionId, String model, Long userId, String ipAddress, String systemPrompt) {
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
                String fullContent = performBlockingChat(initialPrompt, finalSessionId, model, userId, systemPrompt, emitter, ipAddress, fullReasoning);

                // 检查是否包含搜索指令
                String contentTrimmed = fullContent.trim();
                java.util.regex.Pattern searchPattern = java.util.regex.Pattern.compile("<search(?:\\s+site=\"([^\"]+)\")?>(.*?)</search>", java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher matcher = searchPattern.matcher(contentTrimmed);

                java.util.regex.Pattern vocabPattern = java.util.regex.Pattern.compile("<query-vocab\\s+topic=\"([^\"]+)\"\\s+limit=\"(\\d+)\"\\s*/>", java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher vocabMatcher = vocabPattern.matcher(contentTrimmed);

                if (matcher.find()) {
                    String site = matcher.group(1);
                    String keyword = matcher.group(2).trim();
                    log.info("检测到AI搜索请求: keyword={}, site={}", keyword, site);

                    // 1. 发送搜索中的提示给前端
                    Map<String, String> searchMsg = new HashMap<>();
                    String searchDisplay = site != null && !site.isEmpty() ? keyword + " (在 " + site + " 中)" : keyword;
                    searchMsg.put("content", "\n\n*正在为您搜索: " + searchDisplay + "*...\n\n");
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(searchMsg)));

                    // 2. 执行搜索
                    String searchResult = searchService.searchIndustryInfo(keyword, site);

                    // 3. 将搜索结果追加到提示词中，重新进行一次无搜索指令的请求
                    String newPrompt = initialPrompt + "\n\n【系统反馈的搜索结果】\n" + searchResult + "\n\n请根据上述搜索结果回答用户的问题。";

                    // 再次调用（屏蔽搜索指令，防止死循环）
                    String secondContent = performBlockingChat(newPrompt, finalSessionId, model, userId, "【系统提示】你已经获取了搜索结果，请直接回答用户问题，不要再输出<search>标签。", emitter, ipAddress, fullReasoning);

                    fullContent = fullContent + "\n\n" + secondContent;
                } else if (vocabMatcher.find()) {
                    String topic = vocabMatcher.group(1);
                    int limit = Integer.parseInt(vocabMatcher.group(2));
                    log.info("检测到AI单词检索请求: topic={}, limit={}", topic, limit);

                    Map<String, String> searchMsg = new HashMap<>();
                    searchMsg.put("content", "\n\n*正在从本地词库为您检索 " + topic + " 相关的单词*...\n\n");
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(searchMsg)));

                    // 这里应该是真正的 RAG 检索逻辑，从数据库捞取
                    // 目前为了演示跑通，我们返回一段模拟的 RAG 结果（实际应替换为 wordDictRepository.findAll() 等操作）
                    String ragResult = "[\n" +
                            "  {\"id\": 101, \"word\": \"espresso\", \"definition\": \"n. 浓缩咖啡\", \"user_status\": \"易错，需复习\"},\n" +
                            "  {\"id\": 102, \"word\": \"latte\", \"definition\": \"n. 拿铁\", \"user_status\": \"未学\"},\n" +
                            "  {\"id\": 103, \"word\": \"cappuccino\", \"definition\": \"n. 卡布奇诺\", \"user_status\": \"未学\"}\n" +
                            "]";

                    String newPrompt = initialPrompt + "\n\n【系统反馈的候选单词数据（请使用这些数据生成卡片）】\n" + ragResult + "\n\n请严格使用上述数据生成 <vocab-practice> 练习卡片，并补全例句。";

                    String secondContent = performBlockingChat(newPrompt, finalSessionId, model, userId, "【系统提示】你已经获取了本地词库数据，请直接生成练习卡片，不要再输出<query-vocab>标签。", emitter, ipAddress, fullReasoning);
                    fullContent = fullContent + "\n\n" + secondContent;
                }

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
        // 敏感信息脱敏：发送给大模型前过滤身份证号、手机号等隐私数据
        String maskedPrompt = SensitiveDataMasker.mask(prompt);
        StringBuilder fullContent = new StringBuilder();

        // 判断是否为思考模式
        // deepseek-reasoner 或 deepseek-r1 表示思考模式
        boolean enableThinking = "deepseek-reasoner".equals(model) || "deepseek-r1".equals(model);

        // 使用 OkHttp 发送请求，支持思考过程
        performBlockingOkHttpChatWithThinking(maskedPrompt, sessionId, model, userId, systemPrompt, emitter, fullContent, ipAddress, fullReasoning, enableThinking);

        return fullContent.toString();
    }

    private void performBlockingSpringAiChat(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent, String ipAddress, StringBuilder fullReasoning) {
        // Determine client - only DeepSeek now
        StreamingChatClient clientToUse = streamingChatClientProvider.getIfAvailable();
        if (("deepseek".equals(model) || "deepseek-chat".equals(model)) && deepseekStreamingChatClient != null) {
            clientToUse = deepseekStreamingChatClient;
        }

        // 阿里百炼使用 deepseek-v3.2 模型，开启思考模式
        String actualModel = "deepseek-v3.2";
        String provider = "deepseek";

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

        generateTitleAndSuggestionsAsync(prompt, sessionId, userId, emitter, actualModel);

        long startMs = System.currentTimeMillis();
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
        long responseTimeMs = System.currentTimeMillis() - startMs;
        tokenUsageAuditService.recordEstimated(provider, actualModel, userId, sessionId, prompt.length(), fullContent.length(), responseTimeMs, true);
    }

    /**
     * 使用 OkHttp 发送流式请求，支持思考过程
     * @param enableThinking 是否开启思考模式
     */
    private void performBlockingOkHttpChatWithThinking(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent, String ipAddress, StringBuilder fullReasoning, boolean enableThinking) throws IOException {
         long startMs = System.currentTimeMillis();
         String requestModel = "deepseek-v3.2";
         if (userId != null) {
             generateTitleAndSuggestionsAsync(prompt, sessionId, userId, emitter, requestModel);
         }

         String apiKey = deepseekApiKey;
         String apiUrl = "";

         // 阿里百炼 API URL - 需要包含 /v1/chat/completions
         if (deepseekApiUrl.contains("dashscope.aliyuncs.com")) {
             // 阿里百炼兼容模式 URL: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
             apiUrl = deepseekApiUrl + "/v1/chat/completions";
         } else {
             apiUrl = deepseekApiUrl + "/v1/chat/completions";
         }

         // 添加搜索与单词练习指令
         String systemInstructions = "\n【系统搜索能力】如果你需要查询实时信息或不知道的内容，或者需要从特定网站获取信息，请在回答中直接输出XML格式：<search site=\"网站域名(可选)\">关键词</search>。例如：<search site=\"spring.io\">Spring Boot</search> 或 <search>今天的新闻</search>。系统会自动为你搜索并将结果反馈给你，你收到结果后再给出最终回答。如果不需要搜索，请直接回答。" +
                 "\n【单词记忆与RAG检索】当用户需要学习或复习某类单词时，请输出检索意图标签，例如：<query-vocab topic=\"咖啡\" limit=\"5\" />，系统会从本地词库(ECDICT)及用户的易错本中提取真实单词数据反馈给你。收到数据后，请使用如下格式输出要练习的内容：\n" +
                 "<vocab-practice>\n" +
                 "  <vocab word=\"单词\" phonetic=\"音标\" definition=\"详细释义(选填)\" sentence=\"例句\" translation=\"中文翻译\" />\n" +
                 "  <vocab mode=\"spelling\" word=\"隐藏的单词\" phonetic=\"音标\" definition=\"详细释义(选填)\" sentence=\"带下划线的例句[___]\" translation=\"中文翻译\" />\n" +
                 "</vocab-practice>";
         if (systemPrompt == null || systemPrompt.isEmpty()) {
             systemPrompt = systemInstructions;
         } else {
             systemPrompt += systemInstructions;
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
                    if ("user".equals(role)) content = SensitiveDataMasker.mask(content);
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
                    if ("user".equals(role)) content = SensitiveDataMasker.mask(content);
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
         currentMsg.put("content", prompt);  // prompt 已在 performBlockingChat 中脱敏后传入
         messages.add(currentMsg);

         Map<String, Object> payload = new HashMap<>();
         payload.put("model", requestModel);
         payload.put("messages", messages);
         payload.put("stream", true);
         payload.put("temperature", 0.6);
         payload.put("max_tokens", maxTokens);
         // 根据参数决定是否开启思考模式
         if (enableThinking) {
             payload.put("enable_thinking", true);
         }

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
                                        try {
                                            sendChatResponse(emitter, content, reasoningContent);
                                            appendWithLimit(fullContent, content, maxSavedChars);
                                            if (!reasoningContent.isEmpty()) {
                                                appendWithLimit(fullReasoning, reasoningContent, maxSavedReasoningChars);
                                            }
                                        } catch (IllegalStateException ex) {
                                            log.warn("Client disconnected during SSE stream: {}", ex.getMessage());
                                            break; // Stop processing if client disconnected
                                        } catch (Exception ex) {
                                            log.warn("Failed to send chunk, emitter may be closed: {}", ex.getMessage());
                                            break; // 停止读取
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("Parse SSE error in performBlockingChat: {}", e.getMessage());
                            }
                        }
                    }
         }
         long responseTimeMs = System.currentTimeMillis() - startMs;
         String provider = "deepseek-reasoner".equals(model) ? "deepseek" : "default";
         tokenUsageAuditService.recordEstimated(provider, requestModel, userId, sessionId, prompt.length(), fullContent.length(), responseTimeMs, true);
    }


    /**
     * 异步生成会话标题和建议问题
     */
    private void generateTitleAndSuggestionsAsync(String userPrompt, String sessionId, Long userId, SseEmitter emitter, String model) {
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
                        .withModel((model == null || model.isBlank()) ? "deepseek-v3.2" : model)
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
                log.error("Error generating title and suggestions: {}", e.getMessage(), e);
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

        // Check for client disconnection or timeout or already completed emitter
        String msg = e.getMessage();
        if (e instanceof AsyncRequestNotUsableException ||
            e instanceof IllegalStateException ||
            (msg != null && (msg.contains("SocketTimeoutException") || msg.contains("Broken pipe") || msg.contains("connection was aborted") || msg.contains("ResponseBodyEmitter has already completed")))) {
            log.warn("Client disconnected, timed out, or emitter already completed during chat: {}", msg);
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
    public String ask(String prompt, String sessionId, String model, Long userId, String systemPrompt, String ipAddress) {
        try {
            // 敏感信息脱敏
            String maskedPrompt = SensitiveDataMasker.mask(prompt);

            // 异步生成标题（仅限第一条消息）和建议问题（每条消息）
            generateTitleAndSuggestionsAsync(prompt, sessionId, userId, null, model);

            String actualModel = model;
            if (actualModel == null || actualModel.isEmpty()) {
                actualModel = "deepseek-v3.2";
            } else if ("deepseek-chat".equals(actualModel) || "deepseek".equals(actualModel)) {
                actualModel = "deepseek-v3.2";
            }

            // 使用 REST API 调用阿里百炼
            long startMs = System.currentTimeMillis();
            StringBuilder fullContent = new StringBuilder();

            // 构造消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            String systemInstructions = "\n【系统搜索能力】如果你需要查询实时信息或不知道的内容，或者需要从特定网站获取信息，请在回答中直接输出XML格式：<search site=\"网站域名(可选)\">关键词</search>。例如：<search site=\"spring.io\">Spring Boot</search> 或 <search>今天的新闻</search>。系统会自动为你搜索并将结果反馈给你，你收到结果后再给出最终回答。如果不需要搜索，请直接回答。" +
                    "\n【单词记忆与RAG检索】当用户需要学习或复习某类单词时，请输出检索意图标签，例如：<query-vocab topic=\"咖啡\" limit=\"5\" />，系统会从本地词库(ECDICT)及用户的易错本中提取真实单词数据反馈给你。收到数据后，请使用如下格式输出要练习的内容：\n" +
                    "<vocab-practice>\n" +
                    "  <vocab word=\"单词\" phonetic=\"音标\" definition=\"详细释义(选填)\" sentence=\"例句\" translation=\"中文翻译\" />\n" +
                    "  <vocab mode=\"spelling\" word=\"隐藏的单词\" phonetic=\"音标\" definition=\"详细释义(选填)\" sentence=\"带下划线的例句[___]\" translation=\"中文翻译\" />\n" +
                    "</vocab-practice>";
            if (systemPrompt == null || systemPrompt.isEmpty()) {
                systemPrompt = systemInstructions;
            } else {
                systemPrompt += systemInstructions;
            }

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, String> sysMsg = new HashMap<>();
                System.out.println("systemPrompt: " + systemPrompt);
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", maskedPrompt);
            messages.add(userMsg);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", actualModel);
            payload.put("messages", messages);
            payload.put("stream", false);
            payload.put("temperature", 0.7);
            payload.put("max_tokens", maxTokens);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json; charset=utf-8"));

            String finalUrl = deepseekApiUrl;
            if (finalUrl != null && !finalUrl.endsWith("/chat/completions")) {
                if (finalUrl.endsWith("/")) {
                    finalUrl += "v1/chat/completions";
                } else {
                    finalUrl += "/v1/chat/completions";
                }
            }

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .addHeader("Authorization", "Bearer " + deepseekApiKey)
                    .post(body)
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    log.error("AI API Error: Code={}, Body={}", response.code(), errorBody);
                    throw new IOException("Unexpected code " + response);
                }

                if (response.body() == null) throw new IOException("Response body is null");

                String responseBody = response.body().string();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                    JsonNode choice = rootNode.get("choices").get(0);
                    if (choice.has("message") && choice.get("message").has("content")) {
                        fullContent.append(choice.get("message").get("content").asText());
                    }
                }
            }

            long responseTimeMs = System.currentTimeMillis() - startMs;
            String content = fullContent.toString();

            log.info("AI Response received. Length: {}", content.length());

            // 检查是否包含搜索指令
            String contentTrimmed = content.trim();
            java.util.regex.Pattern searchPattern = java.util.regex.Pattern.compile("<search(?:\\s+site=\"([^\"]+)\")?>(.*?)</search>", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = searchPattern.matcher(contentTrimmed);
            if (matcher.find()) {
                String site = matcher.group(1);
                String keyword = matcher.group(2).trim();
                log.info("检测到非流式AI搜索请求: keyword={}, site={}", keyword, site);

                // 1. 执行搜索
                String searchResult = searchService.searchIndustryInfo(keyword, site);

                // 2. 将搜索结果追加到提示词中，重新进行一次无搜索指令的请求
                String newPrompt = prompt + "\n\n【系统反馈的搜索结果】\n" + searchResult + "\n\n请根据上述搜索结果回答用户的问题。";

                // 再次调用（屏蔽搜索指令，防止死循环）
                return ask(newPrompt, sessionId, model, userId, "【系统提示】你已经获取了搜索结果，请直接回答用户问题，不要再输出<search>标签。", ipAddress);
            }

            // 记录 Token 消耗审计
            String provider = "deepseek";
            tokenUsageAuditService.recordEstimated(provider, actualModel, userId, sessionId,
                    prompt.length(), content.length(), responseTimeMs, false);

            return content;
        } catch (Exception e) {
            log.error("AI Chat Error in ask(): {}", e.getMessage(), e);
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
                   .readTimeout(300, TimeUnit.SECONDS);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
