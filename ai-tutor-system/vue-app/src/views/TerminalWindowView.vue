<template>
  <div class="terminal-window">
    <div class="terminal-header">
      <h2>{{ serverInfo ? `${serverInfo.name} (${serverInfo.host})` : '终端' }}</h2>
      <div class="header-actions">
        <span :class="['status', wsConnected ? 'connected' : 'disconnected']">
          {{ wsConnected ? '已连接' : '未连接' }}
        </span>
        <button
          class="btn-danger"
          @click="disconnectAndClose"
        >
          关闭窗口
        </button>
      </div>
    </div>

    <div class="terminal-container">
      <div
        v-if="!wsConnected && !connecting"
        class="terminal-placeholder"
      >
        <p>正在连接服务器...</p>
      </div>

      <div
        ref="terminalContainer"
        class="terminal-output"
        tabindex="0"
        @click="focusTerminal"
        @keydown="handleKeyDown"
        @keyup="handleKeyUp"
        @paste="handlePaste"
        @contextmenu.prevent="handleContextMenu"
      >
        <div v-html="getTerminalContent()" />
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

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
    >
      <div
        class="menu-item"
        @click="copyContent"
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <rect
            x="9"
            y="9"
            width="13"
            height="13"
            rx="2"
            ry="2"
          />
          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
        </svg>
        <span>复制</span>
      </div>
      <div class="menu-divider" />
      <div
        class="menu-item"
        @click="selectAll"
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M9 11l3 3L22 4" />
          <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
        </svg>
        <span>全选</span>
      </div>
      <div class="menu-divider" />
      <div
        class="menu-item"
        @click="clearScreen"
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
        <span>清屏</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const serverId = ref(route.params.serverId)
const serverInfo = ref(null)
const connecting = ref(true)

const terminalContainer = ref(null)
const currentCommand = ref('')
const commandHistory = ref([])
const commandHistoryIndex = ref(-1)
const terminalOutput = ref('')

// 粘贴确认相关
const showPasteModal = ref(false)
const pasteBuffer = ref('')

// 右键菜单相关
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0
})

let ws = null
const wsConnected = ref(false)
let ctrlKeyPressed = false

const API_BASE = '/api/server-terminal'

const getTerminalWsUrl = (id) => {
  const wsBaseFromEnv = import.meta.env.VITE_WS_BASE_URL
  if (wsBaseFromEnv) {
    const normalizedBase = wsBaseFromEnv
      .replace(/^http:\/\//, 'ws://')
      .replace(/^https:\/\//, 'wss://')
      .replace(/\/$/, '')
    return `${normalizedBase}/ws/terminal/${id}`
  }
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${wsProtocol}//${window.location.host}/ws/terminal/${id}`
}

const fetchServerInfo = async () => {
  try {
    const response = await request.get(`${API_BASE}/servers`)
    if (response.code === 200 && response.data) {
      serverInfo.value = response.data.find(s => s.id === parseInt(serverId.value))
    }
  } catch (error) {
    console.error('获取服务器信息失败:', error)
  }
}

const connectServer = async () => {
  try {
    const response = await request.post(`${API_BASE}/servers/${serverId.value}/connect`)
    if (response.code === 200) {
      connectWebSocket()
    } else {
      alert('连接失败：' + response.message)
      connecting.value = false
    }
  } catch (error) {
    console.error('连接服务器失败:', error)
    alert('连接失败：' + error.message)
    connecting.value = false
  }
}

const connectWebSocket = () => {
  const wsUrl = getTerminalWsUrl(serverId.value)

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('WebSocket 连接已建立')
    wsConnected.value = true
    connecting.value = false
    appendOutput('正在连接服务器...\r\n')

    const userId = authStore.userId || authStore.userInfo?.id || 'unknown'
    ws.send(`connect:${userId}`)
  }

  ws.onmessage = (event) => {
    const message = event.data
    console.log('收到 WebSocket 消息:', message)

    if (message.startsWith('connected:')) {
      const msg = message.substring(10)
      appendOutput(msg + '\r\n')
    } else if (message.startsWith('output:')) {
      const output = message.substring(7)
      appendOutput(output)
    } else if (message.startsWith('error:')) {
      const error = message.substring(6)
      appendOutput(`错误：${error}\r\n`)
    } else if (message.startsWith('disconnected:')) {
      const msg = message.substring(13)
      appendOutput(msg + '\r\n')
    }
  }

  ws.onerror = (error) => {
    console.error('WebSocket 错误:', error)
    appendOutput('连接错误\r\n')
    connecting.value = false
  }

  ws.onclose = () => {
    console.log('WebSocket 连接已关闭')
    wsConnected.value = false
    connecting.value = false
  }
}

const disconnectWebSocket = () => {
  if (ws) {
    ws.close()
    ws = null
  }
  wsConnected.value = false
}

const disconnectAndClose = async () => {
  try {
    disconnectWebSocket()
    await request.post(`${API_BASE}/servers/${serverId.value}/disconnect`)
    window.close()
    setTimeout(() => {
      router.push('/terminal')
    }, 100)
  } catch (error) {
    console.error('断开服务器失败:', error)
    window.close()
  }
}

const sendMessage = (message) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(message)
  }
}

