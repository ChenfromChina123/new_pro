<template>
  <div class="file-panel" :class="{ 'light-mode': !isDarkMode }">
    <!-- 工具栏 -->
    <div class="panel-toolbar">
      <div class="toolbar-left">
        <Breadcrumb
          :path="currentPath"
          @navigate="navigateTo"
        />
      </div>
      <div class="toolbar-right">
        <button
          class="btn-toolbar"
          :disabled="currentPath === '/'"
          title="上级目录 (Alt+↑)"
          @click="goUp"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 19V5M5 12l7-7 7 7" />
          </svg>
        </button>
        <button
          class="btn-toolbar"
          :disabled="loading"
          title="刷新 (F5)"
          @click="refresh"
        >
          <svg
            :class="{ spinning: loading }"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M23 4v6h-6M1 20v-6h6" />
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
          </svg>
        </button>
        <button
          class="btn-toolbar"
          title="新建文件夹"
          @click="showNewFolderDialog"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
            <line
              x1="12"
              y1="11"
              x2="12"
              y2="17"
            />
            <line
              x1="9"
              y1="14"
              x2="15"
              y2="14"
            />
          </svg>
        </button>
        <label
          class="btn-toolbar upload-btn"
          title="上传文件"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line
              x1="12"
              y1="3"
              x2="12"
              y2="15"
            />
          </svg>
          <input
            type="file"
            multiple
            hidden
            @change="handleUpload"
          >
        </label>
      </div>
    </div>

    <!-- 连接状态提示 -->
    <div
      v-if="!connected"
      class="connect-prompt"
    >
      <div class="connect-icon">
        🔌
      </div>
      <p>未连接到服务器</p>
      <button
        class="btn-connect"
        @click="$emit('connect', serverId)"
      >
        连接
      </button>
    </div>

    <!-- 文件列表 -->
    <div
      v-else
      ref="listContainer"
      class="file-list-container"
      :class="{ 'is-selecting': isSelecting }"
      @mousedown="handleMouseDown"
      @mousemove="handleMouseMove"
      @mouseup="handleMouseUp"
      @mouseleave="handleMouseUp"
    >
      <!-- 框选遮罩 -->
      <div
        v-if="isSelecting"
        class="selection-marquee"
        :style="marqueeStyle"
      />

      <div
        v-if="loading"
        class="loading-overlay"
      >
        <div class="loading-spinner" />
        <span>加载中...</span>
      </div>

      <table
        v-else
        class="file-table"
      >
        <thead>
          <tr>
            <th class="col-name">
              名称
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="files.length === 0">
            <td
              colspan="1"
              class="empty-row"
            >
              <div class="empty-content">
                <span class="empty-icon">📂</span>
                <span>此目录为空</span>
              </div>
            </td>
          </tr>
          <template v-else>
            <!-- 先显示目录 -->
            <tr
              v-for="file in sortedFiles.filter(f => f.directory)"
              :key="file.path"
              :data-path="file.path"
              :class="['file-row', 'is-directory', { selected: isSelected(file) }]"
              @dblclick="handleDoubleClick(file)"
              @click="handleClick(file, $event)"
              @contextmenu.prevent="showContextMenu(file, $event)"
            >
              <td class="col-name">
                <div class="file-name-cell">
                  <FileIcon
                    :file-name="file.name"
                    :is-directory="true"
                    size="md"
                  />
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-badge directory-badge">目录</span>
                </div>
              </td>
            </tr>
            <!-- 再显示文件 -->
            <tr
              v-for="file in sortedFiles.filter(f => !f.directory)"
              :key="file.path"
              :data-path="file.path"
              :class="['file-row', 'is-file', `file-type-${getFileTypeClass(file)}`, { selected: isSelected(file) }]"
              @dblclick="handleDoubleClick(file)"
              @click="handleClick(file, $event)"
              @contextmenu.prevent="showContextMenu(file, $event)"
            >
              <td class="col-name">
                <div class="file-name-cell">
                  <FileIcon
                    :file-name="file.name"
                    :is-directory="false"
                    size="md"
                  />
                  <span class="file-name">{{ file.name }}</span>
                  <span
                    v-if="getFileExtension(file.name)"
                    class="file-ext"
                  >{{ getFileExtension(file.name) }}</span>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- 新建文件夹对话框 -->
    <div
      v-if="showNewFolder"
      class="dialog-overlay"
      @click.self="showNewFolder = false"
    >
      <div class="dialog-content">
        <h4>新建文件夹</h4>
        <input
          ref="newFolderInput"
          v-model="newFolderName"
          type="text"
          placeholder="文件夹名称"
          @keyup.enter="createFolder"
        >
        <div class="dialog-actions">
          <button
            class="btn btn-secondary"
            @click="showNewFolder = false"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="createFolder"
          >
            创建
          </button>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <div
      v-if="showRename"
      class="dialog-overlay"
      @click.self="showRename = false"
    >
      <div class="dialog-content">
        <h4>重命名</h4>
        <input
          ref="renameInput"
          v-model="renameNewName"
          type="text"
          placeholder="新名称"
          @keyup.enter="doRename"
        >
        <div class="dialog-actions">
          <button
            class="btn btn-secondary"
            @click="showRename = false"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="doRename"
          >
            确定
          </button>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
    >
      <!-- 多文件选中时的批量操作 -->
      <template v-if="selectedFiles.length > 1">
        <div class="menu-header">
          已选中 {{ selectedFiles.length }} 项
        </div>
        <div
          class="menu-item"
          @click="downloadSelected"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line
              x1="12"
              y1="15"
              x2="12"
              y2="3"
            />
          </svg>
          <span>批量下载</span>
        </div>
        <div class="menu-divider" />
        <div
          class="menu-item danger"
          @click="deleteSelected"
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
          <span>批量删除</span>
        </div>
      </template>

      <!-- 单个文件操作 -->
      <template v-else-if="contextMenu.file">
        <template v-if="contextMenu.file?.directory">
          <div
            class="menu-item"
            @click="enterDirectory(contextMenu.file)"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
            </svg>
            <span>打开</span>
          </div>
        </template>
        <template v-else>
          <div
            class="menu-item"
            @click="downloadFile(contextMenu.file)"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line
                x1="12"
                y1="15"
                x2="12"
                y2="3"
              />
            </svg>
            <span>下载</span>
          </div>
        </template>
        <div
          class="menu-item"
          @click="cutFile(contextMenu.file)"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z" />
            <polyline points="16 17 21 12 16 7" />
            <line
              x1="21"
              y1="12"
              x2="9"
              y2="12"
            />
          </svg>
          <span>剪切</span>
        </div>
        <div
          class="menu-item"
          @click="copyFile(contextMenu.file)"
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
        <div
          v-if="clipboard.files.length > 0"
          class="menu-item"
          @click="pasteFiles"
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
              x="12"
              y="12"
              width="13"
              height="13"
              rx="2"
              ry="2"
            />
            <path d="M19 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7" />
          </svg>
          <span>粘贴</span>
        </div>
        <div
          class="menu-item"
          @click="renameFile(contextMenu.file)"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
          </svg>
          <span>重命名</span>
        </div>
        <div class="menu-divider" />
        <div
          class="menu-item danger"
          @click="deleteFile(contextMenu.file)"
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
          <span>删除</span>
        </div>
        <div class="menu-divider" />
        <div
          class="menu-item"
          @click="showFileProperties(contextMenu.file)"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle
              cx="12"
              cy="12"
              r="10"
            />
            <line
              x1="12"
              y1="16"
              x2="12"
              y2="12"
            />
            <line
              x1="12"
              y1="8"
              x2="12.01"
              y2="8"
            />
          </svg>
          <span>属性</span>
        </div>
      </template>
    </div>

    <!-- 属性对话框 -->
    <div
      v-if="showPropertiesDialog"
      class="properties-dialog-overlay"
      @click="showPropertiesDialog = false"
    >
      <div
        class="properties-dialog"
        @click.stop
      >
        <div class="properties-header">
          <h3>{{ selectedFile?.directory ? '文件夹属性' : '文件属性' }}</h3>
          <button
            class="close-btn"
            @click="showPropertiesDialog = false"
          >
            ×
          </button>
        </div>
        <div class="properties-content">
          <div class="property-section">
            <h5>基本信息</h5>
            <div class="property-row">
              <span class="label">类型：</span>
              <span class="value">
                <span v-if="selectedFile?.directory">📁 文件夹</span>
                <span v-else>📄 文件</span>
              </span>
            </div>
            <div
              v-if="!selectedFile?.directory"
              class="property-row"
            >
              <span class="label">大小：</span>
              <span class="value">{{ formatSize(selectedFile?.size || 0) }}</span>
            </div>
            <div
              v-if="!selectedFile?.directory"
              class="property-row"
            >
              <span class="label">扩展名：</span>
              <span class="value">{{ getFileExtension(selectedFile?.name || '') }}</span>
            </div>
            <div class="property-row">
              <span class="label">名称：</span>
              <span class="value">{{ selectedFile?.name }}</span>
            </div>
            <div class="property-row">
              <span class="label">位置：</span>
              <span class="value path-value">{{ selectedFile?.path }}</span>
            </div>
            <div class="property-row">
              <span class="label">修改时间：</span>
              <span class="value">{{ formatDateTime(selectedFile?.modifiedTime) }}</span>
            </div>
          </div>
          <div class="property-section">
            <h5>权限</h5>
            <div class="property-row">
              <span class="label">权限：</span>
              <span class="value permission-value">{{ selectedFile?.permissions }}</span>
            </div>
            <div class="property-row">
              <span class="label">权限说明：</span>
              <span class="value">{{ getPermissionDescription(selectedFile?.permissions) }}</span>
            </div>
          </div>
          <div
            v-if="!selectedFile?.directory"
            class="property-section"
          >
            <h5>其他信息</h5>
            <div class="property-row">
              <span class="label">MIME 类型：</span>
              <span class="value">{{ getMimeType(selectedFile?.name || '') }}</span>
            </div>
          </div>
        </div>
        <div class="properties-footer">
          <button
            class="btn-primary"
            @click="showPropertiesDialog = false"
          >
            确定
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 文件面板组件
 * 显示 SFTP 文件列表，支持文件操作
 * 所有操作通过右键菜单执行
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSFTPStore } from '@/stores/sftp'
import { useEditorStore } from '@/stores/editor'
import { useThemeStore } from '@/stores/theme'
import sftpService from '@/services/sftpService'
import Breadcrumb from './Breadcrumb.vue'
import FileIcon from './FileIcon.vue'

