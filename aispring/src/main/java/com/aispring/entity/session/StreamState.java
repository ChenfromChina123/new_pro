package com.aispring.entity.session;

import com.aispring.entity.agent.ToolCallDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 流式状态实体
 * 用于跟踪 LLM 生成和工具执行的实时状态
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 流式状态类型
     */
    @Builder.Default
    private StreamType type = StreamType.IDLE;
    
    // ========== LLM 流式状态 ==========
    
    /**
     * LLM 生成的内容（累积）
     */
    private String displayContentSoFar;
    
    /**
     * LLM 推理内容（累积）
     */
    private String reasoningSoFar;
    
    /**
     * LLM 返回的工具调用（累积）
     */
    private ToolCallDto toolCallSoFar;
    
    // ========== 工具执行状态 ==========
    
    /**
     * 当前执行的工具名称
     */
    private String toolName;
    
    /**
     * 工具参数
     */
    private Map<String, Object> toolParams;
    
    /**
     * 工具ID（decision_id）
     */
    private String toolId;
    
    /**
     * 工具执行内容/进度
     */
    private String toolContent;
    
    /**
     * MCP 服务器名称（如果是 MCP 工具）
     */
    private String mcpServerName;
    
    // ========== 中断标志 ==========
    
    /**
     * 是否请求中断
     */
    @Builder.Default
    private boolean interruptRequested = false;
    
    /**
     * 创建空闲状态
     */
    public static StreamState idle() {
        return StreamState.builder()
                .type(StreamType.IDLE)
                .interruptRequested(false)
                .build();
    }
    
    /**
     * 创建 LLM 流式状态
     */
    public static StreamState streamingLLM() {
        return StreamState.builder()
                .type(StreamType.STREAMING_LLM)
                .displayContentSoFar("")
                .reasoningSoFar("")
                .interruptRequested(false)
                .build();
    }
    
    /**
     * 创建工具执行状态
     */
    public static StreamState runningTool(String toolName, Map<String, Object> toolParams, String toolId) {
        return StreamState.builder()
                .type(StreamType.RUNNING_TOOL)
                .toolName(toolName)
                .toolParams(toolParams)
                .toolId(toolId)
                .toolContent("(正在执行...)")
                .interruptRequested(false)
                .build();
    }
    
    /**
     * 创建等待用户批准状态
     */
    public static StreamState awaitingUser(String toolName, Map<String, Object> toolParams, String toolId) {
        return StreamState.builder()
                .type(StreamType.AWAITING_USER)
                .toolName(toolName)
                .toolParams(toolParams)
                .toolId(toolId)
                .interruptRequested(false)
                .build();
    }
}

