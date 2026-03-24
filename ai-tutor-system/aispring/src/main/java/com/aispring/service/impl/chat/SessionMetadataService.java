package com.aispring.service.impl.chat;

import com.aispring.service.ChatRecordService;
import com.aispring.entity.ChatSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话元数据服务
 * 负责生成会话标题和建议问题
 */
@Component
@Slf4j
public class SessionMetadataService {

    private final ObjectMapper objectMapper;
    private static final AtomicInteger BG_THREAD_SEQ = new AtomicInteger(1);

    private final ChatClient deepseekChatClient;
    private final ChatRecordService chatRecordService;
    private final ChatHistoryBuilder chatHistoryBuilder;
    private final ExecutorService backgroundExecutor;

    @Value("${ai.max-tokens:4096}")
    private Integer maxTokens;

    public SessionMetadataService(
            ObjectMapper objectMapper,
            ChatClient deepseekChatClient,
            ChatRecordService chatRecordService,
            ChatHistoryBuilder chatHistoryBuilder,
            @org.springframework.beans.factory.annotation.Qualifier("backgroundExecutor") ExecutorService backgroundExecutor) {
        this.objectMapper = objectMapper;
        this.deepseekChatClient = deepseekChatClient;
        this.chatRecordService = chatRecordService;
        this.chatHistoryBuilder = chatHistoryBuilder;
        this.backgroundExecutor = backgroundExecutor;
    }

    /**
     * 异步生成会话标题和建议问题
     * @param userPrompt 用户输入
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param emitter SSE 发射器（可为 null）
     * @param model 模型名称
     */
    public void generateTitleAndSuggestionsAsync(String userPrompt, String sessionId,
                                                  Long userId, SseEmitter emitter, String model) {
        backgroundExecutor.execute(() -> {
            try {
                generateTitleAndSuggestions(userPrompt, sessionId, userId, emitter, model);
            } catch (Exception e) {
                log.error("Error generating title and suggestions: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 生成标题和建议的核心逻辑
     */
    private void generateTitleAndSuggestions(String userPrompt, String sessionId,
                                             Long userId, SseEmitter emitter, String model) throws Exception {
        if (deepseekChatClient == null) return;
        if (sessionId == null || sessionId.isEmpty()) return;

        boolean needTitle = checkNeedTitle(sessionId);
        String systemPrompt = buildSystemPrompt(needTitle);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel((model == null || model.isBlank()) ? "deepseek-v3.2" : model)
                .withTemperature(0.3f)
                .build();

        String userPromptWithHistory = chatHistoryBuilder.buildTitleAndSuggestionsUserPrompt(userPrompt, sessionId, userId);
        List<Message> messages = List.of(
                new org.springframework.ai.chat.messages.SystemMessage(systemPrompt),
                new UserMessage(userPromptWithHistory)
        );

        ChatResponse response = deepseekChatClient.call(new Prompt(messages, options));
        String content = response.getResult().getOutput().getContent();

        parseAndSaveResult(content, needTitle, sessionId, userId, emitter);
    }

    /**
     * 检查是否需要生成标题
     */
    private boolean checkNeedTitle(String sessionId) {
        Optional<ChatSession> sessionOpt = chatRecordService.getChatSession(sessionId);
        return sessionOpt.isEmpty() ||
               sessionOpt.get().getTitle() == null ||
               "新对话".equals(sessionOpt.get().getTitle()) ||
               sessionOpt.get().getTitle().isEmpty();
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(boolean needTitle) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个中文助手，需要基于【当前用户询问】（最重要）以及【历史用户询问】（仅供参考）生成结果。\n")
          .append("仅输出 JSON，不要输出任何额外文字（包括 Markdown/代码块）。\n")
          .append("请生成 3 个\"用户视角\"的下一步追问（用户对助手说的话），要求：\n")
          .append("1) 每个都是完整问题，优先更具体、更可执行；\n")
          .append("2) 不要以 AI 口吻表达（如\"我可以为你.../我还能...\"），不要自称\"AI/助手\"；\n")
          .append("3) 不要复述历史问题，不要照抄历史原句；\n")
          .append("4) 每个问题 8~25 个汉字，末尾使用\"？\"。\n");

        if (needTitle) {
            sb.append("由于这是会话的第一条消息，请同时生成一个简短的标题（不超过15个字）。\n");
        }

        sb.append("请严格按照以下 JSON 格式返回，不要包含任何其他文字：\n")
          .append("{\n");
        if (needTitle) {
            sb.append("  \"title\": \"标题内容\",\n");
        }
        sb.append("  \"suggestions\": [\"问题1\", \"问题2\", \"问题3\"]\n")
          .append("}");

        return sb.toString();
    }

    /**
     * 解析并保存结果
     */
    private void parseAndSaveResult(String content, boolean needTitle, String sessionId,
                                    Long userId, SseEmitter emitter) throws Exception {
        int jsonStart = content.indexOf("{");
        int jsonEnd = content.lastIndexOf("}");
        if (jsonStart < 0 || jsonEnd <= jsonStart) return;

        String jsonStr = content.substring(jsonStart, jsonEnd + 1);
        JsonNode root = objectMapper.readTree(jsonStr);

        String title = needTitle ? root.path("title").asText() : null;
        List<String> suggestionsList = parseSuggestions(root);

        String suggestionsJson = objectMapper.writeValueAsString(suggestionsList);
        chatRecordService.updateSessionTitleAndSuggestions(sessionId, title, suggestionsJson, userId);

        sendSseEvent(sessionId, title, suggestionsList, emitter);
    }

    /**
     * 解析建议问题列表
     */
    private List<String> parseSuggestions(JsonNode root) {
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

        return suggestionsList;
    }

    /**
     * 发送 SSE 事件
     */
    private void sendSseEvent(String sessionId, String title, List<String> suggestionsList, SseEmitter emitter) {
        if (emitter == null) return;

        try {
            Map<String, Object> sseData = new HashMap<>();
            sseData.put("type", "session_update");
            sseData.put("session_id", sessionId);
            if (title != null) sseData.put("title", title);
            sseData.put("suggestions", suggestionsList);
            emitter.send(SseEmitter.event().name("session_update").data(objectMapper.writeValueAsString(sseData)));
        } catch (IllegalStateException | java.io.IOException ex) {
            log.warn("Client disconnected during suggestion stream: {}", ex.getMessage());
        } catch (Exception ex) {
            log.debug("Failed to send SSE event: {}", ex.getMessage());
        }
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        backgroundExecutor.shutdownNow();
    }
}
