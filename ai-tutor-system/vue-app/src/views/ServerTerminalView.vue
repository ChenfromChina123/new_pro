<template>
  <div class="terminal-app" :class="{ 'light-mode': !isDarkMode }" @click="closeContextMenu">
    <!-- 移动端头部 -->
    <header v-if="isMobile" class="mobile-header">
      <button @click="toggleServers">☰ 列表</button>
      <span class="current-title">{{ getServerName(selectedServer) }}</span>
      <button @click="toggleAddForm">✚ 添加</button>
    </header>

    <!-- 主布局 -->
    <div class="main-layout">
      <!-- 侧边栏 -->
      <aside :class="['sidebar', { 'mobile-hidden': isMobile && !showServers }]">
        <div class="sidebar-header">
          <h2>SERVERS</h2>
          <button class="icon-add-btn" @click="toggleAddForm" title="添加服务器">✚</button>
        </div>

        <nav class="server-list">
          <div
            v-for="server in servers"
            :key="server.id"
            :class="['server-item', { active: selectedServer === server.id }]"
            @click="selectServer(server.id)"
          >
            <div class="status-indicator" :class="{ connected: connectedServers.includes(server.id) }"></div>
            <div class="server-meta">
              <span class="name">{{ server.name || server.host }}</span>
              <span class="addr">{{ server.host }}</span>
            </div>
            <div class="item-actions">
              <button @click.stop="openNewTerminal(server.id)" title="新窗口">↗</button>
              <button @click.stop="deleteServer(server.id)" class="del" title="删除">×</button>
            </div>
          </div>
        </nav>
      </aside>

      <!-- 终端主区域 -->
      <main class="terminal-container">
        <div v-if="selectedServer" class="terminal-view">
          <header class="terminal-toolbar">
            <div class="server-info">
              <span class="tag">SSH</span>
              <code>{{ getServerName(selectedServer) }}</code>
            </div>
            <div class="ctrl-group">
              <button v-if="!connectedServers.includes(selectedServer)" @click="connectServer(selectedServer)" class="btn-connect">连接</button>
              <button v-else @click="disconnectServer(selectedServer)" class="btn-disconnect">断开</button>
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
            <div class="terminal-content" v-html="getTerminalContent()"></div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="placeholder-icon">⌨️</div>
          <p>请从左侧选择一个服务器开始工作</p>
        </div>
      </main>
    </div>

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

    <!-- 自定义右键菜单 -->
    <div
      v-if="showContextMenu"
      class="custom-context-menu"
      :style="{ top: menuY + 'px', left: menuX + 'px' }"
    >
      <div class="menu-item" @click="handleMenuCopy">复制 (Copy)</div>
      <div class="menu-item" @click="handleMenuPaste">粘贴 (Paste)</div>
      <div class="menu-separator"></div>
      <div class="menu-item" @click="clearTerminal">清屏 (Clear)</div>
    </div>

    <!-- 粘贴确认对话框 -->
    <div v-if="showPasteModal" class="paste-modal-overlay">
      <div class="paste-modal">
        <div class="paste-modal-header">
          <span>检测到粘贴内容</span>
          <button @click="showPasteModal = false" class="close-btn">&times;</button>
        </div>
        <div class="paste-modal-body">
          <textarea v-model="pasteBuffer" placeholder="在这里编辑要粘贴的内容..."></textarea>
        </div>
        <div class="paste-modal-footer">
          <span class="hint">可以检查内容后点击粘贴</span>
          <div class="actions">
            <button @click="confirmPaste" class="btn-confirm">粘贴</button>
            <button @click="showPasteModal = false" class="btn-cancel">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const authStore = useAuthStore()
const themeStore = useThemeStore()

// 使用全局主题状态（用于 CSS 类切换）
const isDarkMode = computed(() => themeStore.isDarkMode)

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
const terminalFontSize = ref(13) // 终端字体大小

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

// 移动端折叠控制
const showServers = ref(true)
const showAddForm = ref(false)
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

/**
 * 处理滚轮缩放终端字体大小
 * @param {WheelEvent} event - 滚轮事件对象
 */
