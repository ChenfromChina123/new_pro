<template>
  <div class="cloud-disk-page">
    <div class="disk-container">
      <!-- 主文件区域 -->
      <main class="file-main">
        <div class="file-header">
          <div class="breadcrumb">
            <template v-if="breadcrumbSegments.length === 0">
              <span class="breadcrumb-item current">全部文件</span>
            </template>
            <template v-else>
              <template 
                v-for="(segment, index) in breadcrumbSegments" 
                :key="index"
              >
                <span 
                  v-if="index > 0" 
                  class="separator"
                >
                  >
                </span>
                <button
                  class="breadcrumb-item"
                  :class="{ current: index === breadcrumbSegments.length - 1 }"
                  @click="index < breadcrumbSegments.length - 1 ? selectFolder(segment.path) : null"
                >
                  {{ segment.name }}
                </button>
              </template>
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
            
            <div class="toolbar-left">
              <button
                class="btn btn-secondary"
                @click="handleNewFolder"
              >
                📁 新建文件夹
              </button>
              <button
                class="btn btn-secondary"
                @click="$refs.fileInput.click()"
              >
                📤 上传文件
              </button>
            </div>

            <div class="toolbar-right">
              <template v-if="cloudDiskStore.selectedFiles.length > 0">
                <button
                  class="btn btn-action"
                  @click="downloadSelected"
                >
                  💾 下载
                </button>
                <button
                  class="btn btn-action delete"
                  @click="deleteSelected"
                >
                  🗑️ 删除
                </button>
              </template>
            </div>
          </div>
        </div>
        
        <div class="file-list">
          <div
            v-if="cloudDiskStore.isLoading"
            class="loading-state"
          >
            <div class="loading"></div>
            <p>加载中...</p>
          </div>
          
          <div
            v-else-if="cloudDiskStore.files.length === 0"
            class="empty-state"
          >
            <div class="empty-icon">
              📭
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
                  <th class="select-column">
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
                      <span>文件名</span>
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
                      <span>修改时间</span>
                      <span
                        v-if="sortField === 'upload_time'"
                        class="sort-indicator"
                      >
                        {{ sortAscending ? '↑' : '↓' }}
                      </span>
                    </div>
                  </th>
                  <th class="actions-column">
                    <div class="column-header">
                      <span>操作</span>
                    </div>
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
                  <td
                    class="select-column"
                    @click.stop="toggleFileSelection(file.id)"
                  >
                    <input
                      type="checkbox"
                      :checked="isFileSelected(file.id)"
                      @click.stop="toggleFileSelection(file.id)"
                    >
                  </td>
                  <td
                    class="name-column"
                    @click.stop="toggleFileSelection(file.id)"
                  >
                    <div class="file-cell">
                      <span class="file-icon-wrapper">
                        <span class="file-icon-img">{{ getFileIcon(file.filename) }}</span>
                      </span>
                      <span
                        class="file-name"
                        :title="file.filename"
                      >{{ file.filename }}</span>
                    </div>
                  </td>
                  <td
                    class="date-column"
                    @click.stop="toggleFileSelection(file.id)"
                  >
                    {{ formatDate(file.upload_time) }}
                  </td>
                  <td
                    class="actions-column"
                    @click.stop
                  >
                    <div class="file-actions">
                      <button
                        class="action-btn"
                        title="预览"
                        @click="previewFile(file)"
                      >
                        👁️
                      </button>
                      <button
                        class="action-btn"
                        title="下载"
                        @click="downloadFile(file.id)"
                      >
                        📥
                      </button>
                      <button
                        class="action-btn delete-btn"
                        title="删除"
                        @click="deleteFile(file.id)"
                      >
                        🗑️
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
    
    <!-- 文件预览对话框 -->
    <div
      v-if="previewFileData"
      class="modal"
      @click.self="closePreview"
    >
      <div class="modal-content large">
        <div class="modal-header">
          <h3>{{ isEditMode ? '编辑文件' : '预览文件' }}: {{ previewFileData.filename }}</h3>
          <div class="header-actions">
            <!-- 文本文件显示编辑按钮 -->
            <button
              v-if="getFileType(previewFileData.filename) === 'text' && !isEditMode"
              class="edit-btn"
              title="编辑文件"
              @click="enterEditMode"
            >
              ✏️ 编辑
            </button>
            <button
              class="close-btn"
              @click="closePreview"
            >
              ✕
            </button>
          </div>
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
              <div class="loading"></div>
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
              ></video>
              <audio 
                v-else-if="getFileType(previewFileData.filename) === 'audio'" 
                :src="previewUrl" 
                controls 
                class="preview-content preview-audio"
              ></audio>
              <div
                v-else-if="getFileType(previewFileData.filename) === 'text'"
                class="preview-content preview-text"
              >
                <!-- 编辑模式 -->
                <textarea
                  v-if="isEditMode"
                  v-model="editingText"
                  class="text-editor"
                  :disabled="isSaving"
                ></textarea>
                <!-- 预览模式 -->
                <pre v-else><code>{{ previewText }}</code></pre>
              </div>
              <!-- PDF 预览使用 embed 标签以获得更好的兼容性 -->
              <embed
                v-else-if="getFileType(previewFileData.filename) === 'pdf'"
                :src="previewUrl"
                type="application/pdf"
                class="preview-frame"
              >
              <!-- 其他文件类型使用 iframe -->
              <iframe
                v-else
                :src="previewUrl"
                class="preview-frame"
                title="文件预览"
              ></iframe>
            </template>
          </div>
          <div
            v-else
            class="not-previewable"
          >
            <div class="file-icon-large">
              {{ getFileTypeIcon(previewFileData.filename) }}
            </div>
            <p class="file-name">
              {{ previewFileData.filename }}
            </p>
            <p class="file-info">
              {{ getNotPreviewableMessage(previewFileData.filename) }}
            </p>
            <button
              class="btn btn-primary"
              @click="downloadFile(previewFileData.id)"
            >
              <i class="fas fa-download"></i> 下载文件
            </button>
          </div>
        </div>
        <!-- 编辑模式的操作按钮 -->
        <div
          v-if="isEditMode"
          class="modal-footer"
        >
          <button
            class="btn btn-secondary"
            :disabled="isSaving"
            @click="cancelEdit"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            :disabled="isSaving"
            @click="saveFile"
          >
            <span v-if="isSaving">保存中...</span>
            <span v-else>💾 保存</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 上传进度对话框 -->
    <div v-if="isUploading" class="modal-overlay upload-progress-overlay">
      <div class="modal-content progress-modal">
        <div class="modal-header">
          <h3>正在上传...</h3>
        </div>
        <div class="modal-body">
          <div class="progress-info">
            <span class="filename" :title="uploadingFileName">{{ uploadingFileName }}</span>
            <span class="percentage">{{ uploadProgress }}%</span>
          </div>
          <div class="progress-bar-container">
            <div class="progress-bar" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <p class="upload-tip">请勿关闭页面，等待上传完成...</p>
        </div>
      </div>
    </div>

    <!-- 新建文件夹对话框 -->
    <div
      v-if="showCreateFolderDialog"
      class="modal"
      @click.self="cancelCreateFolder"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3>创建新文件夹</h3>
          <button
            class="close-btn"
            @click="cancelCreateFolder"
          >
            ✕
          </button>
        </div>
        <div class="modal-body">
          <input
            v-model="newFolderName"
            type="text"
            class="input"
            placeholder="输入文件夹名称"
            autofocus
            @keyup.enter="confirmCreateFolder"
          >
        </div>
        <div class="modal-footer">
          <button
            class="btn btn-secondary"
            @click="cancelCreateFolder"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="confirmCreateFolder"
          >
            创建
          </button>
        </div>
      </div>
    </div>

    <ConflictResolutionDialog
      :visible="conflictDialogVisible"
      :files="currentConflictFiles"
      :batch-mode="pendingUploads.length > 1"
      :is-folder="!!cloudDiskStore.renamingFolder"
      @resolve="onConflictResolved"
      @cancel="onConflictCancelled"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, onActivated } from 'vue'
