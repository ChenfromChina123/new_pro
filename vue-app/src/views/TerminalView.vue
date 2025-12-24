<template>
  <div class="terminal-container">
    <div class="terminal-main">
      <div class="terminal-layout">
        <!-- Left Panel: Chat -->
        <div class="chat-panel">
          <div class="chat-main-column">
            <div class="chat-header">
              <div class="header-left">
                <h3>AI 终端助手</h3>
                <span
                  v-if="agentStatus !== 'IDLE'"
                  :class="['status-badge', agentStatus.toLowerCase()]"
                >
                  {{ agentStatus }}
                </span>
                <!-- 解耦架构：身份信息快捷显示 -->
                <div 
                  v-if="identityInfo?.task?.goal" 
                  class="identity-quick-view"
                >
                  <span class="quick-label">任务:</span>
                  <span class="quick-value">{{ identityInfo.task.goal }}</span>
                </div>
              </div>
              <div class="header-right">
                <!-- Phase 2: 检查点按钮 -->
                <button 
                  class="control-btn checkpoint-btn"
                  title="检查点（时间旅行）"
                  @click="showCheckpointDialog = true"
                >
                  ⏱️ 检查点
                  <span 
                    v-if="checkpoints.length > 0" 
                    class="badge"
                  >
                    {{ checkpoints.length }}
                  </span>
                </button>
                
                <!-- Phase 2: 批准按钮 -->
                <button 
                  v-if="pendingApprovals.length > 0"
                  class="control-btn approval-btn"
                  title="待批准工具"
                  @click="showApprovalDialog = true"
                >
                  ⚠️ 待批准
                  <span class="badge">{{ pendingApprovals.length }}</span>
                </button>
                
                <button 
                  v-if="agentStatus === 'RUNNING'"
                  class="control-btn stop-btn"
                  @click="handleStop"
                >
                  ⏹ Stop
                </button>
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
                    <div
                      v-if="item.type === 'header'"
                      class="group-separator"
                    >
                      <span class="separator-line" />
                      <span 
                        class="separator-text" 
                        style="cursor: pointer; display: flex; align-items: center; gap: 8px;"
                        @click="toggleTaskExpand(item.taskId)"
                      >
                        <span
                          class="toggle-icon"
                          style="font-size: 0.8em;"
                        >{{ item.expanded ? '▼' : '▶' }}</span>
                        <span>Task {{ item.taskId }}: {{ item.desc }}</span>
                      </span>
                      <span class="separator-line" />
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
                            <div
                              v-if="item.data.steps && item.data.steps.length > 0"
                              class="steps-block"
                            >
                              <div class="steps-title">
                                执行步骤
                              </div>
                              <ul class="steps-list">
                                <li
                                  v-for="(step, sIdx) in item.data.steps"
                                  :key="sIdx"
                                >
                                  {{ step }}
                                </li>
                              </ul>
                            </div>

                            <div
                              v-if="item.data.message"
                              class="ai-text"
                              v-html="formatMarkdown(item.data.message)"
                            />
                            
                            <!-- Command Execution Info -->
                            <div
                              v-if="item.data.tool"
                              class="tool-call-card"
                            >
                              <div class="tool-header">
                                <div class="tool-header-left">
                                  <span class="tool-icon">🛠️</span>
                                  <span class="tool-label">{{ formatToolSummary(item) }}</span>
                                </div>
                                <button
                                  class="collapse-btn"
                                  @click="toggleToolCollapse(item.data.toolKey || item.id)"
                                >
                                  {{ isToolCollapsed(item.data.toolKey || item.id) ? '展开' : '收起' }}
                                </button>
                              </div>
                              
                              <div v-show="!isToolCollapsed(item.data.toolKey || item.id)">
                                <div class="tool-command">
                                  <code v-if="item.data.tool === 'execute_command'">{{ item.data.command }}</code>
                                  <code v-else-if="item.data.tool === 'write_file' || item.data.tool === 'ensure_file'">{{ item.data.filePath }}</code>
                                  <code v-else-if="item.data.tool === 'search_files'">{{ item.data.searchPattern || '搜索中...' }}</code>
                                  <code v-else-if="item.data.tool === 'read_file_context'">{{ item.data.filePath || '读取中...' }}</code>
                                  <code v-else-if="item.data.tool === 'modify_file'">{{ item.data.filePath }}</code>
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

                                <!-- 工具执行结果（默认随卡片折叠） -->
                                <div
                                  v-if="item.data.toolResult"
                                  class="tool-result"
                                >
                                  <div v-if="item.data.toolResult.stdout">
                                    <div class="result-title">
                                      输出 (stdout)
                                    </div>
                                    <pre class="result-block">{{ item.data.toolResult.stdout }}</pre>
                                  </div>
                                  <div v-if="item.data.toolResult.stderr">
                                    <div class="result-title">
                                      错误 (stderr)
                                    </div>
                                    <pre class="result-block error">{{ item.data.toolResult.stderr }}</pre>
                                  </div>
                                </div>
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
              :disabled="isInputDisabled"
              :placeholder="inputPlaceholder"
              :can-send="!!inputMessage.trim() && !isInputDisabled"
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
          <!-- ... (Existing Right Panel Content) ... -->
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
            class="panel-content-wrapper file-panel-container"
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

          <!-- Checkpoint Timeline Panel -->
          <div
            v-if="activeTab === 'checkpoints'"
            class="panel-content-wrapper"
          >
            <CheckpointTimeline
              :checkpoints="checkpoints"
              :current-checkpoint-id="currentCheckpointId"
              :current-state="{ messageCount: messages.length, taskCount: currentTasks.length }"
              @create="handleCreateCheckpoint"
              @jump="jumpToCheckpoint"
              @delete="handleDeleteCheckpoint"
              @export="handleExportCheckpoint"
            />
          </div>

          <!-- Tool Approval Manager Panel -->
          <div
            v-if="activeTab === 'approvals'"
            class="panel-content-wrapper"
          >
            <ToolApprovalManager
              :pending-approvals="pendingApprovals"
              :approval-history="approvalHistory"
              :auto-approval-rules="autoApprovalRules"
              @approve="approveTool"
              @reject="rejectTool"
              @approve-all="approveAll"
              @reject-all="rejectAll"
              @update-rules="updateAutoApprovalRules"
              @clear-history="clearApprovalHistory"
            />
          </div>

          <!-- Session State Panel -->
          <div
            v-if="activeTab === 'session'"
            class="panel-content-wrapper"
          >
            <SessionStatePanel
              :session-state="sessionState"
              :agent-status="agentStatus"
              :tasks="currentTasks"
              :decision-history="decisionHistoryList"
              :is-streaming="isTyping"
              :message-count="messages.length"
              :tool-call-count="toolCallCount"
              :checkpoint-count="checkpoints.length"
              :pending-approval-count="pendingApprovals.length"
              :avg-response-time="0"
              :avg-tool-execution-time="0"
              :llm-call-count="0"
              :total-tokens="0"
              @refresh="loadSessionState"
              @export="exportSessionState"
            />
          </div>

          <!-- Agent Decision Flow Panel -->
          <div
            v-if="activeTab === 'decisions'"
            class="panel-content-wrapper"
          >
            <AgentDecisionFlow
              :decisions="decisionHistoryList"
              @clear="clearDecisionHistory"
            />
          </div>

          <!-- Identity Info Panel -->
          <div
            v-if="activeTab === 'identity'"
            class="panel-content-wrapper identity-panel"
          >
            <div class="identity-panel-header">
              <h4>Agent 身份信息</h4>
              <button
                v-if="identityInfo"
                class="clear-btn"
                @click="terminalStore.clearIdentity()"
              >
                清除
              </button>
            </div>
            <div 
              v-if="identityInfo" 
              class="identity-content"
            >
              <!-- Core Identity -->
              <div class="identity-section">
                <div class="section-title">
                  核心身份
                </div>
                <div class="identity-item">
                  <span class="label">类型:</span>
                  <span class="value">{{ identityInfo.core?.type || 'N/A' }}</span>
                </div>
                <div class="identity-item">
                  <span class="label">权限:</span>
                  <span 
                    class="badge" 
                    :class="identityInfo.core?.authority"
                  >
                    {{ identityInfo.core?.authority || 'N/A' }}
                  </span>
                </div>
                <div class="identity-item">
                  <span class="label">领域:</span>
                  <span class="value">{{ identityInfo.core?.domain || 'N/A' }}</span>
                </div>
              </div>

              <!-- Task Identity -->
              <div 
                v-if="identityInfo.task?.id" 
                class="identity-section"
              >
                <div class="section-title">
                  任务身份
                </div>
                <div class="identity-item">
                  <span class="label">任务ID:</span>
                  <span class="value">{{ identityInfo.task.id }}</span>
                </div>
                <div class="identity-item">
                  <span class="label">角色:</span>
                  <span class="value">{{ identityInfo.task.role || 'N/A' }}</span>
                </div>
                <div class="identity-item">
                  <span class="label">目标:</span>
                  <span class="value">{{ identityInfo.task.goal || 'N/A' }}</span>
                </div>
              </div>

              <!-- Viewpoint Identity -->
              <div 
                v-if="identityInfo.viewpoint?.file" 
                class="identity-section"
              >
                <div class="section-title">
                  视角身份
                </div>
                <div class="identity-item">
                  <span class="label">文件:</span>
                  <span class="value">{{ identityInfo.viewpoint.file }}</span>
                </div>
                <div 
                  v-if="identityInfo.viewpoint.symbol" 
                  class="identity-item"
                >
                  <span class="label">符号:</span>
                  <span class="value">{{ identityInfo.viewpoint.symbol }}</span>
                </div>
                <div 
                  v-if="identityInfo.viewpoint.line" 
                  class="identity-item"
                >
                  <span class="label">行号:</span>
                  <span class="value">{{ identityInfo.viewpoint.line }}</span>
                </div>
              </div>
            </div>
            <div 
              v-else 
              class="empty-state"
            >
              <p>
                暂无身份信息
              </p>
              <p class="hint">
                身份信息将在 Agent 执行任务时自动更新
              </p>
            </div>
          </div>

          <!-- State Slices Panel -->
          <div
            v-if="activeTab === 'state'"
            class="panel-content-wrapper state-panel"
          >
            <div class="state-panel-header">
              <h4>状态切片 ({{ stateSlices.length }})</h4>
              <div class="header-actions">
                <button
                  class="clear-btn"
                  :disabled="stateSlices.length === 0"
                  @click="terminalStore.clearStateSlices()"
                >
                  清除
                </button>
              </div>
            </div>
            <div class="state-content">
              <div 
                v-if="stateSlices.length > 0" 
                class="slices-list"
              >
                <div
                  v-for="(slice, index) in stateSlices.slice().reverse()"
                  :key="index"
                  class="slice-item"
                >
                  <div class="slice-header">
                    <span class="slice-source">{{ slice.source }}</span>
                    <span class="slice-timestamp">{{ formatTime(slice.timestamp) }}</span>
                  </div>
                  <div 
                    v-if="slice.scope && slice.scope.length > 0"
                    class="slice-scope" 
                  >
                    <span class="scope-label">作用域:</span>
                    <span class="scope-value">{{ slice.scope.join(', ') }}</span>
                  </div>
                  <div class="slice-data">
                    <span class="data-label">数据项数:</span>
                    <span class="data-value">{{ Object.keys(slice.data || {}).length }}</span>
                  </div>
                  <div class="slice-authority">
                    <span 
                      class="authority-badge" 
                      :class="slice.authority"
                    >
                      {{ slice.authority === 'fact' ? '事实' : '模型输出' }}
                    </span>
                  </div>
                </div>
              </div>
              <div 
                v-else 
                class="empty-state"
              >
                <p>
                  暂无状态切片
                </p>
                <p class="hint">
                  状态切片将在 Agent 执行时自动生成
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Phase 2: 检查点对话框 -->
    <CheckpointDialog
      :visible="showCheckpointDialog"
      :checkpoints="checkpoints"
      @close="showCheckpointDialog = false"
      @create="handleCreateCheckpoint"
      @jump="jumpToCheckpoint"
      @delete="handleDeleteCheckpoint"
    />
    
    <!-- Phase 2: 批准对话框 -->
    <ToolApprovalDialog
      :visible="showApprovalDialog"
      :pending-approvals="pendingApprovals"
      @close="showApprovalDialog = false"
      @approve="approveTool"
      @reject="rejectTool"
      @approve-all="approveAll"
      @reject-all="rejectAll"
    />
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { useTerminalStore } from '@/stores/terminal'
import { storeToRefs } from 'pinia'
import { API_CONFIG } from '@/config/api'
import TerminalFileExplorer from '@/components/TerminalFileExplorer.vue'
import TerminalNotebook from '@/components/TerminalNotebook.vue'
import TerminalFileEditor from '@/components/TerminalFileEditor.vue'
import TerminalChatInput from '@/components/terminal/TerminalChatInput.vue'
import CheckpointDialog from '@/components/terminal/CheckpointDialog.vue'
import ToolApprovalDialog from '@/components/terminal/ToolApprovalDialog.vue'
import CheckpointTimeline from '@/components/terminal/CheckpointTimeline.vue'
import ToolApprovalManager from '@/components/terminal/ToolApprovalManager.vue'
import SessionStatePanel from '@/components/terminal/SessionStatePanel.vue'
import AgentDecisionFlow from '@/components/terminal/AgentDecisionFlow.vue'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
// Phase 2: 导入新的 Terminal API 服务
import { checkpointService, approvalService, sessionStateService } from '@/services/terminalService'

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
  groupedMessages,
  agentStatus,
  decisionHistoryList,
  identityInfo,
  stateSlices,
  visibleFiles,
  visibleFunctions
} = storeToRefs(terminalStore)

