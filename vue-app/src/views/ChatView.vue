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
                  :class="{ 'streaming': message.isStreaming && !message.content }"
                >
                  <div
                    class="reasoning-header"
                    @click="toggleReasoning(message)"
                  >
                    <div class="header-left">
                      <i class="fas fa-brain" />
                      <span>深度思考</span>
                    </div>
                    <i 
                      class="fas toggle-icon"
                      :class="message.isReasoningCollapsed ? 'fa-chevron-right' : 'fa-chevron-down'" 
                    />
                  </div>
                  <div 
                    v-show="!message.isReasoningCollapsed" 
                    class="reasoning-content"
                  >
                    <div 
                      class="markdown-body"
                      v-html="formatMessage(message.reasoning_content)" 
                    />
                  </div>
                </div>

                <!-- eslint-disable-next-line vue/no-v-html -->
                <div
                  v-if="message.content && !message.isStreaming"
                  class="message-text"
                  v-html="formatMessageCached(message, 'content')"
                />
                <div
                  v-else-if="message.content && message.isStreaming"
                  class="message-text"
                  v-html="formatMessage(sanitizeNullRuns(message.content))"
                />
                <!-- 如果没有内容但有reasoning_content，显示占位符或仅显示reasoning -->
                <div
                  v-else-if="!message.reasoning_content"
                  class="message-text"
                >
                  <span class="typing-cursor" />
                </div>
              </div>
              <div class="message-time">
                {{ formatTime(message.timestamp) }}
              </div>
            </div>

            <!-- 用户头像 - 移除或改为在user角色下不显示以匹配图2 -->
            <!-- 如果需要保留头像但放在右侧，可以在这里添加 v-if="message.role === 'user'" -->
            
            <!-- 复制按钮 - 位于消息容器外的左下角 -->
            <button 
              class="message-copy-button" 
              title="复制这条消息"
              @click="copyMessage(message.content)"
            >
              <i class="fas fa-copy" />
              <span class="copy-text">复制</span>
            </button>
          </div>
        </div>
        
        <!-- 建议问题区域 -->
        <div 
          v-if="chatStore.suggestions && chatStore.suggestions.length > 0 && !chatStore.isLoading" 
          class="suggestions-area"
        >
          <div class="suggestions-list">
            <button 
              v-for="(suggestion, index) in chatStore.suggestions" 
              :key="index"
              class="suggestion-item"
              @click="sendSuggestion(suggestion)"
            >
              {{ suggestion }}
            </button>
          </div>
        </div>
        
        <div class="chat-input-area">
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
                <button 
                  class="tool-btn" 
                  title="上传附件"
                >
                  <i class="fas fa-paperclip" />
                </button>
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
                <button 
                  class="tool-btn" 
                  title="截图"
                >
                  <i class="fas fa-cut" />
                </button>
                <button 
                  class="tool-btn" 
                  title="语音通话"
                >
                  <i class="fas fa-phone" />
                </button>
                <button 
                  class="tool-btn" 
                  title="语音输入"
                >
                  <i class="fas fa-microphone" />
                </button>
                
                <div class="toolbar-divider" />
                
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
    </div>
  </div>
</template>

<script setup>
import DOMPurify from 'dompurify'
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { API_CONFIG } from '@/config/api'

const chatStore = useChatStore()
const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const inputMessage = ref('')
const messagesContainer = ref(null)
const isPinnedToBottom = ref(true)
const SCROLL_BOTTOM_THRESHOLD_PX = 40
let autoScrollScheduled = false

const userAvatarUrl = ref(null) // 用于消息列表头像

/**
 * 解析并返回消息头像图片地址；无可用图片时返回 `null` 以回退到默认图标
 */
const deepseekAvatarUrl = new URL('../../static/image/deepseek-image.png', import.meta.url).href
const doubaoAvatarUrl = new URL('../../static/image/doubao-imge.png', import.meta.url).href

const getMessageAvatarSrc = (message) => {
  if (!message) return null
  if (message.role === 'user') {
    return userAvatarUrl.value || null
  }
  const model = String(message.model || chatStore.selectedModel || '').toLowerCase()
  if (model.includes('deepseek')) return deepseekAvatarUrl
  if (model.includes('doubao')) return doubaoAvatarUrl
  return null
}

