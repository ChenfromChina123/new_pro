<template>
  <div class="req-manager">
    <div class="req-sidebar">
      <div class="sidebar-header">
        <span>需求文档</span>
        <div class="header-actions">
          <button
            class="action-btn smart-btn"
            title="智能生成"
            @click="openSmartGenerate"
          >
            <i class="fas fa-magic" />
          </button>
          <button
            class="action-btn add-btn"
            title="新建文档"
            @click="createNew"
          >
            +
          </button>
        </div>
      </div>
      <div class="doc-list">
        <div 
          v-for="doc in docs" 
          :key="doc.id" 
          class="doc-item"
          :class="{ active: currentDoc?.id === doc.id }"
          @click="selectDoc(doc)"
        >
          <div class="doc-title">
            {{ doc.title || '未命名文档' }}
          </div>
          <div class="doc-date">
            {{ formatDate(doc.updatedAt) }}
          </div>
        </div>
      </div>
    </div>
    <div
      v-if="currentDoc"
      class="req-editor"
    >
      <div class="editor-header">
        <div class="title-section">
          <input
            v-model="currentDoc.title"
            placeholder="文档标题"
            class="title-input"
            :disabled="isGenerating"
          >
          <div
            v-if="isGenerating"
            class="generating-badge"
          >
            <i class="fas fa-spinner fa-spin" /> AI 正在撰写中...
            <button
              class="stop-btn"
              title="停止生成"
              @click="stopGeneration"
            >
              <i class="fas fa-stop-circle" />
            </button>
          </div>
        </div>
        <div class="actions">
          <button
            class="btn"
            :class="{ active: !isPreview }"
            title="编辑源码"
            @click="isPreview = false"
          >
            <i class="fas fa-code" />
          </button>
          <button
            class="btn"
            :class="{ active: isPreview }"
            title="预览文档"
            @click="isPreview = true"
          >
            <i class="fas fa-eye" />
          </button>
          <span class="divider" />
          <button
            class="btn primary"
            :disabled="isGenerating"
            @click="saveDoc"
          >
            保存
          </button>
          <button
            class="btn"
            :disabled="isGenerating"
            @click="showHistory"
          >
            历史版本
          </button>
        </div>
      </div>
      
      <div class="editor-body">
        <textarea
          v-if="!isPreview"
          ref="editorTextareaRef"
          v-model="currentDoc.content"
          class="content-editor"
          placeholder="在此输入需求文档内容..."
        />
        <div 
          v-else 
          ref="previewContainerRef" 
          class="preview-container markdown-body"
          :class="{ 'is-streaming': isGenerating }"
        >
          <div
            v-if="currentDoc.content"
            class="content-wrapper"
          >
            <div v-html="renderMarkdown(currentDoc.content)" />
            <span
              v-if="isGenerating"
              class="streaming-cursor"
            >
              <i class="fas fa-pencil-alt" />
            </span>
          </div>
          <div
            v-else
            class="empty-preview"
          >
            {{ isGenerating ? '正在初始化文档结构...' : '暂无内容' }}
          </div>
        </div>
      </div>
    </div>
    <div
      v-else
      class="empty-state"
    >
      <div class="empty-content">
        <i class="fas fa-file-alt empty-icon" />
        <h3>开始您的需求分析</h3>
        <p class="empty-desc">
          您可以选择下方演示项目快速了解，或使用 AI 智能生成新文档。
        </p>
        
        <div class="guide-actions">
          <button
            class="guide-btn primary"
            @click="openSmartGenerate"
          >
            <i class="fas fa-magic" /> AI 智能生成需求
          </button>
          <button
            class="guide-btn"
            @click="createNew"
          >
            <i class="fas fa-plus" /> 手动新建文档
          </button>
        </div>

        <div class="demo-section">
          <p class="demo-title">
            演示项目推荐：
          </p>
          <div class="demo-grid">
            <div 
              v-for="demo in DEMO_PROJECTS" 
              :key="demo.id" 
              class="demo-card"
              @click="loadDemo(demo)"
            >
              <div class="demo-card-title">
                {{ demo.title }}
              </div>
              <div class="demo-card-desc">
                {{ demo.content.substring(0, 60) }}...
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- History Dialog (Simplified) -->
    <div
      v-if="showHistoryDialog"
      class="modal-overlay"
      @click="showHistoryDialog = false"
    >
      <div
        class="modal-content"
        @click.stop
      >
        <h3>版本历史</h3>
        <div class="history-list">
          <div
            v-for="h in history"
            :key="h.id"
            class="history-item"
          >
            <span>v{{ h.version }}</span>
            <span>{{ formatDate(h.createdAt) }}</span>
            <button @click="restoreVersion(h)">
              查看
            </button>
          </div>
        </div>
        <button @click="showHistoryDialog = false">
          关闭
        </button>
      </div>
    </div>

    <!-- Smart Generate Modal -->
    <div
      v-if="showSmartModal"
      class="modal-overlay"
      @click="closeSmartModal"
    >
      <div
        class="modal-content smart-modal"
        @click.stop
      >
        <div class="modal-header">
          <h3>智能需求生成</h3>
          <button
            class="close-btn"
            @click="closeSmartModal"
          >
            &times;
          </button>
        </div>

        <!-- Step 1: User Idea Input -->
        <div
          v-if="smartStep === 1"
          class="smart-step"
        >
          <p class="step-desc">
            请简要描述您的需求想法，AI将为您识别领域并生成细化问题。
          </p>
          <textarea
            v-model="userIdea"
            class="idea-input"
            placeholder="例如：我想做一个类似美团的社区团购系统，包含团长管理、商品秒杀和订单配送功能..."
            rows="5"
          />
          <div class="modal-footer">
            <button
              class="btn primary"
              :disabled="!userIdea.trim() || isGenerating"
              @click="generateQuestions"
            >
              {{ isGenerating ? '识别中...' : '下一步：识别领域与生成问题' }}
            </button>
          </div>
        </div>

        <!-- Step 2: Answering Questions -->
        <div
          v-else-if="smartStep === 2"
          class="smart-step"
        >
          <p class="step-desc">
            识别到领域：<span class="domain-tag">{{ smartDomain }}</span>
          </p>
          <div class="questions-container">
            <div
              v-for="(q, index) in smartQuestions"
              :key="index"
              class="question-item"
            >
              <p class="question-title">
                {{ index + 1 }}. {{ q.title }}
              </p>
              
              <div
                v-if="q.type === 'radio'"
                class="options"
              >
                <label
                  v-for="opt in q.options"
                  :key="opt.value"
                  class="option-label"
                >
                  <input
                    v-model="userAnswers[index]"
                    type="radio"
                    :name="'q'+index"
                    :value="opt.value"
                  >
                  {{ opt.label }}
                </label>
              </div>

              <div
                v-else-if="q.type === 'checkbox'"
                class="options"
              >
                <label
                  v-for="opt in q.options"
                  :key="opt.value"
                  class="option-label"
                >
                  <input
                    v-model="userAnswers[index]"
                    type="checkbox"
                    :value="opt.value"
                  >
                  {{ opt.label }}
                </label>
              </div>

              <!-- 其他补充内容输入框 -->
              <div class="other-supplement">
                <p class="supplement-label">
                  其他想法/补充 (可选)：
                </p>
                <textarea
                  v-model="userOtherThoughts[index]"
                  class="supplement-input"
                  placeholder="如果您对该问题有其他想法或需要补充说明，请在此输入..."
                  rows="2"
                />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button
              class="btn"
              @click="smartStep = 1"
            >
              上一步
            </button>
            <button
              class="btn primary"
              :disabled="!isAllAnswered || isGenerating"
              @click="generateAgentDoc"
            >
              {{ isGenerating ? '生成中...' : '生成需求文档' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { API_CONFIG } from '@/config/api'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const authStore = useAuthStore()
const uiStore = useUIStore()
const docs = ref([])
const currentDoc = ref(null)
const history = ref([])
const showHistoryDialog = ref(false)
const isPreview = ref(true)
const editorTextareaRef = ref(null)
const previewContainerRef = ref(null)

const PERSISTENCE_KEY = 'requirement_agent_state'
const LAST_DOC_ID_KEY = 'requirement_last_doc_id'

//渲染Markdown
const renderMarkdown = (text) => {
  if (!text) return ''
  return DOMPurify.sanitize(marked.parse(text))
}

// 自动滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  const container = isPreview.value ? previewContainerRef.value : editorTextareaRef.value
  if (container) {
    container.scrollTo({
      top: container.scrollHeight,
      behavior: 'smooth'
    })
  }
}

const DEMO_PROJECTS = [
  {
    id: 'demo-1',
    title: '【演示】智能社区生鲜团购系统',
    content: `# 智能社区生鲜团购系统需求文档\n\n## 1. 项目背景\n旨在为社区居民提供便捷、新鲜、低价的生鲜购买体验，通过团长维护社区关系。\n\n## 2. 核心功能\n- **团长端**：订单统计、佣金结算、到货通知。\n- **用户端**：每日秒杀、附近团点选择、售后申请。\n- **管理端**：供应链管理、团长审核、数据大盘。\n\n## 3. 业务流程\n用户下单 -> 供应商发货至中心仓 -> 分拣配送至团长点 -> 用户自提。`,
    updatedAt: new Date().toISOString(),
    isDemo: true
  },
  {
    id: 'demo-2',
    title: '【演示】企业级 AI 知识库管理平台',
    content: `# 企业级 AI 知识库管理平台需求文档\n\n## 1. 项目背景\n解决企业内部文档碎片化问题，利用大模型实现智能化搜索与问答。\n\n## 2. 核心功能\n- **文档解析**：支持 PDF/Word/Markdown 自动向量化。\n- **智能问答**：基于 RAG 架构的精准搜索与语义回答。\n- **权限控制**：细粒度的部门级文档访问权限管理。\n\n## 3. 技术需求\n对接主流 LLM 接口，支持私有化部署。`,
    updatedAt: new Date().toISOString(),
    isDemo: true
  }
]

// Smart Generate States
const showSmartModal = ref(false)
const smartStep = ref(1)
const userIdea = ref('')
let abortController = null
const smartDomain = ref('')
const smartQuestions = ref([])
const userAnswers = ref([])
const userOtherThoughts = ref([])
const isGenerating = ref(false)

// 持久化保存函数
const saveStateToLocal = () => {
  // 只要有初步想法，就持久化保存进度，直到生成完成
  if (userIdea.value.trim() || smartStep.value > 1) {
    const state = {
      smartStep: smartStep.value,
      userIdea: userIdea.value,
      smartDomain: smartDomain.value,
      smartQuestions: smartQuestions.value,
      userAnswers: userAnswers.value,
      userOtherThoughts: userOtherThoughts.value,
      showSmartModal: showSmartModal.value
    }
    localStorage.setItem(PERSISTENCE_KEY, JSON.stringify(state))
  } else {
    localStorage.removeItem(PERSISTENCE_KEY)
  }
}

// 监听状态变化并保存
watch(
  [showSmartModal, smartStep, userIdea, smartDomain, smartQuestions, userAnswers, userOtherThoughts],
  () => {
    saveStateToLocal()
  },
  { deep: true }
)

// 监听当前文档变化，记录最后编辑的 ID
watch(
  () => currentDoc.value?.id,
  (newId) => {
    if (newId) {
      localStorage.setItem(LAST_DOC_ID_KEY, newId)
    }
  }
)

// 加载持久化状态
const loadPersistedState = () => {
  const saved = localStorage.getItem(PERSISTENCE_KEY)
  if (saved) {
    try {
      const state = JSON.parse(saved)
      smartStep.value = state.smartStep || 1
      userIdea.value = state.userIdea || ''
      smartDomain.value = state.smartDomain || ''
      smartQuestions.value = state.smartQuestions || []
      userAnswers.value = state.userAnswers || []
      userOtherThoughts.value = state.userOtherThoughts || []
      showSmartModal.value = state.showSmartModal || false
    } catch (e) {
      console.error('Failed to load persisted state:', e)
      localStorage.removeItem(PERSISTENCE_KEY)
    }
  }
}

const isAllAnswered = computed(() => {
  if (smartQuestions.value.length === 0) return false
  return userAnswers.value.every(a => {
    if (Array.isArray(a)) return a.length > 0
    return a !== '' && a !== null && a !== undefined
  })
})

const openSmartGenerate = () => {
  if (localStorage.getItem(PERSISTENCE_KEY)) {
    if (!confirm('发现上次未完成的作答，是否继续？点击取消将开启新的生成。')) {
      localStorage.removeItem(PERSISTENCE_KEY)
      resetSmartState()
    } else {
      loadPersistedState()
    }
  } else {
    resetSmartState()
  }
  showSmartModal.value = true
}

const resetSmartState = () => {
  smartStep.value = 1
  userIdea.value = ''
  smartQuestions.value = []
  userAnswers.value = []
  userOtherThoughts.value = []
  isGenerating.value = false
  currentDoc.value = null
}

const stopGeneration = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  isGenerating.value = false
  uiStore.showToast('已停止生成')
}

