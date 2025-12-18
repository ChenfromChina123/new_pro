<template>
  <AppLayout>
    <div class="language-learning-page">
      <!-- Mobile Header -->
      <div class="mobile-header">
        <button
          class="mobile-menu-btn"
          @click="showMobileSidebar = true"
        >
          <span class="icon">☰</span>
        </button>
        <h1 class="mobile-title">
          📚 语言学习
        </h1>
      </div>

      <!-- Sidebar Navigation -->
      <div
        class="sidebar"
        :class="{ 'sidebar-mobile-open': showMobileSidebar }"
      >
        <div class="sidebar-header">
          <h2>📚 语言学习</h2>
          <button
            v-if="showMobileSidebar"
            class="close-mobile-menu"
            @click="showMobileSidebar = false"
          >
            <span class="icon">✕</span>
          </button>
        </div>
        <nav class="sidebar-nav">
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'dashboard' }"
            @click.prevent="currentView = 'dashboard'; showMobileSidebar = false"
          >
            <span class="icon">📊</span>
            <span class="label">学习概览</span>
          </a>
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'my-words' }"
            @click.prevent="currentView = 'my-words'; showMobileSidebar = false"
          >
            <span class="icon">📝</span>
            <span class="label">我的单词</span>
          </a>
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'public-library' }"
            @click.prevent="currentView = 'public-library'; showMobileSidebar = false"
          >
            <span class="icon">🔍</span>
            <span class="label">公共词库</span>
          </a>
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'ai-articles' }"
            @click.prevent="currentView = 'ai-articles'; showMobileSidebar = false"
          >
            <span class="icon">🤖</span>
            <span class="label">AI文章</span>
          </a>
        </nav>
      </div>

      <!-- Main Content Area -->
      <div class="main-content">
        <!-- Dashboard View -->
        <div
          v-if="currentView === 'dashboard'"
          class="view-section dashboard-view"
        >
          <div class="view-header">
            <h2>学习概览</h2>
            <p>查看你的学习进度和今日任务</p>
          </div>

          <div class="dashboard-grid">
            <div class="stat-card primary">
              <div class="stat-icon">
                📚
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ learningStats?.totalWords ?? 0 }}
                </div>
                <div class="stat-label">
                  已学习单词
                </div>
              </div>
            </div>
            <div class="stat-card success">
              <div class="stat-icon">
                ✅
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ learningStats?.masteredWords ?? 0 }}
                </div>
                <div class="stat-label">
                  已掌握
                </div>
              </div>
            </div>
            <div class="stat-card warning">
              <div class="stat-icon">
                ⏱️
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ formatDuration(learningStats?.todayDuration ?? 0) }}
                </div>
                <div class="stat-label">
                  今日时长
                </div>
              </div>
            </div>
            <div class="stat-card info">
              <div class="stat-icon">
                📈
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ formatDuration(learningStats?.totalDuration ?? 0) }}
                </div>
                <div class="stat-label">
                  总时长
                </div>
              </div>
            </div>
          </div>

          <div class="review-section card mt-4">
            <div class="card-header">
              <h3>今日复习</h3>
              <button
                class="btn btn-text"
                @click="refreshReview"
              >
                刷新
              </button>
            </div>
            <div
              v-if="reviewItems.length === 0"
              class="empty-state"
            >
              <div class="illustration">
                🎉
              </div>
              <p>太棒了！今日复习任务已完成</p>
            </div>
            <div
              v-else
              class="review-grid"
            >
              <div
                v-for="item in reviewItems"
                :key="item.id"
                class="review-card-item"
              >
                <div class="review-content">
                  <h4 class="review-word">
                    {{ item.word?.word || item.wordId }}
                  </h4>
                  <p class="review-def">
                    {{ item.word?.definition }}
                  </p>
                </div>
                <div class="review-actions">
                  <button
                    class="btn btn-sm btn-outline"
                    @click="quickReview(getReviewWordId(item), Math.min((item.masteryLevel ?? 0) + 1, 5))"
                  >
                    认识
                  </button>
                  <button
                    class="btn btn-sm btn-primary"
                    @click="quickReview(getReviewWordId(item), 5)"
                  >
                    掌握
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- My Words View -->
        <div
          v-if="currentView === 'my-words'"
          class="view-section my-words-view"
        >
          <div class="two-column-layout">
            <div class="list-column card">
              <div class="card-header">
                <h3>单词表</h3>
                <button
                  class="btn btn-sm btn-primary"
                  @click="showCreateList = true"
                >
                  + 新建
                </button>
              </div>
              <div class="list-container">
                <div
                  v-for="list in vocabularyLists"
                  :key="list.id"
                  class="list-item"
                  :class="{ active: currentListId === list.id }"
                  @click="selectList(list.id)"
                >
                  <div class="list-info">
                    <h4>{{ list.name }}</h4>
                    <span class="badge">{{ (list.language || 'en').toUpperCase() }}</span>
                    <span class="count">{{ list.wordCount || 0 }} 词</span>
                  </div>
                  <button
                    class="btn-icon delete-btn"
                    title="删除"
                    @click.stop="removeList(list.id)"
                  >
                    ×
                  </button>
                </div>
              </div>
            </div>

            <div class="words-column card">
              <div class="card-header">
                <h3>{{ currentList ? currentList.name : '单词列表' }}</h3>
                <div class="actions">
                  <button
                    v-if="currentListId"
                    class="btn btn-sm btn-outline"
                    @click="refreshCurrentList"
                  >
                    刷新
                  </button>
                  <button
                    v-if="currentListId"
                    class="btn btn-sm btn-primary"
                    @click="showAddWord = true"
                  >
                    + 添加单词
                  </button>
                </div>
              </div>

              <div
                v-if="!currentListId"
                class="empty-state"
              >
                <p>请选择一个单词表查看详情</p>
              </div>
              <div
                v-else-if="currentWords.length === 0"
                class="empty-state"
              >
                <p>暂无单词，点击上方按钮添加</p>
              </div>
              <div
                v-else
                class="words-grid"
              >
                <div
                  v-for="word in currentWords"
                  :key="word.id"
                  class="word-card-item"
                >
                  <div class="word-header">
                    <h4>{{ word.word }}</h4>
                    <span class="pos">{{ word.partOfSpeech }}</span>
                  </div>
                  <div class="word-body">
                    <p class="definition">
                      {{ word.definition }}
                    </p>
                    <p
                      v-if="word.example"
                      class="example"
                    >
                      {{ word.example }}
                    </p>
                  </div>
                  <div class="word-footer">
                    <span :class="['status-tag', getMasteryClass(getWordProgress(word.id).masteryLevel)]">
                      {{ getMasteryText(getWordProgress(word.id).masteryLevel) }}
                    </span>
                    <div class="controls">
                      <select
                        class="select-sm"
                        :value="getWordProgress(word.id).masteryLevel ?? 0"
                        @change="changeMastery(word.id, $event.target.value)"
                      >
                        <option
                          v-for="i in 6"
                          :key="i"
                          :value="i-1"
                        >
                          {{ i-1 }}
                        </option>
                      </select>
                      <button
                        class="btn-icon delete-btn"
                        @click="removeWord(word.id)"
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Public Library View -->
        <div
          v-if="currentView === 'public-library'"
          class="view-section public-library-view"
        >
          <div class="view-header">
            <h2>公共词库</h2>
            <p>搜索并添加单词到你的个人词库</p>
          </div>

          <div class="search-bar-container">
            <div class="search-input-wrapper">
              <input
                v-model="publicKeyword"
                type="text"
                class="search-input"
                placeholder="搜索单词、释义..."
                @keyup.enter="searchPublic"
              >
              <button
                class="btn btn-primary search-btn"
                @click="searchPublic"
              >
                搜索
              </button>
            </div>
            <div
              v-if="!currentListId"
              class="search-tips"
            >
              ⚠️ 请先在"我的单词"中选择或创建一个目标单词表
            </div>
            <div
              v-else
              class="search-tips"
            >
              添加到: <strong>{{ currentList?.name }}</strong>
            </div>
          </div>

          <div class="public-results-grid">
            <div
              v-if="publicResults.length === 0"
              class="empty-state"
            >
              <p>输入关键词搜索，或直接点击搜索查看推荐词汇</p>
            </div>
            <div
              v-else
              class="results-list"
            >
              <div
                v-for="w in paginatedResults"
                :key="w.id"
                class="result-card"
              >
                <div class="result-info">
                  <div class="result-header">
                    <h4>{{ w.word }}</h4>
                    <span class="pos-tag">{{ w.partOfSpeech }}</span>
                  </div>
                  <p class="result-def">
                    {{ w.definition }}
                  </p>
                </div>
                <button
                  class="btn btn-sm btn-primary"
                  :disabled="!currentListId"
                  @click="addPublicWord(w)"
                >
                  添加
                </button>
              </div>
                
              <!-- 分页组件 -->
              <div
                v-if="totalPages > 1"
                class="pagination-container"
              >
                <div class="pagination-info">
                  显示第 {{ publicStartIndex }} - {{ publicEndIndex }} 条，共 {{ vocabularyStore.publicSearchTotal }} 条
                </div>
                <div class="pagination-buttons">
                  <button
                    class="btn btn-sm btn-outline"
                    :disabled="currentPage === 1"
                    @click="goToPage(currentPage - 1)"
                  >
                    上一页
                  </button>
                  <span class="pagination-page">
                    第 {{ currentPage }} 页 / 共 {{ totalPages }} 页
                  </span>
                  <button
                    class="btn btn-sm btn-outline"
                    :disabled="currentPage === totalPages"
                    @click="goToPage(currentPage + 1)"
                  >
                    下一页
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Articles View -->
        <div
          v-if="currentView === 'ai-articles'"
          class="view-section ai-articles-view"
        >
          <div class="view-header">
            <h2>AI 生成学习文章</h2>
            <p>基于你的词汇表生成个性化阅读材料</p>
          </div>

          <div class="article-generator card">
            <!-- Step 1: Select List & Words -->
            <div
              v-if="articleStep === 1"
              class="step-container"
            >
              <h3>第一步：选择单词</h3>
              <div class="form-group">
                <label>来源单词表</label>
                <select
                  v-model="currentListId"
                  class="select-input"
                  @change="onListChange"
                >
                  <option
                    :value="null"
                    disabled
                  >
                    请选择单词表
                  </option>
                  <option
                    v-for="list in vocabularyLists"
                    :key="list.id"
                    :value="list.id"
                  >
                    {{ list.name }} ({{ list.wordCount || 0 }}词)
                  </option>
                </select>
              </div>
              
              <div
                v-if="currentListId"
                class="word-selection"
              >
                <div class="selection-header">
                  <label class="checkbox-label">
                    <input
                      type="checkbox"
                      :checked="isAllSelected"
                      @change="toggleSelectAll"
                    >
                    全选 ({{ selectedWordIds.size }})
                  </label>
                </div>
                <div class="word-checkboxes">
                  <label
                    v-for="word in currentWords"
                    :key="word.id"
                    class="word-checkbox"
                  >
                    <input
                      type="checkbox"
                      :value="word.id"
                      :checked="selectedWordIds.has(word.id)"
                      @change="toggleWordSelection(word.id)"
                    >
                    <span class="word-text">{{ word.word }}</span>
                  </label>
                </div>
              </div>
              
              <div class="step-actions">
                <button
                  class="btn btn-primary"
                  :disabled="selectedWordIds.size === 0"
                  @click="articleStep = 2"
                >
                  下一步
                </button>
              </div>
            </div>

            <!-- Step 2: Configure Options -->
            <div
              v-if="articleStep === 2"
              class="step-container"
            >
              <h3>第二步：配置选项</h3>
              <div class="form-group">
                <label>文章难度</label>
                <select
                  v-model="articleOptions.difficulty"
                  class="select-input"
                >
                  <option value="Beginner">
                    初级 (Beginner)
                  </option>
                  <option value="Intermediate">
                    中级 (Intermediate)
                  </option>
                  <option value="Advanced">
                    高级 (Advanced)
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>文章长度</label>
                <select
                  v-model="articleOptions.length"
                  class="select-input"
                >
                  <option value="Short">
                    短篇 (约150词)
                  </option>
                  <option value="Medium">
                    中篇 (约300词)
                  </option>
                  <option value="Long">
                    长篇 (约500词)
                  </option>
                </select>
              </div>
              <div class="step-actions">
                <button
                  class="btn btn-secondary"
                  @click="articleStep = 1"
                >
                  上一步
                </button>
                <button
                  class="btn btn-primary"
                  @click="generateTopics"
                >
                  生成主题建议
                </button>
              </div>
            </div>

            <!-- Step 3: Select Topic -->
            <div
              v-if="articleStep === 3"
              class="step-container"
            >
              <h3>第三步：选择主题</h3>
              <div
                v-if="isGenerating"
                class="loading-state"
              >
                <div class="spinner" />
                <p>正在生成主题建议...</p>
              </div>
              <div
                v-else
                class="topics-grid"
              >
                <label
                  v-for="topic in generatedTopics"
                  :key="topic"
                  class="topic-card"
                  :class="{ selected: articleOptions.topic === topic }"
                >
                  <input
                    v-model="articleOptions.topic"
                    type="radio"
                    :value="topic"
                    name="topic"
                  >
                  <span class="topic-text">{{ topic }}</span>
                </label>
              </div>
              <div
                v-if="!isGenerating"
                class="step-actions"
              >
                <button
                  class="btn btn-secondary"
                  @click="articleStep = 2"
                >
                  上一步
                </button>
                <button
                  class="btn btn-primary"
                  :disabled="!articleOptions.topic"
                  @click="generateArticle"
                >
                  生成文章
                </button>
              </div>
            </div>

            <!-- Step 4: Result -->
            <div
              v-if="articleStep === 4"
              class="step-container result-step"
            >
              <div class="result-header">
                <h3>{{ generatedArticle.topic }}</h3>
                <div class="result-actions">
                  <button
                    class="btn btn-outline"
                    @click="downloadHTML"
                  >
                    📥 下载 HTML
                  </button>
                  <button
                    class="btn btn-outline"
                    @click="downloadPDF"
                  >
                    📥 打印/PDF
                  </button>
                  <button
                    class="btn btn-primary"
                    @click="resetArticleGenerator"
                  >
                    再来一篇
                  </button>
                </div>
              </div>
              
              <div
                v-if="isGenerating"
                class="loading-state"
              >
                <div class="spinner" />
                <p>正在创作文章...</p>
              </div>
              
              <div
                v-else
                id="printable-article"
                class="article-display-area"
              >
                <div class="article-meta print-only">
                  <h1>{{ generatedArticle.topic }}</h1>
                  <p>难度: {{ articleOptions.difficulty }} | 长度: {{ articleOptions.length }}</p>
                </div>
                <div
                  class="article-content"
                  v-html="renderMarkdown(generatedArticle.originalText)"
                />
                
                <div class="vocabulary-list-append print-only">
                  <h3>📚 重点词汇</h3>
                  <table>
                    <thead>
                      <tr>
                        <th>单词</th>
                        <th>释义</th>
                        <th>词性</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="word in getSelectedWordsDetails()"
                        :key="word.id"
                      >
                        <td><strong>{{ word.word }}</strong></td>
                        <td>{{ word.definition }}</td>
                        <td>{{ word.partOfSpeech }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Dialogs -->
    <div
      v-if="showCreateList"
      class="modal-overlay"
      @click.self="showCreateList = false"
    >
      <div class="modal-card">
        <h3>创建单词表</h3>
        <input
          v-model="newList.name"
          type="text"
          class="input"
          placeholder="输入单词表名称"
          @keyup.enter="createList"
        >
        <textarea
          v-model="newList.description"
          class="input textarea"
          placeholder="描述（可选）"
        />
        <select
          v-model="newList.language"
          class="input"
        >
          <option value="en">
            英语
          </option>
          <option value="ja">
            日语
          </option>
          <option value="ko">
            韩语
          </option>
          <option value="fr">
            法语
          </option>
          <option value="de">
            德语
          </option>
          <option value="es">
            西班牙语
          </option>
          <option value="zh">
            中文
          </option>
        </select>
        <div class="modal-actions">
          <button
            class="btn btn-secondary"
            @click="showCreateList = false"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="createList"
          >
            创建
          </button>
        </div>
      </div>
    </div>
    
    <div
      v-if="showAddWord"
      class="modal-overlay"
      @click.self="showAddWord = false"
    >
      <div class="modal-card">
        <h3>添加单词</h3>
        <div class="form-group">
          <label>单词</label>
          <input
            v-model="newWord.word"
            type="text"
            class="input"
            placeholder="单词"
          >
        </div>
        <div class="form-group">
          <label>释义</label>
          <input
            v-model="newWord.definition"
            type="text"
            class="input"
            placeholder="释义"
          >
        </div>
        <div class="form-group">
          <label>词性</label>
          <input
            v-model="newWord.partOfSpeech"
            type="text"
            class="input"
            placeholder="例如: n., v., adj."
          >
        </div>
        <div class="form-group">
          <label>例句</label>
          <textarea
            v-model="newWord.example"
            class="input textarea"
            placeholder="例句（可选）"
          />
        </div>
        <div class="modal-actions">
          <button
            class="btn btn-secondary"
            @click="showAddWord = false"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="addWord"
          >
            添加
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, watch } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import AppLayout from '@/components/AppLayout.vue'
import { useVocabularyStore } from '@/stores/vocabulary'
import { marked } from 'marked'

