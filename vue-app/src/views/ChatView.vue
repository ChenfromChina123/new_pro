<template>
  <AppLayout>
    <div class="chat-page">
      <div class="chat-container">
        <!-- 侧边栏：全局导航 + 会话列表 -->
        <aside class="chat-sidebar">
          <div class="sidebar-top">
            <div class="user-profile">
              <div class="user-avatar-wrapper">
                <img 
                  v-if="authStore.userInfo?.avatar" 
                  :src="avatarUrl || authStore.userInfo.avatar" 
                  :alt="authStore.username" 
                  class="sidebar-avatar"
                >
                <i v-else class="fas fa-user default-avatar-icon" />
              </div>
              <span class="sidebar-user-name">{{ authStore.username || '用户' }}</span>
            </div>
            
            <div class="sidebar-actions">
              <button 
                class="sidebar-icon-btn" 
                :title="themeStore.isDarkMode ? '切换到浅色模式' : '切换到深色模式'" 
                @click="themeStore.toggleDarkMode()"
              >
                <i :class="themeStore.isDarkMode ? 'fas fa-sun' : 'fas fa-moon'" />
              </button>
            </div>
          </div>

          <div class="sidebar-nav">
            <div
              class="nav-item"
              :class="{ active: activeNav === 'chat' }"
              @click="activeNav = 'chat'"
            >
              <i class="fas fa-comments" />
              <span>AI问答</span>
            </div>
            <div
              class="nav-item"
              :class="{ active: activeNav === 'cloud-disk' }"
              @click="activeNav = 'cloud-disk'"
            >
              <i class="fas fa-cloud" />
              <span>云盘</span>
            </div>
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

          <div class="sidebar-divider" />

          <!-- 会话列表 (AI问答模式) -->
          <template v-if="activeNav === 'chat'">
            <div class="sidebar-header">
              <button
                class="btn btn-primary new-chat-btn"
                @click="createNewSession"
              >
                <span class="btn-icon">
                  <i class="fas fa-plus" />
                </span>
                <span class="btn-text">新建对话</span>
              </button>
            </div>
            
            <div class="history-section-title">历史对话</div>
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
                  aria-label="删除会话"
                  @click.stop="deleteSession(session.id)"
                >
                  <i class="fas fa-trash" />
                </button>
              </div>
            </div>
          </template>

          <!-- 文件夹树 (云盘模式) -->
          <template v-else-if="activeNav === 'cloud-disk'">
            <div class="sidebar-header">
              <div class="sidebar-section-header">
                <span class="history-section-title">文件夹</span>
                <button
                  class="icon-btn-small"
                  title="新建文件夹"
                  @click="showCreateFolderDialog"
                >
                  <i class="fas fa-plus" />
                </button>
              </div>
            </div>
            
            <div
              class="folder-tree-container"
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

          <div class="sidebar-footer">
            <button class="logout-btn" @click="handleLogout">
              <i class="fas fa-sign-out-alt" />
              <span>退出登录</span>
            </button>
          </div>
        </aside>
        
        <!-- 主内容区域 -->
        <main class="chat-main">
          <!-- AI问答视图 -->
          <template v-if="activeNav === 'chat'">
            <header class="chat-header">
              <div class="chat-header-inner">
                <div class="header-left">
                  <h2 class="chat-title">{{ currentSessionTitle }}</h2>
                </div>
                <div class="header-right">
                  <div class="model-selector-wrapper" v-click-outside="() => showModelSelector = false">
                    <button class="model-selector-btn" @click="showModelSelector = !showModelSelector">
                      <span class="model-name">{{ chatStore.currentModelName }}</span>
                      <i class="fas fa-chevron-down" :class="{ rotated: showModelSelector }" />
                    </button>
                    
                    <transition name="menu-fade">
                      <div v-if="showModelSelector" class="model-dropdown">
                        <div v-for="brand in modelBrands" :key="brand.name" class="brand-section">
                          <div class="brand-header">{{ brand.name }}</div>
                          <div 
                            v-for="model in brand.models" 
                            :key="model.id"
                            class="model-item"
                            :class="{ active: chatStore.currentModel === model.id }"
                            @click="selectModel(model.id)"
                          >
                            <div class="model-item-info">
                              <span class="item-name">{{ model.name }}</span>
                            </div>
                            <i v-if="chatStore.currentModel === model.id" class="fas fa-check check-icon" />
                          </div>
                        </div>
                      </div>
                    </transition>
                  </div>

                  <div class="toolbar-divider" />
                  
                  <button 
                    class="tool-btn" 
                    :class="{ active: chatStore.isDeepThinking }"
                    title="深度思考"
                    @click="toggleDeepThinking"
                  >
                    <i class="fas fa-brain" />
                    <span>深度思考</span>
                  </button>
                </div>
              </div>
            </header>

            <div class="messages-container" ref="messagesContainer">
              <template v-if="chatStore.messages.length > 0">
                <div
                  v-for="message in chatStore.messages"
                  :key="message.id"
                  class="message"
                  :class="message.role"
                >
                  <div class="message-avatar" :class="{ 'has-image': message.role === 'user' && userAvatarUrl }">
                    <template v-if="message.role === 'user'">
                      <img v-if="userAvatarUrl" :src="userAvatarUrl" class="message-avatar-img" alt="User">
                      <i v-else class="fas fa-user" />
                    </template>
                    <i v-else class="fas fa-robot" />
                  </div>
                  <div class="message-content">
                    <div class="message-bubble">
                      <!-- 思考内容 -->
                      <div v-if="message.reasoning_content" class="reasoning-message" :class="{ collapsed: message.isReasoningCollapsed }">
                        <div class="reasoning-header" @click="toggleReasoning(message)">
                          <div class="reasoning-title-wrapper">
                            <i class="fas fa-brain" :class="{ 'fa-spin': message.isReasoning }" />
                            <span>{{ message.isReasoning ? '正在思考...' : '已完成思考' }}</span>
                          </div>
                          <i class="fas fa-chevron-down reasoning-toggle-icon" />
                        </div>
                        <div v-if="!message.isReasoningCollapsed" class="reasoning-body">
                          <div class="reasoning-text" v-html="renderMarkdown(message.reasoning_content)" />
                        </div>
                      </div>
                      
                      <!-- 消息文本 -->
                      <div class="message-text" v-html="renderMarkdown(message.content)" />
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="empty-state">
                <div class="empty-icon">🤖</div>
                <h1 class="empty-title">我是 AI 助手</h1>
                <p class="empty-description">你可以问我任何问题，我会尽力为你解答。让我们开始对话吧！</p>
              </div>
            </div>

            <div class="chat-input-area">
              <div class="chat-input-wrapper">
                <textarea
                  v-model="inputMessage"
                  class="chat-input"
                  placeholder="输入消息..."
                  rows="1"
                  @keydown.enter.prevent="sendMessage"
                  @input="autoResize"
                />
                <div class="input-actions">
                  <button v-if="chatStore.isLoading" class="stop-btn" @click="chatStore.stopGeneration">
                    <div class="stop-icon-wrapper">
                      <i class="fas fa-stop" />
                    </div>
                  </button>
                  <button v-else class="send-btn-new" :disabled="!inputMessage.trim()" @click="sendMessage">
                    <div class="send-icon-wrapper">
                      <i class="fas fa-paper-plane" />
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </template>

          <!-- 云盘视图 -->
          <template v-else-if="activeNav === 'cloud-disk'">
            <CloudDiskMain :show-sidebar="false" />
          </template>
        </main>
      </div>

      <!-- 创建文件夹对话框 -->
      <div
        v-if="showCreateFolder"
        class="modal-overlay"
        @click.self="showCreateFolder = false"
      >
        <div class="modal-content">
          <h3>创建新文件夹</h3>
          <input
            v-model="newFolderName"
            type="text"
            class="input-field"
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
              @click="showCreateFolder = false"
            >
              取消
            </button>
          </div>
        </div>
      </div>

      <!-- 重命名文件夹对话框 -->
      <div
        v-if="showRenameFolder"
        class="modal-overlay"
        @click.self="closeRenameFolderDialog"
      >
        <div class="modal-content">
          <h3>重命名文件夹</h3>
          <input
            v-model="renameFolderName"
            type="text"
            class="input-field"
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
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useCloudDiskStore } from '@/stores/cloudDisk'
import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import AppLayout from '@/components/AppLayout.vue'
import CloudDiskMain from '@/components/CloudDiskMain.vue'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import { API_CONFIG } from '@/config/api'

