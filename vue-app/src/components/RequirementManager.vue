<template>
  <div class="req-manager">
    <div class="req-sidebar">
      <div class="sidebar-header">
        <span>需求文档</span>
        <button @click="createNew" class="add-btn">+</button>
      </div>
      <div class="doc-list">
        <div 
          v-for="doc in docs" 
          :key="doc.id" 
          class="doc-item"
          :class="{ active: currentDoc?.id === doc.id }"
          @click="selectDoc(doc)"
        >
          <div class="doc-title">{{ doc.title || '未命名文档' }}</div>
          <div class="doc-date">{{ formatDate(doc.updatedAt) }}</div>
        </div>
      </div>
    </div>
    <div class="req-editor" v-if="currentDoc">
      <div class="editor-header">
        <input v-model="currentDoc.title" placeholder="文档标题" class="title-input" />
        <div class="actions">
          <button @click="saveDoc" class="btn primary">保存</button>
          <button @click="showHistory" class="btn">历史版本</button>
        </div>
      </div>
      <textarea v-model="currentDoc.content" class="content-editor" placeholder="在此输入需求文档内容..."></textarea>
    </div>
    <div v-else class="empty-state">
      请选择或新建需求文档
    </div>

    <!-- History Dialog (Simplified) -->
    <div v-if="showHistoryDialog" class="modal-overlay" @click="showHistoryDialog = false">
      <div class="modal-content" @click.stop>
        <h3>版本历史</h3>
        <div class="history-list">
          <div v-for="h in history" :key="h.id" class="history-item">
            <span>v{{ h.version }}</span>
            <span>{{ formatDate(h.createdAt) }}</span>
            <button @click="restoreVersion(h)">查看</button>
          </div>
        </div>
        <button @click="showHistoryDialog = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { API_CONFIG } from '@/config/api'

const authStore = useAuthStore()
const docs = ref([])
const currentDoc = ref(null)
const history = ref([])
const showHistoryDialog = ref(false)

const formatDate = (str) => new Date(str).toLocaleString()

const fetchDocs = async () => {
  const res = await fetch(`${API_CONFIG.baseURL}/api/requirements`, {
    headers: { 'Authorization': `Bearer ${authStore.token}` }
  })
  const data = await res.json()
  if (data.code === 200) docs.value = data.data
}

const createNew = () => {
  currentDoc.value = { title: '新需求文档', content: '' }
}

const selectDoc = (doc) => {
  currentDoc.value = { ...doc }
}

const saveDoc = async () => {
  const isNew = !currentDoc.value.id
  const url = isNew 
    ? `${API_CONFIG.baseURL}/api/requirements`
    : `${API_CONFIG.baseURL}/api/requirements/${currentDoc.value.id}`
  
  const method = isNew ? 'POST' : 'PUT'
  
  const res = await fetch(url, {
    method,
    headers: { 
      'Authorization': `Bearer ${authStore.token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(currentDoc.value)
  })
  const data = await res.json()
  if (data.code === 200) {
    currentDoc.value = data.data
    fetchDocs()
    alert('保存成功')
  }
}

const showHistory = async () => {
  if (!currentDoc.value.id) return
  const res = await fetch(`${API_CONFIG.baseURL}/api/requirements/${currentDoc.value.id}/history`, {
    headers: { 'Authorization': `Bearer ${authStore.token}` }
  })
  const data = await res.json()
  if (data.code === 200) {
    history.value = data.data
    showHistoryDialog.value = true
  }
}

const restoreVersion = (h) => {
  currentDoc.value.content = h.content
  currentDoc.value.version = h.version // Just for display, save will increment
  showHistoryDialog.value = false
}

onMounted(fetchDocs)
</script>

<style scoped>
.req-manager {
  display: flex;
  height: 100%;
  background: #fff;
}
.req-sidebar {
  width: 200px;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  padding: 10px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
}
.doc-list { overflow-y: auto; flex: 1; }
.doc-item {
  padding: 10px;
  cursor: pointer;
  border-bottom: 1px solid #f1f5f9;
}
.doc-item.active { background: #eff6ff; }
.doc-title { font-weight: bold; font-size: 0.9rem; }
.doc-date { font-size: 0.75rem; color: #64748b; }

.req-editor { flex: 1; display: flex; flex-direction: column; }
.editor-header {
  padding: 10px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  gap: 10px;
}
.title-input { flex: 1; padding: 5px; font-size: 1rem; border: 1px solid #e2e8f0; }
.content-editor {
  flex: 1;
  padding: 20px;
  resize: none;
  border: none;
  font-family: monospace;
  font-size: 1rem;
  outline: none;
}
.btn {
  padding: 5px 10px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
  border-radius: 4px;
}
.btn.primary { background: #3b82f6; color: white; border: none; }
.empty-state { flex: 1; display: flex; align-items: center; justify-content: center; color: #94a3b8; }

.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 100;
}
.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}
.history-list { overflow-y: auto; flex: 1; margin: 10px 0; }
.history-item {
  display: flex; justify-content: space-between; padding: 8px; border-bottom: 1px solid #f1f5f9;
}
</style>
