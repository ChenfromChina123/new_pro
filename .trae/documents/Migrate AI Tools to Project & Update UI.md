# 实施计划：AI模型迁移与界面更新（集成深度思考参数）

本计划将把 Python 脚本中的 AI 模型调用逻辑迁移到 Java 后端，并根据用户指示，使用 `thinking` 参数配置深度思考模式，同时更新前端界面。

## 1. 后端实现 (Spring Boot)

### 1.1 更新 `AiChatServiceImpl.java`
- **目标**：使用 `OkHttp` 实现自定义流式客户端，支持 DeepSeek 和 豆包 的推理模型，并适配 `thinking` 参数。
- **模型配置策略**：
    - **DeepSeek Reasoner**:
        - 模型 ID: `deepseek-reasoner`
    - **豆包 Reasoner**:
        - 模型 ID: `doubao-seed-1-6-251015` (参考 `test_doubao.py`)
        - **新增参数配置**：在请求体中添加 `thinking` 字段：
          ```json
          "thinking": {
              "type": "enabled"
          }
          ```
- **行动**：
    - 在 `AiChatServiceImpl` 中引入 `OkHttp` 客户端。
    - 重构 `askStream` 方法，针对 `deepseek-reasoner` 和 `doubao-reasoner` 走自定义 HTTP 请求逻辑。
    - 构建请求体时，若为豆包推理模型，显式加入 `thinking` 参数。
    - 解析流式响应，提取 `reasoning_content` 字段。
    - 封装 SSE 数据格式：`{"reasoning_content": "...", "content": "..."}`。

### 1.2 更新 `AiChatController.java`
- 保持接口不变，通过 `model` 参数区分是否为推理模型。

## 2. 前端实现 (Vue.js)

### 2.1 更新 `ChatView.vue`
- **目标**：更新模型选择器，支持推理模型选项，并展示推理过程。
- **行动**：
    - **模型选择器**：更新下拉菜单选项：
        - DeepSeek Chat (`deepseek-chat`)
        - DeepSeek Reasoner (`deepseek-reasoner`)
        - 豆包 (`doubao`)
        - 豆包-reasoner (`doubao-reasoner`)
    - **推理展示区域**：
        - 在消息气泡中新增“深度思考”区域组件。
        - 样式参考用户提供的截图：独立的灰色背景块，可折叠/展开，仅在有 `reasoning_content` 时显示。

### 2.2 更新 `chat.js`
- **目标**：解析后端返回的包含推理内容的 SSE 数据。
- **行动**：
    - 更新 `sendMessage` 中的 SSE 处理逻辑。
    - 累加 `reasoning_content` 并实时更新到消息对象中。

## 3. 验证
- **功能验证**：
    - 选择 "豆包-reasoner"，发送请求，后端应发送包含 `thinking: {type: "enabled"}` 的请求。
    - 验证前端是否显示“深度思考”折叠框，并实时打字显示推理内容。
    - 验证 DeepSeek Reasoner 同样工作正常。
