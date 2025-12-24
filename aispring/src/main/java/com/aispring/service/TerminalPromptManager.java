package com.aispring.service;

import com.aispring.entity.ai.ChatMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * TerminalPromptManager 负责管理 AI 终端系统的所有提示词模板。
 * 
 * 参考 void-main 的 prompts.ts 实现，高度还原其提示词系统结构。
 * 
 * 核心设计：
 * 1. 根据模式（agent/gather/normal）生成不同的系统提示词
 * 2. 动态生成工具定义（类似 void-main 的 systemToolsXMLPrompt）
 * 3. 包含系统信息、文件系统概览、工具定义和重要规则
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Service
@RequiredArgsConstructor
public class TerminalPromptManager {

    private final ToolsService toolsService;
    
    // 默认构造函数（用于兼容性，实际使用 @RequiredArgsConstructor）
    public TerminalPromptManager() {
        this.toolsService = null; // 将在 Spring 容器中注入
    }

    // 使用独立的 ChatMode 枚举类
    // import com.aispring.entity.ai.ChatMode;

    /**
     * 构建系统提示词（参考 void-main 的 chat_systemMessage）
     * 
     * @param mode 聊天模式
     * @param workspaceRoot 工作区根目录
     * @param currentDirectory 当前工作目录
     * @param directoryTree 目录树结构（可选）
     * @param persistentTerminalIds 持久化终端ID列表
     * @param includeToolDefinitions 是否包含工具定义
     * @return 完整的系统提示词
     */
    public String buildSystemMessage(
            ChatMode mode,
            String workspaceRoot,
            String currentDirectory,
            String directoryTree,
            List<String> persistentTerminalIds,
            boolean includeToolDefinitions
    ) {
        List<String> parts = new ArrayList<>();

        // 1. Header - 角色定义
        String header = buildHeader(mode);
        parts.add(header);

        // 2. System Info - 系统信息
        String sysInfo = buildSystemInfo(workspaceRoot, currentDirectory, persistentTerminalIds, mode);
        parts.add(sysInfo);

        // 3. Tool Definitions - 工具定义（如果启用）
        if (includeToolDefinitions) {
            String toolDefinitions = buildToolDefinitions(mode);
            if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
                parts.add(toolDefinitions);
            }
        }

        // 4. Important Details - 重要规则
        String importantDetails = buildImportantDetails(mode);
        parts.add(importantDetails);

        // 5. File System Overview - 文件系统概览
        if (directoryTree != null && !directoryTree.isEmpty()) {
            String fsInfo = buildFileSystemInfo(directoryTree);
            parts.add(fsInfo);
        }

