<template>
  <div class="settings-container">
    <h2 class="settings-title">
      设置
    </h2>
    
    <div
      v-if="settingsStore.isLoading"
      class="loading-indicator"
    >
      <div class="loading-spinner" />
      <p>加载中...</p>
    </div>
    
    <div
      v-else-if="settingsStore.error"
      class="error-message"
    >
      <p>{{ settingsStore.error }}</p>
      <button
        class="retry-btn"
        @click="settingsStore.fetchSettings"
      >
        重试
      </button>
    </div>
    
    <div
      v-else
      class="settings-content"
    >
      <!-- 个人资料设置 -->
      <div class="settings-section">
        <h3 class="section-title">
          个人资料
        </h3>
        
        <div class="setting-item avatar-upload-item">
          <label class="setting-label">用户头像</label>
          <div class="setting-control avatar-upload-control">
            <div class="avatar-preview-wrapper">
              <img 
                v-if="authStore.userInfo?.avatar" 
                :src="avatarPreviewUrl || authStore.userInfo.avatar" 
                class="avatar-preview"
                alt="头像预览"
              >
              <div 
                v-else 
                class="avatar-placeholder"
              >
                <i class="fas fa-user" />
              </div>
              
              <div 
                v-if="isUploading" 
                class="upload-loading"
              >
                <div class="loading-spinner-small" />
              </div>
            </div>
            
            <div class="avatar-actions">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                style="display: none"
                @change="onFileChange"
              >
              <button 
                class="btn btn-secondary btn-sm" 
                :disabled="isUploading"
                @click="$refs.fileInput.click()"
              >
                {{ isUploading ? '上传中...' : '更换头像' }}
              </button>
              <p class="upload-tip">
                支持 JPG、PNG 格式，大小不超过 2MB
              </p>
            </div>
          </div>
        </div>

        <div class="setting-item">
          <label class="setting-label">用户名</label>
          <div class="setting-control">
            <div
              v-if="isEditingUsername"
              class="edit-group"
            >
              <input
                v-model="newUsername"
                type="text"
                class="input-control sm"
                placeholder="输入新用户名"
                @keyup.enter="saveUsername"
              >
              <div class="edit-actions">
                <button
                  class="btn btn-primary btn-xs"
                  @click="saveUsername"
                >
                  保存
                </button>
                <button
                  class="btn btn-secondary btn-xs"
                  @click="cancelEditingUsername"
                >
                  取消
                </button>
              </div>
            </div>
            <div
              v-else
              class="display-group"
            >
              <span class="text-value">{{ authStore.userInfo?.username }}</span>
              <button
                class="btn-icon"
                title="修改用户名"
                @click="startEditingUsername"
              >
                <i class="fas fa-edit" />
              </button>
            </div>
          </div>
        </div>

        <div class="setting-item">
          <label class="setting-label">电子邮箱</label>
          <div class="setting-control">
            <span class="text-value">{{ authStore.email }}</span>
          </div>
        </div>
      </div>

      <!-- 常规设置 -->
      <div class="settings-section">
        <h3 class="section-title">
          常规设置
        </h3>
        
        <div class="setting-item">
          <label class="setting-label">界面主题</label>
          <div class="setting-control">
            <select 
              v-model="localSettings.theme" 
              class="select-control"
              @change="handleUpdate('theme')"
            >
              <option value="light">
                浅色模式
              </option>
              <option value="dark">
                深色模式
              </option>
            </select>
          </div>
        </div>
        
        <div class="setting-item">
          <label class="setting-label">语言</label>
          <div class="setting-control">
            <select 
              v-model="localSettings.language" 
              class="select-control"
              @change="handleUpdate('language')"
            >
              <option value="zh-CN">
                简体中文
              </option>
              <option value="en-US">
                English
              </option>
            </select>
          </div>
        </div>
      </div>
      
      <!-- AI 模型设置 -->
      <div class="settings-section">
        <h3 class="section-title">
          AI 设置
        </h3>
        
        <div class="setting-item">
          <label class="setting-label">默认模型</label>
          <div class="setting-control">
            <select 
              v-model="localSettings.aiModel" 
              class="select-control"
              @change="handleUpdate('aiModel')"
            >
              <option value="deepseek">
                DeepSeek
              </option>
              <option value="doubao">
                豆包
              </option>
            </select>
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="settings-section">
        <h3 class="section-title">
          通知设置
        </h3>
        
        <div class="setting-item">
          <label class="setting-label">启用系统通知</label>
          <div class="setting-control">
            <label class="switch">
              <input 
                v-model="localSettings.notificationsEnabled" 
                type="checkbox"
                @change="handleUpdate('notificationsEnabled')"
              >
              <span class="slider round" />
            </label>
          </div>
        </div>
        
        <div class="setting-item">
          <label class="setting-label">启用邮件通知</label>
          <div class="setting-control">
            <label class="switch">
              <input 
                v-model="localSettings.emailNotifications" 
                type="checkbox"
                @change="handleUpdate('emailNotifications')"
              >
              <span class="slider round" />
            </label>
          </div>
        </div>
      </div>
      
      <!-- 危险区域 -->
      <div class="settings-section danger-zone">
        <h3 class="section-title danger-title">
          危险区域
        </h3>
        
        <div class="setting-item">
          <label class="setting-label">重置所有设置</label>
          <div class="setting-control">
            <button
              class="btn btn-danger"
              @click="handleReset"
            >
              重置设置
            </button>
          </div>
        </div>
        
        <div class="setting-item">
          <label class="setting-label">退出登录</label>
          <div class="setting-control">
            <button
              class="btn btn-danger"
              @click="handleLogout"
            >
              退出登录
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AppSettings'
}
</script>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSettingsStore } from '@/stores/settings'
import { useUIStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'

const settingsStore = useSettingsStore()
const uiStore = useUIStore()
const authStore = useAuthStore()
const router = useRouter()
const localSettings = ref({})
const isUploading = ref(false)
const avatarPreviewUrl = ref(null)

const isEditingUsername = ref(false)
const newUsername = ref('')

/**
 * 开始编辑用户名
 */
const startEditingUsername = () => {
  newUsername.value = authStore.userInfo?.username || ''
  isEditingUsername.value = true
}

/**
 * 取消编辑用户名
 */
const cancelEditingUsername = () => {
  isEditingUsername.value = false
}

/**
 * 保存新用户名
 */
const saveUsername = async () => {
  if (!newUsername.value || newUsername.value.trim() === '') {
    uiStore.showToast('用户名不能为空', 'error')
    return
  }
  
  if (newUsername.value === authStore.userInfo?.username) {
    isEditingUsername.value = false
    return
  }

  try {
    const result = await authStore.updateProfile({ username: newUsername.value })
    if (result.success) {
      uiStore.showToast('用户名更新成功')
      isEditingUsername.value = false
    } else {
      uiStore.showToast(result.message || '更新失败', 'error')
    }
  } catch (error) {
    console.error('Update username error:', error)
    uiStore.showToast('更新过程中发生错误', 'error')
  }
}

// Initialize local settings from store
watch(() => settingsStore.settings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

onMounted(async () => {
  await settingsStore.fetchSettings()
  localSettings.value = { ...settingsStore.settings }
})

/**
 * 处理头像上传
 */
const onFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件大小 (2MB)
  if (file.size > 2 * 1024 * 1024) {
    uiStore.showToast('图片大小不能超过 2MB', 'error')
    return
  }

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    uiStore.showToast('只能上传图片文件', 'error')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  isUploading.value = true
  try {
    const response = await request.post(API_ENDPOINTS.auth.uploadAvatar, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.code === 200) {
      const avatarPath = response.data
      // 更新本地状态
      authStore.updateUserInfo({ avatar: avatarPath })
      uiStore.showToast('头像上传成功')
      
      // 触发 AppHeader/Sidebar 刷新
      authStore.forceRefreshUserInfo()
    } else {
      uiStore.showToast(response.message || '上传失败', 'error')
    }
  } catch (error) {
    console.error('Avatar upload error:', error)
    uiStore.showToast(error.response?.data?.message || '上传过程中发生错误', 'error')
  } finally {
    isUploading.value = false
    // 清除 input 值，允许重复上传同一张图
    event.target.value = ''
  }
}

const handleUpdate = async (field) => {
  const updatePayload = {}
  updatePayload[field] = localSettings.value[field]
  
  const result = await settingsStore.updateSettings(updatePayload)
  if (result.success) {
    uiStore.showToast('设置已保存')
  } else {
    uiStore.showToast(result.message || '保存失败')
  }
}

const handleReset = async () => {
  if (confirm('确定要重置所有设置吗？这将恢复默认设置。')) {
    const result = await settingsStore.resetSettings()
    if (result.success) {
      uiStore.showToast('设置已重置')
    } else {
      uiStore.showToast(result.message || '重置失败')
    }
  }
}

/**
 * 处理退出登录
 */
const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.settings-container {
  max-width: 800px;
  margin: 0 auto;
  background-color: var(--bg-secondary);
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.settings-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 16px;
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.settings-section {
  background-color: var(--bg-primary);
  border-radius: 8px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.setting-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.setting-label {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
}

.setting-control {
  min-width: 200px;
  display: flex;
  justify-content: flex-end;
}

.display-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.edit-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.edit-actions {
  display: flex;
  gap: 8px;
}

.btn-icon {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon:hover {
  color: var(--primary-color);
  background-color: var(--bg-secondary);
}

.btn-xs {
  padding: 4px 12px;
  font-size: 12px;
  height: auto;
}

.input-control.sm {
  padding: 6px 10px;
  font-size: 13px;
}

.select-control, .input-control {
  width: 100%;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
  transition: all 0.2s ease;
}

/* 头像上传样式 */
.avatar-upload-control {
  display: flex;
  align-items: center;
  gap: 20px;
  justify-content: flex-start;
}

.avatar-preview-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--border-color);
  background-color: var(--bg-secondary);
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--text-tertiary);
}

.upload-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner-small {
  width: 20px;
  height: 20px;
  border: 2px solid #ffffff;
  border-top: 2px solid transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-tip {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 0;
}

.text-value {
  color: var(--text-primary);
  font-size: 15px;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.select-control:focus, .input-control:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

/* Switch Styles */
.switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.switch input { 
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
}

input:checked + .slider {
  background-color: var(--primary-color, #3b82f6);
}

input:focus + .slider {
  box-shadow: 0 0 1px var(--primary-color, #3b82f6);
}

input:checked + .slider:before {
  transform: translateX(26px);
}

.slider.round {
  border-radius: 24px;
}

.slider.round:before {
  border-radius: 50%;
}

/* Loading & Error */
.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--text-tertiary);
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-message {
  padding: 20px;
  background-color: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
  border-radius: 8px;
  text-align: center;
}

.retry-btn {
  margin-top: 10px;
  padding: 6px 16px;
  background-color: var(--danger-color);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* Danger Zone */
.danger-zone {
  border-color: rgba(239, 68, 68, 0.3);
  background-color: rgba(239, 68, 68, 0.05);
}

.danger-title {
  color: var(--danger-color);
}

.btn-danger {
  padding: 8px 16px;
  background-color: var(--danger-color);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-danger:hover {
  background-color: #dc2626;
}

@media (max-width: 640px) {
  .settings-container {
    padding: 20px;
  }
  
  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .setting-control {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
