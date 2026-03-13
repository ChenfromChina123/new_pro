# 全能型文件编辑器 - 使用文档

## 项目概述

全能型文件编辑器是一个类似 VS Code 的核心体验的文件编辑工具，采用混合模式架构，根据文件后缀名自动切换渲染引擎，提供代码、Markdown 和富文本编辑能力。

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **样式**: Tailwind CSS
- **代码编辑器**: Monaco Editor + vite-plugin-monaco-editor
- **Markdown 编辑器**: Tiptap
- **富文本编辑器**: Tiptap
- **状态管理**: Pinia
- **构建工具**: Vite

## 核心功能

### 1. 多引擎编辑

- **代码模式**: 支持 `.js`, `.ts`, `.vue`, `.py`, `.json`, `.css`, `.html`, `.sql` 等文件类型
- **Markdown 模式**: 支持 `.md`, `.markdown` 文件类型
- **富文本模式**: 支持 `.txt`, `.html` 文件类型

### 2. VS Code 风格界面

- **顶部 Tab 栏**: 显示当前打开的文件，支持切换和关闭
- **侧边栏**: 包含文件资源管理器、搜索、大纲和 Git 视图
- **底部状态栏**: 显示文件路径、编码格式、光标位置和保存状态
- **悬浮工具栏**: 右键菜单提供搜索、重构、AI 解释和格式化工具

### 3. 状态管理

- 使用 Pinia 管理打开的文件列表
- 自动检测文件类型并切换编辑器
- 记录未保存的文件状态
- 支持多文件同时编辑

## 组件结构

```
src/
├── components/
│   └── editor/
│       ├── EditorShell.vue     # 编辑器外壳（主布局）
│       ├── MonacoEditor.vue    # 代码编辑器
│       ├── MarkdownEditor.vue  # Markdown 编辑器
│       ├── RichTextEditor.vue  # 富文本编辑器
│       ├── Toolbar.vue         # 悬浮工具栏
│       └── Sidebar.vue         # 侧边栏
├── stores/
│   └── editor.js              # 编辑器状态管理
└── views/
    └── EditorView.vue         # 编辑器主视图
```

## 使用方法

### 1. 访问编辑器

在浏览器中导航到 `/editor` 路径即可访问编辑器。

### 2. 打开文件

- 在侧边栏的文件资源管理器中点击文件即可打开
- 支持同时打开多个文件，会在顶部 Tab 栏显示

### 3. 编辑文件

- **代码文件**: 使用 Monaco Editor，支持语法高亮、代码折叠、智能补全
- **Markdown 文件**: 使用 Tiptap 编辑器，支持所见即所得的编辑体验
- **文本文件**: 使用 Tiptap 富文本编辑器

### 4. 保存文件

- 编辑器会自动记录文件修改状态
- 底部状态栏会显示文件的保存状态
- 支持 `Ctrl + S` 快捷键保存（待实现）

### 5. 关闭文件

- 点击顶部 Tab 栏右侧的 `×` 按钮即可关闭文件
- 关闭当前活动文件时，会自动切换到其他打开的文件

## 技术实现细节

### 1. 动态编辑器切换

```javascript
// 根据后缀名返回编辑器类型
editorType: (state) => {
  if (!state.activeFilePath) return null
  const ext = state.activeFilePath.split('.').pop().toLowerCase()
  const codeExts = ['js', 'ts', 'vue', 'py', 'json', 'css', 'html', 'sql']
  const mdExts = ['md', 'markdown']
  
  if (codeExts.includes(ext)) return 'monaco'
  if (mdExts.includes(ext)) return 'markdown'
  return 'richtext' // 默认文本
}
```

### 2. 异步加载编辑器

```javascript
// 异步引入编辑器组件
const MonacoEditor = defineAsyncComponent(() => import('./MonacoEditor.vue'))
const MarkdownEditor = defineAsyncComponent(() => import('./MarkdownEditor.vue'))
const RichTextEditor = defineAsyncComponent(() => import('./RichTextEditor.vue'))
```

### 3. 内存管理

```javascript
onUnmounted(() => {
  // 销毁编辑器实例，避免内存泄漏
  if (editor.value) {
    editor.value.dispose()
  }
})
```

### 4. 主题一致性

- 使用 CSS 变量实现主题一致性
- 支持与系统主题同步（待实现）
- 所有组件使用统一的颜色和样式

## 性能优化

1. **异步组件加载**: 使用 `defineAsyncComponent` 优化编辑器加载性能
2. **内存管理**: 编辑器组件销毁时正确释放资源
3. **状态管理**: 使用 Pinia 高效管理编辑器状态
4. **响应式设计**: 适配不同屏幕尺寸

## 未来规划

1. **AI 辅助功能**: 集成 AI 代码解释和生成功能
2. **版本控制**: 集成 Git 操作功能
3. **插件系统**: 支持扩展编辑器功能
4. **文件系统集成**: 支持本地文件系统操作
5. **实时协作**: 支持多人同时编辑

## 常见问题

### 1. 编辑器加载缓慢

- **原因**: Monaco Editor 包体积较大
- **解决方法**: 使用异步加载和代码分割

### 2. 内存占用高

- **原因**: 频繁打开和关闭大文件
- **解决方法**: 确保编辑器组件正确销毁，释放内存

### 3. 编辑器类型识别错误

- **原因**: 文件后缀名不匹配
- **解决方法**: 检查文件后缀名是否正确

## 开发指南

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

### 3. 构建生产版本

```bash
npm run build
```

### 4. 代码风格检查

```bash
npm run lint
```

## 贡献指南

欢迎贡献代码和提出建议！请遵循以下步骤：

1. Fork 本项目
2. 创建新分支
3. 提交修改
4. 发起 Pull Request

## 许可证

MIT 许可证
