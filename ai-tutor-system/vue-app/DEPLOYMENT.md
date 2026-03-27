# 部署指南

本文档详细说明如何将Vue 3版本的智学云境 (AI LearnSphere) 系统部署到生产环境。

## 📋 部署前检查清单

- [ ] Node.js 16+已安装
- [ ] 后端API服务正常运行
- [ ] 数据库配置正确
- [ ] 环境变量已配置
- [ ] 域名和SSL证书已准备（如需HTTPS）

## 🚀 快速部署

### 方案1: 本地构建 + 静态服务器

这是最简单的部署方式，适用于大多数场景。

#### 步骤1: 构建项目

```bash
cd Aiproject8.2/vue-app
npm install
npm run build
```

构建完成后，`dist` 目录包含所有静态文件。

#### 步骤2: 部署到服务器

将 `dist` 目录上传到服务器，使用任何静态文件服务器：

**使用Nginx:**

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /var/www/ai-tutor/dist;
    index index.html;
    
    # Vue Router的history模式支持
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API代理到后端
    location /api {
        proxy_pass http://localhost:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

**使用Apache (.htaccess):**

```apache
<IfModule mod_rewrite.c>
    RewriteEngine On
    RewriteBase /
    RewriteRule ^index\.html$ - [L]
    RewriteCond %{REQUEST_FILENAME} !-f
    RewriteCond %{REQUEST_FILENAME} !-d
    RewriteRule . /index.html [L]
</IfModule>
```

#### 步骤3: 配置后端API

确保后端服务允许前端域名的CORS请求。

在后端 `py/app.py` 中：

```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://your-domain.com", "https://your-domain.com"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

### 方案2: Docker部署

#### 创建Dockerfile

```dockerfile
# 构建阶段
FROM node:20-alpine as build-stage

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine as production-stage

COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### 创建nginx.conf

```nginx
server {
    listen 80;
    server_name localhost;
    
    root /usr/share/nginx/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://backend:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

#### 创建docker-compose.yml

```yaml
version: '3.8'

services:
  frontend:
    build: ./vue-app
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - app-network

  backend:
    build: ./py
    ports:
      - "5000:5000"
    environment:
      - DATABASE_URL=mysql+pymysql://user:pass@db:3306/dbname
    depends_on:
      - db
    networks:
      - app-network

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=rootpass
      - MYSQL_DATABASE=ipv6_education
    volumes:
      - db-data:/var/lib/mysql
    networks:
      - app-network

networks:
  app-network:
    driver: bridge

volumes:
  db-data:
```

#### 启动服务

```bash
docker-compose up -d
```

### 方案3: Vercel部署（适用于前端）

Vercel是零配置部署的最佳选择。

#### 步骤1: 安装Vercel CLI

```bash
npm i -g vercel
```

#### 步骤2: 登录并部署

```bash
cd Aiproject8.2/vue-app
vercel
```

#### 步骤3: 配置环境变量

在Vercel仪表板中设置：

- `VITE_API_BASE_URL`: 你的后端API地址

#### 步骤4: 配置vercel.json

```json
{
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://your-backend-api.com/api/:path*"
    }
  ],
  "routes": [
    {
      "src": "/[^.]+",
      "dest": "/",
      "status": 200
    }
  ]
}
```

### 方案4: Netlify部署

#### 步骤1: 创建netlify.toml

```toml
[build]
  command = "npm run build"
  publish = "dist"

[[redirects]]
  from = "/api/*"
  to = "https://your-backend-api.com/api/:splat"
  status = 200
  force = true

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

#### 步骤2: 连接Git仓库

在Netlify仪表板中连接你的Git仓库，自动部署。

## 🔒 HTTPS配置

### 使用Let's Encrypt（免费SSL）

```bash
# 安装Certbot
sudo apt-get update
sudo apt-get install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com
```

Nginx配置会自动更新为HTTPS。

## 🔍 部署后检查

### 1. 功能测试

- [ ] 访问主页正常
- [ ] 用户注册和登录功能正常
- [ ] AI问答功能正常
- [ ] 文件上传下载正常
- [ ] 深色模式切换正常
- [ ] 所有页面路由正常