const closeSmartModal = () => {
  if (abortController && smartStep.value < 2) {
    abortController.abort()
    abortController = null
  }
  showSmartModal.value = false
}

const generateQuestions = async () => {
  if (!userIdea.value.trim()) return
  isGenerating.value = true
  try {
    abortController = new AbortController()
    const res = await fetch(`${API_CONFIG.baseURL}/api/requirements/agent/questions`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ userIdea: userIdea.value }),
      signal: abortController.signal
    })
    const data = await res.json()
    if (data.code === 200) {
      const parsed = JSON.parse(data.data)
      smartDomain.value = parsed.domain
      smartQuestions.value = parsed.questions
      userAnswers.value = smartQuestions.value.map(q => q.type === 'checkbox' ? [] : '')
      userOtherThoughts.value = smartQuestions.value.map(() => '')
      smartStep.value = 2
    } else {
      uiStore.showToast(data.message || '识别失败', 'error')
    }
  } catch (error) {
    if (error.name === 'AbortError') return
    console.error(error)
    uiStore.showToast('请求失败，请检查网络', 'error')
  } finally {
    isGenerating.value = false
  }
}

const generateAgentDoc = async () => {
  isGenerating.value = true
  showSmartModal.value = false
  isPreview.value = true
  
  // 清除持久化状态，因为已经进入正式生成流程
  localStorage.removeItem(PERSISTENCE_KEY)
  
  // Create a new blank doc first
  const newDoc = {
    title: `${smartDomain.value}需求文档 - ${new Date().toLocaleDateString()}`,
    content: ''
  }
  currentDoc.value = newDoc

  try {
    abortController = new AbortController()
    const res = await fetch(`${API_CONFIG.baseURL}/api/requirements/agent/generate-stream`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userIdea: userIdea.value,
        domain: smartDomain.value,
        answers: JSON.stringify(smartQuestions.value.map((q, i) => ({
          question: q.title,
          answer: userAnswers.value[i],
          supplement: userOtherThoughts.value[i] || ''
        })))
      }),
      signal: abortController.signal
    })

    if (!res.ok) throw new Error('Network response was not ok')

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    
    let scrollTimeout = null
    const processStreamData = (data) => {
      try {
        if (data.startsWith('{') && data.endsWith('}')) {
          const parsed = JSON.parse(data)
          if (parsed.content) {
            newDoc.content += parsed.content
          } else if (parsed.error) {
            uiStore.showToast(parsed.error, 'error')
          }
        } else {
          newDoc.content += data
        }
        
        // 使用 requestAnimationFrame 优化滚动性能，避免过度触发
        if (!scrollTimeout) {
          scrollTimeout = window.requestAnimationFrame(() => {
            scrollToBottom()
            scrollTimeout = null
          })
        }
      } catch (e) {
        if (!data.startsWith('{')) {
          newDoc.content += data
          scrollToBottom()
        }
      }
    }

    outer: while (true) {
      const { value, done } = await reader.read()
      
      if (value) {
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        
        for (const line of lines) {
          const trimmedLine = line.trim()
          if (!trimmedLine || !trimmedLine.startsWith('data:')) continue
          
          const data = trimmedLine.slice(5).trim()
          if (data === '[DONE]') {
            isGenerating.value = false
            break outer
          }
          
          processStreamData(data)
        }
      }

      if (done) {
        // Process remaining buffer
        if (buffer) {
          const trimmedLine = buffer.trim()
          if (trimmedLine.startsWith('data:')) {
            const data = trimmedLine.slice(5).trim()
            if (data !== '[DONE]') {
              processStreamData(data)
            }
          }
        }
        break
      }
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('Stream aborted')
    } else {
      console.error('Streaming error:', error)
      uiStore.showToast('生成文档失败', 'error')
    }
  } finally {
    isGenerating.value = false
  }
}

