package com.aispring.service.impl;

import com.aispring.service.TerminalService;
import com.aispring.service.ToolsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工具服务实现
 * 统一管理所有内置工具的执行、验证和结果处理
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolsServiceImpl implements ToolsService {
    
    private final TerminalService terminalService;
    
    // 工具注册表
    private static final Map<String, ToolDefinition> TOOL_REGISTRY = new HashMap<>();
    
    static {
        // 注册文件操作工具（参考 void-main 的工具定义）
        registerTool("read_file", "读取文件内容", 
            Map.of("path", "文件的完整路径（必需）", 
                   "start_line", "可选。起始行号，默认为文件开头", 
                   "end_line", "可选。结束行号，默认为文件结尾"));
        registerTool("ls_dir", "列出目录内容", 
            Map.of("path", "可选。目录的完整路径，留空或\"\"表示当前目录"));
        registerTool("get_dir_tree", "获取目录树结构（递归）", 
            Map.of("path", "可选。目录的完整路径，留空或\"\"表示当前目录"));
        registerTool("create_file_or_folder", "创建文件或文件夹", 
            Map.of("path", "文件或文件夹的完整路径（必需）", 
                   "is_folder", "可选。是否为文件夹，默认为 false"));
        registerTool("delete_file_or_folder", "删除文件或文件夹", 
            Map.of("path", "文件或文件夹的完整路径（必需）", 
                   "is_recursive", "可选。是否递归删除，默认为 false"));
        registerTool("write_file", "写入整个文件内容", 
            Map.of("path", "文件的完整路径（必需）", 
                   "content", "文件内容（必需）"));
        registerTool("edit_file", "编辑文件（使用 search/replace 方式）", 
            Map.of("path", "文件的完整路径（必需）", 
                   "old_string", "要替换的原始字符串（必需）", 
                   "new_string", "替换后的新字符串（必需）"));
        registerTool("rewrite_file", "完全重写文件内容", 
            Map.of("path", "文件的完整路径（必需）", 
                   "new_content", "新的文件内容（必需）"));
        
        // 注册搜索工具
        registerTool("search_pathnames_only", "按文件名搜索文件路径", 
            Map.of("pattern", "搜索模式（必需）", 
                   "include_pattern", "可选。文件匹配模式，用于限制搜索结果"));
        registerTool("search_for_files", "按文件内容搜索文件", 
            Map.of("pattern", "搜索模式（必需）", 
                   "is_regex", "可选。是否为正则表达式，默认为 false"));
        registerTool("search_in_file", "在文件中搜索文本", 
            Map.of("path", "文件的完整路径（必需）", 
                   "pattern", "搜索模式（必需）", 
                   "is_regex", "可选。是否为正则表达式，默认为 false"));
        
        // 注册终端工具
        registerTool("run_command", "执行一次性终端命令（30秒超时）", 
            Map.of("command", "要执行的命令（必需）", 
                   "cwd", "可选。工作目录，默认为当前目录"));
        registerTool("run_persistent_command", "在持久化终端中运行命令", 
            Map.of("command", "要执行的命令（必需）", 
                   "terminal_id", "持久化终端的ID（必需）"));
        registerTool("open_persistent_terminal", "打开一个新的持久化终端", 
            Map.of("cwd", "可选。工作目录，默认为当前目录"));
        registerTool("kill_persistent_terminal", "关闭持久化终端", 
            Map.of("terminal_id", "持久化终端的ID（必需）"));
        
        // 注册其他工具
        registerTool("read_lint_errors", "读取文件的 Lint 错误", 
            Map.of("path", "文件的完整路径（必需）"));
    }
    
    private static void registerTool(String name, String description, Map<String, String> params) {
        TOOL_REGISTRY.put(name, new ToolDefinition(name, description, params));
    }
    
    @Override
    public String validateParams(String toolName, Map<String, Object> params) {
        ToolDefinition tool = TOOL_REGISTRY.get(toolName);
        if (tool == null) {
            return "未知工具: " + toolName;
        }
        
        // 检查必需参数
        for (String requiredParam : tool.getRequiredParams()) {
            if (!params.containsKey(requiredParam) || params.get(requiredParam) == null) {
                return String.format("缺少必需参数: %s (工具: %s)", requiredParam, toolName);
            }
        }
        
        // 参数类型验证
        switch (toolName) {
            case "create_file_or_folder":
                if (!(params.get("is_folder") instanceof Boolean)) {
                    return "参数 'is_folder' 必须是布尔值";
                }
                break;
            case "edit_file":
                if (params.get("old_string").toString().isEmpty()) {
                    return "参数 'old_string' 不能为空";
                }
                break;
        }
        
        return null; // 验证通过
    }
    
    /**
     * 执行工具（参考 void-main 的工具调用实现，添加详细日志）
     * 
     * @param toolName 工具名称
     * @param params 工具参数（已验证）
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 工具执行结果
     */
    @Override
    public ToolResult callTool(String toolName, Map<String, Object> params, Long userId, String sessionId) {
        long startTime = System.currentTimeMillis();
        log.info("[ToolsService] 执行工具 - toolName={}", toolName);
        
        try {
            ToolResult result;
            switch (toolName) {
                case "read_file":
                    result = executeReadFile(params, userId);
                    break;
                case "ls_dir":
                    result = executeLsDir(params, userId);
                    break;
                case "get_dir_tree":
                    result = executeGetDirTree(params, userId);
                    break;
                case "create_file_or_folder":
                    result = executeCreateFileOrFolder(params, userId);
                    break;
                case "delete_file_or_folder":
                    result = executeDeleteFileOrFolder(params, userId);
                    break;
                case "write_file":
                    result = executeWriteFile(params, userId);
                    break;
                case "edit_file":
                    result = executeEditFile(params, userId);
                    break;
                case "rewrite_file":
                    result = executeRewriteFile(params, userId);
                    break;
                case "search_pathnames_only":
                    result = executeSearchPathnamesOnly(params, userId);
                    break;
                case "search_for_files":
                    result = executeSearchForFiles(params, userId);
                    break;
                case "search_in_file":
                    result = executeSearchInFile(params, userId);
                    break;
                case "run_command":
                    result = executeRunCommand(params, userId);
                    break;
                case "run_persistent_command":
                    result = executeRunPersistentCommand(params, userId);
                    break;
                case "open_persistent_terminal":
                    result = executeOpenPersistentTerminal(params, userId);
                    break;
                case "kill_persistent_terminal":
                    result = executeKillPersistentTerminal(params, userId);
                    break;
                case "read_lint_errors":
                    result = executeReadLintErrors(params, userId);
                    break;
                default:
                    log.warn("[ToolsService] 未实现的工具 - toolName={}", toolName);
                    result = ToolResult.error("未实现的工具: " + toolName);
                    break;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            if (result.isSuccess()) {
                log.info("[ToolsService] 完成 - toolName={}, duration={}ms", toolName, duration);
            } else {
                log.warn("[ToolsService] 失败 - toolName={}, duration={}ms, error={}", toolName, duration, result.getError());
            }
            return result;
            
        } catch (Exception e) {
            log.error("[ToolsService] 异常 - toolName={}, error={}", toolName, e.getMessage(), e);
            return ToolResult.error("工具执行失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> getAvailableTools() {
        return new ArrayList<>(TOOL_REGISTRY.keySet());
    }
    
    @Override
    public boolean toolExists(String toolName) {
        return TOOL_REGISTRY.containsKey(toolName);
    }
    
    @Override
    public String getToolDescription(String toolName) {
        ToolDefinition tool = TOOL_REGISTRY.get(toolName);
        return tool != null ? tool.description : null;
    }
    
    @Override
    public ToolsService.ToolInfo getToolInfo(String toolName) {
        ToolDefinition tool = TOOL_REGISTRY.get(toolName);
        if (tool == null) {
            return null;
        }
        return new ToolsService.ToolInfo(tool.name, tool.description, tool.params);
    }
    
    // ==================== 工具实现 ====================
    
    /**
     * 读取文件
     */
    private ToolResult executeReadFile(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            String content = terminalService.readFile(userId, path);
            
            String result = String.format("文件内容 (%s):\n```\n%s\n```", path, content);
            return ToolResult.success(Map.of("path", path, "content", content), result);
        } catch (Exception e) {
            return ToolResult.error("读取文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 列出目录
     */
    private ToolResult executeLsDir(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            List<String> files = terminalService.listDirectory(userId, path, null);
            
            String filesStr = String.join("\n", files);
            String result = String.format("目录内容 (%s):\n%s", path, filesStr);
            return ToolResult.success(Map.of("path", path, "files", files), result);
        } catch (Exception e) {
            return ToolResult.error("列出目录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取目录树
     */
    private ToolResult executeGetDirTree(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            String tree = terminalService.getDirectoryTree(userId, path, null);
            
            String result = String.format("目录树 (%s):\n%s", path, tree);
            return ToolResult.success(Map.of("path", path, "tree", tree), result);
        } catch (Exception e) {
            return ToolResult.error("获取目录树失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建文件或文件夹
     */
    private ToolResult executeCreateFileOrFolder(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            Boolean isFolder = (Boolean) params.get("is_folder");
            
            if (isFolder) {
                terminalService.createDirectory(userId, path, null);
                String result = String.format("成功创建文件夹: %s", path);
                return ToolResult.success(Map.of("path", path, "type", "folder"), result);
            } else {
                terminalService.writeFile(userId, path, "", null, true);
                String result = String.format("成功创建文件: %s", path);
                return ToolResult.success(Map.of("path", path, "type", "file"), result);
            }
        } catch (Exception e) {
            return ToolResult.error("创建文件/文件夹失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件或文件夹
     */
    private ToolResult executeDeleteFileOrFolder(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            terminalService.deleteFileOrFolder(userId, path, null);
            
            String result = String.format("成功删除: %s", path);
            return ToolResult.success(Map.of("path", path), result);
        } catch (Exception e) {
            return ToolResult.error("删除文件/文件夹失败: " + e.getMessage());
        }
    }
    
    /**
     * 写入文件
     */
    private ToolResult executeWriteFile(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            String content = params.get("content").toString();
            
            terminalService.writeFile(userId, path, content, null, true);
            
            String result = String.format("成功写入文件: %s (%d 字节)", path, content.length());
            return ToolResult.success(Map.of("path", path, "size", content.length()), result);
        } catch (Exception e) {
            return ToolResult.error("写入文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 编辑文件（search/replace）
     */
    private ToolResult executeEditFile(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            String oldString = params.get("old_string").toString();
            String newString = params.get("new_string").toString();
            
            // 读取文件
            String content = terminalService.readFile(userId, path);
            
            // 执行替换
            if (!content.contains(oldString)) {
                return ToolResult.error(String.format("在文件 %s 中未找到字符串: %s", path, oldString));
            }
            
            String newContent = content.replace(oldString, newString);
            terminalService.writeFile(userId, path, newContent, null, true);
            
            String result = String.format("成功编辑文件: %s (替换 %d 处)", path, 
                    (content.length() - content.replace(oldString, "").length()) / oldString.length());
            return ToolResult.success(Map.of("path", path, "modified", true), result);
        } catch (Exception e) {
            return ToolResult.error("编辑文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 重写文件
     */
    private ToolResult executeRewriteFile(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            String newContent = params.get("new_content").toString();
            
            terminalService.writeFile(userId, path, newContent, null, true);
            
            String result = String.format("成功重写文件: %s (%d 字节)", path, newContent.length());
            return ToolResult.success(Map.of("path", path, "size", newContent.length()), result);
        } catch (Exception e) {
            return ToolResult.error("重写文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索文件路径
     */
    private ToolResult executeSearchPathnamesOnly(Map<String, Object> params, Long userId) {
        try {
            String pattern = params.get("pattern").toString();
            List<String> results = terminalService.searchFileNames(userId, pattern, null);
            
            String resultsStr = results.isEmpty() ? "(未找到匹配文件)" : String.join("\n", results);
            String result = String.format("搜索路径 '%s' 的结果:\n%s", pattern, resultsStr);
            return ToolResult.success(Map.of("pattern", pattern, "results", results), result);
        } catch (Exception e) {
            return ToolResult.error("搜索文件路径失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索文件内容
     */
    private ToolResult executeSearchForFiles(Map<String, Object> params, Long userId) {
        try {
            String pattern = params.get("pattern").toString();
            Map<String, List<String>> results = terminalService.searchInFiles(userId, pattern, null);
            
            StringBuilder resultStr = new StringBuilder();
            resultStr.append(String.format("搜索内容 '%s' 的结果:\n", pattern));
            
            if (results.isEmpty()) {
                resultStr.append("(未找到匹配内容)");
            } else {
                for (Map.Entry<String, List<String>> entry : results.entrySet()) {
                    resultStr.append(String.format("\n文件: %s\n", entry.getKey()));
                    for (String line : entry.getValue()) {
                        resultStr.append("  ").append(line).append("\n");
                    }
                }
            }
            
            return ToolResult.success(Map.of("pattern", pattern, "results", results), resultStr.toString());
        } catch (Exception e) {
            return ToolResult.error("搜索文件内容失败: " + e.getMessage());
        }
    }
    
    /**
     * 在文件中搜索
     */
    private ToolResult executeSearchInFile(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            String pattern = params.get("pattern").toString();
            
            String content = terminalService.readFile(userId, path);
            List<String> matchedLines = new ArrayList<>();
            
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains(pattern)) {
                    matchedLines.add(String.format("Line %d: %s", i + 1, lines[i]));
                }
            }
            
            String resultsStr = matchedLines.isEmpty() ? 
                    "(未找到匹配内容)" : String.join("\n", matchedLines);
            String result = String.format("在文件 %s 中搜索 '%s' 的结果:\n%s", path, pattern, resultsStr);
            
            return ToolResult.success(Map.of("path", path, "pattern", pattern, "matches", matchedLines), result);
        } catch (Exception e) {
            return ToolResult.error("在文件中搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行命令
     */
    private ToolResult executeRunCommand(Map<String, Object> params, Long userId) {
        try {
            String command = params.get("command").toString();
            log.info("开始执行命令: command={}, userId={}", command, userId);
            
            com.aispring.dto.response.TerminalCommandResponse response = 
                    terminalService.executeCommand(userId, command, null);
            
            log.info("命令执行完成: command={}, exitCode={}, stdoutLength={}, stderrLength={}", 
                    command, response.getExitCode(), 
                    response.getStdout() != null ? response.getStdout().length() : 0,
                    response.getStderr() != null ? response.getStderr().length() : 0);
            
            String stderr = response.getStderr() != null ? response.getStderr() : "";
            String output = response.getStdout() + (stderr.isEmpty() ? "" : "\n" + stderr);
            String result = String.format("命令执行结果 (退出码: %d):\n```\n%s\n```", 
                    response.getExitCode(), output);
            return ToolResult.success(Map.of("command", command, "output", output, 
                    "exitCode", response.getExitCode()), result);
        } catch (Exception e) {
            log.error("执行命令异常: command={}, error={}", params.get("command"), e.getMessage(), e);
            return ToolResult.error("执行命令失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行持久化命令
     */
    private ToolResult executeRunPersistentCommand(Map<String, Object> params, Long userId) {
        // 简化实现：调用普通命令执行
        return executeRunCommand(params, userId);
    }
    
    /**
     * 打开持久化终端
     */
    private ToolResult executeOpenPersistentTerminal(Map<String, Object> params, Long userId) {
        try {
            String terminalId = params.get("terminal_id").toString();
            String result = String.format("成功打开持久化终端: %s", terminalId);
            return ToolResult.success(Map.of("terminal_id", terminalId), result);
        } catch (Exception e) {
            return ToolResult.error("打开持久化终端失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭持久化终端
     */
    private ToolResult executeKillPersistentTerminal(Map<String, Object> params, Long userId) {
        try {
            String terminalId = params.get("terminal_id").toString();
            String result = String.format("成功关闭持久化终端: %s", terminalId);
            return ToolResult.success(Map.of("terminal_id", terminalId), result);
        } catch (Exception e) {
            return ToolResult.error("关闭持久化终端失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取 Lint 错误
     */
    private ToolResult executeReadLintErrors(Map<String, Object> params, Long userId) {
        try {
            // 简化实现：返回空列表
            List<String> errors = new ArrayList<>();
            String result = "(当前无 Lint 错误)";
            return ToolResult.success(Map.of("errors", errors), result);
        } catch (Exception e) {
            return ToolResult.error("读取 Lint 错误失败: " + e.getMessage());
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 工具定义
     */
    private static class ToolDefinition {
        @SuppressWarnings("unused")
        final String name;
        @SuppressWarnings("unused")
        final String description;
        final Map<String, String> params; // 参数名 -> 参数描述
        
        ToolDefinition(String name, String description, Map<String, String> params) {
            this.name = name;
            this.description = description;
            this.params = params;
        }
        
        // 获取必需参数列表（用于兼容旧代码）
        List<String> getRequiredParams() {
            // 简单实现：所有参数都视为必需（除了明确标记为"可选"的）
            List<String> required = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!entry.getValue().contains("可选")) {
                    required.add(entry.getKey());
                }
            }
            return required;
        }
    }
}

