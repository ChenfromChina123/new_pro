<template>
  <div class="status-bar">
    <div class="status-left">
      <span
        class="connection-status"
        :class="connected ? 'connected' : 'disconnected'"
      >
        ● {{ connected ? '已连接' : '未连接' }}
      </span>
      <span
        v-if="serverInfo"
        class="server-info"
      >
        {{ serverInfo.host }}:{{ serverInfo.port }}
      </span>
    </div>
    <div class="status-center">
      <span v-if="selectedCount > 0">
        已选择 {{ selectedCount }} 项，共 {{ formatSize(selectedSize) }}
      </span>
    </div>
    <div class="status-right">
      <span
        v-if="transferCount > 0"
        class="transfer-info"
      >
        传输中: {{ transferCount }} 个任务
      </span>
      <span class="current-time">{{ currentTime }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { formatFileSize } from '@/utils/fileIcons'

defineProps({
  connected: {
    type: Boolean,
    default: false
  },
  serverInfo: {
    type: Object,
    default: null
  },
  selectedCount: {
    type: Number,
    default: 0
  },
  selectedSize: {
    type: Number,
    default: 0
  },
  transferCount: {
    type: Number,
    default: 0
  }
})

const currentTime = ref('')

let timeInterval = null

/**
 * 更新当前时间
 */
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化的大小
 */
function formatSize(bytes) {
  return formatFileSize(bytes)
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.status-bar {
  height: 24px;
  background: var(--sidebar-bg);
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  font-size: 12px;
  color: var(--text-dim);
  flex-shrink: 0;
}

.status-left,
.status-center,
.status-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 4px;
}

.connection-status.connected {
  color: var(--success);
}

.connection-status.disconnected {
  color: var(--danger);
}

.server-info {
  font-family: 'JetBrains Mono', monospace;
}

.transfer-info {
  color: var(--accent);
}

.current-time {
  font-family: 'JetBrains Mono', monospace;
}
</style>