const chatStore = useChatStore()
const authStore = useAuthStore()
const themeStore = useThemeStore() // 导入主题 store
const cloudDiskStore = useCloudDiskStore()
const router = useRouter() // 导入路由
const inputMessage = ref('')
const messagesContainer = ref(null)

const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value(event)
      }
    }
    document.addEventListener('click', el.clickOutsideEvent)
  },
  unmounted(el) {
    document.removeEventListener('click', el.clickOutsideEvent)
  }
}

const activeNav = ref('chat') // 当前激活的导航项：'chat' 或 'cloud-disk'

// --- 云盘侧边栏逻辑 ---
const expandedFolders = ref(new Set())
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)
const showCreateFolder = ref(false)
const showRenameFolder = ref(false)
const renamingFolder = ref(null)
const renameFolderName = ref('')
const newFolderName = ref('')

/**
 * 判断当前路径是否位于指定文件夹下
 */
const isInActiveChain = (folder) => {
  const folderPath = (folder?.folderPath || '').replace(/\/+$/, '')
  const current = (cloudDiskStore.currentFolder || '').replace(/\/+$/, '')
  if (folderPath === '') return true
  return current.startsWith(folderPath + '/')
}

/**
 * 切换文件夹展开状态
 */
const toggleFolderExpand = (folderId, event) => {
  if (event) event.stopPropagation()
  const next = new Set(expandedFolders.value)
  if (next.has(folderId)) {
    next.delete(folderId)
  } else {
    next.add(folderId)
  }
  expandedFolders.value = next
}

/**
 * 判断文件夹是否展开
 */
const isFolderExpanded = (folder) => {
  if (expandedFolders.value.has(folder.id)) return true
  if (isInActiveChain(folder)) return true
  return false
}

/**
 * 计算文件夹树最大深度
 */
const maxFolderDepth = computed(() => {
  const roots = cloudDiskStore.folders || []
  let max = 0
  const stack = roots.map(r => ({ node: r, depth: 0 }))
  while (stack.length) {
    const { node, depth } = stack.pop()
    if (depth > max) max = depth
    const children = node?.children || []
    for (const child of children) {
      stack.push({ node: child, depth: depth + 1 })
    }
  }
  return max
})

/**
 * 动态计算缩进
 */
const folderIndentPx = computed(() => {
  const depth = maxFolderDepth.value
  const isMobile = viewportWidth.value <= 768
  if (isMobile) return depth > 6 ? 10 : 12
  return depth > 8 ? 10 : depth > 5 ? 12 : 14
})

const selectFolder = async (folderPath, folderId, event) => {
  if (event && typeof event.stopPropagation === 'function') {
    event.stopPropagation()
  }
  cloudDiskStore.setActiveFolder({ folderPath, folderId })
}

const showCreateFolderDialog = () => {
  newFolderName.value = ''
  showCreateFolder.value = true
}

const createFolder = async () => {
  if (!newFolderName.value.trim()) return
  const result = await cloudDiskStore.createFolder(newFolderName.value)
  if (result.success) {
    showCreateFolder.value = false
    newFolderName.value = ''
  } else if (result.error === 'FOLDER_EXISTS') {
    alert('文件夹已存在')
  }
}

/**
 * 删除文件夹操作
 */
const deleteFolderAction = async (folderId) => {
  // 查找文件夹名称用于确认
  const findFolder = (folders, id) => {
    for (const f of folders) {
      if (f.id === id) return f
      if (f.children) {
        const found = findFolder(f.children, id)
        if (found) return found
      }
    }
    return null
  }
  const folder = findFolder(cloudDiskStore.folders, folderId)
  const folderName = folder ? folder.folderName : '该文件夹'

  if (confirm(`确定要删除文件夹 "${folderName}" 及其所有内容吗？`)) {
    const result = await cloudDiskStore.deleteFolder(folderId)
    if (!result.success) {
      alert('删除失败: ' + (result.message || '未知错误'))
    }
  }
}

/**
 * 开启重命名对话框
 */
const renameFolderAction = (folder) => {
  renamingFolder.value = folder
  renameFolderName.value = folder.folderName
  showRenameFolder.value = true
}

/**
 * 关闭重命名对话框
 */
const closeRenameFolderDialog = () => {
  showRenameFolder.value = false
  renamingFolder.value = null
  renameFolderName.value = ''
}

/**
 * 确认重命名
 */
const confirmRenameFolder = async () => {
  if (!renameFolderName.value.trim() || !renamingFolder.value) return
  
  if (renameFolderName.value === renamingFolder.value.folderName) {
    closeRenameFolderDialog()
    return
  }

  const result = await cloudDiskStore.renameFolder(
    renamingFolder.value.id,
    renameFolderName.value.trim()
  )

  if (result.success) {
    closeRenameFolderDialog()
  } else if (result.error === 'FOLDER_EXISTS') {
    alert('文件夹名称已存在')
  } else {
    alert('重命名失败: ' + (result.message || '未知错误'))
  }
}
// --- 云盘侧边栏逻辑结束 ---

const showModelSelector = ref(false)
const modelBrands = [
  {
    name: '豆包',
    models: [
      { id: 'doubao-pro-32k', name: 'Doubao Pro' },
      { id: 'doubao-lite-32k', name: 'Doubao Lite' }
    ]
  },
  {
    name: 'DeepSeek',
    models: [
      { id: 'deepseek-chat', name: 'DeepSeek Chat' },
      { id: 'deepseek-reasoner', name: 'DeepSeek Reasoner' }
    ]
  }
]

const selectModel = (modelId) => {
  chatStore.selectedModel = modelId
  showModelSelector.value = false
}

const toggleDeepThinking = () => {
  chatStore.isDeepThinking = !chatStore.isDeepThinking
  
  // 自动切换逻辑
  if (chatStore.isDeepThinking) {
    if (chatStore.selectedModel.includes('deepseek')) {
      chatStore.selectedModel = 'deepseek-reasoner'
    }
  } else {
    if (chatStore.selectedModel === 'deepseek-reasoner') {
      chatStore.selectedModel = 'deepseek-chat'
    }
  }
}

const autoResize = (event) => {
  const textarea = event.target
  textarea.style.height = 'auto'
  textarea.style.height = textarea.scrollHeight + 'px'
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const renderMarkdown = (content) => {
  if (!content) return ''
  const placeholders = []
  let processedContent = renderMathFormula(content, placeholders)
  let html = marked(processedContent)
  placeholders.forEach((mathHtml, index) => {
    html = html.replace(`MATH-PLACEHOLDER-${index}-END`, mathHtml)
  })
  return html
}

const avatarUrl = ref(null) // 用于侧边栏头像
const userAvatarUrl = ref(null) // 用于消息列表头像

// 退出登录逻辑
const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    authStore.logout()
    router.push('/login')
  }
}

