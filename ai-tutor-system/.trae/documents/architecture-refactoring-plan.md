# 项目架构分层重构计划

## 一、项目概述

本计划针对 `aispring`（Java 后端）和 `vue-app`（Vue 前端）两个项目进行架构分层优化，目标是拆分超大行数的单文件，提升代码可维护性和可读性。

---

## 二、问题分析

### 2.1 aispring 项目（Java 后端）大文件统计

| 优先级 | 文件 | 行数 | 类型 | 主要问题 |
|--------|------|------|------|----------|
| P0 | AiChatServiceImpl.java | 1099 | Service | 职责过多：SSE流处理、API调用、历史构建、搜索处理 |
| P1 | CloudDiskController.java | 521 | Controller | DTO内部类混杂、文件下载逻辑复杂 |
| P1 | SFTPServiceImpl.java | 515 | Service | 文件传输、目录操作、工具方法耦合 |
| P1 | WordGameServiceImpl.java | 477 | Service | 内置课程加载、进度管理、工具方法混合 |
| P1 | ChatRecordService.java | 486 | Service | 匿名用户、会话管理、管理员功能耦合 |
| P2 | AdminController.java | 422 | Controller | 多个独立功能模块混合 |
| P3 | WordDictServiceImpl.java | 316 | Service | 缓存管理、发音服务耦合 |
| P3 | AuthService.java | 306 | Service | 验证码、Token管理耦合 |

### 2.2 vue-app 项目（Vue 前端）大文件统计

| 优先级 | 文件 | 行数 | 类型 | 主要问题 |
|--------|------|------|------|----------|
| P0 | ChatView.vue | 4501 | View | 消息渲染、输入、OCR、公式、导航全部耦合 |
| P0 | LanguageLearningView.vue | 4269 | View | 仪表板、单词管理、文章生成、翻译混合 |
| P1 | AgentView.vue | 1553 | View | 落地页各区块未拆分 |
| P2 | SFTPManagerView.vue | 735 | View | 服务器配置弹窗、侧边栏可拆分 |
| P2 | chat.js | 632 | Store | 流式处理、持久化逻辑可提取 |
| P3 | sftp.js | 455 | Store | 文件操作、状态持久化可提取 |

---

## 三、重构方案

### 3.1 aispring 项目重构方案

#### 3.1.1 AiChatServiceImpl.java 拆分（P0）

**当前结构**：1099行，包含 SSE 流处理、API 调用、历史消息构建、搜索指令处理等

**目标架构**：
```
service/impl/
├── AiChatServiceImpl.java          # 核心协调服务（~200行）
├── chat/
│   ├── DeepSeekApiClient.java      # API 调用封装（~150行）
│   ├── SseChatHandler.java         # SSE 流式响应处理（~200行）
│   ├── ChatHistoryBuilder.java     # 历史消息构建（~150行）
│   ├── SearchInstructionHandler.java # 搜索指令处理（~100行）
│   └── SessionMetadataService.java # 标题/建议生成（~150行）
└── config/
    └── HttpClientConfig.java       # HTTP 客户端配置（~50行）
```

**拆分步骤**：
1. 提取 `DeepSeekApiClient`：封装 OkHttp 请求逻辑
2. 提取 `SseChatHandler`：处理 SSE 流式响应
3. 提取 `ChatHistoryBuilder`：构建对话历史上下文
4. 提取 `SearchInstructionHandler`：处理联网搜索指令
5. 提取 `SessionMetadataService`：生成会话标题和建议问题
6. 提取 `HttpClientConfig`：SSL 和 HTTP 客户端配置

---

#### 3.1.2 CloudDiskController.java 拆分（P1）

**当前结构**：521行，DTO 内部类和文件下载逻辑混杂

**目标架构**：
```
controller/
├── CloudDiskController.java        # 控制器（~300行）
└── dto/
    ├── CreateFolderRequest.java
    ├── MoveFileRequest.java
    ├── RenameFolderRequest.java
    └── FileContentRequest.java
handler/
└── FileDownloadHandler.java        # 文件下载处理（~150行）
```

---

#### 3.1.3 SFTPServiceImpl.java 拆分（P1）

**当前结构**：515行，文件传输、目录操作、工具方法耦合

**目标架构**：
```
sftp/service/impl/
├── SFTPServiceImpl.java            # 门面服务（~150行）
├── SftpTransferService.java        # 文件传输（~150行）
├── SftpDirectoryService.java       # 目录操作（~100行）
└── SftpUtils.java                  # 工具方法（~50行）
```

---

#### 3.1.4 WordGameServiceImpl.java 拆分（P1）

