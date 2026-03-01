# blog.aistudy.icu 部署说明

## 项目信息

- **域名**: blog.aistudy.icu
- **框架**: Next.js 15 + Tailwind CSS
- **端口**: 3200

## 本地开发

```bash
# 安装依赖
yarn install

# 启动开发服务器
yarn dev

# 构建生产版本
yarn build

# 启动生产服务器
yarn serve
```

## 服务器部署

### 方式一：直接部署

1. **安装 Node.js 环境**
   ```bash
   # 安装 nvm
   curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
   source ~/.bashrc
   
   # 安装 Node.js 20+
   nvm install 20
   nvm use 20
   ```

2. **安装 yarn**
   ```bash
   npm install -g yarn
   ```

3. **克隆项目并安装依赖**
   ```bash
   cd /var/www
   git clone <your-repo-url> blog
   cd blog
   yarn install
   ```

4. **构建项目**
   ```bash
   yarn build
   ```

5. **使用 PM2 管理进程**
   ```bash
   # 安装 PM2
   npm install -g pm2
   
   # 启动应用
   pm2 start yarn --name "blog" -- serve -- -p 3200
   
   # 设置开机自启
   pm2 startup
   pm2 save
   ```

### 方式二：Docker 部署

1. **构建镜像**
   ```bash
   docker build -t blog-aistudy .
   ```

2. **运行容器**
   ```bash
   docker run -d --name blog -p 3200:3000 blog-aistudy
   ```

### Nginx 配置

1. **复制配置文件**
   ```bash
   sudo cp nginx.conf /etc/nginx/sites-available/blog.aistudy.icu
   sudo ln -s /etc/nginx/sites-available/blog.aistudy.icu /etc/nginx/sites-enabled/
   ```

2. **测试并重载配置**
   ```bash
   sudo nginx -t
   sudo systemctl reload nginx
   ```

### SSL 证书配置

```bash
# 使用 Let's Encrypt 获取免费证书
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d blog.aistudy.icu
```

## DNS 配置

在域名服务商处添加 A 记录：
- 主机记录: `blog`
- 记录类型: `A`
- 记录值: 服务器IP地址

## 常用命令

```bash
# 查看 PM2 状态
pm2 status

# 查看日志
pm2 logs blog

# 重启应用
pm2 restart blog

# 更新部署
git pull
yarn install
yarn build
pm2 restart blog
```
