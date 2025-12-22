<template>
  <div class="file-explorer">
    <div class="explorer-header">
      <span>当前目录: {{ currentPath || '/' }}</span>
      <button @click="refresh" class="refresh-btn">🔄</button>
    </div>
    <div class="file-list">
      <div 
        v-if="currentPath && currentPath !== '/'" 
        class="file-item directory"
        @dblclick="goUp"
      >
        📁 ..
      </div>
      <div 
        v-for="file in files" 
        :key="file.name"
        class="file-item"
        :class="{ directory: file.isDirectory }"
        @dblclick="handleDblClick(file)"
      >
        <span class="icon">{{ file.isDirectory ? '📁' : '📄' }}</span>
        <span class="name">{{ file.name }}</span>
        <span class="size" v-if="!file.isDirectory">{{ formatSize(file.size) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { API_CONFIG } from '@/config/api'

const props = defineProps({
  initialPath: { type: String, default: '/' }
})

const emit = defineEmits(['navigate', 'select'])

const authStore = useAuthStore()
const files = ref([])
const currentPath = ref(props.initialPath)

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const fetchFiles = async (path) => {
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/files?path=${encodeURIComponent(path)}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const data = await res.json()
    if (data.code === 200) {
      files.value = data.data.sort((a, b) => {
        if (a.isDirectory === b.isDirectory) return a.name.localeCompare(b.name)
        return a.isDirectory ? -1 : 1
      })
      currentPath.value = path
    }
  } catch (e) {
    console.error(e)
  }
}

const refresh = () => fetchFiles(currentPath.value)

const handleDblClick = (file) => {
  if (file.isDirectory) {
    // Navigate relative
    const newPath = currentPath.value === '/' 
      ? '/' + file.name 
      : currentPath.value + '/' + file.name
    fetchFiles(newPath)
    emit('navigate', newPath)
  } else {
    emit('select', file)
  }
}

const goUp = () => {
  if (currentPath.value === '/') return
  const parts = currentPath.value.split('/')
  parts.pop()
  const newPath = parts.join('/') || '/'
  fetchFiles(newPath)
  emit('navigate', newPath)
}

// Expose method to navigate from outside (e.g. terminal cd command)
const navigateTo = (path) => {
  fetchFiles(path)
}

defineExpose({ navigateTo, refresh })

onMounted(() => {
  fetchFiles(currentPath.value)
})
</script>

<style scoped>
.file-explorer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1e293b;
  color: #e2e8f0;
  font-family: 'Consolas', monospace;
  font-size: 0.9rem;
}

.explorer-header {
  padding: 8px;
  background: #334155;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.refresh-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #fff;
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  cursor: pointer;
  border-radius: 4px;
}

.file-item:hover {
  background: #475569;
}

.file-item.directory {
  color: #fbbf24;
}

.icon { margin-right: 8px; }
.name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.size { color: #94a3b8; font-size: 0.8rem; }
</style>
