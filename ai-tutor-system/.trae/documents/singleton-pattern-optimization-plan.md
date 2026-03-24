# 单例模式优化计划

## 一、项目概述

本计划针对 `aispring`（Java 后端）和 `vue-app`（Vue 前端）两个项目进行单例模式优化，目标是消除重复创建的对象，提升资源利用率和代码可维护性。

---

## 二、aispring 项目优化方案

### 2.1 高优先级问题

#### 问题 1：ObjectMapper 重复创建（高）

**影响文件**：
| 文件 | 行号 | 问题 |
|------|------|------|
| SessionMetadataService.java | 36 | `private static final ObjectMapper objectMapper = new ObjectMapper();` |
| AiChatServiceImpl.java | 50 | 同上 |
| SearchInstructionHandler.java | 27 | 同上 |
| SseChatHandler.java | 20 | 同上 |
| DeepSeekApiClient.java | 29 | 同上 |
| OcrServiceImpl.java | 30 | 同上 |
| SearchServiceImpl.java | 77 | 方法内每次调用创建新实例 |

**优化方案**：
项目已有 `JacksonConfig.java` 配置类，应通过 Spring 依赖注入 ObjectMapper。

```java
// 优化前
private static final ObjectMapper objectMapper = new ObjectMapper();

// 优化后
private final ObjectMapper objectMapper;

public ClassName(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
}
```

---

#### 问题 2：服务类手动创建实例（高）

**影响文件**：`AiChatServiceImpl.java`

**问题描述**：
```java
// 在构造函数中手动创建实例，而非依赖注入
this.deepSeekApiClient = new DeepSeekApiClient(...);
this.chatHistoryBuilder = new ChatHistoryBuilder(...);
this.searchInstructionHandler = new SearchInstructionHandler(...);
this.sseChatHandler = new SseChatHandler();
this.sessionMetadataService = new SessionMetadataService(...);
```

**风险**：
- 这些类标注了 `@Component`，但被手动创建后无法享受 Spring 的依赖注入
- 每次创建 AiChatServiceImpl 都会创建新的实例，导致资源浪费
- 无法进行单元测试的 Mock

**优化方案**：
将这些服务类改为通过构造函数注入：

```java
public AiChatServiceImpl(
    DeepSeekApiClient deepSeekApiClient,
    ChatHistoryBuilder chatHistoryBuilder,
    SearchInstructionHandler searchInstructionHandler,
    SseChatHandler sseChatHandler,
    SessionMetadataService sessionMetadataService,
    // ... 其他依赖
) {
    this.deepSeekApiClient = deepSeekApiClient;
    this.chatHistoryBuilder = chatHistoryBuilder;
    // ...
}
```

---

### 2.2 中优先级问题

#### 问题 3：OkHttpClient 创建（中）

**影响文件**：`DeepSeekApiClient.java`

**问题描述**：当前在构造函数中创建 OkHttpClient，由于 AiChatServiceImpl 手动创建 DeepSeekApiClient，导致可能存在多个 OkHttpClient 实例。

**优化方案**：
创建 OkHttpClient 配置类：

```java
@Configuration
public class OkHttpClientConfig {
    
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build();
    }
}
```

---

#### 问题 4：DateTimeFormatter 重复创建（中）

**影响文件**：
| 文件 | 格式 |
|------|------|
| AdminChatService.java | `yyyy-MM-dd HH:mm:ss` |
| ChatRecord.java | 同上 |
| ChatRecordService.java | 同上 |
| VocabularyService.java | 同上 |
| JacksonConfig.java | 同上 |

**优化方案**：
创建常量类统一管理：

```java
public final class DateTimeConstants {
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern(DATETIME_FORMAT);
    
    private DateTimeConstants() {}
}
```

---

#### 问题 5：线程池管理（中）

**影响文件**：
| 文件 | 问题 |
|------|------|
| SSHWebSocketHandler.java | `Executors.newCachedThreadPool()` - 无界线程池风险 |
| AiChatServiceImpl.java | `Executors.newFixedThreadPool(8)` |
| SessionMetadataService.java | `Executors.newFixedThreadPool(2)` |

**优化方案**：
创建统一的线程池配置：

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean("sshExecutor")
    public ExecutorService sshExecutor() {
        return new ThreadPoolExecutor(
            5, 50, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    @Bean("chatExecutor")
    public ExecutorService chatExecutor() {
        return new ThreadPoolExecutor(
            8, 16, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100)
        );
    }
    
    @Bean("backgroundExecutor")
    public ExecutorService backgroundExecutor() {
        return Executors.newFixedThreadPool(2);
    }
}
```

---

#### 问题 6：Pattern 重复创建（中）

**影响文件**：
| 文件 | 问题 |
|------|------|
| VocabularyService.java | `countOccurrencesIgnoreCase` 方法每次调用创建新 Pattern |
| SemanticSearchServiceImpl.java | `parseAIResponse` 方法每次调用创建新 Pattern |

**优化方案**：
使用 Pattern 缓存：

```java
private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

private Pattern getOrCreatePattern(String word) {
    return PATTERN_CACHE.computeIfAbsent(word.toLowerCase(), 
        w -> Pattern.compile("\\b" + Pattern.quote(w) + "\\b", Pattern.CASE_INSENSITIVE));
}
```

---

#### 问题 7：SSL 上下文重复创建（中）

**影响文件**：`SearchServiceImpl.java`

**问题描述**：每次搜索请求都创建新的 SSLContext 和 TrustManager。

**优化方案**：
将 SSLContext 作为类成员变量：

```java
private final SSLContext sslContext;

