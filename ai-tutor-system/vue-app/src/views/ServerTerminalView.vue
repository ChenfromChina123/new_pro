<template>
  <div class="server-terminal-view">
    <h1 class="page-title">多服务器终端控制</h1>

    <!-- 移动端折叠按钮 -->
    <div class="top-actions" v-if="isMobile">
      <button class="mobile-toggle" @click="toggleServers">
        <span v-if="showServers">📋 隐藏服务器列表</span>
        <span v-else>📋 显示服务器列表</span>
      </button>
      <button class="add-server-btn" @click="toggleAddForm">
        ➕ 添加服务器
      </button>
    </div>

    <!-- PC 端添加服务器按钮 -->
    <button class="add-server-btn-pc" @click="toggleAddForm" v-else>
      ➕ 添加服务器
    </button>

    <!-- 添加服务器弹窗 -->
    <div class="modal-overlay" v-if="showAddForm" @click.self="toggleAddForm">
      <div class="modal-content">
        <div class="modal-header">
          <h2>添加服务器</h2>
          <button class="close-btn" @click="toggleAddForm">✕</button>
        </div>
        <form @submit.prevent="addServerAndClose">
          <div class="form-group">
            <label>服务器名称</label>
            <input v-model="newServer.name" type="text" placeholder="可选，如：生产服务器 1" />
          </div>
          <div class="form-group">
            <label>主机地址</label>
            <input v-model="newServer.host" type="text" placeholder="如：192.168.1.100" required />
          </div>
          <div class="form-group">
            <label>用户名</label>
            <input v-model="newServer.user" type="text" placeholder="如：root" required />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input v-model="newServer.password" type="password" placeholder="服务器密码" required />
          </div>
          <div class="form-group">
            <label>端口</label>
            <input v-model="newServer.port" type="number" placeholder="22" required />
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="toggleAddForm">取消</button>
            <button type="submit" class="btn btn-primary">添加</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 服务器列表区域 -->
    <div class="content-container" :class="{ 'servers-hidden': !showServers && isMobile }">
      <div class="servers-section">
        <div class="servers-list">
          <h2>服务器列表</h2>
          <div
            v-for="server in servers"
            :key="server.id"
            :class="['server-item', { selected: selectedServer === server.id, connected: connectedServers.includes(server.id) }]"
            @click="selectServer(server.id)"
          >
            <div class="server-info">
              <h3>{{ server.name || server.host }}</h3>
              <p>{{ server.host }}:{{ server.port }}</p>
              <p>用户：{{ server.username }}</p>
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
                @click.stop="openNewTerminal(server.id)"
                class="btn btn-secondary"
              >
                新开窗口
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
            @keyup="handleKeyUp"
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

// 移动端折叠控制
const showServers = ref(true)
const showAddForm = ref(false) // 默认隐藏添加表单
const isMobile = ref(false)

// 检测设备类型
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 切换服务器列表显示
const toggleServers = () => {
  showServers.value = !showServers.value
}

// 切换添加表单显示
const toggleAddForm = () => {
  showAddForm.value = !showAddForm.value
}

// WebSocket 连接
let ws = null
const wsConnected = ref(false)
// 防止 Ctrl 键重复触发
let ctrlKeyPressed = false

// API 基础 URL（Spring Boot 后端）
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

