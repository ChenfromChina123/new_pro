<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="approval-overlay" @click="handleOverlayClick">
        <div class="approval-dialog" @click.stop>
          <div class="dialog-header">
            <div class="header-title">
              <span class="icon">🔐</span>
              <h3>权限审批请求</h3>
            </div>
            <button class="close-btn" @click="handleReject">✕</button>
          </div>

          <div class="dialog-body">
            <div class="request-info">
              <div class="info-row">
                <span class="info-label">操作描述:</span>
                <span class="info-value">{{ request.description }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">所需权限:</span>
                <span class="permission-badge" :class="request.requiredPermission">
                  {{ getPermissionLabel(request.requiredPermission) }}
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">操作范围:</span>
                <span class="scope-badge" :class="request.scope">
                  {{ getScopeLabel(request.scope) }}
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">目标路径:</span>
                <code class="target-path">{{ request.target }}</code>
              </div>
              <div class="info-row">
                <span class="info-label">请求时间:</span>
                <span class="info-value">{{ formatTime(request.timestamp) }}</span>
              </div>
            </div>

            <div class="warning-box">
              <div class="warning-header">
                <span class="warning-icon">⚠️</span>
                <span class="warning-title">安全提示</span>
              </div>
              <ul class="warning-list">
                <li v-if="request.scope === 'execute'">
                  此操作将执行系统命令，可能会修改文件或安装依赖
                </li>
                <li v-if="request.scope === 'write'">
                  此操作将修改文件系统，请确认操作安全性
                </li>
                <li v-if="request.requiredPermission === 'system'">
                  系统级权限操作需要特别注意，建议仔细检查
                </li>
                <li>
                  审批后操作将立即执行，请谨慎决定
                </li>
              </ul>
            </div>

            <div class="timeout-indicator">
              <div class="timeout-label">
                审批倒计时: {{ remainingSeconds }}秒
              </div>
              <div class="timeout-progress">
                <div
                  class="progress-bar"
                  :style="{ width: progressPercent + '%' }"
                />
              </div>
            </div>
          </div>

          <div class="dialog-footer">
            <button class="btn btn-reject" @click="handleReject">
              拒绝
            </button>
            <button class="btn btn-approve" @click="handleApprove">
              批准并执行
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { ApprovalRequest } from '@/services/permission-manager'

interface Props {
  request: ApprovalRequest
  visible: boolean
  timeout?: number
}

interface Emits {
  (e: 'approve'): void
  (e: 'reject'): void
  (e: 'timeout'): void
}

const props = withDefaults(defineProps<Props>(), {
  timeout: 30000
})

const emit = defineEmits<Emits>()

const remainingSeconds = ref(Math.floor(props.timeout / 1000))
const elapsedTime = ref(0)
let intervalId: number | null = null

/**
 * 进度百分比
 */
const progressPercent = computed(() => {
  return Math.max(0, 100 - (elapsedTime.value / props.timeout) * 100)
})

/**
 * 获取权限标签
 */
const getPermissionLabel = (level: string): string => {
  const labels: Record<string, string> = {
    basic: '基础层',
    operation: '操作层',
    system: '系统层'
  }
  return labels[level] || level
}

/**
 * 获取范围标签
 */
const getScopeLabel = (scope: string): string => {
  const labels: Record<string, string> = {
    read: '读取',
    write: '写入',
    execute: '执行'
  }
  return labels[scope] || scope
}

/**
 * 格式化时间
 */
const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN')
}

/**
 * 处理批准
 */
const handleApprove = () => {
  emit('approve')
  stopTimer()
}

/**
 * 处理拒绝
 */
const handleReject = () => {
  emit('reject')
  stopTimer()
}

/**
 * 处理遮罩点击
 */
const handleOverlayClick = () => {
  // 点击遮罩不关闭，强制用户做出选择
}

/**
 * 启动倒计时
 */
const startTimer = () => {
  intervalId = window.setInterval(() => {
    elapsedTime.value += 1000
    remainingSeconds.value = Math.ceil((props.timeout - elapsedTime.value) / 1000)

    if (elapsedTime.value >= props.timeout) {
      emit('timeout')
      stopTimer()
    }
  }, 1000)
}

/**
 * 停止倒计时
 */
const stopTimer = () => {
  if (intervalId !== null) {
    clearInterval(intervalId)
    intervalId = null
  }
}

onMounted(() => {
  if (props.visible) {
    startTimer()
  }
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.approval-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.approval-dialog {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon {
  font-size: 1.8rem;
}

.header-title h3 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 600;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #fff;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 1.2rem;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.dialog-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.request-info {
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.info-label {
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 500;
  min-width: 80px;
}

.info-value {
  color: #1e293b;
  font-size: 0.9rem;
}

.permission-badge,
.scope-badge {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
}

.permission-badge.basic {
  background: #dbeafe;
  color: #1e40af;
}

.permission-badge.operation {
  background: #fef3c7;
  color: #92400e;
}

.permission-badge.system {
  background: #fee2e2;
  color: #991b1b;
}

.scope-badge.read {
  background: #dcfce7;
  color: #166534;
}

.scope-badge.write {
  background: #fef3c7;
  color: #92400e;
}

.scope-badge.execute {
  background: #fee2e2;
  color: #991b1b;
}

.target-path {
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 0.85rem;
  color: #475569;
}

.warning-box {
  background: #fef3c7;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.warning-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.warning-icon {
  font-size: 1.2rem;
}

.warning-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #92400e;
}

.warning-list {
  margin: 0;
  padding-left: 24px;
  color: #78350f;
  font-size: 0.85rem;
  line-height: 1.6;
}

.warning-list li {
  margin-bottom: 6px;
}

.timeout-indicator {
  margin-top: 20px;
}

.timeout-label {
  font-size: 0.9rem;
  color: #64748b;
  margin-bottom: 8px;
  text-align: center;
  font-weight: 500;
}

.timeout-progress {
  width: 100%;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  transition: width 1s linear;
}

.dialog-footer {
  padding: 20px 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-reject {
  background: #f1f5f9;
  color: #64748b;
}

.btn-reject:hover {
  background: #e2e8f0;
  color: #475569;
}

.btn-approve {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-approve:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-enter-active .approval-dialog,
.fade-leave-active .approval-dialog {
  transition: transform 0.3s ease;
}

.fade-enter-from .approval-dialog,
.fade-leave-to .approval-dialog {
  transform: scale(0.9);
}
</style>

