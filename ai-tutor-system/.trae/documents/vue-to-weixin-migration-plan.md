# Vue 组件迁移到微信小程序计划

## 项目概述
将 vue-app 中的核心组件和 UI 样式完整复刻到 weixin-web 微信小程序中，保持视觉风格和交互体验的一致性。

## 一、现状分析

### Vue 项目技术栈
- **框架**: Vue 3 + Pinia + Vue Router
- **UI 组件**: 自定义组件 + Font Awesome 图标
- **样式系统**: CSS 变量 (支持深色模式) + Scoped CSS
- **核心功能**: AI 问答、云盘管理、单词游戏、聊天界面

### 微信小程序现状
- **框架**: 原生小程序框架
- **页面结构**: pages/index, pages/chat, pages/cloud-disk, pages/word-game 等
- **组件**: navigation-bar 组件
- **样式**: 传统 WXSS，使用 rpx 单位

## 二、迁移策略

### 2.1 样式系统迁移
**目标**: 将 Vue 的 CSS 变量系统转换为小程序的自定义属性

**实施步骤**:
1. 创建全局样式文件 `styles/theme.wxss`
2. 转换 CSS 变量为小程序支持的格式
3. 实现深色模式切换机制
4. 迁移所有工具类 (间距、弹性布局、文字大小等)

**关键文件**:
- `weixin-web/styles/theme.wxss` (新建)
- `weixin-web/styles/utils.wxss` (新建)
- `weixin-web/app.wxss` (修改)

### 2.2 核心组件迁移

#### 2.2.1 布局组件
**迁移清单**:
1. **AppHeader** → `components/app-header/`
   - 创建组件：app-header.js, app-header.json, app-header.wxml, app-header.wxss
   - 功能：顶部导航栏、主题切换、用户菜单
   - 适配点：移除 Font Awesome，使用小程序图标或图片

2. **AppSidebar** → `components/app-sidebar/`
   - 创建侧边栏导航组件
   - 功能：移动端菜单、导航链接
   - 适配点：使用小程序的 movable-view 实现拖拽

3. **AppLayout** → `components/app-layout/`
   - 创建页面布局容器
   - 功能：整合 header + sidebar + main content
   - 适配点：使用小程序的 slot 机制

#### 2.2.2 功能组件
**迁移清单**:
1. **CustomSelect** → `components/custom-select/`
   - 自定义下拉选择器
   - 使用小程序的 picker 组件增强

2. **RequirementManager** → `components/requirement-manager/`
   - 需求管理组件
   - 迁移表单和列表交互

3. **Settings** → `components/settings/`
   - 设置面板组件
   - 迁移配置项 UI

4. **TranslationTool** → `components/translation-tool/`
   - 翻译工具组件
   - 迁移输入框和结果展示

5. **ConflictResolutionDialog** → `components/conflict-dialog/`
   - 冲突解决对话框
   - 使用小程序的 modal 增强版

#### 2.2.3 编辑器组件
**迁移清单**:
1. **EditorShell** → `components/editor-shell/`
   - 编辑器外壳组件
   - 整合工具栏和编辑区

2. **Toolbar** → `components/editor-toolbar/`
   - 工具栏组件
   - 迁移格式化按钮

3. **Sidebar** (编辑器侧边栏) → `components/editor-sidebar/`
   - 文件树侧边栏

4. **MarkdownEditor** → `components/markdown-editor/`
   - Markdown 编辑器
   - 使用小程序的 textarea 增强

5. **MonacoEditor** → 暂不迁移 (技术限制)
   -  Monaco 无法在小程序运行
   - 替代方案：使用原生 textarea + 自定义高亮

6. **RichTextEditor** → `components/rich-text-editor/`
   - 富文本编辑器
   - 使用小程序 rich-text 组件

#### 2.2.4 SFTP 组件
**迁移清单**:
1. **FilePanel** → `components/sftp-file-panel/`
   - 文件列表面板
   - 迁移文件展示和交互

