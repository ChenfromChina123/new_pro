<template>
  <div class="file-panel">
    <!-- 工具栏 -->
    <div class="panel-toolbar">
      <div class="toolbar-left">
        <Breadcrumb :path="currentPath" @navigate="navigateTo" />
      </div>
      <div class="toolbar-right">
        <button class="btn-toolbar" @click="goUp" :disabled="currentPath === '/'" title="上级目录">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 19V5M5 12l7-7 7 7"/>
          </svg>
        </button>
        <button class="btn-toolbar" @click="refresh" :disabled="loading" title="刷新">
          <svg :class="{ spinning: loading }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 4v6h-6M1 20v-6h6"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
        </button>
        <button class="btn-toolbar" @click="showNewFolderDialog" title="新建文件夹">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
            <line x1="12" y1="11" x2="12" y2="17"/>
            <line x1="9" y1="14" x2="15" y2="14"/>
          </svg>
        </button>
        <label class="btn-toolbar upload-btn" title="上传文件">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <input type="file" multiple @change="handleUpload" hidden />
        </label>
      </div>
    </div>

    <!-- 连接状态提示 -->
    <div v-if="!connected" class="connect-prompt">
      <div class="connect-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M18.36 6.64a9 9 0 1 1-12.73 0"/>
          <line x1="12" y1="2" x2="12" y2="12"/>
        </svg>
      </div>
      <p>未连接到服务器</p>
      <button class="btn-connect" @click="$emit('connect', serverId)">连接</button>
    </div>

    <!-- 文件列表 -->
    <div v-else class="file-list-container">
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>

      <table class="file-table" v-else>
        <thead>
          <tr>
            <th class="col-checkbox">
              <input type="checkbox" @change="toggleSelectAll" :checked="isAllSelected" />
            </th>
            <th class="col-name">名称</th>
            <th class="col-permissions">权限</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="files.length === 0">
            <td colspan="4" class="empty-row">
              <div class="empty-content">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.5">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                </svg>
                <span>此目录为空</span>
              </div>
            </td>
          </tr>
          <template v-else>
            <!-- 先显示目录 -->
            <tr
              v-for="file in sortedFiles.filter(f => f.isDirectory)"
              :key="file.path"
              :class="['file-row', 'is-directory', { selected: isSelected(file) }]"
              @dblclick="handleDoubleClick(file)"
              @click="handleClick(file, $event)"
              @contextmenu.prevent="showContextMenu(file, $event)"
            >
              <td class="col-checkbox">
                <input type="checkbox" :checked="isSelected(file)" @click.stop />
              </td>
              <td class="col-name">
                <div class="file-name-cell">
                  <FileIcon :fileName="file.name" :isDirectory="true" size="md" />
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-badge directory-badge">目录</span>
                </div>
              </td>
              <td class="col-permissions">
                <span class="permission-text">{{ file.permissions }}</span>
              </td>
              <td class="col-actions">
                <button class="btn-action" @click.stop="renameFile(file)" title="重命名">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
                  </svg>
                </button>
                <button class="btn-action btn-danger" @click.stop="deleteFile(file)" title="删除">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </td>
            </tr>
            <!-- 再显示文件 -->
            <tr
              v-for="file in sortedFiles.filter(f => !f.isDirectory)"
              :key="file.path"
              :class="['file-row', 'is-file', `file-type-${getFileTypeClass(file)}`, { selected: isSelected(file) }]"
              @dblclick="handleDoubleClick(file)"
              @click="handleClick(file, $event)"
              @contextmenu.prevent="showContextMenu(file, $event)"
            >
              <td class="col-checkbox">
                <input type="checkbox" :checked="isSelected(file)" @click.stop />
              </td>
              <td class="col-name">
                <div class="file-name-cell">
                  <FileIcon :fileName="file.name" :isDirectory="false" size="md" />
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-ext" v-if="getFileExtension(file.name)">{{ getFileExtension(file.name) }}</span>
                </div>
              </td>
              <td class="col-permissions">
                <span class="permission-text">{{ file.permissions }}</span>
              </td>
              <td class="col-actions">
                <button class="btn-action" @click.stop="renameFile(file)" title="重命名">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
                  </svg>
                </button>
                <button class="btn-action btn-danger" @click.stop="deleteFile(file)" title="删除">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- 新建文件夹对话框 -->
    <div class="dialog-overlay" v-if="showNewFolder" @click.self="showNewFolder = false">
      <div class="dialog-content">
        <h4>新建文件夹</h4>
        <input
          v-model="newFolderName"
          type="text"
          placeholder="文件夹名称"
          @keyup.enter="createFolder"
          ref="newFolderInput"
        />
        <div class="dialog-actions">
          <button class="btn btn-secondary" @click="showNewFolder = false">取消</button>
          <button class="btn btn-primary" @click="createFolder">创建</button>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <div class="dialog-overlay" v-if="showRename" @click.self="showRename = false">
      <div class="dialog-content">
        <h4>重命名</h4>
        <input
          v-model="renameNewName"
          type="text"
          placeholder="新名称"
          @keyup.enter="doRename"
          ref="renameInput"
        />
        <div class="dialog-actions">
          <button class="btn btn-secondary" @click="showRename = false">取消</button>
          <button class="btn btn-primary" @click="doRename">确定</button>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
    >
      <template v-if="contextMenu.file?.isDirectory">
        <div class="menu-item" @click="enterDirectory(contextMenu.file)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
          </svg>
          <span>打开</span>
        </div>
      </template>
      <template v-else>
        <div class="menu-item" @click="downloadFile(contextMenu.file)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>下载</span>
        </div>
      </template>
      <div class="menu-item" @click="renameFile(contextMenu.file)">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
        </svg>
        <span>重命名</span>
      </div>
      <div class="menu-divider"></div>
      <div class="menu-item danger" @click="deleteFile(contextMenu.file)">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
        </svg>
        <span>删除</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 文件面板组件
 * 显示 SFTP 文件列表，支持文件操作
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useSFTPStore } from '@/stores/sftp'
import sftpService from '@/services/sftpService'
import { formatDateTime } from '@/utils/fileIcons'
import Breadcrumb from './Breadcrumb.vue'
import FileIcon from './FileIcon.vue'

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