const router = useRouter()
const themeStore = useThemeStore()
const isDarkMode = computed(() => themeStore.isDarkMode)

const props = defineProps({
  serverId: {
    type: Number,
    required: true
  },
  connected: {
    type: Boolean,
    default: false
  }
})

defineEmits(['connect', 'disconnect'])

const sftpStore = useSFTPStore()

const files = computed(() => sftpStore.files)
const currentPath = computed(() => sftpStore.currentPath)
const loading = computed(() => sftpStore.loading)
const selectedFiles = computed(() => sftpStore.selectedFiles)

const showNewFolder = ref(false)
const newFolderName = ref('')
const newFolderInput = ref(null)

const showRename = ref(false)
const renameNewName = ref('')
const fileToRename = ref(null)
const renameInput = ref(null)

const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  file: null
})

// 属性对话框相关
const selectedFile = ref(null)
const showPropertiesDialog = ref(false)

// 剪贴板相关
const clipboard = ref({
  files: [],
  operation: null // 'cut' or 'copy'
})

// 框选相关
const listContainer = ref(null)
const isSelecting = ref(false)
const marqueeStart = ref({ x: 0, y: 0 })
const marqueeEnd = ref({ x: 0, y: 0 })

// 用于实时框选缓存的非响应式变量
let initialSelectedFiles = []
let rowCache = []
const marqueeStyle = computed(() => {
  const left = Math.min(marqueeStart.value.x, marqueeEnd.value.x)
  const top = Math.min(marqueeStart.value.y, marqueeEnd.value.y)
  const width = Math.abs(marqueeEnd.value.x - marqueeStart.value.x)
  const height = Math.abs(marqueeEnd.value.y - marqueeStart.value.y)

  return {
    left: left + 'px',
    top: top + 'px',
    width: width + 'px',
    height: height + 'px'
  }
})