const handleWheel = (event) => {
  // 仅当按下 Ctrl 键时触发缩放
  if (event.ctrlKey) {
    // 阻止浏览器默认的全页面缩放行为
    event.preventDefault()

    // 根据滚动方向计算新字号 (向上滚放大，向下滚缩小)
    const delta = event.deltaY > 0 ? -1 : 1
    const newSize = terminalFontSize.value + delta

    // 限制字号范围，防止过大或过小破坏布局 (10px - 40px)
    if (newSize >= 10 && newSize <= 40) {
      terminalFontSize.value = newSize
    }
  }
}

/**
 * 计算两点间的距离（勾股定理）
 * @param {Touch} touch1 - 第一个触摸点
 * @param {Touch} touch2 - 第二个触摸点
 * @returns {number} 两点间的距离
 */
const getDistance = (touch1, touch2) => {
  return Math.hypot(touch2.pageX - touch1.pageX, touch2.pageY - touch1.pageY)
}

/**
 * 处理触摸开始事件
 * @param {TouchEvent} event - 触摸事件对象
 */
const handleTouchStart = (event) => {
  if (event.touches.length === 2) {
    // 记录初始距离和当时的字号
    initialTouchDistance = getDistance(event.touches[0], event.touches[1])
    initialFontSizeAtTouch = terminalFontSize.value
  }
}

/**
 * 处理触摸移动事件（核心缩放逻辑）
 * @param {TouchEvent} event - 触摸事件对象
 */
const handleTouchMove = (event) => {
  if (event.touches.length === 2) {
    // 阻止浏览器默认的缩放行为
    event.preventDefault()

    const currentDistance = getDistance(event.touches[0], event.touches[1])

    // 计算缩放比例 (当前距离 / 初始距离)
    const scale = currentDistance / initialTouchDistance

    // 计算新字号
    let nextSize = Math.round(initialFontSizeAtTouch * scale)

    // 同样限制范围 [10, 40]
    if (nextSize >= 10 && nextSize <= 40) {
      terminalFontSize.value = nextSize
    }
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
  processPasteData(text)
}

/**
 * 处理粘贴数据（公共逻辑）
 * @param {string} text - 要粘贴的文本
 */
const processPasteData = (text) => {
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
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(`input:${pasteBuffer.value}`)
    }

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
 * 处理右键菜单事件
 * @param {MouseEvent} event - 鼠标事件对象
 */
const handleContextMenu = (event) => {
  showContextMenu.value = true
  menuX.value = event.clientX
  menuY.value = event.clientY
}

/**
 * 关闭右键菜单
 */
const closeContextMenu = () => {
  showContextMenu.value = false
}

/**
 * 处理菜单中的复制操作
 */
const handleMenuCopy = () => {
  const selectedText = window.getSelection().toString()
  if (selectedText) {
    navigator.clipboard.writeText(selectedText).then(() => {
      console.log('已复制到剪贴板')
    }).catch(err => {
      console.error('复制失败:', err)
    })
  }
  closeContextMenu()
}

/**
 * 处理菜单中的粘贴操作
 */
const handleMenuPaste = async () => {
  closeContextMenu()
  try {
    const text = await navigator.clipboard.readText()
    if (text) {
      processPasteData(text)
    }
  } catch (err) {
    console.error('访问剪贴板失败:', err)
    alert('浏览器拒绝访问剪贴板，请尝试使用 Ctrl+V')
  }
}

/**
 * 清空终端输出
 */
const clearTerminal = () => {
  terminalOutput.value = ''
  currentCommand.value = ''
  closeContextMenu()
}

// 获取服务器名称
const getServerName = (serverId) => {
  const server = servers.value.find(s => s.id === serverId)
  return server ? (server.name || server.host) : '未选择服务器'
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
/* 引入等宽字体和无衬线字体 */
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&family=Inter:wght@400;500;600&display=swap');

/* CSS 变量定义 - 夜间模式（默认） */
.terminal-app {
  --bg-dark: #0f1115;
  --sidebar-bg: #181a1f;
  --main-bg: #0f1115;
  --accent: #3b82f6;
  --accent-hover: #2563eb;
  --text-main: #e2e8f0;
  --text-dim: #94a3b8;
  --border: #2d3139;
  --hover-bg: #1e222a;
  --success: #10b981;
  --danger: #ef4444;
  --modal-overlay: rgba(0, 0, 0, 0.75);
  --scrollbar-track: #1e222a;
  --scrollbar-thumb: #4b5563;

  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background-color: var(--bg-dark);
  color: var(--text-main);
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 白天模式 */
.terminal-app.light-mode {
  --bg-dark: #ffffff;
  --sidebar-bg: #f8fafc;
  --main-bg: #ffffff;
  --accent: #3b82f6;
  --accent-hover: #2563eb;
  --text-main: #1e293b;
  --text-dim: #64748b;
  --border: #e2e8f0;
  --hover-bg: #f1f5f9;
  --success: #10b981;
  --danger: #ef4444;
  --modal-overlay: rgba(0, 0, 0, 0.5);
  --scrollbar-track: #f1f5f9;
  --scrollbar-thumb: #cbd5e1;
}

/* 移动端头部 */
.mobile-header {
  display: none;
  background: var(--sidebar-bg);
  border-bottom: 1px solid var(--border);
  padding: 10px 12px;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.mobile-header button {
  background: var(--accent);
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.mobile-header button:hover {
  background: var(--accent-hover);
}

.mobile-header .current-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  font-family: 'JetBrains Mono', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
  text-align: center;
}

/* 主布局 */
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 280px;
  min-width: 280px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
  z-index: 10; /* PC 端侧边栏层级 */
}

.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--sidebar-bg);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  color: var(--text-dim);
  font-family: 'JetBrains Mono', monospace;
}

.icon-add-btn {
  background: var(--accent);
  color: white;
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-add-btn:hover {
  background: var(--accent-hover);
  transform: scale(1.05);
}

/* 服务器列表 */
.server-list {
  flex: 1;
  overflow-y: auto;
}

.server-item {
  padding: 12px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--sidebar-bg);
  position: relative;
}

.server-item:hover {
  background: var(--hover-bg);
}

.server-item.active {
  background: var(--hover-bg);
  border-left: 3px solid var(--accent);
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-dim);
  flex-shrink: 0;
}

