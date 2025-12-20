<template>
  <AppLayout>
    <div class="cloud-disk-page">
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
          class="folder-sidebar"
          :class="{ 'sidebar-visible': isSidebarVisible }"
        >
          <div class="sidebar-header">
            <h3>📁 文件夹</h3>
            <button
              class="icon-btn"
              title="新建文件夹"
              @click="showCreateFolderDialog"
            >
              ➕
            </button>
            <button
              class="icon-btn close-btn"
              title="关闭"
              @click="toggleSidebar"
            >
              ✕
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
                📤 上传文件
              </button>
              <button
                class="btn btn-secondary"
                @click="$refs.folderInput.click()"
              >
                📁 上传文件夹
              </button>
              <button
                class="btn btn-secondary"
                @click="downloadCurrentFolder"
              >
                💾 下载文件夹
              </button>
              
              <template v-if="cloudDiskStore.selectedFiles.length > 0">
                <button
                  class="btn btn-secondary"
                  @click="downloadSelected"
                >
                  💾 下载 ({{ cloudDiskStore.selectedFiles.length }})
                </button>
                <button
                  class="btn btn-secondary"
                  @click="deleteSelected"
                >
                  🗑️ 删除 ({{ cloudDiskStore.selectedFiles.length }})
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
                          👁️
                        </button>
                        <button
                          class="action-btn"
                          title="下载"
                          @click="downloadFile(file.id)"
                        >
                          💾
                        </button>
                        <button
                          class="action-btn delete"
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
            ✕
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
  </AppLayout>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useCloudDiskStore } from '@/stores/cloudDisk'
import AppLayout from '@/components/AppLayout.vue'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import ConflictResolutionDialog from '@/components/ConflictResolutionDialog.vue'

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
const hoveredFolderId = ref(null)
const expandedFolders = ref(new Set()) // 用于跟踪哪些文件夹是展开的
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)

// 冲突处理状态
const conflictDialogVisible = ref(false)
const currentConflictFiles = ref([])
const pendingUploads = ref([])
const batchStrategy = ref(null)


// 排序相关
const sortField = ref('upload_time')
const sortAscending = ref(false)

