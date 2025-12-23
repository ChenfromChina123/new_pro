package com.aispring.service;

import com.aispring.entity.checkpoint.ChatCheckpoint;
import com.aispring.entity.checkpoint.CheckpointType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 检查点服务接口
 * 管理聊天检查点，支持"时间旅行"功能
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public interface CheckpointService {
    
    /**
     * 创建检查点
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param messageOrder 消息顺序
     * @param type 检查点类型
     * @param fileSnapshots 文件快照
     * @param description 描述
     * @return 检查点ID
     */
    String createCheckpoint(
            String sessionId,
            Long userId,
            Integer messageOrder,
            CheckpointType type,
            Map<String, ChatCheckpoint.FileSnapshot> fileSnapshots,
            String description
    );
    
    /**
     * 获取检查点
     * 
     * @param checkpointId 检查点ID
     * @return 检查点
     */
    Optional<ChatCheckpoint> getCheckpoint(String checkpointId);
    
    /**
     * 获取会话的所有检查点
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 检查点列表（按时间排序）
     */
    List<ChatCheckpoint> getSessionCheckpoints(String sessionId, Long userId);
    
    /**
     * 获取指定消息顺序之前的最近检查点
     * 
     * @param sessionId 会话ID
     * @param messageOrder 消息顺序
     * @return 检查点
     */
    Optional<ChatCheckpoint> getCheckpointBeforeMessage(String sessionId, Integer messageOrder);
    
    /**
     * 跳转到检查点（恢复文件快照）
     * 
     * @param checkpointId 检查点ID
     * @return 恢复的文件路径列表
     */
    List<String> jumpToCheckpoint(String checkpointId);
    
    /**
     * 更新检查点的用户修改快照
     * 
     * @param checkpointId 检查点ID
     * @param userModifications 用户修改
     */
    void updateUserModifications(
            String checkpointId,
            Map<String, ChatCheckpoint.FileSnapshot> userModifications
    );
    
    /**
     * 删除检查点
     * 
     * @param checkpointId 检查点ID
     */
    void deleteCheckpoint(String checkpointId);
    
    /**
     * 清理会话的旧检查点（保留最新的 N 个）
     * 
     * @param sessionId 会话ID
     * @param keepCount 保留数量
     */
    void cleanupOldCheckpoints(String sessionId, int keepCount);
    
    /**
     * 获取会话的检查点数量
     * 
     * @param sessionId 会话ID
     * @return 检查点数量
     */
    long getCheckpointCount(String sessionId);
    
    /**
     * 导出检查点（用于分享或备份）
     * 
     * @param checkpointId 检查点ID
     * @return JSON 字符串
     */
    String exportCheckpoint(String checkpointId);
    
    /**
     * 导入检查点
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param checkpointJson JSON 字符串
     * @return 新检查点ID
     */
    String importCheckpoint(String sessionId, Long userId, String checkpointJson);
}

