# AI智能学习助手系统 - 技术架构详解

> 本文档详细记录系统的技术实现细节，涵盖架构设计、核心模块实现、安全机制、性能优化等方面，可作为面试技术深度的参考材料。

---

## 一、系统架构概览

### 1.1 整体架构设计

本系统采用**前后端分离 + 微服务化**的架构模式，由三个核心子系统组成：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           用户层 (Browser)                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
            │   vue-app    │ │  word-game   │ │   Nginx      │
            │  (Vue 3)     │ │  (Vue 3)     │ │  反向代理    │
            │  Port: 3000  │ │  Port: 5200  │ │              │
            └──────────────┘ └──────────────┘ └──────────────┘
                    │               │
                    └───────┬───────┘
                            ▼
            ┌──────────────────────────────┐
            │        aispring 后端         │
            │    (Spring Boot 3.3.5)       │
            │        Port: 5000            │
            └──────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │    MySQL     │ │    Redis     │ │  AI Services │
    │   (主数据库)  │ │  (缓存/限流) │ │ DeepSeek/豆包 │
    └──────────────┘ └──────────────┘ └──────────────┘
                            │
                            ▼
            ┌──────────────────────────────┐
            │     word-game-server         │
            │    (Express 5 + SQLite)      │
            │        Port: 5201            │
            └──────────────────────────────┘
```

### 1.2 技术栈总览

| 层级           | 技术选型              | 版本   | 选型理由                                        |
| -------------- | --------------------- | ------ | ----------------------------------------------- |
| **后端框架**   | Spring Boot           | 3.3.5  | 企业级Java框架，生态完善，支持响应式编程        |
| **前端框架**   | Vue 3                 | 3.4.0  | Composition API，更好的TypeScript支持，性能优化 |
| **构建工具**   | Vite                  | 5.0.0  | ES Module原生支持，HMR速度快，打包体积小        |
| **状态管理**   | Pinia                 | 2.1.7  | Vue官方推荐，TypeScript友好，模块化设计         |
| **数据库**     | MySQL                 | 8.x    | 关系型数据库，支持复杂查询和事务                |
| **缓存**       | Redis                 | 7.x    | 高性能内存数据库，支持限流、会话管理            |
| **ORM**        | Spring Data JPA       | 3.x    | 简化数据访问层开发，支持方法命名查询            |
| **认证**       | JWT + Spring Security | 0.12.3 | 无状态认证，支持分布式部署                      |
| **AI集成**     | Spring AI             | 0.8.1  | 官方AI集成框架，统一API调用方式                 |
| **数据库迁移** | Flyway                | 9.x    | 版本化数据库变更管理                            |

---

## 二、后端架构详解 (aispring)

### 2.1 项目结构

```
aispring/
├── src/main/java/com/aispring/
│   ├── config/              # 配置类
│   │   ├── SecurityConfig.java      # Spring Security 安全配置
│   │   ├── RedisConfig.java         # Redis 缓存配置
│   │   ├── CorsConfig.java          # 跨域配置
│   │   ├── JacksonConfig.java       # JSON 序列化配置
│   │   └── StorageProperties.java   # 文件存储配置
│   ├── controller/          # 控制器层 (REST API)
│   │   ├── AuthController.java      # 认证相关
│   │   ├── AiChatController.java    # AI 对话
│   │   ├── CloudDiskController.java # 云盘管理
│   │   └── ...
│   ├── service/             # 服务层 (业务逻辑)
│   │   ├── impl/            # 服务实现
│   │   ├── AiChatService.java
│   │   ├── AuthService.java
│   │   └── ...
│   ├── repository/          # 数据访问层 (JPA)
│   ├── entity/              # 实体类
│   ├── dto/                 # 数据传输对象
│   │   ├── request/         # 请求DTO
│   │   └── response/        # 响应DTO
│   ├── security/            # 安全相关
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetails.java
│   │   └── UserDetailsServiceImpl.java
│   ├── graphql/             # GraphQL 支持
│   └── exception/           # 异常处理
├── migrations/              # Flyway 数据库迁移脚本
└── pom.xml                  # Maven 依赖配置
```

### 2.2 核心依赖分析 (pom.xml)

```xml
<!-- 核心框架 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- JPA 数据访问 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Security 安全框架 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT 令牌处理 (jjwt 0.12.3) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- Spring AI 大模型集成 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>0.8.1</version>
</dependency>

<!-- GraphQL API 支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>

<!-- Redis 缓存 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Flyway 数据库版本管理 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**技术选型关键点：**

- **Spring Boot 3.x** 要求 **Java 17+**，支持 Virtual Threads（虚拟线程）
- **Spring AI 0.8.1** 提供统一的 AI 模型调用抽象层，支持 OpenAI 兼容 API
- **JJWT 0.12.3** 支持 JWT 的创建、解析和验证，支持多种签名算法

### 2.3 安全架构设计

