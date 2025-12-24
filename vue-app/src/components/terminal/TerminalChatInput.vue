<template>
  <div class="input-area-wrapper">
    <slot name="top" />
    <div
      class="input-area"
      :class="{ disabled }"
    >
      <div class="input-toolbar">
        <div class="toolbar-left">
          <span class="toolbar-label">功能</span>
          <div class="selector-container feature-selector">
            <CustomSelect
              v-model="localFeature"
              :options="featureOptions"
            />
          </div>
          <span class="toolbar-label">模型</span>
          <div class="selector-container model-selector">
            <CustomSelect
              v-model="localModel"
              :options="modelOptions"
            />
          </div>
        </div>
      </div>

      <div class="input-row">
        <textarea
          ref="textareaRef"
          v-model="localMessage"
          class="input-textarea"
          :placeholder="placeholder"
          :disabled="disabled"
          rows="1"
          @input="handleInput"
          @keydown.enter.prevent="handleEnter"
        />
        <button
          class="send-btn"
          :disabled="!canSend || disabled"
          @click="handleSend"
        >
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import CustomSelect from '@/components/CustomSelect.vue'

const props = defineProps({
  message: {
    type: String,
    default: ''
  },
  model: {
    type: String,
    default: ''
  },
  modelOptions: {
    type: Array,
    default: () => []
  },
  feature: {
    type: String,
    default: 'CHAT'
  },
  featureOptions: {
    type: Array,
    default: () => [
      { value: 'CHAT', label: '聊天' },
      { value: 'CODEX', label: '代码编辑' },
      { value: 'AUTOCOMPLETE', label: '自动补全' },
      { value: 'APPLY', label: '应用更改' },
      { value: 'SCM', label: '提交消息' }
    ]
  },
  mode: {
    type: String,
    default: 'AGENT'
  },
  modeOptions: {
    type: Array,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false
  },
  canSend: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: '输入指令，例如：创建一个Vue项目...'
  }
})

const emit = defineEmits(['update:message', 'update:model', 'update:feature', 'update:mode', 'send', 'enter'])

const textareaRef = ref(null)

const localMessage = computed({
  get: () => props.message,
  set: (val) => emit('update:message', val)
})

const localModel = computed({
  get: () => props.model,
  set: (val) => emit('update:model', val)
})

const localFeature = computed({
  get: () => props.feature,
  set: (val) => emit('update:feature', val)
})

const localMode = computed({
  get: () => props.mode,
  set: (val) => emit('update:mode', val)
})

/**
 * 自动调整输入框高度（向上增长，最大 200px）
 */
const adjustTextareaHeight = async () => {
  await nextTick()
  const textarea = textareaRef.value
  if (!textarea) return
  textarea.style.height = 'auto'
  textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`
}

/**
 * 处理输入事件：同步内容并自适应高度
 * @param {Event} event 输入事件
 */
const handleInput = async (event) => {
  const textarea = event.target
  emit('update:message', textarea.value)
  await adjustTextareaHeight()
}

/**
 * 处理回车：把键盘事件交给父组件（父组件负责 shift+enter 等策略）
 * @param {KeyboardEvent} event 键盘事件
 */
const handleEnter = (event) => {
  emit('enter', event)
}

/**
 * 点击发送：交给父组件执行发送逻辑
 */
const handleSend = () => {
  emit('send')
}

watch(
  () => props.message,
  () => {
    adjustTextareaHeight()
  }
)
</script>

<style scoped>
.input-area-wrapper {
  padding: 16px 20px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.input-area {
  max-width: 900px;
  margin: 0 auto;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.input-area:focus-within {
  border-color: #3b82f6;
  background: #fff;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.1);
}

.input-area.disabled {
  opacity: 0.6;
  background: #f1f5f9;
  cursor: not-allowed;
}

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 4px;
  border-bottom: 1px solid transparent;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.toolbar-label {
  color: #64748b;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.feature-selector :deep(.custom-select) {
  width: 130px;
}

.model-selector :deep(.custom-select) {
  width: 160px;
}

.selector-container :deep(.select-trigger) {
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 0 10px;
  height: 28px;
  font-size: 0.8rem;
  border-radius: 6px;
  transition: all 0.2s;
}

.selector-container :deep(.select-trigger:hover) {
  border-color: #cbd5e1;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.input-textarea {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  padding: 6px 0;
  color: #1e293b;
  font-size: 0.95rem;
  line-height: 1.6;
  max-height: 200px;
  outline: none;
}

.input-textarea::placeholder {
  color: #94a3b8;
}

.send-btn {
  padding: 0 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.send-btn:hover:not(:disabled) {
  background: #2563eb;
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
  transform: translateY(-1px);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
}

.send-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}
</style>