.status-indicator.connected {
  background: var(--success);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}

.server-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.server-meta .name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  font-family: 'JetBrains Mono', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.server-meta .addr {
  font-size: 11px;
  color: var(--text-dim);
  font-family: 'JetBrains Mono', monospace;
}

.item-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.server-item:hover .item-actions {
  opacity: 1;
}

.item-actions button {
  background: var(--border);
  color: var(--text-main);
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-actions button:hover {
  background: var(--accent);
  color: white;
}

.item-actions button.del:hover {
  background: var(--danger);
}

/* 终端容器 */
.terminal-container {
  flex: 1;
  background: var(--main-bg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  z-index: 1; /* 主内容区域层级 */
}

.terminal-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.terminal-toolbar {
  background: var(--sidebar-bg);
  border-bottom: 1px solid var(--border);
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.server-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tag {
  background: var(--accent);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
}

.server-info code {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  font-family: 'JetBrains Mono', monospace;
}

.ctrl-group {
  display: flex;
  gap: 8px;
}

.btn-connect, .btn-disconnect {
  padding: 6px 16px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Inter', sans-serif;
}

.btn-connect {
  background: var(--success);
  color: white;
}

.btn-connect:hover {
  background: #059669;
  transform: translateY(-1px);
}

.btn-disconnect {
  background: var(--danger);
  color: white;
}

.btn-disconnect:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.xterm-wrapper {
  flex: 1;
  background: var(--main-bg);
  padding: 16px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  /* font-size 由动态绑定控制，移除固定值 */
  line-height: 1.6;
  outline: none;

  /* 允许用户选择文本进行复制 */
  user-select: text;

  /* 阻止系统默认的双指缩放干扰，只允许垂直滚动 */
  touch-action: pan-y;

  /* 添加平滑过渡效果（触摸反馈要求更高的跟手性） */
  transition: font-size 0.1s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: font-size; /* 提示浏览器优化此属性的变动 */
}

.terminal-content {
  color: var(--text-main);
  white-space: pre-wrap;
  word-wrap: break-word;
}

.prompt {
  color: var(--success);
  font-weight: bold;
  margin-right: 8px;
}

.cursor {
  color: var(--text-main);
}

.cursor-blink {
  color: var(--accent);
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-dim);
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 14px;
  color: var(--text-dim);
}

/* 滚动条样式 */
.xterm-wrapper::-webkit-scrollbar,
.server-list::-webkit-scrollbar {
  width: 8px;
}

.xterm-wrapper::-webkit-scrollbar-track,
.server-list::-webkit-scrollbar-track {
  background: var(--scrollbar-track);
}

.xterm-wrapper::-webkit-scrollbar-thumb,
.server-list::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 4px;
}

.xterm-wrapper::-webkit-scrollbar-thumb:hover,
.server-list::-webkit-scrollbar-thumb:hover {
  background: var(--text-dim);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--modal-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  backdrop-filter: blur(8px);
}

.modal-content {
  background: var(--sidebar-bg);
  border-radius: 8px;
  width: 100%;
  max-width: 440px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6);
  border: 1px solid var(--border);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.modal-header h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  font-family: 'JetBrains Mono', monospace;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: var(--text-dim);
  cursor: pointer;
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: var(--hover-bg);
  color: var(--text-main);
}

