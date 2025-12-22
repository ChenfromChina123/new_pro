<template>
  <div class="terminal-container">
    <div class="terminal-main">
      <div class="terminal-layout">
        <!-- Left Panel: Chat -->
        <div class="chat-panel">
          <TaskSidebar 
            v-if="currentTasks && currentTasks.length > 0"
            :tasks="currentTasks"
            :active-task-id="activeTaskId"
            :initial-width="sidebarWidth"
            @update:width="sidebarWidth = $event"
            @select-task="scrollToTask"
          />

          <div class="chat-main-column">
            <div class="chat-header">
              <div class="header-left">
                <h3>AI 终端助手</h3>
              </div>
              <div class="header-right">
                <button 
                  class="toggle-right-panel"
                  :class="{ rotated: rightPanelCollapsed }"
                  title="切换工具面板"
                  @click="rightPanelCollapsed = !rightPanelCollapsed"
                >
                  <span class="btn-icon">🛠️</span>
                </button>
              </div>
            </div>
          
            <div
              class="messages-container"
            >
              <DynamicScroller
                ref="scrollerRef"
                :items="flatViewItems"
                :min-item-size="60"
                class="scroller"
                key-field="id"
              >
                <template #default="{ item, index, active }">
                  <DynamicScrollerItem
                    :item="item"
                    :active="active"
                    :size-dependencies="[
                      item.type === 'message' ? item.data.showThought : null,
                      item.type === 'message' ? item.data.message : null,
                      item.type === 'message' ? item.data.content : null
                    ]"
                    :data-index="index"
                  >
                    <!-- Header Item -->
                    <div v-if="item.type === 'header'" class="group-separator">
                      <span class="separator-line"></span>
                      <span 
                        class="separator-text" 
                        @click="toggleTaskExpand(item.taskId)"
                        style="cursor: pointer; display: flex; align-items: center; gap: 8px;"
                      >
                        <span class="toggle-icon" style="font-size: 0.8em;">{{ item.expanded ? '▼' : '▶' }}</span>
                        <span>Task {{ item.taskId }}: {{ item.desc }}</span>
                      </span>
                      <span class="separator-line"></span>
                    </div>

                    <!-- Message Item -->
                    <div
                      v-else-if="item.type === 'message'"
                      class="message"
                      :class="item.data.role"
                    >
                      <div class="message-content">
                        <!-- User Message -->
                        <div
                          v-if="item.data.role === 'user'"
                          class="user-bubble"
                        >
                          {{ item.data.content }}
                        </div>

                        <!-- AI Message -->
                        <div
                          v-else-if="item.data.role === 'ai'"
                          class="ai-bubble"
                        >
                          <div class="message-content">
                            <div
                              v-if="item.data.thought"
                              class="thought-block"
                            >
                              <div
                                class="thought-title"
                                @click="item.data.showThought = !item.data.showThought"
                              >
                                <span>思考过程</span>
                                <i class="toggle-icon">{{ item.data.showThought ? '▼' : '▶' }}</i>
                              </div>
                              <div
                                v-if="item.data.showThought"
                                class="thought-content"
                              >
                                {{ item.data.thought }}
                              </div>
                            </div>

                            <!-- Execution Steps -->
                            <div v-if="item.data.steps && item.data.steps.length > 0" class="steps-block">
                              <div class="steps-title">执行步骤</div>
                              <ul class="steps-list">
                                <li v-for="(step, sIdx) in item.data.steps" :key="sIdx">{{ step }}</li>
                              </ul>
                            </div>

                            <div
                              v-if="item.data.message"
                              class="ai-text"
                              v-html="formatMarkdown(item.data.message)"
                            >
                            </div>
                            
                            <!-- Command Execution Info -->
                            <div
                              v-if="item.data.tool"
                              class="tool-call-card"
                            >
                              <div class="tool-header">
                                <span class="tool-icon">🐚</span>
                                <span
                                  v-if="item.data.tool === 'execute_command'"
                                  class="tool-label"
                                >执行命令</span>
                                <span
                                  v-else-if="item.data.tool === 'write_file'"
                                  class="tool-label"
                                >写入文件</span>
                                <span
                                  v-else
                                  class="tool-label"
                                >工具调用</span>
                              </div>
                              <div class="tool-command">
                                <code v-if="item.data.tool === 'execute_command'">{{ item.data.command }}</code>
                                <code v-else-if="item.data.tool === 'write_file'">{{ item.data.filePath }}</code>
                              </div>
                              <div
                                class="tool-status"
                                :class="item.data.status"
                              >
                                <span
                                  v-if="item.data.status === 'pending'"
                                  class="spinner"
                                >⌛ 执行中...</span>
                                <span
                                  v-else-if="item.data.status === 'success'"
                                  class="status-success"
                                >✓ 执行成功</span>
                                <span
                                  v-else
                                  class="status-error"
                                >✗ 执行失败</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </DynamicScrollerItem>
                </template>
              </DynamicScroller>

              <div
                v-if="isTyping"
                class="message ai typing-message"
              >
                <div class="message-content">
                  <div class="typing-indicator">
                    <span>.</span><span>.</span><span>.</span>
                  </div>
                </div>
              </div>
            </div>

            <TerminalChatInput
              v-model:message="inputMessage"
              v-model:model="currentModel"
              :model-options="modelOptions"
              :disabled="isTyping || isExecuting"
              :can-send="!!inputMessage.trim() && !isTyping && !isExecuting"
              @enter="handleEnter"
              @send="sendMessage"
            />
          </div>
        </div>

        <!-- Main Resizer -->
        <div 
          v-if="!rightPanelCollapsed"
          class="resizer-v main-resizer"
          @mousedown="initResizeMain"
        />

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
          <div
            v-show="activeTab === 'terminal'"
            class="terminal-content-wrapper"
          >
            <div class="terminal-actions">
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
                  <span class="cwd">{{ log.cwd || '~' }}</span>
                  <span class="cmd">{{ log.command }}</span>
                </div>
                <pre
                  class="output"
                  :class="log.type"
                >{{ log.output }}</pre>
              </div>
            </div>
          </div>

          <!-- File Explorer -->
          <div
            v-if="activeTab === 'files'"
            class="panel-content file-panel-container"
          >
            <template v-if="!editingFile">
              <TerminalFileExplorer
                ref="fileExplorer"
                @select="handleFileSelect"
              />
            </template>
            <template v-else-if="isNotebook">
              <TerminalNotebook 
                :file="editingFile" 
                :initial-content="editedContent" 
                @close="closeEditor"
                @save="closeEditor"
              />
            </template>
            <template v-else>
              <TerminalFileEditor 
                :file="editingFile" 
                :initial-content="editedContent" 
                @close="closeEditor"
                @save="saveEditedFile"
              />
            </template>
          </div>

          <!-- Requirements -->
          <!-- 需求文档已集成到文件管理中 -->
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { useTerminalStore } from '@/stores/terminal'
import { storeToRefs } from 'pinia'
import { API_CONFIG } from '@/config/api'
import TerminalFileExplorer from '@/components/TerminalFileExplorer.vue'
import TerminalNotebook from '@/components/TerminalNotebook.vue'
import TerminalFileEditor from '@/components/TerminalFileEditor.vue'
import TerminalChatInput from '@/components/terminal/TerminalChatInput.vue'
import TaskSidebar from '@/components/terminal/TaskSidebar.vue'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

