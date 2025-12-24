/**
 * Terminal API Service
 * 封装 AI 终端相关的 API 调用
 * 包括：检查点、批准、会话状态等功能
 */

import { API_ENDPOINTS } from '@/config/api'
import request from '@/utils/request'

/**
 * 检查点相关 API
 */
export const checkpointService = {
  /**
   * 获取会话的所有检查点
   */
  async getCheckpoints(sessionId: string) {
    const response = await request.get(API_ENDPOINTS.terminal.checkpoints.list(sessionId))
    return response.data
  },

  /**
   * 创建手动检查点
   */
  async createCheckpoint(data: {
    sessionId: string
    messageOrder: number
    description?: string
    fileSnapshots?: Record<string, any>
  }) {
    const response = await request.post(API_ENDPOINTS.terminal.checkpoints.create, data)
    return response.data
  },

  /**
   * 跳转到检查点（恢复文件快照）
   */
  async jumpToCheckpoint(checkpointId: string) {
    const response = await request.post(API_ENDPOINTS.terminal.checkpoints.jump(checkpointId))
    return response.data
  },

  /**
   * 删除检查点
   */
  async deleteCheckpoint(checkpointId: string) {
    const response = await request.delete(API_ENDPOINTS.terminal.checkpoints.delete(checkpointId))
    return response.data
  },

  /**
   * 导出检查点（JSON格式）
   */
  async exportCheckpoint(checkpointId: string) {
    const response = await request.get(API_ENDPOINTS.terminal.checkpoints.export(checkpointId))
    return response.data
  }
}

/**
 * 批准相关 API
 */
export const approvalService = {
  /**
   * 获取会话的待批准列表
   */
  async getPendingApprovals(sessionId: string) {
    const response = await request.get(API_ENDPOINTS.terminal.approvals.pending(sessionId))
    return response.data
  },

  /**
   * 批准工具调用
   */
  async approveToolCall(decisionId: string, reason?: string) {
    const response = await request.post(
      API_ENDPOINTS.terminal.approvals.approve(decisionId),
      reason ? { reason } : {}
    )
    return response.data
  },

  /**
   * 拒绝工具调用
   */
  async rejectToolCall(decisionId: string, reason: string) {
    const response = await request.post(
      API_ENDPOINTS.terminal.approvals.reject(decisionId),
      { reason }
    )
    return response.data
  },

  /**
   * 获取用户批准设置
   */
  async getSettings() {
    const response = await request.get(API_ENDPOINTS.terminal.approvals.settings)
    return response.data
  },

  /**
   * 更新用户批准设置
   */
  async updateSettings(settings: {
    autoApproveDangerousTools?: boolean
    autoApproveReadFile?: boolean
    autoApproveFileEdits?: boolean
    autoApproveMcpTools?: boolean
    includeToolLintErrors?: boolean
    maxCheckpointsPerSession?: number
  }) {
    const response = await request.put(API_ENDPOINTS.terminal.approvals.settings, settings)
    return response.data
  },

  /**
   * 批量批准所有待批准工具
   */
  async approveAllPending(sessionId: string) {
    const response = await request.post(API_ENDPOINTS.terminal.approvals.approveAll(sessionId))
    return response.data
  }
}

/**
 * 会话状态相关 API
 */
export const sessionStateService = {
  /**
   * 获取会话状态
   */
  async getSessionState(sessionId: string) {
    const response = await request.get(API_ENDPOINTS.terminal.state.get(sessionId))
    return response.data
  },

  /**
   * 请求中断当前 Agent 循环
   */
  async interruptAgentLoop(sessionId: string) {
    const response = await request.post(API_ENDPOINTS.terminal.state.interrupt(sessionId))
    return response.data
  },

  /**
   * 清除中断标志
   */
  async clearInterrupt(sessionId: string) {
    const response = await request.post(API_ENDPOINTS.terminal.state.clearInterrupt(sessionId))
    return response.data
  }
}

/**
 * 统一导出
 */
export default {
  checkpoint: checkpointService,
  approval: approvalService,
  sessionState: sessionStateService
}

