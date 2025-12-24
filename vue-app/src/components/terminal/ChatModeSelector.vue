<template>
  <div class="chat-mode-selector">
    <div class="selector-label">
      <span class="label-text">模式</span>
    </div>
    <div class="mode-buttons">
      <button
        v-for="mode in modeOptions"
        :key="mode.value"
        :class="['mode-btn', { active: selectedMode === mode.value }]"
        :title="mode.description"
        @click="selectMode(mode.value)"
      >
        <span class="mode-icon">{{ mode.icon }}</span>
        <span class="mode-name">{{ mode.label }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: 'AGENT'
  }
})

const emit = defineEmits(['update:modelValue'])

const modeOptions = [
  {
    value: 'AGENT',
    label: '自主操作',
    icon: '🤖',
    description: 'AI 可以自主使用工具进行代码开发、文件编辑等操作'
  },
  {
    value: 'GATHER',
    label: '信息收集',
    icon: '🔍',
    description: 'AI 只能使用读取类工具收集信息，不能进行修改操作'
  },
  {
    value: 'NORMAL',
    label: '普通对话',
    icon: '💬',
    description: '普通对话模式，不提供工具调用功能'
  }
]

const selectedMode = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const selectMode = (mode) => {
  selectedMode.value = mode
}
</script>

<style scoped>
.chat-mode-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 8px;
}

.selector-label {
  display: flex;
  align-items: center;
  min-width: 0;
}

.label-text {
  color: #64748b;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.mode-buttons {
  display: flex;
  gap: 8px;
  flex: 1;
}

.mode-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.8rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  flex: 1;
  justify-content: center;
}

.mode-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #475569;
}

.mode-btn.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.mode-btn.active:hover {
  background: #2563eb;
  border-color: #2563eb;
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
}

.mode-icon {
  font-size: 1rem;
  line-height: 1;
}

.mode-name {
  font-weight: 500;
}
</style>

