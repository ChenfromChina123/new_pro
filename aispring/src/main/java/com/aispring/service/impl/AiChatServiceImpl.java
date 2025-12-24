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
    public SseEmitter askStream(String prompt, String sessionId, String model, String userId) {
        return askAgentStream(prompt, sessionId, model, userId, null, null, null);
    }

    @Override
    public SseEmitter askAgentStream(String prompt, String sessionId, String model, String userId, String systemPrompt, List<Map<String, Object>> tasks, Consumer<String> onResponse) {
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
    private SseEmitter askAgentStreamInternal(String initialPrompt, String sessionId, String model, String userId, String initialSystemPrompt, List<Map<String, Object>> initialTasks, Consumer<String> onResponse) {
        // 创建SSE发射器，设置超时时间为5分钟（Agent 循环可能需要更长时间）
        SseEmitter emitter = new SseEmitter(300_000L);
        
        log.info("=== askAgentStream Called ===");
        log.info("Model: {}, SessionId: {}", model, sessionId);
        
        // 生成循环 ID
        String loopId = java.util.UUID.randomUUID().toString();
        
        new Thread(() -> {
            try {
                Long userIdLong = Long.valueOf(userId);
                com.aispring.entity.session.SessionState sessionState = 
                        sessionStateService.getOrCreateState(sessionId, userIdLong);
                
                // 初始化状态
                sessionState.setStatus(com.aispring.entity.agent.AgentStatus.RUNNING);
                sessionState.setCurrentLoopId(loopId);
                sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                sessionStateService.saveState(sessionState);
                
                log.info("Agent 循环开始: sessionId={}, loopId={}", sessionId, loopId);
                
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
                            fullResponse = performBlockingChat(currentPrompt, sessionId, model, userId, currentSystemPrompt, emitter);
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
                            
                            log.info("检测到 XML 工具调用: {}, decisionId={}, params={}", toolName, decisionId, unvalidatedParams);
                                
                                // 1. 参数验证
                                String validationError = toolsService.validateParams(toolName, unvalidatedParams);
                                if (validationError != null) {
                                    log.warn("工具参数验证失败: {}, error: {}", toolName, validationError);
                                    emitter.send(SseEmitter.event()
                                            .name("tool_error")
                                            .data(objectMapper.writeValueAsString(Map.of(
                                                    "tool", toolName,
                                                    "error", validationError,
                                                    "decision_id", decisionId
                                            ))));
                                    // 继续循环，让 LLM 知道参数错误
                                    shouldSendAnotherMessage = true;
                                    currentPrompt = String.format("工具 '%s' 参数验证失败: %s。请修正参数后重试。", toolName, validationError);
                                    continue;
                                }
                                
                                // 2. 检查批准（参考 void-main 的自动批准逻辑）
                                boolean requiresApproval = toolApprovalService.requiresApproval(userIdLong, toolName);
                                
                                if (requiresApproval) {
                                    log.info("工具需要批准: {}", toolName);
                                    toolApprovalService.createApprovalRequest(sessionId, userIdLong, toolName, unvalidatedParams, decisionId);
                                    
                                    // 发送等待批准事件
                                    emitter.send(SseEmitter.event()
                                            .name("waiting_approval")
                                            .data(objectMapper.writeValueAsString(Map.of(
                                                    "decision_id", decisionId,
                                                    "tool", toolName,
                                                    "params", unvalidatedParams
                                            ))));
                                    
                                    // 更新状态为等待批准
                                    sessionState.setStatus(com.aispring.entity.agent.AgentStatus.AWAITING_APPROVAL);
                                    sessionState.setStreamState(com.aispring.entity.session.StreamState.awaitingUser(toolName, unvalidatedParams, decisionId));
                                    sessionStateService.saveState(sessionState);
                                    
                                    finalStatus = com.aispring.entity.agent.AgentStatus.AWAITING_APPROVAL;
                                    // 不继续循环，等待用户批准
                                    break;
                                } else {
                                    log.info("工具自动批准: {}", toolName);
                                    // 自动批准：创建批准记录但标记为已批准（用于审计）
                                    toolApprovalService.createApprovalRequest(sessionId, userIdLong, toolName, unvalidatedParams, decisionId);
                                    toolApprovalService.approveToolCall(decisionId, "自动批准（根据用户设置）");
                                }
                                
                                // 3. 执行工具
                                log.info("执行工具: {}", toolName);
                                
                                // 设置状态为 running tool
                                sessionState.setStreamState(com.aispring.entity.session.StreamState.runningTool(toolName, unvalidatedParams, decisionId));
                                sessionStateService.saveState(sessionState);
                                
                                ToolsService.ToolResult result = toolsService.callTool(toolName, unvalidatedParams, userIdLong, sessionId);
                                
                                // 4. 发送工具结果给前端
                                emitter.send(SseEmitter.event()
                                        .name("tool_result")
                                        .data(objectMapper.writeValueAsString(result)));
                                
                                // 5. 将工具结果保存到消息历史（参考 void-main 的机制）
                                // void-main 中，工具结果格式化为 XML 并作为用户消息添加到历史
                                // 格式：<toolName_result>...</toolName_result>
                                String toolResultContent = result.isSuccess() 
                                    ? result.getStringResult() 
                                    : (result.getError() != null ? result.getError() : result.getStringResult());
                                
                                // 使用 XML 格式（参考 void-main 的 formatToolResult）
                                String toolResultMessage = toolCallParser.formatToolResult(toolName, toolResultContent);
                                
                                // 保存工具结果到消息历史（作为用户消息，参考 void-main 的 prepareMessages_XML_tools）
                                // void-main 中，工具结果被添加到用户消息中
                                try {
                                    chatRecordService.createChatRecord(
                                        toolResultMessage,
                                        1, // User message (工具结果作为用户消息反馈给 LLM)
                                        userId,
                                        sessionId,
                                        model,
                                        "completed",
                                        "terminal",
                                        null, // reasoning_content
                                        result.isSuccess() ? 0 : -1, // exit_code
                                        result.getStringResult(), // stdout
                                        result.getError() // stderr
                                    );
                                    log.info("工具结果已保存到消息历史（XML格式）: tool={}, success={}", toolName, result.isSuccess());
                                } catch (Exception e) {
                                    log.error("保存工具结果到消息历史失败: {}", e.getMessage(), e);
                                }
                                
                                // 6. 继续循环（参考 void-main：工具结果已在历史中，LLM 自动看到）
                                // void-main 中，工具结果作为用户消息，LLM 会自动处理
                                // 不需要额外的 prompt，直接继续循环
                                shouldSendAnotherMessage = true;
                                currentPrompt = ""; // 空 prompt，让 LLM 基于历史消息继续
                                
                                // 设置状态为 idle（工具执行完成）
                                sessionState.setStreamState(com.aispring.entity.session.StreamState.idle());
                                sessionStateService.saveState(sessionState);
                                
                        } else {
                            // 没有检测到工具调用，检查是否有纯文本响应
                            if (plainText != null && !plainText.isEmpty()) {
                                // 纯文本响应，结束循环
                                log.debug("LLM 返回纯文本响应，结束 Agent 循环");
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
                com.aispring.entity.session.SessionState finalState = sessionStateService.getState(sessionId).orElse(null);
                if (finalState != null) {
                    if (finalState.getStatus() == com.aispring.entity.agent.AgentStatus.RUNNING) {
                        finalState.setStatus(finalStatus);
                    }
                    finalState.setCurrentLoopId(null);
                    finalState.setStreamState(com.aispring.entity.session.StreamState.idle());
                    sessionStateService.saveState(finalState);
                }
                
                log.info("Agent 循环结束: sessionId={}, nMessagesSent={}, finalStatus={}", 
                        sessionId, nMessagesSent, finalStatus);
                
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
                
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
