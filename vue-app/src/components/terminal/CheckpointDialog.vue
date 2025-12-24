<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="checkpoint-overlay" @click="$emit('close')">
        <div class="checkpoint-dialog" @click.stop>
          <div class="dialog-header">
            <div class="header-title">
              <span class="icon">⏱️</span>
              <h3>时间旅行检查点</h3>
            </div>
            <button class="close-btn" @click="$emit('close')">✕</button>
          </div>

          <div class="dialog-body">
            <!-- Create New Checkpoint -->
            <div class="create-section">
              <input 
                v-model="newCheckpointDesc" 
                placeholder="输入检查点描述..." 
                class="desc-input"
                @keyup.enter="handleCreate"
              />
              <button 
                class="btn btn-create" 
                :disabled="!newCheckpointDesc.trim() || isCreating"
                @click="handleCreate"
              >
                {{ isCreating ? '创建中...' : '新建检查点' }}
              </button>
            </div>

            <!-- Checkpoint List -->
            <div class="checkpoint-list">
              <div 
                v-for="cp in checkpoints" 
                :key="cp.id" 
                class="checkpoint-item"
                :class="cp.type.toLowerCase()"
              >
                <div class="cp-left">
                  <div class="cp-icon">{{ getIcon(cp.type) }}</div>
                  <div class="cp-info">
                    <div class="cp-desc">{{ cp.description || '无描述' }}</div>
                    <div class="cp-meta">
                      <span class="cp-time">{{ formatTime(cp.timestamp) }}</span>
                      <span class="cp-type">{{ getTypeLabel(cp.type) }}</span>
                    </div>
                  </div>
                </div>
                
                <div class="cp-actions">
                  <button 
                    class="action-btn btn-jump" 
                    title="恢复到此状态"
                    @click="handleJump(cp.id)"
                  >
                    跳转
                  </button>
                  <button 
                    class="action-btn btn-export" 
                    title="导出 JSON"
                    @click="handleExport(cp.id)"
                  >
                    ⬇
                  </button>
                  <button 
                    class="action-btn btn-delete" 
                    title="删除"
                    @click="handleDelete(cp.id)"
                  >
                    🗑
                  </button>
                </div>
              </div>
              
              <div v-if="checkpoints.length === 0" class="empty-state">
                暂无检查点
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
  visible: Boolean,
  checkpoints: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'create', 'jump', 'delete', 'export'])

const newCheckpointDesc = ref('')
const isCreating = ref(false)

const handleCreate = async () => {
  if (!newCheckpointDesc.value.trim()) return
  isCreating.value = true
  emit('create', newCheckpointDesc.value.trim())
  newCheckpointDesc.value = ''
  isCreating.value = false
}

const handleJump = (id) => emit('jump', id)
const handleDelete = (id) => emit('delete', id)
const handleExport = (id) => emit('export', id)

const getIcon = (type) => {
  const icons = {
    USER_MESSAGE: '💬',
    TOOL_EDIT: '✏️',
    MANUAL: '📌',
    AUTO: '🤖'
  }
  return icons[type] || '📍'
}

const getTypeLabel = (type) => {
  const labels = {
    USER_MESSAGE: '用户消息',
    TOOL_EDIT: '文件编辑',
    MANUAL: '手动创建',
    AUTO: '自动'
  }
  return labels[type] || type
}

const formatTime = (ts) => {
  if (!ts) return ''
  return new Date(ts).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.checkpoint-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.checkpoint-dialog {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 500px;
  height: 70vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-header {
  padding: 20px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title h3 {
  margin: 0;
  font-size: 1.2rem;
}

.close-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: #fff;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  cursor: pointer;
}

.dialog-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.create-section {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.desc-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  outline: none;
}

.desc-input:focus {
  border-color: #10b981;
}

.btn-create {
  background: #10b981;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.btn-create:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.checkpoint-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkpoint-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.2s;
}

.checkpoint-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border-color: #10b981;
}

.cp-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cp-icon {
  font-size: 1.5rem;
  width: 40px;
  height: 40px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

.cp-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cp-desc {
  font-weight: 500;
  color: #1e293b;
}

.cp-meta {
  font-size: 0.8rem;
  color: #64748b;
  display: flex;
  gap: 8px;
}

.cp-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  border: none;
  border-radius: 6px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-jump {
  width: auto;
  padding: 0 12px;
  background: #dbeafe;
  color: #1e40af;
  font-weight: 600;
  font-size: 0.85rem;
}

.btn-jump:hover {
  background: #2563eb;
  color: white;
}

.btn-export {
  background: #f1f5f9;
  color: #64748b;
}

.btn-delete {
  background: #fee2e2;
  color: #ef4444;
}

.btn-delete:hover {
  background: #ef4444;
  color: white;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>