import { useCloudDiskStore } from '@/stores/cloudDisk'
import { useUIStore } from '@/stores/ui'
import ConflictResolutionDialog from '@/components/ConflictResolutionDialog.vue'

const cloudDiskStore = useCloudDiskStore()
const uiStore = useUIStore()

const fileInput = ref(null)
const folderInput = ref(null)
const previewFileData = ref(null)
const previewUrl = ref('')
const previewText = ref('')
const uploadProgress = ref(0)
const uploadingFileName = ref('')
const isUploading = ref(false)
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)

// 编辑相关状态
const isEditMode = ref(false)
const editingText = ref('')
const isSaving = ref(false)

// 新建文件夹对话框状态
const showCreateFolderDialog = ref(false)
const newFolderName = ref('')

// 冲突处理状态
const conflictDialogVisible = ref(false)
const currentConflictFiles = ref([])
const pendingUploads = ref([])

// 排序相关
const sortField = ref('upload_time')
const sortAscending = ref(false)

/**
 * 刷新数据
 */
const refreshData = async () => {
  await Promise.all([
    cloudDiskStore.fetchFolders(),
    cloudDiskStore.fetchFiles(cloudDiskStore.currentFolder),
    cloudDiskStore.fetchQuota()
  ])
}

/**
 * 计算面包屑路径分段
 */
