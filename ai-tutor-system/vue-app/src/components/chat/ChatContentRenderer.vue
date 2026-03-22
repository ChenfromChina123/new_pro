<template>
  <div class="chat-content-renderer">
    <template v-for="(block, index) in parsedBlocks" :key="index">
      <!-- eslint-disable-next-line vue/no-v-html -->
      <div v-if="block.type === 'markdown'" class="markdown-block" v-html="block.htmlContent"></div>
      
      <!-- 如果解析到了 vocab 标签，我们不再直接平铺所有卡片，而是显示一个概览入口面板 -->
      <div v-else-if="block.type === 'vocab-collection' && block.words.length > 0" class="vocab-collection-panel">
        <div class="panel-header">
          <i class="fas fa-bullseye text-blue-500"></i>
          <span>你的专属词汇练习生成完毕</span>
        </div>
        <div class="panel-body">
          <div class="word-tags">
            <span v-for="(w, idx) in block.words.slice(0, 5)" :key="idx" class="word-tag">{{ w.word }}</span>
            <span v-if="block.words.length > 5" class="word-tag more">等 {{ block.words.length }} 词</span>
          </div>
          <button class="start-practice-btn" @click="openVocabModal(block.words)">
            <i class="fas fa-play-circle"></i> 进入专注模式开始练习
          </button>
        </div>
      </div>
      
      <VocabSkeleton v-else-if="block.type === 'vocab-skeleton'" />
    </template>
    <span v-if="isStreaming" class="typing-cursor"></span>

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
import { computed, ref } from 'vue'
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
    
    currentVocabCollection.push({
      word: wordMatch ? wordMatch[1] : '',
      phonetic: phoneticMatch ? phoneticMatch[1] : '',
      sentence: sentenceMatch ? sentenceMatch[1] : '',
      translation: transMatch ? transMatch[1] : '',
      definition: defMatch ? defMatch[1] : '',
      mode: modeMatch ? modeMatch[1] : 'pronunciation'
    })
    
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
    blocks.push({ type: 'vocab-skeleton' })
  } else {
    remaining = remaining.replace(/<\/vocab-practice>/g, '').replace(/<vocab-practice>/g, '')
    if (currentVocabCollection.length > 0) {
      blocks.push({ type: 'vocab-collection', words: [...currentVocabCollection] })
    }
    if (remaining.trim()) {
      blocks.push({ type: 'markdown', content: remaining, htmlContent: props.formatMarkdown(remaining) })
    }
  }
  
  return blocks
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
}

.start-practice-btn:hover {
  opacity: 0.9;
}
</style>
