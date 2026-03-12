<template>
  <div class="file-table-container" :style="{ transform: `scale(${zoomLevel})`, transformOrigin: 'top left' }">
    <!-- 框选遮罩层 -->
    <div v-if="isSelecting" class="selection-marquee" :style="marqueeStyle"></div>

    <vxe-table
      ref="tableRef"
      border="none"
      show-header-overflow
      show-overflow
      :row-config="{ isHover: true, isCurrent: true }"
      :data="fileList"
      :menu-config="menuConfig"
      :row-class="rowClassName"
      @cell-dblclick="handleCellDBLClick"
      @menu-click="handleMenuClick"
      @cell-click="handleCellClick"
      class="xftp-table"
    >
      <vxe-column type="checkbox" width="40" fixed="left" :resizable="false" />
      
      <vxe-column field="name" title="名称" min-width="250" sortable>
        <template #default="{ row }">
          <div class="file-item">
            <FileIcon :fileName="row.name" :isDirectory="row.isDirectory" size="md" />
            <span class="file-name">{{ row.name }}</span>
            <span v-if="row.isDirectory" class="file-badge directory-badge">目录</span>
            <span v-else class="file-ext">{{ getFileExtension(row.name) }}</span>
          </div>
        </template>
      </vxe-column>
      
      <vxe-column field="permissions" title="权限" width="120" />
      <vxe-column field="modifiedTime" title="修改日期" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.modifiedTime) }}
        </template>
      </vxe-column>
      <vxe-column field="size" title="大小" width="100" align="right">
        <template #default="{ row }">
          <span v-if="row.isDirectory">—</span>
          <span v-else>{{ formatSize(row.size) }}</span>
        </template>
      </vxe-column>
    </vxe-table>

    <!-- 属性对话框 -->
    <div v-if="showProperties" class="dialog-overlay" @click.self="closeProperties">
      <div class="property-dialog">
        <div class="dialog-header">
          <h4>
            <FileIcon :fileName="selectedFile?.name || ''" :isDirectory="selectedFile?.isDirectory" size="md" />
            {{ selectedFile?.name }} - 属性
          </h4>
          <button class="close-btn" @click="closeProperties">×</button>
        </div>
        <div class="dialog-content">
          <div class="property-section">
            <h5>基本信息</h5>
            <div class="property-row">
              <span class="label">类型：</span>
              <span class="value">
                <span v-if="selectedFile?.isDirectory">📁 文件夹</span>
                <span v-else>📄 文件</span>
              </span>
            </div>
            <div class="property-row" v-if="!selectedFile?.isDirectory">
              <span class="label">大小：</span>
              <span class="value">{{ formatSize(selectedFile?.size || 0) }}</span>
            </div>
            <div class="property-row">
              <span class="label">位置：</span>
              <span class="value path-value">{{ selectedFile?.path }}</span>
            </div>
            <div class="property-row">
              <span class="label">修改时间：</span>
              <span class="value">{{ formatDateTime(selectedFile?.modifiedTime) }}</span>
            </div>
          </div>
          <div class="property-section">
            <h5>权限</h5>
            <div class="property-row">
              <span class="label">权限：</span>
              <span class="value permission-value">{{ selectedFile?.permissions }}</span>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn btn-secondary" @click="closeProperties">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 桌面级文件表格组件
 * 支持 Windows 风格的多选、右键菜单、双击导航等高级交互
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { VxeTable } from 'vxe-table'
import FileIcon from './FileIcon.vue'
import { formatFileSize, formatDateTime } from '@/utils/fileIcons'

