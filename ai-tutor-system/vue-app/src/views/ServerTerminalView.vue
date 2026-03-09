<template>
  <div class="server-terminal-view">
    <h1 class="page-title">多服务器终端控制</h1>

    <div class="content-container">
      <!-- 服务器列表 -->
      <div class="servers-section">
        <h2>服务器列表</h2>
        <div class="servers-list">
          <div
            v-for="server in servers"
            :key="server.id"
            class="server-item"
            :class="{ 'selected': selectedServer === server.id, 'connected': connectedServers.includes(server.id) }"
            @click="selectServer(server.id)"
          >
            <div class="server-info">
              <h3>{{ server.serverName }}</h3>
              <p>{{ server.host }}:{{ server.port }}</p>
              <p>用户: {{ server.username }}</p>
            </div>
            <div class="server-actions">
              <button
                v-if="!connectedServers.includes(server.id)"
                @click.stop="connectServer(server.id)"
                class="btn btn-primary"
              >
                连接
              </button>
              <button
                v-else
                @click.stop="disconnectServer(server.id)"
                class="btn btn-danger"
              >
                断开
              </button>
              <button
                @click.stop="deleteServer(server.id)"
                class="btn btn-secondary"
              >
                删除
              </button>
            </div>
          </div>
        </div>

        <!-- 添加服务器 -->
        <div class="add-server-form">
          <h3>添加服务器</h3>
          <form @submit.prevent="addServer">
            <div class="form-group">
              <label>服务器名称</label>
              <input type="text" v-model="newServer.name" required>
            </div>
            <div class="form-group">
              <label>主机地址</label>
              <input type="text" v-model="newServer.host" required>
            </div>
            <div class="form-group">
              <label>用户名</label>
              <input type="text" v-model="newServer.user" required>
            </div>
            <div class="form-group">
              <label>密码</label>
              <input type="password" v-model="newServer.password" required>
            </div>
            <div class="form-group">
              <label>端口</label>
              <input type="number" v-model="newServer.port" required>
            </div>
            <button type="submit" class="btn btn-success">添加</button>
          </form>
        </div>
      </div>

      <!-- 终端控制 -->
      <div class="terminal-section">
        <h2>交互式终端</h2>
        <div v-if="selectedServer" class="terminal-wrapper">
          <div class="terminal-header">
            <span>当前服务器：{{ getServerName(selectedServer) }}</span>
            <span v-if="wsConnected" class="connection-status connected">已连接</span>
            <span v-else class="connection-status disconnected">未连接</span>
          </div>
          <div
            ref="terminalContainer"
            class="terminal-output"
            tabindex="0"
            @click="focusTerminal"
            @keydown="handleKeyDown"
          >
            <div v-html="getTerminalContent()"></div>
          </div>
        </div>
        <div v-else class="no-server-selected">
          请选择一个服务器进行连接
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// 服务器列表
const servers = ref([])
// 已连接的服务器
const connectedServers = ref([])
// 新服务器表单
const newServer = ref({ name: '', host: '', user: '', password: '', port: 22 })
// 选中的服务器
const selectedServer = ref(null)

// 终端相关
const terminalContainer = ref(null)
const terminalInput = ref(null)
const currentCommand = ref('')
const commandHistory = ref([])
const commandHistoryIndex = ref(-1)
const terminalOutput = ref('')

// WebSocket连接
let ws = null
const wsConnected = ref(false)

// API基础URL（Spring Boot后端）
const API_BASE = '/api/server-terminal'

// 获取服务器列表
const fetchServers = async () => {
  try {
    const response = await request.get(`${API_BASE}/servers`)
    servers.value = response.data || []
  } catch (error) {
    console.error('获取服务器列表失败:', error)
  }
}

