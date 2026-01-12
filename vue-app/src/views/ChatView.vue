<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 主聊天区域 -->
      <main class="chat-main">
        <div class="chat-header">
          <div class="chat-header-inner">
            <div class="header-left">
              <h2 class="chat-title">
                {{ currentSessionTitle }}
              </h2>
            </div>
          </div>
        </div>
        
        <div
          ref="messagesContainer"
          class="messages-container"
          @scroll.passive="handleMessagesScroll"
        >
          <div
            v-if="chatStore.messages.length === 0"
            class="empty-state"
          >
            <h3 class="empty-title">
              开始新的对话
            </h3>
            <p class="empty-description">
              向AI助手提问任何问题，获取专业的解答和帮助
            </p>
          </div>
          
          <div
            v-for="(message, index) in chatStore.messages"
            :key="index"
            class="message"
            :class="message.role === 'user' ? 'user' : 'assistant'"
          >
            <!-- AI头像 - 移除以匹配图 2 风格 -->
            <!-- <div
              v-if="message.role === 'assistant'"
              class="message-avatar"
              :class="{ 'has-image': !!getMessageAvatarSrc(message) }"
            >
              ...
            </div> -->

            <div class="message-content">
              <div class="message-bubble">
                <!-- 深度思考内容 -->
                <div 
                  v-if="message.reasoning_content" 
                  class="reasoning-block"
                  :class="{ 
                    'streaming': message.isStreaming && !message.content,
                    'collapsed': message.isReasoningCollapsed 
                  }"
                >
                  <div
                    class="reasoning-header"
                    @click="toggleReasoning(message)"
                  >
                    <div class="header-left">
                      <!-- 移除以匹配图 2 风格 -->
                      <!-- <div class="reasoning-icon-wrapper">
                        <i class="fas fa-brain reasoning-icon" />
                      </div> -->
                      <div class="header-text">
                        <span class="reasoning-title">深度思考</span>
                        <span 
                          v-if="!message.isReasoningCollapsed" 
                          class="reasoning-subtitle"
                        >AI 推理过程</span>
                        <span 
                          v-else-if="message.reasoning_content" 
                          class="reasoning-count"
                        >
                          {{ getReasoningLength(message.reasoning_content) }} 字
                        </span>
                      </div>
                    </div>
                    <div class="header-right">
                      <i 
                        class="fas toggle-icon"
                        :class="message.isReasoningCollapsed ? 'fa-chevron-down' : 'fa-chevron-up'" 
                      />
                    </div>
                  </div>
                
                  <transition name="reasoning-slide">
                    <div 
                      v-show="!message.isReasoningCollapsed" 
                      class="reasoning-content"
                    >
                      <div 
                        class="markdown-body"
                        v-html="formatReasoningCached(message)" 
                      />
                    </div>
                  </transition>
                </div>

                <!-- AI 响应内容 -->
                <div
                  v-if="message.error"
                  class="message-text error-state"
                >
                  <div class="error-content">
                    <i class="fas fa-exclamation-triangle error-icon" />
                    <span>{{ message.content || '响应异常，请稍后重试' }}</span>
                  </div>
                </div>
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div
                  v-else-if="message.content && !message.isStreaming"
                  class="message-text"
                  v-html="formatMessageCached(message, 'content')"
                />
                <div
                  v-else-if="message.content && message.isStreaming"
                  class="message-text"
                >
                  <span v-html="formatMessage(sanitizeNullRuns(message.content))" />
                  <span class="typing-cursor" />
                </div>
                <!-- 如果流式进行中且没有内容，显示输入状态 -->
                <div
                  v-else-if="message.isStreaming"
                  class="message-text"
                >
                  <span class="typing-cursor" />
                </div>
                <!-- 如果流式结束但内容为空，显示空状态提示 -->
                <div
                  v-else
                  class="message-text empty-response"
                >
                  <div class="empty-content">
                    <i class="fas fa-info-circle info-icon" />
                    <span>AI 未返回内容，请重试或检查配置。</span>
                  </div>
                </div>
              </div>
              <div class="message-time">
                {{ formatTime(message.timestamp) }}
              </div>

              <!-- 复制按钮 - 移动到建议问题上方 -->
              <button 
                class="message-copy-button" 
                title="复制这条消息"
                @click="copyMessage(message.content)"
              >
                <i class="fas fa-copy" />
                <span class="copy-text">复制</span>
              </button>

              <!-- 建议问题区域 - 仅在最新一条 AI 消息下方显示 -->
              <div 
                v-if="message.role === 'assistant' && index === chatStore.messages.length - 1 && chatStore.suggestions && chatStore.suggestions.length > 0 && !chatStore.isLoading" 
                class="suggestions-area"
              >
                <div class="suggestions-list">
                  <button 
                    v-for="(suggestion, sIndex) in chatStore.suggestions" 
                    :key="sIndex"
                    class="suggestion-item"
                    @click="sendSuggestion(suggestion)"
                  >
                    <span class="suggestion-text">{{ suggestion }}</span>
                    <i class="fas fa-arrow-right suggestion-arrow" />
                  </button>
                </div>
              </div>
            </div>

            <!-- 用户头像 - 移除或改为在user角色下不显示以匹配图2 -->
            <!-- 如果需要保留头像但放在右侧，可以在这里添加 v-if="message.role === 'user'" -->
          </div>
        </div>
        
        <div class="chat-input-area">
          <!-- 返回底部悬浮按钮（移动到输入框上方） -->
          <transition name="fade">
            <div 
              v-if="showScrollToBottomBtn" 
              class="scroll-to-bottom-floating"
              @mouseenter="handleMouseEnterScrollBottom"
              @mouseleave="handleMouseLeaveScrollBottom"
              @touchstart="handleMouseEnterScrollBottom"
              @touchend="handleMouseLeaveScrollBottom"
            >
              <button 
                class="scroll-to-bottom-btn"
                title="返回底部"
                @click="scrollToBottom('smooth')"
              >
                <i class="fas fa-arrow-down" />
                <span>最新消息</span>
              </button>
            </div>
          </transition>

          <div class="input-container">
            <textarea
              v-model="inputMessage"
              class="chat-input"
              placeholder="发送消息或输入 / 选择技能"
              :disabled="chatStore.isLoading"
              rows="1"
              @input="adjustTextareaHeight"
              @keydown.enter.exact.prevent="sendMessage"
            />
            
            <div class="input-toolbar">
              <div class="toolbar-left">
                <!-- 已移除上传附件按钮 -->
                <button 
                  class="tool-btn-special" 
                  :class="{ active: chatStore.selectedModel.includes('reasoner') }"
                  @click="toggleDeepThinking"
                >
                  <i class="fas fa-atom" />
                  <span>深度思考</span>
                </button>
                <div 
                  ref="modelMenuRef"
                  class="tool-btn-pill model-pill" 
                >
                  <div 
                    class="model-selector-trigger" 
                    :class="{ active: isModelMenuOpen }"
                    @click="isModelMenuOpen = !isModelMenuOpen"
                  >
                    <span class="brand-name">{{ currentBrand.name }}</span>
                    <i 
                      class="fas fa-chevron-up toggle-arrow" 
                      :class="{ rotate: isModelMenuOpen }" 
                    />
                  </div>
                  
                  <transition name="menu-fade">
                    <div 
                      v-if="isModelMenuOpen" 
                      class="model-dropdown-menu"
                    >
                      <div 
                        v-for="brand in brands" 
                        :key="brand.id"
                        class="model-menu-item"
                        :class="{ active: currentBrand.id === brand.id }"
                        @click="selectBrand(brand)"
                      >
                        <div class="item-info">
                          <span class="item-name">{{ brand.name }}</span>
                          <span class="item-desc">{{ brand.id === 'deepseek' ? 'DeepSeek-V3' : '豆包-pro-128k' }}</span>
                        </div>
                        <i 
                          v-if="currentBrand.id === brand.id" 
                          class="fas fa-check check-icon" 
                        />
                      </div>
                    </div>
                  </transition>
                </div>
              </div>
              
              <div class="toolbar-right">
                <!-- 已移除无用组件 -->
                
                <button
                  v-if="chatStore.isLoading"
                  class="stop-btn"
                  title="停止生成"
                  @click="chatStore.stopGeneration"
                >
                  <div class="stop-icon-wrapper">
                    <i class="fas fa-stop" />
                  </div>
                </button>
                <button
                  v-else
                  class="send-btn-new"
                  :disabled="!inputMessage.trim()"
                  title="发送消息"
                  @click="sendMessage"
                >
                  <div class="send-icon-wrapper">
                    <i class="fas fa-arrow-up" />
                  </div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 快速导航按钮（右侧中间） -->
      <div
        v-show="showNavArrows"
        class="nav-arrows"
        @mouseenter="handleMouseEnterNavArrows"
        @mouseleave="handleMouseLeaveNavArrows"
        @touchstart="handleMouseEnterNavArrows"
        @touchend="handleMouseLeaveNavArrows"
      >
        <button 
          v-show="canScrollUp"
          class="nav-arrow-btn up"
          title="上一条对话"
          @click="scrollToPrevMessage"
        >
          <i class="fas fa-chevron-up" />
        </button>
        <button 
          v-show="canScrollDown"
          class="nav-arrow-btn down"
          title="下一条对话"
          @click="scrollToNextMessage"
        >
          <i class="fas fa-chevron-down" />
        </button>
      </div>

      <!-- 历史提问导航面板（右上角悬浮） -->
      <div class="history-nav-container">
        <button 
          class="history-nav-toggle"
          title="提问历史"
          @click="showHistoryPanel = !showHistoryPanel"
        >
          <i class="fas fa-list-ul" />
        </button>
        
        <transition name="slide-fade">
          <div
            v-if="showHistoryPanel"
            class="history-nav-panel"
          >
            <div class="panel-header">
              <h3>提问历史</h3>
              <button
                class="close-btn"
                @click="showHistoryPanel = false"
              >
                <i class="fas fa-times" />
              </button>
            </div>
            <div class="panel-content">
              <div 
                v-for="(msg, index) in userMessages" 
                :key="index"
                class="history-item"
                @click="scrollToMessage(msg.elementIndex)"
              >
                <span class="time">{{ formatTimeShort(msg.timestamp) }}</span>
                <span class="text">{{ msg.content }}</span>
              </div>
              <div
                v-if="userMessages.length === 0"
                class="empty-history"
              >
                暂无提问记录
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import DOMPurify from 'dompurify'
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { API_CONFIG } from '@/config/api'

