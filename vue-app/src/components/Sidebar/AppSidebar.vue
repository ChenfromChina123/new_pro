<template>
  <aside class="app-sidebar">
    <!-- 顶部：用户个人资料与设置 -->
    <div class="sidebar-top">
      <div class="user-profile">
        <div class="user-avatar-wrapper">
          <img 
            v-if="authStore.userInfo?.avatar" 
            :src="avatarUrl || authStore.userInfo.avatar" 
            :alt="authStore.username" 
            class="sidebar-avatar"
          >
          <i 
            v-else 
            class="fas fa-user default-avatar-icon" 
          />
        </div>
        <span class="sidebar-user-name">{{ authStore.username || '用户' }}</span>
      </div>
      
      <div class="sidebar-actions">
        <div 
          class="sidebar-action-btn theme-toggle" 
          :title="themeStore.isDarkMode ? '切换到浅色模式' : '切换到深色模式'" 
          @click.stop="handleToggleDarkMode"
        >
          <i :class="themeStore.isDarkMode ? 'fas fa-sun' : 'fas fa-moon'" />
        </div>
        
        <div 
          class="sidebar-action-btn settings-btn"
          title="设置"
          @click.stop="router.push('/settings')"
        >
          <i class="fas fa-cog" />
        </div>
      </div>
    </div>

    <!-- 中间：全局导航 -->
    <div class="sidebar-nav">
      <router-link
        to="/chat"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-comments" />
        <span>AI问答</span>
      </router-link>
      <router-link
        to="/cloud-disk"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-cloud" />
        <span>云盘</span>
      </router-link>
      <router-link
        to="/language-learning"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-book" />
        <span>语言学习</span>
      </router-link>
      <router-link
        v-if="authStore.isAdmin"
        to="/admin"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-cog" />
        <span>管理</span>
      </router-link>
    </div>

    <!-- 存储配额显示 -->
    <div 
      v-if="isCloudDiskRoute" 
      class="sidebar-quota"
    >
      <div class="quota-info">
        <span class="quota-label">存储空间</span>
        <span class="quota-value">
          {{ formatFileSize(cloudDiskStore.quota.usedSize) }}
          <template v-if="cloudDiskStore.quota.limitSize !== -1">
            / {{ formatFileSize(cloudDiskStore.quota.limitSize) }}
          </template>
        </span>
      </div>
      <div 
        v-if="cloudDiskStore.quota.limitSize !== -1" 
        class="quota-progress-bar"
      >
        <div 
          class="quota-progress-fill" 
          :style="{ width: Math.min(100, (cloudDiskStore.quota.usedSize / cloudDiskStore.quota.limitSize) * 100) + '%' }"
          :class="{ 'warning': (cloudDiskStore.quota.usedSize / cloudDiskStore.quota.limitSize) > 0.8, 'danger': (cloudDiskStore.quota.usedSize / cloudDiskStore.quota.limitSize) > 0.9 }"
        />
      </div>
      <div 
        v-else 
        class="quota-admin-tip"
      >
        管理员不计容量
      </div>
    </div>

    <div class="sidebar-divider" />

    <!-- 动态内容区域：根据当前路由显示不同内容 -->
    <div class="dynamic-sidebar-content">
      <!-- 聊天相关的侧边栏内容 -->
      <template v-if="isChatRoute">
        <div class="sidebar-header">
          <button
            class="btn btn-primary new-chat-btn"
            @click="handleNewChat"
          >
            <span class="btn-icon">
              <i class="fas fa-plus" />
            </span>
            <span class="btn-text">新建对话</span>
          </button>
        </div>
        
        <div class="history-section-title">
          历史对话
        </div>
        <div class="session-list-wrapper">
          <div class="session-list">
            <div
              v-for="session in chatStore.sessions"
              :key="session.id"
              class="session-item"
              :class="{ active: session.id === chatStore.currentSessionId }"
              @click="loadSession(session.id)"
            >
              <div class="session-info">
                <div class="session-title">
                  {{ session.title || '新对话' }}
                </div>
                <div class="session-meta">
                  <span class="session-date">{{ formatSessionDate(session.createdAt) }}</span>
                </div>
              </div>
              <button
                class="delete-btn"
                title="删除会话"
                @click.stop="handleDeleteSession(session.id)"
              >
                <i class="fas fa-trash" />
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- 云盘相关的侧边栏内容 -->
      <template v-else-if="isCloudDiskRoute">
        <div class="sidebar-header cloud-sidebar-header">
          <h3>📁 文件夹</h3>
          <button
            class="icon-btn"
            title="新建文件夹"
            @click="handleNewFolder"
          >
            ➕
          </button>
        </div>
        
        <div
          class="folder-tree"
          :class="{ 'folder-tree-scroll': maxFolderDepth >= 3 }"
          :style="{ '--folder-indent': `${folderIndentPx}px` }"
        >
          <FolderTreeItem
            v-for="rootFolder in cloudDiskStore.folders"
            :key="rootFolder.id"
            :folder="rootFolder"
            :select-folder="selectFolder"
            :toggle-folder-expand="toggleFolderExpand"
            :is-folder-expanded="isFolderExpanded"
            :delete-folder-action="deleteFolderAction"
            :rename-folder-action="renameFolderAction"
            :depth="0"
            :indent="folderIndentPx"
          />
        </div>
      </template>

      <!-- 其他路由可以根据需要添加内容 -->
      <template v-else>
        <div class="sidebar-empty-tip">
          选择上方功能开始使用
        </div>
      </template>
    </div>



    <!-- 创建文件夹对话框 -->
    <div
      v-if="cloudDiskStore.showCreateFolderDialog"
      class="modal"
      @click.self="cloudDiskStore.showCreateFolderDialog = false"
    >
      <div class="modal-content">
        <h3>创建新文件夹</h3>
        <input
          v-model="newFolderName"
          type="text"
          class="input"
          placeholder="输入文件夹名称"
          @keyup.enter="createFolder"
        >
        <div class="modal-actions">
          <button
            class="btn btn-primary"
            @click="createFolder"
          >
            创建
          </button>
          <button
            class="btn btn-secondary"
            @click="cloudDiskStore.showCreateFolderDialog = false"
          >
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- 重命名文件夹对话框 -->
    <div
      v-if="cloudDiskStore.showRenameFolderDialog"
      class="modal"
      @click.self="closeRenameFolderDialog"
    >
      <div class="modal-content">
        <h3>重命名文件夹</h3>
        <input
          v-model="cloudDiskStore.renameFolderName"
          type="text"
          class="input"
          placeholder="输入新文件夹名称"
          @keyup.enter="confirmRenameFolder"
        >
        <div class="modal-actions">
          <button
            class="btn btn-primary"
            @click="confirmRenameFolder"
          >
            确定
          </button>
          <button
            class="btn btn-secondary"
            @click="closeRenameFolderDialog"
          >
            取消
          </button>
        </div>
      </div>
    </div>

    <ConflictResolutionDialog
      :visible="conflictDialogVisible"
      :files="currentConflictFiles"
      :batch-mode="false"
      :is-folder="true"
      @resolve="onConflictResolved"
      @cancel="onConflictCancelled"
    />
  </aside>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useThemeStore } from '@/stores/theme'
