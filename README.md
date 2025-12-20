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

## 🚀 最近更新

### ☁️ 云盘功能增强
- **目录层级限制**: 云盘最多支持两层目录（不计根目录），在后端和前端均实现了严格的验证逻辑。
- **存储配额管理**: 实现了用户存储配额限制（普通用户 1GB，管理员无限制），并在上传文件前进行校验。
- **存储空间可视化**: 在侧边栏实时显示当前用户的存储空间使用情况和配额进度条，支持随着文件上传/删除动态更新。
- **路径导航优化**: 修复了面包屑路径显示不全的问题，现在正确显示完整的文件夹层级（如 `123 > 写作`），并自动隐藏根节点以简化显示。
- **UI 交互升级**: 将文件夹操作按钮合并为悬浮显示的选项卡，仅在鼠标悬停时平滑显示；优化了文件夹树的点击高亮逻辑。

### 🤖 AI 问答体验优化
- **全屏布局重构**: 采用 100vh 全屏布局，侧边栏固定高度，提供沉浸式交互体验。
- **输入框 UI 重构**: 现代化大圆角设计，集成附件上传、模型切换、截图、语音等丰富工具栏。
- **深度思考 (Reasoning)**: 支持 DeepSeek Reasoner 等推理模型，实现思考过程的流式展示与智能收起逻辑。
- **停止生成功能**: 支持通过 AbortController 中止 SSE 流式输出。
- **响应式字体**: 优化了聊天消息的字体大小和间距，提升长文本阅读体验。

### 🛡️ 代码质量与安全
- **XSS 安全防护**: 全面集成 `DOMPurify` 库对 `v-html` 内容进行过滤，有效防御跨站脚本攻击。
- **Lombok 注解优化**: 为实体类字段添加 `@Builder.Default`，确保数据模型在 Builder 模式下的正确初始化。
- **代码清理**: 移除了后端多个 Service 和 Controller 中未使用的导入和变量，提升了系统性能与可维护性。

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
│   │   ├── AppLayout.vue    # 基础布局
│   │   └── Sidebar/         # 侧边栏组件
│   ├── views/               # 页面视图
│   │   ├── ChatView.vue     # AI 问答页面
│   │   └── CloudDiskView.vue # 云盘页面
│   ├── stores/              # Pinia 状态管理
│   └── config/              # 配置文件
```
