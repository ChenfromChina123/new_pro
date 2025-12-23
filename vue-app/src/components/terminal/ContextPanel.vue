<template>
  <div class="context-panel" :class="{ collapsed }">
    <div class="panel-header">
      <h3>上下文面板</h3>
      <button class="collapse-btn" @click="toggleCollapse">
        {{ collapsed ? '展开' : '收起' }}
      </button>
    </div>

    <div v-show="!collapsed" class="panel-content">
      <!-- 项目信息 -->
      <div class="section">
        <div class="section-title">
          <span class="icon">📦</span>
          <span>项目信息</span>
        </div>
        <div class="section-body">
          <div class="info-item">
            <span class="label">环境:</span>
            <span class="value">{{ projectContext.env }}</span>
          </div>
          <div class="info-item">
            <span class="label">框架:</span>
            <span class="value">{{ projectContext.framework }}</span>
          </div>
          <div class="info-item">
            <span class="label">语言:</span>
            <span class="value">{{ projectContext.lang.join(', ') }}</span>
          </div>
        </div>
      </div>

      <!-- 可见文件 -->
      <div class="section">
        <div class="section-title">
          <span class="icon">📁</span>
          <span>可见文件 ({{ visibleFiles.length }})</span>
        </div>
        <div class="section-body">
          <div v-if="visibleFiles.length === 0" class="empty-state">
            暂无可见文件
          </div>
          <div
            v-for="file in visibleFiles.slice(0, maxVisibleFiles)"
            :key="file"
            class="file-item"
            :title="file"
          >
            {{ getFileName(file) }}
          </div>
          <div v-if="visibleFiles.length > maxVisibleFiles" class="more-indicator">
            +{{ visibleFiles.length - maxVisibleFiles }} 更多...
          </div>
        </div>
      </div>

      <!-- 历史摘要 -->
      <div class="section">
        <div class="section-title">
          <span class="icon">📝</span>
          <span>历史摘要</span>
        </div>
        <div class="section-body">
          <div v-if="historySummary.length === 0" class="empty-state">
            暂无历史记录
          </div>
          <div
            v-for="(item, index) in historySummary"
            :key="index"
            class="history-item"
          >
            {{ item }}
          </div>
        </div>
      </div>

      <!-- Token统计 -->
      <div class="section">
        <div class="section-title">
          <span class="icon">🔢</span>
          <span>Token统计</span>
        </div>
        <div class="section-body">
          <div class="token-stats">
            <div class="stat-item">
              <span class="stat-label">当前消耗:</span>
              <span class="stat-value">{{ currentTokens.toLocaleString() }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">压缩率:</span>
              <span class="stat-value">{{ compressionRatio }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">信息保留:</span>
              <span class="stat-value">{{ infoRetention }}%</span>
            </div>
          </div>
          <div class="token-progress">
            <div class="progress-bar" :style="{ width: tokenUsagePercent + '%' }"></div>
          </div>
          <div class="token-hint">
            <span v-if="tokenUsagePercent < 70" class="hint-normal">
              Token使用正常
            </span>
            <span v-else-if="tokenUsagePercent < 90" class="hint-warning">
              Token使用较高
            </span>
            <span v-else class="hint-danger">
              Token使用接近上限
            </span>
          </div>
        </div>
      </div>

      <!-- 性能指标 -->
      <div class="section">
        <div class="section-title">
          <span class="icon">⚡</span>
          <span>性能指标</span>
        </div>
        <div class="section-body">
          <div class="perf-item">
            <span class="perf-label">冷启动延迟:</span>
            <span class="perf-value" :class="getLatencyClass(coldStartLatency)">
              {{ coldStartLatency }}ms
            </span>
          </div>
          <div class="perf-item">
            <span class="perf-label">平均响应:</span>
            <span class="perf-value" :class="getLatencyClass(avgResponseTime)">
              {{ avgResponseTime }}ms
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ProjectContext } from '@/types/terminal-message'

interface Props {
  projectContext: ProjectContext
  visibleFiles: string[]
  historySummary: string[]
  currentTokens: number
  maxTokens?: number
  compressionRatio?: number
  infoRetention?: number
  coldStartLatency?: number
  avgResponseTime?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxTokens: 100000,
  compressionRatio: 60,
  infoRetention: 90,
  coldStartLatency: 0,
  avgResponseTime: 0
})

const collapsed = ref(false)
const maxVisibleFiles = ref(10)

/**
 * 切换折叠状态
 */
const toggleCollapse = () => {
  collapsed.value = !collapsed.value
}

/**
 * 获取文件名
 */
const getFileName = (path: string): string => {
  const parts = path.split(/[/\\]/)
  return parts[parts.length - 1]
}

/**
 * Token使用百分比
 */
const tokenUsagePercent = computed(() => {
  return Math.min(100, (props.currentTokens / props.maxTokens) * 100)
})

/**
 * 获取延迟等级类名
 */
const getLatencyClass = (latency: number): string => {
  if (latency === 0) return 'perf-unknown'
  if (latency < 800) return 'perf-good'
  if (latency < 1500) return 'perf-normal'
  return 'perf-slow'
}
</script>

<style scoped>
.context-panel {
  width: 320px;
  min-width: 280px;
  max-width: 400px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s ease;
}

.context-panel.collapsed {
  width: 50px;
  min-width: 50px;
}

.panel-header {
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.collapse-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: #e2e8f0;
  color: #3b82f6;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.section {
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.section-title {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.icon {
  font-size: 1.1rem;
}

.section-body {
  padding: 12px 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 0.85rem;
}

.info-item:last-child {
  margin-bottom: 0;
}

.label {
  color: #64748b;
  font-weight: 500;
}

.value {
  color: #1e293b;
  font-weight: 600;
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  font-size: 0.85rem;
  padding: 16px 0;
}

.file-item {
  padding: 6px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  margin-bottom: 6px;
  font-size: 0.8rem;
  color: #475569;
  font-family: 'Fira Code', monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.file-item:hover {
  background: #e2e8f0;
  border-color: #cbd5e1;
}

.more-indicator {
  text-align: center;
  color: #64748b;
  font-size: 0.8rem;
  margin-top: 8px;
  font-style: italic;
}

.history-item {
  padding: 8px 10px;
  background: #f8fafc;
  border-left: 3px solid #3b82f6;
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.4;
}

.token-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
}

.stat-label {
  color: #64748b;
}

.stat-value {
  color: #1e293b;
  font-weight: 600;
  font-family: 'Fira Code', monospace;
}

.token-progress {
  width: 100%;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  transition: width 0.3s ease;
}

.token-hint {
  text-align: center;
  font-size: 0.8rem;
  font-weight: 500;
}

.hint-normal {
  color: #10b981;
}

.hint-warning {
  color: #f59e0b;
}

.hint-danger {
  color: #ef4444;
}

.perf-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 0.85rem;
}

.perf-label {
  color: #64748b;
}

.perf-value {
  font-weight: 600;
  font-family: 'Fira Code', monospace;
}

.perf-good {
  color: #10b981;
}

.perf-normal {
  color: #f59e0b;
}

.perf-slow {
  color: #ef4444;
}

.perf-unknown {
  color: #94a3b8;
}
</style>