const currentView = ref('dashboard') // dashboard, my-words, public-library, ai-articles
const showMobileSidebar = ref(false)

const currentListId = ref(null)
const showCreateList = ref(false)
const showAddWord = ref(false)
const newList = ref({
  name: '',
  description: '',
  language: 'en'
})
const newWord = ref({
  word: '',
  definition: '',
  partOfSpeech: '',
  example: ''
})
const publicKeyword = ref('')
const isGenerating = ref(false)
const currentPage = ref(1)
const pageSize = ref(50)

const paginatedResults = computed(() => {
  return vocabularyStore.publicSearchResults
})

const totalPages = computed(() => {
  return Math.ceil((vocabularyStore.publicSearchTotal || 0) / pageSize.value)
})

const publicStartIndex = computed(() => {
  if ((vocabularyStore.publicSearchTotal || 0) <= 0) return 0
  return (currentPage.value - 1) * pageSize.value + 1
})

const publicEndIndex = computed(() => {
  if ((vocabularyStore.publicSearchTotal || 0) <= 0) return 0
  return (currentPage.value - 1) * pageSize.value + paginatedResults.value.length
})

const fetchPublicPage = async (page) => {
  const kw = publicKeyword.value.trim()
  const language = currentList.value?.language || 'en'
  const result = await vocabularyStore.searchPublic(kw, language, page, pageSize.value)
  if (!result.success) {
    alert('搜索失败: ' + (result.message || '未知错误'))
  }
}

