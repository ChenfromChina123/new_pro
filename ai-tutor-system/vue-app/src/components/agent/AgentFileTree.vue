<template>
  <div class="agent-file-tree">
    <div
      v-if="files.length === 0"
      class="empty-state"
    >
      <p>暂无文件</p>
    </div>
    <div
      v-else
      class="file-list"
    >
      <FileTreeItem
        v-for="file in files"
        :key="file.path"
        :file="file"
        :depth="0"
        @select="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

defineProps({
  files: {
    type: Array,
    default: () => []
  }
})

defineEmits(['select'])

const FileTreeItem = defineAsyncComponent(() =>
  import('./FileTreeItem.vue')
)
</script>

<style scoped>
.agent-file-tree {
  height: 100%;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100px;
  color: var(--text-tertiary);
  font-size: 0.875rem;
}

.file-list {
  padding: 4px;
}
</style>
