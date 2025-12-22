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
- **Operating System**: %s (CRITICAL: 必须使用适配此操作系统的命令)
- **Dedicated Storage Root**: `%s`
- **Current Working Directory (CWD)**: `%s`
- **User Permission**: Restricted (Sandbox Mode, Quota: 1GB, Max Depth: 10)

# Current Task Context
%s

# Command Style (PowerShell/Windows)
由于运行环境是 Windows，你必须使用 PowerShell 兼容的命令：
- **列出目录**: 使用 `dir` 或 `Get-ChildItem` (不要用 `ls -la`)。
- **创建目录**: 使用 `mkdir` (PowerShell 别名) 或 `New-Item -ItemType Directory`。
- **文件操作**: 使用 `cp`, `mv`, `rm` (PowerShell 别名) 或对应的 PowerShell 命令。
- **内容查看**: 使用 `cat` (PowerShell 别名) 或 `Get-Content`。
- **路径分隔符**: 必须使用反斜杠 `\\` 或正斜杠 `/` (PowerShell 支持两者)，但命令内部参数建议使用反斜杠。
- **禁止交互**: 命令必须是非交互式的（如 `npm init -y`, `rm -Force`）。
- **多命令执行**: **严禁使用 `&&` 分隔符**。在 PowerShell 中请使用分号 `;` 分隔命令（例如：`cd path; dir`）或分步执行。
- **错误处理**: 忽略已存在的目录错误（如 `mkdir -Force` 或先检查是否存在）。
- **执行外部程序**: 执行当前目录下的程序请使用 `.\\program.exe`。

# Capabilities & Tools
你可以使用以下工具（通过特定的输出格式调用）：
1. **execute_command**: 执行 Shell 命令。
2. **read_file**: 读取文件内容。
3. **write_file**: 创建或覆盖文件。
4. **list_files**: 列出目录内容。

# Requirement Documents (CRITICAL)
- **存储位置**: 所有需求文档必须存储在 `/requirements/` 目录下。
- **文件格式**: 必须使用 Markdown (`.md`) 格式。
- **生成逻辑**: 当用户要求生成需求文档时，你应当分析需求，然后使用 `write_file` 工具将文档写入 `/requirements/` 目录。
- **调用逻辑**: 你可以随时使用 `read_file` 读取已有的需求文档，以获取上下文或进行修改。
- **文件命名**: 使用清晰的英文或拼音命名，例如 `user_auth_system.md`。

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
4. **Output Constraint**:
   - **必须且只能输出 JSON 格式**。
   - 严禁在 JSON 之外包含任何 Markdown 标记（如 ```json ... ```）或解释性文字。

# Interaction Protocol
1. **Analyze**: 分析意图。
2. **Plan**: 如果任务复杂，先生成任务列表。
3. **Execute**: 生成工具调用代码。
4. **Feedback**: 根据结果调整。如果命令执行失败，请分析错误信息并尝试修复（例如检查路径是否存在、参数是否正确），不要重复执行相同的错误命令。

# Output Format (Strict JSON Only)
请直接输出以下 JSON 对象，不要使用 Markdown 代码块包裹。每次响应必须包含 content（必填，至少 200 字符，支持 Markdown）、thought（可选）、steps（可选）以及 tool（如有）。

**关于 `steps` 字段的重要说明**：
- 仅当用户提出明确的**任务需求**（如“创建一个项目”、“修复这个bug”）时，才需要包含 `steps` 字段来展示执行步骤。
- 如果用户只是进行**普通对话**（如“你是谁”、“你好”）、**询问信息**或**没有具体操作需求**时，**请勿包含** `steps` 字段。

1. **普通思考/对话 (无 steps)**:
{
  "thought": "思考过程，包含推理细节...",
  "content": "详细的解释、结果或回复（至少 200 字符）。\n\n支持 **Markdown** 格式。"
}

2. **执行任务 (包含 steps)**:
{
  "thought": "思考过程...",
  "content": "任务执行结果...",
  "steps": ["分析需求", "执行操作"]
}

3. **生成任务列表**:
{
  "thought": "任务较多，先规划...",
  "type": "task_list",
  "tasks": [
    {"id": 1, "desc": "创建项目结构", "status": "pending"},
    {"id": 2, "desc": "写入配置文件", "status": "pending"}
  ],
  "content": "根据您的需求，我制定了以下任务计划：\n\n1. 创建项目结构...\n2. 写入配置文件..."
}

4. **更新任务状态**:
{
  "thought": "第一步完成...",
  "type": "task_update",
  "taskId": 1,
  "status": "completed", // or "in_progress"
  "content": "任务 1 已完成。接下来我们将..."
}

5. **调用工具**:
{
  "thought": "执行命令...",
  "tool": "execute_command",
  "command": "ls -F",
  "content": "正在执行命令以查看文件列表...",
  "steps": ["检查当前目录", "执行 ls 命令"] // 仅在属于任务一部分时包含
}

6. **写文件**:
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
        private List<Map<String, Object>> tasks;
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
        String escapedCwd = cwd.replace("\\", "/"); // Frontend friendly
        
        // 构建当前任务链上下文
        StringBuilder taskContext = new StringBuilder();
        if (request.getTasks() != null && !request.getTasks().isEmpty()) {
            taskContext.append("当前任务链状态：\n");
            for (Map<String, Object> task : request.getTasks()) {
                taskContext.append(String.format("- [%s] %s (ID: %s)\n", 
                    task.get("status"), task.get("desc"), task.get("id")));
            }
        } else {
            taskContext.append("当前暂无进行中的任务链。");
        }

        // 使用虚拟根路径 "/" 代替物理路径，防止泄露
        String virtualRoot = "/";
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, os, virtualRoot, escapedCwd, taskContext.toString(), virtualRoot);

        // 如果是 Windows 环境，追加具体的 PowerShell 提示
        if (os.toLowerCase().contains("win")) {
            systemPrompt = systemPrompt.replace("%s (CRITICAL: 必须使用适配此操作系统的命令)", os + " (PowerShell Environment)");
        }

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

    @GetMapping("/read-file")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> readFile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                      @RequestParam String path) {
        String content = terminalService.readFile(currentUser.getUser().getId(), path);
        return ApiResponse.success(content);
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