const chatStore = useChatStore()
const authStore = useAuthStore()
const uiStore = useUIStore()
const router = useRouter()
const route = useRoute()
const inputMessage = ref('')
const messagesContainer = ref(null)
const isPinnedToBottom = ref(true)
const SCROLL_BOTTOM_THRESHOLD_PX = 40  // 判断是否在底部的阈值（像素）
const SCROLL_BOTTOM_SHOW_THRESHOLD_PX = 100  // 显示按钮的阈值（距离底部多少像素内显示）
let autoScrollScheduled = false

// Navigation State
const showNavArrows = ref(false)
const showScrollToBottomBtn = ref(false)
const showHistoryPanel = ref(false)
const canScrollUp = ref(false)
const canScrollDown = ref(false)
const isHoveringNavArrows = ref(false)
const isHoveringScrollBottom = ref(false)
let scrollDownCount = 0
let scrollBottomTimer = null
let navArrowsTimer = null

const userAvatarUrl = ref(null) // 用于消息列表头像

/**
 * 计算属性：获取所有用户提问
 */
const userMessages = computed(() => {
  return chatStore.messages
    .map((msg, index) => ({ ...msg, elementIndex: index }))
    .filter(msg => msg.role === 'user')
    .sort((a, b) => b.timestamp - a.timestamp) // 倒序排列
})

/**
 * 格式化短时间
 */
const formatTimeShort = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 滚动到指定消息
 */
const scrollToMessage = (index) => {
  const elements = messagesContainer.value?.querySelectorAll('.message')
  if (elements && elements[index]) {
    elements[index].scrollIntoView({ behavior: 'smooth', block: 'center' })
    // 将高亮效果应用到消息气泡
    const bubble = elements[index].querySelector('.message-bubble')
    if (bubble) {
      bubble.classList.add('highlight-message')
      setTimeout(() => {
        bubble.classList.remove('highlight-message')
      }, 2000)
    }
    // 移动端下点击后自动关闭面板
    if (window.innerWidth < 768) {
      showHistoryPanel.value = false
    }
  }
}

/**
 * 滚动到上一条用户消息
 */
const scrollToPrevMessage = () => {
  if (!messagesContainer.value) return
  const scrollTop = messagesContainer.value.scrollTop
  const elements = Array.from(messagesContainer.value.querySelectorAll('.message'))
  
  // 找到当前视口上方第一条用户消息（有 'user' 类名）
  for (let i = elements.length - 1; i >= 0; i--) {
    if (elements[i].classList.contains('user') && elements[i].offsetTop < scrollTop) {
      elements[i].scrollIntoView({ behavior: 'smooth', block: 'center' })
      // 将高亮效果应用到消息气泡
      const bubble = elements[i].querySelector('.message-bubble')
      if (bubble) {
        bubble.classList.add('highlight-message')
        setTimeout(() => {
          bubble.classList.remove('highlight-message')
        }, 2000)
      }
      // 滚动后更新按钮状态
      setTimeout(() => {
        handleMessagesScroll()
      }, 300)
      return
    }
  }
  
  // 如果没有找到上一条用户消息，滚动到顶部
  if (scrollTop > 0) {
    messagesContainer.value.scrollTo({ top: 0, behavior: 'smooth' })
    setTimeout(() => {
      handleMessagesScroll()
    }, 300)
  }
}

/**
 * 滚动到下一条用户消息
 */
const scrollToNextMessage = () => {
  if (!messagesContainer.value) return
  const scrollTop = messagesContainer.value.scrollTop
  const viewportBottom = scrollTop + messagesContainer.value.clientHeight
  const elements = Array.from(messagesContainer.value.querySelectorAll('.message'))
  
  // 找到当前视口下方第一条用户消息（有 'user' 类名）
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].classList.contains('user') && elements[i].offsetTop > viewportBottom) {
      elements[i].scrollIntoView({ behavior: 'smooth', block: 'center' })
      // 将高亮效果应用到消息气泡
      const bubble = elements[i].querySelector('.message-bubble')
      if (bubble) {
        bubble.classList.add('highlight-message')
        setTimeout(() => {
          bubble.classList.remove('highlight-message')
        }, 2000)
      }
      // 滚动后更新按钮状态
      setTimeout(() => {
        handleMessagesScroll()
      }, 300)
      return
    }
  }
  
  // 如果没有找到下一条用户消息，滚动到底部
  const { scrollHeight, clientHeight } = messagesContainer.value
  const distanceToBottom = scrollHeight - scrollTop - clientHeight
  if (distanceToBottom > SCROLL_BOTTOM_THRESHOLD_PX) {
    scrollToBottom('smooth')
    setTimeout(() => {
      handleMessagesScroll()
    }, 300)
  }
}

/**
 * 处理鼠标滚轮事件
 */
const handleWheel = (e) => {
  // 1. 快速导航箭头显示逻辑
  // 更新滚动状态，确保按钮显示状态准确
  updatePinnedState()
  handleMessagesScroll()
  
  showNavArrows.value = true
  if (navArrowsTimer) clearTimeout(navArrowsTimer)
  
  // 仅在鼠标未悬停时开启自动隐藏计时器
  if (!isHoveringNavArrows.value) {
    navArrowsTimer = setTimeout(() => {
      showNavArrows.value = false
    }, 2000) // 2秒无操作隐藏箭头
  }

  // 2. 返回底部按钮触发逻辑
  // 优化：更新滚动状态，确保状态是最新的
  updatePinnedState()
  
  // 如果已经在底部，不显示按钮并清除相关状态
  if (isPinnedToBottom.value) {
    showScrollToBottomBtn.value = false
    scrollDownCount = 0
    if (scrollBottomTimer) {
      clearTimeout(scrollBottomTimer)
      scrollBottomTimer = null
    }
    return // 已在底部，无需处理后续逻辑
  }
  
  // 不在底部时的处理逻辑
  if (e.deltaY > 0) {
    // 向下滚动且不在底部时，累加计数
    scrollDownCount++
    if (scrollDownCount >= 3) {
      showScrollToBottomBtn.value = true
      if (scrollBottomTimer) clearTimeout(scrollBottomTimer)
      
      // 仅在鼠标未悬停时开启自动隐藏计时器
      if (!isHoveringScrollBottom.value) {
        scrollBottomTimer = setTimeout(() => {
          showScrollToBottomBtn.value = false
          scrollDownCount = 0
        }, 3000)
      }
    }
  } else if (e.deltaY < 0) {
    scrollDownCount = 0 // 向上滑动重置计数
  }
}

/**
 * 鼠标进入/离开导航箭头的处理
 */
const handleMouseEnterNavArrows = () => {
  isHoveringNavArrows.value = true
  if (navArrowsTimer) {
    clearTimeout(navArrowsTimer)
    navArrowsTimer = null
  }
}

const handleMouseLeaveNavArrows = () => {
  isHoveringNavArrows.value = false
  // 离开后重新开始计时隐藏
  if (showNavArrows.value) {
    if (navArrowsTimer) clearTimeout(navArrowsTimer)
    navArrowsTimer = setTimeout(() => {
      showNavArrows.value = false
    }, 2000)
  }
}

/**
 * 鼠标进入/离开“最新消息”按钮的处理
 */
const handleMouseEnterScrollBottom = () => {
  isHoveringScrollBottom.value = true
  if (scrollBottomTimer) {
    clearTimeout(scrollBottomTimer)
    scrollBottomTimer = null
  }
}

const handleMouseLeaveScrollBottom = () => {
  isHoveringScrollBottom.value = false
  // 离开后重新开始计时隐藏
  if (showScrollToBottomBtn.value) {
    if (scrollBottomTimer) clearTimeout(scrollBottomTimer)
    scrollBottomTimer = setTimeout(() => {
      showScrollToBottomBtn.value = false
      scrollDownCount = 0
    }, 3000)
  }
}

/**
 * 复制代码到剪贴板 - 改为全局函数，供内联事件调用
 */
window.copyCodeBlock = (element) => {
  const code = element.previousElementSibling.textContent
  const button = element
  const icon = button.querySelector('i')
  const text = button.querySelector('span')
  
  navigator.clipboard.writeText(code)
    .then(() => {
      // 显示复制成功的反馈
      const originalIconClass = icon ? icon.className : ''
      const originalText = text ? text.textContent : button.textContent
      
      if (icon) icon.className = 'fas fa-check'
      if (text) text.textContent = '已复制!'
      else if (!icon) button.textContent = '已复制!'
      
      button.classList.add('copied')
      
      setTimeout(() => {
        if (icon) icon.className = originalIconClass
        if (text) text.textContent = originalText
        else if (!icon) button.textContent = originalText
        button.classList.remove('copied')
      }, 2000)
    })
    .catch(err => {
      console.error('复制失败:', err)
    })
}

// 自定义marked渲染器，直接在渲染时添加复制按钮
const renderer = new marked.Renderer()
const originalCode = renderer.code
renderer.code = function(code, language, escaped) {
  const originalResult = originalCode.call(this, code, language, escaped)
  // 在pre标签内添加复制按钮，包含图标和文字
  return originalResult.replace('<pre', '<pre style="position: relative">')
    .replace('</pre>', '<button class="copy-button" onclick="copyCodeBlock(this)"><i class="far fa-copy"></i><span>复制</span></button></pre>')
}

// 配置marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  langPrefix: 'hljs language-',
  breaks: true,
  gfm: true,
  renderer: renderer // 使用自定义渲染器
})

const currentSessionTitle = computed(() => {
  return chatStore.currentSession?.title || '新对话'
})

/**
 * 滚动到底部
 * @param {string} behavior - 滚动行为 'smooth' 或 'auto'
 */
const scrollToBottom = (behavior = 'smooth') => {
  if (messagesContainer.value) {
    const el = messagesContainer.value
    // 优先使用 scrollTop 直接设置，这样最可靠
    if (behavior === 'auto') {
      el.scrollTop = el.scrollHeight
      // 立即检查并隐藏按钮
      nextTick(() => {
        updatePinnedState()
        // 强制检查是否真的到底了
        const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
        if (distanceToBottom <= SCROLL_BOTTOM_THRESHOLD_PX) {
          showScrollToBottomBtn.value = false
          scrollDownCount = 0
          if (scrollBottomTimer) {
            clearTimeout(scrollBottomTimer)
            scrollBottomTimer = null
          }
        }
      })
    } else {
      el.scrollTo({
        top: el.scrollHeight,
        behavior: behavior
      })
      // 平滑滚动完成后检查并隐藏按钮
      // 使用延迟检查，确保滚动动画完成
      const checkBottom = () => {
        updatePinnedState()
        const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
        // 确保真的到底了才隐藏按钮
        if (distanceToBottom <= SCROLL_BOTTOM_THRESHOLD_PX) {
          showScrollToBottomBtn.value = false
          scrollDownCount = 0
          if (scrollBottomTimer) {
            clearTimeout(scrollBottomTimer)
            scrollBottomTimer = null
          }
        }
      }
      // smooth 滚动通常需要 200-500ms，使用较长的延迟确保检测准确
      setTimeout(checkBottom, 500)
      // 如果浏览器支持 scrollend 事件，也监听它
      if ('onscrollend' in el) {
        el.addEventListener('scrollend', checkBottom, { once: true })
      }
      // 额外使用 requestAnimationFrame 确保检测准确
      requestAnimationFrame(() => {
        requestAnimationFrame(checkBottom)
      })
    }
  }
}

