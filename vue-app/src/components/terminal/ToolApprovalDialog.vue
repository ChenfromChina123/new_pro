<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="approval-overlay" @click="$emit('close')">
        <div class="approval-dialog" @click.stop>
          <div class="dialog-header">
            <div class="header-title">
              <span class="icon">🛡️</span>
              <h3>工具调用审批</h3>
            </div>
            <div class="header-actions">
               <span class="badge">{{ pendingApprovals.length }} 待处理</span>
               <button class="close-btn" @click="$emit('close')">✕</button>
            </div>
          </div>

          <div class="dialog-body">
            <div v-if="pendingApprovals.length === 0" class="empty-state">
              🎉 没有待审批的请求
            </div>

            <div v-else class="approval-list">
              <div 
                v-for="req in pendingApprovals" 
                :key="req.id" 
                class="approval-item"
              >
                <div class="req-header">
                  <span class="tool-name">{{ req.toolName }}</span>
                  <span class="req-time">{{ formatTime(req.timestamp) }}</span>
                </div>
                
                <div class="req-content">
                  <div class="params-box">
                    <pre>{{ formatParams(req.parameters) }}</pre>
                  </div>
                  <div v-if="req.reason" class="req-reason">
                    <strong>理由:</strong> {{ req.reason }}
                  </div>
                </div>

                <div class="req-footer">
                  <div class="reject-input-group">
                      <input 
                        v-model="rejectReasons[req.id]" 
                        placeholder="拒绝理由 (可选)..."
                        class="reason-input"
                      />
                      <button 
                        class="btn btn-reject" 
                        @click="handleReject(req.id)"
                      >
                        拒绝
                      </button>
                  </div>
                  <button 
                    class="btn btn-approve" 
                    @click="handleApprove(req.id)"
                  >
                    批准
                  </button>
                </div>
              </div>
            </div>
          </div>
          
          <div v-if="pendingApprovals.length > 0" class="dialog-footer">
             <button class="btn btn-reject-all" @click="$emit('reject-all')">全部拒绝</button>
             <button class="btn btn-approve-all" @click="$emit('approve-all')">全部批准</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits } from 'vue'

const props = defineProps({
  visible: Boolean,
  pendingApprovals: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'approve', 'reject', 'approve-all', 'reject-all'])

const rejectReasons = ref({})

const formatTime = (ts) => {
    if (!ts) return ''
    return new Date(ts).toLocaleTimeString()
}

const formatParams = (params) => {
    try {
        if (typeof params === 'string') return params
        return JSON.stringify(params, null, 2)
    } catch (e) {
        return params
    }
}

const handleApprove = (id) => {
    emit('approve', { id, reason: '' })
}

const handleReject = (id) => {
    emit('reject', { id, reason: rejectReasons.value[id] || 'User rejected' })
    delete rejectReasons.value[id]
}
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
  height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-header {
  padding: 20px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title h3 {
  margin: 0;
  font-size: 1.2rem;
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
}

.badge {
    background: rgba(255,255,255,0.2);
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 0.8rem;
}

.close-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: #fff;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  cursor: pointer;
}

.dialog-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f3f4f6;
}

.empty-state {
    text-align: center;
    padding: 40px;
    color: #6b7280;
    font-size: 1.1rem;
}

.approval-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.approval-item {
    background: white;
    border-radius: 12px;
    padding: 16px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    border: 1px solid #e5e7eb;
}

.req-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f3f4f6;
}

.tool-name {
    font-weight: 700;
    color: #4f46e5;
    font-family: monospace;
    font-size: 1.1rem;
}

.req-time {
    color: #9ca3af;
    font-size: 0.85rem;
}

.params-box {
    background: #1e293b;
    color: #e2e8f0;
    padding: 12px;
    border-radius: 8px;
    font-family: monospace;
    font-size: 0.9rem;
    overflow-x: auto;
    max-height: 200px;
}

.req-reason {
    margin-top: 8px;
    color: #4b5563;
    font-size: 0.95rem;
    padding: 8px;
    background: #fffbeb;
    border-left: 3px solid #f59e0b;
    border-radius: 4px;
}

.req-footer {
    margin-top: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 10px;
}

.reject-input-group {
    display: flex;
    gap: 8px;
    flex: 1;
}

.reason-input {
    flex: 1;
    padding: 8px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.9rem;
}

.btn {
    padding: 8px 16px;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    transition: all 0.2s;
}

.btn-reject {
    background: #fee2e2;
    color: #ef4444;
}

.btn-reject:hover {
    background: #fecaca;
}

.btn-approve {
    background: #4f46e5;
    color: white;
}

.btn-approve:hover {
    background: #4338ca;
}

.dialog-footer {
    padding: 16px 20px;
    background: white;
    border-top: 1px solid #e5e7eb;
    display: flex;
    justify-content: space-between;
}

.btn-reject-all {
    background: #f3f4f6;
    color: #6b7280;
}

.btn-approve-all {
    background: #dbeafe;
    color: #1e40af;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>