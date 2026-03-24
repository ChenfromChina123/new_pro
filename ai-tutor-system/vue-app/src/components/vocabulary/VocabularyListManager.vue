<template>
  <div class="vocabulary-list-manager">
    <div class="two-column-layout">
      <div class="list-column card">
        <div class="card-header">
          <h3>单词表</h3>
          <button class="btn btn-sm btn-primary" @click="$emit('create-list')">
            + 新建
          </button>
        </div>
        <div class="list-container">
          <div
            v-for="list in lists"
            :key="list.id"
            class="list-item"
            :class="{ active: currentListId === list.id }"
            @click="$emit('select-list', list.id)"
          >
            <div class="list-info">
              <h4>{{ list.name }}</h4>
              <span class="badge">{{ (list.language || 'en').toUpperCase() }}</span>
              <span class="count">{{ list.wordCount || 0 }} 词</span>
            </div>
            <button
              class="btn-icon delete-btn"
              title="删除"
              @click.stop="$emit('delete-list', list.id)"
            >
              ×
            </button>
          </div>
        </div>
      </div>

      <div class="words-column card">
        <div class="card-header">
          <h3>{{ currentList ? currentList.name : '单词列表' }}</h3>
          <div class="actions">
            <button
              v-if="currentListId"
              class="btn btn-sm btn-outline"
              @click="$emit('refresh-words')"
            >
              刷新
            </button>
            <button
              v-if="currentListId"
              class="btn btn-sm btn-primary"
              @click="$emit('add-word')"
            >
              + 添加单词
            </button>
          </div>
        </div>

        <div v-if="!currentListId" class="empty-state">
          <p>请选择一个单词表查看详情</p>
        </div>
        <div v-else-if="words.length === 0" class="empty-state">
          <p>暂无单词，点击上方按钮添加</p>
        </div>
        <div v-else class="words-grid">
          <div
            v-for="word in words"
            :key="word.id"
            class="word-card-item"
          >
            <div class="word-header">
              <h4>{{ word.word }}</h4>
              <span class="pos">{{ word.partOfSpeech }}</span>
            </div>
            <div class="word-body">
              <p class="definition">{{ word.definition }}</p>
              <p v-if="word.example" class="example">{{ word.example }}</p>
            </div>
            <div class="word-footer">
              <span :class="['status-tag', getMasteryClass(getProgress(word.id).masteryLevel)]">
                {{ getMasteryText(getProgress(word.id).masteryLevel) }}
              </span>
              <div class="controls">
                <select
                  class="select-sm"
                  :value="getProgress(word.id).masteryLevel ?? 0"
                  @change="$emit('change-mastery', word.id, $event.target.value)"
                >
                  <option v-for="i in 6" :key="i" :value="i-1">{{ i-1 }}</option>
                </select>
                <button class="btn-icon delete-btn" @click="$emit('delete-word', word.id)">
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  lists: {
    type: Array,
    default: () => []
  },
  currentListId: {
    type: [String, Number],
    default: null
  },
  currentList: {
    type: Object,
    default: null
  },
  words: {
    type: Array,
    default: () => []
  },
  progressMap: {
    type: Object,
    default: () => ({})
  }
})

defineEmits(['create-list', 'select-list', 'delete-list', 'refresh-words', 'add-word', 'delete-word', 'change-mastery'])

const getProgress = (wordId, progressMap) => {
  return progressMap?.[wordId] || { masteryLevel: 0 }
}

const getMasteryClass = (level) => {
  if (level >= 5) return 'mastered'
  if (level >= 3) return 'learning'
  return 'new'
}

const getMasteryText = (level) => {
  if (level >= 5) return '已掌握'
  if (level >= 3) return '学习中'
  return '新词'
}
</script>

<style scoped>
.vocabulary-list-manager {
  width: 100%;
}

.two-column-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

@media (max-width: 768px) {
  .two-column-layout {
    grid-template-columns: 1fr;
  }
}

.card {
  background: var(--bg-primary);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.list-container {
  max-height: 500px;
  overflow-y: auto;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-bottom: 1px solid var(--border-color);
}

.list-item:last-child {
  border-bottom: none;
}

.list-item:hover {
  background: var(--bg-secondary);
}

.list-item.active {
  background: rgba(59, 130, 246, 0.1);
  border-left: 3px solid var(--primary-color);
}

.list-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.list-info h4 {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}

.badge {
  font-size: 11px;
  padding: 2px 6px;
  background: var(--bg-tertiary);
  border-radius: 4px;
  color: var(--text-secondary);
}

.count {
  font-size: 12px;
  color: var(--text-tertiary);
}

.btn-icon {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}

.btn-icon:hover {
  background: var(--bg-tertiary);
}

.delete-btn {
  color: var(--text-tertiary);
}

.delete-btn:hover {
  color: #ef4444;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-secondary);
}

.words-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 20px;
}

.word-card-item {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.word-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.word-header h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.pos {
  font-size: 12px;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
}

.definition {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
}

.example {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
  font-style: italic;
}

.word-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.status-tag {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 6px;
}

.status-tag.mastered {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}

.status-tag.learning {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.status-tag.new {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.select-sm {
  padding: 4px 8px;
  font-size: 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}
</style>
