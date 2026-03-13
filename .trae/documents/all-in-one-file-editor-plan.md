# 全能型文件编辑器 - 实现计划

## 项目概述
构建一个类似 VS Code 的核心体验的全能型文件编辑器，采用混合模式架构，根据文件后缀名自动切换渲染引擎，提供代码、Markdown 和富文本编辑能力。

## 技术栈
- **前端框架**: Vue 3 (Composition API)
- **样式**: Tailwind CSS
- **代码编辑器**: Monaco Editor + vite-plugin-monaco-editor
- **Markdown 编辑器**: Tiptap + tiptap-markdown
- **富文本编辑器**: Tiptap
- **状态管理**: Pinia
- **构建工具**: Vite

## 任务分解

### [ ] 任务 1: 项目结构搭建与依赖安装
- **优先级**: P0
- **Depends On**: None
- **Description**:
  - 安装必要的依赖包：
    - `monaco-editor` (代码编辑器)
    - `vite-plugin-monaco-editor` (Monaco Editor Vite 插件)
    - `tiptap-core` (Tiptap 核心)
    - `tiptap-markdown` (Markdown 支持)
    - `tiptap-extension-*` (各种编辑器扩展)
  - 配置 Vite 构建工具：
    - 添加 Monaco Editor 插件配置
    - 配置 web workers 支持
- **Success Criteria**:
  - 项目能够正常启动
  - 所有依赖安装成功
  - Vite 配置正确
- **Test Requirements**:
  - `programmatic` TR-1.1: `npm run dev` 命令能够正常执行
  - `programmatic` TR-1.2: 所有依赖包在 package.json 中正确配置
  - `programmatic` TR-1.3: Vite 配置文件包含 Monaco Editor 插件配置

### [ ] 任务 2: 编辑器外壳布局实现
- **优先级**: P0
- **Depends On**: 任务 1
- **Description**:
  - 实现 VS Code 风格的布局结构
  - 顶部 Tab 栏
  - 主编辑区域
  - 底部状态栏
- **Success Criteria**:
  - 布局结构完整且响应式
  - 界面风格与 VS Code 类似
- **Test Requirements**:
  - `human-judgement` TR-2.1: 布局结构符合设计要求
  - `programmatic` TR-2.2: 布局在不同屏幕尺寸下正常显示

### [ ] 任务 3: Monaco Editor 集成
- **优先级**: P1
- **Depends On**: 任务 2
- **Description**:
  - 封装 Monaco Editor 为独立组件
  - 实现异步加载以优化性能
  - 支持语法高亮、代码折叠、智能补全
- **Success Criteria**:
  - Monaco Editor 能够正常加载和使用
  - 支持代码文件的编辑
- **Test Requirements**:
  - `programmatic` TR-3.1: Monaco Editor 能够正常加载
  - `human-judgement` TR-3.2: 代码编辑体验流畅，支持语法高亮

### [ ] 任务 4: Tiptap Markdown 编辑器集成
- **优先级**: P1
- **Depends On**: 任务 2
- **Description**:
  - 封装 Tiptap 为 Markdown 编辑器组件
  - 实现所见即所得的 Markdown 编辑体验
  - 保持与 Monaco Editor 的视觉一致性
- **Success Criteria**:
  - Markdown 编辑器能够正常加载和使用
  - 支持 Markdown 文件的编辑
- **Test Requirements**:
  - `programmatic` TR-4.1: Tiptap 编辑器能够正常加载
  - `human-judgement` TR-4.2: Markdown 编辑体验流畅，支持所见即所得

### [ ] 任务 5: 富文本编辑器集成
- **优先级**: P2
- **Depends On**: 任务 2
- **Description**:
  - 封装 Tiptap 为富文本编辑器组件
  - 支持 .txt 和 .html 文件的编辑
- **Success Criteria**:
  - 富文本编辑器能够正常加载和使用
  - 支持文本文件的编辑
- **Test Requirements**:
  - `programmatic` TR-5.1: 富文本编辑器能够正常加载
  - `human-judgement` TR-5.2: 文本编辑体验流畅

### [ ] 任务 6: 动态编辑器切换
- **优先级**: P1
- **Depends On**: 任务 3, 任务 4, 任务 5, 任务 9
- **Description**:
  - 根据文件后缀名自动切换编辑器类型
  - 实现文件类型检测逻辑
  - 使用 `defineAsyncComponent` 异步加载编辑器组件
  - 建立编辑器类型映射表
  - 实现 `currentEditor` 计算属性
- **Success Criteria**:
  - 不同类型的文件能够自动切换到对应的编辑器
  - 切换过程流畅无卡顿
  - 编辑器组件异步加载优化性能
