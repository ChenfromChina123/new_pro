# AI智能学习助手系统 - Spring Boot版本

这是从Python FastAPI转换的Spring Boot版本，保持所有原有功能。

## 📋 项目概述

基于Spring Boot 3.2的AI智能学习助手系统，提供AI问答、云盘管理、语言学习等功能。

## 🛠️ 技术栈

- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0+ with JPA/Hibernate
- **安全**: Spring Security + JWT
- **构建工具**: Maven
- **Java版本**: 17+

## 📦 依赖说明

已在`pom.xml`中配置所有必要依赖：
- Spring Boot Web
- Spring Data JPA
- Spring Security
- MySQL Connector
- JWT (jjwt)
- Lombok
- Apache Commons
- OkHttp (用于AI API调用)
- Mail Support

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置

创建数据库：
```sql
CREATE DATABASE IF NOT EXISTS ipv6_education
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. 配置application.yml

修改 `src/main/resources/application.yml` 中的配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ipv6_education
    username: your_username
    password: your_password
  
jwt:
  secret: your_jwt_secret_key

ai:
  deepseek:
    api-key: your_deepseek_key
  doubao:
    api-key: your_doubao_key
```

### 4. 构建和运行

```bash
# 编译项目
mvn clean package

# 运行应用
mvn spring-boot:run

# 或直接运行jar
java -jar target/ai-tutor-1.0.0.jar
```

应用将在 `http://localhost:5000` 启动

## 📖 API端点映射

### 认证相关 (AuthController)

| Python端点 | Spring Boot端点 | 方法 | 说明 |
|-----------|----------------|------|------|
| `/api/register/email` | `/api/auth/register/send-code` | POST | 发送注册验证码 |
| `/api/register` | `/api/auth/register` | POST | 用户注册 |
| `/api/login` | `/api/auth/login` | POST | 用户登录 |
| `/api/forgot-password/email` | `/api/auth/forgot-password/send-code` | POST | 发送重置验证码 |
| `/api/forgot-password` | `/api/auth/forgot-password` | POST | 重置密码 |
| `/api/delete-account` | `/api/auth/delete-account` | DELETE | 删除账户 |

### AI问答 (ChatController)

| Python端点 | Spring Boot端点 | 方法 | 说明 |
|-----------|----------------|------|------|
| `/api/ask-stream` | `/api/chat/ask-stream` | POST | 流式AI问答 |

### 聊天记录 (ChatRecordController)

| Python端点 | Spring Boot端点 | 方法 | 说明 |
|-----------|----------------|------|------|
| `/api/chat-records/save` | `/api/chat-records/save` | POST | 保存聊天记录 |
| `/api/chat-records/sessions` | `/api/chat-records/sessions` | GET | 获取会话列表 |
| `/api/chat-records/session/{id}` | `/api/chat-records/session/{id}` | GET | 获取会话消息 |
| `/api/chat-records/new-session` | `/api/chat-records/new-session` | POST | 创建新会话 |

### 云盘管理 (CloudDiskController)

| Python端点 | Spring Boot端点 | 方法 | 说明 |
|-----------|----------------|------|------|
| `/api/cloud_disk/upload` | `/api/cloud-disk/upload` | POST | 上传文件 |
| `/api/cloud_disk/files` | `/api/cloud-disk/files` | GET | 获取文件列表 |
| `/api/cloud_disk/download/{id}` | `/api/cloud-disk/download/{id}` | GET | 下载文件 |
| `/api/cloud_disk/delete/{id}` | `/api/cloud-disk/delete/{id}` | DELETE | 删除文件 |
| `/api/cloud_disk/folders` | `/api/cloud-disk/folders` | GET | 获取文件夹树 |
| `/api/cloud_disk/create-folder` | `/api/cloud-disk/create-folder` | POST | 创建文件夹 |

### 语言学习 (LanguageLearningController)

| Python端点 | Spring Boot端点 | 方法 | 说明 |
|-----------|----------------|------|------|
| `/api/language/vocabulary-lists` | `/api/language/vocabulary-lists` | GET/POST | 单词表管理 |
| `/api/language/words` | `/api/language/words` | POST | 添加单词 |
| `/api/language/generate-article` | `/api/language/generate-article` | POST | AI生成文章 |

## 📁 项目结构

