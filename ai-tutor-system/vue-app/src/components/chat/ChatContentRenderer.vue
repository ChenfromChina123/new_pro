<template>
  <div class="chat-content-renderer">
    <template
      v-for="(block, index) in parsedBlocks"
      :key="getBlockKey(block, index)"
    >
      <!-- eslint-disable-next-line vue/no-v-html -->
      <div
        v-if="block.type === 'markdown'"
        class="markdown-block"
        v-html="block.htmlContent"
      />

      <!-- 如果解析到了 vocab 标签，我们不再直接平铺所有卡片，而是显示一个概览入口面板 -->
      <div
        v-else-if="block.type === 'vocab-collection' && block.words.length > 0"
        class="vocab-collection-panel"
      >
        <div class="panel-header">
          <i class="fas fa-bullseye text-blue-500" />
          <span>你的专属词汇练习生成完毕（共 {{ block.words.length }} 词）</span>
        </div>
        <div class="panel-body">
          <div class="word-tags">
            <span
              v-for="(w, idx) in block.words"
              :key="idx"
              class="word-tag"
            >{{ w.word }}</span>
          </div>
          <button
            class="start-practice-btn"
            @click="openVocabModal(block.words)"
          >
            <i class="fas fa-play-circle" /> 进入专注模式开始练习
          </button>
        </div>
      </div>

      <VocabSkeleton
        v-else-if="block.type === 'vocab-skeleton'"
        :key="'skeleton'"
      />
    </template>
    <span
      v-if="isStreaming"
      class="typing-cursor"
    />

    <!-- 专注模式弹窗 -->
    <Teleport to="body">
      <VocabPracticeModal
        v-if="isModalOpen"
        :words="currentPracticeWords"
        @close="isModalOpen = false"
      />
    </Teleport>
  </div>
</template>

<script setup>
import { computed, ref, shallowRef, watch } from 'vue'
import VocabPracticeModal from './VocabPracticeModal.vue'
import VocabSkeleton from './VocabSkeleton.vue'

