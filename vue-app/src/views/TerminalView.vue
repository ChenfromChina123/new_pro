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
                <option value="deepseek-chat">DeepSeek Chat</option>
                <option value="deepseek-reasoner">DeepSeek Reasoner</option>
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
                <div v-if="msg.role === 'user'" class="user-bubble">
                  {{ msg.content }}
                </div>

                <!-- AI Message -->
                <div v-else-if="msg.role === 'ai'" class="ai-bubble">
                  <div v-if="msg.thought" class="thought-block">
                    <div class="thought-title" @click="msg.showThought = !msg.showThought">
                      <span>思考过程</span>
                      <i class="toggle-icon">{{ msg.showThought ? '▼' : '▶' }}</i>
                    </div>
                    <div v-if="msg.showThought" class="thought-content">
                      {{ msg.thought }}
                    </div>
                  </div>

                  <!-- Task List Display -->
                  <div v-if="msg.tasks" class="task-list-card">
                    <div class="task-header">📋 任务清单</div>
                    <div v-for="task in msg.tasks" :key="task.id" class="task-item" :class="task.status">
                      <span class="task-icon">
                        {{ task.status === 'completed' ? '✅' : (task.status === 'in_progress' ? '🔄' : '⭕') }}
                      </span>
                      <span class="task-desc">{{ task.desc }}</span>
                    </div>
                  </div>

                  <div v-if="msg.message" class="ai-text">
                    {{ msg.message }}
                  </div>
                  
                  <!-- Command Execution Info -->
                  <div v-if="msg.tool" class="tool-call-card">
                    <div class="tool-header">
                      <span class="tool-icon">🐚</span>
                      <span v-if="msg.tool === 'execute_command'" class="tool-label">执行命令</span>
                      <span v-else-if="msg.tool === 'write_file'" class="tool-label">写入文件</span>
                      <span v-else class="tool-label">工具调用</span>
                    </div>
                    <div class="tool-command">
                      <code v-if="msg.tool === 'execute_command'">{{ msg.command }}</code>
                      <code v-else-if="msg.tool === 'write_file'">{{ msg.filePath }}</code>
                    </div>
                    <div class="tool-status" :class="msg.status">
                      <span v-if="msg.status === 'pending'" class="spinner">⌛ 执行中...</span>
                      <span v-else-if="msg.status === 'success'" class="status-success">✓ 执行成功</span>
                      <span v-else class="status-error">✗ 执行失败</span>
                    </div>
                  </div>
                </div>

                <!-- Command Result -->
                <div v-else-if="msg.role === 'command_result'" class="system-bubble">
                  <div class="result-header">命令执行结果:</div>
                  <pre class="result-content">{{ msg.content }}</pre>
                </div>
              </div>
            </div>
            <div v-if="isTyping" class="message ai">
              <div class="typing-indicator"><span>.</span><span>.</span><span>.</span></div>
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

        <!-- Right Panel -->
        <div class="right-panel">
          <div class="panel-tabs">
            <div 
              class="tab" 
              :class="{ active: activeTab === 'terminal' }"
              @click="activeTab = 'terminal'"
            >
              终端输出
            </div>
            <div 
              class="tab" 
              :class="{ active: activeTab === 'files' }"
              @click="activeTab = 'files'"
            >
              文件管理
            </div>
            <div 
              class="tab" 
              :class="{ active: activeTab === 'req' }"
              @click="activeTab = 'req'"
            >
              需求文档
            </div>
          </div>

          <!-- Terminal Output -->
          <div v-show="activeTab === 'terminal'" class="terminal-content-wrapper">
            <div class="terminal-actions">
               <button class="clear-btn" @click="clearTerminal">Clear</button>
            </div>
            <div ref="terminalRef" class="terminal-content">
              <div v-for="(log, index) in terminalLogs" :key="index" class="log-line">
                <div class="log-cmd-line">
                  <span class="prompt">➜</span>
                  <span class="cwd">{{ log.cwd || '~' }}</span>
                  <span class="cmd">{{ log.command }}</span>
                </div>
                <pre class="output" :class="log.type">{{ log.output }}</pre>
              </div>
            </div>
          </div>

          <!-- File Explorer -->
          <div v-if="activeTab === 'files'" class="panel-content">
             <TerminalFileExplorer ref="fileExplorer" />
          </div>

          <!-- Requirements -->
          <div v-if="activeTab === 'req'" class="panel-content">
             <RequirementManager />
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
import TerminalFileExplorer from '@/components/TerminalFileExplorer.vue'
import RequirementManager from '@/components/RequirementManager.vue'

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
const fileExplorer = ref(null)

