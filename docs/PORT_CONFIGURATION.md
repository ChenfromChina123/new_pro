# 端口配置说明

**更新时间**: 2026-03-02  
**状态**: ✅ 已完成

---

## 📊 端口分配总览

| 服务         | 端口 | 域名                  | 说明                 |
| ------------ | ---- | --------------------- | -------------------- |
| **主站前端** | 3000 | aistudy.icu           | Vue 3 主站前端应用   |
| **简历网站** | 3100 | cv.aistudy.icu        | 个人简历展示网站     |
| **博客系统** | 3200 | blog.aistudy.icu      | Next.js 博客系统     |
| **主站后端** | 5000 | -                     | Spring Boot 后端 API |
| **单词记忆** | 5010 | earthworm.aistudy.icu | Word Game 单词消消乐 |

---

## 🎯 端口分配原则

### 1. **分类明确**

- **3xxx** - 前端应用（3000, 3100, 3200）
- **5xxx** - 后端服务（5000, 5010）

### 2. **避免冲突**

- 各服务端口独立，互不干扰
- 与常见默认端口错开（如 80, 443, 8080, 3306 等）

### 3. **易于记忆**

- 主站前端：3000（常见开发端口）
- 简历网站：3100（前端段 +100）
- 博客系统：3200（前端段 +200）
- 主站后端：5000（常见 API 端口）
- 单词记忆：5010（后端 +10）

---

## 🔧 配置位置

### 1. **主站前端 (3000)**

**文件**: `start_all_linux.sh`, `optimized_start_frontend.bat`

```bash
FRONTEND_PORT=3000
```

**Nginx 配置**:

```nginx
server {
    listen 80;
    server_name aistudy.icu;

    location / {
        proxy_pass http://127.0.0.1:3000;
    }
}
```

---

### 4. **主站后端 (5000)**

**文件**: `aispring/src/main/resources/application.yml`

```yaml
server:
  port: 5000
```

**启动脚本**: `start_all_linux.sh`

```bash
BACKEND_PORT=5000
```

---

### 3. **简历网站 (3100)** ✨ 新增

**文件**: `cv-site/` (静态 HTML 文件)

**启动脚本**: `start_all_linux.sh`, `start-cv-site.sh`, `start-cv-site.bat`

```bash
CV_PORT=3100
```

**启动方式**: 使用 Python 内置 HTTP 服务器

```bash
cd cv-site
python3 -m http.server 3100
```

**Nginx 配置** (`cv-site/nginx.conf`):

```nginx
server {
    listen 80;
    server_name cv.aistudy.icu;

    location / {
        proxy_pass http://127.0.0.1:3100;
    }
}
```

---

### 5. **单词记忆 (5010)**

**文件**: `word-game/server/index.js`

```javascript
const PORT = process.env.PORT || 5010;
```

**启动脚本**: `start_all_linux.sh`

```bash
WORD_GAME_PORT=5010
```

**Nginx 配置**:

```nginx
server {
    listen 80;
    server_name earthworm.aistudy.icu;

    location / {
        proxy_pass http://127.0.0.1:5010;
    }
}
```

---

### 6. **博客系统 (3200)** ✨ 新增

**文件**:

- `个人博客/tailwind-nextjs-starter-blog-main/package.json`
- `个人博客/tailwind-nextjs-starter-blog-main/nginx.conf`

**package.json**:

```json
{
  "scripts": {
    "dev": "cross-env INIT_CWD=$PWD PORT=3200 next dev",
    "start": "cross-env PORT=3200 next dev",
    "serve": "cross-env PORT=3200 next start"
  }
}
```

**启动脚本**: `start_all_linux.sh`, `start-blog.sh`, `start-blog.bat`

```bash
BLOG_PORT=3200
```

**Nginx 配置** (`个人博客/tailwind-nextjs-starter-blog-main/nginx.conf`):

```nginx
upstream blog_backend {
    server 127.0.0.1:3200;
    keepalive 32;
}

server {
    listen 80;
    server_name blog.aistudy.icu;

    location / {
        proxy_pass http://blog_backend;
    }
}
```

---

## 🚀 启动命令

### Linux 系统

```bash
# 全量启动（包含所有服务）
./start_all_linux.sh

# 单独启动简历网站
./start-cv-site.sh

# 单独启动博客系统
./start-blog.sh
```

### Windows 系统

```cmd
:: 单独启动简历网站
start-cv-site.bat

:: 单独启动博客系统
start-blog.bat

:: 启动主站前端
optimized_start_frontend.bat
```