// Configure marked
const renderer = new marked.Renderer()
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  breaks: true,
  gfm: true,
  renderer: renderer
})

const formatMarkdown = (text) => {
  if (!text) return ''
  return marked(text)
}

const authStore = useAuthStore()
const uiStore = useUIStore()
const terminalStore = useTerminalStore()

// Use storeToRefs for reactive state
const { 
  sessions, 
  currentSessionId, 
  messages, 
  terminalLogs, 
  currentTasks, 
  currentCwd,
  activeTaskId,
  groupedMessages
} = storeToRefs(terminalStore)

const sidebarWidth = ref(250)
const scrollerRef = ref(null)
const searchText = ref('')
const expandedTaskIds = ref(new Set())

// Flatten messages for virtual scrolling
const flatViewItems = computed(() => {
  const items = []
  let idCounter = 0
  
  groupedMessages.value.forEach(group => {
    const isExpanded = group.taskId ? expandedTaskIds.value.has(group.taskId) : true // General messages always shown or logic needed
    
    // Add Group Header
    if (group.taskId) {
      items.push({
        id: `group-header-${group.taskId}`,
        type: 'header',
        taskId: group.taskId,
        desc: currentTasks.value.find(t => t.id === group.taskId)?.desc || 'Loading...',
        expanded: isExpanded
      })
    }

    // Add Messages if expanded or no task ID (general messages)
    if (isExpanded || !group.taskId) {
      group.messages.forEach(msg => {
        // Filter logic
        if (searchText.value) {
          const query = searchText.value.toLowerCase()
          const aiMsgMatch = msg.message && String(msg.message).toLowerCase().includes(query)
          const thoughtMatch = msg.thought && String(msg.thought).toLowerCase().includes(query)
          const userContentMatch = msg.role === 'user' && msg.content && String(msg.content).toLowerCase().includes(query)
          if (!aiMsgMatch && !thoughtMatch && !userContentMatch) return
        }

        items.push({
          id: `msg-${idCounter++}`,
          type: 'message',
          data: msg
        })
      })
    }
  })
  return items
})