const currentSessionId = ref(null)
const sessions = ref([])
const sidebarCollapsed = ref(false)
const activeTab = ref('terminal')

const currentCwd = ref('/')

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

const safeReadJson = async (res) => {
  try { return await res.json() } catch { return null }
}

onMounted(async () => {
  await fetchSessions()
  if (sessions.value.length > 0) {
    await selectSession(sessions.value[0].sessionId)
  } else {
    await createNewSession()
  }
})

const fetchSessions = async () => {
  if (!authStore.token) return
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/sessions`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      sessions.value = (data.data || []).map(normalizeSession)
    }
  } catch (e) {
    console.error(e)
  }
}

const selectSession = async (sessionId) => {
  currentSessionId.value = sessionId
  messages.value = []
  terminalLogs.value = []
  
  const session = sessions.value.find(s => s.sessionId === sessionId)
  if (session && session.currentCwd) {
    currentCwd.value = session.currentCwd
  } else {
    currentCwd.value = '/'
  }

  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/history/${sessionId}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      const records = data.data || []
      console.log('History records loaded:', records)
      messages.value = records.map(r => {
        // Compatibility for senderType/sender_type
        const senderType = r.senderType ?? r.sender_type
        const content = r.content

        if (senderType === 1) return { role: 'user', content: content }
        if (senderType === 2) {
          // Parse historical AI message
          try {
             // Simple fallback parsing for history
             if (content && content.trim().startsWith('{')) {
               const action = JSON.parse(content)
               return { 
                 role: 'ai', 
                 thought: action.thought, 
                 message: action.message, 
                 tool: action.tool, 
                 command: action.command,
                 filePath: action.path,
                 tasks: action.tasks, // Restore tasks if saved
                 status: 'success',
                 showThought: false
               }
             }
          } catch(e) {}
          return { role: 'ai', message: content, showThought: false }
        }
        if (senderType === 3) return { role: 'command_result', content: content }
        return null
      }).filter(Boolean)
      scrollToBottom()
    }
  } catch (e) {
    console.error(e)
  }
}

const createNewSession = async () => {
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/new-session`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      const newS = normalizeSession(data.data)
      sessions.value.unshift(newS)
      selectSession(newS.sessionId)
    }
  } catch (e) {
    console.error(e)
  }
}

