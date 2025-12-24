/**
 * Agent 循环状态管理器
 * 
 * 借鉴 Void-Main 的设计理念，实现：
 * 1. Agent 循环的完整生命周期管理
 * 2. 决策流程的状态追踪
 * 3. 工具调用的批准/拒绝机制
 * 4. 中断和恢复机制
 * 5. 检查点的自动创建和恢复
 */

import { ref, computed } from 'vue'
import { sessionStateService, checkpointService, approvalService } from './terminalService'

export class AgentLoopManager {
  constructor(sessionId) {
    this.sessionId = sessionId
    this.loopId = null
    this.status = ref('IDLE') // IDLE, PLANNING, RUNNING, WAITING_TOOL, WAITING_APPROVAL, PAUSED, ERROR, COMPLETED
    this.currentDecision = ref(null)
    this.decisionHistory = ref([])
    this.pendingApprovals = ref([])
    this.checkpoints = ref([])
    this.taskState = ref({
      tasks: [],
      activeTaskId: null,
      completedTasks: []
    })
    this.streamState = ref({
      isStreaming: false,
      currentMessage: '',
      currentThought: '',
      bufferedContent: ''
    })
    
    // 自动批准设置
    this.autoApprovalRules = ref({
      read_file: true,
      search_files: true,
      execute_command: false,
      write_file: false,
      modify_file: false,
      delete_file: false
    })
  }

