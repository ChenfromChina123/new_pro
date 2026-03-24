# Piston 代码执行沙箱 — 实现方案

## Context

项目是一个 AI 智能学习导师系统，已有 Monaco Editor 代码编辑器，但缺少**代码运行能力**。
学生编写代码后无法在平台内执行验证，只能复制到本地运行。
本方案集成 Piston 代码执行引擎（Docker 沙箱），新建 `/playground` 页面，
实现"编写代码 → 一键运行 → 查看输出"的完整闭环。

---

## 架构

```
浏览器 PlaygroundView.vue (Monaco Editor + 语言选择 + 运行按钮)
         │ POST /api/playground/execute (JWT Auth)
         ▼
Spring Boot (:5000) PlaygroundController → CodeExecutionService
         │ 速率限制 (Redis) + 输入校验
         ▼
Piston Engine (:2000) Docker 隔离沙箱执行
```

---

## 第一步：Piston Docker 部署

### 新建 `piston-service/docker-compose.yml`

```yaml
services:
  piston:
    image: ghcr.io/engineer-man/piston
    container_name: piston-engine
    restart: unless-stopped
    ports:
      - "127.0.0.1:2000:2000"   # 仅本地访问
    volumes:
      - piston-packages:/piston/packages
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
    tmpfs:
      - /piston/jobs:exec,size=256M
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:2000/api/v2/runtimes"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  piston-packages:
```

### 新建 `piston-service/install-runtimes.sh`

启动后安装语言运行时（Python, JavaScript/Node, Java, C, C++）：
```bash
#!/bin/bash
# 等待 Piston 启动
sleep 5
# 安装运行时
for pkg in python node java-jdk c-gcc c++-gcc; do
  curl -s "http://localhost:2000/api/v2/packages" | ...
  # 或 docker exec piston-engine /piston/packages/install ...
done
```

**启动与验证：**
```bash
cd piston-service && docker compose up -d
# 验证
curl http://localhost:2000/api/v2/runtimes
```

---

## 第二步：后端实现

### 2.1 修改 `aispring/src/main/resources/application.yml`

在文件末尾追加：

```yaml
piston:
  api-url: ${PISTON_API_URL:http://localhost:2000}
  execute-timeout: 10000
  max-code-length: 65536
  max-stdin-length: 8192
  rate-limit:
    max-requests: 30
    window-hours: 1
```

### 2.2 新建 `config/PistonProperties.java`

```java
@Configuration
@ConfigurationProperties(prefix = "piston")
@Data
public class PistonProperties {
    private String apiUrl = "http://localhost:2000";
    private int executeTimeout = 10000;
    private int maxCodeLength = 65536;
    private int maxStdinLength = 8192;
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private int maxRequests = 30;
        private int windowHours = 1;
    }
}
```

### 2.3 新建 DTO

**`dto/request/CodeExecutionRequest.java`**
```java
@Data
public class CodeExecutionRequest {
    @NotBlank private String language;
    private String version;      // 可选
    @NotBlank @Size(max = 65536) private String code;
    @Size(max = 8192) private String stdin;
}
```

**`dto/response/CodeExecutionResponse.java`**
```java
@Data
public class CodeExecutionResponse {
    private String language;
    private String version;
    private RunResult compile;  // 可空
    private RunResult run;

    @Data
    public static class RunResult {
        private String stdout;
        private String stderr;
        private int code;
        private String signal;
        private String output;
    }
}
```

### 2.4 新建 `service/CodeExecutionService.java` (接口)

```java
public interface CodeExecutionService {
    CodeExecutionResponse execute(CodeExecutionRequest request);
    List<Map<String, Object>> getAvailableRuntimes();
    boolean isAvailable();
}
```

### 2.5 新建 `service/impl/CodeExecutionServiceImpl.java`

核心逻辑：
1. 校验 code 和 stdin 长度
2. 构造 Piston API 请求体：
   ```json
   {
     "language": "python",
     "version": "*",
     "files": [{"name": "main.py", "content": "<code>"}],
     "stdin": "<stdin>",
     "run_timeout": 10000,
     "compile_timeout": 10000,
     "run_memory_limit": 128000000
   }
   ```
3. 使用已有的 `RestTemplate` Bean POST 到 `{piston.api-url}/api/v2/execute`
4. 解析响应，stdout/stderr 超 100KB 时截断
5. 获取运行时列表：GET `{piston.api-url}/api/v2/runtimes`

