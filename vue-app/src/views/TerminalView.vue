<template>
  <div class="terminal-container">
    <!-- Sessions Sidebar -->
    <div
      class="sessions-sidebar"
      :class="{ collapsed: sidebarCollapsed }"
      :style="{ width: sidebarCollapsed ? '0px' : sidebarWidth + 'px' }"
    >
      <div class="sidebar-header" :style="{ minWidth: sidebarWidth + 'px' }">
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

    <!-- Sidebar Resizer -->
    <div 
      v-if="!sidebarCollapsed"
      class="resizer-v sidebar-resizer"
      @mousedown="initResizeSidebar"
    ></div>

    <div class="terminal-main">
      <div class="terminal-layout">
        <!-- Left Panel: Chat -->
        <div class="chat-panel">
          <div class="chat-header">
            <div class="header-left">
              <button
                class="toggle-sidebar"
                :class="{ rotated: sidebarCollapsed }"
                @click="sidebarCollapsed = !sidebarCollapsed"
                title="切换侧边栏"
              >
                <span class="btn-icon">📁</span>
              </button>
              <h3>AI 终端助手</h3>
            </div>
            <div class="header-right">
              <div class="model-selector">
                <CustomSelect 
                  v-model="currentModel" 
                  :options="modelOptions"
                />
              </div>
              <button 
                class="toggle-right-panel"
                :class="{ rotated: rightPanelCollapsed }"
                @click="rightPanelCollapsed = !rightPanelCollapsed"
                title="切换工具面板"
              >
                <span class="btn-icon">🛠️</span>
              </button>
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

          <!-- Floating Task List Panel -->
          <div v-if="currentTasks && currentTasks.length > 0" class="global-task-panel" :class="{ collapsed: taskListCollapsed }">
            <div class="task-panel-header" @click="taskListCollapsed = !taskListCollapsed">
              <div class="header-main">
                <span class="panel-icon">📋</span>
                <span class="panel-title">任务进度</span>
                <span class="task-count">({{ completedCount }}/{{ currentTasks.length }})</span>
              </div>
              <div class="header-right">
                <div class="progress-mini-bar">
                  <div class="progress-fill" :style="{ width: taskProgress + '%' }"></div>
                </div>
                <span class="progress-percent">{{ taskProgress }}%</span>
                <i class="toggle-icon">{{ taskListCollapsed ? '▲' : '▼' }}</i>
              </div>
            </div>
            <div v-if="!taskListCollapsed" class="task-panel-body">
              <div v-for="task in currentTasks" :key="task.id" class="task-item" :class="task.status">
                <span class="task-icon">
                  {{ task.status === 'completed' ? '✅' : (task.status === 'in_progress' ? '🔄' : '⭕') }}
                </span>
                <span class="task-desc" :title="task.desc">{{ task.desc }}</span>
              </div>
            </div>
          </div>

          <div class="input-area-wrapper">
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
        </div>

        <!-- Main Resizer -->
        <div 
          v-if="!rightPanelCollapsed"
          class="resizer-v main-resizer"
          @mousedown="initResizeMain"
        ></div>

        <!-- Right Panel -->
        <div 
          class="right-panel" 
          :class="{ collapsed: rightPanelCollapsed }"
          :style="{ width: rightPanelCollapsed ? '0px' : rightPanelWidth + 'px' }"
        >
          <div class="panel-tabs">
            <div 
              v-for="tab in tabs"
              :key="tab.id"
              class="tab" 
              :class="{ active: activeTab === tab.id }"
              draggable="true"
              @dragstart="handleTabDragStart($event, tab)"
              @dragover.prevent
              @drop="handleTabDrop($event, tab)"
              @click="activeTab = tab.id"
            >
              {{ tab.label }}
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
          <div v-if="activeTab === 'files'" class="panel-content file-panel-container">
            <template v-if="!editingFile">
              <TerminalFileExplorer ref="fileExplorer" @select="handleFileSelect" />
            </template>
            <template v-else>
              <div class="file-editor-view">
                <div class="editor-header">
                  <button class="back-btn" @click="closeEditor">
                    <span class="icon">←</span> 返回
                  </button>
                  <span class="file-name" :title="editingFile.path">{{ editingFile.name }}</span>
                  <button 
                    class="save-btn" 
                    @click="saveEditedFile" 
                    :disabled="isSaving"
                  >
                    {{ isSaving ? '保存中...' : '保存' }}
                  </button>
                </div>
                <div class="editor-body">
                  <textarea 
                    v-model="editedContent" 
                    class="file-editor"
                    spellcheck="false"
                    placeholder="文件内容为空"
                  ></textarea>
                </div>
              </div>
            </template>
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
import { ref, nextTick, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { API_CONFIG } from '@/config/api'
import TerminalFileExplorer from '@/components/TerminalFileExplorer.vue'
import RequirementManager from '@/components/RequirementManager.vue'
import CustomSelect from '@/components/CustomSelect.vue'

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

// File Editor State
const editingFile = ref(null)
const editedContent = ref(null)
const isSaving = ref(false)

const currentSessionId = ref(null)
const sessions = ref([])
const sidebarCollapsed = ref(false)
const rightPanelCollapsed = ref(false)
const activeTab = ref('terminal')

const currentTasks = ref([])
const taskListCollapsed = ref(true)

const completedCount = computed(() => {
  return currentTasks.value.filter(t => t.status === 'completed').length
})

const taskProgress = computed(() => {
  if (currentTasks.value.length === 0) return 0
  return Math.round((completedCount.value / currentTasks.value.length) * 100)
})

const sidebarWidth = ref(260)
const rightPanelWidth = ref(window.innerWidth * 0.4) // Default to 40%

const tabs = ref([
  { id: 'terminal', label: '终端输出' },
  { id: 'files', label: '文件管理' },
  { id: 'req', label: '需求文档' }
])

let draggedTab = null

/**
 * 处理标签拖拽开始
 * @param {DragEvent} e 拖拽事件
 * @param {Object} tab 被拖拽的标签对象
 */
const handleTabDragStart = (e, tab) => {
  draggedTab = tab
  e.dataTransfer.effectAllowed = 'move'
}

/**
 * 处理标签拖拽放下
 * @param {DragEvent} e 拖拽事件
 * @param {Object} targetTab 目标标签对象
 */
const handleTabDrop = (e, targetTab) => {
  if (!draggedTab || draggedTab.id === targetTab.id) return
  
  const fromIndex = tabs.value.findIndex(t => t.id === draggedTab.id)
  const toIndex = tabs.value.findIndex(t => t.id === targetTab.id)
  
  const newTabs = [...tabs.value]
  newTabs.splice(fromIndex, 1)
  newTabs.splice(toIndex, 0, draggedTab)
  tabs.value = newTabs
  draggedTab = null
}

/**
 * 初始化侧边栏宽度调节
 * @param {MouseEvent} e 鼠标事件
 */
const initResizeSidebar = (e) => {
  const startX = e.clientX
  const startWidth = sidebarWidth.value
  
  const onMouseMove = (moveEvent) => {
    const diff = moveEvent.clientX - startX
    const newWidth = Math.max(150, Math.min(500, startWidth + diff))
    sidebarWidth.value = newWidth
  }
  
  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
    document.body.style.cursor = 'default'
  }
  
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  document.body.style.cursor = 'col-resize'
}

