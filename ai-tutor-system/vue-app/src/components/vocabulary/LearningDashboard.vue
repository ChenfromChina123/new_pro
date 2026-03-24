<template>
  <div class="learning-dashboard">
    <div class="dashboard-grid">
      <div class="stat-card primary">
        <div class="stat-icon">📚</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats?.totalWords ?? 0 }}</div>
          <div class="stat-label">已学习单词</div>
        </div>
      </div>
      <div class="stat-card success">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats?.masteredWords ?? 0 }}</div>
          <div class="stat-label">已掌握</div>
        </div>
      </div>
      <div class="stat-card warning">
        <div class="stat-icon">⏱️</div>
        <div class="stat-content">
          <div class="stat-value">{{ formatDuration(stats?.todayDuration ?? 0) }}</div>
          <div class="stat-label">今日时长</div>
        </div>
      </div>
      <div class="stat-card info">
        <div class="stat-icon">📈</div>
        <div class="stat-content">
          <div class="stat-value">{{ formatDuration(stats?.totalDuration ?? 0) }}</div>
          <div class="stat-label">总时长</div>
        </div>
      </div>
    </div>

    <div class="review-section card mt-4">
      <div class="card-header">
        <h3>今日复习</h3>
        <button class="btn btn-text" @click="$emit('refresh-review')">
          刷新
        </button>
      </div>
      <div v-if="reviewItems.length === 0" class="empty-state">
        <div class="illustration">🎉</div>
        <p>太棒了！今日复习任务已完成</p>
      </div>
      <div v-else class="review-grid">
        <div
          v-for="item in reviewItems"
          :key="item.id"
          class="review-card-item"
        >
          <div class="review-content">
            <h4 class="review-word">{{ item.word?.word || item.wordId }}</h4>
            <p class="review-def">{{ item.word?.definition }}</p>
          </div>
          <div class="review-actions">
            <button
              class="btn btn-sm btn-outline"
              @click="$emit('quick-review', getReviewWordId(item), Math.min((item.masteryLevel ?? 0) + 1, 5))"
            >
              认识
            </button>
            <button
              class="btn btn-sm btn-primary"
              @click="$emit('quick-review', getReviewWordId(item), 5)"
            >
              掌握
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  stats: {
    type: Object,
    default: () => ({})
  },
  reviewItems: {
    type: Array,
    default: () => []
  }
})

defineEmits(['refresh-review', 'quick-review'])

const formatDuration = (minutes) => {
  if (!minutes || minutes <= 0) return '0分钟'
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hours > 0) {
    return mins > 0 ? `${hours}小时${mins}分钟` : `${hours}小时`
  }
  return `${mins}分钟`
}

const getReviewWordId = (item) => {
  return item.word?.id || item.wordId
}
</script>

<style scoped>
.learning-dashboard {
  width: 100%;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: var(--bg-primary);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--border-color);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.stat-card.primary { border-left: 4px solid #3b82f6; }
.stat-card.success { border-left: 4px solid #10b981; }
.stat-card.warning { border-left: 4px solid #f59e0b; }
.stat-card.info { border-left: 4px solid #8b5cf6; }

.stat-icon {
  font-size: 32px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.card {
  background: var(--bg-primary);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.illustration {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  color: var(--text-secondary);
  margin: 0;
}

.review-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 20px;
}

.review-card-item {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-word {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.review-def {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
}

.review-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-text {
  background: transparent;
  color: var(--primary-color);
}

.btn-text:hover {
  background: var(--bg-secondary);
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn-outline:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background: var(--primary-color-dark);
}
</style>
