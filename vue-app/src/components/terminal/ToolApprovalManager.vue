<template>
  <div class="tool-approval-manager">
    <div class="manager-header">
      <h3>⚠️ 工具批准管理</h3>
      <div class="header-actions">
        <button class="btn-settings" @click="showSettings = true">
          ⚙️ 批准设置
        </button>
      </div>
    </div>

    <div class="manager-content">
      <!-- 待批准列表 -->
      <div v-if="pendingApprovals.length > 0" class="pending-section">
        <div class="section-header">
          <h4>待批准工具 ({{ pendingApprovals.length }})</h4>
          <div class="bulk-actions">
            <button class="btn-approve-all" @click="$emit('approve-all')">
              ✅ 全部批准
            </button>
            <button class="btn-reject-all" @click="$emit('reject-all')">
              ❌ 全部拒绝
            </button>
          </div>
        </div>

        <div class="approval-list">
          <div
            v-for="approval in pendingApprovals"
            :key="approval.decisionId"
            class="approval-card"
            :class="getDangerLevel(approval.toolName)"
          >
            <div class="card-header">
              <div class="tool-info">
                <div class="tool-icon">
                  {{ getToolIcon(approval.toolName) }}
                </div>
                <div class="tool-details">
                  <div class="tool-name">{{ getToolLabel(approval.toolName) }}</div>
                  <div class="tool-time">{{ formatTime(approval.timestamp) }}</div>
                </div>
              </div>
              <div class="danger-badge" :class="getDangerLevel(approval.toolName)">
                {{ getDangerLabel(approval.toolName) }}
              </div>
            </div>

            <div class="card-body">
              <div class="params-section">
                <div class="params-title">参数详情</div>
                <div class="params-content">
                  <pre>{{ formatParams(approval.params) }}</pre>
                </div>
              </div>

              <!-- 风险提示 -->
              <div v-if="getRiskWarning(approval.toolName)" class="risk-warning">
                <span class="warning-icon">⚠️</span>
                <span class="warning-text">{{ getRiskWarning(approval.toolName) }}</span>
              </div>
            </div>

            <div class="card-actions">
              <button class="btn-approve" @click="approveWithReason(approval)">
                ✅ 批准
              </button>
              <button class="btn-reject" @click="rejectWithReason(approval)">
                ❌ 拒绝
              </button>
              <button class="btn-details" @click="showDetails(approval)">
                🔍 详情
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">✅</div>
        <p>暂无待批准的工具</p>
        <p class="hint">危险操作将在此处等待您的批准</p>
      </div>

      <!-- 历史记录 -->
      <div v-if="approvalHistory.length > 0" class="history-section">
        <div class="section-header">
          <h4>批准历史</h4>
          <button class="btn-clear-history" @click="clearHistory">
            🗑️ 清空
          </button>
        </div>

        <div class="history-list">
          <div
            v-for="(record, index) in approvalHistory"
            :key="index"
            class="history-item"
            :class="record.status"
          >
            <div class="history-icon">
              {{ record.status === 'APPROVED' ? '✅' : '❌' }}
            </div>
            <div class="history-content">
              <div class="history-tool">{{ getToolLabel(record.toolName) }}</div>
              <div class="history-time">{{ formatTime(record.timestamp) }}</div>
            </div>
            <div class="history-reason" v-if="record.reason">
              {{ record.reason }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 批准设置对话框 -->
    <div v-if="showSettings" class="settings-overlay" @click="showSettings = false">
      <div class="settings-dialog" @click.stop>
        <div class="dialog-header">
          <h3>批准设置</h3>
          <button class="close-btn" @click="showSettings = false">✕</button>
        </div>

        <div class="dialog-body">
          <div class="settings-section">
            <h4>自动批准规则</h4>
            <p class="section-hint">启用后，这些工具将自动批准，无需手动确认</p>

            <div class="settings-list">
              <div
                v-for="tool in toolList"
                :key="tool.name"
                class="setting-item"
              >
                <div class="setting-info">
                  <div class="setting-icon">{{ tool.icon }}</div>
                  <div class="setting-details">
                    <div class="setting-name">{{ tool.label }}</div>
                    <div class="setting-desc">{{ tool.description }}</div>
                  </div>
                </div>
                <label class="toggle-switch">
                  <input
                    type="checkbox"
                    :checked="autoApprovalRules[tool.name]"
                    @change="toggleAutoApproval(tool.name, $event.target.checked)"
                  >
                  <span class="toggle-slider" />
                </label>
              </div>
            </div>
          </div>

          <div class="settings-section">
            <h4>批准策略</h4>
            <div class="strategy-options">
              <label class="radio-option">
                <input
                  type="radio"
                  name="strategy"
                  value="strict"
                  :checked="approvalStrategy === 'strict'"
                  @change="approvalStrategy = 'strict'"
                >
                <span>严格模式 - 所有危险操作都需要批准</span>
              </label>
              <label class="radio-option">
                <input
                  type="radio"
                  name="strategy"
                  value="balanced"
                  :checked="approvalStrategy === 'balanced'"
                  @change="approvalStrategy = 'balanced'"
                >
                <span>平衡模式 - 仅高危操作需要批准</span>
              </label>
              <label class="radio-option">
                <input
                  type="radio"
                  name="strategy"
                  value="permissive"
                  :checked="approvalStrategy === 'permissive'"
                  @change="approvalStrategy = 'permissive'"
                >
                <span>宽松模式 - 自动批准大部分操作</span>
              </label>
            </div>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="btn-primary" @click="saveSettings">保存设置</button>
          <button class="btn-secondary" @click="showSettings = false">取消</button>
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <div v-if="showDetailsDialog" class="details-overlay" @click="showDetailsDialog = false">
      <div class="details-dialog" @click.stop>
        <div class="dialog-header">
          <h3>工具详情</h3>
          <button class="close-btn" @click="showDetailsDialog = false">✕</button>
        </div>

        <div class="dialog-body">
          <div v-if="selectedApproval" class="details-content">
            <div class="detail-row">
              <span class="detail-label">工具名称:</span>
              <span class="detail-value">{{ getToolLabel(selectedApproval.toolName) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">决策ID:</span>
              <span class="detail-value">{{ selectedApproval.decisionId }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">请求时间:</span>
              <span class="detail-value">{{ formatFullTime(selectedApproval.timestamp) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">危险等级:</span>
              <span class="detail-value">
                <span class="danger-badge" :class="getDangerLevel(selectedApproval.toolName)">
                  {{ getDangerLabel(selectedApproval.toolName) }}
                </span>
              </span>
            </div>
            <div class="detail-row full-width">
              <span class="detail-label">参数:</span>
              <div class="detail-params">
                <pre>{{ JSON.stringify(selectedApproval.params, null, 2) }}</pre>
              </div>
            </div>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="btn-approve" @click="approveSelected">✅ 批准</button>
          <button class="btn-reject" @click="rejectSelected">❌ 拒绝</button>
          <button class="btn-secondary" @click="showDetailsDialog = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  pendingApprovals: {
    type: Array,
    default: () => []
  },
  approvalHistory: {
    type: Array,
    default: () => []
  },
  autoApprovalRules: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits([
  'approve',
  'reject',
  'approve-all',
  'reject-all',
  'update-rules',
  'clear-history'
])

const showSettings = ref(false)
const showDetailsDialog = ref(false)
const selectedApproval = ref(null)
const approvalStrategy = ref('balanced')

/**
 * 工具列表配置
 */
const toolList = [
  {
    name: 'read_file',
    label: '读取文件',
    icon: '📖',
    description: '读取文件内容',
    danger: 'low'
  },
  {
    name: 'search_files',
    label: '搜索文件',
    icon: '🔍',
    description: '在文件中搜索内容',
    danger: 'low'
  },
  {
    name: 'write_file',
    label: '写入文件',
    icon: '✏️',
    description: '创建或覆盖文件',
    danger: 'high'
  },
  {
    name: 'modify_file',
    label: '修改文件',
    icon: '📝',
    description: '精确修改文件内容',
    danger: 'medium'
  },
  {
    name: 'delete_file',
    label: '删除文件',
    icon: '🗑️',
    description: '删除文件或目录',
    danger: 'critical'
  },
  {
    name: 'execute_command',
    label: '执行命令',
    icon: '⚡',
    description: '执行系统命令',
    danger: 'high'
  }
]

/**
 * 获取工具图标
 */
function getToolIcon(toolName) {
  const tool = toolList.find(t => t.name === toolName)
  return tool?.icon || '🛠️'
}

/**
 * 获取工具标签
 */
function getToolLabel(toolName) {
  const tool = toolList.find(t => t.name === toolName)
  return tool?.label || toolName
}

/**
 * 获取危险等级
 */
function getDangerLevel(toolName) {
  const tool = toolList.find(t => t.name === toolName)
  return tool?.danger || 'medium'
}

/**
 * 获取危险等级标签
 */
function getDangerLabel(toolName) {
  const level = getDangerLevel(toolName)
  const labels = {
    'low': '低风险',
    'medium': '中风险',
    'high': '高风险',
    'critical': '极高风险'
  }
  return labels[level] || '未知'
}

/**
 * 获取风险警告
 */
function getRiskWarning(toolName) {
  const warnings = {
    'write_file': '此操作将创建或覆盖文件，可能导致数据丢失',
    'delete_file': '此操作将永久删除文件，无法恢复',
    'execute_command': '此操作将执行系统命令，可能影响系统安全',
    'modify_file': '此操作将修改文件内容，请确认修改正确'
  }
  return warnings[toolName] || null
}

/**
 * 格式化参数
 */
function formatParams(params) {
  if (!params) return '{}'
  
  // 简化显示
  const simplified = {}
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === 'string' && value.length > 100) {
      simplified[key] = value.substring(0, 100) + '...'
    } else {
      simplified[key] = value
    }
  }
  
  return JSON.stringify(simplified, null, 2)
}

/**
 * 格式化时间
 */
function formatTime(timestamp) {
  if (!timestamp) return 'N/A'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60 * 1000) {
    return '刚刚'
  } else if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 1000))} 分钟前`
  } else {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }
}

/**
 * 格式化完整时间
 */
function formatFullTime(timestamp) {
  if (!timestamp) return 'N/A'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

/**
 * 批准（带原因）
 */
function approveWithReason(approval) {
  const reason = prompt('批准原因（可选）:')
  if (reason !== null) {
    emit('approve', { id: approval.decisionId, reason })
  }
}

/**
 * 拒绝（带原因）
 */
function rejectWithReason(approval) {
  const reason = prompt('拒绝原因（可选）:')
  if (reason !== null) {
    emit('reject', { id: approval.decisionId, reason })
  }
}

/**
 * 显示详情
 */
function showDetails(approval) {
  selectedApproval.value = approval
  showDetailsDialog.value = true
}

/**
 * 批准选中的
 */
function approveSelected() {
  if (selectedApproval.value) {
    emit('approve', { id: selectedApproval.value.decisionId, reason: '' })
    showDetailsDialog.value = false
  }
}

/**
 * 拒绝选中的
 */
function rejectSelected() {
  if (selectedApproval.value) {
    emit('reject', { id: selectedApproval.value.decisionId, reason: '' })
    showDetailsDialog.value = false
  }
}

/**
 * 切换自动批准
 */
function toggleAutoApproval(toolName, enabled) {
  emit('update-rules', { toolName, enabled })
}

/**
 * 保存设置
 */
function saveSettings() {
  // 根据策略更新规则
  const newRules = {}
  
  switch (approvalStrategy.value) {
    case 'strict':
      // 严格模式：所有工具都需要批准
      toolList.forEach(tool => {
        newRules[tool.name] = false
      })
      break
    
    case 'balanced':
      // 平衡模式：低风险自动批准
      toolList.forEach(tool => {
        newRules[tool.name] = tool.danger === 'low'
      })
      break
    
    case 'permissive':
      // 宽松模式：除了极高风险都自动批准
      toolList.forEach(tool => {
        newRules[tool.name] = tool.danger !== 'critical'
      })
      break
  }
  
  emit('update-rules', newRules)
  showSettings.value = false
}

/**
 * 清空历史
 */
function clearHistory() {
  if (confirm('确定要清空批准历史吗？')) {
    emit('clear-history')
  }
}
</script>

<style scoped>
.tool-approval-manager {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.manager-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.manager-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-settings {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-settings:hover {
  background: #e2e8f0;
}

.manager-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 20px;
  scrollbar-width: thin;
  scrollbar-color: #3b82f6 #f1f5f9;
}

.manager-content::-webkit-scrollbar {
  width: 6px;
}

.manager-content::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.manager-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.manager-content::-webkit-scrollbar-thumb:hover {
  background: #3b82f6;
}

.pending-section, .history-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.bulk-actions {
  display: flex;
  gap: 8px;
}

.btn-approve-all, .btn-reject-all {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-approve-all {
  background: #dcfce7;
  color: #166534;
}

.btn-approve-all:hover {
  background: #bbf7d0;
}

.btn-reject-all {
  background: #fee2e2;
  color: #991b1b;
}

.btn-reject-all:hover {
  background: #fecaca;
}

.approval-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.approval-card {
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
}

.approval-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.approval-card.high, .approval-card.critical {
  border-color: #fbbf24;
  background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tool-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tool-icon {
  font-size: 2rem;
}

.tool-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tool-name {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.tool-time {
  font-size: 0.85rem;
  color: #64748b;
}

.danger-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.danger-badge.low {
  background: #dcfce7;
  color: #166534;
}

.danger-badge.medium {
  background: #fef3c7;
  color: #92400e;
}

.danger-badge.high {
  background: #fed7aa;
  color: #9a3412;
}

.danger-badge.critical {
  background: #fee2e2;
  color: #991b1b;
}

.card-body {
  margin-bottom: 12px;
}

.params-section {
  margin-bottom: 12px;
}

.params-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
}

.params-content {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.params-content pre {
  margin: 0;
  font-size: 0.85rem;
  color: #1e293b;
  white-space: pre-wrap;
  word-break: break-all;
}

.risk-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fef3c7;
  border: 1px solid #fbbf24;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #92400e;
}

.warning-icon {
  font-size: 1.2rem;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.card-actions button {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-approve {
  background: #10b981;
  color: #fff;
}

.btn-approve:hover {
  background: #059669;
}

.btn-reject {
  background: #ef4444;
  color: #fff;
}

.btn-reject:hover {
  background: #dc2626;
}

.btn-details {
  background: #f1f5f9;
  color: #64748b;
}

.btn-details:hover {
  background: #e2e8f0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 8px 0;
}

.hint {
  font-size: 0.85rem;
  color: #cbd5e1;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.history-icon {
  font-size: 1.5rem;
}

.history-content {
  flex: 1;
}

.history-tool {
  font-size: 0.9rem;
  font-weight: 500;
  color: #1e293b;
}

.history-time {
  font-size: 0.8rem;
  color: #64748b;
}

.history-reason {
  font-size: 0.85rem;
  color: #64748b;
  font-style: italic;
}

.btn-clear-history {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  background: #fee2e2;
  color: #991b1b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-clear-history:hover {
  background: #fecaca;
}

/* 对话框样式 */
.settings-overlay, .details-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.settings-dialog, .details-dialog {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.dialog-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #64748b;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.close-btn:hover {
  background: #f1f5f9;
}

.dialog-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.settings-section {
  margin-bottom: 24px;
}

.settings-section h4 {
  margin: 0 0 8px 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.section-hint {
  margin: 0 0 16px 0;
  font-size: 0.85rem;
  color: #64748b;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.setting-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.setting-icon {
  font-size: 1.5rem;
}

.setting-details {
  flex: 1;
}

.setting-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: #1e293b;
}

.setting-desc {
  font-size: 0.8rem;
  color: #64748b;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #cbd5e1;
  transition: 0.4s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.4s;
  border-radius: 50%;
}

input:checked + .toggle-slider {
  background-color: #3b82f6;
}

input:checked + .toggle-slider:before {
  transform: translateX(24px);
}

.strategy-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.radio-option:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.radio-option input {
  cursor: pointer;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #e2e8f0;
}

.btn-primary, .btn-secondary {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #3b82f6;
  color: #fff;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-secondary {
  background: #f1f5f9;
  color: #64748b;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

.details-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  gap: 12px;
}

.detail-row.full-width {
  flex-direction: column;
}

.detail-label {
  font-weight: 600;
  color: #64748b;
  min-width: 100px;
}

.detail-value {
  color: #1e293b;
  flex: 1;
}

.detail-params {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.detail-params pre {
  margin: 0;
  font-size: 0.85rem;
  color: #1e293b;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

