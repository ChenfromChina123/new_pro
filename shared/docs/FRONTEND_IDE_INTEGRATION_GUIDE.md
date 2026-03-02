# 前端 AI 终端 IDE 级别集成指南

## 📋 概述

本指南说明如何将新创建的 IDE 级别组件集成到 `TerminalView.vue` 中，实现真正像 IDE 一样工作的 AI 终端。

## 🎯 核心组件

### 1. AgentLoopManager (服务层)
**文件**: `vue-app/src/services/agentLoopManager.js`

**功能**:
- Agent 循环生命周期管理
- 决策流程状态追踪
- 工具调用批准/拒绝机制
- 循环中断和恢复
- 自动检查点创建

**使用方法**:
```javascript
import { createAgentLoopManager } from '@/services/agentLoopManager'

// 在组件中创建实例
const agentLoopManager = ref(null)

onMounted(() => {
  agentLoopManager.value = createAgentLoopManager(currentSessionId.value)
})

// 启动循环
await agentLoopManager.value.startLoop(userPrompt, 'deepseek-chat')

// 处理决策
const result = await agentLoopManager.value.processDecision(decision)

// 批准工具
await agentLoopManager.value.approveTool(decisionId, '用户批准')

// 中断循环
await agentLoopManager.value.interrupt()
```

### 2. CheckpointTimeline (UI 组件)
**文件**: `vue-app/src/components/terminal/CheckpointTimeline.vue`

**功能**:
- 时间线式展示所有检查点
- 检查点对比和跳转
- 支持导出/导入
- 自动和手动检查点管理

**Props**:
```javascript
{
  checkpoints: Array,           // 检查点列表
  currentCheckpointId: Number,  // 当前检查点 ID
  currentState: Object          // 当前状态（用于对比）
}
```

**Events**:
```javascript
@create      // 创建检查点
@jump        // 跳转到检查点
@delete      // 删除检查点
@export      // 导出检查点
@compare     // 对比检查点
```

**集成示例**:
```vue
<CheckpointTimeline
  :checkpoints="checkpoints"
  :current-checkpoint-id="currentCheckpointId"
  :current-state="{ messageCount: messages.length, taskCount: tasks.length }"
  @create="handleCreateCheckpoint"
  @jump="jumpToCheckpoint"
  @delete="handleDeleteCheckpoint"
  @export="handleExportCheckpoint"
/>
```

### 3. ToolApprovalManager (UI 组件)
**文件**: `vue-app/src/components/terminal/ToolApprovalManager.vue`

**功能**:
- 危险等级分类（低/中/高/极高）
- 自动批准规则配置
- 批准策略（严格/平衡/宽松）
- 批准历史审计

**Props**:
```javascript
{
  pendingApprovals: Array,    // 待批准列表
  approvalHistory: Array,     // 批准历史
  autoApprovalRules: Object   // 自动批准规则
}
```

**Events**:
```javascript
@approve         // 批准工具
@reject          // 拒绝工具
@approve-all     // 批量批准
@reject-all      // 批量拒绝
@update-rules    // 更新规则
@clear-history   // 清空历史
```

**集成示例**:
```vue
<ToolApprovalManager
  :pending-approvals="pendingApprovals"
  :approval-history="approvalHistory"
  :auto-approval-rules="autoApprovalRules"
  @approve="approveTool"
  @reject="rejectTool"
  @approve-all="approveAll"
  @reject-all="rejectAll"
  @update-rules="updateAutoApprovalRules"
/>
```

### 4. SessionStatePanel (UI 组件)
**文件**: `vue-app/src/components/terminal/SessionStatePanel.vue`

**功能**:
- Agent 状态可视化
- 任务进度追踪
- 流式状态监控
- 性能指标统计
- 决策历史时间线

**Props**:
```javascript
{
  sessionState: Object,           // 会话状态
  agentStatus: String,            // Agent 状态
  tasks: Array,                   // 任务列表
  decisionHistory: Array,         // 决策历史
  isStreaming: Boolean,           // 是否流式中
  streamType: String,             // 流式类型
  streamBytesReceived: Number,    // 已接收字节数
  streamBufferSize: Number,       // 缓冲区大小
  messageCount: Number,           // 消息数
  toolCallCount: Number,          // 工具调用数
  checkpointCount: Number,        // 检查点数
  pendingApprovalCount: Number,   // 待批准数
  avgResponseTime: Number,        // 平均响应时间
  avgToolExecutionTime: Number,   // 平均工具执行时间
  llmCallCount: Number,           // LLM 调用次数
  totalTokens: Number             // Token 总数
}
```

