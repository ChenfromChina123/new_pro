<template>
  <div class="input-area-wrapper">
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
  padding: 20px;
  background: #fff;
  border-top: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.input-area {
  max-width: 850px;
  margin: 0 auto;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 10px 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.2s;
}

.input-area:focus-within {
  border-color: #3b82f6;
  background: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.08);
}

.input-area.disabled {
  opacity: 0.9;
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
  gap: 10px;
  min-width: 0;
}

.toolbar-label {
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 600;
}

.model-selector :deep(.custom-select) {
  width: 180px;
}

.model-selector :deep(.select-trigger) {
  background: transparent;
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
  padding: 6px 0;
  font-size: 1rem;
  line-height: 1.5;
  color: #1e293b;
  resize: none;
  outline: none;
  min-height: 24px;
  max-height: 200px;
}

.send-btn {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 8px 16px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  margin-bottom: 2px;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #2563eb;
}

.send-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}
</style>