2. **FileTable** → `components/sftp-file-table/`
   - 文件表格组件
   - 使用小程序的 scroll-view

3. **Breadcrumb** → `components/sftp-breadcrumb/`
   - 面包屑导航

4. **FileIcon** → `components/sftp-file-icon/`
   - 文件图标组件
   - 根据文件类型显示图标

5. **StatusBar** → `components/sftp-status-bar/`
   - 状态栏组件
   - 显示传输状态

6. **TransferQueue** → `components/sftp-transfer-queue/`
   - 传输队列组件
   - 显示上传下载进度

#### 2.2.5 终端组件
**迁移清单**:
1. **TerminalTab** → `components/terminal-tab/`
   - 终端标签页
   - 使用 scroll-view 模拟终端输出

### 2.3 页面迁移

#### 2.3.1 核心页面优先级
**P0 - 立即迁移**:
1. **LandingView** → `pages/landing/`
   - 首页落地页
   - Hero 区域、特性展示、CTA 区域
   - 动画效果使用小程序 animation API

2. **ChatView** → `pages/chat/` (增强现有页面)
   - AI 问答界面
   - 消息列表、输入框、工具栏
   - 流式响应显示
   - Markdown 渲染 (使用小程序 rich-text)
   - 代码高亮 (简化版)
   - 深度思考折叠面板

3. **CloudDiskView** → `pages/cloud-disk/` (增强现有页面)
   - 云盘管理界面
   - 文件列表、上传下载、预览
   - 文件夹层级管理
   - 冲突处理对话框

4. **WordGameView** → `pages/word-game/` (增强现有页面)
   - 单词游戏界面
   - 课程包选择、课程列表、答题界面
   - 答题输入组件
   - 完成度展示

**P1 - 第二批迁移**:
5. **LoginView/RegisterView** → `pages/auth/` (增强现有页面)
   - 登录注册页面
   - 表单验证
   - 错误提示

6. **ProfileView** → `pages/profile/` (增强现有页面)
   - 个人中心
   - 用户信息展示
   - 设置入口

7. **PublicFilesView** → `pages/public-files/`
   - 公共资源页面
   - 文件列表和下载

**P2 - 后续迁移**:
8. **AgentView** → `pages/agent/`
   - Agent 终端助手页面

9. **CodeNovaView** → `pages/codenova/`
   - CodeNova 下载页

10. **AuthorView** → `pages/author/`
    - 关于作者页面

11. **LinksView** → `pages/links/`
    - 资源推荐页面

### 2.4 状态管理迁移

**Pinia → 小程序全局数据**

**实施方案**:
1. 创建 `utils/store.js`
2. 实现类似 Pinia 的 store 机制
3. 迁移核心 stores:
   - `auth.js` → 用户认证状态
   - `theme.js` → 主题状态
   - `chat.js` → 聊天数据
   - `cloudDisk.js` → 云盘数据
   - `vocabulary.js` → 单词学习数据
   - `editor.js` → 编辑器状态
   - `sftp.js` → SFTP 连接状态

### 2.5 工具函数迁移

**迁移清单**:
1. `utils/request.js` → API 请求封装 (已有，需增强)
2. `utils/fileIcons.js` → 文件图标映射
3. `utils/graphqlClient.js` → GraphQL 客户端 (如需要)
4. `utils/rsaEncryption.js` → RSA 加密 (小程序 crypto 支持)
5. `config/api.js` → API 配置
6. `config/framework-presets.ts` → 框架预设 (转为 JS)

### 2.6 服务层迁移

**迁移清单**:
1. `services/sftpService.js` → SFTP 服务
2. `services/terminalService.ts` → 终端服务 (转为 JS)
3. `services/context-summarizer.ts` → 上下文总结 (转为 JS)
4. `services/permission-manager.ts` → 权限管理 (转为 JS)
5. `services/validation-pipeline.ts` → 验证管道 (转为 JS)

## 三、技术适配要点

