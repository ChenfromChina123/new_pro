<template>
  <div class="settings-container">
    <h2 class="settings-title">设置</h2>
    
    <div v-if="settingsStore.isLoading" class="loading-indicator">
      <div class="loading-spinner" />
      <p>加载中...</p>
    </div>
    
    <div v-else-if="settingsStore.error" class="error-message">
      <p>{{ settingsStore.error }}</p>
      <button class="retry-btn" @click="settingsStore.fetchSettings">重试</button>
    </div>
    
    <div v-else class="settings-content">
      <!-- 常规设置 -->
      <div class="settings-section">
        <h3 class="section-title">常规设置</h3>
        
        <div class="setting-item">
          <label class="setting-label">界面主题</label>
          <div class="setting-control">
            <select 
              v-model="localSettings.theme" 
              class="select-control"
              @change="handleUpdate('theme')"
            >
              <option value="light">浅色模式</option>
              <option value="dark">深色模式</option>
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
              <option value="zh-CN">简体中文</option>
              <option value="en-US">English</option>
            </select>
          </div>
        </div>
      </div>
      
      <!-- AI 模型设置 -->
      <div class="settings-section">
        <h3 class="section-title">AI 设置</h3>
        
        <div class="setting-item">
          <label class="setting-label">默认模型</label>
          <div class="setting-control">
            <select 
              v-model="localSettings.aiModel" 
              class="select-control"
              @change="handleUpdate('aiModel')"
            >
              <option value="deepseek">DeepSeek</option>
              <option value="doubao">豆包</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="settings-section">
        <h3 class="section-title">通知设置</h3>
        
        <div class="setting-item">
          <label class="setting-label">启用系统通知</label>
          <div class="setting-control">
             <label class="switch">
              <input 
                type="checkbox" 
                v-model="localSettings.notificationsEnabled"
                @change="handleUpdate('notificationsEnabled')"
              >
              <span class="slider round"></span>
            </label>
          </div>
        </div>
        
        <div class="setting-item">
          <label class="setting-label">启用邮件通知</label>
          <div class="setting-control">
            <label class="switch">
              <input 
                type="checkbox" 
                v-model="localSettings.emailNotifications"
                @change="handleUpdate('emailNotifications')"
              >
              <span class="slider round"></span>
            </label>
          </div>
        </div>
      </div>
      
      <!-- 危险区域 -->
      <div class="settings-section danger-zone">
        <h3 class="section-title danger-title">危险区域</h3>
        
        <div class="setting-item">
          <label class="setting-label">重置所有设置</label>
          <div class="setting-control">
            <button class="btn btn-danger" @click="handleReset">重置设置</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { useUIStore } from '@/stores/ui'

const settingsStore = useSettingsStore()
const uiStore = useUIStore()
const localSettings = ref({})

// Initialize local settings from store
watch(() => settingsStore.settings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

onMounted(async () => {
  await settingsStore.fetchSettings()
  localSettings.value = { ...settingsStore.settings }
})

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

.select-control, .input-control {
  width: 100%;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  transition: all 0.2s;
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