const scrollerRef = ref(null)
const searchText = ref('')
const expandedTaskIds = ref(new Set())

// ... (Existing flatViewItems logic) ...
const flatViewItems = computed(() => {
  const items = []
  let idCounter = 0
  
  groupedMessages.value.forEach(group => {
    const isExpanded = group.taskId ? expandedTaskIds.value.has(group.taskId) : true
    
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

    // Add Messages
    if (isExpanded || !group.taskId) {
      group.messages.forEach(msg => {
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

// 工具卡片折叠/展开
const isToolCollapsed = (key) => collapsedTools.value.has(key)
const toggleToolCollapse = (key) => {
  if (!key) return
  if (collapsedTools.value.has(key)) {
    collapsedTools.value.delete(key)
  } else {
    collapsedTools.value.add(key)
  }
}

// 显示工具调用摘要
const formatToolSummary = (item) => {
  const tool = item?.data?.tool
  if (!tool) return '工具调用'

  const path = item.data.filePath || item.data.path
  const pattern = item.data.searchPattern || item.data.pattern
  const filePattern = item.data.filePattern || item.data.file_pattern

  const formatRange = (f) => {
    const start = f?.start_line ?? f?.start ?? '?'
    const end = f?.end_line ?? f?.end ?? start ?? '?'
    return `${f?.path || path || '文件'}[${start},${end}]`
  }

  switch (tool) {
    case 'read_file_context':
      if (item.data.files?.length) {
        return `read: ${item.data.files.map(formatRange).join(', ')}`
      }
      return `read: ${path || '文件'}`
    case 'search_files':
      return `search: ${pattern || '*'} in ${filePattern || '*' }`
    case 'modify_file':
      return `modify: ${path || '文件'} (${item.data.operations?.length || 0} ops)`
    case 'write_file':
    case 'ensure_file':
      return `write: ${path || '文件'}`
    case 'execute_command':
      return `exec: ${item.data.command || ''}`
    default:
      return tool
  }
}

const inputMessage = ref('')
const currentModel = ref('deepseek-chat')
const collapsedTools = ref(new Set()) // 默认折叠工具执行结果
const isTyping = ref(false)
const isExecuting = ref(false)
const terminalRef = ref(null)
const fileExplorer = ref(null)

// File Editor State
const editingFile = ref(null)
const editedContent = ref(null)
const isSaving = ref(false)
const isNotebook = ref(false)

// UI Persistence
const rightPanelCollapsed = ref(uiStore.rightPanelCollapsed)
const taskListCollapsed = ref(uiStore.taskListCollapsed)
const rightPanelWidth = ref(uiStore.rightPanelWidth)
const activeTab = ref(uiStore.activeTab === 'req' ? 'terminal' : uiStore.activeTab)

// 监听 activeTab 变化，确保选中的标签在视图内
watch(activeTab, () => {
  nextTick(() => {
    const activeTabEl = document.querySelector('.tab.active')
    if (activeTabEl) {
      activeTabEl.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
    }
  })
})

// Phase 2: 检查点相关状态
const checkpoints = ref([])
const showCheckpointDialog = ref(false)

// Phase 2: 批准相关状态
const pendingApprovals = ref([])
const showApprovalDialog = ref(false)

// Phase 2: 会话状态
const sessionState = ref(null)

const approvalHistory = ref([])
const autoApprovalRules = ref({
  read_file: true,
  search_files: true,
  execute_command: false,
  write_file: false,
  modify_file: false,
  delete_file: false
})
const toolCallCount = ref(0)
const currentCheckpointId = ref(null)

watch(rightPanelCollapsed, (val) => uiStore.saveState('rightPanelCollapsed', val))
watch(taskListCollapsed, (val) => uiStore.saveState('taskListCollapsed', val))
watch(rightPanelWidth, (val) => uiStore.saveState('rightPanelWidth', val))
watch(activeTab, (val) => uiStore.saveState('activeTab', val))

// 标签定义
const tabMeta = {
  'terminal': { id: 'terminal', label: '终端输出' },
  'files': { id: 'files', label: '文件管理' },
  'tasks': { id: 'tasks', label: '任务链' },
  'checkpoints': { id: 'checkpoints', label: '检查点' },
  'approvals': { id: 'approvals', label: '工具批准' },
  'session': { id: 'session', label: '会话状态' },
  'decisions': { id: 'decisions', label: '决策流程' },
  'identity': { id: 'identity', label: '身份信息' },
  'state': { id: 'state', label: '状态切片' }
}
// 确保所有标签都显示，包括新添加的
const defaultTabOrder = ['terminal', 'files', 'tasks', 'checkpoints', 'approvals', 'session', 'decisions', 'identity', 'state']
const tabs = ref([])

// 初始化标签页，确保包含所有默认标签
const initTabs = () => {
  let storedOrder = uiStore.tabOrder || []
  
  // 确保 storedOrder 是数组
  if (!Array.isArray(storedOrder)) {
    storedOrder = []
  }
  
  // 确保所有默认标签都在列表中
  const mergedOrder = [...storedOrder]
  defaultTabOrder.forEach(id => {
    if (!mergedOrder.includes(id)) {
      mergedOrder.push(id)
    }
  })
  
  // 过滤掉不存在于 tabMeta 中的 id
  const validOrder = mergedOrder.filter(id => tabMeta[id])
  
  // 更新 store 中的 tabOrder，确保下一次加载时包含所有标签
  if (validOrder.length !== storedOrder.length) {
    uiStore.saveState('tabOrder', validOrder)
  }
  
  tabs.value = validOrder.map(id => tabMeta[id])
}

initTabs()
watch(tabs, (newTabs) => {
  const order = newTabs.map(t => t.id)
  uiStore.saveState('tabOrder', order)
}, { deep: true })

let draggedTab = null
const handleTabDragStart = (e, tab) => {
  draggedTab = tab
  e.dataTransfer.effectAllowed = 'move'
}
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
    // 解耦架构：创建新会话时清除状态
    terminalStore.clearIdentity()
    terminalStore.clearStateSlices()
    terminalStore.clearScope()
  }
  
  // Phase 3: 初始化 Agent 循环管理器
  if (currentSessionId.value) {
    await loadCheckpoints()
    await loadPendingApprovals()
    await loadSessionState()
  }
  
  // 状态轮询（参考 void-main 的状态同步机制）
  // 当 Agent 正在运行时，定期轮询状态以确保同步
  const statePollInterval = setInterval(async () => {
    if (currentSessionId.value && (agentStatus.value === 'RUNNING' || agentStatus.value === 'AWAITING_APPROVAL')) {
      await loadSessionState()
    }
  }, 2000) // 每 2 秒轮询一次
  
  // 清理定时器
  onUnmounted(() => {
    clearInterval(statePollInterval)
  })
  
  scrollToBottom()
})

watch(currentSessionId, async () => {
  await nextTick()
  scrollToBottom()
  // Phase 2: 加载检查点和批准数据
  if (currentSessionId.value) {
    await loadCheckpoints()
    await loadPendingApprovals()
    await loadSessionState()
  }
})

// Phase 2: 加载检查点
const loadCheckpoints = async () => {
  if (!currentSessionId.value) return
  try {
    const result = await checkpointService.getCheckpoints(currentSessionId.value)
    if (result?.data) {
      checkpoints.value = result.data
    }
  } catch (error) {
    console.error('加载检查点失败:', error)
  }
}

// Phase 2: 加载待批准列表
const loadPendingApprovals = async () => {
  if (!currentSessionId.value) return
  try {
    const result = await approvalService.getPendingApprovals(currentSessionId.value)
    if (result?.data) {
      pendingApprovals.value = result.data
      // 如果有待批准项，显示对话框
      if (result.data.length > 0) {
        showApprovalDialog.value = true
      }
    }
  } catch (error) {
    console.error('加载待批准列表失败:', error)
  }
}

// Phase 2: 加载会话状态
// Phase 2: 加载会话状态（参考 void-main 的状态同步）
const loadSessionState = async () => {
  if (!currentSessionId.value) return
  try {
    const result = await sessionStateService.getSessionState(currentSessionId.value)
    if (result?.data) {
      sessionState.value = result.data
      
      // 同步 AgentStatus（参考 void-main 的状态管理）
      if (result.data.status) {
        terminalStore.setAgentStatus(result.data.status)
        agentStatus.value = result.data.status
      }
      
      // 同步 StreamState
      if (result.data.streamState) {
        const streamState = result.data.streamState
        // 根据 StreamState 更新 UI 状态
        if (streamState.type === 'STREAMING_LLM') {
          isTyping.value = true
        } else if (streamState.type === 'RUNNING_TOOL') {
          isTyping.value = false
          // 可以在这里显示工具执行状态
        } else if (streamState.type === 'AWAITING_USER') {
          isTyping.value = false
          // 如果等待批准，加载待批准列表
          if (result.data.status === 'AWAITING_APPROVAL') {
            await loadPendingApprovals()
          }
        } else if (streamState.type === 'IDLE') {
          isTyping.value = false
        }
      }
      
      console.log('[TerminalView] Session state loaded:', {
        status: result.data.status,
        streamState: result.data.streamState?.type
      })
    }
  } catch (error) {
    console.error('加载会话状态失败:', error)
  }
}

// Phase 2: 跳转到检查点
const jumpToCheckpoint = async (checkpointId) => {
  try {
    const result = await checkpointService.jumpToCheckpoint(checkpointId)
    if (result?.data) {
      console.log('已跳转到检查点，恢复文件:', result.data)
      uiStore.showToast(`已恢复 ${result.data.length} 个文件`)
      // 刷新文件浏览器
      if (fileExplorer.value) {
        fileExplorer.value.refresh()
      }
      showCheckpointDialog.value = false
    }
  } catch (error) {
    console.error('跳转到检查点失败:', error)
    uiStore.showToast('跳转失败: ' + error.message)
  }
}

// Phase 2: 创建检查点
const handleCreateCheckpoint = async (description) => {
  if (!currentSessionId.value) return
  try {
    const messageOrder = messages.value ? messages.value.length : 0
    await checkpointService.createCheckpoint({
      sessionId: currentSessionId.value,
      messageOrder: messageOrder,
      description: description
    })
    await loadCheckpoints()
    uiStore.showToast('检查点创建成功')
  } catch (error) {
    console.error('创建检查点失败:', error)
    uiStore.showToast('创建失败: ' + error.message)
  }
}

// Phase 2: 删除检查点
const handleDeleteCheckpoint = async (checkpointId) => {
  try {
    await checkpointService.deleteCheckpoint(checkpointId)
    await loadCheckpoints()
    uiStore.showToast('检查点已删除')
  } catch (error) {
    console.error('删除检查点失败:', error)
    uiStore.showToast('删除失败: ' + error.message)
  }
}

// Phase 2: 导出检查点
const handleExportCheckpoint = async (checkpointId) => {
  try {
    const data = await checkpointService.exportCheckpoint(checkpointId)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `checkpoint-${checkpointId}.json`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
    uiStore.showToast('导出成功')
  } catch (error) {
    console.error('导出检查点失败:', error)
    uiStore.showToast('导出失败: ' + error.message)
  }
}

// Phase 2: 批准工具调用（参考 void-main 的批准机制）
const approveTool = async (payload) => {
  // Support both (id, reason) and ({id, reason})
  let decisionId, reason
  if (typeof payload === 'object' && payload.id) {
    decisionId = payload.id
    reason = payload.reason
  } else {
    decisionId = payload
    reason = arguments[1]
  }

  try {
    console.log('[TerminalView] Approving tool call:', decisionId)
    await approvalService.approveToolCall(decisionId, reason)
    
    // 重新加载待批准列表
    await loadPendingApprovals()
    
    // 检查是否还有待批准的工具
    if (pendingApprovals.value.length === 0) {
      showApprovalDialog.value = false
      // 更新状态为运行中（后端会自动继续循环）
      terminalStore.setAgentStatus('RUNNING')
      
      // 轮询状态以确保同步（后端可能正在执行工具）
      setTimeout(async () => {
        await loadSessionState()
      }, 500)
    }
    
    uiStore.showToast('工具调用已批准')
  } catch (error) {
    console.error('批准工具调用失败:', error)
    uiStore.showToast('批准失败: ' + error.message)
  }
}

// Phase 2: 拒绝工具调用（参考 void-main 的拒绝机制）
const rejectTool = async (payload) => {
  // Support both (id, reason) and ({id, reason})
  let decisionId, reason
  if (typeof payload === 'object' && payload.id) {
    decisionId = payload.id
    reason = payload.reason
  } else {
    decisionId = payload
    reason = arguments[1]
  }

  try {
    console.log('[TerminalView] Rejecting tool call:', decisionId)
    await approvalService.rejectToolCall(decisionId, reason)
    
    // 重新加载待批准列表
    await loadPendingApprovals()
    
    // 检查是否还有待批准的工具
    if (pendingApprovals.value.length === 0) {
      showApprovalDialog.value = false
      // 拒绝后，Agent 状态应该回到 IDLE 或继续等待
      // 后端会处理拒绝后的逻辑
      terminalStore.setAgentStatus('IDLE')
      
      // 轮询状态以确保同步
      setTimeout(async () => {
        await loadSessionState()
      }, 500)
    }
    
    uiStore.showToast('工具调用已拒绝')
  } catch (error) {
    console.error('拒绝工具调用失败:', error)
    uiStore.showToast('拒绝失败: ' + error.message)
  }
}

// Phase 2: 批量批准
const approveAll = async () => {
  if (!currentSessionId.value) return
  try {
    await approvalService.approveAllPending(currentSessionId.value)
    await loadPendingApprovals()
    showApprovalDialog.value = false
    uiStore.showToast('已全部批准')
  } catch (error) {
    console.error('批量批准失败:', error)
    uiStore.showToast('批量批准失败: ' + error.message)
  }
}

// Phase 2: 批量拒绝
const rejectAll = async () => {
  if (!currentSessionId.value) return
  try {
    // Assuming backend supports this or we loop through
    // Since backend API might not have rejectAll, we can loop if needed
    // But let's check if approvalService has rejectAllPending. 
    // If not, we iterate.
    if (approvalService.rejectAllPending) {
        await approvalService.rejectAllPending(currentSessionId.value)
    } else {
        // Fallback: reject one by one
        for (const approval of pendingApprovals.value) {
            await approvalService.rejectToolCall(approval.decisionId, '批量拒绝')
        }
    }
    await loadPendingApprovals()
    showApprovalDialog.value = false
    uiStore.showToast('已全部拒绝')
  } catch (error) {
    console.error('批量拒绝失败:', error)
    uiStore.showToast('批量拒绝失败: ' + error.message)
  }
}

watch(terminalLogs, () => {
  nextTick()
  scrollToBottom()
}, { deep: true })

// ... (Existing file handling methods: handleFileSelect, closeEditor, saveEditedFile) ...
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
      if (fileExplorer.value) fileExplorer.value.refresh()
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

// ... (Existing utils) ...
let scrollScheduled = false
const scrollToBottom = (force = false) => {
  if (scrollScheduled) return
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

const saveMessage = async (content, senderType, extra = {}) => {
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
        model: currentModel.value,
        ...extra
      })
    })
  } catch (e) { console.error(e) }
}

