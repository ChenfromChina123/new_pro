<template>
  <div class="playground-container">
    <div class="playground-toolbar">
      <div class="toolbar-left">
        <select v-model="selectedLanguage" class="language-select" @change="handleLanguageChange">
          <option v-for="lang in availableLanguages" :key="lang.language" :value="lang.language">
            {{ lang.language }} ({{ lang.version }})
          </option>
        </select>
        <button
          class="run-button"
          @click="executeCode"
          :disabled="isRunning"
        >
          <i :class="isRunning ? 'fas fa-spinner fa-spin' : 'fas fa-play'"></i>
          <span>{{ isRunning ? '运行中...' : '运行' }}</span>
        </button>
        <span class="shortcut-hint">Ctrl+Enter</span>
      </div>
      <div class="toolbar-right">
        <span v-if="pistonHealth" class="status-indicator healthy">
          <i class="fas fa-check-circle"></i> Piston 就绪
        </span>
        <span v-else class="status-indicator unhealthy">
          <i class="fas fa-exclamation-circle"></i> Piston 未连接
        </span>
      </div>
    </div>

    <Splitpanes class="playground-splitpanes" horizontal>
      <Pane class="editor-pane" :size="60">
        <div ref="editorContainer" class="monaco-editor-container"></div>
      </Pane>
      <Pane class="output-pane" :size="40">
        <div class="output-container">
          <div class="output-header">
            <span class="output-title">输出</span>
          </div>
          <div class="output-content">
            <div v-if="executionResult?.compile || executionResult?.run" class="result-output">
              <div v-if="executionResult.compile?.stdout" class="stdout">
                <div class="output-label">编译输出:</div>
                <pre>{{ executionResult.compile.stdout }}</pre>
              </div>
              <div v-if="executionResult.compile?.stderr" class="stderr">
                <div class="output-label">编译错误:</div>
                <pre>{{ executionResult.compile.stderr }}</pre>
              </div>
              <div v-if="executionResult.run?.stdout" class="stdout">
                <div class="output-label">标准输出:</div>
                <pre>{{ executionResult.run.stdout }}</pre>
              </div>
              <div v-if="executionResult.run?.stderr" class="stderr">
                <div class="output-label">标准错误:</div>
                <pre>{{ executionResult.run.stderr }}</pre>
              </div>
              <div v-if="executionResult.run" class="exit-info">
                <span class="exit-code">退出码: {{ executionResult.run.code }}</span>
                <span v-if="executionResult.run.signal" class="signal">信号: {{ executionResult.run.signal }}</span>
              </div>
            </div>
            <div v-else-if="errorMessage" class="error-message">
              <i class="fas fa-exclamation-triangle"></i>
              <span>{{ errorMessage }}</span>
            </div>
            <div v-else class="empty-output">
              <i class="fas fa-terminal"></i>
              <span>点击"运行"执行代码</span>
            </div>
          </div>
          <div class="stdin-container">
            <label for="stdin-input">标准输入:</label>
            <textarea
              id="stdin-input"
              v-model="stdinInput"
              placeholder="输入运行时需要的标准输入..."
              class="stdin-input"
            ></textarea>
          </div>
        </div>
      </Pane>
    </Splitpanes>

    <div class="playground-statusbar">
      <span class="status-item">语言: {{ selectedLanguage }}</span>
      <span class="status-item">版本: {{ currentVersion }}</span>
      <span class="status-item">剩余执行次数: {{ remainingRequests }}/30</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import * as monaco from 'monaco-editor'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import { useThemeStore } from '@/stores/theme'
import { API_ENDPOINTS } from '@/config/api'
import request from '@/utils/request'

// 修复 Monaco Editor Web Worker 报错
window.MonacoEnvironment = {
  getWorkerUrl: (workerId, label) => {
    const code = `
      self.MonacoEnvironment = { baseUrl: '${import.meta.env.BASE_URL}' };
      importScripts('${import.meta.env.BASE_URL}node_modules/monaco-editor/min/vs/base/worker/workerMain.js');
    `;
    return `data:text/javascript;charset=utf-8,${encodeURIComponent(code)}`;
  }
};

const themeStore = useThemeStore()
const editorContainer = ref(null)
let editor = null

const selectedLanguage = ref('python')
const stdinInput = ref('')
const isRunning = ref(false)
const pistonHealth = ref(false)
const availableLanguages = ref([])
const executionResult = ref(null)
const errorMessage = ref('')
const remainingRequests = ref(30)

