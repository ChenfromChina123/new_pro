-- =============================================
-- AISpring AI Terminal System Refactor
-- Phase 1: Database Schema Migration
-- Version: 2.2
-- Date: 2025-12-23
-- Description: 创建会话状态表（作为 Redis 的持久化备份）
-- =============================================

-- 创建 session_states 表（会话状态持久化表）
-- 注意：主要状态存储在 Redis 中，此表作为持久化备份和恢复使用
CREATE TABLE IF NOT EXISTS session_states (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id VARCHAR(100) NOT NULL UNIQUE COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- Agent 状态
    agent_status ENUM('IDLE', 'RUNNING', 'AWAITING_APPROVAL', 'PAUSED', 'COMPLETED', 'ERROR') 
        NOT NULL DEFAULT 'IDLE' COMMENT 'Agent 状态',
    current_loop_id VARCHAR(64) NULL COMMENT '当前 Agent 循环ID',
    
    -- 流式状态（JSON 格式）
    -- 结构: {"type": "STREAMING_LLM", "displayContentSoFar": "...", "reasoningSoFar": "...", ...}
    stream_state JSON COMMENT '流式状态',
    
    -- 任务状态（JSON 格式）
    -- 结构: {"pipelineId": "...", "currentTaskId": "...", "tasks": [...]}
    task_state JSON COMMENT '任务流水线状态',
    
    -- 最后一次决策（JSON 格式）
    last_decision JSON COMMENT '最后一次决策信封',
    
    -- 最后一个检查点ID
    last_checkpoint_id VARCHAR(64) NULL COMMENT '最后一个检查点ID',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_loop_id (current_loop_id),
    INDEX idx_status (agent_status),
    INDEX idx_last_active (last_active_at),
    
    -- 外键
    CONSTRAINT fk_session_states_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='会话状态持久化表 - Redis 的备份存储';

-- 创建 agent_loops 表（Agent 循环历史记录）
CREATE TABLE IF NOT EXISTS agent_loops (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    loop_id VARCHAR(64) NOT NULL UNIQUE COMMENT '循环ID',
    session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 循环信息
    loop_status ENUM('RUNNING', 'COMPLETED', 'INTERRUPTED', 'ERROR') 
        NOT NULL DEFAULT 'RUNNING' COMMENT '循环状态',
    messages_sent INT NOT NULL DEFAULT 0 COMMENT '发送的消息数量',
    tools_called INT NOT NULL DEFAULT 0 COMMENT '调用的工具数量',
    
    -- 终止原因
    termination_reason VARCHAR(500) COMMENT '终止原因',
    error_message TEXT COMMENT '错误消息（如果有）',
    
    -- 时间戳
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    duration_ms INT NULL COMMENT '持续时间（毫秒）',
    
    -- 索引
    INDEX idx_session_user (session_id, user_id),
    INDEX idx_status (loop_status),
    INDEX idx_started_at (started_at),
    
    -- 外键
    CONSTRAINT fk_agent_loops_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Agent 循环历史记录表 - 用于监控和调试';

-- =============================================
-- 初始化数据
-- =============================================

-- 为所有现有会话创建初始状态（IDLE）
INSERT INTO session_states (session_id, user_id, agent_status, stream_state, last_active_at)
SELECT 
    cs.session_id,
    cs.user_id,
    'IDLE' AS agent_status,
    '{"type": "IDLE"}' AS stream_state,
    cs.updated_at AS last_active_at
FROM chat_sessions cs
WHERE cs.session_id NOT IN (SELECT session_id FROM session_states)
    AND cs.chat_type = 'terminal';

-- =============================================
-- 清理任务（定期执行，建议通过定时任务调用）
-- =============================================

-- 创建存储过程：清理过期的会话状态
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS cleanup_expired_sessions()
BEGIN
    -- 删除 7 天内未活跃的会话状态
    DELETE FROM session_states 
    WHERE last_active_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
    
    -- 删除 30 天前的 Agent 循环记录
    DELETE FROM agent_loops 
    WHERE started_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    SELECT CONCAT('Cleaned up expired sessions at ', NOW()) AS result;
END //

DELIMITER ;

-- 验证表和存储过程
SELECT 
    TABLE_NAME, 
    TABLE_ROWS, 
    CREATE_TIME 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME IN ('session_states', 'agent_loops');

SELECT 
    ROUTINE_NAME, 
    CREATED 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = DATABASE() 
    AND ROUTINE_NAME = 'cleanup_expired_sessions';

