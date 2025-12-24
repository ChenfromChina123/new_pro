<template>
  <div class="input-area-wrapper">
    <slot name="top" />
    <div
      class="input-area"
      :class="{ disabled }"
    >
      <div class="input-toolbar">
        <div class="toolbar-left">
          <span class="toolbar-label">模型</span>
          <div class="model-selector">
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

const emit = defineEmits(['update:message', 'update:model', 'send', 'enter'])

const textareaRef = ref(null)

const localMessage = computed({
  get: () => props.message,
  set: (val) => emit('update:message', val)
})

const localModel = computed({
  get: () => props.model,
  set: (val) => emit('update:model', val)
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
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.input-area {
  max-width: 800px;
  margin: 0 auto;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: all 0.2s ease;
}

.input-area:focus-within {
  border-color: #3b82f6;
  background: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.08);
}

.input-area.disabled {
  opacity: 0.6;
  background: #f1f5f9;
}

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.toolbar-label {
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.model-selector :deep(.custom-select) {
  width: 150px;
}

.model-selector :deep(.select-trigger) {
  background: transparent;
  padding: 2px 8px;
  height: 28px;
  font-size: 0.8rem;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.input-textarea {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  padding: 4px 0;
  color: #1e293b;
  font-size: 0.95rem;
  line-height: 1.5;
  max-height: 150px;
  outline: none;
}

.input-textarea::placeholder {
  color: #94a3b8;
}

.send-btn {
  padding: 6px 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
}

.send-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}
</style>