const languageTemplates = {
  python: 'print("Hello, World!")',
  javascript: 'console.log("Hello, World!");',
  java: 'public class Main { public static void main(String[] args) { System.out.println("Hello, World!"); } }',
  c: '#include <stdio.h>\nint main() { printf("Hello, World!\\n"); return 0; }',
  'c++': '#include <iostream>\nint main() { std::cout << "Hello, World!" << std::endl; return 0; }'
}

const languageMonacoMap = {
  python: 'python',
  javascript: 'javascript',
  java: 'java',
  c: 'c',
  'c++': 'cpp'
}

const currentVersion = computed(() => {
  const lang = availableLanguages.value.find(l => l.language === selectedLanguage.value)
  return lang ? lang.version : '-'
})

const getEditorTheme = () => {
  return themeStore.isDarkMode ? 'vs-dark' : 'vs'
}

const handleLanguageChange = () => {
  const template = languageTemplates[selectedLanguage.value] || ''
  if (editor) {
    editor.setValue(template)
    monaco.editor.setModelLanguage(editor.getModel(), languageMonacoMap[selectedLanguage.value] || 'plaintext')
  }
}

const executeCode = async () => {
  if (isRunning.value || !editor) return

  isRunning.value = true
  errorMessage.value = ''
  executionResult.value = null

  try {
    const code = editor.getValue()
    const response = await request.post(API_ENDPOINTS.playground.execute, {
      language: selectedLanguage.value,
      code: code,
      stdin: stdinInput.value
    })

    executionResult.value = response.data
    remainingRequests.value = response.remaining || Math.max(0, remainingRequests.value - 1)
  } catch (error) {
    if (error.response?.status === 429) {
      errorMessage.value = '执行次数已达上限，请稍后再试'
    } else {
      errorMessage.value = error.response?.data?.message || error.message || '执行失败'
    }
  } finally {
    isRunning.value = false
  }
}

const checkPistonHealth = async () => {
  try {
    await request.get(API_ENDPOINTS.playground.health)
    pistonHealth.value = true
  } catch {
    pistonHealth.value = false
  }
}

const fetchRuntimes = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.playground.runtimes)
    availableLanguages.value = response || []
    if (availableLanguages.value.length > 0 && !availableLanguages.value.find(l => l.language === selectedLanguage.value)) {
      selectedLanguage.value = availableLanguages.value[0].language
    }
  } catch (error) {
    console.error('Failed to fetch runtimes:', error)
  }
}

const handleKeydown = (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    executeCode()
  }
}

onMounted(async () => {
  await checkPistonHealth()
  await fetchRuntimes()

  editor = monaco.editor.create(editorContainer.value, {
    value: languageTemplates[selectedLanguage.value] || '',
    language: languageMonacoMap[selectedLanguage.value] || 'plaintext',
    theme: getEditorTheme(),
    automaticLayout: true,
    minimap: { enabled: true },
    fontSize: 14,
    lineNumbers: 'on',
    scrollBeyondLastLine: false
  })

  document.addEventListener('keydown', handleKeydown)

  watch(() => themeStore.isDarkMode, () => {
    if (editor) {
      monaco.editor.setTheme(getEditorTheme())
    }
  })
})

onUnmounted(() => {
  if (editor) {
    editor.dispose()
  }
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.playground-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--bg-primary);
}

.playground-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.language-select {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  outline: none;
}

.language-select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.run-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.run-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.run-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.shortcut-hint {
  font-size: 12px;
  color: var(--text-tertiary);
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.status-indicator.healthy {
  color: #4caf50;
}

.status-indicator.unhealthy {
  color: #f44336;
}

.playground-splitpanes {
  flex: 1;
  overflow: hidden;
}

.editor-pane,
.output-pane {
  overflow: hidden;
}

.monaco-editor-container {
  width: 100%;
  height: 100%;
}

.output-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-secondary);
}

.output-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
}

.output-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.output-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.result-output {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.output-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.stdout pre,
.stderr pre {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.stdout pre {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.stderr pre {
  background-color: rgba(239, 68, 68, 0.1);
  color: #f44336;
}

.exit-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-secondary);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: 8px;
  background-color: rgba(239, 68, 68, 0.1);
  color: #f44336;
}

.empty-output {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-tertiary);
  gap: 12px;
}

.empty-output i {
  font-size: 32px;
}

.stdin-container {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
}

.stdin-container label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.stdin-input {
  width: 100%;
  min-height: 80px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
}

.stdin-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.playground-statusbar {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 20px;
  background-color: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-secondary);
}

.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
