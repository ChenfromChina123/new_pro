# AI Agent 沙箱模块开发计划

## 一、项目背景

根据《2核1.8G极限环境AI智能体（Agent）完整技术架构方案》文档，开发一个新的 AI Agent 模块，实现类 Manus AI 电脑助手功能，支持自主决策、沙箱内文件操作与命令执行。

## 二、技术架构

### 2.1 整体架构（三层解耦）

```
┌─────────────────────────────────────────────────────────────┐
│                    Vue 3 前端（交互层）                       │
│  - Xterm.js 终端模拟器                                        │
│  - Monaco 编辑器                                              │
│  - 文件树渲染                                                  │
│  - SSE 流式消息接收                                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               Spring Boot 后端（资源管理层）                   │
│  - 用户鉴权                                                    │
│  - 数据持久化                                                  │
│  - 任务元数据存储                                              │
│  - 权限管控                                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               Bun + TS 核心（执行层）                          │
│  - LLM 任务编排                                               │
│  - 指令解析                                                    │
│  - Git 版本控制                                               │
│  - bwrap 沙箱调度                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型

| 层级  | 技术栈                       | 说明               |
| --- | ------------------------- | ---------------- |
| 前端  | Vue 3 + TypeScript + Vite | 复用现有 vue-app 项目  |
| 后端  | Spring Boot 3.3 + Java 17 | 复用现有 aispring 项目 |
| 执行层 | Bun + TypeScript          | 新增独立服务模块         |
| 沙箱  | Bubblewrap + cgroups      | Linux 环境隔离       |
| 数据库 | MySQL + Redis             | 复用现有配置           |

## 三、模块划分

### 3.1 后端模块（Spring Boot）

#### 3.1.1 新增实体类

```
ai-tutor-system/aispring/src/main/java/com/aispring/entity/
├── AgentSession.java          # Agent 会话实体
├── AgentTask.java             # Agent 任务实体
├── AgentFileSnapshot.java     # 文件快照实体
└── AgentToolCall.java         # 工具调用记录实体
```

#### 3.1.2 新增服务接口

```
ai-tutor-system/aispring/src/main/java/com/aispring/service/
├── AgentOrchestratorService.java     # Agent 编排服务
├── AgentSandboxService.java          # 沙箱管理服务
├── AgentFileService.java             # 文件操作服务
├── AgentTerminalService.java         # 终端执行服务
├── AgentGitService.java              # Git 影子仓库服务
└── AgentContextService.java          # 上下文管理服务
```

#### 3.1.3 新增控制器

```
ai-tutor-system/aispring/src/main/java/com/aispring/controller/
├── AgentSessionController.java       # 会话管理 API
├── AgentTaskController.java          # 任务管理 API
├── AgentFileController.java          # 文件操作 API
├── AgentTerminalController.java      # 终端执行 API
└── AgentStreamController.java        # SSE 流式响应 API
```

### 3.2 前端模块（Vue 3）

#### 3.2.1 新增视图组件

```
ai-tutor-system/vue-app/src/views/
├── AgentSandboxView.vue              # Agent 沙箱主界面
├── AgentChatView.vue                 # Agent 对话界面
└── AgentFileManagerView.vue          # 文件管理界面
```

#### 3.2.2 新增组件

```
ai-tutor-system/vue-app/src/components/agent/
├── AgentTerminal.vue                 # Agent 终端组件
├── AgentFileTree.vue                 # 文件树组件
├── AgentCodeEditor.vue               # 代码编辑器组件
├── AgentChatPanel.vue                # 对话面板组件
├── AgentToolCallViewer.vue           # 工具调用可视化组件
├── AgentStatusPanel.vue              # 状态面板组件
└── AgentHistoryPanel.vue             # 历史记录面板组件
```

#### 3.2.3 新增服务

```
ai-tutor-system/vue-app/src/services/
├── agentService.js                   # Agent API 服务
├── agentWebSocket.js                 # WebSocket 连接服务
└── agentSSE.js                       # SSE 流式接收服务
```

#### 3.2.4 新增状态管理

```
ai-tutor-system/vue-app/src/stores/
└── agent.js                          # Agent 状态管理
```

### 3.3 执行层模块（Bun + TypeScript）

#### 3.3.1 目录结构

```
ai-tutor-system/agent-executor/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts                      # 入口文件
│   ├── orchestrator/
│   │   ├── ReActEngine.ts            # ReAct 循环引擎
│   │   ├── TaskScheduler.ts          # 任务调度器
│   │   └── ContextBuilder.ts         # 上下文构建器
│   ├── sandbox/
│   │   ├── SandboxManager.ts         # 沙箱管理器
│   │   ├── ResourceLimiter.ts        # 资源限制器
│   │   └── ProcessManager.ts         # 进程管理器
│   ├── tools/
│   │   ├── ToolRegistry.ts           # 工具注册表
│   │   ├── TerminalTool.ts           # 终端执行工具
│   │   ├── FileReadTool.ts           # 文件读取工具
│   │   ├── FileEditTool.ts           # 文件编辑工具
│   │   ├── FileSearchTool.ts         # 文件搜索工具
│   │   └── GitTool.ts                # Git 操作工具
│   ├── llm/
│   │   ├── LLMClient.ts              # LLM 客户端
│   │   ├── PromptBuilder.ts          # 提示词构建器
│   │   └── ResponseParser.ts         # 响应解析器
│   └── utils/
│       ├── logger.ts                 # 日志工具
│       ├── pathUtils.ts              # 路径工具
│       └── ansiStripper.ts           # ANSI 清理工具
└── tests/
    └── ...
