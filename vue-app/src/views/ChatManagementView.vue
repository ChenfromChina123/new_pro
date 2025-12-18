<template>
  <AppLayout>
    <div class="chat-management-page">
      <div class="container">
        <div class="page-header">
          <h1>💾 聊天记录管理</h1>
          <p>查看和管理你的所有对话记录</p>
        </div>
        
        <div class="sessions-grid">
          <div
            v-for="session in sessions"
            :key="session.id"
            class="session-card card"
          >
            <div class="session-header">
              <h3>{{ session.title || '新对话' }}</h3>
              <button
                class="delete-btn"
                title="删除"
                @click="deleteSession(session.id)"
              >
                🗑️
              </button>
            </div>
            
            <div class="session-info">
              <p class="session-date">
                📅 {{ formatDate(session.created_at) }}
              </p>
              <p
                v-if="session.message_count"
                class="session-count"
              >
                💬 {{ session.message_count }} 条消息
              </p>
            </div>
            
            <button
              class="btn btn-primary"
              @click="viewSession(session.id)"
            >
              查看详情
            </button>
          </div>
        </div>
        
        <div
          v-if="sessions.length === 0"
          class="empty-state"
        >
          <div class="empty-icon">
            💬
          </div>
          <h3>暂无聊天记录</h3>
          <p>开始新的对话来创建记录</p>
          <router-link
            to="/chat"
            class="btn btn-primary"
          >
            开始对话
          </router-link>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import AppLayout from '@/components/AppLayout.vue'

const router = useRouter()
const chatStore = useChatStore()
const sessions = ref([])

onMounted(async () => {
  await fetchSessions()
})

const fetchSessions = async () => {
  await chatStore.fetchSessions()
  sessions.value = chatStore.sessions
}

const viewSession = (sessionId) => {
  router.push({ name: 'Chat', query: { session: sessionId } })
}

const deleteSession = async (sessionId) => {
  if (confirm('确定要删除这个会话吗？')) {
    const result = await chatStore.deleteSession(sessionId)
    if (result.success) {
      await fetchSessions()
    }
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.chat-management-page {
  min-height: calc(100vh - 64px);
  padding: 32px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.page-header p {
  color: var(--text-secondary);
  font-size: 16px;
}

.sessions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.session-card {
  display: flex;
  flex-direction: column;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.session-header h3 {
  font-size: 18px;
  margin: 0;
  flex: 1;
}

.delete-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  opacity: 1;
  transform: scale(1.2);
}

.session-info {
  flex: 1;
  margin-bottom: 16px;
}

.session-date,
.session-count {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 4px 0;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
}

.empty-state h3 {
  font-size: 24px;
  margin-bottom: 12px;
}

.empty-state p {
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .sessions-grid {
    grid-template-columns: 1fr;
  }
}
</style>

