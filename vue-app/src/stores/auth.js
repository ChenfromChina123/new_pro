import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import { useChatStore } from './chat'
import rsaEncryption from '@/utils/rsaEncryption'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const isValidating = ref(false)
  const rsaInitialized = ref(false)
  
  // 计算属性
  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.is_admin === true)
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)
  const email = computed(() => userInfo.value?.email)
  
  /**
   * 初始化RSA加密
   */
  async function initializeRsa() {
    if (!rsaInitialized.value) {
      rsaInitialized.value = await rsaEncryption.initialize()
    }
    return rsaInitialized.value
  }
  
  /**
   * 检查JWT token是否过期
   * @param {string} t - JWT token
   * @returns {boolean} - 是否过期
   */
  function isTokenExpired(t) {
    if (!t) return true
    try {
      const p = t.split('.')[1]
      const b = p.replace(/-/g, '+').replace(/_/g, '/')
      const json = decodeURIComponent(atob(b).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''))
      const payload = JSON.parse(json)
      return !!(payload && payload.exp && payload.exp * 1000 <= Date.now())
    } catch {
      return true
    }
  }
  
  /**
   * 验证token有效性（向后端发送请求）
   * 用于实现持久化登录，在应用启动时调用
   */
  async function validateToken() {
    if (!token.value) {
      return false
    }
    
    // 先检查本地是否过期
    if (isTokenExpired(token.value)) {
      logout()
      return false
    }
    
    isValidating.value = true
    try {
      const response = await request.get('/api/auth/validate')
      if (response?.code === 200 || response?.data === true) {
        return true
      }
      // 如果验证失败，清除登录状态
      logout()
      return false
    } catch (error) {
      console.error('Token validation error:', error)
      // 如果是401错误，说明token无效，清除登录状态
      if (error.response?.status === 401) {
        logout()
      }
      return false
    } finally {
      isValidating.value = false
    }
  }
  
  // 登录
  async function login(email, password) {
    try {
      // 初始化RSA加密
      const rsaReady = await initializeRsa()
      
      let response
      if (rsaReady) {
        // 使用RSA加密密码
        const encryptedPassword = rsaEncryption.encrypt(password)
        if (!encryptedPassword) {
          throw new Error('密码加密失败')
        }
        
        // 发送加密登录请求
        response = await request.post('/api/auth/encrypted-login', {
          email,
          password: encryptedPassword,
          encrypted: true
        })
      } else {
        // 如果RSA初始化失败，使用普通登录（备选方案）
        console.warn('RSA加密未就绪，使用普通登录')
        response = await request.post(API_ENDPOINTS.auth.login, {
          email,
          password
        })
      }
      
      // 后端返回结构: { code, message, data: { access_token, user_id, ... } }
      // request.js 拦截器已返回 response.data，即整个 {code, message, data} 对象
      const payload = response?.data ?? response
      const accessToken = payload?.access_token ?? payload?.accessToken
      const userIdVal = payload?.user_id ?? payload?.userId ?? payload?.user?.id ?? null
      const emailVal = payload?.email ?? payload?.user?.email ?? null
      const usernameVal = payload?.username ?? payload?.user?.username ?? null
      const isAdminVal = payload?.is_admin ?? payload?.isAdmin ?? payload?.user?.is_admin ?? false
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
      const message = error.response?.data?.message || error.response?.data?.detail || '登录失败，请检查账号密码'
      return { 
        success: false, 
        message
      }
    }
  }
  
  // 注册
  async function register(email, password, verificationCode, username) {
    try {
      // 初始化RSA加密
      const rsaReady = await initializeRsa()
      
      let response
      if (rsaReady) {
        // 使用RSA加密密码
        const encryptedPassword = rsaEncryption.encrypt(password)
        if (!encryptedPassword) {
          throw new Error('密码加密失败')
        }
        
        // 发送加密注册请求
        response = await request.post('/api/auth/encrypted-register', {
          email,
          password: encryptedPassword,
          code: verificationCode,
          username,
          encrypted: true
        })
      } else {
        // 如果RSA初始化失败，使用普通注册（备选方案）
        console.warn('RSA加密未就绪，使用普通注册')
        response = await request.post(API_ENDPOINTS.auth.register, {
          email,
          password,
          code: verificationCode,
          username
        })
      }
      
      // 后端返回结构: { code, message, data: { access_token, user_id, ... } }
      const payload = response?.data ?? response
      const accessToken = payload?.access_token ?? payload?.accessToken
      const userIdVal = payload?.user_id ?? payload?.userId ?? payload?.user?.id ?? null
      const emailVal = payload?.email ?? payload?.user?.email ?? null
      const usernameVal = payload?.username ?? payload?.user?.username ?? null
      const isAdminVal = payload?.is_admin ?? payload?.isAdmin ?? payload?.user?.is_admin ?? false
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
      const message = error.response?.data?.message || error.response?.data?.detail || '注册失败'
      return { 
        success: false, 
        message
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
      const message = error.response?.data?.message || error.response?.data?.detail || '发送验证码失败'
      return { 
        success: false, 
        message
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
      const message = error.response?.data?.message || error.response?.data?.detail || '发送验证码失败'
      return { 
        success: false, 
        message
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
      return { success: true, message: response.message || '密码已重置' }
    } catch (error) {
      console.error('Reset password error:', error)
      const message = error.response?.data?.message || error.response?.data?.detail || '密码重置失败'
      return { 
        success: false, 
        message
      }
    }
  }

  // 更新用户资料
  async function updateProfile(data) {
    try {
      const response = await request.put('/api/users/profile', data)
      const updatedUser = response.data
      
      if (updatedUser) {
        updateUserInfo({
          username: updatedUser.username
        })
      }
      
      return { success: true, message: '个人资料更新成功' }
    } catch (error) {
      console.error('Update profile error:', error)
      const message = error.response?.data?.message || error.response?.data?.detail || '更新失败'
      return { 
        success: false, 
        message
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
    isValidating,
    rsaInitialized,
    isTokenExpired,
    validateToken,
    initializeRsa,
    login,
    register,
    sendVerificationCode,
    sendResetCode,
    resetPassword,
    updateProfile,
    logout,
    updateUserInfo,
    forceRefreshUserInfo
  }
})