// 添加服务器
const addServer = async () => {
  try {
    const formData = new FormData()
    formData.append('serverName', newServer.value.name)
    formData.append('host', newServer.value.host)
    formData.append('username', newServer.value.user)
    formData.append('password', newServer.value.password)
    formData.append('port', newServer.value.port)

    const response = await request.post(`${API_BASE}/servers`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (response.data) {
      servers.value.push(response.data)
      newServer.value = { name: '', host: '', user: '', password: '', port: 22 }
    }
  } catch (error) {
    console.error('添加服务器失败:', error)
  }
}

// 删除服务器
const deleteServer = async (serverId) => {
  try {
    const response = await request.delete(`${API_BASE}/servers/${serverId}`)
    if (response.code === 200) {
      servers.value = servers.value.filter(s => s.id !== serverId)
      connectedServers.value = connectedServers.value.filter(id => id !== serverId)
      if (selectedServer.value === serverId) {
        selectedServer.value = null
        disconnectWebSocket()
      }
    }
  } catch (error) {
    console.error('删除服务器失败:', error)
  }
}

// 选择服务器
const selectServer = (serverId) => {
  selectedServer.value = serverId
  terminalOutput.value = ''
  currentCommand.value = ''
}

// 连接服务器（通过 WebSocket）
const connectServer = async (serverId) => {
  try {
    const response = await request.post(`${API_BASE}/servers/${serverId}/connect`)
    if (response.code === 200) {
      if (!connectedServers.value.includes(serverId)) {
        connectedServers.value.push(serverId)
      }
      // 自动选择该服务器
      selectServer(serverId)
      // 连接 WebSocket
      connectWebSocket(serverId)
    } else {
      alert('连接失败：' + response.message)
    }
  } catch (error) {
    console.error('连接服务器失败:', error)
    alert('连接失败：' + error.message)
  }
}

// 断开服务器
const disconnectServer = async (serverId) => {
  try {
    disconnectWebSocket()
    const response = await request.post(`${API_BASE}/servers/${serverId}/disconnect`)
    if (response.code === 200) {
      connectedServers.value = connectedServers.value.filter(id => id !== serverId)
    }
  } catch (error) {
    console.error('断开服务器失败:', error)
  }
}

// WebSocket 连接（使用普通 WebSocket 连接到 Spring Boot 后端）
const connectWebSocket = (serverId) => {
  // 连接到 Spring Boot 后端的 WebSocket（端口 5000）
  const wsUrl = `ws://localhost:5000/ws/terminal/${serverId}`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('WebSocket 连接已建立')
    wsConnected.value = true
    appendOutput('正在连接服务器...\r\n')

    // 发送连接命令
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
      // 检查输出是否以换行符结尾，如果不是，说明是提示符，需要特殊处理
      if (!output.endsWith('\n') && !output.endsWith('\r')) {
        // 这是提示符，追加输出并在下一行添加输入提示符
        appendOutput(output)
      } else {
        appendOutput(output)
      }
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
    appendOutput(`连接错误\r\n`)
  }

  ws.onclose = () => {
    console.log('WebSocket 连接已关闭')
    wsConnected.value = false
    appendOutput('\r\n连接已关闭\r\n')
  }
}

// 断开 WebSocket
const disconnectWebSocket = () => {
  if (ws) {
    ws.send('disconnect')
    ws.close()
    ws = null
    wsConnected.value = false
  }
}

// 发送命令
const sendCommand = () => {
  if (!currentCommand.value || !wsConnected.value) return

  const command = currentCommand.value

  // 添加到命令历史
  commandHistory.value.push(command)
  commandHistoryIndex.value = commandHistory.value.length

  // 发送命令到服务器
  ws.send(`input:${command}\n`)

  // 清空输入
  currentCommand.value = ''
}

// 上一条命令
const previousCommand = () => {
  if (commandHistoryIndex.value > 0) {
    commandHistoryIndex.value--
    currentCommand.value = commandHistory.value[commandHistoryIndex.value]
  }
}

// 下一条命令
const nextCommand = () => {
  if (commandHistoryIndex.value < commandHistory.value.length - 1) {
    commandHistoryIndex.value++
    currentCommand.value = commandHistory.value[commandHistoryIndex.value]
  } else {
    commandHistoryIndex.value = commandHistory.value.length
    currentCommand.value = ''
  }
}

// 处理键盘输入
const handleKeyDown = (event) => {
  // console.log('键盘事件:', event.key, 'wsConnected:', wsConnected.value)

  if (!wsConnected.value) {
    // console.log('WebSocket 未连接，忽略输入')
    return
  }

  // 阻止默认行为
  if (event.key === 'Enter' || event.key === 'ArrowUp' || event.key === 'ArrowDown') {
    event.preventDefault()
  }

  if (event.key === 'Enter') {
    // console.log('发送命令:', currentCommand.value)
    // 追加当前命令到输出
    if (currentCommand.value) {
      appendOutput(`${currentCommand.value}\r\n`)
    }
    sendCommand()
  } else if (event.key === 'ArrowUp') {
    previousCommand()
  } else if (event.key === 'ArrowDown') {
    nextCommand()
  } else if (event.key === 'Backspace') {
    if (currentCommand.value.length > 0) {
      currentCommand.value = currentCommand.value.slice(0, -1)
    }
  } else if (event.key.length === 1 && !event.ctrlKey && !event.altKey && !event.metaKey) {
    // 普通字符输入
    currentCommand.value += event.key
    // console.log('输入字符:', event.key, '当前命令:', currentCommand.value)
  }
}