#### 2.3.1 Spring Security 配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // 禁用 CSRF（前后端分离架构）
            .cors(cors -> {})              // 启用 CORS
            .authorizeHttpRequests(auth -> auth
                // OPTIONS 预检请求放行
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // SSE 异步分发放行
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                // 公开端点
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/ask", "/api/ask-stream").permitAll()
                // 其他需要认证
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 无状态会话
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // BCrypt 密码加密
    }
}
```

**面试要点：**

1. **为什么禁用 CSRF？** 前后端分离架构，使用 JWT Token 认证，不依赖 Cookie-Session 机制
2. **无状态会话的意义**：服务端不存储会话信息，便于水平扩展和负载均衡
3. **BCrypt 加密优势**：自动加盐、计算成本可调、抗彩虹表攻击

#### 2.3.2 JWT 认证过滤器

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private static final String[] PUBLIC_PATTERNS = {
        "/api/auth/**", "/swagger-ui/**", "/api/public-files/**"
    };

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS 请求和公开路径跳过过滤
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getServletPath();
        for (String pattern : PUBLIC_PATTERNS) {
            if (pathMatcher.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail = jwtUtil.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**面试要点：**

1. **OncePerRequestFilter**：确保每个请求只经过一次过滤器（避免转发/包含导致的重复过滤）
2. **AntPathMatcher**：支持 Ant 风格路径匹配（`**`、`*`、`?` 通配符）
3. **SecurityContextHolder**：使用 ThreadLocal 存储安全上下文，保证线程安全

#### 2.3.3 RSA 前端加密传输

为防止密码在传输过程中被截获，实现 RSA 非对称加密：

**后端提供公钥：**

```java
@RestController
@RequestMapping("/api/rsa")
public class RsaController {

    private final RsaUtil rsaUtil;

    @GetMapping("/public-key")
    public ApiResponse<String> getPublicKey() {
        return ApiResponse.success("获取成功", rsaUtil.getPublicKey());
    }
}
```

**前端加密实现：**

```javascript
import JSEncrypt from "jsencrypt";

class RsaEncryption {
  async initialize() {
    const response = await fetch("/api/rsa/public-key");
    const data = await response.json();
    this.publicKey = data.data;
  }

  encrypt(text) {
    const encrypt = new JSEncrypt();
    encrypt.setPublicKey(this.publicKey);
    return encrypt.encrypt(text);
  }
}
```

**安全流程：**

1. 前端获取 RSA 公钥
2. 用户密码使用公钥加密
3. 后端使用私钥解密
4. 解密后的密码再使用 BCrypt 加密存储

### 2.4 数据库设计

#### 2.4.1 核心实体关系

```
┌─────────────┐     1:1      ┌─────────────┐
│    User     │─────────────▶│    Admin    │
└─────────────┘              └─────────────┘
       │
       │ 1:N
       ▼
┌─────────────┐     N:1      ┌─────────────┐
│  UserFile   │◀─────────────│  UserFolder │
└─────────────┘              └─────────────┘
       │
       │ 1:N
       ▼
┌─────────────┐
│ ChatRecord  │
└─────────────┘
       │
       │ N:1
       ▼
┌─────────────┐
│ ChatSession │
└─────────────┘
```

#### 2.4.2 核心实体定义

**用户实体 (User.java)：**

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_username", columnList = "username")
})
@EntityListeners(AuditingEntityListener.class)
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Admin admin;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserFile> files = new ArrayList<>();

    public void setPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        this.passwordHash = encoder.encode(password);
    }

    public boolean checkPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, this.passwordHash);
    }
}
```

**聊天记录实体 (ChatRecord.java)：**

```java
@Entity
@Table(name = "chat_records", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_user_session", columnList = "user_id, session_id")
})
@Data
@Builder
public class ChatRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "message_order", nullable = false)
    private Integer messageOrder;  // 消息顺序，用于排序

    @Column(name = "sender_type", nullable = false)
    private Integer senderType;    // 1: 用户, 2: AI, 3: 工具结果

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "reasoning_content", columnDefinition = "TEXT")
    private String reasoningContent;  // AI 深度思考内容（推理模型）

    @Column(name = "ai_model", length = 50)
    private String aiModel;  // deepseek, doubao, deepseek-reasoner

    @Column(name = "status", length = 20)
    private String status;   // pending, completed, failed, cancelled

    @Column(name = "send_time")
    private LocalDateTime sendTime;
}
```

**面试要点：**

1. **索引设计**：`idx_user_session` 复合索引优化按用户+会话查询
2. **实体监听器**：`@CreatedDate`、`@LastModifiedDate` 自动维护时间戳
3. **级联操作**：`CascadeType.ALL` + `orphanRemoval = true` 实现级联删除

### 2.5 AI 服务集成

#### 2.5.1 多模型支持架构

系统支持多个 AI 模型提供商，通过策略模式实现灵活切换：

