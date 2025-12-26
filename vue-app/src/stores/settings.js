import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import { useThemeStore } from './theme'

export const useSettingsStore = defineStore('settings', () => {
  // 状态 - 与后端UserSettings模型匹配
  const settings = ref({
    aiModel: 'deepseek',
    theme: 'light',
    language: 'zh-CN',
    notificationsEnabled: true,
    emailNotifications: false
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
        settings.value = {
          aiModel: response.aiModel || 'deepseek',
          theme: response.theme || 'light',
          language: response.language || 'zh-CN',
          notificationsEnabled: response.notificationsEnabled !== undefined ? response.notificationsEnabled : true,
          emailNotifications: response.emailNotifications !== undefined ? response.emailNotifications : false
        }
        
        // 【重构】仅在初始化加载时同步一次主题，之后主题状态由 themeStore 独立控制
        const themeStore = useThemeStore()
        const isDark = settings.value.theme === 'dark'
        if (themeStore.isDarkMode !== isDark) {
          themeStore.setDarkMode(isDark)
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
      
      const response = await request.post(API_ENDPOINTS.settings.update, updatedSettings)
      
      if (response) {
        settings.value = updatedSettings
        
        // 【重构】如果通过设置页面显式修改了主题，则同步
        if (newSettings.theme) {
          const themeStore = useThemeStore()
          themeStore.setDarkMode(newSettings.theme === 'dark')
        }
        
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
  
  // 重置设置 (如果有后端API支持)
  async function resetSettings() {
    isLoading.value = true
    error.value = null
    
    try {
      const response = await request.delete(API_ENDPOINTS.settings.delete)
      
      if (response) {
        // 重置为默认设置
        settings.value = {
          aiModel: 'deepseek',
          theme: 'light',
          language: 'zh-CN',
          notificationsEnabled: true,
          emailNotifications: false
        }
        
        // 重置主题
        const themeStore = useThemeStore()
        themeStore.setDarkMode(false)
        
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
  
  return { 
    settings, 
    isLoading, 
    error, 
    fetchSettings, 
    updateSettings,
    resetSettings
  }
})
