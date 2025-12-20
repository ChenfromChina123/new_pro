<template>
  <div class="cloud-disk-component">
    <!-- 移动端侧边栏控制按钮 -->
    <button 
      class="mobile-sidebar-toggle"
      title="切换文件夹视图"
      @click="toggleSidebar"
    >
      📁 文件夹
    </button>
    
    <div 
      class="disk-container"
      @touchstart="handleTouchStart"
      @touchmove="handleTouchMove"
      @touchend="handleTouchEnd"
    >
      <!-- 左侧文件夹树 -->
      <aside 
        v-if="showSidebar"
        class="folder-sidebar"
        :class="{ 'sidebar-visible': isSidebarVisible }"
      >
        <div class="sidebar-header">
          <h3>📁 文件夹</h3>
          <div class="header-actions">
            <button
              class="icon-btn"
              title="新建文件夹"
              @click="showCreateFolderDialog"
            >
              <i class="fas fa-plus"></i>
            </button>
            <button
              class="icon-btn close-btn"
              title="关闭"
              @click="toggleSidebar"
            >
              <i class="fas fa-times"></i>
            </button>
          </div>
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
      </aside>
      
      <!-- 主文件区域 -->
      <main class="file-main">
        <div class="file-header">
          <div class="breadcrumb">
            <button
              class="breadcrumb-item"
              @click="goToRoot"
            >
              🏠 根目录
            </button>
            <!-- 只在有子文件夹时显示斜杠和当前文件夹 -->
            <template v-if="cloudDiskStore.currentFolder && cloudDiskStore.currentFolder !== ''">
              <span class="separator">/</span>
              <span class="breadcrumb-item current">
                {{ cloudDiskStore.currentFolder.replace(/^\//, '') }}
              </span>
            </template>
          </div>
          
          <div class="toolbar">
            <input
              ref="fileInput"
              type="file"
              multiple
              style="display: none"
              @change="handleFileSelect"
            >
            <input
              ref="folderInput"
              type="file"
              webkitdirectory
              directory
              multiple
              style="display: none"
              @change="handleFolderSelect"
            >
            <button
              class="btn btn-primary"
              @click="$refs.fileInput.click()"
            >
              <i class="fas fa-upload"></i> 上传文件
            </button>
            <button
              class="btn btn-secondary"
              @click="$refs.folderInput.click()"
            >
              <i class="fas fa-folder-plus"></i> 上传文件夹
            </button>
            <button
              class="btn btn-secondary"
              @click="downloadCurrentFolder"
            >
              <i class="fas fa-download"></i> 下载文件夹
            </button>
            
            <template v-if="cloudDiskStore.selectedFiles.length > 0">
              <button
                class="btn btn-secondary"
                @click="downloadSelected"
              >
                <i class="fas fa-download"></i> 下载 ({{ cloudDiskStore.selectedFiles.length }})
              </button>
              <button
                class="btn btn-secondary"
                @click="deleteSelected"
              >
                <i class="fas fa-trash"></i> 删除 ({{ cloudDiskStore.selectedFiles.length }})
              </button>
            </template>
          </div>
        </div>
        
        <div class="file-list">
          <div
            v-if="cloudDiskStore.isLoading"
            class="loading-state"
          >
            <div class="loading" />
            <p>加载中...</p>
          </div>
          
          <div
            v-else-if="cloudDiskStore.files.length === 0"
            class="empty-state"
          >
            <div class="empty-icon">
              <i class="fas fa-folder-open"></i>
            </div>
            <h3>暂无文件</h3>
            <p>点击上传文件按钮开始上传</p>
          </div>
          
          <div
            v-else
            class="file-table-container"
          >
            <table class="file-table">
              <thead>
                <tr>
                  <th class="select-all-column">
                    <input
                      type="checkbox"
                      :checked="areAllFilesSelected"
                      @click="toggleSelectAll"
                    >
                  </th>
                  <th
                    class="name-column"
                    @click="sortFiles('filename')"
                  >
                    <div class="column-header">
                      <span>名称</span>
                      <span
                        v-if="sortField === 'filename'"
                        class="sort-indicator"
                      >
                        {{ sortAscending ? '↑' : '↓' }}
                      </span>
                    </div>
                  </th>
                  <th
                    class="date-column"
                    @click="sortFiles('upload_time')"
                  >
                    <div class="column-header">
                      <span>修改日期</span>
                      <span
                        v-if="sortField === 'upload_time'"
                        class="sort-indicator"
                      >
                        {{ sortAscending ? '↑' : '↓' }}
                      </span>
                    </div>
                  </th>
                  <th
                    class="type-column"
                    @click="sortFiles('file_type')"
                  >
                    <div class="column-header">
                      <span>类型</span>
                      <span
                        v-if="sortField === 'file_type'"
                        class="sort-indicator"
                      >
                        {{ sortAscending ? '↑' : '↓' }}
                      </span>
                    </div>
                  </th>
                  <th
                    class="size-column"
                    @click="sortFiles('file_size')"
                  >
                    <div class="column-header">
                      <span>大小</span>
                      <span
                        v-if="sortField === 'file_size'"
                        class="sort-indicator"
                      >
                        {{ sortAscending ? '↑' : '↓' }}
                      </span>
                    </div>
                  </th>
                  <th class="actions-column">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="file in sortedFiles"
                  :key="file.id"
                  class="file-row"
                  :class="{ selected: isFileSelected(file.id) }"
                >
                  <td class="select-column">
                    <input
                      type="checkbox"
                      :checked="isFileSelected(file.id)"
                      @click="toggleFileSelection(file.id)"
                    >
                  </td>
                  <td class="name-column">
                    <div class="file-cell">
                      <span class="file-icon">{{ getFileIcon(file.filename) }}</span>
                      <span
                        class="file-name"
                        :title="file.filename"
                      >{{ file.filename }}</span>
                    </div>
                  </td>
                  <td class="date-column">
                    {{ formatDate(file.upload_time) }}
                  </td>
                  <td class="type-column">
                    {{ getFileTypeLabel(file.filename) }}
                  </td>
                  <td class="size-column">
                    {{ formatFileSize(file.file_size) }}
                  </td>
                  <td class="actions-column">
                    <div class="file-actions">
                      <button
                        class="action-btn"
                        title="预览"
                        @click="previewFile(file)"
                      >
                        <i class="fas fa-eye"></i>
                      </button>
                      <button
                        class="action-btn"
                        title="下载"
                        @click="downloadFile(file.id)"
                      >
                        <i class="fas fa-download"></i>
                      </button>
                      <button
                        class="action-btn delete"
                        title="删除"
                        @click="deleteFile(file.id)"
                      >
                        <i class="fas fa-trash"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
    
    <!-- 创建文件夹对话框 -->
    <div
      v-if="showCreateFolder"
      class="modal"
      @click.self="showCreateFolder = false"
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
      class="modal"
      @click.self="closeRenameFolderDialog"
    >
      <div class="modal-content">
        <h3>重命名文件夹</h3>
        <input
          v-model="renameFolderName"
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
    
    <!-- 文件预览对话框 -->
    <div
      v-if="previewFileData"
      class="modal"
      @click.self="closePreview"
    >
      <div class="modal-content large">
        <div class="modal-header">
          <h3>{{ previewFileData.filename }}</h3>
          <button
            class="close-btn"
            @click="closePreview"
          >
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div
            v-if="isPreviewable(previewFileData.filename)"
            class="preview-container"
          >
            <div
              v-if="!previewUrl"
              class="loading-preview"
            >
              <div class="loading" />
              <p>正在加载预览...</p>
            </div>
            <template v-else>
              <img 
                v-if="getFileType(previewFileData.filename) === 'image'" 
                :src="previewUrl" 
                class="preview-content preview-image" 
                alt="预览图片" 
              >
              <video 
                v-else-if="getFileType(previewFileData.filename) === 'video'" 
                :src="previewUrl" 
                controls 
                class="preview-content preview-video"
              />
              <audio 
                v-else-if="getFileType(previewFileData.filename) === 'audio'" 
                :src="previewUrl" 
                controls 
                class="preview-content preview-audio"
              />
              <div
                v-else-if="getFileType(previewFileData.filename) === 'text'"
                class="preview-content preview-text"
              >
                <pre><code>{{ previewText }}</code></pre>
              </div>
              <iframe
                v-else
                :src="previewUrl"
                class="preview-frame"
                sandbox="allow-scripts allow-same-origin"
              />
            </template>
          </div>
          <div
            v-else
            class="not-previewable"
          >
            <p>此文件类型不支持预览</p>
            <button
              class="btn btn-primary"
              @click="downloadFile(previewFileData.id)"
            >
              下载文件
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 上传进度 -->
    <div
      v-if="uploadProgress > 0 && uploadProgress < 100"
      class="upload-progress"
    >
      <div class="progress-bar">
        <div
          class="progress-fill"
          :style="{ width: uploadProgress + '%' }"
        />
      </div>
      <p>上传中... {{ uploadProgress }}%</p>
    </div>

    <ConflictResolutionDialog
      :visible="conflictDialogVisible"
      :files="currentConflictFiles"
      :batch-mode="pendingUploads.length > 1"
      :is-folder="!!renamingFolder"
      @resolve="onConflictResolved"
      @cancel="onConflictCancelled"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useCloudDiskStore } from '@/stores/cloudDisk'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import ConflictResolutionDialog from '@/components/ConflictResolutionDialog.vue'

const props = defineProps({
  showSidebar: {
    type: Boolean,
    default: true
  }
})

const cloudDiskStore = useCloudDiskStore()

const fileInput = ref(null)
const folderInput = ref(null)
const showCreateFolder = ref(false)
const showRenameFolder = ref(false)
const renamingFolder = ref(null)
const renameFolderName = ref('')
const newFolderName = ref('')
const previewFileData = ref(null)
const previewUrl = ref('')
const previewText = ref('')
const uploadProgress = ref(0)
const isSidebarVisible = ref(false)
const touchStartX = ref(0)
const touchEndX = ref(0)
const expandedFolders = ref(new Set()) // 用于跟踪哪些文件夹是展开的
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)

// 冲突处理状态
const conflictDialogVisible = ref(false)
const currentConflictFiles = ref([])
const pendingUploads = ref([])

// 排序相关
const sortField = ref('upload_time')
const sortAscending = ref(false)

const normalizeFolderPath = (folderPath) => {
  return (folderPath || '').replace(/^\//, '').replace(/\/+$/, '')
}

/**
 * 判断当前路径是否位于指定文件夹下（用于自动展开当前路径链路上的父级）。
 */
const isInActiveChain = (folder) => {
  const folderPath = (folder?.folderPath || '').replace(/\/+$/, '')
  const current = (cloudDiskStore.currentFolder || '').replace(/\/+$/, '')
  
  // 根目录始终展开
  if (folderPath === '') return true
  
  // 仅展开当前路径的父级（严格前缀检查）
  return current.startsWith(folderPath + '/')
}

/**
 * 切换文件夹展开状态。
 */
const toggleFolderExpand = (folderId, event) => {
  event.stopPropagation()
  const next = new Set(expandedFolders.value)
  if (next.has(folderId)) {
    next.delete(folderId)
  } else {
    next.add(folderId)
  }
  expandedFolders.value = next
}

/**
 * 判断文件夹是否展开。
 */
const isFolderExpanded = (folder) => {
  if (expandedFolders.value.has(folder.id)) return true
  if (isInActiveChain(folder)) return true
  return false
}

/**
 * 计算文件夹树最大深度。
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
 * 根据深度与屏幕尺寸动态计算缩进。
 */
const folderIndentPx = computed(() => {
  const depth = maxFolderDepth.value
  const isMobile = viewportWidth.value <= 768
  if (isMobile) return depth > 6 ? 10 : 12
  return depth > 8 ? 10 : depth > 5 ? 12 : 14
})

const toggleSidebar = () => {
  isSidebarVisible.value = !isSidebarVisible.value
}

const handleTouchStart = (e) => {
  touchStartX.value = e.touches[0].clientX
}

const handleTouchMove = (e) => {
  touchEndX.value = e.touches[0].clientX
}

const handleTouchEnd = () => {
  const diffX = touchEndX.value - touchStartX.value
  if (diffX > 50 && !isSidebarVisible.value) {
    isSidebarVisible.value = true
  } else if (diffX < -50 && isSidebarVisible.value) {
    isSidebarVisible.value = false
  }
  touchStartX.value = 0
  touchEndX.value = 0
}

const handleResize = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(async () => {
  window.addEventListener('resize', handleResize, { passive: true })
  await cloudDiskStore.fetchFolders()
  await cloudDiskStore.fetchFiles()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

const selectFolder = async (folderPath, folderId, event) => {
  if (event && typeof event.stopPropagation === 'function') {
    event.stopPropagation()
  }
  cloudDiskStore.setActiveFolder({ folderPath, folderId })
}

const goToRoot = () => {
  cloudDiskStore.setActiveFolder({ folderPath: '', folderId: null })
}

// 文件夹操作
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

const deleteFolderAction = async (folder) => {
  if (confirm(`确定要删除文件夹 "${folder.name}" 及其所有内容吗？`)) {
    await cloudDiskStore.deleteFolder(folder.id)
  }
}

const renameFolderAction = (folder) => {
  renamingFolder.value = folder
  renameFolderName.value = folder.name
  showRenameFolder.value = true
}

const confirmRenameFolder = async () => {
  if (!renameFolderName.value.trim() || !renamingFolder.value) return
  if (renameFolderName.value === renamingFolder.value.name) {
    closeRenameFolderDialog()
    return
  }
  
  const result = await cloudDiskStore.renameFolder(renamingFolder.value.id, renameFolderName.value)
  if (result.success) {
    closeRenameFolderDialog()
  } else if (result.error === 'FOLDER_EXISTS') {
    alert('该名称已存在')
  }
}

const closeRenameFolderDialog = () => {
  showRenameFolder.value = false
  renamingFolder.value = null
  renameFolderName.value = ''
}

// 文件选择
const handleFileSelect = async (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return
  
  pendingUploads.value = files
  await checkConflictsAndUpload()
  event.target.value = ''
}

const handleFolderSelect = async (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return
  
  pendingUploads.value = files
  await checkConflictsAndUpload()
  event.target.value = ''
}

const checkConflictsAndUpload = async () => {
  const conflicts = []
  for (const file of pendingUploads.value) {
    const relativePath = file.webkitRelativePath || file.name
    const exists = cloudDiskStore.files.some(f => f.filename === relativePath)
    if (exists) conflicts.push(file)
  }

  if (conflicts.length > 0) {
    currentConflictFiles.value = conflicts
    conflictDialogVisible.value = true
  } else {
    await performUpload('skip')
  }
}

const onConflictResolved = async ({ strategy }) => {
  conflictDialogVisible.value = false
  await performUpload(strategy)
}

const onConflictCancelled = () => {
  conflictDialogVisible.value = false
  pendingUploads.value = []
}

const performUpload = async (strategy) => {
  const files = pendingUploads.value
  pendingUploads.value = []
  
  uploadProgress.value = 1
  const result = await cloudDiskStore.uploadFiles(files, strategy, (progress) => {
    uploadProgress.value = progress
  })
  
  if (result.success) {
    setTimeout(() => { uploadProgress.value = 0 }, 1000)
  } else {
    uploadProgress.value = 0
    alert('上传失败: ' + result.message)
  }
}

// 文件列表操作
const sortedFiles = computed(() => {
  return [...cloudDiskStore.files].sort((a, b) => {
    let valA = a[sortField.value]
    let valB = b[sortField.value]
    
    if (sortField.value === 'filename') {
      valA = valA.toLowerCase()
      valB = valB.toLowerCase()
    }
    
    if (valA < valB) return sortAscending.value ? -1 : 1
    if (valA > valB) return sortAscending.value ? 1 : -1
    return 0
  })
})

const sortFiles = (field) => {
  if (sortField.value === field) {
    sortAscending.value = !sortAscending.value
  } else {
    sortField.value = field
    sortAscending.value = true
  }
}

const isFileSelected = (fileId) => cloudDiskStore.selectedFiles.includes(fileId)

const toggleFileSelection = (fileId) => {
  cloudDiskStore.toggleFileSelection(fileId)
}

const areAllFilesSelected = computed(() => {
  return cloudDiskStore.files.length > 0 && 
         cloudDiskStore.selectedFiles.length === cloudDiskStore.files.length
})

const toggleSelectAll = () => {
  if (areAllFilesSelected.value) {
    cloudDiskStore.clearSelection()
  } else {
    cloudDiskStore.selectAll()
  }
}

const downloadFile = (fileId) => cloudDiskStore.downloadFile(fileId)

const downloadSelected = () => cloudDiskStore.downloadSelectedFiles()

const deleteFile = async (fileId) => {
  if (confirm('确定要删除这个文件吗？')) {
    await cloudDiskStore.deleteFile(fileId)
  }
}

const deleteSelected = async () => {
  if (confirm(`确定要删除选中的 ${cloudDiskStore.selectedFiles.length} 个文件吗？`)) {
    await cloudDiskStore.deleteSelectedFiles()
  }
}

const downloadCurrentFolder = () => cloudDiskStore.downloadCurrentFolder()

// 预览相关
const isPreviewable = (filename) => {
  const type = getFileType(filename)
  return ['image', 'video', 'audio', 'text', 'pdf'].includes(type)
}

const getFileType = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp'].includes(ext)) return 'image'
  if (['mp4', 'webm', 'ogg'].includes(ext)) return 'video'
  if (['mp3', 'wav', 'ogg'].includes(ext)) return 'audio'
  if (['txt', 'js', 'css', 'html', 'json', 'md', 'py', 'c', 'cpp', 'java'].includes(ext)) return 'text'
  if (ext === 'pdf') return 'pdf'
  return 'other'
}

const previewFile = async (file) => {
  previewFileData.value = file
  const type = getFileType(file.filename)
  
  if (type === 'text') {
    const text = await cloudDiskStore.getFileContent(file.id)
    previewText.value = text
    previewUrl.value = 'text-preview'
  } else {
    const url = await cloudDiskStore.getFilePreviewUrl(file.id)
    previewUrl.value = url
  }
}

const closePreview = () => {
  if (previewUrl.value && previewUrl.value !== 'text-preview') {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewFileData.value = null
  previewUrl.value = ''
  previewText.value = ''
}

// 格式化工具
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getFileTypeLabel = (filename) => {
  const ext = filename.split('.').pop().toUpperCase()
  return ext || '未知'
}

const getFileIcon = (filename) => {
  const type = getFileType(filename)
  switch (type) {
    case 'image': return '🖼️'
    case 'video': return '🎥'
    case 'audio': return '🎵'
    case 'text': return '📄'
    case 'pdf': return '📕'
    default: return '📁'
  }
}
</script>

<style scoped>
.cloud-disk-component {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  position: relative;
}

.disk-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  position: relative;
}

/* 文件夹侧边栏 */
.folder-sidebar {
  width: 300px;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 24px 20px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 0 4px;
}

.sidebar-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.icon-btn:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.folder-tree {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.folder-tree::-webkit-scrollbar {
  width: 5px;
}

.folder-tree::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 10px;
}

/* 主文件区域 */
.file-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--bg-primary);
}

.file-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-primary);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}