const isInputDisabled = computed(() => {
    return isTyping.value || isExecuting.value || (agentStatus.value === 'RUNNING' || agentStatus.value === 'WAITING_TOOL')
})

const inputPlaceholder = computed(() => {
    if (agentStatus.value === 'RUNNING') return 'Agent 运行中... (可输入 pause 或 stop)'
    if (agentStatus.value === 'WAITING_TOOL') return '等待工具执行...'
    if (agentStatus.value === 'ERROR') return '发生错误，请输入 retry 重试'
    return '输入您的指令...'
})

// Phase 2: 更新中断处理函数
const handleStop = async () => {
  if (!currentSessionId.value) return
  
  try {
    const result = await sessionStateService.interruptAgentLoop(currentSessionId.value)
    if (result?.data) {
      console.log('Agent 循环已中断')
      // 可以显示提示消息
    }
  } catch (error) {
    console.error('中断 Agent 循环失败:', error)
    // 降级处理：发送 stop 消息
    await sendMessage('stop')
  }
}

const sendMessage = async (overrideText) => {
  const text = overrideText || inputMessage.value.trim()
  if (!text) return
  
  messages.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  isTyping.value = true
  scrollToBottom()
  await saveMessage(text, 1)

  // Phase 3: 使用 AgentLoopManager 启动循环
  // if (agentLoopManager.value) {
  //   await agentLoopManager.value.startLoop(text, currentModel.value)
  //   // 创建检查点（用户消息后）
  //   await agentLoopManager.value.createCheckpoint('AUTO', `用户消息: ${text.substring(0, 50)}`)
  //   await loadCheckpoints()
  // }

  // Start Loop with initial prompt
  await processAgentLoop(text, null)
}

