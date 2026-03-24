# 单例模式优化计划（第二阶段）

## 一、项目概述

本计划针对 `aispring`（Java 后端）和 `vue-app`（Vue 前端）两个项目进行单例模式优化，目标是消除重复创建的对象，提升资源利用率和代码可维护性。

---

## 二、第一阶段已完成工作

### 2.1 aispring 项目

| 任务 | 状态 | 说明 |
|------|------|------|
| ObjectMapper 统一注入 | ✅ 已完成 | 多个服务类已改为依赖注入 |
| DateTimeConstants 常量类 | ✅ 已完成 | 创建 `constant/DateTimeConstants.java` |
| ThreadPoolConfig 配置类 | ✅ 已完成 | 创建 `config/ThreadPoolConfig.java` |
| OkHttpClient 配置 | ✅ 已完成 | DeepSeekApiClient 已使用依赖注入 |

### 2.2 vue-app 项目

| 任务 | 状态 | 说明 |
|------|------|------|
| WebSocket 服务单例 | ✅ 已完成 | 创建 `services/webSocketService.js` |
| Audio 服务单例 | ✅ 已完成 | 创建 `services/audioService.js` |

---

## 三、第二阶段优化方案（新增发现）

### 3.1 aispring 项目

#### 问题 1：RestTemplate 重复创建（高优先级）⚠️ 待优化

**影响文件**：
| 文件 | 行号 | 代码片段 |
|------|------|----------|
| AiVocabServiceImpl.java | 33 | `private final RestTemplate restTemplate = new RestTemplate();` |
| SystemPromptServiceImpl.java | 26 | `private final RestTemplate restTemplate = new RestTemplate();` |

**优化方案**：
创建 `RestTemplateConfig` 配置类，提供全局 RestTemplate Bean。

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

#### 问题 2：ObjectMapper 未完全优化（高优先级）⚠️ 待优化

**影响文件**：
| 文件 | 行号 | 问题 |
|------|------|------|
| OcrServiceImpl.java | 30 | `private final ObjectMapper objectMapper = new ObjectMapper();` |
| RedisConfig.java | 69 | 私有方法创建 ObjectMapper，未暴露为 Bean |

**优化方案**：
1. 将 RedisConfig 中的 `objectMapper()` 方法改为 `@Bean`
2. 修改 OcrServiceImpl 注入使用

---

#### 问题 3：线程池未完全注入（高优先级）⚠️ 待优化

**影响文件**：
| 文件 | 行号 | 问题 |
|------|------|------|
| AiChatServiceImpl.java | 120 | `Executors.newFixedThreadPool(8)` 应注入 ThreadPoolConfig 的 chatExecutor |
| SSHWebSocketHandler.java | 32 | `Executors.newCachedThreadPool()` 应注入 ThreadPoolConfig 的 sshExecutor |

**优化方案**：
修改这两个类，注入 ThreadPoolConfig 中已配置的线程池 Bean。

---

#### 问题 4：HashMap 作为缓存（中优先级）⚠️ 待优化

**影响文件**：
| 文件 | 行号 | 问题 |
|------|------|------|
| ModelCapabilityServiceImpl.java | 26 | `private final Map<String, ModelCapability> capabilityCache = new HashMap<>();` |

**优化方案**：
改用 `ConcurrentHashMap` 保证线程安全，或使用 Spring Cache。

---

### 3.2 vue-app 项目

#### 问题 1：Audio 对象未使用单例服务（高优先级）⚠️ 待优化

**影响文件**：
| 文件 | 行号 | 问题 |
|------|------|------|
| VocabPracticeCard.vue | 381 | `const audio = new Audio(...)` 每次播放创建新对象 |
| useEnglishSound.js | 61 | `const wordAudio = new Audio()` 每次调用创建新对象 |

**优化方案**：
修改这些文件使用已创建的 `audioService` 单例。

---

#### 问题 2：WebSocket 未使用单例服务（高优先级）⚠️ 待优化