// Remove old processData function if it's no longer used
// It's not used anymore since we moved it into generateAgentDoc to capture newDoc
const formatDate = (str) => new Date(str).toLocaleString()

const fetchDocs = async () => {
  const res = await fetch(`${API_CONFIG.baseURL}/api/requirements`, {
    headers: { 'Authorization': `Bearer ${authStore.token}` }
  })
  const data = await res.json()
  if (data.code === 200) {
    docs.value = data.data
    
    // 如果没有当前选中的文档，尝试恢复上一次的
    if (!currentDoc.value && docs.value.length > 0) {
      const lastId = localStorage.getItem(LAST_DOC_ID_KEY)
      if (lastId) {
        const lastDoc = docs.value.find(d => d.id.toString() === lastId.toString())
        if (lastDoc) {
          selectDoc(lastDoc)
        }
      }
    }
  }
}

const createNew = () => {
  currentDoc.value = { title: '新需求文档', content: '' }
  localStorage.removeItem(LAST_DOC_ID_KEY)
}

const loadDemo = (demo) => {
  currentDoc.value = { ...demo, id: null } // 设为 null 以便用户可以“保存”为自己的文档
  uiStore.showToast('已加载演示项目，您可以根据需要进行修改并保存')
}

const selectDoc = (doc) => {
  currentDoc.value = { ...doc }
}