// 侧边栏头像逻辑
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
          const url = URL.createObjectURL(blob)
          avatarUrl.value = url
          userAvatarUrl.value = url // 同时更新消息头像
        } else {
          avatarUrl.value = null
          userAvatarUrl.value = null
        }
      } catch {
        avatarUrl.value = null
        userAvatarUrl.value = null
      }
    } else {
      avatarUrl.value = null
      userAvatarUrl.value = null
    }
  },
  { immediate: true }
)

/**
 * 解析并返回消息头像图片地址；无可用图片时返回 `null` 以回退到默认图标
 */
const deepseekAvatarUrl = new URL('../../static/image/deepseek-image.png', import.meta.url).href
const doubaoAvatarUrl = new URL('../../static/image/doubao-imge.png', import.meta.url).href

const getMessageAvatarSrc = (message) => {
  if (!message) return null
  if (message.role === 'user') {
    return userAvatarUrl.value || null
  }
  const model = String(message.model || chatStore.selectedModel || '').toLowerCase()
  if (model.includes('deepseek')) return deepseekAvatarUrl
  if (model.includes('doubao')) return doubaoAvatarUrl
  return null
}

// 复制代码到剪贴板 - 改为全局函数，供内联事件调用
window.copyCodeBlock = (element) => {
  const code = element.previousElementSibling.textContent
  const button = element
  navigator.clipboard.writeText(code)
    .then(() => {
      // 显示复制成功的反馈
      const originalText = button.textContent
      button.textContent = '已复制!'
      button.classList.add('copied')
      setTimeout(() => {
        button.textContent = originalText
        button.classList.remove('copied')
      }, 2000)
    })
    .catch(err => {
      console.error('复制失败:', err)
    })
}

// 自定义marked渲染器，直接在渲染时添加复制按钮
const renderer = new marked.Renderer()
const originalCode = renderer.code
renderer.code = function(code, language, escaped) {
  const originalResult = originalCode.call(this, code, language, escaped)
  // 在pre标签内添加复制按钮
  return originalResult.replace('<pre', '<pre style="position: relative">')
    .replace('</pre>', '<button class="copy-button" onclick="copyCodeBlock(this)">复制</button></pre>')
}

// 配置marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  breaks: true,
  gfm: true,
  renderer: renderer // 使用自定义渲染器
})

const currentSessionTitle = computed(() => {
  return chatStore.currentSession?.title || '新对话'
})

onMounted(async () => {
  await chatStore.fetchSessions()
  
  // 如果没有当前会话，创建一个新的
  if (!chatStore.currentSessionId && chatStore.sessions.length === 0) {
    await createNewSession()
  } else if (chatStore.sessions.length > 0 && !chatStore.currentSessionId) {
    await loadSession(chatStore.sessions[0].id)
  }
})

watch(
  () => authStore.userInfo?.avatar,
  async (path) => {
    if (userAvatarUrl.value) {
      URL.revokeObjectURL(userAvatarUrl.value)
      userAvatarUrl.value = null
    }
    if (!path) return
    try {
      const res = await fetch(`${API_CONFIG.baseURL}${path}`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!res.ok) return
      const blob = await res.blob()
      userAvatarUrl.value = URL.createObjectURL(blob)
    } catch {
      userAvatarUrl.value = null
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  if (userAvatarUrl.value) {
    URL.revokeObjectURL(userAvatarUrl.value)
    userAvatarUrl.value = null
  }
})

// 监听消息变化，自动滚动到底部
watch(
  [
    () => chatStore.messages.length,
    () => chatStore.messages[chatStore.messages.length - 1]?.content,
    () => chatStore.messages[chatStore.messages.length - 1]?.reasoning_content
  ],
  () => {
    nextTick(() => {
      scrollToBottom()
    })
  }
)

const createNewSession = async () => {
  const result = await chatStore.createSession()
  if (result.success) {
    inputMessage.value = ''
  }
}

const loadSession = async (sessionId) => {
  await chatStore.fetchSessionMessages(sessionId)
  scrollToBottom()
}

const deleteSession = async (sessionId) => {
  if (confirm('确定要删除这个会话吗？')) {
    await chatStore.deleteSession(sessionId)
  }
}

const toggleReasoning = (message) => {
  message.isReasoningCollapsed = !message.isReasoningCollapsed
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || chatStore.isLoading) return
  
  // 如果没有当前会话，先创建一个
  if (!chatStore.currentSessionId) {
    await createNewSession()
  }
  
  const message = inputMessage.value.trim()
  inputMessage.value = ''
  
  // 重置输入框高度
  const textarea = document.querySelector('.chat-input')
  if (textarea) {
    textarea.style.height = 'auto'
  }
  
  await chatStore.sendMessage(message, () => {
    nextTick(() => scrollToBottom())
  })
}