const processAgentLoop = async (prompt, toolResult) => {
  try {
    console.log('[TerminalView] processAgentLoop called:', {
      prompt: prompt || '(empty)',
      hasToolResult: toolResult != null,
      toolResultDecisionId: toolResult?.decision_id,
      toolResultExitCode: toolResult?.exit_code,
      sessionId: currentSessionId.value
    })
    
    const body = {
        prompt: prompt || "",
        session_id: currentSessionId.value,
        model: currentModel.value,
        tool_result: toolResult,
        // 解耦架构：包含作用域信息
        scope: {
            visible_files: visibleFiles.value,
            visible_functions: visibleFunctions.value
        }
    }
    
    console.log('[TerminalView] Request body:', {
      prompt: body.prompt,
      session_id: body.session_id,
      hasToolResult: body.tool_result != null,
      toolResult: body.tool_result
    })
    
    const response = await fetch(`${API_CONFIG.baseURL}/api/terminal/chat-stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`,
        'Accept': 'text/event-stream, application/json'
      },
      body: JSON.stringify(body)
    })

    if (!response.ok) throw new Error('Request failed')

    let currentAiMsg = { 
      role: 'ai', 
      thought: '', 
      message: '', 
      tool: null, 
      status: 'pending', 
      showThought: true 
    }
    messages.value.push(currentAiMsg)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullContent = ''
    let currentEvent = 'message'
    
    const processBuffer = () => {
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      let needsScroll = false
      
      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) {
            currentEvent = 'message'
            continue
        }
        
        if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const dataStr = line.slice(5).trim()
          if (dataStr === '[DONE]') continue
          
          try {
            const json = JSON.parse(dataStr)
            
            if (currentEvent === 'message') {
                // 处理推理内容
                if (json.reasoning_content) {
                  currentAiMsg.thought = (currentAiMsg.thought || '') + json.reasoning_content
                }
                
                // 处理回复内容 - 实时更新，不等待累积
                if (json.content) {
                  fullContent += json.content
                  needsScroll = true
                  
                  // 实时更新显示内容，提供更好的流式体验
                  // 如果包含代码块标记，尝试提取前面的文本
                  if (fullContent.includes('```json')) {
                    const codeBlockStart = fullContent.indexOf('```json')
                    if (codeBlockStart >= 0) {
                      currentAiMsg.message = fullContent.substring(0, codeBlockStart).trim()
                    } else {
                      currentAiMsg.message = fullContent
                    }
                  } else if (fullContent.includes('{') && !fullContent.trim().startsWith('{')) {
                    // 如果有 JSON 但不是以 { 开头，提取前面的文本
                    const jsonStart = fullContent.indexOf('{')
                    if (jsonStart >= 0) {
                      currentAiMsg.message = fullContent.substring(0, jsonStart).trim()
                    } else {
                      currentAiMsg.message = fullContent
                    }
                  } else {
                    // 直接显示全部内容
                    currentAiMsg.message = fullContent
                  }
                }
            } else if (currentEvent === 'tool_result') {
                // 处理工具执行结果（参考 void-main 的 tool result 处理）
                console.log('[TerminalView] Received tool result:', json)
                
                // 更新消息状态
                currentAiMsg.toolResult = json
                currentAiMsg.status = json.success ? 'success' : 'error'
                
                // 提取工具信息
                const toolName = json.toolName || currentAiMsg.tool || 'Unknown'
                const decisionId = json.decisionId || json.decision_id
                
                // 更新工具显示
                if (!currentAiMsg.tool) {
                  currentAiMsg.tool = toolName
                }
                if (decisionId && !currentAiMsg.toolKey) {
                  currentAiMsg.toolKey = decisionId
                }
                
                // 更新消息内容
                if (json.success) {
                  currentAiMsg.message = `工具 "${toolName}" 执行成功`
                  if (json.stringResult) {
                    currentAiMsg.toolResultText = json.stringResult
                  }
                } else {
                  currentAiMsg.message = `工具 "${toolName}" 执行失败`
                  if (json.error) {
                    currentAiMsg.toolResultText = json.error
                  }
                }
                
                // 追加到终端日志
                terminalLogs.value.push({ 
                  command: `Tool: ${toolName}`, 
                  output: json.stringResult || json.result || json.error || '', 
                  type: json.success ? 'stdout' : 'stderr',
                  cwd: currentCwd.value 
                })
                
                // 保存工具结果消息
                await saveMessage(`TOOL_RESULT: ${toolName}`, 3, {
                  exit_code: json.success ? 0 : -1,
                  stdout: json.stringResult || json.result || '',
                  stderr: json.error || ''
                })
                
                // 重要：工具结果已由后端处理，Agent 循环会自动继续
                // 不需要前端再次调用 processAgentLoop
                // 后端会在工具执行后自动继续循环
                
                // 更新状态
                terminalStore.setAgentStatus('RUNNING')
                
            } else if (currentEvent === 'waiting_approval') {
                // 处理等待批准事件（参考 void-main 的 approval 机制）
                console.log('[TerminalView] Waiting for approval:', json)
                
                const toolName = json.tool || json.toolName
                const decisionId = json.decision_id || json.decisionId
                const params = json.params || {}
                
                // 更新消息状态
                currentAiMsg.status = 'pending'
                currentAiMsg.message = `等待用户批准工具调用: ${toolName}`
                currentAiMsg.tool = toolName
                currentAiMsg.toolKey = decisionId
                
                // 更新 Agent 状态
                terminalStore.setAgentStatus('AWAITING_APPROVAL')
                
                // 重新加载待批准列表并显示对话框
                await loadPendingApprovals()
                if (pendingApprovals.value.length > 0) {
                  showApprovalDialog.value = true
                }
                
            } else if (currentEvent === 'interrupt') {
                // 处理中断事件
                console.log('[TerminalView] Agent loop interrupted:', json)
                
                currentAiMsg.status = 'interrupted'
                currentAiMsg.message = json.message || 'Agent 循环已被中断'
                
                terminalStore.setAgentStatus('IDLE')
                isTyping.value = false
                
            } else if (currentEvent === 'error') {
                // 处理错误事件（参考 void-main 的 error handling）
                console.error('[TerminalView] Agent error:', json)
                
                currentAiMsg.status = 'error'
                currentAiMsg.message = json.message || '发生错误'
                
                terminalStore.setAgentStatus('ERROR')
                isTyping.value = false
                
                // 显示错误提示
                if (json.message) {
                  // 可以在这里添加错误提示 UI
                  console.error('Agent Error:', json.message)
                }
            }
          } catch (err) {
            // JSON 解析失败，可能是部分数据，继续累积
            console.debug('Partial JSON data or parse error:', err, dataStr.substring(0, 50))
          }
        }
      }
      
      // 优化滚动：使用 requestAnimationFrame 减少滚动频率
      if (needsScroll) {
        requestAnimationFrame(() => {
          scrollToBottom()
        })
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      processBuffer()
    }
    if (buffer) processBuffer()

    isTyping.value = false
    
    // Parse Final JSON Content (Decision Envelope)
    try {
      // 尝试多种方式提取 JSON
      let jsonStr = null
      
      // 方式1: 查找 ```json 代码块
      const codeBlockMatch = fullContent.match(/```json\s*([\s\S]*?)```/)
      if (codeBlockMatch) {
        jsonStr = codeBlockMatch[1].trim()
      }
      
      // 方式2: 查找第一个 { 到最后一个 }
      if (!jsonStr) {
        const jsonMatch = fullContent.match(/\{[\s\S]*\}/)
        if (jsonMatch) {
          jsonStr = jsonMatch[0]
        }
      }
      
      // 方式3: 如果整个内容看起来像 JSON
      if (!jsonStr && fullContent.trim().startsWith('{')) {
        jsonStr = fullContent.trim()
      }
      
      if (!jsonStr) {
        console.warn('No JSON found in response:', fullContent.substring(0, 200))
        currentAiMsg.message = fullContent
        await saveMessage(fullContent, 2)
        return
      }
      
      // 清理 JSON 字符串（移除可能的换行和多余空格）
      jsonStr = jsonStr.trim()
      
      // 尝试解析 JSON
      let decision
      try {
        decision = JSON.parse(jsonStr)
      } catch (parseErr) {
        console.error('JSON parse error:', parseErr, 'Content:', jsonStr.substring(0, 200))
        // 如果解析失败，尝试修复常见的 JSON 问题
        jsonStr = jsonStr.replace(/,\s*}/g, '}').replace(/,\s*]/g, ']')
        try {
          decision = JSON.parse(jsonStr)
        } catch (cleanupErr) {
          console.error('JSON parse failed after cleanup:', cleanupErr)
          currentAiMsg.message = fullContent
          await saveMessage(fullContent, 2)
          return
        }
      }

      // 检查是否是任务列表消息（实时）
      if (decision.type === 'task_list' || decision.tasks) {
        console.log('[TerminalView] Task list received:', decision.tasks)
        terminalStore.currentTasks = decision.tasks || []
        currentAiMsg.message = '任务计划已生成，即将开始执行...'
        currentAiMsg.status = 'success'
        
        await saveMessage(JSON.stringify(decision), 2)
        
        // 自动触发第一步执行
        setTimeout(async () => {
            await processAgentLoop("开始执行任务", null)
        }, 1000)
        return
      }
      
      // 1. De-duplication check
      if (decision.decision_id && terminalStore.hasDecision(decision.decision_id)) {
          console.warn('Duplicate decision ignored:', decision.decision_id)
          return
      }
      if (decision.decision_id) {
          // Phase 3: 添加时间戳并保存完整决策对象
          decision.timestamp = decision.timestamp || Date.now()
          terminalStore.addDecision(decision)
      }

      // Update UI with Decision info
      currentAiMsg.tool = decision.type === 'TOOL_CALL' ? decision.action : decision.type
      currentAiMsg.command = decision.params?.command
      currentAiMsg.filePath = decision.params?.path
      currentAiMsg.searchPattern = decision.params?.pattern
      currentAiMsg.filePattern = decision.params?.file_pattern
      currentAiMsg.files = decision.params?.files
      currentAiMsg.operations = decision.params?.operations
      currentAiMsg.toolKey = decision.decision_id || `tool-${messages.value.length}`
      // 工具调用的正文避免显示原始 JSON，改用折叠卡片展示
      if (decision.type === 'TOOL_CALL') {
        currentAiMsg.message = ''
        // 后端接管执行，这里仅显示状态
        terminalStore.setAgentStatus('WAITING_TOOL')
        currentAiMsg.status = 'pending'
      }
      // 默认折叠工具结果
      collapsedTools.value.add(currentAiMsg.toolKey)
      
      // 解耦架构：保存身份信息（如果后端返回）
      if (decision.identity) {
          terminalStore.setIdentity(decision.identity)
      }

      // 解耦架构：保存状态切片（如果后端返回）
      if (decision.state_slices) {
          decision.state_slices.forEach(slice => {
              terminalStore.addStateSlice(slice)
          })
      }

      // 2. Handle Action (参考 void-main 的 Agent 循环)
      // 注意：后端已经接管了工具执行和循环控制
      // 前端只需要显示状态，不需要主动触发下一步
      
      if (decision.type === 'TOOL_CALL') {
          // 工具调用已由后端处理，等待 SSE tool_result 事件
          // 后端会自动执行工具并继续循环
          // 前端只需要显示工具调用信息
          console.log('[TerminalView] Tool call detected, waiting for backend execution')
          
      } else if (decision.type === 'TASK_COMPLETE') {
          currentAiMsg.message = "当前任务已完成"
          currentAiMsg.status = 'success'
          
          await saveMessage(JSON.stringify(decision), 2)
      } else if (decision.type === 'PAUSE') {
          terminalStore.setAgentStatus('PAUSED')
      }
      
      await saveMessage(JSON.stringify(decision), 2)

  } catch (finalErr) {
    // Fallback for non-JSON or partial content
    console.debug('Final JSON process fallback:', finalErr)
    currentAiMsg.message = fullContent
    await saveMessage(fullContent, 2)
  }

} catch (outerErr) {
  console.error(outerErr)
  isTyping.value = false
  terminalStore.setAgentStatus('ERROR')
}
}

