<template>
  <div class="checkpoint-timeline">
    <div class="timeline-header">
      <h3>🕐 检查点时间线</h3>
      <div class="header-actions">
        <button class="btn-create" @click="$emit('create')">
          ➕ 创建检查点
        </button>
        <button class="btn-export" @click="exportTimeline">
          📤 导出
        </button>
      </div>
    </div>

    <div class="timeline-container">
      <div v-if="checkpoints.length === 0" class="empty-state">
        <div class="empty-icon">⏱️</div>
        <p>暂无检查点</p>
        <p class="hint">检查点将在关键时刻自动创建</p>
      </div>

      <div v-else class="timeline-track">
        <div
          v-for="(checkpoint, index) in sortedCheckpoints"
          :key="checkpoint.id"
          class="checkpoint-node"
          :class="{
            active: checkpoint.id === currentCheckpointId,
            'type-auto': checkpoint.type === 'AUTO',
            'type-manual': checkpoint.type === 'MANUAL',
            'type-before-edit': checkpoint.type === 'BEFORE_EDIT'
          }"
          @click="selectCheckpoint(checkpoint)"
        >
          <div class="node-marker">
            <div class="marker-dot" />
            <div v-if="index < sortedCheckpoints.length - 1" class="marker-line" />
          </div>

          <div class="node-content">
            <div class="node-header">
              <div class="node-type-badge">
                {{ getTypeLabel(checkpoint.type) }}
              </div>
              <div class="node-time">
                {{ formatTime(checkpoint.createdAt) }}
              </div>
            </div>

            <div class="node-description">
              {{ checkpoint.description || '无描述' }}
            </div>

            <div class="node-meta">
              <span class="meta-item">
                📝 消息序号: {{ checkpoint.messageOrder }}
              </span>
              <span v-if="checkpoint.fileSnapshots" class="meta-item">
                📁 文件快照: {{ checkpoint.fileSnapshots.length }}
              </span>
            </div>

            <div class="node-actions">
              <button
                class="btn-jump"
                :disabled="checkpoint.id === currentCheckpointId"
                @click.stop="jumpTo(checkpoint)"
              >
                🚀 跳转
              </button>
              <button class="btn-compare" @click.stop="compare(checkpoint)">
                🔍 对比
              </button>
              <button class="btn-export-single" @click.stop="exportCheckpoint(checkpoint)">
                💾 导出
              </button>
              <button
                v-if="checkpoint.type === 'MANUAL'"
                class="btn-delete"
                @click.stop="deleteCheckpoint(checkpoint)"
              >
                🗑️ 删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 对比对话框 -->
    <div v-if="showCompareDialog" class="compare-dialog-overlay" @click="showCompareDialog = false">
      <div class="compare-dialog" @click.stop>
        <div class="dialog-header">
          <h3>检查点对比</h3>
          <button class="close-btn" @click="showCompareDialog = false">✕</button>
        </div>
        <div class="dialog-body">
          <div class="compare-section">
            <h4>当前状态</h4>
            <div class="compare-content">
              <p>消息数: {{ currentState.messageCount }}</p>
              <p>任务数: {{ currentState.taskCount }}</p>
            </div>
          </div>
          <div class="compare-divider">→</div>
          <div class="compare-section">
            <h4>检查点状态</h4>
            <div class="compare-content">
              <p>消息数: {{ compareCheckpoint?.messageOrder || 0 }}</p>
              <p>文件快照: {{ compareCheckpoint?.fileSnapshots?.length || 0 }}</p>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-primary" @click="confirmJump">确认跳转</button>
          <button class="btn-secondary" @click="showCompareDialog = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  checkpoints: {
    type: Array,
    default: () => []
  },
  currentCheckpointId: {
    type: Number,
    default: null
  },
  currentState: {
    type: Object,
    default: () => ({ messageCount: 0, taskCount: 0 })
  }
})

const emit = defineEmits(['create', 'jump', 'delete', 'export', 'compare'])

const showCompareDialog = ref(false)
const compareCheckpoint = ref(null)
const selectedCheckpoint = ref(null)

/**
 * 按时间排序的检查点列表（最新的在上面）
 */
const sortedCheckpoints = computed(() => {
  return [...props.checkpoints].sort((a, b) => {
    return new Date(b.createdAt) - new Date(a.createdAt)
  })
})

/**
 * 获取类型标签
 */
function getTypeLabel(type) {
  const labels = {
    'AUTO': '自动',
    'MANUAL': '手动',
    'BEFORE_EDIT': '编辑前',
    'LOOP_START': '循环开始',
    'LOOP_END': '循环结束',
    'PAUSE': '暂停',
    'INTERRUPT': '中断'
  }
  return labels[type] || type
}

/**
 * 格式化时间
 */
function formatTime(timestamp) {
  if (!timestamp) return 'N/A'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  // 如果是今天
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  }
  
  // 否则显示完整日期
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 选择检查点
 */