const props = defineProps({
  content: {
    type: String,
    required: true
  },
  formatMarkdown: {
    type: Function,
    required: true
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

const isModalOpen = ref(false)
const currentPracticeWords = ref([])

const openVocabModal = (words) => {
  currentPracticeWords.value = words
  isModalOpen.value = true
}

/**
 * 生成稳定的块 key，避免不必要的重新渲染
 * @param {Object} block - 解析后的块对象
 * @param {number} index - 块索引
 * @returns {string} 稳定的 key
 */
const getBlockKey = (block, index) => {
  if (block.type === 'markdown') {
    return `md-${index}-${block.content.length}`
  }
  if (block.type === 'vocab-collection') {
    return `vocab-${index}-${block.words.length}`
  }
  if (block.type === 'vocab-skeleton') {
    return 'skeleton'
  }
  return `block-${index}`
}

/**
 * 缓存上一次解析的词汇集合，避免流式传输时频繁切换
 */
const lastVocabCollection = shallowRef([])
const lastBlockCount = ref(0)

/**
 * 解析内容块，优化流式传输时的渲染
 */
const parsedBlocks = computed(() => {
  const blocks = []
  const text = props.content || ''

  const vocabRegex = /<vocab\s+([^>]*)\/?>/g
  let lastIndex = 0
  let match

  const currentVocabCollection = []

  while ((match = vocabRegex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      let mdContent = text.substring(lastIndex, match.index)
      mdContent = mdContent.replace(/<vocab-practice>/g, '').replace(/<\/vocab-practice>/g, '')
      if (mdContent.trim()) {
        if (currentVocabCollection.length > 0) {
          blocks.push({ type: 'vocab-collection', words: [...currentVocabCollection] })
          currentVocabCollection.length = 0
        }
        blocks.push({ type: 'markdown', content: mdContent, htmlContent: props.formatMarkdown(mdContent) })
      }
    }

    const attrStr = match[1]
    const wordMatch = attrStr.match(/word="([^"]*)"/)
    const phoneticMatch = attrStr.match(/phonetic="([^"]*)"/)
    const sentenceMatch = attrStr.match(/sentence="([^"]*)"/)
    const transMatch = attrStr.match(/translation="([^"]*)"/)
    const defMatch = attrStr.match(/definition="([^"]*)"/)
    const modeMatch = attrStr.match(/mode="([^"]*)"/)

    if (wordMatch) {
      currentVocabCollection.push({
        word: wordMatch[1],
        phonetic: phoneticMatch ? phoneticMatch[1] : '',
        sentence: sentenceMatch ? sentenceMatch[1] : '',
        translation: transMatch ? transMatch[1] : '',
        definition: defMatch ? defMatch[1] : '',
        mode: modeMatch ? modeMatch[1] : 'pronunciation'
      })
    }

    lastIndex = vocabRegex.lastIndex
  }

  let remaining = text.substring(lastIndex)

  const hasOpenTag = remaining.includes('<vocab-practice>') || remaining.includes('<vocab ')

  if (props.isStreaming && hasOpenTag) {
    const openIndex = remaining.indexOf('<vocab')
    if (openIndex > 0) {
      let mdContent = remaining.substring(0, openIndex).replace(/<vocab-practice>/g, '').replace(/<\/vocab-practice>/g, '')
      if (mdContent.trim()) {
        if (currentVocabCollection.length > 0) {
          blocks.push({ type: 'vocab-collection', words: [...currentVocabCollection] })
          currentVocabCollection.length = 0
        }
        blocks.push({ type: 'markdown', content: mdContent, htmlContent: props.formatMarkdown(mdContent) })
      }
    }

    // 只有在没有已完成的词汇集合时才显示骨架屏
    // 如果已经有词汇数据，先显示已有的，避免闪烁
    if (currentVocabCollection.length === 0 && lastVocabCollection.value.length === 0) {
      blocks.push({ type: 'vocab-skeleton' })
    } else if (currentVocabCollection.length > 0) {
      // 更新缓存的词汇集合
      lastVocabCollection.value = [...currentVocabCollection]
      blocks.push({ type: 'vocab-collection', words: [...currentVocabCollection] })
    } else if (lastVocabCollection.value.length > 0) {
      // 使用缓存的词汇集合，避免闪烁
      blocks.push({ type: 'vocab-collection', words: lastVocabCollection.value })
    }
  } else {
    remaining = remaining.replace(/<\/vocab-practice>/g, '').replace(/<vocab-practice>/g, '')
    if (currentVocabCollection.length > 0) {
      lastVocabCollection.value = [...currentVocabCollection]
      blocks.push({ type: 'vocab-collection', words: [...currentVocabCollection] })
    }
    if (remaining.trim()) {
      blocks.push({ type: 'markdown', content: remaining, htmlContent: props.formatMarkdown(remaining) })
    }
  }

  // 记录块数量，用于调试
  lastBlockCount.value = blocks.length

  return blocks
})

// 当内容完全更新后，清除缓存
watch(() => props.isStreaming, (newVal, oldVal) => {
  if (oldVal && !newVal) {
    // 流式传输结束，清除缓存
    lastVocabCollection.value = []
  }
})
</script>

<style scoped>
.chat-content-renderer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.markdown-block {
  width: 100%;
}

.vocab-collection-panel {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background-color: var(--bg-secondary);
  overflow: hidden;
  margin: 12px 0;
  max-width: 480px;
  animation: panel-fade-in 0.25s ease-out;
}

@keyframes panel-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.panel-header {
  padding: 12px 16px;
  background-color: rgba(59, 130, 246, 0.1);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text-primary);
}

body.dark-mode .panel-header {
  background-color: rgba(59, 130, 246, 0.15);
  color: #e5e7eb;
}

.panel-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.word-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.word-tag {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.85rem;
  color: var(--text-secondary);
  transition: all 0.2s;
}

body.dark-mode .word-tag {
  background: var(--bg-secondary);
  border-color: var(--border-color);
  color: var(--text-primary);
}

.word-tag.more {
  background: transparent;
  border: 1px dashed var(--border-color);
}

.start-practice-btn {
  width: 100%;
  padding: 12px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: opacity 0.2s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

body.dark-mode .start-practice-btn {
  background: #2563eb;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
}

.start-practice-btn:hover {
  opacity: 0.9;
}
</style>
