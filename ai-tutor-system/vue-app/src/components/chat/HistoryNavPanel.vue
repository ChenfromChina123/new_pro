<template>
  <div class="history-nav-container">
    <button
      class="history-nav-toggle"
      title="提问历史"
      @click="togglePanel"
    >
      <i class="fas fa-list-ul" />
    </button>

    <transition name="slide-fade">
      <div v-if="showPanel" class="history-nav-panel">
        <div class="panel-header">
          <h3>提问历史</h3>
          <button
            class="close-btn"
            @click="closePanel"
          >
            <i class="fas fa-times" />
          </button>
        </div>
        <div class="panel-content">
          <div
            v-for="(msg, index) in userMessages"
            :key="index"
            class="history-item"
            @click="scrollToMessage(msg.elementIndex)"
          >
            <span class="time">{{ formatTimeShort(msg.timestamp) }}</span>
            <span class="text">{{ truncateText(msg.content, 50) }}</span>
          </div>
          <div
            v-if="userMessages.length === 0"
            class="empty-history"
          >
            暂无提问记录
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { formatTimeShort as formatTime } from '@/utils/chat/messageFormatter'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['scroll-to-message'])

const showPanel = ref(false)

const userMessages = computed(() => {
  return props.messages
    .map((msg, index) => ({ ...msg, elementIndex: index }))
    .filter(msg => msg.role === 'user')
    .sort((a, b) => b.timestamp - a.timestamp)
})

const togglePanel = () => {
  showPanel.value = !showPanel.value
}

const closePanel = () => {
  showPanel.value = false
}

const scrollToMessage = (index) => {
  emit('scroll-to-message', index)
  if (window.innerWidth < 768) {
    closePanel()
  }
}

const truncateText = (text, maxLength) => {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

const formatTimeShort = (timestamp) => {
  return formatTime(timestamp)
}

defineExpose({
  showPanel,
  togglePanel,
  closePanel
})
</script>

<style scoped>
.history-nav-container {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 40;
}

.history-nav-toggle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.history-nav-toggle:hover {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.history-nav-panel {
  position: absolute;
  top: 0;
  right: 48px;
  width: 320px;
  max-height: 400px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.panel-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.close-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background-color: transparent;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.panel-content {
  max-height: 340px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-bottom: 1px solid var(--border-color);
}

.history-item:last-child {
  border-bottom: none;
}

.history-item:hover {
  background-color: var(--bg-secondary);
}

.history-item .time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.history-item .text {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-history {
  padding: 24px 16px;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 13px;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.2s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

@media (max-width: 768px) {
  .history-nav-panel {
    width: 280px;
    right: 44px;
  }
}
</style>
