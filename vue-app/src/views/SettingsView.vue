<template>
  <div class="settings-view">
    <Settings />
  </div>
</template>

<script setup>
import Settings from '@/components/Settings.vue'
</script>

<style scoped>
.settings-view {
  min-height: 100vh;
  background-color: var(--bg-color, #f5f7fa);
  padding: 20px;
  transition: background-color 0.3s ease;
}

/* 深色主题适配 */
@media (prefers-color-scheme: dark) {
  .settings-view {
    background-color: #1e1e1e;
  }
}
</style>