<template>
  <div class="file-panel">
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
      ref="containerRef"
      class="file-list-container"
    >
      <div
        v-if="loading"
        class="loading-overlay"
      >
        <div class="loading-spinner" />
        <span>加载中...</span>
      </div>

      <FileTable
        v-else
        ref="fileTableRef"
        :file-list="sortedFiles"
        :current-path="currentPath"
        :selected-files="selectedFiles"
        @update:selected-files="handleSelectedFilesChange"
        @open-directory="handleOpenDirectory"
        @download-file="handleDownloadFile"
        @refresh="refresh"
        @show-context-menu="handleContextMenu"
      />
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
  </div>
</template>

<script setup>
/**
 * 文件面板组件 - 桌面级交互版本
 * 使用 vxe-table 实现专业文件管理器体验
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useSFTPStore } from '@/stores/sftp'
import sftpService from '@/services/sftpService'
import Breadcrumb from './Breadcrumb.vue'
import FileTable from './FileTable.vue'

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
const fileTableRef = ref(null)
const containerRef = ref(null)

const files = computed(() => sftpStore.files)
const currentPath = computed(() => sftpStore.currentPath)
const loading = computed(() => sftpStore.loading)

const showNewFolder = ref(false)
const newFolderName = ref('')
const newFolderInput = ref(null)

const showRename = ref(false)
const renameNewName = ref('')
const fileToRename = ref(null)
const renameInput = ref(null)

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

const selectedFiles = computed(() => sftpStore.selectedFiles)

/**
 * 处理文件选中变化
 */
function handleSelectedFilesChange(selected) {
  sftpStore.selectedFiles = selected
}

/**
 * 处理打开目录
 */
function handleOpenDirectory(file) {
  if (file.isDirectory) {
    sftpStore.enterDirectory(file.name)
  }
}

/**
 * 处理下载文件
 */
async function handleDownloadFile(file) {
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
 * 处理右键菜单
 */
function handleContextMenu({ action, file }) {
  switch (action) {
    case 'rename':
      renameFile(file)
      break
    case 'delete':
      deleteFile(file)
      break
    case 'cut':
      // 处理剪切操作
      sftpStore.cutItem(file.path)
      break
    case 'copy':
      // 处理复制操作
      sftpStore.copyToClipboard(file.path)
      break
  }
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
  if (!confirm(`确定要删除 "${file.name}" 吗？`)) return
  await sftpStore.deleteItem(file.path, file.isDirectory)
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
 * 键盘快捷键处理
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
    if (confirm(`确定要删除选中的 ${selectedFiles.value.length} 项吗？`)) {
      selectedFiles.value.forEach(file => {
        sftpStore.deleteItem(file.path, file.isDirectory)
      })
    }
  }
}

watch(() => props.connected, (connected) => {
  if (connected) {
    sftpStore.fetchFiles()
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.file-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #1e1e1e;
  color: #d4d4d4;
}

.panel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #252526;
  border-bottom: 1px solid #3e3e42;
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
  background: #3e3e42;
  border: 1px solid #505054;
  color: #cccccc;
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
  background: #505054;
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
  color: #808080;
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
  background: #1e1e1e;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #3e3e42;
  border-top-color: #007acc;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
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
  background: #252526;
  border: 1px solid #3e3e42;
  border-radius: 8px;
  padding: 20px;
  width: 320px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.dialog-content h4 {
  margin: 0 0 16px;
  font-size: 15px;
  color: #cccccc;
}

.dialog-content input {
  width: 100%;
  padding: 10px 12px;
  background: #1e1e1e;
  border: 1px solid #3e3e42;
  color: #d4d4d4;
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
  background: #3e3e42;
  color: #cccccc;
}

.btn-secondary:hover {
  background: #505054;
}

/* 滚动条样式 */
.file-list-container::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.file-list-container::-webkit-scrollbar-track {
  background: #1e1e1e;
}

.file-list-container::-webkit-scrollbar-thumb {
  background: #424242;
  border-radius: 5px;
}

.file-list-container::-webkit-scrollbar-thumb:hover {
  background: #505050;
}
</style>