// 将 ANSI 转义序列转换为 HTML 样式
const ansiToHtml = (text) => {
  if (!text) return text

  let html = text
    // 转义 HTML 特殊字符
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // ANSI 颜色代码映射
  const colorMap = {
    '30': '#000000', // 黑色
    '31': '#cd3131', // 红色
    '32': '#0dbc79', // 绿色
    '33': '#e5e510', // 黄色
    '34': '#2472c8', // 蓝色
    '35': '#bc3fbc', // 紫色
    '36': '#11a8cd', // 青色
    '37': '#e5e5e5', // 白色
    '90': '#666666', // 亮黑色
    '91': '#f14c4c', // 亮红色
    '92': '#23d18b', // 亮绿色
    '93': '#f5f543', // 亮黄色
    '94': '#3b8eea', // 亮蓝色
    '95': '#d670d6', // 亮紫色
    '96': '#29b8db', // 亮青色
    '97': '#ffffff'  // 亮白色
  }

  // 处理 ANSI 转义序列
  html = html.replace(/\x1b\[([0-9;]*)m/g, (match, codes) => {
    const codeArray = codes.split(';')
    const styles = []
    let color = null
    let bgColor = null

    for (const code of codeArray) {
      if (code === '0') {
        // 重置所有样式
        return '</span>'
      } else if (code === '1') {
        styles.push('font-weight: bold')
      } else if (code === '3' || code === '4') {
        // 斜体或下划线（可选）
        styles.push(`font-style: ${code === '3' ? 'italic' : 'underline'}`)
      } else if (code.startsWith('3') || code.startsWith('9')) {
        // 前景色
        color = colorMap[code]
      } else if (code.startsWith('4')) {
        // 背景色
        const bgCode = code.replace('4', '')
        const bgMap = {
          '0': '#000000', '1': '#cd3131', '2': '#0dbc79', '3': '#e5e510',
          '4': '#2472c8', '5': '#bc3fbc', '6': '#11a8cd', '7': '#e5e5e5'
        }
        bgColor = bgMap[bgCode]
      }
    }

    if (color) {
      styles.push(`color: ${color}`)
    }
    if (bgColor) {
      styles.push(`background-color: ${bgColor}`)
    }

    if (styles.length > 0) {
      return `<span style="${styles.join(';')}">`
    }
    return '<span>'
  })

  // 确保所有 span 标签都正确关闭
  const openSpans = (html.match(/<span/g) || []).length
  const closeSpans = (html.match(/<\/span>/g) || []).length
  if (openSpans > closeSpans) {
    html += '</span>'.repeat(openSpans - closeSpans)
  }

  return html
}

// 获取终端内容（包含输入提示符）
const getTerminalContent = () => {
  if (!wsConnected.value) {
    return ansiToHtml(terminalOutput.value)
  }
  // 在输出后面追加输入提示符和当前命令
  return ansiToHtml(terminalOutput.value) + '<span class="prompt">$</span><span class="cursor">' + currentCommand.value + '</span><span class="cursor-blink">_</span>'
}

// 追加输出
const appendOutput = (text) => {
  terminalOutput.value += text
  nextTick(() => {
    if (terminalContainer.value) {
      terminalContainer.value.scrollTop = terminalContainer.value.scrollHeight
    }
  })
}

// 聚焦终端输入框
const focusTerminal = () => {
  if (terminalContainer.value) {
    terminalContainer.value.focus()
  }
}

// 根据ID获取服务器名称
const getServerName = (serverId) => {
  const server = servers.value.find(s => s.id === serverId)
  return server ? server.serverName : '未知服务器'
}

// 初始化
onMounted(() => {
  fetchServers()
})

// 组件卸载时断开WebSocket
onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<style scoped>
.server-terminal-view {
  padding: 20px;
}

.page-title {
  color: #333;
  margin-bottom: 30px;
}

.content-container {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 30px;
}

.servers-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.servers-list {
  margin-bottom: 30px;
}

.server-item {
  background: white;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.server-item:hover {
  transform: translateX(5px);
}

.server-item.selected {
  border: 2px solid #4CAF50;
}

.server-item.connected {
  border-left: 4px solid #4CAF50;
}

.server-info h3 {
  margin: 0 0 5px 0;
  color: #333;
}

.server-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.server-actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-primary {
  background: #4CAF50;
  color: white;
}

.btn-primary:hover {
  background: #45a049;
}

.btn-danger {
  background: #f44336;
  color: white;
}

.btn-danger:hover {
  background: #da190b;
}

.btn-secondary {
  background: #9e9e9e;
  color: white;
}

.btn-secondary:hover {
  background: #757575;
}

.btn-success {
  background: #2196F3;
  color: white;
}

.btn-success:hover {
  background: #0b7dda;
}

.add-server-form {
  background: white;
  padding: 20px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.add-server-form h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #666;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.terminal-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.terminal-wrapper {
  background: #1e1e1e;
  border-radius: 6px;
  overflow: hidden;
  height: 500px;
  display: flex;
  flex-direction: column;
}

.terminal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: #2d2d2d;
  color: #ccc;
  font-size: 14px;
}

.connection-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.connection-status.connected {
  background: #4CAF50;
  color: white;
}

.connection-status.disconnected {
  background: #f44336;
  color: white;
}

.terminal-output {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  color: #f0f0f0;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
  outline: none;
  cursor: text;
}

.terminal-output:focus {
  outline: none;
}

.terminal-input-inline {
  display: inline-flex;
  align-items: center;
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
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.no-server-selected {
  background: white;
  padding: 40px;
  border-radius: 6px;
  text-align: center;
  color: #666;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .content-container {
    grid-template-columns: 1fr;
  }
}
</style>