import { useUIStore } from '@/stores/ui'
import { useCloudDiskStore } from '@/stores/cloudDisk'
import { useSettingsStore } from '@/stores/settings'
import { storeToRefs } from 'pinia'
import { API_CONFIG } from '@/config/api'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import ConflictResolutionDialog from '@/components/ConflictResolutionDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const themeStore = useThemeStore()
const uiStore = useUIStore()
const cloudDiskStore = useCloudDiskStore()
const settingsStore = useSettingsStore()

// 状态
const newFolderName = ref('')
const conflictDialogVisible = ref(false)
const currentConflictFiles = ref([])

/**
 * 处理主题切换并同步到后端
 */
const handleToggleDarkMode = async () => {
  themeStore.toggleDarkMode()
  
  // 同步到后端设置 (如果已登录)
  if (authStore.isLoggedIn) {
    await settingsStore.updateSettings({ 
      theme: themeStore.isDarkMode ? 'dark' : 'light' 
    })
  }
}

// 路由判断
const isChatRoute = computed(() => route.path.startsWith('/chat'))
const isCloudDiskRoute = computed(() => route.path.startsWith('/cloud-disk'))

// 头像逻辑
const avatarUrl = ref(null)
watch(
  () => authStore.userInfo?.avatar,
  async (path) => {
    if (path) {
      try {
        const res = await fetch(`${API_CONFIG.baseURL}${path}`, {
          headers: { Authorization: `Bearer ${authStore.token}` }
        })
        if (res.ok) {
          const blob = await res.blob()
          avatarUrl.value = URL.createObjectURL(blob)
        } else {
          avatarUrl.value = null
        }
      } catch {
        avatarUrl.value = null
      }
    } else {
      avatarUrl.value = null
    }
  },
  { immediate: true }
)

