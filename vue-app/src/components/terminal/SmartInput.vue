<template>
  <div class="smart-input-container">
    <!-- 智能补全下拉框 -->
    <div
      v-if="showSuggestions && filteredSuggestions.length > 0"
      class="suggestions-dropdown"
      :style="{ bottom: dropdownBottom }"
    >
      <div
        v-for="(suggestion, index) in filteredSuggestions"
        :key="index"
        class="suggestion-item"
        :class="{ active: selectedIndex === index }"
        @click="selectSuggestion(suggestion)"
        @mouseenter="selectedIndex = index"
      >
        <div class="suggestion-header">
          <span class="suggestion-icon">{{ suggestion.icon }}</span>
          <span class="suggestion-title">{{ suggestion.title }}</span>
          <span class="suggestion-shortcut">{{ suggestion.shortcut }}</span>
        </div>
        <div class="suggestion-desc">{{ suggestion.description }}</div>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="input-wrapper">
      <div class="input-header">
        <select v-model="localModel" class="model-selector">
          <option v-for="opt in modelOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
        <div class="input-actions">
          <button
            v-if="showTemplateButton"
            class="template-btn"
            title="提示词模板"
            @click="toggleTemplates"
          >
            📋
          </button>
        </div>
      </div>
      
      <textarea
        ref="textareaRef"
        v-model="localMessage"
        class="message-input"
        :placeholder="placeholder"
        :disabled="disabled"
        :rows="rows"
        @input="handleInput"
        @keydown="handleKeydown"
        @focus="handleFocus"
        @blur="handleBlur"
      />
      
      <div class="input-footer">
        <div class="input-hints">
          <span v-if="!localMessage.trim()" class="hint">
            <kbd>↑</kbd> 快捷模板 | <kbd>Tab</kbd> 补全
          </span>
          <span v-else class="char-count">
            {{ localMessage.length }} 字符
          </span>
        </div>
        <button
          class="send-btn"
          :disabled="!canSend"
          :class="{ active: canSend }"
          @click="handleSend"
        >
          发送 <kbd>Enter</kbd>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'

/**
 * 提示词模板
 */
interface PromptTemplate {
  icon: string
  title: string
  shortcut: string
  description: string
  content: string
  params?: string[]
}

interface ModelOption {
  label: string
  value: string
  description: string
}

interface Props {
  message: string
  model: string
  modelOptions: ModelOption[]
  disabled?: boolean
  placeholder?: string
  canSend?: boolean
  showTemplateButton?: boolean
  framework?: string
}

interface Emits {
  (e: 'update:message', value: string): void
  (e: 'update:model', value: string): void
  (e: 'send'): void
  (e: 'enter', event: KeyboardEvent): void
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  placeholder: '输入您的指令...',
  canSend: false,
  showTemplateButton: true,
  framework: 'vue'
})

const emit = defineEmits<Emits>()

const localMessage = ref(props.message)
const localModel = ref(props.model)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const rows = ref(3)

// 智能补全状态
const showSuggestions = ref(false)
const selectedIndex = ref(0)
const isFocused = ref(false)

// 监听props变化
watch(() => props.message, (val) => { localMessage.value = val })
watch(() => props.model, (val) => { localModel.value = val })
watch(localMessage, (val) => emit('update:message', val))
watch(localModel, (val) => emit('update:model', val))

/**
 * 提示词模板库（根据框架动态生成）
 */
const templates = computed<PromptTemplate[]>(() => {
  const commonTemplates: PromptTemplate[] = [
    {
      icon: '🚀',
      title: '快速开始',
      shortcut: 'Ctrl+1',
      description: '创建新项目或功能模块',
      content: '请帮我创建一个${module}模块，包含${features}功能'
    },
    {
      icon: '🐛',
      title: '问题诊断',
      shortcut: 'Ctrl+2',
      description: '分析并修复代码问题',
      content: '请帮我分析并修复${file}文件中的问题'
    },
    {
      icon: '♻️',
      title: '代码重构',
      shortcut: 'Ctrl+3',
      description: '优化现有代码结构',
      content: '请帮我重构${file}，优化${aspect}'
    },
    {
      icon: '📝',
      title: '添加文档',
      shortcut: 'Ctrl+4',
      description: '生成代码注释和文档',
      content: '请为${file}添加详细的函数注释和文档'
    },
    {
      icon: '🧪',
      title: '编写测试',
      shortcut: 'Ctrl+5',
      description: '生成单元测试代码',
      content: '请为${file}编写单元测试'
    }
  ]

  // 框架特定模板
  if (props.framework === 'vue') {
    commonTemplates.push({
      icon: '🎨',
      title: 'Vue组件',
      shortcut: 'Ctrl+6',
      description: '创建Vue组件',
      content: '请创建一个Vue组件${name}，包含${props}属性'
    })
  } else if (props.framework === 'react') {
    commonTemplates.push({
      icon: '⚛️',
      title: 'React组件',
      shortcut: 'Ctrl+6',
      description: '创建React组件',
      content: '请创建一个React组件${name}，使用TypeScript'
    })
  }

  return commonTemplates
})

