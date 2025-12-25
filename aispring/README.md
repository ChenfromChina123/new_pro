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

### AI问答 (ChatController & AiChatController)

| Python端点 | Spring Boot端点 | 方法 | 说明 |
|-----------|----------------|------|------|
| `/api/ask-stream` | `/api/ask-stream` | POST | 流式AI问答 |
| `/api/ask` | `/api/ask` | POST | 非流式AI问答 |

## 🔄 最新修改

### UI 优化
- **模式选择器**: 移除了前端页面中的模式选择器 UI，统一使用功能选择器和模型选择器。
- **样式统一**: 确保所有选择器组件（功能、模型）在输入栏中样式一致。

### AI 工具调用与类型对齐
- **统一 UserId 类型**: 将 `ChatSession`、`ChatRecord` 实体类以及所有相关 Controller、Service 和 Repository 中的 `user_id` 从 `String` 统一修改为 `Long`。
- **解决外键约束错误**: 修复了由于 `user_id` 类型不一致导致的数据库外键约束冲突问题。
- **Linter 错误修复**: 解决了 16 处由于 `user_id` 类型变更引起的 Java 编译错误。
- **工具调用稳定性**: 确保 AI 在调用本地工具（如文件读写、代码执行）时，用户 ID 能够正确传递并匹配数据库记录。
- **Git 冲突处理**: 解决了 `git revert` 过程中在 `VocabularyService.java` 中产生的 PDF 字体注册逻辑冲突，保留了反射加载机制以增强环境兼容性。
- **代码清理**: 移除了 `ToolCallParser.java` 等文件中的未使用导入和字段。

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
│   │   ├── FileConfig.java
│   │   └── ...
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
│   ├── application-dev.yml           # 开发环境配置
│   └── application-prod.yml          # 生产环境配置
├── pom.xml                          # Maven配置
└── README.md                        # 本文件
```

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
- **服务对齐**: `CloudDiskService` 内部共享相同的编码识别逻辑，确保全系统文件查看一致性。

### 3. 管理员高级权限

为 `CloudDiskService` 增加了管理员专用方法：
- `getFileContentAdmin(Long fileId)`: 允许管理员跨用户读取物理文件内容。
- `updateFileContentAdmin(Long fileId, String content)`: 允许管理员直接编辑并同步更新用户文件。
- **安全性**: 所有管理员接口均受 `@PreAuthorize("hasRole('ADMIN')")` 保护。

### 4. 文件上传

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

### 5. 流式AI回复

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
**最后更新**: 2025年12月
