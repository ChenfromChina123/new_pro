<template>
  <div class="vocab-skeleton">
    <div class="skeleton-header">
      <div class="skeleton-icon pulse"></div>
      <div class="skeleton-title pulse"></div>
    </div>
    <div class="skeleton-content">
      <div class="skeleton-line pulse" style="width: 70%"></div>
      <div class="skeleton-line pulse" style="width: 40%"></div>
    </div>
    <div class="skeleton-footer">
      <div class="skeleton-btn pulse"></div>
      <div class="skeleton-btn pulse"></div>
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
  max-width: 400px;
}

.pulse {
  animation: pulse 1.5s infinite ease-in-out;
  background-color: var(--border-color);
  border-radius: 4px;
}

@keyframes pulse {
  0% { opacity: 0.6; }
  50% { opacity: 0.3; }
  100% { opacity: 0.6; }
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
  gap: 8px;
  margin-bottom: 20px;
}

.skeleton-line {
  height: 16px;
}

.skeleton-footer {
  display: flex;
  gap: 12px;
}

.skeleton-btn {
  height: 36px;
  width: 80px;
  border-radius: 18px;
}
</style>
