<template>
  <Transition name="fade">
    <div v-if="show" class="modal-overlay" @click.self="emit('close')">
      <div class="modal-box">
        <div class="modal-header">
          <div class="completion-icon">🎉</div>
          <h2 class="modal-title">恭喜完成！</h2>
          <p class="modal-subtitle">
            {{ courseTitle }} · 共 {{ totalCount }} 题
          </p>
        </div>

        <div class="stats">
          <div class="stat-item">
            <span class="stat-value">{{ totalCount }}</span>
            <span class="stat-label">完成题目</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <span class="stat-value">100%</span>
            <span class="stat-label">完成率</span>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-outline" @click="emit('again')">再练一遍</button>
          <button class="btn btn-primary" @click="emit('back-to-list')">返回课程列表</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },
  courseTitle: {
    type: String,
    default: ''
  },
  totalCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['close', 'again', 'back-to-list'])
</script>

<style scoped>
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

.modal-box {
  background: var(--bg-secondary);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  padding: 40px 32px;
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  border: 1px solid var(--card-border, var(--border-color));
}

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

.stats {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 32px;
  background: var(--bg-tertiary);
  border-radius: 10px;
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

.modal-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.modal-actions .btn {
  flex: 1;
  justify-content: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
