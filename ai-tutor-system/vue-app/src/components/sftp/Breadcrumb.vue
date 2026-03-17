<template>
  <div class="breadcrumb">
    <span
      v-for="(part, index) in pathParts"
      :key="index"
      class="breadcrumb-item"
      :class="{ clickable: index < pathParts.length - 1 }"
      @click="navigateTo(index)"
    >
      <span
        v-if="index > 0"
        class="separator"
      >/</span>
      <span class="name">{{ part || '/' }}</span>
    </span>
  </div>
</template>

<script>
export default {
  name: 'SftpBreadcrumb'
}
</script>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  path: {
    type: String,
    default: '/'
  }
})

const emit = defineEmits(['navigate'])

const pathParts = computed(() => {
  if (props.path === '/') return ['']
  return props.path.split('/').filter(p => p !== '')
})

/**
 * 导航到指定层级
 * @param {number} index - 层级索引
 */
function navigateTo(index) {
  if (index >= pathParts.value.length - 1) return
  
  if (index === 0 && pathParts.value[0] === '') {
    emit('navigate', '/')
  } else {
    const newPath = '/' + pathParts.value.slice(0, index + 1).join('/')
    emit('navigate', newPath)
  }
}
</script>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
}

.breadcrumb-item.clickable {
  cursor: pointer;
  color: var(--accent);
}

.breadcrumb-item.clickable:hover {
  text-decoration: underline;
}

.separator {
  margin: 0 4px;
  color: var(--text-dim);
}

.name {
  color: var(--text-main);
}

.breadcrumb-item.clickable .name {
  color: var(--accent);
}
</style>