function selectCheckpoint(checkpoint) {
  selectedCheckpoint.value = checkpoint
}

/**
 * 跳转到检查点
 */
function jumpTo(checkpoint) {
  emit('jump', checkpoint.id)
}

/**
 * 对比检查点
 */
function compare(checkpoint) {
  compareCheckpoint.value = checkpoint
  showCompareDialog.value = true
}

/**
 * 确认跳转
 */
function confirmJump() {
  if (compareCheckpoint.value) {
    emit('jump', compareCheckpoint.value.id)
    showCompareDialog.value = false
  }
}

/**
 * 导出检查点
 */
function exportCheckpoint(checkpoint) {
  emit('export', checkpoint.id)
}

/**
 * 删除检查点
 */
function deleteCheckpoint(checkpoint) {
  if (confirm(`确定要删除检查点 "${checkpoint.description}" 吗？`)) {
    emit('delete', checkpoint.id)
  }
}

/**
 * 导出时间线
 */
function exportTimeline() {
  const data = {
    checkpoints: props.checkpoints,
    exportTime: new Date().toISOString()
  }
  
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `checkpoint-timeline-${Date.now()}.json`
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}
</script>

<style scoped>
.checkpoint-timeline {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.timeline-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-create, .btn-export {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-create {
  background: #3b82f6;
  color: #fff;
}

.btn-create:hover {
  background: #2563eb;
}

.btn-export {
  background: #f1f5f9;
  color: #64748b;
}

.btn-export:hover {
  background: #e2e8f0;
}

.timeline-container {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 20px;
  position: relative;
  scrollbar-width: thin;
  scrollbar-color: #3b82f6 #f1f5f9;
}

.timeline-container::-webkit-scrollbar {
  width: 6px;
}

.timeline-container::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.timeline-container::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.timeline-container::-webkit-scrollbar-thumb:hover {
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

.timeline-track {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.checkpoint-node {
  display: flex;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.checkpoint-node:hover .node-content {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

.checkpoint-node.active .node-content {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
}

.node-marker {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 8px;
}

.marker-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #3b82f6;
  border: 3px solid #fff;
  box-shadow: 0 0 0 2px #3b82f6;
  z-index: 2;
}

.checkpoint-node.type-auto .marker-dot {
  background: #10b981;
  box-shadow: 0 0 0 2px #10b981;
}

.checkpoint-node.type-manual .marker-dot {
  background: #f59e0b;
  box-shadow: 0 0 0 2px #f59e0b;
}

.checkpoint-node.type-before-edit .marker-dot {
  background: #8b5cf6;
  box-shadow: 0 0 0 2px #8b5cf6;
}

.marker-line {
  width: 2px;
  flex: 1;
  background: linear-gradient(to bottom, #3b82f6, #cbd5e1);
  margin-top: 4px;
  min-height: 40px;
}

.node-content {
  flex: 1;
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.node-type-badge {
  background: #3b82f6;
  color: #fff;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.checkpoint-node.type-auto .node-type-badge {
  background: #10b981;
}

.checkpoint-node.type-manual .node-type-badge {
  background: #f59e0b;
}

.checkpoint-node.type-before-edit .node-type-badge {
  background: #8b5cf6;
}

.node-time {
  font-size: 0.85rem;
  color: #64748b;
}

.node-description {
  font-size: 0.95rem;
  color: #1e293b;
  margin-bottom: 12px;
  line-height: 1.5;
}

.node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.meta-item {
  font-size: 0.8rem;
  color: #64748b;
}

.node-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.node-actions button {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-jump {
  background: #3b82f6;
  color: #fff;
}

.btn-jump:hover:not(:disabled) {
  background: #2563eb;
}

.btn-jump:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.btn-compare {
  background: #f1f5f9;
  color: #64748b;
}

.btn-compare:hover {
  background: #e2e8f0;
}

.btn-export-single {
  background: #f1f5f9;
  color: #64748b;
}

.btn-export-single:hover {
  background: #e2e8f0;
}

.btn-delete {
  background: #fee2e2;
  color: #991b1b;
}

.btn-delete:hover {
  background: #fecaca;
}

/* 对比对话框 */
.compare-dialog-overlay {
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

.compare-dialog {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
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
  display: flex;
  gap: 20px;
  align-items: center;
}

.compare-section {
  flex: 1;
}

.compare-section h4 {
  margin: 0 0 12px 0;
  font-size: 1rem;
  color: #1e293b;
}

.compare-content {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

.compare-content p {
  margin: 8px 0;
  font-size: 0.9rem;
  color: #475569;
}

.compare-divider {
  font-size: 2rem;
  color: #3b82f6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #e2e8f0;
}

.btn-primary, .btn-secondary {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
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
  background: #f1f5f9;
  color: #64748b;
}

.btn-secondary:hover {
  background: #e2e8f0;
}
</style>

