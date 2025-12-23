# 修复 AI 工具调用问题

## 🔍 问题分析

从图片看，AI 只是在展示步骤说明和代码，但没有输出 JSON 格式的决策信封，导致前端无法识别工具调用。

## ✅ 已完成的修复

### 1. 增强 Prompt 约束

**文件**: `aispring/src/main/java/com/aispring/service/TerminalPromptManager.java`

**关键改进**:
- ✅ 更强制要求只输出 JSON，不要任何其他文字
- ✅ 明确说明不要使用 Markdown 代码块
- ✅ 提供完整的 JSON 示例（一行格式）
- ✅ 多次强调输出格式要求

### 2. 改进 JSON 提取逻辑

**后端**: `aispring/src/main/java/com/aispring/controller/TerminalController.java`
- ✅ 多种方式提取 JSON（代码块、对象、数组）
- ✅ 更好的错误处理和日志

**前端**: `vue-app/src/views/TerminalView.vue`
- ✅ 改进 JSON 解析逻辑
- ✅ 更好的错误处理和提示
- ✅ 尝试修复常见的 JSON 格式问题

### 3. 任务规划后自动执行

**改进**: 规划完成后，自动设置第一个任务为 IN_PROGRESS，并标记为 RUNNING 状态

## 🔧 还需要检查的点

### 1. 检查 AI 模型响应

如果 AI 仍然不输出 JSON，可能需要：
- 检查模型配置
- 调整 temperature（降低以获得更确定的输出）
- 使用更明确的 few-shot 示例

### 2. 添加调试日志

在关键位置添加日志，帮助诊断：
- AI 的原始响应
- JSON 提取结果
- 解析错误信息

### 3. 前端错误提示

如果无法解析 JSON，前端应该：
- 显示错误信息
- 提示用户重试
- 显示 AI 的原始响应

## 🚀 测试步骤

1. **重启后端服务**（应用新的 Prompt）
2. **清除浏览器缓存**（确保使用新的前端代码）
3. **测试工具调用**：
   - 输入："请做一个简单网页"
   - 观察是否输出 JSON 决策信封
   - 检查终端是否有输出

## 📝 如果问题仍然存在

### 方案 1: 使用更严格的 Prompt

在 Prompt 开头添加：
```
你是一个 JSON 输出机器。你只能输出 JSON，不能输出任何其他文字。
你的响应必须是一个有效的 JSON 对象，以 { 开头，以 } 结尾。
```

### 方案 2: 后处理 AI 响应

如果 AI 输出了说明文字 + JSON，可以：
- 使用正则表达式提取 JSON
- 使用 LLM 再次提取 JSON（如果第一次失败）

### 方案 3: 使用 Function Calling

如果模型支持 function calling，可以：
- 定义工具为 function
- 让模型直接调用 function
- 不需要 JSON 解析

## 🔍 调试建议

1. **查看后端日志**：检查 AI 的原始响应
2. **查看前端控制台**：检查 JSON 解析错误
3. **检查网络请求**：查看 SSE 流的内容
4. **测试简单场景**：先用简单的工具调用测试

## 📊 预期行为

修复后，AI 应该：
1. ✅ 直接输出 JSON 对象（不带任何说明文字）
2. ✅ 包含正确的字段（decision_id, type, action, params）
3. ✅ 前端能正确解析并执行工具
4. ✅ 终端显示工具执行结果

