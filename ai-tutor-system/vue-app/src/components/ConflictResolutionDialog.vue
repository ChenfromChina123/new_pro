<template>
  <div
    v-if="visible"
    class="modal-overlay"
  >
    <div class="modal-content">
      <h3>{{ isFolder ? '文件夹冲突' : '文件冲突' }}</h3>
      <div class="conflict-info">
        <p>{{ isFolder ? '检测到同名文件夹：' : '检测到同名文件：' }}</p>
        <ul class="file-list">
          <li
            v-for="file in files"
            :key="file.name"
          >
            <span class="file-icon">{{ (file.isFolder || isFolder) ? '📁' : '📄' }}</span>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-details">({{ formatSize(file.size) }})</span>
          </li>
        </ul>
        <p
          v-if="isFolder"
          class="warning"
        >
          注意：文件夹覆盖将合并内容。
        </p>
      </div>
      
      <div class="actions">
        <div
          class="option"
          :class="{ active: strategy === 'RENAME' }"
          @click="selectStrategy('RENAME')"
        >
          <div class="radio" />
          <div class="text">
            <strong>智能重命名</strong>
            <small>保留两个{{ isFolder ? '文件夹' : '文件' }}，新{{ isFolder ? '文件夹' : '文件' }}将自动重命名 (例如: {{ isFolder ? 'folder(1)' : 'file(1).txt' }})</small>
          </div>
        </div>
        
        <div
          class="option"
          :class="{ active: strategy === 'OVERWRITE' }"
          @click="selectStrategy('OVERWRITE')"
        >
          <div class="radio" />
          <div class="text">
            <strong>覆盖</strong>
            <small>替换现有{{ isFolder ? '文件夹' : '文件' }} (不可撤销)</small>
          </div>
        </div>
      </div>

      <div class="footer">
        <label
          v-if="files.length > 1 || batchMode"
          class="checkbox-label"
        >
          <input
            v-model="applyToAll"
            type="checkbox"
          >
          应用到所有冲突文件
        </label>
        <div class="buttons">
          <button
            class="btn btn-secondary"
            @click="$emit('cancel')"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="confirm"
          >
            确定
          </button>
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
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.modal-content {
  background: var(--bg-secondary);
  padding: 32px;
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  box-shadow: var(--shadow-lg);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
}

.conflict-info p {
  margin-bottom: 8px;
  color: var(--text-secondary);
}

.file-list {
  background: var(--bg-tertiary);
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
  list-style: none;
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid var(--border-color);
}

.file-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  color: var(--text-primary);
}

.file-icon {
  font-size: 18px;
}

.file-name {
  font-weight: 500;
}

.file-details {
  color: var(--text-tertiary);
  font-size: 13px;
}

.warning {
  color: var(--warning-color);
  font-size: 13px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.actions {
  margin-top: 20px;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border: 2px solid var(--border-color);
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option:hover {
  border-color: var(--gray-300);
  background-color: var(--bg-tertiary);
}

.option.active {
  border-color: var(--primary-color);
  background-color: rgba(29, 78, 216, 0.04);
}

.radio {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--gray-300);
  margin-top: 2px;
  position: relative;
  flex-shrink: 0;
}

.option.active .radio {
  border-color: var(--primary-color);
}

.option.active .radio::after {
  content: '';
  position: absolute;
  top: 4px;
  left: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
}

.text strong {
  display: block;
  margin-bottom: 4px;
  font-size: 15px;
}

.text small {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.4;
}

.footer {
  margin-top: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.buttons {
  display: flex;
  gap: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  font-size: 14px;
  color: var(--text-secondary);
}

.checkbox-label input {
  width: 16px;
  height: 16px;
}
</style>