const breadcrumbSegments = computed(() => {
  const folderPath = cloudDiskStore.currentFolder
  if (!folderPath) return []
  
  const segments = []
  const parts = folderPath.split('/')
  let currentPath = ''
  
  for (const part of parts) {
    if (part) {
      currentPath = currentPath ? `${currentPath}/${part}` : part
      segments.push({
        name: part,
        path: currentPath
      })
    }
  }
  return segments
})

/**
 * 监听窗口尺寸变化
 */
const handleResize = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(async () => {
  window.addEventListener('resize', handleResize, { passive: true })
  await refreshData()
})

onActivated(async () => {
  await refreshData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

/**
 * 选择文件夹并刷新文件列表。
 */
const selectFolder = async (folderPath, folderId, event) => {
  if (event && typeof event.stopPropagation === 'function') {
    event.stopPropagation()
  }
  cloudDiskStore.setActiveFolder({ folderPath, folderId })
  await cloudDiskStore.fetchFiles(folderPath)
  cloudDiskStore.clearSelection()
}

const goToRoot = async () => {
  cloudDiskStore.setActiveFolder({ folderPath: '', folderId: null })
  await cloudDiskStore.fetchFiles('')
  cloudDiskStore.clearSelection()
}

/**
 * 处理新建文件夹
 */
const handleNewFolder = () => {
  // 检查层级限制
  if (!cloudDiskStore.canCreateSubFolder()) {
    uiStore.showToast('目录层级超出限制，最多支持两层目录（不计根目录）')
    return
  }
  
  newFolderName.value = '新建文件夹'
  showCreateFolderDialog.value = true
}

// 取消创建文件夹
const cancelCreateFolder = () => {
  showCreateFolderDialog.value = false
  newFolderName.value = ''
}

// 确认创建文件夹
const confirmCreateFolder = () => {
  if (newFolderName.value && newFolderName.value.trim()) {
    createFolder(newFolderName.value)
    showCreateFolderDialog.value = false
    newFolderName.value = ''
  }
}

const createFolder = async (folderName) => {
  const result = await cloudDiskStore.createFolder(folderName)
  if (result.success) {
    uiStore.showToast('创建成功')
    await refreshData()
  } else {
    uiStore.showToast(`创建文件夹失败: ${result.message}`)
  }
}

const handleFileSelect = async (event) => {
  const files = Array.from(event.target.files)
  if (!files || files.length === 0) return
  
  // 检查存储空间
  if (cloudDiskStore.quota.limitSize !== -1) {
    const totalSize = files.reduce((acc, f) => acc + f.size, 0)
    if (cloudDiskStore.quota.usedSize + totalSize > cloudDiskStore.quota.limitSize) {
      uiStore.showToast(`存储空间不足！当前可用空间约 ${formatFileSize(cloudDiskStore.quota.limitSize - cloudDiskStore.quota.usedSize)}`)
      event.target.value = ''
      return
    }
  }
  
  // 重置批处理策略和队列
  cloudDiskStore.batchStrategy = null
  pendingUploads.value = []
  
  // 准备上传队列
  for (const file of files) {
      pendingUploads.value.push({ file })
  }
  
  // 开始处理队列
  await processUploadQueue()
  
  event.target.value = '' // 重置input
}

const processUploadQueue = async () => {
    if (pendingUploads.value.length === 0) {
        uploadProgress.value = 0
        return
    }

    const item = pendingUploads.value[0]
    const { file } = item
    
    // 检查当前文件夹中是否存在同名文件
    // 注意：如果是批量上传，前一个文件的上传可能会更新列表，但fetchFiles通常是异步的
    // 这里我们假设cloudDiskStore.files在上传成功后会被更新
    const exists = cloudDiskStore.files.some(f => 
        (f.filename === file.name || f.originalFilename === file.name || f.original_filename === file.name)
    )
    
    if (exists) {
        if (cloudDiskStore.batchStrategy) {
            // 应用批处理策略
            await performUpload(file, cloudDiskStore.batchStrategy)
            pendingUploads.value.shift()
            await processUploadQueue()
        } else {
            // 显示冲突对话框
            currentConflictFiles.value = [file]
            conflictDialogVisible.value = true
            // 暂停在这里，等待对话框回调
        }
    } else {
        // 无冲突，默认使用重命名（虽然没冲突时后端不care，但保持一致）
        await performUpload(file, 'RENAME') 
        pendingUploads.value.shift()
        await processUploadQueue()
    }
}

/**
 * 执行文件上传
 * @param {File} file - 要上传的文件对象
 * @param {string} strategy - 冲突处理策略 (RENAME, OVERWRITE, SKIP)
 */
const performUpload = async (file, strategy) => {
  uploadProgress.value = 0
  uploadingFileName.value = file.name
  isUploading.value = true
  const folderPath = cloudDiskStore.currentFolder
  
  try {
    const result = await cloudDiskStore.uploadFile(
      file, 
      folderPath, 
      (progress) => { uploadProgress.value = progress },
      strategy
    )
    
    if (result.success) {
      console.log('文件上传成功:', file.name)
    } else {
      uiStore.showToast(`上传失败: ${result.message}`)
    }
  } catch (error) {
    console.error('上传过程出错:', error)
    if (error.code === 'ECONNABORTED') {
      uiStore.showToast('上传超时，文件可能过大，请稍后重试')
    } else {
      uiStore.showToast('上传出错，请检查网络连接')
    }
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
    uploadingFileName.value = ''
  }
}

const onConflictResolved = async ({ strategy, applyToAll }) => {
    conflictDialogVisible.value = false

    if (applyToAll) {
        cloudDiskStore.batchStrategy = strategy
    }
    
    // 继续上传当前文件
    const item = pendingUploads.value[0]
    performUpload(item.file, strategy).then(() => {
        pendingUploads.value.shift()
        processUploadQueue()
    })
}

const onConflictCancelled = () => {
    conflictDialogVisible.value = false
    
    // 跳过当前文件
    pendingUploads.value.shift()
    processUploadQueue()
}

/**
 * 处理文件夹上传
 * @param {Event} event - 文件选择事件
 */
const handleFolderSelect = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  
  uploadProgress.value = 0
  uploadingFileName.value = `文件夹: ${files[0].webkitRelativePath.split('/')[0]}`
  isUploading.value = true
  
  try {
    const result = await cloudDiskStore.uploadFolderStream(
      files,
      cloudDiskStore.currentFolder,
      (progress) => {
        uploadProgress.value = progress
      }
    )
    
    if (result.success) {
      console.log('文件夹上传成功')
    } else {
      uiStore.showToast(`上传失败: ${result.message}`)
    }
  } catch (error) {
    console.error('文件夹上传出错:', error)
    if (error.code === 'ECONNABORTED') {
      uiStore.showToast('上传超时，文件夹可能过大，请稍后重试')
    } else {
      uiStore.showToast('上传出错，请检查网络连接')
    }
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
    uploadingFileName.value = ''
    event.target.value = '' // 重置input
  }
}

const isFileSelected = (fileId) => {
  return cloudDiskStore.selectedFiles.includes(fileId)
}

const toggleFileSelection = (fileId) => {
  cloudDiskStore.toggleFileSelection(fileId)
}

const downloadFile = async (fileId) => {
  const file = cloudDiskStore.files.find(f => f.id === fileId)
  if (file) {
    await cloudDiskStore.downloadFileBlob(file)
  }
}

const downloadSelected = async () => {
  for (const fileId of cloudDiskStore.selectedFiles) {
    await downloadFile(fileId)
  }
}

const deleteFile = async (fileId) => {
  if (confirm('确定要删除这个文件吗？')) {
    const result = await cloudDiskStore.deleteFile(fileId)
    if (!result.success) {
      uiStore.showToast(`删除失败: ${result.message}`)
    }
  }
}

const deleteSelected = async () => {
  if (confirm(`确定要删除选中的 ${cloudDiskStore.selectedFiles.length} 个文件吗？`)) {
    const result = await cloudDiskStore.deleteFiles([...cloudDiskStore.selectedFiles])
    if (result.success) {
      cloudDiskStore.clearSelection()
      uiStore.showToast('删除成功')
    } else {
      uiStore.showToast(`删除失败: ${result.successCount}/${result.totalCount} 个文件已删除`)
    }
  }
}

// 下载当前文件夹
const downloadCurrentFolder = async () => {
  const folderPath = cloudDiskStore.currentFolder
  const folderName = folderPath || '根目录'
  
  if (confirm(`确定要下载文件夹 "${folderName}" 吗？`)) {
    const result = await cloudDiskStore.downloadFolder(folderPath)
    if (!result.success) {
      uiStore.showToast(`下载失败: ${result.message}`)
    }
  }
}

const getMimeType = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const mimeMap = {
    jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', gif: 'image/gif', webp: 'image/webp',
    pdf: 'application/pdf',
    txt: 'text/plain', md: 'text/markdown', json: 'application/json',
    js: 'text/javascript', css: 'text/css', html: 'text/html',
    mp4: 'video/mp4', webm: 'video/webm',
    mp3: 'audio/mpeg', wav: 'audio/wav'
  }
  return mimeMap[ext] || 'application/octet-stream'
}

