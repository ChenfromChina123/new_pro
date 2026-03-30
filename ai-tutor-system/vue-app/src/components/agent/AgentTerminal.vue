<template>
  <div class="agent-terminal">
    <div
      ref="terminalRef"
      class="terminal-output"
    >
      <div
        v-for="(line, index) in output"
        :key="index"
        :class="['terminal-line', line.type]"
      >
        <span
          v-if="line.timestamp"
          class="timestamp"
        >{{ formatTime(line.timestamp) }}</span>
        <span class="content">{{ line.content }}</span>
      </div>
      <div
        v-if="isStreaming"
        class="terminal-line streaming"
      >
        <span class="cursor">▋</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  output: {
    type: Array,
    default: () => []
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

const terminalRef = ref(null)

watch(() => props.output, () => {
  nextTick(() => {
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight
    }
  })
}, { deep: true })

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.agent-terminal {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.terminal-output {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.terminal-line {
  display: flex;
  gap: 8px;
  padding: 2px 0;
  color: #d4d4d4;
}

.terminal-line.error {
  color: #f48771;
}

.terminal-line.success {
  color: #4ec9b0;
}

.terminal-line.warning {
  color: #dcdcaa;
}

.terminal-line.info {
  color: #569cd6;
}

.timestamp {
  color: #6a9955;
  font-size: 11px;
  flex-shrink: 0;
}

.content {
  white-space: pre-wrap;
  word-break: break-all;
}

.streaming .cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
</style>