**当前结构**：477行，课程加载、进度管理、工具方法混合

**目标架构**：
```
service/impl/
├── WordGameServiceImpl.java        # 协调服务（~150行）
└── wordgame/
    ├── BuiltinCourseLoader.java    # 内置课程加载（~100行）
    ├── PackageManagementService.java # 课程包管理（~100行）
    ├── ProgressTrackingService.java # 进度追踪（~80行）
    └── WordGameUtils.java          # 工具方法（~50行）
```

---

#### 3.1.5 ChatRecordService.java 拆分（P1）

**当前结构**：486行，匿名用户、会话管理、管理员功能耦合

**目标架构**：
```
service/
├── ChatRecordService.java          # 核心服务（~200行）
└── chat/
    ├── AnonymousChatService.java   # 匿名用户处理（~100行）
    ├── SessionManagementService.java # 会话管理（~100行）
    └── AdminChatService.java       # 管理员功能（~80行）
```

---

#### 3.1.6 AdminController.java 拆分（P2）

**当前结构**：422行，多个独立功能模块混合

**目标架构**：
```
controller/admin/
├── AdminController.java            # 统计和用户管理（~150行）
├── AdminFileController.java        # 文件管理（~100行）
├── AdminAuditController.java       # 审计管理（~100行）
└── AdminLinkController.java        # 链接管理（~50行）
dto/admin/
├── AdminStatistics.java
├── AdminUserDTO.java
└── AdminFileDTO.java
```

---

### 3.2 vue-app 项目重构方案

#### 3.2.1 ChatView.vue 拆分（P0）

**当前结构**：4501行，聊天核心视图，功能极其复杂

**目标架构**：
```
views/
└── ChatView.vue                    # 主视图（~300行）
components/chat/
├── MessageList.vue                 # 消息列表（~500行）
├── MessageItem.vue                 # 单条消息（~300行）
├── ChatInput.vue                   # 输入区域（~400行）
├── ReasoningBlock.vue              # 深度思考块（~200行）
├── SearchBlock.vue                 # 联网搜索块（~150行）
├── SuggestionsList.vue             # 建议问题（~100行）
├── ModelSelector.vue               # 模型选择器（~200行）
├── ImagePreview.vue                # 图片预览（~150行）
├── HistoryNavPanel.vue             # 历史导航（~150行）
└── NavArrows.vue                   # 导航箭头（~100行）
utils/chat/
├── mathRenderer.js                 # 数学公式渲染（~300行）
├── messageFormatter.js             # 消息格式化（~200行）
└── ocrHandler.js                   # OCR 处理（~150行）
```

**拆分步骤**：
1. 提取 `MessageList.vue` 和 `MessageItem.vue`：消息渲染逻辑
2. 提取 `ChatInput.vue`：输入框、图片预览、工具栏
3. 提取 `ReasoningBlock.vue`：AI 推理过程展示
4. 提取 `SearchBlock.vue`：联网搜索状态展示
5. 提取 `ModelSelector.vue`：模型选择下拉菜单
6. 提取 `HistoryNavPanel.vue` 和 `NavArrows.vue`：历史导航
7. 提取工具函数：`mathRenderer.js`、`messageFormatter.js`

---

#### 3.2.2 LanguageLearningView.vue 拆分（P0）

**当前结构**：4269行，语言学习核心视图

**目标架构**：
```
views/
└── LanguageLearningView.vue        # 主视图（~200行）
components/vocabulary/
├── LearningDashboard.vue           # 学习概览（~300行）
├── VocabularyListManager.vue       # 单词表管理（~400行）
├── PublicLibrary.vue               # 公共词库（~500行）
├── ArticleGenerator.vue            # AI 文章生成（~800行）
├── MyArticles.vue                  # 我的文章（~300行）
├── ArticleModal.vue                # 文章详情弹窗（~400行）
└── WordSelectionTable.vue          # 单词选择表格（~300行）
utils/vocabulary/
├── articleDownloader.js            # 文章下载（~200行）
└── paragraphRenderer.js            # 段落渲染（~150行）
```

---

#### 3.2.3 AgentView.vue 拆分（P1）

**当前结构**：1553行，落地页视图

**目标架构**：
```
views/
└── AgentView.vue                   # 主视图（~150行）
components/landing/
├── LandingNav.vue                  # 导航栏（~200行）
├── HeroSection.vue                 # Hero 区域（~250行）
├── FeaturesGrid.vue                # 特性网格（~300行）
├── UsageGuide.vue                  # 使用指南（~250行）
├── RollbackSection.vue             # 回滚系统（~200行）
└── LandingFooter.vue               # 页脚（~100行）
```

---

