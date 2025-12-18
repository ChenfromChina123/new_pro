<template>
  <AppLayout>
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
              <h3>{{ statistics.total_users || 0 }}</h3>
              <p>总用户数</p>
            </div>
          </div>
          
          <div class="stat-card card">
            <div class="stat-icon">
              💬
            </div>
            <div class="stat-info">
              <h3>{{ statistics.total_chats || 0 }}</h3>
              <p>对话次数</p>
            </div>
          </div>
          
          <div class="stat-card card">
            <div class="stat-icon">
              📁
            </div>
            <div class="stat-info">
              <h3>{{ statistics.total_files || 0 }}</h3>
              <p>文件总数</p>
            </div>
          </div>
          
          <div class="stat-card card">
            <div class="stat-icon">
              💾
            </div>
            <div class="stat-info">
              <h3>{{ formatSize(statistics.total_storage || 0) }}</h3>
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
          <h2>用户管理</h2>
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
                  v-for="user in users"
                  :key="user.id"
                >
                  <td>{{ user.id }}</td>
                  <td>{{ user.email }}</td>
                  <td>{{ formatDate(user.created_at) }}</td>
                  <td>
                    <span :class="['badge', user.is_active ? 'success' : 'danger']">
                      {{ user.is_active ? '正常' : '禁用' }}
                    </span>
                  </td>
                  <td>
                    <button class="btn-small btn-secondary">
                      详情
                    </button>
                  </td>
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
          <h2>文件管理</h2>
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
                  v-for="file in files"
                  :key="file.id"
                >
                  <td>{{ file.filename }}</td>
                  <td>{{ file.user_email }}</td>
                  <td>{{ formatSize(file.file_size) }}</td>
                  <td>{{ formatDate(file.upload_time) }}</td>
                  <td>
                    <button class="btn-small btn-danger">
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
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
                <span class="feedback-user">{{ feedback.user_email }}</span>
                <span class="feedback-date">{{ formatDate(feedback.created_at) }}</span>
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
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import AppLayout from '@/components/AppLayout.vue'

const statistics = ref({})
const users = ref([])
const files = ref([])
const feedbacks = ref([])
const currentTab = ref('users')

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'files', label: '文件管理' },
  { key: 'feedback', label: '反馈管理' }
]

onMounted(async () => {
  await fetchStatistics()
  await fetchUsers()
})

const fetchStatistics = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.statistics)
    statistics.value = response || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchUsers = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.users)
    users.value = response.users || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
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
  min-height: calc(100vh - 64px);
  padding: 32px 0;
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
  margin-bottom: 20px;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.data-table th {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 14px;
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
</style>