const props = defineProps({
  fileList: {
    type: Array,
    default: () => []
  },
  currentPath: {
    type: String,
    default: '/'
  },
  selectedFiles: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:selectedFiles', 'openDirectory', 'downloadFile', 'refresh', 'showContextMenu'])

const tableRef = ref(null)
const zoomLevel = ref(1.0)
const showProperties = ref(false)
const selectedFile = ref(null)

// 右键菜单配置
const menuConfig = ref({
  visible: true,
  trigger: 'contextmenu',
  items: [
    { code: 'download', name: '下载', icon: 'vxe-icon-download' },
    { code: 'upload', name: '上传到此处', icon: 'vxe-icon-upload' },
    { code: 'refresh', name: '刷新', icon: 'vxe-icon-refresh' },
    { code: '-', }, // 分割线
    { code: 'rename', name: '重命名', icon: 'vxe-icon-edit' },
    { code: 'move', name: '移动到...', icon: 'vxe-icon-move' },
    { code: 'copy', name: '复制', icon: 'vxe-icon-copy' },
    { code: 'delete', name: '删除', icon: 'vxe-icon-delete' },
    { code: '-', },
    { code: 'chmod', name: '修改权限', icon: 'vxe-icon-settings' },
    { code: 'edit', name: '在线编辑', icon: 'vxe-icon-edit' },
    { code: '-', },
    { code: 'properties', name: '属性', icon: 'vxe-icon-info' }
  ]
})

// 框选相关
const isSelecting = ref(false)
const marqueeStart = ref({ x: 0, y: 0 })
const marqueeEnd = ref({ x: 0, y: 0 })
const containerRef = ref(null)

const marqueeStyle = computed(() => {
  const left = Math.min(marqueeStart.value.x, marqueeEnd.value.x)
  const top = Math.min(marqueeStart.value.y, marqueeEnd.value.y)
  const width = Math.abs(marqueeEnd.value.x - marqueeStart.value.x)
  const height = Math.abs(marqueeEnd.value.y - marqueeStart.value.y)
  
  return {
    left: left + 'px',
    top: top + 'px',
    width: width + 'px',
    height: height + 'px'
  }
})

// 格式化函数
const formatSize = (bytes) => formatFileSize(bytes)
const formatDateTime = (dateString) => formatDateTime(dateString)

const getFileExtension = (fileName) => {
  const lastDot = fileName.lastIndexOf('.')
  if (lastDot === -1 || lastDot === 0) return ''
  return fileName.substring(lastDot).toLowerCase()
}

// 行样式
const rowClassName = ({ row }) => {
  if (props.selectedFiles.some(f => f.path === row.path)) {
    return 'selected-row'
  }
  return ''
}

// 处理单元格点击
const handleCellClick = (e) => {
  // 处理框选逻辑
  if (e.ctrlKey || e.metaKey) {
    // Ctrl 点击：切换选中状态
    const row = e.row
    const index = props.selectedFiles.findIndex(f => f.path === row.path)
    if (index === -1) {
      emit('update:selectedFiles', [...props.selectedFiles, row])
    } else {
      const newSelected = props.selectedFiles.filter((_, i) => i !== index)
      emit('update:selectedFiles', newSelected)
    }
  } else if (e.shiftKey && props.selectedFiles.length > 0) {
    // Shift 点击：范围选中
    const lastSelected = props.selectedFiles[props.selectedFiles.length - 1]
    const currentIndex = props.fileList.findIndex(f => f.path === e.row.path)
    const lastIndex = props.fileList.findIndex(f => f.path === lastSelected.path)
    
    const start = Math.min(currentIndex, lastIndex)
    const end = Math.max(currentIndex, lastIndex)
    const range = props.fileList.slice(start, end + 1)
    
    emit('update:selectedFiles', [...props.selectedFiles, ...range])
  } else {
    // 普通点击：单选
    emit('update:selectedFiles', [e.row])
  }
}

// 处理双击事件
const handleCellDBLClick = ({ row }) => {
  if (row.isDirectory) {
    emit('openDirectory', row)
  } else {
    // 文件双击：下载
    emit('downloadFile', row)
  }
}

// 处理右键菜单
const handleMenuClick = ({ row, option }) => {
  // 如果右键点击的是未选中的行，先选中该行
  if (!props.selectedFiles.some(f => f.path === row.path)) {
    emit('update:selectedFiles', [row])
  }
  
  switch (option.code) {
    case 'download':
      emit('downloadFile', row)
      break
    case 'upload':
      // 触发上传
      break
    case 'refresh':
      emit('refresh')
      break
    case 'rename':
      emit('showContextMenu', { action: 'rename', file: row })
      break
    case 'delete':
      emit('showContextMenu', { action: 'delete', file: row })
      break
    case 'properties':
      showPropertiesDialog(row)
      break
    default:
      emit('showContextMenu', { action: option.code, file: row })
  }
}

// 显示属性对话框
const showPropertiesDialog = (file) => {
  selectedFile.value = file
  showProperties.value = true
}

const closeProperties = () => {
  showProperties.value = false
  selectedFile.value = null
}

// 缩放控制
const handleZoom = (delta) => {
  zoomLevel.value = Math.min(Math.max(0.5, zoomLevel.value + delta), 2.0)
}

// 监听 Ctrl + 滚轮缩放
const handleWheel = (e) => {
  if (e.ctrlKey) {
    e.preventDefault()
    const delta = e.deltaY > 0 ? -0.1 : 0.1
    handleZoom(delta)
  }
}

// 框选逻辑
const handleMouseDown = (e) => {
  if (e.button === 0 && !e.target.closest('.vxe-cell--checkbox')) {
    isSelecting.value = true
    marqueeStart.value = { x: e.clientX, y: e.clientY }
    marqueeEnd.value = { x: e.clientX, y: e.clientY }
  }
}

const handleMouseMove = (e) => {
  if (isSelecting.value) {
    marqueeEnd.value = { x: e.clientX, y: e.clientY }
  }
}

const handleMouseUp = (e) => {
  if (isSelecting.value) {
    isSelecting.value = false
    // 计算选中的行
    const rect = containerRef.value?.getBoundingClientRect()
    if (rect) {
      const selectLeft = Math.min(marqueeStart.value.x, marqueeEnd.value.x) - rect.left
      const selectTop = Math.min(marqueeStart.value.y, marqueeEnd.value.y) - rect.top
      const selectWidth = Math.abs(marqueeEnd.value.x - marqueeStart.value.x)
      const selectHeight = Math.abs(marqueeEnd.value.y - marqueeStart.value.y)
      
      // 这里需要根据实际行高计算选中的行
      // 简化处理：遍历所有行，检查是否在选框内
      const selectedRows = []
      props.fileList.forEach((row, index) => {
        const rowTop = index * 40 // 假设行高 40px
        if (rowTop >= selectTop && rowTop <= selectTop + selectHeight) {
          selectedRows.push(row)
        }
      })
      
      if (selectedRows.length > 0) {
        emit('update:selectedFiles', selectedRows)
      }
    }
  }
}

onMounted(() => {
  window.addEventListener('wheel', handleWheel, { passive: false })
  document.addEventListener('mousedown', handleMouseDown)
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
})

onUnmounted(() => {
  window.removeEventListener('wheel', handleWheel)
  document.removeEventListener('mousedown', handleMouseDown)
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})

// 暴露方法给父组件
defineExpose({
  handleZoom
})
</script>

<style scoped>
.file-table-container {
  width: 100%;
  height: 100%;
  overflow: auto;
  position: relative;
  background: #1e1e1e;
}

.selection-marquee {
  position: fixed;
  background: rgba(59, 130, 246, 0.2);
  border: 1px solid rgba(59, 130, 246, 0.5);
  pointer-events: none;
  z-index: 9999;
}

.xftp-table {
  background: #1e1e1e;
  color: #d4d4d4;
  font-size: 13px;
}

.xftp-table :deep(.vxe-table--header) {
  background: #252526;
  color: #cccccc;
  font-weight: 500;
}

.xftp-table :deep(.vxe-table--body) {
  background: #1e1e1e;
}

.xftp-table :deep(.vxe-body--row.row--hover) {
  background: #2a2d2e;
}

.xftp-table :deep(.selected-row) {
  background: rgba(59, 130, 246, 0.2) !important;
}

.xftp-table :deep(.vxe-cell--checkbox) {
  display: none;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 500;
}

.directory-badge {
  background: rgba(240, 198, 116, 0.2);
  color: #f0c674;
}

.file-ext {
  font-size: 10px;
  color: #808080;
  background: #2a2d2e;
  padding: 2px 5px;
  border-radius: 3px;
  font-family: monospace;
}

/* 属性对话框 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.property-dialog {
  background: #252526;
  border: 1px solid #3e3e42;
  border-radius: 8px;
  width: 450px;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #2d2d30;
  border-bottom: 1px solid #3e3e42;
}

.dialog-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #cccccc;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-btn {
  background: transparent;
  border: none;
  color: #cccccc;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-btn:hover {
  background: #3e3e42;
}

.dialog-content {
  padding: 20px;
  max-height: calc(80vh - 120px);
  overflow-y: auto;
}

.property-section {
  margin-bottom: 20px;
}

.property-section h5 {
  margin: 0 0 12px;
  font-size: 12px;
  font-weight: 600;
  color: #808080;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.property-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 13px;
}

.property-row .label {
  width: 100px;
  color: #808080;
  flex-shrink: 0;
}

.property-row .value {
  color: #d4d4d4;
  flex: 1;
  word-break: break-word;
}

.path-value {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  background: #1e1e1e;
  padding: 4px 8px;
  border-radius: 4px;
}

.permission-value {
  font-family: 'Consolas', 'Monaco', monospace;
  color: #569cd6;
}

.dialog-footer {
  padding: 16px 20px;
  background: #2d2d30;
  border-top: 1px solid #3e3e42;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-secondary {
  background: #3e3e42;
  color: #cccccc;
}

.btn-secondary:hover {
  background: #505054;
}

/* 滚动条样式 */
.file-table-container::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.file-table-container::-webkit-scrollbar-track {
  background: #1e1e1e;
}

.file-table-container::-webkit-scrollbar-thumb {
  background: #424242;
  border-radius: 5px;
}

.file-table-container::-webkit-scrollbar-thumb:hover {
  background: #505050;
}
</style>