/**
 * 初始化主面板与右面板宽度调节
 * @param {MouseEvent} e 鼠标事件
 */
const initResizeMain = (e) => {
  const startX = e.clientX
  const startWidth = rightPanelWidth.value
  
  const onMouseMove = (moveEvent) => {
    const diff = startX - moveEvent.clientX
    const newWidth = Math.max(300, Math.min(window.innerWidth * 0.7, startWidth + diff))
    rightPanelWidth.value = newWidth
  }
  
  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
    document.body.style.cursor = 'default'
  }
  
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  document.body.style.cursor = 'col-resize'
}

const modelOptions = [
  { label: 'DeepSeek Chat', value: 'deepseek-chat', description: '适用于通用对话和指令遵循' },
  { label: 'DeepSeek Reasoner', value: 'deepseek-reasoner', description: '深度思考模型，擅长复杂逻辑推理' }
]

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
  currentTasks.value = []
  taskListCollapsed.value = true
  
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
               
               // Restore tasks to global state if this is the latest task list
               if (action.type === 'task_list' || action.tasks) {
                 currentTasks.value = action.tasks || []
                 taskListCollapsed.value = false
               }
               if (action.type === 'task_update' && currentTasks.value.length > 0) {
                 const task = currentTasks.value.find(t => t.id === action.taskId)
                 if (task) task.status = action.status
               }

               return { 
                 role: 'ai', 
                 thought: action.thought, 
                 message: action.message, 
                 tool: action.tool, 
                 command: action.command,
                 filePath: action.path,
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

const handleFileSelect = async (file) => {
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/read-file?path=${encodeURIComponent(file.path)}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      editingFile.value = file
      editedContent.value = data.data
    }
  } catch (e) {
    console.error('Failed to read file:', e)
  }
}

