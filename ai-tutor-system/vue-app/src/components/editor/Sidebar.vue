<script setup>
import { ref, computed } from 'vue'
import { useEditorStore } from '@/stores/editor'

const editorStore = useEditorStore()
const activeView = ref('explorer') // explorer, search, outline, git

const views = [
  { id: 'explorer', icon: '📁', label: '文件资源管理器' },
  { id: 'search', icon: '🔍', label: '搜索' },
  { id: 'outline', icon: '📋', label: '大纲' },
  { id: 'git', icon: '🌿', label: 'Git' }
]

// 模拟文件列表
const files = ref([
  { name: 'src', type: 'directory', children: [
    { name: 'components', type: 'directory', children: [
      { name: 'editor', type: 'directory', children: [
        { name: 'EditorShell.vue', type: 'file' },
        { name: 'MonacoEditor.vue', type: 'file' },
        { name: 'MarkdownEditor.vue', type: 'file' },
        { name: 'RichTextEditor.vue', type: 'file' }
      ]},
      { name: 'sftp', type: 'directory' }
    ]},
    { name: 'stores', type: 'directory', children: [
      { name: 'editor.js', type: 'file' },
      { name: 'sftp.js', type: 'file' }
    ]},
    { name: 'App.vue', type: 'file' },
    { name: 'main.js', type: 'file' }
  ]},
  { name: 'package.json', type: 'file' },
  { name: 'vite.config.js', type: 'file' }
])

// 切换视图
function switchView(viewId) {
  activeView.value = viewId
}

// 打开文件
function openFile(file) {
  if (file.type === 'file') {
    editorStore.openFile({
      name: file.name,
      path: `/${file.name}`, // 简化路径，实际项目中需要完整路径
      content: '// 文件内容',
      type: file.name.split('.').pop()
    })
  }
}
</script>

<template>
  <div class="flex flex-col h-full bg-[var(--bg-tertiary)] border-r border-[var(--border-color)]">
    <!-- 活动栏 -->
    <div class="flex flex-col items-center p-2 space-y-4">
      <button
        v-for="view in views"
        :key="view.id"
        :class="[
          'p-2 rounded',
          activeView === view.id ? 'bg-[var(--icon-btn-bg)] text-[var(--primary-color)]' : 'text-[var(--text-secondary)]'
        ]"
        :title="view.label"
        @click="switchView(view.id)"
      >
        {{ view.icon }}
      </button>
    </div>

    <!-- 侧边面板 -->
    <div class="flex-1 overflow-auto p-2">
      <!-- 文件资源管理器 -->
      <div
        v-if="activeView === 'explorer'"
        class="space-y-2"
      >
        <h3 class="text-xs text-[var(--text-tertiary)] uppercase font-semibold mb-2">
          文件
        </h3>
        <div class="space-y-1">
          <div
            v-for="file in files"
            :key="file.name"
            class="cursor-pointer"
          >
            <div
              class="flex items-center space-x-1 p-1 hover:bg-[var(--icon-btn-bg)] rounded"
              @click="openFile(file)"
            >
              <span>{{ file.type === 'directory' ? '📁' : '📄' }}</span>
              <span class="text-sm text-[var(--text-primary)]">{{ file.name }}</span>
            </div>
            <!-- 子文件 -->
            <div
              v-if="file.children && file.children.length > 0"
              class="pl-4 space-y-1"
            >
              <div
                v-for="child in file.children"
                :key="child.name"
                class="cursor-pointer"
              >
                <div
                  class="flex items-center space-x-1 p-1 hover:bg-[var(--icon-btn-bg)] rounded"
                  @click="openFile(child)"
                >
                  <span>{{ child.type === 'directory' ? '📁' : '📄' }}</span>
                  <span class="text-sm text-[var(--text-primary)]">{{ child.name }}</span>
                </div>
                <!-- 孙子文件 -->
                <div
                  v-if="child.children && child.children.length > 0"
                  class="pl-4 space-y-1"
                >
                  <div
                    v-for="grandchild in child.children"
                    :key="grandchild.name"
                    class="cursor-pointer"
                  >
                    <div
                      class="flex items-center space-x-1 p-1 hover:bg-[var(--icon-btn-bg)] rounded"
                      @click="openFile(grandchild)"
                    >
                      <span>{{ grandchild.type === 'directory' ? '📁' : '📄' }}</span>
                      <span class="text-sm text-[var(--text-primary)]">{{ grandchild.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索 -->
      <div
        v-else-if="activeView === 'search'"
        class="space-y-2"
      >
        <h3 class="text-xs text-[var(--text-tertiary)] uppercase font-semibold mb-2">
          搜索
        </h3>
        <div class="relative">
          <input
            type="text"
            placeholder="搜索..."
            class="w-full bg-[var(--bg-secondary)] border border-[var(--border-color)] rounded px-3 py-1 text-sm text-[var(--text-primary)] focus:outline-none focus:border-[var(--primary-color)]"
          >
          <span class="absolute right-2 top-1/2 transform -translate-y-1/2 text-[var(--text-tertiary)]">🔍</span>
        </div>
      </div>

      <!-- 大纲 -->
      <div
        v-else-if="activeView === 'outline'"
        class="space-y-2"
      >
        <h3 class="text-xs text-[var(--text-tertiary)] uppercase font-semibold mb-2">
          大纲
        </h3>
        <div class="text-sm text-[var(--text-tertiary)]">
          选择一个文件查看大纲
        </div>
      </div>

      <!-- Git -->
      <div
        v-else-if="activeView === 'git'"
        class="space-y-2"
      >
        <h3 class="text-xs text-[var(--text-tertiary)] uppercase font-semibold mb-2">
          Git
        </h3>
        <div class="text-sm text-[var(--text-tertiary)]">
          暂未实现
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 侧边栏样式 */
</style>