const updatePinnedState = () => {
  const el = messagesContainer.value
  if (!el) return
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  isPinnedToBottom.value = distanceToBottom <= SCROLL_BOTTOM_THRESHOLD_PX
}

const scheduleAutoScrollToBottom = () => {
  if (!isPinnedToBottom.value) return
  if (autoScrollScheduled) return
  autoScrollScheduled = true
  const raf = (typeof window !== 'undefined' && window.requestAnimationFrame)
    ? window.requestAnimationFrame.bind(window)
    : (fn) => setTimeout(fn, 16)
  raf(() => {
    autoScrollScheduled = false
    if (!isPinnedToBottom.value) return
    scrollToBottom('auto')
  })
}

/**
 * 处理消息容器的滚动事件
 * 当滚动到底部时自动隐藏"最新消息"按钮
 */
const handleMessagesScroll = () => {
  updatePinnedState()
  
  const el = messagesContainer.value
  if (!el) return
  
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  
  // 优化：当已经在底部时，立即隐藏"最新消息"按钮
  if (isPinnedToBottom.value && showScrollToBottomBtn.value) {
    showScrollToBottomBtn.value = false
    scrollDownCount = 0
    if (scrollBottomTimer) {
      clearTimeout(scrollBottomTimer)
      scrollBottomTimer = null
    }
  } else if (!isPinnedToBottom.value && distanceToBottom <= SCROLL_BOTTOM_SHOW_THRESHOLD_PX) {
    // 如果不在底部但在显示阈值内，显示按钮
    if (!showScrollToBottomBtn.value) {
      showScrollToBottomBtn.value = true
      if (scrollBottomTimer) clearTimeout(scrollBottomTimer)
      // 设置自动隐藏计时器
      if (!isHoveringScrollBottom.value) {
        scrollBottomTimer = setTimeout(() => {
          if (!isPinnedToBottom.value) {
            showScrollToBottomBtn.value = false
            scrollDownCount = 0
          }
        }, 3000)
      }
    }
  }
  
  // 更新导航箭头状态
  if (messagesContainer.value) {
    const { scrollTop, scrollHeight, clientHeight } = messagesContainer.value
    const distanceToBottom = scrollHeight - scrollTop - clientHeight
    
    // 优化：更精确的判断逻辑
    // 上箭头：如果不在顶部（有内容可以向上滚动）
    canScrollUp.value = scrollTop > 5
    
    // 下箭头：如果不在底部（有内容可以向下滚动）
    // 考虑到底部的阈值，避免在底部时还显示下箭头
    canScrollDown.value = distanceToBottom > SCROLL_BOTTOM_THRESHOLD_PX
    
    // 如果已经在最底部，确保下箭头隐藏
    if (isPinnedToBottom.value) {
      canScrollDown.value = false
    }

    // 移动端适配：滚动时显示导航箭头
    if (window.innerWidth <= 768) {
      showNavArrows.value = true
      if (navArrowsTimer) clearTimeout(navArrowsTimer)
      navArrowsTimer = setTimeout(() => {
        showNavArrows.value = false
      }, 2000)
    }
  }
}

/**
 * 统一的滚动到底部触发函数
 * @param {number} delay 延迟时间
 */
const triggerScrollToBottom = (delay = 100) => {
  nextTick(() => {
    // 第一次尝试：立即执行
    scrollToBottom('auto')
    // 第二次尝试：短延迟
    setTimeout(() => scrollToBottom('auto'), delay)
    // 第三次尝试：中等延迟
    setTimeout(() => scrollToBottom('auto'), delay + 200)
    // 第四次尝试：长延迟，确保公式和复杂内容渲染
    setTimeout(() => scrollToBottom('auto'), delay + 500)
    // 第五次尝试：超长延迟，针对低性能设备或大量内容
    setTimeout(() => {
      scrollToBottom('auto')
      updatePinnedState()
    }, delay + 1000)
  })
}

onMounted(async () => {
  // 如果是游客，不加载会话
  if (!authStore.isAuthenticated) {
    chatStore.clearMessages()
    chatStore.currentSessionId = null
    triggerScrollToBottom(100)
    return
  }

  // 0. 确保会话列表已加载
  if (chatStore.sessions.length === 0) {
    await chatStore.fetchSessions()
  }

  // 1. 如果 URL 中有会话 ID，优先加载该会话
  const querySessionId = route.query.session
  if (querySessionId) {
    chatStore.currentSessionId = querySessionId
    await chatStore.fetchSessionMessages(querySessionId)
    
    // 加载草稿
    inputMessage.value = chatStore.getDraft(querySessionId)
    
    // 恢复滚动位置
    nextTick(() => {
      const scrollTop = chatStore.getScrollPosition(querySessionId)
      if (messagesContainer.value && scrollTop > 0) {
        messagesContainer.value.scrollTop = scrollTop
      } else {
        triggerScrollToBottom(100)
      }
    })
    return
  }

  // 2. 如果没有当前会话，但有会话列表，加载第一个
  if (!chatStore.currentSessionId && chatStore.sessions.length > 0) {
    const firstSessionId = chatStore.sessions[0].id
    chatStore.currentSessionId = firstSessionId
    await chatStore.fetchSessionMessages(firstSessionId)
    
    // 加载草稿
    inputMessage.value = chatStore.getDraft(firstSessionId)
    
    // 恢复滚动位置
    nextTick(() => {
      const scrollTop = chatStore.getScrollPosition(firstSessionId)
      if (messagesContainer.value && scrollTop > 0) {
        messagesContainer.value.scrollTop = scrollTop
      } else {
        triggerScrollToBottom(100)
      }
    })
    // 更新 URL
    router.replace(`/chat?session=${firstSessionId}`)
  } 
  // 3. 如果没有任何会话，创建一个新的
  else if (!chatStore.currentSessionId && chatStore.sessions.length === 0) {
    const result = await chatStore.createSession()
    if (result?.success && result.sessionId) {
      router.replace(`/chat?session=${result.sessionId}`)
    }
    triggerScrollToBottom(100)
  }
})

// 监听输入框变化保存草稿
watch(inputMessage, (newVal) => {
  chatStore.saveDraft(chatStore.currentSessionId, newVal)
})

// 监听 URL 参数变化 (例如点击侧边栏时)
watch(
  () => route.query.session,
  async (newSessionId, oldSessionId) => {
    // 1. 处理旧会话的保存与清理
    if (oldSessionId) {
      // 保存旧会话的滚动位置
      if (messagesContainer.value) {
        chatStore.saveScrollPosition(oldSessionId, messagesContainer.value.scrollTop)
      }
      
      // 检查旧会话是否需要自动清理 (无消息且无草稿)
      const oldSession = chatStore.sessions.find(s => s.id === oldSessionId)
      // 注意：这里需要检查该会话是否有实际消息。messages.value 已经被 fetchSessionMessages 覆盖，
      // 所以我们需要一个更可靠的方法来检查旧会话是否为空。
      // 为了简单起见，我们假设如果 messages 为空且没有草稿，就应该清理。
      // 但 fetchSessionMessages 是异步的，这里逻辑需要小心。
      
      const oldDraft = chatStore.getDraft(oldSessionId)
      // 如果旧会话没有草稿，且当前消息列表为空（因为即将加载新会话），
      // 我们可能需要从后端或 store 的 sessions 列表中判断消息数。
      // 暂时先处理最直接的情况：没有草稿且是“新对话”标题的会话。
      if (oldSession && oldSession.title === '新对话' && !oldDraft) {
        // 静默删除空的新会话
        chatStore.deleteSession(oldSessionId)
      }
    }
    
    // 2. 加载新会话
    if (newSessionId) {
      await chatStore.fetchSessionMessages(newSessionId)
      
      // 加载草稿
      inputMessage.value = chatStore.getDraft(newSessionId)
      
      // 恢复新会话的滚动位置
      nextTick(() => {
        const scrollTop = chatStore.getScrollPosition(newSessionId)
        if (messagesContainer.value && scrollTop > 0) {
          messagesContainer.value.scrollTop = scrollTop
        } else {
          triggerScrollToBottom(100)
        }
      })
    } else {
      // 如果没有会话 ID，可能回到了 /chat 根路径，尝试加载最近的会话或显示空状态
      if (chatStore.sessions.length > 0) {
        const firstSessionId = chatStore.sessions[0].id
        chatStore.currentSessionId = firstSessionId
        await chatStore.fetchSessionMessages(firstSessionId)
        // 恢复滚动位置
        nextTick(() => {
          const scrollTop = chatStore.getScrollPosition(firstSessionId)
          if (messagesContainer.value && scrollTop > 0) {
            messagesContainer.value.scrollTop = scrollTop
          } else {
            triggerScrollToBottom(100)
          }
        })
      } else {
        chatStore.currentSessionId = null
        chatStore.messages = []
      }
    }
  }
)

// 监听当前会话 ID 变化
watch(
  () => chatStore.currentSessionId,
  async (newId, oldId) => {
    if (newId && newId !== oldId) {
      // 保存旧会话的滚动位置
      if (oldId && messagesContainer.value) {
        chatStore.saveScrollPosition(oldId, messagesContainer.value.scrollTop)
      }
      // 只有当路由中的 session 与 newId 不一致时才加载，避免与路由监听冲突
      if (newId !== route.query.session) {
        await chatStore.fetchSessionMessages(newId)
        // 恢复新会话的滚动位置
        nextTick(() => {
          const scrollTop = chatStore.getScrollPosition(newId)
          if (messagesContainer.value && scrollTop > 0) {
            messagesContainer.value.scrollTop = scrollTop
          } else {
            triggerScrollToBottom(100)
          }
        })
      }
    }
  }
)

