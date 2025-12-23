# 自动循环问题修复总结

## 🐛 问题描述

用户反馈：AI 执行命令后没有自动继续，系统反复显示"Agent 正在运行中"的提示，但没有后续响应。

## 🔍 根本原因

1. **字段名不匹配**: 前端发送 `decision_id`/`exit_code`，但代码中使用了 `decisionId`/`exitCode`
2. **后端全局 Jackson 配置**: 使用 `SNAKE_CASE`，所有字段都会转换为下划线命名
3. **输入守卫逻辑**: 在有 tool_result 时仍然可能被误拦截

## ✅ 修复方案

### 1. 统一字段命名
- **前端**: 所有 `result` 对象统一使用 `decision_id`, `exit_code`（下划线）
- **后端**: `ToolResult` 类字段保持驼峰，JSON 序列化自动转换为下划线（受全局配置影响）

### 2. 修复输入守卫逻辑
```java
// 修复前
if (state.getStatus() == RUNNING && request.getTool_result() == null) {
    return "Agent 正在运行中..."
}

// 修复后
if ((state.getStatus() == RUNNING || state.getStatus() == WAITING_TOOL) 
    && request.getTool_result() == null) {
    // 只有在没有 tool_result 时才拦截
    if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
        return "Agent 正在运行中..."
    }
}
```

### 3. 移除 Prompt 验证
- 移除了 `@NotBlank` 验证，允许空 prompt（工具结果反馈时）

### 4. 完善自动循环逻辑
- 即时执行模式（EXECUTE）在工具执行后也会自动继续
- 不再依赖任务流水线的存在

## 📊 修复后的流程

```
用户输入: "查看目录"
  ↓
意图识别: EXECUTE
  ↓
AI 决策: {"type":"TOOL_CALL","action":"execute_command","params":{"command":"ls -la"}}
  ↓
系统生成 decision_id: "xxx-xxx-xxx"
  ↓
前端执行工具: executeCommand("ls -la")
  ↓
工具结果: {decision_id: "xxx", exit_code: 1, stderr: "错误信息"}
  ↓
前端发送: processAgentLoop("", result)  // ✅ tool_result 有值
  ↓
后端接收: 检测到 tool_result，跳过输入守卫 ✅
  ↓
后端处理: applyToolResult() → 自动构建继续执行的 prompt
  ↓
后端触发: askAgentStream() → 自动继续执行 ✅
  ↓
AI 分析错误: 根据错误信息修正命令
  ↓
循环继续...
```

## 🔧 关键修复点

### 前端修复
1. ✅ 统一使用 `decision_id`, `exit_code`（下划线命名）
2. ✅ 添加详细日志记录
3. ✅ 确保所有工具执行后都调用 `processAgentLoop("", result)`

### 后端修复
1. ✅ 修复输入守卫逻辑，有 tool_result 时允许通过
2. ✅ 移除 prompt 的 @NotBlank 验证
3. ✅ 即时执行模式自动循环逻辑
4. ✅ 详细日志记录所有关键步骤

## 📝 日志调试

### 前端日志
```javascript
console.log('[TerminalView] processAgentLoop called:', {...})
console.log('[TerminalView] Sending tool result back:', {...})
```

### 后端日志
```java
log.info("=== Terminal Chat Stream Request ===")
log.info("Tool Result - DecisionId: {}, ExitCode: {}", ...)
log.info("Processing tool result...")
log.info("Triggering next round for immediate execution")
```

## ✅ 验证步骤

1. **测试自动循环**:
   - 输入: "查看当前目录"
   - 预期: AI 执行命令 → 如果失败 → 自动分析 → 修正命令 → 重新执行

2. **检查日志**:
   - 前端控制台: 应该看到 `processAgentLoop called` 和 `Sending tool result back`
   - 后端日志: 应该看到 `Processing tool result` 和 `Triggering next round`

3. **验证字段名**:
   - 前端发送的 JSON: `{decision_id: "...", exit_code: 0}`
   - 后端接收的字段: 自动转换为 `decisionId`, `exitCode`

## 🎯 预期效果

修复后，系统应该：
- ✅ 命令执行后自动继续（无论成功失败）
- ✅ AI 能够分析错误并自动修正
- ✅ 不再出现"Agent 正在运行中"的阻塞
- ✅ 完整的日志记录便于调试

---

**修复时间**: 2025-12-23  
**状态**: ✅ 已完成并提交