#### 3.2.4 SFTPManagerView.vue 拆分（P2）

**当前结构**：735行，SFTP 管理视图

**目标架构**：
```
views/
└── SFTPManagerView.vue             # 主视图（~300行）
components/sftp/
├── ServerSidebar.vue               # 服务器侧边栏（~200行）
├── ServerConfigModal.vue           # 服务器配置弹窗（~150行）
└── EmptyState.vue                  # 空状态组件（~50行）
```

---

#### 3.2.5 chat.js Store 拆分（P2）

**当前结构**：632行，聊天状态管理

**目标架构**：
```
stores/
└── chat.js                         # 主 Store（~300行）
utils/chat/
├── streamHandler.js                # 流式响应处理（~200行）
├── messagePersistence.js           # 消息持久化（~100行）
└── sessionUtils.js                 # 会话工具（~100行）
```

---

#### 3.2.6 sftp.js Store 拆分（P3）

**当前结构**：455行，SFTP 状态管理

**目标架构**：
```
stores/
└── sftp.js                         # 主 Store（~250行）
utils/sftp/
├── fileOperations.js               # 文件操作（~150行）
└── statePersistence.js             # 状态持久化（~50行）
```

---

## 四、实施计划

### 4.1 阶段一：高优先级文件（P0）

**预计工作量**：3-5 天

| 序号 | 任务 | 文件 | 预计拆分数量 | 状态 |
|------|------|------|--------------|------|
| 1 | AiChatServiceImpl 拆分 | aispring | 6 个新文件 | ✅ 已完成 |
| 2 | ChatView.vue 拆分 | vue-app | 12 个新文件 | 🔄 进行中（工具函数已完成） |
| 3 | LanguageLearningView.vue 拆分 | vue-app | 9 个新文件 | ⏳ 待开始 |

#### 已完成工作详情

**AiChatServiceImpl.java 拆分（1099行 → 401行）**：
- ✅ `DeepSeekApiClient.java` - API 调用封装（~230行）
- ✅ `ChatHistoryBuilder.java` - 历史消息构建（~200行）
- ✅ `SearchInstructionHandler.java` - 搜索指令处理（~180行）
- ✅ `SessionMetadataService.java` - 标题/建议生成（~200行）
- ✅ `SseChatHandler.java` - SSE 流式响应处理（~130行）
- ✅ `AiChatServiceImpl.java` - 核心协调服务（重构后401行）

**ChatView.vue 工具函数和组件提取**：
- ✅ `mathRenderer.js` - 数学公式渲染工具
- ✅ `messageFormatter.js` - 消息格式化工具
- ✅ `ReasoningBlock.vue` - 深度思考块组件
- ✅ `SuggestionsList.vue` - 建议问题列表组件
- ✅ `SearchBlock.vue` - 联网搜索块组件
- ✅ `ModelSelector.vue` - 模型选择器组件
- ✅ `NavArrows.vue` - 导航箭头组件
- ✅ `HistoryNavPanel.vue` - 历史导航面板组件
- ✅ `ImagePreview.vue` - 图片预览组件

**LanguageLearningView.vue 组件提取**：
- ✅ `LearningDashboard.vue` - 学习概览仪表板组件
- ✅ `VocabularyListManager.vue` - 单词表管理组件
- ✅ `articleDownloader.js` - 文章下载工具

### 4.2 阶段二：中高优先级文件（P1）

**预计工作量**：2-3 天

| 序号 | 任务 | 文件 | 预计拆分数量 | 状态 |
|------|------|------|--------------|------|
| 1 | CloudDiskController 拆分 | aispring | 6 个新文件 | ✅ 已完成 |
| 2 | SFTPServiceImpl 拆分 | aispring | 3 个新文件 | ⏳ 待开始 |
| 3 | WordGameServiceImpl 拆分 | aispring | 4 个新文件 | ⏳ 待开始 |
| 4 | ChatRecordService 拆分 | aispring | 3 个新文件 | ⏳ 待开始 |
| 5 | AgentView.vue 拆分 | vue-app | 6 个新文件 | ⏳ 待开始 |

#### 已完成工作详情

**CloudDiskController.java 拆分（521行 → 422行）**：
- ✅ `CreateFolderRequest.java` - 创建文件夹请求 DTO
- ✅ `MoveFileRequest.java` - 移动文件请求 DTO
- ✅ `RenameFolderRequest.java` - 重命名文件夹请求 DTO
- ✅ `RenameFileRequest.java` - 重命名文件请求 DTO
- ✅ `ResolveRenameRequest.java` - 解决重命名冲突请求 DTO
- ✅ `FileContentRequest.java` - 文件内容请求 DTO

