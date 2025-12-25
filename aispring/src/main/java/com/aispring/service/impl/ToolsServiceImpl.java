package com.aispring.service.impl;

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
    
    // 工具注册表
    private static final Map<String, ToolDefinition> TOOL_REGISTRY = new HashMap<>();
    
    static {
        // 目前没有注册任何工具，因为 AI 终端模块已被移除
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
        
        return null; // 验证通过
    }
    
    /**
     * 执行工具
     * 
     * @param toolName 工具名称
     * @param params 工具参数（已验证）
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 工具执行结果
     */
    @Override
    public ToolResult callTool(String toolName, Map<String, Object> params, Long userId, String sessionId) {
        log.info("[ToolsService] 执行工具 - toolName={}", toolName);
        return ToolResult.error("未实现的工具: " + toolName);
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
    
    // ==================== 内部类 ====================
    
    /**
     * 工具 definition
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
