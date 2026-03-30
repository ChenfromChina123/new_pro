<template>
  <div class="agent-sandbox-simple">
    <!-- 主聊天区域 -->
    <main class="chat-main">
      <div class="chat-header">
        <div class="chat-header-inner">
          <div class="header-left">
            <h2 class="chat-title">
              <i class="fas fa-robot" />
              AI Agent 助手
            </h2>
          </div>
        </div>
      </div>

      <div
        ref="messagesContainer"
        class="messages-container"
      >
        <!-- 空状态 -->
        <div
          v-if="messages.length === 0"
          class="empty-state"
        >
          <h3 class="empty-title">
            AI Agent 助手
          </h3>
          <p class="empty-description">
            选择功能并输入指令，让 AI Agent 帮你完成任务。
          </p>
          <div class="quick-examples">
            <button
              class="example-btn"
              @click="quickStart('分析当前目录下的代码结构')"
            >
              <i class="fas fa-search" />
              分析代码结构
            </button>
            <button
              class="example-btn"
              @click="quickStart('创建一个简单的 Express 后端')"
            >
              <i class="fas fa-server" />
              创建 Express 后端
            </button>
            <button
              class="example-btn"
              @click="quickStart('帮我优化这个项目的性能')"
            >
              <i class="fas fa-rocket" />
              优化项目性能
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message"
          :class="message.role === 'user' ? 'user' : 'assistant'"
        >
          <div class="message-content">
            <div class="message-bubble">
              <!-- 用户消息 -->
              <div v-if="message.role === 'user'" class="message-text">
                {{ message.content }}
              </div>

              <!-- AI 消息 -->
              <template v-else>
                <!-- 工具调用状态 -->
                <div
                  v-if="message.toolStatusList && message.toolStatusList.length > 0"
                  class="tool-status-container"
                >
                  <div
                    v-for="(toolStatus, toolIndex) in message.toolStatusList"
                    :key="toolIndex"
                    class="tool-status-block"
                    :class="{
                      'streaming': toolStatus.status === 'processing',
                      'done': toolStatus.status === 'done',
                      'error': toolStatus.status === 'error'
                    }"
                  >
                    <div class="tool-status-header">
                      <div class="header-left">
                        <div class="header-text">
                          <i
                            v-if="toolStatus.status === 'processing'"
                            class="fas fa-circle-notch fa-spin"
                          />
                          <i
                            v-else-if="toolStatus.status === 'done'"
                            class="fas fa-check-circle"
                          />
                          <i
                            v-else
                            class="fas fa-exclamation-circle"
                          />
                          <span class="tool-name">{{ getToolTypeName(toolStatus.toolType) }}</span>
                          <span class="tool-message">{{ toolStatus.message }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 终端输出 -->
                <div
                  v-if="message.terminalOutput && message.terminalOutput.length > 0"
                  class="terminal-output"
                >
                  <div class="terminal-header">
                    <span class="terminal-title">
                      <i class="fas fa-terminal" />
                      终端输出
                    </span>
                  </div>
                  <div class="terminal-body">
                    <pre>{{ message.terminalOutput }}</pre>
                  </div>
                </div>

                <!-- AI 响应内容 -->
                <div
                  v-if="message.content"
                  class="message-text"
                  v-html="formatMessage(message.content)"
                />

                <!-- 流式输入状态 -->
                <div
                  v-if="message.isStreaming && !message.content"
                  class="message-text"
                >
                  <span class="typing-cursor" />
                </div>
              </template>
            </div>
            <div class="message-time">
              {{ formatTime(message.timestamp) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="input-container">
          <!-- 功能选择器 -->
          <div class="function-selector">
            <select v-model="selectedFunction" class="function-select">
              <option value="general">通用任务</option>
              <option value="code_edit">代码编辑</option>
              <option value="file_search">文件搜索</option>
              <option value="terminal">终端命令</option>
            </select>
          </div>

          <textarea
            v-model="inputMessage"
            class="chat-input"
            placeholder="输入指令，让 AI Agent 帮你完成..."
            :disabled="isLoading"
            rows="1"
            @input="adjustTextareaHeight"
            @keydown.enter.exact.prevent="sendMessage"
          />

          <div class="input-toolbar">
            <button
              v-if="isLoading"
              class="stop-btn"
              title="停止生成"
              @click="stopGeneration"
            >
              <i class="fas fa-stop" />
            </button>
            <button
              v-else
              class="send-btn"
              :disabled="!inputMessage.trim()"
              title="发送"
              @click="sendMessage"
            >
              <i class="fas fa-paper-plane" />
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useAgentStore } from '@/stores/agent'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const agentStore = useAgentStore()

// 状态
const messages = ref([])
const inputMessage = ref('')
const selectedFunction = ref('general')
const isLoading = ref(false)
const messagesContainer = ref(null)

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  breaks: true,
  gfm: true
})