watch(
  () => authStore.userInfo?.avatar,
  async (path) => {
    if (userAvatarUrl.value) {
      URL.revokeObjectURL(userAvatarUrl.value)
      userAvatarUrl.value = null
    }
    if (!path) return
    try {
      const res = await fetch(`${API_CONFIG.baseURL}${path}`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!res.ok) return
      const blob = await res.blob()
      userAvatarUrl.value = URL.createObjectURL(blob)
    } catch {
      userAvatarUrl.value = null
    }
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('wheel', handleWheel)
})

onUnmounted(() => {
  window.removeEventListener('wheel', handleWheel)
  if (userAvatarUrl.value) {
    URL.revokeObjectURL(userAvatarUrl.value)
    userAvatarUrl.value = null
  }
  
  // 保存当前滚动位置和草稿
  if (chatStore.currentSessionId) {
    if (messagesContainer.value) {
      chatStore.saveScrollPosition(chatStore.currentSessionId, messagesContainer.value.scrollTop)
    }
    chatStore.saveDraft(chatStore.currentSessionId, inputMessage.value)
    
    // 检查当前会话是否需要自动清理 (无消息且无草稿)
    const currentSession = chatStore.sessions.find(s => s.id === chatStore.currentSessionId)
    if (currentSession && currentSession.title === '新对话' && !inputMessage.value.trim() && chatStore.messages.length === 0) {
      chatStore.deleteSession(chatStore.currentSessionId)
    }
  }
})

const toggleReasoning = (message) => {
  message.isReasoningCollapsed = !message.isReasoningCollapsed
}

/**
 * 点击建议问题发送
 * @param {string} suggestion 建议问题文本
 */
const sendSuggestion = (suggestion) => {
  inputMessage.value = suggestion
  sendMessage()
}

const createNewSession = async () => {
  const result = await chatStore.createSession()
  if (result?.success && result.sessionId) {
    router.replace(`/chat?session=${result.sessionId}`)
    return result.sessionId
  }
  return null
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || chatStore.isLoading) return
  
  // 如果没有当前会话，先创建一个（仅针对已登录用户）
  // 游客模式下不需要预先创建会话，由后端在发送第一条消息时自动生成
  if (!chatStore.currentSessionId && authStore.isAuthenticated) {
    await createNewSession()
  }
  
  const message = inputMessage.value.trim()
  inputMessage.value = ''
  
  // 清除草稿
  chatStore.saveDraft(chatStore.currentSessionId, '')
  
  // 重置输入框高度
  const textarea = document.querySelector('.chat-input')
  if (textarea) {
    textarea.style.height = 'auto'
  }
  
  isPinnedToBottom.value = true
  await chatStore.sendMessage(message, () => {
    scheduleAutoScrollToBottom()
  })
  scheduleAutoScrollToBottom()
}

// 渲染数学公式
const renderMathFormula = (content, placeholders = []) => {
  // 1. 先处理特定格式的公式，比如用户提供的截图中的格式
  let processedContent = content;
  
  // 0. 预处理：标准化 LaTeX 定界符和转义符
  // 处理双反斜杠转义问题 (例如 \\int -> \int, \\( -> \()
  // 仅处理常见的数学命令和定界符，避免破坏换行符 \\
  processedContent = processedContent.replace(/\\\\(int|sqrt|frac|left|right|,|d[xyt]|sigma|alpha|beta|gamma|pi|theta|infty|cdot|approx|le|ge|ne|equiv|sum|lim|to)/g, '\\$1');
  processedContent = processedContent.replace(/\\\\([\[\]()])/g, '\\$1');

  // 辅助函数：清理捕获内容中的HTML标签
  const cleanTags = (str) => {
    if (!str) return '';
    // 移除常见的块级和内联标签，避免破坏公式结构
    return str.replace(/<\/?(li|ul|ol|p|div|span|br|h\d|strong|em)[^>]*>/gi, "").trim();
  };

  // 辅助函数：创建占位符并存储KaTeX结果
  const createPlaceholder = (formula, displayMode) => {
    try {
      const html = katex.renderToString(formula, {
        throwOnError: false,
        displayMode: displayMode
      });
      const index = placeholders.length;
      placeholders.push(html);
      return `MATH-PLACEHOLDER-${index}-END`;
    } catch (error) {
      console.error('KaTeX渲染错误:', error);
      return formula;
    }
  };
  
  // 0.5 优先处理标准 LaTeX 块级和行内公式定界符
  // 处理 $$ ... $$
  processedContent = processedContent.replace(/\$\$([\s\S]+?)\$\$/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula);
    return createPlaceholder(cleanedFormula, true);
  });

  // 处理 \[ ... \]
  processedContent = processedContent.replace(/\\\[([\s\S]+?)\\\]/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula);
    return createPlaceholder(cleanedFormula, true);
  });

  // 处理 \( ... \)
  processedContent = processedContent.replace(/\\\(([\s\S]+?)\\\)/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula);
    return createPlaceholder(cleanedFormula, false);
  });

  // 处理带有<br>标签的定积分公式
  const brFormulaRegex = /<br\s*\/?>\\int\s*_{(\d+)}^\{(\d+)}\s*(\d+)x\s*,\s*dx\s*=\s*F\((\d+)\)\s*-\s*F\((\d+)\)\s*=\s*\((\d+)\^2\)\s*-\s*\((\d+)\^2\)\s*=\s*(\d+)\s*-\s*(\d+)\s*=\s*(\d+)<br\s*\/?>/g;
  processedContent = processedContent.replace(brFormulaRegex, (match, lower, upper, coeff, fUpper, fLower, squareUpper, squareLower, val1, val2, result) => {
    const formula = `\\int_{${lower}}^{${upper}} ${coeff}x dx = F(${fUpper}) - F(${fLower}) = (${squareUpper}^2) - (${squareLower}^2) = ${val1} - ${val2} = ${result}`;
    return createPlaceholder(formula, true);
  });
  
  // 2. 处理基本定积分公式
  const basicIntegralRegex = /\\int\s*_{(\w+)}^\{(\w+)}\s*f\(x\)\s*,\s*dx\s*=\s*F\((\w+)\)\s*-\s*F\((\w+)\)/g;
  processedContent = processedContent.replace(basicIntegralRegex, (match, lower, upper, fUpper, fLower) => {
    const formula = `\\int_{${lower}}^{${upper}} f(x) dx = F(${fUpper}) - F(${fLower})`;
    return createPlaceholder(formula, true);
  });
  
  // 2.5 处理带 \left. ... \right| 的完整积分公式（优先匹配，因为更具体）
  // 匹配如：\int_{a}^{b} ... dx = \left. ... \right|_{a}^{b} = ...
  // 支持负号下标，如 \int_{-\pi}^{\pi}
  const integralWithEvalRegex = /\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?d(?:[a-z]+|\\[a-zA-Z]+)\s*=\s*\\left\.[^\n]*?\\right\|(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?(?:\s*=\s*[^\n]*?)?/g;
  processedContent = processedContent.replace(integralWithEvalRegex, (match) => {
    let cleanedMatch = cleanTags(match);
    const intIndex = cleanedMatch.indexOf('\\int');
    if (intIndex < 0) return match;
    let formula = cleanedMatch.substring(intIndex);
    // 移除末尾的中文和多余标点
    formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '');
    formula = formula.replace(/\s*\)\s*\]\s*$/, '').replace(/\s*\]\s*$/, '');
    if (formula && formula.includes('\\int') && formula.includes('\\left.')) {
      return createPlaceholder(formula.trim(), true);
    }
    return match;
  });
  
  // 3. 处理导数基本公式
  const derivativeRegex = /\\left\(([^)]+)\\right\)'\s*=\s*([^\n]+?)(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,)/g;
  processedContent = processedContent.replace(derivativeRegex, (match, func, result) => {
    let cleanedResult = cleanTags(result);
    // 移除编号和中文文本，但保留LaTeX命令
    cleanedResult = cleanedResult.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
    cleanedResult = cleanedResult.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
    cleanedResult = cleanedResult.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
    if (cleanedResult) {
    const formula = `\\left(${func}\\right)' = ${cleanedResult}`;
    return createPlaceholder(formula, false);
    }
    return match;
  });
  
  // 4. 处理积分基本公式（带逗号的格式）
  const integralRegex = /\\int\s*([^,\n]+?)\s*,\s*dx\s*=\s*([^\n]+?)(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,)/g;
  processedContent = processedContent.replace(integralRegex, (match, integrand, result) => {
    let cleanedResult = cleanTags(result);
    // 移除编号和中文文本，但保留LaTeX命令
    cleanedResult = cleanedResult.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
    cleanedResult = cleanedResult.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
    cleanedResult = cleanedResult.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
    if (cleanedResult) {
    const formula = `\\int ${integrand} dx = ${cleanedResult}`;
    return createPlaceholder(formula, true);
    }
    return match;
  });
  
  // 4.5 处理不带逗号的积分公式（\int_{a}^{b} ... dx = ...）
  // 支持负号下标和等号后面的多个积分
  const integralNoCommaRegex = /\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?\s+dx\s*=\s*[^\n]*?(?:\\int[^\n]*?dx[^\n]*?)?[^\n]+?(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,|\.)/g;
  processedContent = processedContent.replace(integralNoCommaRegex, (match) => {
    let cleanedMatch = cleanTags(match);
    const intIndex = cleanedMatch.indexOf('\\int');
    if (intIndex < 0) return match;
    let formula = cleanedMatch.substring(intIndex);
    formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '').trim();
    if (formula && formula.includes('\\int') && /\s+dx\s*=/.test(formula)) {
      return createPlaceholder(formula, true);
    }
    return match;
  });
  
  // 4.6 处理简单积分（无上下限的），如 \int x e^x dx
  const simpleIntegralRegex = /\\int\s+[^\n]+?\s+d(?:[a-z]+|\\[a-zA-Z]+)(?=\s|$|[\u4e00-\u9fa5]|\[|\(|\)|,|\.)/g;
  processedContent = processedContent.replace(simpleIntegralRegex, (match) => {
    let cleanedMatch = cleanTags(match);
    const intIndex = cleanedMatch.indexOf('\\int');
    if (intIndex < 0) return match;
    let formula = cleanedMatch.substring(intIndex);
    // 移除末尾的中文和多余标点
    formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '');
    formula = formula.replace(/\s*\)\s*\]\s*$/, '').replace(/\s*\]\s*$/, '');
    if (formula && formula.includes('\\int') && /\bd[a-z]+\b/.test(formula)) {
      return createPlaceholder(formula.trim(), true);
    }
    return match;
  });
  
  // 5. 处理分式 (已废弃，避免破坏复杂公式结构)
  // const fracRegex = /\\frac{([^}]+)}{([^}]+)}/g;
  // processedContent = processedContent.replace(fracRegex, (match, numerator, denominator) => {
  //   const formula = `\\frac{${numerator}}{${denominator}}`;
  //   return createPlaceholder(formula, false);
  // });

  // 6. 新增：通用积分公式匹配 (针对 \int_{a}^{b} x^n dx 这种未被特定规则捕获的情况)
  // 匹配完整的积分公式，包括：
  // - \int_{a}^{b} ... dx = ... 的形式
  // - \int_{-a}^{a} ... dx = 2 \int_{0}^{a} ... dx (包含多个积分)
  // - \int x e^x dx (无上下限的简单积分)
  // 使用更宽松的匹配，确保能捕获完整的公式（包括等号后面的所有内容，可能包含多个积分）
  // 改进：匹配到等号后，继续匹配可能存在的第二个积分
  const generalIntegralRegex = /\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?d(?:[a-z]+|\\[a-zA-Z]+)(?:\s*=\s*[^\n]*?(?:\\int\s*(?:(?:_\{[^}]+\})|(?:_[-a-zA-Z0-9]+))?(?:\^\{[^}]+\}|\^[-a-zA-Z0-9]+)?\s*[^\n]*?d(?:[a-z]+|\\[a-zA-Z]+))?[^\n]*?)?(?=[\s\u4e00-\u9fa5]|$|\[|\(|\)|,|\.\s)/g;
  processedContent = processedContent.replace(generalIntegralRegex, (match) => {
     // 清理匹配内容：移除HTML标签
     let cleanedMatch = cleanTags(match);
     
     // 找到公式的起始位置（\int的位置）
     const intIndex = cleanedMatch.indexOf('\\int');
     if (intIndex < 0) {
       return match; // 如果没有找到\int，返回原匹配
     }
     
     // 从\int开始提取公式
     let formula = cleanedMatch.substring(intIndex);
     
     // 移除公式末尾的中文和多余标点，但保留公式结构
     // 找到公式的实际结束位置（最后一个数学符号或括号）
     formula = formula.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]+(?=\s*$)/g, '');
     // 移除末尾多余的括号和方括号，但保留公式中的括号
     formula = formula.replace(/\s*\)\s*\]\s*$/, ''); // 只移除末尾的 )]
     formula = formula.replace(/\s*\]\s*$/, ''); // 移除末尾的 ]
     
     // 确保公式完整（至少包含\int和dx或dt等）
     if (formula.trim() && formula.includes('\\int') && /\bd[a-z]+\b/.test(formula)) {
       return createPlaceholder(formula.trim(), true);
     }
     return match; // 如果清理后无效，返回原匹配
  });

  // 7. 新增：处理 \left| ... \right| 绝对值/范数
  const absRegex = /\\left\|[^\n]+?\\right\|/g;
  processedContent = processedContent.replace(absRegex, (match) => {
      let cleanedMatch = cleanTags(match);
      // 移除编号和中文文本，但保留LaTeX命令
      cleanedMatch = cleanedMatch.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
      cleanedMatch = cleanedMatch.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
      cleanedMatch = cleanedMatch.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
      if (cleanedMatch && cleanedMatch.includes('\\left|')) {
      return createPlaceholder(cleanedMatch, false);
      }
      return match;
  });

  // 8. 新增：处理 \left. ... \right| 代换值
  const evalRegex = /\\left\.[^\n]+?\\right\|(?:_\{[^}]+\})?(?:\^\{[^}]+\})?/g;
  processedContent = processedContent.replace(evalRegex, (match) => {
      let cleanedMatch = cleanTags(match);
      // 移除编号和中文文本，但保留LaTeX命令
      cleanedMatch = cleanedMatch.replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/, '');
      cleanedMatch = cleanedMatch.replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/, '');
      cleanedMatch = cleanedMatch.replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '').trim();
      if (cleanedMatch && cleanedMatch.includes('\\left.')) {
      return createPlaceholder(cleanedMatch, false);
      }
      return match;
  });
  
  // 9. 清理HTML标签 (移除：不要在公式处理阶段盲目替换 br)
  // processedContent = processedContent.replace(/<br\s*\/?>/g, ' ');
  
  return processedContent;
};

