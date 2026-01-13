<template>
  <div class="req-manager">
    <div class="req-sidebar">
      <div class="sidebar-header">
        <span>需求文档</span>
        <button
          class="add-btn"
          @click="createNew"
        >
          +
        </button>
      </div>
      <div class="doc-list">
        <div 
          v-for="doc in docs" 
          :key="doc.id" 
          class="doc-item"
          :class="{ active: currentDoc?.id === doc.id }"
          @click="selectDoc(doc)"
        >
          <div class="doc-title">
            {{ doc.title || '未命名文档' }}
          </div>
          <div class="doc-date">
            {{ formatDate(doc.updatedAt) }}
          </div>
        </div>
      </div>
    </div>
    <div
      v-if="currentDoc"
      class="req-editor"
    >
      <div class="editor-header">
        <input
          v-model="currentDoc.title"
          placeholder="文档标题"
          class="title-input"
        >
        <div class="actions">
          <button
            class="btn primary"
            @click="saveDoc"
          >
            保存
          </button>
          <button
            class="btn"
            @click="showHistory"
          >
            历史版本
          </button>
        </div>
      </div>
      <textarea
        v-model="currentDoc.content"
        class="content-editor"
        placeholder="在此输入需求文档内容..."
      />
    </div>
    <div
      v-else
      class="empty-state"
    >
      请选择或新建需求文档
    </div>

    <!-- History Dialog (Simplified) -->
    <div
      v-if="showHistoryDialog"
      class="modal-overlay"
      @click="showHistoryDialog = false"
    >
      <div
        class="modal-content"
        @click.stop
      >
        <h3>版本历史</h3>
        <div class="history-list">
          <div
            v-for="h in history"
            :key="h.id"
            class="history-item"
          >
            <span>v{{ h.version }}</span>
            <span>{{ formatDate(h.createdAt) }}</span>
            <button @click="restoreVersion(h)">
              查看
            </button>
          </div>
        </div>
        <button @click="showHistoryDialog = false">
          关闭
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { API_CONFIG } from '@/config/api'

const authStore = useAuthStore()
const uiStore = useUIStore()
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
    uiStore.showToast('保存成功')
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
  background: var(--bg-secondary);
  color: var(--text-primary);
  --primary-color: #06b6d4;
  --primary-gradient: linear-gradient(135deg, #06b6d4 0%, #10b981 100%);
}
.req-sidebar {
  width: 260px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}
.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sidebar-header span {
  font-weight: 600;
  font-size: 1.1rem;
  background: var(--primary-gradient);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.doc-list { overflow-y: auto; flex: 1; padding: 12px 8px; }
.doc-item {
  padding: 12px 16px;
  cursor: pointer;
  border-radius: 8px;
  margin-bottom: 4px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.doc-item:hover {
  background: var(--bg-tertiary);
}
.doc-item.active { 
  background: rgba(6, 182, 212, 0.1);
  border-color: rgba(6, 182, 212, 0.2);
  border-left: 4px solid var(--primary-color);
}
body.dark-mode .doc-item.active {
  background: rgba(6, 182, 212, 0.15);
}
.doc-title {
  font-weight: 500;
  font-size: 0.95rem;
  margin-bottom: 4px;
  color: var(--text-primary);
}
.doc-date {
  font-size: 0.8rem;
  color: var(--text-tertiary);
}
.req-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
}
.editor-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
  background: var(--bg-primary);
}
.title-input {
  flex: 1;
  background: transparent;
  border: 1px solid transparent;
  padding: 8px 0;
  font-size: 1.4rem;
  font-weight: 600;
  color: var(--text-primary);
}
.title-input:focus {
  outline: none;
}
.content-editor {
  flex: 1;
  padding: 32px;
  background: transparent;
  color: var(--text-primary);
  border: none;
  resize: none;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  font-size: 1.05rem;
  line-height: 1.8;
}
.content-editor:focus {
  outline: none;
}
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  gap: 1rem;
}
.empty-state::before {
  content: '\f15c';
  font-family: 'Font Awesome 5 Free';
  font-weight: 900;
  font-size: 3rem;
  opacity: 0.2;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.modal-content {
  background: var(--bg-primary);
  padding: 32px;
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--border-color);
}
.history-list {
  margin: 20px 0;
  max-height: 400px;
  overflow-y: auto;
}
.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}
.add-btn {
  background: var(--primary-gradient);
  color: white;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px -1px rgba(6, 182, 212, 0.2);
}
.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px -2px rgba(6, 182, 212, 0.3);
}
.btn {
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all 0.2s ease;
}
.btn:hover {
  background: var(--bg-tertiary);
}
.btn.primary {
  background: var(--primary-gradient);
  color: white;
  border: none;
  box-shadow: 0 4px 6px -1px rgba(6, 182, 212, 0.2);
}
.btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px -2px rgba(6, 182, 212, 0.3);
}
</style>