const deleteSession = async (sessionId) => {
  if (!confirm('确定删除?')) return
  try {
    await fetch(`${API_CONFIG.baseURL}/api/terminal/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    if (currentSessionId.value === sessionId) {
      if (sessions.value.length > 0) selectSession(sessions.value[0].sessionId)
      else createNewSession()
    }
  } catch (e) { console.error(e) }
}

const formatDate = (d) => new Date(d).toLocaleString()

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    if (terminalRef.value) terminalRef.value.scrollTop = terminalRef.value.scrollHeight
  })
}

const clearTerminal = () => terminalLogs.value = []

const handleEnter = (e) => {
  if (!e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
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
  } catch (e) { console.error(e) }
}

const sendMessage = async () => {
  const text = inputMessage.value.trim()
  if (!text) return
  
  messages.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  isTyping.value = true
  scrollToBottom()
  await saveMessage(text, 1)
  await processAgentLoop(text)
}

const processAgentLoop = async (prompt) => {
  try {
    const response = await fetch(`${API_CONFIG.baseURL}/api/terminal/chat-stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`,
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({
        prompt: prompt,
        session_id: currentSessionId.value,
        model: currentModel.value
      })
    })

    if (!response.ok) throw new Error('Request failed')

    let currentAiMsg = { 
      role: 'ai', 
      thought: '', 
      message: '', 
      tasks: null,
      tool: null, 
      status: 'pending', 
      showThought: true 
    }
    messages.value.push(currentAiMsg)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      const lines = (buffer + chunk).split('\n')
      buffer = lines.pop()

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const dataStr = line.slice(5).trim()
          if (dataStr === '[DONE]') continue
          try {
            const json = JSON.parse(dataStr)
            if (json.content) {
              fullContent += json.content
              // Basic streaming display logic could go here
              // For now we wait for full JSON or update thought
            }
            if (json.reasoning_content) {
              currentAiMsg.thought += json.reasoning_content
            }
          } catch (e) {}
        }
      }
    }

    isTyping.value = false
    
    // Parse full content
    try {
      // Attempt to extract JSON
      const jsonMatch = fullContent.match(/\{[\s\S]*\}/)
      const jsonStr = jsonMatch ? jsonMatch[0] : fullContent
      const action = JSON.parse(jsonStr)

      currentAiMsg.thought = action.thought || currentAiMsg.thought
      
      if (action.type === 'task_list') {
        currentAiMsg.tasks = action.tasks
        currentAiMsg.message = "已生成任务列表"
      } else if (action.type === 'task_update') {
        // Find previous task list and update
        // We look backwards
        for (let i = messages.value.length - 1; i >= 0; i--) {
          if (messages.value[i].tasks) {
             const task = messages.value[i].tasks.find(t => t.id === action.taskId)
             if (task) {
               task.status = action.status
               currentAiMsg.message = `任务更新: ${task.desc} -> ${action.status}`
             }
             break
          }
        }
      } else if (action.tool === 'execute_command') {
         currentAiMsg.tool = 'execute_command'
         currentAiMsg.command = action.command
         
         isExecuting.value = true
         const res = await executeCommand(action.command)
         isExecuting.value = false
         
         currentAiMsg.status = res.exitCode === 0 ? 'success' : 'error'
         
         // Update CWD from response
         if (res.cwd) {
            currentCwd.value = res.cwd
            if (fileExplorer.value) fileExplorer.value.refresh()
         }

         const output = res.stdout || res.stderr
         terminalLogs.value.push({ 
           command: action.command, 
           output: output, 
           type: res.exitCode === 0 ? 'stdout' : 'stderr',
           cwd: res.cwd 
         })
         
         await saveMessage(output, 3)
         
         // Loop back with result
         await processAgentLoop(`命令执行结果(ExitCode: ${res.exitCode}):\n${output}`)
         return // Stop this loop, next loop handles next step
      } else if (action.tool === 'write_file') {
         currentAiMsg.tool = 'write_file'
         currentAiMsg.filePath = action.path
         
         isExecuting.value = true
         const content = extractContent(action.content)
         const res = await writeFile(action.path, content, action.overwrite)
         isExecuting.value = false
         
         currentAiMsg.status = res.exitCode === 0 ? 'success' : 'error'
         
         const output = res.stdout || res.stderr
         await saveMessage(output, 3)
         if (fileExplorer.value) fileExplorer.value.refresh()
         
         await processAgentLoop(`写文件结果:\n${output}`)
         return
      } else {
         currentAiMsg.message = action.message || fullContent
      }
      
      await saveMessage(JSON.stringify(action), 2)

    } catch (e) {
      currentAiMsg.message = fullContent
      await saveMessage(fullContent, 2)
    }

  } catch (e) {
    console.error(e)
    isTyping.value = false
  }
}

const extractContent = (raw) => {
  const start = raw.indexOf('<<<<AI_FILE_CONTENT_BEGIN>>>>')
  const end = raw.indexOf('<<<<AI_FILE_CONTENT_END>>>>')
  if (start !== -1 && end !== -1) {
    return raw.slice(start + 29, end).trim()
  }
  return raw
}

const executeCommand = async (cmd) => {
  const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authStore.token}`
    },
    body: JSON.stringify({ command: cmd, cwd: currentCwd.value, sessionId: currentSessionId.value })
  })
  const data = await safeReadJson(res)
  return data?.data || { exitCode: -1, stderr: 'Execution failed' }
}

const writeFile = async (path, content, overwrite) => {
  const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/write-file`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authStore.token}`
    },
    body: JSON.stringify({ path, content, cwd: currentCwd.value, overwrite })
  })
  const data = await safeReadJson(res)
  return data?.data || { exitCode: -1, stderr: 'Write failed' }
}
</script>