const saveDoc = async () => {
  const isNew = !currentDoc.value.id
  const url = isNew 
    ? `${API_CONFIG.baseURL}/api/requirements`
    : `${API_CONFIG.baseURL}/api/requirements/${currentDoc.value.id}`
  
  const method = isNew ? 'POST' : 'PUT'
  
  const res = await fetch(url, {
    method,
    headers: { 
      'Authorization': `Bearer ${authStore.token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(currentDoc.value)
  })
  const data = await res.json()
  if (data.code === 200) {
    currentDoc.value = data.data
    fetchDocs()
    uiStore.showToast('保存成功')
  }
}

const showHistory = async () => {
  if (!currentDoc.value.id) return
  const res = await fetch(`${API_CONFIG.baseURL}/api/requirements/${currentDoc.value.id}/history`, {
    headers: { 'Authorization': `Bearer ${authStore.token}` }
  })
  const data = await res.json()
  if (data.code === 200) {
    history.value = data.data
    showHistoryDialog.value = true
  }
}

const restoreVersion = (h) => {
  currentDoc.value.content = h.content
  currentDoc.value.version = h.version // Just for display, save will increment
  showHistoryDialog.value = false
}

onMounted(() => {
  fetchDocs()
  loadPersistedState()
})
</script>

<style scoped>
.req-manager {
  display: flex;
  height: 100%;
  background: var(--bg-secondary);
  color: var(--text-primary);
  --primary-color: #06b6d4;
  --primary-gradient: linear-gradient(135deg, #06b6d4 0%, #10b981 100%);
}
.req-sidebar {
  width: 260px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}
.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sidebar-header span {
  font-weight: 600;
  font-size: 1.1rem;
  background: var(--primary-gradient);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary-color);
  border-color: var(--primary-color);
}
.smart-btn {
  color: #8b5cf6; /* Purple for magic/AI */
}
.smart-btn:hover {
  color: #7c3aed;
  border-color: #8b5cf6;
}
.doc-list { overflow-y: auto; flex: 1; padding: 12px 8px; }
.doc-item {
  padding: 12px 16px;
  cursor: pointer;
  border-radius: 8px;
  margin-bottom: 4px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.doc-item:hover {
  background: var(--bg-tertiary);
}
.doc-item.active { 
  background: rgba(6, 182, 212, 0.1);
  border-color: rgba(6, 182, 212, 0.2);
  border-left: 4px solid var(--primary-color);
}
body.dark-mode .doc-item.active {
  background: rgba(6, 182, 212, 0.15);
}
.doc-title {
  font-weight: 500;
  font-size: 0.95rem;
  margin-bottom: 4px;
  color: var(--text-primary);
}
.doc-date {
  font-size: 0.8rem;
  color: var(--text-tertiary);
}
.req-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: hidden;
}
.editor-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-primary);
}
.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}
.title-input {
  font-size: 1.25rem;
  font-weight: 600;
  border: none;
  background: transparent;
  color: var(--text-primary);
  width: 100%;
  outline: none;
}
.generating-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: var(--primary-gradient);
  color: white;
  border-radius: 20px;
  font-size: 0.9rem;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3);
  animation: badge-glow 2s infinite;
}
@keyframes badge-glow {
  0% { box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3); }
  50% { box-shadow: 0 4px 20px rgba(6, 182, 212, 0.6); }
  100% { box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3); }
}
.stop-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 50%;
  margin-left: 8px;
  transition: all 0.2s;
}
.stop-btn:hover {
  background: rgba(255, 255, 255, 0.4);
  transform: scale(1.1);
}
.editor-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}
.content-editor {
  flex: 1;
  padding: 24px;
  border: none;
  resize: none;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: 'Fira Code', monospace;
  font-size: 1rem;
  line-height: 1.6;
  outline: none;
}
.preview-container {
  flex: 1;
  padding: 32px 48px;
  overflow-y: auto;
  background: var(--bg-primary);
  color: var(--text-primary);
  line-height: 1.8;
  scroll-behavior: smooth;
}
.content-wrapper {
  position: relative;
  display: block;
}
.streaming-cursor {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  margin-left: 4px;
  font-size: 1.2rem;
  animation: pencil-write 0.8s ease-in-out infinite;
  pointer-events: none;
  vertical-align: text-bottom;
}
@keyframes pencil-write {
  0% { transform: translate(0, 0) rotate(-10deg); }
  25% { transform: translate(2px, -2px) rotate(5deg); }
  50% { transform: translate(0, -4px) rotate(-5deg); }
  75% { transform: translate(-2px, -2px) rotate(10deg); }
  100% { transform: translate(0, 0) rotate(-10deg); }
}
/* 强制让 markdown 的最后一个元素与光标并排 */
.preview-container :deep(.markdown-body > *:last-child) {
  display: inline !important;
}
.preview-container :deep(.markdown-body) {
  display: inline;
}
.is-streaming .markdown-body {
  transition: all 0.3s ease;
}
.empty-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--text-tertiary);
  font-style: italic;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
  margin: 0 8px;
}
.btn.active {
  background: var(--bg-secondary);
  color: var(--primary-color);
  border-color: var(--primary-color);
}
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
}
.empty-content {
  max-width: 600px;
  text-align: center;
  padding: 40px;
}
.empty-icon {
  font-size: 4rem;
  color: var(--primary-color);
  opacity: 0.2;
  margin-bottom: 24px;
}
.empty-desc {
  color: var(--text-tertiary);
  margin-bottom: 32px;
}
.guide-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 48px;
}
.guide-btn {
  padding: 12px 24px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  transition: all 0.2s;
}
.guide-btn.primary {
  background: var(--primary-gradient);
  color: white;
  border: none;
}
.guide-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.demo-section {
  text-align: left;
}
.demo-title {
  font-size: 0.9rem;
  color: var(--text-tertiary);
  margin-bottom: 16px;
  font-weight: 500;
}
.demo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.demo-card {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.demo-card:hover {
  border-color: var(--primary-color);
  background: var(--bg-tertiary);
}
.demo-card-title {
  font-weight: 600;
  font-size: 0.95rem;
  margin-bottom: 8px;
  color: var(--text-primary);
}
.demo-card-desc {
  font-size: 0.85rem;
  color: var(--text-tertiary);
  line-height: 1.4;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.smart-modal {
  max-width: 800px;
  width: 95%;
  height: 80vh;
  display: flex;
  flex-direction: column;
}
.smart-step.full-height {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.streaming-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.agent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 12px;
  margin-bottom: 16px;
}
.agent-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.agent-avatar {
  width: 40px;
  height: 40px;
  background: var(--primary-gradient);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}
.status-text {
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}
.status-subtext {
  font-size: 0.85rem;
  color: var(--text-tertiary);
  margin: 4px 0 0 0;
}
.loader-small {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-color);
  border-top: 2px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
.streaming-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  line-height: 1.6;
  font-size: 1rem;
}
.streaming-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-tertiary);
  font-style: italic;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Markdown Styles */
.markdown-body {
  color: var(--text-primary);
  word-wrap: break-word;
}
.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
  color: var(--text-primary);
}
.markdown-body h1 { font-size: 1.6rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
.markdown-body h2 { font-size: 1.4rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
.markdown-body h3 { font-size: 1.2rem; }
.markdown-body p { margin-bottom: 16px; line-height: 1.7; }
.markdown-body ul, .markdown-body ol { padding-left: 2em; margin-bottom: 16px; }
.markdown-body li { margin-bottom: 6px; }
.markdown-body code { background: var(--bg-tertiary); padding: 0.2em 0.4em; border-radius: 4px; font-family: 'Fira Code', monospace; font-size: 85%; }
.markdown-body pre { background: var(--bg-tertiary); padding: 16px; border-radius: 8px; overflow: auto; margin-bottom: 16px; }
.markdown-body blockquote { padding: 0 1em; color: var(--text-tertiary); border-left: 0.25em solid var(--border-color); margin: 0 0 16px 0; }
.markdown-body table { border-spacing: 0; border-collapse: collapse; margin-bottom: 16px; width: 100%; overflow: auto; }
.markdown-body table th, .markdown-body table td { padding: 8px 13px; border: 1px solid var(--border-color); }
.markdown-body table tr { background-color: var(--bg-primary); border-top: 1px solid var(--border-color); }
.markdown-body table tr:nth-child(2n) { background-color: var(--bg-secondary); }
.markdown-body strong { font-weight: 600; }
.markdown-body hr { height: 0.25em; padding: 0; margin: 24px 0; background-color: var(--border-color); border: 0; }

.modal-content {
  background: var(--bg-primary);
  padding: 32px;
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--border-color);
}

/* Smart Modal Specifics */
.smart-modal {
  max-width: 800px;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.close-btn {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  color: var(--text-tertiary);
  cursor: pointer;
}
.smart-step {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}
.step-desc {
  color: var(--text-secondary);
  font-size: 0.95rem;
  line-height: 1.5;
}
.idea-input {
  width: 100%;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
  resize: none;
  font-size: 1rem;
}
.idea-input:focus {
  outline: none;
  border-color: var(--primary-color);
}
.domain-tag {
  background: rgba(6, 182, 212, 0.1);
  color: var(--primary-color);
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 600;
  margin-left: 8px;
}
.questions-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 8px 0;
}
.question-item {
  background: var(--bg-secondary);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}
.question-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
}
.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.option-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}
.option-label:hover {
  background: var(--bg-tertiary);
}
.other-supplement {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
}
.supplement-label {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.supplement-input {
  width: 100%;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
  resize: vertical;
  font-size: 0.9rem;
}
.supplement-input:focus {
  outline: none;
  border-color: var(--primary-color);
}
.modal-footer {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}
.generating-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 20px;
  text-align: center;
}
.loader {
  width: 48px;
  height: 48px;
  border: 5px solid var(--border-color);
  border-bottom-color: var(--primary-color);
  border-radius: 50%;
  display: inline-block;
  box-sizing: border-box;
  animation: rotation 1s linear infinite;
}
@keyframes rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.preview-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.success-msg {
  color: #10b981;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-content {
  background: var(--bg-secondary);
  padding: 20px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  font-family: monospace;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
  font-size: 0.9rem;
  color: var(--text-secondary);
}
.history-list {
  margin: 20px 0;
  max-height: 400px;
  overflow-y: auto;
}
.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}
.add-btn {
  background: var(--primary-gradient);
  color: white;
  border: none;
  font-size: 1.2rem;
  font-weight: bold;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px -1px rgba(6, 182, 212, 0.2);
}
.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px -2px rgba(6, 182, 212, 0.3);
  color: white;
}
.btn {
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all 0.2s ease;
}
.btn:hover {
  background: var(--bg-tertiary);
}
.btn.primary {
  background: var(--primary-gradient);
  color: white;
  border: none;
  box-shadow: 0 4px 6px -1px rgba(6, 182, 212, 0.2);
}
.btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px -2px rgba(6, 182, 212, 0.3);
}
</style>