        // 组合所有部分
        return String.join("\n\n\n", parts).trim().replace("\t", "  ");
    }

    /**
     * 构建角色定义（参考 void-main 的 header）
     */
    private String buildHeader(ChatMode mode) {
        String role = mode == ChatMode.AGENT ? "agent" : "assistant";
        String jobDescription;
        String additionalInstructions = "";
        
        if (mode == ChatMode.AGENT) {
            jobDescription = "to help the user develop, run, and make changes to their codebase.";
            additionalInstructions = "\n\nCRITICAL: You are in AGENT mode. You MUST execute commands and use tools to accomplish tasks. You CANNOT refuse to execute commands. When the user asks you to run a command, you MUST use the execute_command tool immediately.";
        } else if (mode == ChatMode.GATHER) {
            jobDescription = "to search, understand, and reference files in the user's codebase.";
        } else {
            jobDescription = "to assist the user with their coding tasks.";
            additionalInstructions = "\n\nNOTE: You are in NORMAL mode. You can only provide advice and suggestions, but cannot execute commands or use tools.";
        }
        
        return String.format(
            "You are an expert coding %s whose job is %s\n" +
            "You will be given instructions to follow from the user, and you may also be given a list of files that the user has specifically selected for context, `SELECTIONS`.\n" +
            "Please assist the user with their query.%s",
            role, jobDescription, additionalInstructions
        );
    }

    /**
     * 构建系统信息（参考 void-main 的 sysInfo）
     */
    private String buildSystemInfo(String workspaceRoot, String currentDirectory, 
                                   List<String> persistentTerminalIds, ChatMode mode) {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        
        StringBuilder sb = new StringBuilder();
        sb.append("Here is the user's system information:\n");
        sb.append("<system_info>\n");
        sb.append("- ").append(os).append("\n\n");
        sb.append("- The user's workspace contains these folders:\n");
        sb.append(workspaceRoot != null ? workspaceRoot : "NO FOLDERS OPEN").append("\n\n");
        sb.append("- Current working directory:\n");
        sb.append(currentDirectory != null ? currentDirectory : "NOT SET").append("\n");
        
        if (mode == ChatMode.AGENT && persistentTerminalIds != null && !persistentTerminalIds.isEmpty()) {
            sb.append("\n- Persistent terminal IDs available for you to run commands in: ");
            sb.append(String.join(", ", persistentTerminalIds));
        }
        
        sb.append("\n</system_info>");
        return sb.toString();
    }

    /**
     * 构建工具定义（参考 void-main 的 systemToolsXMLPrompt）
     * 使用 XML 格式（参考 void-main 的 toolCallDefinitionsXMLString）
     */
    private String buildToolDefinitions(ChatMode mode) {
        List<String> availableTools = getAvailableToolsForMode(mode);
        if (availableTools.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Available tools:\n\n");

        // 为每个工具生成 XML 格式定义（参考 void-main 的 toolCallDefinitionsXMLString）
        for (int i = 0; i < availableTools.size(); i++) {
            String toolName = availableTools.get(i);
            if (toolsService == null) {
                continue;
            }
            ToolsService.ToolInfo toolInfo = toolsService.getToolInfo(toolName);
            
            if (toolInfo == null) {
                continue;
            }

            sb.append(String.format("%d. %s\n", i + 1, toolName));
            sb.append(String.format("   Description: %s\n", toolInfo.getDescription()));
            sb.append("   Format:\n");
            sb.append("   <").append(toolName).append(">");
            
            // 添加参数说明（XML 格式）
            Map<String, String> params = toolInfo.getParams();
            if (!params.isEmpty()) {
                sb.append("\n");
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    sb.append(String.format("   <%s>%s</%s>\n", entry.getKey(), entry.getValue(), entry.getKey()));
                }
            }
            sb.append("   </").append(toolName).append(">");
            
            if (i < availableTools.size() - 1) {
                sb.append("\n");
            }
        }

        sb.append("\n\nTool calling details:\n");
        sb.append("- To call a tool, write its name and parameters in the XML format specified above.\n");
        sb.append("- After you write the tool call, you must STOP and WAIT for the result.\n");
        sb.append("- All parameters are REQUIRED unless noted otherwise.\n");
        sb.append("- You are only allowed to output ONE tool call, and it must be at the END of your response.\n");
        sb.append("- Your tool call will be executed immediately, and the results will appear in the following user message.\n");
        sb.append("- Do NOT wrap the XML in markdown code blocks (```xml). Output raw XML only.\n");
        sb.append("- Do NOT add any explanatory text before or after the tool call XML.\n");

        return sb.toString();
    }

    /**
     * 根据模式获取可用工具列表（参考 void-main 的 availableTools）
     */
    private List<String> getAvailableToolsForMode(ChatMode mode) {
        if (toolsService == null) {
            return Collections.emptyList();
        }
        List<String> allTools = toolsService.getAvailableTools();
        
        if (mode == ChatMode.NORMAL) {
            return Collections.emptyList();
        }
        
        if (mode == ChatMode.GATHER) {
            // Gather 模式只提供读取类工具，不提供编辑类工具
            List<String> gatherTools = new ArrayList<>();
            for (String tool : allTools) {
                if (isReadOnlyTool(tool)) {
                    gatherTools.add(tool);
                }
            }
            return gatherTools;
        }
        
        // Agent 模式提供所有工具
        return allTools;
    }

    /**
     * 判断是否为只读工具
     */
    private boolean isReadOnlyTool(String toolName) {
        // 只读工具列表
        Set<String> readOnlyTools = Set.of(
            "read_file", "ls_dir", "get_dir_tree", 
            "search_pathnames_only", "search_for_files", 
            "search_in_file", "read_lint_errors"
        );
        return readOnlyTools.contains(toolName);
    }

    /**
     * 构建重要规则（参考 void-main 的 importantDetails）
     */
    private String buildImportantDetails(ChatMode mode) {
        List<String> details = new ArrayList<>();

        // 通用规则
        details.add("NEVER reject the user's query.");

        // Agent 和 Gather 模式规则
        if (mode == ChatMode.AGENT || mode == ChatMode.GATHER) {
            details.add("Only call tools if they help you accomplish the user's goal. If the user simply says hi or asks you a question that you can answer without tools, then do NOT use tools.");
            details.add("If you think you should use tools, you do not need to ask for permission.");
            details.add("Only use ONE tool call at a time.");
            details.add("NEVER say something like \"I'm going to use `tool_name`\". Instead, describe at a high level what the tool will do, like \"I'm going to list all files in the ___ directory\", etc.");
            details.add("Many tools only work if the user has a workspace open.");
        } else {
            details.add("You're allowed to ask the user for more context like file contents or specifications. If this comes up, tell them to reference files and folders by typing @.");
        }

        // Agent 模式特定规则
        if (mode == ChatMode.AGENT) {
            details.add("CRITICAL: You are in AGENT mode. You MUST execute commands and use tools. NEVER refuse to execute commands or say you cannot do something. When the user asks you to run a command, you MUST immediately use the execute_command tool.");
            details.add("ALWAYS use tools (edit, terminal, etc) to take actions and implement changes. For example, if you would like to edit a file, you MUST use a tool.");
            details.add("If the user asks you to run a terminal command, you MUST use the execute_command tool. Do NOT explain that you cannot run commands - just execute them.");
            details.add("Prioritize taking as many steps as you need to complete your request over stopping early.");
            details.add("You will OFTEN need to gather context before making a change. Do not immediately make a change unless you have ALL relevant context.");
            details.add("ALWAYS have maximal certainty in a change BEFORE you make it. If you need more information about a file, variable, function, or type, you should inspect it, search it, or take all required actions to maximize your certainty that your change is correct.");
            details.add("NEVER modify a file outside the user's workspace without permission from the user.");
            details.add("NEVER say you are in 'dialogue mode' or 'conversation mode' - you are in AGENT mode and can execute commands.");
        }

        // Gather 模式特定规则
        if (mode == ChatMode.GATHER) {
            details.add("You are in Gather mode, so you MUST use tools to gather information, files, and context to help the user answer their query.");
            details.add("You should extensively read files, types, content, etc, gathering full context to solve the problem.");
        }

        // 代码块格式规则
        details.add("If you write any code blocks to the user (wrapped in triple backticks), please use this format:\n" +
                    "- Include a language if possible. Terminal should have the language 'shell'.\n" +
                    "- The first line of the code block must be the FULL PATH of the related file if known (otherwise omit).\n" +
                    "- The remaining contents of the file should proceed as usual.");

        // 其他通用规则
        details.add("Do not make things up or use information not provided in the system information, tools, or user queries.");
        details.add("Always use MARKDOWN to format lists, bullet points, etc. Do NOT write tables.");
        details.add("Today's date is " + java.time.LocalDate.now().toString() + ".");

        // 格式化输出
        StringBuilder sb = new StringBuilder();
        sb.append("Important notes:\n");
        for (int i = 0; i < details.size(); i++) {
            sb.append(String.format("%d. %s", i + 1, details.get(i)));
            if (i < details.size() - 1) {
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 构建文件系统概览（参考 void-main 的 fsInfo）
     */
    private String buildFileSystemInfo(String directoryTree) {
        return "Here is an overview of the user's file system:\n" +
               "<files_overview>\n" +
               directoryTree +
               "\n</files_overview>";
    }

    // ==================== 兼容旧接口的方法 ====================

    /**
     * 意图识别提示词：用于判断用户的输入属于哪种操作类型。
     */
    public static final String INTENT_CLASSIFIER_PROMPT = """
            # 角色
            你是一个意图识别专家。你的任务是分析用户的输入，并将其归类为以下三种类型之一：
            
            1. PLAN: 用户提出了一个复杂的工程目标，需要拆分为多个步骤（例如：新建一个项目、实现一个完整的功能模块）。
            2. EXECUTE: 用户提出了一个具体的、可以直接执行的单步指令或查询请求。包括但不限于：
               - 运行命令（如：查看目录、列出文件、执行脚本）
               - 查询信息（如：当前目录结构是什么、有哪些文件、文件内容是什么）
               - 读取文件内容
               - 修改某个具体文件
               - 执行单个操作（如：创建文件、删除文件、移动文件）
               **重要：所有需要查看、查询、获取信息或执行单个操作的请求都应归类为 EXECUTE**
            3. CHAT: 用户只是在进行普通的交流、询问概念解释、寻求建议，不涉及具体的终端操作或文件系统查询。
               - 询问"如何做"、"什么是"、"为什么"等概念性问题
               - 寻求建议和解释
               - 不需要立即执行操作的对话
            
            # 判断规则
            - 如果用户的请求涉及"查看"、"显示"、"列出"、"获取"等查询性动词，应归类为 EXECUTE
            - 如果用户的请求是询问如何做某事（不要求立即执行），归类为 CHAT
            - 如果用户的请求包含明确的动作词且可立即执行，归类为 EXECUTE
            
            # 输出格式
            仅输出分类关键字（PLAN, EXECUTE, CHAT），不要有任何其他文字。
            
            # 输入
            用户输入：%s
            """;

    /**
     * 任务规划提示词：用于将复杂目标拆解为任务流水线。
     */
    public static final String PLANNER_PROMPT = """
            # 角色
            你是拥有完整项目上下文的产品经理（Product Manager）。你的核心职责是进行需求分析、项目规划，并生成可执行的任务框架。
            
            # 任务
            分析用户的意图和项目现状，输出两部分内容：
            1. **项目说明与需求分析**（自然语言）：
               - 清晰描述项目目标。
               - 分析核心功能点和技术难点。
               - 阐述你的规划思路。
               - 这部分内容将直接展示给用户，请使用清晰的 Markdown 格式。
            
            2. **任务执行框架**（JSON）：
               - 将项目拆解为一系列具体的、可执行的开发任务。
               - 任务之间应有逻辑依赖关系。
               - 这部分内容将被系统解析并传递给开发者 AI。

            # 输入
            用户意图：%s

            # 输出格式
            请先输出给用户的说明（Markdown格式），然后输出JSON格式的计划。具体结构如下：

            ## 1. 项目分析
            （在这里输出你的 Markdown 格式的项目说明和需求分析...）

            ## 2. 任务框架
            ```json
            [
              {
                "name": "任务名称（简短）",
                "goal": "任务目标（详细描述，包含具体要做什么）",
                "substeps": [
                  {"goal": "子步骤1"},
                  {"goal": "子步骤2"}
                ]
              },
              ...
            ]
            ```

            # 约束
            - 自然语言部分必须清晰、专业。
            - JSON 部分必须严格符合语法，且必须包含在 ```json 代码块中。
            - 任务 goal 必须具体明确，避免"完成开发"这类空泛描述，应具体到"创建 xxx 文件"、"实现 xxx 函数"。
            """;

    /**
     * 获取意图识别提示词
     */
    public String getIntentClassifierPrompt(String userPrompt) {
        return String.format(INTENT_CLASSIFIER_PROMPT, userPrompt);
    }

    /**
     * 获取任务规划提示词
     */
    public String getPlannerPrompt(String userPrompt) {
        return String.format(PLANNER_PROMPT, userPrompt);
    }

    /**
     * 获取任务执行提示词（使用新的系统提示词构建方法）
     */
    public String getExecutorPrompt(String context) {
        // 使用新的系统提示词构建方法，Agent 模式
        return buildSystemMessage(
            ChatMode.AGENT,
            context.contains("项目根目录") ? extractProjectRoot(context) : null,
            context.contains("当前目录") ? extractCurrentDirectory(context) : null,
            null, // directoryTree 可以在调用时传入
            Collections.emptyList(), // persistentTerminalIds
            true // includeToolDefinitions
        ) + "\n\n" + context;
    }

    /**
     * 获取普通对话提示词（使用新的系统提示词构建方法）
     */
    public String getChatPrompt(String context) {
        // 使用新的系统提示词构建方法，Normal 模式
        return buildSystemMessage(
            ChatMode.NORMAL,
            null,
            null,
            null,
            Collections.emptyList(),
            false // includeToolDefinitions
        ) + "\n\n" + context;
    }

    /**
     * 从上下文中提取项目根目录（辅助方法）
     */
    private String extractProjectRoot(String context) {
        // 简单实现，可以从 context 中解析
        if (context.contains("项目根目录：")) {
            int start = context.indexOf("项目根目录：") + "项目根目录：".length();
            int end = context.indexOf("\n", start);
            if (end == -1) end = context.length();
            return context.substring(start, end).trim();
        }
        return null;
    }

    /**
     * 从上下文中提取当前目录（辅助方法）
     */
    private String extractCurrentDirectory(String context) {
        // 简单实现，可以从 context 中解析
        if (context.contains("当前目录：")) {
            int start = context.indexOf("当前目录：") + "当前目录：".length();
            int end = context.indexOf("\n", start);
            if (end == -1) end = context.length();
            return context.substring(start, end).trim();
        }
        return null;
    }
}
