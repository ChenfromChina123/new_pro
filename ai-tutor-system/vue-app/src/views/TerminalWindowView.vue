<template>
  <div class="terminal-window">
    <div class="terminal-header">
      <h2>{{ serverInfo ? `${serverInfo.name} (${serverInfo.host})` : '终端' }}</h2>
      <div class="header-actions">
        <span :class="['status', wsConnected ? 'connected' : 'disconnected']">
          {{ wsConnected ? '已连接' : '未连接' }}
        </span>
        <button @click="disconnectAndClose" class="btn-danger">关闭窗口</button>
      </div>
    </div>

    <div class="terminal-container">
      <div v-if="!wsConnected && !connecting" class="terminal-placeholder">
        <p>正在连接服务器...</p>
      </div>

      <div
        ref="terminalContainer"
        class="terminal-output"
        tabindex="0"
        @click="focusTerminal"
        @keydown="handleKeyDown"
        @keyup="handleKeyUp"
      >
        <div v-html="getTerminalContent()"></div>
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

let ws = null
const wsConnected = ref(false)
let ctrlKeyPressed = false

const API_BASE = '/api/server-terminal'

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
  const wsUrl = `ws://localhost:5000/ws/terminal/${serverId.value}`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('WebSocket 连接已建立')
    wsConnected.value = true
    connecting.value = false
    appendOutput('正在连接服务器...\r\n')

    const userId = authStore.user?.id || '1'
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
})

onUnmounted(() => {
  disconnectWebSocket()
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
</style>
