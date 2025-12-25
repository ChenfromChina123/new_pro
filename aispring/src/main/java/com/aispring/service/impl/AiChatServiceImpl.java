package com.aispring.service.impl;

import com.aispring.service.AiChatService;
import com.aispring.service.ToolsService;
import com.aispring.service.ToolCallParser;
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
    private final com.aispring.service.ToolCallParser toolCallParser;
    
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
    
    // Agent 循环配置（参考 void-main）
    private static final int CHAT_RETRIES = 3; // LLM 请求最大重试次数
    private static final long RETRY_DELAY_MS = 1000; // 重试延迟（毫秒）
    private static final int MAX_AGENT_LOOPS = 50; // Agent 循环最大次数

    public AiChatServiceImpl(ObjectProvider<ChatClient> chatClientProvider,
                             ObjectProvider<StreamingChatClient> streamingChatClientProvider,
                             ChatRecordRepository chatRecordRepository,
                             com.aispring.service.ChatRecordService chatRecordService, // 添加到构造函数
                             com.aispring.service.SessionStateService sessionStateService,
                             com.aispring.service.ToolApprovalService toolApprovalService,
                             com.aispring.service.ToolsService toolsService,
                             com.aispring.service.ToolCallParser toolCallParser,
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
        this.toolCallParser = toolCallParser;
        
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
    public SseEmitter askStream(String prompt, String sessionId, String model, Long userId) {
        return askAgentStream(prompt, sessionId, model, userId, null, null, null);
    }

    @Override
    public SseEmitter askAgentStream(String prompt, String sessionId, String model, Long userId, String systemPrompt, List<Map<String, Object>> tasks, Consumer<String> onResponse) {
        return askAgentStreamInternal(prompt, sessionId, model, userId, systemPrompt, tasks, onResponse);
    }


    /**
     * Agent 流式问答核心实现（参考 void-main 的 _runChatAgent）
     * 
     * 改进点：
     * 1. 更清晰的状态管理（idle -> LLM -> tool -> idle）
     * 2. 重试机制（LLM 请求失败时自动重试）
     * 3. 更好的错误处理
     * 4. 统一的中断机制
     */
    private SseEmitter askAgentStreamInternal(String initialPrompt, String sessionId, String model, Long userId, String initialSystemPrompt, List<Map<String, Object>> initialTasks, Consumer<String> onResponse) {
        // 创建SSE发射器，设置超时时间为5分钟（Agent 循环可能需要更长时间）
        SseEmitter emitter = new SseEmitter(300_000L);
        
        log.info("=== askAgentStreamInternal Called ===");
        log.info("Model: {}, SessionId: {}, UserId: {}", model, sessionId, userId);
        log.info("Initial Prompt: {}", initialPrompt);
        log.info("System Prompt length: {}", initialSystemPrompt != null ? initialSystemPrompt.length() : 0);
        if (initialSystemPrompt != null && initialSystemPrompt.length() > 0) {
            log.info("System Prompt preview (first 500 chars): {}", 
                initialSystemPrompt.substring(0, Math.min(500, initialSystemPrompt.length())));
        }
        
        // 生成循环 ID
        String loopId = java.util.UUID.randomUUID().toString();
        
        new Thread(() -> {
            try {
                log.info("=== Agent Loop Thread Started ===");
                com.aispring.entity.session.SessionState sessionState = 
                        sessionStateService.getOrCreateState(sessionId, userId);
                
                log.info("SessionState retrieved: status={}, sessionId={}", sessionState.getStatus(), sessionId);
                
                // 初始化状态
                sessionState.setStatus(com.aispring.entity.agent.AgentStatus.RUNNING);
                sessionState.setCurrentLoopId(loopId);
                sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                sessionStateService.saveState(sessionState);
                
                log.info("Agent 循环开始: sessionId={}, loopId={}, systemPrompt length={}", 
                    sessionId, loopId, initialSystemPrompt != null ? initialSystemPrompt.length() : 0);
                
                // 循环变量
                String currentPrompt = initialPrompt;
                String currentSystemPrompt = initialSystemPrompt;
                List<Map<String, Object>> currentTasks = initialTasks != null ? new ArrayList<>(initialTasks) : new ArrayList<>();
                int nMessagesSent = 0;
                boolean shouldSendAnotherMessage = true;
                com.aispring.entity.agent.AgentStatus finalStatus = com.aispring.entity.agent.AgentStatus.COMPLETED;
                
                // 主循环（参考 void-main 的 while (shouldSendAnotherMessage)）
                while (shouldSendAnotherMessage && nMessagesSent < MAX_AGENT_LOOPS) {
                    shouldSendAnotherMessage = false;
                    nMessagesSent++;
                    
                    log.debug("Agent Loop iteration {}: sessionId={}", nMessagesSent, sessionId);
                    
                    // 检查中断标志（在 idle 状态检查）
                    if (sessionStateService.isInterruptRequested(sessionId)) {
                        log.warn("Agent 循环被中断: sessionId={}, loopId={}, nMessagesSent={}", 
                                sessionId, loopId, nMessagesSent);
                        sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                        sessionStateService.saveState(sessionState);
                        emitter.send(SseEmitter.event()
                                .name("interrupt")
                                .data("{\"message\": \"Agent 循环已被用户中断\"}"));
                        break;
                    }
                    
                    // 设置状态为 idle（准备发送 LLM 请求）
                    sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                    sessionStateService.saveState(sessionState);
                    
                    // LLM 请求重试循环（参考 void-main 的 while (shouldRetryLLM)）
                    boolean shouldRetryLLM = true;
                    int nAttempts = 0;
                    String fullResponse = null;
                    boolean llmSuccess = false;
                    
                    while (shouldRetryLLM && nAttempts < CHAT_RETRIES) {
                        shouldRetryLLM = false;
                        nAttempts++;
                        
                        try {
                            // 设置状态为 streaming LLM
                            sessionState.setStreamState(com.aispring.entity.session.StreamState.streamingLLM());
                            sessionStateService.saveState(sessionState);
                    
                    // 执行对话并获取完整回复
                            log.info("准备调用 LLM: prompt length={}, systemPrompt length={}", 
                                currentPrompt != null ? currentPrompt.length() : 0,
                                currentSystemPrompt != null ? currentSystemPrompt.length() : 0);
                            fullResponse = performBlockingChat(currentPrompt, sessionId, model, userId, currentSystemPrompt, emitter);
                            log.info("LLM 响应完成: response length={}", fullResponse != null ? fullResponse.length() : 0);
                            llmSuccess = true;
                    
                    // Hook for capturing response
                    if (onResponse != null) {
                        try {
                            onResponse.accept(fullResponse);
                        } catch (Exception e) {
                                    log.error("Error in onResponse hook: {}", e.getMessage(), e);
                        }
                    }

                        } catch (Exception e) {
                            log.error("LLM 请求失败 (attempt {}/{}): {}", nAttempts, CHAT_RETRIES, e.getMessage(), e);
                            
                            // 检查是否需要重试
                            if (nAttempts < CHAT_RETRIES) {
                                shouldRetryLLM = true;
                                sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                                sessionStateService.saveState(sessionState);
                                
                                // 等待后重试
                                try {
                                    Thread.sleep(RETRY_DELAY_MS);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                
                                // 再次检查中断
                                if (sessionStateService.isInterruptRequested(sessionId)) {
                                    break;
                                }
                                continue;
                            } else {
                                // 重试次数用尽，发送错误消息
                                String errorMsg = "LLM 请求失败，已重试 " + CHAT_RETRIES + " 次: " + e.getMessage();
                                log.error(errorMsg);
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("{\"message\": \"" + errorMsg + "\"}"));
                                
                                sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                                sessionState.setStatus(com.aispring.entity.agent.AgentStatus.IDLE);
                                sessionStateService.saveState(sessionState);
                                
                                finalStatus = com.aispring.entity.agent.AgentStatus.IDLE;
                                        break;
                                    }
                                }
                    }
                    
                    // 如果 LLM 请求失败，退出循环
                    if (!llmSuccess || fullResponse == null) {
                        break;
                    }
                    
                    // 设置状态为 idle（LLM 响应完成）
                    sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                    sessionStateService.saveState(sessionState);
                    
                    // 解析回复，检查是否需要继续循环
                    // 使用 XML 格式解析工具调用（参考 void-main 的 extractXMLToolsWrapper）
                    try {
                        // 提取纯文本（移除工具调用部分）
                        List<String> availableTools = toolsService.getAvailableTools();
                        log.debug("可用工具列表: {}", availableTools);
                        log.debug("LLM 完整响应: {}", fullResponse);
                        
                        String plainText = toolCallParser.extractPlainText(fullResponse, availableTools);
                        log.debug("提取的纯文本: {}", plainText);
                        
                        // 尝试解析工具调用
                        ToolCallParser.ParsedToolCall parsedToolCall = toolCallParser.extractToolCall(fullResponse, availableTools);
                        log.info("工具调用解析结果: {}", parsedToolCall != null ? 
                            String.format("tool=%s, complete=%s, id=%s", 
                                parsedToolCall.getToolName(), 
                                parsedToolCall.isComplete(),
                                parsedToolCall.getToolId()) : "null");
                        
                        if (parsedToolCall != null && parsedToolCall.isComplete()) {
                            // 工具调用处理（参考 void-main 的 _runToolCall）
                            String toolName = parsedToolCall.getToolName();
                            String decisionId = parsedToolCall.getToolId();
                            Map<String, Object> unvalidatedParams = parsedToolCall.getRawParams();
                            
                            // 调用重构后的工具调用方法
                            ToolCallResult toolCallResult = runToolCall(
                                    toolName, 
                                    decisionId, 
                                    unvalidatedParams, 
                                    sessionId, 
                                    userId, 
                                    model, 
                                    sessionState, 
                                    emitter, 
                                    false // preapproved
                            );
                            
                            if (toolCallResult.isAwaitingApproval()) {
                                log.info("[Agent循环] 工具需要用户批准，暂停循环等待 - toolName={}, decisionId={}", 
                                        toolName, decisionId);
                                finalStatus = com.aispring.entity.agent.AgentStatus.AWAITING_APPROVAL;
                                // 不设置 shouldSendAnotherMessage，保持循环暂停
                                // 等待用户批准后，前端会检测到状态变化并重新启动Agent循环
                                shouldSendAnotherMessage = false;
                                
                                // 发送等待批准完成事件，通知前端
                                try {
                                    Map<String, Object> approvalCompleteData = new HashMap<>();
                                    approvalCompleteData.put("decisionId", decisionId);
                                    approvalCompleteData.put("decision_id", decisionId);
                                    approvalCompleteData.put("toolName", toolName);
                                    approvalCompleteData.put("tool", toolName);
                                    approvalCompleteData.put("status", "awaiting_approval");
                                    approvalCompleteData.put("message", String.format("工具 '%s' 等待用户批准", toolName));
                                    
                                    emitter.send(SseEmitter.event()
                                            .name("approval_required")
                                            .data(objectMapper.writeValueAsString(approvalCompleteData)));
                                    log.info("[Agent循环] 已发送等待批准事件 - toolName={}, decisionId={}", toolName, decisionId);
                                } catch (Exception e) {
                                    log.error("[Agent循环] 发送等待批准事件失败 - toolName={}", toolName, e);
                                }
                                
                                break;
                            }
                            
                            if (toolCallResult.isInterrupted()) {
                                break;
                            }
                            
                            if (toolCallResult.hasError()) {
                                shouldSendAnotherMessage = true;
                                currentPrompt = String.format("工具 '%s' 执行失败: %s。请修正后重试。", toolName, toolCallResult.getError());
                                continue;
                            }
                            
                            // 工具执行成功，继续循环
                            log.info("[Agent循环] 工具执行成功，准备继续循环 - toolName={}, decisionId={}, nMessagesSent={}", 
                                    toolName, decisionId, nMessagesSent);
                                shouldSendAnotherMessage = true;
                            currentPrompt = ""; // 空prompt，让LLM基于历史消息继续
                            log.info("[Agent循环] 已设置 shouldSendAnotherMessage=true，将在下一轮继续");
                                
                        } else {
                            // 没有检测到工具调用，检查是否有纯文本响应
                            if (plainText != null && !plainText.isEmpty()) {
                                // 纯文本响应，结束循环
                                log.info("[Agent循环] LLM 返回纯文本响应，结束循环 - plainTextLength={}", plainText.length());
                                shouldSendAnotherMessage = false;
                            } else {
                                log.info("[Agent循环] 未检测到工具调用和纯文本，结束循环");
                                shouldSendAnotherMessage = false;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error parsing agent response: {}", e.getMessage(), e);
                        // 解析错误不影响继续，但不再继续循环
                    }
                    
                    // 处理任务更新（保留兼容性）
                    try {
                        String jsonStr = extractJson(fullResponse);
                        if (jsonStr != null) {
                            JsonNode root = objectMapper.readTree(jsonStr);
                            String type = root.has("type") ? root.get("type").asText() : "";
                            
                            if ("task_update".equals(type)) {
                                // 任务状态更新
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
                                    shouldSendAnotherMessage = true;
                                    currentPrompt = String.format("任务 %s (ID: %s) 状态已更新为 %s。请继续执行该任务的具体操作，或进行下一步。", desc, taskId, status);
                                    currentSystemPrompt = updateSystemPromptWithTasks(currentSystemPrompt, currentTasks);
                                }
                            } else if ("TASK_LIST".equals(type) || "task_list".equals(type)) {
                                // 任务列表更新
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
                                    shouldSendAnotherMessage = true;
                                }
                            } else if ("TASK_COMPLETE".equals(type)) {
                                currentPrompt = "当前任务已完成。请检查是否还有剩余任务，如果有则继续，没有则结束。";
                                shouldSendAnotherMessage = true;
                            }
                        } else {
                            // 没有 JSON，纯文本响应，结束循环
                            log.debug("LLM 返回纯文本响应，结束 Agent 循环");
                        }
                    } catch (Exception e) {
                        log.error("Error parsing agent response: {}", e.getMessage(), e);
                        // 解析错误不影响继续，但不再继续循环
                    }
                }
                
                // 清理和结束
                log.info("[Agent循环] 循环结束，开始清理 - sessionId={}, nMessagesSent={}, finalStatus={}, shouldSendAnotherMessage={}", 
                        sessionId, nMessagesSent, finalStatus, shouldSendAnotherMessage);
                
                com.aispring.entity.session.SessionState finalState = sessionStateService.getState(sessionId).orElse(null);
                if (finalState != null) {
                    // 如果状态是等待批准，保持状态不变，不清理loopId
                    if (finalStatus == com.aispring.entity.agent.AgentStatus.AWAITING_APPROVAL) {
                        log.info("[Agent循环] 保持等待批准状态，不清理 - sessionId={}", sessionId);
                        // 保持状态，等待用户批准
                    } else {
                        // 其他状态，正常清理
                        if (finalState.getStatus() == com.aispring.entity.agent.AgentStatus.RUNNING) {
                            finalState.setStatus(finalStatus);
                        }
                        finalState.setCurrentLoopId(null);
                        finalState.setStreamState(com.aispring.entity.session.StreamState.idle());
                        sessionStateService.saveState(finalState);
                        log.info("[Agent循环] 最终状态已保存 - sessionId={}, finalStatus={}", sessionId, finalStatus);
                    }
                }
                
                // 只有在非等待批准状态时才发送完成事件
                if (finalStatus != com.aispring.entity.agent.AgentStatus.AWAITING_APPROVAL) {
                    log.info("[Agent循环] 发送完成事件 - sessionId={}", sessionId);
                    try {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                        log.info("[Agent循环] 完成事件已发送 - sessionId={}", sessionId);
                    } catch (Exception e) {
                        log.error("[Agent循环] 发送完成事件失败 - sessionId={}", sessionId, e);
                    }
                } else {
                    log.info("[Agent循环] 等待批准中，不发送完成事件，保持连接 - sessionId={}", sessionId);
                    // 保持SSE连接打开，等待用户批准
                }
                
            } catch (Exception e) {
                log.error("Agent 循环异常: sessionId={}", sessionId, e);
                handleError(emitter, e);
            }
        }).start();
        
        return emitter;
    }

    /**
     * 提取 JSON（参考 void-main 的解析机制，支持多种格式）
     * 改进：更准确地提取工具调用 JSON，支持各种可能的格式
     */
    private String extractJson(String content) {
        if (content == null || content.isEmpty()) return null;
        
        // 方式1: 查找 ```json 代码块（最优先，最可靠）
        int codeBlockStart = content.indexOf("```json");
        if (codeBlockStart != -1) {
            int jsonStart = codeBlockStart + 7; // Length of "```json"
            int codeBlockEnd = content.indexOf("```", jsonStart);
            if (codeBlockEnd > jsonStart) {
                String json = content.substring(jsonStart, codeBlockEnd).trim();
                if (!json.isEmpty()) {
                    return json;
                }
            }
        }
        
        // 方式2: 查找 ``` 代码块（可能是其他语言标记，但内容是 JSON）
        codeBlockStart = content.indexOf("```");
        if (codeBlockStart != -1) {
            int jsonStart = content.indexOf("\n", codeBlockStart);
            if (jsonStart < 0) jsonStart = codeBlockStart + 3;
            int codeBlockEnd = content.indexOf("```", jsonStart);
            if (codeBlockEnd > jsonStart) {
                String json = content.substring(jsonStart, codeBlockEnd).trim();
                // 检查是否是 JSON（以 { 或 [ 开头）
                if ((json.startsWith("{") || json.startsWith("[")) && 
                    (json.endsWith("}") || json.endsWith("]"))) {
                    return json;
                }
            }
        }
        
        // 方式3: 查找第一个完整的 JSON 对象（从第一个 { 到匹配的 }）
        // 使用栈来匹配括号，确保提取完整的 JSON
        int startObj = content.indexOf("{");
        if (startObj >= 0) {
            int braceCount = 0;
            int endObj = startObj;
            for (int i = startObj; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') braceCount++;
                else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endObj = i;
                        break;
                    }
                }
            }
            if (braceCount == 0 && endObj > startObj) {
                String json = content.substring(startObj, endObj + 1).trim();
                // 验证是否是有效的 JSON 格式
                if (json.startsWith("{") && json.endsWith("}")) {
                    return json;
                }
            }
        }
        
        // 方式4: 查找第一个完整的 JSON 数组（从第一个 [ 到匹配的 ]）
        int startArr = content.indexOf("[");
        if (startArr >= 0) {
            int bracketCount = 0;
            int endArr = startArr;
            for (int i = startArr; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '[') bracketCount++;
                else if (c == ']') {
                    bracketCount--;
                    if (bracketCount == 0) {
                        endArr = i;
                        break;
                    }
                }
            }
            if (bracketCount == 0 && endArr > startArr) {
                String json = content.substring(startArr, endArr + 1).trim();
                if (json.startsWith("[") && json.endsWith("]")) {
                    return json;
                }
            }
        }
        
        // 方式5: 如果整个内容看起来像 JSON
        String trimmed = content.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        
        log.debug("Failed to extract JSON from content (length: {}): {}", 
                content.length(), 
                content.length() > 200 ? content.substring(0, 200) + "..." : content);
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

    private String performBlockingChat(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter) throws IOException {
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
    
    private void performBlockingSpringAiChat(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent) {
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

    private void performBlockingOkHttpChat(String prompt, String sessionId, String model, Long userId, String systemPrompt, SseEmitter emitter, StringBuilder fullContent) throws IOException {
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
                 // 参考 void-main：工具结果（senderType=3）作为用户消息反馈给 LLM
                 if (record.getSenderType() == 1 || record.getSenderType() == 3) {
                     msg.put("role", "user");
                 } else {
                     msg.put("role", "assistant");
                 }
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
    private void generateTitleAndSuggestionsAsync(String userPrompt, String sessionId, Long userId, SseEmitter emitter) {
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
    private String buildTitleAndSuggestionsUserPrompt(String userPrompt, String sessionId, Long userId) {
        final int maxHistoryQuestions = 6;
        final int maxEachQuestionChars = 180;
        final int maxHistoryTotalChars = 1200;
        
        String current = userPrompt == null ? "" : userPrompt.trim();
        if (current.isEmpty()) current = "(空)";
        String currentForCompare = current.replaceAll("\\s+", " ").trim();
        
        if (sessionId == null || sessionId.isEmpty() || userId == null) {
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

    private Prompt buildPrompt(String promptText, String sessionId, Long userId, OpenAiChatOptions options) {
        return buildPrompt(promptText, sessionId, userId, options, null);
    }

    private Prompt buildPrompt(String promptText, String sessionId, Long userId, OpenAiChatOptions options, String systemPrompt) {
        List<Message> messages = new ArrayList<>();
        
        // Add System Prompt if exists
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            log.debug("添加系统提示词到消息列表: length={}", systemPrompt.length());
            messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        } else {
            log.warn("系统提示词为空或null！这可能导致 AI 不使用工具！");
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
                } else if (record.getSenderType() == 3) { // Tool Result / System Feedback
                    // 参考 void-main：工具结果作为用户消息反馈给 LLM（环境反馈）
                    // 格式：清晰标识这是工具执行结果，让 LLM 知道这是环境反馈
                    String toolResultContent = record.getContent();
                    if (record.getExitCode() != null && record.getExitCode() == 0) {
                        // 成功：直接显示结果，LLM 会自动理解这是工具执行结果
                        messages.add(new UserMessage(toolResultContent));
                    } else {
                        // 失败：明确标识错误
                        String errorMsg = toolResultContent;
                        if (record.getStderr() != null && !record.getStderr().isEmpty()) {
                            errorMsg = toolResultContent + "\n错误信息: " + record.getStderr();
                        }
                        messages.add(new UserMessage(errorMsg));
                    }
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
    public String ask(String prompt, String sessionId, String model, Long userId) {
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
    
    /**
     * 工具调用结果类（参考 void-main 的 _runToolCall 返回值）
     */
    private static class ToolCallResult {
        private final boolean awaitingApproval;
        private final boolean interrupted;
        private final String error;
        
        private ToolCallResult(boolean awaitingApproval, boolean interrupted, String error) {
            this.awaitingApproval = awaitingApproval;
            this.interrupted = interrupted;
            this.error = error;
        }
        
        public static ToolCallResult success() {
            return new ToolCallResult(false, false, null);
        }
        
        public static ToolCallResult awaitingApproval() {
            return new ToolCallResult(true, false, null);
        }
        
        public static ToolCallResult interrupted() {
            return new ToolCallResult(false, true, null);
        }
        
        public static ToolCallResult error(String error) {
            return new ToolCallResult(false, false, error);
        }
        
        public boolean isAwaitingApproval() {
            return awaitingApproval;
        }
        
        public boolean isInterrupted() {
            return interrupted;
        }
        
        public boolean hasError() {
            return error != null;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * 执行工具调用（参考 void-main 的 _runToolCall 方法）
     * 
     * 流程：
     * 1. 参数验证
     * 2. 检查是否需要批准
     * 3. 执行工具（设置状态为 running）
     * 4. 处理结果并字符串化
     * 5. 添加到消息历史
     * 
     * @param toolName 工具名称
     * @param toolId 工具调用ID（decisionId）
     * @param unvalidatedParams 未验证的参数
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param model 模型名称
     * @param sessionState 会话状态
     * @param emitter SSE发射器
     * @param preapproved 是否已预批准
     * @return 工具调用结果
     */
    /**
     * 执行工具调用（参考 void-main 的 _runToolCall 方法，简化日志）
     */
    private ToolCallResult runToolCall(
            String toolName,
            String toolId,
            Map<String, Object> unvalidatedParams,
            String sessionId,
            Long userId,
            String model,
            com.aispring.entity.session.SessionState sessionState,
            SseEmitter emitter,
            boolean preapproved
    ) {
        log.info("[工具调用] 开始 - toolName={}, toolId={}, sessionId={}", toolName, toolId, sessionId);
        
        Map<String, Object> validatedParams;
        ToolsService.ToolResult toolResult;
        String toolResultStr;
        
        try {
            // 步骤 1: 参数验证
            if (!preapproved) {
                String validationError = toolsService.validateParams(toolName, unvalidatedParams);
                if (validationError != null) {
                    log.warn("[工具调用] 参数验证失败 - toolName={}, error={}", toolName, validationError);
                    try {
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("tool", toolName);
                        errorData.put("toolName", toolName);
                        errorData.put("error", validationError);
                        errorData.put("decision_id", toolId);
                        errorData.put("decisionId", toolId);
                        errorData.put("type", "invalid_params");
                        emitter.send(SseEmitter.event()
                                .name("tool_error")
                                .data(objectMapper.writeValueAsString(errorData)));
                    } catch (Exception e) {
                        log.error("[工具调用] 发送错误事件失败 - toolName={}", toolName, e);
                    }
                    return ToolCallResult.error(validationError);
                }
                validatedParams = unvalidatedParams;
            } else {
                validatedParams = unvalidatedParams;
            }
            
            // 步骤 2: 检查批准
            boolean requiresApproval = toolApprovalService.requiresApproval(userId, toolName);
            if (requiresApproval && !preapproved) {
                log.info("[工具调用] 需要用户批准 - toolName={}, toolId={}", toolName, toolId);
                
                // 先创建批准请求（确保数据库已保存）
                Long approvalId = toolApprovalService.createApprovalRequest(sessionId, userId, toolName, unvalidatedParams, toolId);
                log.info("[工具调用] 批准请求已创建 - approvalId={}, toolName={}, toolId={}", approvalId, toolName, toolId);
                
                // 获取待批准列表（确保数据已保存）
                List<com.aispring.entity.approval.ToolApproval> pendingApprovals = 
                        toolApprovalService.getPendingApprovals(sessionId);
                log.info("[工具调用] 当前待批准数量: {}", pendingApprovals.size());
                
                // 发送等待批准事件（包含完整的待批准列表数据）
                try {
                    Map<String, Object> approvalData = new HashMap<>();
                    approvalData.put("decision_id", toolId);
                    approvalData.put("decisionId", toolId);
                    approvalData.put("tool", toolName);
                    approvalData.put("toolName", toolName);
                    approvalData.put("params", unvalidatedParams);
                    approvalData.put("type", "tool_request");
                    approvalData.put("sessionId", sessionId);
                    approvalData.put("message", String.format("工具 '%s' 需要您的批准", toolName));
                    approvalData.put("approvalId", approvalId);
                    
                    // 包含待批准列表（前端可以直接使用，无需再次调用API）
                    List<Map<String, Object>> pendingList = new ArrayList<>();
                    for (com.aispring.entity.approval.ToolApproval approval : pendingApprovals) {
                        Map<String, Object> approvalMap = new HashMap<>();
                        approvalMap.put("id", approval.getDecisionId());
                        approvalMap.put("decisionId", approval.getDecisionId());
                        approvalMap.put("toolName", approval.getToolName());
                        approvalMap.put("params", approval.getToolParams());
                        approvalMap.put("createdAt", approval.getCreatedAt().toString());
                        pendingList.add(approvalMap);
                    }
                    approvalData.put("pendingApprovals", pendingList);
                    
                    String approvalJson = objectMapper.writeValueAsString(approvalData);
                    log.info("[工具调用] 发送等待批准事件 - toolName={}, toolId={}, jsonLength={}, pendingCount={}", 
                            toolName, toolId, approvalJson.length(), pendingList.size());
                    
                    emitter.send(SseEmitter.event()
                            .name("waiting_approval")
                            .data(approvalJson));
                    
                    log.info("[工具调用] 等待批准事件已发送 - toolName={}, toolId={}", toolName, toolId);
                } catch (Exception e) {
                    log.error("[工具调用] 发送批准事件失败 - toolName={}, toolId={}, error={}", 
                            toolName, toolId, e.getMessage(), e);
                }
                
                // 更新状态为等待批准
                sessionState.setStatus(com.aispring.entity.agent.AgentStatus.AWAITING_APPROVAL);
                sessionState.setStreamState(com.aispring.entity.session.StreamState.awaitingUser(toolName, unvalidatedParams, toolId));
                sessionStateService.saveState(sessionState);
                log.info("[工具调用] 状态已更新为等待批准 - toolName={}, toolId={}", toolName, toolId);
                
                return ToolCallResult.awaitingApproval();
            } else if (!preapproved) {
                toolApprovalService.createApprovalRequest(sessionId, userId, toolName, unvalidatedParams, toolId);
                toolApprovalService.approveToolCall(toolId, "自动批准（根据用户设置）");
            }
            
            // 步骤 3: 执行工具
            sessionState.setStreamState(com.aispring.entity.session.StreamState.runningTool(toolName, validatedParams, toolId));
            sessionStateService.saveState(sessionState);
            
            // 发送工具运行中事件（前端需要这个事件来显示"执行中"状态）
            try {
                Map<String, Object> runningData = new HashMap<>();
                runningData.put("toolName", toolName);
                runningData.put("tool", toolName);
                runningData.put("params", validatedParams);
                runningData.put("decisionId", toolId);
                runningData.put("decision_id", toolId);
                runningData.put("type", "running_now");
                runningData.put("content", "(执行中...)");
                emitter.send(SseEmitter.event()
                        .name("tool_running")
                        .data(objectMapper.writeValueAsString(runningData)));
            } catch (Exception e) {
                log.warn("[工具调用] 发送运行中事件失败（非关键） - toolName={}", toolName);
            }
            
            long startTime = System.currentTimeMillis();
            try {
                toolResult = toolsService.callTool(toolName, validatedParams, userId, sessionId);
                long duration = System.currentTimeMillis() - startTime;
                log.info("[工具调用] 执行完成 - toolName={}, success={}, duration={}ms", 
                        toolName, toolResult.isSuccess(), duration);
            } catch (Exception e) {
                log.error("[工具调用] 执行异常 - toolName={}, error={}", toolName, e.getMessage(), e);
                toolResult = ToolsService.ToolResult.error("工具执行异常: " + e.getMessage());
            }
            
            // 步骤 4: 字符串化结果
            toolResultStr = toolResult.isSuccess() 
                    ? toolResult.getStringResult() 
                    : (toolResult.getError() != null ? toolResult.getError() : toolResult.getStringResult());
            
            // 步骤 5: 发送工具结果给前端
            log.info("[工具调用] 准备发送结果事件 - toolName={}, toolId={}, success={}", 
                    toolName, toolId, toolResult.isSuccess());
            try {
                Map<String, Object> toolResultData = new HashMap<>();
                toolResultData.put("toolName", toolName);
                toolResultData.put("tool", toolName);
                toolResultData.put("params", validatedParams);
                toolResultData.put("decisionId", toolId);
                toolResultData.put("decision_id", toolId);
                toolResultData.put("success", toolResult.isSuccess());
                toolResultData.put("stringResult", toolResultStr);
                toolResultData.put("result", toolResultStr);
                toolResultData.put("error", toolResult.getError());
                toolResultData.put("data", toolResult.getData());
                toolResultData.put("type", toolResult.isSuccess() ? "success" : "tool_error");
                
                String resultJson = objectMapper.writeValueAsString(toolResultData);
                log.info("[工具调用] 发送结果事件 - toolName={}, toolId={}, jsonLength={}", 
                        toolName, toolId, resultJson.length());
                
                emitter.send(SseEmitter.event()
                        .name("tool_result")
                        .data(resultJson));
                
                log.info("[工具调用] 结果事件已发送 - toolName={}, toolId={}", toolName, toolId);
            } catch (Exception e) {
                log.error("[工具调用] 发送结果事件失败 - toolName={}, toolId={}, error={}", 
                        toolName, toolId, e.getMessage(), e);
            }
            
            // 步骤 6: 保存到消息历史
            String toolResultMessage = toolCallParser.formatToolResult(toolName, toolResultStr);
            try {
                chatRecordService.createChatRecord(
                        toolResultMessage, 1, userId, sessionId, model,
                        "completed", "terminal", null,
                        toolResult.isSuccess() ? 0 : -1,
                        toolResult.getStringResult(),
                        toolResult.getError()
                );
            } catch (Exception e) {
                log.error("[工具调用] 保存历史失败 - toolName={}", toolName, e);
            }
            
            // 步骤 7: 更新状态为idle（工具执行完成）
            log.info("[工具调用] 更新状态为idle - toolName={}, toolId={}", toolName, toolId);
            sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
            sessionStateService.saveState(sessionState);
            log.info("[工具调用] 状态已更新为idle - toolName={}, toolId={}", toolName, toolId);
            
            log.info("[工具调用] 工具调用完成，返回成功 - toolName={}, toolId={}, success={}", 
                    toolName, toolId, toolResult.isSuccess());
            return ToolCallResult.success();
            
        } catch (Exception e) {
            log.error("[工具调用] 异常 - toolName={}, error={}", toolName, e.getMessage(), e);
            try {
                sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                sessionStateService.saveState(sessionState);
            } catch (Exception stateError) {
                log.error("[工具调用] 更新状态失败 - toolName={}", toolName, stateError);
            }
            return ToolCallResult.error("工具调用异常: " + e.getMessage());
        }
    }
}