const restoreMathFormula = (content, placeholders) => {
  // 1. 先尝试清理被marked错误包裹在代码块中的占位符
  // 处理 <code>MATH-PLACEHOLDER-0-END</code> 或 <pre><code>...</code></pre>
  // 更强的正则：匹配带有属性的code标签，以及多行情况
  let html = content.replace(/<pre[^>]*>\s*<code[^>]*>\s*(MATH-PLACEHOLDER-(\d+)-END)\s*<\/code>\s*<\/pre>/gi, '$1');
  html = html.replace(/<code[^>]*>\s*(MATH-PLACEHOLDER-(\d+)-END)\s*<\/code>/gi, '$1');
  
  // 2. 还原占位符
  return html.replace(/MATH-PLACEHOLDER-(\d+)-END/g, (match, index) => {
    return placeholders[parseInt(index)] || match;
  });
};

const sanitizeNullRuns = (content) => {
  if (typeof content !== 'string') return content
  return content.replace(/(?:null){2,}/g, '')
}

const formatMessageCached = (() => {
  const cache = new WeakMap()
  return (message, field) => {
    if (!message || !field) return ''
    const raw = sanitizeNullRuns(message[field] || '')
    let entry = cache.get(message)
    if (!entry) {
      entry = {}
      cache.set(message, entry)
    }
    const prev = entry[field]
    if (prev && prev.raw === raw) return prev.html
    const html = formatMessage(raw)
    entry[field] = { raw, html }
    return html
  }
})()

/**
 * 缓存深度思考内容的渲染结果
 */
const formatReasoningCached = (() => {
  const cache = new WeakMap()
  return (message) => {
    if (!message || !message.reasoning_content) return ''
    const raw = sanitizeNullRuns(message.reasoning_content)
    let cached = cache.get(message)
    if (cached && cached.raw === raw) return cached.html
    const html = formatMessage(raw)
    cache.set(message, { raw, html })
    return html
  }
})()

/**
 * 获取深度思考内容的字符长度（用于显示统计）
 */
const getReasoningLength = (content) => {
  if (!content) return 0
  // 移除 markdown 标记和 HTML 标签来计算实际文本长度
  const text = content
    .replace(/```[\s\S]*?```/g, '')
    .replace(/`[^`]+`/g, '')
    .replace(/#{1,6}\s+/g, '')
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/\*([^*]+)\*/g, '$1')
    .replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1')
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length
}

const formatMessage = (content) => {
  try {
    if (!content) return '';
    
    // 0. 预处理：修复可能导致解析问题的特殊标记
    let processedContent = content;
    // 匹配四个或更多反引号，替换为三个（标准代码块标记）
    processedContent = processedContent.replace(/`{4,}/g, '```');
    
    // 1. 保护代码块，避免被后续的数学公式识别或文本清理正则破坏
    const codeBlocks = [];
    // 匹配多行代码块 ```...``` (包括未闭合的) 和 行内代码 `...`
    // 注意：使用非贪婪匹配 [\s\S]*? 以支持跨行代码块
    let contentWithCodeProtected = processedContent.replace(/(```[\s\S]*?```|```[\s\S]*$|`[^`\n]+`)/g, (match) => {
      // 自动闭合未闭合的代码块，防止渲染吞噬
      if (match.startsWith('```') && !match.endsWith('```')) {
        match += '\n```';
      }
      const placeholder = `CODE_BLOCK_PLACEHOLDER_${codeBlocks.length}_END`;
      codeBlocks.push(match);
      return placeholder;
    });

    // 2. 在保护了代码块的基础上，进行数学公式预处理和文本清理
    let cleanContent = contentWithCodeProtected;

    // 移除可能包裹公式的 Markdown 代码标记 (反引号) - 此时只会处理非代码块中的反引号
    cleanContent = cleanContent.replace(/`(\$\$[\s\S]+?\$\$)`/g, '$1');
    cleanContent = cleanContent.replace(/`(\\\[[\s\S]+?\\\])`/g, '$1');
    cleanContent = cleanContent.replace(/`(\\\\([\s\S]+?\\\\))`/g, '$1');
    cleanContent = cleanContent.replace(/`(\\\\int(?:\\[\s\S]|[^`])+?)`/g, '$1');
    
    // 清理公式行中的编号
    cleanContent = cleanContent.replace(/^(\s*)(\d+\.\s*)([\u4e00-\u9fa5：:，,。.；;！!？?\s]*?)(\\\\int|\\\\left|\\\\right|\\\\frac|\\\\sqrt|\\\\sum|\\\\lim|\\\\sin|\\\\cos|\\\\tan|\\\\sec|\\\\ln|\\\\log|\\\\exp)/gm, '$1$4');
    
    // 处理HTML标签问题 - 只处理非代码块区域
    cleanContent = cleanContent.replace(/&lt;\s*\/?\s*(li|ul|ol|p|br|div|span|strong|em)\s*&gt;/gi, '');
    cleanContent = cleanContent.replace(/<\s*\/?\s*(li|ul|ol|p|div|span)\s*>/gi, '');
    cleanContent = cleanContent.replace(/<\s*\/?\s*(strong|em)\s*>/gi, '');
    
    // 处理导数公式
    cleanContent = cleanContent.replace(/\(\(([\s\S]*?)\)\)'\s*=/g, "($1)' =");
    
    // 处理其他文本中的多余括号
    cleanContent = cleanContent.replace(/\(la,\s*b\]\)/g, '[a, b]');
    cleanContent = cleanContent.replace(/\(a,\s*b\)\)/g, '[a, b]');
    cleanContent = cleanContent.replace(/\(f\(x\)\)/g, 'f(x)');
    cleanContent = cleanContent.replace(/\(F\(x\)\)/g, 'F(x)');
    cleanContent = cleanContent.replace(/\(F'\(x\)\s*=\s*/g, "F'(x) = ");
    cleanContent = cleanContent.replace(/\s*\(f\(x\)\)\)/g, " f(x)");
    cleanContent = cleanContent.replace(/\(即\s*/g, "即 ");
    cleanContent = cleanContent.replace(/\(\)/g, "");
    cleanContent = cleanContent.replace(/\)\)/g, ")");
    cleanContent = cleanContent.replace(/\[\s*\\\\int/g, '\\\\int');
    cleanContent = cleanContent.replace(/dx\s*\]/g, 'dx');
    
    // 3. 识别并保护数学公式 (生成占位符)
    const mathPlaceholders = [];
    let contentWithMathPlaceholders = renderMathFormula(cleanContent, mathPlaceholders);

    // 4. 还原代码块，以便 marked 可以正确解析并渲染它们
    let contentToParse = contentWithMathPlaceholders;
    codeBlocks.forEach((block, i) => {
      // 使用函数作为替换参数，避免特殊字符（如 $）被错误解析
      contentToParse = contentToParse.replace(`CODE_BLOCK_PLACEHOLDER_${i}_END`, () => block);
    });

    // 5. 使用marked解析Markdown
    let html = marked.parse(contentToParse.trim());
    
    // 6. 还原数学公式 (将占位符替换回KaTeX生成的HTML)
    html = restoreMathFormula(html, mathPlaceholders);
    
    // 7. 后置清理：处理 marked 解析后可能出现的格式问题
    html = html.replace(/\[\s*<p>\s*/g, '<p>');
    html = html.replace(/\s*<\/p>\s*\]/g, '</p>');
    html = html.replace(/<br\s*\/?>\]\s*<\/p>/g, '</p>');
    html = html.replace(/^\s*\[/g, '');
    html = html.replace(/\]\s*$/g, '');
    
    // 8. 安全过滤，防止 XSS
    return DOMPurify.sanitize(html, {
      ADD_ATTR: ['onclick', 'style', 'class'],
      ADD_TAGS: ['button', 'i', 'pre', 'code', 'span']
    });
  } catch (error) {
    console.error('消息格式化错误:', error);
    return content;
  }
}

// 复制消息内容
const copyMessage = (content) => {
  // 从HTML中提取纯文本
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = content
  const plainText = tempDiv.textContent || tempDiv.innerText || ''
  
  // 复制到剪贴板
  navigator.clipboard.writeText(plainText)
    .then(() => {
      // 显示复制成功的反馈
      const button = event.target.closest('.message-copy-button')
      if (button) {
        const originalText = button.innerHTML
        button.innerHTML = '<i class="fas fa-check"></i><span class="copy-text">已复制</span>'
        button.classList.add('copied')
        setTimeout(() => {
          button.innerHTML = originalText
          button.classList.remove('copied')
        }, 2000)
      }
    })
    .catch(err => {
      console.error('复制失败:', err)
    })
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const isModelMenuOpen = ref(false)
const modelMenuRef = ref(null)

const brands = [
  {
    id: 'deepseek',
    name: 'DeepSeek',
    icon: 'fas fa-brain',
    standard: 'deepseek-chat',
    reasoner: 'deepseek-reasoner'
  },
  {
    id: 'doubao',
    name: '豆包',
    icon: 'fas fa-robot',
    standard: 'doubao',
    reasoner: 'doubao-reasoner'
  }
]

// 获取当前选中的品牌
const currentBrand = computed(() => {
  const model = chatStore.selectedModel
  return brands.find(b => model === b.standard || model === b.reasoner) || brands[0]
})

// 切换品牌
const selectBrand = (brand) => {
  if (brand.id === 'doubao') {
    // 豆包强制开启深度思考
    chatStore.setModel(brand.reasoner)
  } else {
    const isReasoning = chatStore.selectedModel.includes('reasoner')
    const newModel = isReasoning ? brand.reasoner : brand.standard
    chatStore.setModel(newModel)
  }
  isModelMenuOpen.value = false
}

/**
 * 切换深度思考模式
 */
const toggleDeepThinking = () => {
  const brand = currentBrand.value
  const isReasoning = chatStore.selectedModel.includes('reasoner')
  
  // 豆包限制：不能关闭深度思考
  if (brand.id === 'doubao' && isReasoning) {
    uiStore.showToast('豆包模型目前仅支持在“深度思考”模式下运行，以提供更优质的回复。')
    return
  }
  
  const newModel = isReasoning ? brand.standard : brand.reasoner
  chatStore.setModel(newModel)
}

/**
 * 处理点击外部关闭模型选择菜单
 */
onMounted(() => {
  const handleClickOutside = (event) => {
    if (modelMenuRef.value && !modelMenuRef.value.contains(event.target)) {
      isModelMenuOpen.value = false
    }
  }
  document.addEventListener('click', handleClickOutside)
  onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
  })
})

/**
 * 自动调整输入框高度
 * @param {Event} event 输入事件
 */
const adjustTextareaHeight = (event) => {
  const textarea = event.target
  textarea.style.height = 'auto'
  textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`
}
</script>