  /**
   * 启动新的 Agent 循环
   */
  async startLoop(initialPrompt, model = 'deepseek-chat') {
    this.loopId = `loop_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    this.status.value = 'PLANNING'
    this.decisionHistory.value = []
    
    console.log('[AgentLoopManager] Starting new loop:', {
      loopId: this.loopId,
      sessionId: this.sessionId,
      prompt: initialPrompt
    })
    
    // 创建初始检查点
    await this.createCheckpoint('LOOP_START', '循环开始')
    
    return {
      loopId: this.loopId,
      sessionId: this.sessionId,
      prompt: initialPrompt,
      model: model
    }
  }

  /**
   * 处理 LLM 返回的决策
   */
  async processDecision(decision) {
    console.log('[AgentLoopManager] Processing decision:', decision)
    
    // 去重检查
    if (this.hasDecision(decision.decision_id)) {
      console.warn('[AgentLoopManager] Duplicate decision ignored:', decision.decision_id)
      return { action: 'IGNORE', reason: 'duplicate' }
    }
    
    // 添加到历史
    this.addDecision(decision)
    this.currentDecision.value = decision
    
    // 根据决策类型处理
    switch (decision.type) {
      case 'TASK_LIST':
        return await this.handleTaskList(decision)
      
      case 'TOOL_CALL':
        return await this.handleToolCall(decision)
      
      case 'TASK_COMPLETE':
        return await this.handleTaskComplete(decision)
      
      case 'PAUSE':
        return await this.handlePause(decision)
      
      case 'ERROR':
        return await this.handleError(decision)
      
      default:
        console.warn('[AgentLoopManager] Unknown decision type:', decision.type)
        return { action: 'CONTINUE', reason: 'unknown_type' }
    }
  }

  /**
   * 处理任务列表
   */
  async handleTaskList(decision) {
    this.taskState.value.tasks = decision.tasks || []
    this.status.value = 'RUNNING'
    
    console.log('[AgentLoopManager] Task list received:', this.taskState.value.tasks)
    
    // 自动开始执行第一个任务
    if (this.taskState.value.tasks.length > 0) {
      this.taskState.value.activeTaskId = this.taskState.value.tasks[0].id
    }
    
    return { action: 'CONTINUE', reason: 'task_list_received' }
  }

  /**
   * 处理工具调用
   */
  async handleToolCall(decision) {
    const toolName = decision.action
    
    // 检查是否需要批准
    if (this.needsApproval(toolName)) {
      this.status.value = 'WAITING_APPROVAL'
      
      // 添加到待批准列表
      this.pendingApprovals.value.push({
        decisionId: decision.decision_id,
        toolName: toolName,
        params: decision.params,
        timestamp: Date.now(),
        status: 'PENDING'
      })
      
      console.log('[AgentLoopManager] Tool requires approval:', toolName)
      return { action: 'WAIT_APPROVAL', decisionId: decision.decision_id }
    }
    
    // 自动批准，继续执行
    this.status.value = 'WAITING_TOOL'
    return { action: 'EXECUTE', decisionId: decision.decision_id }
  }

  /**
   * 处理任务完成
   */
  async handleTaskComplete(decision) {
    const currentTask = this.taskState.value.tasks.find(
      t => t.id === this.taskState.value.activeTaskId
    )
    
    if (currentTask) {
      currentTask.status = 'completed'
      this.taskState.value.completedTasks.push(currentTask.id)
    }
    
    // 检查是否还有待执行的任务
    const nextTask = this.taskState.value.tasks.find(t => t.status === 'pending')
    if (nextTask) {
      this.taskState.value.activeTaskId = nextTask.id
      nextTask.status = 'in_progress'
      return { action: 'CONTINUE', reason: 'next_task' }
    }
    
    // 所有任务完成
    this.status.value = 'COMPLETED'
    await this.createCheckpoint('LOOP_END', '所有任务完成')
    return { action: 'COMPLETE', reason: 'all_tasks_done' }
  }

  /**
   * 处理暂停
   */
  async handlePause(decision) {
    this.status.value = 'PAUSED'
    await this.createCheckpoint('PAUSE', '用户暂停')
    return { action: 'PAUSE', reason: 'user_requested' }
  }

  /**
   * 处理错误
   */
  async handleError(decision) {
    this.status.value = 'ERROR'
    console.error('[AgentLoopManager] Error in loop:', decision.message)
    return { action: 'ERROR', reason: decision.message }
  }

  /**
   * 批准工具调用
   */
  async approveTool(decisionId, reason = '') {
    const approval = this.pendingApprovals.value.find(a => a.decisionId === decisionId)
    if (!approval) {
      console.warn('[AgentLoopManager] Approval not found:', decisionId)
      return false
    }
    
    approval.status = 'APPROVED'
    approval.approvalReason = reason
    approval.approvalTime = Date.now()
    
    // 从待批准列表移除
    this.pendingApprovals.value = this.pendingApprovals.value.filter(
      a => a.decisionId !== decisionId
    )
    
    // 通知后端
    try {
      await approvalService.approveToolCall(decisionId, reason)
      console.log('[AgentLoopManager] Tool approved:', decisionId)
      
      // 如果没有其他待批准项，恢复运行
      if (this.pendingApprovals.value.length === 0) {
        this.status.value = 'RUNNING'
      }
      
      return true
    } catch (error) {
      console.error('[AgentLoopManager] Failed to approve tool:', error)
      return false
    }
  }

  /**
   * 拒绝工具调用
   */
  async rejectTool(decisionId, reason = '') {
    const approval = this.pendingApprovals.value.find(a => a.decisionId === decisionId)
    if (!approval) {
      console.warn('[AgentLoopManager] Approval not found:', decisionId)
      return false
    }
    
    approval.status = 'REJECTED'
    approval.rejectionReason = reason
    approval.rejectionTime = Date.now()
    
    // 从待批准列表移除
    this.pendingApprovals.value = this.pendingApprovals.value.filter(
      a => a.decisionId !== decisionId
    )
    
    // 通知后端
    try {
      await approvalService.rejectToolCall(decisionId, reason)
      console.log('[AgentLoopManager] Tool rejected:', decisionId)
      
      // 如果没有其他待批准项，恢复运行
      if (this.pendingApprovals.value.length === 0) {
        this.status.value = 'RUNNING'
      }
      
      return true
    } catch (error) {
      console.error('[AgentLoopManager] Failed to reject tool:', error)
      return false
    }
  }

  /**
   * 中断 Agent 循环
   */
  async interrupt() {
    if (!this.sessionId) return false
    
    try {
      await sessionStateService.interruptAgentLoop(this.sessionId)
      this.status.value = 'PAUSED'
      await this.createCheckpoint('INTERRUPT', '用户中断')
      console.log('[AgentLoopManager] Loop interrupted')
      return true
    } catch (error) {
      console.error('[AgentLoopManager] Failed to interrupt loop:', error)
      return false
    }
  }

  /**
   * 恢复 Agent 循环
   */
  async resume() {
    if (this.status.value !== 'PAUSED') {
      console.warn('[AgentLoopManager] Cannot resume, status:', this.status.value)
      return false
    }
    
    this.status.value = 'RUNNING'
    console.log('[AgentLoopManager] Loop resumed')
    return true
  }

  /**
   * 创建检查点
   */
  async createCheckpoint(type, description) {
    if (!this.sessionId) return null
    
    try {
      const checkpoint = await checkpointService.createCheckpoint({
        sessionId: this.sessionId,
        type: type,
        description: description,
        messageOrder: this.decisionHistory.value.length
      })
      
      this.checkpoints.value.push(checkpoint)
      console.log('[AgentLoopManager] Checkpoint created:', checkpoint)
      return checkpoint
    } catch (error) {
      console.error('[AgentLoopManager] Failed to create checkpoint:', error)
      return null
    }
  }

  /**
   * 跳转到检查点
   */
  async jumpToCheckpoint(checkpointId) {
    try {
      const result = await checkpointService.jumpToCheckpoint(checkpointId)
      console.log('[AgentLoopManager] Jumped to checkpoint:', checkpointId)
      
      // 重置状态
      this.status.value = 'IDLE'
      this.currentDecision.value = null
      this.pendingApprovals.value = []
      
      return result
    } catch (error) {
      console.error('[AgentLoopManager] Failed to jump to checkpoint:', error)
      return null
    }
  }

  /**
   * 加载检查点列表
   */
  async loadCheckpoints() {
    if (!this.sessionId) return []
    
    try {
      const result = await checkpointService.getCheckpoints(this.sessionId)
      this.checkpoints.value = result.data || []
      return this.checkpoints.value
    } catch (error) {
      console.error('[AgentLoopManager] Failed to load checkpoints:', error)
      return []
    }
  }

  /**
   * 加载待批准列表
   */
  async loadPendingApprovals() {
    if (!this.sessionId) return []
    
    try {
      const result = await approvalService.getPendingApprovals(this.sessionId)
      this.pendingApprovals.value = result.data || []
      return this.pendingApprovals.value
    } catch (error) {
      console.error('[AgentLoopManager] Failed to load pending approvals:', error)
      return []
    }
  }

  /**
   * 检查决策是否已存在
   */
  hasDecision(decisionId) {
    return this.decisionHistory.value.some(d => d.decision_id === decisionId)
  }

  /**
   * 添加决策到历史
   */
  addDecision(decision) {
    this.decisionHistory.value.push(decision)
  }

  /**
   * 检查工具是否需要批准
   */
  needsApproval(toolName) {
    // 如果设置了自动批准，则不需要批准
    if (this.autoApprovalRules.value[toolName] === true) {
      return false
    }
    
    // 危险工具默认需要批准
    const dangerousTools = ['write_file', 'modify_file', 'delete_file', 'execute_command']
    return dangerousTools.includes(toolName)
  }

  /**
   * 更新自动批准规则
   */
  updateAutoApprovalRule(toolName, enabled) {
    this.autoApprovalRules.value[toolName] = enabled
  }

  /**
   * 获取循环统计信息
   */
  getLoopStats() {
    return {
      loopId: this.loopId,
      status: this.status.value,
      totalDecisions: this.decisionHistory.value.length,
      totalTasks: this.taskState.value.tasks.length,
      completedTasks: this.taskState.value.completedTasks.length,
      pendingApprovals: this.pendingApprovals.value.length,
      checkpoints: this.checkpoints.value.length
    }
  }

  /**
   * 清理循环状态
   */
  cleanup() {
    this.loopId = null
    this.status.value = 'IDLE'
    this.currentDecision.value = null
    this.decisionHistory.value = []
    this.pendingApprovals.value = []
    this.taskState.value = {
      tasks: [],
      activeTaskId: null,
      completedTasks: []
    }
    this.streamState.value = {
      isStreaming: false,
      currentMessage: '',
      currentThought: '',
      bufferedContent: ''
    }
  }
}

/**
 * 创建 Agent 循环管理器实例
 */
export function createAgentLoopManager(sessionId) {
  return new AgentLoopManager(sessionId)
}

