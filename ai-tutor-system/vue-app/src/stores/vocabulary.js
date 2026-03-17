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
  const publicSearchTotal = ref(0)
  const aiArticleLibrary = ref([])
  const aiArticleLibraryTotal = ref(0)
  const isLoading = ref(false)

  function normalizeList(raw) {
    if (!raw) return raw
    const wordCount = raw.wordCount ?? raw.word_count ?? 0
    return { ...raw, wordCount }
  }

  function normalizeWord(raw) {
    if (!raw) return raw
    return {
      ...raw,
      partOfSpeech: raw.partOfSpeech ?? raw.part_of_speech ?? '',
      vocabularyListId: raw.vocabularyListId ?? raw.vocabulary_list_id
    }
  }

  function normalizeProgress(raw) {
    if (!raw) return raw
    return {
      ...raw,
      userId: raw.userId ?? raw.user_id,
      wordId: raw.wordId ?? raw.word_id ?? raw.word?.id ?? raw.word?.wordId ?? raw.word?.word_id,
      masteryLevel: raw.masteryLevel ?? raw.mastery_level ?? 0,
      isDifficult: raw.isDifficult ?? raw.is_difficult ?? false,
      lastReviewed: raw.lastReviewed ?? raw.last_reviewed,
      nextReviewDate: raw.nextReviewDate ?? raw.next_review_date,
      reviewCount: raw.reviewCount ?? raw.review_count ?? 0,
      createdAt: raw.createdAt ?? raw.created_at,
      updatedAt: raw.updatedAt ?? raw.updated_at,
      word: normalizeWord(raw.word)
    }
  }

  function normalizeStats(raw) {
    if (!raw) return raw
    return {
      ...raw,
      totalWords: raw.totalWords ?? raw.total_words ?? 0,
      masteredWords: raw.masteredWords ?? raw.mastered_words ?? 0,
      totalDuration: raw.totalDuration ?? raw.total_duration ?? 0,
      todayDuration: raw.todayDuration ?? raw.today_duration ?? 0
    }
  }

  function updateListWordCount(listId, nextCount) {
    const idx = lists.value.findIndex(l => l.id === listId)
    if (idx === -1) return
    const current = lists.value[idx]
    lists.value = [
      ...lists.value.slice(0, idx),
      { ...current, wordCount: nextCount },
      ...lists.value.slice(idx + 1)
    ]
  }

  async function fetchLists() {
    try {
      isLoading.value = true
      const response = await request.get(API_ENDPOINTS.vocabulary.lists)
      lists.value = (response.lists || []).map(normalizeList)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取单词表失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function createList(payload) {
    try {
      isLoading.value = true
      const created = await request.post(API_ENDPOINTS.vocabulary.lists, payload)
      lists.value = [normalizeList(created), ...lists.value]
      return { success: true, data: created }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '创建单词表失败' }
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
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '删除单词表失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function fetchWords(listId) {
    try {
      isLoading.value = true
      const response = await request.get(API_ENDPOINTS.vocabulary.words(listId))
      const words = (response.words || []).map(normalizeWord)
      wordsByListId.value = { ...wordsByListId.value, [listId]: words }
      const list = lists.value.find(l => l.id === listId)
      if (list && (list.wordCount == null || Number.isNaN(list.wordCount))) {
        updateListWordCount(listId, words.length)
      }
      return { success: true, data: words }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取单词失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function fetchListProgress(listId) {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.listProgress(listId))
      const progress = (response.progress || []).map(normalizeProgress)
      const map = { ...progressByWordId.value }
      for (const p of progress) {
        if (p && p.wordId != null) {
          map[p.wordId] = p
        }
      }
      progressByWordId.value = map
      return { success: true, data: progress }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取进度失败' }
    }
  }

  async function addWord(listId, payload) {
    try {
      isLoading.value = true
      const created = normalizeWord(await request.post(API_ENDPOINTS.vocabulary.addWord(listId), payload))
      const current = wordsByListId.value[listId] || []
      wordsByListId.value = { ...wordsByListId.value, [listId]: [created, ...current] }
      const list = lists.value.find(l => l.id === listId)
      if (list) {
        updateListWordCount(listId, (list.wordCount ?? 0) + 1)
      }
      return { success: true, data: created }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '添加单词失败' }
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
      const list = lists.value.find(l => l.id === listId)
      if (list) {
        updateListWordCount(listId, Math.max(0, (list.wordCount ?? 0) - 1))
      }
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '删除单词失败' }
    } finally {
      isLoading.value = false
    }
  }

  async function updateProgress(payload) {
    try {
      const response = normalizeProgress(await request.post(API_ENDPOINTS.vocabulary.updateProgress, {
        word_id: payload.wordId,
        mastery_level: payload.masteryLevel,
        is_difficult: payload.isDifficult
      }))
      const wordId = response?.wordId ?? payload.wordId
      if (wordId != null) {
        progressByWordId.value = { ...progressByWordId.value, [wordId]: response }
      }
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '更新进度失败' }
    }
  }

  async function fetchReviewWords() {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.review)
      reviewWords.value = (response.words || []).map(normalizeProgress)
      const map = { ...progressByWordId.value }
      for (const p of reviewWords.value) {
        if (p && p.wordId != null) {
          map[p.wordId] = p
        }
      }
      progressByWordId.value = map
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取复习列表失败' }
    }
  }

  async function fetchStats() {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.stats)
      stats.value = normalizeStats(response)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取统计失败' }
    }
  }

  async function recordActivity(payload) {
    try {
      await request.post(API_ENDPOINTS.vocabulary.activity, {
        activity_type: payload.activityType,
        activity_details: payload.activityDetails,
        duration: payload.duration
      })
      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '记录学习活动失败' }
    }
  }

  async function searchPublic(keyword, language = 'en', page = 1, size = 50) {
    try {
      const response = await request.get(API_ENDPOINTS.vocabulary.searchPublic, {
        params: { keyword, language, page, size }
      })
      publicSearchResults.value = (response.words || []).map(normalizeWord)
      publicSearchTotal.value = response.total ?? response.totalElements ?? response.total_elements ?? publicSearchResults.value.length
      return { success: true, data: publicSearchResults.value }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '搜索公共词库失败' }
    }
  }

  async function generateTopics(words, language) {
    try {
      const response = await request.post(API_ENDPOINTS.aiArticle.recommendTheme, {
        word_list: words,
        language
      }, { timeout: 60000 })
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '生成主题失败' }
    }
  }

  // Generate article
  async function generateArticle(payload) {
    try {
      const response = await request.post(API_ENDPOINTS.aiArticle.generate, {
        list_id: payload.listId,
        word_ids: payload.wordIds,
        word_list: payload.wordList,
        topic: payload.topic,
        target_language: payload.targetLanguage || 'en',
        difficulty: payload.difficulty,
        length_type: payload.length
      }, { timeout: 180000 })
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '生成文章失败' }
    }
  }

  async function fetchArticleWordLibrary(params = {}) {
    try {
      const response = await request.get(API_ENDPOINTS.aiArticle.wordLibrary, {
        params: {
          library_type: params.libraryType || 'mine',
          keyword: params.keyword || '',
          category: params.category || '',
          language: params.language || '',
          page_num: params.pageNum || 1,
          page_size: params.pageSize || 20
        }
      })
      aiArticleLibrary.value = response.list || []
      aiArticleLibraryTotal.value = response.total || 0
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取单词库失败' }
    }
  }

  /**
   * 获取当前用户生成的文章列表
   */
  async function fetchArticles(filters = {}) {
    try {
      const response = await request.get(API_ENDPOINTS.aiArticle.historyList, {
        params: {
          keyword: filters.keyword || '',
          target_language: filters.targetLanguage || '',
          start_time: filters.startTime || '',
          end_time: filters.endTime || '',
          page_num: filters.pageNum || 1,
          page_size: filters.pageSize || 10
        }
      })
      return { success: true, data: response.list || [], total: response.total || 0 }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取文章列表失败' }
    }
  }

  /**
   * 获取文章详情（包含文章内容与已用单词信息）
   */
  async function fetchArticle(articleId) {
    try {
      const response = await request.get(API_ENDPOINTS.aiArticle.historyDetail, {
        params: { record_id: articleId }
      })
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '获取文章详情失败' }
    }
  }

  async function deleteArticleHistory(recordIdList) {
    try {
      const response = await request.post(API_ENDPOINTS.aiArticle.deleteHistory, {
        record_id_list: recordIdList
      })
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '删除历史失败' }
    }
  }

  async function clearArticleHistory() {
    try {
      const response = await request.post(API_ENDPOINTS.aiArticle.clearHistory)
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '清空历史失败' }
    }
  }

  async function exportArticlePdf(payload) {
    try {
      const response = await request.post(API_ENDPOINTS.aiArticle.exportPdf, {
        record_id: payload.recordId,
        content: payload.content,
        theme: payload.theme
      }, { responseType: 'blob', timeout: 60000 })
      return { success: true, data: response }
    } catch (error) {
      return { success: false, message: error.response?.data?.detail || error.response?.data?.message || '导出PDF失败', error }
    }
  }

  return {
    lists,
    wordsByListId,
    progressByWordId,
    reviewWords,
    stats,
    publicSearchResults,
    publicSearchTotal,
    aiArticleLibrary,
    aiArticleLibraryTotal,
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
    generateArticle,
    fetchArticleWordLibrary,
    fetchArticles,
    fetchArticle,
    deleteArticleHistory,
    clearArticleHistory,
    exportArticlePdf
  }
})