const goToPage = async (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  await fetchPublicPage(page)
}

// AI Article State
const articleStep = ref(1)
const selectedWordIds = reactive(new Set())
const articleOptions = reactive({
  difficulty: 'Intermediate',
  length: 'Medium',
  topic: ''
})
const generatedTopics = ref([])
const generatedArticle = ref(null)

const vocabularyStore = useVocabularyStore()
const vocabularyLists = computed(() => vocabularyStore.lists)
const currentList = computed(() => vocabularyStore.lists.find(l => l.id === currentListId.value) || null)
const currentWords = computed(() => vocabularyStore.wordsByListId[currentListId.value] || [])
const reviewItems = computed(() => vocabularyStore.reviewWords)
const learningStats = computed(() => vocabularyStore.stats)
const publicResults = computed(() => vocabularyStore.publicSearchResults)

const isAllSelected = computed(() => {
  return currentWords.value.length > 0 && selectedWordIds.size === currentWords.value.length
})

const lastActiveAt = ref(Date.now())
const lastTickAt = ref(Date.now())
const pendingDuration = ref(0)
const isFlushingDuration = ref(false)
let durationTimer = null

const markActive = () => {
  lastActiveAt.value = Date.now()
}

const flushDuration = async () => {
  if (isFlushingDuration.value) return
  const duration = pendingDuration.value
  if (duration <= 0) return

  pendingDuration.value = 0
  isFlushingDuration.value = true
  try {
    await vocabularyStore.recordActivity({
      activityType: 'vocabulary_learning',
      activityDetails: JSON.stringify({ view: currentView.value, listId: currentListId.value }),
      duration
    })
    await vocabularyStore.fetchStats()
  } catch (_) {
    pendingDuration.value += duration
  } finally {
    isFlushingDuration.value = false
  }
}

