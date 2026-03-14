<template>
  <div
    class="sftp-manager"
    :class="{ 'light-mode': !isDarkMode }"
  >
    <!-- 头部工具栏 -->
    <header class="manager-header">
      <div class="header-left">
        <h1 class="title">
          SFTP 文件管理器
        </h1>
        <button
          class="btn-icon"
          title="添加服务器"
          @click="showAddServerModal = true"
        >
          <span>+</span>
        </button>
      </div>
    </header>

    <!-- 主内容区域 -->
    <div class="manager-content">
      <!-- 左侧：服务器列表 -->
      <aside class="server-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">服务器列表</span>
        </div>
        <div class="server-list">
          <div
            v-for="server in servers"
            :key="server.id"
            :class="['server-item', { active: selectedServerId === server.id }]"
            @click="selectServer(server.id)"
          >
            <div
              class="server-status"
              :class="{ connected: connectedServers.includes(server.id) }"
            />
            <div class="server-info">
              <span class="server-name">{{ server.name || server.host }}</span>
              <span class="server-host">{{ server.host }}:{{ server.port }}</span>
            </div>
            <div class="server-actions">
              <button
                class="btn-small"
                title="编辑"
                @click.stop="editServer(server)"
              >
                ✏️
              </button>
              <button
                class="btn-small btn-danger"
                title="删除"
                @click.stop="deleteServer(server.id)"
              >
                🗑️
              </button>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧：文件面板 -->
      <main class="file-panel-container">
        <FilePanel
          v-if="selectedServerId"
          :server-id="selectedServerId"
          :connected="connectedServers.includes(selectedServerId)"
          @connect="connectServer"
          @disconnect="disconnectServer"
        />
        <div
          v-else
          class="empty-state"
        >
          <div class="empty-icon">
            📁
          </div>
          <p>请选择一个服务器开始管理文件</p>
        </div>
      </main>
    </div>

    <!-- 底部：传输队列 -->
    <TransferQueue v-if="transferTasks.length > 0" />

    <!-- 状态栏 -->
    <StatusBar
      :connected="selectedServerId && connectedServers.includes(selectedServerId)"
      :server-info="currentServerInfo"
      :selected-count="selectedCount"
      :selected-size="selectedSize"
      :transfer-count="transferCount"
    />

    <!-- 添加服务器弹窗 -->
    <div
      v-if="showAddServerModal"
      class="modal-overlay"
      @click.self="showAddServerModal = false"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingServer ? '编辑服务器' : '添加服务器' }}</h3>
          <button
            class="btn-close"
            @click="closeServerModal"
          >
            ✕
          </button>
        </div>
        <form @submit.prevent="saveServer">
          <div class="form-group">
            <label>服务器名称</label>
            <input
              v-model="serverForm.name"
              type="text"
              placeholder="可选，如：生产服务器"
            >
          </div>
          <div class="form-group">
            <label>主机地址 *</label>
            <input
              v-model="serverForm.host"
              type="text"
              placeholder="如：192.168.1.100"
              required
            >
          </div>
          <div class="form-group">
            <label>端口 *</label>
            <input
              v-model.number="serverForm.port"
              type="number"
              placeholder="22"
              required
            >
          </div>
          <div class="form-group">
            <label>用户名 *</label>
            <input
              v-model="serverForm.username"
              type="text"
              placeholder="如：root"
              required
            >
          </div>
          <div class="form-group">
            <label>密码 *</label>
            <input
              v-model="serverForm.password"
              type="password"
              placeholder="服务器密码"
              required
            >
          </div>
          <div class="modal-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="closeServerModal"
            >
              取消
            </button>
            <button
              type="submit"
              class="btn btn-primary"
            >
              保存
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useSFTPStore } from '@/stores/sftp'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
import FilePanel from '@/components/sftp/FilePanel.vue'
import TransferQueue from '@/components/sftp/TransferQueue.vue'
import StatusBar from '@/components/sftp/StatusBar.vue'