```
aispring/
├── src/main/java/com/aispring/
│   ├── AiTutorApplication.java      # 主应用类
│   ├── entity/                       # 实体类(对应Python models)
│   │   ├── User.java
│   │   ├── Admin.java
│   │   ├── VerificationCode.java
│   │   ├── ChatRecord.java
│   │   ├── UserFile.java
│   │   ├── UserFolder.java
│   │   ├── VocabularyList.java
│   │   └── ...
│   ├── repository/                   # 数据访问层
│   │   ├── UserRepository.java
│   │   ├── VerificationCodeRepository.java
│   │   └── ...
│   ├── service/                      # 业务逻辑层(对应Python services)
│   │   ├── AuthService.java
│   │   ├── ChatService.java
│   │   ├── FileService.java
│   │   └── ...
│   ├── controller/                   # 控制器层(对应Python routers)
│   │   ├── AuthController.java
│   │   ├── ChatController.java
│   │   ├── CloudDiskController.java
│   │   └── ...
│   ├── dto/                          # 数据传输对象(对应Python schemas)
│   │   ├── request/
│   │   └── response/
│   ├── config/                       # 配置类
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   ├── JwtConfig.java
│   │   └── FileConfig.java
│   ├── security/                     # 安全相关
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   └── UserDetailsServiceImpl.java
│   ├── util/                         # 工具类(对应Python utils)
│   │   ├── JwtUtil.java
│   │   ├── EmailUtil.java
│   │   ├── FileUtil.java
│   │   └── ...
│   └── exception/                    # 异常处理
│       ├── GlobalExceptionHandler.java
│       └── CustomException.java
├── src/main/resources/
│   ├── application.yml               # 主配置文件
│   └── application-dev.yml           # 开发环境配置
├── pom.xml                          # Maven配置
└── README.md                        # 本文件
```

## 🤖 AI Agent 系统改进（参考 void-main）

### 改进概述

参考 void-main 项目的 AI Agent 实现，对 aispring 项目的 Agent 系统进行了深入改进：

#### 1. Agent 循环结构优化
- **清晰的状态管理**：实现了 idle → LLM → tool → idle 的状态流转
- **重试机制**：LLM 请求失败时自动重试（最多 3 次）
- **循环控制**：使用 `shouldSendAnotherMessage` 标志控制循环，更符合 void-main 的设计
- **最大循环次数**：从 20 次增加到 50 次，支持更复杂的任务

#### 2. 工具批准机制增强
- **自动批准**：根据用户设置自动批准工具调用（参考 void-main 的 `autoApprove` 配置）
- **批准审计**：即使自动批准也会创建批准记录，用于审计
- **工具分类**：支持危险工具、读文件工具、文件编辑工具、MCP 工具的分类管理

#### 3. 错误处理和重试机制
- **LLM 请求重试**：失败时自动重试，延迟 1 秒
- **错误消息**：重试失败后发送清晰的错误消息给前端
- **状态恢复**：错误后正确恢复状态，避免状态不一致

#### 4. 状态管理改进
- **StreamState 增强**：支持 idle、streamingLLM、runningTool、awaitingUser 四种状态
- **状态持久化**：每次状态变更都保存到数据库
- **中断机制**：统一的中断检查点，支持各状态下的中断

#### 5. 工具调用流程优化
- **参数验证**：在工具执行前验证参数，失败时返回错误信息
- **工具结果处理**：无论成功或失败都继续循环，让 LLM 根据结果决定下一步
- **状态更新**：工具执行过程中正确更新 StreamState

### 关键改进点对比

| 特性 | 改进前 | 改进后（参考 void-main） |
|------|--------|------------------------|
| 循环结构 | 简单的 while 循环 | 嵌套循环（主循环 + 重试循环） |
| 状态管理 | 基础状态 | 完整的 StreamState 状态机 |
| 错误处理 | 简单异常捕获 | 重试机制 + 错误恢复 |
| 工具批准 | 仅手动批准 | 自动批准 + 手动批准 |
| 中断机制 | 基础中断检查 | 统一中断接口，各状态支持 |

### 配置说明

Agent 循环相关配置（在代码中定义）：
- `CHAT_RETRIES = 3`：LLM 请求最大重试次数
- `RETRY_DELAY_MS = 1000`：重试延迟（毫秒）
- `MAX_AGENT_LOOPS = 50`：Agent 循环最大次数

工具批准配置（通过 API 配置）：
- `autoApproveDangerousTools`：自动批准危险工具
- `autoApproveReadFile`：自动批准读文件工具
- `autoApproveFileEdits`：自动批准文件编辑工具
- `autoApproveMcpTools`：自动批准 MCP 工具

## 🔑 核心功能实现

### 1. JWT认证

```java
// JwtUtil.java - JWT工具类
public class JwtUtil {
    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
}
```

### 2. 文件编码自动识别

系统实现了智能文件读取逻辑，能够自动识别多种编码格式，解决跨平台（Windows/Linux）文件读取乱码问题：
- **BOM检测**: 支持 UTF-8、UTF-16LE、UTF-16BE 的 BOM 头识别。
- **多编码尝试**: 依次尝试 UTF-8 -> GBK -> UTF-16LE -> 强制 UTF-8 降级读取。
- **服务对齐**: `CloudDiskService` 与 `TerminalService` 共享相同的编码识别逻辑，确保全系统文件查看一致性。

