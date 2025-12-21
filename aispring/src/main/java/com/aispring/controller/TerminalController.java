package com.aispring.controller;

import com.aispring.dto.request.TerminalCommandRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.security.CustomUserDetails;
import com.aispring.service.AiChatService;
import com.aispring.service.TerminalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aispring.entity.ChatRecord;
import com.aispring.entity.ChatSession;
import com.aispring.service.ChatRecordService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;
    private final AiChatService aiChatService;
    private final ChatRecordService chatRecordService;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
# Role
你是一个运行在安全沙箱环境中的智能终端助手 (AI Terminal Agent)。你的目标是根据用户的自然语言指令，通过执行终端命令来协助用户完成文件管理、代码构建、系统运维等任务。

# Environment Context
- **Operating System**: %s
- **Current Working Directory (CWD)**: `%s`
- **User Permission**: Restricted (Sandbox Mode)

# Capabilities & Tools
你可以使用以下工具（通过特定的输出格式调用）：
1. **execute_command**: 执行 Shell 命令。
   - Linux: bash/sh
   - Windows: PowerShell/CMD
2. **read_file**: 读取文件内容。
3. **write_file**: 创建或覆盖文件。
4. **list_files**: 列出目录内容。

# Constraints & Safety Rules (CRITICAL)
1. **Directory Isolation**: 
   - 你只能在 `%s` 及其子目录下操作。
   - 严禁访问父级目录 (`..`) 或系统敏感路径 (e.g., `/etc`, `C:\\Windows`)。
2. **Destructive Actions**: 
   - 对于删除 (rm/del)、覆盖 (overwrite) 等高风险操作，必须先向用户请求确认，除非用户明确表示“强制”或“自动确认”。
3. **OS Adaptation**:
   - 根据 OS 类型自动选择正确的命令语法。
   - 例如：Linux 使用 `ls -la`, Windows PowerShell 使用 `ls` 或 `dir`。
   - 路径分隔符：Linux 使用 `/`, Windows 使用 `\\` (但在 PowerShell 中 `/` 通常也可通用)。
4. **Efficiency**:
   - 优先使用单条组合命令完成任务（如 `mkdir -p path && touch file`）。
   - 如果任务复杂，请分步骤执行并向用户汇报进度。

# Interaction Protocol
1. **Analyze**: 首先分析用户意图。
2. **Plan**: 思考需要执行的命令序列。
3. **Execute**: 生成工具调用代码。
4. **Feedback**: 根据执行结果（Stdout/Stderr）决定下一步操作或向用户汇报。

# Output Format
请使用以下 JSON 格式输出你的思考和行动（不要输出其他 Markdown 格式）：
{
  "thought": "用户想要创建一个新的 Vue 项目，我需要先检查当前目录下是否已存在同名文件夹。",
  "command": "ls -F",
  "tool": "execute_command"
}

如果无需执行命令（例如需要用户确认或回答问题），请输出：
{
  "thought": "用户询问天气，我无法直接查询，但可以建议...",
  "message": "抱歉，我只能执行终端命令，无法查询实时天气。"
}
""";

    @Data
    public static class TerminalChatRequest {
        @NotBlank(message = "提示词不能为空")
        private String prompt;
        private String session_id;
        private String model;
    }

    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter chatStream(@AuthenticationPrincipal CustomUserDetails currentUser,
                                 @Valid @RequestBody TerminalChatRequest request) {
        Long userId = currentUser.getUser().getId();
        String rootPath = terminalService.getUserTerminalRoot(userId);
        String os = System.getProperty("os.name");
        
        // Escape backslashes for JSON/String format in prompt
        String escapedRootPath = rootPath.replace("\\", "\\\\");
        
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, os, escapedRootPath, escapedRootPath);

        return aiChatService.askAgentStream(
                request.getPrompt(),
                request.getSession_id(),
                request.getModel(),
                String.valueOf(userId),
                systemPrompt
        );
    }

    @PostMapping("/execute")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TerminalCommandResponse> executeCommand(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                               @RequestBody TerminalCommandRequest request) {
        TerminalCommandResponse response = terminalService.executeCommand(currentUser.getUser().getId(), request.getCommand(), request.getCwd());
        return ApiResponse.success(response);
    }

    /**
     * 获取所有终端会话
     */
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatSession>> getSessions(@AuthenticationPrincipal CustomUserDetails currentUser) {
        List<ChatSession> sessions = chatRecordService.getTerminalSessions(currentUser.getUser().getId().toString());
        return ApiResponse.success(sessions);
    }

    /**
     * 获取终端会话历史消息
     */
    @GetMapping("/history/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatRecord>> getHistory(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                  @PathVariable String sessionId) {
        List<ChatRecord> history = chatRecordService.getSessionMessages(currentUser.getUser().getId().toString(), sessionId);
        return ApiResponse.success(history);
    }

    /**
     * 保存终端聊天记录
     */
    @PostMapping("/save-record")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> saveRecord(@AuthenticationPrincipal CustomUserDetails currentUser,
                                      @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        String content = (String) request.get("content");
        Integer senderType = (Integer) request.get("sender_type"); // 1: user, 2: AI, 3: Command Result
        String model = (String) request.get("model");

        chatRecordService.createChatRecord(
            content,
            senderType,
            currentUser.getUser().getId().toString(),
            sessionId,
            model,
            "completed",
            "terminal"
        );
        return ApiResponse.success(null);
    }
}