// 渲染数学公式
const renderMathFormula = (content, placeholders = []) => {
  // 1. 先处理特定格式的公式，比如用户提供的截图中的格式
  let processedContent = content;
  
  // 0. 预处理：标准化 LaTeX 定界符和转义符
  // 处理双反斜杠转义问题 (例如 \\int -> \int, \\( -> \()
  // 仅处理常见的数学命令和定界符，避免破坏换行符 \\
  processedContent = processedContent.replace(/\\\\(int|sqrt|frac|left|right|,|d[xyt]|sigma|alpha|beta|gamma|pi|theta|infty|cdot|approx|le|ge|ne|equiv|sum|lim|to)/g, '\\$1');
  processedContent = processedContent.replace(/\\\\([\[\]()])/g, '\\$1');

  // 辅助函数：清理捕获内容中的HTML标签
  const cleanTags = (str) => {
    if (!str) return '';
    // 移除常见的块级和内联标签，避免破坏公式结构
    return str.replace(/<\/?(li|ul|ol|p|div|span|br|h\d|strong|em)[^>]*>/gi, "").trim();
  };

  // 辅助函数：创建占位符并存储KaTeX结果
  const createPlaceholder = (formula, displayMode) => {
    try {
      const html = katex.renderToString(formula, {
        throwOnError: false,
        displayMode: displayMode
      });
      const index = placeholders.length;
      placeholders.push(html);
      return `MATH-PLACEHOLDER-${index}-END`;
    } catch (error) {
      console.error('KaTeX渲染错误:', error);
      return formula;
    }
  };
  
  // 0.5 优先处理标准 LaTeX 块级和行内公式定界符
  // 处理 $$ ... $$
  processedContent = processedContent.replace(/\$\$([\s\S]+?)\$\$/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula);
    return createPlaceholder(cleanedFormula, true);
  });

  // 处理 \[ ... \]
  processedContent = processedContent.replace(/\\\[([\s\S]+?)\\\]/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula);
    return createPlaceholder(cleanedFormula, true);
  });

  // 处理 \( ... \)
  processedContent = processedContent.replace(/\\\(([\s\S]+?)\\\)/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula);
    return createPlaceholder(cleanedFormula, false);
  });

  // 处理带有<br>标签的定积分公式
  const brFormulaRegex = /<br\s*\/?>\\int\s*_{(\d+)}^\{(\d+)}\s*(\d+)x\s*,\s*dx\s*=\s*F\((\d+)\)\s*-\s*F\((\d+)\)\s*=\s*\((\d+)\^2\)\s*-\s*\((\d+)\^2\)\s*=\s*(\d+)\s*-\s*(\d+)\s*=\s*(\d+)<br\s*\/?>/g;
  processedContent = processedContent.replace(brFormulaRegex, (match, lower, upper, coeff, fUpper, fLower, squareUpper, squareLower, val1, val2, result) => {
    const formula = `\\int_{${lower}}^{${upper}} ${coeff}x dx = F(${fUpper}) - F(${fLower}) = (${squareUpper}^2) - (${squareLower}^2) = ${val1} - ${val2} = ${result}`;
    return createPlaceholder(formula, true);
  });
  
  // 2. 处理基本定积分公式
  const basicIntegralRegex = /\\int\s*_{(\w+)}^\{(\w+)}\s*f\(x\)\s*,\s*dx\s*=\s*F\((\w+)\)\s*-\s*F\((\w+)\)/g;
  processedContent = processedContent.replace(basicIntegralRegex, (match, lower, upper, fUpper, fLower) => {
    const formula = `\\int_{${lower}}^{${upper}} f(x) dx = F(${fUpper}) - F(${fLower})`;
    return createPlaceholder(formula, true);
  });
  
  // 2.5 处理带 \left. ... \right| 的完整积分公式（优先匹配，因为更具体）
  // 匹配如：\int_{a}^{b} ... dx = \left. ... \right|_{a}^{b} = ...
  // 支持负号下标，如 \int_{-\pi}^{\pi}
  const integralWithEvalRegex = /\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?d(?:[a-z]+|\\[a-zA-Z]+)\s*=\s*\\left\.[^\n]*?\\right\|(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?(?:\s*=\s*[^\n]*?)?/g;
  processedContent = processedContent.replace(integralWithEvalRegex, (match) => {
    let cleanedMatch = cleanTags(match);
    const intIndex = cleanedMatch.indexOf('\\int');
    if (intIndex < 0) return match;
    let formula = cleanedMatch.substring(intIndex);
    // 移除末尾的中文和多余标点
    formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '');
    formula = formula.replace(/\s*\)\s*\]\s*$/, '').replace(/\s*\]\s*$/, '');
    if (formula && formula.includes('\\int') && formula.includes('\\left.')) {
      return createPlaceholder(formula.trim(), true);
    }
    return match;
  });
  
  // 3. 处理导数基本公式
  const derivativeRegex = /\\left\(([^)]+)\\right\)'\s*=\s*([^\n]+?)(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,)/g;
  processedContent = processedContent.replace(derivativeRegex, (match, func, result) => {
    let cleanedResult = cleanTags(result);
    // 移除编号和中文文本，但保留LaTeX命令
    cleanedResult = cleanedResult.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
    cleanedResult = cleanedResult.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
    cleanedResult = cleanedResult.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
    if (cleanedResult) {
    const formula = `\\left(${func}\\right)' = ${cleanedResult}`;
    return createPlaceholder(formula, false);
    }
    return match;
  });
  
  // 4. 处理积分基本公式（带逗号的格式）
  const integralRegex = /\\int\s*([^,\n]+?)\s*,\s*dx\s*=\s*([^\n]+?)(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,)/g;
  processedContent = processedContent.replace(integralRegex, (match, integrand, result) => {
    let cleanedResult = cleanTags(result);
    // 移除编号和中文文本，但保留LaTeX命令
    cleanedResult = cleanedResult.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
    cleanedResult = cleanedResult.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
    cleanedResult = cleanedResult.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
    if (cleanedResult) {
    const formula = `\\int ${integrand} dx = ${cleanedResult}`;
    return createPlaceholder(formula, true);
    }
    return match;
  });
  
  // 4.5 处理不带逗号的积分公式（\int_{a}^{b} ... dx = ...）
  // 支持负号下标和等号后面的多个积分
  const integralNoCommaRegex = /\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?\s+dx\s*=\s*[^\n]*?(?:\\int[^\n]*?dx[^\n]*?)?[^\n]+?(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,|\.)/g;
  processedContent = processedContent.replace(integralNoCommaRegex, (match) => {
    let cleanedMatch = cleanTags(match);
    const intIndex = cleanedMatch.indexOf('\\int');
    if (intIndex < 0) return match;
    let formula = cleanedMatch.substring(intIndex);
    formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '').trim();
    if (formula && formula.includes('\\int') && /\s+dx\s*=/.test(formula)) {
      return createPlaceholder(formula, true);
    }
    return match;
  });
  
  // 4.6 处理简单积分（无上下限的），如 \int x e^x dx
  const simpleIntegralRegex = /\\int\s+[^\n]+?\s+d(?:[a-z]+|\\[a-zA-Z]+)(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,|\.)/g;
  processedContent = processedContent.replace(simpleIntegralRegex, (match) => {
    let cleanedMatch = cleanTags(match);
    const intIndex = cleanedMatch.indexOf('\\int');
    if (intIndex < 0) return match;
    let formula = cleanedMatch.substring(intIndex);
    // 移除末尾的中文和多余标点
    formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '');
    formula = formula.replace(/\s*\)\s*\]\s*$/, '').replace(/\s*\]\s*$/, '');
    if (formula && formula.includes('\\int') && /\bd[a-z]+\b/.test(formula)) {
      return createPlaceholder(formula.trim(), true);
    }
    return match;
  });
  
  // 5. 处理分式 (已废弃，避免破坏复杂公式结构)
  // const fracRegex = /\\frac{([^}]+)}{([^}]+)}/g;
  // processedContent = processedContent.replace(fracRegex, (match, numerator, denominator) => {
  //   const formula = `\\frac{${numerator}}{${denominator}}`;
  //   return createPlaceholder(formula, false);
  // });

  // 6. 新增：通用积分公式匹配 (针对 \int_{a}^{b} x^n dx 这种未被特定规则捕获的情况)
  // 匹配完整的积分公式，包括：
  // - \int_{a}^{b} ... dx = ... 的形式
  // - \int_{-a}^{a} ... dx = 2 \int_{0}^{a} ... dx (包含多个积分)
  // - \int x e^x dx (无上下限的简单积分)
  // 使用更宽松的匹配，确保能捕获完整的公式（包括等号后面的所有内容，可能包含多个积分）
  // 改进：匹配到等号后，继续匹配可能存在的第二个积分
  const generalIntegralRegex = /\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?d(?:[a-z]+|\\[a-zA-Z]+)(?:\s*=\s*[^\n]*?(?:\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?d(?:[a-z]+|\\[a-zA-Z]+))?[^\n]*?)?(?=[\s\u4e00-\u9fa5]|$|\[|\(|\)|,|\.\s)/g;
  processedContent = processedContent.replace(generalIntegralRegex, (match, offset, string) => {
     // 清理匹配内容：移除HTML标签
     let cleanedMatch = cleanTags(match);
     
     // 找到公式的起始位置（\int的位置）
     const intIndex = cleanedMatch.indexOf('\\int');
     if (intIndex < 0) {
       return match; // 如果没有找到\int，返回原匹配
     }
     
     // 从\int开始提取公式
     let formula = cleanedMatch.substring(intIndex);
     
     // 移除公式末尾的中文和多余标点，但保留公式结构
     // 找到公式的实际结束位置（最后一个数学符号或括号）
     formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '');
     // 移除末尾多余的括号和方括号，但保留公式中的括号
     formula = formula.replace(/\s*\)\s*\]\s*$/, ''); // 只移除末尾的 )]
     formula = formula.replace(/\s*\]\s*$/, ''); // 移除末尾的 ]
     
     // 确保公式完整（至少包含\int和dx或dt等）
     if (formula.trim() && formula.includes('\\int') && /\bd[a-z]+\b/.test(formula)) {
       return createPlaceholder(formula.trim(), true);
     }
     return match; // 如果清理后无效，返回原匹配
  });

  // 7. 新增：处理 \left| ... \right| 绝对值/范数
  const absRegex = /\\left\|[^\n]+?\\right\|/g;
  processedContent = processedContent.replace(absRegex, (match) => {
      let cleanedMatch = cleanTags(match);
      // 移除编号和中文文本，但保留LaTeX命令
      cleanedMatch = cleanedMatch.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
      cleanedMatch = cleanedMatch.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
      cleanedMatch = cleanedMatch.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
      if (cleanedMatch && cleanedMatch.includes('\\left|')) {
      return createPlaceholder(cleanedMatch, false);
      }
      return match;
  });

  // 8. 新增：处理 \left. ... \right| 代换值
  const evalRegex = /\\left\.[^\n]+?\\right\|(?:_\{[^}]+\})?(?:\^\{[^}]+\})?/g;
  processedContent = processedContent.replace(evalRegex, (match) => {
      let cleanedMatch = cleanTags(match);
      // 移除编号和中文文本，但保留LaTeX命令
      cleanedMatch = cleanedMatch.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
      cleanedMatch = cleanedMatch.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
      cleanedMatch = cleanedMatch.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
      if (cleanedMatch && cleanedMatch.includes('\\left.')) {
      return createPlaceholder(cleanedMatch, false);
      }
      return match;
  });
  
  // 9. 清理HTML标签 (在占位符替换后做)
  processedContent = processedContent.replace(/<br\s*\/?>/g, ' ');
  
  return processedContent;
};

