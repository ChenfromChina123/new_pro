<script setup>
import Sidebar from '@/components/editor/Sidebar.vue'
import EditorShell from '@/components/editor/EditorShell.vue'
</script>

<template>
  <div class="h-screen flex bg-[#1e1e1e] overflow-hidden">
    <!-- 主编辑区域 -->
    <EditorShell class="flex-1" />
  </div>
</template>

<style scoped>
/* 编辑器视图样式 */
</style>