// 解耦架构：格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return 'N/A'
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  })
}

// Phase 3: 新增方法

/**
 * 更新自动批准规则
 */
function updateAutoApprovalRules(payload) {
  if (typeof payload === 'object' && !payload.toolName) {
    // 批量更新
    autoApprovalRules.value = { ...autoApprovalRules.value, ...payload }
  } else {
    // 单个更新
    autoApprovalRules.value[payload.toolName] = payload.enabled
  }
  
  // 同步到 AgentLoopManager
  // if (agentLoopManager.value) {
  //   Object.entries(autoApprovalRules.value).forEach(([toolName, enabled]) => {
  //     agentLoopManager.value.updateAutoApprovalRule(toolName, enabled)
  //   })
  // }
}

/**
 * 清空批准历史
 */
function clearApprovalHistory() {
  approvalHistory.value = []
}

/**
 * 清空决策历史
 */
function clearDecisionHistory() {
  if (confirm('确定要清空决策历史吗？')) {
    terminalStore.decisionHistory.clear()
    terminalStore.decisionHistoryList = []
  }
}

/**
 * 导出会话状态
 */
function exportSessionState(data) {
  console.log('导出会话状态:', data)
  uiStore.showToast('会话状态已导出')
}
</script>

<style scoped>
/* ... (Existing Styles) ... */
/* Add Status Badge Styles */
.status-badge {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: 600;
    margin-left: 10px;
}
.status-badge.running { background: #dcfce7; color: #166534; }
.status-badge.paused { background: #fef9c3; color: #854d0e; }
.status-badge.error { background: #fee2e2; color: #991b1b; }
.status-badge.idle { background: #f1f5f9; color: #64748b; }

/* 解耦架构：身份信息快捷显示 */
.identity-quick-view {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f1f5f9;
  border-radius: 6px;
  font-size: 0.8rem;
  margin-left: 10px;
}

.quick-label {
  color: #64748b;
  font-weight: 500;
}

.quick-value {
  color: #1e293b;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.control-btn {
    padding: 4px 12px;
    border-radius: 6px;
    font-size: 0.85rem;
    cursor: pointer;
    border: none;
    transition: all 0.2s;
}
.stop-btn {
    background: #fee2e2;
    color: #991b1b;
}
.stop-btn:hover {
    background: #fecaca;
}

/* ... Include previous styles ... */
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
  padding: 0 20px; 
  height: 48px;
  border-bottom: 1px solid #e2e8f0; 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  background: #f8fafc; 
  z-index: 50; 
  flex-shrink: 0;
}
.header-left { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  height: 100%;
}
.header-left h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
}
.header-right { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  height: 100%;
}

.control-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.85rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  height: 32px;
}

.control-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #1e293b;
}

