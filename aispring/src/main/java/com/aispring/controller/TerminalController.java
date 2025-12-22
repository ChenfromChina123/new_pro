package com.aispring.controller;

import com.aispring.dto.request.TerminalCommandRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.dto.response.TerminalFileDto;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;
    private final AiChatService aiChatService;
    private final ChatRecordService chatRecordService;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
# Role
你是一个运行在专用文件夹系统中的智能终端助手 (AI Terminal Agent)。你的目标是根据用户的自然语言指令，通过执行终端命令来协助用户完成文件管理、代码构建、系统运维等任务。

# Environment Context
- **Operating System**: %s
- **Dedicated Storage Root**: `%s`
- **Current Working Directory (CWD)**: `%s`
- **User Permission**: Restricted (Sandbox Mode, Quota: 1GB, Max Depth: 10)

# Capabilities & Tools
你可以使用以下工具（通过特定的输出格式调用）：
1. **execute_command**: 执行 Shell 命令。
2. **read_file**: 读取文件内容。
3. **write_file**: 创建或覆盖文件。
4. **list_files**: 列出目录内容。

# Constraints & Safety Rules (CRITICAL)
1. **Directory Isolation**: 
   - 你只能在 `%s` 及其子目录下操作。
   - 严禁访问父级目录 (`..`) 或系统敏感路径。
2. **Path Display**:
   - 在输出中显示的路径必须简化，仅显示相对于用户根目录的路径（例如 `/src/main` 或 `/`）。
   - 禁止显示物理绝对路径。
3. **Task Management**:
   - 在执行复杂任务（涉及多个步骤）前，必须先生成详细的任务列表。
   - 每完成一个步骤，需实时更新任务状态。

# Interaction Protocol
1. **Analyze**: 分析意图。
2. **Plan**: 如果任务复杂，先生成任务列表。
3. **Execute**: 生成工具调用代码。
4. **Feedback**: 根据结果调整。

# Output Format (JSON Only)
请使用以下 JSON 格式输出（不要输出 Markdown）：

1. **普通思考/对话**:
{
  "thought": "思考过程...",
  "message": "回复给用户的内容"
}

2. **生成任务列表**:
{
  "thought": "任务较多，先规划...",
  "type": "task_list",
  "tasks": [
    {"id": 1, "desc": "创建项目结构", "status": "pending"},
    {"id": 2, "desc": "写入配置文件", "status": "pending"}
  ]
}

3. **更新任务状态**:
{
  "thought": "第一步完成...",
  "type": "task_update",
  "taskId": 1,
  "status": "completed"
}

4. **调用工具**:
{
  "thought": "执行命令...",
  "tool": "execute_command",
  "command": "ls -F"
}

5. **写文件**:
{
  "thought": "需要创建 index.html 并写入内容。",
  "tool": "write_file",
  "path": "index.html",
  "overwrite": false,
  "content": "<<<<AI_FILE_CONTENT_BEGIN>>>>\\n...文件正文...\\n<<<<AI_FILE_CONTENT_END>>>>",
  "message": "已生成并写入 index.html"
}
""";

    @Data
    public static class TerminalChatRequest {
        @NotBlank(message = "提示词不能为空")
        private String prompt;
        private String session_id;
        private String model;
    }

    @Data
    public static class TerminalWriteFileRequest {
        @NotBlank(message = "文件路径不能为空")
        private String path;
        private String content;
        private String cwd;
        private Boolean overwrite;
    }

    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter chatStream(@AuthenticationPrincipal CustomUserDetails currentUser,
                                 @Valid @RequestBody TerminalChatRequest request) {
        Long userId = currentUser.getUser().getId();
        String rootPath = terminalService.getUserTerminalRoot(userId);
        String os = System.getProperty("os.name");
        
        // Get CWD from session
        String cwd = "/";
        if (request.getSession_id() != null) {
            Optional<ChatSession> session = chatRecordService.getChatSession(request.getSession_id());
            if (session.isPresent() && session.get().getCurrentCwd() != null) {
                cwd = session.get().getCurrentCwd();
            }
        }
        
        // Escape backslashes for JSON/String format in prompt
        String escapedRootPath = rootPath.replace("\\", "\\\\");
        String escapedCwd = cwd.replace("\\", "/"); // Frontend friendly
        
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, os, escapedRootPath, escapedCwd, escapedRootPath);

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
        
        // Update session CWD
        if (request.getSessionId() != null && response.getCwd() != null) {
            chatRecordService.updateSessionCwd(request.getSessionId(), response.getCwd(), currentUser.getUser().getId().toString());
        }
        
        return ApiResponse.success(response);
    }

    @PostMapping("/write-file")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TerminalCommandResponse> writeFile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                          @Valid @RequestBody TerminalWriteFileRequest request) {
        boolean overwrite = request.getOverwrite() != null && request.getOverwrite();
        TerminalCommandResponse response = terminalService.writeFile(
                currentUser.getUser().getId(),
                request.getPath(),
                request.getContent(),
                request.getCwd(),
                overwrite
        );
        return ApiResponse.success(response);
    }
    
    @GetMapping("/files")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TerminalFileDto>> listFiles(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                      @RequestParam(required = false, defaultValue = "") String path) {
        List<TerminalFileDto> files = terminalService.listFiles(currentUser.getUser().getId(), path);
        return ApiResponse.success(files);
    }

    @PostMapping("/new-session")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChatSession> createNewSession(@AuthenticationPrincipal CustomUserDetails currentUser) {
        ChatSession session = chatRecordService.createTerminalSession(currentUser.getUser().getId().toString());
        return ApiResponse.success(session);
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatSession>> getSessions(@AuthenticationPrincipal CustomUserDetails currentUser) {
        List<ChatSession> sessions = chatRecordService.getTerminalSessions(currentUser.getUser().getId().toString());
        return ApiResponse.success(sessions);
    }

    @GetMapping("/history/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatRecord>> getHistory(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                  @PathVariable String sessionId) {
        List<ChatRecord> history = chatRecordService.getSessionMessages(currentUser.getUser().getId().toString(), sessionId);
        return ApiResponse.success(history);
    }

    @PostMapping("/save-record")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> saveRecord(@AuthenticationPrincipal CustomUserDetails currentUser,
                                      @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        String content = (String) request.get("content");
        Integer senderType = (Integer) request.get("sender_type"); 
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

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteSession(@AuthenticationPrincipal CustomUserDetails currentUser,
                                         @PathVariable String sessionId) {
        chatRecordService.deleteSession(currentUser.getUser().getId().toString(), sessionId);
        return ApiResponse.success(null);
    }
}
