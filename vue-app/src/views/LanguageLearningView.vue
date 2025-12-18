<template>
  <AppLayout>
    <div class="language-learning-page">
      <div class="container">
        <div class="page-header">
          <h1>📚 语言学习</h1>
          <p>创建单词表，跟踪学习进度</p>
        </div>
        
        <div class="content-grid">
          <!-- 单词表列表 -->
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
                  <p>{{ list.word_count || 0 }} 个单词</p>
                </div>
                <div class="list-progress">
                  <div class="progress-circle">
                    {{ list.progress || 0 }}%
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 单词列表 -->
          <div class="word-list card">
            <div class="card-header">
              <h2>单词列表</h2>
              <button
                v-if="currentListId"
                class="btn btn-primary"
                @click="showAddWord = true"
              >
                ➕ 添加单词
              </button>
            </div>
            
            <div
              v-if="!currentListId"
              class="empty-state"
            >
              <p>请先选择一个单词表</p>
            </div>
            
            <div
              v-else-if="words.length === 0"
              class="empty-state"
            >
              <p>暂无单词，点击添加单词开始学习</p>
            </div>
            
            <div
              v-else
              class="words-container"
            >
              <div
                v-for="word in words"
                :key="word.id"
                class="word-card"
              >
                <div class="word-front">
                  <h3>{{ word.word }}</h3>
                  <p class="phonetic">
                    {{ word.phonetic }}
                  </p>
                </div>
                <div class="word-back">
                  <p class="translation">
                    {{ word.translation }}
                  </p>
                  <p
                    v-if="word.example"
                    class="example"
                  >
                    {{ word.example }}
                  </p>
                </div>
                <div class="word-status">
                  <span :class="['status-badge', word.status]">
                    {{ getStatusText(word.status) }}
                  </span>
                </div>
              </div>
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
              <h3>{{ generatedArticle.title }}</h3>
              <div v-html="generatedArticle.content" />
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
          v-model="newListName"
          type="text"
          class="input"
          placeholder="输入单词表名称"
          @keyup.enter="createList"
        >
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
          <label>音标</label>
          <input
            v-model="newWord.phonetic"
            type="text"
            class="input"
            placeholder="音标（可选）"
          >
        </div>
        <div class="form-group">
          <label>翻译</label>
          <input
            v-model="newWord.translation"
            type="text"
            class="input"
            placeholder="翻译"
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
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import AppLayout from '@/components/AppLayout.vue'

const vocabularyLists = ref([])
const currentListId = ref(null)
const words = ref([])
const showCreateList = ref(false)
const showAddWord = ref(false)
const newListName = ref('')
const newWord = ref({
  word: '',
  phonetic: '',
  translation: '',
  example: ''
})
const isGenerating = ref(false)
const generatedArticle = ref(null)

onMounted(async () => {
  await fetchVocabularyLists()
})

const fetchVocabularyLists = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.language.vocabularyLists)
    vocabularyLists.value = response.lists || []
  } catch (error) {
    console.error('获取单词表失败:', error)
  }
}

const selectList = async (listId) => {
  currentListId.value = listId
  try {
    const response = await request.get(API_ENDPOINTS.language.getWords(listId))
    words.value = response.words || []
  } catch (error) {
    console.error('获取单词失败:', error)
  }
}

const createList = async () => {
  if (!newListName.value.trim()) {
    alert('请输入单词表名称')
    return
  }
  
  try {
    await request.post(API_ENDPOINTS.language.createList, {
      name: newListName.value
    })
    
    showCreateList.value = false
    newListName.value = ''
    await fetchVocabularyLists()
  } catch (error) {
    alert('创建失败: ' + (error.response?.data?.detail || '未知错误'))
  }
}

const addWord = async () => {
  if (!newWord.value.word.trim() || !newWord.value.translation.trim()) {
    alert('请填写单词和翻译')
    return
  }
  
  try {
    await request.post(API_ENDPOINTS.language.addWord, {
      list_id: currentListId.value,
      ...newWord.value
    })
    
    showAddWord.value = false
    newWord.value = { word: '', phonetic: '', translation: '', example: '' }
    await selectList(currentListId.value)
  } catch (error) {
    alert('添加失败: ' + (error.response?.data?.detail || '未知错误'))
  }
}

const generateArticle = async () => {
  isGenerating.value = true
  
  try {
    const response = await request.post(API_ENDPOINTS.language.generateArticle, {
      list_id: currentListId.value
    })
    
    generatedArticle.value = response.article
  } catch (error) {
    alert('生成失败: ' + (error.response?.data?.detail || '未知错误'))
  } finally {
    isGenerating.value = false
  }
}

const getStatusText = (status) => {
  const statusMap = {
    new: '新单词',
    learning: '学习中',
    mastered: '已掌握'
  }
  return statusMap[status] || '未学习'
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
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
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
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>