// 添加服务器并关闭弹窗
const addServerAndClose = async () => {
  await addServer()
  toggleAddForm()
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
const selectServer = (serverId, keepOutput = false) => {
  selectedServer.value = serverId
  // 只有在切换服务器或明确指定时才清空输出
  if (!keepOutput) {
    terminalOutput.value = ''
    currentCommand.value = ''
  }
  // 聚焦终端
  nextTick(() => {
    if (terminalContainer.value) {
      terminalContainer.value.focus()
    }
  })
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

// 打开新终端窗口
const openNewTerminal = (serverId) => {
  const url = `${window.location.origin}/terminal/${serverId}`
  window.open(url, '_blank', 'width=1200,height=800')
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

  // 特殊处理：Ctrl+V 应该执行粘贴操作，不阻止默认行为
  if (event.ctrlKey && event.key.toLowerCase() === 'v') {
    // 允许浏览器默认行为（粘贴）
    return
  }

  // 阻止默认行为（Enter、方向键、Ctrl 组合键）
  if (event.key === 'Enter' || event.key === 'ArrowUp' || event.key === 'ArrowDown' || event.ctrlKey) {
    event.preventDefault()
  }

  // 处理 Ctrl 组合键（只在第一次按下时触发）
  if (event.ctrlKey && !ctrlKeyPressed) {
    ctrlKeyPressed = true

    switch (event.key.toLowerCase()) {
      case 'c':
        // Ctrl+C - 中断当前命令
        sendControlCharacter('\x03')
        appendOutput('^C\r\n')
        currentCommand.value = ''
        break

      case 'l':
        // Ctrl+L - 清屏
        sendControlCharacter('\x0c')
        terminalOutput.value = ''
        currentCommand.value = ''
        break

      case 'u':
        // Ctrl+U - 删除整行
        sendControlCharacter('\x15')
        currentCommand.value = ''
        break

      case 'a':
        // Ctrl+A - 光标移到行首
        sendControlCharacter('\x01')
        break

      case 'e':
        // Ctrl+E - 光标移到行尾
        sendControlCharacter('\x05')
        break

      case 'w':
        // Ctrl+W - 删除前一个单词
        sendControlCharacter('\x17')
        // 删除前一个单词
        const lastSpaceIndex = currentCommand.value.trimEnd().lastIndexOf(' ')
        if (lastSpaceIndex === -1) {
          currentCommand.value = ''
        } else {
          currentCommand.value = currentCommand.value.substring(0, lastSpaceIndex + 1)
        }
        break

      case 'k':
        // Ctrl+K - 删除从光标到行尾
        sendControlCharacter('\x0b')
        currentCommand.value = ''
        break

      case 'r':
        // Ctrl+R - 搜索历史命令（简单实现：使用上一条命令）
        sendControlCharacter('\x12')
        if (commandHistory.value.length > 0) {
          commandHistoryIndex.value = commandHistory.value.length - 1
          currentCommand.value = commandHistory.value[commandHistoryIndex.value]
        }
        break

      case 'z':
        // Ctrl+Z - 挂起当前进程
        sendControlCharacter('\x1a')
        appendOutput('^Z\r\n')
        currentCommand.value = ''
        break

      case 'd':
        // Ctrl+D - 发送 EOF（End Of File）
        sendControlCharacter('\x04')
        break

      case 'h':
        // Ctrl+H - 删除前一个字符（退格）
        sendControlCharacter('\x08')
        if (currentCommand.value.length > 0) {
          currentCommand.value = currentCommand.value.slice(0, -1)
        }
        break

      case 't':
        // Ctrl+T - 交换光标前的两个字符
        sendControlCharacter('\x14')
        if (currentCommand.value.length >= 2) {
          const chars = currentCommand.value.split('')
          const temp = chars[chars.length - 1]
          chars[chars.length - 1] = chars[chars.length - 2]
          chars[chars.length - 2] = temp
          currentCommand.value = chars.join('')
        }
        break

      default:
        // 其他 Ctrl 组合键，不发送任何字符（避免单独按 Ctrl 时发送）
        break
    }
    return
  }

  // 处理普通按键
  if (event.key === 'Enter') {
    // console.log('发送命令:', currentCommand.value)
    // 发送命令，不需要本地回显（SSH 服务器会回显）
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
    // 普通字符输入
    currentCommand.value += event.key
    // console.log('输入字符:', event.key, '当前命令:', currentCommand.value)
  }
}

// 处理键盘释放
const handleKeyUp = (event) => {
  if (event.key === 'Control') {
    ctrlKeyPressed = false
  }
}

// 发送控制字符到 SSH
const sendControlCharacter = (char) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(`input:${char}`)
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
  const content = ansiToHtml(terminalOutput.value) + '<span class="prompt">$</span><span class="cursor">' + currentCommand.value + '</span><span class="cursor-blink">_</span>'

  // 确保终端容器保持焦点
  nextTick(() => {
    if (terminalContainer.value && document.activeElement !== terminalContainer.value) {
      terminalContainer.value.focus()
    }
    // 滚动到底部
    if (terminalContainer.value) {
      terminalContainer.value.scrollTop = terminalContainer.value.scrollHeight
    }
  })

  return content
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
onMounted(async () => {
  await fetchServers()
  // 检测设备类型
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

// 组件卸载时移除监听器
onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  disconnectWebSocket()
})

// 组件卸载时断开WebSocket
onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<style scoped>
.server-terminal-view {
  padding: 20px;
  max-width: 100%;
  overflow-x: hidden;
}

.page-title {
  color: #333;
  margin-bottom: 30px;
  font-size: 24px;
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
  flex-direction: column;
  gap: 10px;
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
  font-size: 16px;
}

.server-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.server-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  white-space: nowrap;
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
  background: #2196F3;
  color: white;
}

.btn-secondary:hover {
  background: #0b7dda;
}

.add-server-form {
  background: white;
  padding: 20px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.add-server-form h3 {
  margin: 0 0 15px 0;
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
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.terminal-section {
  background: #1e1e1e;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 500px;
}

.terminal-header {
  background: #2d2d2d;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: #f0f0f0;
}

.terminal-header h2 {
  margin: 0;
  color: #f0f0f0;
  font-size: 18px;
}

.terminal-status {
  display: flex;
  align-items: center;
  gap: 10px;
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
  padding: 20px;
  text-align: center;
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
  -webkit-overflow-scrolling: touch;
  color: #f0f0f0;
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

/* 移动端适配 */
.mobile-toggle {
  display: none;
  width: 100%;
  padding: 12px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-bottom: 15px;
  transition: all 0.3s ease;
}

.mobile-toggle:hover {
  background: #0b7dda;
}

/* 添加服务器表单折叠按钮 */
.toggle-form-btn {
  width: 100%;
  padding: 12px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  margin-bottom: 15px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.toggle-form-btn:hover {
  background: #45a049;
}

.toggle-form-btn.active {
  background: #f44336;
}

.toggle-form-btn.active:hover {
  background: #da190b;
}

/* 顶部操作栏（移动端） */
.top-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.top-actions .mobile-toggle {
  flex: 1;
  margin-bottom: 0;
}

/* 添加服务器按钮（PC 端） */
.add-server-btn-pc {
  width: 100%;
  padding: 12px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-bottom: 15px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.add-server-btn-pc:hover {
  background: #45a049;
}

/* 移动端添加服务器按钮 */
.add-server-btn {
  flex: 1;
  padding: 12px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.add-server-btn:hover {
  background: #45a049;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.modal-actions {
  display: flex;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
  justify-content: flex-end;
}

.modal-actions .btn {
  min-width: 80px;
}

@media (max-width: 768px) {
  .mobile-toggle {
    display: block;
  }

  .server-terminal-view {
    padding: 10px;
  }

  .page-title {
    font-size: 20px;
    margin-bottom: 15px;
  }

  .content-container {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .content-container.servers-hidden .servers-section {
    display: none;
  }

  .content-container.servers-hidden .terminal-section {
    grid-row: 1;
  }

  .servers-section {
    padding: 15px;
    order: 2;
  }

  .server-item {
    padding: 12px;
  }

  .server-info h3 {
    font-size: 15px;
  }

  .server-info p {
    font-size: 13px;
  }

  .server-actions {
    gap: 6px;
  }

  .btn {
    padding: 6px 12px;
    font-size: 13px;
  }

  .add-server-form {
    padding: 15px;
  }

  .add-server-form h3 {
    font-size: 16px;
  }

  .form-group input {
    padding: 8px;
    font-size: 13px;
  }

  .terminal-section {
    min-height: 60vh;
    order: 1;
  }

  .terminal-header {
    padding: 12px 15px;
  }

  .terminal-header h2 {
    font-size: 16px;
  }

  .terminal-output {
    padding: 15px;
    font-size: 13px;
    line-height: 1.5;
  }
}

@media (max-width: 480px) {
  .server-terminal-view {
    padding: 8px;
  }

  .page-title {
    font-size: 18px;
    margin-bottom: 10px;
  }

  .mobile-toggle {
    padding: 10px;
    font-size: 14px;
  }

  .servers-section {
    padding: 12px;
  }

  .server-item {
    padding: 10px;
  }

  .server-info h3 {
    font-size: 14px;
  }

  .server-info p {
    font-size: 12px;
  }

  .server-actions {
    gap: 4px;
  }

  .btn {
    padding: 5px 10px;
    font-size: 12px;
  }

  .terminal-section {
    min-height: 70vh;
  }

  .terminal-header {
    padding: 10px 12px;
  }

  .terminal-header h2 {
    font-size: 15px;
  }

  .terminal-output {
    padding: 12px;
    font-size: 12px;
    line-height: 1.4;
  }
}
</style>
