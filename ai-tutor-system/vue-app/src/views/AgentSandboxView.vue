<template>
  <div class="agent-sandbox-view">
    <!-- 顶部工具栏 -->
    <header class="sandbox-header">
      <div class="header-left">
        <h1 class="title">
          <i class="fas fa-robot" />
          AI Agent 沙箱
        </h1>
        <div
          v-if="agentStore.currentSession"
          class="session-info"
        >
          <span class="session-name">{{ agentStore.currentSession.name }}</span>
          <span class="session-status">{{ agentStore.currentSession.status }}</span>
        </div>
      </div>
      <div class="header-right">
        <button
          class="btn btn-secondary"
          @click="showSessionModal = true"
        >
          <i class="fas fa-folder-plus" />
          新建会话
        </button>
        <button
          class="btn btn-primary"
          :disabled="!agentStore.currentSession"
          @click="showTaskModal = true"
        >
          <i class="fas fa-plus" />
          新建任务
        </button>
      </div>
    </header>

    <!-- 主内容区 -->
    <div class="sandbox-main">
      <!-- 左侧边栏 - 会话列表 -->
      <aside class="sidebar sessions-sidebar">
        <div class="sidebar-header">
          <h3>会话列表</h3>
          <button
            class="icon-btn"
            @click="agentStore.fetchSessions"
          >
            <i class="fas fa-sync-alt" />
          </button>
        </div>
        <div class="sidebar-content">
          <div
            v-for="session in agentStore.sessions"
            :key="session.id"
            :class="['session-item', { active: agentStore.currentSession?.id === session.id }]"
            @click="selectSession(session.id)"
          >
            <div class="session-icon">
              <i :class="session.status === 'active' ? 'fas fa-circle' : 'fas fa-circle'" />
            </div>
            <div class="session-details">
              <span class="session-name">{{ session.name }}</span>
              <span class="session-meta">{{ formatDate(session.createdAt) }}</span>
            </div>
            <div class="session-actions">
              <button
                class="icon-btn"
                title="关闭"
                @click.stop="closeSession(session.id)"
              >
                <i class="fas fa-times" />
              </button>
            </div>
          </div>
          <div
            v-if="agentStore.sessions.length === 0"
            class="empty-state"
          >
            <p>暂无会话</p>
            <button
              class="btn btn-sm"
              @click="showSessionModal = true"
            >
              创建第一个会话
            </button>
          </div>
        </div>
      </aside>

      <!-- 中间主区域 -->
      <main class="main-content">
        <!-- 任务列表 -->
        <div
          v-if="agentStore.currentSession"
          class="tasks-panel"
        >
          <div class="panel-header">
            <h3>任务列表</h3>
            <span class="task-count">{{ agentStore.tasks.length }} 个任务</span>
          </div>
          <div class="tasks-list">
            <div
              v-for="task in agentStore.tasks"
              :key="task.id"
              :class="['task-item', task.status, { active: agentStore.currentTask?.id === task.id }]"
              @click="selectTask(task)"
            >
              <div class="task-status">
                <i :class="getTaskStatusIcon(task.status)" />
              </div>
              <div class="task-info">
                <span class="task-type">{{ task.taskType }}</span>
                <span class="task-input">{{ truncate(task.input, 50) }}</span>
              </div>
              <div class="task-meta">
                <span class="task-time">{{ formatDate(task.createdAt) }}</span>
                <span
                  v-if="task.executionTimeMs"
                  class="task-duration"
                >{{ formatDuration(task.executionTimeMs) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 对话面板 -->
        <AgentChatPanel
          v-if="agentStore.currentTask"
          :task="agentStore.currentTask"
          :messages="agentStore.messages"
          :tool-calls="agentStore.currentTaskToolCalls"
          :is-streaming="agentStore.isStreaming"
          @cancel="cancelTask"
        />

        <!-- 空状态 -->
        <div
          v-if="!agentStore.currentSession"
          class="empty-main"
        >
          <div class="empty-icon">
            <i class="fas fa-robot" />
          </div>
          <h2>欢迎使用 AI Agent 沙箱</h2>
          <p>创建一个会话开始使用</p>
          <button
            class="btn btn-primary btn-lg"
            @click="showSessionModal = true"
          >
            <i class="fas fa-plus" />
            创建会话
          </button>
        </div>
      </main>

      <!-- 右侧边栏 - 文件树和终端 -->
      <aside class="sidebar tools-sidebar">
        <!-- 文件树 -->
        <div class="sidebar-section">
          <div class="sidebar-header">
            <h3>文件树</h3>
            <button
              class="icon-btn"
              @click="refreshFileTree"
            >
              <i class="fas fa-sync-alt" />
            </button>
          </div>
          <AgentFileTree
            v-if="agentStore.currentSession"
            :files="agentStore.fileTree"
            @select="handleFileSelect"
          />
        </div>

        <!-- 终端输出 -->
        <div class="sidebar-section terminal-section">
          <div class="sidebar-header">
            <h3>终端输出</h3>
            <button
              class="icon-btn"
              @click="agentStore.clearTerminalOutput"
            >
              <i class="fas fa-trash" />
            </button>
          </div>
          <AgentTerminal
            :output="agentStore.terminalOutput"
            :is-streaming="agentStore.isStreaming"
          />
        </div>
      </aside>
    </div>

    <!-- 新建会话弹窗 -->
    <div
      v-if="showSessionModal"
      class="modal-overlay"
      @click.self="showSessionModal = false"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h2>新建会话</h2>
          <button
            class="close-btn"
            @click="showSessionModal = false"
          >
            <i class="fas fa-times" />
          </button>
        </div>
        <form @submit.prevent="createSession">
          <div class="form-group">
            <label>会话名称</label>
            <input
              v-model="newSession.name"
              type="text"
              placeholder="输入会话名称"
            >
          </div>
          <div class="form-group">
            <label>工作目录</label>
            <input
              v-model="newSession.workingDirectory"
              type="text"
              placeholder="输入工作目录路径（可选）"
            >
          </div>
          <div class="modal-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="showSessionModal = false"
            >
              取消
            </button>
            <button
              type="submit"
              class="btn btn-primary"
              :disabled="agentStore.isLoading"
            >
              创建
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 新建任务弹窗 -->
    <div
      v-if="showTaskModal"
      class="modal-overlay"
      @click.self="showTaskModal = false"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h2>新建任务</h2>
          <button
            class="close-btn"
            @click="showTaskModal = false"
          >
            <i class="fas fa-times" />
          </button>
        </div>
        <form @submit.prevent="createTask">
          <div class="form-group">
            <label>任务类型</label>
            <select v-model="newTask.taskType">
              <option value="general">
                通用任务
              </option>
              <option value="code_edit">
                代码编辑
              </option>
              <option value="file_search">
                文件搜索
              </option>
              <option value="terminal">
                终端命令
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>任务描述</label>
            <textarea
              v-model="newTask.input"
              placeholder="描述你想要 Agent 执行的任务..."
              rows="4"
            />
          </div>
          <div class="modal-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="showTaskModal = false"
            >
              取消
            </button>
            <button
              type="submit"
              class="btn btn-primary"
              :disabled="agentStore.isLoading || !newTask.input"
            >
              创建并执行
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useAgentStore } from '@/stores/agent'
import AgentChatPanel from '@/components/agent/AgentChatPanel.vue'
import AgentFileTree from '@/components/agent/AgentFileTree.vue'
import AgentTerminal from '@/components/agent/AgentTerminal.vue'

const agentStore = useAgentStore()

const showSessionModal = ref(false)
const showTaskModal = ref(false)

const newSession = reactive({
  name: '',
  workingDirectory: ''
})

const newTask = reactive({
  taskType: 'general',
  input: ''
})

onMounted(async () => {
  try {
    await agentStore.fetchSessions()
    if (agentStore.sessions.length > 0) {
      await agentStore.selectSession(agentStore.sessions[0].id)
    }
  } catch (error) {
    console.error('初始化失败:', error)
  }
})

async function createSession() {
  try {
    await agentStore.createSession(newSession.name || null, newSession.workingDirectory || null)
    showSessionModal.value = false
    newSession.name = ''
    newSession.workingDirectory = ''
  } catch (error) {
    console.error('创建会话失败:', error)
  }
}

async function selectSession(sessionId) {
  try {
    await agentStore.selectSession(sessionId)
  } catch (error) {
    console.error('选择会话失败:', error)
  }
}

async function closeSession(sessionId) {
  if (confirm('确定要关闭此会话吗？')) {
    try {
      await agentStore.closeSession(sessionId)
    } catch (error) {
      console.error('关闭会话失败:', error)
    }
  }
}

async function createTask() {
  try {
    const task = await agentStore.createTask(newTask.input, newTask.taskType)
    showTaskModal.value = false
    newTask.input = ''
    newTask.taskType = 'general'
    
    await agentStore.startTaskStream(task.id)
  } catch (error) {
    console.error('创建任务失败:', error)
  }
}

function selectTask(task) {
  agentStore.currentTask = task
}

async function cancelTask() {
  if (agentStore.currentTask) {
    try {
      await agentStore.cancelTask(agentStore.currentTask.id)
    } catch (error) {
      console.error('取消任务失败:', error)
    }
  }
}

async function refreshFileTree() {
  if (agentStore.currentSession) {
    try {
      await agentStore.fetchFileTree(agentStore.currentSession.id)
    } catch (error) {
      console.error('刷新文件树失败:', error)
    }
  }
}

async function handleFileSelect(file) {
  try {
    const content = await agentStore.readFile(
      agentStore.currentSession.id,
      file.path,
      1,
      100
    )
    console.log('文件内容:', content)
  } catch (error) {
    console.error('读取文件失败:', error)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDuration(ms) {
  if (!ms) return ''
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}m`
}

function truncate(str, length) {
  if (!str) return ''
  return str.length > length ? str.substring(0, length) + '...' : str
}

function getTaskStatusIcon(status) {
  const icons = {
    pending: 'fas fa-clock',
    running: 'fas fa-spinner fa-spin',
    completed: 'fas fa-check-circle',
    failed: 'fas fa-times-circle',
    cancelled: 'fas fa-ban'
  }
  return icons[status] || 'fas fa-question-circle'
}
</script>

<style scoped>
.agent-sandbox-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.sandbox-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title i {
  color: var(--primary-color);
}

.session-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: var(--bg-tertiary);
  border-radius: 16px;
  font-size: 0.875rem;
}

.session-status {
  color: var(--success-color);
  font-weight: 500;
}

.header-right {
  display: flex;
  gap: 8px;
}

.sandbox-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 280px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-header h3 {
  font-size: 0.875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-secondary);
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.session-item:hover {
  background: var(--bg-tertiary);
}

.session-item.active {
  background: var(--primary-color-transparent);
}

.session-icon {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.session-item.active .session-icon i {
  color: var(--success-color);
}

.session-details {
  flex: 1;
  min-width: 0;
}

.session-name {
  display: block;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: block;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.session-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tasks-panel {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.panel-header h3 {
  font-size: 1rem;
  font-weight: 600;
}

.task-count {
  font-size: 0.875rem;
  color: var(--text-tertiary);
}

.tasks-list {
  display: flex;
  gap: 8px;
  padding: 0 16px 12px;
  overflow-x: auto;
}

.task-item {
  flex-shrink: 0;
  width: 200px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.task-item:hover {
  background: var(--bg-primary);
}

.task-item.active {
  border-color: var(--primary-color);
}

.task-item.running {
  border-color: var(--warning-color);
}

.task-item.completed {
  border-color: var(--success-color);
}

.task-item.failed {
  border-color: var(--danger-color);
}

.task-status {
  margin-bottom: 8px;
}

.task-status i {
  font-size: 1rem;
}

.task-item.running .task-status i {
  color: var(--warning-color);
}

.task-item.completed .task-status i {
  color: var(--success-color);
}

.task-item.failed .task-status i {
  color: var(--danger-color);
}

.task-info {
  margin-bottom: 8px;
}

.task-type {
  display: block;
  font-size: 0.75rem;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.task-input {
  display: block;
  font-size: 0.875rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.empty-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.empty-icon {
  font-size: 4rem;
  color: var(--primary-color);
  opacity: 0.5;
}

.empty-main h2 {
  font-size: 1.5rem;
}

.empty-main p {
  color: var(--text-secondary);
}

.tools-sidebar {
  display: flex;
  flex-direction: column;
}

.sidebar-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-section:last-child {
  border-bottom: none;
}

.terminal-section {
  flex: 0 0 250px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  text-align: center;
  color: var(--text-tertiary);
}

.icon-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary-color);
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-color-dark);
}

.btn-secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background: var(--bg-primary);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-sm {
  padding: 4px 12px;
  font-size: 0.875rem;
}

.btn-lg {
  padding: 12px 24px;
  font-size: 1rem;
}

.modal-overlay {
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

.modal-content {
  background: var(--bg-secondary);
  border-radius: 12px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h2 {
  font-size: 1.125rem;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
}

.close-btn:hover {
  color: var(--text-primary);
}

form {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 0.875rem;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--primary-color);
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
}
</style>
