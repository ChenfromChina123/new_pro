<template>
  <div class="admin-page">
    <div class="container">
      <div class="page-header">
        <h1>⚙️ 管理后台</h1>
        <p>系统管理和数据统计</p>
      </div>
      
      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card card">
          <div class="stat-icon">
            👥
          </div>
          <div class="stat-info">
            <h3>{{ statistics.totalUsers || 0 }}</h3>
            <p>总用户数</p>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">
            💬
          </div>
          <div class="stat-info">
            <h3>{{ statistics.totalChats || 0 }}</h3>
            <p>对话次数</p>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">
            📁
          </div>
          <div class="stat-info">
            <h3>{{ statistics.totalFiles || 0 }}</h3>
            <p>文件总数</p>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">
            💾
          </div>
          <div class="stat-info">
            <h3>{{ formatSize(statistics.totalStorage || 0) }}</h3>
            <p>存储空间</p>
          </div>
        </div>
      </div>
      
      <!-- 功能选项卡 -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>
      
      <!-- 用户管理 -->
      <div
        v-if="currentTab === 'users'"
        class="tab-content card"
      >
        <div class="content-header">
          <h2>用户管理</h2>
          <div class="search-box">
            <input 
              v-model="userSearchQuery" 
              type="text" 
              placeholder="搜索用户邮箱..."
              class="search-input"
            >
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>邮箱</th>
                <th>注册时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="user in filteredUsers"
                :key="user.id"
              >
                <td>{{ user.id }}</td>
                <td>{{ user.email }}</td>
                <td>{{ formatDate(user.created_at || user.createdAt) }}</td>
                <td>
                  <span :class="['badge', user.active ? 'success' : 'danger']">
                    {{ user.active ? '正常' : '禁用' }}
                  </span>
                </td>
                <td>
                  <button 
                    class="btn-small btn-secondary"
                    style="margin-right: 8px;"
                    @click="viewUserFiles(user.email)"
                  >
                    查看文件
                  </button>
                  <button class="btn-small btn-secondary">
                    详情
                  </button>
                </td>
              </tr>
              <tr v-if="filteredUsers.length === 0">
                <td colspan="5" class="empty-row">未找到匹配的用户</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 文件管理 -->
      <div
        v-if="currentTab === 'files'"
        class="tab-content card"
      >
        <div class="content-header">
          <h2>文件管理</h2>
          <div class="filter-group">
            <div class="user-select-wrapper">
              <select v-model="selectedUserEmail" class="user-select">
                <option value="">所有用户</option>
                <option v-for="user in users" :key="user.id" :value="user.email">
                  {{ user.email }}
                </option>
              </select>
            </div>
            <div class="search-box">
              <input 
                v-model="fileSearchQuery" 
                type="text" 
                placeholder="搜索文件名..."
                class="search-input"
              >
            </div>
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>文件名</th>
                <th>用户</th>
                <th>大小</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="file in filteredFiles"
                :key="file.id"
              >
                <td>{{ file.filename }}</td>
                <td>{{ file.user_email || file.userEmail }}</td>
                <td>{{ formatSize(file.file_size || file.fileSize) }}</td>
                <td>{{ formatDate(file.upload_time || file.uploadTime) }}</td>
                <td>
                  <button 
                    class="btn-small btn-secondary"
                    style="margin-right: 8px;"
                    @click="handleEditFile(file)"
                  >
                    编辑
                  </button>
                  <button 
                    class="btn-small btn-danger"
                    @click="handleDeleteFile(file.id)"
                  >
                    删除
                  </button>
                </td>
              </tr>
              <tr v-if="filteredFiles.length === 0">
                <td colspan="5" class="empty-row">未找到匹配的文件</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 文件编辑弹窗 -->
      <div 
        v-if="showEditModal" 
        class="modal-overlay"
      >
        <div class="modal-content edit-modal animate-slideIn">
          <div class="modal-header">
            <h3>编辑文件: {{ editingFile?.filename }}</h3>
            <button 
              class="close-btn" 
              @click="showEditModal = false"
            >
              &times;
            </button>
          </div>
          <div class="modal-body">
            <div 
              v-if="editContent === '获取内容失败'" 
              class="error-state"
            >
              <div class="error-icon">⚠️</div>
              <p>获取文件内容失败</p>
              <button 
                class="btn btn-secondary btn-small" 
                @click="handleEditFile(editingFile)"
              >
                重试
              </button>
            </div>
            <div 
              v-else
              class="editor-container"
              :class="{ loading: editContent === '加载中...' }"
            >
              <textarea 
                v-model="editContent" 
                class="file-editor"
                spellcheck="false"
                placeholder="文件内容加载中..."
                :disabled="editContent === '加载中...'"
              />
              <div v-if="editContent === '加载中...'" class="editor-loading-overlay">
                <div class="loading"></div>
                <span>内容加载中...</span>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button 
              class="btn btn-secondary" 
              @click="showEditModal = false"
            >
              取消
            </button>
            <button 
              class="btn btn-primary" 
              :disabled="saving || editContent === '加载中...' || editContent === '获取内容失败'"
              @click="saveFileContent"
            >
              <span v-if="saving" class="loading-spinner"></span>
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
      
      <!-- 反馈管理 -->
      <div
        v-if="currentTab === 'feedback'"
        class="tab-content card"
      >
        <h2>用户反馈</h2>
        <div class="feedback-list">
          <div
            v-for="feedback in feedbacks"
            :key="feedback.id"
            class="feedback-item"
          >
            <div class="feedback-header">
              <span class="feedback-user">{{ feedback.user_email || feedback.userEmail }}</span>
              <span class="feedback-date">{{ formatDate(feedback.created_at || feedback.createdAt) }}</span>
            </div>
            <p class="feedback-content">
              {{ feedback.content }}
            </p>
            <button class="btn-small btn-secondary">
              标记已处理
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import AppLayout from '@/components/AppLayout.vue'

