package com.aispring.service;

import java.util.Map;

/**
 * 工具服务接口
 * 统一管理所有内置工具的执行、验证和结果处理
 * 
 * 参考 void-main 的 toolsService.ts 实现
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public interface ToolsService {
    
    /**
     * 工具执行结果
     */
    class ToolResult {
        private final boolean success;
        private final Object data;
        private final String error;
        private final String stringResult;
        
        public ToolResult(boolean success, Object data, String error, String stringResult) {
            this.success = success;
            this.data = data;
            this.error = error;
            this.stringResult = stringResult;
        }
        
        public static ToolResult success(Object data, String stringResult) {
            return new ToolResult(true, data, null, stringResult);
        }
        
        public static ToolResult error(String error) {
            return new ToolResult(false, null, error, error);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Object getData() {
            return data;
        }
        
        public String getError() {
            return error;
        }
        
        public String getStringResult() {
            return stringResult;
        }
    }
    
    /**
     * 验证工具参数
     * 
     * @param toolName 工具名称
     * @param params 工具参数
     * @return 验证结果（null 表示验证通过，否则返回错误信息）
     */
    String validateParams(String toolName, Map<String, Object> params);
    
    /**
     * 执行工具
     * 
     * @param toolName 工具名称
     * @param params 工具参数
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 工具执行结果
     */
    ToolResult callTool(String toolName, Map<String, Object> params, Long userId, String sessionId);
    
    /**
     * 获取工具列表
     * 
     * @return 所有可用工具的名称列表
     */
    java.util.List<String> getAvailableTools();
    
    /**
     * 检查工具是否存在
     * 
     * @param toolName 工具名称
     * @return 是否存在
     */
    boolean toolExists(String toolName);
    
    /**
     * 获取工具描述（用于生成 System Prompt）
     * 
     * @param toolName 工具名称
     * @return 工具描述
     */
    String getToolDescription(String toolName);
}

