# 前端解耦架构适配指南

## 一、当前状态分析

前端代码已经基本符合解耦架构的要求：

1. ✅ **决策信封处理** - `TerminalView.vue` 正确解析 `DecisionEnvelope`
2. ✅ **工具执行反馈** - 执行工具后返回 `ToolResult` 给后端
3. ✅ **去重机制** - 使用 `decision_id` 防止重复执行
4. ✅ **状态管理** - 通过 `terminalStore` 管理 Agent 状态

## 二、前端适配建议

### 1. 增强工具执行策略检查

当前前端直接执行工具，建议添加策略检查：

```javascript
// 在 processAgentLoop 中，执行工具前
if (decision.type === 'TOOL_CALL') {
    // 检查工具是否在白名单中
    const allowedActions = ['execute_command', 'read_file', 'write_file', 'ensure_file']
    if (!allowedActions.includes(decision.action)) {
        console.warn('Tool action not allowed:', decision.action)
        result.exit_code = -1
        result.stderr = `Action ${decision.action} is not allowed`
        // 仍然发送结果，让后端处理
        await processAgentLoop("tool_result_feedback", result)
        return
    }
    
    // 继续执行...
}
```

### 2. 增强信息管理

前端可以更好地管理可见范围：

```javascript
// 在 terminal.js store 中
const visibleFiles = ref([])
const visibleFunctions = ref([])

const setScope = (files, functions) => {
    visibleFiles.value = files || []
    visibleFunctions.value = functions || []
}

// 在发送请求时，可以包含作用域信息
const body = {
    prompt: prompt,
    session_id: currentSessionId.value,
    model: currentModel.value,
    tool_result: toolResult,
    scope: {
        visible_files: visibleFiles.value,
        visible_functions: visibleFunctions.value
    }
}
```

### 3. 身份信息可视化

可以在 UI 中显示当前的身份信息：

```vue
<template>
  <div class="identity-info" v-if="identityInfo">
    <div class="core-identity">
      <span>身份: {{ identityInfo.core.type }}</span>
      <span>权限: {{ identityInfo.core.authority }}</span>
    </div>
    <div class="task-identity" v-if="identityInfo.task.id">
      <span>任务: {{ identityInfo.task.goal }}</span>
      <span>角色: {{ identityInfo.task.role }}</span>
    </div>
    <div class="viewpoint-identity" v-if="identityInfo.viewpoint.file">
      <span>文件: {{ identityInfo.viewpoint.file }}</span>
      <span>符号: {{ identityInfo.viewpoint.symbol }}</span>
    </div>
  </div>
</template>

<script>
// 从后端响应中提取身份信息
const identityInfo = ref(null)

// 在 processAgentLoop 中
const response = await fetch(...)
// 如果后端返回身份信息，保存它
if (data.identity) {
    identityInfo.value = data.identity
}
</script>
```

### 4. 状态切片可视化

可以显示当前的状态切片信息：

```javascript
// 在 terminal.js store 中
const stateSlices = ref([])

const addStateSlice = (slice) => {
    stateSlices.value.push({
        ...slice,
        timestamp: new Date()
    })
}

// 在 UI 中显示
const displayStateSlices = computed(() => {
    return stateSlices.value.map(slice => ({
        source: slice.source,
        scope: slice.scope,
        dataCount: Object.keys(slice.data || {}).length,
        timestamp: slice.timestamp
    }))
})
```

## 三、完整的前端解耦架构实现示例

### 更新的 TerminalView.vue（关键部分）

