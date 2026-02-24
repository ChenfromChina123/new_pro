<template>
  <aside
    class="app-sidebar"
    :class="{ collapsed: uiStore.sidebarCollapsed }"
    :style="{ width: uiStore.sidebarCollapsed ? '64px' : '300px' }"
  >
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
            class="fas fa-user-secret default-avatar-icon"
          ></i>
        </div>
        <span class="sidebar-user-name">{{ authStore.username || '游客' }}</span>
      </div>
      
      <div class="sidebar-actions">
        <!-- 返回首页 -->
        <div 
          class="sidebar-action-btn home-btn"
          title="返回首页"
          @click.stop="router.push('/')"
        >
          <i class="fas fa-home"></i>
        </div>

        <div 
          class="sidebar-action-btn theme-toggle" 
          :title="themeStore.isDarkMode ? '切换到浅色模式' : '切换到深色模式'" 
          @click.stop="handleToggleDarkMode"
        >
          <i :class="themeStore.isDarkMode ? 'fas fa-sun' : 'fas fa-moon'"></i>
        </div>
        
        <div 
          v-if="authStore.isAuthenticated"
          class="sidebar-action-btn settings-btn"
          title="设置"
          @click.stop="router.push('/settings')"
        >
          <i class="fas fa-cog"></i>
        </div>
        
        <div 
          v-else
          class="sidebar-action-btn login-btn"
          title="登录"
          @click.stop="router.push('/login')"
        >
          <i class="fas fa-sign-in-alt"></i>
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
        <i class="fas fa-comments"></i>
        <span>AI问答</span>
      </router-link>
      <router-link
        v-if="authStore.isAuthenticated"
        to="/cloud-disk"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-cloud"></i>
        <span>云盘</span>
      </router-link>
      <router-link
        v-if="authStore.isAuthenticated"
        to="/language-learning"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-book"></i>
        <span>语言学习</span>
      </router-link>
      <router-link
        to="/word-game"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-spell-check"></i>
        <span>单词记忆</span>
      </router-link>
      <router-link
        v-if="authStore.isAuthenticated"
        to="/requirement"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-project-diagram"></i>
        <span>需求分析</span>
      </router-link>
      <router-link
        to="/public-files"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-folder-open"></i>
        <span>公共资源</span>
      </router-link>
      <router-link
        v-if="authStore.isAdmin"
        to="/admin"
        class="nav-item"
        active-class="active"
      >
        <i class="fas fa-cog"></i>
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
        ></div>
      </div>
      <div 
        v-else 
        class="quota-admin-tip"
      >
        管理员不计容量
      </div>
    </div>

    <div class="sidebar-divider"></div>

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
              <i class="fas fa-plus"></i>
            </span>
            <span class="btn-text">新建对话</span>
          </button>
        </div>
        
        <div class="history-section-title">
          历史对话
        </div>
        <div class="session-list-wrapper">
          <div 
            v-if="!authStore.isAuthenticated" 
            class="guest-sidebar-tip"
          >
            <p>登录后可保存历史对话</p>
            <button 
              class="btn btn-secondary btn-small" 
              @click="router.push('/login')"
            >
              立即登录
            </button>
          </div>
          <div 
            v-else 
            class="session-list"
          >
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
                <i class="fas fa-trash"></i>
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

      <!-- 公共资源相关的侧边栏内容 -->
      <template v-else-if="isPublicFilesRoute">
        <div class="sidebar-header">
          <h3>📚 公共资源</h3>
        </div>
        <div class="sidebar-info-text">
          <p>这里提供常用的公共文件供大家下载使用。</p>
          <p 
            v-if="authStore.isAdmin" 
            class="admin-tip"
          >
            您是管理员，可以上传文件。
          </p>
        </div>
      </template>

      <!-- 语言学习相关的侧边栏内容 -->
      <template v-else-if="isLanguageLearningRoute">
        <div class="sidebar-header">
          <h3>📖 语言学习</h3>
        </div>
        <div class="sub-nav-list">
          <router-link
            :to="{ path: '/language-learning', query: { view: 'dashboard' } }"
            class="sub-nav-item"
            :class="{ active: !route.query.view || route.query.view === 'dashboard' }"
          >
            <span class="item-icon">📊</span>
            <span class="item-text">学习概览</span>
          </router-link>
          <router-link
            :to="{ path: '/language-learning', query: { view: 'my-words' } }"
            class="sub-nav-item"
            :class="{ active: route.query.view === 'my-words' }"
          >
            <span class="item-icon">📚</span>
            <span class="item-text">我的单词</span>
          </router-link>
          <router-link
            :to="{ path: '/language-learning', query: { view: 'public-library' } }"
            class="sub-nav-item"
            :class="{ active: route.query.view === 'public-library' }"
          >
            <span class="item-icon">🌐</span>
            <span class="item-text">公共词库</span>
          </router-link>
          <router-link
            :to="{ path: '/language-learning', query: { view: 'ai-articles' } }"
            class="sub-nav-item"
            :class="{ active: route.query.view === 'ai-articles' }"
          >
            <span class="item-icon">🤖</span>
            <span class="item-text">AI文章</span>
          </router-link>
          <router-link
            :to="{ path: '/language-learning', query: { view: 'translation' } }"
            class="sub-nav-item"
            :class="{ active: route.query.view === 'translation' }"
          >
            <span class="item-icon">🔤</span>
            <span class="item-text">智能翻译</span>
          </router-link>
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

    <!-- 侧栏折叠/展开按钮（仅桌面端显示） -->
    <button
      type="button"
      class="sidebar-collapse-btn"
      :title="uiStore.sidebarCollapsed ? '展开侧栏' : '收起侧栏'"
      @click="toggleSidebarCollapse"
    >
      <i :class="uiStore.sidebarCollapsed ? 'fas fa-chevron-right' : 'fas fa-chevron-left'"></i>
    </button>
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
  if (authStore.isAuthenticated) {
    await settingsStore.updateSettings({
      theme: themeStore.isDarkMode ? 'dark' : 'light'
    })
  }
}