const sftpStore = useSFTPStore()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const isDarkMode = computed(() => themeStore.isDarkMode)

const servers = ref([])
const selectedServerId = ref(sftpStore.currentServerId || null)
const connectedServers = ref([])
const showAddServerModal = ref(false)
const editingServer = ref(null)

const serverForm = ref({
  name: '',
  host: '',
  port: 22,
  username: '',
  password: ''
})

const selectedCount = computed(() => sftpStore.selectedCount)
const selectedSize = computed(() => sftpStore.selectedSize)
const transferCount = computed(() => sftpStore.transferCount)
const transferTasks = computed(() => sftpStore.transferTasks)
const loading = computed(() => sftpStore.loading)

const currentServerInfo = computed(() => {
  if (!selectedServerId.value) return null
  const server = servers.value.find(s => s.id === selectedServerId.value)
  return server ? { host: server.host, port: server.port } : null
})

/**
 * 切换主题
 */
function toggleTheme() {
  themeStore.toggleTheme()
}

/**
 * 获取服务器列表
 */
async function fetchServers() {
  try {
    const response = await request.get('/api/server-terminal/servers')
    servers.value = response.data || []
    if (selectedServerId.value) {
      const exists = servers.value.some(server => server.id === selectedServerId.value)
      if (!exists) {
        selectedServerId.value = null
        sftpStore.setCurrentServer(null)
        return
      }
      sftpStore.setCurrentServer(selectedServerId.value)
    }
  } catch (error) {
    console.error('获取服务器列表失败:', error)
  }
}

/**
 * 选择服务器
 * @param {number} serverId - 服务器 ID
 */
function selectServer(serverId) {
  selectedServerId.value = serverId
  sftpStore.setCurrentServer(serverId)
}

/**
 * 处理服务器选择变化
 */
function handleServerChange() {
  if (selectedServerId.value) {
    selectServer(selectedServerId.value)
  }
}

/**
 * 连接服务器
 * @param {number} serverId - 服务器 ID
 */
async function connectServer(serverId) {
  try {
    const response = await request.post(`/api/server-terminal/servers/${serverId}/connect`)
    if (response.code === 200) {
      if (!connectedServers.value.includes(serverId)) {
        connectedServers.value.push(serverId)
      }
      sftpStore.connected = true
      await sftpStore.fetchFiles()
    }
  } catch (error) {
    console.error('连接服务器失败:', error)
  }
}

/**
 * 断开服务器连接
 * @param {number} serverId - 服务器 ID
 */
async function disconnectServer(serverId) {
  try {
    await request.post(`/api/server-terminal/servers/${serverId}/disconnect`)
    connectedServers.value = connectedServers.value.filter(id => id !== serverId)
    sftpStore.connected = false
  } catch (error) {
    console.error('断开连接失败:', error)
  }
}

/**
 * 刷新文件列表
 */
async function refreshFiles() {
  await sftpStore.fetchFiles()
}

/**
 * 编辑服务器
 * @param {object} server - 服务器信息
 */
function editServer(server) {
  editingServer.value = server
  serverForm.value = {
    name: server.name || '',
    host: server.host,
    port: server.port,
    username: server.username || '',
    password: ''
  }
  showAddServerModal.value = true
}

/**
 * 删除服务器
 * @param {number} serverId - 服务器 ID
 */
async function deleteServer(serverId) {
  if (!confirm('确定要删除这个服务器吗？')) return

  try {
    await request.delete(`/api/server-terminal/servers/${serverId}`)
    servers.value = servers.value.filter(s => s.id !== serverId)
    if (selectedServerId.value === serverId) {
      selectedServerId.value = null
      sftpStore.setCurrentServer(null)
    }
  } catch (error) {
    console.error('删除服务器失败:', error)
  }
}

/**
 * 保存服务器
 */