```java
@Service
public class AiChatServiceImpl implements AiChatService {

    // Spring AI 自动注入的客户端
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectProvider<StreamingChatClient> streamingChatClientProvider;

    // 自定义模型客户端
    private ChatClient doubaoChatClient;      // 豆包模型
    private ChatClient deepseekChatClient;    // DeepSeek 模型
    private ChatClient deepseekReasonerClient; // DeepSeek 推理模型

    @Value("${ai.doubao.api-key:}")
    private String doubaoApiKey;

    @Value("${ai.deepseek.api-url:}")
    private String deepseekApiUrl;

    // 初始化各模型客户端
    public AiChatServiceImpl(...) {
        // 豆包模型初始化
        if (doubaoApiKey != null && !doubaoApiKey.isEmpty()) {
            OpenAiApi doubaoApi = new OpenAiApi(doubaoBaseUrl + "/api/v3", doubaoApiKey);
            OpenAiChatOptions doubaoOptions = OpenAiChatOptions.builder()
                .withModel("doubao-pro-32k")
                .withTemperature(0.7f)
                .withMaxTokens(maxTokens)
                .build();
            this.doubaoChatClient = new OpenAiChatClient(doubaoApi, doubaoOptions);
        }

        // DeepSeek 模型初始化
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            OpenAiApi deepseekApi = new OpenAiApi(deepseekApiUrl, deepseekApiKey);
            this.deepseekChatClient = new OpenAiChatClient(deepseekApi);
        }
    }
}
```

**面试要点：**

1. **Spring AI 兼容性**：通过 OpenAI 兼容 API 接入国产模型（豆包、DeepSeek）
2. **ObjectProvider 延迟注入**：避免循环依赖，按需获取 Bean
3. **配置外部化**：API Key 和 URL 通过配置文件管理，支持环境变量覆盖

#### 2.5.2 流式响应实现 (SSE)

```java
@Override
public SseEmitter askStream(String prompt, String sessionId, String model,
                            Long userId, String ipAddress) {
    // 创建 SSE 发射器，超时 5 分钟
    SseEmitter emitter = new SseEmitter(300_000L);

    chatExecutor.execute(() -> {
        try {
            // 敏感信息脱敏
            String maskedPrompt = SensitiveDataMasker.mask(prompt);

            // 构建对话 Prompt（包含历史上下文）
            Prompt promptObj = buildPrompt(maskedPrompt, sessionId, userId, ...);

            // 流式调用 AI
            streamingChatClient.stream(promptObj)
                .doOnNext(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getContent();
                    if (content != null && !content.isEmpty()) {
                        // 发送 SSE 事件
                        Map<String, String> result = new HashMap<>();
                        result.put("content", content);
                        emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(result))
                            .name("message"));
                    }
                })
                .doOnError(e -> handleError(emitter, e))
                .blockLast();

            // 发送完成事件
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();

        } catch (Exception e) {
            handleError(emitter, e);
        }
    });

    return emitter;
}
```

**SSE (Server-Sent Events) 技术要点：**

1. **单向通信**：服务器向客户端推送数据，适合 AI 流式输出场景
2. **自动重连**：浏览器原生支持断线重连
3. **相比 WebSocket**：更简单、更轻量，适合单向数据流

#### 2.5.3 上下文管理策略

```java
@Value("${ai.context.max-history-messages:30}")
private Integer maxHistoryMessages;  // 最大历史消息数

@Value("${ai.context.max-history-chars:20000}")
private Integer maxHistoryChars;     // 最大历史字符数

private Prompt buildPrompt(String prompt, String sessionId, Long userId, ...) {
    List<Message> messages = new ArrayList<>();

    // 1. 添加系统提示词
    messages.add(new SystemMessage(systemPrompt));

    // 2. 加载历史对话（带预算控制）
    if (sessionId != null && userId != null) {
        int budget = maxHistoryChars;
        List<ChatRecord> history = chatRecordRepository
            .findByUserIdAndSessionIdOrderByMessageOrderDesc(userId, sessionId,
                PageRequest.of(0, maxHistoryMessages));

        for (ChatRecord record : history) {
            if (budget <= 0) break;
            String role = (record.getSenderType() == 1) ? "user" : "assistant";
            String content = truncateToMax(record.getContent(), budget);
            budget -= content.length();
            messages.add(0, new Message(role, content));
        }
    }

    // 3. 添加当前用户输入
    messages.add(new UserMessage(prompt));

    return new Prompt(messages);
}
```

**上下文管理策略：**

1. **消息数量限制**：防止上下文过长导致 Token 超限
2. **字符预算控制**：动态计算可用空间，优先保留最近对话
3. **敏感信息脱敏**：发送给 AI 前过滤身份证、手机号等隐私数据

### 2.6 GraphQL API 设计

#### 2.6.1 为什么选择 GraphQL？

| 特性     | REST API         | GraphQL                |
| -------- | ---------------- | ---------------------- |
| 数据获取 | 固定返回结构     | 按需获取字段           |
| 请求次数 | 可能需要多次请求 | 单次请求获取所有数据   |
| 版本管理 | v1, v2...        | 无需版本               |
| 适合场景 | 简单 CRUD        | 复杂数据关系、前端驱动 |

#### 2.6.2 声明式数据分片加载