const sendCommand = () => {
  if (currentCommand.value.trim()) {
    commandHistory.value.push(currentCommand.value)
    commandHistoryIndex.value = commandHistory.value.length
  }
  sendMessage(`input:${currentCommand.value}\n`)
  currentCommand.value = ''
}

const sendControlCharacter = (char) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(`input:${char}`)
  }
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

const ansiToHtml = (text) => {
  if (!text) return text

  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

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
    let bgColor = null

    for (const code of codeArray) {
      if (code === '0') {
        return '</span>'
      } else if (code === '1') {
        styles.push('font-weight: bold')
      } else if (code.startsWith('3') || code.startsWith('9')) {
        color = colorMap[code]
      } else if (code.startsWith('4')) {
        const bgCode = code.replace('4', '')
        const bgMap = {
          '0': '#000000', '1': '#cd3131', '2': '#0dbc79', '3': '#e5e510',
          '4': '#2472c8', '5': '#bc3fbc', '6': '#11a8cd', '7': '#e5e5e5'
        }
        bgColor = bgMap[bgCode]
      }
    }

    if (color) styles.push(`color: ${color}`)
    if (bgColor) styles.push(`background-color: ${bgColor}`)

    if (styles.length > 0) {
      return `<span style="${styles.join(';')}">`
    }
    return '<span>'
  })

  const openSpans = (html.match(/<span/g) || []).length
  const closeSpans = (html.match(/<\/span>/g) || []).length
  if (openSpans > closeSpans) {
    html += '</span>'.repeat(openSpans - closeSpans)
  }

  return html
}

const getTerminalContent = () => {
  if (!wsConnected.value) {
    return ansiToHtml(terminalOutput.value)
  }
  const content = ansiToHtml(terminalOutput.value) + '<span class="prompt">$</span><span class="cursor">' + currentCommand.value + '</span><span class="cursor-blink">_</span>'

  nextTick(() => {
    if (terminalContainer.value && document.activeElement !== terminalContainer.value) {
      terminalContainer.value.focus()
    }
    if (terminalContainer.value) {
      terminalContainer.value.scrollTop = terminalContainer.value.scrollHeight
    }
  })

  return content
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
  if (terminalContainer.value) {
    terminalContainer.value.focus()
  }
}

/**
 * 处理粘贴事件
 * @param {ClipboardEvent} event - 粘贴事件对象
 */
const handlePaste = (event) => {
  event.preventDefault()
  const text = (event.clipboardData || window.clipboardData).getData('text')
  if (!text) return

  // 如果是多行或者内容较长，弹出确认框
  if (text.includes('\n') || text.length > 20) {
    pasteBuffer.value = text
    showPasteModal.value = true
  } else {
    // 短文本直接输入
    currentCommand.value += text
  }
}

