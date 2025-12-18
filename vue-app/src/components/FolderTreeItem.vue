<template>
  <div
    class="folder-item"
    :class="{ active: folder.folderPath === cloudDiskStore.currentFolder }"
    @click.stop.prevent="selectFolder(folder.folderPath)"
  >
    <div class="folder-header">
      <span
        v-if="folder.children && folder.children.length > 0"
        class="folder-toggle"
        @click="toggleFolderExpand(folder.id, $event)"
      >
        {{ isFolderExpanded(folder) ? '▼' : '▶' }}
      </span>
      <span
        v-else
        class="folder-toggle empty"
      />
      <span class="folder-icon">📁</span>
      <span class="folder-name">{{ folder.folderName || '未命名文件夹' }}</span>
      <button
        v-if="folder.id !== 'virtual-root'"
        class="folder-rename-btn"
        title="重命名"
        @click.stop="renameFolderAction(folder)"
      >
        ✏️
      </button>
      <button
        v-if="folder.id !== 'virtual-root'"
        class="folder-delete-btn"
        title="删除文件夹"
        @click.stop="deleteFolderAction(folder.id)"
      >
        🗑️
      </button>
    </div>

    <div
      v-if="folder.children && folder.children.length > 0 && isFolderExpanded(folder) && depth < 20"
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
        :rename-folder-action="renameFolderAction"
        :depth="depth + 1"
      />
    </div>
  </div>
</template>

<script setup>
import { useCloudDiskStore } from '@/stores/cloudDisk'

defineOptions({
  name: 'FolderTreeItem'
})

const cloudDiskStore = useCloudDiskStore()

const props = defineProps({
  folder: Object,
  selectFolder: Function,
  toggleFolderExpand: Function,
  isFolderExpanded: Function,
  deleteFolderAction: Function,
  renameFolderAction: Function,
  depth: {
    type: Number,
    default: 0
  }
})
</script>

<style scoped>
.folder-item {
  cursor: pointer;
  padding-left: 20px;
  user-select: none;
}

.folder-item.active > .folder-header {
  background-color: var(--color-primary-light);
  color: var(--color-primary-dark);
  border-radius: 4px;
}

.folder-header {
  display: flex;
  align-items: center;
  padding: 5px 0;
  transition: background-color 0.2s ease;
}

.folder-header:hover {
  background-color: var(--color-background-soft);
  border-radius: 4px;
}

.folder-toggle {
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.folder-toggle.empty {
  visibility: hidden;
}

.folder-icon {
  margin-right: 5px;
}

.folder-name {
  flex-grow: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.folder-delete-btn {
  background: none;
  border: none;
  color: var(--color-text-light);
  cursor: pointer;
  margin-left: 5px;
  font-size: 0.9em;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.folder-rename-btn {
  background: none;
  border: none;
  color: var(--color-text-light);
  cursor: pointer;
  margin-left: 5px;
  font-size: 0.9em;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.folder-item:hover .folder-rename-btn {
  opacity: 1;
}

.folder-item:hover .folder-delete-btn {
  opacity: 1;
}

.folder-children {
  border-left: 1px solid var(--color-border);
  margin-left: 8px;
}
</style>