/**
 * 过滤后的建议
 */
const filteredSuggestions = computed(() => {
  if (!localMessage.value.trim() || !showSuggestions.value) {
    return templates.value
  }

  const query = localMessage.value.toLowerCase()
  return templates.value.filter(
    t => t.title.toLowerCase().includes(query) || 
         t.description.toLowerCase().includes(query)
  )
})

/**
 * 下拉框位置
 */
const dropdownBottom = computed(() => {
  return `${60 + rows.value * 24}px`
})

/**
 * 处理输入
 */
const handleInput = () => {
  // 自动调整行数
  if (textareaRef.value) {
    const lineCount = localMessage.value.split('\n').length
    rows.value = Math.max(3, Math.min(10, lineCount))
  }

  // 触发智能补全
  if (localMessage.value.length > 0 && isFocused.value) {
    showSuggestions.value = true
    selectedIndex.value = 0
  } else {
    showSuggestions.value = false
  }
}

/**
 * 处理键盘事件
 */
const handleKeydown = (e: KeyboardEvent) => {
  // 智能补全导航
  if (showSuggestions.value && filteredSuggestions.value.length > 0) {
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      selectedIndex.value = Math.max(0, selectedIndex.value - 1)
      return
    } else if (e.key === 'ArrowDown') {
      e.preventDefault()
      selectedIndex.value = Math.min(
        filteredSuggestions.value.length - 1,
        selectedIndex.value + 1
      )
      return
    } else if (e.key === 'Tab') {
      e.preventDefault()
      selectSuggestion(filteredSuggestions.value[selectedIndex.value])
      return
    } else if (e.key === 'Escape') {
      showSuggestions.value = false
      return
    }
  }

  // Enter发送
  if (e.key === 'Enter') {
    emit('enter', e)
  }

  // 快捷键模板
  if (e.ctrlKey && e.key >= '1' && e.key <= '9') {
    e.preventDefault()
    const index = parseInt(e.key) - 1
    if (index < templates.value.length) {
      selectSuggestion(templates.value[index])
    }
  }
}

/**
 * 选择建议
 */
const selectSuggestion = (suggestion: PromptTemplate) => {
  localMessage.value = suggestion.content
  showSuggestions.value = false
  
  // 聚焦到第一个参数占位符
  nextTick(() => {
    if (textareaRef.value) {
      const match = suggestion.content.match(/\$\{([^}]+)\}/)
      if (match) {
        const start = match.index!
        const end = start + match[0].length
        textareaRef.value.focus()
        textareaRef.value.setSelectionRange(start, end)
      }
    }
  })
}

/**
 * 切换模板面板
 */
const toggleTemplates = () => {
  showSuggestions.value = !showSuggestions.value
  if (showSuggestions.value) {
    selectedIndex.value = 0
  }
}

/**
 * 处理聚焦
 */
const handleFocus = () => {
  isFocused.value = true
}

/**
 * 处理失焦
 */
const handleBlur = () => {
  // 延迟隐藏，以便点击建议项
  setTimeout(() => {
    isFocused.value = false
    showSuggestions.value = false
  }, 200)
}

/**
 * 发送消息
 */
const handleSend = () => {
  if (props.canSend) {
    emit('send')
  }
}
</script>

<style scoped>
.smart-input-container {
  position: relative;
}

.suggestions-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  overflow-y: auto;
  z-index: 1000;
}

.suggestion-item {
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #f1f5f9;
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover,
.suggestion-item.active {
  background: #f8fafc;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.suggestion-icon {
  font-size: 1.2rem;
}

.suggestion-title {
  flex: 1;
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e293b;
}

.suggestion-shortcut {
  font-size: 0.75rem;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.suggestion-desc {
  font-size: 0.8rem;
  color: #64748b;
  margin-left: 32px;
}

.input-wrapper {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.input-wrapper:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-header {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.model-selector {
  padding: 4px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.85rem;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.model-selector:hover {
  border-color: #cbd5e1;
}

.input-actions {
  display: flex;
  gap: 8px;
}

.template-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1.1rem;
  transition: all 0.2s;
}

.template-btn:hover {
  background: #e2e8f0;
  transform: scale(1.05);
}

.message-input {
  width: 100%;
  padding: 16px;
  border: none;
  outline: none;
  font-size: 0.95rem;
  line-height: 1.6;
  resize: none;
  font-family: inherit;
  color: #1e293b;
}

.message-input:disabled {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

.input-footer {
  padding: 12px 16px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-hints {
  font-size: 0.8rem;
  color: #64748b;
}

.hint kbd {
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.75rem;
}

.char-count {
  color: #94a3b8;
}

.send-btn {
  padding: 8px 20px;
  background: #e2e8f0;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #64748b;
  cursor: not-allowed;
  transition: all 0.2s;
}

.send-btn.active {
  background: #3b82f6;
  color: #fff;
  cursor: pointer;
}

.send-btn.active:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.send-btn kbd {
  margin-left: 6px;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.75rem;
}
</style>