const tickDuration = () => {
  const now = Date.now()
  const deltaSeconds = Math.floor((now - lastTickAt.value) / 1000)
  if (deltaSeconds <= 0) return
  lastTickAt.value = now

  if (document.hidden) return
  if (now - lastActiveAt.value > 60_000) return

  pendingDuration.value += deltaSeconds
  if (pendingDuration.value >= 60) {
    void flushDuration()
  }
}

const onVisibilityChange = () => {
  if (document.hidden) {
    void flushDuration()
    return
  }
  markActive()
  lastTickAt.value = Date.now()
}

onMounted(async () => {
  await vocabularyStore.fetchLists()
  await Promise.all([
    vocabularyStore.fetchStats(),
    vocabularyStore.fetchReviewWords()
  ])

  markActive()
  lastTickAt.value = Date.now()
  durationTimer = window.setInterval(tickDuration, 1000)
  window.addEventListener('mousemove', markActive, { passive: true })
  window.addEventListener('keydown', markActive)
  window.addEventListener('scroll', markActive, { passive: true })
  window.addEventListener('click', markActive, { passive: true })
  window.addEventListener('focus', markActive)
  window.addEventListener('blur', flushDuration)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  if (durationTimer) {
    window.clearInterval(durationTimer)
    durationTimer = null
  }
  window.removeEventListener('mousemove', markActive)
  window.removeEventListener('keydown', markActive)
  window.removeEventListener('scroll', markActive)
  window.removeEventListener('click', markActive)
  window.removeEventListener('focus', markActive)
  window.removeEventListener('blur', flushDuration)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  void flushDuration()
})