.control-btn.stop-btn {
  background: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

.control-btn.stop-btn:hover {
  background: #fee2e2;
  border-color: #fca5a5;
}

.control-btn.approval-btn {
  background: #fffbeb;
  border-color: #fef3c7;
  color: #d97706;
}

.control-btn.approval-btn:hover {
  background: #fef3c7;
}

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
  overflow: hidden;
  background: #fcfcfc; 
  display: flex; 
  flex-direction: column; 
}

.scroller {
  height: 100%;
  overflow-y: auto;
  padding: 24px 20px;
  box-sizing: border-box;
}

.message { 
  width: 100%; 
  display: flex; 
  flex-direction: column;
  margin-bottom: 24px;
}

.message-content { 
  max-width: 900px; 
  margin: 0 auto; 
  width: 100%;
  display: flex; 
  flex-direction: column;
}

.user-bubble {
  background: #3b82f6;
  color: #fff;
  padding: 10px 16px;
  border-radius: 12px 12px 2px 12px;
  max-width: 80%;
  align-self: flex-end;
  margin-left: auto;
  font-size: 0.95rem;
  line-height: 1.5;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.1);
}

.ai-bubble {
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 16px;
  border-radius: 12px 12px 12px 2px;
  max-width: 90%;
  align-self: flex-start;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  word-break: break-word;
  overflow-wrap: anywhere;
}