const getFileType = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) return 'image'
  if (['pdf'].includes(ext)) return 'pdf'
  if (['mp4', 'webm'].includes(ext)) return 'video'
  if (['mp3', 'wav'].includes(ext)) return 'audio'
  if (['txt', 'md', 'json', 'js', 'css', 'html'].includes(ext)) return 'text'
  return 'other'
}

const previewFile = async (file) => {
  console.log('👁️ Preview file clicked:', file)
  
  previewFileData.value = file
  previewText.value = ''
  previewUrl.value = ''
  
  if (isPreviewable(file.filename)) {
    const fileType = getFileType(file.filename)
    const mimeType = getMimeType(file.filename)
    
    console.log('📋 File type:', fileType, 'MIME type:', mimeType)
    
    try {
      if (fileType === 'text') {
        // 读取文本文件内容 - 使用带认证的请求
        console.log('📝 Loading text file...')
        const textContent = await cloudDiskStore.fetchTextFileContent(file.id)
        if (textContent !== null) {
          previewText.value = textContent
          previewUrl.value = 'text-preview'
          console.log('✅ Text file loaded, length:', textContent.length)
        } else {
          console.error('❌ Failed to load text file')
          uiStore.showToast('预览失败：无法加载文本文件')
        }
      } else {
        // 其他类型文件使用URL预览
        console.log('🖼️ Loading binary file...')
        const url = await cloudDiskStore.fetchPreviewUrl(file.id, mimeType)
        if (url) {
          previewUrl.value = url
          console.log('✅ Binary file preview URL set:', url)
        } else {
          console.error('❌ Failed to get preview URL')
          uiStore.showToast('预览失败：无法加载文件')
        }
      }
    } catch (error) {
      console.error('❌ Error in previewFile:', error)
      uiStore.showToast('预览失败：' + error.message)
    }
  } else {
    console.log('ℹ️ File is not previewable, showing download option')
  }
}

