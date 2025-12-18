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
      <!-- AI 模型设置 -->
      <div class="settings-section">
        <h3 class="section-title">
          AI 模型设置
        </h3>
        
        <div class="setting-item">
          <label class="setting-label">模型名称</label>
          <div class="setting-control">
            <select 
              v-model="localSettings.model_name" 
              class="select-control"
              @change="handleUpdate('model_name')"
            >
              <option value="deepseek">
                DeepSeek
              </option>
              <option value="gpt3">
                GPT-3.5
              </option>
              <option value="gpt4">
                GPT-4
              </option>
              <option value="claude">
                Claude
              </option>
            </select>
          </div>
        </div>
      </div>
      
      <!-- API 配置 -->
      <div class="settings-section">
        <h3 class="section-title">
          API 配置
        </h3>
        
        <div class="setting-item">
          <label class="setting-label">API 基础地址</label>
          <div class="setting-control">
            <input 
              v-model="localSettings.api_base" 
              type="text" 
              class="input-control"
              placeholder="https://api.example.com/v1"
              @change="handleUpdate('api_base')"
            >
          </div>
        </div>
        
        <div class="setting-item">
          <label class="setting-label">API 密钥</label>
          <div class="setting-control">
            <input 
              v-model="localSettings.api_key" 
              type="password" 
              class="input-control"
              placeholder="sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
              @change="handleUpdate('api_key')"
            >
          </div>
        </div>
      </div>
      
      <!-- 模型参数 -->
      <div class="settings-section">
        <h3 class="section-title">
          模型参数
        </h3>
        
        <div class="setting-item">
          <label class="setting-label">温度 (Temperature)</label>
          <div class="setting-control">
            <input 
              v-model.number="localSettings.model_params.temperature" 
              type="range" 
              min="0"
              max="2" 
              step="0.1" 
              class="range-control"
              @change="handleUpdate('model_params')"
            >
            <span class="range-value">{{ localSettings.model_params.temperature }}</span>
          </div>
        </div>
        
        <div class="setting-item">
          <label class="setting-label">最大令牌数 (Max Tokens)</label>
          <div class="setting-control">
            <input 
              v-model.number="localSettings.model_params.max_tokens" 
              type="range" 
              min="500"
              max="4000" 
              step="100" 
              class="range-control"
              @change="handleUpdate('model_params')"
            >
            <span class="range-value">{{ localSettings.model_params.max_tokens }}</span>
          </div>
        </div>
        
        <div class="setting-item">
          <label class="setting-label">Top P</label>
          <div class="setting-control">
            <input 
              v-model.number="localSettings.model_params.top_p" 
              type="range" 
              min="0"
              max="1" 
              step="0.1" 
              class="range-control"
              @change="handleUpdate('model_params')"
            >
            <span class="range-value">{{ localSettings.model_params.top_p }}</span>
          </div>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="settings-actions">
        <button
          class="reset-btn"
          @click="resetSettings"
        >
          重置为默认设置
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settings'

// 初始化状态管理
const settingsStore = useSettingsStore()

// 本地设置副本，用于实时编辑 - 初始化为完整结构
const localSettings = ref({
  model_name: 'deepseek',
  api_base: '',
  api_key: '',
  model_params: {
    temperature: 0.7,
    max_tokens: 2000,
    top_p: 1.0
  }
})

// 处理设置更新
const handleUpdate = async (field) => {
  // 确保model_params存在
  if (field === 'model_params' && !localSettings.value.model_params) {
    localSettings.value.model_params = {
      temperature: 0.7,
      max_tokens: 2000,
      top_p: 1.0
    }
  }
  
  const updateData = { [field]: localSettings.value[field] }
  const result = await settingsStore.updateSettings(updateData)
  
  if (result.success) {
    console.log('设置更新成功')
  }
}