const restoreMathFormula = (content, placeholders) => {
  // 1. 先尝试清理被marked错误包裹在代码块中的占位符
  // 处理 <code>MATH-PLACEHOLDER-0-END</code> 或 <pre><code>...</code></pre>
  // 更强的正则：匹配带有属性的code标签，以及多行情况
  let html = content.replace(/<pre[^>]*>\s*<code[^>]*>\s*(MATH-PLACEHOLDER-(\d+)-END)\s*<\/code>\s*<\/pre>/gi, '$1');
  html = html.replace(/<code[^>]*>\s*(MATH-PLACEHOLDER-(\d+)-END)\s*<\/code>/gi, '$1');
  
  // 2. 还原占位符
  return html.replace(/MATH-PLACEHOLDER-(\d+)-END/g, (match, index) => {
    return placeholders[parseInt(index)] || match;
  });
};

const sanitizeNullRuns = (content) => {
  if (typeof content !== 'string') return content
  return content.replace(/(?:null){2,}/g, '')
}

const formatMessage = (content) => {
  try {
    // 1. 先清理原始内容中的问题
    let cleanContent = content;

    // 0. 预处理：移除可能包裹公式的 Markdown 代码标记 (反引号)
    // AI 有时会输出 `\( x^2 \)` 导致公式被渲染为代码块
    // 移除包裹 $$ ... $$ 的反引号
    cleanContent = cleanContent.replace(/`(\$\$[\s\S]+?\$\$)`/g, '$1');
    // 移除包裹 \[ ... \] 的反引号
    cleanContent = cleanContent.replace(/`(\\\[[\s\S]+?\\\])`/g, '$1');
    // 移除包裹 \( ... \) 的反引号
    cleanContent = cleanContent.replace(/`(\\\([\s\S]+?\\\))`/g, '$1');
    // 移除包裹 \int ... 的反引号 (需要匹配到对应的结束反引号)
    // 匹配 `\int ... `，确保内部不包含反引号
    cleanContent = cleanContent.replace(/`(\\int(?:\\[\s\S]|[^`])+?)`/g, '$1');
    
    // 预处理：清理公式行中的编号（如 "5."、"1." 等）
    // 只处理包含LaTeX命令的行，避免误删正常文本
    // 匹配模式：行首编号 + 可选中文 + LaTeX命令
    // 注意：这里只清理行首的编号，不清理公式内部的编号
    cleanContent = cleanContent.replace(/^(\s*)(\d+\.\s*)([\u4e00-\u9fa5：:，,。.；;！!？?\s]*?)(\\int|\\left|\\right|\\frac|\\sqrt|\\sum|\\lim|\\sin|\\cos|\\tan|\\sec|\\ln|\\log|\\exp)/gm, '$1$4');
    
    // 处理列表符号和多余括号
    cleanContent = cleanContent.replace(/^\s*\s*\(/g, '');
    cleanContent = cleanContent.replace(/\)\s*$/g, '');
    
    // 处理HTML标签问题 - 更强的正则，包含转义字符
    cleanContent = cleanContent.replace(/&lt;\s*\/?\s*(li|ul|ol|p|br|div|span|strong|em)\s*&gt;/gi, '');
    cleanContent = cleanContent.replace(/<\s*\/?\s*(li|ul|ol|p|div|span)\s*>/gi, '');
    // 处理带有空格的标签，如 < br >
    cleanContent = cleanContent.replace(/<\s*br\s*\/?\s*>/gi, ' ');
    // 强力清除 strong 和 em 标签及其空格变体 (如 < strong >)
    cleanContent = cleanContent.replace(/<\s*\/?\s*(strong|em)\s*>/gi, '');
    
    // 处理数学符号问题：将错误显示的符号替换为正确的
    cleanContent = cleanContent.replace(/目/g, '≠');
    
    // 移除公式周围的双重括号 ((...)) -> ...
    // 使用 [\s\S]*? 非贪婪匹配任意字符(包括换行)，直到遇到 ))
    cleanContent = cleanContent.replace(/\(\(([\s\S]*?)\)\)/g, '$1');
    // 暴力修复：直接将 (( 替换为 (，将 )) 替换为 )
    cleanContent = cleanContent.replace(/\(\(/g, '(');
    cleanContent = cleanContent.replace(/\)\)/g, ')');

    // 处理导数公式 ((...)' = ...)
    cleanContent = cleanContent.replace(/\(\(([\s\S]*?)\)\)'\s*=/g, "($1)' =");
    
    // 去除公式周围的多余括号 ( \int ... ) -> \int ...
    cleanContent = cleanContent.replace(/\(\s*\\int/g, '\\int');
    // 去除结尾的多余双括号 (针对 ...)) 的情况)
    cleanContent = cleanContent.replace(/\)\)\s*$/gm, ')');
    // 尝试去除单独行的右括号
    cleanContent = cleanContent.replace(/^\s*\)\s*$/gm, '');
    // 尝试去除单独行的左括号
    cleanContent = cleanContent.replace(/^\s*\(\s*$/gm, '');
    
    // 处理行尾的多余方括号 ] (通常是AI生成的格式错误)
    // 匹配非转义的 ] 出现在行尾的情况
    cleanContent = cleanContent.replace(/([^\\])\]\s*$/gm, '$1');
    // 处理行首的多余方括号 [
    cleanContent = cleanContent.replace(/^\s*\[/gm, '');

    // 处理区间表示中的错误：(la, b]) → [a, b]
    cleanContent = cleanContent.replace(/\(la,\s*b\]\)/g, '[a, b]');
    cleanContent = cleanContent.replace(/\(a,\s*b\)\)/g, '[a, b]');
    
    // 处理函数表示中的多余括号：(f(x)) → f(x)
    cleanContent = cleanContent.replace(/\(f\(x\)\)/g, 'f(x)');
    cleanContent = cleanContent.replace(/\(F\(x\)\)/g, 'F(x)');
    
    // 处理导数和等式中的多余括号
    cleanContent = cleanContent.replace(/\(F'\(x\)\s*=\s*/g, "F'(x) = ");
    cleanContent = cleanContent.replace(/\s*\(f\(x\)\)\)/g, " f(x)");
    
    // 处理文本中的多余括号
    cleanContent = cleanContent.replace(/\(即\s*/g, "即 ");
    cleanContent = cleanContent.replace(/\(\)/g, "");
    cleanContent = cleanContent.replace(/\)\)/g, ")");
    
    // 处理公式周围的多余括号和方括号
    cleanContent = cleanContent.replace(/\[\s*\\int/g, '\\int');
    cleanContent = cleanContent.replace(/dx\s*\]/g, 'dx');
    
    // 2. 先识别并保护数学公式 (生成占位符)
    const placeholders = [];
    let contentWithPlaceholders = renderMathFormula(cleanContent, placeholders);

    // 3. 使用marked解析Markdown (此时公式已被占位符保护，不会被marked破坏)
    let html = marked.parse(contentWithPlaceholders);
    
    // 4. 还原数学公式 (将占位符替换回KaTeX生成的HTML)
    html = restoreMathFormula(html, placeholders);
    
    // 5. 清理多余的HTML标签和格式问题
    html = html.replace(/\[\s*<p>\s*/g, '<p>');
    html = html.replace(/\s*<\/p>\s*\]/g, '</p>');
    html = html.replace(/<br\s*\/?>\]\s*<\/p>/g, '</p>');
    html = html.replace(/<br\s*\/?>/g, ' ');
    
    // 6. 处理公式周围的多余字符 (仅处理行首行尾的方括号，避免误删数学符号)
    html = html.replace(/^\s*\[/g, '');
    html = html.replace(/\]\s*$/g, '');
    
    return html;
  } catch (error) {
    console.error('消息格式化错误:', error);
    return content;
  }
}

// 复制消息内容
const copyMessage = (content) => {
  // 从HTML中提取纯文本
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = content
  const plainText = tempDiv.textContent || tempDiv.innerText || ''
  
  // 复制到剪贴板
  navigator.clipboard.writeText(plainText)
    .then(() => {
      // 显示复制成功的反馈
      const button = event.target.closest('.message-copy-button')
      if (button) {
        const originalText = button.innerHTML
        button.innerHTML = '<i class="fas fa-check"></i><span class="copy-text">已复制</span>'
        button.classList.add('copied')
        setTimeout(() => {
          button.innerHTML = originalText
          button.classList.remove('copied')
        }, 2000)
      }
    })
    .catch(err => {
      console.error('复制失败:', err)
    })
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatSessionDate = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diffTime = Math.abs(now - date)
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) {
    return '今天 '
  } else if (diffDays === 1) {
    return '昨天 '
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const isModelMenuOpen = ref(false)
const modelMenuRef = ref(null)

const brands = [
  {
    id: 'deepseek',
    name: 'DeepSeek',
    icon: 'fas fa-brain',
    standard: 'deepseek-chat',
    reasoner: 'deepseek-reasoner'
  },
  {
    id: 'doubao',
    name: '豆包',
    icon: 'fas fa-robot',
    standard: 'doubao',
    reasoner: 'doubao-reasoner'
  }
]

// 获取当前选中的品牌
const currentBrand = computed(() => {
  const model = chatStore.selectedModel
  return brands.find(b => model === b.standard || model === b.reasoner) || brands[0]
})

// 切换品牌
const selectBrand = (brand) => {
  const isReasoning = chatStore.selectedModel.includes('reasoner')
  const newModel = isReasoning ? brand.reasoner : brand.standard
  chatStore.setModel(newModel)
  isModelMenuOpen.value = false
}

// 切换深度思考
const toggleDeepThinking = () => {
  const brand = currentBrand.value
  const isReasoning = chatStore.selectedModel.includes('reasoner')
  const newModel = isReasoning ? brand.standard : brand.reasoner
  chatStore.setModel(newModel)
}

// 点击外部关闭菜单
onMounted(() => {
  const handleClickOutside = (event) => {
    if (modelMenuRef.value && !modelMenuRef.value.contains(event.target)) {
      isModelMenuOpen.value = false
    }
  }
  document.addEventListener('click', handleClickOutside)
  onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
  })
})

const adjustTextareaHeight = (event) => {
  const textarea = event.target
  textarea.style.height = 'auto'
  textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`
}
</script>

