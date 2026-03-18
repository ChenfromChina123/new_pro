# AI文章模块 P0+P1 开发计划（双路径兼容）

## 1. Summary
- 目标：基于现有 `aispring + vue-app` 代码，按需求文档落地 AI 文章模块的 P0+P1 能力。
- 范围：在现有 `LanguageLearningView.vue` 的 `ai-articles` 视图迭代；后端保留 `/api/vocabulary/articles/*`，新增 `/api/ai/article/*` 兼容路径。
- 交付：单词选择（我的/公共）、主题推荐、多语言生成、结果操作（复制/PDF）、历史管理（列表/筛选/详情/删除/批量/清空）与异常处理。

## 2. Current State Analysis

### 2.1 后端现状（已具备）
- 已有文章生成链路：`/api/vocabulary/articles/topics|generate|{id}|download-pdf`，核心在 `VocabularyController`、`VocabularyService`。
- 已有文章实体与关联：`GeneratedArticle`、`ArticleUsedWord`，支持文章内容和用词明细存储。
- 已有 PDF 渲染能力：`renderPdfFromHtml`，包含 Windows 字体兜底方案。
- 已有公共词库搜索分页：`/api/vocabulary/public/search`。

### 2.2 后端缺口（与需求相比）
- 缺少需求路径 `/api/ai/article/*`。
- 缺少历史管理完整接口：筛选分页、删除、批量删除、清空。
- 文章详情接口存在越权风险（当前 `getArticle` 未校验用户归属）。
- 缺少明确迁移脚本覆盖 `generated_articles` / `article_used_words`，当前更依赖运行时建表。
- 生成参数与需求不对齐：缺少显式 `target_language`，长度/难度/校验规则不完整。

### 2.3 前端现状（已具备）
- 入口与页面已存在：`LanguageLearningView.vue` 的 `ai-articles` 区块。
- 已有功能：选词、主题推荐、生成、我的文章列表、详情弹窗、HTML/TXT/PDF 下载。
- API 封装已存在：`src/stores/vocabulary.js`、`src/config/api.js`。

### 2.4 前端缺口（与需求相比）
- 未支持“我的单词库/公共词库”双来源并保留已选词的完整交互。
- 历史管理缺少筛选、分页、删除、批量删除、清空。
- 生成参数未完整覆盖需求（目标语言、长度档位、难度校验、20词上限、必填前置校验）。
- PDF 下载调用分散在视图层，未统一封装到 store。

## 3. Proposed Changes

### 3.1 数据层与迁移
1) 新增 Flyway 迁移脚本  
- 文件：`aispring/src/main/resources/db/migration/V3_8__enhance_ai_article_module.sql`  
- 内容：
  - 为 `generated_articles` 补齐/对齐字段（如 `target_language`、`word_count`、`updated_at`、软删标记），保留现有字段兼容旧逻辑。
  - 为历史筛选与排序补索引（`user_id + created_at`、`target_language`、软删字段）。
  - 为 `article_used_words` 增补必要索引与约束。
- 原因：避免仅依赖 `ddl-auto`，确保生产可重复部署。

2) 实体与仓库补齐  
- 文件：  
  - `aispring/src/main/java/com/aispring/entity/GeneratedArticle.java`  
  - `aispring/src/main/java/com/aispring/repository/GeneratedArticleRepository.java`  
  - `aispring/src/main/java/com/aispring/repository/ArticleUsedWordRepository.java`  
- 变更：
  - 实体增加新字段映射（与迁移一致）。
  - Repository 增加分页筛选、按用户软删、批量删除、清空等方法。

### 3.2 后端接口与业务逻辑
1) 新增需求路径控制器（双路径兼容）  
- 新文件：`aispring/src/main/java/com/aispring/controller/AiArticleController.java`  
- 路径前缀：`/api/ai/article`  
- 提供接口：
  - `GET /word-library`
  - `POST /recommend-theme`
  - `POST /generate`
  - `GET /history-list`
  - `GET /history-detail`
  - `POST /export-pdf`
  - `POST /delete-history`
  - `POST /clear-history`
- 说明：内部复用现有服务层能力，避免两套实现分叉。