public SearchServiceImpl() {
    try {
        TrustManager[] trustAllCerts = ...;
        this.sslContext = SSLContext.getInstance("TLSv1.2");
        this.sslContext.init(null, trustAllCerts, new SecureRandom());
    } catch (Exception e) {
        throw new RuntimeException("Failed to initialize SSL context", e);
    }
}
```

---

## 三、vue-app 项目优化方案

### 3.1 高优先级问题

#### 问题 1：WebSocket 连接重复创建（高）

**影响文件**：
| 文件 | 问题 |
|------|------|
| TerminalWindowView.vue | 创建独立的 WebSocket 连接 |
| TerminalTab.vue | 也创建独立的 WebSocket 连接，功能相似 |

**问题描述**：两个组件各自创建 WebSocket 连接，可能导致资源浪费和连接管理混乱。

**优化方案**：
创建 WebSocket 服务单例：

```javascript
// services/webSocketService.js
class WebSocketService {
  constructor() {
    this.connections = new Map()
  }
  
  getConnection(id, url) {
    if (!this.connections.has(id)) {
      this.connections.set(id, new WebSocket(url))
    }
    return this.connections.get(id)
  }
  
  closeConnection(id) {
    const ws = this.connections.get(id)
    if (ws) {
      ws.close()
      this.connections.delete(id)
    }
  }
}

export default new WebSocketService()
```

---

#### 问题 2：Audio 对象管理（高）

**影响文件**：
| 文件 | 问题 |
|------|------|
| useEnglishSound.js | `usePlayWordSound` 每次调用创建新 Audio 实例 |
| VocabPracticeCard.vue | 创建 Audio 但未在组件销毁时清理 |

**优化方案**：
创建 Audio 服务单例：

```javascript
// services/audioService.js
class AudioService {
  constructor() {
    this.sentenceAudio = new Audio()
    this.wordAudioPool = []
  }
  
  playWordSound(word) {
    const audio = this.wordAudioPool.pop() || new Audio()
    audio.src = `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word)}&type=1`
    audio.play()
    audio.onended = () => this.wordAudioPool.push(audio)
  }
}

export default new AudioService()
```

---

### 3.2 中优先级问题

#### 问题 3：Theme 状态重复（中）

**影响文件**：
| 文件 | 问题 |
|------|------|
| stores/theme.js | 管理深色模式状态 |
| stores/settings.js | 也包含 theme 设置 |

**问题描述**：两个 store 都管理主题状态，可能导致状态不一致。

**优化方案**：
合并主题状态到单一 store，或创建统一的主题管理服务。

---

#### 问题 4：事件监听器清理（中）

**影响文件**：`VocabPracticeCard.vue`

**问题描述**：创建 Audio 但未在组件销毁时清理。

**优化方案**：
```javascript
let audio = null

const playAudio = (word) => {
  if (audio) {
    audio.pause()
    audio.src = ''
  }
  audio = new Audio(`https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word)}&type=1`)
  audio.play()
}

onUnmounted(() => {
  if (audio) {
    audio.pause()
    audio.src = ''
    audio = null
  }
})
```

---

### 3.3 已正确实现的模块

以下模块已正确实现单例模式，无需优化：

| 文件 | 说明 |
|------|------|
| request.js | axios 实例单例 |
| graphqlClient.js | GraphQL 客户端单例 |
| rsaEncryption.js | RSA 加密单例 |
| permission-manager.ts | 经典单例模式实现 |
| context-summarizer.ts | 导出单例实例 |
| useGameMode.js | 模块级 ref 状态共享 |
| LanguageLearningView.vue | 正确实现事件监听器清理 |

---

## 四、实施计划

### 4.1 阶段一：高优先级（预计 2 天）

| 序号 | 任务 | 项目 | 状态 |
|------|------|------|------|
| 1 | ObjectMapper 统一注入 | aispring | ⏳ 待开始 |
| 2 | 服务类依赖注入改造 | aispring | ⏳ 待开始 |
| 3 | WebSocket 服务单例化 | vue-app | ⏳ 待开始 |
| 4 | Audio 服务单例化 | vue-app | ⏳ 待开始 |

### 4.2 阶段二：中优先级（预计 1 天）

| 序号 | 任务 | 项目 | 状态 |
|------|------|------|------|
| 1 | OkHttpClient 配置类 | aispring | ⏳ 待开始 |
| 2 | DateTimeFormatter 常量类 | aispring | ⏳ 待开始 |
| 3 | 线程池配置类 | aispring | ⏳ 待开始 |
| 4 | Pattern 缓存优化 | aispring | ⏳ 待开始 |
| 5 | SSLContext 单例化 | aispring | ⏳ 待开始 |
| 6 | Theme 状态合并 | vue-app | ⏳ 待开始 |
| 7 | 事件监听器清理完善 | vue-app | ⏳ 待开始 |

---

## 五、预期收益

1. **资源利用率提升**：减少重复创建的对象，降低内存占用
2. **配置一致性**：统一管理 ObjectMapper、DateTimeFormatter 等配置
3. **可测试性增强**：依赖注入使单元测试更容易 Mock
4. **可维护性提升**：统一管理线程池、连接池等资源
5. **性能优化**：减少对象创建开销，Pattern 缓存提升正则匹配性能

---

## 六、风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 依赖注入改造影响现有功能 | 高 | 逐步改造，充分测试 |
| 线程池配置变更影响并发 | 中 | 保留原有参数，逐步调优 |
| WebSocket 单例化影响多连接场景 | 中 | 支持多连接管理 |

---

## 七、总结

本计划共识别出 **14 个优化点**：

- **aispring 项目**：7 个优化点（2 高优先级 + 5 中优先级）
- **vue-app 项目**：4 个优化点（2 高优先级 + 2 中优先级）
- **已正确实现**：7 个模块

通过本次优化，将显著提升代码的资源利用率和可维护性。
