<template>
  <div class="vocab-modal-overlay">
    <div class="vocab-modal-container">
      <div class="modal-header">
        <div class="progress-indicator">
          {{ currentIndex + 1 }} / {{ words.length }}
        </div>
        <button class="close-btn" @click="emit('close')" title="退出练习">
          <i class="fas fa-times"></i>
        </button>
      </div>

      <div class="modal-body">
        <Transition name="slide-fade" mode="out-in">
          <div :key="currentIndex" class="card-wrapper">
            <VocabPracticeCard
              v-if="currentWord"
              :word="currentWord.word"
              :phonetic="currentWord.phonetic"
              :sentence="currentWord.sentence"
              :translation="currentWord.translation"
              :initialMode="currentWord.mode"
            />
            
            <!-- 进阶补充：单词详细释义展示 -->
            <div class="definition-box" v-if="currentWord && currentWord.definition">
              <h4 class="def-title">详细释义</h4>
              <p class="def-content">{{ currentWord.definition }}</p>
            </div>
          </div>
        </Transition>
      </div>

      <div class="modal-footer">
        <button 
          class="nav-btn prev-btn" 
          :disabled="currentIndex === 0" 
          @click="prevWord"
        >
          <i class="fas fa-chevron-left"></i> 上一个
        </button>
        <button 
          class="nav-btn next-btn" 
          :class="{ finish: currentIndex === words.length - 1 }"
          @click="nextWord"
        >
          {{ currentIndex === words.length - 1 ? '完成练习' : '下一个' }} <i v-if="currentIndex !== words.length - 1" class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import VocabPracticeCard from './VocabPracticeCard.vue'

const props = defineProps({
  words: {
    type: Array,
    required: true,
    default: () => []
  }
})

const emit = defineEmits(['close'])

const currentIndex = ref(0)
const currentWord = computed(() => props.words[currentIndex.value])

const nextWord = () => {
  if (currentIndex.value < props.words.length - 1) {
    currentIndex.value++
  } else {
    // 最后一题，直接关闭
    emit('close')
  }
}

const prevWord = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}
</script>

<style scoped>
.vocab-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 16px;
}

.vocab-modal-container {
  width: 100%;
  max-width: 540px;
  background: var(--bg-primary);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  height: 80vh;
  max-height: 800px;
}

.modal-header {
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
}

.progress-indicator {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--primary-color);
  background: rgba(59, 130, 246, 0.1);
  padding: 4px 12px;
  border-radius: 20px;
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.close-btn:hover {
  color: var(--text-primary);
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--bg-primary);
}

.card-wrapper {
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.definition-box {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--border-color);
}

.def-title {
  margin: 0 0 8px 0;
  font-size: 0.9rem;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.def-content {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text-primary);
  line-height: 1.6;
  white-space: pre-wrap;
}

.modal-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
}

.nav-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nav-btn:not(:disabled):hover {
  background: var(--bg-tertiary);
}

.nav-btn.next-btn {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.nav-btn.next-btn:hover {
  opacity: 0.9;
}

.nav-btn.finish {
  background: #10b981;
  border-color: #10b981;
}

/* 轮播切换动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from {
  transform: translateX(20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(-20px);
  opacity: 0;
}

/* 让内部的卡片撑满且去掉原来的外边距 */
:deep(.vocab-practice-card) {
  margin: 0 !important;
  max-width: 100% !important;
  box-shadow: none !important;
}
</style>