/**
 * 排序后的文件列表（目录在前，文件在后）
 */
const sortedFiles = computed(() => {
  return [...files.value].sort((a, b) => {
    if (a.isDirectory && !b.isDirectory) return -1
    if (!a.isDirectory && b.isDirectory) return 1
    return a.name.localeCompare(b.name)
  })
})

/**
 * 检查文件是否被选中
 */
function isSelected(file) {
  return selectedFiles.value.some(f => f.path === file.path)
}

/**
 * 处理单击事件 - 支持 Ctrl/Shift 多选
 */
function handleClick(file, event) {
  if (event.ctrlKey || event.metaKey) {
    // Ctrl 点击：切换选中状态
    const index = selectedFiles.value.findIndex(f => f.path === file.path)
    if (index === -1) {
      sftpStore.selectedFiles = [...selectedFiles.value, file]
    } else {
      const newSelected = selectedFiles.value.filter((_, i) => i !== index)
      sftpStore.selectedFiles = newSelected
    }
  } else if (event.shiftKey && selectedFiles.value.length > 0) {
    // Shift 点击：范围选中
    const lastSelected = selectedFiles.value[selectedFiles.value.length - 1]
    const currentIndex = sortedFiles.value.findIndex(f => f.path === file.path)
    const lastIndex = sortedFiles.value.findIndex(f => f.path === lastSelected.path)

    const start = Math.min(currentIndex, lastIndex)
    const end = Math.max(currentIndex, lastIndex)
    const range = sortedFiles.value.slice(start, end + 1)

    // 合并选中项，去重
    const merged = [...selectedFiles.value, ...range]
    const unique = merged.filter((item, index, self) =>
      index === self.findIndex(t => t.path === item.path)
    )
    sftpStore.selectedFiles = unique
  } else {
    // 普通点击：单选
    sftpStore.selectedFiles = [file]
  }
}

/**
 * 处理双击事件 - 目录进入，文件打开编辑器
 * 修复：改用 file.path 替代 file.name，确保路径准确无误
 */
