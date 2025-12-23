package com.aispring.entity.checkpoint;

/**
 * 检查点类型枚举
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public enum CheckpointType {
    /**
     * 用户消息检查点
     * 在用户发送消息后自动创建
     */
    USER_MESSAGE,
    
    /**
     * 工具编辑检查点
     * 在工具（如 edit_file, write_file）修改文件后创建
     */
    TOOL_EDIT,
    
    /**
     * 手动检查点
     * 用户手动创建的检查点
     */
    MANUAL
}

