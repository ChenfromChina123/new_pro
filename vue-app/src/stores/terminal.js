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
  const currentCwd = ref('/')
  const isLoading = ref(false)

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
        
        // 用于临时存储上一个 AI 命令，以便关联结果
        let lastCommandAction = null

        records.forEach(r => {
          const senderType = r.senderType ?? r.sender_type
          const content = r.content

          if (senderType === 1) {
            newMessages.push({ role: 'user', content: content })
            lastCommandAction = null
          } else if (senderType === 2) {
            try {
              if (content && content.trim().startsWith('{')) {
                const action = JSON.parse(content)
                if (action.type === 'task_list' || action.tasks) {
                  currentTasks.value = action.tasks || []
                } else if (action.type === 'task_update') {
                  const taskIndex = currentTasks.value.findIndex(t => String(t.id) === String(action.taskId))
                  if (taskIndex !== -1) {
                    currentTasks.value[taskIndex].status = action.status
                  }
                }
                
                const msg = { 
                  role: 'ai', 
                  thought: action.thought, 
                  message: action.message, 
                  tool: action.tool, 
                  command: action.command,
                  filePath: action.path,
                  status: action.exitCode === 0 ? 'success' : 'error'
                }
                newMessages.push(msg)
                
                // 如果是工具调用，记录下来以便关联结果
                if (action.tool === 'execute_command' || action.tool === 'write_file') {
                  lastCommandAction = action
                }
              } else {
                newMessages.push({ role: 'ai', message: content })
                lastCommandAction = null
              }
            } catch (e) {
              newMessages.push({ role: 'ai', message: content })
              lastCommandAction = null
            }
          } else if (senderType === 3) {
            // terminal_output 记录不再存入 messages (聊天框)，仅用于 terminalLogs
            // newMessages.push({ role: 'command_result', content: content })
            
            // 关联到 terminalLogs
            if (lastCommandAction) {
              newLogs.push({
                command: lastCommandAction.command || (lastCommandAction.tool === 'write_file' ? `write_file: ${lastCommandAction.path}` : 'unknown'),
                output: content,
                type: 'stdout', // 历史记录中暂未区分 stdout/stderr
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

  return {
    sessions,
    currentSessionId,
    messages,
    terminalLogs,
    currentTasks,
    currentCwd,
    isLoading,
    groupedSessions,
    fetchSessions,
    createNewSession,
    deleteSession,
    selectSession
  }
})
