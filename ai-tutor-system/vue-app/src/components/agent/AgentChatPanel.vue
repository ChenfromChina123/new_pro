<template>
  <div class="agent-chat-panel">
    <!-- 消息列表 -->
    <div
      ref="messagesRef"
      class="messages-container"
    >
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message', message.type]"
      >
        <div class="message-header">
          <span class="message-type">{{ getMessageLabel(message.type) }}</span>
          <span class="message-time">{{ formatTime(message.timestamp) }}</span>
        </div>
        <div class="message-content">
          <template v-if="message.type === 'tool'">
            <ToolCallDisplay :tool-call="message.content" />
          </template>
          <template v-else-if="message.type === 'progress'">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: getProgressWidth(message.content) }"
              />
            </div>
            <span class="progress-text">{{ message.content }}</span>
          </template>
          <template v-else>
            <pre>{{ message.content }}</pre>
          </template>
        </div>
      </div>
      
      <!-- 工具调用列表 -->
      <div
        v-if="toolCalls.length > 0"
        class="tool-calls-section"
      >
        <h4>工具调用记录</h4>
        <div
          v-for="call in toolCalls"
          :key="call.id"
          class="tool-call-item"
        >
          <div class="tool-header">
            <span class="tool-name">
              <i class="fas fa-wrench" />
              {{ call.toolName }}
            </span>
            <span :class="['tool-status', call.status]">{{ call.status }}</span>
          </div>
          <div
            v-if="call.thought"
            class="tool-thought"
          >
            <strong>思考:</strong> {{ call.thought }}
          </div>
          <div
            v-if="call.observation"
            class="tool-observation"
          >
            <strong>观察:</strong> {{ call.observation }}
          </div>
          <div
            v-if="call.durationMs"
            class="tool-duration"
          >
            耗时: {{ call.durationMs }}ms
          </div>
        </div>
      </div>
    </div>

    <!-- 任务状态栏 -->
    <div class="task-status-bar">
      <div class="status-left">
        <span :class="['status-badge', task.status]">
          {{ getStatusLabel(task.status) }}
        </span>
        <span
          v-if="task.currentStep && task.totalSteps"
          class="step-info"
        >
          步骤: {{ task.currentStep }}/{{ task.totalSteps }}
        </span>
      </div>
      <div class="status-right">
        <button
          v-if="isStreaming"
          class="btn btn-danger btn-sm"
          @click="$emit('cancel')"
        >
          <i class="fas fa-stop" />
          取消
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, defineAsyncComponent } from 'vue'

const props = defineProps({
  task: {
    type: Object,
    required: true
  },
  messages: {
    type: Array,
    default: () => []
  },
  toolCalls: {
    type: Array,
    default: () => []
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

defineEmits(['cancel'])

const messagesRef = ref(null)

const ToolCallDisplay = defineAsyncComponent(() =>
  import('./ToolCallDisplay.vue')
)

watch(() => props.messages, () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}, { deep: true })

function getMessageLabel(type) {
  const labels = {
    system: '系统',
    user: '用户',
    tool: '工具',
    error: '错误',
    progress: '进度',
    output: '输出'
  }
  return labels[type] || type
}

function getStatusLabel(status) {
  const labels = {
    pending: '等待中',
    running: '执行中',
    completed: '已完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return labels[status] || status
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function getProgressWidth(content) {
  const match = content?.match(/(\d+)\/(\d+)/)
  if (match) {
    const current = parseInt(match[1])
    const total = parseInt(match[2])
    return total > 0 ? `${(current / total) * 100}%` : '0%'
  }
  return '0%'
}
</script>

<style scoped>
.agent-chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: 8px;
  border-left: 3px solid var(--border-color);
}

.message.system {
  border-left-color: var(--info-color);
}

.message.tool {
  border-left-color: var(--primary-color);
}

.message.error {
  border-left-color: var(--danger-color);
  background: rgba(239, 68, 68, 0.1);
}

.message.progress {
  border-left-color: var(--warning-color);
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.message-type {
  font-weight: 600;
  text-transform: uppercase;
}

.message-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

.progress-bar {
  height: 4px;
  background: var(--bg-tertiary);
  border-radius: 2px;
  margin-bottom: 8px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--primary-color);
  transition: width 0.3s;
}

.progress-text {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.tool-calls-section {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.tool-calls-section h4 {
  font-size: 0.875rem;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-secondary);
}

.tool-call-item {
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: 6px;
  margin-bottom: 8px;
}

.tool-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.tool-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.tool-name i {
  color: var(--primary-color);
}

.tool-status {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 12px;
  text-transform: uppercase;
}

.tool-status.success {
  background: rgba(16, 185, 129, 0.2);
  color: var(--success-color);
}

.tool-status.failed {
  background: rgba(239, 68, 68, 0.2);
  color: var(--danger-color);
}

.tool-status.running {
  background: rgba(245, 158, 11, 0.2);
  color: var(--warning-color);
}

.tool-thought,
.tool-observation {
  font-size: 0.875rem;
  margin-bottom: 4px;
  color: var(--text-secondary);
}

.tool-duration {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.task-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
}

.status-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.pending {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.status-badge.running {
  background: rgba(245, 158, 11, 0.2);
  color: var(--warning-color);
}

.status-badge.completed {
  background: rgba(16, 185, 129, 0.2);
  color: var(--success-color);
}

.status-badge.failed {
  background: rgba(239, 68, 68, 0.2);
  color: var(--danger-color);
}

.status-badge.cancelled {
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
}

.step-info {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.btn-danger {
  background: var(--danger-color);
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}
</style>