// 复制代码到剪贴板 - 改为全局函数，供内联事件调用
window.copyCodeBlock = (element) => {
  const code = element.previousElementSibling.textContent
  const button = element
  navigator.clipboard.writeText(code)
    .then(() => {
      // 显示复制成功的反馈
      const originalText = button.textContent
      button.textContent = '已复制!'
      button.classList.add('copied')
      setTimeout(() => {
        button.textContent = originalText
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
  // 在pre标签内添加复制按钮
  return originalResult.replace('<pre', '<pre style="position: relative">')
    .replace('</pre>', '<button class="copy-button" onclick="copyCodeBlock(this)">复制</button></pre>')
}

// 配置marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  breaks: true,
  gfm: true,
  renderer: renderer // 使用自定义渲染器
})

const currentSessionTitle = computed(() => {
  return chatStore.currentSession?.title || '新对话'
})

const scrollToBottom = (behavior = 'smooth') => {
  if (messagesContainer.value) {
    // 优先使用 scrollTop 直接设置，这样最可靠
    if (behavior === 'auto') {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    } else {
      messagesContainer.value.scrollTo({
        top: messagesContainer.value.scrollHeight,
        behavior: behavior
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

const handleMessagesScroll = () => {
  updatePinnedState()
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
    // 第三次尝试：长延迟，确保公式渲染
    setTimeout(() => scrollToBottom('auto'), delay + 200)
    setTimeout(() => updatePinnedState(), delay + 250)
  })
}

onMounted(async () => {
  // 0. 确保会话列表已加载
  if (chatStore.sessions.length === 0) {
    await chatStore.fetchSessions()
  }

  // 1. 如果 URL 中有会话 ID，优先加载该会话
  const querySessionId = route.query.session
  if (querySessionId) {
    chatStore.currentSessionId = querySessionId
    await chatStore.fetchSessionMessages(querySessionId)
    triggerScrollToBottom(100)
    return
  }

  // 2. 如果没有当前会话，但有会话列表，加载第一个
  if (!chatStore.currentSessionId && chatStore.sessions.length > 0) {
    chatStore.currentSessionId = chatStore.sessions[0].id
    await chatStore.fetchSessionMessages(chatStore.sessions[0].id)
    triggerScrollToBottom(100)
    // 更新 URL
    router.replace(`/chat?session=${chatStore.sessions[0].id}`)
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

// 监听 URL 参数变化 (例如点击侧边栏时)
watch(
  () => route.query.session,
  async (newSessionId) => {
    if (newSessionId) {
      await chatStore.fetchSessionMessages(newSessionId)
      triggerScrollToBottom(100)
    } else {
      // 如果没有会话 ID，可能回到了 /chat 根路径，尝试加载最近的会话或显示空状态
      if (chatStore.sessions.length > 0) {
        chatStore.currentSessionId = chatStore.sessions[0].id
        await chatStore.fetchSessionMessages(chatStore.sessions[0].id)
        triggerScrollToBottom(100)
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
      // 只有当路由中的 session 与 newId 不一致时才加载，避免与路由监听冲突
      if (newId !== route.query.session) {
        await chatStore.fetchSessionMessages(newId)
        triggerScrollToBottom(100)
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

onUnmounted(() => {
  if (userAvatarUrl.value) {
    URL.revokeObjectURL(userAvatarUrl.value)
    userAvatarUrl.value = null
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
  
  // 如果没有当前会话，先创建一个
  if (!chatStore.currentSessionId) {
    await createNewSession()
  }
  
  const message = inputMessage.value.trim()
  inputMessage.value = ''
  
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
  processedContent = processedContent.replace(generalIntegralRegex, (match, offset, string) => {
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
  
  // 9. 清理HTML标签 (在占位符替换后做)
  processedContent = processedContent.replace(/<br\s*\/?>/g, ' ');
  
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

const formatMessage = (content) => {
  try {
    // 1. 先清理原始内容中的问题
    let cleanContent = content;

    // 0. 预处理：移除可能包裹公式的 Markdown 代码标记 (反引号)
    // AI 有时会输出 `\( x^2 \)` 导致公式被渲染为代码块
    // 移除包裹 $$ ... $$ 的反引号
    cleanContent = cleanContent.replace(/`(\$\$[\s\S]+?\$\$)`/g, '$1');
    // 移除包裹 \[ ... \] 的反引号
    cleanContent = cleanContent.replace(/`(\\\[[\s\S]+?\\\])`/g, '$1');
    // 移除包裹 \( ... \) 的反引号
    cleanContent = cleanContent.replace(/`(\\\([\s\S]+?\\\))`/g, '$1');
    // 移除包裹 \int ... 的反引号 (需要匹配到对应的结束反引号)
    // 匹配 `\int ... `，确保内部不包含反引号
    cleanContent = cleanContent.replace(/`(\\int(?:\\[\s\S]|[^`])+?)`/g, '$1');
    
    // 预处理：清理公式行中的编号（如 "5."、"1." 等）
    // 只处理包含LaTeX命令的行，避免误删正常文本
    // 匹配模式：行首编号 + 可选中文 + LaTeX命令
    // 注意：这里只清理行首的编号，不清理公式内部的编号
    cleanContent = cleanContent.replace(/^(\s*)(\d+\.\s*)([\u4e00-\u9fa5：:，,。.；;！!？?\s]*?)(\\int|\\left|\\right|\\frac|\\sqrt|\\sum|\\lim|\\sin|\\cos|\\tan|\\sec|\\ln|\\log|\\exp)/gm, '$1$4');
    
    // 处理列表符号和多余括号
    cleanContent = cleanContent.replace(/^\s*\s*\(/g, '');
    cleanContent = cleanContent.replace(/\)\s*$/g, '');
    
    // 处理HTML标签问题 - 更强的正则，包含转义字符
    cleanContent = cleanContent.replace(/&lt;\s*\/?\s*(li|ul|ol|p|br|div|span|strong|em)\s*&gt;/gi, '');
    cleanContent = cleanContent.replace(/<\s*\/?\s*(li|ul|ol|p|div|span)\s*>/gi, '');
    // 处理带有空格的标签，如 < br >
    cleanContent = cleanContent.replace(/<\s*br\s*\/?\s*>/gi, ' ');
    // 强力清除 strong 和 em 标签及其空格变体 (如 < strong >)
    cleanContent = cleanContent.replace(/<\s*\/?\s*(strong|em)\s*>/gi, '');
    
    // // 处理数学符号问题：将错误显示的符号替换为正确的
    // cleanContent = cleanContent.replace(/目/g, '≠');
    
    // 移除公式周围的双重括号 ((...)) -> ...
    // 使用 [\s\S]*? 非贪婪匹配任意字符(包括换行)，直到遇到 ))
    cleanContent = cleanContent.replace(/\(\(([\s\S]*?)\)\)/g, '$1');
    // 暴力修复：直接将 (( 替换为 (，将 )) 替换为 )
    cleanContent = cleanContent.replace(/\(\(/g, '(');
    cleanContent = cleanContent.replace(/\)\)/g, ')');

    // 处理导数公式 ((...)' = ...)
    cleanContent = cleanContent.replace(/\(\(([\s\S]*?)\)\)'\s*=/g, "($1)' =");
    
    // 去除公式周围的多余括号 ( \int ... ) -> \int ...
    cleanContent = cleanContent.replace(/\(\s*\\int/g, '\\int');
    // 去除结尾的多余双括号 (针对 ...)) 的情况)
    cleanContent = cleanContent.replace(/\)\)\s*$/gm, ')');
    // 尝试去除单独行的右括号
    cleanContent = cleanContent.replace(/^\s*\)\s*$/gm, '');
    // 尝试去除单独行的左括号
    cleanContent = cleanContent.replace(/^\s*\(\s*$/gm, '');
    
    // 处理行尾的多余方括号 ] (通常是AI生成的格式错误)
    // 匹配非转义的 ] 出现在行尾的情况
    cleanContent = cleanContent.replace(/([^\\])\]\s*$/gm, '$1');
    // 处理行首的多余方括号 [
    cleanContent = cleanContent.replace(/^\s*\[/gm, '');

    // 处理区间表示中的错误：(la, b]) → [a, b]
    cleanContent = cleanContent.replace(/\(la,\s*b\]\)/g, '[a, b]');
    cleanContent = cleanContent.replace(/\(a,\s*b\)\)/g, '[a, b]');
    
    // 处理函数表示中的多余括号：(f(x)) → f(x)
    cleanContent = cleanContent.replace(/\(f\(x\)\)/g, 'f(x)');
    cleanContent = cleanContent.replace(/\(F\(x\)\)/g, 'F(x)');
    
    // 处理导数和等式中的多余括号
    cleanContent = cleanContent.replace(/\(F'\(x\)\s*=\s*/g, "F'(x) = ");
    cleanContent = cleanContent.replace(/\s*\(f\(x\)\)\)/g, " f(x)");
    
    // 处理文本中的多余括号
    cleanContent = cleanContent.replace(/\(即\s*/g, "即 ");
    cleanContent = cleanContent.replace(/\(\)/g, "");
    cleanContent = cleanContent.replace(/\)\)/g, ")");
    
    // 处理公式周围的多余括号和方括号
    cleanContent = cleanContent.replace(/\[\s*\\int/g, '\\int');
    cleanContent = cleanContent.replace(/dx\s*\]/g, 'dx');
    
    // 2. 先识别并保护数学公式 (生成占位符)
    const placeholders = [];
    let contentWithPlaceholders = renderMathFormula(cleanContent, placeholders);

    // 3. 使用marked解析Markdown (此时公式已被占位符保护，不会被marked破坏)
    let html = marked.parse(contentWithPlaceholders);
    
    // 4. 还原数学公式 (将占位符替换回KaTeX生成的HTML)
    html = restoreMathFormula(html, placeholders);
    
    // 5. 清理多余的HTML标签和格式问题
    html = html.replace(/\[\s*<p>\s*/g, '<p>');
    html = html.replace(/\s*<\/p>\s*\]/g, '</p>');
    html = html.replace(/<br\s*\/?>\]\s*<\/p>/g, '</p>');
    html = html.replace(/<br\s*\/?>/g, ' ');
    
    // 6. 处理公式周围的多余字符 (仅处理行首行尾的方括号，避免误删数学符号)
    html = html.replace(/^\s*\[/g, '');
    html = html.replace(/\]\s*$/g, '');
    
    // 7. 安全过滤，防止 XSS
    return DOMPurify.sanitize(html, {
      ADD_ATTR: ['onclick', 'style'], // 允许 onclick 和 style 属性，因为我们自定义了复制按钮
      ADD_TAGS: ['button', 'i'] // 允许 button 和 i 标签
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

const formatSessionDate = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diffTime = Math.abs(now - date)
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) {
    return '今天 '
  } else if (diffDays === 1) {
    return '昨天 '
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
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
  const isReasoning = chatStore.selectedModel.includes('reasoner')
  const newModel = isReasoning ? brand.reasoner : brand.standard
  chatStore.setModel(newModel)
  isModelMenuOpen.value = false
}

/**
 * 切换深度思考模式
 */
const toggleDeepThinking = () => {
  const brand = currentBrand.value
  const isReasoning = chatStore.selectedModel.includes('reasoner')
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
  height: 100vh;
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
}

.message.user .message-content {
  align-items: flex-end;
}

.message-bubble {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: fit-content;
}

.message.user .message-bubble {
  align-items: flex-end;
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
  max-width: 100%;
  overflow-x: auto; /* 处理代码、表格等内容的横向溢出 */
  box-sizing: border-box;
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
  max-width: 100%;
  word-break: break-word;
  overflow-x: auto; /* 处理代码、表格等内容的横向溢出 */
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
  background-color: rgba(255, 255, 255, 0.9); /* 半透明白色背景 */
  color: var(--text-primary);
  border: 1px solid var(--border-color); /* 添加边框 */
  border-radius: var(--border-radius-md); /* 使用更大的圆角 */
  padding: 8px 16px; /* 增加内边距 */
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  z-index: 100;
  /* 确保按钮位于右上角 */
  margin: 0;
  transform: none;
  box-sizing: border-box;
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 深色模式下的复制按钮样式 */
body.dark-mode .copy-button {
  background-color: rgba(31, 41, 55, 0.95); /* 深色背景 */
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 确保代码块是相对定位的容器 */
.message-text :deep(pre) {
  position: relative !important;
}

/* 确保复制按钮样式正确应用 */
.message-text :deep(pre) .copy-button {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 100;
}

.message-text :deep(pre):hover .copy-button {
  opacity: 1;
}

.copy-button:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  opacity: 1 !important;
}

.copy-button:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
}

.copy-button.copied {
  background-color: var(--success-color);
  color: white;
  border-color: var(--success-color);
  animation: copiedPulse 0.6s ease-in-out;
  box-shadow: 0 0 0 3px rgba(74, 222, 128, 0.2);
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
  width: 100%;
  max-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin: 16px 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow-x: auto;
  display: block; /* 改为 block 以便 overflow-x 生效 */
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
  position: absolute;
  bottom: -24px;
  background-color: rgba(255, 255, 255, 0.9); /* 半透明白色背景 */
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md); /* 使用更大的圆角 */
  padding: 8px 16px; /* 增加内边距 */
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10;
  box-shadow: var(--shadow-sm);
}

/* 深色模式下的消息复制按钮样式 */
body.dark-mode .message-copy-button {
  background-color: rgba(31, 41, 55, 0.95); /* 深色背景 */
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* AI消息（左对齐）的复制按钮在下方靠左 */
.message.assistant .message-copy-button {
  left: 0;
}

/* 用户消息（右对齐）的复制按钮在下方靠右 */
.message.user .message-copy-button {
  right: 0;
}

.message:hover .message-copy-button {
  opacity: 1;
}

.message-copy-button:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  opacity: 1 !important;
}

.message-copy-button:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
}

/* 复制成功状态样式 */
.message-copy-button.copied {
  background-color: var(--success-color);
  color: white;
  border-color: var(--success-color);
  animation: copiedPulse 0.6s ease-in-out;
  box-shadow: 0 0 0 3px rgba(74, 222, 128, 0.2);
}

.message {
  position: relative;
  display: flex;
  gap: 16px;
  margin-bottom: 40px; /* 增加底部边距，为复制按钮留出空间 */
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
  padding: 0 32px 12px;
  display: flex;
  justify-content: center;
  animation: fadeIn 0.3s ease-out;
}

.suggestions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-width: 980px;
  width: 100%;
}

.suggestion-item {
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 6px 16px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.suggestion-item:hover {
  background-color: var(--toolbar-btn-bg);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-input-area {
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
  background-color: var(--bg-secondary);
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
  background-color: #ebf5ff;
  border-color: #bfdbfe;
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
  background-color: var(--bg-secondary);
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
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.model-selector-trigger:hover, .model-selector-trigger.active {
  background-color: var(--bg-primary);
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

.toolbar-divider {
  width: 1px;
  height: 20px;
  background-color: var(--border-color);
  margin: 0 4px;
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
  background-color: var(--text-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.send-icon-wrapper {
  width: 32px;
  height: 32px;
  background-color: var(--text-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: opacity 0.2s;
}

.send-btn-new:disabled .send-icon-wrapper {
  background-color: var(--border-color);
  cursor: not-allowed;
}

.send-btn-new:not(:disabled):hover .send-icon-wrapper {
  opacity: 0.8;
}

.stop-btn:hover .stop-icon-wrapper {
  opacity: 0.8;
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
    max-width: 85%;
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
    padding: 24px 16px;
  }
  
  .chat-input-area {
    padding: 16px 16px;
  }
  
  .message-content {
    max-width: 90%;
  }
  
  .chat-header {
    padding: 16px 16px;
  }
  
  .sidebar-title {
    font-size: 14px;
  }
  
  .empty-icon {
    font-size: 64px;
  }
  
  .empty-title {
    font-size: 24px;
  }
  
  .empty-description {
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
    max-width: 95%;
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
  background-color: white;
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

.typing-cursor::after {
  content: '▋';
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

.reasoning-block {
  margin-bottom: 12px;
  border-radius: 8px;
  background-color: var(--bg-tertiary);
  border-left: 3px solid var(--border-color);
  overflow: hidden;
}

.reasoning-block.streaming {
  border-left-color: var(--primary-color);
}

.reasoning-block .reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  background-color: var(--bg-secondary);
  font-size: 13px;
  color: var(--text-secondary);
  user-select: none;
  border-radius: 0;
}

.reasoning-block .reasoning-header:hover {
  background-color: var(--bg-hover);
}

.reasoning-block .header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reasoning-block .reasoning-header i {
  font-size: 12px;
}

.reasoning-block .reasoning-content {
  padding: 12px;
  background-color: var(--bg-primary);
  font-size: 13px;
  color: var(--text-secondary);
  border-top: 1px solid var(--border-color);
  line-height: 1.6;
}

.reasoning-block .reasoning-content .markdown-body {
  background-color: transparent;
  font-size: 13px;
  color: var(--text-secondary);
}

.reasoning-block .reasoning-content .markdown-body p {
  margin-bottom: 8px;
}

.reasoning-block .reasoning-content .markdown-body p:last-child {
  margin-bottom: 0;
}
</style>