const closePreview = () => {
  previewFileData.value = null
  previewText.value = ''
  editingText.value = ''
  isEditMode.value = false
  isSaving.value = false
  if (previewUrl.value && previewUrl.value !== 'text-preview') {
    window.URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
}

// 进入编辑模式
const enterEditMode = () => {
  editingText.value = previewText.value
  isEditMode.value = true
}

// 取消编辑
const cancelEdit = () => {
  if (confirm('确定要取消编辑吗？未保存的更改将丢失。')) {
    isEditMode.value = false
    editingText.value = ''
  }
}

// 保存文件
const saveFile = async () => {
  if (!previewFileData.value) return
  
  try {
    isSaving.value = true
    console.log('💾 Saving file:', previewFileData.value.id)
    
    const result = await cloudDiskStore.updateFileContent(
      previewFileData.value.id,
      editingText.value
    )
    
    if (result.success) {
      previewText.value = editingText.value
      isEditMode.value = false
      uiStore.showToast('保存成功！')
      console.log('✅ File saved successfully')
    } else {
      uiStore.showToast('保存失败：' + result.message)
      console.error('❌ Failed to save file:', result.message)
    }
  } catch (error) {
    console.error('❌ Error saving file:', error)
    uiStore.showToast('保存失败：' + error.message)
  } finally {
    isSaving.value = false
  }
}

const isPreviewable = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const previewableTypes = [
    'jpg', 'jpeg', 'png', 'gif', 'webp',
    'pdf',
    'txt', 'md', 'json', 'html', 'css', 'js',
    'mp4', 'webm',
    'mp3', 'wav'
  ]
  return previewableTypes.includes(ext)
}

// 获取文件类型标签
const getFileTypeLabel = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const typeMap = {
    pdf: 'PDF文档',
    doc: 'Word文档', docx: 'Word文档',
    xls: 'Excel表格', xlsx: 'Excel表格',
    ppt: 'PowerPoint演示', pptx: 'PowerPoint演示',
    jpg: '图片', jpeg: '图片', png: '图片', gif: '图片', webp: '图片',
    mp4: '视频', avi: '视频', mov: '视频', webm: '视频',
    mp3: '音频', wav: '音频',
    zip: '压缩文件', rar: '压缩文件', '7z': '压缩文件',
    txt: '文本文件', md: 'Markdown文件',
    js: 'JavaScript文件', py: 'Python文件', java: 'Java文件',
    html: 'HTML文件', css: 'CSS文件',
    json: 'JSON文件',
  }
  return typeMap[ext] || '文件'
}