```javascript
const processAgentLoop = async (prompt, toolResult) => {
  try {
    const body = {
        prompt: prompt,
        session_id: currentSessionId.value,
        model: currentModel.value,
        tool_result: toolResult,
        // 新增：作用域信息
        scope: {
            visible_files: terminalStore.visibleFiles,
            visible_functions: terminalStore.visibleFunctions
        }
    }
    
    const response = await fetch(`${API_CONFIG.baseURL}/api/terminal/chat-stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`,
        'Accept': 'text/event-stream, application/json'
      },
      body: JSON.stringify(body)
    })

    // ... 处理响应 ...

    // 解析 Decision Envelope
    const decision = JSON.parse(jsonStr)

    // 1. 去重检查（已有）
    if (decision.decision_id && terminalStore.hasDecision(decision.decision_id)) {
        console.warn('Duplicate decision ignored:', decision.decision_id)
        return
    }
    if (decision.decision_id) {
        terminalStore.addDecision(decision.decision_id)
    }

    // 2. 工具执行策略检查（新增）
    if (decision.type === 'TOOL_CALL') {
        const allowedActions = ['execute_command', 'read_file', 'write_file', 'ensure_file']
        if (!allowedActions.includes(decision.action)) {
            console.warn('Tool action not in whitelist:', decision.action)
            const result = {
                decision_id: decision.decision_id,
                exit_code: -1,
                stderr: `Action ${decision.action} is not allowed`,
                artifacts: []
            }
            await processAgentLoop("tool_result_feedback", result)
            return
        }

        // 继续执行工具...
    }

    // 3. 保存身份信息（如果后端返回）
    if (decision.identity) {
        terminalStore.setIdentity(decision.identity)
    }

    // 4. 保存状态切片（如果后端返回）
    if (decision.state_slices) {
        decision.state_slices.forEach(slice => {
            terminalStore.addStateSlice(slice)
        })
    }
  } catch (e) {
    // 错误处理...
  }
}
```

### 更新的 terminal.js store

```javascript
export const useTerminalStore = defineStore('terminal', () => {
  // ... 现有状态 ...

  // 新增：身份信息
  const identityInfo = ref(null)
  const setIdentity = (identity) => {
    identityInfo.value = identity
  }

  // 新增：状态切片
  const stateSlices = ref([])
  const addStateSlice = (slice) => {
    stateSlices.value.push({
      ...slice,
      timestamp: new Date()
    })
  }

  // 新增：作用域
  const visibleFiles = ref([])
  const visibleFunctions = ref([])
  const setScope = (files, functions) => {
    visibleFiles.value = files || []
    visibleFunctions.value = functions || []
  }

  return {
    // ... 现有返回值 ...
    identityInfo,
    setIdentity,
    stateSlices,
    addStateSlice,
    visibleFiles,
    visibleFunctions,
    setScope
  }
})
```

## 四、UI 增强建议

### 1. 身份信息显示组件

```vue
<template>
  <div class="agent-identity-panel">
    <h4>Agent 身份</h4>
    <div class="identity-section">
      <div class="core">
        <strong>核心身份:</strong>
        <span>{{ identityInfo?.core?.type }}</span>
        <span class="badge">{{ identityInfo?.core?.authority }}</span>
      </div>
      <div class="task" v-if="identityInfo?.task?.id">
        <strong>当前任务:</strong>
        <span>{{ identityInfo?.task?.goal }}</span>
        <span class="role">{{ identityInfo?.task?.role }}</span>
      </div>
      <div class="viewpoint" v-if="identityInfo?.viewpoint?.file">
        <strong>视角:</strong>
        <span>{{ identityInfo?.viewpoint?.file }}</span>
        <span v-if="identityInfo?.viewpoint?.line">:{{ identityInfo?.viewpoint?.line }}</span>
      </div>
    </div>
  </div>
</template>
```

### 2. 状态切片可视化

```vue
<template>
  <div class="state-slices-panel">
    <h4>状态切片</h4>
    <div class="slices-list">
      <div 
        v-for="(slice, index) in stateSlices" 
        :key="index"
        class="slice-item"
      >
        <span class="source">{{ slice.source }}</span>
        <span class="scope">{{ slice.scope.length }} 项</span>
        <span class="timestamp">{{ formatTime(slice.timestamp) }}</span>
      </div>
    </div>
  </div>
</template>
```

## 五、总结

前端已经基本符合解耦架构的要求，主要改进点：

1. ✅ **工具执行策略** - 添加白名单检查
2. ✅ **信息管理** - 支持作用域设置
3. ✅ **身份可视化** - 显示当前身份信息
4. ✅ **状态切片** - 可视化状态切片

这些改进都是可选的增强功能，不会破坏现有功能。可以根据需要逐步添加。