const closeEditor = () => {
  editingFile.value = null
  editedContent.value = null
}

const saveEditedFile = async () => {
  if (!editingFile.value) return
  
  isSaving.value = true
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/write-file`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        path: editingFile.value.path,
        content: editedContent.value,
        overwrite: true
      })
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      alert('保存成功')
    } else {
      alert('保存失败: ' + (data?.message || '未知错误'))
    }
  } catch (e) {
    console.error('Failed to save file:', e)
    alert('保存失败，请检查网络或权限')
  } finally {
    isSaving.value = false
  }
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
              
              // 实时解析 JSON 中的字段
              if (fullContent.trim().startsWith('{')) {
                const extractedThought = tryExtractField(fullContent, 'thought')
                const extractedMessage = tryExtractField(fullContent, 'message')
                
                if (extractedThought) currentAiMsg.thought = extractedThought
                if (extractedMessage) currentAiMsg.message = extractedMessage
              } else {
                // 如果不是 JSON 格式，直接显示
                currentAiMsg.message = fullContent
              }
              scrollToBottom()
            }
            if (json.reasoning_content) {
              currentAiMsg.thought += json.reasoning_content
              scrollToBottom()
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
      currentAiMsg.message = action.message || currentAiMsg.message
      
      if (action.type === 'task_list') {
        currentTasks.value = action.tasks || []
        taskListCollapsed.value = false
        currentAiMsg.message = "已生成任务列表"
      } else if (action.type === 'task_update') {
        const task = currentTasks.value.find(t => t.id === action.taskId)
        if (task) {
          task.status = action.status
          currentAiMsg.message = `任务更新: ${task.desc} -> ${action.status}`
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

const tryExtractField = (content, fieldName) => {
  if (!content) return ''
  
  // 1. 尝试匹配完整的字段: "fieldName": "value"
  const fullRegex = new RegExp(`"${fieldName}"\\s*:\\s*"((?:[^\\\\"]|\\\\.)*)"`)
  const fullMatch = content.match(fullRegex)
  if (fullMatch) {
    return unescapeJsonString(fullMatch[1])
  }
  
  // 2. 如果没有完整匹配，尝试匹配正在流式输出的字段: "fieldName": "部分内容...
  // 匹配字段名及其开启的引号，然后捕获直到字符串末尾的所有内容
  const partialRegex = new RegExp(`"${fieldName}"\\s*:\\s*"((?:[^\\\\"]|\\\\.)*)$`)
  const partialMatch = content.match(partialRegex)
  if (partialMatch) {
    return unescapeJsonString(partialMatch[1])
  }
  
  return ''
}