const getFileIcon = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const iconMap = {
    pdf: '📄',
    doc: '📝', docx: '📝',
    xls: '📊', xlsx: '📊',
    ppt: '📊', pptx: '📊',
    jpg: '🖼️', jpeg: '🖼️', png: '🖼️', gif: '🖼️',
    mp4: '🎬', avi: '🎬', mov: '🎬',
    mp3: '🎵', wav: '🎵',
    zip: '📦', rar: '📦', '7z': '📦',
    txt: '📃', md: '📃',
    js: '💻', py: '🐍', java: '☕',
  }
  return iconMap[ext] || '📄'
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

// 获取大图标（用于预览）
const getFileTypeIcon = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const iconMap = {
    doc: '📄', docx: '📄',
    xls: '📊', xlsx: '📊',
    ppt: '📽️', pptx: '📽️',
    zip: '📦', rar: '📦', '7z': '📦',
  }
  return iconMap[ext] || '📁'
}

// 获取不可预览文件的提示信息
const getNotPreviewableMessage = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const messages = {
    doc: 'Word 文档无法在线预览，请下载后使用 Microsoft Word 或 WPS 打开',
    docx: 'Word 文档无法在线预览，请下载后使用 Microsoft Word 或 WPS 打开',
    xls: 'Excel 表格无法在线预览，请下载后使用 Microsoft Excel 或 WPS 打开',
    xlsx: 'Excel 表格无法在线预览，请下载后使用 Microsoft Excel 或 WPS 打开',
    ppt: 'PowerPoint 演示文稿无法在线预览，请下载后使用 Microsoft PowerPoint 或 WPS 打开',
    pptx: 'PowerPoint 演示文稿无法在线预览，请下载后使用 Microsoft PowerPoint 或 WPS 打开',
    zip: '压缩文件无法预览，请下载后解压查看',
    rar: '压缩文件无法预览，请下载后解压查看',
    '7z': '压缩文件无法预览，请下载后解压查看',
  }
  return messages[ext] || '此文件类型不支持在线预览，请下载后查看'
}

// 排序文件
const sortFiles = (field) => {
  if (sortField.value === field) {
    sortAscending.value = !sortAscending.value
  } else {
    sortField.value = field
    sortAscending.value = true
  }
}

// 计算属性：排序后的文件列表
const sortedFiles = computed(() => {
  const files = [...cloudDiskStore.files]
  return files.sort((a, b) => {
    let aVal, bVal
    
    switch (sortField.value) {
      case 'filename':
        aVal = a.filename.toLowerCase()
        bVal = b.filename.toLowerCase()
        break
      case 'upload_time':
        aVal = new Date(a.upload_time)
        bVal = new Date(b.upload_time)
        break
      case 'file_size':
        aVal = a.file_size
        bVal = b.file_size
        break
      case 'file_type':
        aVal = getFileTypeLabel(a.filename)
        bVal = getFileTypeLabel(b.filename)
        break
      default:
        return 0
    }
    
    if (aVal < bVal) return sortAscending.value ? -1 : 1
    if (aVal > bVal) return sortAscending.value ? 1 : -1
    return 0
  })
})

// 计算属性：是否所有文件都被选中
const areAllFilesSelected = computed(() => {
  return cloudDiskStore.files.length > 0 && 
         cloudDiskStore.selectedFiles.length === cloudDiskStore.files.length
})

// 切换全选/取消全选
const toggleSelectAll = () => {
  if (areAllFilesSelected.value) {
    cloudDiskStore.clearSelection()
  } else {
    cloudDiskStore.toggleSelectAll()
  }
}
</script>

<style scoped>
.cloud-disk-page {
  height: 100%;
  overflow: hidden;
  position: relative;
}

.disk-container {
  display: flex;
  height: 100%;
  width: 100%;
}

.file-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
}

.file-header {
  padding: 20px 32px;
  background-color: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-tertiary);
}

.breadcrumb-item {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 0;
  transition: all 0.2s ease;
  font-weight: 400;
}

.breadcrumb-item:hover {
  color: var(--primary-color);
  background: none;
  transform: none;
}

.breadcrumb-item.current {
  color: var(--text-primary);
  font-weight: 500;
  cursor: default;
  background: none;
}