async function saveServer() {
  try {
    const formData = new FormData()
    formData.append('serverName', serverForm.value.name)
    formData.append('host', serverForm.value.host)
    formData.append('port', serverForm.value.port)
    formData.append('username', serverForm.value.username)
    formData.append('password', serverForm.value.password)

    if (editingServer.value) {
      await request.put(`/api/server-terminal/servers/${editingServer.value.id}`, formData)
    } else {
      const response = await request.post('/api/server-terminal/servers', formData)
      if (response.data) {
        servers.value.push(response.data)
      }
    }

    closeServerModal()
    await fetchServers()
  } catch (error) {
    console.error('保存服务器失败:', error)
  }
}

/**
 * 关闭服务器弹窗
 */
function closeServerModal() {
  showAddServerModal.value = false
  editingServer.value = null
  serverForm.value = {
    name: '',
    host: '',
    port: 22,
    username: '',
    password: ''
  }
}

onMounted(() => {
  fetchServers()
})

onUnmounted(() => {
  connectedServers.value.forEach(serverId => {
    disconnectServer(serverId)
  })
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&family=Inter:wght@400;500;600&display=swap');

.sftp-manager {
  --bg-dark: #0f1115;
  --sidebar-bg: #181a1f;
  --panel-bg: #1e222a;
  --header-bg: #181a1f;
  --accent: #3b82f6;
  --accent-hover: #2563eb;
  --text-main: #e2e8f0;
  --text-dim: #94a3b8;
  --border: #2d3139;
  --hover-bg: #2d3139;
  --success: #10b981;
  --danger: #ef4444;
  --warning: #f59e0b;

  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background-color: var(--bg-dark);
  color: var(--text-main);
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sftp-manager.light-mode {
  --bg-dark: #ffffff;
  --sidebar-bg: #f8fafc;
  --panel-bg: #ffffff;
  --header-bg: #f8fafc;
  --text-main: #1e293b;
  --text-dim: #64748b;
  --border: #e2e8f0;
  --hover-bg: #f1f5f9;
}

.manager-header {
  background: var(--header-bg);
  border-bottom: 1px solid var(--border);
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--text-main);
}

.server-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.server-selector select {
  background: var(--panel-bg);
  border: 1px solid var(--border);
  color: var(--text-main);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  min-width: 200px;
}

.header-right {
  display: flex;
  gap: 8px;
}

.btn-icon {
  background: var(--panel-bg);
  border: 1px solid var(--border);
  color: var(--text-main);
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: var(--hover-bg);
}

.btn-icon:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.manager-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.server-sidebar {
  width: 280px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.sidebar-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--text-dim);
}

.server-list {
  flex: 1;
  overflow-y: auto;
}

.server-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.server-item:hover {
  background: var(--hover-bg);
}

.server-item.active {
  background: var(--hover-bg);
  border-left: 3px solid var(--accent);
}

.server-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-dim);
  flex-shrink: 0;
}

.server-status.connected {
  background: var(--success);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}

.server-info {
  flex: 1;
  min-width: 0;
}

.server-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.server-host {
  display: block;
  font-size: 11px;
  color: var(--text-dim);
  font-family: 'JetBrains Mono', monospace;
}

.server-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.server-item:hover .server-actions {
  opacity: 1;
}

.btn-small {
  background: transparent;
  border: none;
  padding: 4px;
  cursor: pointer;
  font-size: 12px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.btn-small:hover {
  opacity: 1;
}

.btn-small.btn-danger:hover {
  opacity: 1;
}

.file-panel-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-dim);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 14px;
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

.modal-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.btn-close {
  background: none;
  border: none;
  color: var(--text-dim);
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.btn-close:hover {
  color: var(--text-main);
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
  font-weight: 500;
  color: var(--text-dim);
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-dark);
  border: 1px solid var(--border);
  color: var(--text-main);
  border-radius: 6px;
  font-size: 13px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: var(--accent);
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: var(--accent);
  color: white;
}

.btn-primary:hover {
  background: var(--accent-hover);
}

.btn-secondary {
  background: var(--border);
  color: var(--text-main);
}

.btn-secondary:hover {
  background: var(--hover-bg);
}
</style>
