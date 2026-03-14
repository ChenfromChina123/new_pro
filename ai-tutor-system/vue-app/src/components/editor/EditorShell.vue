<script setup>
import { defineAsyncComponent, computed } from 'vue'
import { useEditorStore } from '@/stores/editor'
import Toolbar from './Toolbar.vue'

const editorStore = useEditorStore()

// 异步引入编辑器组件
const MonacoEditor = defineAsyncComponent(() => import('./MonacoEditor.vue'))
const MarkdownEditor = defineAsyncComponent(() => import('./MarkdownEditor.vue'))
const RichTextEditor = defineAsyncComponent(() => import('./RichTextEditor.vue'))

// 建立编辑器类型映射表
const editorMap = {
  monaco: MonacoEditor,
  markdown: MarkdownEditor,
  richtext: RichTextEditor
}

const currentEditor = computed(() => editorMap[editorStore.editorType])
</script>

<template>
  <div
    class="h-full flex flex-col bg-[var(--bg-secondary)]"
    @contextmenu="$event => $event.preventDefault()"
  >
    <Toolbar />
    <header class="flex bg-[var(--bg-tertiary)] h-9 overflow-x-auto border-b border-[var(--border-color)]">
      <div
        v-for="file in editorStore.openFiles"
        :key="file.path"
        :class="['tab px-4 flex items-center cursor-pointer border-r border-[var(--border-color)] text-sm text-[var(--text-secondary)] h-full', editorStore.activeFilePath === file.path ? 'active bg-[var(--bg-secondary)] text-[var(--text-primary)] border-t-2 border-t-[var(--primary-color)]' : '']"
        @click="editorStore.activeFilePath = file.path"
      >
        {{ file.name }}
        <i
          class="ml-2 hover:bg-[var(--icon-btn-bg)] rounded p-0.5"
          @click.stop="editorStore.closeFile(file.path)"
        >×</i>
      </div>
    </header>

    <main class="flex-1 relative bg-[var(--bg-primary)]">
      <keep-alive>
        <component
          :is="currentEditor"
          v-if="editorStore.activeFile"
          :file="editorStore.activeFile"
          class="absolute inset-0"
        />
      </keep-alive>

      <div
        v-if="!editorStore.activeFile"
        class="flex items-center justify-center h-full text-[var(--text-tertiary)] text-lg"
      >
        <div class="text-center">
          <div class="text-6xl mb-4">
            📝
          </div>
          <div>从 SFTP 管理器中选择文件进行编辑</div>
        </div>
      </div>
    </main>

    <footer class="h-7 bg-[var(--primary-color)] text-white flex items-center px-3 text-xs">
      <div class="flex items-center space-x-3">
        <span class="truncate max-w-md">{{ editorStore.activeFilePath || '无文件' }}</span>
        <span>UTF-8</span>
      </div>
      <div class="flex items-center space-x-3 ml-auto">
        <span>Line 1, Col 1</span>
        <span>{{ editorStore.isDirty.has(editorStore.activeFilePath) ? '● 未保存' : '✓ 已保存' }}</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.tab:hover {
  background-color: var(--icon-btn-bg);
}
</style>
