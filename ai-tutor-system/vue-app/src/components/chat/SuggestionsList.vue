<template>
  <div class="suggestions-area">
    <div class="suggestions-list">
      <button
        v-for="(suggestion, index) in suggestions"
        :key="index"
        class="suggestion-item"
        @click="$emit('select', suggestion)"
      >
        <span class="suggestion-text">{{ suggestion }}</span>
        <i class="fas fa-arrow-right suggestion-arrow" />
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  suggestions: {
    type: Array,
    default: () => []
  }
})

defineEmits(['select'])
</script>

<style scoped>
.suggestions-area {
  margin-top: 24px;
  margin-bottom: 8px;
  display: flex;
  justify-content: flex-start;
  animation: fadeIn 0.3s ease-out;
  width: 100%;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 600px;
}

.suggestion-item {
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  width: 100%;
}

.suggestion-item:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.suggestion-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
  flex: 1;
}

.suggestion-arrow {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-left: 12px;
  transition: transform 0.2s ease, color 0.2s ease;
}

.suggestion-item:hover .suggestion-arrow {
  transform: translateX(4px);
  color: var(--primary-color);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
