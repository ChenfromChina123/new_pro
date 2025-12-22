<template>
  <div class="terminal-container">
    <!-- Sessions Sidebar -->
    <div
      class="sessions-sidebar"
      :class="{ collapsed: sidebarCollapsed }"
    >
      <div class="sidebar-header">
        <h3>终端会话</h3>
        <button
          class="new-session-btn"
          title="新建会话"
          @click="createNewSession"
        >
          <span class="btn-icon">+</span>
        </button>
      </div>
      <div class="sessions-list">
        <div 
          v-for="session in sessions" 
          :key="session.sessionId" 
          class="session-item" 
          :class="{ active: currentSessionId === session.sessionId }"
          @click="selectSession(session.sessionId)"
        >
          <div class="session-info-wrapper">
            <div class="session-title">
              {{ session.title || '未命名会话' }}
            </div>
            <div class="session-time">
              {{ formatDate(session.createdAt) }}
            </div>
          </div>
          <button
            class="delete-session-btn"
            title="删除会话"
            @click.stop="deleteSession(session.sessionId)"
          >
            <span class="btn-icon">🗑️</span>
          </button>
        </div>
      </div>
    </div>

    <div class="terminal-main">
      <div class="terminal-layout">
        <!-- Left Panel: Chat -->
        <div class="chat-panel">
          <div class="chat-header">
            <div class="header-left">
              <button
                class="toggle-sidebar"
                @click="sidebarCollapsed = !sidebarCollapsed"
              >
                <span class="btn-icon">☰</span>
              </button>
              <h3>AI 终端助手</h3>
            </div>
            <div class="model-selector">
              <select v-model="currentModel">
                <option value="deepseek-chat">
                  DeepSeek Chat
                </option>
                <option value="deepseek-reasoner">
                  DeepSeek Reasoner
                </option>
                <option value="doubao-pro-32k">
                  豆包 Pro
                </option>
              </select>
            </div>
          </div>
          
          <div
            ref="messagesRef"
            class="messages-container"
          >
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message"
              :class="msg.role"
            >
              <div class="message-content">
                <!-- User Message -->
                <div
                  v-if="msg.role === 'user'"
                  class="user-bubble"
                >
                  {{ msg.content }}
                </div>

                <!-- AI Message -->
                <div
                  v-else-if="msg.role === 'ai'"
                  class="ai-bubble"
                >
                  <div
                    v-if="msg.thought"
                    class="thought-block"
                  >
                    <div
                      class="thought-title"
                      @click="msg.showThought = !msg.showThought"
                    >
                      <span>思考过程</span>
                      <i class="toggle-icon">{{ msg.showThought ? '▼' : '▶' }}</i>
                    </div>
                    <div
                      v-if="msg.showThought"
                      class="thought-content"
                    >
                      {{ msg.thought }}
                    </div>
                  </div>
                  <div
                    v-if="msg.message"
                    class="ai-text"
                  >
                    {{ msg.message }}
                  </div>
                  
                  <!-- Command Execution Info in Chat -->
                  <div
                    v-if="msg.tool === 'execute_command'"
                    class="tool-call-card"
                  >
                    <div class="tool-header">
                      <span class="tool-icon">🐚</span>
                      <span class="tool-label">执行命令</span>
                    </div>
                    <div class="tool-command">
                      <code>{{ msg.command }}</code>
                    </div>
                    <div
                      class="tool-status"
                      :class="msg.status"
                    >
                      <span
                        v-if="msg.status === 'pending'"
                        class="spinner"
                      >⌛ 执行中...</span>
                      <span
                        v-else-if="msg.status === 'success'"
                        class="status-success"
                      >✓ 执行成功</span>
                      <span
                        v-else
                        class="status-error"
                      >✗ 执行失败</span>
                    </div>
                  </div>
                </div>

                <!-- Command Result Message (Special) -->
                <div
                  v-else-if="msg.role === 'command_result'"
                  class="system-bubble"
                >
                  <div class="result-header">
                    命令执行结果:
                  </div>
                  <pre class="result-content">{{ msg.content }}</pre>
                </div>
              </div>
            </div>
            <div
              v-if="isTyping"
              class="message ai"
            >
              <div class="typing-indicator">
                <span>.</span><span>.</span><span>.</span>
              </div>
            </div>
          </div>

          <div class="input-area">
            <textarea 
              v-model="inputMessage" 
              placeholder="输入指令，例如：创建一个Vue项目..."
              :disabled="isTyping || isExecuting"
              @keydown.enter.prevent="handleEnter"
            />
            <button
              class="send-btn"
              :disabled="!inputMessage.trim() || isTyping || isExecuting"
              @click="sendMessage"
            >
              发送
            </button>
          </div>
        </div>

        <!-- Right Panel: Terminal Log -->
        <div class="terminal-panel">
          <div class="terminal-header">
            <div class="terminal-tabs">
              <div class="tab active">
                Terminal Output
              </div>
            </div>
            <button
              class="clear-btn"
              @click="clearTerminal"
            >
              Clear
            </button>
          </div>
          <div
            ref="terminalRef"
            class="terminal-content"
          >
            <div
              v-for="(log, index) in terminalLogs"
              :key="index"
              class="log-line"
            >
              <div class="log-cmd-line">
                <span class="prompt">➜</span>
                <span class="cwd">~</span>
                <span class="cmd">{{ log.command }}</span>
              </div>
              <pre
                class="output"
                :class="log.type"
              >{{ log.output }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { API_CONFIG } from '@/config/api'

const authStore = useAuthStore()
const uiStore = useUIStore()
const messages = ref([])
const terminalLogs = ref([])
const inputMessage = ref('')
const currentModel = ref('deepseek-chat')
const isTyping = ref(false)
const isExecuting = ref(false)
const messagesRef = ref(null)
const terminalRef = ref(null)

const currentSessionId = ref(null)
const sessions = ref([])
const sidebarCollapsed = ref(false)

/**
 * 将后端 snake_case 的会话对象规范化为前端统一结构
 * @param {any} raw 会话原始数据
 * @returns {{sessionId: string, title?: string, createdAt?: string, sessionType?: string, localOnly?: boolean}} 规范化会话
 */
const normalizeSession = (raw) => {
  if (!raw) return null
  return {
    sessionId: raw.sessionId ?? raw.session_id,
    title: raw.title,
    createdAt: raw.createdAt ?? raw.created_at,
    sessionType: raw.sessionType ?? raw.session_type,
    localOnly: raw.localOnly === true
  }
}

/**
 * 将后端 snake_case 的聊天记录对象规范化为前端统一结构
 * @param {any} raw 记录原始数据
 * @returns {{senderType: number, content: string}} 规范化记录
 */
const normalizeRecord = (raw) => {
  if (!raw) return null
  return {
    senderType: raw.senderType ?? raw.sender_type,
    content: raw.content
  }
}

/**
 * 安全解析 JSON 响应，避免 401/500 返回非 JSON 导致页面无提示
 * @param {Response} res fetch 响应
 * @returns {Promise<any|null>} 解析成功返回对象，否则返回 null
 */
const safeReadJson = async (res) => {
  try {
    return await res.json()
  } catch {
    return null
  }
}

onMounted(async () => {
  try {
    await fetchSessions()
    if (sessions.value.length > 0) {
      const firstSession = sessions.value[0]
      if (firstSession && firstSession.sessionId) {
        await selectSession(firstSession.sessionId)
      }
    } else {
      await createNewSession()
    }
  } catch (error) {
    console.error('Initialization error:', error)
    uiStore.showToast('初始化失败，请刷新页面')
  }
})

/**
 * 拉取会话列表，并与本地临时会话合并
 */
const fetchSessions = async () => {
  try {
    if (!authStore.token) {
      uiStore.showToast('未登录或登录已失效')
      return
    }
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/sessions`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (!data || data.code !== 200) {
      uiStore.showToast(data?.message || '会话列表加载失败')
      return
    }

    const serverSessionsRaw = Array.isArray(data.data) ? data.data : []
    const serverSessions = serverSessionsRaw.map(normalizeSession).filter(s => s?.sessionId)
    const localSessions = sessions.value
      .map(normalizeSession)
      .filter(s => s && s.localOnly && s.sessionId)

    const serverSessionIdSet = new Set(serverSessions.map(s => s.sessionId))
    const remainingLocal = localSessions.filter(s => !serverSessionIdSet.has(s.sessionId))
    sessions.value = [...remainingLocal, ...serverSessions]
  } catch (error) {
    console.error('Failed to fetch sessions:', error)
    uiStore.showToast('会话列表加载失败')
  }
}

/**
 * 切换当前会话并加载历史记录
 * @param {string} sessionId 会话ID
 */
const selectSession = async (sessionId) => {
  if (!sessionId) {
    uiStore.showToast('会话ID无效')
    return
  }
  currentSessionId.value = sessionId
  messages.value = []
  terminalLogs.value = []
  
  try {
    if (!authStore.token) {
      uiStore.showToast('未登录或登录已失效')
      return
    }
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/history/${sessionId}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (!data || data.code !== 200) {
      uiStore.showToast(data?.message || '会话历史加载失败')
      return
    }
      // Map history to view messages
      const normalizedRecords = (Array.isArray(data.data) ? data.data : []).map(normalizeRecord).filter(Boolean)
      messages.value = normalizedRecords.map(record => {
        if (record.senderType === 1) return { role: 'user', content: record.content }
        if (record.senderType === 2) {
          // Try to parse JSON from AI response if it contains tool calls
          try {
            const jsonMatch = record.content.match(/\{[\s\S]*\}/)
            if (jsonMatch) {
              const action = JSON.parse(jsonMatch[0])
              return { 
                role: 'ai', 
                thought: action.thought, 
                message: action.message, 
                tool: action.tool, 
                command: action.command,
                status: 'success', // Historical commands are assumed success if we don't store status
                showThought: false
              }
            }
          } catch (e) {}
          return { role: 'ai', message: record.content, showThought: false }
        }
        if (record.senderType === 3) return { role: 'command_result', content: record.content }
        return null
      }).filter(Boolean)
      
      // Also populate terminal logs from history if they were commands
      let lastCommand = ''
      normalizedRecords.forEach(record => {
        if (record.senderType === 2) {
          try {
            const jsonMatch = record.content.match(/\{[\s\S]*\}/)
            if (jsonMatch) {
              const action = JSON.parse(jsonMatch[0])
              if (action.tool === 'execute_command') {
                lastCommand = action.command
              }
            }
          } catch (e) {}
        } else if (record.senderType === 3 && lastCommand) {
          terminalLogs.value.push({
            command: lastCommand,
            output: record.content,
            type: 'stdout' // Simplified, we don't store stderr separately in history yet
          })
          lastCommand = '' // Reset
        }
      })
      
      scrollToBottom()
      uiStore.showToast('会话已切换', 1200)
  } catch (error) {
    console.error('Failed to fetch history:', error)
    uiStore.showToast('会话历史加载失败')
  }
}

/**
 * 新建终端会话（优先走后端，失败则创建本地临时会话）
 */
const createNewSession = async () => {
  try {
    if (!authStore.token) {
      uiStore.showToast('未登录或登录已失效')
      return
    }
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/new-session`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    const newSession = normalizeSession(data?.data)
    if (data?.code === 200 && newSession?.sessionId) {
      sessions.value = [newSession, ...sessions.value.filter(s => s?.sessionId !== newSession.sessionId)]
      await selectSession(newSession.sessionId)
      uiStore.showToast('会话创建成功')
      return
    }
  } catch (error) {
    console.error('Failed to create session:', error)
  }

  const newSession = {
    sessionId: 'term_' + Date.now(),
    title: '未命名会话',
    createdAt: new Date().toISOString(),
    sessionType: 'terminal',
    localOnly: true
  }
  sessions.value = [newSession, ...sessions.value]
  await selectSession(newSession.sessionId)
  uiStore.showToast('已创建本地临时会话')
}

/**
 * 删除终端会话（本地临时会话直接移除；服务端会话调用删除接口）
 * @param {string} sessionId 会话ID
 */
const deleteSession = async (sessionId) => {
  if (!sessionId) {
    uiStore.showToast('会话ID无效')
    return
  }
  if (!confirm('确定要删除这个会话吗？')) return
  
  try {
    const localIndex = sessions.value.findIndex(s => s?.sessionId === sessionId)
    const localSession = localIndex >= 0 ? sessions.value[localIndex] : null

    if (localSession?.localOnly) {
      sessions.value = sessions.value.filter(s => s?.sessionId !== sessionId)
      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          await selectSession(sessions.value[0].sessionId)
        } else {
          await createNewSession()
        }
      }
      uiStore.showToast('会话已删除')
      return
    }

    if (!authStore.token) {
      uiStore.showToast('未登录或登录已失效')
      return
    }
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      sessions.value = sessions.value.filter(s => s?.sessionId !== sessionId)
      await fetchSessions()
      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          await selectSession(sessions.value[0].sessionId)
        } else {
          await createNewSession()
        }
      }
      uiStore.showToast('会话已删除')
    } else {
      uiStore.showToast(data?.message || '删除会话失败')
    }
  } catch (error) {
    console.error('Failed to delete session:', error)
    uiStore.showToast('删除会话失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight
    }
  })
}

