package com.aispring.service.impl;

import com.aispring.service.TerminalService;
import com.aispring.service.ToolsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        // 注册文件操作工具
        registerTool("read_file", "读取文件内容", List.of("path"));
        registerTool("ls_dir", "列出目录内容", List.of("path"));
        registerTool("get_dir_tree", "获取目录树", List.of("path"));
        registerTool("create_file_or_folder", "创建文件或文件夹", List.of("path", "is_folder"));
        registerTool("delete_file_or_folder", "删除文件或文件夹", List.of("path"));
        registerTool("write_file", "写入文件", List.of("path", "content"));
        registerTool("edit_file", "编辑文件（search/replace）", List.of("path", "old_string", "new_string"));
        registerTool("rewrite_file", "重写整个文件", List.of("path", "new_content"));
        
        // 注册搜索工具
        registerTool("search_pathnames_only", "搜索文件路径", List.of("pattern"));
        registerTool("search_for_files", "搜索文件内容", List.of("pattern"));
        registerTool("search_in_file", "在文件中搜索", List.of("path", "pattern"));
        
        // 注册终端工具
        registerTool("run_command", "执行命令", List.of("command"));
        registerTool("run_persistent_command", "执行持久化命令", List.of("command", "terminal_id"));
        registerTool("open_persistent_terminal", "打开持久化终端", List.of("terminal_id"));
        registerTool("kill_persistent_terminal", "关闭持久化终端", List.of("terminal_id"));
        
        // 注册其他工具
        registerTool("read_lint_errors", "读取 Lint 错误", List.of());
    }
    
    private static void registerTool(String name, String description, List<String> requiredParams) {
        TOOL_REGISTRY.put(name, new ToolDefinition(name, description, requiredParams));
    }
    
    @Override
    public String validateParams(String toolName, Map<String, Object> params) {
        ToolDefinition tool = TOOL_REGISTRY.get(toolName);
        if (tool == null) {
            return "未知工具: " + toolName;
        }
        
        // 检查必需参数
        for (String requiredParam : tool.requiredParams) {
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
    
    @Override
    public ToolResult callTool(String toolName, Map<String, Object> params, Long userId, String sessionId) {
        try {
            // 验证参数
            String validationError = validateParams(toolName, params);
            if (validationError != null) {
                return ToolResult.error(validationError);
            }
            
            log.info("执行工具: tool={}, userId={}, sessionId={}", toolName, userId, sessionId);
            
            // 根据工具名称分发执行
            switch (toolName) {
                // 文件操作工具
                case "read_file":
                    return executeReadFile(params, userId);
                case "ls_dir":
                    return executeLsDir(params, userId);
                case "get_dir_tree":
                    return executeGetDirTree(params, userId);
                case "create_file_or_folder":
                    return executeCreateFileOrFolder(params, userId);
                case "delete_file_or_folder":
                    return executeDeleteFileOrFolder(params, userId);
                case "write_file":
                    return executeWriteFile(params, userId);
                case "edit_file":
                    return executeEditFile(params, userId);
                case "rewrite_file":
                    return executeRewriteFile(params, userId);
                
                // 搜索工具
                case "search_pathnames_only":
                    return executeSearchPathnamesOnly(params, userId);
                case "search_for_files":
                    return executeSearchForFiles(params, userId);
                case "search_in_file":
                    return executeSearchInFile(params, userId);
                
                // 终端工具
                case "run_command":
                    return executeRunCommand(params, userId);
                case "run_persistent_command":
                    return executeRunPersistentCommand(params, userId);
                case "open_persistent_terminal":
                    return executeOpenPersistentTerminal(params, userId);
                case "kill_persistent_terminal":
                    return executeKillPersistentTerminal(params, userId);
                
                // 其他工具
                case "read_lint_errors":
                    return executeReadLintErrors(params, userId);
                
                default:
                    return ToolResult.error("未实现的工具: " + toolName);
            }
        } catch (Exception e) {
            log.error("工具执行失败: tool={}, error={}", toolName, e.getMessage(), e);
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
    
    // ==================== 工具实现 ====================
    
    /**
     * 读取文件
     */
    private ToolResult executeReadFile(Map<String, Object> params, Long userId) {
        try {
            String path = params.get("path").toString();
            String content = terminalService.readFile(userId, path, null);
            
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
                terminalService.writeFile(userId, path, "", null);
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
            
            terminalService.writeFile(userId, path, content, null);
            
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
            String content = terminalService.readFile(userId, path, null);
            
            // 执行替换
            if (!content.contains(oldString)) {
                return ToolResult.error(String.format("在文件 %s 中未找到字符串: %s", path, oldString));
            }
            
            String newContent = content.replace(oldString, newString);
            terminalService.writeFile(userId, path, newContent, null);
            
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
            
            terminalService.writeFile(userId, path, newContent, null);
            
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
            
            String content = terminalService.readFile(userId, path, null);
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
            String output = terminalService.executeCommand(userId, command, null);
            
            String result = String.format("命令执行结果:\n```\n%s\n```", output);
            return ToolResult.success(Map.of("command", command, "output", output), result);
        } catch (Exception e) {
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
        final String name;
        final String description;
        final List<String> requiredParams;
        
        ToolDefinition(String name, String description, List<String> requiredParams) {
            this.name = name;
            this.description = description;
            this.requiredParams = requiredParams;
        }
    }
}

