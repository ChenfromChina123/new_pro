<template>
  <div class="file-explorer">
    <div class="explorer-header">
      <div class="breadcrumbs">
        <span class="breadcrumb-item" @click="fetchFiles('/')">root</span>
        <template v-for="(part, index) in pathParts" :key="index">
          <span class="separator">/</span>
          <span class="breadcrumb-item" @click="navigateToPart(index)">{{ part }}</span>
        </template>
      </div>
      <div class="header-actions">
        <button @click="createNewNotebook" class="action-btn" title="新建笔记本">📓+</button>
        <button @click="refresh" class="refresh-btn" title="刷新">🔄</button>
      </div>
    </div>
    <div class="file-list">
      <div 
        v-if="currentPath && currentPath !== '/'" 
        class="file-item directory"
        @click="goUp"
      >
        <span class="icon">📁</span>
        <span class="name">.. (返回上级)</span>
      </div>
      <div 
        v-for="file in files" 
        :key="file.name"
        class="file-item"
        :class="{ 
          directory: file.isDirectory || file.is_directory || file.directory,
          selected: selectedFile === file.name
        }"
        @click="handleItemClick(file)"
        @dblclick="handleDblClick(file)"
      >
        <span class="icon">{{ (file.isDirectory || file.is_directory || file.directory) ? '📁' : '📄' }}</span>
        <span class="name">{{ file.name }}</span>
        <span class="size" v-if="!(file.isDirectory || file.is_directory || file.directory)">{{ formatSize(file.size) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { API_CONFIG } from '@/config/api'

const props = defineProps({
  initialPath: { type: String, default: '/' }
})

const emit = defineEmits(['navigate', 'select'])

const authStore = useAuthStore()
const uiStore = useUIStore()
const files = ref([])
const currentPath = ref(props.initialPath)
const selectedFile = ref(null)

const pathParts = computed(() => {
  if (!currentPath.value || currentPath.value === '/') return []
  return currentPath.value.split('/').filter(p => p)
})

const navigateToPart = (index) => {
  const parts = pathParts.value.slice(0, index + 1)
  const newPath = '/' + parts.join('/')
  fetchFiles(newPath)
  emit('navigate', newPath)
}

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
        const aIsDir = a.isDirectory || a.is_directory || a.directory
        const bIsDir = b.isDirectory || b.is_directory || b.directory
        if (aIsDir === bIsDir) return a.name.localeCompare(b.name)
        return aIsDir ? -1 : 1
      })
      currentPath.value = path
      selectedFile.value = null
    }
  } catch (e) {
    console.error(e)
  }
}

const refresh = () => fetchFiles(currentPath.value)

const createNewNotebook = async () => {
  const name = prompt('请输入笔记本名称 (无需后缀):', 'new_notebook')
  if (!name) return
  
  const fileName = name.endsWith('.nb') ? name : `${name}.nb`
  const path = currentPath.value === '/' ? `/${fileName}` : `${currentPath.value}/${fileName}`
  
  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/write-file`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        path: path,
        content: JSON.stringify([{ type: 'code', content: '', output: '', error: '' }]),
        overwrite: false
      })
    })
    const data = await res.json()
    if (data.code === 200) {
      refresh()
      uiStore.showToast('创建成功')
    } else {
      uiStore.showToast('创建失败: ' + data.message)
    }
  } catch (e) {
    uiStore.showToast('创建失败，请检查网络')
  }
}

const handleItemClick = (file) => {
  selectedFile.value = file.name
  if (file.isDirectory || file.is_directory || file.directory) {
    // Single click on directory enters it
    const newPath = currentPath.value === '/' 
      ? '/' + file.name 
      : currentPath.value + (currentPath.value.endsWith('/') ? '' : '/') + file.name
    fetchFiles(newPath)
    emit('navigate', newPath)
  } else {
    // Single click on file selects it (optionally emits)
    emit('select', file)
  }
}

const handleDblClick = (file) => {
  if (!(file.isDirectory || file.is_directory || file.directory)) {
    emit('select', file)
  }
}

const goUp = () => {
  if (currentPath.value === '/') return
  const parts = currentPath.value.split('/').filter(p => p)
  parts.pop()
  const newPath = '/' + parts.join('/')
  fetchFiles(newPath)
  emit('navigate', newPath)
}

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
  padding: 8px 12px;
  background: #334155;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #475569;
}

.breadcrumbs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 0.85rem;
}

.breadcrumb-item {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  color: #94a3b8;
  transition: all 0.2s;
}

.breadcrumb-item:hover {
  background: #475569;
  color: #fff;
}

.separator {
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.action-btn {
  background: #475569;
  border: none;
  cursor: pointer;
  color: #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #0ea5e9;
  color: #fff;
}

.refresh-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: #475569;
  color: #fff;
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 2px;
  transition: background 0.2s;
}

.file-item:hover {
  background: #334155;
}

.file-item.selected {
  background: #1e3a8a;
}

.file-item.directory {
  color: #fbbf24;
}

.icon { 
  margin-right: 10px; 
  font-size: 1.1rem;
  width: 20px;
  display: flex;
  justify-content: center;
}

.name { 
  flex: 1; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  white-space: nowrap; 
}

.size { 
  color: #94a3b8; 
  font-size: 0.75rem; 
  margin-left: 8px;
}
</style>
