<template>
  <div class="requirement-view">
    <RequirementManager />
  </div>
</template>

<script setup>
/**
 * 需求分析视图组件
 * 集成了 RequirementManager 组件，提供完整的智能需求分析页面
 */
import RequirementManager from '@/components/RequirementManager.vue'
</script>

<style scoped>
.requirement-view {
  height: 100%;
  background-color: var(--bg-primary);
}
</style>