const toggleTaskExpand = (taskId) => {
  if (expandedTaskIds.value.has(taskId)) {
    expandedTaskIds.value.delete(taskId)
  } else {
    expandedTaskIds.value.add(taskId)
  }
}

watch(activeTaskId, (newId) => {
  if (newId) {
    expandedTaskIds.value.add(newId)
  }
})

const scrollToTask = (taskId) => {
  if (!scrollerRef.value) return
  
  // Expand the task first
  if (taskId) {
    expandedTaskIds.value.add(taskId)
  }
  
  // Wait for re-computation
  nextTick(() => {
    const index = flatViewItems.value.findIndex(item => item.taskId === taskId && item.type === 'header')
    if (index !== -1) {
      scrollerRef.value.scrollToItem(index)
    }
  })
}

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
const isNotebook = ref(false)

// --- UI State Persistence (Mapped to UI Store) ---
const rightPanelCollapsed = ref(uiStore.rightPanelCollapsed)
const taskListCollapsed = ref(uiStore.taskListCollapsed)
const rightPanelWidth = ref(uiStore.rightPanelWidth)
const activeTab = ref(uiStore.activeTab === 'req' ? 'terminal' : uiStore.activeTab)

// Watchers for Persistence
watch(rightPanelCollapsed, (val) => uiStore.saveState('rightPanelCollapsed', val))
watch(taskListCollapsed, (val) => uiStore.saveState('taskListCollapsed', val))
watch(rightPanelWidth, (val) => uiStore.saveState('rightPanelWidth', val))
watch(activeTab, (val) => uiStore.saveState('activeTab', val))

const completedCount = computed(() => {
  return currentTasks.value.filter(t => t.status === 'completed').length
})

const taskProgress = computed(() => {
  if (currentTasks.value.length === 0) return 0
  return Math.round((completedCount.value / currentTasks.value.length) * 100)
})

// Tab Logic with Persistence
const tabMeta = {
  'terminal': { id: 'terminal', label: '终端输出' },
  'files': { id: 'files', label: '文件管理' }
}

const tabs = ref(uiStore.tabOrder.filter(id => tabMeta[id]).map(id => tabMeta[id]))

watch(tabs, (newTabs) => {
  const order = newTabs.map(t => t.id)
  uiStore.saveState('tabOrder', order)
}, { deep: true })

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

const safeReadJson = async (res) => {
  try { return await res.json() } catch { return null }
}

onMounted(async () => {
  await terminalStore.fetchSessions()
  if (sessions.value.length > 0) {
    await terminalStore.selectSession(sessions.value[0].sessionId)
  } else {
    await terminalStore.createNewSession()
  }
  // 页面加载完成后滚动到底部
  scrollToBottom()
})

watch(currentSessionId, async () => {
  await nextTick()
  scrollToBottom()
})

// 监听终端日志变化，自动滚动到底部
watch(terminalLogs, () => {
  nextTick()
  scrollToBottom()
}, { deep: true })

const handleFileSelect = async (file) => {
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/read-file?path=${encodeURIComponent(file.path)}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      editingFile.value = file
      editedContent.value = data.data
      isNotebook.value = file.name.endsWith('.nb') || file.name.endsWith('.ipynb')
    }
  } catch (e) {
    console.error('Failed to read file:', e)
  }
}

const closeEditor = () => {
  editingFile.value = null
  editedContent.value = null
  isNotebook.value = false
}