<style scoped>
.chat-page {
  height: 100vh;
  overflow: hidden;
  background-color: var(--bg-primary);
}

.chat-container {
  display: flex;
  height: 100%;
  width: 100%;
  max-width: 100%;
  margin: 0;
  background-color: var(--bg-secondary);
  box-shadow: none;
  border-radius: 0;
  overflow: hidden;
}

.chat-sidebar {
  width: 300px;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  height: 100%;
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
  gap: 14px;
  flex: 1;
  min-width: 0;
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
}

.sidebar-icon-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-icon-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
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

.sidebar-nav {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sidebar-nav .nav-item {
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

.sidebar-nav .nav-item:hover {
  background-color: var(--bg-tertiary);
}

.sidebar-nav .nav-item.active {
  background-color: #ebf5ff;
  color: #2563eb;
  font-weight: 500;
}

.sidebar-divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 8px 16px;
}

.sidebar-header {
  padding: 12px 20px;
}

.new-chat-btn {
  display: flex !important;
  align-items: center;
  justify-content: center !important;
  gap: 8px;
  padding: 12px 16px !important;
  border-radius: 12px !important;
  background: #1d4ed8 !important; /* 使用明确的深蓝色 */
  color: #ffffff !important; /* 确保文字是纯白色 */
  border: none !important;
  font-weight: 600 !important;
  width: 100%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
  cursor: pointer;
}

.new-chat-btn:hover {
  background-color: #1e40af !important; /* 悬停时颜色加深 */
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
}

.new-chat-btn:active {
  transform: translateY(0);
}

.new-chat-btn .btn-text,
.new-chat-btn .btn-icon,
.new-chat-btn i {
  color: #ffffff !important; /* 强制所有内部元素为白色 */
}

.history-section-title {
  padding: 16px 16px 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar-title {
  display: none;
}

.sidebar-header .btn {
  width: 100%;
  justify-content: center;
}

.btn-icon {
  font-size: 14px;
}

.btn-text {
  font-size: 14px;
  letter-spacing: 0.2px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.session-list::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: 3px;
}

.session-list::-webkit-scrollbar-thumb {
  background: var(--gray-300);
  border-radius: 3px;
}

.session-list::-webkit-scrollbar-thumb:hover {
  background: var(--gray-400);
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  margin-bottom: 6px;
  border-radius: var(--border-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: var(--bg-secondary);
  border: 1px solid transparent;
}

.session-item:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--border-color);
  transform: translateX(2px);
}

.session-item.active {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
  letter-spacing: 0.2px;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-date {
  font-size: 12px;
  color: var(--text-tertiary);
}

.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  opacity: 0;
  transition: all 0.2s ease;
  color: var(--text-tertiary);
  padding: 4px;
  border-radius: var(--border-radius-sm);
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: var(--danger-color);
  background-color: rgba(239, 68, 68, 0.1);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
  position: relative;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 24px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.chat-header-inner {
  width: 100%;
  max-width: 980px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  scroll-behavior: smooth;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: var(--gray-300);
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: var(--gray-400);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
  padding: 40px 20px;
  width: 100%;
  max-width: 980px;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
  opacity: 0.8;
}

.empty-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.empty-description {
  font-size: 16px;
  color: var(--text-secondary);
  text-align: center;
  max-width: 400px;
  line-height: 1.6;
}

.message {
  display: flex;
  gap: 16px;
  margin-bottom: 28px;
  animation: slideUp 0.3s ease-out;
  padding: 0 8px;
  width: 100%;
  max-width: 980px;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  justify-content: flex-end;
}

.message-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: var(--shadow-md);
  transition: all 0.2s ease;
}

.message-avatar.has-image {
  background: transparent;
}

.message-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.message-avatar:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-lg);
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message-content {
  flex: 1;
  max-width: 80%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.message.user .message-content {
  align-items: flex-end;
}

.message-bubble {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: fit-content;
}

.message.user .message-bubble {
  align-items: flex-end;
}

.message-text {
  padding: 16px 20px;
  border-radius: var(--border-radius-lg);
  line-height: 1.65;
  word-wrap: break-word;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  font-size: 16px;
  letter-spacing: 0.2px;
}

.message-text:hover {
  box-shadow: var(--shadow-md);
}

.message.assistant .message-text {
  background-color: transparent;
  color: var(--text-primary);
  border: none;
  box-shadow: none;
  padding: 8px 0;
  font-size: 17px;
  line-height: 1.8;
}

.message.assistant .message-text :deep(h1),
.message.assistant .message-text :deep(h2),
.message.assistant .message-text :deep(h3) {
  margin: 24px 0 12px;
  color: var(--text-primary);
  font-weight: 600;
}

.message.assistant .message-text :deep(p) {
  margin-bottom: 12px;
}

.message.assistant .message-text :deep(ul),
.message.assistant .message-text :deep(ol) {
  margin-bottom: 12px;
  padding-left: 24px;
}

.message.assistant .message-text :deep(li) {
  margin-bottom: 6px;
}

.message.user .message-text {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: none;
}

.message-text :deep(pre) {
    background-color: var(--bg-tertiary); /* 在白天模式下是浅色，夜间模式下是深色 */
    color: var(--text-primary); /* 在白天模式下是深色，夜间模式下是浅色 */
    padding: 16px;
    border-radius: var(--border-radius-md);
    overflow-x: auto;
    margin: 12px 0;
    box-shadow: var(--shadow-sm);
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    line-height: 1.5;
    position: relative;
    border: 1px solid var(--border-color); /* 添加边框 */
  }

.message-text :deep(code) {
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    background-color: transparent; /* 行内代码背景透明 */
    color: var(--text-primary); /* 使用主题文字颜色 */
    padding: 0;
    border-radius: 0;
  }

/* 复制按钮样式 */
.copy-button {
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: rgba(255, 255, 255, 0.9); /* 半透明白色背景 */
  color: var(--text-primary);
  border: 1px solid var(--border-color); /* 添加边框 */
  border-radius: var(--border-radius-md); /* 使用更大的圆角 */
  padding: 8px 16px; /* 增加内边距 */
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  z-index: 100;
  /* 确保按钮位于右上角 */
  margin: 0;
  transform: none;
  box-sizing: border-box;
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 深色模式下的复制按钮样式 */
body.dark-mode .copy-button {
  background-color: rgba(31, 41, 55, 0.95); /* 深色背景 */
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 确保代码块是相对定位的容器 */
.message-text :deep(pre) {
  position: relative !important;
}

/* 确保复制按钮样式正确应用 */
.message-text :deep(pre) .copy-button {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 100;
}

.message-text :deep(pre):hover .copy-button {
  opacity: 1;
}

.copy-button:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  opacity: 1 !important;
}

.copy-button:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
}

.copy-button.copied {
  background-color: var(--success-color);
  color: white;
  border-color: var(--success-color);
  animation: copiedPulse 0.6s ease-in-out;
  box-shadow: 0 0 0 3px rgba(74, 222, 128, 0.2);
}

@keyframes copiedPulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 8px rgba(74, 222, 128, 0);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(74, 222, 128, 0);
  }
}

