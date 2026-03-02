<template>
  <!-- 遮罩层 -->
  <Transition name="fade">
    <div v-if="show" class="modal-overlay" @click.self="emit('close')">
      <div class="modal-box">
        <!-- 头部 -->
        <div class="modal-header">
          <div class="completion-icon">🎉</div>
          <h2 class="modal-title">恭喜完成！</h2>
          <p class="modal-subtitle">
            {{ courseStore.currentCourse?.title }} · 共 {{ courseStore.totalCount }} 题
          </p>
        </div>

        <!-- 统计信息 -->
        <div class="stats">
          <div class="stat-item">
            <span class="stat-value">{{ courseStore.totalCount }}</span>
            <span class="stat-label">完成题目</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">100%</span>
            <span class="stat-label">完成率</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="modal-actions">
          <button class="btn btn-outline" @click="handleDoAgain">再练一遍</button>
          <button class="btn btn-primary" @click="emit('back-to-list')">
            返回课程列表
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { useCourseStore } from "@/stores/courseStore";

const courseStore = useCourseStore();

defineProps<{
  show: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "again"): void;
  (e: "back-to-list"): void;
}>();

/** 再练一遍：重置课程并关闭弹窗 */
function handleDoAgain() {
  courseStore.resetCourse();
  emit("again");
}
</script>

<style scoped>
/* 遮罩 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
  backdrop-filter: blur(4px);
}

/* 弹窗主体 */
.modal-box {
  background: var(--bg-secondary);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 40px 32px;
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  border: 1px solid var(--card-border);
}

/* 头部 */
.modal-header {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.completion-icon {
  font-size: 3rem;
  margin-bottom: 4px;
}

.modal-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
}

.modal-subtitle {
  font-size: 0.95rem;
  color: var(--text-secondary);
}

/* 统计 */
.stats {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 32px;
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-md);
  width: 100%;
  justify-content: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--primary-color);
}

.stat-label {
  font-size: 0.8rem;
  color: var(--text-tertiary);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--border-color);
}

/* 操作按钮 */
.modal-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.modal-actions .btn {
  flex: 1;
  justify-content: center;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