<style scoped>
.terminal-container { display: flex; height: 100vh; background: #f8fafc; overflow: hidden; }
.sessions-sidebar { width: 260px; background: #fff; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; transition: width 0.3s; }
.sessions-sidebar.collapsed { width: 0; overflow: hidden; border: none; }
.sidebar-header { padding: 15px; display: flex; justify-content: space-between; border-bottom: 1px solid #f1f5f9; }
.sessions-list { flex: 1; overflow-y: auto; padding: 10px; }
.session-item { padding: 10px; border-radius: 6px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; }
.session-item:hover { background: #f1f5f9; }
.session-item.active { background: #eff6ff; border-left: 3px solid #3b82f6; }
.session-title { font-size: 0.9rem; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-time { font-size: 0.75rem; color: #94a3b8; }
.delete-session-btn { background: none; border: none; opacity: 0; color: #94a3b8; cursor: pointer; }
.session-item:hover .delete-session-btn { opacity: 1; }

.terminal-main { flex: 1; display: flex; flex-direction: column; }
.terminal-layout { display: flex; flex: 1; overflow: hidden; }

.chat-panel { flex: 1; display: flex; flex-direction: column; background: #fff; border-right: 1px solid #e2e8f0; min-width: 400px; }
.chat-header { padding: 12px 20px; border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; gap: 10px; align-items: center; }
.toggle-sidebar { background: none; border: none; cursor: pointer; font-size: 1.2rem; color: #64748b; }

.messages-container { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 15px; background: #f8fafc; }
.message { max-width: 90%; display: flex; }
.message.user { align-self: flex-end; }
.message.ai { align-self: flex-start; width: 100%; }
.user-bubble { background: #3b82f6; color: white; padding: 10px 15px; border-radius: 12px 12px 2px 12px; }
.ai-bubble { background: #fff; border: 1px solid #e2e8f0; padding: 15px; border-radius: 12px 12px 12px 2px; width: 100%; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }

.thought-block { background: #f1f5f9; border-radius: 6px; margin-bottom: 10px; overflow: hidden; }
.thought-title { padding: 8px 12px; font-size: 0.8rem; color: #64748b; cursor: pointer; display: flex; justify-content: space-between; background: #e2e8f0; }
.thought-content { padding: 10px; font-size: 0.85rem; color: #475569; white-space: pre-wrap; }

.task-list-card { background: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px; padding: 10px; margin-bottom: 10px; }
.task-header { font-weight: bold; color: #c2410c; margin-bottom: 8px; font-size: 0.9rem; }
.task-item { display: flex; gap: 8px; padding: 4px 0; font-size: 0.9rem; color: #431407; }
.task-item.completed { text-decoration: line-through; color: #9ca3af; }

.tool-call-card { background: #1e293b; color: #e2e8f0; padding: 10px; border-radius: 6px; margin-top: 10px; }
.tool-header { display: flex; gap: 8px; font-size: 0.8rem; color: #94a3b8; margin-bottom: 5px; }
.tool-command code { color: #38bdf8; font-family: monospace; }
.tool-status { font-size: 0.8rem; margin-top: 5px; }
.status-success { color: #4ade80; }
.status-error { color: #f87171; }

.input-area { padding: 20px; border-top: 1px solid #e2e8f0; display: flex; gap: 10px; }
textarea { flex: 1; height: 50px; padding: 10px; border: 1px solid #e2e8f0; border-radius: 8px; resize: none; }
.send-btn { background: #3b82f6; color: white; border: none; padding: 0 20px; border-radius: 8px; cursor: pointer; }
.send-btn:disabled { background: #94a3b8; cursor: not-allowed; }

.right-panel { width: 50%; display: flex; flex-direction: column; border-left: 1px solid #e2e8f0; background: #fff; }
.panel-tabs { display: flex; border-bottom: 1px solid #e2e8f0; background: #f1f5f9; }
.tab { padding: 10px 20px; cursor: pointer; font-size: 0.9rem; color: #64748b; border-right: 1px solid #e2e8f0; }
.tab.active { background: #fff; color: #3b82f6; font-weight: 500; border-bottom: 2px solid #3b82f6; }

.terminal-content-wrapper { flex: 1; display: flex; flex-direction: column; background: #0f172a; color: #e2e8f0; }
.terminal-actions { display: flex; justify-content: flex-end; padding: 5px; background: #1e293b; }
.clear-btn { background: none; border: 1px solid #475569; color: #94a3b8; padding: 2px 8px; border-radius: 4px; cursor: pointer; font-size: 0.8rem; }
.terminal-content { flex: 1; overflow-y: auto; padding: 15px; font-family: monospace; }
.log-line { margin-bottom: 5px; font-size: 0.9rem; }
.log-cmd-line { display: flex; gap: 8px; color: #fff; }
.prompt { color: #4ade80; }
.cwd { color: #38bdf8; }
.output { color: #cbd5e1; white-space: pre-wrap; margin-left: 20px; }
.output.stderr { color: #f87171; }

.panel-content { flex: 1; overflow: hidden; }
</style>
