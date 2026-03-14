<template>
  <div
    class="terminal-app"
    :class="{ 'light-mode': !isDarkMode }"
    @click="closeContextMenu"
  >
    <!-- 移动端头部 -->
    <header
      v-if="isMobile"
      class="mobile-header"
    >
      <button @click="toggleServers">
        ☰ 列表
      </button>
      <span class="current-title">{{ activeTab ? activeTab.title : '未选择终端' }}</span>
      <button @click="toggleAddForm">
        ✚ 添加
      </button>
    </header>

    <!-- 主布局 -->
    <div class="main-layout">
      <!-- 侧边栏 -->
      <aside :class="['sidebar', { 'mobile-hidden': isMobile && !showServers }]">
        <div class="sidebar-header">
          <h2>SERVERS</h2>
          <button
            class="icon-add-btn"
            title="添加服务器"
            @click="toggleAddForm"
          >
            ✚
          </button>
        </div>

        <nav class="server-list">
          <div
            v-for="server in servers"
            :key="server.id"
            class="server-item"
            @click="openTab(server)"
          >
            <div
              class="status-indicator"
              :class="{ connected: isServerConnected(server.id) }"
            />
            <div class="server-meta">
              <span class="name">{{ server.name || server.host }}</span>
              <span class="addr">{{ server.host }}</span>
            </div>
            <div class="item-actions">
              <button
                title="新窗口"
                @click.stop="openNewWindow(server.id)"
              >
                ↗
              </button>
              <button
                class="del"
                title="删除"
                @click.stop="deleteServer(server.id)"
              >
                ×
              </button>
            </div>
          </div>
        </nav>
      </aside>

      <!-- 终端主区域 -->
      <main class="terminal-container">
        <!-- 标签页栏 -->
        <div
          v-if="tabs.length > 0"
          class="tabs-header"
        >
          <div 
            v-for="tab in tabs" 
            :key="tab.id"
            :class="['tab-item', { active: activeTabId === tab.id }]"
            @click="activateTab(tab.id)"
          >
            <span
              class="tab-status"
              :class="{ connected: tab.connected }"
            />
            <span class="tab-title">{{ tab.title }}</span>
            <button
              class="tab-close"
              @click.stop="closeTab(tab.id)"
            >
              ×
            </button>
          </div>
        </div>

        <!-- 标签页内容 -->
        <div
          v-if="tabs.length > 0"
          class="tabs-content"
        >
          <div 
            v-for="tab in tabs" 
            v-show="activeTabId === tab.id"
            :key="tab.id"
            class="tab-pane"
          >
            <TerminalTab 
              :server-id="tab.serverId"
              :server-name="tab.title"
              :initial-path="tab.initialPath"
              @status-change="(status) => updateTabStatus(tab.id, status)"
            />
          </div>
        </div>

        <div
          v-else
          class="empty-state"
        >
          <div class="placeholder-icon">
            ⌨️
          </div>
          <p>请从左侧选择一个服务器开始工作</p>
        </div>
      </main>
    </div>

    <!-- 添加服务器弹窗 -->
    <div
      v-if="showAddForm"
      class="modal-overlay"
      @pointerdown.self="onModalOverlayPointerDown"
      @pointerup.self="onModalOverlayPointerUp"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h2>添加服务器</h2>
          <button
            class="close-btn"
            @click="closeAddForm"
          >
            ✕
          </button>
        </div>
        <form @submit.prevent="addServerAndClose">
          <div class="form-group">
            <label>服务器名称</label>
            <input
              v-model="newServer.name"
              type="text"
              placeholder="可选，如：生产服务器 1"
            >
          </div>
          <div class="form-group">
            <label>主机地址</label>
            <input
              v-model="newServer.host"
              type="text"
              placeholder="如：192.168.1.100"
              required
            >
          </div>
          <div class="form-group">
            <label>用户名</label>
            <input
              v-model="newServer.user"
              type="text"
              placeholder="如：root"
              required
            >
          </div>
          <div class="form-group">
            <label>密码</label>
            <input
              v-model="newServer.password"
              type="password"
              placeholder="服务器密码"
              required
            >
          </div>
          <div class="form-group">
            <label>端口</label>
            <input
              v-model="newServer.port"
              type="number"
              placeholder="22"
              required
            >
          </div>
          <div class="modal-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="closeAddForm"
            >
              取消
            </button>
            <button
              type="submit"
              class="btn btn-primary"
            >
              添加
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { useThemeStore } from '@/stores/theme'
import TerminalTab from '@/components/terminal/TerminalTab.vue'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const isDarkMode = computed(() => themeStore.isDarkMode)

