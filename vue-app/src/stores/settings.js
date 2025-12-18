import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'

export const useSettingsStore = defineStore('settings', () => {
  // 状态 - 与后端UserSettings模型匹配
  const settings = ref({
    // 默认设置
    model_name: 'deepseek',
    api_base: '',
    api_key: '',
    model_params: {
      temperature: 0.7,
      max_tokens: 2000,
      top_p: 1.0
    }
  })
  
  const isLoading = ref(false)
  const error = ref(null)
  
  // 获取设置
  async function fetchSettings() {
    isLoading.value = true
    error.value = null
    
    try {
      const response = await request.get(API_ENDPOINTS.settings.get)
      
      // 处理API响应
      if (response) {
        // 转换model_params为JSON对象（如果是字符串）
        let modelParams = response.model_params || {}
        if (typeof modelParams === 'string') {
          try {
            modelParams = JSON.parse(modelParams)
          } catch (e) {
            console.error('Failed to parse model_params:', e)
            modelParams = {}
          }
        }
        
        settings.value = {
          model_name: response.model_name || 'deepseek',
          api_base: response.api_base || '',
          api_key: response.api_key || '', // API密钥会被后端掩码处理
          model_params: modelParams
        }
      }
      
      return { success: true, data: settings.value }
    } catch (err) {
      console.error('Failed to fetch settings:', err)
      error.value = err.response?.data?.message || '获取设置失败'
      return { success: false, message: error.value }
    } finally {
      isLoading.value = false
    }
  }
  
  // 更新设置
  async function updateSettings(newSettings) {
    isLoading.value = true
    error.value = null
    
    try {
      const updatedSettings = { ...settings.value, ...newSettings }
      
      // 直接使用后端字段名，无需转换
      const response = await request.post(API_ENDPOINTS.settings.update, updatedSettings)
      
      if (response) {
        settings.value = updatedSettings
        return { success: true, message: '设置更新成功' }
      }
      
      return { success: false, message: '更新设置失败' }
    } catch (err) {
      console.error('Failed to update settings:', err)
      error.value = err.response?.data?.message || '更新设置失败'
      return { success: false, message: error.value }
    } finally {
      isLoading.value = false
    }
  }
  
  // 重置设置
  async function resetSettings() {
    isLoading.value = true
    error.value = null
    
    try {
      const response = await request.delete(API_ENDPOINTS.settings.delete)
      
      if (response) {
        // 重置为默认设置
        settings.value = {
          model_name: 'deepseek',
          api_base: '',
          api_key: '',
          model_params: {
            temperature: 0.7,
            max_tokens: 2000,
            top_p: 1.0
          }
        }
        return { success: true, message: '设置已重置' }
      }
      
      return { success: false, message: '重置设置失败' }
    } catch (err) {
      console.error('Failed to reset settings:', err)
      error.value = err.response?.data?.message || '重置设置失败'
      return { success: false, message: error.value }
    } finally {
      isLoading.value = false
    }
  }
  
  // 更新AI模型
  async function updateAiModel(model_name) {
    return updateSettings({ model_name })
  }
  
  // 更新API配置
  async function updateApiConfig(api_base, api_key) {
    return updateSettings({ api_base, api_key })
  }
  
  // 更新模型参数
  async function updateModelParams(model_params) {
    return updateSettings({ model_params })
  }
  
  return {
    settings,
    isLoading,
    error,
    fetchSettings,
    updateSettings,
    resetSettings,
    updateAiModel,
    updateApiConfig,
    updateModelParams
  }
})