const statistics = ref({})
const users = ref([])
const files = ref([])
const feedbacks = ref([])
const currentTab = ref('users')
const showEditModal = ref(false)
const editingFile = ref(null)
const editContent = ref('')
const saving = ref(false)
const fileSearchQuery = ref('')
const userSearchQuery = ref('')
const selectedUserEmail = ref('')

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'files', label: '文件管理' },
  { key: 'feedback', label: '反馈管理' }
]

// 过滤用户列表
const filteredUsers = computed(() => {
  if (!userSearchQuery.value) return users.value
  const query = userSearchQuery.value.toLowerCase()
  return users.value.filter(user => 
    user.email?.toLowerCase().includes(query)
  )
})

// 过滤文件列表
const filteredFiles = computed(() => {
  let result = files.value
  
  // 按选择的用户邮箱筛选
  if (selectedUserEmail.value) {
    result = result.filter(file => (file.user_email || file.userEmail) === selectedUserEmail.value)
  }
  
  // 按搜索关键词筛选
  if (fileSearchQuery.value) {
    const query = fileSearchQuery.value.toLowerCase()
    result = result.filter(file => 
      file.filename?.toLowerCase().includes(query) || 
      (file.user_email || file.userEmail)?.toLowerCase().includes(query)
    )
  }
  
  return result
})

// 监听标签页切换
watch(currentTab, (newTab) => {
  if (newTab === 'users') fetchUsers()
  if (newTab === 'files') fetchFiles()
  if (newTab === 'feedback') fetchFeedbacks()
})

onMounted(async () => {
  await fetchStatistics()
  await fetchUsers()
})

const fetchStatistics = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.statistics)
    statistics.value = response.data || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchUsers = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.users)
    users.value = response.data?.users || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchFiles = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.files)
    files.value = response.data || []
  } catch (error) {
    console.error('获取文件列表失败:', error)
  }
}

const fetchFeedbacks = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.feedback.admin.list)
    feedbacks.value = response.data || []
  } catch (error) {
    console.error('获取反馈列表失败:', error)
  }
}

const viewUserFiles = (email) => {
  selectedUserEmail.value = email
  currentTab.value = 'files'
}

const handleEditFile = async (file) => {
  editingFile.value = file
  showEditModal.value = true
  editContent.value = '加载中...'
  
  try {
    const response = await request.get(API_ENDPOINTS.admin.getFileContent(file.id))
    editContent.value = response.data || ''
  } catch (error) {
    console.error('获取文件内容失败:', error)
    editContent.value = '获取内容失败'
  }
}