```

## 四、核心功能实现

### 4.1 ReAct 循环引擎

```typescript
interface ReActStep {
  thought: string;      // 思考
  action: ToolCall;     // 行动
  observation: string;  // 观察
}

class ReActEngine {
  async execute(task: string, context: AgentContext): Promise<void> {
    while (!context.isComplete) {
      // 1. 思考：结合任务和上下文规划下一步
      const thought = await this.think(task, context);
      
      // 2. 行动：调用工具执行操作
      const action = await this.act(thought);
      
      // 3. 观察：获取执行结果
      const observation = await this.observe(action);
      
      // 4. 更新：更新上下文状态
      context.update(thought, action, observation);
    }
  }
}
```

### 4.2 沙箱隔离机制

```typescript
class SandboxManager {
  async createSandbox(taskId: string): Promise<Sandbox> {
    const sandboxDir = `/tmp/agent-sandbox/${taskId}`;
    
    // 创建隔离目录
    await fs.mkdir(sandboxDir, { recursive: true });
    
    // 配置 bwrap 参数
    const bwrapConfig = {
      roBind: ['/bin', '/lib', '/usr'],
      bind: [sandboxDir],
      unshareNet: true,
      memoryLimit: '512M',
      cpuLimit: 50
    };
    
    return new Sandbox(sandboxDir, bwrapConfig);
  }
}
```

### 4.3 文件编辑工具（精确行号锚点）

```typescript
class FileEditTool {
  async editByAnchor(
    path: string,
    anchor: string,      // 10字符锚点
    range: [number, number],
    content: string
  ): Promise<EditResult> {
    // 1. 读取文件
    const lines = await this.readFileLines(path);
    
    // 2. 校验锚点
    const targetLine = lines[range[0] - 1];
    if (!targetLine.startsWith(anchor)) {
      // 模糊匹配：上下5-10行
      return this.fuzzyMatch(lines, anchor, range);
    }
    
    // 3. 精确替换
    const newLines = [...lines];
    newLines.splice(range[0] - 1, range[1] - range[0] + 1, content);
    
    // 4. 写入文件
    await this.writeFile(path, newLines);
    
    return { success: true, linesModified: range[1] - range[0] + 1 };
  }
}
```

### 4.4 Git 影子仓库

```typescript
class GitShadowService {
  async initShadowRepo(sandboxDir: string): Promise<void> {
    await exec(`git init`, { cwd: sandboxDir });
    await exec(`git config user.email "agent@local"`, { cwd: sandboxDir });
    await exec(`git config user.name "Agent"`, { cwd: sandboxDir });
    await exec(`git add .`, { cwd: sandboxDir });
    await exec(`git commit -m "Initial snapshot"`, { cwd: sandboxDir });
  }
  