### 2.6 扩展 `service/RateLimitService.java`

在现有类中新增一个通用重载方法（不影响原有 `checkAndIncrement(String ip)` 方法）：

```java
public boolean checkAndIncrement(String keyPrefix, String identifier, int maxRequests, Duration window) {
    String key = keyPrefix + identifier;
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1) {
        redisTemplate.expire(key, window);
    }
    return count == null || count <= maxRequests;
}

public int getRemainingRequests(String keyPrefix, String identifier, int maxRequests) {
    String key = keyPrefix + identifier;
    String val = redisTemplate.opsForValue().get(key);
    if (val == null) return maxRequests;
    return Math.max(0, maxRequests - Integer.parseInt(val));
}
```

### 2.7 新建 `controller/PlaygroundController.java`

三个端点（遵循 OcrController 的编码风格）：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/playground/execute` | 执行代码，需认证+限流 |
| GET | `/api/playground/runtimes` | 获取可用语言列表 |
| GET | `/api/playground/health` | Piston 健康检查 |

execute 端点流程：
1. `@Valid` 校验 `CodeExecutionRequest`
2. 从 `@AuthenticationPrincipal` 获取 userId
3. `rateLimitService.checkAndIncrement("playground_limit:", userId, 30, Duration.ofHours(1))`
4. 限流超出 → 返回 HTTP 429
5. 调用 `codeExecutionService.execute(request)`
6. 返回 `ResponseEntity.ok(result)`

---

## 第三步：前端实现

### 3.1 修改 `vue-app/src/config/api.js`

在 `API_ENDPOINTS` 中 `ocr` 后面新增：

```js
playground: {
  execute: '/api/playground/execute',
  runtimes: '/api/playground/runtimes',
  health: '/api/playground/health',
}
```

### 3.2 修改 `vue-app/src/router/index.js`

在 AppLayout children 中（`editor` 路由后面）新增：

```js
{
  path: 'playground',
  name: 'Playground',
  component: () => import('@/views/PlaygroundView.vue'),
  meta: { requiresAuth: true }
}
```

### 3.3 修改 `vue-app/src/components/Sidebar/AppSidebar.vue`

在"运维工具"导航项前面添加：

```html
<router-link
  v-if="authStore.isAuthenticated"
  to="/playground"
  class="nav-item"
  active-class="active"
>
  <i class="fas fa-play-circle" />
  <span>代码运行</span>
</router-link>
```

### 3.4 新建 `vue-app/src/views/PlaygroundView.vue`

**页面布局：**

```
┌─────────────────────────────────────────────────────┐
│ 顶部工具栏                                           │
│ [语言选择 ▼] [版本] [▶ 运行 Ctrl+Enter] [状态信息]    │
├──────────────────────────┬──────────────────────────┤
│                          │ 输出面板                   │
│   Monaco Editor          │ ┌──────────────────────┐ │
│   (代码编辑区)            │ │ stdout (正常色)       │ │
│                          │ │ stderr (红色)         │ │
│                          │ │ 退出码 / 执行时间      │ │
│                          │ ├──────────────────────┤ │
│                          │ │ stdin 输入 (textarea) │ │
│                          │ └──────────────────────┘ │
└──────────────────────────┴──────────────────────────┘
│ 底部状态栏: 语言 | 版本 | 剩余执行次数: 28/30        │
└─────────────────────────────────────────────────────┘
```

**核心实现要点：**

- 直接使用 `monaco-editor` 包创建独立编辑器实例（不复用 EditorStore 耦合的 MonacoEditor.vue）
- `onMounted` 时调用 `/api/playground/runtimes` 获取可用语言列表
- 语言切换时自动填充 Hello World 模板代码 + 更新 Monaco language mode
- 暗色模式跟随 `themeStore.isDarkMode`：编辑器 theme 在 `vs-dark` / `vs` 间切换
- `Ctrl+Enter` / `Cmd+Enter` 快捷键触发运行
- 运行时禁用按钮，显示 loading 动画
- 输出面板区分 stdout（正常色）和 stderr（红色），显示退出码和执行耗时
- 429 错误时友好提示"执行次数已达上限，请稍后再试"
- 使用 `splitpanes`（已安装的依赖）实现编辑器和输出面板的可拖拽分割

**内置语言模板（常量对象）：**
- python → `print("Hello, World!")`
- javascript → `console.log("Hello, World!");`
- java → `public class Main { public static void main(String[] args) { System.out.println("Hello, World!"); } }`
- c → `#include <stdio.h>\nint main() { printf("Hello, World!\\n"); return 0; }`
- c++ → `#include <iostream>\nint main() { std::cout << "Hello, World!" << std::endl; return 0; }`