.message-text :deep(a) {
  color: var(--primary-color);
  text-decoration: none;
  border-bottom: 1px solid rgba(29, 78, 216, 0.3);
  transition: all 0.2s ease;
}

.message-text :deep(a:hover) {
  border-bottom-color: var(--primary-color);
}

.message.user .message-text :deep(a) {
  color: #a5b4fc;
  border-bottom-color: rgba(165, 180, 252, 0.5);
}

.message.user .message-text :deep(a:hover) {
  color: white;
  border-bottom-color: white;
}

/* 表格样式 */
.message-text :deep(table) {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin: 16px 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  display: table;
}

.message-text :deep(th) {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
  border-right: 1px solid var(--border-color);
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 14px;
}

.message-text :deep(td) {
  border-bottom: 1px solid var(--border-color);
  border-right: 1px solid var(--border-color);
  padding: 12px 16px;
  vertical-align: top;
  font-size: 14px;
}

.message-text :deep(tr:last-child td) {
  border-bottom: none;
}

.message-text :deep(th:last-child),
.message-text :deep(td:last-child) {
  border-right: none;
}

.message-text :deep(tr:nth-child(even)) {
  background-color: rgba(0, 0, 0, 0.02);
}

.message-text :deep(tr:hover) {
  background-color: var(--toolbar-btn-bg);
}

/* 消息复制按钮样式 */
.message-copy-button {
  position: absolute;
  bottom: -24px;
  background-color: rgba(255, 255, 255, 0.9); /* 半透明白色背景 */
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md); /* 使用更大的圆角 */
  padding: 8px 16px; /* 增加内边距 */
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10;
  box-shadow: var(--shadow-sm);
}

/* 深色模式下的消息复制按钮样式 */
body.dark-mode .message-copy-button {
  background-color: rgba(31, 41, 55, 0.95); /* 深色背景 */
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* AI消息（左对齐）的复制按钮在下方靠左 */
.message.assistant .message-copy-button {
  left: 0;
}

/* 用户消息（右对齐）的复制按钮在下方靠右 */
.message.user .message-copy-button {
  right: 0;
}

.message:hover .message-copy-button {
  opacity: 1;
}

.message-copy-button:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  opacity: 1 !important;
}

.message-copy-button:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
}

/* 复制成功状态样式 */
.message-copy-button.copied {
  background-color: var(--success-color);
  color: white;
  border-color: var(--success-color);
  animation: copiedPulse 0.6s ease-in-out;
  box-shadow: 0 0 0 3px rgba(74, 222, 128, 0.2);
}

.message {
  position: relative;
  display: flex;
  gap: 16px;
  margin-bottom: 40px; /* 增加底部边距，为复制按钮留出空间 */
  animation: slideUp 0.3s ease-out;
  padding: 0 8px;
  width: 100%;
  max-width: 980px;
}

/* 重置message-bubble的相对定位 */
.message-bubble {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-time {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 4px;
  padding: 0 4px;
  align-self: flex-end;
  letter-spacing: 0.2px;
}

/* 聊天输入区域 */
.chat-input-area {
  padding: 20px 40px 32px;
  background-color: var(--bg-primary);
  display: flex;
  justify-content: center;
}

.chat-input-wrapper {
  width: 100%;
  max-width: 980px;
  position: relative;
  background-color: var(--bg-tertiary);
  border-radius: 16px;
  padding: 12px 16px;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  border: 1px solid var(--border-color);
  transition: all 0.2s;
}

.chat-input-wrapper:focus-within {
  border-color: var(--primary-color);
  background-color: var(--bg-primary);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.6;
  padding: 8px 0;
  resize: none;
  min-height: 24px;
  max-height: 200px;
}

.chat-input:focus {
  outline: none;
}

.input-actions {
  display: flex;
  align-items: center;
  padding-bottom: 4px;
}

.send-btn-new, .stop-btn {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-icon-wrapper, .stop-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--gradient-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s;
}

.send-btn-new:disabled .send-icon-wrapper {
  background: var(--gray-300);
  cursor: not-allowed;
}

.stop-icon-wrapper {
  background: #ef4444;
}

/* 模型选择器 */
.model-selector-wrapper {
  position: relative;
}

.model-selector-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.model-selector-btn:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
}

