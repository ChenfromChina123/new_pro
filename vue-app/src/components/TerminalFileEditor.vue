<template>
  <div class="editor-container">
    <div class="editor-header">
      <div class="header-left">
        <button
          class="nav-btn"
          title="返回文件列表"
          @click="$emit('close')"
        >
          <i class="fas fa-arrow-left" /> 返回
        </button>
        <span class="file-icon">📄</span>
        <span class="file-name">{{ file.name }}</span>
        <span class="file-path">{{ file.path }}</span>
      </div>
      <div class="header-right">
        <button 
          v-if="isMarkdown"
          class="preview-toggle-btn"
          @click="showPreview = !showPreview"
        >
          {{ showPreview ? '编辑' : '预览' }}
        </button>
        <div
          v-if="isDirty"
          class="editor-status"
        >
          <span class="dirty-dot">●</span> 未保存
        </div>
        <button 
          class="save-btn" 
          :disabled="isSaving || !isDirty" 
          @click="handleSave"
        >
          <i class="fas fa-save" />
          {{ isSaving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>

    <div class="editor-body">
      <template v-if="!showPreview">
        <div
          ref="lineNumbersRef"
          class="line-numbers"
        >
          <div
            v-for="n in lineCount"
            :key="n"
            class="line-num"
          >
            {{ n }}
          </div>
        </div>
        <textarea
          ref="textareaRef"
          v-model="content"
          class="code-textarea"
          spellcheck="false"
          @input="handleInput"
          @scroll="syncScroll"
          @keydown.tab.prevent="insertTab"
        />
      </template>
      <div 
        v-else 
        class="markdown-preview"
        v-html="renderedMarkdown"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useUIStore } from '@/stores/ui'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const props = defineProps({
  file: { type: Object, required: true },
  initialContent: { type: String, default: '' }
})

const emit = defineEmits(['close', 'save'])

const uiStore = useUIStore()
const content = ref(props.initialContent)
const isSaving = ref(false)
const isDirty = ref(false)
const showPreview = ref(false)
const textareaRef = ref(null)
const lineNumbersRef = ref(null)

const isMarkdown = computed(() => {
  return props.file.name.toLowerCase().endsWith('.md')
})

const renderedMarkdown = computed(() => {
  if (!content.value) return ''
  return marked(content.value)
})

// Configure marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  breaks: true,
  gfm: true
})

const lineCount = computed(() => {
  return content.value.split('\n').length
})

const handleInput = () => {
  isDirty.value = true
}

const handleSave = async () => {
  if (!isDirty.value) return
  isSaving.value = true
  try {
    await emit('save', content.value)
    isDirty.value = false
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}

const syncScroll = (e) => {
  if (lineNumbersRef.value) {
    lineNumbersRef.value.scrollTop = e.target.scrollTop
  }
}

const insertTab = (e) => {
  const start = e.target.selectionStart
  const end = e.target.selectionEnd
  const value = content.value
  content.value = value.substring(0, start) + '  ' + value.substring(end)
  isDirty.value = true
  
  // Restore cursor position
  setTimeout(() => {
    e.target.selectionStart = e.target.selectionEnd = start + 2
  }, 0)
}

// Watch for external content updates (e.g. reload)
watch(() => props.initialContent, (newVal) => {
  if (newVal !== content.value && !isDirty.value) {
    content.value = newVal
  }
})
</script>

<style scoped>
.editor-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #2d2d2d;
  border-bottom: 1px solid #1e1e1e;
  height: 40px;
  flex-shrink: 0;
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-btn {
  background: transparent;
  border: none;
  color: #cccccc;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.9rem;
  padding: 4px 8px;
  border-radius: 4px;
}

.nav-btn:hover {
  background: #3e3e42;
  color: #ffffff;
}

.file-name {
  font-weight: bold;
  color: #ffffff;
}

.file-path {
  color: #858585;
  font-size: 0.8rem;
}

.save-btn {
  background: #0e639c;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 2px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
}

.save-btn:hover {
  background: #1177bb;
}

.save-btn:disabled {
  background: #4d4d4d;
  color: #858585;
  cursor: not-allowed;
}

.preview-toggle-btn {
  background: #3e3e42;
  color: #ffffff;
  border: 1px solid #474747;
  padding: 4px 12px;
  border-radius: 2px;
  cursor: pointer;
  font-size: 0.85rem;
}

.preview-toggle-btn:hover {
  background: #4e4e52;
}

.editor-status {
  font-size: 0.8rem;
  color: #cccccc;
  display: flex;
  align-items: center;
  gap: 4px;
}

.dirty-dot {
  color: #ffffff;
  font-size: 1.2rem;
  line-height: 0.5;
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.markdown-preview {
  flex: 1;
  padding: 20px;
  background: #ffffff;
  color: #333;
  overflow-y: auto;
  line-height: 1.6;
}

.markdown-preview :deep(h1), 
.markdown-preview :deep(h2), 
.markdown-preview :deep(h3) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-preview :deep(h1) { padding-bottom: 0.3em; border-bottom: 1px solid #eaecef; font-size: 2em; }
.markdown-preview :deep(h2) { padding-bottom: 0.3em; border-bottom: 1px solid #eaecef; font-size: 1.5em; }

.markdown-preview :deep(p) {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-preview :deep(code) {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27,31,35,0.05);
  border-radius: 3px;
  font-family: monospace;
}

.markdown-preview :deep(pre) {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 3px;
  margin-bottom: 16px;
}

.markdown-preview :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.markdown-preview :deep(ul), 
.markdown-preview :deep(ol) {
  padding-left: 2em;
  margin-bottom: 16px;
}

.line-numbers {
  width: 48px;
  background: #1e1e1e;
  color: #858585;
  text-align: right;
  padding: 10px 8px 10px 0;
  font-size: 14px;
  line-height: 1.5;
  border-right: 1px solid #333;
  user-select: none;
  overflow: hidden;
}

.line-num {
  height: 21px; /* Match line-height of textarea */
}

.code-textarea {
  flex: 1;
  background: #1e1e1e;
  color: #d4d4d4;
  border: none;
  resize: none;
  padding: 10px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.5;
  outline: none;
  white-space: pre;
  overflow: auto;
  tab-size: 2;
}

/* Scrollbar styling */
.code-textarea::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.code-textarea::-webkit-scrollbar-track {
  background: #1e1e1e;
}

.code-textarea::-webkit-scrollbar-thumb {
  background: #424242;
}

.code-textarea::-webkit-scrollbar-thumb:hover {
  background: #4f4f4f;
}
</style>