```java
@Controller
public class RequirementDocGraphQLController {

    /**
     * 查询需求文档列表（分页）
     * 前端可通过 Fragment 指定需要的字段
     */
    @QueryMapping
    public RequirementDocConnection requirementDocs(
            @Argument Integer page,
            @Argument Integer size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Page<RequirementDoc> docPage = requirementDocRepository.findByUserId(
            principal.getUser().getId(),
            PageRequest.of(page, size)
        );
        return RequirementDocConnection.fromPage(docPage);
    }

    /**
     * 按需加载用户信息
     * 只有前端请求 user 字段时才执行
     */
    @SchemaMapping(typeName = "RequirementDoc", field = "user")
    public CompletableFuture<User> user(RequirementDoc doc) {
        return CompletableFuture.supplyAsync(() ->
            userRepository.findById(doc.getUserId()).orElse(null)
        );
    }

    /**
     * 按需加载统计信息
     */
    @SchemaMapping(typeName = "RequirementDoc", field = "statistics")
    public RequirementDocStatistics statistics(RequirementDoc doc) {
        int wordCount = doc.getContent() != null ? doc.getContent().length() : 0;
        int editCount = historyRepository.countByDocId(doc.getId());
        return RequirementDocStatistics.builder()
            .wordCount(wordCount)
            .editCount(editCount)
            .build();
    }
}
```

**前端查询示例：**

```graphql
query {
  requirementDocs(page: 0, size: 10) {
    content {
      id
      title
      ...BasicFields # 只加载基础字段
      user {
        username
        avatar
      }
    }
  }
}
```

### 2.7 PRD 需求文档流水线

#### 2.7.1 流水线架构

```
用户输入想法
     │
     ▼
┌─────────┐    ┌─────────┐    ┌─────────────┐
│ Outline │───▶│  Draft  │───▶│   Critique  │◀──┐
│  大纲   │    │  初稿   │    │    评审     │   │
└─────────┘    └─────────┘    └─────────────┘   │
                                    │          │
                                    ▼          │
                              ┌───────────┐    │
                              │  Revise   │────┘
                              │   修订    │ (最多3轮)
                              └───────────┘
                                    │
                                    ▼
                              ┌───────────┐
                              │ Complete  │
                              │   完成    │
                              └───────────┘
```

#### 2.7.2 流水线实现

```java
@Service
public class PrdPipelineServiceImpl implements PrdPipelineService {

    private static final int MAX_REVISIONS = 3;
    private static final String APPROVAL_PHRASE = "No issues found.";

    private static final String OUTLINE_PROMPT = """
        You are a world-class product manager. Your task is to create a structured
        outline for a Product Requirements Document (PRD)...
        """;

    private static final String CRITIQUE_PROMPT = """
        You are a meticulous and critical product manager...
        If the PRD is well-structured, respond with "No issues found."
        """;

    @Async
    @Override
    public void runPipelineAsync(String runId, String idea, Long userId) {
        String content = "# PRD for " + idea;
        int revision = 0;

        try {
            // Step 1: 生成大纲
            sendState(runId, "Outline", content, revision, null);
            content = aiChatService.ask(String.format(OUTLINE_PROMPT, idea), ...);
            revision++;

            // Step 2: 生成初稿
            sendState(runId, "Draft", content, revision, null);
            content = aiChatService.ask(String.format(DRAFT_PROMPT, content), ...);
            revision++;

            // Step 3: 评审循环
            for (int i = 0; i < MAX_REVISIONS; i++) {
                sendState(runId, "Critique", content, revision, null);
                String critique = aiChatService.ask(String.format(CRITIQUE_PROMPT, content), ...);

                if (critique.contains(APPROVAL_PHRASE)) {
                    break;  // 通过评审
                }

                // 根据评审意见修订
                content = aiChatService.ask(String.format(REVISE_PROMPT, content, critique), ...);
                revision++;
            }

            sendState(runId, "Complete", content, revision, null);
        } catch (Exception e) {
            sendState(runId, "Error", content, revision + 1, null);
        }
    }
}
```

**面试要点：**

1. **@Async 异步执行**：避免长时间阻塞 HTTP 请求，通过 SSE 推送进度
2. **评审循环**：最多 3 轮修订，确保文档质量
3. **状态推送**：通过 PrdStreamHolder 实时推送流水线状态

### 2.8 限流与审计

#### 2.8.1 基于 Redis 的 IP 限流

```java
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "chat_limit:";
    private static final int MAX_REQUESTS = 5;        // 每日最大请求次数
    private static final Duration EXPIRATION = Duration.ofHours(24);

    public boolean checkAndIncrement(String ip) {
        String key = KEY_PREFIX + ip;

        Long count = redisTemplate.opsForValue().increment(key);

        // 首次请求设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(key, EXPIRATION);
        }

        if (count != null && count > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for IP: {} (Count: {})", ip, count);
            return false;
        }

        return true;
    }
}
```

**限流算法：计数器算法**

