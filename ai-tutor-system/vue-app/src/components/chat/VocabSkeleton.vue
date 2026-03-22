<template>
  <div class="vocab-skeleton">
    <div class="skeleton-header">
      <div class="skeleton-icon pulse"></div>
      <div class="skeleton-title pulse"></div>
    </div>
    <div class="skeleton-content">
      <div class="skeleton-tags">
        <div class="skeleton-tag pulse"></div>
        <div class="skeleton-tag pulse"></div>
        <div class="skeleton-tag pulse"></div>
        <div class="skeleton-tag pulse"></div>
        <div class="skeleton-tag pulse"></div>
      </div>
      <div class="skeleton-btn pulse"></div>
    </div>
    <div class="loading-text">
      <i class="fas fa-spinner fa-spin"></i>
      <span>正在生成词汇卡片...</span>
    </div>
  </div>
</template>

<style scoped>
.vocab-skeleton {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  background-color: var(--bg-secondary);
  margin: 12px 0;
  max-width: 480px;
  animation: skeleton-fade-in 0.25s ease-out;
}

@keyframes skeleton-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.pulse {
  animation: pulse 1.5s infinite ease-in-out;
  background-color: var(--border-color);
  border-radius: 4px;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 0.3;
    transform: scale(0.98);
  }
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.skeleton-title {
  height: 24px;
  width: 120px;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 0;
}

.skeleton-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skeleton-tag {
  height: 28px;
  width: 80px;
  border-radius: 6px;
}

.skeleton-btn {
  height: 44px;
  width: 100%;
  border-radius: 8px;
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.loading-text i {
  color: var(--primary-color);
}
</style>
