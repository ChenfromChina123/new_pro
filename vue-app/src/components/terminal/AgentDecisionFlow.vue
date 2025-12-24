<template>
  <div class="agent-decision-flow">
    <div class="flow-header">
      <h3>🧠 Agent 决策流程</h3>
      <div class="header-controls">
        <button class="btn-auto-scroll" :class="{ active: autoScroll }" @click="autoScroll = !autoScroll">
          {{ autoScroll ? '📍 自动滚动' : '📌 固定' }}
        </button>
        <button class="btn-clear" @click="$emit('clear')">
          🗑️ 清空
        </button>
      </div>
    </div>

    <div ref="flowContainer" class="flow-container">
      <div v-if="decisions.length === 0" class="empty-state">
        <div class="empty-icon">🤔</div>
        <p>暂无决策记录</p>
        <p class="hint">Agent 的决策过程将在这里可视化展示</p>
      </div>

      <div v-else class="decision-flow">
        <div
          v-for="(decision, index) in decisions"
          :key="decision.decision_id"
          class="decision-node"
          :class="[
            `type-${decision.type.toLowerCase()}`,
            { 'is-current': index === decisions.length - 1 }
          ]"
        >
          <!-- 连接线 -->
          <div v-if="index > 0" class="connection-line">
            <div class="line-arrow">↓</div>
          </div>

          <!-- 决策卡片 -->
          <div class="decision-card">
            <div class="card-header">
              <div class="header-left">
                <div class="decision-icon">
                  {{ getDecisionIcon(decision.type) }}
                </div>
                <div class="decision-info">
                  <div class="decision-type">
                    {{ getDecisionTypeLabel(decision.type) }}
                  </div>
                  <div class="decision-id">
                    ID: {{ decision.decision_id?.substring(0, 8) }}...
                  </div>
                </div>
              </div>
              <div class="header-right">
                <div class="decision-time">
                  {{ formatTime(decision.timestamp) }}
                </div>
                <button
                  class="btn-expand"
                  @click="toggleExpand(decision.decision_id)"
                >
                  {{ isExpanded(decision.decision_id) ? '▼' : '▶' }}
                </button>
              </div>
            </div>

            <!-- 决策详情（可展开） -->
            <div v-if="isExpanded(decision.decision_id)" class="card-body">
              <!-- 任务列表类型 -->
              <div v-if="decision.type === 'TASK_LIST'" class="decision-content">
                <div class="content-section">
                  <h5>📋 任务列表 ({{ decision.tasks?.length || 0 }})</h5>
                  <div class="task-list">
                    <div
                      v-for="(task, taskIndex) in decision.tasks"
                      :key="taskIndex"
                      class="task-item"
                    >
                      <span class="task-number">{{ taskIndex + 1 }}</span>
                      <span class="task-desc">{{ task.desc }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 工具调用类型 -->
              <div v-if="decision.type === 'TOOL_CALL'" class="decision-content">
                <div class="content-section">
                  <h5>🛠️ 工具调用</h5>
                  <div class="tool-info">
                    <div class="info-row">
                      <span class="info-label">工具名称:</span>
                      <span class="info-value">{{ decision.action }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">参数:</span>
                      <div class="params-box">
                        <pre>{{ formatParams(decision.params) }}</pre>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 工具执行结果 -->
                <div v-if="decision.result" class="content-section result-section">
                  <h5>📊 执行结果</h5>
                  <div class="result-info">
                    <div class="info-row">
                      <span class="info-label">退出码:</span>
                      <span class="info-value" :class="{ 'text-success': decision.result.exit_code === 0, 'text-error': decision.result.exit_code !== 0 }">
                        {{ decision.result.exit_code }}
                      </span>
                    </div>
                    <div v-if="decision.result.stdout" class="info-row">
                      <span class="info-label">输出:</span>
                      <div class="output-box stdout">
                        <pre>{{ decision.result.stdout }}</pre>
                      </div>
                    </div>
                    <div v-if="decision.result.stderr" class="info-row">
                      <span class="info-label">错误:</span>
                      <div class="output-box stderr">
                        <pre>{{ decision.result.stderr }}</pre>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 任务完成类型 -->
              <div v-if="decision.type === 'TASK_COMPLETE'" class="decision-content">
                <div class="content-section">
                  <h5>✅ 任务完成</h5>
                  <div class="completion-info">
                    <p>{{ decision.message || '当前任务已成功完成' }}</p>
                  </div>
                </div>
              </div>

              <!-- 暂停类型 -->
              <div v-if="decision.type === 'PAUSE'" class="decision-content">
                <div class="content-section">
                  <h5>⏸️ 暂停</h5>
                  <div class="pause-info">
                    <p>{{ decision.reason || '用户请求暂停' }}</p>
                  </div>
                </div>
              </div>

              <!-- 错误类型 -->
              <div v-if="decision.type === 'ERROR'" class="decision-content">
                <div class="content-section error-section">
                  <h5>❌ 错误</h5>
                  <div class="error-info">
                    <p>{{ decision.message || '发生未知错误' }}</p>
                  </div>
                </div>
              </div>

              <!-- 身份信息（如果有） -->
              <div v-if="decision.identity" class="content-section identity-section">
                <h5>🎭 身份信息</h5>
                <div class="identity-info">
                  <div v-if="decision.identity.core" class="identity-item">
                    <span class="identity-label">核心身份:</span>
                    <span class="identity-value">
                      {{ decision.identity.core.type }} ({{ decision.identity.core.authority }})
                    </span>
                  </div>
                  <div v-if="decision.identity.task" class="identity-item">
                    <span class="identity-label">任务身份:</span>
                    <span class="identity-value">{{ decision.identity.task.goal }}</span>
                  </div>
                </div>
              </div>

              <!-- 状态切片（如果有） -->
              <div v-if="decision.state_slices && decision.state_slices.length > 0" class="content-section slices-section">
                <h5>📦 状态切片 ({{ decision.state_slices.length }})</h5>
                <div class="slices-list">
                  <div
                    v-for="(slice, sliceIndex) in decision.state_slices"
                    :key="sliceIndex"
                    class="slice-item"
                  >
                    <span class="slice-source">{{ slice.source }}</span>
                    <span class="slice-authority">{{ slice.authority === 'fact' ? '事实' : '模型输出' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  decisions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['clear'])

const flowContainer = ref(null)
const autoScroll = ref(true)
const expandedDecisions = ref(new Set())

/**
 * 监听决策变化，自动滚动到底部
 */
watch(() => props.decisions.length, async () => {
  if (autoScroll.value) {
    await nextTick()
    scrollToBottom()
  }
})

/**
 * 滚动到底部
 */
function scrollToBottom() {
  if (flowContainer.value) {
    flowContainer.value.scrollTop = flowContainer.value.scrollHeight
  }
}

/**
 * 切换展开状态
 */
function toggleExpand(decisionId) {
  if (expandedDecisions.value.has(decisionId)) {
    expandedDecisions.value.delete(decisionId)
  } else {
    expandedDecisions.value.add(decisionId)
  }
}

/**
 * 检查是否展开
 */
function isExpanded(decisionId) {
  return expandedDecisions.value.has(decisionId)
}

/**
 * 获取决策图标
 */
function getDecisionIcon(type) {
  const icons = {
    'TASK_LIST': '📋',
    'TOOL_CALL': '🛠️',
    'TASK_COMPLETE': '✅',
    'PAUSE': '⏸️',
    'ERROR': '❌'
  }
  return icons[type] || '❓'
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
 * 格式化参数
 */
function formatParams(params) {
  if (!params) return '{}'
  
  // 简化显示长字符串
  const simplified = {}
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === 'string' && value.length > 200) {
      simplified[key] = value.substring(0, 200) + '...'
    } else {
      simplified[key] = value
    }
  }
  
  return JSON.stringify(simplified, null, 2)
}
</script>

<style scoped>
.agent-decision-flow {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.flow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.flow-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.header-controls {
  display: flex;
  gap: 8px;
}

.btn-auto-scroll, .btn-clear {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-auto-scroll {
  background: #f1f5f9;
  color: #64748b;
}

.btn-auto-scroll.active {
  background: #3b82f6;
  color: #fff;
}

.btn-auto-scroll:hover {
  background: #e2e8f0;
}

.btn-auto-scroll.active:hover {
  background: #2563eb;
}

.btn-clear {
  background: #fee2e2;
  color: #991b1b;
}

.btn-clear:hover {
  background: #fecaca;
}

.flow-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scrollbar-width: thin;
  scrollbar-color: #3b82f6 #f1f5f9;
}

.flow-container::-webkit-scrollbar {
  width: 6px;
}

.flow-container::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.flow-container::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.flow-container::-webkit-scrollbar-thumb:hover {
  background: #3b82f6;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 8px 0;
}

.hint {
  font-size: 0.85rem;
  color: #cbd5e1;
}

.decision-flow {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.decision-node {
  position: relative;
  margin-bottom: 16px;
}

.connection-line {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.line-arrow {
  font-size: 1.5rem;
  color: #cbd5e1;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(4px);
  }
}

.decision-card {
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.decision-node.is-current .decision-card {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.decision-node.type-tool_call .decision-card {
  border-left: 4px solid #f59e0b;
}

.decision-node.type-task_list .decision-card {
  border-left: 4px solid #10b981;
}

.decision-node.type-task_complete .decision-card {
  border-left: 4px solid #10b981;
}

.decision-node.type-error .decision-card {
  border-left: 4px solid #ef4444;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.decision-icon {
  font-size: 2rem;
}

.decision-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.decision-type {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.decision-id {
  font-size: 0.75rem;
  color: #94a3b8;
  font-family: monospace;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.decision-time {
  font-size: 0.85rem;
  color: #64748b;
}

.btn-expand {
  background: none;
  border: none;
  font-size: 1rem;
  color: #64748b;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-expand:hover {
  background: #f1f5f9;
}

.card-body {
  padding: 16px;
}

.decision-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-section {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
}

.content-section h5 {
  margin: 0 0 12px 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e293b;
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
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.task-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #3b82f6;
  color: #fff;
  border-radius: 50%;
  font-size: 0.8rem;
  font-weight: 600;
}

.task-desc {
  flex: 1;
  font-size: 0.85rem;
  color: #1e293b;
}

.tool-info, .result-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
}

.info-value {
  font-size: 0.85rem;
  color: #1e293b;
}

.text-success {
  color: #10b981;
}

.text-error {
  color: #ef4444;
}

.params-box, .output-box {
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 6px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.params-box pre, .output-box pre {
  margin: 0;
  font-size: 0.8rem;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Fira Code', monospace;
}

.output-box.stdout {
  border-left: 3px solid #10b981;
}

.output-box.stderr {
  border-left: 3px solid #ef4444;
}

.output-box.stderr pre {
  color: #fca5a5;
}

.result-section {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.error-section {
  background: #fef2f2;
  border-color: #fecaca;
}

.completion-info, .pause-info, .error-info {
  font-size: 0.85rem;
  color: #1e293b;
}

.identity-section {
  background: #fef3c7;
  border-color: #fde68a;
}

.identity-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.identity-item {
  display: flex;
  gap: 8px;
  font-size: 0.85rem;
}

.identity-label {
  font-weight: 600;
  color: #92400e;
}

.identity-value {
  color: #1e293b;
}

.slices-section {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.slices-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.slice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 0.8rem;
}

.slice-source {
  font-weight: 600;
  color: #3b82f6;
}

.slice-authority {
  color: #64748b;
  font-size: 0.75rem;
}
</style>

