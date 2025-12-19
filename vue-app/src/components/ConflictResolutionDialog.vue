<template>
  <div v-if="visible" class="modal-overlay">
    <div class="modal-content">
      <h3>文件冲突</h3>
      <div class="conflict-info">
        <p>检测到同名文件：</p>
        <ul class="file-list">
          <li v-for="file in files" :key="file.name">
            <span class="file-icon">📄</span>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-details">({{ formatSize(file.size) }})</span>
          </li>
        </ul>
        <p class="warning" v-if="isFolder">注意：文件夹覆盖将合并内容。</p>
      </div>
      
      <div class="actions">
        <div class="option" @click="selectStrategy('RENAME')" :class="{ active: strategy === 'RENAME' }">
          <div class="radio"></div>
          <div class="text">
            <strong>智能重命名</strong>
            <small>保留两个文件，新文件将自动重命名 (例如: file(1).txt)</small>
          </div>
        </div>
        
        <div class="option" @click="selectStrategy('OVERWRITE')" :class="{ active: strategy === 'OVERWRITE' }">
          <div class="radio"></div>
          <div class="text">
            <strong>覆盖</strong>
            <small>替换现有文件 (不可撤销)</small>
          </div>
        </div>
      </div>

      <div class="footer">
        <label class="checkbox-label" v-if="files.length > 1 || batchMode">
          <input type="checkbox" v-model="applyToAll">
          应用到所有冲突文件
        </label>
        <div class="buttons">
          <button class="btn btn-secondary" @click="$emit('cancel')">取消</button>
          <button class="btn btn-primary" @click="confirm">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  visible: Boolean,
  files: {
    type: Array,
    default: () => []
  },
  batchMode: Boolean,
  isFolder: Boolean
})

const emit = defineEmits(['resolve', 'cancel'])

const strategy = ref('RENAME')
const applyToAll = ref(false)

const selectStrategy = (s) => {
  strategy.value = s
}

const confirm = () => {
  emit('resolve', { strategy: strategy.value, applyToAll: applyToAll.value })
}

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 1.25rem;
}

.file-list {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 12px;
  margin: 12px 0;
  list-style: none;
  max-height: 150px;
  overflow-y: auto;
}

.file-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.file-details {
  color: #6c757d;
  font-size: 0.9em;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.option:hover {
  border-color: #ced4da;
}

.option.active {
  border-color: #0d6efd;
  background-color: #f8faff;
}

.radio {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #adb5bd;
  margin-top: 2px;
  position: relative;
}

.option.active .radio {
  border-color: #0d6efd;
}

.option.active .radio::after {
  content: '';
  position: absolute;
  top: 4px;
  left: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0d6efd;
}

.text strong {
  display: block;
  margin-bottom: 4px;
}

.text small {
  color: #6c757d;
}

.footer {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.buttons {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-weight: 500;
}

.btn-primary {
  background: #0d6efd;
  color: white;
}

.btn-secondary {
  background: #e9ecef;
  color: #212529;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
</style>
