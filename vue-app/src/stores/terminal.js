import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { API_CONFIG } from '@/config/api'
import terminalService from '@/services/terminalService'

export const useTerminalStore = defineStore('terminal', () => {
  const authStore = useAuthStore()
  
  // 状态
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const terminalLogs = ref([])
  const currentTasks = ref([])
  const activeTaskId = ref(null) // 当前正在进行的任务 ID
  const currentCwd = ref('/')
  const isLoading = ref(false)

  // Agent V2 State
  const agentStatus = ref('IDLE') // IDLE, RUNNING, PAUSED, ERROR, AWAITING_APPROVAL
  const decisionHistory = ref(new Set()) // decision_id set for de-duplication
  const checkpoints = ref([])
  const pendingApprovals = ref([])

  // 解耦架构：身份信息管理
  const identityInfo = ref(null) // { core, task, viewpoint }

  // 解耦架构：状态切片管理
  const stateSlices = ref([]) // Array of { source, scope, data, timestamp, authority }

  // 解耦架构：作用域管理
  const visibleFiles = ref([]) // 可见文件列表
  const visibleFunctions = ref([]) // 可见函数列表

  // 解耦架构：工具执行白名单
  const toolWhitelist = ref([
    'execute_command',
    'read_file',
    'write_file',
    'ensure_file',
    'read_file_context',  // 批量读取文件上下文
    'search_files',       // 搜索文件内容
    'modify_file'         // 精确修改文件
  ])

  // 计算属性：按任务分组的消息
  const groupedMessages = computed(() => {
    const groups = []
    let currentGroup = { taskId: null, messages: [] }
    
    messages.value.forEach(msg => {
      if (msg.taskId !== currentGroup.taskId) {
        if (currentGroup.messages.length > 0) {
          groups.push(currentGroup)
        }
        currentGroup = { taskId: msg.taskId, messages: [] }
      }
      currentGroup.messages.push(msg)
    })
    
    if (currentGroup.messages.length > 0) {
      groups.push(currentGroup)
    }
    
    return groups
  })

  // 计算属性：按日期分组的会话
  const groupedSessions = computed(() => {
    const groups = {
      '今天': [],
      '昨天': [],
      '最近 7 天': [],
      '更早': []
    }
    
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    const lastWeek = new Date(today)
    lastWeek.setDate(lastWeek.getDate() - 7)
    
    sessions.value.forEach(s => {
      const date = new Date(s.createdAt)
      if (date >= today) groups['今天'].push(s)
      else if (date >= yesterday) groups['昨天'].push(s)
      else if (date >= lastWeek) groups['最近 7 天'].push(s)
      else groups['更早'].push(s)
    })
    
    return Object.entries(groups).filter(([_, items]) => items.length > 0)
  })

  // 规范化会话数据
  const normalizeSession = (raw) => {
    if (!raw) return null
    return {
      sessionId: raw.sessionId ?? raw.session_id,
      title: raw.title,
      createdAt: raw.createdAt ?? raw.created_at,
      sessionType: raw.sessionType ?? raw.session_type,
      localOnly: raw.localOnly === true,
      currentCwd: raw.currentCwd
    }
  }

  // 获取会话列表
  const fetchSessions = async () => {
    if (!authStore.token) return
    try {
      const data = await request.get('/api/terminal/sessions')
      if (data?.code === 200) {
        sessions.value = (data.data || []).map(normalizeSession)
        return sessions.value
      }
    } catch (e) {
      console.error('Fetch sessions failed:', e)
    }
    return []
  }

  // 创建新会话
  const createNewSession = async () => {
    try {
      const data = await request.post('/api/terminal/new-session')
      if (data?.code === 200) {
        const newSession = normalizeSession(data.data)
        sessions.value.unshift(newSession)
        // Reset state
        agentStatus.value = 'IDLE'
        decisionHistory.value.clear()
        // 解耦架构：清除解耦相关状态
        identityInfo.value = null
        stateSlices.value = []
        visibleFiles.value = []
        visibleFunctions.value = []
        return newSession
      }
    } catch (e) {
      console.error('Create session failed:', e)
    }
    return null
  }

  // 删除会话
  const deleteSession = async (sessionId) => {
    try {
      const data = await request.delete(`/api/terminal/sessions/${sessionId}`)
      if (data?.code === 200) {
        sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
        if (currentSessionId.value === sessionId) {
          currentSessionId.value = null
          messages.value = []
          terminalLogs.value = []
          currentTasks.value = []
          agentStatus.value = 'IDLE'
          decisionHistory.value.clear()
        }
        return true
      }
    } catch (e) {
      console.error('Delete session failed:', e)
    }
    return false
  }

  // 选择会话并加载历史记录
  const selectSession = async (sessionId) => {
    currentSessionId.value = sessionId
    messages.value = []
    terminalLogs.value = []
    currentTasks.value = []
    activeTaskId.value = null
    agentStatus.value = 'IDLE' 
    decisionHistory.value.clear()
    // 解耦架构：清除解耦相关状态
    identityInfo.value = null
    stateSlices.value = []
    visibleFiles.value = []
    visibleFunctions.value = []
    
    const session = sessions.value.find(s => s.sessionId === sessionId)
    if (session && session.currentCwd) {
      currentCwd.value = session.currentCwd
    } else {
      currentCwd.value = '/'
    }

    try {
      const data = await request.get(`/api/terminal/history/${sessionId}`)
      if (data?.code === 200) {
        const records = data.data || []
        const newMessages = []
        const newLogs = []
        
        let lastCommandAction = null
        let trackedTaskId = null

        records.forEach(r => {
          const senderType = r.senderType ?? r.sender_type
          const content = r.content

          if (senderType === 1) {
            newMessages.push({ role: 'user', content: content, taskId: trackedTaskId })
            lastCommandAction = null
          } else if (senderType === 2) {
            try {
              if (content && content.trim().startsWith('{')) {
                const action = JSON.parse(content)
                // Restore Task State
                if (action.type === 'task_list' || action.tasks) {
                  currentTasks.value = action.tasks || []
                } else if (action.type === 'task_update') {
                  const taskIndex = currentTasks.value.findIndex(t => String(t.id) === String(action.taskId))
                  if (taskIndex !== -1) {
                    currentTasks.value[taskIndex].status = action.status
                    if (action.status === 'in_progress') {
                      trackedTaskId = action.taskId
                      activeTaskId.value = action.taskId
                    }
                  }
                } else if (action.decision_id) {
                    decisionHistory.value.add(action.decision_id)
                }
                
                const msg = { 
                  role: 'ai', 
                  thought: action.thought, 
                  message: action.content || action.message, 
                  steps: action.steps || [], 
                  tool: action.tool || (action.type === 'TOOL_CALL' ? action.action : null), 
                  command: action.command || (action.params ? action.params.command : null),
                  filePath: action.path || (action.params ? action.params.path : null),
                  status: action.exitCode === 0 ? 'success' : 'error',
                  taskId: trackedTaskId
                }
                newMessages.push(msg)
                
                if (msg.tool) {
                  lastCommandAction = msg
                }
              } else {
                newMessages.push({ role: 'ai', message: content, taskId: trackedTaskId })
                lastCommandAction = null
              }
            } catch (e) {
              newMessages.push({ role: 'ai', message: content, taskId: trackedTaskId })
              lastCommandAction = null
            }
          } else if (senderType === 3) {
            const exitCode = r.exitCode ?? r.exit_code ?? 0
            const stdout = r.stdout || ''
            const stderr = r.stderr || ''
            const output = stdout || stderr || content

            if (lastCommandAction) {
              newLogs.push({
                command: lastCommandAction.command || (lastCommandAction.tool === 'write_file' || lastCommandAction.tool === 'ensure_file' ? `write_file: ${lastCommandAction.filePath}` : 'unknown'),
                output: output,
                type: exitCode === 0 ? 'stdout' : 'stderr',
                cwd: lastCommandAction.cwd || '/'
              })
            } else {
              newLogs.push({
                command: 'Command Result',
                output: output,
                type: exitCode === 0 ? 'stdout' : 'stderr',
                cwd: '/'
              })
            }
          }
        })
        
        messages.value = newMessages
        terminalLogs.value = newLogs
        return true
      }
    } catch (e) {
      console.error('Fetch history failed:', e)
    }
    return false
  }
  
  const setAgentStatus = (status) => {
      agentStatus.value = status
  }
  
  const addDecision = (id) => {
      decisionHistory.value.add(id)
  }
  
  const hasDecision = (id) => {
      return decisionHistory.value.has(id)
  }

  // 解耦架构：身份管理方法
  const setIdentity = (identity) => {
    identityInfo.value = identity
  }

  const clearIdentity = () => {
    identityInfo.value = null
  }

  // 解耦架构：状态切片管理方法
  const addStateSlice = (slice) => {
    stateSlices.value.push({
      ...slice,
      timestamp: new Date().toISOString()
    })
    // 保持最近 100 个切片
    if (stateSlices.value.length > 100) {
      stateSlices.value.shift()
    }
  }

  const clearStateSlices = () => {
    stateSlices.value = []
  }

  // 解耦架构：作用域管理方法
  const setScope = (files, functions) => {
    visibleFiles.value = files || []
    visibleFunctions.value = functions || []
  }

  const clearScope = () => {
    visibleFiles.value = []
    visibleFunctions.value = []
  }

  // 解耦架构：工具执行策略检查
  const canExecuteTool = (action) => {
    return toolWhitelist.value.includes(action)
  }

  const addToolToWhitelist = (action) => {
    if (!toolWhitelist.value.includes(action)) {
      toolWhitelist.value.push(action)
    }
  }

  const removeToolFromWhitelist = (action) => {
    const index = toolWhitelist.value.indexOf(action)
    if (index > -1) {
      toolWhitelist.value.splice(index, 1)
    }
  }

  // --- Phase 1-5 Refactoring Features ---

  // Checkpoints
  const loadCheckpoints = async () => {
    if (!currentSessionId.value) return
    try {
      const res = await terminalService.checkpoint.getCheckpoints(currentSessionId.value)
      if (res.code === 200) {
        checkpoints.value = res.data || []
      }
    } catch (e) {
      console.error('Failed to load checkpoints:', e)
    }
  }

  const createCheckpoint = async (description) => {
    if (!currentSessionId.value) return
    try {
      await terminalService.checkpoint.createCheckpoint({
        sessionId: currentSessionId.value,
        messageOrder: messages.value.length, // Approximate order
        description
      })
      await loadCheckpoints()
      return true
    } catch (e) {
      console.error('Failed to create checkpoint:', e)
      return false
    }
  }

  const restoreCheckpoint = async (checkpointId) => {
    try {
      const res = await terminalService.checkpoint.jumpToCheckpoint(checkpointId)
      if (res.code === 200) {
        // Reload history after jump
        await selectSession(currentSessionId.value)
        await loadCheckpoints()
        return true
      }
    } catch (e) {
      console.error('Failed to restore checkpoint:', e)
    }
    return false
  }

  const removeCheckpoint = async (checkpointId) => {
    try {
      await terminalService.checkpoint.deleteCheckpoint(checkpointId)
      await loadCheckpoints()
      return true
    } catch (e) {
      console.error('Failed to delete checkpoint:', e)
      return false
    }
  }

  // Approvals
  const loadPendingApprovals = async () => {
    if (!currentSessionId.value) return
    try {
      const res = await terminalService.approval.getPendingApprovals(currentSessionId.value)
      if (res.code === 200) {
        pendingApprovals.value = res.data || []
        if (pendingApprovals.value.length > 0) {
            agentStatus.value = 'AWAITING_APPROVAL'
        }
      }
    } catch (e) {
      console.error('Failed to load pending approvals:', e)
    }
  }

  const handleApproval = async (decisionId, approved, reason = '') => {
    try {
      if (approved) {
        await terminalService.approval.approveToolCall(decisionId, reason)
      } else {
        await terminalService.approval.rejectToolCall(decisionId, reason)
      }
      // Remove from local list immediately for UI responsiveness
      pendingApprovals.value = pendingApprovals.value.filter(p => p.id !== decisionId)
      
      // If no more approvals, status might change (will be updated by next poll or SSE)
      if (pendingApprovals.value.length === 0 && agentStatus.value === 'AWAITING_APPROVAL') {
          agentStatus.value = 'RUNNING' 
      }
      return true
    } catch (e) {
      console.error('Approval action failed:', e)
      return false
    }
  }

  // Session State
  const checkSessionStatus = async () => {
      if (!currentSessionId.value) return
      try {
          const res = await terminalService.sessionState.getSessionState(currentSessionId.value)
          if (res.code === 200) {
              const state = res.data
              agentStatus.value = state.status
              // If status is awaiting approval, load approvals
              if (state.status === 'AWAITING_APPROVAL') {
                  loadPendingApprovals()
              }
          }
      } catch (e) {
          console.error('Check status failed:', e)
      }
  }

  const interruptSession = async () => {
      if (!currentSessionId.value) return
      try {
          await terminalService.sessionState.interruptAgentLoop(currentSessionId.value)
          // Optimistic update
          agentStatus.value = 'PAUSED' // or whatever the backend sets
          return true
      } catch (e) {
          console.error('Interrupt failed:', e)
          return false
      }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    terminalLogs,
    currentTasks,
    activeTaskId,
    groupedMessages,
    currentCwd,
    isLoading,
    agentStatus, 
    decisionHistory, 
    groupedSessions,
    fetchSessions,
    createNewSession,
    deleteSession,
    selectSession,
    setAgentStatus,
    addDecision,
    hasDecision,
    // New features
    checkpoints,
    pendingApprovals,
    loadCheckpoints,
    createCheckpoint,
    restoreCheckpoint,
    removeCheckpoint,
    loadPendingApprovals,
    handleApproval,
    checkSessionStatus,
    interruptSession,
    // 解耦架构：导出新方法
    identityInfo,
    setIdentity,
    clearIdentity,
    stateSlices,
    addStateSlice,
    clearStateSlices,
    visibleFiles,
    visibleFunctions,
    setScope,
    clearScope,
    toolWhitelist,
    canExecuteTool,
    addToolToWhitelist,
    removeToolFromWhitelist
  }
})
