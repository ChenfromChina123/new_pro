# Python FastAPI 到 Spring Boot 迁移指南

本文档详细说明如何将Python FastAPI项目迁移到Spring Boot。

## 📊 技术栈对比

| 组件 | Python FastAPI | Spring Boot |
|------|---------------|-------------|
| Web框架 | FastAPI | Spring MVC |
| ORM | SQLAlchemy | JPA/Hibernate |
| 依赖注入 | Depends() | @Autowired/@RequiredArgsConstructor |
| 数据验证 | Pydantic | Bean Validation (JSR-380) |
| 异步处理 | async/await | @Async/CompletableFuture |
| 密码加密 | Passlib (bcrypt) | BCryptPasswordEncoder |
| JWT | python-jose | jjwt |
| HTTP客户端 | httpx/aiohttp | OkHttp/RestTemplate |
| 邮件发送 | smtplib | JavaMailSender |

## 🔄 代码对比

### 1. 实体类定义

**Python (SQLAlchemy)**:
```python
class User(Base):
    __tablename__ = 'users'
    
    id = Column(Integer, primary_key=True)
    username = Column(String(80), unique=True, nullable=False)
    email = Column(String(120), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    created_at = Column(DateTime, default=lambda: datetime.now(UTC))
    
    def set_password(self, password: str):
        self.password_hash = pwd_context.hash(password)
```

**Java (JPA)**:
```java
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 80)
    private String username;
    
    @Column(unique = true, nullable = false, length = 120)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public void setPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        this.passwordHash = encoder.encode(password);
    }
}
```

### 2. 数据访问层

**Python (SQLAlchemy)**:
```python
# 直接使用Session
def get_user_by_email(db: Session, email: str):
    return db.query(User).filter(User.email == email).first()
```

**Java (Spring Data JPA)**:
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### 3. API路由定义

**Python (FastAPI)**:
```python
@app.post("/api/register")
async def register(
    email: str,
    password: str,
    code: str,
    db: Session = Depends(get_db)
):
    # 业务逻辑
    return {"message": "注册成功"}
```

**Java (Spring MVC)**:
```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);
        return ResponseEntity.ok(
            MessageResponse.builder()
                .message("注册成功")
                .build()
        );
    }
}
```

### 4. 依赖注入

**Python**:
```python
def register(db: Session = Depends(get_db)):
    # db会自动注入
    pass
```

**Java**:
```java
@RequiredArgsConstructor  // Lombok自动生成构造器注入
public class AuthService {
    private final UserRepository userRepository;
    // Spring自动注入
}
```

### 5. 数据验证

**Python (Pydantic)**:
```python
class RegisterRequest(BaseModel):
    email: EmailStr
    password: str
    code: str
    
    @validator('password')
    def validate_password(cls, v):
        if len(v) < 6:
            raise ValueError('密码至少6位')
        return v
```

**Java (Bean Validation)**:
```java
@Data
public class RegisterRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;
    
    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

### 6. 异常处理

**Python**:
```python
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail}
    )
```

**Java**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
        CustomException ex
    ) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .detail(ex.getMessage())
                .build());
    }
}
```

### 7. JWT Token生成

**Python**:
```python
from jose import jwt

def generate_jwt(user_id: int, email: str) -> str:
    payload = {
        'user_id': user_id,
        'email': email,
        'exp': datetime.utcnow() + timedelta(hours=2)
    }
    return jwt.encode(payload, SECRET_KEY, algorithm='HS256')
```

**Java**:
```java
public String generateToken(UserDetails userDetails) {
    return Jwts.builder()
        .setSubject(userDetails.getUsername())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
}
```

### 8. 文件上传

**Python**:
```python
@app.post("/api/cloud_disk/upload")
async def upload_file(
    file: UploadFile = File(...),
    folder: str = "",
    current_user: User = Depends(get_current_user)
):
    file_location = f"uploads/{current_user.id}/{file.filename}"
    with open(file_location, "wb") as f:
        content = await file.read()
        f.write(content)
    return {"filename": file.filename}
```

**Java**:
```java
@PostMapping("/upload")
public ResponseEntity<FileUploadResponse> uploadFile(
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "folder", defaultValue = "") String folder,
    @AuthenticationPrincipal UserDetails userDetails
) {
    String filename = fileService.saveFile(file, folder, userDetails);
    return ResponseEntity.ok(
        FileUploadResponse.builder()
            .filename(filename)
            .build()
    );
}
```

### 9. 流式响应 (SSE)

**Python**:
```python
@app.post("/api/ask-stream")
async def ask_stream(prompt: str):
    async def generate():
        for chunk in ai_client.stream_chat(prompt):
            yield f"data: {chunk}\n\n"
    
    return StreamingResponse(generate(), media_type="text/event-stream")
```

