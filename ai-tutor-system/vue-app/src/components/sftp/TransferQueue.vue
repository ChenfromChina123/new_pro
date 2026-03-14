<template>
  <div class="transfer-queue">
    <div class="queue-header">
      <span class="queue-title">传输队列 ({{ tasks.length }})</span>
      <button
        class="btn-clear"
        @click="clearCompleted"
      >
        清除已完成
      </button>
    </div>
    <div class="queue-list">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="transfer-item"
      >
        <div class="item-icon">
          {{ task.type === 'upload' ? '📤' : '📥' }}
        </div>
        <div class="item-info">
          <div class="item-name">
            {{ task.fileName }}
          </div>
          <div class="item-progress">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: task.progress + '%' }"
              />
            </div>
            <span class="progress-text">{{ task.progress }}%</span>
            <span class="speed-text">{{ task.speed }}</span>
          </div>
        </div>
        <div
          class="item-status"
          :class="task.status"
        >
          {{ getStatusText(task.status) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useSFTPStore } from '@/stores/sftp'

const sftpStore = useSFTPStore()

const tasks = computed(() => sftpStore.transferTasks)

/**
 * 获取状态文本
 * @param {string} status - 状态
 * @returns {string} 状态文本
 */
function getStatusText(status) {
  const statusMap = {
    pending: '等待中',
    transferring: '传输中',
    completed: '已完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

/**
 * 清除已完成的任务
 */
function clearCompleted() {
  sftpStore.clearCompletedTasks()
}
</script>

<style scoped>
.transfer-queue {
  background: var(--sidebar-bg);
  border-top: 1px solid var(--border);
  max-height: 200px;
  display: flex;
  flex-direction: column;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.queue-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-dim);
}

.btn-clear {
  background: transparent;
  border: none;
  color: var(--accent);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.btn-clear:hover {
  text-decoration: underline;
}

.queue-list {
  flex: 1;
  overflow-y: auto;
}

.transfer-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
}

.item-icon {
  font-size: 18px;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.item-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: var(--border);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent);
  transition: width 0.3s;
}

.progress-text {
  font-size: 11px;
  color: var(--text-dim);
  min-width: 36px;
}

.speed-text {
  font-size: 11px;
  color: var(--text-dim);
}

.item-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--border);
}

.item-status.completed {
  background: rgba(16, 185, 129, 0.2);
  color: var(--success);
}

.item-status.failed {
  background: rgba(239, 68, 68, 0.2);
  color: var(--danger);
}

.item-status.transferring {
  background: rgba(59, 130, 246, 0.2);
  color: var(--accent);
}
</style>
