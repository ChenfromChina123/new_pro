# AI智能学习助手系统 - Vue 3版本

这是基于Vue 3的现代化前端重构版本，将原有的HTML页面转换为组件化的Vue应用。

## ✨ 特性

- 🚀 **Vue 3 + Vite** - 使用最新的Vue 3 Composition API和Vite构建工具
- 🎨 **现代化UI** - 全新的界面设计，支持深色模式
- 📦 **组件化开发** - 模块化的组件结构，易于维护和扩展
- 🔐 **完整的认证系统** - JWT Token认证，路由守卫
- 💾 **状态管理** - 使用Pinia进行全局状态管理
- 🌐 **API集成** - 完整对接后端FastAPI接口
- 📱 **响应式设计** - 适配各种屏幕尺寸

## 🛠️ 技术栈

- **框架**: Vue 3.4+
- **构建工具**: Vite 5.0+
- **路由**: Vue Router 4.2+
- **状态管理**: Pinia 2.1+
- **HTTP客户端**: Axios 1.6+
- **Markdown渲染**: Marked 11.0+
- **代码高亮**: Highlight.js 11.9+

## 📦 安装

### 前置要求

- Node.js 16+ 或 20+
- npm 或 yarn

### 安装步骤

1. **克隆项目**

```bash
cd Aiproject8.2/vue-app
```

2. **安装依赖**

```bash
npm install
# 或
yarn install
```

3. **配置环境变量**

项目会自动使用代理将 `/api` 请求转发到 `http://localhost:5000`

如需修改后端地址，可编辑 `vite.config.js`:

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://your-backend-url:5000',
      changeOrigin: true
    }
  }
}
```

## 🚀 运行

### 最近更新
- **公共词库优化**: 修复了公共词库不显示的问题，实现了后端分页加载，添加了加载状态提示。
- **夜间模式适配**: 统一了所有前端模块的样式，特别是语言学习模块，现在完全适配夜间模式。

### 开发模式

```bash
npm run dev
```

应用将在 `http://localhost:3000` 启动

### 生产构建

```bash
npm run build
```

构建文件将输出到 `dist` 目录

### 预览生产构建

```bash
npm run preview
```

## 📁 项目结构

```
vue-app/
├── src/
│   ├── assets/              # 静态资源
│   │   └── styles/          # 全局样式
│   │       └── main.css     # 主样式文件
│   ├── components/          # 通用组件
│   │   ├── AppHeader.vue    # 顶部导航栏
│   │   └── AppLayout.vue    # 布局组件
│   ├── config/              # 配置文件
│   │   └── api.js           # API配置和端点
│   ├── router/              # 路由配置
│   │   └── index.js         # 路由定义
│   ├── stores/              # Pinia状态管理
│   │   ├── auth.js          # 认证状态
│   │   ├── chat.js          # 聊天状态
│   │   ├── cloudDisk.js     # 云盘状态
│   │   └── theme.js         # 主题状态
│   ├── utils/               # 工具函数
│   │   └── request.js       # Axios封装
│   ├── views/               # 页面组件
│   │   ├── auth/            # 认证相关页面
│   │   │   ├── LoginView.vue     # 登录页
│   │   │   └── RegisterView.vue  # 注册页
│   │   ├── AdminView.vue         # 管理后台
│   │   ├── ChatManagementView.vue # 聊天记录管理
│   │   ├── ChatView.vue          # AI问答主页
│   │   ├── CloudDiskView.vue     # 云盘管理
│   │   ├── LanguageLearningView.vue # 语言学习
│   │   └── NotFoundView.vue      # 404页面
│   ├── App.vue              # 根组件
│   └── main.js              # 应用入口
├── public/                  # 公共静态资源
├── index.html               # HTML模板
├── package.json             # 项目配置
├── vite.config.js           # Vite配置
└── README.md                # 本文件
```

## 🎯 主要功能

### 1. 用户认证
- ✅ 邮箱注册（含验证码）
- ✅ 用户登录
- ✅ JWT Token认证
- ✅ 路由守卫保护

### 2. AI问答
- ✅ 流式AI回复
- ✅ 会话管理
- ✅ 消息历史
- ✅ 多模型支持（DeepSeek、豆包）
- ✅ Markdown渲染
- ✅ 代码高亮

### 3. 云盘管理
- ✅ 文件上传（支持多文件）
- ✅ 文件夹管理
- ✅ 文件预览
- ✅ 批量下载
- ✅ 批量删除
- ✅ 上传进度显示