.separator {
  color: var(--text-tertiary);
  font-size: 12px;
  margin: 0 4px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.toolbar-left, .toolbar-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.toolbar .btn {
  padding: 8px 16px;
  font-size: 13px;
  height: 36px;
  border-radius: 6px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.toolbar .btn-secondary {
  background-color: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.toolbar .btn-secondary:hover {
  background-color: var(--bg-secondary);
  box-shadow: var(--shadow-md);
}

.toolbar .btn-action {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.toolbar .btn-action:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.toolbar .btn-link {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 13px;
}

.toolbar .btn-link:hover {
  color: var(--primary-color);
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  background-color: var(--bg-primary);
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
  padding: 40px;
  text-align: center;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
  opacity: 0.8;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.1));
}

.empty-state h3 {
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.loading {
  width: 40px;
  height: 40px;
  border: 3px solid var(--bg-tertiary);
  border-top: 3px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 表格容器 */
.file-table-container {
  width: 100%;
  margin: 0;
  background-color: var(--bg-primary);
  border-radius: 0;
  box-shadow: none;
  overflow: visible;
  border: none;
}

/* 表格样式 */
.file-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

/* 表头样式 */
.file-table thead {
  background-color: transparent;
  position: sticky;
  top: 0;
  z-index: 10;
}

.file-table th {
  padding: 12px 24px;
  text-align: left;
  font-weight: 400;
  color: var(--text-tertiary);
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s ease;
  text-transform: none;
  font-size: 13px;
  letter-spacing: normal;
}

.file-table th:hover {
  background-color: var(--bg-secondary);
}

.select-column {
  width: 48px;
  padding-left: 24px !important;
}

.name-column {
  padding-left: 0 !important;
}

.date-column {
  text-align: right !important;
  padding-right: 32px !important;
  width: 200px;
}

.type-column {
  min-width: 120px;
  width: 15%;
}

.size-column {
  min-width: 100px;
  width: 10%;
  text-align: right;
}

.actions-column {
  min-width: 140px;
  width: 140px;
  text-align: center;
}

.actions-column th {
  cursor: default !important;
}

.actions-column th:hover {
  background-color: transparent !important;
}

/* 文件操作按钮 */
.file-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.action-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.7;
}

.action-btn:hover {
  background-color: var(--bg-secondary);
  opacity: 1;
  transform: scale(1.1);
}

.action-btn.delete-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

/* 表头内容 */
.column-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 排序指示器 */
.sort-indicator {
  font-size: 12px;
  color: var(--primary-color);
}

/* 表格行样式 */
.file-row {
  transition: background-color 0.2s ease;
  cursor: pointer;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.file-row:hover {
  background-color: var(--bg-secondary);
}

.file-row.selected {
  background-color: rgba(59, 130, 246, 0.05);
}

/* 表格单元格样式 */
.file-table td {
  padding: 12px 20px;
  vertical-align: middle;
  color: var(--text-primary);
}

/* 文件单元格内容 */
.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.file-icon-wrapper {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(59, 130, 246, 0.1);
  border-radius: 6px;
  color: #3b82f6;
  font-size: 16px;
}

/* 文件名样式 */
.file-name {
  color: var(--text-primary);
  font-weight: 400;
  font-size: 14px;
}

.file-row:hover .file-name {
  color: var(--primary-color);
}

/* 文件图标 */
.file-icon {
  font-size: 20px;
  width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 操作按钮容器 */
.file-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 操作按钮样式 */
.action-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s ease;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background-color: var(--bg-secondary);
  color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.action-btn.delete:hover {
  color: var(--danger-color);
  background-color: rgba(239, 68, 68, 0.1);
}

/* 复选框样式 */
input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 响应式表格 */
@media (max-width: 768px) {
  .file-list {
    padding: 0 16px;
  }
  
  .file-table th,
  .file-table td {
    padding: 12px 16px;
    font-size: 14px;
  }
  
  .name-column {
    min-width: 150px;
    width: 35%;
  }
  
  .date-column {
    min-width: 120px;
    width: 25%;
  }
  
  .type-column {
    display: none;
  }
  
  .size-column {
    min-width: 80px;
    width: 15%;
  }
  
  .actions-column {
    min-width: 100px;
    width: 25%;
  }
  
  .file-actions {
    gap: 8px;
  }
  
  .action-btn {
    font-size: 16px;
    padding: 6px;
  }
}

/* Modal样式 */
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
  padding: 0;
  min-width: 450px;
  max-width: 90vw;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-color);
  animation: modal-in 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  max-height: 90vh;
  overflow: hidden;
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

.modal-content.large {
  min-width: 80vw;
  max-width: 95vw;
  width: 80vw;
  max-height: 90vh;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.edit-btn {
  background: var(--gradient-primary);
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.close-btn {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: rotate(90deg);
}

.modal-body {
  padding: 24px 32px 32px 32px;
  max-height: 80vh;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.modal-content:not(.large) .modal-body {
  padding: 24px;
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
}

.modal-actions .btn-primary {
  background: var(--gradient-primary);
  color: white;
  border: none;
}

.modal-actions .btn-secondary {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.modal-actions .btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.upload-progress {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background-color: var(--bg-secondary);
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: var(--shadow-lg);
  min-width: 300px;
  z-index: 999;
  border: 1px solid var(--border-color);
}

.progress-bar {
  height: 8px;
  background-color: var(--bg-tertiary);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  transition: width 0.3s ease;
}

@media (max-width: 768px) {
  .file-list {
    padding: 0 16px;
  }
  
  .file-header {
    padding: 16px;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .toolbar-left, .toolbar-right {
    width: 100%;
    justify-content: space-between;
  }

  .toolbar .btn {
    width: 100%;
    justify-content: center;
  }
  
  /* Table adaptation */
  .file-table-container {
    overflow-x: auto;
  }
  
  .file-table {
    min-width: 600px;
  }
  
  .file-table th,
  .file-table td {
    padding: 12px 16px;
    font-size: 14px;
  }
  
  .type-column {
    display: none;
  }
  
  .actions-column {
    min-width: 120px;
  }
  
  .action-btn {
    font-size: 16px;
    padding: 4px 6px;
  }
  
  /* Modal adaptation */
  .modal-content {
    min-width: unset;
    width: 95vw;
    padding: 24px 16px;
  }
  
  .modal-content.large {
    min-width: unset;
    width: 95vw;
    height: 90vh;
    display: flex;
    flex-direction: column;
  }
}

/* 预览容器样式 */
.preview-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.loading-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--text-secondary);
}

/* 预览内容样式 */
.preview-content {
  max-width: 100%;
  max-height: 75vh;
  object-fit: contain;
}

.preview-image {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.preview-video,
.preview-audio {
  width: 100%;
}

.preview-text {
  width: 100%;
  height: 70vh;
  overflow: auto;
  background-color: var(--bg-primary);
  border-radius: 8px;
  padding: 24px;
}

.preview-text pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.preview-text code {
  font-family: 'Fira Code', 'Cascadia Code', 'Source Code Pro', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
}

/* 文本编辑器 */
.text-editor {
  width: 100%;
  height: 70vh;
  padding: 24px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  font-family: 'Fira Code', 'Cascadia Code', 'Source Code Pro', monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  outline: none;
}

.text-editor:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.text-editor:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 模态框底部按钮 */
.modal-footer {
  padding: 16px 32px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background-color: var(--bg-secondary);
}

.modal-footer .btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.2s;
  cursor: pointer;
}

.modal-footer .btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-footer .btn-primary {
  background: var(--gradient-primary);
  color: white;
  border: none;
}

.modal-footer .btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.modal-footer .btn-secondary {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.modal-footer .btn-secondary:hover:not(:disabled) {
  background-color: var(--bg-primary);
}

/* PDF 和 iframe 预览 */
.preview-frame {
  width: 100%;
  height: 75vh;
  border: none;
  border-radius: 8px;
}

/* 不可预览文件样式 */
.not-previewable {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px 20px;
  gap: 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(147, 51, 234, 0.05) 100%);
  border-radius: 12px;
}

.not-previewable .file-icon-large {
  font-size: 80px;
  opacity: 0.9;
  animation: float 3s ease-in-out infinite;
}

.not-previewable .file-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  text-align: center;
  max-width: 80%;
  word-break: break-word;
}

.not-previewable .file-info {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  max-width: 600px;
  line-height: 1.6;
  margin: 0;
}

.not-previewable .btn {
  margin-top: 12px;
  min-width: 160px;
  gap: 8px;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

@media (max-width: 480px) {
  .breadcrumb {
    font-size: 12px;
  }
  
  .breadcrumb-item {
    padding: 4px;
  }
  
  .preview-frame {
    height: 400px;
  }
  
  .not-previewable .file-icon-large {
    font-size: 60px;
  }
  
  .not-previewable .file-name {
    font-size: 16px;
  }
}
/* 上传进度对话框 */
.upload-progress-overlay {
  z-index: 10000 !important;
}

.progress-modal {
  min-width: 400px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-info .filename {
  font-size: 14px;
  color: var(--text-primary);
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-info .percentage {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.progress-bar-container {
  width: 100%;
  height: 10px;
  background-color: var(--bg-tertiary);
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 16px;
}

.progress-bar {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: 5px;
  transition: width 0.3s ease;
}

.upload-tip {
  font-size: 12px;
  color: var(--text-tertiary);
  text-align: center;
  margin: 0;
}
</style>
