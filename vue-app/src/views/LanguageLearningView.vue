<template>
  <AppLayout>
    <div class="language-learning-page">
      <div class="container">
        <div class="page-header">
          <h1>📚 语言学习</h1>
          <p>创建单词表，跟踪学习进度</p>
        </div>

        <div class="overview-grid">
          <div class="card">
            <div class="card-header">
              <h2>学习统计</h2>
              <button
                class="btn btn-secondary"
                @click="refreshOverview"
              >
                刷新
              </button>
            </div>
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-value">
                  {{ learningStats?.totalWords ?? 0 }}
                </div>
                <div class="stat-label">
                  已学习单词
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-value">
                  {{ learningStats?.masteredWords ?? 0 }}
                </div>
                <div class="stat-label">
                  已掌握
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-value">
                  {{ formatDuration(learningStats?.todayDuration ?? 0) }}
                </div>
                <div class="stat-label">
                  今日时长
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-value">
                  {{ formatDuration(learningStats?.totalDuration ?? 0) }}
                </div>
                <div class="stat-label">
                  总时长
                </div>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-header">
              <h2>今日复习</h2>
              <button
                class="btn btn-secondary"
                @click="refreshReview"
              >
                刷新
              </button>
            </div>
            <div
              v-if="reviewItems.length === 0"
              class="empty-state"
            >
              <p>暂无需要复习的单词</p>
            </div>
            <div
              v-else
              class="review-list"
            >
              <div
                v-for="item in reviewItems"
                :key="item.id"
                class="review-item"
              >
                <div class="review-main">
                  <div class="review-word">
                    {{ item.word?.word || item.wordId }}
                  </div>
                  <div class="review-definition">
                    {{ item.word?.definition }}
                  </div>
                </div>
                <div class="review-actions">
                  <button
                    class="btn btn-secondary"
                    @click="quickReview(item.wordId, Math.min((item.masteryLevel ?? 0) + 1, 5))"
                  >
                    认识
                  </button>
                  <button
                    class="btn btn-primary"
                    @click="quickReview(item.wordId, 5)"
                  >
                    掌握
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="content-grid">
          <div class="vocabulary-lists card">
            <div class="card-header">
              <h2>我的单词表</h2>
              <button
                class="btn btn-primary"
                @click="showCreateList = true"
              >
                ➕ 新建
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
                  <h3>{{ list.name }}</h3>
                  <p>{{ getListWordCount(list.id) }} 个单词</p>
                </div>
                <div class="list-progress">
                  <div class="progress-circle">
                    {{ (list.language || 'en').toUpperCase() }}
                  </div>
                </div>
                <button
                  class="btn btn-secondary list-delete"
                  @click.stop="removeList(list.id)"
                >
                  删除
                </button>
              </div>
            </div>
          </div>
          
          <div class="word-list card">
            <div class="card-header">
              <h2>单词列表</h2>
              <div class="word-actions">
                <button
                  v-if="currentListId"
                  class="btn btn-secondary"
                  @click="refreshCurrentList"
                >
                  刷新
                </button>
                <button
                  v-if="currentListId"
                  class="btn btn-primary"
                  @click="showAddWord = true"
                >
                  ➕ 添加单词
                </button>
              </div>
            </div>
            
            <div
              v-if="!currentListId"
              class="empty-state"
            >
              <p>请先选择一个单词表</p>
            </div>
            
            <div
              v-else-if="currentWords.length === 0"
              class="empty-state"
            >
              <p>暂无单词，点击添加单词开始学习</p>
            </div>
            
            <div
              v-else
              class="words-container"
            >
              <div
                v-for="word in currentWords"
                :key="word.id"
                class="word-card"
              >
                <div class="word-front">
                  <h3>{{ word.word }}</h3>
                  <p class="phonetic">
                    {{ word.partOfSpeech }}
                  </p>
                </div>
                <div class="word-back">
                  <p class="translation">
                    {{ word.definition }}
                  </p>
                  <p
                    v-if="word.example"
                    class="example"
                  >
                    {{ word.example }}
                  </p>
                </div>
                <div class="word-status">
                  <span :class="['status-badge', getMasteryClass(getWordProgress(word.id).masteryLevel)]">
                    {{ getMasteryText(getWordProgress(word.id).masteryLevel) }}
                  </span>
                  <div class="progress-controls">
                    <label class="toggle">
                      <input
                        type="checkbox"
                        :checked="!!getWordProgress(word.id).isDifficult"
                        @change="toggleDifficult(word.id, $event.target.checked)"
                      >
                      <span>难词</span>
                    </label>
                    <select
                      class="input mastery-select"
                      :value="getWordProgress(word.id).masteryLevel ?? 0"
                      @change="changeMastery(word.id, $event.target.value)"
                    >
                      <option value="0">
                        0
                      </option>
                      <option value="1">
                        1
                      </option>
                      <option value="2">
                        2
                      </option>
                      <option value="3">
                        3
                      </option>
                      <option value="4">
                        4
                      </option>
                      <option value="5">
                        5
                      </option>
                    </select>
                    <button
                      class="btn btn-secondary"
                      @click="removeWord(word.id)"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card mt-3">
          <div class="card-header">
            <h2>公共词库</h2>
          </div>
          <div class="public-search">
            <input
              v-model="publicKeyword"
              type="text"
              class="input"
              placeholder="搜索公共词库（单词/释义）"
              :disabled="!currentListId"
              @keyup.enter="searchPublic"
            >
            <button
              class="btn btn-secondary"
              :disabled="!currentListId || !publicKeyword.trim()"
              @click="searchPublic"
            >
              搜索
            </button>
          </div>
          <div
            v-if="publicResults.length === 0"
            class="empty-state"
          >
            <p>搜索后可一键添加到当前单词表</p>
          </div>
          <div
            v-else
            class="public-results"
          >
            <div
              v-for="w in publicResults"
              :key="w.id"
              class="public-item"
            >
              <div class="public-main">
                <div class="public-word">
                  {{ w.word }}
                </div>
                <div class="public-meta">
                  <span class="public-pos">{{ w.partOfSpeech }}</span>
                  <span class="public-def">{{ w.definition }}</span>
                </div>
              </div>
              <button
                class="btn btn-primary"
                :disabled="!currentListId"
                @click="addPublicWord(w)"
              >
                添加
              </button>
            </div>
          </div>
        </div>
        
        <!-- AI生成文章功能 -->
        <div class="card mt-3">
          <div class="card-header">
            <h2>🤖 AI生成学习文章</h2>
          </div>
          
          <div class="generate-section">
            <p>根据你的单词表生成个性化学习文章</p>
            <button
              class="btn btn-primary"
              :disabled="!currentListId || isGenerating"
              @click="generateArticle"
            >
              {{ isGenerating ? '生成中...' : '生成文章' }}
            </button>
            
            <div
              v-if="generatedArticle"
              class="article-content"
            >
              <pre class="article-pre">{{ generatedArticle }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 创建单词表对话框 -->
    <div
      v-if="showCreateList"
      class="modal"
      @click.self="showCreateList = false"
    >
      <div class="modal-content">
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
            class="btn btn-primary"
            @click="createList"
          >
            创建
          </button>
          <button
            class="btn btn-secondary"
            @click="showCreateList = false"
          >
            取消
          </button>
        </div>
      </div>
    </div>
    
    <!-- 添加单词对话框 -->
    <div
      v-if="showAddWord"
      class="modal"
      @click.self="showAddWord = false"
    >
      <div class="modal-content">
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
            placeholder="词性（可选）"
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
            class="btn btn-primary"
            @click="addWord"
          >
            添加
          </button>
          <button
            class="btn btn-secondary"
            @click="showAddWord = false"
          >
            取消
          </button>
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

const toggleDifficult = async (wordId, checked) => {
  const current = getWordProgress(wordId)
  const result = await vocabularyStore.updateProgress({
    wordId,
    masteryLevel: current.masteryLevel ?? 0,
    isDifficult: !!checked
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
  if (!currentListId.value) return
  const kw = publicKeyword.value.trim()
  if (!kw) return
  const language = currentList.value?.language || 'en'
  const result = await vocabularyStore.searchPublic(kw, language)
  if (!result.success) {
    alert('搜索失败: ' + (result.message || '未知错误'))
  }
}

const addPublicWord = async (w) => {
  if (!currentListId.value) return
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
.language-learning-page {
  min-height: calc(100vh - 64px);
  padding: 32px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.page-header p {
  color: var(--text-secondary);
  font-size: 16px;
}

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background-color: var(--bg-primary);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background-color: var(--bg-primary);
}

.review-main {
  min-width: 0;
  flex: 1;
}

.review-word {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.review-definition {
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.content-grid {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h2 {
  font-size: 20px;
  margin: 0;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.list-item:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-soft);
}

.list-item.active {
  border-color: var(--primary-color);
  background-color: rgba(52, 152, 219, 0.05);
}

.list-delete {
  margin-left: 10px;
  flex-shrink: 0;
}

.list-info h3 {
  font-size: 16px;
  margin: 0 0 4px 0;
}

.list-info p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.progress-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.word-actions {
  display: flex;
  gap: 10px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-secondary);
}

.words-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.word-card {
  padding: 20px;
  border: 2px solid var(--border-color);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.word-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-soft);
  transform: translateY(-4px);
}

.word-front h3 {
  font-size: 24px;
  margin: 0 0 8px 0;
  color: var(--primary-color);
}

.phonetic {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0 0 12px 0;
}

.translation {
  font-size: 16px;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.example {
  font-size: 14px;
  color: var(--text-secondary);
  font-style: italic;
  margin: 0;
}

.word-status {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.status-badge.new {
  background-color: rgba(52, 152, 219, 0.1);
  color: var(--primary-color);
}

.status-badge.learning {
  background-color: rgba(243, 156, 18, 0.1);
  color: #f39c12;
}

.status-badge.mastered {
  background-color: rgba(39, 174, 96, 0.1);
  color: var(--success-color);
}

.progress-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.mastery-select {
  width: 88px;
}

.public-search {
  display: flex;
  gap: 12px;
  align-items: center;
}

.public-results {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.public-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background-color: var(--bg-primary);
}

.public-main {
  min-width: 0;
  flex: 1;
}

.public-word {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.public-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: baseline;
  color: var(--text-secondary);
  font-size: 13px;
}

.public-pos {
  padding: 2px 8px;
  border-radius: 10px;
  background-color: rgba(52, 152, 219, 0.08);
  color: var(--primary-color);
  flex-shrink: 0;
}

.public-def {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.generate-section {
  text-align: center;
  padding: 20px 0;
}

.generate-section p {
  margin-bottom: 16px;
  color: var(--text-secondary);
}

.article-content {
  margin-top: 24px;
  padding: 24px;
  background-color: var(--chat-bg);
  border-radius: 12px;
  text-align: left;
}

.article-content h3 {
  margin-bottom: 16px;
  color: var(--primary-color);
}

.article-pre {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-family: inherit;
  color: var(--text-primary);
  line-height: 1.7;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: var(--bg-secondary);
  border-radius: 12px;
  padding: 24px;
  min-width: 400px;
  max-width: 90vw;
}

.modal-content h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
