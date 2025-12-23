package com.aispring.service;

import com.aispring.entity.approval.ApprovalStatus;
import com.aispring.entity.approval.ToolApproval;
import com.aispring.entity.approval.UserApprovalSettings;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具批准服务接口
 * 管理工具调用的批准流程
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public interface ToolApprovalService {
    
    /**
     * 创建工具批准请求
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param toolName 工具名称
     * @param toolParams 工具参数
     * @param decisionId 决策ID
     * @return 批准记录ID
     */
    Long createApprovalRequest(
            String sessionId,
            Long userId,
            String toolName,
            Map<String, Object> toolParams,
            String decisionId
    );
    
    /**
     * 检查工具是否需要用户批准
     * 
     * @param userId 用户ID
     * @param toolName 工具名称
     * @return 是否需要批准
     */
    boolean requiresApproval(Long userId, String toolName);
    
    /**
     * 批准工具调用
     * 
     * @param decisionId 决策ID
     * @param reason 批准原因（可选）
     * @return 是否成功
     */
    boolean approveToolCall(String decisionId, String reason);
    
    /**
     * 拒绝工具调用
     * 
     * @param decisionId 决策ID
     * @param reason 拒绝原因
     * @return 是否成功
     */
    boolean rejectToolCall(String decisionId, String reason);
    
    /**
     * 获取批准记录
     * 
     * @param decisionId 决策ID
     * @return 批准记录
     */
    Optional<ToolApproval> getApproval(String decisionId);
    
    /**
     * 获取会话的待批准列表
     * 
     * @param sessionId 会话ID
     * @return 待批准列表
     */
    List<ToolApproval> getPendingApprovals(String sessionId);
    
    /**
     * 获取用户的批准设置
     * 
     * @param userId 用户ID
     * @return 批准设置
     */
    UserApprovalSettings getUserSettings(Long userId);
    
    /**
     * 更新用户批准设置
     * 
     * @param userId 用户ID
     * @param settings 新设置
     */
    void updateUserSettings(Long userId, UserApprovalSettings settings);
    
    /**
     * 批量批准会话的所有待批准工具
     * 
     * @param sessionId 会话ID
     * @return 批准的数量
     */
    int approveAllPending(String sessionId);
    
    /**
     * 批量拒绝会话的所有待批准工具
     * 
     * @param sessionId 会话ID
     * @param reason 拒绝原因
     * @return 拒绝的数量
     */
    int rejectAllPending(String sessionId, String reason);
    
    /**
     * 清理过期的批准记录
     * 
     * @param daysToKeep 保留天数
     * @return 清理的数量
     */
    int cleanupExpiredApprovals(int daysToKeep);
}

