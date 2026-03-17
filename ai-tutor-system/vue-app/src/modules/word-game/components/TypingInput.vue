<template>
  <div class="typing-area">
    <div class="words-wrapper">
      <template v-for="(w, i) in words" :key="i">
        <div
          v-if="isWord(w)"
          class="word-slot"
          :class="getWordClass(i)"
          :style="{ minWidth: `${getWordWidth(w)}ch` }"
        >
          {{ findWordById(i)?.userInput }}
        </div>
        <div v-else class="word-punct">{{ w }}</div>
      </template>

      <input
        ref="inputEl"
        lang="en"
        class="hidden-input"
        type="text"
        autocomplete="off"
        autocorrect="off"
        autocapitalize="off"
        spellcheck="false"
        v-model="inputValue"
        @keydown="handleKeydown"
        @focus="onFocus"
        @blur="onBlur"
        @dblclick.prevent
        @mousedown="preventCursorMove"
        @compositionstart="onCompositionStart"
        @compositionend="onCompositionEnd"
        autofocus
      />
    </div>

    <div v-if="showAnswerTip" class="answer-tip">
      <span class="tip-label">答案：</span>
      <span class="tip-content">{{ english }}</span>
    </div>

    <button type="button" class="mobile-submit-btn" @click="doSubmitAnswer">
      提交 / 下一词
    </button>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { isWord, useInput } from '@/modules/word-game/composables/useQuestion'
import { playEnglishSound } from '@/modules/word-game/composables/useEnglishSound'

const props = defineProps({
  english: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['correct', 'wrong'])
const words = computed(() => (props.english || '').split(' '))
const inputEl = ref(null)
const isComposing = ref(false)
const isFocused = ref(false)
const showAnswerTip = ref(false)

function focusInput() {
  inputEl.value?.focus()
}

function onFocus() {
  isFocused.value = true
}

function onBlur() {
  isFocused.value = false
}

function preventCursorMove(event) {
  event.preventDefault()
  focusInput()
}

function toggleAnswerTip() {
  showAnswerTip.value = !showAnswerTip.value
}

function getInputCursorPosition() {
  return inputEl.value?.selectionStart ?? 0
}

function setInputCursorPosition(pos) {
  if (inputEl.value) {
    inputEl.value.setSelectionRange(pos, pos)
  }
}

const {
  inputValue,
  findWordById,
  submitAnswer,
  handleKeyboardInput,
  isFixMode,
  initialize,
  setInputValue,
  resetUserInputWords
} = useInput({
  source: () => props.english || '',
  setInputCursorPosition,
  getInputCursorPosition
})

watch(
  () => props.english,
  () => {
    resetUserInputWords()
    initialize()
    showAnswerTip.value = false
    focusInput()
    playEnglishSound(props.english)
  },
  { immediate: true }
)

watch(inputValue, val => {
  setInputValue(val)
})

function onCompositionStart() {
  isComposing.value = true
}

function onCompositionEnd() {
  isComposing.value = false
}

function handleKeydown(e) {
  if (e.code === 'Backspace' && e.ctrlKey) {
    e.preventDefault()
    deleteLastWord()
    return
  }
  if (e.ctrlKey) {
    e.preventDefault()
    return
  }
  if (e.code === 'Enter' && !isComposing.value) {
    e.stopPropagation()
    doSubmitAnswer()
    return
  }
  handleKeyboardInput(e, {
    useSpaceSubmitAnswer: {
      enable: true,
      rightCallback: () => emit('correct'),
      errorCallback: () => emit('wrong')
    }
  })
}

function doSubmitAnswer() {
  submitAnswer(
    () => emit('correct'),
    () => emit('wrong')
  )
  focusInput()
}

function deleteLastWord() {
  if (!inputEl.value) return
  const start = inputEl.value.selectionStart ?? 0
  if (start === 0) return
  let pos = start
  while (pos > 0 && inputValue.value[pos - 1] === ' ') pos -= 1
  const valueToCursor = inputValue.value.substring(0, pos)
  const newEnd = valueToCursor.lastIndexOf(' ') + 1 || 0
  inputValue.value = inputValue.value.substring(0, newEnd)
  inputEl.value.setSelectionRange(newEnd, newEnd)
}

function getWordClass(index) {
  const word = findWordById(index)
  if (!word) return ''
  if (word.isActive && isFocused.value) return 'word-active'
  if (word.incorrect && isFocused.value) {
    return isFixMode() ? 'word-error word-shake' : 'word-error'
  }
  return 'word-default'
}

function getWordWidth(word) {
  return Math.max((word || '').length, 2)
}

onMounted(() => {
  initialize()
  focusInput()
  window.addEventListener('focus', focusInput)
})

onUnmounted(() => {
  window.removeEventListener('focus', focusInput)
})

defineExpose({ focusInput, toggleAnswerTip })
</script>

<style scoped>
.typing-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.words-wrapper {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: flex-end;
  gap: 10px;
}

.word-slot {
  height: 56px;
  border-bottom: 2px solid var(--word-border-default, #cbd5e1);
  font-size: 2.2rem;
  line-height: 1;
  padding: 0 4px;
  text-align: center;
  transition: color 0.15s ease, border-color 0.15s ease;
  border-radius: 2px;
}

.word-punct {
  height: 56px;
  font-size: 2.2rem;
  line-height: 1;
  color: var(--text-secondary);
}

.word-active {
  color: var(--word-active-color, #2563eb);
  border-color: var(--word-active-color, #2563eb);
}

.word-error {
  color: var(--word-error-color, #dc2626);
  border-color: var(--word-error-color, #dc2626);
}

.word-shake {
  animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

.word-default {
  color: var(--word-default-color, #64748b);
  border-color: var(--word-border-default, #cbd5e1);
}

.hidden-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: default;
}

.answer-tip {
  padding: 10px 20px;
  background: var(--chip-bg, #eff6ff);
  border-radius: 8px;
  font-size: 1.1rem;
  color: var(--chip-color, #1d4ed8);
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.tip-label {
  font-weight: 600;
  margin-right: 6px;
}

.mobile-submit-btn {
  display: none;
}

@media (max-width: 768px) {
  .mobile-submit-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    max-width: 220px;
    padding: 12px 20px;
    margin-top: 8px;
    font-size: 1rem;
    font-weight: 600;
    color: #fff;
    background: var(--primary-color, #3b82f6);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
    transition: opacity 0.2s ease, transform 0.1s ease;
    -webkit-tap-highlight-color: transparent;
  }

  .mobile-submit-btn:active {
    opacity: 0.9;
    transform: scale(0.98);
  }
}

@keyframes shake {
  10%, 90% { transform: translate3d(-1px, 0, 0); }
  20%, 80% { transform: translate3d(2px, 0, 0); }
  30%, 50%, 70% { transform: translate3d(-4px, 0, 0); }
  40%, 60% { transform: translate3d(4px, 0, 0); }
}
</style>