/**
 * 格式化消息内容
 */
const formatMessage = (content) => {
  if (!content) return ''
  return marked.parse(content)
}

/**
 * 获取工具类型名称
 */
const getToolTypeName = (toolType) => {
  const names = {
    'terminal': '终端执行',
    'file_read': '文件读取',
    'file_edit': '文件编辑',
    'file_search': '文件搜索',
    'ls': '目录列表',
    'git': 'Git 操作'
  }
  return names[toolType] || toolType
}

/**
 * 格式化时间
 */
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 调整文本域高度
 */
const adjustTextareaHeight = () => {
  const textarea = document.querySelector('.chat-input')
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
  }
}

/**
 * 滚动到底部
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

/**
 * 快速开始
 */
const quickStart = (content) => {
  inputMessage.value = content
  sendMessage()
}

/**
 * 发送消息
 */
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  const userMessage = {
    role: 'user',
    content: inputMessage.value.trim(),
    timestamp: new Date().toISOString()
  }

  messages.value.push(userMessage)
  const userContent = inputMessage.value.trim()
  inputMessage.value = ''

  // 重置文本域高度
  const textarea = document.querySelector('.chat-input')
  if (textarea) {
    textarea.style.height = 'auto'
  }

  isLoading.value = true
  scrollToBottom()

  // 添加 AI 消息占位
  const aiMessage = {
    role: 'assistant',
    content: '',
    timestamp: new Date().toISOString(),
    isStreaming: true,
    toolStatusList: [],
    terminalOutput: ''
  }
  messages.value.push(aiMessage)
  scrollToBottom()

  try {
    // 这里模拟 AI 响应，实际应该调用 agentStore 的方法
    // 先添加工具状态
    aiMessage.toolStatusList = [
      {
        toolType: selectedFunction.value,
        status: 'processing',
        message: '正在处理...'
      }
    ]
    scrollToBottom()

    // 模拟延迟
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 更新工具状态
    aiMessage.toolStatusList[0].status = 'done'
    aiMessage.toolStatusList[0].message = '处理完成'

    // 添加终端输出（模拟）
    if (selectedFunction.value === 'terminal') {
      aiMessage.terminalOutput = '$ ls -la\n' +
        'total 40\n' +
        'drwxr-xr-x  5 user user 4096 Jan 13 10:00 .\n' +
        'drwxr-xr-x 10 user user 4096 Jan 13 09:00 ..\n' +
        '-rw-r--r--  1 user user  123 Jan 13 10:00 package.json'
    }

    // 添加 AI 响应
    aiMessage.content = `好的，我已经完成了您的"${getToolTypeName(selectedFunction.value)}"任务。\n\n` +
      `**任务摘要：**\n` +
      `- 输入：${userContent}\n` +
      `- 功能：${getToolTypeName(selectedFunction.value)}\n` +
      `- 状态：已完成\n\n` +
      `如需进一步操作，请继续告诉我！`

    aiMessage.isStreaming = false
    scrollToBottom()

  } catch (error) {
    console.error('发送消息失败:', error)
    aiMessage.content = '抱歉，处理失败，请稍后重试。'
    aiMessage.isStreaming = false
    if (aiMessage.toolStatusList.length > 0) {
      aiMessage.toolStatusList[0].status = 'error'
      aiMessage.toolStatusList[0].message = '处理失败'
    }
  } finally {
    isLoading.value = false
  }
}

