<template>
  <div class="feature-selector">
    <div class="selector-label">
      <span class="label-text">功能</span>
    </div>
    <div class="feature-buttons">
      <button
        v-for="feature in featureOptions"
        :key="feature.value"
        :class="['feature-btn', { active: selectedFeature === feature.value }]"
        :title="feature.description"
        @click="selectFeature(feature.value)"
      >
        <span class="feature-icon">{{ feature.icon }}</span>
        <span class="feature-name">{{ feature.label }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: 'CHAT'
  }
})

const emit = defineEmits(['update:modelValue'])

const featureOptions = [
  {
    value: 'CHAT',
    label: '聊天',
    icon: '💬',
    description: '聊天助手：支持对话、工具调用、任务执行'
  },
  {
    value: 'CODEX',
    label: '代码编辑',
    icon: '✏️',
    description: '代码编辑（FIM）：在代码中间插入或替换代码片段'
  },
  {
    value: 'AUTOCOMPLETE',
    label: '自动补全',
    icon: '⚡',
    description: '自动补全：代码自动补全建议（需要支持 FIM 的模型）'
  },
  {
    value: 'APPLY',
    label: '应用更改',
    icon: '✅',
    description: '应用更改：将 AI 建议的更改应用到代码'
  },
  {
    value: 'SCM',
    label: '提交消息',
    icon: '📝',
    description: '提交消息生成：生成 Git 提交消息'
  }
]

const selectedFeature = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const selectFeature = (feature) => {
  selectedFeature.value = feature
}
</script>

<style scoped>
.feature-selector {
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

.feature-buttons {
  display: flex;
  gap: 8px;
  flex: 1;
  flex-wrap: wrap;
}

.feature-btn {
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
  min-width: 0;
  justify-content: center;
}

.feature-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #475569;
}

.feature-btn.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.feature-btn.active:hover {
  background: #2563eb;
  border-color: #2563eb;
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
}

.feature-icon {
  font-size: 1rem;
  line-height: 1;
}

.feature-name {
  font-weight: 500;
}
</style>

