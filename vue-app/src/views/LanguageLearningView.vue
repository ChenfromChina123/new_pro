<template>
  <AppLayout>
    <div class="language-learning-page">
      <!-- Sidebar Navigation -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h2>📚 语言学习</h2>
        </div>
        <nav class="sidebar-nav">
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'dashboard' }"
            @click.prevent="currentView = 'dashboard'"
          >
            <span class="icon">📊</span>
            <span class="label">学习概览</span>
          </a>
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'my-words' }"
            @click.prevent="currentView = 'my-words'"
          >
            <span class="icon">📝</span>
            <span class="label">我的单词</span>
          </a>
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'public-library' }"
            @click.prevent="currentView = 'public-library'"
          >
            <span class="icon">🔍</span>
            <span class="label">公共词库</span>
          </a>
          <a
            href="#"
            class="nav-item"
            :class="{ active: currentView === 'ai-articles' }"
            @click.prevent="currentView = 'ai-articles'"
          >
            <span class="icon">🤖</span>
            <span class="label">AI文章</span>
          </a>
        </nav>
      </div>

      <!-- Main Content Area -->
      <div class="main-content">
        
        <!-- Dashboard View -->
        <div v-if="currentView === 'dashboard'" class="view-section dashboard-view">
          <div class="view-header">
            <h2>学习概览</h2>
            <p>查看你的学习进度和今日任务</p>
          </div>

          <div class="stats-card card">
            <div class="card-header">
              <h3>数据统计</h3>
              <button class="btn btn-text" @click="refreshOverview">
                刷新
              </button>
            </div>
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-value">{{ learningStats?.totalWords ?? 0 }}</div>
                <div class="stat-label">已学习单词</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ learningStats?.masteredWords ?? 0 }}</div>
                <div class="stat-label">已掌握</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ formatDuration(learningStats?.todayDuration ?? 0) }}</div>
                <div class="stat-label">今日时长</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ formatDuration(learningStats?.totalDuration ?? 0) }}</div>
                <div class="stat-label">总时长</div>
              </div>
            </div>
          </div>

          <div class="review-card card mt-4">
            <div class="card-header">
              <h3>今日复习</h3>
              <button class="btn btn-text" @click="refreshReview">
                刷新
              </button>
            </div>
            <div v-if="reviewItems.length === 0" class="empty-state">
              <p>暂无需要复习的单词</p>
            </div>
            <div v-else class="review-list">
              <div v-for="item in reviewItems" :key="item.id" class="review-item">
                <div class="review-main">
                  <div class="review-word">{{ item.word?.word || item.wordId }}</div>
                  <div class="review-definition">{{ item.word?.definition }}</div>
                </div>
                <div class="review-actions">
                  <button class="btn btn-sm btn-outline" @click="quickReview(item.wordId, Math.min((item.masteryLevel ?? 0) + 1, 5))">
                    认识
                  </button>
                  <button class="btn btn-sm btn-primary" @click="quickReview(item.wordId, 5)">
                    掌握
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- My Words View -->
        <div v-if="currentView === 'my-words'" class="view-section my-words-view">
          <div class="two-column-layout">
            <div class="list-column card">
              <div class="card-header">
                <h3>单词表</h3>
                <button class="btn btn-sm btn-primary" @click="showCreateList = true">
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
                    <span class="count">{{ getListWordCount(list.id) }} 词</span>
                  </div>
                  <button class="btn-icon delete-btn" @click.stop="removeList(list.id)" title="删除">
                    ×
                  </button>
                </div>
              </div>
            </div>

            <div class="words-column card">
              <div class="card-header">
                <h3>{{ currentList ? currentList.name : '单词列表' }}</h3>
                <div class="actions">
                  <button v-if="currentListId" class="btn btn-sm btn-outline" @click="refreshCurrentList">
                    刷新
                  </button>
                  <button v-if="currentListId" class="btn btn-sm btn-primary" @click="showAddWord = true">
                    + 添加单词
                  </button>
                </div>
              </div>

              <div v-if="!currentListId" class="empty-state">
                <p>请选择一个单词表查看详情</p>
              </div>
              <div v-else-if="currentWords.length === 0" class="empty-state">
                <p>暂无单词，点击上方按钮添加</p>
              </div>
              <div v-else class="words-grid">
                <div v-for="word in currentWords" :key="word.id" class="word-card-item">
                  <div class="word-header">
                    <h4>{{ word.word }}</h4>
                    <span class="pos">{{ word.partOfSpeech }}</span>
                  </div>
                  <div class="word-body">
                    <p class="definition">{{ word.definition }}</p>
                    <p v-if="word.example" class="example">{{ word.example }}</p>
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
                        <option v-for="i in 6" :key="i" :value="i-1">{{ i-1 }}</option>
                      </select>
                      <button class="btn-icon delete-btn" @click="removeWord(word.id)">🗑️</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Public Library View -->
        <div v-if="currentView === 'public-library'" class="view-section public-library-view">
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
              <button class="btn btn-primary search-btn" @click="searchPublic">
                搜索
              </button>
            </div>
            <div class="search-tips" v-if="!currentListId">
              ⚠️ 请先在"我的单词"中选择或创建一个目标单词表
            </div>
            <div class="search-tips" v-else>
              添加到: <strong>{{ currentList?.name }}</strong>
            </div>
          </div>

          <div class="public-results-grid">
            <div v-if="publicResults.length === 0" class="empty-state">
              <p>输入关键词搜索，或直接点击搜索查看推荐词汇</p>
            </div>
            <div v-else class="results-list">
              <div v-for="w in publicResults" :key="w.id" class="result-card">
                <div class="result-info">
                  <div class="result-header">
                    <h4>{{ w.word }}</h4>
                    <span class="pos-tag">{{ w.partOfSpeech }}</span>
                  </div>
                  <p class="result-def">{{ w.definition }}</p>
                </div>
                <button
                  class="btn btn-sm btn-primary"
                  :disabled="!currentListId"
                  @click="addPublicWord(w)"
                >
                  添加
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Articles View -->
        <div v-if="currentView === 'ai-articles'" class="view-section ai-articles-view">
          <div class="view-header">
            <h2>AI 生成学习文章</h2>
            <p>基于你的词汇表生成个性化阅读材料</p>
          </div>

          <div class="article-generator card">
            <div class="generator-controls">
              <div class="control-item">
                <label>来源单词表</label>
                <select v-model="currentListId" class="select-input">
                  <option :value="null" disabled>请选择单词表</option>
                  <option v-for="list in vocabularyLists" :key="list.id" :value="list.id">
                    {{ list.name }} ({{ getListWordCount(list.id) }}词)
                  </option>
                </select>
              </div>
              <button
                class="btn btn-primary generate-btn"
                :disabled="!currentListId || isGenerating"
                @click="generateArticle"
              >
                <span v-if="isGenerating" class="spinner"></span>
                {{ isGenerating ? '正在生成...' : '✨ 生成文章' }}
              </button>
            </div>
            
            <div v-if="generatedArticle" class="article-display">
              <div class="article-paper">
                <pre class="article-text">{{ generatedArticle }}</pre>
              </div>
            </div>
            <div v-else class="empty-state large">
              <div class="illustration">🤖</div>
              <p>选择一个单词表，AI 将为你生成一篇包含这些词汇的短文，帮助你通过上下文记忆。</p>
            </div>
          </div>
        </div>

      </div>
    </div>
    
    <!-- Dialogs -->
    <div v-if="showCreateList" class="modal-overlay" @click.self="showCreateList = false">
      <div class="modal-card">
        <h3>创建单词表</h3>
        <input v-model="newList.name" type="text" class="input" placeholder="输入单词表名称" @keyup.enter="createList">
        <textarea v-model="newList.description" class="input textarea" placeholder="描述（可选）"></textarea>
        <select v-model="newList.language" class="input">
          <option value="en">英语</option>
          <option value="ja">日语</option>
          <option value="ko">韩语</option>
          <option value="fr">法语</option>
          <option value="de">德语</option>
          <option value="es">西班牙语</option>
          <option value="zh">中文</option>
        </select>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showCreateList = false">取消</button>
          <button class="btn btn-primary" @click="createList">创建</button>
        </div>
      </div>
    </div>
    
    <div v-if="showAddWord" class="modal-overlay" @click.self="showAddWord = false">
      <div class="modal-card">
        <h3>添加单词</h3>
        <div class="form-group">
          <label>单词</label>
          <input v-model="newWord.word" type="text" class="input" placeholder="单词">
        </div>
        <div class="form-group">
          <label>释义</label>
          <input v-model="newWord.definition" type="text" class="input" placeholder="释义">
        </div>
        <div class="form-group">
          <label>词性</label>
          <input v-model="newWord.partOfSpeech" type="text" class="input" placeholder="例如: n., v., adj.">
        </div>
        <div class="form-group">
          <label>例句</label>
          <textarea v-model="newWord.example" class="input textarea" placeholder="例句（可选）"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showAddWord = false">取消</button>
          <button class="btn btn-primary" @click="addWord">添加</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import AppLayout from '@/components/AppLayout.vue'