// 重置设置
const resetSettings = async () => {
  if (confirm('确定要重置所有设置为默认值吗？')) {
    const result = await settingsStore.resetSettings()
    if (result.success) {
      localSettings.value = { 
        ...settingsStore.settings,
        model_params: { ...(settingsStore.settings.model_params || {}) } // 安全深拷贝
      }
      alert('设置已重置')
    }
  }
}

// 初始化
onMounted(async () => {
  await settingsStore.fetchSettings()
  // 确保model_params存在且是对象
  const settingsData = settingsStore.settings || {}
  const modelParams = settingsData.model_params || {}
  
  localSettings.value = { 
    ...settingsData,
    model_params: { 
      temperature: modelParams.temperature !== undefined ? modelParams.temperature : 0.7,
      max_tokens: modelParams.max_tokens !== undefined ? modelParams.max_tokens : 2000,
      top_p: modelParams.top_p !== undefined ? modelParams.top_p : 1.0
    } // 安全合并model_params
  }
})
</script>

<style scoped>
.settings-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background-color: var(--bg-color, #ffffff);
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: background-color 0.3s ease;
}

.settings-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
  color: var(--text-color, #333333);
  text-align: center;
}

.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color, #e0e0e0);
  border-top-color: var(--primary-color, #409eff);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-message {
  padding: 20px;
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #f56c6c;
  text-align: center;
}

.retry-btn {
  margin-top: 12px;
  padding: 6px 16px;
  background-color: var(--primary-color, #409eff);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.retry-btn:hover {
  background-color: var(--primary-hover-color, #66b1ff);
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-section {
  background-color: var(--card-bg-color, #fafafa);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid var(--border-color, #e0e0e0);
  transition: all 0.3s ease;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-color, #333333);
  border-bottom: 1px solid var(--border-color, #e0e0e0);
  padding-bottom: 8px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
}

.setting-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.setting-label {
  font-size: 16px;
  color: var(--text-color, #333333);
  flex: 1;
}

.setting-control {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  justify-content: flex-end;
}

.select-control {
  padding: 8px 12px;
  border: 1px solid var(--border-color, #dcdfe6);
  border-radius: 4px;
  background-color: var(--bg-color, #ffffff);
  color: var(--text-color, #333333);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 150px;
}

.select-control:hover {
  border-color: var(--primary-color, #409eff);
}

.select-control:focus {
  outline: none;
  border-color: var(--primary-color, #409eff);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.input-control {
  padding: 8px 12px;
  border: 1px solid var(--border-color, #dcdfe6);
  border-radius: 4px;
  background-color: var(--bg-color, #ffffff);
  color: var(--text-color, #333333);
  font-size: 14px;
  width: 100%;
  max-width: 300px;
  transition: all 0.3s ease;
}

.input-control:hover {
  border-color: var(--primary-color, #409eff);
}

.input-control:focus {
  outline: none;
  border-color: var(--primary-color, #409eff);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.range-control {
  flex: 1;
  max-width: 200px;
  cursor: pointer;
}

.range-value {
  min-width: 50px;
  text-align: center;
  font-size: 14px;
  color: var(--text-color, #333333);
}

.toggle-control {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  justify-content: space-between;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--border-color, #dcdfe6);
  transition: .4s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .toggle-slider {
  background-color: var(--primary-color, #409eff);
}

input:focus + .toggle-slider {
  box-shadow: 0 0 1px var(--primary-color, #409eff);
}

input:checked + .toggle-slider:before {
  transform: translateX(26px);
}

.settings-actions {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.reset-btn {
  padding: 10px 20px;
  background-color: var(--warning-color, #e6a23c);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.reset-btn:hover {
  background-color: var(--warning-hover-color, #ebb563);
}

/* 深色主题适配 */
@media (prefers-color-scheme: dark) {
  .settings-container {
    background-color: #1e1e1e;
    color: #ffffff;
  }
  
  .settings-section {
    background-color: #2d2d2d;
    border-color: #444444;
  }
  
  .select-control,
  .input-control {
    background-color: #333333;
    border-color: #555555;
    color: #ffffff;
  }
}
</style>