---

## 🔍 端口检测

### Linux

```bash
# 查看所有服务端口
netstat -tlnp | grep -E '3000|3100|3200|5000|5010'

# 或使用 lsof
lsof -i :3000  # 主站前端
lsof -i :3100  # 简历网站
lsof -i :3200  # 博客系统
lsof -i :5000  # 主站后端
lsof -i :5010  # 单词记忆
```

### Windows

```cmd
:: 查看所有服务端口
netstat -ano | findstr "3000 3100 3200 5000 5010"

:: 查看特定端口
netstat -ano | findstr :3100
netstat -ano | findstr :3200
```

---

## 🛑 停止服务

### Linux

```bash
# 停止特定服务
kill $(lsof -ti:3000)  # 停止主站前端
kill $(lsof -ti:3100)  # 停止简历网站
kill $(lsof -ti:3200)  # 停止博客系统
kill $(lsof -ti:5000)  # 停止主站后端
kill $(lsof -ti:5010)  # 停止单词记忆

# 或全量停止
./stop_linux.sh
```

### Windows

```cmd
:: 停止 Node.js 服务
taskkill /F /IM node.exe

:: 停止 Java 服务
taskkill /F /IM java.exe
```

---

## 📝 日志文件

| 服务     | 日志文件        | 查看命令                |
| -------- | --------------- | ----------------------- |
| 主站前端 | `frontend.log`  | `tail -f frontend.log`  |
| 简历网站 | `cv-site.log`   | `tail -f cv-site.log`   |
| 主站后端 | `backend.log`   | `tail -f backend.log`   |
| 单词记忆 | `word-game.log` | `tail -f word-game.log` |
| 博客系统 | `blog.log`      | `tail -f blog.log`      |

---

## 🔐 防火墙配置

### Linux (UFW)

```bash
# 开放必要端口
sudo ufw allow 3000/tcp  # 主站前端
sudo ufw allow 3100/tcp  # 简历网站
sudo ufw allow 3200/tcp  # 博客系统
sudo ufw allow 5000/tcp  # 主站后端
sudo ufw allow 5010/tcp  # 单词记忆

# 查看状态
sudo ufw status
```

### Windows (防火墙)

```powershell
# 开放端口（管理员权限）
New-NetFirewallRule -DisplayName "AI Study CV Site" -Direction Inbound -LocalPort 3100 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "AI Study Blog" -Direction Inbound -LocalPort 3200 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "AI Study Frontend" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "AI Study Backend" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "Word Game" -Direction Inbound -LocalPort 5010 -Protocol TCP -Action Allow
```

---

## 🐳 Docker 部署

如果使用 Docker 部署，端口映射配置：

```yaml
version: "3.8"

services:
  frontend:
    image: aispring-frontend
    ports:
      - "3000:80"

  cv-site:
    image: cv-site
    ports:
      - "3100:80"

  backend:
    image: aispring-backend
    ports:
      - "5000:5000"

  word-game:
    image: word-game
    ports:
      - "5010:80"

  blog:
    image: blog-system
    ports:
      - "3200:3000"
```

---

## ⚠️ 常见问题

### 1. 端口被占用

**症状**: 启动时提示 "Port 3100 is already in use" 或 "Port 3200 is already in use"

**解决**:

```bash
# Linux
lsof -ti:3100 | xargs kill -9  # 停止简历网站
lsof -ti:3200 | xargs kill -9  # 停止博客系统

# Windows
netstat -ano | findstr :3100
taskkill /pid <PID> /F

netstat -ano | findstr :3200
taskkill /pid <PID> /F
```

### 2. 服务启动失败

**检查**:

- Node.js 版本是否正确（推荐 18+）
- 依赖是否完整安装
- 端口是否被占用
- 日志文件中的错误信息

### 3. Nginx 反向代理失败

**检查**:

- Nginx 配置中的端口是否正确
- 后端服务是否正常运行
- 防火墙是否开放端口

```bash
# 测试 Nginx 配置
nginx -t

# 重载 Nginx
nginx -s reload
```

---

## 📚 相关文档

- [启动脚本说明](../START_SCRIPTS_README.md)
- [Nginx 配置说明](../docs/NGINX_CONFIG.md)
- [Docker 部署指南](../个人博客/tailwind-nextjs-starter-blog-main/faq/deploy-with-docker.md)

---

**最后更新**: 2026-03-02  
**维护者**: AI Study Team
