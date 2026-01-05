<template>
  <div class="public-files-page">
    <div class="files-container">
      <main class="files-main">
        <div class="files-header">
          <div class="header-title">
            <h2>公共资源下载</h2>
            <p class="subtitle">这里提供常用的公共文件供大家下载使用</p>
          </div>
          
          <div class="toolbar">
            <input
              ref="fileInput"
              type="file"
              style="display: none"
              @change="handleFileSelect"
            >
            
            <div class="toolbar-right">
              <button
                v-if="authStore.isAdmin"
                class="btn btn-primary"
                :disabled="isUploading"
                @click="$refs.fileInput.click()"
              >
                <i class="fas fa-upload" />
                {{ isUploading ? '上传中...' : '上传文件 (管理员)' }}
              </button>
            </div>
          </div>
        </div>
        
        <div class="file-list-content">
          <div
            v-if="publicFilesStore.isLoading"
            class="loading-state"
          >
            <div class="loading-spinner" />
            <p>加载中...</p>
          </div>
          
          <div
            v-else-if="publicFilesStore.files.length === 0"
            class="empty-state"
          >
            <div class="empty-icon">📭</div>
            <h3>暂无公共文件</h3>
            <p v-if="authStore.isAdmin">作为管理员，您可以上传第一个文件</p>
          </div>
          
          <div
            v-else
            class="file-table-container"
          >
            <table class="file-table">
              <thead>
                <tr>
                  <th class="name-column">文件名</th>
                  <th class="size-column">大小</th>
                  <th class="date-column">上传时间</th>
                  <th class="action-column">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="file in publicFilesStore.files"
                  :key="file.name"
                  class="file-row"
                >
                  <td class="name-column">
                    <div class="file-cell">
                      <span class="file-icon">
                        <i class="fas fa-file-alt" />
                      </span>
                      <span class="file-name" :title="file.name">{{ file.name }}</span>
                    </div>
                  </td>
                  <td class="size-column">{{ formatFileSize(file.size) }}</td>
                  <td class="date-column">{{ formatDate(file.lastModified) }}</td>
                  <td class="action-column">
                    <button
                      class="btn-icon"
                      title="下载"
                      @click="handleDownload(file.name)"
                    >
                      <i class="fas fa-download" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePublicFilesStore } from '@/stores/publicFiles'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'

const publicFilesStore = usePublicFilesStore()
const authStore = useAuthStore()
const uiStore = useUIStore()
const fileInput = ref(null)
const isUploading = ref(false)

onMounted(() => {
  publicFilesStore.fetchFiles()
})

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (timestamp) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString()
}

const handleFileSelect = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  isUploading.value = true
  try {
    const result = await publicFilesStore.uploadFile(file)
    if (result.success) {
      uiStore.showToast('文件上传成功')
    } else {
      uiStore.showToast(result.message || '上传失败')
    }
  } finally {
    isUploading.value = false
    // Clear input
    event.target.value = ''
  }
}

const handleDownload = (filename) => {
  publicFilesStore.downloadFile(filename)
}
</script>

<style scoped>
.public-files-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-secondary);
  overflow: hidden;
}

.files-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  overflow: hidden;
}

.files-main {
  flex: 1;
  background-color: var(--bg-primary);
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.files-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title h2 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--text-primary);
}

.subtitle {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.file-list-content {
  flex: 1;
  overflow: auto;
  position: relative;
}

.file-table-container {
  min-width: 100%;
}

.file-table {
  width: 100%;
  border-collapse: collapse;
}

.file-table th,
.file-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.file-table th {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 0.875rem;
  position: sticky;
  top: 0;
  z-index: 1;
}

.file-row:hover {
  background-color: var(--bg-secondary);
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-icon {
  color: var(--accent-color);
  font-size: 1.2rem;
}

.file-name {
  color: var(--text-primary);
  font-weight: 500;
}

.name-column { width: 50%; }
.size-column { width: 15%; }
.date-column { width: 25%; }
.action-column { width: 10%; text-align: center; }

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background-color: var(--accent-color);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  transition: color 0.2s;
}

.btn-icon:hover {
  color: var(--accent-color);
  background-color: rgba(59, 130, 246, 0.1);
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--text-secondary);
  height: 100%;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid var(--bg-secondary);
  border-top-color: var(--accent-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