// 聊天逻辑
const handleNewChat = async () => {
  // 检查当前会话是否为空（无消息且无草稿）
  const isCurrentEmpty = chatStore.messages.length === 0 && !chatStore.getDraft(chatStore.currentSessionId)
  
  if (isCurrentEmpty && chatStore.currentSessionId) {
    uiStore.showToast('当前已是新对话，请先开始聊天吧')
    return
  }
  
  const result = await chatStore.createSession()
  if (result.success) {
    router.push(`/chat?session=${result.sessionId}`)
  }
}

const loadSession = (sessionId) => {
  router.push(`/chat?session=${sessionId}`)
}

const handleDeleteSession = async (sessionId) => {
  if (confirm('确定要删除这条会话吗？')) {
    const result = await chatStore.deleteSession(sessionId)
    if (result.success && chatStore.currentSessionId === sessionId) {
      router.push('/chat')
    }
  }
}

const formatSessionDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return date.toLocaleDateString()
}

// 云盘逻辑
const folderIndentPx = 20
const expandedFolders = ref(new Set())

const handleNewFolder = () => {
  // 检查层级限制
  if (!cloudDiskStore.canCreateSubFolder()) {
    uiStore.showToast('目录层级超出限制，最多支持两层目录（不计根目录）')
    return
  }
  
  // 这里通过 store 触发视图层显示对话框
  cloudDiskStore.showCreateFolderDialog = true
}

const maxFolderDepth = computed(() => {
  let max = 0
  const walk = (node, depth) => {
    if (depth > max) max = depth
    if (node.children && node.children.length > 0) {
      node.children.forEach(child => walk(child, depth + 1))
    }
  }
  cloudDiskStore.folders.forEach(f => walk(f, 1))
  return max
})

const isFolderExpanded = (folderId) => {
  const id = typeof folderId === 'object' ? folderId.id : folderId
  return expandedFolders.value.has(id)
}