const saveEditedFile = async (newContent) => {
  if (!editingFile.value) return
  
  // Use passed content if available, otherwise use local ref
  const contentToSave = typeof newContent === 'string' ? newContent : editedContent.value
  
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
        content: contentToSave,
        overwrite: true
      })
    })
    const data = await safeReadJson(res)
    if (data?.code === 200) {
      uiStore.showToast('保存成功')
      if (fileExplorer.value) {
        fileExplorer.value.refresh()
      }
      // Update local state
      editedContent.value = contentToSave
    } else {
      uiStore.showToast('保存失败: ' + (data?.message || '未知错误'))
    }
  } catch (e) {
    console.error('Failed to save file:', e)
    uiStore.showToast('保存失败，请检查网络或权限')
  } finally {
    isSaving.value = false
  }
}

const formatDate = (d) => new Date(d).toLocaleString()

let scrollScheduled = false
const scrollToBottom = (force = false) => {
  if (scrollScheduled) return
  
  // 如果不是强制滚动，检查是否在底部附近
  if (!force && scrollerRef.value) {
    const el = scrollerRef.value.$el
    const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100
    if (!isNearBottom) return
  }

  scrollScheduled = true
  requestAnimationFrame(() => {
    scrollScheduled = false
    nextTick(() => {
      if (scrollerRef.value && flatViewItems.value.length > 0) {
        scrollerRef.value.scrollToItem(flatViewItems.value.length - 1)
      }
      if (terminalRef.value) terminalRef.value.scrollTop = terminalRef.value.scrollHeight
    })
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

  // 2. 执行 Agent 循环
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
        model: currentModel.value,
        tasks: tasks.value // 传入当前任务链
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
    let animationFrameId = null
    
    const processBuffer = () => {
      const lines = buffer.split('\n')
      buffer = lines.pop() // Keep incomplete line
      let needsScroll = false

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const dataStr = line.slice(5).trim()
          if (dataStr === '[DONE]') continue
          try {
            const json = JSON.parse(dataStr)
            
            if (json.content) {
              fullContent += json.content
              needsScroll = true
              
              // 实时解析 JSON 中的字段
              if (fullContent.includes('{')) {
                const extractedThought = tryExtractField(fullContent, 'thought')
                const extractedMessage = tryExtractField(fullContent, 'content') || tryExtractField(fullContent, 'message')
                
                if (extractedThought) currentAiMsg.thought = extractedThought
                if (extractedMessage) currentAiMsg.message = extractedMessage
                
                if (!extractedMessage && !fullContent.trim().startsWith('{')) {
                   const jsonStart = fullContent.indexOf('{')
                   if (jsonStart > 0) {
                     currentAiMsg.message = fullContent.substring(0, jsonStart).trim()
                   } else {
                     currentAiMsg.message = fullContent
                   }
                }
              } else {
                currentAiMsg.message = fullContent
              }
            }
            if (json.reasoning_content) {
              currentAiMsg.thought += json.reasoning_content
              needsScroll = true
            }
          } catch (e) {}
        }
      }
      if (needsScroll) {
        scrollToBottom()
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk
      
      // 使用 requestAnimationFrame 优化渲染频率
      if (!animationFrameId) {
        animationFrameId = requestAnimationFrame(() => {
          processBuffer()
          animationFrameId = null
        })
      }
    }
    
    // Process remaining buffer
    if (buffer) {
       processBuffer()
    }

    isTyping.value = false
    
    // Parse full content
    try {
      // Attempt to extract JSON
      const jsonMatch = fullContent.match(/\{[\s\S]*\}/)
      const jsonStr = jsonMatch ? jsonMatch[0] : fullContent
      const action = JSON.parse(jsonStr)

      currentAiMsg.thought = action.thought || currentAiMsg.thought
      currentAiMsg.message = action.content || action.message || currentAiMsg.message
      currentAiMsg.steps = action.steps || []
      
      if (action.type === 'task_list') {
        currentTasks.value = action.tasks || []
        taskListCollapsed.value = false
        currentAiMsg.message = "已生成任务列表"
      } else if (action.type === 'task_update') {
        const taskIndex = currentTasks.value.findIndex(t => String(t.id) === String(action.taskId))
        if (taskIndex !== -1) {
          const updatedTasks = [...currentTasks.value]
          updatedTasks[taskIndex] = { ...updatedTasks[taskIndex], status: action.status }
          currentTasks.value = updatedTasks
          currentAiMsg.message = `任务更新: ${updatedTasks[taskIndex].desc} -> ${action.status}`
        }
      } else if (action.tool === 'execute_command') {
         currentAiMsg.tool = 'execute_command'
         currentAiMsg.command = action.command
         
         isExecuting.value = true
         const res = await executeCommand(action.command)
         isExecuting.value = false
         
         // Check both camelCase and snake_case for exit code
         const exitCode = res.exitCode !== undefined ? res.exitCode : res.exit_code
         currentAiMsg.status = exitCode === 0 ? 'success' : 'error'
         
         // Update CWD from response
         if (res.cwd) {
            currentCwd.value = res.cwd
            if (fileExplorer.value) fileExplorer.value.refresh()
         }

         const output = res.stdout || res.stderr
         terminalLogs.value.push({ 
           command: action.command, 
           output: output, 
           type: exitCode === 0 ? 'stdout' : 'stderr',
           cwd: res.cwd 
         })
         
         // Save command result to history (terminal log only, not chat messages)
         // messages.value.push({ role: 'command_result', content: output })
         await saveMessage(output, 3)
         scrollToBottom()
         
         // Loop back with result
         await processAgentLoop(`命令执行结果(ExitCode: ${exitCode}):\n${output}`)
         return // Stop this loop, next loop handles next step
      } else if (action.tool === 'write_file') {
         currentAiMsg.tool = 'write_file'
         currentAiMsg.filePath = action.path
         
         isExecuting.value = true
         const content = extractContent(action.content)
         const res = await writeFile(action.path, content, action.overwrite)
         isExecuting.value = false
         
         const exitCode = res.exitCode !== undefined ? res.exitCode : res.exit_code
         currentAiMsg.status = exitCode === 0 ? 'success' : 'error'
         
         const output = res.stdout || res.stderr
         const resultText = output || `文件 ${action.path} 写入成功`
         terminalLogs.value.push({ 
           command: `write_file: ${action.path}`, 
           output: resultText, 
           type: exitCode === 0 ? 'stdout' : 'stderr',
           cwd: res.cwd 
         })
         
         // Save write result to history (terminal log only, not chat messages)
         // messages.value.push({ role: 'command_result', content: resultText })
         await saveMessage(resultText, 3)
         scrollToBottom()

         if (fileExplorer.value) fileExplorer.value.refresh()
         
         await processAgentLoop(`文件写入结果(ExitCode: ${exitCode}):\n${resultText}`)
         return
      } else {
         currentAiMsg.message = action.content || action.message || fullContent
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

  const key = `"${fieldName}"`
  const keyIndex = content.indexOf(key)
  if (keyIndex === -1) return ''

  const colonIndex = content.indexOf(':', keyIndex + key.length)
  if (colonIndex === -1) return ''

  let i = colonIndex + 1
  while (i < content.length && /\s/.test(content[i])) i++
  if (content[i] !== '"') return ''
  i++

  let raw = ''
  let escaped = false
  for (; i < content.length; i++) {
    const ch = content[i]
    if (escaped) {
      raw += ch
      escaped = false
      continue
    }
    if (ch === '\\') {
      raw += '\\'
      escaped = true
      continue
    }
    if (ch === '"') return unescapeJsonString(raw)
    raw += ch
  }

  return unescapeJsonString(raw)
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
    body: JSON.stringify({ command: cmd, cwd: currentCwd.value, session_id: currentSessionId.value })
  })
  const data = await safeReadJson(res)
  return data?.data || { exit_code: -1, stderr: 'Execution failed' }
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
  return data?.data || { exit_code: -1, stderr: 'Write failed' }
}
</script>

<style scoped>
.terminal-container { 
  display: flex; 
  height: 100vh; 
  background: #f8fafc; 
  overflow: hidden; 
  color: #334155;
}

/* Sidebar Styles */
.sessions-sidebar { 
  display: none; 
}

/* Chat Layout */
.terminal-main { flex: 1; display: flex; flex-direction: column; min-width: 0; background: #fff; height: 100%; }
.terminal-layout { flex: 1; display: flex; overflow: hidden; height: 100%; }
.chat-panel { 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  background: #ffffff; 
  position: relative; 
  min-width: 0;
  height: 100%;
}
.chat-header { 
  padding: 12px 20px; 
  border-bottom: 1px solid #f1f5f9; 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  background: #fff; 
  z-index: 50; 
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-right { display: flex; align-items: center; gap: 12px; }

.toggle-sidebar, .toggle-right-panel {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #64748b;
}
.toggle-sidebar:hover, .toggle-right-panel:hover {
  background: #e2e8f0;
  color: #3b82f6;
}
.toggle-sidebar.rotated, .toggle-right-panel.rotated {
  transform: rotate(180deg);
}

.messages-container { 
  flex: 1; 
  overflow: hidden; /* Delegated to scroller */
  padding: 0; /* Padding moved to items or scroller content */
  display: flex; 
  flex-direction: column; 
  background: #ffffff; 
}

.scroller {
  height: 100%;
  overflow-y: auto;
  padding: 30px 20px; /* Restore padding here if needed, but better on items if scrolling content needs padding */
}

/* Ensure padding doesn't mess up scroll width */
.scroller {
  box-sizing: border-box;
}

.message { 
  width: 100%; 
  display: flex; 
  flex-direction: column;
  margin-bottom: 32px; /* Replace gap */
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  padding: 6px 12px;
  padding-right: 25px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.85rem;
  outline: none;
  width: 200px;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.clear-search {
  position: absolute;
  right: 8px;
  cursor: pointer;
  color: #94a3b8;
  font-size: 1.1rem;
}

.clear-search:hover {
  color: #64748b;
}
.message-content { 
  max-width: 850px; 
  width: 100%; 
  margin: 0 auto;
  display: flex; 
  flex-direction: column;
}

/* Bubble Styles */
.user-bubble { 
  align-self: flex-end;
  background: #f1f5f9; 
  color: #1e293b; 
  padding: 12px 18px; 
  border-radius: 18px 18px 2px 18px; 
  max-width: 80%;
  font-size: 0.95rem;
  line-height: 1.5;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.ai-bubble { 
  align-self: flex-start;
  width: 100%; 
}

/* Markdown and Text Styles */
.ai-text {
  font-size: 1rem;
  line-height: 1.6;
  color: #1e293b;
}
.ai-text :deep(p) { margin-bottom: 16px; }
.ai-text :deep(p:last-child) { margin-bottom: 0; }
.ai-text :deep(code) {
  background: #f1f5f9;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 0.9em;
}
.ai-text :deep(pre) {
  background: #1e293b;
  padding: 16px;
  border-radius: 12px;
  overflow-x: auto;
  margin: 16px 0;
}
.ai-text :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #e2e8f0;
}

/* Thought Block */
.thought-block { 
  background: #f8fafc; 
  border: 1px solid #e2e8f0;
  border-radius: 12px; 
  margin-bottom: 20px; 
  overflow: hidden; 
  transition: all 0.3s ease;
}
.thought-title { 
  padding: 10px 16px; 
  font-size: 0.85rem; 
  color: #64748b; 
  cursor: pointer; 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
  background: #f1f5f9; 
  font-weight: 500;
}
.thought-title:hover {
  background: #e2e8f0;
}
.thought-content { 
  padding: 16px; 
  font-size: 0.9rem; 
  color: #475569; 
  white-space: pre-wrap; 
  line-height: 1.6;
  border-top: 1px solid #e2e8f0;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Tool and Command Result Styles */
.tool-call-card { 
  background: #1e293b; 
  color: #e2e8f0; 
  padding: 16px; 
  border-radius: 12px; 
  margin: 16px 0; 
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.tool-header { 
  display: flex; 
  align-items: center;
  gap: 10px; 
  font-size: 0.85rem; 
  color: #94a3b8; 
  margin-bottom: 12px; 
}
.tool-command {
  background: #0f172a;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #334155;
}
.tool-command code { 
  color: #38bdf8; 
  font-family: 'Fira Code', monospace; 
  font-size: 0.9rem;
}
.tool-status { 
  font-size: 0.85rem; 
  margin-top: 12px; 
  display: flex;
  align-items: center;
  gap: 8px;
}

.system-bubble {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}
.result-header {
  background: #f8fafc;
  padding: 8px 16px;
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
}
.result-content {
  background: #0f172a;
  color: #4ade80;
  padding: 16px;
  margin: 0;
  font-family: 'Fira Code', monospace;
  font-size: 0.9rem;
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;
}

/* Global Task Panel */
.chat-panel {
  display: flex;
  flex-direction: row;
}

.chat-main-column {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100%;
}

.group-separator {
  display: flex;
  align-items: center;
  margin: 20px 0;
  padding: 0 20px;
  color: #64748b;
  font-size: 0.85rem;
}

.separator-line {
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.separator-text {
  padding: 0 10px;
  font-weight: 500;
}

.steps-block {
  margin: 10px 0;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.steps-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 5px;
}

.steps-list {
  margin: 0;
  padding-left: 20px;
  font-size: 0.9rem;
  color: #475569;
}

.steps-list li {
  margin-bottom: 4px;
}

.global-task-panel {
  position: relative;
  margin: 0 auto 8px;
  width: 100%;
  max-width: 850px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  z-index: 10;
  overflow: hidden;
  transition: all 0.2s ease;
}
.task-panel-header {
  padding: 12px 20px;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

/* Task Panel Styles */
.task-panel-body {
  padding: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.2s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.task-item:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.task-item.completed {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-color: #bbf7d0;
  border-left: 4px solid #22c55e;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.1);
}

.task-item.in_progress {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-color: #fde68a;
  border-left: 4px solid #f59e0b;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.1);
}

.task-item.pending {
  background: #ffffff;
  border-color: #e2e8f0;
  border-left: 4px solid #94a3b8;
}

.task-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-desc {
  flex: 1;
  font-size: 0.95rem;
  color: #334155;
  line-height: 1.5;
}

.task-item.completed .task-desc {
  color: #166534;
  font-weight: 500;
}

.task-item.in_progress .task-desc {
  color: #92400e;
  font-weight: 600;
}

.task-item.pending .task-desc {
  color: #64748b;
}

.task-item.in_progress .task-desc {
  color: #92400e;
  font-weight: 500;
}

/* Resizer and Utilities */
.resizer-v { width: 4px; cursor: col-resize; transition: background 0.2s; z-index: 20; }
.resizer-v:hover { background: #3b82f6; }

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 18px;
  background: #f8fafc;
  border-radius: 18px;
  width: fit-content;
  margin-left: 0;
}
.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #94a3b8;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1.0); }
}

/* Right Panel Tabs */
.right-panel { 
  border-left: 1px solid #e2e8f0; 
  background: #fff; 
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  height: 100%;
}
.right-panel.collapsed {
  width: 0 !important;
  border-left: none;
  opacity: 0;
  pointer-events: none;
}
.panel-tabs { display: flex; background: #f8fafc; border-bottom: 1px solid #e2e8f0; flex-shrink: 0; }
.tab { padding: 12px 20px; font-size: 0.9rem; color: #64748b; border-right: 1px solid #e2e8f0; cursor: pointer; }
.tab.active { background: #fff; color: #3b82f6; font-weight: 600; border-bottom: 2px solid #3b82f6; }

.terminal-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #0f172a;
}

.terminal-actions {
  padding: 8px 16px;
  background: #1e293b;
  display: flex;
  justify-content: flex-end;
  border-bottom: 1px solid #334155;
}

.clear-btn {
  background: transparent;
  border: 1px solid #475569;
  color: #94a3b8;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover {
  background: #334155;
  color: #f8fafc;
  border-color: #64748b;
}

.terminal-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  font-family: 'Fira Code', 'Cascadia Code', Consolas, monospace;
  font-size: 0.9rem;
  line-height: 1.5;
}

.log-line {
  margin-bottom: 16px;
}

.log-cmd-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.prompt {
  color: #10b981;
  font-weight: bold;
}

.cwd {
  color: #3b82f6;
}

.cmd {
  color: #f8fafc;
  font-weight: 500;
}

.output {
  margin: 0;
  padding: 8px 12px;
  background: rgba(30, 41, 59, 0.5);
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #e2e8f0;
}

.output.stderr {
  color: #f87171;
  border-left: 3px solid #ef4444;
}

.output.stdout {
  border-left: 3px solid #10b981;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  height: 100%;
}

/* File Editor Styles are now handled in TerminalFileEditor.vue component */
.panel-content.file-panel-container {
  padding: 0;
  overflow: hidden;
}

</style>
