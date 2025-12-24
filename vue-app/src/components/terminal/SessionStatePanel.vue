<template>
  <div class="session-state-panel">
    <div class="panel-header">
      <h3>📊 会话状态</h3>
      <div class="header-actions">
        <button class="btn-refresh" @click="$emit('refresh')">
          🔄 刷新
        </button>
        <button class="btn-export" @click="exportState">
          📤 导出
        </button>
      </div>
    </div>

    <div class="session-content">
      <!-- Agent 状态卡片 -->
      <div class="state-card agent-card">
        <div class="card-header">
          <h4>🤖 Agent 状态</h4>
          <div class="status-indicator" :class="agentStatus.toLowerCase()">
            {{ getStatusLabel(agentStatus) }}
          </div>
        </div>

        <div class="card-body">
          <div class="stat-row">
            <span class="stat-label">循环ID:</span>
            <span class="stat-value">{{ sessionState?.loopId || 'N/A' }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">当前决策:</span>
            <span class="stat-value">{{ sessionState?.currentDecisionId || 'N/A' }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">决策历史:</span>
            <span class="stat-value">{{ decisionCount }} 条</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">运行时间:</span>
            <span class="stat-value">{{ formatDuration(sessionState?.startTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 任务进度卡片 -->
      <div class="state-card task-card">
        <div class="card-header">
          <h4>📋 任务进度</h4>
          <div class="progress-badge">
            {{ completedTasks }} / {{ totalTasks }}
          </div>
        </div>

        <div class="card-body">
          <div class="progress-bar-container">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: taskProgress + '%' }"
              />
            </div>
            <span class="progress-text">{{ taskProgress }}%</span>
          </div>

          <div v-if="tasks.length > 0" class="task-list">
            <div
              v-for="task in tasks"
              :key="task.id"
              class="task-item"
              :class="task.status"
            >
              <span class="task-icon">{{ getTaskIcon(task.status) }}</span>
              <span class="task-desc">{{ task.desc }}</span>
            </div>
          </div>
          <div v-else class="empty-hint">
            暂无任务
          </div>
        </div>
      </div>

      <!-- 流式状态卡片 -->
      <div class="state-card stream-card">
        <div class="card-header">
          <h4>📡 流式状态</h4>
          <div v-if="isStreaming" class="streaming-indicator">
            <span class="pulse-dot" />
            流式中
          </div>
        </div>

        <div class="card-body">
          <div class="stat-row">
            <span class="stat-label">流式类型:</span>
            <span class="stat-value">{{ streamType || 'N/A' }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">已接收:</span>
            <span class="stat-value">{{ streamBytesReceived }} 字节</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">缓冲区:</span>
            <span class="stat-value">{{ streamBufferSize }} 字节</span>
          </div>
        </div>
      </div>

      <!-- 资源统计卡片 -->
      <div class="state-card resource-card">
        <div class="card-header">
          <h4>💾 资源统计</h4>
        </div>

        <div class="card-body">
          <div class="stat-row">
            <span class="stat-label">消息数:</span>
            <span class="stat-value">{{ messageCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">工具调用:</span>
            <span class="stat-value">{{ toolCallCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">检查点:</span>
            <span class="stat-value">{{ checkpointCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">待批准:</span>
            <span class="stat-value">{{ pendingApprovalCount }}</span>
          </div>
        </div>
      </div>

      <!-- 决策历史时间线 -->
      <div class="state-card timeline-card">
        <div class="card-header">
          <h4>🕐 决策时间线</h4>
          <button class="btn-expand" @click="expandTimeline = !expandTimeline">
            {{ expandTimeline ? '收起' : '展开' }}
          </button>
        </div>

        <div v-if="expandTimeline" class="card-body">
          <div v-if="decisionHistory.length > 0" class="decision-timeline">
            <div
              v-for="(decision, index) in recentDecisions"
              :key="decision.decision_id"
              class="timeline-item"
            >
              <div class="timeline-marker">
                <div class="marker-dot" :class="getDecisionTypeClass(decision.type)" />
                <div v-if="index < recentDecisions.length - 1" class="marker-line" />
              </div>
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="decision-type">{{ getDecisionTypeLabel(decision.type) }}</span>
                  <span class="decision-time">{{ formatTime(decision.timestamp) }}</span>
                </div>
                <div class="decision-action">
                  {{ decision.action || decision.type }}
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-hint">
            暂无决策历史
          </div>
        </div>
      </div>

      <!-- 性能指标卡片 -->
      <div class="state-card performance-card">
        <div class="card-header">
          <h4>⚡ 性能指标</h4>
        </div>

        <div class="card-body">
          <div class="metric-row">
            <span class="metric-label">平均响应时间:</span>
            <span class="metric-value">{{ avgResponseTime }} ms</span>
          </div>
          <div class="metric-row">
            <span class="metric-label">工具执行时间:</span>
            <span class="metric-value">{{ avgToolExecutionTime }} ms</span>
          </div>
          <div class="metric-row">
            <span class="metric-label">LLM 调用次数:</span>
            <span class="metric-value">{{ llmCallCount }}</span>
          </div>
          <div class="metric-row">
            <span class="metric-label">Token 使用:</span>
            <span class="metric-value">{{ totalTokens }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  sessionState: {
    type: Object,
    default: () => ({})
  },
  agentStatus: {
    type: String,
    default: 'IDLE'
  },
  tasks: {
    type: Array,
    default: () => []
  },
  decisionHistory: {
    type: Array,
    default: () => []
  },
  isStreaming: {
    type: Boolean,
    default: false
  },
  streamType: {
    type: String,
    default: ''
  },
  streamBytesReceived: {
    type: Number,
    default: 0
  },
  streamBufferSize: {
    type: Number,
    default: 0
  },
  messageCount: {
    type: Number,
    default: 0
  },
  toolCallCount: {
    type: Number,
    default: 0
  },
  checkpointCount: {
    type: Number,
    default: 0
  },
  pendingApprovalCount: {
    type: Number,
    default: 0
  },
  avgResponseTime: {
    type: Number,
    default: 0
  },
  avgToolExecutionTime: {
    type: Number,
    default: 0
  },
  llmCallCount: {
    type: Number,
    default: 0
  },
  totalTokens: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['refresh', 'export'])

const expandTimeline = ref(false)

/**
 * 决策数量
 */
const decisionCount = computed(() => props.decisionHistory.length)

/**
 * 完成的任务数
 */
const completedTasks = computed(() => {
  return props.tasks.filter(t => t.status === 'completed').length
})

/**
 * 总任务数
 */
const totalTasks = computed(() => props.tasks.length)

/**
 * 任务进度百分比
 */
const taskProgress = computed(() => {
  if (totalTasks.value === 0) return 0
  return Math.round((completedTasks.value / totalTasks.value) * 100)
})

/**
 * 最近的决策（最多显示 10 条）
 */
const recentDecisions = computed(() => {
  return props.decisionHistory.slice(-10).reverse()
})

/**
 * 获取状态标签
 */
function getStatusLabel(status) {
  const labels = {
    'IDLE': '空闲',
    'PLANNING': '规划中',
    'RUNNING': '运行中',
    'WAITING_TOOL': '等待工具',
    'WAITING_APPROVAL': '等待批准',
    'PAUSED': '已暂停',
    'ERROR': '错误',
    'COMPLETED': '已完成'
  }
  return labels[status] || status
}

/**
 * 获取任务图标
 */
function getTaskIcon(status) {
  const icons = {
    'pending': '⏳',
    'in_progress': '🔄',
    'completed': '✅',
    'failed': '❌'
  }
  return icons[status] || '❓'
}

/**
 * 获取决策类型标签
 */
function getDecisionTypeLabel(type) {
  const labels = {
    'TASK_LIST': '任务列表',
    'TOOL_CALL': '工具调用',
    'TASK_COMPLETE': '任务完成',
    'PAUSE': '暂停',
    'ERROR': '错误'
  }
  return labels[type] || type
}

/**
 * 获取决策类型样式类
 */
function getDecisionTypeClass(type) {
  const classes = {
    'TASK_LIST': 'type-task-list',
    'TOOL_CALL': 'type-tool-call',
    'TASK_COMPLETE': 'type-complete',
    'PAUSE': 'type-pause',
    'ERROR': 'type-error'
  }
  return classes[type] || 'type-default'
}

/**
 * 格式化时间
 */
function formatTime(timestamp) {
  if (!timestamp) return 'N/A'
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/**
 * 格式化持续时间
 */
function formatDuration(startTime) {
  if (!startTime) return 'N/A'
  const start = new Date(startTime)
  const now = new Date()
  const diff = now - start
  
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}

/**
 * 导出状态
 */
function exportState() {
  const data = {
    sessionState: props.sessionState,
    agentStatus: props.agentStatus,
    tasks: props.tasks,
    decisionHistory: props.decisionHistory,
    statistics: {
      messageCount: props.messageCount,
      toolCallCount: props.toolCallCount,
      checkpointCount: props.checkpointCount,
      pendingApprovalCount: props.pendingApprovalCount,
      avgResponseTime: props.avgResponseTime,
      avgToolExecutionTime: props.avgToolExecutionTime,
      llmCallCount: props.llmCallCount,
      totalTokens: props.totalTokens
    },
    exportTime: new Date().toISOString()
  }
  
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `session-state-${Date.now()}.json`
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
  
  emit('export', data)
}
</script>

<style scoped>
.session-state-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.panel-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-refresh, .btn-export {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh {
  background: #f1f5f9;
  color: #64748b;
}

.btn-refresh:hover {
  background: #e2e8f0;
}

.btn-export {
  background: #3b82f6;
  color: #fff;
}

.btn-export:hover {
  background: #2563eb;
}

.session-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.state-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.state-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.card-header h4 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
}

.status-indicator {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-indicator.idle {
  background: #f1f5f9;
  color: #64748b;
}

.status-indicator.planning {
  background: #fef3c7;
  color: #92400e;
}

.status-indicator.running {
  background: #dcfce7;
  color: #166534;
}

.status-indicator.waiting_tool,
.status-indicator.waiting_approval {
  background: #fef3c7;
  color: #92400e;
}

.status-indicator.paused {
  background: #e0e7ff;
  color: #3730a3;
}

.status-indicator.error {
  background: #fee2e2;
  color: #991b1b;
}

.status-indicator.completed {
  background: #dcfce7;
  color: #166534;
}

.progress-badge {
  background: #3b82f6;
  color: #fff;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #10b981;
  font-size: 0.85rem;
  font-weight: 500;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

.card-body {
  padding: 16px;
}

.stat-row, .metric-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #e2e8f0;
}

.stat-row:last-child, .metric-row:last-child {
  border-bottom: none;
}

.stat-label, .metric-label {
  font-size: 0.85rem;
  color: #64748b;
}

.stat-value, .metric-value {
  font-size: 0.9rem;
  font-weight: 500;
  color: #1e293b;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #10b981);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.85rem;
  font-weight: 600;
  color: #3b82f6;
  min-width: 40px;
  text-align: right;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.85rem;
}

.task-item.completed {
  background: #dcfce7;
  border-color: #bbf7d0;
}

.task-item.in_progress {
  background: #fef3c7;
  border-color: #fde68a;
}

.task-icon {
  font-size: 1rem;
}

.task-desc {
  flex: 1;
  color: #1e293b;
}

.empty-hint {
  text-align: center;
  padding: 20px;
  color: #94a3b8;
  font-size: 0.85rem;
}

.btn-expand {
  padding: 4px 10px;
  border: none;
  border-radius: 6px;
  font-size: 0.8rem;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-expand:hover {
  background: #e2e8f0;
}

.decision-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 4px;
}

.marker-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #3b82f6;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #3b82f6;
}

.marker-dot.type-task-list {
  background: #10b981;
  box-shadow: 0 0 0 2px #10b981;
}

.marker-dot.type-tool-call {
  background: #f59e0b;
  box-shadow: 0 0 0 2px #f59e0b;
}

.marker-dot.type-complete {
  background: #10b981;
  box-shadow: 0 0 0 2px #10b981;
}

.marker-dot.type-error {
  background: #ef4444;
  box-shadow: 0 0 0 2px #ef4444;
}

.marker-line {
  width: 2px;
  flex: 1;
  background: #e2e8f0;
  margin-top: 4px;
  min-height: 30px;
}

.timeline-content {
  flex: 1;
  padding-bottom: 8px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.decision-type {
  font-size: 0.85rem;
  font-weight: 600;
  color: #3b82f6;
}

.decision-time {
  font-size: 0.75rem;
  color: #94a3b8;
}

.decision-action {
  font-size: 0.8rem;
  color: #64748b;
}
</style>

