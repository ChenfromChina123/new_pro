<template>
  <div class="typing-area">
    <!-- 单词槽列表 + 隐藏的实际 input -->
    <div class="words-wrapper">
      <template v-for="(w, i) in courseStore.words" :key="i">
        <!-- 单词槽：显示用户已输入的内容 -->
        <div
          v-if="isWord(w)"
          class="word-slot"
          :class="getWordClass(i)"
          :style="{ minWidth: `${getWordWidth(w)}ch` }"
        >
          {{ findWordById(i)?.userInput }}
        </div>
        <!-- 标点/空格：直接显示 -->
        <div v-else class="word-punct">{{ w }}</div>
      </template>

      <!-- 隐藏的真实输入框，捕获所有键盘事件 -->
      <input
        lang="en"
        ref="inputEl"
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

    <!-- 答案提示区 -->
    <div v-if="showAnswerTip" class="answer-tip">
      <span class="tip-label">答案：</span>
      <span class="tip-content">{{ courseStore.currentStatement?.english }}</span>
    </div>

    <!-- 移动端专用：提交按钮（部分输入法无回车键时使用） -->
    <button
      type="button"
      class="mobile-submit-btn"
      @click="doSubmitAnswer"
    >
      提交 / 下一词
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from "vue";
import { useCourseStore } from "@/stores/courseStore";
import { useInput, isWord } from "@/composables/useQuestion";
import { useGameMode } from "@/composables/useGameMode";
import { playEnglishSound } from "@/composables/useEnglishSound";

const courseStore = useCourseStore();
const { showAnswer } = useGameMode();

/** 对实际 input 元素的引用 */
const inputEl = ref<HTMLInputElement | null>(null);
/** 是否正在使用输入法（避免中文 IME 误触发提交） */
const isComposing = ref(false);
/** 当前输入框是否聚焦 */
const isFocused = ref(false);
/** 是否展示答案提示 */
const showAnswerTip = ref(false);

/** 暴露给父组件调用 */
defineExpose({ focusInput, toggleAnswerTip });

// ---- 输入框焦点管理 ----

function focusInput() {
  inputEl.value?.focus();
}

function onFocus() {
  isFocused.value = true;
}

function onBlur() {
  isFocused.value = false;
}

/** 阻止鼠标点击移动光标 */
function preventCursorMove(event: MouseEvent) {
  event.preventDefault();
  focusInput();
}

/** 切换答案提示 */
function toggleAnswerTip() {
  showAnswerTip.value = !showAnswerTip.value;
}

// ---- 输入核心逻辑 ----

/**
 * 获取 input 元素当前光标位置
 */
function getInputCursorPosition() {
  return inputEl.value?.selectionStart ?? 0;
}

/**
 * 设置 input 元素光标位置
 */
function setInputCursorPosition(pos: number) {
  if (inputEl.value) {
    inputEl.value.setSelectionRange(pos, pos);
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
  source: () => courseStore.currentStatement?.english ?? "",
  setInputCursorPosition,
  getInputCursorPosition,
});

/** 每次切换题目时重新初始化 */
onMounted(() => {
  initialize();
  showAnswerTip.value = false;
  focusInput();
  window.addEventListener("focus", focusInput);
  // 题目出现时立即播放英文读音（原版行为：打字前先听发音）
  playEnglishSound(courseStore.currentStatement?.english);
});

onUnmounted(() => {
  window.removeEventListener("focus", focusInput);
});

watch(
  () => courseStore.statementIndex,
  () => {
    resetUserInputWords();
    initialize();
    showAnswerTip.value = false;
    focusInput();
    // 切换到新题目时立即播放英文读音
    playEnglishSound(courseStore.currentStatement?.english);
  },
);

/**
 * 监听 inputValue 变化，同步到各 word 的 userInput 状态
 * 这是 earthworm 打字逻辑的核心数据流
 */
watch(inputValue, (val) => {
  setInputValue(val);
});

// ---- 键盘事件 ----

function onCompositionStart() { isComposing.value = true; }
function onCompositionEnd() { isComposing.value = false; }

const emit = defineEmits<{
  (e: "correct"): void;
  (e: "wrong"): void;
}>();

