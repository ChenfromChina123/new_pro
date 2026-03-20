/**
 * 认证 Store - 管理用户认证状态
 */
import { createStore, getFromStorage, saveToStorage, removeFromStorage } from './store'
import request from './request'
import config from '../config/config'

const store = createStore('auth', () => ({
  token: getFromStorage('token', ''),
  userInfo: getFromStorage('userInfo', null),
  isLoading: false
}))

// 计算属性
store.isAuthenticated = function() {
  return !!this.state.token
}

store.isAdmin = function() {
  return this.state.userInfo?.is_admin === true
}

store.userId = function() {
  return this.state.userInfo?.id
}

store.username = function() {
  return this.state.userInfo?.username
}

store.email = function() {
  return this.state.userInfo?.email
}

// 登录
store.login = async function(email, password) {
  this.state.isLoading = true
  try {
    const response = await request.post(`${config.apiBase}/auth/login`, {
      email,
      password
    })
    
    const payload = response?.data ?? response
    const accessToken = payload?.access_token ?? payload?.accessToken
    
    if (accessToken) {
      this.state.token = accessToken
      this.state.userInfo = {
        id: payload?.user_id ?? payload?.userId ?? payload?.user?.id,
        email: payload?.email ?? payload?.user?.email,
        username: payload?.username ?? payload?.user?.username,
        is_admin: payload?.is_admin ?? payload?.isAdmin ?? payload?.user?.is_admin ?? false,
        avatar: payload?.avatar ?? payload?.user?.avatar
      }
      
      saveToStorage('token', accessToken)
      saveToStorage('userInfo', this.state.userInfo)
      
      this.setState({ 
        token: this.state.token,
        userInfo: this.state.userInfo 
      })
      
      return { success: true }
    }
    
    return { success: false, message: '登录失败' }
  } catch (error) {
    console.error('Login error:', error)
    const message = error?.message || '登录失败，请检查账号密码'
    return { success: false, message }
  } finally {
    this.state.isLoading = false
    this.setState({ isLoading: false })
  }
}

// 注册
store.register = async function(email, password, verificationCode, username) {
  this.state.isLoading = true
  try {
    const response = await request.post(`${config.apiBase}/auth/register`, {
      email,
      password,
      code: verificationCode,
      username
    })
    
    const payload = response?.data ?? response
    const accessToken = payload?.access_token ?? payload?.accessToken
    
    if (accessToken) {
      this.state.token = accessToken
      this.state.userInfo = {
        id: payload?.user_id ?? payload?.userId ?? payload?.user?.id,
        email: payload?.email ?? payload?.user?.email,
        username: payload?.username ?? payload?.user?.username,
        is_admin: payload?.is_admin ?? payload?.isAdmin ?? payload?.user?.is_admin ?? false,
        avatar: payload?.avatar ?? payload?.user?.avatar
      }
      
      saveToStorage('token', accessToken)
      saveToStorage('userInfo', this.state.userInfo)
      
      this.setState({ 
        token: this.state.token,
        userInfo: this.state.userInfo 
      })
      
      return { success: true, message: '注册成功' }
    }
    
    return { success: false, message: '注册失败' }
  } catch (error) {
    console.error('Register error:', error)
    const message = error?.message || '注册失败'
    return { success: false, message }
  } finally {
    this.state.isLoading = false
    this.setState({ isLoading: false })
  }
}

// 发送验证码
store.sendVerificationCode = async function(email) {
  try {
    const response = await request.post(`${config.apiBase}/auth/send-verification-code`, {
      email
    })
    return { success: true, message: response?.message || '验证码已发送' }
  } catch (error) {
    console.error('Send code error:', error)
    const message = error?.message || '发送验证码失败'
    return { success: false, message }
  }
}

// 退出登录
store.logout = function() {
  this.state.token = ''
  this.state.userInfo = null
  removeFromStorage('token')
  removeFromStorage('userInfo')
  this.setState({ 
    token: '',
    userInfo: null
  })
}

// 更新用户信息
store.updateUserInfo = function(info) {
  this.state.userInfo = { ...this.state.userInfo, ...info }
  saveToStorage('userInfo', this.state.userInfo)
  this.setState({ userInfo: this.state.userInfo })
}

export default store