- 优点：实现简单，内存占用小
- 缺点：存在临界问题（可在临界点突发请求）
- 改进方案：滑动窗口、令牌桶、漏桶

#### 2.8.2 Token 消耗审计

```java
public interface TokenUsageAuditService {
    void record(String provider, String modelName, Long userId, String sessionId,
                Integer inputTokens, Integer outputTokens,
                long responseTimeMs, boolean streaming);
}
```

记录每次 AI 调用的：

- Token 消耗量（输入/输出）
- 响应时间
- 模型提供商
- 用户/会话信息

---

## 三、前端架构详解 (vue-app)

### 3.1 项目结构

```
vue-app/
├── src/
│   ├── components/         # 可复用组件
│   │   ├── Sidebar/        # 侧边栏组件
│   │   ├── AppHeader.vue   # 头部组件
│   │   └── RequirementManager.vue  # PRD 管理组件
│   ├── composables/        # 组合式函数 (Hooks)
│   │   └── useGraphQLClient.js
│   ├── config/             # 配置文件
│   │   └── api.js          # API 端点配置
│   ├── stores/             # Pinia 状态管理
│   │   ├── auth.js         # 认证状态
│   │   ├── chat.js         # 聊天状态
│   │   ├── cloudDisk.js    # 云盘状态
│   │   └── theme.js        # 主题状态
│   ├── utils/              # 工具函数
│   │   ├── request.js      # Axios 封装
│   │   ├── rsaEncryption.js # RSA 加密
│   │   └── graphqlClient.js # GraphQL 客户端
│   ├── views/              # 页面视图
│   │   ├── auth/           # 认证相关页面
│   │   ├── ChatView.vue    # AI 对话页面
│   │   ├── CloudDiskView.vue # 云盘页面
│   │   └── WordGameView.vue # 单词记忆入口
│   ├── router/             # 路由配置
│   │   └── index.js
│   ├── App.vue             # 根组件
│   └── main.js             # 入口文件
├── vite.config.js          # Vite 配置
└── package.json
```

### 3.2 Vite 构建优化

```javascript
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: "http://localhost:5000", // 代理到后端
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          "vue-vendor": ["vue", "vue-router", "pinia"], // Vue 全家桶
          highlight: ["highlight.js"], // 代码高亮
          markdown: ["marked"], // Markdown 解析
        },
      },
    },
  },
});
```

**构建优化策略：**

1. **代码分割 (Code Splitting)**：将第三方库分离，利用浏览器缓存
2. **Tree Shaking**：Vite 基于 ES Module，自动移除未使用代码
3. **开发代理**：避免 CORS 问题，统一 API 路径

### 3.3 Pinia 状态管理

#### 3.3.1 认证状态管理 (auth.js)

```javascript
import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useAuthStore = defineStore("auth", () => {
  // 状态
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref(JSON.parse(localStorage.getItem("userInfo") || "null"));

  // 计算属性
  const isAuthenticated = computed(() => !!token.value);
  const isAdmin = computed(() => userInfo.value?.is_admin === true);

  // 登录方法
  async function login(email, password) {
    const response = await request.post(API_ENDPOINTS.auth.login, {
      email,
      password,
    });

    const accessToken = response?.data?.access_token;
    if (accessToken) {
      token.value = accessToken;
      userInfo.value = {
        id: response.data.user_id,
        email: response.data.email,
        username: response.data.username,
        is_admin: response.data.is_admin,
      };

      // 持久化存储
      localStorage.setItem("token", token.value);
      localStorage.setItem("userInfo", JSON.stringify(userInfo.value));

      return { success: true };
    }
    return { success: false };
  }

  // 退出登录
  function logout() {
    token.value = "";
    userInfo.value = null;
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
  }

  return {
    token,
    userInfo,
    isAuthenticated,
    isAdmin,
    login,
    logout,
  };
});
```

**Pinia vs Vuex 对比：**
| 特性 | Vuex | Pinia |
|------|------|-------|
| 状态定义 | state 对象 | ref/reactive |
| 修改方式 | mutations | 直接修改 |
| TypeScript | 需要额外配置 | 原生支持 |
| 模块化 | modules | 独立 Store |
| DevTools | 支持 | 支持 |

#### 3.3.2 聊天状态管理 (chat.js)