import { useVocabularyStore } from '@/stores/vocabulary'

const currentView = ref('dashboard') // dashboard, my-words, public-library, ai-articles

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
const generatedArticle = ref('')

const vocabularyStore = useVocabularyStore()
const vocabularyLists = computed(() => vocabularyStore.lists)
const currentList = computed(() => vocabularyStore.lists.find(l => l.id === currentListId.value) || null)
const currentWords = computed(() => vocabularyStore.wordsByListId[currentListId.value] || [])
const reviewItems = computed(() => vocabularyStore.reviewWords)
const learningStats = computed(() => vocabularyStore.stats)
const publicResults = computed(() => vocabularyStore.publicSearchResults)

onMounted(async () => {
  await vocabularyStore.fetchLists()
  await Promise.all([
    vocabularyStore.fetchStats(),
    vocabularyStore.fetchReviewWords()
  ])
})

const selectList = async (listId) => {
  currentListId.value = listId
  if (!vocabularyStore.wordsByListId[listId]) {
    await vocabularyStore.fetchWords(listId)
  }
  await vocabularyStore.fetchListProgress(listId)
}

const createList = async () => {
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

const generateArticle = async () => {
  if (!currentListId.value) return
  isGenerating.value = true
  
  try {
    if (!vocabularyStore.wordsByListId[currentListId.value]) {
      await vocabularyStore.fetchWords(currentListId.value)
    }
    const ws = (vocabularyStore.wordsByListId[currentListId.value] || []).slice(0, 30)
    const vocabulary = ws.map(w => w.word).filter(Boolean).join(', ')
    const prompt = [
      '你是一名语言学习助教。',
      `请为我生成一篇 ${(currentList.value?.language || 'en').toUpperCase()} 学习文章：`,
      '1) 文章长度控制在 250-400 词。',
      '2) 尽量自然地包含以下词汇（可以变形）：',
      vocabulary || '（当前单词表为空）',
      '3) 文章后附：重点词汇清单（给出简短释义）。',
      '输出使用纯文本，分段清晰。'
    ].join('\n')
    const response = await request.post(API_ENDPOINTS.chat.ask, {
      prompt,
      session_id: null,
      model: 'deepseek-chat'
    })
    generatedArticle.value = response?.data?.answer || response?.answer || ''
    await vocabularyStore.recordActivity({
      activityType: 'article_generation',
      activityDetails: JSON.stringify({ listId: currentListId.value, wordCount: ws.length }),
      duration: 0
    })
  } catch (error) {
    alert('生成失败: ' + (error.response?.data?.message || error.message || '未知错误'))
  } finally {
    isGenerating.value = false
  }
}

const refreshCurrentList = async () => {
  if (!currentListId.value) return
  await vocabularyStore.fetchWords(currentListId.value)
  await vocabularyStore.fetchListProgress(currentListId.value)
}

const refreshOverview = async () => {
  await vocabularyStore.fetchStats()
}

const refreshReview = async () => {
  await vocabularyStore.fetchReviewWords()
}

const getListWordCount = (listId) => {
  const ws = vocabularyStore.wordsByListId[listId]
  return Array.isArray(ws) ? ws.length : 0
}

const getWordProgress = (wordId) => {
  return vocabularyStore.progressByWordId[wordId] || { masteryLevel: 0, isDifficult: false }
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
  if (!currentListId.value) return
  if (!confirm('确定要删除这个单词吗？')) return
  const result = await vocabularyStore.deleteWord(currentListId.value, wordId)
  if (!result.success) {
    alert('删除失败: ' + (result.message || '未知错误'))
  }
}

const removeList = async (listId) => {
  if (!confirm('确定要删除这个单词表吗？')) return
  const result = await vocabularyStore.deleteList(listId)
  if (!result.success) {
    alert('删除失败: ' + (result.message || '未知错误'))
    return
  }
  if (currentListId.value === listId) {
    currentListId.value = null
    generatedArticle.value = ''
    publicKeyword.value = ''
  }
}

const quickReview = async (wordId, masteryLevel) => {
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
  // Allow empty search to get random/default words if backend supports it
  const kw = publicKeyword.value.trim()
  const language = currentList.value?.language || 'en'
  const result = await vocabularyStore.searchPublic(kw, language)
  if (!result.success) {
    alert('搜索失败: ' + (result.message || '未知错误'))
  }
}

const addPublicWord = async (w) => {
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

/* Sidebar */
.sidebar {
  width: 240px;
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
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

/* Cards & Stats */
.card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 18px;
  margin: 0;
  color: #334155;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
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

/* Public Library */
.search-bar-container {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 24px;
}

.search-input-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.search-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 16px;
}

.search-tips {
  font-size: 13px;
  color: #64748b;
}

.results-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.result-card {
  background: white;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.result-header h4 {
  margin: 0;
  font-size: 16px;
}

.pos-tag {
  font-size: 11px;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}

.result-def {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* AI Articles */
.article-generator {
  min-height: 500px;
}

.generator-controls {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f1f5f9;
}

.control-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.select-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
}

.generate-btn {
  padding: 10px 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #ffffff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.article-paper {
  background: #fff;
  padding: 40px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  max-width: 800px;
  margin: 0 auto;
}

.article-text {
  font-family: 'Georgia', serif;
  font-size: 18px;
  line-height: 1.8;
  color: #1e293b;
  white-space: pre-wrap;
  margin: 0;
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
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  margin-bottom: 12px;
}

.textarea {
  min-height: 100px;
  resize: vertical;
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

</style>
