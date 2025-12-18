import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'

export const useVocabularyStore = defineStore('vocabulary', () => {
  const lists = ref([])
  const wordsByListId = ref({})
  const progressByWordId = ref({})
  const reviewWords = ref([])
  const stats = ref(null)
  const publicSearchResults = ref([])
  const isLoading = ref(false)

  async function fetchLists() {
    try {
      isLoading.value = true
      const response = await request.get(API_ENDPOINTS.vocabulary.lists)
      lists.value = response.lists || []
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '获取单词表失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function createList(payload) {
    try {
      isLoading.value = true
      const created = await request.post(API_ENDPOINTS.vocabulary.lists, payload)
      lists.value = [created, ...lists.value]
      return { success: true, data: created }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '创建单词表失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function deleteList(listId) {
    try {
      isLoading.value = true
      await request.delete(API_ENDPOINTS.vocabulary.deleteList(listId))
      lists.value = lists.value.filter(l => l.id !== listId)
      const next = { ...wordsByListId.value }
      delete next[listId]
      wordsByListId.value = next
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '删除单词表失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function fetchWords(listId) {
    try {
      isLoading.value = true
      const response = await request.get(API_ENDPOINTS.vocabulary.words(listId))
      const words = response.words || []
      wordsByListId.value = { ...wordsByListId.value, [listId]: words }
      return { success: true, data: words }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '获取单词失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function fetchListProgress(listId) {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.listProgress(listId))
      const progress = response.progress || []
      const map = { ...progressByWordId.value }
      for (const p of progress) {
        if (p && p.wordId != null) {
          map[p.wordId] = p
        }
      }
      progressByWordId.value = map
      return { success: true, data: progress }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '获取进度失败' }
    }
  }

  async function addWord(listId, payload) {
    try {
      isLoading.value = true
      const created = await request.post(API_ENDPOINTS.vocabulary.addWord(listId), payload)
      const current = wordsByListId.value[listId] || []
      wordsByListId.value = { ...wordsByListId.value, [listId]: [created, ...current] }
      return { success: true, data: created }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '添加单词失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function deleteWord(listId, wordId) {
    try {
      isLoading.value = true
      await request.delete(API_ENDPOINTS.vocabulary.deleteWord(wordId))
      const current = wordsByListId.value[listId] || []
      wordsByListId.value = { ...wordsByListId.value, [listId]: current.filter(w => w.id !== wordId) }
      const nextProgress = { ...progressByWordId.value }
      delete nextProgress[wordId]
      progressByWordId.value = nextProgress
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '删除单词失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function updateProgress(payload) {
    try {
      const response = await request.post(API_ENDPOINTS.vocabulary.updateProgress, payload)
      const wordId = response?.wordId ?? payload.wordId
      if (wordId != null) {
        progressByWordId.value = { ...progressByWordId.value, [wordId]: response }
      }
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '更新进度失败' }
    }
  }

  async function fetchReviewWords() {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.review)
      reviewWords.value = response.words || []
      const map = { ...progressByWordId.value }
      for (const p of reviewWords.value) {
        if (p && p.wordId != null) {
          map[p.wordId] = p
        }
      }
      progressByWordId.value = map
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '获取复习列表失败' }
    }
  }

  async function fetchStats() {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.stats)
      stats.value = response
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '获取统计失败' }
    }
  }

  async function recordActivity(payload) {
    try {
      await request.post(API_ENDPOINTS.vocabulary.activity, payload)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '记录学习活动失败' }
    }
  }

  async function searchPublic(keyword, language = 'en') {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.searchPublic, {
        params: { keyword, language }
      })
      publicSearchResults.value = response.words || []
      return { success: true, data: publicSearchResults.value }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '搜索公共词库失败' }
    }
  }

  async function generateTopics(words, language) {
    try {
      const response = await request.post(API_ENDPOINTS.vocabulary.generateTopics, { words, language })
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '生成主题失败' }
    }
  }

  async function generateArticle(payload) {
    try {
      const response = await request.post(API_ENDPOINTS.vocabulary.generateArticle, payload)
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '生成文章失败' }
    }
  }

  return {
    lists,
    wordsByListId,
    progressByWordId,
    reviewWords,
    stats,
    publicSearchResults,
    isLoading,
    fetchLists,
    createList,
    deleteList,
    fetchWords,
    fetchListProgress,
    addWord,
    deleteWord,
    updateProgress,
    fetchReviewWords,
    fetchStats,
    recordActivity,
    searchPublic,
    generateTopics,
    generateArticle
  }
})
