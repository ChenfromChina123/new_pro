/**
 * 聊天 Store - 管理聊天会话和消息
 */
import { createStore, getFromStorage, saveToStorage } from './store'
import request from './request'
import config from '../config/config'

const store = createStore('chat', () => ({
  sessions: [],
  currentSessionId: null,
  messages: [],
  suggestions: [],
  isLoading: false,
  selectedModel: getFromStorage('selectedModel', 'deepseek-chat'),
  abortController: null
}))

// 获取会话列表
store.fetchSessions = async function() {
  try {
    const response = await request.get(`${config.apiBase}/chat/sessions`)
    const backendSessions = response?.sessions || []
    
    this.state.sessions = backendSessions.map(session => {
      const createdAt = session.last_message_time 
        ? new Date(session.last_message_time.replace(' ', 'T')).toISOString()
        : new Date().toISOString()
      
      const originalTitle = session.title || session.last_message || '新对话'
      const truncatedTitle = originalTitle.length > 50 
        ? originalTitle.substring(0, 50) + '...' 
        : originalTitle
      
      return {
        id: session.session_id,
        title: truncatedTitle,
        createdAt: createdAt
      }
    })
    
    this.setState({ sessions: this.state.sessions })
    return { success: true }
  } catch (error) {
    console.error('Fetch sessions error:', error)
    return { success: false, message: '获取会话列表失败' }
  }
}

// 创建新会话
store.createSession = async function() {
  try {
    const response = await request.post(`${config.apiBase}/chat/sessions`)
    const newSession = {
      id: response.session_id,
      title: '新对话',
      createdAt: new Date().toISOString()
    }
    this.state.sessions.unshift(newSession)
    this.state.currentSessionId = newSession.id
    this.state.messages = []
    this.setState({ 
      sessions: this.state.sessions,
      currentSessionId: newSession.id,
      messages: []
    })
    return { success: true, sessionId: newSession.id }
  } catch (error) {
    console.error('Create session error:', error)
    return { success: false, message: '创建会话失败' }
  }
}

// 获取会话消息
store.fetchSessionMessages = async function(sessionId) {
  try {
    const response = await request.get(`${config.apiBase}/chat/sessions/${sessionId}/messages`)
    
    this.state.messages = (response.messages || []).map(msg => {
      const role = (msg.role === 'user' || msg.sender_type === 1) ? 'user' : 'assistant'
      return {
        ...msg,
        role,
        model: msg?.model ?? msg?.ai_model,
        timestamp: msg?.timestamp ?? msg?.send_time,
        reasoning_content: msg?.reasoning_content || '',
        isReasoningCollapsed: role === 'assistant'
      }
    })
    
    this.state.currentSessionId = sessionId
    
    // 解析建议问题
    if (response.suggestions) {
      try {
        this.state.suggestions = typeof response.suggestions === 'string' 
          ? JSON.parse(response.suggestions) 
          : response.suggestions
      } catch (e) {
        this.state.suggestions = []
      }
    } else {
      this.state.suggestions = []
    }
    
    this.setState({ 
      messages: this.state.messages,
      currentSessionId: sessionId,
      suggestions: this.state.suggestions
    })
    return { success: true }
  } catch (error) {
    console.error('Fetch messages error:', error)
    return { success: false, message: '获取消息失败' }
  }
}

// 发送消息
store.sendMessage = async function(content, onChunk) {
  this.state.isLoading = true
  this.state.suggestions = []
  this.setState({ isLoading: true, suggestions: [] })
  
  const authStore = require('./auth').default
  
  if (!authStore.isAuthenticated() && !this.state.currentSessionId) {
    // 游客模式允许没有会话 ID
  } else if (!this.state.currentSessionId) {
    this.state.isLoading = false
    this.setState({ isLoading: false })
    return { success: false, message: '缺少会话 ID' }
  }
  
  // 添加用户消息
  const userMessage = {
    role: 'user',
    content,
    timestamp: new Date().toISOString(),
    model: this.state.selectedModel
  }
  this.state.messages.push(userMessage)
  
  // 添加 AI 消息占位符
  const aiMessage = {
    role: 'assistant',
    content: '',
    reasoning_content: '',
    isStreaming: true,
    error: false,
    isReasoningCollapsed: false,
    timestamp: new Date().toISOString(),
    model: this.state.selectedModel
  }
  this.state.messages.push(aiMessage)
  this.setState({ messages: this.state.messages })
  
  // 准备请求头
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream'
  }
  
  if (authStore.isAuthenticated() && authStore.state.token) {
    headers['Authorization'] = `Bearer ${authStore.state.token}`
  }
  
  try {
    const response = await wx.request({
      url: `${config.apiBase}/chat/ask`,
      method: 'POST',
      header: headers,
      data: {
        prompt: content,
        session_id: this.state.currentSessionId,
        model: this.state.selectedModel
      },
      responseType: 'arraybuffer'
    })
    
    // 处理响应
    // 注意：小程序不支持 SSE 流，需要使用普通请求
    // 这里简化处理，使用非流式响应
    const aiMessage = this.state.messages[this.state.messages.length - 1]
    aiMessage.content = response?.data?.data?.answer || response?.data?.answer || ''
    aiMessage.isStreaming = false
    
    this.setState({ 
      messages: this.state.messages,
      isLoading: false
    })
    
    return { success: true }
  } catch (error) {
    console.error('Send message error:', error)
    const aiMessage = this.state.messages[this.state.messages.length - 1]
    aiMessage.isStreaming = false
    aiMessage.error = true
    aiMessage.content = '发送消息失败，请稍后重试。'
    
    this.setState({ 
      messages: this.state.messages,
      isLoading: false
    })
    
    return { success: false, message: '发送消息失败' }
  }
}

// 删除会话
store.deleteSession = async function(sessionId) {
  try {
    await request.delete(`${config.apiBase}/chat/sessions/${sessionId}`)
    this.state.sessions = this.state.sessions.filter(s => s.id !== sessionId)
    if (this.state.currentSessionId === sessionId) {
      this.state.currentSessionId = null
      this.state.messages = []
    }
    this.setState({ 
      sessions: this.state.sessions,
      currentSessionId: this.state.currentSessionId,
      messages: this.state.messages
    })
    return { success: true }
  } catch (error) {
    console.error('Delete session error:', error)
    return { success: false, message: '删除会话失败' }
  }
}

// 设置模型
store.setModel = function(model) {
  this.state.selectedModel = model
  saveToStorage('selectedModel', model)
  this.setState({ selectedModel: model })
}

export default store
