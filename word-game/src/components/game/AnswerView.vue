<template>
  <div class="answer-view">
    <!-- 英文句子展示（点击单词可播放发音） -->
    <div class="english-sentence">
      <span
        v-for="word in englishWords"
        :key="word"
        class="answer-word"
        @click="playWordSound(word)"
      >
        {{ word }}
      </span>
    </div>

    <!-- 音标 -->
    <div class="soundmark">{{ courseStore.currentStatement?.soundmark }}</div>

    <!-- 中文翻译 -->
    <div class="chinese">{{ courseStore.currentStatement?.chinese }}</div>

    <!-- 操作按钮 -->
    <div class="actions">
      <button class="btn btn-outline" @click="emit('retry')">再来一次</button>
      <button class="btn btn-primary" @click="emit('next')">
        下一题
        <span class="kbd-hint">Space / Enter</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from "vue";
import { useCourseStore } from "@/stores/courseStore";
import { usePlayWordSound } from "@/composables/useEnglishSound";

const courseStore = useCourseStore();
const { handlePlayWordSound } = usePlayWordSound();

const emit = defineEmits<{
  (e: "next"): void;
  (e: "retry"): void;
}>();

/** 英文单词数组，用于逐词展示 */
const englishWords = computed(
  () => courseStore.currentStatement?.english.split(" ") ?? [],
);

/**
 * 点击单词时使用有道 API 播放单词发音
 * @param word 被点击的英文单词
 */
function playWordSound(word: string) {
  handlePlayWordSound(word);
}

/** 注册空格/回车快捷键进入下一题 */
function handleKeydown(e: KeyboardEvent) {
  if (e.code === "Space" || e.code === "Enter") {
    e.preventDefault();
    emit("next");
  }
}

onMounted(() => {
  window.addEventListener("keydown", handleKeydown);
  // 答案页不再自动播放读音，读音已在题目出现时（打字前）播放
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped>
.answer-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

/* 英文句子 */
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

/* 音标 */
.soundmark {
  font-size: 1rem;
  color: var(--text-tertiary);
  letter-spacing: 0.5px;
}

/* 中文翻译 */
.chinese {
  font-size: 1.2rem;
  color: var(--text-secondary);
}

/* 操作按钮行 */
.actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

/* 快捷键提示标记 */
.kbd-hint {
  font-size: 0.75rem;
  opacity: 0.75;
  margin-left: 4px;
  background: rgba(255, 255, 255, 0.2);
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