const toggleFolderExpand = (folderId) => {
  const id = typeof folderId === 'object' ? folderId.id : folderId
  if (expandedFolders.value.has(id)) {
    expandedFolders.value.delete(id)
  } else {
    expandedFolders.value.add(id)
  }
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const selectFolder = (folderPath, folderId) => {
  cloudDiskStore.fetchFiles(folderPath)
  cloudDiskStore.setActiveFolder({ folderId, folderPath })
}

const deleteFolderAction = async (folderOrId) => {
  const folder = typeof folderOrId === 'object' ? folderOrId : { id: folderOrId, folderName: '文件夹', folderPath: '' }
  if (confirm(`确定要删除文件夹 "${folder.folderName || '该文件夹'}" 及其所有内容吗？`)) {
    const path = folder.folderPath || ''
    await cloudDiskStore.deleteFolder(path)
  }
}

const renameFolderAction = (folder) => {
  cloudDiskStore.renamingFolder = folder
  cloudDiskStore.renameFolderName = folder.folderName
  cloudDiskStore.showRenameFolderDialog = true
}

/**
 * 创建文件夹
 */
const createFolder = async () => {
  if (!newFolderName.value.trim()) {
    uiStore.showToast('请输入文件夹名称')
    return
  }
  const result = await cloudDiskStore.createFolder(
    newFolderName.value,
    cloudDiskStore.currentFolder
  )
  if (result.success) {
    cloudDiskStore.showCreateFolderDialog = false
    newFolderName.value = ''
    uiStore.showToast('创建成功')
  } else {
    uiStore.showToast(`创建失败: ${result.message}`)
  }
}

/**
 * 关闭重命名对话框
 */
const closeRenameFolderDialog = () => {
  cloudDiskStore.showRenameFolderDialog = false
  cloudDiskStore.renamingFolder = null
  cloudDiskStore.renameFolderName = ''
}

/**
 * 确认重命名文件夹
 */
const confirmRenameFolder = async () => {
  if (!cloudDiskStore.renameFolderName.trim()) {
    uiStore.showToast('请输入文件夹名称')
    return
  }
  if (cloudDiskStore.renamingFolder.name === cloudDiskStore.renameFolderName) {
    closeRenameFolderDialog()
    return
  }
  const result = await cloudDiskStore.renameFolder(
    cloudDiskStore.renamingFolder.id,
    cloudDiskStore.renameFolderName
  )
  if (result.conflict) {
    cloudDiskStore.showRenameFolderDialog = false
    currentConflictFiles.value = [{
      name: cloudDiskStore.renameFolderName,
      size: 0,
      isFolder: true
    }]
    conflictDialogVisible.value = true
  } else if (result.success) {
    closeRenameFolderDialog()
    uiStore.showToast('重命名成功')
  } else {
    uiStore.showToast(`重命名失败: ${result.message}`)
  }
}

/**
 * 处理冲突解决
 */
const onConflictResolved = async ({ strategy }) => {
  conflictDialogVisible.value = false
  if (cloudDiskStore.renamingFolder) {
    const action = strategy === 'OVERWRITE' ? 'override' : 'rename'
    const result = await cloudDiskStore.resolveRenameFolder(
      cloudDiskStore.renamingFolder.id,
      action,
      cloudDiskStore.renameFolderName
    )
    if (result.success) {
      closeRenameFolderDialog()
      uiStore.showToast('重命名成功')
    } else {
      uiStore.showToast(result.message)
    }
  }
}

/**
 * 处理冲突取消
 */
const onConflictCancelled = () => {
  conflictDialogVisible.value = false
  if (cloudDiskStore.renamingFolder) {
    closeRenameFolderDialog()
  }
}



// 监听路由变化加载数据
watch(
  () => route.path,
  async (newPath) => {
    if (newPath.startsWith('/chat')) {
      await chatStore.fetchSessions()
    } else if (newPath.startsWith('/cloud-disk')) {
      await cloudDiskStore.fetchFolders()
      await cloudDiskStore.fetchQuota()
    } else if (newPath.startsWith('/terminal')) {
      await terminalStore.fetchSessions()
    }
  },
  { immediate: true }
)

// 初始化
onMounted(() => {
  // 初始加载由 watch { immediate: true } 处理
})
</script>

<style scoped>
.app-sidebar {
  width: 300px;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  height: 100vh;
  flex-shrink: 0;
  z-index: 100;
}

.sidebar-top {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}

.user-avatar-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar-icon {
  font-size: 18px;
  color: var(--text-tertiary);
}

.sidebar-user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  margin-left: 8px;
}