.thought-block {
  margin-bottom: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  word-break: break-word;
}

.thought-title {
  padding: 8px 12px;
  background: #f1f5f9;
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.thought-content {
  padding: 12px;
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
  font-style: italic;
  border-top: 1px solid #e2e8f0;
  word-break: break-word;
}

.ai-text {
  font-size: 1rem;
  line-height: 1.6;
  color: #1e293b;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.ai-text :deep(p) {
  margin-bottom: 12px;
  word-break: break-word;
}

.ai-text :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-text :deep(ol), .ai-text :deep(ul) {
  margin-bottom: 12px;
  padding-left: 24px;
}

.ai-text :deep(li) {
  margin-bottom: 4px;
}

.ai-text :deep(code) {
  background: #f1f5f9;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 0.9em;
  word-break: break-all;
}

.ai-text :deep(pre) {
  background: #1e293b;
  color: #f8fafc;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.tool-call-card {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.tool-header {
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tool-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #334155;
}

.tool-command {
  padding: 10px 12px;
  background: #1e293b;
  color: #38bdf8;
  font-family: 'Fira Code', monospace;
  font-size: 0.85rem;
  overflow-x: auto;
  word-break: break-all;
}

.tool-status {
  padding: 6px 12px;
  font-size: 0.8rem;
  font-weight: 500;
  border-top: 1px solid #e2e8f0;
  word-break: break-word;
}

.status-success { color: #10b981; }
.status-error { color: #ef4444; }
.status-pending { color: #3b82f6; }

.tool-result {
  padding: 0;
  border-top: 1px solid #e2e8f0;
}

.result-title {
  padding: 6px 12px;
  background: #f1f5f9;
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
}

.result-block {
  margin: 0;
  padding: 12px;
  background: #fff;
  font-family: 'Fira Code', monospace;
  font-size: 0.85rem;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  color: #334155;
  word-break: break-all;
}

.result-block.error {
  background: #fff1f2;
  color: #be123c;
}

/* System Messages and Task Panels */
.system-bubble {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  word-break: break-word;
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
/* 极简滚动条全局样式 - 仅对右侧面板生效 */
.right-panel ::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.right-panel ::-webkit-scrollbar-track {
  background: transparent;
}

.right-panel ::-webkit-scrollbar-thumb {
  background: rgba(100, 116, 139, 0.2);
  border-radius: 10px;
}

.right-panel ::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 116, 139, 0.4);
}

.panel-tabs { 
  display: flex; 
  background: #f8fafc; 
  border-bottom: 1px solid #e2e8f0; 
  flex-shrink: 0; 
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding: 0;
}

.panel-tabs::-webkit-scrollbar {
  display: none;
}

.tab { 
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px; 
  font-size: 0.85rem; 
  color: #64748b; 
  border-right: 1px solid #f1f5f9; 
  cursor: pointer; 
  flex-shrink: 0;
  transition: all 0.2s;
  user-select: none;
  min-width: 80px;
  position: relative;
}

.tab.active { 
  background: #fff; 
  color: #3b82f6; 
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #3b82f6;
}
.tab { 
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 24px; 
  font-size: 0.9rem; 
  color: #64748b; 
  border-right: 1px solid #e2e8f0; 
  cursor: pointer; 
  flex-shrink: 0;
  transition: all 0.2s;
  user-select: none;
  min-width: 100px;
}
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
  min-height: 0;
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

.panel-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  background: #fff;
  min-height: 0;
}

.panel-content-wrapper.identity-panel,
.panel-content-wrapper.state-panel {
  overflow-y: auto;
}

/* File Editor Styles are now handled in TerminalFileEditor.vue component */
.panel-content-wrapper.file-panel-container {
  padding: 0;
  overflow: hidden;
}

/* Identity Panel Styles */
.identity-panel {
  padding: 16px;
  overflow-y: auto;
  background: #fff;
}

.identity-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.identity-panel-header h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.identity-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.identity-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

.section-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.identity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 0.9rem;
}

.identity-item:last-child {
  margin-bottom: 0;
}

.identity-item .label {
  color: #64748b;
  font-weight: 500;
  min-width: 60px;
}

.identity-item .value {
  color: #1e293b;
  flex: 1;
}

.badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge.suggest_only {
  background: #fef3c7;
  color: #92400e;
}

.badge.execute {
  background: #dbeafe;
  color: #1e40af;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #94a3b8;
}

.empty-state p {
  margin: 8px 0;
}

.empty-state .hint {
  font-size: 0.85rem;
  color: #cbd5e1;
}

/* State Slices Panel Styles */
.state-panel {
  padding: 16px;
  overflow-y: auto;
  background: #fff;
}

.state-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.state-panel-header h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.slices-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.slice-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s;
}