**影响文件**：
| 文件 | 行号 | 问题 |
|------|------|------|
| TerminalWindowView.vue | 234 | `ws = new WebSocket(wsUrl)` 直接创建 |
| TerminalTab.vue | 373 | `ws = new WebSocket(wsUrl)` 直接创建 |

**优化方案**：
修改这些文件使用已创建的 `webSocketService.getConnection()` 方法。

---

#### 问题 3：localStorage 分散使用（中优先级）⚠️ 待优化

**影响文件**：
| 文件 | 使用次数 | 主要用途 |
|------|----------|----------|
| auth.js | 10 | Token、用户信息存储 |
| RequirementManager.vue | 11 | 需求文档持久化 |
| ui.js | 4 | UI 状态持久化 |
| chat.js | 4 | 聊天设置持久化 |
| ChatView.vue | 3 | 上传图片缓存 |
| theme.js | 2 | 主题设置 |
| sftp.js | 2 | SFTP 连接信息 |

**优化方案**：
创建统一的 `StorageService` 单例管理所有持久化存储：
- 统一的键名管理
- 数据序列化/反序列化
- 过期时间支持

---

## 四、第二阶段实施计划

### 4.1 高优先级任务

| 序号 | 任务 | 项目 | 状态 |
|------|------|------|------|
| 1 | 创建 RestTemplateConfig 配置类 | aispring | ✅ 已完成 |
| 2 | 修改 AiVocabServiceImpl 注入 RestTemplate | aispring | ✅ 已完成 |
| 3 | 修改 SystemPromptServiceImpl 注入 RestTemplate | aispring | ✅ 已完成 |
| 4 | RedisConfig ObjectMapper 改为 Bean | aispring | ✅ 已完成 |
| 5 | 修改 OcrServiceImpl 注入 ObjectMapper | aispring | ✅ 已完成 |
| 6 | AiChatServiceImpl 注入 chatExecutor | aispring | ✅ 已完成 |
| 7 | SSHWebSocketHandler 注入 sshExecutor | aispring | ✅ 已完成 |
| 8 | VocabPracticeCard.vue 使用 audioService | vue-app | ✅ 已完成 |
| 9 | useEnglishSound.js 使用 audioService | vue-app | ✅ 已完成 |
| 10 | TerminalWindowView.vue 使用 webSocketService | vue-app | ✅ 已完成 |
| 11 | TerminalTab.vue 使用 webSocketService | vue-app | ✅ 已完成 |

### 4.2 中优先级任务

| 序号 | 任务 | 项目 | 状态 |
|------|------|------|------|
| 1 | ModelCapabilityServiceImpl 缓存优化 | aispring | ✅ 已完成 |
| 2 | 创建 StorageService 统一管理 localStorage | vue-app | ⏳ 待开始 |

---

## 五、预期收益

1. **资源利用率提升**：减少重复创建的对象，降低内存占用
2. **配置一致性**：统一管理 ObjectMapper、RestTemplate、线程池等配置
3. **可测试性增强**：依赖注入使单元测试更容易 Mock
4. **可维护性提升**：统一管理连接池、缓存等资源
5. **性能优化**：减少对象创建开销，WebSocket 连接复用

---

## 六、风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 依赖注入改造影响现有功能 | 高 | 逐步改造，充分测试 |
| 线程池配置变更影响并发 | 中 | 保留原有参数，逐步调优 |
| WebSocket 单例化影响多连接场景 | 中 | 支持多连接管理 |

---

## 七、总结

**第一阶段**：已完成 6 项核心任务，创建了单例服务基础架构。

**第二阶段**：发现 13 个新优化点（11 高优先级 + 2 中优先级），主要涉及：
- aispring：RestTemplate、ObjectMapper、线程池注入、缓存优化
- vue-app：Audio/WebSocket 服务使用、localStorage 统一管理

通过两阶段优化，将实现高度的单例模式，显著提升代码的资源利用率和可维护性。