### 4. 语言学习
- ✅ 企业级侧边栏布局，支持固定高度和内部滚动
- ✅ 单词表管理（创建、删除、统计）
- ✅ 单词添加和学习（支持掌握度分级）
- ✅ 学习时长统计（今日时长/总时长）
- ✅ 今日复习支持“认识/掌握”快速更新进度
- ✅ 公共词库默认加载前50条，分页按需加载
- ✅ 独立视图的 AI 学习文章生成（支持选择来源词表）

### 5. 聊天记录管理
- ✅ 查看所有对话
- ✅ 删除对话
- ✅ 快速跳转到会话

### 6. 管理后台（需要管理员权限）
- ✅ 用户管理
- ✅ 文件管理
- ✅ 反馈管理
- ✅ 数据统计

### 7. 其他功能
- ✅ 深色模式切换
- ✅ 响应式设计
- ✅ 动画效果
- ✅ 路由过渡

## 🔌 API对接

所有API端点配置在 `src/config/api.js` 中：

```javascript
export const API_ENDPOINTS = {
  auth: { ... },      // 认证相关
  chat: { ... },      // AI问答
  cloudDisk: { ... }, // 云盘管理
  language: { ... },  // 语言学习
  admin: { ... }      // 管理后台
}
```

### HTTP拦截器

- **请求拦截器**: 自动添加JWT Token到请求头
- **响应拦截器**: 统一处理错误，401自动跳转登录

## 🎨 样式定制

### CSS变量

所有主题颜色定义在 `src/assets/styles/main.css` 中的CSS变量：

```css
:root {
  --primary-color: #3498db;
  --secondary-color: #2980b9;
  --success-color: #27ae60;
  /* ... 更多变量 */
}

body.dark-mode {
  /* 深色模式变量 */
}
```

### 深色模式

使用Pinia store管理主题：

```javascript
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
themeStore.toggleDarkMode() // 切换主题
```

## 🔒 路由守卫

路由守卫配置在 `src/router/index.js` 中：

- `requiresAuth`: 需要登录才能访问
- `requiresGuest`: 只有未登录用户可访问（如登录、注册页）
- `requiresAdmin`: 需要管理员权限

## 📱 响应式设计

- 桌面端: 完整功能，侧边栏布局
- 平板: 适配中等屏幕
- 移动端: 隐藏侧边栏，优化触摸操作

## 🚀 部署

### 部署到生产环境

1. **构建项目**

```bash
npm run build
```

2. **部署dist目录**

将 `dist` 目录部署到任何静态服务器：

- Nginx
- Apache
- Vercel
- Netlify
- GitHub Pages

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🔧 开发指南

### 添加新页面

1. 在 `src/views/` 创建新的Vue组件
2. 在 `src/router/index.js` 添加路由
3. 在 `src/components/AppHeader.vue` 添加导航链接

### 添加新的API

1. 在 `src/config/api.js` 添加API端点
2. 在对应的store或组件中调用

### 添加新的Store

1. 在 `src/stores/` 创建新的store文件
2. 使用Composition API风格定义store
3. 在组件中引入使用

## ❓ 常见问题

### 问: API请求失败，提示网络错误
答: 确保后端服务运行在 `http://localhost:5000`，或修改vite.config.js中的代理配置

### 问: 登录后刷新页面需要重新登录
答: Token已保存在localStorage，检查浏览器控制台是否有错误

### 问: 深色模式不生效
答: 确保已在Pinia store中正确初始化主题

### 问: 文件上传失败
答: 检查后端MAX_FILE_SIZE配置，默认为500MB

## 📝 与原HTML版本的对比

| 特性 | HTML版本 | Vue 3版本 |
|------|---------|-----------|
| 技术栈 | 原生JS | Vue 3 + Vite |
| 代码组织 | 单文件数千行 | 组件化模块化 |
| 状态管理 | 全局变量 | Pinia |
| 路由 | 页面跳转 | Vue Router |
| API调用 | Fetch | Axios + 拦截器 |
| 样式管理 | 内联样式 | CSS变量 + Scoped |
| 开发体验 | 手动刷新 | HMR热更新 |
| 构建优化 | 无 | Vite自动优化 |
| 代码复用 | 复制粘贴 | 组件化复用 |
| 维护性 | 较差 | 优秀 |

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

## 📮 联系方式

如有问题或建议，请提交Issue或联系开发团队。

---

**Made with ❤️ using Vue 3**

最后更新: 2024年12月

