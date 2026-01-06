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
  height: 100%;
  overflow-y: auto;
  background-color: var(--bg-primary);
  padding: 20px;
  transition: background-color 0.3s ease;
}

@media (max-width: 768px) {
  .settings-view {
    padding: 16px;
  }
}
</style>