.breadcrumb-item {
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
  background: none;
  border: none;
}

.breadcrumb-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.breadcrumb-item.current {
  color: var(--text-primary);
  font-weight: 500;
  cursor: default;
}

.breadcrumb-item.current:hover {
  background: none;
}

.separator {
  color: var(--border-color);
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.btn-primary {
  background: var(--gradient-primary);
  color: white;
  border: none;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-secondary {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--bg-hover);
  border-color: var(--text-secondary);
}

/* 文件列表 */
.file-list {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.file-table-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px 24px;
}

.file-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.file-table th {
  position: sticky;
  top: 0;
  background-color: var(--bg-primary);
  z-index: 10;
  text-align: left;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
}

.column-header {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  user-select: none;
}

.column-header:hover {
  color: var(--text-primary);
}

.file-row {
  transition: background-color 0.2s;
}

.file-row:hover {
  background-color: var(--bg-hover);
}

.file-row.selected {
  background-color: rgba(var(--primary-rgb), 0.05);
}

.file-table td {
  padding: 12px 16px;
  font-size: 14px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  font-size: 18px;
  width: 24px;
  display: flex;
  justify-content: center;
}

.file-name {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.file-row:hover .file-actions {
  opacity: 1;
}

.action-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s;
  font-size: 14px;
}

.action-btn:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.action-btn.delete:hover {
  color: #ff4d4f;
}

/* 状态展示 */
.loading-state, .empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  animation: fadeIn 0.3s ease;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.loading {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 弹窗 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
}

.modal-content {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  animation: modal-in 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-content.large {
  max-width: 900px;
  height: 80vh;
  display: flex;
  flex-direction: column;
}

@keyframes modal-in {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal h3 {
  margin: 0 0 20px;
  font-size: 18px;
}

.input {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-primary);
  color: var(--text-primary);
  margin-bottom: 20px;
  outline: none;
}

.input:focus {
  border-color: var(--primary-color);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-body {
  flex: 1;
  overflow: hidden;
  display: flex;
}

.preview-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
  border-radius: 8px;
  overflow: hidden;
}

.preview-content {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}

.preview-text {
  width: 100%;
  height: 100%;
  padding: 20px;
  overflow: auto;
  font-family: monospace;
  font-size: 14px;
  background-color: #1e1e1e;
  color: #d4d4d4;
}

.preview-text pre {
  margin: 0;
}

/* 移动端适配 */
.mobile-sidebar-toggle {
  display: none;
}

@media (max-width: 768px) {
  .folder-sidebar {
    position: absolute;
    left: -300px;
    top: 0;
    bottom: 0;
    z-index: 100;
    box-shadow: 10px 0 30px rgba(0,0,0,0.2);
  }
  
  .folder-sidebar.sidebar-visible {
    left: 0;
  }
  
  .mobile-sidebar-toggle {
    display: block;
    padding: 12px;
    background-color: var(--bg-secondary);
    border: none;
    border-bottom: 1px solid var(--border-color);
    color: var(--text-primary);
    text-align: left;
    font-weight: 500;
  }
}

/* 上传进度 */
.upload-progress {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  width: 240px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.2);
  z-index: 1001;
}

.progress-bar {
  height: 6px;
  background-color: var(--bg-hover);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  transition: width 0.3s ease;
}

.upload-progress p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