/**
 * 处理 JSON 字符串中的转义字符
 * @param {string} str 原始字符串
 * @returns {string} 处理后的字符串
 */
const unescapeJsonString = (str) => {
  return str
    .replace(/\\n/g, '\n')
    .replace(/\\"/g, '"')
    .replace(/\\\\/g, '\\')
    .replace(/\\t/g, '\t')
    .replace(/\\r/g, '\r')
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
.sessions-sidebar { background: #fff; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; transition: opacity 0.3s ease, transform 0.3s ease; z-index: 10; overflow: hidden; }
.sessions-sidebar.collapsed { width: 0 !important; opacity: 0; pointer-events: none; border: none; }
.sidebar-header { padding: 15px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; box-sizing: border-box; }
.sessions-list { flex: 1; overflow-y: auto; padding: 10px; }

/* Resizer Styles */
.resizer-v {
  width: 4px;
  cursor: col-resize;
  background: transparent;
  transition: background 0.2s;
  z-index: 20;
  position: relative;
}
.resizer-v:hover, .resizer-v:active {
  background: #3b82f6;
}
.sidebar-resizer {
  margin-right: -4px;
}
.main-resizer {
  margin-left: -2px;
  margin-right: -2px;
}
.session-item { padding: 10px; border-radius: 6px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; }
.session-item:hover { background: #f1f5f9; }
.session-item.active { background: #eff6ff; border-left: 3px solid #3b82f6; }
.session-title { font-size: 0.9rem; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-time { font-size: 0.75rem; color: #94a3b8; }
.delete-session-btn { background: none; border: none; opacity: 0; color: #94a3b8; cursor: pointer; }
.session-item:hover .delete-session-btn { opacity: 1; }

.terminal-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.terminal-layout { display: flex; flex: 1; overflow: hidden; }

.chat-panel { flex: 1; display: flex; flex-direction: column; background: #fff; border-right: 1px solid #e2e8f0; min-width: 300px; position: relative; }
.chat-header { padding: 12px 20px; border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; background: #fff; z-index: 5; }
.header-left { display: flex; gap: 15px; align-items: center; }
.header-right { display: flex; gap: 15px; align-items: center; }

.toggle-sidebar, .toggle-right-panel { 
  background: #f1f5f9; 
  border: 1px solid #e2e8f0; 
  cursor: pointer; 
  font-size: 1rem; 
  color: #64748b; 
  width: 36px; 
  height: 36px; 
  border-radius: 8px; 
  display: flex; 
  align-items: center; 
  justify-content: center;
  transition: all 0.2s;
}
.toggle-sidebar:hover, .toggle-right-panel:hover { background: #e2e8f0; color: #3b82f6; }
.toggle-sidebar.rotated, .toggle-right-panel.rotated { background: #eff6ff; color: #3b82f6; border-color: #bfdbfe; }

.messages-container { 
  flex: 1; 
  overflow-y: auto; 
  padding: 20px; 
  display: flex; 
  flex-direction: column; 
  gap: 20px; 
  background: #f8fafc; 
}
.message { 
  width: 100%; 
  display: flex; 
  justify-content: center; 
}
.message-content { 
  max-width: 980px; 
  width: 100%; 
  display: flex; 
  flex-direction: column;
}
.message.user .message-content { align-items: flex-end; }
.message.ai .message-content { align-items: flex-start; }
.message.command_result .message-content { align-items: stretch; }

.user-bubble { 
  background: #3b82f6; 
  color: white; 
  padding: 10px 15px; 
  border-radius: 12px 12px 2px 12px; 
  max-width: 80%;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.1);
}
.ai-bubble { 
  background: #fff; 
  border: 1px solid #e2e8f0; 
  padding: 15px; 
  border-radius: 12px 12px 12px 2px; 
  width: 100%; 
  box-shadow: 0 1px 3px rgba(0,0,0,0.05); 
}

.system-bubble {
  background: #1e293b;
  border-radius: 8px;
  padding: 15px;
  width: 100%;
  border: 1px solid #334155;
}
.result-header {
  color: #94a3b8;
  font-size: 0.8rem;
  margin-bottom: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}
.result-header::before {
  content: '>';
  color: #4ade80;
  font-weight: bold;
}
.result-content {
  color: #e2e8f0;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  font-size: 0.9rem;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  background: #0f172a;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #1e293b;
}

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

.input-area-wrapper {
  padding: 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: center;
  background: #fff;
}
.input-area { 
  display: flex; 
  gap: 10px; 
  width: 100%;
  max-width: 980px;
}

/* Global Task Panel */
.global-task-panel {
  margin: 0 auto 10px auto;
  width: calc(100% - 40px);
  max-width: 980px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  z-index: 10;
}
.global-task-panel.collapsed {
  margin-bottom: 0;
}
.task-panel-header {
  padding: 10px 15px;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}
.task-panel-header:hover {
  background: #f1f5f9;
}
.header-main {
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-icon {
  font-size: 1.1rem;
}
.panel-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.9rem;
}
.task-count {
  color: #64748b;
  font-size: 0.8rem;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.progress-mini-bar {
  width: 100px;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #3b82f6;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.progress-percent {
  font-size: 0.8rem;
  font-weight: 500;
  color: #3b82f6;
  min-width: 35px;
}
.task-panel-body {
  padding: 12px 15px;
  border-top: 1px solid #f1f5f9;
  max-height: 250px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fff;
}
.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}
.task-item:hover {
  background: #f8fafc;
}
.task-item.completed {
  opacity: 0.6;
}
.task-item.completed .task-desc {
  text-decoration: line-through;
}
.task-icon {
  font-size: 1rem;
  flex-shrink: 0;
}
.task-desc {
  font-size: 0.9rem;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
.task-item.in_progress .task-desc {
  font-weight: 500;
  color: #2563eb;
}

textarea { flex: 1; height: 50px; padding: 10px; border: 1px solid #e2e8f0; border-radius: 8px; resize: none; }
.send-btn { background: #3b82f6; color: white; border: none; padding: 0 20px; border-radius: 8px; cursor: pointer; }
.send-btn:disabled { background: #94a3b8; cursor: not-allowed; }

.right-panel { display: flex; flex-direction: column; border-left: 1px solid #e2e8f0; background: #fff; transition: opacity 0.3s ease; overflow: hidden; }
.right-panel.collapsed { width: 0 !important; opacity: 0; pointer-events: none; border: none; }
.panel-tabs { display: flex; border-bottom: 1px solid #e2e8f0; background: #f1f5f9; overflow-x: auto; flex-shrink: 0; }
.tab { padding: 10px 20px; cursor: pointer; font-size: 0.9rem; color: #64748b; border-right: 1px solid #e2e8f0; white-space: nowrap; user-select: none; }
.tab:hover { background: #e2e8f0; }
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

.file-panel-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.file-editor-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1e1e1e;
}

.editor-header {
  padding: 8px 12px;
  background: #2d2d2d;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #3d3d3d;
}

.back-btn {
  background: transparent;
  border: 1px solid #4d4d4d;
  color: #cccccc;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 4px;
}

.back-btn:hover {
  background: #3d3d3d;
  color: #ffffff;
}

.file-name {
  flex: 1;
  color: #e0e0e0;
  font-size: 0.9rem;
  font-family: 'Consolas', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.save-btn {
  background: #2563eb;
  color: white;
  border: none;
  padding: 4px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
}

.save-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.editor-body {
  flex: 1;
  overflow: hidden;
}

.file-editor {
  width: 100%;
  height: 100%;
  background: #1e1e1e;
  color: #d4d4d4;
  border: none;
  padding: 16px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.95rem;
  line-height: 1.5;
  resize: none;
  outline: none;
}

/* File Viewer Modal Styles (Cleanup) */
</style>
