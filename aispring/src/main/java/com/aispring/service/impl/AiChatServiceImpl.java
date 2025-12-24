package com.aispring.service.impl;

import com.aispring.service.AiChatService;
import com.aispring.service.ToolsService;
import com.aispring.repository.ChatRecordRepository;
import com.aispring.entity.ChatRecord;
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
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.*;
import java.security.cert.CertificateException;

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
    private final com.aispring.service.ChatRecordService chatRecordService; // 注入 ChatRecordService
    private final OkHttpClient okHttpClient;
    
    // Phase 2 新增服务
    private final com.aispring.service.SessionStateService sessionStateService;
    private final com.aispring.service.ToolApprovalService toolApprovalService;
    private final com.aispring.service.ToolsService toolsService;
    
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
    
    private ChatClient doubaoChatClient;
    private StreamingChatClient doubaoStreamingChatClient;
    
    private ChatClient deepseekChatClient;
    private StreamingChatClient deepseekStreamingChatClient;
    
    // 上下文最大消息数
    private static final int MAX_CONTEXT_MESSAGES = 10;

    public AiChatServiceImpl(ObjectProvider<ChatClient> chatClientProvider,
                             ObjectProvider<StreamingChatClient> streamingChatClientProvider,
                             ChatRecordRepository chatRecordRepository,
                             com.aispring.service.ChatRecordService chatRecordService, // 添加到构造函数
                             com.aispring.service.SessionStateService sessionStateService,
                             com.aispring.service.ToolApprovalService toolApprovalService,
                             com.aispring.service.ToolsService toolsService,
                             @Value("${ai.doubao.api-key:}") String doubaoApiKey,
                             @Value("${ai.doubao.api-url:}") String doubaoApiUrl,
                             @Value("${ai.deepseek.api-key:}") String deepseekApiKey,
                             @Value("${ai.deepseek.api-url:}") String deepseekApiUrl) {
        this.chatClientProvider = chatClientProvider;
        this.streamingChatClientProvider = streamingChatClientProvider;
        this.chatRecordRepository = chatRecordRepository;
        this.chatRecordService = chatRecordService; // 初始化
        this.sessionStateService = sessionStateService;
        this.toolApprovalService = toolApprovalService;
        this.toolsService = toolsService;
        
        this.doubaoApiKey = doubaoApiKey;
        this.doubaoApiUrl = doubaoApiUrl;
        this.deepseekApiKey = deepseekApiKey;
        this.deepseekApiUrl = deepseekApiUrl;
        
        // Initialize OkHttpClient with custom timeouts and unsafe SSL
        this.okHttpClient = createUnsafeOkHttpClient();

        // Initialize Doubao Client

        // Initialize Doubao Client
        System.out.println("[Doubao] Initializing Doubao AI client...");
        System.out.println("[Doubao] API Key length: " + (doubaoApiKey != null ? doubaoApiKey.length() : 0));
        System.out.println("[Doubao] API Key: " + (doubaoApiKey != null && !doubaoApiKey.isEmpty() ? "********" : "NOT SET"));
        System.out.println("[Doubao] API URL: " + (doubaoApiUrl != null ? doubaoApiUrl : "NOT SET"));
        
        if (doubaoApiKey != null && !doubaoApiKey.isEmpty() && doubaoApiUrl != null && !doubaoApiUrl.isEmpty()) {
            try {
                System.out.println("[Doubao] Creating OpenAiApi instance for Doubao...");
                // Doubao requires /api/v3 appended to base URL for OpenAiApi
                String doubaoBaseUrl = doubaoApiUrl;
                if (!doubaoBaseUrl.endsWith("/api/v3") && !doubaoBaseUrl.endsWith("/api/v3/")) {
                    if (doubaoBaseUrl.endsWith("/")) {
                        doubaoBaseUrl += "api/v3";
                    } else {
                        doubaoBaseUrl += "/api/v3";
                    }
                }
                System.out.println("[Doubao] OpenAiApi constructor params - URL: " + doubaoBaseUrl + ", Key length: " + doubaoApiKey.length());
                OpenAiApi doubaoApi = new OpenAiApi(doubaoBaseUrl, doubaoApiKey);
                System.out.println("[Doubao] OpenAiApi instance created successfully");
                
                System.out.println("[Doubao] Creating OpenAiChatOptions for Doubao...");
                OpenAiChatOptions doubaoOptions = OpenAiChatOptions.builder()
                        .withModel("doubao-pro-32k") // 设置豆包模型名称
                        .withTemperature(0.7f)
                        .withMaxTokens(maxTokens)
                        .build();
                System.out.println("[Doubao] OpenAiChatOptions created with model: " + doubaoOptions.getModel());
                
                System.out.println("[Doubao] Creating OpenAiChatClient instance for Doubao...");
                OpenAiChatClient client = new OpenAiChatClient(doubaoApi, doubaoOptions);
                this.doubaoChatClient = client;
                this.doubaoStreamingChatClient = client;
                System.out.println("[Doubao] Doubao AI client initialized successfully!");
                System.out.println("[Doubao] doubaoChatClient: " + (this.doubaoChatClient != null ? this.doubaoChatClient.getClass().getSimpleName() : "null"));
                System.out.println("[Doubao] doubaoStreamingChatClient: " + (this.doubaoStreamingChatClient != null ? this.doubaoStreamingChatClient.getClass().getSimpleName() : "null"));
            } catch (Exception e) {
                System.err.println("[Doubao ERROR] Failed to initialize Doubao AI client: " + e.getMessage());
                System.err.println("[Doubao ERROR] Stack trace:");
                e.printStackTrace();
                System.err.println("[Doubao ERROR] API URL: " + doubaoApiUrl);
                System.err.println("[Doubao ERROR] API Key length: " + (doubaoApiKey != null ? doubaoApiKey.length() : 0));
            }
        } else {
            System.err.println("[Doubao ERROR] Initialization skipped: missing API key or URL");
            System.err.println("[Doubao ERROR] API Key provided: " + (doubaoApiKey != null && !doubaoApiKey.isEmpty()));
            System.err.println("[Doubao ERROR] API URL provided: " + (doubaoApiUrl != null && !doubaoApiUrl.isEmpty()));
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
        }
    }

    @Override
    public SseEmitter askStream(String prompt, String sessionId, String model, String userId) {
        return askAgentStream(prompt, sessionId, model, userId, null, null, null);
    }

    @Override
    public SseEmitter askAgentStream(String prompt, String sessionId, String model, String userId, String systemPrompt, List<Map<String, Object>> tasks, Consumer<String> onResponse) {
        return askAgentStreamInternal(prompt, sessionId, model, userId, systemPrompt, tasks, onResponse);
    }


    private SseEmitter askAgentStreamInternal(String initialPrompt, String sessionId, String model, String userId, String initialSystemPrompt, List<Map<String, Object>> initialTasks, Consumer<String> onResponse) {
        // 创建SSE发射器，设置超时时间为3分钟
        SseEmitter emitter = new SseEmitter(180_000L);
        
        System.out.println("=== askAgentStream Called ===");
        System.out.println("Model: " + model);
        
        // Phase 3: 生成循环 ID
        String loopId = java.util.UUID.randomUUID().toString();
        
        new Thread(() -> {
            try {
                // Phase 3: 初始化会话状态
                Long userIdLong = Long.valueOf(userId);
                com.aispring.entity.session.SessionState sessionState = 
                        sessionStateService.getOrCreateState(sessionId, userIdLong);
                sessionState.setStatus(com.aispring.entity.agent.AgentStatus.RUNNING);
                sessionState.setCurrentLoopId(loopId);
                sessionState.setStreamState(com.aispring.entity.session.StreamState.streamingLLM());
                sessionStateService.saveState(sessionState);
                
                log.info("Agent 循环开始: sessionId={}, loopId={}", sessionId, loopId);
                
                String currentPrompt = initialPrompt;
                String currentSystemPrompt = initialSystemPrompt;
                List<Map<String, Object>> currentTasks = initialTasks != null ? new ArrayList<>(initialTasks) : new ArrayList<>();
                int loopCount = 0;
                int maxLoops = 20; // 增加最大循环次数

                while (loopCount < maxLoops) {
                    loopCount++;
                    System.out.println("Agent Loop: " + loopCount);
                    
                    // Phase 3: 检查中断标志
                    if (sessionStateService.isInterruptRequested(sessionId)) {
                        log.warn("Agent 循环被中断: sessionId={}, loopId={}, loopCount={}", 
                                sessionId, loopId, loopCount);
                        emitter.send(SseEmitter.event()
                                .name("interrupt")
                                .data("{\"message\": \"Agent 循环已被用户中断\"}"));
                        break;
                    }
                    
                    // 执行对话并获取完整回复
                    String fullResponse = performBlockingChat(currentPrompt, sessionId, model, userId, currentSystemPrompt, emitter);
                    
                    // Hook for capturing response
                    if (onResponse != null) {
                        try {
                            onResponse.accept(fullResponse);
                        } catch (Exception e) {
                            System.err.println("Error in onResponse hook: " + e.getMessage());
                        }
                    }

                    // 解析回复，检查是否需要继续循环
                    boolean shouldContinue = false;
                    try {
                        // 简单提取 JSON
                        String jsonStr = extractJson(fullResponse);
                        
                        if (jsonStr != null) {
                            JsonNode root = objectMapper.readTree(jsonStr);
                            String type = root.has("type") ? root.get("type").asText() : "";
                            
                            if ("task_update".equals(type)) {
                                // ... existing task update logic ...
                                String taskId = root.path("taskId").asText();
                                String status = root.path("status").asText();
                                String desc = root.path("desc").asText("");
                                
                                boolean taskFound = false;
                                for (Map<String, Object> task : currentTasks) {
                                    if (String.valueOf(task.get("id")).equals(taskId)) {
                                        task.put("status", status);
                                        desc = (String) task.get("desc");
                                        taskFound = true;
                                        break;
                                    }
                                }
                                
                                if (taskFound) {
                                    shouldContinue = true;
                                    currentPrompt = String.format("任务 %s (ID: %s) 状态已更新为 %s。请继续执行该任务的具体操作，或进行下一步。", desc, taskId, status);
                                    currentSystemPrompt = updateSystemPromptWithTasks(currentSystemPrompt, currentTasks);
                                }
                            } else if ("TOOL_CALL".equals(type) || "tool_call".equals(type)) {
                                // === 后端工具执行逻辑 ===
                                String toolName = root.path("action").asText();
                                String decisionId = root.path("decision_id").asText(java.util.UUID.randomUUID().toString());
                                JsonNode paramsNode = root.path("params");
                                Map<String, Object> params = objectMapper.convertValue(paramsNode, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                                
                                log.info("检测到工具调用: {}, decisionId={}", toolName, decisionId);
                                
                                // 1. 检查批准
                                if (toolApprovalService.requiresApproval(userIdLong, toolName)) {
                                    log.info("工具需要批准: {}", toolName);
                                    toolApprovalService.createApprovalRequest(sessionId, userIdLong, toolName, params, decisionId);
                                    
                                    // 发送等待批准事件
                                    emitter.send(SseEmitter.event()
                                            .name("waiting_approval")
                                            .data(objectMapper.writeValueAsString(Map.of(
                                                    "decision_id", decisionId,
                                                    "tool", toolName,
                                                    "params", params
                                            ))));
                                    
                                    // 更新状态为等待批准
                                    sessionState.setStatus(com.aispring.entity.agent.AgentStatus.AWAITING_APPROVAL);
                                    sessionStateService.saveState(sessionState);
                                    
                                    // 暂停循环
                                    shouldContinue = false; 
                                    break; // 跳出循环，等待用户操作
                                }
                                
                                // 2. 执行工具
                                log.info("执行工具: {}", toolName);
                                ToolsService.ToolResult result = toolsService.callTool(toolName, params, userIdLong, sessionId);
                                
                                // 3. 发送工具结果给前端（用于展示）
                                emitter.send(SseEmitter.event()
                                        .name("tool_result")
                                        .data(objectMapper.writeValueAsString(result)));
                                
                                // 4. 构建下一次 Prompt
                                currentPrompt = String.format("工具 '%s' 执行结果 (Exit Code: %s):\n%s\n%s\n请根据结果继续执行。", 
                                        toolName, 
                                        result.isSuccess() ? "0" : "-1",
                                        result.getStringResult(),
                                        result.getError() != null ? "Error: " + result.getError() : "");
                                
                                shouldContinue = true;
                                
                            } else if ("TASK_LIST".equals(type) || "task_list".equals(type)) {
                                // 任务列表更新，通常意味着开始
                                JsonNode tasksNode = root.path("tasks");
                                if (tasksNode.isArray()) {
                                    List<Map<String, Object>> newTasks = new ArrayList<>();
                                    for (JsonNode t : tasksNode) {
                                        Map<String, Object> taskMap = objectMapper.convertValue(t, Map.class);
                                        newTasks.add(taskMap);
                                    }
                                    currentTasks = newTasks;
                                    currentSystemPrompt = updateSystemPromptWithTasks(currentSystemPrompt, currentTasks);
                                    
                                    currentPrompt = "任务列表已接收。请开始执行第一个任务。";
                                    shouldContinue = true;
                                }
                            } else if ("TASK_COMPLETE".equals(type)) {
                                currentPrompt = "当前任务已完成。请检查是否还有剩余任务，如果有则继续，没有则结束。";
                                shouldContinue = true;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing agent response for loop: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    if (!shouldContinue) {
                        break;
                    }
                }
                
                // ... cleanup logic ...
                // Only mark as COMPLETED if not PAUSED or AWAITING_APPROVAL
                com.aispring.entity.session.SessionState finalState = sessionStateService.getState(sessionId).orElse(null);
                if (finalState != null && finalState.getStatus() == com.aispring.entity.agent.AgentStatus.RUNNING) {
                     finalState.setStatus(com.aispring.entity.agent.AgentStatus.COMPLETED);
                     finalState.setCurrentLoopId(null);
                     finalState.setStreamState(com.aispring.entity.session.StreamState.idle());
                     sessionStateService.saveState(finalState);
                     
                     emitter.send(SseEmitter.event().data("[DONE]"));
                     emitter.complete();
                } else {
                    // 如果是等待批准或暂停，不要发送 [DONE]，保持连接或者让前端知道是暂停状态
                    // 但 SSE 连接通常在一次请求后结束，所以这里可能需要断开，前端根据状态决定是否重新发起
                    emitter.send(SseEmitter.event().data("[DONE]")); // 结束本次 SSE 连接
                    emitter.complete();
                }
                
            } catch (Exception e) {
                // ... error handling ...
                handleError(emitter, e);
            }
        }).start();
        
        return emitter;
    }

    private String extractJson(String content) {
        if (content == null) return null;
        int jsonStart = content.indexOf("```json");
        if (jsonStart != -1) {
             int end = content.indexOf("```", jsonStart + 7);
             if (end != -1) return content.substring(jsonStart + 7, end).trim();
        }
        jsonStart = content.indexOf("{");
        int jsonEnd = content.lastIndexOf("}");
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return content.substring(jsonStart, jsonEnd + 1);
        }
        return null;
    }
    

    
    private String updateSystemPromptWithTasks(String systemPrompt, List<Map<String, Object>> tasks) {
        if (tasks == null || tasks.isEmpty()) return systemPrompt;
        
        StringBuilder taskContext = new StringBuilder();
        taskContext.append("当前任务链状态：\n");
        for (Map<String, Object> task : tasks) {
            taskContext.append(String.format("- [%s] %s (ID: %s)\n", 
                task.get("status"), task.get("desc"), task.get("id")));
        }
        
        String startMarker = "当前任务链状态：";
        int startIndex = systemPrompt.indexOf(startMarker);
        if (startIndex == -1) {
            startMarker = "当前暂无进行中的任务链。";
            startIndex = systemPrompt.indexOf(startMarker);
        }
        
        if (startIndex != -1) {
            // 查找 # Current Task Context 标题
            String sectionHeader = "# Current Task Context";
            int sectionIndex = systemPrompt.indexOf(sectionHeader);
            if (sectionIndex != -1) {
                 int nextSectionIndex = systemPrompt.indexOf("#", sectionIndex + sectionHeader.length());
                 if (nextSectionIndex == -1) nextSectionIndex = systemPrompt.length();
                 
                 String pre = systemPrompt.substring(0, sectionIndex + sectionHeader.length());
                 String post = systemPrompt.substring(nextSectionIndex);
                 return pre + "\n" + taskContext.toString() + "\n" + post;
            }
        }
        return systemPrompt;
    }

    private String performBlockingChat(String prompt, String sessionId, String model, String userId, String systemPrompt, SseEmitter emitter) throws IOException {
        StringBuilder fullContent = new StringBuilder();
        
        // 检查是否为推理模型
        boolean isReasoner = "deepseek-reasoner".equals(model) || "doubao-reasoner".equals(model);
        
        if (isReasoner) {
             performBlockingOkHttpChat(prompt, sessionId, model, userId, systemPrompt, emitter, fullContent);
        } else {
             performBlockingSpringAiChat(prompt, sessionId, model, userId, systemPrompt, emitter, fullContent);
        }
        
        return fullContent.toString();
    }
    
    private void performBlockingSpringAiChat(String prompt, String sessionId, String model, String userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent) {
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
        
        Prompt promptObj = buildPrompt(prompt, sessionId, userId, options, systemPrompt);
        
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
                    fullContent.append(content);
                }
            })
            .doOnError(e -> {
                throw new RuntimeException(e);
            })
            .blockLast();
    }

    private void performBlockingOkHttpChat(String prompt, String sessionId, String model, String userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent) throws IOException {
         generateTitleAndSuggestionsAsync(prompt, sessionId, userId, emitter);
         
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
             List<ChatRecord> history = chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
             int start = Math.max(0, history.size() - MAX_CONTEXT_MESSAGES);
             for (int i = start; i < history.size(); i++) {
                 ChatRecord record = history.get(i);
                 Map<String, String> msg = new HashMap<>();
                 msg.put("role", record.getSenderType() == 1 ? "user" : "assistant");
                 msg.put("content", record.getContent());
                 messages.add(msg);
             }
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
                                        fullContent.append(content);
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
    private void generateTitleAndSuggestionsAsync(String userPrompt, String sessionId, String userId, SseEmitter emitter) {
        new Thread(() -> {
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
        }).start();
    }

    /**
     * 构建用于“标题+引导问题”生成的用户输入：以当前询问为主，历史询问仅作参考，并限制长度。
     */
    private String buildTitleAndSuggestionsUserPrompt(String userPrompt, String sessionId, String userId) {
        final int maxHistoryQuestions = 6;
        final int maxEachQuestionChars = 180;
        final int maxHistoryTotalChars = 1200;

        String current = userPrompt == null ? "" : userPrompt.trim();
        if (current.isEmpty()) current = "(空)";
        String currentForCompare = current.replaceAll("\\s+", " ").trim();

        if (sessionId == null || sessionId.isEmpty() || userId == null || userId.isEmpty()) {
            return "【当前用户询问（最重要）】\n" + current + "\n";
        }

        List<ChatRecord> history = chatRecordRepository.findByUserIdAndSessionIdOrderByMessageOrderAsc(userId, sessionId);
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

    private Prompt buildPrompt(String promptText, String sessionId, String userId, OpenAiChatOptions options) {
        return buildPrompt(promptText, sessionId, userId, options, null);
    }

    private Prompt buildPrompt(String promptText, String sessionId, String userId, OpenAiChatOptions options, String systemPrompt) {
        List<Message> messages = new ArrayList<>();
        
        // Add System Prompt if exists
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        }

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
                } else if (record.getSenderType() == 3) { // Command Result / System Feedback
                    // Feed command results back as User messages or System messages
                    // In agent loops, command results are usually treated as environment feedback (User-like)
                    messages.add(new UserMessage("Command execution result: " + record.getContent()));
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