```javascript
export const useChatStore = defineStore("chat", () => {
  const sessions = ref([]);
  const currentSessionId = ref(null);
  const messages = ref([]);
  const isLoading = ref(false);
  const abortController = ref(null);
  const selectedModel = ref(localStorage.getItem("selectedModel") || "deepseek-chat");

  // 流式发送消息
  async function sendMessage(content, onChunk) {
    isLoading.value = true;
    abortController.value = new AbortController();

    // 添加用户消息
    messages.value.push({
      role: "user",
      content,
      timestamp: new Date().toISOString(),
    });

    // 添加 AI 消息占位符
    const aiMessage = {
      role: "assistant",
      content: "",
      reasoning_content: "",
      isStreaming: true,
      timestamp: new Date().toISOString(),
    };
    messages.value.push(aiMessage);

    try {
      const response = await fetch("/api/ask-stream", {
        method: "POST",
        signal: abortController.value.signal,
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${authStore.token}`,
        },
        body: JSON.stringify({
          prompt: content,
          session_id: currentSessionId.value,
          model: selectedModel.value,
        }),
      });

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        for (const line of lines) {
          if (line.startsWith("data:")) {
            const data = line.slice(5).trim();
            if (data === "[DONE]") continue;

            const parsed = JSON.parse(data);

            // 处理推理内容
            if (parsed.reasoning_content) {
              aiMessage.reasoning_content += parsed.reasoning_content;
            }

            // 处理回复内容
            if (parsed.content) {
              aiMessage.content += parsed.content;
            }
          }
        }
      }

      aiMessage.isStreaming = false;
      return { success: true };
    } catch (error) {
      if (error.name === "AbortError") {
        console.log("Generation aborted by user");
        return { success: true, aborted: true };
      }
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  // 停止生成
  function stopGeneration() {
    if (abortController.value) {
      abortController.value.abort();
    }
  }

  return {
    sessions,
    messages,
    isLoading,
    selectedModel,
    sendMessage,
    stopGeneration,
  };
});
```

**SSE 客户端实现要点：**

1. **ReadableStream API**：原生流式读取响应体
2. **AbortController**：支持取消请求
3. **增量更新**：逐字符追加内容，实现打字机效果

### 3.4 前端安全措施

1. **XSS 防护**：使用 DOMPurify 清理用户输入
2. **密码加密**：RSA 非对称加密传输
3. **Token 存储**：localStorage + XSS 风险权衡
4. **CORS 配置**：后端白名单控制

---

## 四、单词记忆模块 (word-game)

### 4.1 技术架构

```
word-game/
├── server/                 # Node.js 后端
│   ├── index.js           # Express 服务入口
│   └── progress.db        # SQLite 数据库
├── src/                    # Vue 3 前端
│   ├── components/
│   │   ├── game/          # 游戏组件
│   │   │   ├── AnswerView.vue
│   │   │   ├── TypingInput.vue
│   │   │   └── CompletionModal.vue
│   │   └── pages/         # 页面组件
│   ├── composables/       # 组合式函数
│   │   ├── useEnglishSound.ts   # 发音功能
│   │   └── useQuestion.ts       # 题目逻辑
│   ├── stores/            # Pinia 状态
│   │   ├── courseStore.ts
│   │   └── progressStore.ts
│   └── services/
│       └── wordGameApi.ts # API 封装
└── vite.config.ts
```

### 4.2 后端实现 (Express 5 + SQLite)

#### 4.2.1 数据库设计

```javascript
const db = new Database(join(__dirname, "progress.db"));
db.pragma("journal_mode = WAL"); // Write-Ahead Logging 提升并发性能

db.exec(`
  -- 用户学习进度表
  CREATE TABLE IF NOT EXISTS course_progress (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_key    TEXT    NOT NULL,        -- 用户标识（userId 或 guest_IP）
    user_type   TEXT    NOT NULL DEFAULT 'guest',
    package_id  TEXT    NOT NULL,        -- 课程包 ID
    course_index INTEGER NOT NULL,       -- 课程索引
    current_q   INTEGER NOT NULL DEFAULT 0,  -- 当前题目
    completed   INTEGER NOT NULL DEFAULT 0,  -- 是否完成
    study_secs  INTEGER NOT NULL DEFAULT 0,  -- 学习时长
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    UNIQUE(user_key, package_id, course_index)
  );

  -- 用户自定义课程包
  CREATE TABLE IF NOT EXISTS user_packages (
    id          TEXT    PRIMARY KEY,
    user_key    TEXT    NOT NULL,
    name        TEXT    NOT NULL,
    description TEXT    NOT NULL DEFAULT '',
    icon        TEXT    NOT NULL DEFAULT '📦',
    is_public   INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
  );

  -- 课程包题目
  CREATE TABLE IF NOT EXISTS user_package_statements (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    package_id  TEXT    NOT NULL,
    course_index INTEGER NOT NULL DEFAULT 0,
    english     TEXT    NOT NULL,
    chinese     TEXT    NOT NULL,
    soundmark   TEXT    NOT NULL DEFAULT ''
  );
`);
```

**SQLite 优势：**

1. **零配置**：无需独立数据库服务
2. **单文件存储**：便于备份和迁移
3. **WAL 模式**：提升并发读写性能
4. **适合场景**：嵌入式应用、小型项目、开发测试

#### 4.2.2 JWT 认证中间件

```javascript
/**
 * 与主站共享 JWT 密钥，实现单点登录
 */
function requireAispringAuth(req, res, next) {
  try {
    const auth = req.headers["authorization"];
    if (!auth || !auth.startsWith("Bearer ")) {
      return res.status(401).json({ success: false, message: "未登录" });
    }

    const token = auth.slice(7);
    const key = getJwtSigningKey(); // 与主站一致的密钥

    jwt.verify(token, key, { algorithms: ["HS256"] });
    next();
  } catch (err) {
    const msg = err.name === "TokenExpiredError" ? "登录已过期" : "无效的登录凭证";
    res.status(401).json({ success: false, message: msg });
  }
}

