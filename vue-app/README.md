# AI智能学习助手系统 - Vue 3版本

这是基于Vue 3的现代化前端重构版本，将原有的HTML页面转换为组件化的Vue应用。

## ✨ 特性

- 🚀 **Vue 3 + Vite** - 使用最新的Vue 3 Composition API和Vite构建工具
- 🎨 **现代化UI** - 全新的界面设计，支持深色模式
- 📦 **组件化开发** - 模块化的组件结构，易于维护和扩展
- 🔐 **完整的认证系统** - JWT Token认证，路由守卫
- 💾 **状态管理** - 使用Pinia进行全局状态管理
- 🌐 **API集成** - 完整对接后端FastAPI接口
- 🤖 **AI辅助学习** - 集成AI文章生成、话题推荐及智能问答功能
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
- **全屏布局重构**:
  - 将聊天界面重构为 100vh 全屏布局，移除了原有容器的最大宽度限制和圆角阴影，使视野更开阔。
  - 侧边栏改为固定宽度并占据整个屏幕高度，实现了真正的全屏 AI 交互体验。
- **侧边栏用户信息集成**:
  - 在侧边栏顶部集成了用户头像和用户名，支持自动获取和展示用户头像。
  - 在侧边栏顶部添加了主题切换按钮，方便用户一键切换深色/浅色模式。
  - 在侧边栏底部添加了“退出登录”按钮，提升了账户管理的便捷性。
- **聊天头像优化**: DeepSeek 与 豆包使用静态头像资源，用户消息优先使用已上传头像，无头像则自动兜底到原有图标。
- **UI 细节修复**: 修复了聊天界面侧边栏“新建对话”按钮字体不可见的问题，通过明确设置高对比度的背景颜色和文字颜色，提升了界面的可用性和视觉清晰度。
- **模型选择器重构**: 
  - 将原有的下拉框重构为仿图 2 风格的品牌分类选择器（DeepSeek、豆包）。
  - 引入了平滑的 `cubic-bezier` 展开动画和悬停缩放效果，极大提升了交互的丝滑感。
  - 移除了冗余的装饰性图标，使界面更加简洁专业，符合现代审美。
- **视觉体验升级**:
  - 显著调大了聊天消息字体（16px/17px），缓解长时间阅读带来的视觉疲劳。
  - 优化了消息文本的行高（1.8）和段落间距，大幅提升内容可读性。
  - 移除了界面中多余的背景图案，打造更加纯净的沉浸式对话环境。
- **深度思考逻辑优化**: 实现了深度思考开关与模型品牌的智能联动。开启“深度思考”时，系统会自动根据当前选中的品牌切换到对应的推理模型（如 DeepSeek Reasoner 或 豆包 Reasoner），关闭时则回退到标准模型。
- **AI文章单词高亮**: 修复文章预览中高亮样式在 `scoped` 下不生效的问题，确保已选单词在文章中可见高亮显示。
- **公共词库优化**: 修复了公共词库不显示的问题，实现了后端分页加载，添加了加载状态提示。
- **云盘文件夹树优化**: 
  - 文件夹树按层级深度自适应缩进，超过 3 层自动启用滚动条，展开/折叠加入平滑过渡并兼容移动端与桌面端。
  - 修复了选中父目录时子目录显示高亮的问题，通过 CSS 子代选择器实现精确高亮。
   - 引入了悬浮操作选项卡，将重命名和删除按钮整合在内，仅在悬停时展示平滑的入场动画。
   - 优化名称展示：移除文件夹名称的省略号限制，支持长名称完整显示及自动换行。
- **文件夹重命名机制升级**: 实现了与文件一致的文件夹重命名功能，支持智能冲突检测、自动重命名及覆盖合并策略，并自动处理子文件和子文件夹的递归路径更新。
- **云盘组件 UI 深度适配**:
  - 将云盘侧边栏宽度统一调整为 300px，与聊天界面保持一致。
  - 优化了面包屑导航、工具栏按钮及文件列表的间距与排版，提升了操作体验。
  - 重新设计了文件表格样式，调优了表头视觉效果及操作按钮的交互反馈。
  - 为冲突处理对话框引入了毛玻璃背景及更精致的卡片设计，确保视觉风格的连贯性。
  - 优化了云盘的加载状态（Loading）和空状态（Empty）界面，添加了平滑的淡入动画和更现代的视觉元素。

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
- ✅ 企业级侧边栏布局，支持固定高度 and 内部滚动
- ✅ 单词表管理（创建、删除、统计）
- ✅ 单词添加和学习（支持掌握度分级）
- ✅ 学习时长统计（今日时长/总时长）
- ✅ 今日复习支持“认识/掌握”快速更新进度
- ✅ 公共词库默认加载前50条，分页按需加载
- ✅ **AI 文章生成/管理**：
  - 支持选择单词表与目标单词（全选/多选）
  - AI 生成主题建议，一键填入主题
  - 优化生成配置布局，修复文章主题输入框宽度问题
  - 生成后在弹窗内展示中英段落对照，并对重点词汇高亮（悬停可看释义）
  - “我的文章”支持列表查看与再次打开
- ✅ **学习资料下载**：支持将文章导出为 HTML/TXT，或直接打印为 PDF

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

