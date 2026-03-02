import axios from 'axios'
import { API_CONFIG } from '@/config/api'
import { useAuthStore } from '@/stores/auth'

// 创建axios实例
const request = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
  headers: API_CONFIG.headers
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    
    const t = authStore.token
    if (t) {
      try {
        const p = t.split('.')[1]
        const b = p.replace(/-/g, '+').replace(/_/g, '/');
        const json = decodeURIComponent(atob(b).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''))
        const payload = JSON.parse(json)
        
        if (payload && payload.exp && payload.exp * 1000 <= Date.now()) {
          authStore.logout()
          
          // 获取当前路径，如果是游客允许的页面，则不强制跳转
          const guestAllowedPaths = ['/chat', '/public-files']
          const isGuestAllowed = guestAllowedPaths.some(path => window.location.pathname.startsWith(path))
          
          if (!isGuestAllowed) {
            window.location.href = '/login'
            return Promise.reject(new Error('Token expired'))
          }
          // 如果是允许游客的页面，清除 Token 后继续以游客身份请求
          delete config.headers.Authorization
        }
      } catch (e) {
        console.error('Token parsing failed:', e)
        // 解析失败说明 Token 格式错误，安全起见清除它
        authStore.logout()
      }
    }
    
    // 添加token到请求头
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    const authStore = useAuthStore()
    
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，清除token
          authStore.logout()
          
          // 如果当前不是游客允许的页面，才跳转到登录页
          {
            const guestAllowedPaths = ['/chat', '/public-files']
            const isGuestAllowed = guestAllowedPaths.some(path => window.location.pathname.startsWith(path))
            if (!isGuestAllowed) {
              window.location.href = '/login'
            }
          }
          break;
        case 403:
          console.error('没有权限访问')
          break
        case 404:
          console.error('请求的资源不存在')
          break
        case 500:
          console.error('服务器错误')
          break
        default:
          console.error('请求失败:', error.response.data.message || error.message)
      }
    } else if (error.request) {
      console.error('网络错误，请检查网络连接')
    } else {
      console.error('请求配置错误:', error.message)
    }
    
    return Promise.reject(error)
  }
)

export default request