const clearTerminal = () => {
  terminalLogs.value = []
}

const appendTerminalLog = (command, output, type = 'stdout') => {
  terminalLogs.value.push({ command, output, type })
  scrollToBottom()
}

const handleEnter = (e) => {
  if (e.shiftKey) return
  e.preventDefault()
  sendMessage()
}

const saveMessage = async (content, senderType) => {
  try {
    await fetch(`${API_CONFIG.baseURL}/api/terminal/save-record`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        session_id: currentSessionId.value,
        content: content,
        sender_type: senderType,
        model: currentModel.value
      })
    })
    // Refresh sessions list to show new title/session if it's new
    fetchSessions()
  } catch (error) {
    console.error('Failed to save message:', error)
  }
}

const sendMessage = async () => {
  const text = inputMessage.value.trim()
  if (!text) return

  messages.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  isTyping.value = true
  scrollToBottom()

  // Save user message
  await saveMessage(text, 1)

  await processAgentLoop(text)
}

const processAgentLoop = async (prompt) => {
  console.log('=== processAgentLoop Started ===', prompt)
  try {
    const response = await fetch(`${API_CONFIG.baseURL}/api/terminal/chat-stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        prompt: prompt,
        session_id: currentSessionId.value,
        model: currentModel.value
      })
    })

    if (!response.ok) {
      const errorData = await safeReadJson(response)
      throw new Error(errorData?.message || '网络请求失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentAiMsg = { 
      role: 'ai', 
      thought: '', 
      message: '', 
      tool: null, 
      command: '', 
      status: 'pending', 
      showThought: true 
    }
    messages.value.push(currentAiMsg)
    
    let fullContent = ''
    let currentEvent = null

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      const lines = (buffer + chunk).split('\n')
      buffer = lines.pop()

      for (const line of lines) {
        const trimmedLine = line.trim()
        if (!trimmedLine) {
          currentEvent = null
          continue
        }

        if (trimmedLine.startsWith('event: ')) {
          currentEvent = trimmedLine.slice(7)
          continue
        }

        if (trimmedLine.startsWith('data: ')) {
          const data = trimmedLine.slice(6)
          console.log('SSE Data:', data)
          if (data === '[DONE]') continue
          
          try {
            const json = JSON.parse(data)
            
            // Handle session_update event
            if (currentEvent === 'session_update') {
              if (json.title) {
                const session = sessions.value.find(s => s.sessionId === currentSessionId.value)
                if (session) session.title = json.title
              }
              currentEvent = null // Reset event after processing
              continue
            }

            // Handle normal content (might be partial or JSON)
            if (json.content || json.reasoning_content) {
              if (json.reasoning_content) {
                currentAiMsg.thought += json.reasoning_content
              }
              if (json.content) {
                fullContent += json.content
                // Improved logic to detect if we are likely streaming a JSON block
                const trimmedFull = fullContent.trim()
                const looksLikeJson = trimmedFull.startsWith('{') || trimmedFull.startsWith('```')
                
                if (!looksLikeJson) {
                  currentAiMsg.message = fullContent
                } else {
                  // While streaming what looks like JSON/Markdown, show a hint
                  currentAiMsg.message = 'AI 正在思考并生成指令...'
                }
              }
            }
          } catch (e) {
            console.warn('Failed to parse SSE data:', data, e)
          }
        }
      }
    }
    
    isTyping.value = false
    
    // Process full content once stream ends
    const trimmedFull = fullContent.trim()
    const looksLikeJson = trimmedFull.startsWith('{') || trimmedFull.includes('```json') || trimmedFull.includes('```')
    
    if (looksLikeJson) {
      try {
        // Try to find the JSON part - it might be wrapped in markdown or have extra text
        let jsonStr = trimmedFull
        
        // Handle ```json ... ``` or ``` ... ```
        const markdownMatch = trimmedFull.match(/```(?:json)?\s*([\s\S]*?)\s*```/)
        if (markdownMatch) {
          jsonStr = markdownMatch[1]
        } else {
          // Fallback: just find the first { and last }
          const startBrace = trimmedFull.indexOf('{')
          const endBrace = trimmedFull.lastIndexOf('}')
          if (startBrace !== -1 && endBrace !== -1 && endBrace > startBrace) {
            jsonStr = trimmedFull.substring(startBrace, endBrace + 1)
          }
        }

        const action = JSON.parse(jsonStr)
        currentAiMsg.thought = action.thought || currentAiMsg.thought
        currentAiMsg.message = action.message || ''
        
        if (action.tool === 'execute_command' && action.command) {
            currentAiMsg.tool = action.tool
            currentAiMsg.command = action.command
            
            isExecuting.value = true
            const cmdRes = await executeCommand(action.command)
            isExecuting.value = false
            
            currentAiMsg.status = cmdRes.exitCode === 0 ? 'success' : 'error'
            
            const feedback = `命令执行结果:\n状态码: ${cmdRes.exitCode}\n标准输出: ${cmdRes.stdout}\n标准错误: ${cmdRes.stderr}`
            appendTerminalLog(action.command, cmdRes.stdout || cmdRes.stderr, cmdRes.exitCode === 0 ? 'stdout' : 'stderr')
            
            // Save result
            await saveMessage(cmdRes.stdout || cmdRes.stderr, 3)
            
            // Loop back with feedback
            await processAgentLoop(feedback)
          } else {
            currentAiMsg.message = action.message || ''
          }
        } catch (e) {
          console.error("JSON Parse Error at end of stream", e)
          currentAiMsg.message = fullContent
        }
      } else if (fullContent) {
        currentAiMsg.message = fullContent
      }

    // Save final AI response to history
    await saveMessage(fullContent, 2)

  } catch (error) {
    console.error('Agent loop error:', error)
    uiStore.showToast(error.message || '通信发生错误')
    isTyping.value = false
  }
}

