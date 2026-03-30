<template>
  <div class="tool-call-display">
    <div class="tool-header">
      <span class="tool-icon">
        <i :class="getToolIcon(toolCall.toolName)" />
      </span>
      <span class="tool-name">{{ toolCall.toolName }}</span>
    </div>
    <div
      v-if="toolCall.toolInput"
      class="tool-input"
    >
      <div class="label">输入参数:</div>
      <pre>{{ formatJson(toolCall.toolInput) }}</pre>
    </div>
    <div
      v-if="toolCall.toolOutput"
      class="tool-output"
    >
      <div class="label">输出结果:</div>
      <pre>{{ truncateOutput(toolCall.toolOutput) }}</pre>
    </div>
  </div>
</template>

<script setup>
defineProps({
  toolCall: {
    type: Object,
    required: true
  }
})

function getToolIcon(toolName) {
  const iconMap = {
    terminal_run: 'fas fa-terminal',
    read_file: 'fas fa-file-code',
    edit_file_by_anchor: 'fas fa-edit',
    search_in_files: 'fas fa-search',
    ls: 'fas fa-folder-open',
    undo_last_action: 'fas fa-undo',
    write_file: 'fas fa-file-plus'
  }
  return iconMap[toolName] || 'fas fa-wrench'
}

function formatJson(str) {
  try {
    const obj = typeof str === 'string' ? JSON.parse(str) : str
    return JSON.stringify(obj, null, 2)
  } catch {
    return str
  }
}

function truncateOutput(output) {
  if (!output) return ''
  if (output.length > 500) {
    return output.substring(0, 500) + '\n... (输出已截断)'
  }
  return output
}
</script>

<style scoped>
.tool-call-display {
  font-size: 0.875rem;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.tool-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-color-transparent);
  border-radius: 4px;
  color: var(--primary-color);
}

.tool-name {
  font-weight: 600;
  font-family: monospace;
}

.tool-input,
.tool-output {
  margin-top: 8px;
}

.label {
  font-size: 0.75rem;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

pre {
  margin: 0;
  padding: 8px;
  background: var(--bg-tertiary);
  border-radius: 4px;
  font-size: 0.8rem;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