**CSS 样式：** 遵循项目已有的 CSS 变量体系 `--bg-primary`, `--bg-secondary`, `--text-primary`, `--border-color` 等。

---

## 文件清单

### 新建文件 (8 个)

| 文件 | 说明 |
|------|------|
| `piston-service/docker-compose.yml` | Piston Docker 部署 |
| `piston-service/install-runtimes.sh` | 语言运行时安装脚本 |
| `aispring/.../config/PistonProperties.java` | 配置属性类 |
| `aispring/.../dto/request/CodeExecutionRequest.java` | 请求 DTO |
| `aispring/.../dto/response/CodeExecutionResponse.java` | 响应 DTO（含 RunResult 内部类） |
| `aispring/.../service/CodeExecutionService.java` | 服务接口 |
| `aispring/.../service/impl/CodeExecutionServiceImpl.java` | 服务实现 |
| `aispring/.../controller/PlaygroundController.java` | REST Controller |
| `vue-app/src/views/PlaygroundView.vue` | 前端页面 |

### 修改文件 (4 个)

| 文件 | 改动 |
|------|------|
| `aispring/src/main/resources/application.yml` | 追加 `piston:` 配置块 |
| `aispring/.../service/RateLimitService.java` | 新增通用限流重载方法 |
| `vue-app/src/router/index.js` | 新增 `/playground` 路由 |
| `vue-app/src/config/api.js` | 新增 `playground` 端点分组 |
| `vue-app/src/components/Sidebar/AppSidebar.vue` | 导航栏新增"代码运行"入口 |

---

## 安全防护

| 层级 | 措施 |
|------|------|
| 认证 | JWT `@PreAuthorize("isAuthenticated()")` |
| 限流 | Redis 每用户 30 次/小时 |
| 输入校验 | 代码 ≤ 64KB, stdin ≤ 8KB |
| 执行沙箱 | Piston: 超时 10s, 内存 128MB, 进程数限制 |
| 容器隔离 | Docker: CPU 2核, 内存 2G, 端口仅 127.0.0.1 |
| 输出截断 | stdout/stderr 超 100KB 时截断 |

---

## 验证测试

### 基础设施验证
```bash
# 1. 启动 Piston
cd piston-service && docker compose up -d

# 2. 验证 Piston API
curl http://localhost:2000/api/v2/runtimes

# 3. 安装运行时后直接测试执行
curl -X POST http://localhost:2000/api/v2/execute \
  -H "Content-Type: application/json" \
  -d '{"language":"python","version":"*","files":[{"content":"print(42)"}]}'
```

### 后端验证
```bash
# 启动 Spring Boot 后端
cd aispring && mvn spring-boot:run

# 获取测试 token（项目已有 /api/auth/test/token 端点）
TOKEN=$(curl -s -X POST http://localhost:5000/api/auth/test/token | jq -r '.data.token')

# 测试健康检查
curl -H "Authorization: Bearer $TOKEN" http://localhost:5000/api/playground/health

# 测试获取运行时列表
curl -H "Authorization: Bearer $TOKEN" http://localhost:5000/api/playground/runtimes

# 测试代码执行
curl -X POST http://localhost:5000/api/playground/execute \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"language":"python","code":"print(\"Hello from Piston!\")","stdin":""}'
```

### 前端验证
```bash
cd vue-app && npm run dev
```
1. 登录后点击侧边栏"代码运行"进入 /playground
2. 选择 Python → 点击运行 → 确认输出 "Hello, World!"
3. 切换 JavaScript / Java / C / C++ 分别测试
4. 测试 stdin 输入（Python: `input()` → 输入面板填值 → 运行）
5. 测试 Ctrl+Enter 快捷键
6. 测试暗色/亮色模式切换
7. 测试错误代码的 stderr 展示
8. 测试超时代码（`while True: pass`）确认 10s 后返回超时错误

### 安全边界测试
- 连续执行 31 次，确认第 31 次返回 429
- 提交超过 64KB 的代码，确认返回 400
- 提交无限循环代码，确认 10s 超时