const executeCommand = async (cmd) => {
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/execute`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        command: cmd
      })
    })
    
    const data = await safeReadJson(res)
    if (!data || data.code !== 200) {
      throw new Error(data?.message || '命令执行失败')
    }
    return data.data
  } catch (error) {
    console.error('Command execution failed:', error)
    return {
      exitCode: -1,
      stdout: '',
      stderr: error.message || 'Unknown error'
    }
  }
}
</script>

<style scoped>
.terminal-container {
  display: flex;
  height: 100vh;
  background: #f8fafc;
  overflow: hidden;
}

/* Sidebar Styles */
.sessions-sidebar {
  width: 260px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.sessions-sidebar.collapsed {
  width: 0;
  overflow: hidden;
  border-right: none;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #1e293b;
}

.new-session-btn {
  background: #f1f5f9;
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  cursor: pointer;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
}

.new-session-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}

.session-info-wrapper {
  flex: 1;
  min-width: 0;
}

.delete-session-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 1;
  transition: all 0.2s;
  font-size: 1rem;
  line-height: 1;
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.session-item:hover {
  background: #f1f5f9;
}

.session-item.active {
  background: #eff6ff;
  border-left: 3px solid #3b82f6;
}

.session-title {
  font-size: 0.9rem;
  font-weight: 500;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 4px;
}

/* Main Content Styles */
.terminal-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.terminal-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  min-width: 400px;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-sidebar {
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  font-size: 1.1rem;
}

.chat-header h3 {
  margin: 0;
  font-size: 1rem;
  color: #1e293b;
}

.model-selector select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  font-size: 0.85rem;
  background: #f8fafc;
  color: #475569;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: #fcfcfc;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 3px;
}

.message {
  max-width: 85%;
  display: flex;
}

.message.user {
  align-self: flex-end;
}

.message.ai {
  align-self: flex-start;
}

.user-bubble {
  background: #3b82f6;
  color: white;
  padding: 12px 16px;
  border-radius: 18px 18px 2px 18px;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.15);
  font-size: 0.95rem;
  line-height: 1.5;
}

.ai-bubble {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 16px;
  border-radius: 18px 18px 18px 2px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  width: 100%;
}

.thought-block {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}

.thought-title {
  padding: 8px 12px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f1f5f9;
}

.thought-content {
  padding: 12px;
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
  border-top: 1px solid #f1f5f9;
}

.ai-text {
  font-size: 0.95rem;
  color: #1e293b;
  line-height: 1.6;
  white-space: pre-wrap;
}

.tool-call-card {
  margin-top: 12px;
  background: #1e293b;
  border-radius: 8px;
  padding: 12px;
  color: #e2e8f0;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 0.8rem;
  color: #94a3b8;
}

.tool-command {
  background: #0f172a;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.tool-command code {
  font-family: 'Consolas', monospace;
  color: #38bdf8;
  font-size: 0.85rem;
}

.tool-status {
  font-size: 0.8rem;
  display: flex;
  align-items: center;
}

.status-success { color: #4ade80; }
.status-error { color: #f87171; }
.spinner { color: #fbbf24; }

.system-bubble {
  background: #0f172a;
  padding: 12px;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #e2e8f0;
  border: 1px solid #334155;
  max-width: 100%;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.result-header {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 0.75rem;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #1e293b;
  padding-bottom: 4px;
}

.result-content {
  margin: 0;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.85rem;
  max-height: 300px;
  overflow-y: auto;
  color: #38bdf8;
  scrollbar-width: thin;
  scrollbar-color: #334155 transparent;
}

.result-content::-webkit-scrollbar {
  width: 4px;
}

.result-content::-webkit-scrollbar-thumb {
  background-color: #334155;
  border-radius: 2px;
}

.input-area {
  padding: 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  gap: 12px;
  background: #ffffff;
}

textarea {
  flex: 1;
  height: 50px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  resize: none;
  font-size: 0.95rem;
  transition: border-color 0.2s;
}

textarea:focus {
  outline: none;
  border-color: #3b82f6;
}

.send-btn {
  padding: 0 24px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #2563eb;
}

.send-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

/* Terminal Panel Styles */
.terminal-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  color: #e2e8f0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.terminal-header {
  padding: 0 12px;
  background: #1e293b;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 40px;
}

.terminal-tabs {
  display: flex;
  height: 100%;
}

.tab {
  padding: 0 16px;
  display: flex;
  align-items: center;
  font-size: 0.8rem;
  color: #94a3b8;
  border-right: 1px solid #0f172a;
}

.tab.active {
  background: #0f172a;
  color: #38bdf8;
  font-weight: 600;
}

.clear-btn {
  background: transparent;
  border: 1px solid #475569;
  color: #94a3b8;
  padding: 2px 8px;
  font-size: 0.75rem;
  border-radius: 4px;
  cursor: pointer;
}

.clear-btn:hover {
  background: #334155;
  color: #f1f5f9;
}

.terminal-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  scrollbar-width: thin;
  scrollbar-color: #334155 transparent;
}

.terminal-content::-webkit-scrollbar {
  width: 8px;
}

.terminal-content::-webkit-scrollbar-thumb {
  background-color: #334155;
  border-radius: 4px;
}

.log-line {
  margin-bottom: 12px;
}

.log-cmd-line {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.prompt {
  color: #4ade80;
  font-weight: bold;
}

.cwd {
  color: #38bdf8;
}

.cmd {
  color: #ffffff;
  font-weight: 600;
}

.output {
  margin: 0 0 0 20px;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 0.85rem;
  line-height: 1.4;
  color: #cbd5e1;
}

.output.stderr {
  color: #f87171;
}

/* Scrollbar Styles */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.terminal-panel ::-webkit-scrollbar-thumb {
  background: #334155;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 16px;
  background: #f1f5f9;
  border-radius: 12px;
  color: #64748b;
}

.typing-indicator span {
  animation: blink 1.4s infinite both;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0% { opacity: 0.2; }
  20% { opacity: 1; }
  100% { opacity: 0.2; }
}
</style>