watch([currentView, currentListId], () => {
  markActive()
  if (currentView.value === 'public-library') {
    currentPage.value = 1
    void fetchPublicPage(1)
  }
})

const selectList = async (listId) => {
  markActive()
  currentListId.value = listId
  if (!vocabularyStore.wordsByListId[listId]) {
    await vocabularyStore.fetchWords(listId)
  }
  await vocabularyStore.fetchListProgress(listId)
}

// AI Wizard Methods
const onListChange = async () => {
  markActive()
  if (currentListId.value) {
    await vocabularyStore.fetchWords(currentListId.value)
    selectedWordIds.clear()
  }
}

const toggleSelectAll = (e) => {
  markActive()
  if (e.target.checked) {
    currentWords.value.forEach(w => selectedWordIds.add(w.id))
  } else {
    selectedWordIds.clear()
  }
}

const toggleWordSelection = (wordId) => {
  markActive()
  if (selectedWordIds.has(wordId)) {
    selectedWordIds.delete(wordId)
  } else {
    selectedWordIds.add(wordId)
  }
}

const generateTopics = async () => {
  markActive()
  if (selectedWordIds.size === 0) return
  isGenerating.value = true
  articleStep.value = 3
  
  const words = currentWords.value
    .filter(w => selectedWordIds.has(w.id))
    .map(w => w.word)
    
  const result = await vocabularyStore.generateTopics(words, currentList.value?.language || 'en')
  if (result.success) {
    generatedTopics.value = result.data
  } else {
    alert(result.message)
    articleStep.value = 2
  }
  isGenerating.value = false
}

const generateArticle = async () => {
  markActive()
  if (!articleOptions.topic) return
  isGenerating.value = true
  articleStep.value = 4
  
  const result = await vocabularyStore.generateArticle({
    listId: currentListId.value,
    wordIds: Array.from(selectedWordIds),
    topic: articleOptions.topic,
    difficulty: articleOptions.difficulty,
    length: articleOptions.length
  })
  
  if (result.success) {
    generatedArticle.value = result.data
  } else {
    alert(result.message)
    articleStep.value = 3
  }
  isGenerating.value = false
}

const resetArticleGenerator = () => {
  markActive()
  articleStep.value = 1
  selectedWordIds.clear()
  articleOptions.topic = ''
  generatedArticle.value = null
}

const renderMarkdown = (text) => {
  if (!text) return ''
  return marked(text)
}

const getSelectedWordsDetails = () => {
  if (!generatedArticle.value) return []
  // We can filter currentWords if they are still loaded, or parse from usedWordIds if available
  // Assuming currentWords are still valid for the current list
  return currentWords.value.filter(w => selectedWordIds.has(w.id))
}

