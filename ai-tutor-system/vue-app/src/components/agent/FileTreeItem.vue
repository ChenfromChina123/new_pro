<template>
  <div
    class="file-tree-item"
    :style="{ paddingLeft: depth * 16 + 'px' }"
  >
    <div
      :class="['item-content', { selected: isSelected }]"
      @click="handleClick"
    >
      <span
        class="expand-icon"
        @click.stop="toggleExpand"
      >
        <i
          v-if="file.isDirectory"
          :class="isExpanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'"
        />
      </span>
      <span class="file-icon">
        <i :class="getFileIcon(file)" />
      </span>
      <span class="file-name">{{ file.name }}</span>
      <span
        v-if="file.size"
        class="file-size"
      >{{ formatSize(file.size) }}</span>
    </div>
    <div
      v-if="file.isDirectory && isExpanded && file.children"
      class="children"
    >
      <FileTreeItem
        v-for="child in file.children"
        :key="child.path"
        :file="child"
        :depth="depth + 1"
        @select="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  file: {
    type: Object,
    required: true
  },
  depth: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['select'])

const isExpanded = ref(false)
const isSelected = ref(false)

function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

function handleClick() {
  isSelected.value = true
  emit('select', props.file)
}

function getFileIcon(file) {
  if (file.isDirectory) {
    return isExpanded.value ? 'fas fa-folder-open' : 'fas fa-folder'
  }
  
  const ext = file.name.split('.').pop()?.toLowerCase()
  const iconMap = {
    js: 'fab fa-js text-yellow-400',
    ts: 'fab fa-js text-blue-400',
    vue: 'fab fa-vuejs text-green-400',
    json: 'fas fa-code text-yellow-300',
    md: 'fab fa-markdown text-gray-400',
    py: 'fab fa-python text-blue-300',
    java: 'fab fa-java text-red-400',
    html: 'fab fa-html5 text-orange-400',
    css: 'fab fa-css3-alt text-blue-500',
    txt: 'fas fa-file-alt text-gray-400',
    default: 'fas fa-file text-gray-400'
  }
  
  return iconMap[ext] || iconMap.default
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.file-tree-item {
  user-select: none;
}

.item-content {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.item-content:hover {
  background: var(--bg-tertiary);
}

.item-content.selected {
  background: var(--primary-color-transparent);
}

.expand-icon {
  width: 12px;
  font-size: 10px;
  color: var(--text-tertiary);
}

.file-icon {
  width: 16px;
  text-align: center;
}

.file-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.875rem;
}

.file-size {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.children {
  /* Children are indented via padding-left */
}
</style>
