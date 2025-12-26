import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import { useChatStore } from './chat'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  
  // 计算属性
  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.is_admin === true)
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)
  const email = computed(() => userInfo.value?.email)
  
  // 初始化用户信息
  function initUserInfo() {
    const storedToken = localStorage.getItem('token')
    const storedUserInfo = localStorage.getItem('userInfo')
    
    if (storedToken) {
      token.value = storedToken
    }
    
    if (storedUserInfo) {
      try {
        userInfo.value = JSON.parse(storedUserInfo)
      } catch (e) {
        console.error('Failed to parse userInfo:', e)
        localStorage.removeItem('userInfo')
      }
    }
  }
  
  // 登录
  async function login(email, password) {
    try {
      const response = await request.post(API_ENDPOINTS.auth.login, {
        email,
        password
      })
      
      const payload = response?.data || response
      const accessToken = payload?.accessToken || payload?.access_token
      const userIdVal = payload?.userId ?? payload?.user_id ?? payload?.user?.id ?? null
      const emailVal = payload?.email ?? payload?.user?.email ?? null
      const usernameVal = payload?.username ?? payload?.user?.username ?? null
      const isAdminVal = payload?.isAdmin ?? payload?.is_admin ?? payload?.user?.is_admin ?? false
      const avatarVal = payload?.avatar ?? payload?.user?.avatar ?? null

      if (accessToken) {
        token.value = accessToken
        userInfo.value = {
          id: userIdVal,
          email: emailVal,
          username: usernameVal,
          is_admin: isAdminVal || false,
          avatar: avatarVal || null
        }
        
        localStorage.setItem('token', token.value)
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        
        // 登录成功后刷新会话列表
        const chatStore = useChatStore()
        await chatStore.fetchSessions()
        
        // 【新增】登录成功后获取设置并同步一次主题
        const { useSettingsStore } = await import('./settings')
        const settingsStore = useSettingsStore()
        const settingsResult = await settingsStore.fetchSettings()
        if (settingsResult.success) {
          const { useThemeStore } = await import('./theme')
          const themeStore = useThemeStore()
          themeStore.setDarkMode(settingsStore.settings.theme === 'dark')
        }
        
        return { success: true }
      }
      return { success: false, message: '登录失败' }
    } catch (error) {
      console.error('Login error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '登录失败，请检查账号密码' 
      }
    }
  }
  
  // 注册
  async function register(email, password, verificationCode, username) {
    try {
      const response = await request.post(API_ENDPOINTS.auth.register, {
        email,
        password,
        code: verificationCode,
        username
      })
      
      const payload = response?.data || response
      const accessToken = payload?.accessToken || payload?.access_token
      const userIdVal = payload?.userId ?? payload?.user_id ?? payload?.user?.id ?? null
      const emailVal = payload?.email ?? payload?.user?.email ?? null
      const usernameVal = payload?.username ?? payload?.user?.username ?? null
      const isAdminVal = payload?.isAdmin ?? payload?.is_admin ?? payload?.user?.is_admin ?? false
      const avatarVal = payload?.avatar ?? payload?.user?.avatar ?? null

      if (accessToken) {
        token.value = accessToken
        userInfo.value = {
          id: userIdVal,
          email: emailVal,
          username: usernameVal,
          is_admin: isAdminVal || false,
          avatar: avatarVal || null
        }
        localStorage.setItem('token', token.value)
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        
        // 注册成功后刷新会话列表
        const chatStore = useChatStore()
        await chatStore.fetchSessions()
      }
      
      return { success: true, message: '注册成功' }
    } catch (error) {
      console.error('Register error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '注册失败' 
      }
    }
  }
  
  // 发送验证码
  async function sendVerificationCode(email) {
    try {
      const response = await request.post(API_ENDPOINTS.auth.sendVerificationCode, {
        email
      })
      return { success: true, message: response.message || '验证码已发送' }
    } catch (error) {
      console.error('Send code error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '发送验证码失败' 
      }
    }
  }
  
  // 发送密码重置验证码
  async function sendResetCode(email) {
    try {
      const response = await request.post(API_ENDPOINTS.auth.sendResetCode, {
        email
      })
      return { success: true, message: response.message || '验证码已发送至邮箱' }
    } catch (error) {
      console.error('Send reset code error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '发送验证码失败' 
      }
    }
  }
  
  // 重置密码
  async function resetPassword(email, code, newPassword) {
    try {
      const response = await request.post(API_ENDPOINTS.auth.forgotPassword, {
        email,
        code,
        newPassword
      })
      return { success: true, message: response.message || '密码重置成功' }
    } catch (error) {
      console.error('Reset password error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '密码重置失败' 
      }
    }
  }
  
  // 退出登录
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }
  
  // 更新用户信息
  function updateUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }
  
  // 强制刷新用户信息（用于测试头像功能）
  function forceRefreshUserInfo() {
    // 从localStorage重新加载用户信息
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedUserInfo) {
      try {
        userInfo.value = JSON.parse(storedUserInfo)
      } catch (e) {
        console.error('Failed to parse userInfo:', e)
      }
    }
  }
  
  return {
    token,
    userInfo,
    isAuthenticated,
    isAdmin,
    userId,
    username,
    email,
    login,
    register,
    sendVerificationCode,
    sendResetCode,
    resetPassword,
    logout,
    updateUserInfo,
    forceRefreshUserInfo
  }
})