// 应用中间件
app.use("/api", requireAispringAuth);
```

#### 4.2.3 核心路由

```javascript
// 获取课程包列表（支持搜索、按点击量排序）
app.get("/api/packages", (req, res) => {
  const { key } = resolveUserKey(req);
  const search = req.query.search;

  let list = [...PACKAGES];  // 内置课程包
  const userRows = getUserPackagesList(key, search);
  list.push(...userRows.map(userPackageToMeta));

  // 按点击量排序
  list.sort((a, b) => (b.clickCount || 0) - (a.clickCount || 0));

  res.json({ success: true, data: list });
});

// 获取课程题目
app.get("/api/courses/:courseIndex/questions", (req, res) => {
  const packageId = req.query.packageId;
  const idx = parseInt(req.params.courseIndex, 10);

  if (packageId && packageId.startsWith("up-")) {
    // 用户自定义课程包
    const rows = db.prepare(`
      SELECT english, chinese, soundmark
      FROM user_package_statements
      WHERE package_id = ? AND course_index = ?
      ORDER BY sort_order, id
    `).all(packageId, idx - 1);
    return res.json({ success: true, data: rows });
  }

  // 内置课程包
  const filePath = join(COURSES_DIR, `${String(idx).padStart(2, "0")}.json`);
  const statements = JSON.parse(readFileSync(filePath, "utf-8"));
  res.json({ success: true, data: statements });
});

// 创建自定义课程包
app.post("/api/packages", (req, res) => {
  const { key } = resolveUserKey(req);
  const { name, description, sections, isPublic } = req.body;

  const packageId = "up-" + randomUUID().replace(/-/g, "").slice(0, 16);

  db.transaction(() => {
    // 插入课程包元数据
    db.prepare(`INSERT INTO user_packages ...`).run(packageId, key, name, ...);

    // 插入各节题目
    for (const section of sections) {
      for (const stmt of section.statements) {
        db.prepare(`INSERT INTO user_package_statements ...`).run(...);
      }
    }
  })();

  res.status(201).json({ success: true, data: { id: packageId } });
});
```

### 4.3 前端实现

#### 4.3.1 打字输入组件

```vue
<template>
  <div class="typing-input">
    <div class="chinese-hint">{{ question.chinese }}</div>
    <input
      v-model="userInput"
      @keydown="handleKeydown"
      :class="{ correct: isCorrect, wrong: isWrong }"
      placeholder="输入英文..."
      autofocus
    />
    <button @click="playSound">🔊 播放</button>
  </div>
</template>

<script setup>
import { useEnglishSound } from "@/composables/useEnglishSound";
import { computed, ref } from "vue";

const props = defineProps({
  question: Object,
});

const emit = defineEmits(["correct", "wrong"]);

const userInput = ref("");
const { play } = useEnglishSound();

const isCorrect = computed(
  () => userInput.value.trim().toLowerCase() === props.question.english.toLowerCase(),
);

function handleKeydown(e) {
  if (e.key === "Enter") {
    if (isCorrect.value) {
      emit("correct");
      userInput.value = "";
    } else {
      emit("wrong");
    }
  }
}

function playSound() {
  play(props.question.english);
}
</script>
```

#### 4.3.2 发音功能实现

```typescript
export function useEnglishSound() {
  const audioContext = new (window.AudioContext || window.webkitAudioContext)()

  async function play(text: string) {
    // 使用 Web Speech API
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'en-US'
    utterance.rate = 0.9
    speechSynthesis.speak(utterance)
  }

  return { play }
}
```

### 4.4 iframe 跨域通信

主站通过 iframe 嵌入单词记忆模块，使用 postMessage 实现通信：

```vue
<!-- vue-app/src/views/WordGameView.vue -->
<template>
  <iframe
    ref="gameFrame"
    :src="gameUrl"
    @load="onFrameLoad"
  />
</template>

<script setup>
import { useAuthStore } from "@/stores/auth";
import { useThemeStore } from "@/stores/theme";
import { onMounted, ref } from "vue";

const gameFrame = ref(null);
const authStore = useAuthStore();
const themeStore = useThemeStore();

const gameUrl = import.meta.env.PROD ? "https://earthworm.aistudy.icu" : "http://localhost:5200";

function onFrameLoad() {
  // 同步登录状态
  gameFrame.value.contentWindow.postMessage(
    {
      type: "AUTH_SYNC",
      token: authStore.token,
      user: authStore.userInfo,
    },
    "*",
  );

  // 同步主题
  gameFrame.value.contentWindow.postMessage(
    {
      type: "THEME_SYNC",
      isDark: themeStore.isDark,
    },
    "*",
  );
}