**Java**:
```java
@PostMapping("/ask-stream")
public SseEmitter streamChat(@RequestBody ChatRequest request) {
    SseEmitter emitter = new SseEmitter();
    
    CompletableFuture.runAsync(() -> {
        try {
            aiClient.streamChat(request.getPrompt(), chunk -> {
                emitter.send(SseEmitter.event()
                    .data(chunk)
                    .name("message"));
            });
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    
    return emitter;
}
```

## 📋 迁移检查清单

### 第一阶段：基础设施
- [ ] 创建Maven项目
- [ ] 配置pom.xml依赖
- [ ] 设置application.yml配置
- [ ] 配置数据库连接
- [ ] 设置日志配置

### 第二阶段：核心功能
- [ ] 创建实体类 (对应Python models)
- [ ] 创建Repository接口
- [ ] 实现Service层业务逻辑
- [ ] 创建Controller处理HTTP请求
- [ ] 创建DTO类 (对应Python schemas)

### 第三阶段：安全配置
- [ ] 配置Spring Security
- [ ] 实现JWT认证
- [ ] 创建认证过滤器
- [ ] 配置CORS

### 第四阶段：高级功能
- [ ] 文件上传下载
- [ ] 邮件发送
- [ ] AI API集成
- [ ] 流式响应

### 第五阶段：测试和优化
- [ ] 单元测试
- [ ] 集成测试
- [ ] 性能优化
- [ ] 文档完善

## 🎯 关键差异和注意事项

### 1. 类型系统
- Python: 动态类型，运行时检查
- Java: 静态类型，编译时检查
- **建议**: 充分利用Java的类型安全特性

### 2. 异步处理
- Python: async/await原生支持
- Java: 需要使用@Async或CompletableFuture
- **建议**: 对于I/O密集型操作使用异步

### 3. 数据库会话管理
- Python: 需要手动管理Session
- Java: Spring自动管理EntityManager
- **建议**: 使用@Transactional注解

### 4. 配置管理
- Python: 使用.env或config.py
- Java: 使用application.yml或@ConfigurationProperties
- **建议**: 使用Spring Boot的配置绑定功能

### 5. 错误处理
- Python: try/except + HTTPException
- Java: try/catch + @ExceptionHandler
- **建议**: 创建统一的异常处理器

### 6. 依赖管理
- Python: requirements.txt或poetry
- Java: pom.xml (Maven)
- **建议**: 使用BOM管理版本

### 7. 启动速度
- Python: 快速启动
- Java: 首次启动较慢
- **建议**: 开发时使用spring-boot-devtools

### 8. 内存使用
- Python: 相对较低
- Java: JVM开销较大
- **建议**: 合理配置JVM参数

## 🔧 常见问题及解决方案

### Q1: 如何实现Python的Depends依赖注入？
**A**: 使用Spring的@Autowired或构造器注入（推荐使用Lombok的@RequiredArgsConstructor）

### Q2: 如何处理Python的async/await？
**A**: 使用@Async注解或CompletableFuture

### Q3: 如何实现FastAPI的自动文档？
**A**: 添加springdoc-openapi依赖，自动生成Swagger文档

### Q4: Python的Pydantic BaseModel对应什么？
**A**: Java的DTO类 + Bean Validation注解

### Q5: SQLAlchemy的关系映射如何实现？
**A**: 使用JPA的@OneToMany、@ManyToOne等注解

### Q6: 如何处理Python的datetime？
**A**: 使用Java的LocalDateTime和@JsonFormat

### Q7: 流式响应如何实现？
**A**: 使用SseEmitter或ResponseBodyEmitter

### Q8: 文件上传大小限制？
**A**: 在application.yml中配置spring.servlet.multipart.max-file-size

## 📚 推荐学习资源

1. **Spring Boot官方文档**: https://spring.io/projects/spring-boot
2. **Spring Data JPA文档**: https://spring.io/projects/spring-data-jpa
3. **Spring Security文档**: https://spring.io/projects/spring-security
4. **Baeldung Spring教程**: https://www.baeldung.com/spring-boot
5. **JWT Java实现**: https://github.com/jwtk/jjwt

## 🎉 迁移完成后的优势

1. **类型安全**: 编译时捕获错误
2. **成熟生态**: 大量企业级组件
3. **性能优化**: JIT编译优化
4. **工具支持**: 优秀的IDE支持
5. **企业认可**: 广泛的企业采用
6. **长期维护**: Spring的长期支持

---

**迁移建议**: 
- 逐模块迁移，不要一次性全部迁移
- 保持API接口兼容性
- 充分测试每个迁移的模块
- 利用Java的强类型优势

祝迁移顺利！ 🚀