/**
 * 确认粘贴操作
 */
const confirmPaste = () => {
  if (pasteBuffer.value) {
    // 发送粘贴内容到服务器
    sendMessage(`input:${pasteBuffer.value}`)

    // 如果粘贴内容包含换行，通常意味着执行了命令，清空当前输入行
    if (pasteBuffer.value.includes('\n')) {
      currentCommand.value = ''
    }

    // 在终端显示粘贴的内容
    appendOutput(pasteBuffer.value.replace(/\n/g, '\r\n'))
  }
  showPasteModal.value = false
  pasteBuffer.value = ''
  focusTerminal()
}

/**
 * 处理右键菜单
 */
const handleContextMenu = (event) => {
  event.preventDefault()
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY
  }
}

/**
 * 隐藏右键菜单
 */
const hideContextMenu = () => {
  contextMenu.value.visible = false
}

/**
 * 复制内容
 */
const copyContent = async () => {
  try {
    const selection = window.getSelection()
    const selectedText = selection.toString()

    if (selectedText) {
      // 如果有选中的文本，直接复制
      await navigator.clipboard.writeText(selectedText)
    } else {
      // 如果没有选中，复制全部终端内容
      await navigator.clipboard.writeText(terminalOutput.value)
    }

    // 显示复制成功提示
    const toast = document.createElement('div')
    toast.textContent = '已复制到剪贴板'
    toast.style.cssText = `
      position: fixed;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(0, 0, 0, 0.8);
      color: white;
      padding: 8px 16px;
      border-radius: 4px;
      font-size: 14px;
      z-index: 10000;
    `
    document.body.appendChild(toast)
    setTimeout(() => document.body.removeChild(toast), 2000)
  } catch (error) {
    console.error('复制失败:', error)
    alert('复制失败：' + error.message)
  }
  hideContextMenu()
}

/**
 * 全选
 */
const selectAll = () => {
  const range = document.createRange()
  range.selectNode(terminalContainer.value)
  const selection = window.getSelection()
  selection.removeAllRanges()
  selection.addRange(range)
  hideContextMenu()
}

/**
 * 清屏
 */
const clearScreen = () => {
  terminalOutput.value = ''
  currentCommand.value = ''
  hideContextMenu()
}