const normalizeFolderPath = (folderPath) => {
  return (folderPath || '').replace(/^\//, '').replace(/\/+$/, '')
}

const findFolderByPath = (folderPath) => {
  const target = normalizeFolderPath(folderPath)
  const stack = Array.isArray(cloudDiskStore.folders) ? [...cloudDiskStore.folders] : []
  while (stack.length) {
    const node = stack.pop()
    if (!node) continue
    if ((node.folderPath || '') === target) return node
    const children = Array.isArray(node.children) ? node.children : []
    for (const child of children) {
      stack.push(child)
    }
  }
  return null
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
  // 例如：current='a/b', folder='a' -> startWith('a/') -> true
  // 例如：current='a', folder='a' -> startWith('a/') -> false
  return current.startsWith(folderPath + '/')
}

/**
 * 切换文件夹展开状态（通过替换 Set 触发视图更新）。
 */
const toggleFolderExpand = (folderId, event) => {
  // 阻止事件冒泡，避免触发文件夹选择
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
 * 判断文件夹是否展开：手动展开优先，其次自动展开当前路径链路上的父级。
 */
const isFolderExpanded = (folder) => {
  // 如果文件夹被手动展开，返回true
  if (expandedFolders.value.has(folder.id)) {
    return true
  }
  
  // 自动展开：当前路径链路上的父级
  if (isInActiveChain(folder)) {
    return true
  }
  
  return false
}

/**
 * 计算文件夹树最大深度（用于超过阈值时启用滚动与缩进调整）。
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
 * 根据深度与屏幕尺寸动态计算缩进像素，避免深层级挤压布局。
 */
const folderIndentPx = computed(() => {
  const depth = maxFolderDepth.value
  const isMobile = viewportWidth.value <= 768
  if (isMobile) return depth > 6 ? 10 : 12
  return depth > 8 ? 10 : depth > 5 ? 12 : 14
})

// 切换侧边栏显示
const toggleSidebar = () => {
  isSidebarVisible.value = !isSidebarVisible.value
}

// 触摸事件处理
const handleTouchStart = (e) => {
  touchStartX.value = e.touches[0].clientX
}

const handleTouchMove = (e) => {
  touchEndX.value = e.touches[0].clientX
}

const handleTouchEnd = () => {
  const diffX = touchEndX.value - touchStartX.value
  
  // 从左向右滑动，显示侧边栏
  if (diffX > 50 && !isSidebarVisible.value) {
    isSidebarVisible.value = true
  }
  // 从右向左滑动，隐藏侧边栏
  else if (diffX < -50 && isSidebarVisible.value) {
    isSidebarVisible.value = false
  }
  
  // 重置触摸坐标
  touchStartX.value = 0
  touchEndX.value = 0
}

/**
 * 监听窗口尺寸变化，确保深层级文件夹缩进在不同设备上自适应。
 */
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

/**
 * 选择文件夹并刷新文件列表。
 */
const selectFolder = async (folderPath, folderId, event) => {
  if (event && typeof event.stopPropagation === 'function') {
    event.stopPropagation()
  }
  if (folderId !== undefined && folderId !== null) {
    const next = new Set(expandedFolders.value)
    next.delete(folderId)
    expandedFolders.value = next
  }
  cloudDiskStore.setActiveFolder({ folderPath, folderId })
  // 查找对应folderPath的文件夹id
  // 注意：这里我们不再传递folderId，而是传递folderPath
  // 后端需要修改为接受folderPath参数，或者前端需要先根据folderPath查找folderId
  await cloudDiskStore.fetchFiles(folderPath)
  cloudDiskStore.clearSelection()
}

const goToRoot = async () => {
  cloudDiskStore.setActiveFolder({ folderPath: '', folderId: null })
  await cloudDiskStore.fetchFiles('')
  cloudDiskStore.clearSelection()
}

const handleFileSelect = async (event) => {
  const files = Array.from(event.target.files)
  if (!files || files.length === 0) return
  
  // 重置批处理策略和队列
  batchStrategy.value = null
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
        if (batchStrategy.value) {
            // 应用批处理策略
            await performUpload(file, batchStrategy.value)
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

const performUpload = async (file, strategy) => {
  uploadProgress.value = 0
  const folderPath = cloudDiskStore.currentFolder
  const result = await cloudDiskStore.uploadFile(
    file, 
    folderPath, 
    (progress) => { uploadProgress.value = progress },
    strategy
  )
  
  if (result.success) {
    console.log('文件上传成功:', file.name)
  } else {
    alert(`上传失败: ${result.message}`)
  }
}

const onConflictResolved = async ({ strategy, applyToAll }) => {
    conflictDialogVisible.value = false

    // 处理文件夹重命名冲突
    if (renamingFolder.value) {
        const action = strategy === 'OVERWRITE' ? 'override' : 'rename'
        const result = await cloudDiskStore.resolveRenameFolder(
            renamingFolder.value.id,
            action,
            renameFolderName.value
        )
        
        if (result.success) {
             closeRenameFolderDialog()
        } else {
             alert(result.message)
        }
        return
    }

    if (applyToAll) {
        batchStrategy.value = strategy
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
    
    // 如果是重命名文件夹取消，则清除重命名状态
    if (renamingFolder.value) {
        // 保持重命名对话框关闭（因为它在冲突对话框出现前已经关闭了，或者我们应该重新打开它？）
        // 这里简单地重置状态，用户需要重新发起重命名
        closeRenameFolderDialog()
        return
    }

    // 跳过当前文件
    pendingUploads.value.shift()
    processUploadQueue()
}

// 处理文件夹上传
const handleFolderSelect = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  
  uploadProgress.value = 0
  
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
    alert(`上传失败: ${result.message}`)
  }
  
  uploadProgress.value = 0
  event.target.value = '' // 重置input
}

const showCreateFolderDialog = () => {
  newFolderName.value = ''
  showCreateFolder.value = true
}

const createFolder = async () => {
  if (!newFolderName.value.trim()) {
    alert('请输入文件夹名称')
    return
  }
  
  const result = await cloudDiskStore.createFolder(
    newFolderName.value,
    cloudDiskStore.currentFolder
  )
  
  if (result.success) {
    const currentFolderNode = findFolderByPath(cloudDiskStore.currentFolder)
    if (currentFolderNode?.id) {
      const next = new Set(expandedFolders.value)
      next.add(currentFolderNode.id)
      expandedFolders.value = next
    }
    showCreateFolder.value = false
    newFolderName.value = ''
  } else {
    alert(`创建失败: ${result.message}`)
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
      alert(`删除失败: ${result.message}`)
    }
  }
}

const renameFolderAction = (folder) => {
  renamingFolder.value = folder
  renameFolderName.value = folder.folderName || folder.name || ''
  showRenameFolder.value = true
}

const closeRenameFolderDialog = () => {
  showRenameFolder.value = false
  renamingFolder.value = null
  renameFolderName.value = ''
}

const confirmRenameFolder = async () => {
  if (!renameFolderName.value.trim()) {
    alert('请输入文件夹名称')
    return
  }
  
  // 如果名称没变，直接关闭
  if (renamingFolder.value.folderName === renameFolderName.value) {
    closeRenameFolderDialog()
    return
  }
  
  const result = await cloudDiskStore.renameFolder(
    renamingFolder.value.id,
    renameFolderName.value
  )
  
  if (result.conflict) {
    // 关闭重命名输入框
    showRenameFolder.value = false
    
    // 显示冲突解决对话框
    currentConflictFiles.value = [{
      name: renameFolderName.value,
      size: 0,
      isFolder: true
    }]
    conflictDialogVisible.value = true
    // 注意：不要在这里清空 renamingFolder，因为 onConflictResolved 需要它
  } else if (result.success) {
    closeRenameFolderDialog()
  } else {
    alert(`重命名失败: ${result.message}`)
  }
}

const deleteFolderAction = async (folderId) => {
  if (confirm('确定要删除这个文件夹及其所有内容吗？此操作不可恢复！')) {
    const result = await cloudDiskStore.deleteFolder(folderId)
    if (!result.success) {
      alert(`删除失败: ${result.message}`)
    }
  }
}

const deleteSelected = async () => {
  if (confirm(`确定要删除选中的 ${cloudDiskStore.selectedFiles.length} 个文件吗？`)) {
    const result = await cloudDiskStore.deleteFiles([...cloudDiskStore.selectedFiles])
    if (result.success) {
      cloudDiskStore.clearSelection()
    } else {
      alert(`删除失败: ${result.successCount}/${result.totalCount} 个文件已删除`)
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
      alert(`下载失败: ${result.message}`)
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
  previewFileData.value = file
  previewText.value = ''
  
  if (isPreviewable(file.filename)) {
    const fileType = getFileType(file.filename)
    const mimeType = getMimeType(file.filename)
    
    if (fileType === 'text') {
      // 读取文本文件内容
      const response = await fetch(cloudDiskStore.getDownloadUrl(file.id))
      previewText.value = await response.text()
      previewUrl.value = 'text-preview'
    } else {
      // 其他类型文件使用URL预览
      const url = await cloudDiskStore.fetchPreviewUrl(file.id, mimeType)
      if (url) {
        previewUrl.value = url
      }
    }
  }
}

const closePreview = () => {
  previewFileData.value = null
  previewText.value = ''
  if (previewUrl.value && previewUrl.value !== 'text-preview') {
    window.URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
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
  height: 100vh;
  overflow: hidden;
  position: relative;
}

/* 移动端侧边栏切换按钮 */
.mobile-sidebar-toggle {
  display: none;
  position: fixed;
  top: 80px;
  right: 16px;
  z-index: 80;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: all 0.3s ease;
}

.mobile-sidebar-toggle:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-lg);
}

.disk-container {
  display: flex;
  height: 100%;
  width: 100%;
}

.folder-sidebar {
  width: 300px;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.3s ease;
  z-index: 70;
  height: 100%;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 20px;
  border-bottom: 1px solid var(--border-color);
  gap: 8px;
}

.sidebar-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  flex: 1;
}

.sidebar-header .close-btn {
  display: none;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border: none;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  border: 1px solid var(--border-color);
}

.icon-btn:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.folder-tree {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px;
  overscroll-behavior: contain;
}

.folder-tree.folder-tree-scroll {
  overflow: auto;
}

.folder-item {
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.folder-item:hover .folder-header {
  background-color: var(--bg-tertiary);
}

.folder-item.active .folder-header {
  background: var(--bg-tertiary);
  border: 1px solid var(--primary-color);
}

.folder-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.folder-icon {
  font-size: 18px;
}

.folder-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 500;
  display: block;
  color: var(--text-primary);
  letter-spacing: 0.2px;
}

/* 文件夹展开/折叠图标样式 */
.folder-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  cursor: pointer;
  font-size: 10px;
  font-weight: bold;
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

/* 子文件夹样式 */
.folder-children {
  margin-left: 20px;
  padding-left: 12px;
  border-left: 1px solid var(--border-color);
}

.folder-children .folder-item {
  margin-bottom: 2px;
}

.file-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
}

.file-header {
  padding: 24px 32px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  color: var(--text-secondary);
}

.breadcrumb-item {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.breadcrumb-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  transform: translateY(-1px);
}

.breadcrumb-item.current {
  color: var(--text-primary);
  font-weight: 600;
  cursor: default;
  background-color: var(--bg-tertiary);
}

.breadcrumb-item.current:hover {
  transform: none;
}

.separator {
  color: var(--text-tertiary);
  font-size: 14px;
  margin: 0 4px;
  opacity: 0.6;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar .btn {
  padding: 10px 20px;
  font-size: 14px;
  height: 42px;
  border-radius: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.toolbar .btn-primary {
  background: var(--gradient-primary);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.2);
}

.toolbar .btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(29, 78, 216, 0.3);
  filter: brightness(1.1);
}

.toolbar .btn-secondary {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.toolbar .btn-secondary:hover {
  background-color: var(--bg-secondary);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 32px 32px;
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
  max-width: 1200px;
  margin: 24px auto 0;
  background-color: var(--bg-secondary);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

/* 表格样式 */
.file-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 15px;
}

/* 表头样式 */
.file-table thead {
  background-color: var(--bg-tertiary);
  position: sticky;
  top: 0;
  z-index: 10;
}

.file-table th {
  padding: 16px 20px;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s ease;
  text-transform: uppercase;
  font-size: 12px;
  letter-spacing: 0.8px;
}

.file-table th:hover {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

/* 表头列 */
.select-all-column {
  width: 50px;
  text-align: center;
}

.name-column {
  min-width: 250px;
  width: 40%;
}

.date-column {
  min-width: 150px;
  width: 20%;
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
  min-width: 120px;
  width: 15%;
  text-align: center;
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
.file-table tbody tr {
  border-bottom: 1px solid var(--border-color);
  transition: all 0.2s ease;
}

.file-table tbody tr:last-child {
  border-bottom: none;
}

.file-table tbody tr:hover {
  background-color: var(--bg-tertiary);
}

.file-row.selected {
  background-color: rgba(29, 78, 216, 0.04);
}

/* 表格单元格样式 */
.file-table td {
  padding: 12px 20px;
  vertical-align: middle;
  color: var(--text-primary);
}

/* 选择列 */
.select-column {
  text-align: center;
}

/* 文件单元格内容 */
.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 文件名样式 */
.file-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  font-size: 15px;
  color: var(--text-primary);
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
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
}

.modal-content {
  background-color: var(--bg-secondary);
  border-radius: 20px;
  padding: 32px;
  min-width: 400px;
  max-width: 90vw;
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

.modal-content.large {
  min-width: 800px;
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
  padding: 32px;
  max-height: 70vh;
  overflow-y: auto;
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
  .mobile-sidebar-toggle {
    display: block;
  }
  
  .folder-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    transform: translateX(-100%);
    z-index: 100;
    box-shadow: var(--shadow-lg);
  }
  
  .folder-sidebar.sidebar-visible {
    transform: translateX(0);
  }
  
  .sidebar-header .close-btn {
    display: block;
  }
  
  .file-header {
    padding: 16px;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .breadcrumb {
    font-size: 12px;
  }
  
  .breadcrumb-item {
    padding: 4px;
  }
  
  .modal-content {
    min-width: unset;
    width: 95vw;
    padding: 24px 16px;
  }
  
  .modal-content.large {
    min-width: unset;
    width: 95vw;
  }
  
  .preview-frame {
    height: 400px;
  }
  
  .upload-progress {
    min-width: unset;
    width: 90vw;
    padding: 16px;
  }
}
</style>