/**
 * 切换侧栏折叠/展开状态，并持久化到 localStorage
 */
const toggleSidebarCollapse = () => {
  uiStore.saveState('sidebarCollapsed', !uiStore.sidebarCollapsed)
}

// 路由判断
const isChatRoute = computed(() => route.path.startsWith('/chat'))
const isCloudDiskRoute = computed(() => route.path.startsWith('/cloud-disk'))
const isPublicFilesRoute = computed(() => route.path.startsWith('/public-files'))
const isLanguageLearningRoute = computed(() => route.path.startsWith('/language-learning'))

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
/**
 * 创建新对话
 * 检查当前会house是否为空，若为空则提示用户，否则创建新会话并跳转
 */
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

/**
 * 加载指定的会话
 * @param {string} sessionId 会话 ID
 */
const loadSession = (sessionId) => {
  router.push(`/chat?session=${sessionId}`)
}

/**
 * 删除指定的会话
 * @param {string} sessionId 会话 ID
 */
const handleDeleteSession = async (sessionId) => {
  if (confirm('确定要删除这条会话吗？')) {
    const result = await chatStore.deleteSession(sessionId)
    if (result.success && chatStore.currentSessionId === sessionId) {
      router.push('/chat')
    }
  }
}

/**
 * 格式化会话日期
 * @param {string} dateStr 日期字符串
 * @returns {string} 格式化后的日期描述（如：今天、昨天、N天前）
 */
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

/**
 * 处理新建文件夹逻辑
 * 检查层级限制并显示创建对话框
 */
const handleNewFolder = () => {
  // 检查层级限制
  if (!cloudDiskStore.canCreateSubFolder()) {
    uiStore.showToast('目录层级超出限制，最多支持两层目录（不计根目录）')
    return
  }
  
  // 这里通过 store 触发视图层显示对话框
  cloudDiskStore.showCreateFolderDialog = true
}

/**
 * 计算文件夹树的最大深度
 */
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

/**
 * 检查文件夹是否已展开
 * @param {string|object} folderId 文件夹 ID 或对象
 * @returns {boolean} 是否已展开
 */
const isFolderExpanded = (folderId) => {
  const id = typeof folderId === 'object' ? folderId.id : folderId
  return expandedFolders.value.has(id)
}

/**
 * 切换文件夹的展开/折叠状态
 * @param {string|object} folderId 文件夹 ID 或对象
 */
const toggleFolderExpand = (folderId) => {
  const id = typeof folderId === 'object' ? folderId.id : folderId
  if (expandedFolders.value.has(id)) {
    expandedFolders.value.delete(id)
  } else {
    expandedFolders.value.add(id)
  }
}

/**
 * 格式化文件大小
 * @param {number} bytes 字节数
 * @returns {string} 格式化后的文件大小（如：KB, MB, GB）
 */
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

/**
 * 选择文件夹并加载其内容
 * @param {string} folderPath 文件夹路径
 * @param {string} folderId 文件夹 ID
 */
const selectFolder = (folderPath, folderId) => {
  cloudDiskStore.fetchFiles(folderPath)
  cloudDiskStore.setActiveFolder({ folderId, folderPath })
}

/**
 * 处理删除文件夹操作
 * @param {object|string} folderOrId 文件夹对象或 ID
 */
const deleteFolderAction = async (folderOrId) => {
  const folder = typeof folderOrId === 'object' ? folderOrId : { id: folderOrId, folderName: '文件夹', folderPath: '' }
  if (confirm(`确定要删除文件夹 "${folder.folderName || '该文件夹'}" 及其所有内容吗？`)) {
    // deleteFolder 需要 folderId，传路径会导致后端参数不匹配
    await cloudDiskStore.deleteFolder(folder.id)
  }
}