async function handleDoubleClick(file) {
  console.log('双击事件触发:', file)
  // 修复：使用正确的属性名 directory 而不是 isDirectory
  if (file.directory) {
    console.log('尝试进入目录:', file.path)
    sftpStore.enterDirectory(file.path)
  } else {
    // 文件双击则打开编辑器
    console.log('打开文件编辑器:', file.path)
    await openFileInEditor(file)
  }
}

/**
 * 在编辑器中打开文件
 */
async function openFileInEditor(file) {
  try {
    // 获取文件内容
    const response = await sftpService.getFileContent(props.serverId, file.path)

    if (response.code === 200) {
      const editorStore = useEditorStore()
      editorStore.openFile({
        name: file.name,
        path: file.path,
        content: response.content || response.data || '',
        type: getFileType(file.name)
      })

      // 使用 Vue Router 导航到编辑器页面（保持单页应用状态）
      router.push('/editor')
    } else {
      alert('无法打开文件: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('打开文件失败:', error)
    alert('打开文件失败: ' + error.message)
  }
}

/**
 * 获取文件类型
 */
function getFileType(fileName) {
  const ext = fileName.split('.').pop().toLowerCase()
  const typeMap = {
    'js': 'javascript',
    'ts': 'typescript',
    'vue': 'vue',
    'py': 'python',
    'json': 'json',
    'css': 'css',
    'html': 'html',
    'md': 'markdown',
    'txt': 'text'
  }
  return typeMap[ext] || 'text'
}

/**
 * 进入目录
 * 修复：同样改用 file.path
 */
function enterDirectory(file) {
  // 修复：使用正确的属性名 directory 而不是 isDirectory
  if (file.directory) {
    sftpStore.enterDirectory(file.path)
  }
  hideContextMenu()
}

/**
 * 导航到指定路径
 */
function navigateTo(path) {
  sftpStore.fetchFiles(path)
}

/**
 * 返回上级目录
 */
function goUp() {
  sftpStore.goUp()
}

/**
 * 刷新文件列表
 */
function refresh() {
  sftpStore.fetchFiles()
}

/**
 * 显示新建文件夹对话框
 */
function showNewFolderDialog() {
  showNewFolder.value = true
  newFolderName.value = ''
  nextTick(() => {
    newFolderInput.value?.focus()
  })
}

/**
 * 创建文件夹
 */
async function createFolder() {
  if (!newFolderName.value.trim()) return

  const path = currentPath.value === '/'
    ? `/${newFolderName.value}`
    : `${currentPath.value}/${newFolderName.value}`

  const success = await sftpStore.createDirectory(path)
  if (success) {
    showNewFolder.value = false
  }
}

/**
 * 显示重命名对话框
 */
function renameFile(file) {
  hideContextMenu()
  fileToRename.value = file
  renameNewName.value = file.name
  showRename.value = true
  nextTick(() => {
    renameInput.value?.focus()
  })
}

/**
 * 执行重命名
 */
async function doRename() {
  if (!renameNewName.value.trim() || !fileToRename.value) return

  const oldPath = fileToRename.value.path
  const parentPath = oldPath.substring(0, oldPath.lastIndexOf('/'))
  const newPath = parentPath ? `${parentPath}/${renameNewName.value}` : `/${renameNewName.value}`

  const success = await sftpStore.renameItem(oldPath, newPath)
  if (success) {
    showRename.value = false
    fileToRename.value = null
  }
}

/**
 * 删除文件
 */
async function deleteFile(file) {
  hideContextMenu()
  if (!confirm(`确定要删除 "${file.name}" 吗？`)) return

  await sftpStore.deleteItem(file.path, file.isDirectory)
}

/**
 * 显示文件属性
 */
function showFileProperties(file) {
  hideContextMenu()
  selectedFile.value = file
  showPropertiesDialog.value = true
}

/**
 * 格式化文件大小
 */
function formatSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 格式化日期时间
 */
function formatDateTime(timestamp) {
  if (!timestamp) return '未知'
  const date = new Date(timestamp * 1000)
  return date.toLocaleString('zh-CN')
}

/**
 * 获取文件扩展名
 */
function getFileExtension(fileName) {
  const lastDot = fileName.lastIndexOf('.')
  if (lastDot === -1 || lastDot === 0) return ''
  return fileName.substring(lastDot).toLowerCase()
}

/**
 * 获取权限说明
 */
function getPermissionDescription(permissions) {
  if (!permissions) return '未知'

  const parts = permissions.split('')
  if (parts.length < 9) return '无效权限'

  const owner = parts.slice(0, 3).join('')
  const group = parts.slice(3, 6).join('')
  const others = parts.slice(6, 9).join('')

  const getPermissionText = (perm) => {
    let text = ''
    if (perm[0] === 'r') text += '读取 '
    if (perm[1] === 'w') text += '写入 '
    if (perm[2] === 'x') text += '执行'
    return text || '无'
  }

  return `所有者: ${getPermissionText(owner)}, 组: ${getPermissionText(group)}, 其他: ${getPermissionText(others)}`
}

/**
 * 获取 MIME 类型
 */
function getMimeType(fileName) {
  const ext = getFileExtension(fileName).toLowerCase()
  const mimeTypes = {
    '.txt': 'text/plain',
    '.js': 'application/javascript',
    '.json': 'application/json',
    '.html': 'text/html',
    '.css': 'text/css',
    '.md': 'text/markdown',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.pdf': 'application/pdf',
    '.zip': 'application/zip',
    '.tar': 'application/x-tar',
    '.gz': 'application/gzip',
    '.mp3': 'audio/mpeg',
    '.mp4': 'video/mp4'
  }

  return mimeTypes[ext] || 'application/octet-stream'
}

/**
 * 剪切文件
 */
function cutFile(file) {
  hideContextMenu()
  clipboard.value = {
    files: [file],
    operation: 'cut'
  }
  alert('已添加到剪切板')
}

/**
 * 复制文件
 */
function copyFile(file) {
  hideContextMenu()
  clipboard.value = {
    files: [file],
    operation: 'copy'
  }
  alert('已复制到剪切板')
}

/**
 * 粘贴文件
 */
async function pasteFiles() {
  hideContextMenu()
  if (clipboard.value.files.length === 0) {
    alert('剪切板为空')
    return
  }

  for (const file of clipboard.value.files) {
    const fileName = file.name
    const newPath = currentPath.value === '/' ? `/${fileName}` : `${currentPath.value}/${fileName}`

    try {
      if (clipboard.value.operation === 'cut') {
        // 剪切操作：重命名/移动
        await sftpStore.renameItem(file.path, newPath)
        // 清空剪切板
        clipboard.value = { files: [], operation: null }
      } else if (clipboard.value.operation === 'copy') {
        // 复制操作：需要实现文件复制功能
        // 这里需要调用 sftpStore 的复制方法
        await sftpStore.copyItem(file.path, newPath, file.isDirectory)
      }
    } catch (error) {
      console.error(`操作失败:`, error)
      alert(`操作失败：${error.message}`)
    }
  }

  // 刷新文件列表
  await sftpStore.fetchFiles()
}

/**
 * 下载文件
 */
async function downloadFile(file) {
  hideContextMenu()
  // 修复：使用正确的属性名 directory 而不是 isDirectory
  if (file.directory) return

  try {
    const blob = await sftpService.downloadFileBlob(props.serverId, file.path)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = file.name
    a.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败:', error)
    alert('下载失败：' + error.message)
  }
}

/**
 * 批量下载选中的文件
 */
async function downloadSelected() {
  hideContextMenu()
  // 修复：使用正确的属性名 directory 而不是 isDirectory
  const filesToDownload = selectedFiles.value.filter(f => !f.directory)

  if (filesToDownload.length === 0) {
    alert('没有可下载的文件')
    return
  }

  for (const file of filesToDownload) {
    try {
      await downloadFile(file)
    } catch (error) {
      console.error(`下载 ${file.name} 失败:`, error)
    }
  }

  sftpStore.selectedFiles = []
}

/**
 * 批量删除选中的项
 */
async function deleteSelected() {
  hideContextMenu()
  if (!confirm(`确定要删除选中的 ${selectedFiles.value.length} 项吗？`)) return

  for (const file of selectedFiles.value) {
    try {
      await sftpStore.deleteItem(file.path, file.isDirectory)
    } catch (error) {
      console.error(`删除 ${file.name} 失败:`, error)
    }
  }

  sftpStore.selectedFiles = []
}

/**
 * 清除选中
 */
function clearSelection() {
  sftpStore.selectedFiles = []
}

/**
 * 处理文件上传
 */
async function handleUpload(event) {
  const fileList = event.target.files
  if (!fileList.length) return

  for (const file of fileList) {
    try {
      sftpStore.addTransferTask({
        fileName: file.name,
        filePath: `${currentPath.value}/${file.name}`,
        totalSize: file.size,
        type: 'upload'
      })

      await sftpService.uploadFile(props.serverId, currentPath.value, file)
      await sftpStore.fetchFiles()
    } catch (error) {
      console.error('上传失败:', error)
      alert('上传失败：' + error.message)
    }
  }

  event.target.value = ''
}

/**
 * 显示右键菜单
 */
function showContextMenu(file, event) {
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    file
  }
}

/**
 * 隐藏右键菜单
 */
function hideContextMenu() {
  contextMenu.value.visible = false
}

/**
 * 获取文件类型 CSS 类名
 */
function getFileTypeClass(file) {
  // 修复：使用正确的属性名 directory 而不是 isDirectory
  if (file.directory) return 'folder'

  const ext = getFileExtension(file.name).slice(1)
  if (!ext) return 'unknown'

  const typeMap = {
    code: ['js', 'ts', 'jsx', 'tsx', 'vue', 'html', 'css', 'scss', 'less', 'json', 'py', 'java', 'go', 'rs', 'php', 'rb', 'c', 'cpp', 'cs', 'swift', 'kt'],
    image: ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'ico', 'bmp'],
    video: ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv', 'webm'],
    audio: ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma'],
    archive: ['zip', 'rar', 'tar', 'gz', '7z', 'bz2', 'xz'],
    document: ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'md', 'txt'],
    config: ['yaml', 'yml', 'toml', 'ini', 'env', 'xml'],
    database: ['sql', 'db', 'sqlite']
  }

  for (const [type, exts] of Object.entries(typeMap)) {
    if (exts.includes(ext)) return type
  }

  return 'unknown'
}

