<template>
  <div class="file-panel">
    <!-- 工具栏 -->
    <div class="panel-toolbar">
      <div class="toolbar-left">
        <Breadcrumb :path="currentPath" @navigate="navigateTo" />
      </div>
      <div class="toolbar-right">
        <button class="btn-toolbar" @click="goUp" :disabled="currentPath === '/'" title="上级目录">
          ⬆️
        </button>
        <button class="btn-toolbar" @click="refresh" :disabled="loading" title="刷新">
          ↻
        </button>
        <button class="btn-toolbar" @click="showNewFolderDialog" title="新建文件夹">
          📁+
        </button>
        <label class="btn-toolbar upload-btn" title="上传文件">
          📤
          <input type="file" multiple @change="handleUpload" hidden />
        </label>
      </div>
    </div>

    <!-- 连接状态提示 -->
    <div v-if="!connected" class="connect-prompt">
      <div class="connect-icon">🔌</div>
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
            <th class="col-size">大小</th>
            <th class="col-modified">修改时间</th>
            <th class="col-permissions">权限</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="files.length === 0">
            <td colspan="6" class="empty-row">此目录为空</td>
          </tr>
          <tr
            v-for="file in files"
            :key="file.path"
            :class="{ selected: isSelected(file) }"
            @dblclick="handleDoubleClick(file)"
            @click="handleClick(file, $event)"
            @contextmenu.prevent="showContextMenu(file, $event)"
          >
            <td class="col-checkbox">
              <input type="checkbox" :checked="isSelected(file)" @click.stop />
            </td>
            <td class="col-name">
              <div class="file-name-cell">
                <span class="file-icon">{{ getFileIcon(file.name, file.isDirectory) }}</span>
                <span class="file-name">{{ file.name }}</span>
              </div>
            </td>
            <td class="col-size">{{ file.isDirectory ? '-' : formatSize(file.size) }}</td>
            <td class="col-modified">{{ formatDateTime(file.modifiedTime) }}</td>
            <td class="col-permissions">{{ file.permissions }}</td>
            <td class="col-actions">
              <button v-if="!file.isDirectory" class="btn-action" @click.stop="downloadFile(file)" title="下载">
                📥
              </button>
              <button class="btn-action" @click.stop="renameFile(file)" title="重命名">
                ✏️
              </button>
              <button class="btn-action btn-danger" @click.stop="deleteFile(file)" title="删除">
                🗑️
              </button>
            </td>
          </tr>
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
      <div class="menu-item" @click="downloadFile(contextMenu.file)" v-if="!contextMenu.file?.isDirectory">
        📥 下载
      </div>
      <div class="menu-item" @click="renameFile(contextMenu.file)">
        ✏️ 重命名
      </div>
      <div class="menu-item" @click="deleteFile(contextMenu.file)">
        🗑️ 删除
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useSFTPStore } from '@/stores/sftp'
import sftpService from '@/services/sftpService'
import { getFileIcon, formatFileSize, formatDateTime } from '@/utils/fileIcons'
import Breadcrumb from './Breadcrumb.vue'

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
  } else {
    downloadFile(file)
  }
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
  if (!confirm(`确定要删除 "${file.name}" 吗？`)) return

  await sftpStore.deleteItem(file.path, file.isDirectory)
}

/**
 * 下载文件
 * @param {object} file - 文件对象
 */
async function downloadFile(file) {
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
    alert('下载失败: ' + error.message)
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
      alert('上传失败: ' + error.message)
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
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化的大小
 */
function formatSize(bytes) {
  return formatFileSize(bytes)
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
}

.btn-toolbar:hover {
  background: var(--hover-bg);
}

.btn-toolbar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.upload-btn {
  cursor: pointer;
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
  font-size: 48px;
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

@keyframes spin {
  to { transform: rotate(360deg); }
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

.file-table tr:hover {
  background: var(--hover-bg);
}

.file-table tr.selected {
  background: rgba(59, 130, 246, 0.1);
}

.col-checkbox {
  width: 40px;
}

.col-name {
  min-width: 200px;
}

.col-size {
  width: 100px;
  text-align: right;
}

.col-modified {
  width: 160px;
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

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 16px;
}

.file-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-action {
  background: transparent;
  border: none;
  padding: 4px;
  cursor: pointer;
  font-size: 14px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.btn-action:hover {
  opacity: 1;
}

.btn-action.btn-danger:hover {
  opacity: 1;
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
  border-radius: 6px;
  padding: 6px 0;
  min-width: 140px;
  z-index: 2000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
}

.menu-item:hover {
  background: var(--hover-bg);
}
</style>