<style scoped>
.chat-page {
  height: 100%;
  overflow: hidden;
  background-color: var(--bg-primary);
}

.chat-container {
  display: flex;
  height: 100%;
  width: 100%;
  max-width: 100%;
  margin: 0;
  background-color: var(--bg-secondary);
  box-shadow: none;
  border-radius: 0;
  overflow: hidden;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
  position: relative;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  background-color: transparent;
  z-index: 10;
}

.chat-header-inner {
  width: 100%;
  max-width: 1200px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-left: 40px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
  align-items: center; /* 居中对齐消息容器 */
  overflow-x: hidden;
}

.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: var(--gray-300);
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: var(--gray-400);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
  padding: 40px 20px;
  width: 100%;
  max-width: 980px; /* 与 input-container 一致 */
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
  opacity: 0.8;
}

.empty-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.empty-description {
  font-size: 16px;
  color: var(--text-secondary);
  text-align: center;
  max-width: 400px;
  line-height: 1.6;
}

.message {
  display: flex;
  gap: 16px;
  margin-bottom: 28px;
  animation: slideUp 0.3s ease-out;
  padding: 0 8px;
  width: 100%;
  max-width: 980px; /* 限制宽度与 input-container 一致 */
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: var(--shadow-md);
  transition: all 0.2s ease;
}

.message-avatar.has-image {
  background: transparent;
}

.message-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.message-avatar:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-lg);
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%; /* 确保占据可用宽度 */
}

.message.user .message-content {
  align-items: flex-end;
}

.message-bubble {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%; /* 默认占满，方便内部长内容触发溢出 */
  max-width: 100%;
}

.message.user .message-bubble {
  align-items: flex-end;
  width: fit-content; /* 用户消息保持紧凑 */
}

.message-text {
  padding: 16px 20px;
  border-radius: var(--border-radius-lg);
  line-height: 1.65;
  word-wrap: break-word;
  word-break: break-word;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  font-size: 16px;
  letter-spacing: 0.2px;
  width: 100%;
  box-sizing: border-box;
  min-width: 0; /* 允许内部元素收缩 */
}