**Events**:
```javascript
@refresh  // 刷新状态
@export   // 导出状态
```

**集成示例**:
```vue
<SessionStatePanel
  :session-state="sessionState"
  :agent-status="agentStatus"
  :tasks="currentTasks"
  :decision-history="decisionHistory"
  :is-streaming="isStreaming"
  :message-count="messages.length"
  :tool-call-count="toolCallCount"
  :checkpoint-count="checkpoints.length"
  :pending-approval-count="pendingApprovals.length"
  @refresh="loadSessionState"
  @export="exportSessionState"
/>
```

### 5. AgentDecisionFlow (UI 组件)
**文件**: `vue-app/src/components/terminal/AgentDecisionFlow.vue`

**功能**:
- 可视化展示决策流程
- 自动滚动到最新决策
- 展开/收起决策详情
- 显示工具执行结果
- 身份信息和状态切片展示

**Props**:
```javascript
{
  decisions: Array  // 决策列表
}
```

**Events**:
```javascript
@clear  // 清空决策历史
```

**集成示例**:
```vue
<AgentDecisionFlow
  :decisions="decisionHistory"
  @clear="clearDecisionHistory"
/>
```

## 🔧 集成步骤

### 步骤 1: 更新 TerminalView.vue 的 script 部分

```vue
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { createAgentLoopManager } from '@/services/agentLoopManager'
import CheckpointTimeline from '@/components/terminal/CheckpointTimeline.vue'
import ToolApprovalManager from '@/components/terminal/ToolApprovalManager.vue'
import SessionStatePanel from '@/components/terminal/SessionStatePanel.vue'
import AgentDecisionFlow from '@/components/terminal/AgentDecisionFlow.vue'

// 现有的导入...

// 创建 Agent 循环管理器
const agentLoopManager = ref(null)

// 状态
const checkpoints = ref([])
const pendingApprovals = ref([])
const approvalHistory = ref([])
const autoApprovalRules = ref({
  read_file: true,
  search_files: true,
  execute_command: false,
  write_file: false,
  modify_file: false,
  delete_file: false
})
const sessionState = ref(null)
const toolCallCount = ref(0)
const isStreaming = ref(false)

// 初始化
onMounted(async () => {
  // 创建 Agent 循环管理器
  if (currentSessionId.value) {
    agentLoopManager.value = createAgentLoopManager(currentSessionId.value)
    
    // 加载初始数据
    await loadCheckpoints()
    await loadPendingApprovals()
    await loadSessionState()
  }
})

// 监听会话变化
watch(currentSessionId, async (newSessionId) => {
  if (newSessionId) {
    agentLoopManager.value = createAgentLoopManager(newSessionId)
    await loadCheckpoints()
    await loadPendingApprovals()
    await loadSessionState()
  }
})

// 加载检查点
async function loadCheckpoints() {
  if (agentLoopManager.value) {
    checkpoints.value = await agentLoopManager.value.loadCheckpoints()
  }
}

// 加载待批准列表
async function loadPendingApprovals() {
  if (agentLoopManager.value) {
    pendingApprovals.value = await agentLoopManager.value.loadPendingApprovals()
  }
}

// 加载会话状态
async function loadSessionState() {
  if (!currentSessionId.value) return
  try {
    const result = await sessionStateService.getSessionState(currentSessionId.value)
    if (result?.data) {
      sessionState.value = result.data
    }
  } catch (error) {
    console.error('加载会话状态失败:', error)
  }
}

// 处理用户消息
async function sendMessage(overrideText) {
  const text = overrideText || inputMessage.value.trim()
  if (!text) return
  
  // 启动 Agent 循环
  if (agentLoopManager.value) {
    await agentLoopManager.value.startLoop(text, currentModel.value)
  }
  
  // 调用现有的 processAgentLoop
  await processAgentLoop(text, null)
}

// 修改 processAgentLoop 以使用 AgentLoopManager
async function processAgentLoop(prompt, toolResult) {
  // ... 现有代码 ...
  
  // 在解析决策后，使用 AgentLoopManager 处理
  if (decision && agentLoopManager.value) {
    const result = await agentLoopManager.value.processDecision(decision)
    
    if (result.action === 'WAIT_APPROVAL') {
      // 等待批准
      await loadPendingApprovals()
      return
    } else if (result.action === 'EXECUTE') {
      // 继续执行工具
      // ... 现有的工具执行代码 ...
    }
  }
  
  // ... 现有代码 ...
}

// 批准工具
async function approveTool(payload) {
  if (agentLoopManager.value) {
    await agentLoopManager.value.approveTool(payload.id, payload.reason)
    await loadPendingApprovals()
    
    // 继续执行 Agent 循环
    await processAgentLoop('', null)
  }
}

// 拒绝工具
async function rejectTool(payload) {
  if (agentLoopManager.value) {
    await agentLoopManager.value.rejectTool(payload.id, payload.reason)
    await loadPendingApprovals()
  }
}

// 创建检查点
async function handleCreateCheckpoint(description) {
  if (agentLoopManager.value) {
    await agentLoopManager.value.createCheckpoint('MANUAL', description)
    await loadCheckpoints()
  }
}

// 跳转到检查点
async function jumpToCheckpoint(checkpointId) {
  if (agentLoopManager.value) {
    await agentLoopManager.value.jumpToCheckpoint(checkpointId)
    // 刷新界面
    await terminalStore.fetchSessions()
    await terminalStore.selectSession(currentSessionId.value)
  }
}

// 更新自动批准规则
function updateAutoApprovalRules(payload) {
  if (typeof payload === 'object' && !payload.toolName) {
    // 批量更新
    autoApprovalRules.value = { ...autoApprovalRules.value, ...payload }
  } else {
    // 单个更新
    autoApprovalRules.value[payload.toolName] = payload.enabled
  }
  
  // 同步到 AgentLoopManager
  if (agentLoopManager.value) {
    Object.entries(autoApprovalRules.value).forEach(([toolName, enabled]) => {
      agentLoopManager.value.updateAutoApprovalRule(toolName, enabled)
    })
  }
}

// 中断 Agent 循环
async function handleStop() {
  if (agentLoopManager.value) {
    await agentLoopManager.value.interrupt()
  }
}
</script>
```