// --- 用于实时框选缓存的非响应式变量 ---

/**
 * 框选：鼠标按下 (初始化与缓存)
 */
function handleMouseDown(e) {
  // 只处理左键，且不在表头和按钮上
  if (e.button !== 0 || e.target.closest('th') || e.target.closest('button')) return

  const container = listContainer.value
  const rect = container.getBoundingClientRect()

  marqueeStart.value = {
    x: e.clientX - rect.left + container.scrollLeft,
    y: e.clientY - rect.top + container.scrollTop
  }
  marqueeEnd.value = { ...marqueeStart.value }

  // 核心优化：在按下时一次性计算并缓存所有行的绝对位置坐标
  // 避免在 MouseMove 中频繁读取 DOM 导致卡顿
  const rows = container.querySelectorAll('.file-row')
  rowCache = Array.from(rows).map(row => {
    const rowRect = row.getBoundingClientRect()
    return {
      path: row.getAttribute('data-path'),
      top: rowRect.top - rect.top + container.scrollTop,
      bottom: rowRect.bottom - rect.top + container.scrollTop,
      left: rowRect.left - rect.left + container.scrollLeft,
      right: rowRect.right - rect.left + container.scrollLeft
    }
  })

  // 记录下 mousedown 时的初始选中状态，用于后续结合 Shift/Ctrl 键做增量运算
  initialSelectedFiles = [...sftpStore.selectedFiles]
  isSelecting.value = true

  // 清除浏览器原生的文字选中，防止拖拽干扰
  window.getSelection()?.removeAllRanges()
}

