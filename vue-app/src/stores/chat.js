import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { API_ENDPOINTS } from '@/config/api'

export const useChatStore = defineStore('chat', () => {
  // 状态
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const suggestions = ref([]) // 新增：当前会话的建议问题
  const isLoading = ref(false)
  const abortController = ref(null)
  const selectedModel = ref(localStorage.getItem('selectedModel') || 'deepseek-chat')
  
  // 流式更新节流控制
  let updateThrottle = null
  let chunkCount = 0

  const normalizeStreamChunk = (chunk) => {
    if (chunk === null || chunk === undefined) return ''
    if (typeof chunk !== 'string') return ''
    if (chunk === 'null') return ''
    return chunk
  }
  
  // 计算属性
  const currentSession = computed(() => 
    sessions.value.find(s => s.id === currentSessionId.value)
  )
  
  // 获取会话列表
  async function fetchSessions() {
    try {
      const response = await request.get(API_ENDPOINTS.chat.getSessions)
      const backendSessions = response.sessions || []
      
      // 将后端返回的数据转换为前端期望的格式
      sessions.value = backendSessions.map(session => {
        // 将后端返回的session_id作为id
        // 将最后一条消息作为会话标题，限制长度为50个字符
        // 将最后一条消息时间转换为ISO格式
        let createdAt = new Date().toISOString()
        if (session.last_message_time) {
          // 处理可能的日期格式差异 (替换空格为T以符合ISO标准)
          createdAt = new Date(session.last_message_time.replace(' ', 'T')).toISOString()
        }
        
        // 限制标题长度为50个字符
        const originalTitle = session.last_message || '新对话'
        const truncatedTitle = originalTitle.length > 50 
          ? originalTitle.substring(0, 50) + '...' 
          : originalTitle
        
        return {
          id: session.session_id,
          title: truncatedTitle,
          createdAt: createdAt
        }
      })
      return { success: true }
    } catch (error) {
      console.error('Fetch sessions error:', error)
      return { success: false, message: '获取会话列表失败' }
    }
  }
  
  // 创建新会话
  async function createSession() {
    try {
      const response = await request.post(API_ENDPOINTS.chat.createSession)
      const newSession = {
        id: response.session_id,
        title: '新对话',
        createdAt: new Date().toISOString()
      }
      sessions.value.unshift(newSession)
      currentSessionId.value = newSession.id
      messages.value = []
      return { success: true, sessionId: newSession.id }
    } catch (error) {
      console.error('Create session error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '创建会话失败' 
      }
    }
  }
  
  // 发送消息（非流式）
  async function sendMessageNonStream(content) {
    isLoading.value = true
    
    // 添加用户消息
    const userMessage = {
      role: 'user',
      content,
      timestamp: new Date().toISOString(),
      model: selectedModel.value
    }
    messages.value.push(userMessage)
    
    try {
      const response = await request.post(API_ENDPOINTS.chat.ask, {
        prompt: content,
        session_id: currentSessionId.value,
        model: selectedModel.value
      })
      
      // 添加AI消息
      const aiMessage = {
        role: 'assistant',
        content: (response?.data?.answer || response?.answer || ''),
        timestamp: new Date().toISOString(),
        model: selectedModel.value
      }
      messages.value.push(aiMessage)
      
      // 保存消息记录
      await saveMessages(userMessage, aiMessage)
      
      return { success: true }
    } catch (error) {
      console.error('Send message error:', error)
      messages.value.pop() // 移除用户消息
      return { 
        success: false, 
        message: error.response?.data?.message || '发送消息失败' 
      }
    } finally {
      isLoading.value = false
    }
  }
  
  // 获取会话消息
  async function fetchSessionMessages(sessionId) {
    try {
      const response = await request.get(API_ENDPOINTS.chat.getSessionMessages(sessionId))
      // 确保消息角色正确映射
      messages.value = (response.messages || []).map(msg => {
        const role = (msg.role === 'user' || msg.sender_type === 1) ? 'user' : 'assistant'
        const reasoningContent = msg?.reasoning_content ?? ''
        return {
          ...msg,
        // 确保角色是前端期望的格式：user 或 assistant
        // 后端返回 sender_type: 1 (user), 2 (AI)
          role,
          model: msg?.model ?? msg?.model_name ?? msg?.modelName ?? null,
          reasoning_content: normalizeStreamChunk(reasoningContent),
          isReasoningCollapsed: role === 'assistant'
            ? (msg?.isReasoningCollapsed ?? true)
            : undefined
        }
      })
      currentSessionId.value = sessionId
      
      // 解析建议问题
      if (response.suggestions) {
        try {
          suggestions.value = typeof response.suggestions === 'string' 
            ? JSON.parse(response.suggestions) 
            : response.suggestions
        } catch (e) {
          console.warn('Failed to parse suggestions:', e)
          suggestions.value = []
        }
      } else {
        suggestions.value = []
      }
      
      return { success: true }
    } catch (error) {
      console.error('Fetch messages error:', error)
      return { success: false, message: '获取消息失败' }
    }
  }
  
  // 发送消息（流式）
  async function sendMessage(content, onChunk) {
    isLoading.value = true
    abortController.value = new AbortController()

    if (!currentSessionId.value) {
      isLoading.value = false
      return { success: false, message: '缺少会话ID' }
    }

    const truncateTitle = (text) => {
      const str = String(text || '')
      return str.length > 50 ? `${str.slice(0, 50)}...` : str
    }

    const nowIso = new Date().toISOString()
    const sessionIndex = sessions.value.findIndex(s => s.id === currentSessionId.value)
    if (sessionIndex === -1) {
      sessions.value.unshift({
        id: currentSessionId.value,
        title: truncateTitle(content) || '新对话',
        createdAt: nowIso
      })
    } else {
      const session = sessions.value[sessionIndex]
      if (!session.title || session.title === '新对话') {
        session.title = truncateTitle(content) || '新对话'
      }
      if (sessionIndex !== 0) {
        sessions.value.unshift(sessions.value.splice(sessionIndex, 1)[0])
      }
    }
    
    // 添加用户消息
    const userMessage = {
      role: 'user',
      content,
      timestamp: new Date().toISOString(),
      model: selectedModel.value
    }
    messages.value.push(userMessage)
    
    // 添加AI消息占位符
    const aiMessage = {
      role: 'assistant',
      content: '',
      reasoning_content: '', // 新增推理内容字段
      isReasoningCollapsed: false,
      timestamp: new Date().toISOString(),
      model: selectedModel.value
    }
    messages.value.push(aiMessage)
    // 获取响应式对象
    const activeAiMessage = messages.value[messages.value.length - 1]
    
    try {
      const authStore = useAuthStore()
      const response = await fetch(`${request.defaults.baseURL}${API_ENDPOINTS.chat.askStream}`, {
        method: 'POST',
        signal: abortController.value.signal,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
          'Authorization': `Bearer ${authStore.token}`
        },
        body: JSON.stringify({
          prompt: content,
          session_id: currentSessionId.value,
          model: selectedModel.value
        })
      })
      
      // 若鉴权失败或服务拒绝，降级为非流式接口
      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          const authStore = useAuthStore()
          authStore.logout()
          window.location.href = '/login'
          return { success: false, message: '未授权' }
        } else {
          throw new Error(`请求失败: ${response.status}`)
        }
      }
      
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        
        // 保留最后一行（可能不完整），放入缓冲区等待下一次拼接
        buffer = lines.pop() || ''
        
        for (const line of lines) {
          if (line.trim() === '') continue
          
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') {
              continue
            }
            try {
              const parsed = JSON.parse(data)
              
              // 处理会话更新（标题和建议问题）
              if (parsed.type === 'session_update') {
                if (parsed.suggestions) {
                  suggestions.value = parsed.suggestions
                }
                if (parsed.title) {
                  const session = sessions.value.find(s => s.id === currentSessionId.value)
                  if (session) {
                    session.title = parsed.title
                  } else {
                    sessions.value.unshift({
                      id: currentSessionId.value,
                      title: parsed.title,
                      createdAt: new Date().toISOString()
                    })
                  }
                }
                continue
              }
              
              // 处理推理内容
              const reasoningChunk = normalizeStreamChunk(parsed.reasoning_content)
              if (reasoningChunk) {
                activeAiMessage.reasoning_content = (activeAiMessage.reasoning_content || '') + reasoningChunk
              }
              
              // 处理回复内容
              const contentChunk = normalizeStreamChunk(parsed.content)
              if (contentChunk) {
                if (!activeAiMessage.content) {
                  activeAiMessage.isReasoningCollapsed = true
                }
                activeAiMessage.content += contentChunk
                chunkCount++
                
                // 节流机制：每100ms或每5个chunk更新一次UI，减少渲染压力
                if (!updateThrottle) {
                  updateThrottle = setTimeout(() => {
                    updateThrottle = null
                    chunkCount = 0
                  }, 100)
                }
                
                // 如果达到5个chunk，立即触发UI更新
                if (chunkCount >= 5) {
                  clearTimeout(updateThrottle)
                  updateThrottle = null
                  chunkCount = 0
                }
                
                if (onChunk) {
                  onChunk(contentChunk)
                }
              }
            } catch (e) {
              console.warn('Failed to parse SSE data:', data)
            }
          }
        }
      }
      
      // 处理流结束时剩余的 buffer
      if (buffer.trim() && buffer.startsWith('data:')) {
        const data = buffer.slice(5).trim()
        if (data !== '[DONE]') {
          try {
            const parsed = JSON.parse(data)
            
            // 处理会话更新（标题和建议问题）
            if (parsed.type === 'session_update') {
              if (parsed.suggestions) {
                suggestions.value = parsed.suggestions
              }
              if (parsed.title) {
                const session = sessions.value.find(s => s.id === currentSessionId.value)
                if (session) {
                  session.title = parsed.title
                } else {
                  sessions.value.unshift({
                    id: currentSessionId.value,
                    title: parsed.title,
                    createdAt: new Date().toISOString()
                  })
                }
              }
              return // 结束当前 buffer 处理
            }
            
            const reasoningChunk = normalizeStreamChunk(parsed.reasoning_content)
            if (reasoningChunk) {
              activeAiMessage.reasoning_content = (activeAiMessage.reasoning_content || '') + reasoningChunk
            }
            
            const contentChunk = normalizeStreamChunk(parsed.content)
            if (contentChunk) {
              if (!activeAiMessage.content) {
                activeAiMessage.isReasoningCollapsed = true
              }
              activeAiMessage.content += contentChunk
              if (onChunk) {
                onChunk(contentChunk)
              }
            }
          } catch (e) {
            console.warn('Failed to parse final SSE data:', data)
          }
        }
      }
      
      // 保存消息记录
      await saveMessages(userMessage, activeAiMessage)
      
      return { success: true }
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('Generation aborted by user')
        // 被用户中止时，也要尝试保存已生成的内容
        if (activeAiMessage.content || activeAiMessage.reasoning_content) {
          await saveMessages(userMessage, activeAiMessage)
        }
        return { success: true, aborted: true }
      }
      console.error('Send message error:', error)
      messages.value.pop() // 移除AI消息占位符
      return { 
        success: false, 
        message: error.response?.data?.message || '发送消息失败' 
      }
    } finally {
      isLoading.value = false
    }
  }
  
  // 停止生成
  function stopGeneration() {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
    }
    isLoading.value = false
  }

  // 保存消息记录
  async function saveMessages(userMessage, aiMessage) {
    try {
      await request.post(API_ENDPOINTS.chat.saveRecord, {
        session_id: currentSessionId.value,
        user_message: userMessage.content,
        ai_response: aiMessage.content,
        model: selectedModel.value
      })
    } catch (error) {
      console.error('Save messages error:', error)
    }
  }
  
  // 删除会话
  async function deleteSession(sessionId) {
    try {
      await request.delete(API_ENDPOINTS.chat.deleteSession(sessionId))
      sessions.value = sessions.value.filter(s => s.id !== sessionId)
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = null
        messages.value = []
      }
      return { success: true }
    } catch (error) {
      console.error('Delete session error:', error)
      return { success: false, message: '删除会话失败' }
    }
  }
  
  // 设置模型
  function setModel(model) {
    selectedModel.value = model
    localStorage.setItem('selectedModel', model)
  }
  
  return {
    sessions,
    currentSessionId,
    currentSession,
    messages,
    suggestions,
    isLoading,
    selectedModel,
    fetchSessions,
    createSession,
    fetchSessionMessages,
    sendMessage,
    stopGeneration,
    deleteSession,
    setModel
  }
})
