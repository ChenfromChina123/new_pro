<template>
  <div class="terminal-tab-content">
    <header class="terminal-toolbar">
      <div class="server-info">
        <span class="tag">SSH</span>
        <code>{{ serverName }}</code>
        <span
          class="status-badge"
          :class="{ connected: wsConnected }"
        >
          {{ wsConnected ? '已连接' : '未连接' }}
        </span>
      </div>
      <div class="ctrl-group">
        <button
          v-if="!wsConnected"
          class="btn-connect"
          @click="reconnect"
        >
          重连
        </button>
        <button
          v-else
          class="btn-disconnect"
          @click="disconnect"
        >
          断开
        </button>
      </div>
    </header>

    <div
      ref="terminalContainer"
      class="xterm-wrapper"
      tabindex="0"
      :style="{ fontSize: terminalFontSize + 'px' }"
      @keydown="handleKeyDown"
      @keyup="handleKeyUp"
      @wheel="handleWheel"
      @touchstart="handleTouchStart"
      @touchmove="handleTouchMove"
      @paste="handlePaste"
      @contextmenu.prevent="handleContextMenu"
    >
      <div
        class="terminal-content"
        v-html="getTerminalContent()"
      />
    </div>

    <!-- 自定义右键菜单 -->
    <div
      v-if="showContextMenu"
      class="custom-context-menu"
      :style="{ top: menuY + 'px', left: menuX + 'px' }"
    >
      <div
        class="menu-item"
        @click="handleMenuCopy"
      >
        复制 (Copy)
      </div>
      <div
        class="menu-item"
        @click="handleMenuPaste"
      >
        粘贴 (Paste)
      </div>
      <div class="menu-separator" />
      <div
        class="menu-item"
        @click="clearTerminal"
      >
        清屏 (Clear)
      </div>
    </div>

    <!-- 粘贴确认对话框 -->
    <div
      v-if="showPasteModal"
      class="paste-modal-overlay"
    >
      <div class="paste-modal">
        <div class="paste-modal-header">
          <span>检测到粘贴内容</span>
          <button
            class="close-btn"
            @click="showPasteModal = false"
          >
            &times;
          </button>
        </div>
        <div class="paste-modal-body">
          <textarea
            v-model="pasteBuffer"
            placeholder="在这里编辑要粘贴的内容..."
          />
        </div>
        <div class="paste-modal-footer">
          <span class="hint">可以检查内容后点击粘贴</span>
          <div class="actions">
            <button
              class="btn-confirm"
              @click="confirmPaste"
            >
              粘贴
            </button>
            <button
              class="btn-cancel"
              @click="showPasteModal = false"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  serverId: {
    type: Number,
    required: true
  },
  serverName: {
    type: String,
    default: 'Server'
  },
  initialPath: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['status-change'])

const authStore = useAuthStore()

// 终端相关
const terminalContainer = ref(null)
const currentCommand = ref('')
const commandHistory = ref([])
const commandHistoryIndex = ref(-1)
const terminalOutput = ref('')
const terminalFontSize = ref(13)

// 移动端双指缩放相关
let initialTouchDistance = 0
let initialFontSizeAtTouch = 0

// 右键菜单相关
const showContextMenu = ref(false)
const menuX = ref(0)
const menuY = ref(0)

// 粘贴确认相关
const showPasteModal = ref(false)
const pasteBuffer = ref('')

// WebSocket 连接
let ws = null
const wsConnected = ref(false)
let ctrlKeyPressed = false
// 自动导航状态标记
const hasAutoNavigated = ref(false)
const autoNavWaitingPrompt = ref(false)
const autoNavPendingCommand = ref('')
const autoNavExpectedPath = ref('')
const autoNavAwaitingPwd = ref(false)

// API 基础 URL
const API_BASE = '/api/server-terminal'