### 3. 管理员高级权限

为 `CloudDiskService` 增加了管理员专用方法：
- `getFileContentAdmin(Long fileId)`: 允许管理员跨用户读取物理文件内容。
- `updateFileContentAdmin(Long fileId, String content)`: 允许管理员直接编辑并同步更新用户文件。
- **安全性**: 所有管理员接口均受 `@PreAuthorize("hasRole('ADMIN')")` 保护。

### 4. 终端助手 UI 优化

系统对终端助手（Terminal Assistant）进行了全面的 UI 升级，使其与其他 AI 组件保持高度一致：
- **布局一致性**: 统一了聊天面板头部（Header）样式，与右侧任务面板完美对齐。
- **视觉体验升级**: 
  - 重新设计了聊天气泡，采用更柔和的圆角和阴影效果。
  - 区分了用户和 AI 的气泡风格，增强了对话的可读性。
  - 优化了思考过程（Thought Block）和工具调用（Tool Call）的展示样式。
- **交互细节**:
  - 改进了底部输入区域，增加了悬浮阴影和聚焦动画。
  - **组件整合**: 将模式选择器（自主操作/信息收集/普通对话）整合进输入框内部，与模型选择器并排显示，统一了视觉风格并节省了空间。
  - 统一了模型选择器和发送按钮的视觉风格。
  - 响应式适配，确保在不同屏幕尺寸下均有良好的操作体验。
- **代码规范**: 同步修复了 Vue 组件中的所有 Linter 警告，移除了冗余代码，增强了类型安全。

### 2. 文件上传

```java
// FileService.java
@Service
public class FileService {
    public UserFile uploadFile(MultipartFile file, Long userId, String folderPath) {
        // 1. 验证文件
        // 2. 生成唯一文件名
        // 3. 保存文件
        // 4. 创建数据库记录
    }
}
```

### 3. 流式AI回复

```java
// ChatService.java
@Service
public class ChatService {
    public SseEmitter streamChat(String prompt, String model) {
        SseEmitter emitter = new SseEmitter();
        // 异步调用AI API并流式返回
        return emitter;
    }
}
```

## 🔐 安全配置

Spring Security配置：
- 所有API端点需要认证（除了登录、注册）
- 使用JWT Token进行认证
- CORS配置支持前端跨域请求

## 📝 开发指南

### 添加新的API端点

1. 在`entity`包中创建实体类
2. 在`repository`包中创建Repository接口
3. 在`service`包中创建Service类
4. 在`controller`包中创建Controller
5. 在`dto`包中创建请求/响应DTO

### 数据库迁移

使用JPA自动更新表结构（开发环境）：
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

生产环境建议使用Flyway或Liquibase进行版本控制。

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=UserServiceTest
```

## 📦 部署

### 打包

```bash
mvn clean package -DskipTests
```

### Docker部署

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/ai-tutor-1.0.0.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 🔧 环境变量

支持通过环境变量配置：

```bash
# 数据库
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ipv6_education
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=password

# JWT
export JWT_SECRET=your_secret_key

# AI API
export AI_DEEPSEEK_API_KEY=your_key
export AI_DOUBAO_API_KEY=your_key

# 文件存储
export FILE_UPLOAD_DIR=/path/to/uploads
export FILE_CLOUD_DISK_DIR=/path/to/cloud_disk
```

## 📊 与Python版本的对比

| 特性 | Python FastAPI | Spring Boot |
|------|---------------|-------------|
| 启动速度 | 快 | 较慢（首次） |
| 内存占用 | 低 | 较高 |
| 类型安全 | 部分（Pydantic） | 完全（Java） |
| 异步支持 | 原生支持 | @Async |
| 文档生成 | Swagger自动 | 需配置 |
| 生态系统 | 较新 | 成熟 |
| 企业采用 | 增长中 | 广泛 |

## 🐛 常见问题

### Q: 如何启用Swagger文档？
A: 添加springdoc-openapi依赖并访问`/swagger-ui.html`

### Q: 文件上传大小限制？
A: 在application.yml中配置`spring.servlet.multipart.max-file-size`

### Q: 如何连接到PostgreSQL？
A: 修改datasource配置并添加PostgreSQL驱动依赖

## 📚 参考资料

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA文档](https://spring.io/projects/spring-data-jpa)
- [Spring Security文档](https://spring.io/projects/spring-security)

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

---

**开发团队**: AI Spring Team
**版本**: 1.0.0
**最后更新**: 2024年12月