.form-group {
  padding: 0 20px;
  margin-bottom: 16px;
}

.form-group:first-of-type {
  margin-top: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-dim);
  font-family: 'JetBrains Mono', monospace;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-dark);
  border: 1px solid var(--border);
  color: var(--text-main);
  border-radius: 4px;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-group input:focus {
  border-color: var(--accent);
  outline: none;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.form-group input::placeholder {
  color: var(--text-dim);
  opacity: 0.5;
}

.modal-actions {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
  justify-content: flex-end;
  background: var(--sidebar-bg);
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Inter', sans-serif;
}

.btn-primary {
  background: var(--accent);
  color: white;
}

.btn-primary:hover {
  background: var(--accent-hover);
  transform: translateY(-1px);
}

.btn-secondary {
  background: var(--border);
  color: var(--text-main);
}

.btn-secondary:hover {
  background: var(--text-dim);
}

/* 自定义右键菜单 */
.custom-context-menu {
  position: fixed;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 10000;
  padding: 5px 0;
  min-width: 150px;
}

.menu-item {
  padding: 8px 20px;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.menu-separator {
  height: 1px;
  background: #ebeef5;
  margin: 5px 0;
}

/* 粘贴确认对话框 */
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

/* 移动端适配 */
@media (max-width: 768px) {
  .mobile-header {
    display: flex;
  }

  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000; /* 提高 z-index 确保在最上层 */
    transform: translateX(-100%);
    box-shadow: 2px 0 20px rgba(0, 0, 0, 0.3); /* 添加阴影增强层次感 */
  }

  .sidebar.mobile-hidden {
    transform: translateX(-100%);
  }

  .terminal-container {
    width: 100%;
    position: relative;
    z-index: 1; /* 确保主内容区域在侧边栏下层 */
  }

  /* 当侧边栏打开时，主内容区域添加遮罩效果 */
  .sidebar:not(.mobile-hidden) ~ .terminal-container::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 999;
    pointer-events: none;
  }

  .terminal-toolbar {
    padding: 10px 16px;
  }

  .xterm-wrapper {
    padding: 12px;
    /* font-size 由动态绑定控制，移除固定值 */
  }

  .modal-content {
    max-width: 100%;
    max-height: 95vh;
  }
}

@media (max-width: 480px) {
  .sidebar {
    width: 100%;
    min-width: 100%;
  }

  .server-item {
    padding: 10px 16px;
  }

  .server-meta .name {
    font-size: 12px;
  }

  .server-meta .addr {
    font-size: 10px;
  }

  .terminal-toolbar {
    padding: 8px 12px;
  }

  .server-info code {
    font-size: 12px;
  }

  .tag {
    font-size: 10px;
    padding: 3px 6px;
  }

  .btn-connect, .btn-disconnect {
    padding: 5px 12px;
    font-size: 11px;
  }

  .xterm-wrapper {
    padding: 10px;
    /* font-size 由动态绑定控制，移除固定值 */
  }
}
</style>