/**
 * 框选：鼠标拖动 (实时计算与高亮)
 */
function handleMouseMove(e) {
  if (!isSelecting.value) return

  const container = listContainer.value
  const rect = container.getBoundingClientRect()

  marqueeEnd.value = {
    x: e.clientX - rect.left + container.scrollLeft,
    y: e.clientY - rect.top + container.scrollTop
  }

  const selectLeft = Math.min(marqueeStart.value.x, marqueeEnd.value.x)
  const selectTop = Math.min(marqueeStart.value.y, marqueeEnd.value.y)
  const selectRight = Math.max(marqueeStart.value.x, marqueeEnd.value.x)
  const selectBottom = Math.max(marqueeStart.value.y, marqueeEnd.value.y)

  // 拖动距离太小不视为框选，防止单纯的点击被误判
  if (selectRight - selectLeft < 5 && selectBottom - selectTop < 5) return

  const newlySelectedPaths = new Set()

  // O(n) 复杂度遍历缓存，性能极佳
  for (const row of rowCache) {
    if (
      row.left < selectRight &&
      row.right > selectLeft &&
      row.top < selectBottom &&
      row.bottom > selectTop
    ) {
      newlySelectedPaths.add(row.path)
    }
  }

  // 映射回文件对象
  const newlySelectedFiles = sortedFiles.value.filter(f => newlySelectedPaths.has(f.path))

  let combined = []
  if (e.ctrlKey || e.metaKey || e.shiftKey) {
    // 如果按住了修饰键，在原有的基础上追加
    combined = [...initialSelectedFiles, ...newlySelectedFiles]
  } else {
    // 否则实时覆盖选中（产生原生资源管理器那样的实时高亮感）
    combined = [...newlySelectedFiles]
  }

  // 去重并触发响应式更新
  const unique = combined.filter((item, index, self) =>
    index === self.findIndex(t => t.path === item.path)
  )

  sftpStore.selectedFiles = unique
}

/**
 * 框选：鼠标松开 (清理状态)
 */
function handleMouseUp() {
  if (!isSelecting.value) return
  isSelecting.value = false
  rowCache = [] // 清空缓存释放内存
}

/**
 * 键盘快捷键
 */
