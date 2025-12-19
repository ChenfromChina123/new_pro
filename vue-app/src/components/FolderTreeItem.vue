<template>
  <div
    class="folder-item"
    :class="{ active: isActive }"
  >
    <div
      class="folder-header"
      :style="{ paddingLeft: `${(depth + 1) * indentPx}px` }"
      @click.stop.prevent="handleSelect"
    >
      <span
        v-if="hasChildren"
        class="folder-toggle"
        @click.stop="handleToggleExpand"
      >
        {{ isExpanded ? '▼' : '▶' }}
      </span>
      <span
        v-else
        class="folder-toggle empty"
      />
      <span class="folder-icon">{{ isRoot ? '📂' : '📁' }}</span>
      <span class="folder-name">{{ displayName }}</span>
      <button
        v-if="canDelete"
        class="folder-delete-btn"
        title="删除文件夹"
        @click.stop="deleteFolderAction(folder.id)"
      >
        🗑️
      </button>
    </div>

    <Transition
      name="folder-expand"
      @before-enter="beforeEnter"
      @enter="enter"
      @after-enter="afterEnter"
      @before-leave="beforeLeave"
      @leave="leave"
      @after-leave="afterLeave"
    >
      <div
        v-if="hasChildren && isExpanded && depth < maxDepthLimit"
        class="folder-children"
      >
        <FolderTreeItem
          v-for="childFolder in folder.children"
          :key="childFolder.id"
          :folder="childFolder"
          :select-folder="selectFolder"
          :toggle-folder-expand="toggleFolderExpand"
          :is-folder-expanded="isFolderExpanded"
          :delete-folder-action="deleteFolderAction"
          :depth="depth + 1"
          :indent="indent"
        />
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'FolderTreeItem'
})

const props = defineProps({
  folder: {
    type: Object,
    required: true
  },
  selectFolder: {
    type: Function,
    required: true
  },
  toggleFolderExpand: {
    type: Function,
    required: true
  },
  isFolderExpanded: {
    type: Function,
    required: true
  },
  deleteFolderAction: {
    type: Function,
    required: true
  },
  depth: {
    type: Number,
    default: 0
  },
  indent: {
    type: Number,
    default: 14
  }
})

const maxDepthLimit = 50

const indentPx = computed(() => {
  return Number.isFinite(props.indent) ? props.indent : 14
})

const hasChildren = computed(() => {
  return Array.isArray(props.folder?.children) && props.folder.children.length > 0
})

const isRoot = computed(() => {
  return props.depth === 0 || (props.folder?.folderPath || '') === ''
})

const displayName = computed(() => {
  const name = props.folder?.folderName || ''
  if (name) return name
  return isRoot.value ? '根目录' : '未命名文件夹'
})

const isExpanded = computed(() => {
  return props.isFolderExpanded(props.folder)
})

const isActive = computed(() => {
  return props.folder?.isActive === true
})

const canDelete = computed(() => {
  return !isRoot.value && props.folder?.id !== 'virtual-root'
})

/**
 * 选择文件夹并触发外部刷新。
 */
const handleSelect = (event) => {
  props.selectFolder(props.folder.folderPath, props.folder.id, event)
}

/**
 * 切换展开状态并阻止冒泡触发选择。
 */
const handleToggleExpand = (event) => {
  props.toggleFolderExpand(props.folder.id, event)
}

/**
 * 获取当前设备下的动画时长（尊重系统减少动画设置）。
 */
const getMotionDurationMs = () => {
  const reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  return reduce ? 0 : 220
}

/**
 * 展开前初始化折叠区域样式。
 */
const beforeEnter = (el) => {
  el.style.height = '0'
  el.style.opacity = '0'
  el.style.overflow = 'hidden'
}

/**
 * 执行展开动画（高度从 0 过渡到 scrollHeight）。
 */
const enter = (el) => {
  const duration = getMotionDurationMs()
  el.style.transition = `height ${duration}ms ease, opacity ${duration}ms ease`
  requestAnimationFrame(() => {
    el.style.height = el.scrollHeight + 'px'
    el.style.opacity = '1'
  })
}

/**
 * 展开完成后清理内联样式，避免后续内容变化卡顿。
 */
const afterEnter = (el) => {
  el.style.height = 'auto'
  el.style.opacity = ''
  el.style.overflow = ''
  el.style.transition = ''
}

/**
 * 折叠前固定高度，保证可动画过渡。
 */
const beforeLeave = (el) => {
  el.style.height = el.scrollHeight + 'px'
  el.style.opacity = '1'
  el.style.overflow = 'hidden'
}

/**
 * 执行折叠动画（高度从 scrollHeight 过渡到 0）。
 */
const leave = (el) => {
  const duration = getMotionDurationMs()
  el.style.transition = `height ${duration}ms ease, opacity ${duration}ms ease`
  requestAnimationFrame(() => {
    el.style.height = '0'
    el.style.opacity = '0'
  })
}

/**
 * 折叠完成后清理内联样式。
 */
const afterLeave = (el) => {
  el.style.height = ''
  el.style.opacity = ''
  el.style.overflow = ''
  el.style.transition = ''
}
</script>

<style scoped>
.folder-item {
  margin-bottom: 2px;
  user-select: none;
  color: var(--text-primary);
}

.folder-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-left: 4px solid transparent;
  transition: all 0.2s ease;
}

.folder-item.active .folder-header {
  background-color: var(--chip-bg);
  color: var(--primary-color);
  border-left-color: var(--primary-color);
  font-weight: 500;
}

.folder-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  cursor: pointer;
  font-size: 10px;
  font-weight: bold;
  color: inherit;
  transition: all 0.2s ease;
}

.folder-toggle.empty {
  visibility: hidden;
}

.folder-icon {
  font-size: 18px;
}

.folder-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  display: block;
  color: inherit;
}

.folder-delete-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 0.9em;
  /* 始终显示删除按钮，不依赖hover */
  opacity: 1;
  transition: opacity 0.2s ease;
}

.folder-children {
  /* 移除左边框，使外观更简洁，符合图2风格 */
  margin-left: 0;
  padding-left: 0;
}

@media (prefers-reduced-motion: reduce) {
  .folder-header,
  .folder-item,
  .folder-toggle {
    transition: none;
  }
}
</style>
