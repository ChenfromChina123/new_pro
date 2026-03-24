<template>
  <div class="search-block" :class="{ 'streaming': isSearching, 'collapsed': isCollapsed }">
    <div class="search-header" @click="$emit('toggle')">
      <div class="header-left">
        <div class="header-text">
          <i v-if="isSearching" class="fas fa-circle-notch fa-spin" style="margin-right: 6px; color: #3b82f6;" />
          <i v-else-if="isDone" class="fas fa-globe" style="margin-right: 6px; color: #10b981;" />
          <i v-else class="fas fa-exclamation-circle" style="margin-right: 6px; color: #ef4444;" />
          <span class="search-title">联网搜索</span>
          <span v-if="!isCollapsed" class="search-subtitle">
            {{ statusText }}
          </span>
        </div>
      </div>
      <div class="header-right">
        <i class="fas toggle-icon" :class="isCollapsed ? 'fa-chevron-down' : 'fa-chevron-up'" />
      </div>
    </div>

    <transition name="search-slide">
      <div v-show="!isCollapsed" class="search-content">
        <div v-if="results" class="markdown-body">
          <p><strong>搜索内容：</strong> {{ query }}</p>
          <div v-html="formattedResults" />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatMessage } from '@/utils/chat/messageFormatter'

const props = defineProps({
  query: {
    type: String,
    default: ''
  },
  results: {
    type: String,
    default: ''
  },
  status: {
    type: String,
    default: ''
  },
  isCollapsed: {
    type: Boolean,
    default: false
  }
})

defineEmits(['toggle'])

const isSearching = computed(() => props.status === 'searching')
const isDone = computed(() => props.status === 'done')

const statusText = computed(() => {
  if (isSearching.value) return `正在搜索: ${props.query}`
  if (isDone.value) return '已完成搜索'
  return '搜索失败'
})

const formattedResults = computed(() => {
  if (!props.results) return ''
  return formatMessage(props.results)
})
</script>

<style scoped>
.search-block {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08) 0%, rgba(6, 182, 212, 0.08) 100%);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.search-block.streaming {
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.15);
}

.search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.search-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
}

.header-right {
  display: flex;
  align-items: center;
}

.toggle-icon {
  font-size: 12px;
  color: var(--text-tertiary);
  transition: transform 0.3s ease;
}

.search-content {
  padding: 0 16px 16px;
  max-height: 300px;
  overflow-y: auto;
}

.search-content::-webkit-scrollbar {
  width: 6px;
}

.search-content::-webkit-scrollbar-thumb {
  background: rgba(16, 185, 129, 0.3);
  border-radius: 3px;
}

.search-slide-enter-active,
.search-slide-leave-active {
  transition: all 0.3s ease;
}

.search-slide-enter-from,
.search-slide-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