const downloadHTML = () => {
  markActive()
  if (!generatedArticle.value) return
  
  const words = getSelectedWordsDetails()
  const wordRows = words.map(w => `
    <tr>
      <td><strong>${w.word}</strong></td>
      <td>${w.definition}</td>
      <td>${w.partOfSpeech || ''}</td>
    </tr>
  `).join('')
  
  const htmlContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>${generatedArticle.value.topic}</title>
      <style>
        body { font-family: sans-serif; max-width: 800px; margin: 0 auto; padding: 40px; line-height: 1.6; }
        h1 { color: #2c3e50; text-align: center; }
        .meta { text-align: center; color: #7f8c8d; margin-bottom: 30px; }
        .content { font-size: 18px; margin-bottom: 50px; text-align: justify; }
        strong { color: #e74c3c; } /* Highlight bold words */
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #bdc3c7; padding: 10px; text-align: left; }
        th { background-color: #ecf0f1; }
      </style>
    </head>
    <body>
      <h1>${generatedArticle.value.topic}</h1>
      <div class="meta">Difficulty: ${articleOptions.difficulty} | Length: ${articleOptions.length}</div>
      <div class="content">
        ${renderMarkdown(generatedArticle.value.originalText)}
      </div>
      <div class="vocabulary">
        <h3>Vocabulary List</h3>
        <table>
          <thead><tr><th>Word</th><th>Definition</th><th>Part of Speech</th></tr></thead>
          <tbody>${wordRows}</tbody>
        </table>
      </div>
    </body>
    </html>
  `
  
  const blob = new Blob([htmlContent], { type: 'text/html' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${generatedArticle.value.topic}.html`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const downloadPDF = () => {
  markActive()
  window.print()
}

// Other existing methods
const createList = async () => {
  markActive()
  if (!newList.value.name.trim()) {
    alert('请输入单词表名称')
    return
  }
  
  const result = await vocabularyStore.createList({
    name: newList.value.name.trim(),
    description: newList.value.description?.trim() || '',
    language: newList.value.language || 'en'
  })
  if (!result.success) {
    alert('创建失败: ' + (result.message || '未知错误'))
    return
  }
  showCreateList.value = false
  newList.value = { name: '', description: '', language: 'en' }
  if (result.data?.id) {
    await selectList(result.data.id)
    currentView.value = 'my-words' // Switch to list view
  }
}

const addWord = async () => {
  markActive()
  if (!currentListId.value) return
  if (!newWord.value.word.trim() || !newWord.value.definition.trim()) {
    alert('请填写单词和释义')
    return
  }

  const result = await vocabularyStore.addWord(currentListId.value, {
    word: newWord.value.word.trim(),
    definition: newWord.value.definition.trim(),
    partOfSpeech: newWord.value.partOfSpeech?.trim() || '',
    example: newWord.value.example?.trim() || '',
    language: currentList.value?.language || 'en'
  })
  if (!result.success) {
    alert('添加失败: ' + (result.message || '未知错误'))
    return
  }
  showAddWord.value = false
  newWord.value = { word: '', definition: '', partOfSpeech: '', example: '' }
  await vocabularyStore.recordActivity({
    activityType: 'vocabulary_add_word',
    activityDetails: JSON.stringify({ listId: currentListId.value, wordId: result.data?.id }),
    duration: 0
  })
}

const refreshCurrentList = async () => {
  markActive()
  if (!currentListId.value) return
  await vocabularyStore.fetchWords(currentListId.value)
  await vocabularyStore.fetchListProgress(currentListId.value)
}

const refreshOverview = async () => {
  markActive()
  await vocabularyStore.fetchStats()
}

const refreshReview = async () => {
  markActive()
  await vocabularyStore.fetchReviewWords()
}

const getListWordCount = (listId) => {
  // Now using list.wordCount from backend if available, fallback to length
  const list = vocabularyLists.value.find(l => l.id === listId)
  if (list && list.wordCount !== undefined) return list.wordCount
  
  const ws = vocabularyStore.wordsByListId[listId]
  return Array.isArray(ws) ? ws.length : 0
}

const getWordProgress = (wordId) => {
  return vocabularyStore.progressByWordId[wordId] || { masteryLevel: 0, isDifficult: false }
}

const getReviewWordId = (item) => {
  return item?.wordId ?? item?.word?.id ?? item?.word_id ?? item?.word?.wordId ?? item?.word?.word_id ?? null
}

const getMasteryText = (level) => {
  const l = Number(level || 0)
  if (l >= 5) return '完全掌握'
  if (l >= 4) return '已掌握'
  if (l >= 2) return '学习中'
  return '新单词'
}

const getMasteryClass = (level) => {
  const l = Number(level || 0)
  if (l >= 4) return 'mastered'
  if (l >= 2) return 'learning'
  return 'new'
}

const changeMastery = async (wordId, value) => {
  markActive()
  const masteryLevel = Number(value)
  const current = getWordProgress(wordId)
  const result = await vocabularyStore.updateProgress({
    wordId,
    masteryLevel,
    isDifficult: current.isDifficult
  })
  if (!result.success) {
    alert('更新失败: ' + (result.message || '未知错误'))
    return
  }
  await Promise.all([vocabularyStore.fetchStats(), vocabularyStore.fetchReviewWords()])
}

const removeWord = async (wordId) => {
  markActive()
  if (!currentListId.value) return
  if (!confirm('确定要删除这个单词吗？')) return
  const result = await vocabularyStore.deleteWord(currentListId.value, wordId)
  if (!result.success) {
    alert('删除失败: ' + (result.message || '未知错误'))
  }
}

const removeList = async (listId) => {
  markActive()
  if (!confirm('确定要删除这个单词表吗？')) return
  const result = await vocabularyStore.deleteList(listId)
  if (!result.success) {
    alert('删除失败: ' + (result.message || '未知错误'))
    return
  }
  if (currentListId.value === listId) {
    currentListId.value = null
    generatedArticle.value = null
    publicKeyword.value = ''
  }
}

const quickReview = async (wordId, masteryLevel) => {
  markActive()
  if (wordId == null) {
    alert('单词ID不能为空')
    return
  }
  const current = getWordProgress(wordId)
  const result = await vocabularyStore.updateProgress({
    wordId,
    masteryLevel,
    isDifficult: current.isDifficult
  })
  if (!result.success) {
    alert('更新失败: ' + (result.message || '未知错误'))
    return
  }
  await vocabularyStore.recordActivity({
    activityType: 'vocabulary_review',
    activityDetails: JSON.stringify({ wordId, masteryLevel }),
    duration: 0
  })
  await Promise.all([vocabularyStore.fetchStats(), vocabularyStore.fetchReviewWords()])
}

const searchPublic = async () => {
  markActive()
  currentPage.value = 1
  await fetchPublicPage(1)
}

const addPublicWord = async (w) => {
  markActive()
  if (!currentListId.value) {
    alert('请先选择一个单词表')
    return
  }
  const result = await vocabularyStore.addWord(currentListId.value, {
    word: w.word,
    definition: w.definition,
    partOfSpeech: w.partOfSpeech,
    example: w.example,
    language: w.language || currentList.value?.language || 'en'
  })
  if (!result.success) {
    alert('添加失败: ' + (result.message || '未知错误'))
    return
  }
  await vocabularyStore.recordActivity({
    activityType: 'vocabulary_add_public_word',
    activityDetails: JSON.stringify({ listId: currentListId.value, publicWordId: w.id, wordId: result.data?.id }),
    duration: 0
  })
}

const formatDuration = (seconds) => {
  const s = Math.max(0, Number(seconds || 0))
  const m = Math.floor(s / 60)
  const h = Math.floor(m / 60)
  const mm = m % 60
  if (h > 0) return `${h}小时${mm}分钟`
  if (mm > 0) return `${mm}分钟`
  return `${s}秒`
}
</script>

<style scoped>
/* Page Layout */
.language-learning-page {
  display: flex;
  height: calc(100vh - 64px); /* Fixed height */
  overflow: hidden;
  background-color: #f5f7fa;
}

/* Dark mode support */
.dark-mode .language-learning-page {
  background-color: #0f172a;
}

/* Mobile Header */
.mobile-header {
  display: none;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
}

.dark-mode .mobile-header {
  background-color: #1e293b;
  border-bottom: 1px solid #334155;
}

.mobile-menu-btn {
  background: transparent;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #3b82f6;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.mobile-menu-btn:hover {
  background-color: #eff6ff;
}

.mobile-title {
  font-size: 18px;
  color: #1e293b;
  margin: 0;
  font-weight: 600;
}

/* Sidebar */
.sidebar {
  width: 240px;
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.dark-mode .sidebar {
  background-color: #1e293b;
  border-right: 1px solid #334155;
}

.sidebar-mobile-open {
  transform: translateX(0);
}

.close-mobile-menu {
  display: none;
  background: transparent;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #64748b;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.sidebar-header {
  padding: 24px;
  border-bottom: 1px solid #f1f5f9;
}

.sidebar-header h2 {
  font-size: 18px;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-nav {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  color: #64748b;
  text-decoration: none;
  transition: all 0.2s;
}

.nav-item:hover {
  background-color: #f8fafc;
  color: #3b82f6;
}

.nav-item.active {
  background-color: #eff6ff;
  color: #3b82f6;
  font-weight: 500;
}

.nav-item .icon {
  font-size: 18px;
}

/* Main Content */
.main-content {
  flex: 1;
  overflow-y: auto; /* Scroll internally */
  padding: 32px;
  position: relative;
}

.dark-mode .main-content {
  background-color: #0f172a;
}

.view-header {
  margin-bottom: 32px;
}

.view-header h2 {
  font-size: 24px;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.view-header p {
  color: #64748b;
  margin: 0;
}

/* Dashboard Grid - Improved */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.03);
  transition: transform 0.2s;
}

.dark-mode .stat-card {
  background: #1e293b;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0,0,0,0.06);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background-color: #f8fafc;
}

.stat-card.primary .stat-icon { background-color: #eff6ff; color: #3b82f6; }
.stat-card.success .stat-icon { background-color: #dcfce7; color: #22c55e; }
.stat-card.warning .stat-icon { background-color: #fef3c7; color: #f59e0b; }
.stat-card.info .stat-icon { background-color: #e0f2fe; color: #0ea5e9; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

/* Review Section */
.review-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.review-card-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-word {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.review-def {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.review-actions {
  display: flex;
  gap: 8px;
}

/* My Words Layout */
.two-column-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  height: calc(100vh - 160px); /* Fit within main content */
}

.list-column, .words-column {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-container {
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-item {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.list-item:hover {
  border-color: #3b82f6;
  background-color: #f8fafc;
}

.list-item.active {
  border-color: #3b82f6;
  background-color: #eff6ff;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.1);
}

.list-info h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  color: #1e293b;
}

.badge {
  display: inline-block;
  padding: 2px 6px;
  background: #e2e8f0;
  border-radius: 4px;
  font-size: 10px;
  color: #64748b;
  margin-right: 8px;
}

.count {
  font-size: 12px;
  color: #94a3b8;
}

.words-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
  overflow-y: auto;
  padding: 4px;
}

.word-card-item {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.word-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.word-header h4 {
  margin: 0;
  font-size: 18px;
  color: #3b82f6;
}

.pos {
  font-size: 12px;
  color: #64748b;
  font-style: italic;
}

.word-body {
  flex: 1;
  margin-bottom: 12px;
}

.definition {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.example {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.word-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
}
.status-tag.new { background: #eff6ff; color: #3b82f6; }
.status-tag.learning { background: #fef3c7; color: #d97706; }
.status-tag.mastered { background: #dcfce7; color: #16a34a; }

.controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.select-sm {
  padding: 2px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  font-size: 12px;
}

/* AI Wizard Styles */
.step-container {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px;
}

.step-container h3 {
  margin-bottom: 24px;
  color: #334155;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #334155;
}

.select-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 14px;
  background-color: #ffffff;
  transition: border-color 0.2s;
}

.select-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.word-selection {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
  max-height: 500px;
  overflow-y: auto;
}

.selection-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 12px;
}

.word-checkboxes {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.word-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.word-checkbox:hover {
  background-color: #f8fafc;
}

/* Pagination Styles */
.pagination-container {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination-info {
  font-size: 14px;
  color: #64748b;
  text-align: center;
}

.pagination-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.pagination-page {
  font-size: 14px;
  color: #64748b;
  padding: 0 12px;
}

.btn-outline {
  background-color: transparent;
  border: 1px solid #cbd5e1;
  color: #3b82f6;
  transition: all 0.2s;
}

.btn-outline:hover:not(:disabled) {
  background-color: #eff6ff;
  border-color: #3b82f6;
}

.btn-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.step-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 24px;
  padding: 24px;
  border-top: 1px solid #f1f5f9;
  background-color: #f8fafc;
  border-radius: 0 0 8px 8px;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.topic-card {
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 12px;
}

.topic-card:hover {
  border-color: #94a3b8;
}

.topic-card.selected {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.topic-text {
  font-weight: 500;
}

.loading-state {
  text-align: center;
  padding: 60px;
  color: #64748b;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.article-display-area {
  background: white;
  padding: 40px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.article-content :deep(strong) {
  color: #3b82f6;
  font-weight: 700;
}

.vocabulary-list-append {
  margin-top: 40px;
  padding-top: 40px;
  border-top: 1px solid #e2e8f0;
}

.vocabulary-list-append table {
  width: 100%;
  border-collapse: collapse;
}

.vocabulary-list-append th,
.vocabulary-list-append td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.print-only {
  display: none;
}

@media print {
  body * {
    visibility: hidden;
  }
  #printable-article, #printable-article * {
    visibility: visible;
  }
  #printable-article {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    border: none;
  }
  .print-only {
    display: block;
  }
}

/* Modals */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 100%;
  max-width: 400px;
}

.input, .textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  margin-bottom: 12px;
  font-size: 16px; /* Prevent zoom on iOS */
  min-height: 44px; /* Touch target size */
}

.textarea {
  min-height: 120px;
  resize: vertical;
  font-size: 16px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

/* Buttons */
.btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  min-height: 44px; /* Touch target size */
  font-weight: 500;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}
.btn-primary:hover { background: #2563eb; }
.btn-primary:disabled { background: #94a3b8; cursor: not-allowed; }

.btn-secondary { background: #f1f5f9; color: #475569; }
.btn-secondary:hover { background: #e2e8f0; }

.btn-outline {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #475569;
}
.btn-outline:hover { border-color: #3b82f6; color: #3b82f6; }

.btn-text {
  background: transparent;
  color: #64748b;
  padding: 4px 8px;
}
.btn-text:hover { color: #3b82f6; }

.btn-icon {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  color: #94a3b8;
  border-radius: 4px;
}
.btn-icon:hover { background: #f1f5f9; color: #ef4444; }

.empty-state { 
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}
.empty-state.large { padding: 80px; }
.empty-state .illustration { font-size: 48px; margin-bottom: 16px; }

/* Responsive Design */
@media (max-width: 768px) {
  .mobile-header {
    display: flex;
  }

  .language-learning-page {
    flex-direction: column;
    height: auto;
    overflow: visible;
  }

  .sidebar {
    position: fixed;
    top: 64px;
    left: 0;
    bottom: 0;
    width: 280px;
    z-index: 1000;
    transform: translateX(-100%);
    box-shadow: 2px 0 10px rgba(0,0,0,0.1);
  }

  .sidebar-mobile-open {
    transform: translateX(0);
  }

  .close-mobile-menu {
    display: block;
  }

  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .main-content {
    padding: 16px;
    margin-top: 64px; /* Account for mobile header */
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 24px;
  }

  .two-column-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .words-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .word-card-item {
    padding: 12px;
  }

  .word-header h4 {
    font-size: 16px;
  }

  .review-grid {
    grid-template-columns: 1fr;
  }

  .word-checkboxes {
    grid-template-columns: 1fr;
  }

  .topics-grid {
    grid-template-columns: 1fr;
  }

  .article-display-area {
    padding: 16px;
  }

  .modal-card {
    margin: 16px;
    max-width: calc(100% - 32px);
  }

  .step-container {
    padding: 0 16px;
  }

  .form-group {
    margin-bottom: 16px;
  }

  .select-input {
    padding: 14px 16px;
    font-size: 16px;
  }

  .word-selection {
    max-height: 400px;
    padding: 12px;
  }

  .word-checkboxes {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .word-checkbox {
    padding: 12px;
  }

  .step-actions {
    flex-direction: column;
    padding: 16px;
  }

  .btn {
    min-height: 48px;
    font-size: 16px;
    padding: 12px 24px;
  }

  .pagination-container {
    margin-top: 16px;
    padding-top: 16px;
  }

  .pagination-info {
    font-size: 13px;
  }

  .pagination-buttons {
    gap: 8px;
  }

  .pagination-page {
    font-size: 13px;
    padding: 0 8px;
  }

  .btn-outline {
    min-height: 40px;
    font-size: 14px;
    padding: 8px 16px;
  }

  .btn-icon {
    min-height: 40px;
    min-width: 40px;
    font-size: 18px;
  }

  .input, .textarea {
    padding: 14px 16px;
    font-size: 16px;
  }

  .nav-item {
    padding: 16px;
    font-size: 16px;
  }

  .nav-item .icon {
    font-size: 20px;
  }

  .stat-card {
    padding: 20px;
  }

  .stat-value {
    font-size: 24px;
  }

  .stat-label {
    font-size: 14px;
  }
}

@media (max-width: 1024px) and (min-width: 769px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .words-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>