  async createSnapshot(sandboxDir: string, message: string): Promise<string> {
    await exec(`git add .`, { cwd: sandboxDir });
    await exec(`git commit -m "${message}"`, { cwd: sandboxDir });
    const result = await exec(`git rev-parse HEAD`, { cwd: sandboxDir });
    return result.stdout.trim();
  }
  
  async rollback(sandboxDir: string): Promise<void> {
    await exec(`git reset --hard HEAD^`, { cwd: sandboxDir });
  }
}
```

### 4.5 系统提示词模板

```typescript
const SYSTEM_PROMPT = `
# Role: AI Sandbox Developer Agent
你是一个运行在高度受限（2核/1.8G RAM）且隔离的 Linux 沙箱环境中的高级智能体。你通过精准的工具调用协助用户完成代码编写、系统运维和项目管理。

## 🔴 VOID RULES (绝对执行准则)
1. **SANDBOX_ONLY**: 只能访问当前工作目录。禁止尝试访问 \`/etc\`, \`/root\` 等绝对路径。
2. **GIT_SHADOW**: 系统在每次工具调用前会自动创建 Git 快照。若操作失败或逻辑错误，必须调用 \`<undo_last_action>{}</undo_last_action>\`。
3. **MEMORY_FIRST**: 1.8G 内存严禁一次性读取 > 500 行的文件。禁止执行高内存占用的扫描命令。
4. **ANCHOR_EDIT**: 修改已有文件时，禁止重写全文。必须使用 \`<edit_file_by_anchor>\`，通过 10 字符锚点定位。
5. **REACT_PROCESS**: 必须遵循 "思考 -> 行动 -> 观察" 循环。在调用工具前，必须输出 \`<thinking>\` 标签说明理由。
6. **NO_WASTE**: 保持回复极其简练。任务完成后仅需简短确认。如果没有任务，仅使用自然语言交流。

## 🧩 TOOL PROTOCOL (XML + JSON 混合格式)
所有工具调用必须严格遵循 \`<tool_name>{ "json_args": "value" }</tool_name>\`。

### 1. 终端执行
\`<terminal_run>{ "command": "npm install --silent", "is_long_running": false }</terminal_run>\`
- 必须使用 \`--silent\` 或 \`-q\` 减少输出。长耗时任务设置 \`is_long_running: true\`。

### 2. 精确编辑 (推荐)
\`<edit_file_by_anchor>
{
  "path": "./src/App.vue",
  "anchor": "const msg ",
  "range": [10, 10],
  "content": "const msg = ref('Hello World');"
}
</edit_file_by_anchor>\`
- \`anchor\`: 目标行起始的 10 个关键字符。\`range\`: [起始行, 结束行]。

### 3. 分页读取
\`<read_file>{ "path": "filename", "start_line": 1, "end_line": 100 }</read_file>\`

### 4. 搜索与发现
\`<search_in_files>{ "regex": "keyword", "glob": "**/*.ts" }</search_in_files>\`
\`<ls>{ "path": "./src" }</ls>\`

### 5. 状态回滚
\`<undo_last_action>{}</undo_last_action>\`

## 📋 FEW-SHOT EXAMPLES (工具调用范例)

### 示例 A: 定位并修改代码
**User:** "修改 Home.vue 的标题"
**Assistant:** \`<thinking>需要先读取 Home.vue 确定标题行号和锚点。</thinking>\`
\`<read_file>{"path": "./src/views/Home.vue", "start_line": 1, "end_line": 30}</read_file>\`

**Observation:** (10) \`<h1>Old Title</h1>\`

**Assistant:** \`<thinking>行号 10 匹配。锚点为 "<h1>Old "。执行精确替换。</thinking>\`
\`<edit_file_by_anchor>{"path": "./src/views/Home.vue", "anchor": "<h1>Old ", "range": [10, 10], "content": "<h1>New Title</h1>"}</edit_file_by_anchor>\`

### 示例 B: 报错处理与回滚
**User:** "运行 test.py"
**Assistant:** \`<thinking>执行脚本并观察结果。</thinking>\`
\`<terminal_run>{"command": "python3 test.py"}</terminal_run>\`

**Observation:** Traceback... NameError: name 'x' is not defined. [Exit Code: 1]

**Assistant:** \`<thinking>代码运行报错。为保持环境纯净，先撤销刚才可能的干扰操作（如临时创建的文件）。</thinking>\`
\`<undo_last_action>{}</undo_last_action>\`

## 📋 USER CONTEXT (由动态消息注入)
- 当前目录: {cwd}
- 内存余量: {free_mem}
- 项目类型: {project_type}
`;
```

## 五、API 设计

### 5.1 会话管理 API

| 方法     | 路径                       | 说明     |
| ------ | ------------------------ | ------ |
| POST   | /api/agent/sessions      | 创建新会话  |
| GET    | /api/agent/sessions      | 获取会话列表 |
| GET    | /api/agent/sessions/{id} | 获取会话详情 |
| DELETE | /api/agent/sessions/{id} | 删除会话   |

### 5.2 任务管理 API

| 方法   | 路径                           | 说明       |
| ---- | ---------------------------- | -------- |
| POST | /api/agent/tasks             | 创建任务     |
| GET  | /api/agent/tasks/{id}        | 获取任务状态   |
| POST | /api/agent/tasks/{id}/cancel | 取消任务     |
| GET  | /api/agent/tasks/{id}/stream | SSE 流式响应 |

### 5.3 文件操作 API

| 方法   | 路径                       | 说明     |
| ---- | ------------------------ | ------ |
| GET  | /api/agent/files         | 获取文件树  |
| GET  | /api/agent/files/content | 读取文件内容 |
| PUT  | /api/agent/files/content | 编辑文件   |
| POST | /api/agent/files/search  | 搜索文件   |

### 5.4 终端执行 API

| 方法     | 路径                            | 说明     |
| ------ | ----------------------------- | ------ |
| POST   | /api/agent/terminal/execute   | 执行命令   |
| GET    | /api/agent/terminal/jobs      | 获取任务列表 |
| DELETE | /api/agent/terminal/jobs/{id} | 终止任务   |

## 六、开发阶段

### 阶段一：基础架构搭建（预计 3 天）

1. **后端基础设施**

   * [ ] 创建实体类（AgentSession, AgentTask, AgentFileSnapshot）

   * [ ] 创建 Repository 接口

   * [ ] 创建基础 Controller

   * [ ] 配置数据库迁移脚本

2. **前端基础组件**

   * [ ] 创建 AgentSandboxView 主视图

   * [ ] 创建 agent.js 状态管理

   * [ ] 创建 agentService.js API 服务

   * [ ] 配置路由

3. **执行层初始化**

   * [ ] 初始化 Bun 项目

   * [ ] 配置 TypeScript

   * [ ] 创建基础目录结构

### 阶段二：核心功能实现（预计 5 天）

1. **ReAct 引擎**

   * [ ] 实现 ReActEngine 核心逻辑

   * [ ] 实现 ContextBuilder 上下文构建

   * [ ] 实现 TaskScheduler 任务调度

2. **工具系统**

   * [ ] 实现 ToolRegistry 工具注册

   * [ ] 实现 TerminalTool 终端执行

   * [ ] 实现 FileReadTool 文件读取

   * [ ] 实现 FileEditTool 文件编辑

   * [ ] 实现 FileSearchTool 文件搜索

   * [ ] 实现 GitTool Git 操作

3. **沙箱管理**

   * [ ] 实现 SandboxManager 沙箱创建

   * [ ] 实现 ResourceLimiter 资源限制

   * [ ] 实现 ProcessManager 进程管理

### 阶段三：前后端集成（预计 3 天）

1. **API 集成**

   * [ ] 实现会话管理 API

   * [ ] 实现任务管理 API

   * [ ] 实现文件操作 API

   * [ ] 实现终端执行 API

   * [ ] 实现 SSE 流式响应

2. **前端组件集成**

   * [ ] 集成 AgentTerminal 终端组件

   * [ ] 集成 AgentFileTree 文件树组件

   * [ ] 集成 AgentCodeEditor 编辑器组件

   * [ ] 集成 AgentChatPanel 对话面板

   * [ ] 集成 AgentToolCallViewer 工具调用可视化

### 阶段四：优化与测试（预计 2 天）

1. **性能优化**

   * [ ] 实现 Prompt Caching 前缀缓存

   * [ ] 实现上下文压缩

   * [ ] 优化内存使用

2. **测试**

   * [ ] 单元测试

   * [ ] 集成测试

   * [ ] 端到端测试

3. **文档**

   * [ ] API 文档

   * [ ] 用户手册

   * [ ] 部署指南

## 七、数据库设计

### 7.1 agent\_sessions 表

```sql
CREATE TABLE agent_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(255),
  working_directory VARCHAR(500),
  status VARCHAR(50) DEFAULT 'active',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 7.2 agent\_tasks 表