const escapePathForShell = (path) => {
  return String(path || '').replace(/(["\\$`])/g, '\\$1')
}

const normalizeOutputText = (text) => {
  return String(text || '')
    .replace(/\x1b\[[0-9;]*m/g, '')
    .replace(/\r/g, '')
}

const pushAutoNavOutput = (text) => {
  appendOutput(`[AutoNav] ${text}\r\n`)
}

const getTerminalWsUrl = (serverId) => {
  const wsBaseFromEnv = import.meta.env.VITE_WS_BASE_URL
  if (wsBaseFromEnv) {
    const normalizedBase = wsBaseFromEnv
      .replace(/^http:\/\//, 'ws://')
      .replace(/^https:\/\//, 'wss://')
      .replace(/\/$/, '')
    return `${normalizedBase}/ws/terminal/${serverId}`
  }
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${wsProtocol}//${window.location.host}/ws/terminal/${serverId}`
}

const sendAutoNavCommand = (reason) => {
  if (!ws || ws.readyState !== WebSocket.OPEN || !autoNavExpectedPath.value) return
  const escapedPath = escapePathForShell(autoNavExpectedPath.value)
  pushAutoNavOutput(`触发发送(${reason})`)
  appendOutput(`cd "${escapedPath}"\r\npwd\r\n`)
  ws.send(`input:cd "${escapedPath}"\r`)
  ws.send('input:pwd\r')
  autoNavWaitingPrompt.value = false
  autoNavAwaitingPwd.value = true
}

// 监听连接状态变化
watch(wsConnected, (val) => {
  emit('status-change', { serverId: props.serverId, connected: val })
})

/**
 * 处理滚轮缩放终端字体大小
 */
const handleWheel = (event) => {
  if (event.ctrlKey) {
    event.preventDefault()
    const delta = event.deltaY > 0 ? -1 : 1
    const newSize = terminalFontSize.value + delta
    if (newSize >= 10 && newSize <= 40) {
      terminalFontSize.value = newSize
    }
  }
}

/**
 * 触摸相关处理
 */
const getDistance = (touch1, touch2) => {
  return Math.hypot(touch2.pageX - touch1.pageX, touch2.pageY - touch1.pageY)
}

const handleTouchStart = (event) => {
  if (event.touches.length === 2) {
    initialTouchDistance = getDistance(event.touches[0], event.touches[1])
    initialFontSizeAtTouch = terminalFontSize.value
  }
}

const handleTouchMove = (event) => {
  if (event.touches.length === 2) {
    event.preventDefault()
    const currentDistance = getDistance(event.touches[0], event.touches[1])
    const scale = currentDistance / initialTouchDistance
    let nextSize = Math.round(initialFontSizeAtTouch * scale)
    if (nextSize >= 10 && nextSize <= 40) {
      terminalFontSize.value = nextSize
    }
  }
}

/**
 * 粘贴处理
 */
const handlePaste = (event) => {
  event.preventDefault()
  const text = (event.clipboardData || window.clipboardData).getData('text')
  if (!text) return
  processPasteData(text)
}

const processPasteData = (text) => {
  if (text.includes('\n') || text.length > 20) {
    pasteBuffer.value = text
    showPasteModal.value = true
  } else {
    currentCommand.value += text
  }
}

const confirmPaste = () => {
  if (pasteBuffer.value) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(`input:${pasteBuffer.value}`)
    }
    if (pasteBuffer.value.includes('\n')) {
      currentCommand.value = ''
    }
    appendOutput(pasteBuffer.value.replace(/\n/g, '\r\n'))
  }
  showPasteModal.value = false
  pasteBuffer.value = ''
  focusTerminal()
}

/**
 * 右键菜单
 */
const handleContextMenu = (event) => {
  showContextMenu.value = true
  menuX.value = event.clientX
  menuY.value = event.clientY
}

const closeContextMenu = () => {
  showContextMenu.value = false
}

const handleMenuCopy = () => {
  const selectedText = window.getSelection().toString()
  if (selectedText) {
    navigator.clipboard.writeText(selectedText)
  }
  closeContextMenu()
}

const handleMenuPaste = async () => {
  closeContextMenu()
  try {
    const text = await navigator.clipboard.readText()
    if (text) processPasteData(text)
  } catch {
    alert('无法访问剪贴板，请使用 Ctrl+V')
  }
}

const clearTerminal = () => {
  terminalOutput.value = ''
  currentCommand.value = ''
  closeContextMenu()
}

/**
 * 核心终端逻辑
 */
const connect = async () => {
  try {
    // 先尝试建立后端连接
    const response = await request.post(`${API_BASE}/servers/${props.serverId}/connect`)
    if (response.code === 200) {
      connectWebSocket()
    } else {
      appendOutput(`连接初始化失败：${response.message}\r\n`)
    }
  } catch (error) {
    appendOutput(`连接初始化错误：${error.message}\r\n`)
  }
}

const reconnect = () => {
  disconnectWebSocket()
  connect()
}

const disconnect = async () => {
  try {
    disconnectWebSocket()
    await request.post(`${API_BASE}/servers/${props.serverId}/disconnect`)
  } catch (error) {
    console.error('断开失败:', error)
  }
}

const connectWebSocket = () => {
  const wsUrl = getTerminalWsUrl(props.serverId)
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    wsConnected.value = true
    appendOutput('正在连接服务器...\r\n')
    const userId = authStore.userId || authStore.userInfo?.id || 'unknown'
    ws.send(`connect:${userId}`)
  }

  ws.onmessage = (event) => {
    const message = event.data
    if (message.startsWith('connected:')) {
      appendOutput(message.substring(10) + '\r\n')
      if (props.initialPath) {
        hasAutoNavigated.value = false
        autoNavExpectedPath.value = props.initialPath
        const escapedPath = escapePathForShell(props.initialPath)
        autoNavPendingCommand.value = `cd "${escapedPath}"`
        autoNavWaitingPrompt.value = true
        autoNavAwaitingPwd.value = false
        pushAutoNavOutput(`已挂起命令，等待首个提示符: ${autoNavPendingCommand.value}`)
        sendAutoNavCommand('connected')
      }
    } else if (message.startsWith('output:')) {
      const content = message.substring(7)
      appendOutput(content)
      const cleanText = normalizeOutputText(content)
      if (props.initialPath && !hasAutoNavigated.value) {
        console.log('[AutoNav] 输出片段:', JSON.stringify(cleanText))
      }
      if (autoNavWaitingPrompt.value && ws && ws.readyState === WebSocket.OPEN) {
        const hasPrompt = /[$#>]\s*$/.test(cleanText) || /[$#>]\s/.test(cleanText.slice(-80))
        if (hasPrompt) {
          sendAutoNavCommand('prompt')
        }
      }
      if (autoNavAwaitingPwd.value && autoNavExpectedPath.value) {
        if (cleanText.includes(autoNavExpectedPath.value)) {
          hasAutoNavigated.value = true
          autoNavAwaitingPwd.value = false
          pushAutoNavOutput(`目录确认成功: ${autoNavExpectedPath.value}`)
        }
      }
    } else if (message.startsWith('error:')) {
      appendOutput(`错误：${message.substring(6)}\r\n`)
    } else if (message.startsWith('disconnected:')) {
      appendOutput(message.substring(13) + '\r\n')
      hasAutoNavigated.value = false
      autoNavWaitingPrompt.value = false
      autoNavAwaitingPwd.value = false
      autoNavPendingCommand.value = ''
      autoNavExpectedPath.value = ''
    }
  }

  ws.onerror = () => appendOutput(`连接错误\r\n`)

  ws.onclose = () => {
    wsConnected.value = false
    appendOutput('\r\n连接已关闭\r\n')
  }
}

const disconnectWebSocket = () => {
  if (ws) {
    ws.send('disconnect')
    ws.close()
    ws = null
    wsConnected.value = false
  }
}

const sendCommand = () => {
  if (!currentCommand.value || !wsConnected.value) return
  const command = currentCommand.value
  commandHistory.value.push(command)
  commandHistoryIndex.value = commandHistory.value.length
  ws.send(`input:${command}\n`)
  currentCommand.value = ''
}

const previousCommand = () => {
  if (commandHistoryIndex.value > 0) {
    commandHistoryIndex.value--
    currentCommand.value = commandHistory.value[commandHistoryIndex.value]
  }
}

const nextCommand = () => {
  if (commandHistoryIndex.value < commandHistory.value.length - 1) {
    commandHistoryIndex.value++
    currentCommand.value = commandHistory.value[commandHistoryIndex.value]
  } else {
    commandHistoryIndex.value = commandHistory.value.length
    currentCommand.value = ''
  }
}

const handleKeyDown = (event) => {
  if (!wsConnected.value) return

  if (event.ctrlKey && event.key.toLowerCase() === 'v') return

  if (['Enter', 'ArrowUp', 'ArrowDown'].includes(event.key) || event.ctrlKey) {
    event.preventDefault()
  }

  if (event.ctrlKey && !ctrlKeyPressed) {
    ctrlKeyPressed = true
    handleCtrlKey(event.key.toLowerCase())
    return
  }

  if (event.key === 'Enter') {
    sendCommand()
  } else if (event.key === 'ArrowUp') {
    previousCommand()
  } else if (event.key === 'ArrowDown') {
    nextCommand()
  } else if (event.key === 'Backspace') {
    if (currentCommand.value.length > 0) {
      currentCommand.value = currentCommand.value.slice(0, -1)
    }
  } else if (event.key.length === 1 && !event.altKey && !event.metaKey) {
    currentCommand.value += event.key
  }
}

const handleCtrlKey = (key) => {
  const ctrlMap = {
    'c': '\x03', 'l': '\x0c', 'u': '\x15', 'a': '\x01', 'e': '\x05',
    'w': '\x17', 'k': '\x0b', 'r': '\x12', 'z': '\x1a', 'd': '\x04',
    'h': '\x08', 't': '\x14'
  }

  if (ctrlMap[key]) {
    sendControlCharacter(ctrlMap[key])

    // 特殊处理前端显示逻辑
    if (key === 'c') {
      appendOutput('^C\r\n')
      currentCommand.value = ''
    } else if (key === 'l') {
      terminalOutput.value = ''
      currentCommand.value = ''
    } else if (key === 'u') {
      currentCommand.value = ''
    } else if (key === 'w') {
      const lastSpace = currentCommand.value.trimEnd().lastIndexOf(' ')
      currentCommand.value = lastSpace === -1 ? '' : currentCommand.value.substring(0, lastSpace + 1)
    } else if (key === 'k') {
      currentCommand.value = ''
    } else if (key === 'h') {
      if (currentCommand.value.length > 0) currentCommand.value = currentCommand.value.slice(0, -1)
    }
  }
}

const handleKeyUp = (event) => {
  if (event.key === 'Control') ctrlKeyPressed = false
}

const sendControlCharacter = (char) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(`input:${char}`)
  }
}

const ansiToHtml = (text) => {
  if (!text) return text
  let html = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  const colorMap = {
    '30': '#000000', '31': '#cd3131', '32': '#0dbc79', '33': '#e5e510',
    '34': '#2472c8', '35': '#bc3fbc', '36': '#11a8cd', '37': '#e5e5e5',
    '90': '#666666', '91': '#f14c4c', '92': '#23d18b', '93': '#f5f543',
    '94': '#3b8eea', '95': '#d670d6', '96': '#29b8db', '97': '#ffffff'
  }

  html = html.replace(/\x1b\[([0-9;]*)m/g, (match, codes) => {
    const codeArray = codes.split(';')
    const styles = []
    let color = null

    for (const code of codeArray) {
      if (code === '0') return '</span>'
      if (code === '1') styles.push('font-weight: bold')
      if (code === '3') styles.push('font-style: italic')
      if (code === '4') styles.push('text-decoration: underline')
      if (colorMap[code]) color = colorMap[code]
    }

    if (color) styles.push(`color: ${color}`)
    return styles.length > 0 ? `<span style="${styles.join(';')}">` : '<span>'
  })

  const openSpans = (html.match(/<span/g) || []).length
  const closeSpans = (html.match(/<\/span>/g) || []).length
  if (openSpans > closeSpans) html += '</span>'.repeat(openSpans - closeSpans)

  return html
}

const getTerminalContent = () => {
  if (!wsConnected.value) return ansiToHtml(terminalOutput.value)
  return ansiToHtml(terminalOutput.value) +
    '<span class="cursor">' +
    currentCommand.value +
    '</span><span class="cursor-blink">_</span>'
}

const appendOutput = (text) => {
  terminalOutput.value += text
  nextTick(() => {
    if (terminalContainer.value) {
      terminalContainer.value.scrollTop = terminalContainer.value.scrollHeight
    }
  })
}

const focusTerminal = () => {
  if (terminalContainer.value) terminalContainer.value.focus()
}

onMounted(() => {
  window.addEventListener('click', closeContextMenu)
  connect()
})

onUnmounted(() => {
  window.removeEventListener('click', closeContextMenu)
  disconnectWebSocket()
})
</script>

<style scoped>
.terminal-tab-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  background-color: #0f1115;
}

.terminal-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #181a1f;
  border-bottom: 1px solid #2d3139;
  flex-shrink: 0;
}

.server-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.tag {
  background: #3b82f6;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
}

.status-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #ef4444;
  color: white;
}

