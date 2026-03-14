<script setup>
import { ref, onMounted, watch } from 'vue'
import { Editor } from '@tiptap/core'
import Document from '@tiptap/extension-document'
import Paragraph from '@tiptap/extension-paragraph'
import Text from '@tiptap/extension-text'
import Bold from '@tiptap/extension-bold'
import Italic from '@tiptap/extension-italic'
import Underline from '@tiptap/extension-underline'
import HardBreak from '@tiptap/extension-hard-break'
import { useEditorStore } from '@/stores/editor'

const props = defineProps({
  file: {
    type: Object,
    required: true
  }
})

const editorContainer = ref(null)
const editor = ref(null)
const editorStore = useEditorStore()

onMounted(() => {
  // 初始化 Tiptap 编辑器
  editor.value = new Editor({
    element: editorContainer.value,
    content: props.file.content || '',
    extensions: [
      Document,
      Paragraph,
      Text,
      Bold,
      Italic,
      Underline,
      HardBreak
    ],
    onUpdate: ({ editor }) => {
      const content = editor.getHTML()
      editorStore.updateFileContent(props.file.path, content)
    }
  })

  // 监听文件变化
  watch(() => props.file, (newFile) => {
    if (editor.value && newFile.content !== editor.value.getHTML()) {
      editor.value.commands.setContent(newFile.content || '')
    }
  }, { deep: true })
})

// 监听组件卸载
const cleanup = () => {
  if (editor.value) {
    editor.value.destroy()
  }
}

// 注册清理函数
import { onBeforeUnmount } from 'vue'
onBeforeUnmount(cleanup)
</script>

<template>
  <div class="w-full h-full bg-[var(--bg-secondary)] text-[var(--text-primary)]">
    <div
      ref="editorContainer"
      class="w-full h-full p-4"
    />
  </div>
</template>

<style scoped>
/* 自定义 Tiptap 编辑器样式 */
:deep(.tiptap) {
  width: 100%;
  height: 100%;
  outline: none;
  color: var(--text-primary);
}

:deep(.tiptap p) {
  margin-bottom: 0.75rem;
  line-height: 1.5;
  color: var(--text-primary);
}

:deep(.tiptap strong) {
  font-weight: 600;
}

:deep(.tiptap em) {
  font-style: italic;
}

:deep(.tiptap u) {
  text-decoration: underline;
}
</style>
