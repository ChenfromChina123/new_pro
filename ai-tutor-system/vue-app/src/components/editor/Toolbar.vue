<script>
export default {
  name: 'EditorToolbar'
}
</script>

<script setup>
import { ref } from 'vue'

const showToolbar = ref(false)
const toolbarPosition = ref({ x: 0, y: 0 })

// 隐藏工具栏
function hideContextMenu() {
  showToolbar.value = false
}

// 执行工具操作
function executeTool(tool) {
  console.log('执行工具:', tool)
  // 这里可以实现具体的工具逻辑
  hideContextMenu()
}
</script>

<template>
  <div class="relative">
    <!-- 全局右键菜单事件监听 -->
    <div
      v-if="showToolbar"
      class="absolute inset-0 z-10"
      @click="hideContextMenu"
    />

    <!-- 悬浮工具栏 -->
    <div
      v-if="showToolbar"
      class="absolute z-20 bg-[var(--bg-tertiary)] border border-[var(--border-color)] rounded shadow-lg p-2"
      :style="{
        left: `${toolbarPosition.x}px`,
        top: `${toolbarPosition.y}px`
      }"
    >
      <div class="flex flex-col space-y-2">
        <button
          class="px-3 py-1 text-sm text-[var(--text-secondary)] hover:bg-[var(--icon-btn-bg)] rounded flex items-center"
          @click="executeTool('search')"
        >
          <i class="mr-2">🔍</i> 搜索
        </button>
        <button
          class="px-3 py-1 text-sm text-[var(--text-secondary)] hover:bg-[var(--icon-btn-bg)] rounded flex items-center"
          @click="executeTool('refactor')"
        >
          <i class="mr-2">✏️</i> 重构
        </button>
        <button
          class="px-3 py-1 text-sm text-[var(--text-secondary)] hover:bg-[var(--icon-btn-bg)] rounded flex items-center"
          @click="executeTool('ai-explain')"
        >
          <i class="mr-2">🤖</i> AI 解释
        </button>
        <button
          class="px-3 py-1 text-sm text-[var(--text-secondary)] hover:bg-[var(--icon-btn-bg)] rounded flex items-center"
          @click="executeTool('format')"
        >
          <i class="mr-2">📝</i> 格式化
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 工具栏样式 */
</style>