- **Test Requirements**:
  - `programmatic` TR-6.1: .js, .py, .json, .vue 文件使用 Monaco Editor
  - `programmatic` TR-6.2: .md 文件使用 Markdown 编辑器
  - `programmatic` TR-6.3: .txt, .html 文件使用富文本编辑器
  - `programmatic` TR-6.4: 编辑器组件使用异步加载

### [ ] 任务 7: 多文件 Tab 管理
- **优先级**: P1
- **Depends On**: 任务 2
- **Description**:
  - 实现多文件同时打开
  - Tab 切换功能
  - 文件关闭功能
- **Success Criteria**:
  - 能够同时打开多个文件
  - Tab 切换和关闭功能正常
- **Test Requirements**:
  - `programmatic` TR-7.1: 能够打开多个文件并显示为 Tab
  - `programmatic` TR-7.2: Tab 切换功能正常
  - `programmatic` TR-7.3: 关闭 Tab 功能正常

### [ ] 任务 8: 状态栏实现
- **优先级**: P2
- **Depends On**: 任务 2
- **Description**:
  - 实现文件路径显示
  - 编码格式显示
  - 光标位置显示
  - 保存状态显示
- **Success Criteria**:
  - 状态栏能够显示所有必要信息
  - 信息实时更新
- **Test Requirements**:
  - `programmatic` TR-8.1: 状态栏显示文件路径
  - `programmatic` TR-8.2: 状态栏显示光标位置
  - `programmatic` TR-8.3: 状态栏显示保存状态

### [ ] 任务 9: 状态管理实现
- **优先级**: P1
- **Depends On**: 任务 2
- **Description**:
  - 使用 Pinia 创建 `editorStore`
  - 管理 `openFiles` 数组（包含文件对象：{ name, path, content, type }）
  - 管理 `activeFilePath` 当前编辑文件路径
  - 管理 `isDirty` Set 记录未保存文件
  - 实现 `activeFile` 和 `editorType` getters
  - 实现 `openFile` 和 `closeFile` actions
  - 实现文件类型检测逻辑（根据后缀名）
- **Success Criteria**:
  - Pinia store 能够正确管理文件状态
  - 跨组件状态同步正常
  - 编辑器类型自动检测正确
- **Test Requirements**:
  - `programmatic` TR-9.1: Pinia store 能够正确管理文件状态
  - `programmatic` TR-9.2: 状态变更能够在组件间同步
  - `programmatic` TR-9.3: `editorType` getter 能够根据文件后缀名返回正确的编辑器类型

### [ ] 任务 10: 集成与测试
- **优先级**: P1
- **Depends On**: 所有其他任务
- **Description**:
  - 整合所有组件
  - 实现 `Ctrl + S` 快捷键全局监听
  - 实现保存逻辑（调用后端 API）
  - 进行功能测试
  - 性能优化
  - 内存管理（Monaco 编辑器 dispose）
- **Success Criteria**:
  - 所有功能正常工作
  - 性能良好，无明显卡顿
  - 内存管理良好，无内存泄漏
  - 保存功能正常
- **Test Requirements**:
  - `programmatic` TR-10.1: 所有编辑器功能正常
  - `human-judgement` TR-10.2: 整体用户体验流畅
  - `programmatic` TR-10.3: 性能测试通过
  - `programmatic` TR-10.4: 内存管理测试通过
  - `programmatic` TR-10.5: 保存功能测试通过

## 实现顺序
1. 任务 1: 项目结构搭建与依赖安装
2. 任务 2: 编辑器外壳布局实现
3. 任务 9: 状态管理实现
4. 任务 3: Monaco Editor 集成
5. 任务 4: Tiptap Markdown 编辑器集成
6. 任务 5: 富文本编辑器集成
7. 任务 6: 动态编辑器切换
8. 任务 7: 多文件 Tab 管理
9. 任务 8: 状态栏实现
10. 任务 10: 集成与测试

## 关键技术点
1. **Monaco Editor 异步加载**：使用 `defineAsyncComponent` 优化性能
2. **Monaco Workers 配置**：使用 `vite-plugin-monaco-editor` 处理 web workers 路径问题
3. **动态编辑器切换**：基于文件后缀名的条件渲染，使用组件映射表
4. **状态管理**：使用 Pinia 管理复杂的编辑器状态，包含 `editorType` 自动检测
5. **响应式布局**：使用 Tailwind CSS 实现 VS Code 风格的界面
6. **Tiptap Markdown 转换**：使用 `tiptap-markdown` 扩展实现 Markdown 字符串与编辑器状态的转换
7. **内存管理**：在组件销毁时调用 `monaco.dispose()` 避免内存泄漏
8. **保存逻辑**：实现 `Ctrl + S` 快捷键全局监听和保存功能

## 预期交付物
- 完整的全能型文件编辑器
- 支持多种文件类型的编辑
- VS Code 风格的用户界面
- 流畅的编辑体验