### 步骤 2: 更新 TerminalView.vue 的 template 部分

在右侧面板添加新的标签页：

```vue
<template>
  <div class="terminal-container">
    <!-- ... 现有代码 ... -->
    
    <div class="right-panel" :class="{ collapsed: rightPanelCollapsed }">
      <div class="panel-tabs">
        <div class="tab" :class="{ active: activeTab === 'terminal' }" @click="activeTab = 'terminal'">
          终端输出
        </div>
        <div class="tab" :class="{ active: activeTab === 'files' }" @click="activeTab = 'files'">
          文件管理
        </div>
        <div class="tab" :class="{ active: activeTab === 'checkpoints' }" @click="activeTab = 'checkpoints'">
          检查点
        </div>
        <div class="tab" :class="{ active: activeTab === 'approvals' }" @click="activeTab = 'approvals'">
          工具批准
          <span v-if="pendingApprovals.length > 0" class="tab-badge">{{ pendingApprovals.length }}</span>
        </div>
        <div class="tab" :class="{ active: activeTab === 'state' }" @click="activeTab = 'state'">
          会话状态
        </div>
        <div class="tab" :class="{ active: activeTab === 'decisions' }" @click="activeTab = 'decisions'">
          决策流程
        </div>
      </div>

      <!-- 检查点面板 -->
      <div v-if="activeTab === 'checkpoints'" class="panel-content">
        <CheckpointTimeline
          :checkpoints="checkpoints"
          :current-checkpoint-id="currentCheckpointId"
          :current-state="{ messageCount: messages.length, taskCount: currentTasks.length }"
          @create="handleCreateCheckpoint"
          @jump="jumpToCheckpoint"
          @delete="handleDeleteCheckpoint"
          @export="handleExportCheckpoint"
        />
      </div>

      <!-- 工具批准面板 -->
      <div v-if="activeTab === 'approvals'" class="panel-content">
        <ToolApprovalManager
          :pending-approvals="pendingApprovals"
          :approval-history="approvalHistory"
          :auto-approval-rules="autoApprovalRules"
          @approve="approveTool"
          @reject="rejectTool"
          @approve-all="approveAll"
          @reject-all="rejectAll"
          @update-rules="updateAutoApprovalRules"
          @clear-history="clearApprovalHistory"
        />
      </div>

      <!-- 会话状态面板 -->
      <div v-if="activeTab === 'state'" class="panel-content">
        <SessionStatePanel
          :session-state="sessionState"
          :agent-status="agentStatus"
          :tasks="currentTasks"
          :decision-history="decisionHistory"
          :is-streaming="isStreaming"
          :message-count="messages.length"
          :tool-call-count="toolCallCount"
          :checkpoint-count="checkpoints.length"
          :pending-approval-count="pendingApprovals.length"
          @refresh="loadSessionState"
          @export="exportSessionState"
        />
      </div>

      <!-- 决策流程面板 -->
      <div v-if="activeTab === 'decisions'" class="panel-content">
        <AgentDecisionFlow
          :decisions="decisionHistory"
          @clear="clearDecisionHistory"
        />
      </div>

      <!-- ... 现有的其他面板 ... -->
    </div>
  </div>
</template>
```