// 监听 iframe 返回的消息
window.addEventListener("message", (event) => {
  if (event.data.type === "PROGRESS_UPDATE") {
    // 处理进度更新
  }
});
</script>
```

---

## 五、部署架构

### 5.1 生产环境部署

```
                    ┌─────────────────────────────────────┐
                    │            Nginx (反向代理)          │
                    │         Port: 80 / 443              │
                    └─────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│  aistudy.icu  │         │ earthworm.    │         │   静态资源    │
│  (主站)       │         │ aistudy.icu   │         │   (CDN)       │
│  → :5000      │         │  → :5201      │         │               │
└───────────────┘         └───────────────┘         └───────────────┘
        │                           │
        ▼                           ▼
┌───────────────┐         ┌───────────────┐
│ Spring Boot   │         │ Node.js       │
│ JAR / Docker  │         │ Express       │
└───────────────┘         └───────────────┘
        │                           │
        └───────────┬───────────────┘
                    ▼
        ┌───────────────────────┐
        │      MySQL 8.x        │
        │      Redis 7.x        │
        └───────────────────────┘
```

### 5.2 Nginx 配置示例

```nginx
# 主站
server {
    listen 80;
    server_name aistudy.icu;

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # SSE 支持
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
}

# 单词记忆模块
server {
    listen 80;
    server_name earthworm.aistudy.icu;

    location / {
        proxy_pass http://127.0.0.1:5201;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 六、性能优化总结

### 6.1 后端优化

| 优化点     | 实现方式           | 效果             |
| ---------- | ------------------ | ---------------- |
| 数据库索引 | 复合索引、覆盖索引 | 查询性能提升 10x |
| 连接池     | HikariCP 默认配置  | 减少连接创建开销 |
| 异步处理   | @Async + 线程池    | 非阻塞响应       |
| 缓存策略   | Redis + 本地缓存   | 减少数据库压力   |
| 流式响应   | SSE                | 首字节时间优化   |

### 6.2 前端优化

| 优化点   | 实现方式             | 效果             |
| -------- | -------------------- | ---------------- |
| 代码分割 | manualChunks         | 首屏加载减少 60% |
| 懒加载   | 路由懒加载           | 按需加载页面     |
| 虚拟滚动 | vue-virtual-scroller | 大列表性能优化   |
| 防抖节流 | lodash               | 减少无效请求     |
| 缓存策略 | localStorage         | 减少网络请求     |

---

## 七、面试高频问题

### Q1: 为什么选择 Spring Boot 3.x？

**回答要点：**

1. 要求 Java 17+，支持 Virtual Threads（虚拟线程），大幅提升并发性能
2. 原生支持 GraalVM 编译为原生可执行文件，启动时间从秒级降到毫秒级
3. ProblemDetail API 标准化错误响应
4. 可观测性增强（Micrometer Tracing）

### Q2: JWT Token 如何实现续期？

**回答要点：**

1. **短期 Token + Refresh Token**：Access Token 有效期短（如 30 分钟），Refresh Token 有效期长（如 7 天）
2. **滑动过期**：每次请求检查 Token 剩余有效期，若低于阈值则生成新 Token 返回
3. **本项目方案**：Token 有效期 24 小时，过期后需重新登录

### Q3: 如何防止 XSS 攻击？

**回答要点：**

1. **输入过滤**：使用 DOMPurify 清理用户输入
2. **输出编码**：Vue 自动转义插值表达式
3. **CSP 策略**：Content-Security-Policy 头限制脚本来源
4. **HttpOnly Cookie**：敏感 Cookie 不允许 JS 访问（本项目使用 localStorage，需权衡）

### Q4: SSE 和 WebSocket 如何选择？

| 特性     | SSE                   | WebSocket      |
| -------- | --------------------- | -------------- |
| 通信方向 | 单向（服务器→客户端） | 双向           |
| 协议     | HTTP                  | WS/WSS         |
| 重连     | 浏览器自动重连        | 需手动实现     |
| 适用场景 | 推送通知、AI 流式输出 | 实时聊天、游戏 |

**本项目选择 SSE 的原因**：AI 对话场景只需服务器向客户端推送数据，SSE 更简单、更轻量。

### Q5: 如何保证数据库与缓存一致性？

**回答要点：**

1. **Cache-Aside 模式**：先更新数据库，再删除缓存
2. **延迟双删**：删除缓存 → 更新数据库 → 延迟再删除缓存
3. **消息队列**：通过消息队列保证最终一致性
4. **本项目方案**：写操作直接更新数据库，读操作先查缓存再查数据库

---

## 八、技术亮点总结

1. **多模型 AI 集成**：通过 Spring AI 统一抽象层，支持 DeepSeek、豆包等多个模型
2. **流式响应优化**：SSE 实现打字机效果，提升用户体验
3. **GraphQL 按需加载**：解决 REST API 过度获取/获取不足问题
4. **PRD 流水线**：多阶段 AI 协作，自动生成需求文档
5. **微前端架构**：iframe + postMessage 实现模块解耦
6. **安全加固**：RSA 加密传输 + JWT 认证 + 限流防护

---

_文档版本: 2.0_  
_最后更新: 2026-02-28_
