<template>
  <div class="answer-view">
    <div class="english-sentence">
      <span
        v-for="(word, idx) in englishWords"
        :key="`${word}-${idx}`"
        class="answer-word"
        @click="playWordSound(word)"
      >
        {{ word }}
      </span>
    </div>

    <div class="soundmark">
      {{ statement?.soundmark }}
    </div>
    <div class="chinese">
      {{ statement?.chinese }}
    </div>

    <div class="actions">
      <button
        class="btn btn-outline"
        @click="emit('retry')"
      >
        再来一次
      </button>
      <button
        class="btn btn-primary"
        @click="emit('next')"
      >
        下一题
        <span class="kbd-hint">Space / Enter</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { usePlayWordSound } from '@/modules/word-game/composables/useEnglishSound'

const props = defineProps({
  statement: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['next', 'retry'])
const { handlePlayWordSound } = usePlayWordSound()
const englishWords = computed(() => String(props.statement?.english || '').split(' ').filter(Boolean))

function playWordSound(word) {
  handlePlayWordSound(word)
}

function handleKeydown(e) {
  if (e.code === 'Space' || e.code === 'Enter') {
    e.preventDefault()
    emit('next')
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.answer-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.english-sentence {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  font-size: 2.5rem;
  font-weight: 600;
  color: var(--text-primary);
}

.answer-word {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  transition: color 0.15s ease;
}

.answer-word:hover {
  color: var(--primary-color);
}

.soundmark {
  font-size: 1rem;
  color: var(--text-tertiary);
  letter-spacing: 0.5px;
}

.chinese {
  font-size: 1.2rem;
  color: var(--text-secondary);
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.kbd-hint {
  font-size: 0.75rem;
  opacity: 0.75;
  margin-left: 4px;
  background: rgba(255, 255, 255, 0.2);
  padding: 1px 6px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .kbd-hint {
    display: none;
  }
}
</style>