.message-text-raw {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-text:hover {
  box-shadow: var(--shadow-md);
}

.message.assistant .message-text {
  background-color: transparent;
  color: var(--text-primary);
  border: none;
  box-shadow: none;
  padding: 8px 0;
  font-size: 17px;
  line-height: 1.8;
  width: 100%;
  word-break: break-word;
}

.message-text.error-state {
  color: #ef4444;
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 8px;
  padding: 12px 16px !important;
  font-size: 14px;
  margin-top: 8px;
  width: fit-content;
  max-width: 100%;
}

body.dark-mode .message-text.error-state {
  background-color: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
}

.message-text.empty-response {
  color: #6b7280;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px !important;
  font-size: 14px;
  margin-top: 8px;
  width: fit-content;
  max-width: 100%;
}

body.dark-mode .message-text.empty-response {
  color: #94a3b8;
  background-color: rgba(148, 163, 184, 0.1);
  border-color: rgba(148, 163, 184, 0.2);
}

.error-content, .empty-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.error-icon, .info-icon {
  font-size: 14px;
}

.message.assistant .message-text :deep(h1),
.message.assistant .message-text :deep(h2),
.message.assistant .message-text :deep(h3) {
  margin: 24px 0 12px;
  color: var(--text-primary);
  font-weight: 600;
}

.message.assistant .message-text :deep(p) {
  margin-bottom: 12px;
}

.message.assistant .message-text :deep(ul),
.message.assistant .message-text :deep(ol) {
  margin-bottom: 12px;
  padding-left: 24px;
}

.message.assistant .message-text :deep(li) {
  margin-bottom: 6px;
}

.message.user .message-text {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: none;
}

.message-text :deep(pre) {
    background-color: var(--bg-tertiary); /* 在白天模式下是浅色，夜间模式下是深色 */
    color: var(--text-primary); /* 在白天模式下是深色，夜间模式下是浅色 */
    padding: 16px;
    border-radius: var(--border-radius-md);
    overflow-x: auto;
    white-space: pre; /* 确保不换行以触发滚动 */
    word-wrap: normal;
    margin: 12px 0;
    box-shadow: var(--shadow-sm);
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    line-height: 1.5;
    position: relative;
    border: 1px solid var(--border-color); /* 添加边框 */
    max-width: 100%;
    box-sizing: border-box;
  }

.message-text :deep(code) {
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    background-color: transparent; /* 行内代码背景透明 */
    color: var(--text-primary); /* 使用主题文字颜色 */
    padding: 0;
    border-radius: 0;
  }

/* 复制按钮样式 */
.copy-button {
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: rgba(243, 244, 246, 0.8); /* 浅灰色半透明背景 */
  color: #4b5563; /* 中灰色文字 */
  border: 1px solid #d1d5db; /* 浅灰色边框 */
  border-radius: 6px; /* 圆角 */
  padding: 4px 8px; /* 减小内边距，使其更精致 */
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0; /* 默认隐藏 */
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(4px); /* 背景模糊效果 */
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

/* 深色模式下的复制按钮样式 */
body.dark-mode .copy-button {
  background-color: rgba(31, 41, 55, 0.8); /* 深色半透明背景 */
  color: #e5e7eb;
  border-color: #4b5563;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

/* 确保代码块是相对定位的容器 */
.message-text :deep(pre) {
  position: relative !important;
  padding-top: 32px !important; /* 为按钮留出顶部空间 */
}

/* 确保复制按钮样式正确应用 */
.message-text :deep(pre) .copy-button {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
}

/* 鼠标悬停在代码块上时显示按钮 */
.message-text :deep(pre):hover .copy-button {
  opacity: 1;
}

.copy-button:hover {
  background-color: #f9fafb;
  color: var(--primary-color);
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  opacity: 1 !important;
}

body.dark-mode .copy-button:hover {
  background-color: #374151;
  color: #60a5fa;
  border-color: #60a5fa;
}

.copy-button:active {
  transform: translateY(0);
}

.copy-button.copied {
  background-color: #ecfdf5;
  color: #10b981;
  border-color: #10b981;
  opacity: 1 !important;
}

body.dark-mode .copy-button.copied {
  background-color: rgba(6, 78, 59, 0.4);
  color: #34d399;
  border-color: #34d399;
}

@keyframes copiedPulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 8px rgba(74, 222, 128, 0);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(74, 222, 128, 0);
  }
}

.message-text :deep(a) {
  color: var(--primary-color);
  text-decoration: none;
  border-bottom: 1px solid rgba(29, 78, 216, 0.3);
  transition: all 0.2s ease;
}

.message-text :deep(a:hover) {
  border-bottom-color: var(--primary-color);
}

.message.user .message-text :deep(a) {
  color: #a5b4fc;
  border-bottom-color: rgba(165, 180, 252, 0.5);
}

.message.user .message-text :deep(a:hover) {
  color: white;
  border-bottom-color: white;
}

/* 表格样式 */
.message-text :deep(table) {
  display: block;
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  white-space: nowrap; /* 强制不换行以触发滚动，如果列内容很多 */
  border-collapse: separate;
  border-spacing: 0;
  margin: 16px 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.message-text :deep(table) td,
.message-text :deep(table) th {
  white-space: normal; /* 单元格内允许换行，除非列太多导致整体溢出 */
  min-width: 120px; /* 给列一个最小宽度，增加触发滚动的机会 */
}

.message-text :deep(th) {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
  border-right: 1px solid var(--border-color);
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 14px;
}

.message-text :deep(td) {
  border-bottom: 1px solid var(--border-color);
  border-right: 1px solid var(--border-color);
  padding: 12px 16px;
  vertical-align: top;
  font-size: 14px;
}

.message-text :deep(tr:last-child td) {
  border-bottom: none;
}

.message-text :deep(th:last-child),
.message-text :deep(td:last-child) {
  border-right: none;
}

.message-text :deep(tr:nth-child(even)) {
  background-color: rgba(0, 0, 0, 0.02);
}

.message-text :deep(tr:hover) {
  background-color: var(--toolbar-btn-bg);
}

/* 消息复制按钮样式 */
.message-copy-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 8px;
  background-color: transparent; /* 初始背景透明 */
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  width: fit-content;
  margin-top: 12px;
  margin-bottom: 4px;
  opacity: 0;
  visibility: hidden;
}

.message-content:hover .message-copy-button {
  opacity: 1;
  visibility: visible;
  transform: translateY(-1px);
}

.message-copy-button:hover {
  background-color: #3b82f6; /* 鼠标悬浮在按钮上时变为蓝色 */
  color: white;
  border-color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(59, 130, 246, 0.3);
}

.message-copy-button:active {
  transform: translateY(0);
}

/* 复制成功状态样式 */
.message-copy-button.copied {
  background-color: #10b981; /* 复制成功显示绿色 */
  color: white;
  border-color: #10b981;
  animation: copiedPulse 0.6s ease-in-out;
}

/* 深色模式下的消息复制按钮样式 */
body.dark-mode .message-copy-button {
  background-color: transparent;
  color: var(--text-secondary);
  border-color: var(--border-color);
}

body.dark-mode .message-copy-button:hover {
  background-color: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.message {
  position: relative;
  display: flex;
  gap: 16px;
  margin-bottom: 28px; /* 恢复正常的底部边距 */
  animation: slideUp 0.3s ease-out;
  padding: 0 8px;
  width: 100%;
  max-width: 980px; /* 限制宽度与 input-container 一致 */
}

/* 重置message-bubble的相对定位 */
.message-bubble {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-time {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 4px;
  padding: 0 4px;
  align-self: flex-end;
  letter-spacing: 0.2px;
}

.loading-indicator {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: var(--text-secondary);
    font-size: 14px;
    padding: 16px 24px;
    background-color: var(--bg-secondary);
    border-radius: var(--border-radius-md);
    box-shadow: var(--shadow-sm);
    margin: 0 auto 24px;
    max-width: fit-content;
    position: sticky;
    top: 20px;
    z-index: 20;
}

.suggestions-area {
  margin-top: 24px;
  margin-bottom: 8px;
  display: flex;
  justify-content: flex-start;
  animation: fadeIn 0.3s ease-out;
  width: 100%;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 600px;
}

.suggestion-item {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  padding: 10px 20px;
  font-size: 15px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: fit-content;
  text-align: left;
  line-height: 1.4;
}

.suggestion-arrow {
  font-size: 12px;
  color: var(--text-tertiary);
  transition: transform 0.2s ease;
}

.suggestion-item:hover {
  background-color: var(--bg-primary);
  border-color: var(--primary-color);
  transform: translateX(4px);
  box-shadow: var(--shadow-sm);
}

.suggestion-item:hover .suggestion-arrow {
  color: var(--primary-color);
  transform: translateX(2px);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-input-area {
  position: relative;
  padding: 20px 32px 32px;
  background-color: transparent;
  display: flex;
  justify-content: center;
}

.input-container {
  width: 100%;
  max-width: 980px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-container:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.05);
}

.chat-input {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 16px;
  resize: none;
  padding: 8px 4px;
  line-height: 1.6;
  min-height: 24px;
  max-height: 200px;
}

.chat-input:focus {
  outline: none;
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 4px;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-btn {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  font-size: 16px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tool-btn:hover {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.tool-btn-special {
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-btn-special.active {
  background-color: rgba(37, 99, 235, 0.08);
  border-color: #2563eb;
  color: #2563eb;
}

.tool-btn-pill {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-btn-pill:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.model-pill {
  position: relative;
  padding: 0 !important;
  border: none !important;
  background: transparent !important;
}

.model-selector-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 14px;
  background-color: transparent;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  box-shadow: none;
}

.model-selector-trigger:hover, .model-selector-trigger.active {
  background-color: transparent;
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.brand-icon {
  color: var(--primary-color);
  font-size: 14px;
}

.brand-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.toggle-arrow {
  font-size: 10px;
  color: var(--text-tertiary);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-arrow.rotate {
  transform: rotate(180deg);
}

.model-dropdown-menu {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 0;
  width: 240px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  padding: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  z-index: 100;
  transform-origin: bottom left;
  backdrop-filter: blur(10px);
}

.model-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  margin-bottom: 2px;
}

.model-menu-item:last-child {
  margin-bottom: 0;
}

.model-menu-item:hover {
  background-color: var(--bg-secondary);
  transform: scale(1.02);
}

.model-menu-item.active {
  background-color: rgba(37, 99, 235, 0.05);
}

.item-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background-color: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  font-size: 16px;
  transition: all 0.3s ease;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05);
}

.model-menu-item.active .item-icon-wrapper {
  background-color: var(--primary-color);
  color: white;
  box-shadow: 0 4px 8px rgba(37, 99, 235, 0.3);
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.item-desc {
  font-size: 11px;
  color: var(--text-tertiary);
}

.check-icon {
  font-size: 12px;
  color: var(--primary-color);
  animation: checkPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes checkPop {
  from { transform: scale(0); }
  to { transform: scale(1); }
}

/* 菜单动画 */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.95);
}

.stop-btn, .send-btn-new {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stop-icon-wrapper {
  width: 32px;
  height: 32px;
  background-color: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: all 0.2s;
}

.send-icon-wrapper {
  width: 32px;
  height: 32px;
  background-color: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s;
}

.send-btn-new:disabled .send-icon-wrapper {
  border-color: var(--border-color);
  color: var(--text-tertiary);
  cursor: not-allowed;
}

.send-btn-new:not(:disabled):hover .send-icon-wrapper {
  background-color: var(--bg-secondary);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.stop-btn:hover .stop-icon-wrapper {
  background-color: var(--bg-secondary);
  border-color: var(--danger-color);
  color: var(--danger-color);
}

.send-btn {
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

@media (max-width: 1200px) {
  .message-content {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .chat-container {
    border-radius: 0;
    box-shadow: none;
  }
  
  .chat-sidebar {
    display: none;
  }
  
  .messages-container {
    padding: 20px 16px;
  }
  
  .chat-input-area {
    padding: 12px 16px 20px;
  }
  
  .message-content {
    max-width: 100%;
  }
  
  .chat-header {
    padding: 12px 16px;
  }
  
  .sidebar-title {
    font-size: 14px;
  }
  
  .empty-icon {
    font-size: 56px;
    margin-bottom: 16px;
  }
  
  .empty-title {
    font-size: 20px;
  }
  
  .empty-description {
    font-size: 14px;
    max-width: 100%;
  }
  
  .nav-arrows {
    display: none;
  }
  
  .input-toolbar {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .toolbar-left, .toolbar-right {
    gap: 4px;
  }
  
  .model-selector-trigger {
    padding: 6px 10px;
    font-size: 13px;
  }
  
  .send-btn {
    padding: 8px 16px;
    font-size: 13px;
  }
  
  .message-avatar {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }
  
  .message {
    gap: 10px;
    margin-bottom: 20px;
  }
  
  .reasoning-header {
    padding: 6px 10px;
  }
  
  .reasoning-text {
    padding: 12px;
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .messages-container {
    padding: 16px 8px;
  }
  
  .message {
    gap: 8px;
    margin-bottom: 16px;
  }
  
  .message-avatar {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }
  
  .message-text {
    padding: 12px 16px;
    font-size: 13px;
  }
  
  .message-content {
    max-width: 100%;
  }
}

.reasoning-message {
  margin-bottom: 12px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  overflow: hidden;
  max-width: 100%;
  transition: all 0.3s ease;
}

.reasoning-message.collapsed {
  background-color: var(--bg-secondary);
  border-color: var(--border-color);
  width: fit-content;
}

.message.assistant .reasoning-message {
  border-top-left-radius: 12px;
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  background-color: var(--bg-secondary);
  user-select: none;
  transition: background-color 0.2s;
  border-radius: 8px;
}

.reasoning-header:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.reasoning-title-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 400;
}

.reasoning-toggle-icon {
  font-size: 12px;
  color: var(--text-tertiary);
  transition: all 0.2s;
  background: var(--bg-secondary);
  padding: 6px;
  border-radius: 6px;
}

.reasoning-message.collapsed .reasoning-header {
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.reasoning-message:not(.collapsed) .reasoning-toggle-icon {
  transform: none;
}

.reasoning-body {
  padding: 0;
  border-top: 1px solid var(--border-color);
  background-color: transparent;
}

.reasoning-text {
  font-size: 15px;
  color: var(--text-secondary);
  padding: 16px;
  line-height: 1.7;
  word-wrap: break-word;
}

.reasoning-text-raw {
  white-space: pre-wrap;
  word-break: break-word;
}

.reasoning-text :deep(p) {
  margin-bottom: 8px;
}

.reasoning-text :deep(p:last-child) {
  margin-bottom: 0;
}

/* 调整reasoning-text内部的pre样式 */
.reasoning-text :deep(pre) {
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  margin: 8px 0;
  padding: 12px;
}

/* 导航功能样式 */
.nav-arrows {
  position: fixed;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 16px;
  z-index: 100;
  padding: 8px;
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-radius: 30px;
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

body.dark-mode .nav-arrows {
  background-color: rgba(30, 41, 59, 0.8);
}

.nav-arrow-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 1;
}

.nav-arrow-btn:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.nav-arrow-btn:active {
  transform: translateY(0) scale(0.95);
}

.nav-arrow-btn.up {
  margin-bottom: 2px;
}

.nav-arrow-btn.down {
  margin-top: 2px;
}

/* 悬浮的返回底部按钮（现在相对于输入框区域定位） */
.scroll-to-bottom-floating {
  position: absolute;
  top: -25px; /* 稍微再向上一点，确保不遮挡输入框边框 */
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  pointer-events: none;
  animation: slideUpFade 0.3s ease-out;
}

@keyframes slideUpFade {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.scroll-to-bottom-btn {
  background-color: rgba(255, 255, 255, 0.95);
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
  border-radius: 24px;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(8px);
  pointer-events: auto;
  white-space: nowrap;
}

.scroll-to-bottom-btn:hover {
  background-color: rgba(37, 99, 235, 0.1);
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  border-color: var(--primary-color);
}

.scroll-to-bottom-btn:active {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.history-nav-container {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 90;
}

.history-nav-toggle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: none;
  transition: all 0.2s ease;
}

.history-nav-toggle:hover {
  background-color: var(--bg-secondary);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

.history-nav-panel {
  position: absolute;
  top: 50px;
  right: 0;
  width: 300px;
  max-height: 400px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-nav-panel .panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
}

.history-nav-panel .panel-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.history-nav-panel .close-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  font-size: 14px;
}

.history-nav-panel .panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-item {
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: var(--bg-tertiary);
}

.history-item .time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.history-item .text {
  font-size: 13px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-history {
  padding: 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
}

/* 动画效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}

.highlight-message {
  animation: highlight-pulse 2s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  position: relative;
}

@keyframes highlight-pulse {
  0% { 
    background-color: rgba(59, 130, 246, 0.15);
    box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
  }
  30% {
    background-color: rgba(59, 130, 246, 0.25);
    box-shadow: 0 0 0 8px rgba(59, 130, 246, 0.15);
  }
  100% { 
    background-color: transparent;
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0);
  }
}

.typing-cursor::after {
  content: ''; /* 移除光标以匹配图 2 风格 */
  display: inline-block;
  vertical-align: middle;
  animation: blink 1s step-end infinite;
  color: var(--primary-color);
  margin-left: 2px;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 深度思考组件样式优化 */
.reasoning-block {
  margin-bottom: 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-left: 3px solid #6366f1;
  overflow: hidden;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

body.dark-mode .reasoning-block {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
  border-left-color: #818cf8;
}

.reasoning-block:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
}

body.dark-mode .reasoning-block:hover {
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(255, 255, 255, 0.2);
}

.reasoning-block.streaming {
  border-left-color: #8b5cf6;
  background: #f5f3ff;
  animation: reasoning-border-pulse 2s infinite;
}

body.dark-mode .reasoning-block.streaming {
  background: rgba(139, 92, 246, 0.1);
}

.reasoning-block.collapsed {
  cursor: pointer;
  border-left: 2px solid #e2e8f0;
  width: fit-content;
  max-width: 100%;
  border-radius: 18px;
  margin-bottom: 8px;
  background: #f1f5f9;
}

body.dark-mode .reasoning-block.collapsed {
  background: rgba(51, 65, 85, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

.reasoning-block.collapsed:hover {
  background: #e2e8f0;
  border-color: #cbd5e1;
}

body.dark-mode .reasoning-block.collapsed:hover {
  background: rgba(51, 65, 85, 0.8);
}

.reasoning-block.collapsed .reasoning-header {
  padding: 4px 12px;
  gap: 6px;
}

.reasoning-block.collapsed .reasoning-icon-wrapper {
  width: 18px;
  height: 18px;
}

.reasoning-block.collapsed .reasoning-icon {
  font-size: 11px;
}

.reasoning-block.collapsed .reasoning-title {
  font-size: 12px;
}

.reasoning-block.collapsed .reasoning-count {
  font-size: 10px;
}

.reasoning-block.collapsed .toggle-icon {
  font-size: 9px;
}

@keyframes reasoning-border-pulse {
  0% { border-left-color: #6366f1; }
  50% { border-left-color: #8b5cf6; }
  100% { border-left-color: #6366f1; }
}

.reasoning-preview {
  padding: 8px 16px 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  border-top: 1px solid #f1f5f9;
}

body.dark-mode .reasoning-preview {
  color: #94a3b8;
  border-top-color: rgba(255, 255, 255, 0.05);
}

.reasoning-preview .preview-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reasoning-block .reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;
}

.reasoning-block .reasoning-header:hover {
  background: rgba(0, 0, 0, 0.02);
}

.reasoning-block .header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reasoning-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #6366f1;
}

.reasoning-icon {
  font-size: 14px;
}

.header-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reasoning-title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

body.dark-mode .reasoning-title {
  color: #e2e8f0;
}

.reasoning-subtitle {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}

body.dark-mode .reasoning-subtitle {
  color: #64748b;
}

.reasoning-count {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}

body.dark-mode .reasoning-count {
  color: #64748b;
}

.reasoning-block .header-right {
  display: flex;
  align-items: center;
}

.reasoning-block .toggle-icon {
  font-size: 11px;
  color: #94a3b8;
  transition: transform 0.3s ease;
}

.reasoning-block .reasoning-header:hover .toggle-icon {
  color: #6366f1;
}

.reasoning-block.collapsed .toggle-icon {
  transform: rotate(0deg);
}

.reasoning-block:not(.collapsed) .toggle-icon {
  transform: rotate(180deg);
}

.reasoning-block .reasoning-content {
  padding: 4px 16px 16px;
  font-size: 13.5px;
  color: #334155;
  line-height: 1.6;
  border-top: 1px dashed #e2e8f0;
}

body.dark-mode .reasoning-block .reasoning-content {
  color: #cbd5e1;
  border-top-color: rgba(255, 255, 255, 0.1);
}

.reasoning-block .reasoning-content .markdown-body {
  background-color: transparent;
  font-size: 13.5px;
  color: #334155;
}

body.dark-mode .reasoning-block .reasoning-content .markdown-body {
  color: #cbd5e1;
}

.reasoning-block .reasoning-content :deep(pre) {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px;
  margin: 10px 0;
}

body.dark-mode .reasoning-block .reasoning-content :deep(pre) {
  background: rgba(15, 23, 42, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

.reasoning-block .reasoning-content :deep(pre code) {
  color: #1e293b;
  font-size: 12px;
}

body.dark-mode .reasoning-block .reasoning-content :deep(pre code) {
  color: #e2e8f0;
}

/* 内容区域动画 */
.reasoning-slide-enter-active,
.reasoning-slide-leave-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.reasoning-slide-enter-from {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
}

.reasoning-slide-enter-to {
  opacity: 1;
  max-height: 2000px;
  transform: translateY(0);
}

.reasoning-slide-leave-from {
  opacity: 1;
  max-height: 2000px;
  transform: translateY(0);
}

.reasoning-slide-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
}

.reasoning-block .reasoning-content :deep(table) {
  display: block;
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  white-space: nowrap;
  border-collapse: collapse;
  margin: 12px 0;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

body.dark-mode .reasoning-block .reasoning-content :deep(table) {
  border-color: rgba(255, 255, 255, 0.1);
}

.reasoning-block .reasoning-content :deep(table th),
.reasoning-block .reasoning-content :deep(table td) {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
}

body.dark-mode .reasoning-block .reasoning-content :deep(table th),
body.dark-mode .reasoning-block .reasoning-content :deep(table td) {
  border-color: rgba(255, 255, 255, 0.1);
}

.reasoning-block .reasoning-content :deep(table th) {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
}

body.dark-mode .reasoning-block .reasoning-content :deep(table th) {
  background: rgba(51, 65, 85, 0.5);
  color: #e2e8f0;
}

.reasoning-block .reasoning-content .markdown-body p {
  margin-bottom: 10px;
}

.reasoning-block .reasoning-content .markdown-body p:last-child {
  margin-bottom: 0;
}

.reasoning-block .reasoning-content :deep(strong) {
  color: #1e293b;
  font-weight: 600;
}

body.dark-mode .reasoning-block .reasoning-content :deep(strong) {
  color: #f1f5f9;
}

.reasoning-block .reasoning-content :deep(em) {
  color: #64748b;
  font-style: italic;
}

body.dark-mode .reasoning-block .reasoning-content :deep(em) {
  color: #94a3b8;
}

.reasoning-block .reasoning-content :deep(ul),
.reasoning-block .reasoning-content :deep(ol) {
  margin: 10px 0;
  padding-left: 24px;
}

.reasoning-block .reasoning-content :deep(li) {
  margin-bottom: 6px;
  color: #475569;
}

body.dark-mode .reasoning-block .reasoning-content :deep(li) {
  color: #cbd5e1;
}
@media (max-width: 768px) {
  .chat-header {
    padding: 12px 16px;
  }
  
  .chat-header-inner {
    margin-left: 0;
    width: 100%;
    padding-right: 40px; /* Make space for history toggle */
  }

  .messages-container {
    padding: 16px;
  }
  
  .message {
    max-width: 100%;
    padding: 0;
    margin-bottom: 20px;
  }

  .message-content {
    max-width: 100%;
  }

  /* 移动端导航箭头适配 */
  .nav-arrows {
    right: 12px;
    padding: 6px;
    gap: 12px;
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(8px);
  }
  
  body.dark-mode .nav-arrows {
    background-color: rgba(30, 41, 59, 0.9);
  }
  
  .nav-arrow-btn {
    width: 36px;
    height: 36px;
  }
  
  /* 移动端历史记录面板适配 */
  .history-nav-container {
    top: 16px; /* Move to header area */
    right: 16px;
    z-index: 100;
  }
  
  .history-nav-toggle {
    width: 32px;
    height: 32px;
    background-color: var(--bg-secondary);
  }
  
  .history-nav-panel {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    max-height: 100vh;
    border-radius: 0;
    z-index: 2000;
    display: flex;
    flex-direction: column;
  }

  .history-nav-panel .panel-content {
    flex: 1;
    overflow-y: auto;
  }

  /* 输入框区域适配 */
  .chat-input-area {
    padding: 12px;
  }

  .input-container {
    padding: 8px;
  }

  /* 调整深度思考块在移动端的显示 */
  .reasoning-block {
    margin-left: 0;
    margin-right: 0;
  }
}
</style>