const emit = defineEmits(['connect', 'disconnect'])

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

const isAllSelected = computed(() => {
  return files.value.length > 0 && files.value.every(f => isSelected(f))
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
 * @param {object} file - 文件对象
 * @returns {boolean} 是否选中
 */
function isSelected(file) {
  return selectedFiles.value.some(f => f.path === file.path)
}

/**
 * 切换全选
 * @param {Event} event - 事件对象
 */
function toggleSelectAll(event) {
  if (event.target.checked) {
    sftpStore.selectedFiles = [...files.value]
  } else {
    sftpStore.selectedFiles = []
  }
}

/**
 * 处理单击事件
 * @param {object} file - 文件对象
 * @param {Event} event - 事件对象
 */
function handleClick(file, event) {
  if (event.ctrlKey || event.metaKey) {
    sftpStore.selectFile(file, !isSelected(file))
  } else {
    sftpStore.selectedFiles = [file]
  }
}

/**
 * 处理双击事件
 * @param {object} file - 文件对象
 */
function handleDoubleClick(file) {
  if (file.isDirectory) {
    sftpStore.enterDirectory(file.name)
  }
  // 文件不处理双击，只能通过右键菜单下载
}

/**
 * 进入目录
 * @param {object} file - 目录对象
 */
function enterDirectory(file) {
  if (file.isDirectory) {
    sftpStore.enterDirectory(file.name)
  }
  hideContextMenu()
}

/**
 * 导航到指定路径
 * @param {string} path - 目标路径
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
 * @param {object} file - 文件对象
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
 * @param {object} file - 文件对象
 */
async function deleteFile(file) {
  hideContextMenu()
  if (!confirm(`确定要删除 "${file.name}" 吗？`)) return

  await sftpStore.deleteItem(file.path, file.isDirectory)
}

/**
 * 下载文件
 * @param {object} file - 文件对象
 */
async function downloadFile(file) {
  hideContextMenu()
  if (file.isDirectory) return

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
 * 处理文件上传
 * @param {Event} event - 事件对象
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
 * @param {object} file - 文件对象
 * @param {Event} event - 事件对象
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
 * 获取文件扩展名
 * @param {string} fileName - 文件名
 * @returns {string} 扩展名（带点）
 */
function getFileExtension(fileName) {
  const lastDot = fileName.lastIndexOf('.')
  if (lastDot === -1 || lastDot === 0) return ''
  return fileName.substring(lastDot).toLowerCase()
}

/**
 * 获取文件类型 CSS 类名
 * @param {object} file - 文件对象
 * @returns {string} 类型类名
 */
function getFileTypeClass(file) {
  if (file.isDirectory) return 'folder'

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

watch(() => props.connected, (connected) => {
  if (connected) {
    sftpStore.fetchFiles()
  }
})

onMounted(() => {
  document.addEventListener('click', hideContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', hideContextMenu)
})
</script>

<style scoped>
.file-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--panel-bg);
}

.panel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--sidebar-bg);
  border-bottom: 1px solid var(--border);
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
  background: var(--panel-bg);
  border: 1px solid var(--border);
  color: var(--text-main);
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
  background: var(--hover-bg);
  border-color: var(--accent);
}

.btn-toolbar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-toolbar:disabled:hover {
  border-color: var(--border);
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
  color: var(--text-dim);
}

.connect-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.btn-connect {
  background: var(--accent);
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
  background: var(--accent-hover);
}

.file-list-container {
  flex: 1;
  overflow: auto;
  position: relative;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
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
  background: var(--sidebar-bg);
  border-bottom: 1px solid var(--border);
  color: var(--text-dim);
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
  border-bottom: 1px solid var(--border);
}

.file-row {
  cursor: pointer;
  transition: all 0.15s ease;
}

.file-row:hover {
  background: var(--hover-bg);
}

.file-row.selected {
  background: rgba(59, 130, 246, 0.15);
}

.file-row.selected:hover {
  background: rgba(59, 130, 246, 0.2);
}

/* 目录行样式 */
.file-row.is-directory {
  background: rgba(240, 198, 116, 0.05);
}

.file-row.is-directory:hover {
  background: rgba(240, 198, 116, 0.12);
}

.file-row.is-directory.selected {
  background: rgba(240, 198, 116, 0.2);
}

/* 文件行样式 - 根据类型 */
.file-row.is-file.file-type-code {
  border-left: 3px solid #61dafb;
}

.file-row.is-file.file-type-image {
  border-left: 3px solid #a855f7;
}

.file-row.is-file.file-type-video {
  border-left: 3px solid #ef4444;
}

.file-row.is-file.file-type-audio {
  border-left: 3px solid #22c55e;
}

.file-row.is-file.file-type-archive {
  border-left: 3px solid #f59e0b;
}

.file-row.is-file.file-type-document {
  border-left: 3px solid #3b82f6;
}

.file-row.is-file.file-type-config {
  border-left: 3px solid #6b7280;
}

.file-row.is-file.file-type-database {
  border-left: 3px solid #8b5cf6;
}

.file-row.is-file.file-type-unknown {
  border-left: 3px solid transparent;
}

.col-checkbox {
  width: 40px;
}

.col-name {
  min-width: 200px;
}

.col-permissions {
  width: 100px;
}

.col-actions {
  width: 100px;
  text-align: center;
}

.empty-row {
  text-align: center;
  color: var(--text-dim);
  padding: 40px !important;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
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
  background: rgba(240, 198, 116, 0.2);
  color: #f0c674;
}

.file-ext {
  font-size: 10px;
  color: var(--text-dim);
  background: var(--hover-bg);
  padding: 2px 5px;
  border-radius: 3px;
  font-family: monospace;
}

.permission-text {
  font-family: monospace;
  font-size: 11px;
  color: var(--text-dim);
}

.btn-action {
  background: transparent;
  border: none;
  padding: 4px 6px;
  cursor: pointer;
  opacity: 0.5;
  transition: all 0.2s;
  border-radius: 4px;
  color: var(--text-main);
}

.btn-action:hover {
  opacity: 1;
  background: var(--hover-bg);
}

.btn-action.btn-danger:hover {
  opacity: 1;
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

/* 对话框样式 */
.dialog-overlay {
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
}

.dialog-content {
  background: var(--sidebar-bg);
  border-radius: 8px;
  padding: 20px;
  width: 320px;
  border: 1px solid var(--border);
}

.dialog-content h4 {
  margin: 0 0 16px;
  font-size: 15px;
}

.dialog-content input {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-dark);
  border: 1px solid var(--border);
  color: var(--text-main);
  border-radius: 6px;
  font-size: 13px;
  box-sizing: border-box;
  margin-bottom: 16px;
}

.dialog-content input:focus {
  outline: none;
  border-color: var(--accent);
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 6px 0;
  min-width: 160px;
  z-index: 2000;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.15s;
  color: var(--text-main);
}

.menu-item:hover {
  background: var(--hover-bg);
}

.menu-item.danger {
  color: #ef4444;
}

.menu-item.danger:hover {
  background: rgba(239, 68, 68, 0.15);
}

.menu-divider {
  height: 1px;
  background: var(--border);
  margin: 6px 0;
}

/* 按钮样式 */
.btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
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
  background: var(--hover-bg);
  color: var(--text-main);
  border: 1px solid var(--border);
}

.btn-secondary:hover {
  background: var(--border);
}
</style>
