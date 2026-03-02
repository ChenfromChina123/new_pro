<template>
  <div class="game-page">
    <!-- 顶部导航栏 -->
    <header class="game-header">
      <div class="header-inner">
        <!-- 返回按钮 -->
        <button class="btn btn-ghost back-btn" @click="goBack">
          <span class="back-icon">←</span>
          课程列表
        </button>

        <!-- 课程标题 + 进度 -->
        <div class="course-info">
          <span class="course-title">{{ courseStore.currentCourse?.title }}</span>
          <span class="progress-text">
            {{ courseStore.visibleIndex }} / {{ courseStore.totalCount }}
          </span>
        </div>

        <!-- 提示答案按钮 -->
        <button
          class="btn btn-outline hint-btn"
          @click="typingInputRef?.toggleAnswerTip()"
          v-if="isQuestion()"
        >
          提示
        </button>
        <div v-else class="hint-placeholder"></div>
      </div>

      <!-- 进度条 -->
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent }"></div>
      </div>
    </header>

    <!-- 游戏主体 -->
    <main class="game-main">
      <div class="game-content">
        <!-- 准备开始提示（首次进入）：支持键盘与点击/触摸，兼容移动端 -->
        <Transition name="fade">
          <div
            v-if="!gameStarted"
            class="ready-screen"
            @click="startGame"
            @touchstart.passive="startGame"
          >
            <p class="ready-hint">准备好了吗？</p>
            <p class="ready-sub">点击或按任意键开始</p>
          </div>
        </Transition>

        <!-- 游戏区域 -->
        <Transition name="fade">
          <div v-if="gameStarted" class="playing-area">
            <!-- 问题模式 -->
            <template v-if="isQuestion()">
              <!-- 中文提示 -->
              <div class="chinese-hint">
                {{ courseStore.currentStatement?.chinese }}
              </div>
              <!-- 音标 -->
              <div class="soundmark">
                {{ courseStore.currentStatement?.soundmark }}
              </div>
              <!-- 打字输入区 -->
              <TypingInput
                ref="typingInputRef"
                @correct="handleCorrect"
                @wrong="handleWrong"
              />
            </template>

            <!-- 答案模式 -->
            <template v-else>
              <AnswerView @next="handleNext" @retry="handleRetry" />
            </template>
          </div>
        </Transition>
      </div>
    </main>

    <!-- 课程完成弹窗 -->
    <CompletionModal
      :show="courseStore.showCompletion"
      @close="courseStore.hideCompletion()"
      @again="handleAgain"
      @back-to-list="goBack"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useCourseStore } from "@/stores/courseStore";
import { useGameMode } from "@/composables/useGameMode";
import { playEnglishSound } from "@/composables/useEnglishSound";
import TypingInput from "@/components/game/TypingInput.vue";
import AnswerView from "@/components/game/AnswerView.vue";
import CompletionModal from "@/components/game/CompletionModal.vue";

const router = useRouter();
const route = useRoute();
const courseStore = useCourseStore();
const { isQuestion, isAnswer, showAnswer, showQuestion, resetMode } = useGameMode();

/** 对 TypingInput 组件的引用 */
const typingInputRef = ref<InstanceType<typeof TypingInput> | null>(null);

/** 是否已开始游戏（按任意键激活） */
const gameStarted = ref(false);

/** 进度条百分比 */
const progressPercent = computed(() => {
  const total = courseStore.totalCount;
  if (!total) return "0%";
  return `${Math.round((courseStore.visibleIndex / total) * 100)}%`;
});

// ---- 生命周期 ----

onMounted(() => {
  // 如果从 URL 直接访问（如刷新页面），重新加载课程；用户课程包从 query 取 packageId
  const courseIndex = Number(route.params.courseIndex);
  const packageId = (route.query.packageId as string) || undefined;
  if (courseIndex && !courseStore.currentCourse) {
    courseStore.loadCourse(courseIndex, packageId);
  }

  // 监听任意键启动游戏（桌面端）
  window.addEventListener("keydown", startGame, { once: true });
});

onUnmounted(() => {
  window.removeEventListener("keydown", startGame);
});

/**
 * 开始游戏：支持键盘（桌面端）与点击/触摸（移动端），仅生效一次
 */
function startGame() {
  if (gameStarted.value) return;
  gameStarted.value = true;
}

// ---- 导航 ----

function goBack() {
  courseStore.hideCompletion();
  resetMode();
  const packageId = (route.query.packageId as string) || "";
  if (packageId && packageId.startsWith("up-")) {
    router.push(`/package/${packageId}`);
  } else {
    router.push("/");
  }
}

// ---- 游戏逻辑 ----

/** 答对时：播放一次读音，再切换到答案展示模式 */
function handleCorrect() {
  playEnglishSound(courseStore.currentStatement?.english);
  showAnswer();
}

/** 答错时：保持在问题模式，等用户修正 */
function handleWrong() {
  // 错误处理由 TypingInput 内部处理（Fix 模式抖动/颜色）
}

/** 答案展示页「下一题」 */
function handleNext() {
  if (courseStore.isAllDone) {
    // 最后一题 → 显示完成弹窗
    courseStore.completeCourse();
  } else {
    courseStore.toNextStatement();
    showQuestion();
  }
}

/** 答案展示页「再来一次」 */
function handleRetry() {
  showQuestion();
}

/** 完成弹窗「再练一遍」 */
function handleAgain() {
  courseStore.resetCourse();
  showQuestion();
  gameStarted.value = true;
}
</script>

<style scoped>
/* 页面整体 */
.game-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: hidden;
}

/* 顶部导航栏 */
.game-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.header-inner {
  max-width: 900px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  font-size: 0.875rem;
  gap: 4px;
  flex-shrink: 0;
}

.back-icon {
  font-size: 1rem;
}

/* 课程信息居中 */
.course-info {
  flex: 1;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.course-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.progress-text {
  font-size: 0.85rem;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 2px 10px;
  border-radius: 20px;
}

.hint-btn {
  font-size: 0.8rem;
  padding: 6px 14px;
  flex-shrink: 0;
}

/* 占位保持 header 布局平衡 */
.hint-placeholder {
  width: 64px;
  flex-shrink: 0;
}

/* 进度条 */
.progress-bar {
  height: 3px;
  background: var(--bg-tertiary);
}
.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  transition: width 0.4s ease;
}

/* 游戏主体 */
.game-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.game-content {
  width: 100%;
  max-width: 800px;
  padding: 40px 24px;
  position: relative;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 准备开始提示：可点击/触摸，兼容移动端 */
.ready-screen {
  position: absolute;
  text-align: center;
  cursor: pointer;
  padding: 24px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ready-hint {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.ready-sub {
  font-size: 1rem;
  color: var(--text-tertiary);
}

/* 游戏区域 */
.playing-area {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

/* 中文提示 */
.chinese-hint {
  font-size: 1.5rem;
  font-weight: 500;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.4;
}

/* 音标 */
.soundmark {
  font-size: 0.95rem;
  color: var(--text-tertiary);
  text-align: center;
  letter-spacing: 0.5px;
}

/* 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 600px) {
  .header-inner {
    padding: 10px 16px;
  }
  .game-content {
    padding: 24px 16px;
  }
  .chinese-hint {
    font-size: 1.2rem;
  }
}
</style>