// 服务器列表
const servers = ref([])
// 新服务器表单
const newServer = ref({ name: '', host: '', user: '', password: '', port: 22 })

// 标签页系统
const tabs = ref([])
const activeTabId = ref(null)

const activeTab = computed(() => tabs.value.find(t => t.id === activeTabId.value))

// 移动端折叠控制
const showServers = ref(true)
const showAddForm = ref(false)
const isMobile = ref(false)
const isOverlayPressStarted = ref(false)

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

const closeAddForm = () => {
  showAddForm.value = false
}

const onModalOverlayPointerDown = () => {
  isOverlayPressStarted.value = true
}

const onModalOverlayPointerUp = () => {
  if (isOverlayPressStarted.value) {
    closeAddForm()
  }
  isOverlayPressStarted.value = false
}

// 获取服务器列表
const fetchServers = async () => {
  try {
    const response = await request.get('/api/server-terminal/servers')
    servers.value = response.data || []
    
    // 恢复之前的标签页
    restoreTabs()
    
    // 处理路由参数（SFTP 跳转）
    handleRouteParams()
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

    const response = await request.post('/api/server-terminal/servers', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (response.data) {
      servers.value.push(response.data)
      newServer.value = { name: '', host: '', user: '', password: '', port: 22 }
    }
  } catch (error) {
    console.error('添加服务器失败:', error)
  }
}

const addServerAndClose = async () => {
  await addServer()
  closeAddForm()
}

// 删除服务器
const deleteServer = async (serverId) => {
  if (!confirm('确定要删除该服务器配置吗？')) return
  try {
    const response = await request.delete(`/api/server-terminal/servers/${serverId}`)
    if (response.code === 200) {
      servers.value = servers.value.filter(s => s.id !== serverId)
      // 关闭相关标签页
      tabs.value = tabs.value.filter(t => t.serverId !== serverId)
      if (activeTabId.value && !tabs.value.find(t => t.id === activeTabId.value)) {
        activeTabId.value = tabs.value.length > 0 ? tabs.value[tabs.value.length - 1].id : null
      }
      saveTabs()
    }
  } catch (error) {
    console.error('删除服务器失败:', error)
  }
}

// 打开新窗口（浏览器新标签）
const openNewWindow = (serverId) => {
  const url = `${window.location.origin}/terminal/${serverId}`
  window.open(url, '_blank', 'width=1200,height=800')
}

// --- 标签页管理 ---

const openTab = (server, path = '') => {
  // 生成唯一 ID (添加随机后缀防止快速点击重复)
  const tabId = Date.now().toString() + Math.random().toString(36).substr(2, 5)
  
  // 计算标题（如果有多个同名标签，增加序号）
  const sameServerTabs = tabs.value.filter(t => t.serverId === server.id)
  let title = server.name || server.host
  if (sameServerTabs.length > 0) {
    title += ` (${sameServerTabs.length + 1})`
  }

  const newTab = {
    id: tabId,
    serverId: server.id,
    title: title,
    initialPath: path,
    connected: false
  }
  
  tabs.value.push(newTab)
  activeTabId.value = tabId
  saveTabs()
}

const closeTab = (tabId) => {
  const index = tabs.value.findIndex(t => t.id === tabId)
  if (index !== -1) {
    tabs.value.splice(index, 1)
    if (activeTabId.value === tabId) {
      activeTabId.value = tabs.value.length > 0 ? tabs.value[Math.max(0, index - 1)].id : null
    }
    saveTabs()
  }
}

const activateTab = (tabId) => {
  activeTabId.value = tabId
  saveTabs() // 保存激活状态（可选）
}

const updateTabStatus = (tabId, status) => {
  const tab = tabs.value.find(t => t.id === tabId)
  if (tab) {
    tab.connected = status.connected
  }
}

const isServerConnected = (serverId) => {
  return tabs.value.some(t => t.serverId === serverId && t.connected)
}

// --- 持久化 ---

const STORAGE_KEY = 'terminal_tabs'

const saveTabs = () => {
  const data = {
    tabs: tabs.value.map(t => ({
      id: t.id,
      serverId: t.serverId,
      title: t.title,
      // initialPath 不持久化，避免刷新后重复执行 cd
      connected: false // 状态不持久化
    })),
    activeTabId: activeTabId.value
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

const restoreTabs = () => {
  const dataStr = localStorage.getItem(STORAGE_KEY)
  if (dataStr) {
    try {
      const data = JSON.parse(dataStr)
      if (Array.isArray(data.tabs)) {
        // 过滤掉已删除的服务器
        tabs.value = data.tabs.filter(t => servers.value.some(s => s.id === t.serverId))
        activeTabId.value = data.activeTabId
        
        // 如果当前激活的 tab 不存在了，修正激活项
        if (activeTabId.value && !tabs.value.find(t => t.id === activeTabId.value)) {
          activeTabId.value = tabs.value.length > 0 ? tabs.value[0].id : null
        }
      }
    } catch (e) {
      console.error('恢复标签页失败:', e)
    }
  }
}

// --- 路由参数处理 (SFTP 集成) ---

const handleRouteParams = () => {
  const { serverId, path } = route.query
  if (serverId) {
    const sId = parseInt(serverId)
    const server = servers.value.find(s => s.id === sId)
    if (server) {
      // 检查是否已经有针对该服务器且未指定路径的空闲 tab？
      // 策略：总是打开新 tab，避免干扰现有工作
      openTab(server, path)
      
      // 清除路由参数，避免刷新重复打开
      router.replace({ query: {} })
    }
  }
}

onMounted(() => {
  fetchServers()
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&family=Inter:wght@400;500;600&display=swap');

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
  --tab-bg: #181a1f;
  --tab-active-bg: #0f1115;
  
  font-family: 'Inter', sans-serif;
  background-color: var(--bg-dark);
  color: var(--text-main);
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.terminal-app.light-mode {
  --bg-dark: #ffffff;
  --sidebar-bg: #f8fafc;
  --main-bg: #ffffff;
  --text-main: #1e293b;
  --border: #e2e8f0;
  --hover-bg: #f1f5f9;
  --tab-bg: #f1f5f9;
  --tab-active-bg: #ffffff;
}

.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 260px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.3s ease;
}

.sidebar.mobile-hidden {
  width: 0;
  overflow: hidden;
  border: none;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h2 {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--text-dim);
  margin: 0;
}

.icon-add-btn {
  background: none;
  border: none;
  color: var(--text-dim);
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.icon-add-btn:hover {
  background: var(--hover-bg);
  color: var(--accent);
}

.server-list {
  flex: 1;
  overflow-y: auto;
}

.server-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  transition: background 0.2s;
}

.server-item:hover {
  background: var(--hover-bg);
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-dim);
  opacity: 0.5;
}

.status-indicator.connected {
  background: #10b981;
  opacity: 1;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
}

.server-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.server-meta .name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.server-meta .addr {
  font-size: 11px;
  color: var(--text-dim);
  font-family: monospace;
}

.item-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.server-item:hover .item-actions {
  opacity: 1;
}

.item-actions button {
  background: none;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
  font-size: 12px;
  padding: 4px;
}

.item-actions button:hover {
  color: var(--text-main);
}

.item-actions button.del:hover {
  color: var(--danger);
}

/* 终端容器 */
.terminal-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--main-bg);
  min-width: 0;
}

/* 标签页样式 */
.tabs-header {
  display: flex;
  background: var(--tab-bg);
  border-bottom: 1px solid var(--border);
  overflow-x: auto;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  min-width: 120px;
  max-width: 200px;
  background: var(--tab-bg);
  border-right: 1px solid var(--border);
  cursor: pointer;
  user-select: none;
  font-size: 13px;
  color: var(--text-dim);
}

.tab-item:hover {
  background: var(--hover-bg);
}

.tab-item.active {
  background: var(--tab-active-bg);
  color: var(--text-main);
  border-top: 2px solid var(--accent);
}

.tab-status {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-dim);
}

.tab-status.connected {
  background: #10b981;
}

.tab-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-close {
  background: none;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
  font-size: 14px;
  padding: 0 4px;
  border-radius: 4px;
}

.tab-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-main);
}

.tabs-content {
  flex: 1;
  position: relative;
}

.tab-pane {
  width: 100%;
  height: 100%;
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
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.3;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: var(--sidebar-bg);
  border-radius: 8px;
  width: 100%;
  max-width: 400px;
  border: 1px solid var(--border);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.4);
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
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-dim);
  font-size: 18px;
  cursor: pointer;
}

form {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-dim);
}

.form-group input {
  width: 100%;
  padding: 10px;
  background: var(--bg-dark);
  border: 1px solid var(--border);
  color: var(--text-main);
  border-radius: 6px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  border: none;
}

.btn-primary {
  background: var(--accent);
  color: white;
}

.btn-secondary {
  background: var(--border);
  color: var(--text-main);
}
</style>