### 2. 性能检查

使用浏览器开发者工具检查：

- 首屏加载时间 < 3秒
- 资源压缩启用
- 缓存策略正确
- 图片优化

### 3. SEO检查

- [ ] 正确的meta标签
- [ ] 正确的title
- [ ] robots.txt配置

### 4. 安全检查

- [ ] HTTPS启用
- [ ] CORS配置正确
- [ ] XSS防护
- [ ] CSRF防护
- [ ] API密钥安全存储

## 📊 监控和日志

### 前端错误监控

可以集成Sentry:

```javascript
// main.js
import * as Sentry from "@sentry/vue"

Sentry.init({
  app,
  dsn: "your-sentry-dsn",
  integrations: [
    new Sentry.BrowserTracing({
      routingInstrumentation: Sentry.vueRouterInstrumentation(router),
    }),
  ],
  tracesSampleRate: 1.0,
})
```

### 后端日志

确保后端日志配置正确，便于排查问题。

## 🔄 更新部署

### 零停机更新

使用蓝绿部署或滚动更新：

```bash
# 1. 构建新版本
npm run build

# 2. 备份当前版本
cp -r dist dist.backup

# 3. 部署新版本
cp -r dist/* /var/www/ai-tutor/

# 4. 验证新版本
curl https://your-domain.com

# 5. 如果有问题，回滚
# cp -r dist.backup/* /var/www/ai-tutor/
```

## 🐛 常见问题

### 问题1: 刷新页面404

**原因**: Vue Router的history模式需要服务器配置

**解决**: 按照上面的Nginx或Apache配置添加URL重写

### 问题2: API请求跨域错误

**原因**: 后端没有正确配置CORS

**解决**: 在后端添加CORS中间件，允许前端域名

### 问题3: 静态资源404

**原因**: 构建路径配置不正确

**解决**: 检查vite.config.js中的base配置

```javascript
export default defineConfig({
  base: '/', // 如果部署在子目录，改为 '/subdir/'
})
```

### 问题4: 文件上传失败

**原因**: Nginx文件大小限制

**解决**: 在Nginx配置中增加：

```nginx
client_max_body_size 500M;
```

### 问题5: WebSocket连接失败（如果使用）

**解决**: 在Nginx中添加WebSocket支持：

```nginx
location /ws {
    proxy_pass http://backend:5000;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

## 📝 部署检查清单

### 部署前

- [ ] 代码已提交到版本控制
- [ ] 所有测试通过
- [ ] 环境变量已配置
- [ ] API文档已更新
- [ ] 备份计划已制定

### 部署中

- [ ] 构建成功无错误
- [ ] 文件权限正确
- [ ] 服务器配置正确
- [ ] SSL证书有效

### 部署后

- [ ] 功能测试通过
- [ ] 性能测试通过
- [ ] 安全测试通过
- [ ] 监控已启用
- [ ] 文档已更新

## 🎯 性能优化建议

### 1. 代码分割

Vite自动进行代码分割，但可以手动优化：

```javascript
// vite.config.js
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        'vendor': ['vue', 'vue-router', 'pinia'],
        'highlight': ['highlight.js'],
        'markdown': ['marked']
      }
    }
  }
}
```

### 2. 压缩

启用Gzip或Brotli压缩：

```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
gzip_min_length 1000;
```

### 3. CDN

将静态资源部署到CDN:

```javascript
// vite.config.js
build: {
  assetsDir: 'assets',
  rollupOptions: {
    output: {
      assetFileNames: 'assets/[name].[hash][extname]'
    }
  }
}
```

### 4. 缓存策略

```nginx
# 强缓存静态资源
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# 协商缓存HTML
location ~* \.html$ {
    expires -1;
    add_header Cache-Control "no-cache";
}
```

## 📚 相关资源

- [Vite部署文档](https://vitejs.dev/guide/static-deploy.html)
- [Vue Router部署文档](https://router.vuejs.org/guide/essentials/history-mode.html)
- [Nginx文档](https://nginx.org/en/docs/)
- [Let's Encrypt文档](https://letsencrypt.org/docs/)

---

**部署愉快！** 🚀

