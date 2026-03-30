import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as agentService from '@/services/agentService'

/**
 * Agent 状态管理 Store
 */
export const useAgentStore = defineStore('agent', () => {
  // 状态
  const sessions = ref([])
  const currentSession = ref(null)
  const tasks = ref([])
  const currentTask = ref(null)
  const toolCalls = ref([])
  const fileTree = ref([])
  const isLoading = ref(false)
  const isStreaming = ref(false)
  const error = ref(null)
  const messages = ref([])
  const terminalOutput = ref([])

  // 计算属性
  const activeSessions = computed(() =>
    sessions.value.filter(s => s.status === 'active')
  )

  const runningTasks = computed(() =>
    tasks.value.filter(t => t.status === 'running')
  )

  const completedTasks = computed(() =>
    tasks.value.filter(t => t.status === 'completed')
  )

  const currentTaskToolCalls = computed(() =>
    toolCalls.value.filter(tc => tc.taskId === currentTask.value?.id)
  )

  // 会话操作
  async function fetchSessions() {
    try {
      isLoading.value = true
      const response = await agentService.getSessions()
      sessions.value = response.data || []
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function createSession(name, workingDirectory) {
    try {
      isLoading.value = true
      const response = await agentService.createSession({ name, workingDirectory })
      const session = response.data
      sessions.value.unshift(session)
      currentSession.value = session
      return session
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function selectSession(sessionId) {
    try {
      isLoading.value = true
      const response = await agentService.getSession(sessionId)
      currentSession.value = response.data

      // 加载该会话的任务
      await fetchTasks(sessionId)

      // 加载文件树
      await fetchFileTree(sessionId)
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function closeSession(sessionId) {
    try {
      await agentService.closeSession(sessionId)
      const session = sessions.value.find(s => s.id === sessionId)
      if (session) {
        session.status = 'closed'
      }
      if (currentSession.value?.id === sessionId) {
        currentSession.value = null
      }
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  async function deleteSession(sessionId) {
    try {
      await agentService.deleteSession(sessionId)
      sessions.value = sessions.value.filter(s => s.id !== sessionId)
      if (currentSession.value?.id === sessionId) {
        currentSession.value = null
      }
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  // 任务操作
  async function fetchTasks(sessionId) {
    try {
      const response = await agentService.getTasksBySession(sessionId)
      tasks.value = response.data || []
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  async function createTask(input, taskType = 'general') {
    if (!currentSession.value) {
      throw new Error('请先选择或创建会话')
    }

    try {
      isLoading.value = true
      const response = await agentService.createTask({
        sessionId: currentSession.value.id,
        taskType,
        input
      })
      const task = response.data
      tasks.value.unshift(task)
      currentTask.value = task
      return task
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function startTaskStream(taskId) {
    isStreaming.value = true
    currentTask.value = tasks.value.find(t => t.id === taskId)

    try {
      await agentService.streamTask(taskId, {
        onStart: (data) => {
          addMessage('system', `任务开始: ${data.taskId}`)
        },
        onProgress: (data) => {
          if (currentTask.value) {
            currentTask.value.currentStep = data.currentStep
            currentTask.value.totalSteps = data.totalSteps
          }
          addMessage('progress', `进度: ${data.currentStep}/${data.totalSteps}`)
        },
        onToolCall: (data) => {
          toolCalls.value.push(data)
          addMessage('tool', data)
        },
        onOutput: (data) => {
          // 后端/执行层用 \\n 转义换行，前端渲染前还原
          const normalized = typeof data === 'string' ? data.replace(/\\n/g, '\n') : data
          addTerminalOutput(normalized)
        },
        onComplete: (data) => {
          if (currentTask.value) {
            currentTask.value.status = 'completed'
            const normalized = typeof data === 'string' ? data.replace(/\\n/g, '\n') : data
            currentTask.value.output = normalized
          }
          addMessage('system', '任务完成')
          isStreaming.value = false
        },
        onError: (data) => {
          if (currentTask.value) {
            currentTask.value.status = 'failed'
            currentTask.value.errorMessage = data.error
          }
          addMessage('error', data.error)
          error.value = data.error
          isStreaming.value = false
        }
      })
    } catch (e) {
      error.value = e.message
      isStreaming.value = false
      throw e
    }
  }

  async function cancelTask(taskId) {
    try {
      await agentService.cancelTask(taskId)
      const task = tasks.value.find(t => t.id === taskId)
      if (task) {
        task.status = 'cancelled'
      }
      if (currentTask.value?.id === taskId) {
        currentTask.value.status = 'cancelled'
      }
      isStreaming.value = false
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  // 文件操作
  async function fetchFileTree(sessionId) {
    try {
      const response = await agentService.getFileTree(sessionId)
      fileTree.value = response.data || []
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  async function readFile(sessionId, filePath, startLine = 1, endLine = 100) {
    try {
      const response = await agentService.readFile(sessionId, filePath, startLine, endLine)
      return response.data
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  async function editFile(sessionId, filePath, anchor, range, content) {
    try {
      const response = await agentService.editFile(sessionId, {
        path: filePath,
        anchor,
        range,
        content
      })
      // 刷新文件树
      await fetchFileTree(sessionId)
      return response.data
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  async function searchFiles(sessionId, query, glob = '**/*') {
    try {
      const response = await agentService.searchFiles(sessionId, { query, glob })
      return response.data
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  // 终端操作
  async function executeCommand(sessionId, command, isLongRunning = false) {
    try {
      const response = await agentService.executeCommand(sessionId, { command, isLongRunning })
      return response.data
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  // 消息管理
  function addMessage(type, content) {
    messages.value.push({
      id: Date.now(),
      type,
      content,
      timestamp: new Date().toISOString()
    })
  }

  function addTerminalOutput(output) {
    terminalOutput.value.push({
      id: Date.now(),
      content: output,
      timestamp: new Date().toISOString()
    })
  }

  function clearMessages() {
    messages.value = []
  }

  function clearTerminalOutput() {
    terminalOutput.value = []
  }

  // 重置状态
  function reset() {
    sessions.value = []
    currentSession.value = null
    tasks.value = []
    currentTask.value = null
    toolCalls.value = []
    fileTree.value = []
    messages.value = []
    terminalOutput.value = []
    error.value = null
    isStreaming.value = false
  }

  return {
    // 状态
    sessions,
    currentSession,
    tasks,
    currentTask,
    toolCalls,
    fileTree,
    isLoading,
    isStreaming,
    error,
    messages,
    terminalOutput,

    // 计算属性
    activeSessions,
    runningTasks,
    completedTasks,
    currentTaskToolCalls,

    // 会话操作
    fetchSessions,
    createSession,
    selectSession,
    closeSession,
    deleteSession,

    // 任务操作
    fetchTasks,
    createTask,
    startTaskStream,
    cancelTask,

    // 文件操作
    fetchFileTree,
    readFile,
    editFile,
    searchFiles,

    // 终端操作
    executeCommand,

    // 消息管理
    addMessage,
    addTerminalOutput,
    clearMessages,
    clearTerminalOutput,

    // 重置
    reset
  }
})
