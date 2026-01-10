<template>
  <div class="translation-tool">
    <div class="tool-header">
      <h3>智能翻译</h3>
      <p>支持多种语言互译，由 AI 提供技术支持</p>
    </div>

    <div class="translation-grid">
      <!-- Source Text -->
      <div class="input-section">
        <div class="section-header">
          <label>原文</label>
          <div class="language-select">
            <select v-model="sourceLanguage">
              <option value="">自动检测</option>
              <option v-for="lang in languages" :key="lang.code" :value="lang.name">
                {{ lang.name }}
              </option>
            </select>
          </div>
        </div>
        <textarea
          v-model="sourceText"
          placeholder="请输入需要翻译的文本..."
          class="text-area"
        ></textarea>
        <div class="char-count">{{ sourceText.length }} / 5000</div>
      </div>

      <!-- Controls -->
      <div class="controls">
        <button class="swap-btn" @click="swapLanguages" title="交换语言">
          <i class="fas fa-exchange-alt"></i>
        </button>
        <button 
          class="translate-btn" 
          :disabled="loading || !sourceText.trim()"
          @click="handleTranslate"
        >
          <span v-if="!loading">翻译</span>
          <span v-else class="loading-spinner"></span>
        </button>
      </div>

      <!-- Target Text -->
      <div class="input-section">
        <div class="section-header">
          <label>译文</label>
          <div class="language-select">
            <select v-model="targetLanguage">
              <option v-for="lang in languages" :key="lang.code" :value="lang.name">
                {{ lang.name }}
              </option>
            </select>
          </div>
        </div>
        <div class="result-area" :class="{ 'is-loading': loading }">
          <template v-if="translatedText">
            {{ translatedText }}
          </template>
          <template v-else-if="!loading">
            <span class="placeholder">翻译结果将在这里显示...</span>
          </template>
        </div>
        <div class="result-actions" v-if="translatedText">
          <button class="action-btn" @click="copyResult" title="复制内容">
            <i class="far fa-copy"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'

const sourceText = ref('')
const translatedText = ref('')
const sourceLanguage = ref('')
const targetLanguage = ref('English')
const loading = ref(false)

const languages = [
  { code: 'zh', name: 'Chinese' },
  { code: 'en', name: 'English' },
  { code: 'ja', name: 'Japanese' },
  { code: 'ko', name: 'Korean' },
  { code: 'fr', name: 'French' },
  { code: 'de', name: 'German' },
  { code: 'es', name: 'Spanish' },
  { code: 'ru', name: 'Russian' }
]

const handleTranslate = async () => {
  if (!sourceText.value.trim()) return

  loading.value = true
  try {
    const response = await request.post(API_ENDPOINTS.translation.translate, {
      text: sourceText.value,
      targetLanguage: targetLanguage.value || 'English',
      sourceLanguage: sourceLanguage.value
    })
    
    if (response.success) {
      translatedText.value = response.data
    } else {
      alert(response.message || '翻译失败')
    }
  } catch (error) {
    console.error('Translation error:', error)
    alert('翻译出错，请稍后再试')
  } finally {
    loading.value = false
  }
}

const swapLanguages = () => {
  if (sourceLanguage.value === '') return // Cannot swap with auto-detect
  const temp = sourceLanguage.value
  sourceLanguage.value = targetLanguage.value
  targetLanguage.value = temp
  
  if (translatedText.value) {
    sourceText.value = translatedText.value
    translatedText.value = ''
  }
}

const copyResult = () => {
  if (!translatedText.value) return
  navigator.clipboard.writeText(translatedText.value)
    .then(() => alert('已复制到剪贴板'))
    .catch(err => console.error('Copy failed:', err))
}
</script>

<style scoped>
.translation-tool {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.tool-header {
  margin-bottom: 24px;
}

.tool-header h3 {
  margin: 0 0 8px 0;
  font-size: 1.5rem;
  color: #2c3e50;
}

.tool-header p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.translation-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

@media (min-width: 768px) {
  .translation-grid {
    grid-template-columns: 1fr 60px 1fr;
    align-items: center;
  }
}

.input-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header label {
  font-weight: 600;
  color: #2c3e50;
}

.language-select select {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background-color: #f8f9fa;
  outline: none;
  font-size: 0.9rem;
}

.text-area, .result-area {
  min-height: 200px;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #eee;
  font-size: 1rem;
  line-height: 1.6;
}

.text-area {
  width: 100%;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
}

.text-area:focus {
  border-color: #3498db;
}

.result-area {
  background-color: #f8f9fa;
  white-space: pre-wrap;
  position: relative;
}

.result-area.is-loading {
  opacity: 0.6;
}

.placeholder {
  color: #999;
}

.char-count {
  text-align: right;
  font-size: 0.8rem;
  color: #999;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.swap-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #eee;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.swap-btn:hover {
  background-color: #f0f0f0;
  transform: rotate(180deg);
}

.translate-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background-color: #3498db;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  min-width: 80px;
}

.translate-btn:hover:not(:disabled) {
  background-color: #2980b9;
}

.translate-btn:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.action-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 1.1rem;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.action-btn:hover {
  background-color: #f0f0f0;
  color: #3498db;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255,255,255,.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