### 3.1 样式适配
- **单位转换**: px → rpx (1px = 2rpx)
- **CSS 变量**: 使用全局 class 模拟
- **深色模式**: 通过 page 的 class 切换
- **响应式**: 使用小程序的媒体查询

### 3.2 图标适配
- **Font Awesome** → 小程序图标库或图片
- **方案**: 
  - 使用 iconfont.cn 自定义图标
  - 或使用小程序内置图标
  - 或下载 SVG 转图片

### 3.3 动画适配
- **Vue Transition** → 小程序 animation API
- **CSS Animation** → 保持不变 (小程序支持)
- **JavaScript 动画** → 使用 wx.createAnimation

### 3.4 事件处理
- **@click** → `bindtap`
- **@input** → `bindinput`
- **@change** → `bindchange`
- **@scroll** → `bindscroll`

### 3.5 数据绑定
- **v-model** → `value` + `bindinput`
- **v-for** → `wx:for`
- **v-if/v-else** → `wx:if/wx:else`
- **:class** → 动态 class 拼接
- **:style** → 动态 style 对象

### 3.6 组件通信
- **Props** → `properties`
- **Emits** → `triggerEvent`
- **Slots** → `<slot>`
- **Provide/Inject** → 全局 data 或 getApp()

## 四、文件目录结构

```
weixin-web/
├── components/              # 组件目录
│   ├── app-header/         # 顶部导航
│   ├── app-sidebar/        # 侧边栏
│   ├── app-layout/         # 布局容器
│   ├── custom-select/      # 自定义选择器
│   ├── conflict-dialog/    # 冲突对话框
│   ├── editor-shell/       # 编辑器外壳
│   ├── editor-toolbar/     # 编辑器工具栏
│   ├── editor-sidebar/     # 编辑器侧边栏
│   ├── markdown-editor/    # Markdown 编辑器
│   ├── rich-text-editor/   # 富文本编辑器
│   ├── sftp-file-panel/    # SFTP 文件面板
│   ├── sftp-file-table/    # SFTP 文件表格
│   ├── sftp-breadcrumb/    # SFTP 面包屑
│   ├── sftp-file-icon/     # SFTP 文件图标
│   ├── sftp-status-bar/    # SFTP 状态栏
│   ├── sftp-transfer-queue/# SFTP 传输队列
│   └── terminal-tab/       # 终端标签页
├── pages/                   # 页面目录
│   ├── landing/            # 落地页 (新增)
│   ├── auth/               # 认证页面 (增强)
│   ├── chat/               # 聊天页面 (增强)
│   ├── cloud-disk/         # 云盘页面 (增强)
│   ├── word-game/          # 单词游戏 (增强)
│   ├── profile/            # 个人中心 (增强)
│   ├── public-files/       # 公共资源 (新增)
│   ├── agent/              # Agent 助手 (新增)
│   ├── codenova/           # CodeNova (新增)
│   ├── author/             # 关于作者 (新增)
│   └── links/              # 资源推荐 (新增)
├── utils/                   # 工具函数
│   ├── store.js            # 状态管理
│   ├── request.js          # 请求封装 (增强)
│   ├── fileIcons.js        # 文件图标
│   ├── rsaEncryption.js    # RSA 加密
│   └── formatUtils.js      # 格式化工具
├── services/                # 服务层
│   ├── sftpService.js      # SFTP 服务
│   ├── terminalService.js  # 终端服务
│   ├── contextSummarizer.js# 上下文总结
│   ├── permissionManager.js# 权限管理
│   └── validationPipeline.js# 验证管道
├── config/                  # 配置文件
│   ├── api.js              # API 配置
│   └── frameworkPresets.js # 框架预设
├── styles/                  # 样式文件
│   ├── theme.wxss          # 主题变量
│   ├── utils.wxss          # 工具类
│   └── animation.wxss      # 动画
└── images/                  # 图片资源
    ├── icons/              # 图标
    └── illustrations/      # 插图
```

## 五、实施计划