const saveFileContent = async () => {
  if (!editingFile.value) return
  
  saving.value = true
  try {
    await request.put(API_ENDPOINTS.admin.updateFileContent(editingFile.value.id), {
      content: editContent.value
    })
    showEditModal.value = false
    alert('文件保存成功')
    await fetchFiles()
  } catch (error) {
    console.error('保存文件失败:', error)
    alert('保存失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

const handleDeleteFile = async (fileId) => {
  if (!confirm('确定要删除这个文件吗？')) return
  
  try {
    await request.delete(API_ENDPOINTS.cloudDisk.delete(fileId))
    await fetchFiles()
    await fetchStatistics()
  } catch (error) {
    console.error('删除文件失败:', error)
    alert('删除失败')
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped>
.admin-page {
  height: 100%;
  overflow-y: auto;
  padding: 32px 0;
  background-color: var(--bg-primary);
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .admin-page {
    padding: 16px;
  }

  .page-header {
    margin-bottom: 24px;
  }

  .page-header h1 {
    font-size: 24px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .stat-card {
    padding: 16px;
  }
  
  .stat-icon {
    width: 60px;
    height: 60px;
    font-size: 32px;
  }
  
  .stat-info h3 {
    font-size: 24px;
  }

  .tabs {
    overflow-x: auto;
    white-space: nowrap;
    padding-bottom: 4px; /* Hide scrollbar potentially or give space */
    -webkit-overflow-scrolling: touch;
  }

  .tab-btn {
    padding: 10px 16px;
    font-size: 14px;
    flex-shrink: 0;
  }
  
  .data-table th, 
  .data-table td {
    padding: 8px;
    font-size: 12px;
  }
  
  /* Hide less important columns on mobile if needed, or just rely on scroll */
  
  .modal-content {
    width: 95%;
    max-height: 85vh;
  }
  
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 16px;
  }
  
  .editor-container {
    height: 300px;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  font-size: 48px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
  border-radius: 12px;
}

.stat-info h3 {
  font-size: 32px;
  margin: 0 0 4px 0;
  color: var(--primary-color);
}

.stat-info p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--border-color);
}

.tab-btn {
  padding: 12px 24px;
  border: none;
  background: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  position: relative;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  color: var(--primary-color);
}

.tab-btn.active {
  color: var(--primary-color);
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
}

.tab-content h2 {
  font-size: 20px;
  margin-bottom: 0;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-select-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.user-select-wrapper::after {
  content: '▼';
  font-size: 10px;
  color: var(--text-secondary);
  position: absolute;
  right: 12px;
  pointer-events: none;
}

.user-select {
  padding: 8px 32px 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background-color: var(--input-bg);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  cursor: pointer;
  min-width: 200px;
  transition: all 0.3s ease;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
}

.user-select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

.user-select:hover {
  border-color: var(--gray-300);
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.search-box {
  width: 250px;
}

.search-input {
  width: 100%;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background-color: var(--input-bg);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

.empty-row {
  text-align: center;
  padding: 32px !important;
  color: var(--text-secondary);
  font-style: italic;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.data-table th,
.data-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.data-table th {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background-color: var(--bg-secondary);
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table tr:hover td {
  background-color: rgba(var(--primary-rgb), 0.02);
}

.data-table td {
  font-size: 14px;
  color: var(--text-primary);
  transition: background-color 0.2s ease;
}

.badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge.success {
  background-color: rgba(39, 174, 96, 0.1);
  color: var(--success-color);
}

.badge.danger {
  background-color: rgba(231, 76, 60, 0.1);
  color: var(--danger-color);
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-item {
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.feedback-user {
  font-weight: 500;
}

.feedback-date {
  color: var(--text-secondary);
}

.feedback-content {
  margin-bottom: 12px;
  line-height: 1.6;
}

/* 弹窗样式 */
.modal-overlay {
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
}

.modal-content {
  background-color: var(--card-bg);
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.modal-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-secondary);
  cursor: pointer;
}

.modal-body {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.editor-container {
  height: 500px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  background-color: var(--input-bg);
  transition: all 0.3s ease;
}

.editor-container.loading {
  opacity: 0.7;
}

.editor-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: rgba(var(--bg-secondary-rgb), 0.5);
  gap: 12px;
  z-index: 5;
}

.error-state {
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 16px;
  color: var(--text-secondary);
}

.error-icon {
  font-size: 48px;
}

.file-editor {
  width: 100%;
  height: 100%;
  padding: 16px;
  border: none;
  resize: none;
  background-color: transparent;
  color: var(--text-primary);
  font-family: 'Fira Code', 'Cascadia Code', 'Source Code Pro', monospace;
  font-size: 14px;
  line-height: 1.6;
  outline: none;
}

.file-editor:disabled {
  cursor: wait;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