/**
 * 触发重命名文件夹对话框
 * @param {object} folder 文件夹对象
 */
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
  if (cloudDiskStore.renamingFolder?.folderName === cloudDiskStore.renameFolderName) {
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
  () => route.fullPath,
  async (newPath) => {
    // 移动端路由切换时自动关闭侧边栏
    if (window.innerWidth < 768) {
      uiStore.closeMobileSidebar()
    }

    if (newPath.startsWith('/chat')) {
      await chatStore.fetchSessions()
    } else if (newPath.startsWith('/cloud-disk')) {
      await cloudDiskStore.fetchFolders()
      await cloudDiskStore.fetchQuota()
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
  transition: width 0.25s ease, min-width 0.25s ease;
  height: 100vh;
  flex-shrink: 0;
  z-index: 100;
  position: relative;
}

/* 侧栏折叠按钮：贴在右侧边，仅桌面端显示 */
.sidebar-collapse-btn {
  display: none;
  position: absolute;
  top: 50%;
  right: 0;
  transform: translate(100%, -50%);
  width: 20px;
  height: 56px;
  padding: 0;
  border: none;
  border-radius: 0 8px 8px 0;
  background-color: var(--bg-tertiary);
  color: var(--text-secondary);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  box-shadow: 2px 0 6px rgba(0, 0, 0, 0.08);
  transition: background-color 0.2s, color 0.2s;
  z-index: 101;
}

.sidebar-collapse-btn:hover {
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.sidebar-collapse-btn i {
  font-size: 12px;
}

/* 折叠状态下隐藏文字，仅保留图标 */
.app-sidebar.collapsed .sidebar-user-name,
.app-sidebar.collapsed .nav-item span,
.app-sidebar.collapsed .sidebar-empty-tip,
.app-sidebar.collapsed .sidebar-info-text,
.app-sidebar.collapsed .guest-sidebar-tip,
.app-sidebar.collapsed .sidebar-header span,
.app-sidebar.collapsed .dynamic-sidebar-content .sidebar-header:not(.cloud-sidebar-header) .btn-small,
.app-sidebar.collapsed .cloud-sidebar-header h3,
.app-sidebar.collapsed .sidebar-quota {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 0;
  opacity: 0;
  padding: 0;
  margin: 0;
  min-width: 0;
  border: none;
  pointer-events: none;
}

.app-sidebar.collapsed .sidebar-top {
  padding: 16px 12px;
  justify-content: center;
}

.app-sidebar.collapsed .user-profile {
  justify-content: center;
  margin-right: 0;
}

.app-sidebar.collapsed .sidebar-actions {
  display: none;
}

.app-sidebar.collapsed .nav-item {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.app-sidebar.collapsed .nav-item i {
  margin-right: 0;
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
  background-color: var(--chip-bg);
  color: var(--primary-color);
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

/* 语言学习子导航样式 */
.sub-nav-list {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.sub-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 12px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
  background-color: transparent;
}

.sub-nav-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.sub-nav-item.active {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
  font-weight: 600;
  border-left: 3px solid var(--primary-color);
  border-radius: 4px 10px 10px 4px;
}

.item-icon {
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
}

.item-text {
  font-size: 14px;
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

.guest-sidebar-tip {
  padding: 24px 16px;
  text-align: center;
  color: var(--text-secondary);
  background-color: var(--bg-secondary);
  border-radius: 12px;
  margin: 16px 8px;
  border: 1px dashed var(--border-color);
}

.guest-sidebar-tip p {
  font-size: 13px;
  margin-bottom: 16px;
}

.guest-sidebar-tip .btn-small {
  padding: 6px 16px;
  font-size: 12px;
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

.sidebar-info-text {
  padding: 0 16px;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.5;
}

.sidebar-info-text .admin-tip {
  margin-top: 10px;
  color: var(--accent-color);
  font-weight: 500;
}

/* 滚动条样式 */
.app-sidebar::-webkit-scrollbar {
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

/* 桌面端显示折叠按钮 */
@media (min-width: 769px) {
  .sidebar-collapse-btn {
    display: flex;
  }
}

/* 移动端适配：隐藏折叠按钮，侧栏使用抽屉 */
@media (max-width: 768px) {
  .sidebar-collapse-btn {
    display: none !important;
  }

  .app-sidebar {
    position: fixed;
    top: 0;
    left: -300px;
    width: 300px !important;
    height: 100vh;
    z-index: 100;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }

  .app-sidebar.mobile-open {
    left: 0;
  }

  .app-sidebar.collapsed {
    width: 300px !important;
  }
}

/* Modal 对话框样式 */
.modal {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  z-index: 9999 !important;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  margin: 0 !important;
  padding: 0 !important;
}

.modal-content {
  background-color: var(--bg-secondary);
  border-radius: 20px;
  padding: 32px;
  min-width: 400px;
  max-width: 500px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-color);
  animation: modal-in 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-content h3 {
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-content .input {
  width: 100%;
  padding: 12px 16px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 15px;
  transition: all 0.2s;
  box-sizing: border-box;
}

.modal-content .input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1);
  background-color: var(--bg-secondary);
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.modal-actions .btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.2s;
  cursor: pointer;
}

.modal-actions .btn-primary {
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  color: white;
  border: none;
}

.modal-actions .btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.modal-actions .btn-secondary {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.modal-actions .btn-secondary:hover {
  background-color: var(--bg-primary);
}
</style>
