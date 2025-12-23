# 服务启动指南

## 🚀 服务已启动

前后端服务已在后台启动：

### 后端服务 (Spring Boot)
- **端口**: 5000
- **地址**: http://localhost:5000
- **API 文档**: http://localhost:5000/swagger-ui.html (如果配置了 Swagger)
- **状态**: 正在启动中...

### 前端服务 (Vue 3 + Vite)
- **端口**: 3000
- **地址**: http://localhost:3000
- **代理**: 自动代理 `/api` 请求到后端 `http://localhost:5000`
- **状态**: 正在启动中...

## 📋 手动启动命令

如果服务没有自动启动，可以手动执行以下命令：

### 启动后端

```powershell
# 方式 1: 使用 Maven Wrapper (推荐)
cd aispring
.\mvnw.cmd spring-boot:run

# 方式 2: 使用系统 Maven
cd aispring
mvn spring-boot:run

# 方式 3: 使用 IDE 运行
# 直接运行 AiTutorApplication.java 的 main 方法
```

### 启动前端

```powershell
# 进入前端目录
cd vue-app

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

## 🔍 检查服务状态

### 检查后端
```powershell
# 检查端口是否被占用
netstat -ano | findstr :5000

# 或者访问健康检查端点（如果有）
curl http://localhost:5000/api/health
```

### 检查前端
```powershell
# 检查端口是否被占用
netstat -ano | findstr :3000

# 或者直接访问
curl http://localhost:3000
```

## 🛠️ 常见问题

### 1. 端口被占用

如果端口被占用，可以：

**后端**：修改 `aispring/src/main/resources/application.yml`
```yaml
server:
  port: 5001  # 改为其他端口
```

**前端**：修改 `vue-app/vite.config.js`
```javascript
server: {
  port: 3001,  // 改为其他端口
  proxy: {
    '/api': {
      target: 'http://localhost:5000',  // 确保与后端端口一致
    }
  }
}
```

### 2. 数据库连接失败

检查 `aispring/src/main/resources/application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ipv6_education?...
    username: root
    password: 123456
```

确保：
- MySQL 服务已启动
- 数据库 `ipv6_education` 已创建
- 用户名和密码正确

### 3. 前端依赖未安装

```powershell
cd vue-app
npm install
```

### 4. Maven 依赖下载失败

```powershell
cd aispring
.\mvnw.cmd clean install -U
```

## 📝 日志查看

### 后端日志
- 控制台输出
- 日志文件：`aispring/logs/application.log`

### 前端日志
- 控制台输出
- 浏览器开发者工具 (F12)

## 🎯 访问地址

启动成功后，访问：

- **前端应用**: http://localhost:3000
- **后端 API**: http://localhost:5000/api
- **API 文档**: http://localhost:5000/swagger-ui.html (如果配置)

## ⚠️ 注意事项

1. **首次启动**：后端可能需要一些时间来编译和启动
2. **数据库**：确保 MySQL 数据库已启动并配置正确
3. **端口冲突**：如果端口被占用，请修改配置或关闭占用端口的程序
4. **环境变量**：某些配置可能需要环境变量（如 API Key）

## 🔄 停止服务

### 停止后端
- 在运行后端服务的终端按 `Ctrl + C`

### 停止前端
- 在运行前端服务的终端按 `Ctrl + C`

### 或者使用 PowerShell
```powershell
# 查找并终止进程
Get-Process | Where-Object {$_.ProcessName -like "*java*"} | Stop-Process
Get-Process | Where-Object {$_.ProcessName -like "*node*"} | Stop-Process
```