/**
 * 停止生成
 */
const stopGeneration = () => {
  isLoading.value = false
  // 找到最后一条 AI 消息并停止流式
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.role === 'assistant') {
    lastMessage.isStreaming = false
  }
}

onMounted(() => {
  // 初始化
})
</script>

<style scoped>
.agent-sandbox-simple {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 16px 24px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.chat-header-inner {
  max-width: 900px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
}

.chat-title {
  font-size: 1.25rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
}

.chat-title i {
  color: var(--primary-color);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.empty-state {
  max-width: 700px;
  margin: 80px auto 0;
  text-align: center;
}

.empty-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.empty-description {
  font-size: 1rem;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.quick-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.example-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.example-btn:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateY(-2px);
}

.example-btn i {
  color: var(--primary-color);
}

.message {
  max-width: 900px;
  margin: 0 auto 24px;
  display: flex;
  justify-content: flex-start;
}

.message.user {
  justify-content: flex-end;
}

.message-content {
  max-width: 80%;
  display: flex;
  flex-direction: column;
}

.message.user .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 16px 20px;
  border-radius: 16px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
}

.message.user .message-bubble {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.message-text {
  line-height: 1.6;
  word-wrap: break-word;
}

.message.user .message-text {
  color: white;
}

.message-time {
  font-size: 0.75rem;
  color: var(--text-tertiary);
  margin-top: 6px;
  padding: 0 4px;
}

.message.user .message-time {
  text-align: right;
}

/* 工具状态样式 */
.tool-status-container {
  margin-bottom: 16px;
}

.tool-status-block {
  padding: 12px 16px;
  background-color: var(--bg-tertiary);
  border-radius: 8px;
  margin-bottom: 8px;
  border-left: 3px solid var(--border-color);
}

.tool-status-block.streaming {
  border-left-color: var(--warning-color);
}

.tool-status-block.done {
  border-left-color: var(--success-color);
}

.tool-status-block.error {
  border-left-color: var(--danger-color);
}

.tool-status-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-name {
  font-weight: 600;
  color: var(--text-primary);
}

.tool-message {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

/* 终端输出样式 */
.terminal-output {
  margin-bottom: 16px;
  background-color: #1e1e1e;
  border-radius: 8px;
  overflow: hidden;
}

.terminal-header {
  padding: 8px 12px;
  background-color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.terminal-title {
  color: #999;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 6px;
}

.terminal-body {
  padding: 12px;
}

.terminal-body pre {
  margin: 0;
  color: #d4d4d4;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.85rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 输入区域 */
.chat-input-area {
  padding: 16px 24px;
  background-color: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
}

.input-container {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.function-selector {
  display: flex;
  align-items: center;
}

.function-select {
  padding: 8px 12px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 0.9rem;
  cursor: pointer;
  outline: none;
}

.function-select:focus {
  border-color: var(--primary-color);
}

.chat-input {
  width: 100%;
  padding: 12px 16px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 1rem;
  resize: none;
  outline: none;
  min-height: 48px;
  max-height: 200px;
  line-height: 1.5;
}

.chat-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.chat-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.stop-btn,
.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.stop-btn {
  background-color: var(--danger-color);
  color: white;
}

.stop-btn:hover {
  background-color: #dc2626;
  transform: scale(1.05);
}

.send-btn {
  background-color: var(--primary-color);
  color: white;
}

.send-btn:hover:not(:disabled) {
  background-color: var(--primary-color-dark);
  transform: scale(1.05);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Markdown 内容样式 */
.message-text :deep(p) {
  margin: 0 0 12px 0;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(code) {
  background-color: var(--bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
}

.message-text :deep(pre) {
  background-color: #1e1e1e;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.message-text :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: #d4d4d4;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.message-text :deep(li) {
  margin: 4px 0;
}

/* 打字光标 */
.typing-cursor {
  display: inline-block;
  width: 8px;
  height: 20px;
  background-color: var(--text-primary);
  animation: blink 1s infinite;
  vertical-align: middle;
  margin-left: 4px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}
</style>