```sql
CREATE TABLE agent_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  task_type VARCHAR(50),
  input TEXT,
  output TEXT,
  status VARCHAR(50) DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP,
  FOREIGN KEY (session_id) REFERENCES agent_sessions(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 7.3 agent\_file\_snapshots 表

```sql
CREATE TABLE agent_file_snapshots (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  task_id BIGINT,
  file_path VARCHAR(500),
  content_hash VARCHAR(64),
  snapshot_type VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (session_id) REFERENCES agent_sessions(id),
  FOREIGN KEY (task_id) REFERENCES agent_tasks(id)
);
```

### 7.4 agent\_tool\_calls 表

```sql
CREATE TABLE agent_tool_calls (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  tool_name VARCHAR(100),
  tool_input TEXT,
  tool_output TEXT,
  status VARCHAR(50),
  duration_ms INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (task_id) REFERENCES agent_tasks(id)
);
```

## 八、安全考虑

1. **沙箱隔离**：使用 bwrap + cgroups 确保操作隔离
2. **路径校验**：禁止访问沙箱外的路径
3. **资源限制**：限制内存、CPU、进程数
4. **命令过滤**：禁止执行危险命令
5. **审计日志**：记录所有操作

## 九、依赖清单

### 9.1 后端新增依赖

```xml
<!-- 无需新增，复用现有依赖 -->
```

### 9.2 前端新增依赖

```json
{
  "dependencies": {
    "xterm": "^5.3.0",
    "xterm-addon-fit": "^0.8.0",
    "xterm-addon-web-links": "^0.9.0"
  }
}
```

### 9.3 执行层依赖

```json
{
  "dependencies": {
    "ai": "^3.0.0",
    "strip-ansi": "^7.1.0",
    "tree-kill": "^1.2.2",
    "pino": "^8.0.0",
    "zod": "^3.22.0"
  }
}
```

## 十、风险与对策

| 风险                  | 影响 | 对策                 |
| ------------------- | -- | ------------------ |
| Windows 环境不支持 bwrap | 高  | 使用 Docker 容器作为替代方案 |
| 内存不足导致服务崩溃          | 高  | 实现严格的资源限制和监控       |
| AI 幻觉导致错误操作         | 中  | 实现 Git 回滚机制        |
| 长时间任务超时             | 中  | 实现异步任务和进度通知        |

## 十一、验收标准

1. **功能验收**

   * [ ] 用户可以创建和管理 Agent 会话

   * [ ] Agent 可以执行终端命令并返回结果

   * [ ] Agent 可以读取和编辑文件

   * [ ] Agent 可以搜索文件内容

   * [ ] 支持 Git 快照和回滚

   * [ ] 支持 SSE 流式响应

2. **性能验收**

   * [ ] 单任务内存占用 < 512MB

   * [ ] 命令执行超时 < 60秒

   * [ ] 文件读取支持分页

3. **安全验收**

   * [ ] 无法访问沙箱外的文件

   * [ ] 危险命令被拦截

   * [ ] 所有操作有审计日志