.status-badge.connected {
  background: #10b981;
}

.ctrl-group button {
  padding: 4px 12px;
  border-radius: 4px;
  border: none;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-connect {
  background: #10b981;
  color: white;
}

.btn-disconnect {
  background: #ef4444;
  color: white;
}

.xterm-wrapper {
  flex: 1;
  background-color: #000;
  padding: 10px;
  overflow-y: auto;
  outline: none;
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  color: #d4d4d4;
  line-height: 1.4;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.prompt {
  color: #10b981;
  margin-right: 8px;
}

.cursor-blink {
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 粘贴确认弹窗 */
.paste-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.paste-modal {
  background: #1e222a;
  border: 1px solid #2d3139;
  border-radius: 8px;
  width: 400px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.paste-modal-header {
  padding: 12px 16px;
  border-bottom: 1px solid #2d3139;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.paste-modal-body {
  padding: 16px;
}

.paste-modal-body textarea {
  width: 100%;
  height: 120px;
  background: #0f1115;
  border: 1px solid #2d3139;
  color: #e2e8f0;
  padding: 8px;
  border-radius: 4px;
  resize: none;
  font-family: monospace;
}

.paste-modal-footer {
  padding: 12px 16px;
  border-top: 1px solid #2d3139;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #94a3b8;
}

.actions {
  display: flex;
  gap: 8px;
}

.actions button {
  padding: 6px 12px;
  border-radius: 4px;
  border: none;
  font-size: 12px;
  cursor: pointer;
}

.btn-confirm {
  background: #3b82f6;
  color: white;
}

.btn-cancel {
  background: #4b5563;
  color: white;
}

/* 自定义右键菜单 */
.custom-context-menu {
  position: fixed;
  background: #1e222a;
  border: 1px solid #2d3139;
  border-radius: 6px;
  padding: 4px 0;
  min-width: 160px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.menu-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #e2e8f0;
  cursor: pointer;
}

.menu-item:hover {
  background: #3b82f6;
  color: white;
}

.menu-separator {
  height: 1px;
  background: #2d3139;
  margin: 4px 0;
}
</style>