.slice-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.slice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.slice-source {
  font-size: 0.85rem;
  font-weight: 600;
  color: #3b82f6;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.slice-timestamp {
  font-size: 0.75rem;
  color: #94a3b8;
}

.slice-scope,
.slice-data {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 0.85rem;
}

.scope-label,
.data-label {
  color: #64748b;
  font-weight: 500;
}

.scope-value,
.data-value {
  color: #1e293b;
}

.slice-authority {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
}

.authority-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.authority-badge.fact {
  background: #dcfce7;
  color: #166534;
}

.authority-badge.model_output {
  background: #fef3c7;
  color: #92400e;
}

/* Phase 2: 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.dialog-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #64748b;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.close-btn:hover {
  background: #f1f5f9;
}

.dialog-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

/* 检查点列表样式 */
.checkpoint-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkpoint-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.checkpoint-info {
  flex: 1;
}

.checkpoint-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.checkpoint-type {
  background: #3b82f6;
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.checkpoint-time {
  color: #64748b;
  font-size: 0.875rem;
}

.checkpoint-description {
  color: #1e293b;
  margin-bottom: 4px;
}

.checkpoint-files {
  color: #64748b;
  font-size: 0.875rem;
}

.checkpoint-actions {
  display: flex;
  gap: 8px;
}

/* 批准列表样式 */
.approval-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-item {
  background: #fef3c7;
  border: 1px solid #fbbf24;
  border-radius: 8px;
  padding: 16px;
}

.approval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tool-name {
  background: #f59e0b;
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
}

.approval-time {
  color: #64748b;
  font-size: 0.875rem;
}

.approval-params {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.approval-params pre {
  margin: 0;
  font-size: 0.875rem;
  color: #1e293b;
}

.approval-actions {
  display: flex;
  gap: 8px;
}

.approval-bulk-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

/* 按钮样式 */
.btn-primary, .btn-secondary, .btn-success, .btn-danger {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #3b82f6;
  color: #fff;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-secondary {
  background: #64748b;
  color: #fff;
}

.btn-secondary:hover {
  background: #475569;
}

.btn-success {
  background: #10b981;
  color: #fff;
}

.btn-success:hover {
  background: #059669;
}

.btn-danger {
  background: #ef4444;
  color: #fff;
}

.btn-danger:hover {
  background: #dc2626;
}

/* 控制按钮样式增强 */
.control-btn {
  position: relative;
}

.control-btn .badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: #fff;
  font-size: 0.75rem;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.checkpoint-btn, .approval-btn {
  margin-right: 8px;
}

</style>
