# Token 消耗监控与敏感信息脱敏功能完成报告

## 功能概述

### 1. Token 消耗监控
已成功实现完整的 Token 消耗审计系统，用于成本控制与分析：

**核心组件：**
- `TokenUsageAudit` 实体：记录 API 提供方、模型、用户、Token 数、响应耗时等
- `TokenUsageAuditRepository`：数据持久化接口
- `TokenUsageAuditService`：异步审计服务（不阻塞主流程）
- Flyway 迁移脚本 `V3_2__create_token_usage_audit.sql`：自动创建审计表

**审计维度：**
- `provider`：API 提供方（deepseek / doubao / default）
- `modelName`：模型名称（deepseek-chat、doubao-pro-32k 等）
- `userId`：用户 ID（匿名为 null）
- `sessionId`：会话 ID
- `inputTokens / outputTokens / totalTokens`：Token 消耗
- `responseTimeMs`：响应耗时（毫秒）
- `streaming`：是否流式请求

**集成点：**
- 流式 Spring AI 调用：`performBlockingSpringAiChat`
- 流式 OkHttp 调用：`performBlockingOkHttpChat`
- 非流式调用：`ask()`

### 2. 敏感信息脱敏
已实现请求发送前的自动脱敏过滤层：

**核心组件：**
- `SensitiveDataMasker` 工具类：使用正则识别并替换敏感信息

**脱敏类型：**
- 身份证号（18 位）
- 手机号（11 位，1 开头）
- 固定电话（带区号）
- 邮箱地址
- 银行卡号（16-19 位）

**集成点：**
- 在 `performBlockingChat` 入口统一脱敏当前 prompt
- 在 `performBlockingOkHttpChat` 中脱敏历史 user 消息
- 在 `ask()` 中脱敏 prompt

**脱敏策略：**
- 发送给大模型的内容：**已脱敏**（保护隐私）
- 保存到数据库的聊天记录：**原文**（用户可查看完整历史）

## 技术亮点

1. **异步审计**：使用 `@Async` 注解，审计写入不阻塞 AI 响应流程
2. **智能估算**：无法获取实际 Token 数时，按字符数 / 4 估算
3. **无侵入设计**：脱敏逻辑在最外层统一处理，内部逻辑无感知
4. **完整覆盖**：所有 AI 调用路径（Spring AI、OkHttp、流式、非流式）均已接入

## 数据库变更

新增表 `token_usage_audit`：
- 主键：`id`
- 索引：`user_id`、`provider`、`created_at`（便于按用户/提供方/时间统计）

## 使用场景

### Token 消耗监控
- 按用户统计 Token 使用量，实现配额管理
- 按 API 提供方分析成本，优化模型选择
- 追踪响应耗时，发现性能瓶颈

### 敏感信息脱敏
- 符合隐私合规要求（GDPR、PIPL 等）
- 防止用户隐私数据泄露到外部 AI 服务
- 降低数据安全风险

## 后续建议

1. **审计可视化**：在管理后台增加 Token 消耗统计图表
2. **配额控制**：基于审计数据实现用户 Token 配额限制
3. **告警机制**：异常消耗（单次请求过大、频率异常）自动告警
4. **脱敏规则扩展**：支持自定义正则规则，适配更多场景

## 项目文件清单

### 新增文件
- `aispring/src/main/java/com/aispring/entity/TokenUsageAudit.java`
- `aispring/src/main/java/com/aispring/repository/TokenUsageAuditRepository.java`
- `aispring/src/main/java/com/aispring/service/TokenUsageAuditService.java`
- `aispring/src/main/java/com/aispring/service/impl/TokenUsageAuditServiceImpl.java`
- `aispring/src/main/java/com/aispring/util/SensitiveDataMasker.java`
- `aispring/src/main/resources/db/migration/V3_2__create_token_usage_audit.sql`

### 修改文件
- `aispring/src/main/java/com/aispring/service/impl/AiChatServiceImpl.java`（集成脱敏与审计）
- `.experience`（记录功能实现经验）

---

**实现完成时间**：2026-02-19  
**功能状态**：✅ 已完成并集成到主流程