### 步骤 3: 添加样式

```vue
<style scoped>
/* ... 现有样式 ... */

.tab-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 2px 6px;
  background: #ef4444;
  color: #fff;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
}

.panel-content {
  height: 100%;
  overflow: hidden;
}
</style>
```

## 📊 数据流

```
用户输入
  ↓
AgentLoopManager.startLoop()
  ↓
processAgentLoop() → LLM 调用
  ↓
AgentLoopManager.processDecision()
  ↓
┌─────────────┬──────────────┬────────────────┐
│             │              │                │
TASK_LIST   TOOL_CALL   TASK_COMPLETE    PAUSE/ERROR
│             │              │                │
更新任务列表  检查批准规则   标记完成         暂停/错误处理
              │
        需要批准? ──Yes→ ToolApprovalManager
              │              ↓
              No         用户批准/拒绝
              │              ↓
        执行工具        AgentLoopManager.approveTool()
              │              ↓
        返回结果        继续执行
              ↓
        processAgentLoop(result)
              ↓
        继续循环...
```

## 🎯 最佳实践

### 1. 检查点策略
- 在用户发送消息前自动创建检查点
- 在执行危险操作前自动创建检查点
- 允许用户手动创建检查点
- 定期清理过期检查点

### 2. 批准策略
- 默认使用"平衡模式"
- 对于新用户，使用"严格模式"
- 对于高级用户，可以使用"宽松模式"
- 记录所有批准/拒绝操作用于审计

### 3. 性能优化
- 使用虚拟滚动展示大量决策
- 懒加载检查点文件快照
- 缓存会话状态，减少 API 调用
- 使用 WebSocket 实时更新状态

### 4. 错误处理
- 捕获所有 API 调用错误
- 提供友好的错误提示
- 支持重试机制
- 记录错误日志用于调试

## 🧪 测试清单

- [ ] Agent 循环启动和停止
- [ ] 决策流程正确展示
- [ ] 工具批准/拒绝功能
- [ ] 检查点创建和跳转
- [ ] 会话状态实时更新
- [ ] 自动批准规则生效
- [ ] 中断和恢复功能
- [ ] 性能指标统计准确
- [ ] 导出/导入功能
- [ ] 错误处理和恢复

## 📚 参考文档

- [Void-Main AI 机制解析](./VOID_MAIN_AI_MECHANISM_ANALYSIS.md)
- [AISpring 重构指南](./AISPRING_AI_TERMINAL_REFACTOR_GUIDE.md)
- [重构进度文档](./REFACTOR_PROGRESS.md)

## 🔄 后续改进

1. **实时协作**: 支持多用户同时查看同一会话
2. **AI 建议**: 基于历史数据提供批准建议
3. **性能分析**: 更详细的性能指标和瓶颈分析
4. **自定义工作流**: 允许用户自定义 Agent 循环流程
5. **插件系统**: 支持第三方工具和扩展

---

**创建日期**: 2025-12-24  
**版本**: 1.0.0  
**作者**: AI Assistant

