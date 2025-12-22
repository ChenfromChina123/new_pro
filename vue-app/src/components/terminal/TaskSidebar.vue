<template>
  <div 
    class="task-sidebar" 
    :style="{ width: width + 'px' }"
  >
    <div class="sidebar-header">
      <div class="header-row">
        <h3>任务链</h3>
        <span class="task-count">{{ completedCount }}/{{ tasks.length }}</span>
      </div>
      <div class="progress-track">
        <div 
          class="progress-fill" 
          :style="{ width: progress + '%', backgroundColor: progressColor }"
        ></div>
      </div>
    </div>

    <div class="task-list">
      <TransitionGroup name="list">
        <div 
          v-for="(task, index) in tasks" 
          :key="task.id"
          class="task-item"
          :class="{ 
            'active': activeTaskId === task.id,
            'completed': task.status === 'completed',
            'in-progress': task.status === 'in_progress',
            'pending': task.status === 'pending'
          }"
          draggable="true"
          @dragstart="onDragStart($event, index)"
          @dragover="onDragOver($event)"
          @drop="onDrop($event, index)"
          @click="$emit('select-task', task.id)"
        >
          <div class="task-status-icon">
            <i v-if="task.status === 'completed'" class="icon-success">✓</i>
            <i v-else-if="task.status === 'in_progress'" class="icon-spin">⟳</i>
            <i v-else class="icon-pending">○</i>
          </div>
          <div class="task-content">
            <div class="task-title" :title="task.desc">{{ task.desc }}</div>
            <div class="task-meta">
              <span class="task-id">#{{ task.id }}</span>
              <span v-if="task.time" class="task-time">{{ formatTime(task.time) }}</span>
            </div>
          </div>
          <div class="drag-handle">⋮⋮</div>
        </div>
      </TransitionGroup>
    </div>

    <!-- Resizer Handle -->
    <div 
      class="resizer" 
      @mousedown="startResize"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  tasks: {
    type: Array,
    required: true
  },
  activeTaskId: {
    type: [Number, String],
    default: null
  },
  initialWidth: {
    type: Number,
    default: 250
  }
})

const emit = defineEmits(['update:width', 'reorder', 'select-task'])

const width = ref(props.initialWidth)
let draggedItemIndex = null

const completedCount = computed(() => props.tasks.filter(t => t.status === 'completed').length)
const progress = computed(() => {
  if (props.tasks.length === 0) return 0
  return Math.round((completedCount.value / props.tasks.length) * 100)
})

const progressColor = computed(() => {
  if (progress.value === 100) return '#4ade80'
  if (progress.value > 50) return '#60a5fa'
  return '#94a3b8'
})

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Drag and Drop Logic
const onDragStart = (e, index) => {
  draggedItemIndex = index
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.dropEffect = 'move'
  e.target.style.opacity = '0.5'
}

const onDragOver = (e) => {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

const onDrop = (e, dropIndex) => {
  e.preventDefault()
  e.target.style.opacity = '1'
  
  if (draggedItemIndex !== null && draggedItemIndex !== dropIndex) {
    const newTasks = [...props.tasks]
    const [movedItem] = newTasks.splice(draggedItemIndex, 1)
    newTasks.splice(dropIndex, 0, movedItem)
    emit('reorder', newTasks)
  }
  draggedItemIndex = null
}

// Resize Logic
const startResize = (e) => {
  const startX = e.clientX
  const startWidth = width.value
  
  const doDrag = (e) => {
    const newWidth = startWidth + (e.clientX - startX)
    if (newWidth >= 200 && newWidth <= 400) {
      width.value = newWidth
      emit('update:width', newWidth)
    }
  }
  
  const stopDrag = () => {
    document.removeEventListener('mousemove', doDrag)
    document.removeEventListener('mouseup', stopDrag)
  }
  
  document.addEventListener('mousemove', doDrag)
  document.addEventListener('mouseup', stopDrag)
}
</script>

<style scoped>
.task-sidebar {
  background: #f8fafc; /* Lighter background for better contrast */
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
  flex-shrink: 0;
  user-select: none;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #334155;
}

.task-count {
  font-size: 0.85rem;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 12px;
}

.progress-track {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  transition: width 0.3s ease, background-color 0.3s ease;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #ffffff;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
  position: relative;
}

.task-item:hover {
  border-color: #cbd5e1;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.task-item.active {
  border-color: #3b82f6;
  background: #eff6ff;
  box-shadow: 0 0 0 1px #3b82f6;
}

.task-item.completed .task-title {
  text-decoration: line-through;
  color: #94a3b8;
}

.task-status-icon {
  margin-right: 12px;
  font-size: 1.1em;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
}

.icon-success { color: #4ade80; font-style: normal; }
.icon-pending { color: #cbd5e1; font-style: normal; }
.icon-spin { 
  color: #3b82f6; 
  font-style: normal;
  animation: spin 1.5s linear infinite;
  display: inline-block;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-content {
  flex: 1;
  overflow: hidden;
}

.task-title {
  font-size: 0.9rem;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
  font-weight: 500;
}

.task-meta {
  font-size: 0.75rem;
  color: #94a3b8;
  display: flex;
  justify-content: space-between;
}

.drag-handle {
  margin-left: 8px;
  color: #cbd5e1;
  cursor: grab;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.task-item:hover .drag-handle {
  opacity: 1;
}

/* List Transitions */
.list-move,
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
.list-leave-active {
  position: absolute;
}

/* Resizer */
.resizer {
  position: absolute;
  top: 0;
  right: -2px; /* Center over border */
  width: 4px;
  height: 100%;
  cursor: col-resize;
  background: transparent;
  z-index: 10;
  transition: background 0.2s;
}

.resizer:hover {
  background: #3b82f6;
}
</style>