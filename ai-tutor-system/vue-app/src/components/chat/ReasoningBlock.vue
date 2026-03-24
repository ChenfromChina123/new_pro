<template>
  <div class="reasoning-block" :class="{ 'streaming': isStreaming, 'collapsed': isCollapsed }">
    <div class="reasoning-header" @click="toggleCollapse">
      <div class="header-left">
        <div class="header-text">
          <span class="reasoning-title">深度思考</span>
          <span v-if="!isCollapsed" class="reasoning-subtitle">AI 推理过程</span>
          <span v-else class="reasoning-count">{{ contentLength }} 字</span>
        </div>
      </div>
      <div class="header-right">
        <i class="fas toggle-icon" :class="isCollapsed ? 'fa-chevron-down' : 'fa-chevron-up'" />
      </div>
    </div>
    <transition name="reasoning-slide">
      <div v-show="!isCollapsed" class="reasoning-content">
        <div class="markdown-body" v-html="formattedContent" />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { formatMessage, getReasoningLength } from '@/utils/chat/messageFormatter'
import { sanitizeNullRuns } from '@/utils/chat/mathRenderer'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  isStreaming: {
    type: Boolean,
    default: false
  },
  defaultCollapsed: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['toggle'])

const isCollapsed = ref(props.defaultCollapsed)

const formattedContent = computed(() => {
  if (!props.content) return ''
  const raw = sanitizeNullRuns(props.content)
  return formatMessage(raw)
})

const contentLength = computed(() => {
  return getReasoningLength(props.content)
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  emit('toggle', isCollapsed.value)
}
</script>

<style scoped>
.reasoning-block {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08) 0%, rgba(139, 92, 246, 0.08) 100%);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.reasoning-block.streaming {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.15);
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
}

.reasoning-header:hover {
  background: rgba(99, 102, 241, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reasoning-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.reasoning-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
}

.reasoning-count {
  font-size: 12px;
  color: var(--text-tertiary);
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.header-right {
  display: flex;
  align-items: center;
}

.toggle-icon {
  color: var(--text-secondary);
  transition: transform 0.3s ease;
}

.reasoning-content {
  padding: 0 16px 16px;
  max-height: 400px;
  overflow-y: auto;
}

.reasoning-content::-webkit-scrollbar {
  width: 6px;
}

.reasoning-content::-webkit-scrollbar-thumb {
  background: rgba(99, 102, 241, 0.3);
  border-radius: 3px;
}

.reasoning-slide-enter-active,
.reasoning-slide-leave-active {
  transition: all 0.3s ease;
}

.reasoning-slide-enter-from,
.reasoning-slide-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
