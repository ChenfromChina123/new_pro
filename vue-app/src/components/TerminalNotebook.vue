<template>
  <div class="notebook-container">
    <div class="notebook-header">
      <div class="notebook-info">
        <span class="notebook-icon">📓</span>
        <span class="notebook-name">{{ fileName }}</span>
      </div>
      <div class="notebook-actions">
        <button @click="addCell('code')" class="action-btn" title="添加代码块">
          <i class="fas fa-code"></i> +代码
        </button>
        <button @click="addCell('markdown')" class="action-btn" title="添加文本块">
          <i class="fas fa-font"></i> +文本
        </button>
        <div class="separator"></div>
        <button @click="saveNotebook" class="save-btn" :disabled="isSaving">
          {{ isSaving ? '保存中...' : '保存' }}
        </button>
        <button @click="$emit('close')" class="close-btn">退出</button>
      </div>
    </div>

    <div class="notebook-body" ref="notebookBody">
      <div v-for="(cell, index) in cells" :key="cell.id" class="notebook-cell" :class="cell.type">
        <div class="cell-controls">
          <span class="cell-type-label">{{ cell.type === 'code' ? 'Code' : 'Markdown' }} [{{ index }}]</span>
          <div class="cell-actions">
            <button v-if="cell.type === 'code'" @click="runCell(index)" :disabled="cell.isRunning" class="run-btn">
              <span v-if="cell.isRunning">...</span>
              <span v-else>▶</span>
            </button>
            <button @click="moveCell(index, -1)" :disabled="index === 0" class="ctrl-btn">↑</button>
            <button @click="moveCell(index, 1)" :disabled="index === cells.length - 1" class="ctrl-btn">↓</button>
            <button @click="deleteCell(index)" class="ctrl-btn delete">×</button>
          </div>
        </div>

        <div class="cell-input">
          <textarea
            v-model="cell.content"
            class="cell-editor"
            :placeholder="cell.type === 'code' ? '输入代码...' : '输入 Markdown...'"
            @input="adjustHeight($event)"
            ref="cellEditors"
          ></textarea>
        </div>

        <div v-if="cell.type === 'code' && (cell.output || cell.error)" class="cell-output-container">
          <div v-if="cell.output" class="cell-output stdout">
            <pre>{{ cell.output }}</pre>
          </div>
          <div v-if="cell.error" class="cell-output stderr">
            <pre>{{ cell.error }}</pre>
          </div>
        </div>
        
        <div v-if="cell.type === 'markdown' && cell.content" class="cell-preview markdown-body" v-html="renderMarkdown(cell.content)">
        </div>
      </div>
      
      <div v-if="cells.length === 0" class="empty-notebook">
        <p>点击上方按钮添加第一个单元格</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import { API_CONFIG } from '@/config/api'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'

/**
 * 笔记本组件
 * 支持代码单元执行与 Markdown 渲染
 */

const props = defineProps({
  file: { type: Object, required: true },
  initialContent: { type: String, default: '[]' }
})

const emit = defineEmits(['close', 'save'])

const authStore = useAuthStore()
const uiStore = useUIStore()
const cells = ref([])
const isSaving = ref(false)
const fileName = ref(props.file.name)
const cellEditors = ref([])

onMounted(() => {
  try {
    const parsed = JSON.parse(props.initialContent)
    if (Array.isArray(parsed)) {
      cells.value = parsed.map(c => ({
        id: Math.random().toString(36).substr(2, 9),
        type: c.type || 'code',
        content: c.content || '',
        output: c.output || '',
        error: c.error || '',
        isRunning: false
      }))
    } else {
      addCell('code')
    }
  } catch (e) {
    console.error('Failed to parse notebook content', e)
    addCell('code')
  }
})

/**
 * 添加单元格
 * @param {string} type 'code' | 'markdown'
 */
const addCell = (type) => {
  cells.value.push({
    id: Math.random().toString(36).substr(2, 9),
    type,
    content: '',
    output: '',
    error: '',
    isRunning: false
  })
}

/**
 * 删除单元格
 */
const deleteCell = (index) => {
  if (confirm('确定删除这个单元格吗？')) {
    cells.value.splice(index, 1)
  }
}

/**
 * 移动单元格
 */