2) 扩展服务层能力  
- 文件：`aispring/src/main/java/com/aispring/service/VocabularyService.java`  
- 变更：
  - 增加目标语言参数，生成 Prompt 按语言输出。
  - 统一参数校验（必填、20词上限、主题长度、长度档位、难度值）。
  - 历史记录查询支持关键词/语言/时间范围分页。
  - 增加删除、批量删除、清空逻辑（按 userId 严格隔离）。
  - 详情查询强制校验归属，修复越权。
  - 主题推荐失败与生成失败统一错误码与提示映射。

3) 兼容旧接口  
- 文件：`aispring/src/main/java/com/aispring/controller/VocabularyController.java`  
- 变更：
  - 旧接口继续可用，调用同一服务逻辑。
  - 文章详情接口补用户归属校验，行为与新路径一致。

### 3.3 单词库查询能力（我的/公共）
- 文件：  
  - `aispring/src/main/java/com/aispring/repository/VocabularyWordRepository.java`  
  - `aispring/src/main/java/com/aispring/repository/PublicVocabularyWordRepository.java`  
  - `aispring/src/main/java/com/aispring/service/VocabularyService.java`  
- 变更：
  - 增加统一查询方法，支持关键词、分类（我的词库优先基于词表语言与可用字段过滤；公共词库使用 `tag`）。
  - 输出统一响应结构，适配前端双词库切换。

### 3.4 前端页面改造（在现有页面迭代）
1) API 与 store 扩展  
- 文件：  
  - `vue-app/src/config/api.js`  
  - `vue-app/src/stores/vocabulary.js`  
- 变更：
  - 新增 `/api/ai/article/*` 端点封装。
  - 新增词库查询、历史筛选分页、删除/批量删除/清空、PDF 导出接口。
  - 将 PDF 下载逻辑统一收敛到 store，视图仅调用。

2) 视图交互改造  
- 文件：`vue-app/src/views/LanguageLearningView.vue`  
- 变更：
  - 生成页新增“我的单词库/公共词库”切换、搜索、分类筛选、已选词区管理。
  - 保持跨词库已选词状态；超过20词时阻止勾选并提示。
  - 生成参数补齐：主题（必填/200字）、目标语言、长度、难度。
  - 生成按钮前置校验与加载态，失败提示统一。
  - 历史页支持关键词/语言/时间筛选、分页、查看详情、删除/批量删除/清空。
  - 结果区保留复制与 PDF 下载，下载失败提示完善。

### 3.5 文档同步
- 文件：
  - `aispring/README.md`
  - `vue-app/README.md`
  - （如根 README 有相关章节）`README.md`
- 变更：
  - 增补 AI 文章模块接口说明（新旧路径兼容说明）。
  - 增补前端使用方式、筛选与历史管理能力说明。

## 4. Assumptions & Decisions
- 已确认实现范围：P0+P1，P2 不在本次交付。
- 已确认接口策略：双路径兼容（保留 `/api/vocabulary/articles/*`，新增 `/api/ai/article/*`）。
- 已确认前端策略：在 `LanguageLearningView.vue` 现有页面迭代，不新建独立路由页。
- 兼容策略：对外响应继续兼容 snake_case 字段习惯，避免前端现有逻辑大面积重写。
- 安全策略：所有历史详情/删除/清空操作按 `userId` 强校验，禁止越权。

## 5. Verification Steps
1) 后端验证  
- 运行编译与单测：`mvn -q -DskipTests=false test`（在 `aispring` 目录）。  
- 增加控制器层用例（重点：详情越权、删除/批量/清空、筛选分页、参数校验）。  

2) 前端验证  
- 运行构建与基础检查：`npm run build`（在 `vue-app` 目录）。  
- 手工验证关键流程：
  - 双词库切换后已选词保持；
  - 20词上限提示；
  - 主题推荐与文章生成前置校验；
  - 历史筛选分页与删除/批量/清空；
  - PDF 下载（中文文件名、多语言显示）。

3) 回归验证  
- 旧路径 `/api/vocabulary/articles/*` 能继续驱动当前页面核心流程。  
- 新路径 `/api/ai/article/*` 与需求文档接口行为一致。  

