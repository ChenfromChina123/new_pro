<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as monaco from 'monaco-editor'
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
  // 初始化 Monaco Editor
  editor.value = monaco.editor.create(editorContainer.value, {
    value: props.file.content || '',
    language: getLanguageFromExtension(props.file.path),
    theme: 'vs-dark',
    automaticLayout: true,
    minimap: {
      enabled: true
    },
    scrollBeyondLastLine: false,
    fontSize: 14,
    lineNumbers: 'on',
    renderLineHighlight: 'all',
    tabSize: 2
  })

  // 监听内容变化
  editor.value.onDidChangeModelContent(() => {
    const content = editor.value.getValue()
    editorStore.updateFileContent(props.file.path, content)
  })

  // 监听文件变化
  watch(() => props.file, (newFile) => {
    if (editor.value && newFile.content !== editor.value.getValue()) {
      editor.value.setValue(newFile.content || '')
    }
  }, { deep: true })
})

onUnmounted(() => {
  // 销毁编辑器实例，避免内存泄漏
  if (editor.value) {
    editor.value.dispose()
  }
})

// 根据文件扩展名获取语言
function getLanguageFromExtension(filePath) {
  const ext = filePath.split('.').pop().toLowerCase()
  const languageMap = {
    'js': 'javascript',
    'ts': 'typescript',
    'vue': 'vue',
    'py': 'python',
    'json': 'json',
    'css': 'css',
    'html': 'html',
    'sql': 'sql'
  }
  return languageMap[ext] || 'plaintext'
}
</script>

<template>
  <div
    ref="editorContainer"
    class="w-full h-full"
  />
</template>

<style scoped>
/* Monaco Editor 会自动填充整个容器 */
</style>