const handleKeyDown = (event) => {
  if (!wsConnected.value) return

  if (event.key === 'Enter' || event.key === 'ArrowUp' || event.key === 'ArrowDown' || event.ctrlKey) {
    event.preventDefault()
  }

  if (event.ctrlKey && !ctrlKeyPressed) {
    ctrlKeyPressed = true

    if (event.key.toLowerCase() === 'v') {
      return
    }

    event.preventDefault()

    switch (event.key.toLowerCase()) {
      case 'c':
        sendControlCharacter('\x03')
        appendOutput('^C\r\n')
        currentCommand.value = ''
        break
      case 'l':
        sendControlCharacter('\x0c')
        terminalOutput.value = ''
        currentCommand.value = ''
        break
      case 'u':
        sendControlCharacter('\x15')
        currentCommand.value = ''
        break
      case 'a':
        sendControlCharacter('\x01')
        break
      case 'e':
        sendControlCharacter('\x05')
        break
      case 'w':
        sendControlCharacter('\x17')
        const lastSpaceIndex = currentCommand.value.trimEnd().lastIndexOf(' ')
        if (lastSpaceIndex === -1) {
          currentCommand.value = ''
        } else {
          currentCommand.value = currentCommand.value.substring(0, lastSpaceIndex + 1)
        }
        break
      case 'k':
        sendControlCharacter('\x0b')
        currentCommand.value = ''
        break
      case 'r':
        sendControlCharacter('\x12')
        if (commandHistory.value.length > 0) {
          commandHistoryIndex.value = commandHistory.value.length - 1
          currentCommand.value = commandHistory.value[commandHistoryIndex.value]
        }
        break
      case 'z':
        sendControlCharacter('\x1a')
        appendOutput('^Z\r\n')
        currentCommand.value = ''
        break
      case 'd':
        sendControlCharacter('\x04')
        break
      case 'h':
        sendControlCharacter('\x08')
        if (currentCommand.value.length > 0) {
          currentCommand.value = currentCommand.value.slice(0, -1)
        }
        break
      case 't':
        sendControlCharacter('\x14')
        if (currentCommand.value.length >= 2) {
          const chars = currentCommand.value.split('')
          const temp = chars[chars.length - 1]
          chars[chars.length - 1] = chars[chars.length - 2]
          chars[chars.length - 2] = temp
          currentCommand.value = chars.join('')
        }
        break
    }
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

const handleKeyUp = (event) => {
  if (event.key === 'Control') {
    ctrlKeyPressed = false
  }
}

onMounted(async () => {
  await fetchServerInfo()
  await connectServer()
  document.addEventListener('click', hideContextMenu)
})

onUnmounted(() => {
  disconnectWebSocket()
  document.removeEventListener('click', hideContextMenu)
})
</script>

<style scoped>
.terminal-window {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  color: #f0f0f0;
}

.terminal-header {
  background: #2d2d2d;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #3e3e3e;
}

.terminal-header h2 {
  margin: 0;
  font-size: 18px;
  color: #f0f0f0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status {
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}

.status.connected {
  background: #4CAF50;
  color: white;
}

.status.disconnected {
  background: #f44336;
  color: white;
}

.btn-danger {
  padding: 8px 16px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-danger:hover {
  background: #d32f2f;
}

.terminal-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.terminal-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
  font-size: 18px;
}

.terminal-output {
  flex: 1;
  background: #1e1e1e;
  padding: 20px;
  font-family: 'Courier New', 'Monaco', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  overflow-y: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  outline: none;
}

.terminal-output:focus {
  outline: none;
}

.prompt {
  color: #4CAF50;
  font-weight: bold;
  margin-right: 8px;
}

.cursor {
  color: #f0f0f0;
}

.cursor-blink {
  color: #4CAF50;
  animation: blink 1s step-end infinite;
  font-weight: bold;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.terminal-output::-webkit-scrollbar {
  width: 8px;
}

.terminal-output::-webkit-scrollbar-track {
  background: #2d2d2d;
}

.terminal-output::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 4px;
}

.terminal-output::-webkit-scrollbar-thumb:hover {
  background: #666;
}

/* 粘贴确认对话框样式 */
.paste-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.paste-modal {
  background: #ffffff;
  width: 600px;
  max-width: 90vw;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.paste-modal-header {
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #333;
  font-weight: bold;
}

.paste-modal-body {
  padding: 15px;
}

.paste-modal-body textarea {
  width: 100%;
  height: 250px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  padding: 10px;
  font-family: 'Courier New', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  resize: none;
  outline: none;
  background: #fdfdfd;
  box-sizing: border-box;
}

.paste-modal-body textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.paste-modal-footer {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #e9ecef;
}

.paste-modal-footer .hint {
  color: #6c757d;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 10px;
}

.btn-confirm {
  background: #007bff;
  color: white;
  border: none;
  padding: 6px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.btn-confirm:hover {
  background: #0056b3;
}

.btn-cancel {
  background: #6c757d;
  color: white;
  border: none;
  padding: 6px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.btn-cancel:hover {
  background: #5a6268;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.close-btn:hover {
  color: #333;
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: #2d2d2d;
  border: 1px solid #3e3e3e;
  border-radius: 6px;
  padding: 6px 0;
  min-width: 180px;
  z-index: 10000;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.15s;
  color: #f0f0f0;
}

.menu-item:hover {
  background: #3e3e3e;
}

.menu-item svg {
  flex-shrink: 0;
}

.menu-divider {
  height: 1px;
  background: #3e3e3e;
  margin: 6px 0;
}
</style>
