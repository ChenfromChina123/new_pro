<template>
  <div
    v-show="visible"
    class="nav-arrows"
    @mouseenter="$emit('mouseenter')"
    @mouseleave="$emit('mouseleave')"
    @touchstart="$emit('mouseenter')"
    @touchend="$emit('mouseleave')"
  >
    <button
      v-show="canScrollUp"
      class="nav-arrow-btn up"
      title="上一条对话"
      @click="$emit('scroll-up')"
    >
      <i class="fas fa-chevron-up" />
    </button>
    <button
      v-show="canScrollDown"
      class="nav-arrow-btn down"
      title="下一条对话"
      @click="$emit('scroll-down')"
    >
      <i class="fas fa-chevron-down" />
    </button>
  </div>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  canScrollUp: {
    type: Boolean,
    default: false
  },
  canScrollDown: {
    type: Boolean,
    default: false
  }
})

defineEmits(['scroll-up', 'scroll-down', 'mouseenter', 'mouseleave'])
</script>

<style scoped>
.nav-arrows {
  position: fixed;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 50;
}

.nav-arrow-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-arrow-btn:hover {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.nav-arrow-btn:active {
  transform: scale(0.95);
}

.nav-arrow-btn i {
  font-size: 14px;
}

@media (max-width: 768px) {
  .nav-arrows {
    right: 12px;
  }

  .nav-arrow-btn {
    width: 36px;
    height: 36px;
  }
}
</style>