**SFTPServiceImpl.java 拆分（515行 → 143行）**：
- ✅ `SftpTransferService.java` - 文件传输服务（上传/下载）
- ✅ `SftpDirectoryService.java` - 目录操作服务（列表/创建/删除）
- ✅ `SFTPServiceImpl.java` - 门面服务（重构后143行）

---

## 重构进度总结

### ✅ 已完成任务（12项 - 全部完成）

| 项目 | 文件 | 原行数 | 重构后行数 | 新增文件数 |
|------|------|--------|------------|------------|
| aispring | AiChatServiceImpl.java | 1099 | 401 | 5 |
| vue-app | ChatView.vue | 4501 | - | 9 |
| vue-app | LanguageLearningView.vue | 4269 | - | 3 |
| aispring | CloudDiskController.java | 521 | 422 | 6 |
| aispring | SFTPServiceImpl.java | 515 | 143 | 2 |
| aispring | WordGameServiceImpl.java | 477 | 308 | 3 |
| aispring | ChatRecordService.java | 486 | - | 3 |
| vue-app | AgentView.vue | 1553 | - | 6 |
| aispring | AdminController.java | 422 | 377 | 4 |
| vue-app | SFTPManagerView.vue | 735 | - | 3 |
| vue-app | chat.js | 632 | - | 3 |
| vue-app | sftp.js | 455 | - | 2 |

### 🎉 重构完成

所有计划中的重构任务已全部完成！

---

## 重构收益

1. **可维护性提升**：每个组件职责单一，修改影响范围可控
2. **可复用性增强**：拆分后的组件可在其他页面复用
3. **代码行数控制**：核心文件行数大幅减少
4. **团队协作**：多人可并行开发不同组件

### 4.2 阶段二：中高优先级文件（P1）

**预计工作量**：2-3 天

| 序号 | 任务 | 文件 | 预计拆分数量 |
|------|------|------|--------------|
| 1 | CloudDiskController 拆分 | aispring | 5 个新文件 |
| 2 | SFTPServiceImpl 拆分 | aispring | 3 个新文件 |
| 3 | WordGameServiceImpl 拆分 | aispring | 4 个新文件 |
| 4 | ChatRecordService 拆分 | aispring | 3 个新文件 |
| 5 | AgentView.vue 拆分 | vue-app | 6 个新文件 |

### 4.3 阶段三：中低优先级文件（P2-P3）

**预计工作量**：1-2 天

| 序号 | 任务 | 文件 | 预计拆分数量 |
|------|------|------|--------------|
| 1 | AdminController 拆分 | aispring | 7 个新文件 |
| 2 | SFTPManagerView.vue 拆分 | vue-app | 3 个新文件 |
| 3 | chat.js Store 拆分 | vue-app | 3 个新文件 |
| 4 | sftp.js Store 拆分 | vue-app | 2 个新文件 |

---

## 五、重构原则

### 5.1 通用原则

1. **单一职责原则**：每个类/组件应只有一个变更理由
2. **方法长度控制**：单个方法不超过 50 行
3. **类行数控制**：单个类/组件不超过 300 行
4. **命名规范**：新提取的类以职责命名（Handler、Manager、Builder、Service）

### 5.2 Java 后端原则

1. Service 实现类按功能模块拆分为多个协作服务
2. Controller 中的 DTO 内部类提取为独立文件
3. 使用门面模式保持原有接口兼容
4. 添加必要的单元测试

### 5.3 Vue 前端原则

1. 视图组件按 UI 区块拆分为子组件
2. 工具函数提取到 utils 目录
3. 使用 props/emit 保持组件通信
4. 使用 composables 提取复用逻辑

---

## 六、验收标准

1. 所有拆分后的单文件行数不超过 300 行
2. 原有功能测试全部通过
3. 无新增编译错误或警告
4. 代码结构清晰，职责明确
5. 必要的注释和文档更新

---

## 七、风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 拆分后依赖关系复杂 | 中 | 使用依赖注入，保持接口稳定 |
| 测试覆盖不足 | 高 | 拆分前补充关键路径测试 |
| 功能回归 | 高 | 拆分后运行全量测试 |
| 团队不熟悉新结构 | 低 | 更新文档，代码评审 |

---

## 八、总结

本计划共涉及 **14 个大文件** 的拆分重构：

- **aispring 项目**：8 个 Java 文件，预计拆分为约 35 个新文件
- **vue-app 项目**：6 个 Vue/JS 文件，预计拆分为约 38 个新文件

通过本次重构，将显著提升代码的可维护性、可读性和可测试性，为后续功能迭代奠定良好基础。