function handleKeyDown(e) {
  // F5 刷新
  if (e.key === 'F5') {
    e.preventDefault()
    refresh()
  }

  // Alt + ↑ 返回上级
  if (e.altKey && e.key === 'ArrowUp') {
    e.preventDefault()
    goUp()
  }

  // Delete 删除选中
  if (e.key === 'Delete' && selectedFiles.value.length > 0) {
    e.preventDefault()
    deleteSelected()
  }

  // Esc 清除选中
  if (e.key === 'Escape') {
    e.preventDefault()
    clearSelection()
    hideContextMenu()
  }
}

watch(() => props.connected, (connected) => {
  if (connected) {
    sftpStore.fetchFiles()
  }
})

onMounted(() => {
  document.addEventListener('click', hideContextMenu)
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('click', hideContextMenu)
  document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.file-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--panel-bg, #1e1e1e);
  color: var(--text-main, #d4d4d4);
  position: relative;
}

.file-panel.light-mode {
  --panel-bg: #ffffff;
  --text-main: #1e293b;
  --text-dim: #64748b;
  --border: #e2e8f0;
  --hover-bg: #f1f5f9;
  --toolbar-bg: #f8fafc;
  --btn-bg: #e2e8f0;
  --btn-text: #334155;
  --btn-hover-bg: #cbd5e1;
  --selected-bg: rgba(59, 130, 246, 0.15);
  --directory-bg: rgba(245, 158, 11, 0.08);
  --loading-bg: rgba(0, 0, 0, 0.3);
  --dialog-bg: #ffffff;
  --dialog-border: #e2e8f0;
  --context-menu-bg: #ffffff;
  --context-menu-hover: #f1f5f9;
  --scrollbar-track: #f1f5f9;
  --scrollbar-thumb: #cbd5e1;
  --scrollbar-thumb-hover: #94a3b8;
}

.panel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--toolbar-bg, #252526);
  border-bottom: 1px solid var(--border, #3e3e42);
}

.toolbar-left {
  flex: 1;
  min-width: 0;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.btn-toolbar {
  background: var(--btn-bg, #3e3e42);
  border: 1px solid var(--border, #505054);
  color: var(--btn-text, #cccccc);
  padding: 6px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-toolbar:hover {
  background: var(--btn-hover-bg, #505054);
  border-color: #007acc;
}

.btn-toolbar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.upload-btn {
  cursor: pointer;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.connect-prompt {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-dim, #808080);
}

.connect-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.btn-connect {
  background: #007acc;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  margin-top: 16px;
  transition: all 0.2s;
}

.btn-connect:hover {
  background: #005a9e;
}

.file-list-container {
  flex: 1;
  overflow: auto;
  position: relative;
  background: var(--panel-bg, #1e1e1e);
  /* 全局禁止文字被选中，从根源解决双击失效或拖拽卡顿的冲突 */
  user-select: none;
  -webkit-user-select: none;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--loading-bg, rgba(0, 0, 0, 0.7));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border, #3e3e42);
  border-top-color: #007acc;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

.file-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.file-table th {
  text-align: left;
  padding: 10px 12px;
  background: var(--toolbar-bg, #252526);
  border-bottom: 1px solid var(--border, #3e3e42);
  color: var(--text-dim, #808080);
  font-weight: 500;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  position: sticky;
  top: 0;
  z-index: 1;
}

.file-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border, #3e3e42);
}

.file-row {
  cursor: pointer;
  transition: all 0.15s ease;
}

.file-row:hover {
  background: var(--hover-bg, #2a2d2e);
}

.file-row.selected {
  background: var(--selected-bg, rgba(59, 130, 246, 0.25));
  box-shadow: inset 3px 0 0 0 #3b82f6;
}

.file-row.selected:hover {
  background: var(--selected-bg, rgba(59, 130, 246, 0.3));
}

/* 目录行样式 */
.file-row.is-directory {
  background: var(--directory-bg, rgba(240, 198, 116, 0.05));
}

.file-row.is-directory:hover {
  background: var(--directory-bg, rgba(240, 198, 116, 0.12));
}

.file-row.is-directory.selected {
  background: var(--directory-bg, rgba(240, 198, 116, 0.2));
  box-shadow: inset 3px 0 0 0 #f0c674;
}

/* 文件行样式 - 根据类型 */
.file-row.is-file.file-type-code {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-code.selected {
  border-left-color: #61dafb;
}

.file-row.is-file.file-type-image {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-image.selected {
  border-left-color: #a855f7;
}

.file-row.is-file.file-type-video {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-video.selected {
  border-left-color: #ef4444;
}

.file-row.is-file.file-type-audio {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-audio.selected {
  border-left-color: #22c55e;
}

.file-row.is-file.file-type-archive {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-archive.selected {
  border-left-color: #f59e0b;
}

.file-row.is-file.file-type-document {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-document.selected {
  border-left-color: #3b82f6;
}

.file-row.is-file.file-type-config {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-config.selected {
  border-left-color: #6b7280;
}

.file-row.is-file.file-type-database {
  border-left: 3px solid transparent;
}

.file-row.is-file.file-type-database.selected {
  border-left-color: #8b5cf6;
}

.file-row.is-file.file-type-unknown {
  border-left: 3px solid transparent;
}

.col-name {
  min-width: 200px;
}

.col-permissions {
  width: 100px;
}

.empty-row {
  text-align: center;
  color: var(--text-dim, #808080);
  padding: 40px !important;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.file-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.directory-badge {
  background: var(--directory-bg, rgba(240, 198, 116, 0.2));
  color: var(--text-main, #f0c674);
}

.file-ext {
  font-size: 10px;
  color: var(--text-dim, #808080);
  background: var(--hover-bg, #2a2d2e);
  padding: 2px 5px;
  border-radius: 3px;
  font-family: monospace;
}

.permission-text {
  font-family: monospace;
  font-size: 11px;
  color: var(--text-dim, #808080);
}

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog-content {
  background: var(--dialog-bg, #252526);
  border: 1px solid var(--dialog-border, #3e3e42);
  border-radius: 8px;
  padding: 20px;
  width: 320px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.dialog-content h4 {
  margin: 0 0 16px;
  font-size: 15px;
  color: var(--text-main, #cccccc);
}

.dialog-content input {
  width: 100%;
  padding: 10px 12px;
  background: var(--panel-bg, #1e1e1e);
  border: 1px solid var(--border, #3e3e42);
  color: var(--text-main, #d4d4d4);
  border-radius: 6px;
  font-size: 13px;
  box-sizing: border-box;
  margin-bottom: 16px;
}

.dialog-content input:focus {
  outline: none;
  border-color: #007acc;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: #007acc;
  color: white;
}

.btn-primary:hover {
  background: #005a9e;
}

.btn-secondary {
  background: var(--btn-bg, #3e3e42);
  color: var(--btn-text, #cccccc);
}

.btn-secondary:hover {
  background: var(--btn-hover-bg, #505054);
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  background: var(--context-menu-bg, #252526);
  border: 1px solid var(--border, #3e3e42);
  border-radius: 8px;
  padding: 6px 0;
  min-width: 200px;
  z-index: 2000;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.menu-header {
  padding: 8px 14px;
  font-size: 11px;
  color: var(--text-dim, #808080);
  font-weight: 600;
  border-bottom: 1px solid var(--border, #3e3e42);
  margin-bottom: 6px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.15s;
  color: var(--text-main, #cccccc);
}

.menu-item:hover {
  background: var(--context-menu-hover, #3e3e42);
}

.menu-item.danger {
  color: #ef4444;
}

.menu-item.danger:hover {
  background: rgba(239, 68, 68, 0.15);
}

.menu-divider {
  height: 1px;
  background: #3e3e42;
  margin: 6px 0;
}



/* 框选遮罩 */
.selection-marquee {
  position: absolute;
  background: rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.5);
  pointer-events: none;
  z-index: 100;
  transition: all 0.05s ease;
}

/* 滚动条样式 */
.file-list-container::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.file-list-container::-webkit-scrollbar-track {
  background: var(--scrollbar-track, #1e1e1e);
}

.file-list-container::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb, #424242);
  border-radius: 5px;
}

.file-list-container::-webkit-scrollbar-thumb:hover {
  background: var(--scrollbar-thumb-hover, #505050);
}

/* 属性对话框样式 */
.properties-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.properties-dialog {
  background: var(--dialog-bg, #252526);
  border: 1px solid var(--dialog-border, #3e3e42);
  border-radius: 8px;
  width: 450px;
  max-height: 80vh;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
}

.properties-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border, #3e3e42);
}

.properties-header h3 {
  margin: 0;
  font-size: 16px;
  color: var(--text-main, #cccccc);
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-dim, #808080);
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: var(--text-main, #cccccc);
}

.properties-content {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.property-section {
  margin-bottom: 20px;
}

.property-section:last-child {
  margin-bottom: 0;
}

.property-section h5 {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--text-dim, #808080);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.property-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 13px;
}

.property-row .label {
  color: var(--text-dim, #808080);
  width: 90px;
  flex-shrink: 0;
}

.property-row .value {
  color: var(--text-main, #cccccc);
  word-break: break-all;
}

.property-row .path-value {
  font-family: monospace;
  font-size: 12px;
  background: var(--panel-bg, #1e1e1e);
  padding: 4px 8px;
  border-radius: 4px;
}

.property-row .permission-value {
  font-family: monospace;
  font-size: 12px;
  background: var(--panel-bg, #1e1e1e);
  padding: 4px 8px;
  border-radius: 4px;
}

.properties-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border, #3e3e42);
  display: flex;
  justify-content: flex-end;
}
</style>