const moveCell = (index, direction) => {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= cells.value.length) return
  const temp = cells.value[index]
  cells.value[index] = cells.value[newIndex]
  cells.value[newIndex] = temp
}

/**
 * 执行代码单元
 */
const runCell = async (index) => {
  const cell = cells.value[index]
  if (cell.type !== 'code' || !cell.content.trim()) return

  cell.isRunning = true
  cell.output = ''
  cell.error = ''

  try {
    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/execute`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        command: cell.content,
        cwd: '/', // Default to root or use a session cwd
        session_id: null // Independent execution
      })
    })
    
    const data = await res.json()
    if (data.code === 200) {
      cell.output = data.data.stdout
      cell.error = data.data.stderr
    } else {
      cell.error = data.message || '执行失败'
    }
  } catch (e) {
    cell.error = '网络错误，请稍后再试'
  } finally {
    cell.isRunning = false
  }
}

/**
 * 保存笔记本
 */
const saveNotebook = async () => {
  isSaving.value = true
  try {
    const content = JSON.stringify(cells.value.map(c => ({
      type: c.type,
      content: c.content,
      output: c.output,
      error: c.error
    })), null, 2)

    const res = await fetch(`${API_CONFIG.baseURL}/api/terminal/write-file`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        path: props.file.path,
        content: content,
        overwrite: true
      })
    })
    
    const data = await res.json()
    if (data.code === 200) {
      uiStore.showToast('保存成功')
      emit('save')
    } else {
      uiStore.showToast('保存失败: ' + data.message)
    }
  } catch (e) {
    uiStore.showToast('保存失败，请检查网络')
  } finally {
    isSaving.value = false
  }
}

/**
 * 渲染 Markdown
 */
const renderMarkdown = (content) => {
  return marked(content)
}

/**
 * 自动调整 Textarea 高度
 */
const adjustHeight = (event) => {
  const target = event.target
  target.style.height = 'auto'
  target.style.height = target.scrollHeight + 'px'
}
</script>

<style scoped>
.notebook-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0f172a;
  color: #e2e8f0;
}

.notebook-header {
  padding: 12px 20px;
  background: #1e293b;
  border-bottom: 1px solid #334155;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}

.notebook-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notebook-icon { font-size: 1.2rem; }
.notebook-name { font-weight: 600; font-family: 'Consolas', monospace; }

.notebook-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.action-btn {
  background: #334155;
  color: #e2e8f0;
  border: 1px solid #475569;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.action-btn:hover { background: #475569; }

.separator { width: 1px; height: 20px; background: #475569; margin: 0 4px; }

.save-btn {
  background: #0ea5e9;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}

.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.close-btn {
  background: transparent;
  color: #94a3b8;
  border: 1px solid #475569;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
}

.notebook-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.notebook-cell {
  background: #1e293b;
  border-radius: 8px;
  border: 1px solid #334155;
  overflow: hidden;
  transition: border-color 0.2s;
}

.notebook-cell:focus-within {
  border-color: #38bdf8;
}

.cell-controls {
  padding: 4px 12px;
  background: #0f172a;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  color: #64748b;
}

.cell-actions { display: flex; gap: 4px; }

.ctrl-btn, .run-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.ctrl-btn:hover { background: #334155; color: #fff; }
.run-btn { color: #10b981; font-weight: bold; }
.run-btn:hover { background: #065f46; color: #fff; }
.ctrl-btn.delete:hover { background: #991b1b; color: #fff; }

.cell-input { padding: 10px; }

.cell-editor {
  width: 100%;
  background: transparent;
  border: none;
  color: #e2e8f0;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.95rem;
  resize: none;
  outline: none;
  min-height: 40px;
}

.cell-output-container {
  background: #0f172a;
  padding: 12px;
  border-top: 1px solid #334155;
  font-family: 'Fira Code', monospace;
  font-size: 0.85rem;
}

.cell-output { white-space: pre-wrap; margin-bottom: 4px; }
.cell-output.stdout { color: #e2e8f0; }
.cell-output.stderr { color: #f87171; }

.cell-preview {
  padding: 16px;
  background: #1e293b;
  border-top: 1px solid #334155;
}

.empty-notebook {
  text-align: center;
  color: #64748b;
  padding: 40px;
}
</style>