.sidebar-action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  background: transparent;
  border: none;
  text-decoration: none;
}

.sidebar-action-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.sidebar-action-btn i {
  font-size: 16px;
}

.sidebar-nav {
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-quota {
  padding: 12px 20px;
  font-size: 12px;
}

.quota-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  color: var(--text-secondary);
}

.quota-value {
  font-weight: 500;
}

.quota-progress-bar {
  height: 6px;
  background-color: var(--bg-tertiary);
  border-radius: 3px;
  overflow: hidden;
}

.quota-progress-fill {
  height: 100%;
  background-color: #4CAF50;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.quota-progress-fill.warning {
  background-color: #FF9800;
}

.quota-progress-fill.danger {
  background-color: #F44336;
}

.quota-admin-tip {
  color: var(--text-tertiary);
  font-style: italic;
  font-size: 11px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--text-primary);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}

.nav-item:hover {
  background-color: var(--bg-tertiary);
}

.nav-item.active {
  background-color: #ebf5ff;
  color: #2563eb;
  font-weight: 500;
}

.sidebar-divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 8px 16px;
}

.dynamic-sidebar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 12px 20px;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #1d4ed8;
  color: #ffffff;
  border: none;
  font-weight: 600;
  width: 100%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
  cursor: pointer;
}

.new-chat-btn:hover {
  background-color: #1e40af;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
}

.history-section-title {
  padding: 16px 16px 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.session-list-wrapper {
  flex: 1;
  overflow-y: auto;
}

.session-list {
  padding: 0 12px 12px;
}

.session-badge {
  font-size: 10px;
  background-color: var(--bg-tertiary);
  color: var(--text-tertiary);
  padding: 1px 4px;
  border-radius: 4px;
  margin-left: 6px;
  border: 1px solid var(--border-color);
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.session-item:hover {
  background-color: var(--bg-tertiary);
}

.session-item.active {
  background-color: var(--bg-tertiary);
  border-left: 3px solid var(--primary-color);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.session-meta {
  font-size: 12px;
  color: var(--text-tertiary);
}

.delete-btn {
  padding: 6px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: var(--danger-color);
  background-color: rgba(239, 68, 68, 0.1);
}

/* 云盘侧边栏样式 */
.cloud-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cloud-sidebar-header h3 {
  font-size: 16px;
  margin: 0;
  color: var(--text-primary);
}

.icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover {
  background-color: var(--primary-color);
  color: white;
}

.folder-tree {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.sidebar-empty-tip {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background-color: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--danger-color);
  border-color: var(--danger-color);
}

/* 滚动条 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--gray-300);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--gray-400);
}

/* Modal Styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background-color: var(--bg-secondary);
  border-radius: 16px;
  padding: 32px;
  min-width: 360px;
  max-width: 90vw;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border-color);
  animation: modal-in 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modal-in {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-content h3 {
  margin: 0 0 24px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  text-align: center;
}

.modal-content .input {
  width: 100%;
  padding: 12px 16px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 15px;
  margin-bottom: 24px;
  transition: all 0.2s;
  box-sizing: border-box;
}

.modal-content .input:focus {
  outline: none;
  border-color: var(--primary-color);
  background-color: var(--bg-secondary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.modal-actions {
  display: flex;
  justify-content: center; /* Center actions as requested */
  gap: 16px;
}

.modal-actions .btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.modal-actions .btn:hover {
  transform: translateY(-1px);
}

.modal-actions .btn-primary {
  background: #2563eb;
  color: white;
  box-shadow: 0 4px 6px rgba(37, 99, 235, 0.2);
}

.modal-actions .btn-primary:hover {
  background: #1d4ed8;
  box-shadow: 0 6px 8px rgba(37, 99, 235, 0.3);
}

.modal-actions .btn-secondary {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.modal-actions .btn-secondary:hover {
  background-color: var(--bg-secondary);
  border-color: var(--text-tertiary);
}
</style>
