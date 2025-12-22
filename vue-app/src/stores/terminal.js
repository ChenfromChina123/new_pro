import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { API_CONFIG } from '@/config/api'

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
  const agentStatus = ref('IDLE') // IDLE, RUNNING, PAUSED, ERROR, etc.
  const decisionHistory = ref(new Set()) // decision_id set for de-duplication

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
            if (lastCommandAction) {
              newLogs.push({
                command: lastCommandAction.command || (lastCommandAction.tool === 'write_file' || lastCommandAction.tool === 'ensure_file' ? `write_file: ${lastCommandAction.filePath}` : 'unknown'),
                output: content,
                type: 'stdout',
                cwd: lastCommandAction.cwd || '/'
              })
            } else {
              newLogs.push({
                command: 'Command Result',
                output: content,
                type: 'stdout',
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
    hasDecision
  }
})