function handleKeydown(e: KeyboardEvent) {
  // Windows 下 Ctrl+Backspace 删除上一个单词
  if (e.code === "Backspace" && e.ctrlKey) {
    e.preventDefault();
    deleteLastWord();
    return;
  }
  // 避免中文输入法下 Ctrl 键上屏
  if (e.ctrlKey) {
    e.preventDefault();
    return;
  }
  // Enter 提交（排除 IME 确认）
  if (e.code === "Enter" && !isComposing.value) {
    e.stopPropagation();
    doSubmitAnswer();
    return;
  }
  handleKeyboardInput(e, {
    useSpaceSubmitAnswer: {
      enable: true,
      rightCallback: () => emit("correct"),
      errorCallback: () => emit("wrong"),
    },
  });
}

function doSubmitAnswer() {
  submitAnswer(
    () => emit("correct"),
    () => emit("wrong"),
  );
  focusInput();
}

/** Windows 下 Ctrl+Backspace：删除光标前的上一个单词 */
function deleteLastWord() {
  if (!inputEl.value) return;
  const start = inputEl.value.selectionStart ?? 0;
  if (start === 0) return;
  let pos = start;
  // 先跳过连续空格
  while (pos > 0 && inputValue.value[pos - 1] === " ") pos--;
  const valueToCursor = inputValue.value.substring(0, pos);
  const newEnd = valueToCursor.lastIndexOf(" ") + 1 || 0;
  inputValue.value = inputValue.value.substring(0, newEnd);
  inputEl.value.setSelectionRange(newEnd, newEnd);
}

// ---- 样式 ----

/**
 * 计算单词槽的 CSS 类名
 * - 激活中：蓝色
 * - 错误：红色 + 可选抖动
 * - 默认：灰色
 */
function getWordClass(index: number) {
  const word = findWordById(index);
  if (!word) return "";
  if (word.isActive && isFocused.value) return "word-active";
  if (word.incorrect && isFocused.value) {
    return isFixMode() ? "word-error word-shake" : "word-error";
  }
  return "word-default";
}

/**
 * 根据单词长度估算最小宽度（字符数），避免槽过窄
 */
function getWordWidth(word: string) {
  return Math.max(word.length, 2);
}
</script>

<style scoped>
/* 打字区域容器 */
.typing-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

/* 单词槽行 */
.words-wrapper {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: flex-end;
  gap: 10px;
}

/* 单词槽 */
.word-slot {
  height: 56px;
  border-bottom: 2px solid var(--word-border-default);
  font-size: 2.2rem;
  line-height: 1;
  padding: 0 4px;
  text-align: center;
  transition:
    color 0.15s ease,
    border-color 0.15s ease;
  border-radius: 2px;
}

/* 标点直接显示，不加下划线 */
.word-punct {
  height: 56px;
  font-size: 2.2rem;
  line-height: 1;
  color: var(--text-secondary);
}

/* 激活状态：蓝色 */
.word-active {
  color: var(--word-active-color);
  border-color: var(--word-active-color);
}

/* 错误状态：红色 */
.word-error {
  color: var(--word-error-color);
  border-color: var(--word-error-color);
}

/* 错误且处于修复模式：抖动动画 */
.word-shake {
  animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

/* 默认状态 */
.word-default {
  color: var(--word-default-color);
  border-color: var(--word-border-default);
}

/* 完全隐藏的真实 input（盖满整行，透明） */
.hidden-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: default;
}

/* 答案提示 */
.answer-tip {
  padding: 10px 20px;
  background: var(--chip-bg);
  border-radius: var(--border-radius-md);
  font-size: 1.1rem;
  color: var(--chip-color);
  border: 1px solid rgba(59, 130, 246, 0.3);
}
.tip-label {
  font-weight: 600;
  margin-right: 6px;
}

/* 移动端专用提交按钮：部分输入法无回车键，用按钮提交 */
.mobile-submit-btn {
  display: none;
}

@media (max-width: 768px) {
  .mobile-submit-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    max-width: 200px;
    padding: 12px 20px;
    margin-top: 8px;
    font-size: 1rem;
    font-weight: 600;
    color: #fff;
    background: var(--primary-color, #3b82f6);
    border: none;
    border-radius: var(--border-radius-md, 8px);
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