### 阶段一：基础设施 (预计 2 天)
1. 创建样式系统 (theme.wxss, utils.wxss)
2. 创建状态管理机制 (utils/store.js)
3. 迁移工具函数和配置
4. 准备图标资源

### 阶段二：核心组件 (预计 3 天)
1. 迁移布局组件 (AppHeader, AppSidebar, AppLayout)
2. 迁移通用组件 (CustomSelect, Settings, Dialog 等)
3. 迁移编辑器组件 (EditorShell, Toolbar 等)
4. 迁移 SFTP 组件

### 阶段三：核心页面 (预计 4 天)
1. 迁移 LandingView
2. 增强 ChatView (深度思考、Markdown 渲染)
3. 增强 CloudDiskView (文件预览、冲突处理)
4. 增强 WordGameView (答题交互)

### 阶段四：次要页面 (预计 2 天)
1. 迁移认证页面
2. 迁移个人中心
3. 迁移其他展示页面

### 阶段五：测试优化 (预计 1 天)
1. 功能测试
2. 性能优化
3. 样式微调
4. 深色模式测试

## 六、技术难点与解决方案

### 难点 1: Monaco 编辑器无法迁移
**解决方案**: 
- 使用原生 textarea + 自定义语法高亮
- 或使用第三方小程序编辑器组件
- 或简化为纯文本输入

### 难点 2: Font Awesome 图标
**解决方案**:
- 使用 iconfont.cn 创建自定义图标库
- 下载 SVG 转为图片格式
- 使用小程序内置图标

### 难点 3: Markdown 渲染
**解决方案**:
- 使用小程序 rich-text 组件
- 后端预渲染 Markdown 为 HTML
- 使用第三方库如 towxml

### 难点 4: 代码高亮
**解决方案**:
- 简化版：使用 predefined styles
- 使用 highlight.js 的简化版本
- 后端预高亮返回 HTML

### 难点 5: 深色模式
**解决方案**:
- 使用 page 的 class 切换
- 定义两套主题变量
- 通过全局样式控制

### 难点 6: 动画效果
**解决方案**:
- CSS 动画保持不变
- Vue Transition 转为 wx.createAnimation
- 复杂动画简化处理

## 七、质量保障

### 代码规范
1. 统一命名规范 (驼峰命名)
2. 添加详细注释
3. 遵循小程序最佳实践

### 测试策略
1. 功能测试：每个组件单独测试
2. 集成测试：页面级别测试
3. 兼容性测试：不同设备测试
4. 性能测试：加载速度测试

### 验收标准
1. UI 还原度 ≥ 95%
2. 功能完整性 100%
3. 深色模式正常切换
4. 响应式布局正常
5. 无严重 bug

## 八、后续优化

### 性能优化
1. 图片懒加载
2. 分页加载
3. 虚拟列表 (长列表)
4. 请求缓存

### 体验优化
1. 骨架屏加载
2. 错误边界处理
3. 网络状态提示
4. 加载进度提示

### 功能增强
1. 离线缓存
2. 消息推送
3. 分享功能
4. 小程序码生成

## 九、注意事项

1. **不要直接复制代码**: 需要适配小程序语法
2. **单位转换**: 所有 px 转为 rpx
3. **图标处理**: Font Awesome 转为图片或其他形式
4. **事件绑定**: Vue 事件转为小程序 bindtap 等
5. **数据绑定**: v-model 转为 value + bindinput
6. **组件通信**: props/emit 转为 properties/triggerEvent
7. **生命周期**: Vue 生命周期转为小程序生命周期
8. **样式隔离**: 组件样式需要 scoped 或使用命名空间

## 十、总结

本次迁移将完整复刻 Vue 项目的核心功能和 UI 样式到微信小程序，涉及:
- **组件**: 20+ 个组件
- **页面**: 10+ 个页面
- **样式**: 完整的主题系统
- **状态管理**: 类似 Pinia 的机制
- **工具函数**: 完整的工具库

预计总工作量：**12 人天**

迁移完成后，微信小程序将拥有与 Web 端一致的视觉体验和交互效果。