.model-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.model-selector-btn i {
  font-size: 12px;
  color: var(--text-tertiary);
  transition: transform 0.2s;
}

.model-selector-btn i.rotated {
  transform: rotate(180deg);
}

.model-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 240px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 8px;
  box-shadow: var(--shadow-xl);
  z-index: 1000;
  animation: menu-in 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes menu-in {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.brand-section {
  margin-bottom: 8px;
}

.brand-section:last-child {
  margin-bottom: 0;
}

.brand-header {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.model-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.model-item:hover {
  background-color: var(--bg-tertiary);
}

.model-item.active {
  background-color: rgba(37, 99, 235, 0.05);
  color: var(--primary-color);
}

.item-name {
  font-size: 14px;
  font-weight: 500;
}

.check-icon {
  font-size: 12px;
}

/* 工具栏 */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background-color: var(--border-color);
  margin: 0 4px;
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-btn:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
  color: var(--text-primary);
}

.tool-btn.active {
  background-color: rgba(37, 99, 235, 0.05);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 云盘相关样式 */
.sidebar-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 12px;
  margin-top: 24px;
}

.icon-btn-small {
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.icon-btn-small:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.folder-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 20px;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background-color: var(--bg-primary);
  padding: 24px;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  box-shadow: var(--shadow-xl);
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
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.input-field {
  width: 100%;
  padding: 12px 16px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 14px;
  margin-bottom: 24px;
  transition: all 0.2s;
}

.input-field:focus {
  outline: none;
  border-color: var(--primary-color);
  background-color: var(--bg-primary);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: var(--gradient-primary);
  color: white;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-secondary {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--bg-secondary);
}

.model-selector-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 14px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.model-selector-trigger:hover, .model-selector-trigger.active {
  background-color: var(--bg-primary);
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.brand-icon {
  color: var(--primary-color);
  font-size: 14px;
}

.brand-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.toggle-arrow {
  font-size: 10px;
  color: var(--text-tertiary);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-arrow.rotate {
  transform: rotate(180deg);
}

.model-dropdown-menu {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 0;
  width: 240px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  padding: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  z-index: 100;
  transform-origin: bottom left;
  backdrop-filter: blur(10px);
}

.model-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  margin-bottom: 2px;
}

.model-menu-item:last-child {
  margin-bottom: 0;
}

.model-menu-item:hover {
  background-color: var(--bg-secondary);
  transform: scale(1.02);
}

.model-menu-item.active {
  background-color: rgba(37, 99, 235, 0.05);
}

.item-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background-color: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  font-size: 16px;
  transition: all 0.3s ease;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05);
}

.model-menu-item.active .item-icon-wrapper {
  background-color: var(--primary-color);
  color: white;
  box-shadow: 0 4px 8px rgba(37, 99, 235, 0.3);
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 云盘相关样式 */
.sidebar-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.icon-btn-small {
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.icon-btn-small:hover {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
}

.folder-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 20px;
}

.folder-tree-container::-webkit-scrollbar {
  width: 5px;
}

.folder-tree-container::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 10px;
}

/* 弹窗通用样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background-color: var(--bg-primary);
  padding: 24px;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -6px rgba(0, 0, 0, 0.1);
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
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.input-field {
  width: 100%;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  margin-bottom: 24px;
  transition: all 0.2s;
}

.input-field:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: var(--gradient-primary);
  color: white;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-secondary {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.btn-secondary:hover {
  background-color: var(--border-color);
}

.item-desc {
  font-size: 11px;
  color: var(--text-tertiary);
}

.check-icon {
  font-size: 12px;
  color: var(--primary-color);
  animation: checkPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes checkPop {
  from { transform: scale(0); }
  to { transform: scale(1); }
}

/* 菜单动画 */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.95);
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background-color: var(--border-color);
  margin: 0 4px;
}

.stop-btn, .send-btn-new {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stop-icon-wrapper {
  width: 32px;
  height: 32px;
  background-color: var(--text-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.send-icon-wrapper {
  width: 32px;
  height: 32px;
  background-color: var(--text-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: opacity 0.2s;
}

.send-btn-new:disabled .send-icon-wrapper {
  background-color: var(--border-color);
  cursor: not-allowed;
}

.send-btn-new:not(:disabled):hover .send-icon-wrapper {
  opacity: 0.8;
}

.stop-btn:hover .stop-icon-wrapper {
  opacity: 0.8;
}

.send-btn {
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

@media (max-width: 1200px) {
  .message-content {
    max-width: 85%;
  }
}

@media (max-width: 768px) {
  .chat-container {
    border-radius: 0;
    box-shadow: none;
  }
  
  .chat-sidebar {
    display: none;
  }
  
  .messages-container {
    padding: 24px 16px;
  }
  
  .chat-input-area {
    padding: 16px 16px;
  }
  
  .message-content {
    max-width: 90%;
  }
  
  .chat-header {
    padding: 16px 16px;
  }
  
  .sidebar-title {
    font-size: 14px;
  }
  
  .empty-icon {
    font-size: 64px;
  }
  
  .empty-title {
    font-size: 24px;
  }
  
  .empty-description {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .messages-container {
    padding: 16px 8px;
  }
  
  .message {
    gap: 8px;
    margin-bottom: 16px;
  }
  
  .message-avatar {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }
  
  .message-text {
    padding: 12px 16px;
    font-size: 13px;
  }
  
  .message-content {
    max-width: 95%;
  }
}

.reasoning-message {
  margin-bottom: 12px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  overflow: hidden;
  max-width: 100%;
  transition: all 0.3s ease;
}

.reasoning-message.collapsed {
  background-color: var(--bg-secondary);
  border-color: var(--border-color);
  width: fit-content;
}

.message.assistant .reasoning-message {
  border-top-left-radius: 12px;
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  background-color: white;
  user-select: none;
  transition: background-color 0.2s;
  border-radius: 8px;
}

.reasoning-header:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.reasoning-title-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 400;
}

.reasoning-toggle-icon {
  font-size: 12px;
  color: var(--text-tertiary);
  transition: all 0.2s;
  background: var(--bg-secondary);
  padding: 6px;
  border-radius: 6px;
}

.reasoning-message.collapsed .reasoning-header {
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.reasoning-message:not(.collapsed) .reasoning-toggle-icon {
  transform: none;
}

.reasoning-body {
  padding: 0;
  border-top: 1px solid var(--border-color);
  background-color: transparent;
}

.reasoning-text {
  font-size: 15px;
  color: var(--text-secondary);
  padding: 16px;
  line-height: 1.7;
  word-wrap: break-word;
}

.reasoning-text :deep(p) {
  margin-bottom: 8px;
}

.reasoning-text :deep(p:last-child) {
  margin-bottom: 0;
}

/* 调整reasoning-text内部的pre样式 */
.reasoning-text :deep(pre) {
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  margin: 8px 0;
  padding: 12px;
}

.typing-cursor::after {
  content: '▋';
  display: inline-block;
  vertical-align: middle;
  animation: blink 1s step-end infinite;
  color: var(--primary-color);
  margin-left: 2px;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
