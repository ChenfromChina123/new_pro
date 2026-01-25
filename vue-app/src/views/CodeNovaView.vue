<template>
  <div class="codenova-container">
    <div class="header-section">
      <p align="center">
        <img src="/docs/assets/icon.png" width="150" alt="CodeNova" class="logo">
      </p>
      
      <h1 align="center">
        <b>CodeNova</b>
      </h1>
      
      <p align="center" class="tagline">
        随时随地写代码
      </p>
      
      <p align="center" class="badges">
        <a href="#"><img src="https://img.shields.io/badge/License-GPLv3-blue" alt="GPLv3 License"></a>
        <img src="https://img.shields.io/badge/Active%20Users-250%2B-blue" alt="Users">
        <img src="https://img.shields.io/badge/Api-Android%208%2B-brightgreen" alt="Android">
      </p>

      <div class="download-area" align="center">
        <a href="/CodeNova.apk" class="download-btn" download>
          <i class="fas fa-download"></i> 下载 CodeNova APK (v1.1.0)
        </a>
        <p class="download-sub">
          或者访问 <a href="https://github.com/CodeNova/CodeNova/releases" target="_blank">GitHub Releases</a>
        </p>
      </div>
    </div>

    <div class="content-body">
      <section>
        <h2>仓库简介</h2>
        <p>本仓库包含 <strong>CodeNova</strong> 的开源代码 —— 这是一个基于移动端的集成开发环境 (IDE) 和代码编辑器，用于直接在 Android 设备上构建和管理项目。</p>
      </section>

      <section>
        <h2>技术文档</h2>
        <ul>
          <li>项目版本信息、技术栈与关键技术细节： <a href="#">项目技术文档</a></li>
        </ul>
      </section>

      <section>
        <h2>用户界面</h2>
        <div class="screenshots-row">
          <img src="/docs/assets/intro.png" alt="Intro Screen"/>
          <img src="/docs/assets/editor.png" alt="Code Editor"/>
          <img src="/docs/assets/breadcrumbs.png" alt="Breadcrumb Navigation"/>
        </div>
        
        <div class="screenshots-row">
          <img src="/docs/assets/filetree.png" alt="File Tree Explorer"/>
          <img src="/docs/assets/jsconsole.png" alt="JS Console"/>
          <img src="/docs/assets/site_preview.png" alt="Web Preview"/>
        </div>
        
        <div class="screenshots-row">
          <img src="/docs/assets/bin_dect.png" alt="Binary Detection"/>
          <img src="/docs/assets/edit_ops.png" alt="Edit Ops"/>
          <img src="/docs/assets/syntax_var.png" alt="Syntax"/>
        </div>
      </section>

      <section>
        <h2>功能特性</h2>
        
        <h3>已实现（核心功能）</h3>
        <ul class="task-list">
          <li><input type="checkbox" checked disabled> 平板设备支持</li>
          <li><input type="checkbox" checked disabled> ZIP 项目导入</li>
          <li><input type="checkbox" checked disabled> 文件系统浏览器</li>
          <li><input type="checkbox" checked disabled> 面包屑导航</li>
          <li><input type="checkbox" checked disabled> 多窗格标签界面</li>
          <li><input type="checkbox" checked disabled> 实时文件监控</li>
          <li><input type="checkbox" checked disabled> 会话状态持久化</li>
          <li><input type="checkbox" checked disabled> 全方位持久化架构（集成 UI 状态、Agent 循环、LLM 配置与聊天上下文的深度保存与恢复）</li>
          <li><input type="checkbox" checked disabled> 编辑器文件异常容错（缺失/不可读文件不闪退）</li>
          <li><input type="checkbox" checked disabled> 启动阶段第三方运行时初始化容错（异常不阻断启动）</li>
          <li><input type="checkbox" checked disabled> Python 运行增强（脚本目录作为工作目录、无输出提示、实时日志输出路由）</li>
          <li><input type="checkbox" checked disabled> Android 10+ 兼容性增强（通过 Linker 绕过 W^X 限制，优化 Python 进程通信）</li>
          <li><input type="checkbox" checked disabled> Python Pip 包管理支持（自动引导安装 pip，修复 runpy 启动问题）</li>
          <li><input type="checkbox" checked disabled> Python 运行时升级支持（支持通过 <code>updates/</code> 目录放置新版资源包实现热更新）</li>
          <li><input type="checkbox" checked disabled> 终端 UI 整合（底部栏第三标签页整合原生 PTY 终端）</li>
          <li><input type="checkbox" checked disabled> 终端交互增强（新增 ESC, TAB, CTRL, ALT, 方向键等特殊按键栏，支持锁定反馈）</li>
          <li><input type="checkbox" checked disabled> 终端界面美化（简化提示符 PS1，支持色彩显示，优化移动端布局）</li>
          <li><input type="checkbox" checked disabled> C/C++ 语言编译运行支持（集成 clang 工具链，支持自动环境引导与权限修复）</li>
          <li><input type="checkbox" checked disabled> 自动化运行流（点击运行自动弹出半屏终端并执行命令）</li>
          <li><input type="checkbox" checked disabled> 实时构建输出（Python 执行结果实时同步至"构建输出"面板）</li>
          <li><input type="checkbox" checked disabled> 虚拟终端 Pane（用于展示运行输出）</li>
          <li><input type="checkbox" checked disabled> 终端会话重连（复用已有会话，关闭/重开终端 Pane 不丢上下文）</li>
          <li><input type="checkbox" checked disabled> 终端输出冗余内容清理（通过临时脚本封装执行，简化环境变量展示，保持界面整洁）</li>
          <li><input type="checkbox" checked disabled> 增强终端管理逻辑（基于窗口检测而非内存 ID 记录，支持应用重启后的会话持久化识别与同步关闭）</li>
          <li><input type="checkbox" checked disabled> 终端跳转链接（聊天输出/运行卡片支持一键定位到对应终端会话）</li>
          <li><input type="checkbox" checked disabled> AI 助手聊天（支持流式 Markdown 与代码块复制）</li>
          <li><input type="checkbox" checked disabled> AI 会话双轨存储（用户可见历史 / 系统完整上下文）</li>
          <li><input type="checkbox" checked disabled> 用户消息仅展示原始输入（不展示注入上下文头）</li>
          <li><input type="checkbox" checked disabled> 优化 Agent 工具调用渲染（实现数据分离存储、精简 UI 消息展示）</li>
          <li><input type="checkbox" checked disabled> 优化 Agent 运行状态显示（文件夹创建显示为 "Create" 等）</li>
          <li><input type="checkbox" checked disabled> 设置界面 UI 优化（分组配置、图标展示与全屏 Overlay）</li>
          <li><input type="checkbox" checked disabled> 首次启动引导：支持选择默认工作区路径</li>
          <li><input type="checkbox" checked disabled> 欢迎页面增强：新增"新建工作区"与"最近工作区"列表，支持一键创建空目录并实时同步历史记录</li>
          <li><input type="checkbox" checked disabled> Agent 功能增强：支持文件夹创建、结构化多处编辑工具（<code>&lt;edit_lines&gt;</code>）、Token 动态压缩策略与自动偏移校准</li>
          <li><input type="checkbox" checked disabled> Agent 上下文优化：路径变更按需注入、压缩后保留完整本地历史并精简发送内容</li>
          <li><input type="checkbox" checked disabled> Agent 交互增强：新增"停止循环"功能，实时控制自主执行流程</li>
          <li><input type="checkbox" checked disabled> Agent 交互优化：优化聊天界面布局，实现统一滚动与圆角 UI 联动</li>
          <li><input type="checkbox" checked disabled> Agent 交互动画优化：实现工具调用与 LLM 思考期间的动态加载动画（ProgressBar 与 "Thinking..." 状态提示），引入异常容错机制确保状态同步，提升 UI 响应感知度</li>
          <li><input type="checkbox" checked disabled> Agent 消息精准更新：基于全局唯一 ID 的消息定位机制，确保工具执行状态（如 Spinner 隐藏）能准确同步到对应的 UI 卡片，避免高频调用下的状态错乱</li>
          <li><input type="checkbox" checked disabled> Agent 选中提问功能（支持单次携带多处代码片段，自动注入文件路径与行号上下文）</li>
          <li><input type="checkbox" checked disabled> Agent 工具增强：新增 <code>&lt;check_syntax&gt;</code> 工具，支持对 Python 代码进行静态语法校验（支持文件修改后自动触发）</li>
          <li><input type="checkbox" checked disabled> Agent 性能优化：工具输出结果按需 JSON 化，读取文件保持行号与缩进清晰（支持动态行号对齐），优先使用结构化精确编辑支持</li>
          <li><input type="checkbox" checked disabled> Markdown 增强：支持表格渲染与高级排版优化（集成 Markwon Table/Strikethrough/TaskList 插件，自定义表格主题与间距，统一 commonmark 0.17.1 解决崩溃问题，修复流式渲染表格错乱问题）</li>
          <li><input type="checkbox" checked disabled> 交互反馈优化：Agent 执行工具（写入、读取、运行、思考）时增加动态 Emoji 循环图案与打点动画，显著提升运行感知度</li>
          <li><input type="checkbox" checked disabled> Agent 循环连续性优化：支持在路由到终端进行交互时保持 Agent 循环不中断，并在终端会话结束后（用户完成交互）自动恢复 Agent 运行流程，支持交互式任务的超时自动延长</li>
          <li><input type="checkbox" checked disabled> Agent 代码变更管理：新增专门的变更展示组件，支持多文件变更列表化查看、Diff 预览与一键撤销功能</li>
          <li>
             Agent 交互与历史管理增强：
             <ul class="task-list">
               <li><input type="checkbox" checked disabled> 消息回撤与上下文栈回滚（支持撤销代码修改与对话分支清理）</li>
               <li><input type="checkbox" checked disabled> 会话历史快速导航（支持展示所有用户消息摘要并一键跳转）</li>
               <li><input type="checkbox" checked disabled> Agent 任务 UI 重构：支持未开始、进行中、已完成三种状态，优化视觉边界</li>
               <li><input type="checkbox" checked disabled> 用户自定义规则注入：支持在设置中配置 1000 字符上限的开发规范，并按需（首次对话、路由切换、上下文压缩）自动注入上下文</li>
             </ul>
          </li>
          <li><input type="checkbox" checked disabled> 构建排查与依赖管理</li>
          <li><input type="checkbox" checked disabled> 跨存储卷访问</li>
          <li><input type="checkbox" checked disabled> 二进制/文本启发式检测</li>
          <li><input type="checkbox" checked disabled> 多语言语法高亮</li>
          <li><input type="checkbox" checked disabled> 高级代码编辑功能</li>
          <li><input type="checkbox" checked disabled> 强大的离线编辑功能</li>
          <li><input type="checkbox" checked disabled> 全面优化启动速度（异步化耗时初始化任务，UI 线程安全更新）</li>
          <li><input type="checkbox" checked disabled> QPython 运行时全面集成（自动软链接、环境变量动态注入、预加载优化）</li>
          <li><input type="checkbox" checked disabled> Android 10+ Linker 包装器方案（绕过 W^X 执行限制，确保 Python 运行时在私有目录正常运行）</li>
        </ul>

        <h3>已实现（Web 与服务器）</h3>
        <ul class="task-list">
          <li><input type="checkbox" checked disabled> 本地服务器测试（localhost/WiFi 热点）</li>
          <li><input type="checkbox" checked disabled> Web 开发工具</li>
        </ul>

        <h3>开发中</h3>
        <ul class="task-list">
          <li><input type="checkbox" disabled> Git 集成 (部分完成)
            <ul class="task-list">
              <li><input type="checkbox" checked disabled> 克隆仓库多策略候选与失败汇总提示</li>
            </ul>
          </li>
          <li><input type="checkbox" disabled> 自动补全 (部分完成)</li>
          <li><input type="checkbox" checked disabled> 命令面板
             <ul class="task-list">
               <li><input type="checkbox" checked disabled> 行号跳转</li>
               <li><input type="checkbox" disabled> 文件跳转</li>
               <li><input type="checkbox" disabled> 帮助</li>
               <li><input type="checkbox" disabled> 动作</li>
               <li><input type="checkbox" checked disabled> 语法切换</li>
               <li><input type="checkbox" disabled> 最近文档</li>
               <li><input type="checkbox" disabled> 符号跳转</li>
               <li><input type="checkbox" disabled> 命令</li>
             </ul>
          </li>
          <li><input type="checkbox" disabled> 中文界面本地化（已完成）</li>
        </ul>

        <h3>路线图</h3>
        <ul class="task-list">
          <li><input type="checkbox" disabled> 调试工具</li>
          <li><input type="checkbox" disabled> 插件生态</li>
          <li><input type="checkbox" disabled> 完整 Git 集成</li>
          <li><input type="checkbox" checked disabled> 终端集成（基于 Termux）</li>
          <li><input type="checkbox" checked disabled> Termux Shared 基础库集成（termux-shared）</li>
          <li><input type="checkbox" disabled> AI 助手集成</li>
          <li><input type="checkbox" disabled> 智能代码补全</li>
        </ul>
      </section>

      <section>
        <h2>快速开始</h2>
        <p>当前软件版本：<code>v1.1.0</code></p>
        
        <div class="alert alert-important">
          <p><strong>IMPORTANT</strong></p>
          <ul>
            <li>安装前，请确保您的设备设置中已启用 <strong>允许安装来自未知来源的应用</strong>。</li>
            <li>请务必仅从受信任的来源安装 CodeNova。</li>
          </ul>
        </div>
        
        <p><a href="/CodeNova.apk" download>https://github.com/CodeNova/CodeNova/releases/download/v1.1.0/CodeNova.v1.1.0.apk</a></p>

        <h3>安装步骤</h3>
        <ol>
          <li>访问 <a href="https://github.com/CodeNova/CodeNova/releases">https://github.com/CodeNova/CodeNova/releases</a> 页面</li>
          <li>下载最新的 CodeNova APK</li>
          <li>在您的设备上安装 APK</li>
          <li>随时随地开始编码</li>
        </ol>
      </section>

      <section>
        <h2>构建与签名</h2>
        <ul>
          <li>本地构建：使用 <code>./gradlew assembleDebug</code> 或 <code>./gradlew assemble</code>。</li>
          <li>Release 签名：CI 会从 <code>app/codenova.keystore.enc</code> 解密输出到 <code>app/codenova.keystore</code>，并通过环境变量/<code>secrets.properties</code> 提供签名密码与别名。</li>
        </ul>
      </section>

      <section>
        <h2>贡献指南</h2>
        <div class="alert alert-note">
          <p><strong>NOTE</strong></p>
          <ul>
            <li>Pull requests 应提交至：<code>dev</code> 分支</li>
          </ul>
        </div>
        <p>请参考 <a href="#">贡献指南</a> 了解如何贡献的详细说明。</p>
      </section>

      <section>
        <h2>许可</h2>
        <p>版权所有 (c) CodeNova Team. 保留所有权利。</p>
        <p>基于 <a href="#">GNU GPLv3</a> 许可。</p>
      </section>

      <section>
        <h2>致谢</h2>
        <p>非常感谢所有贡献者、测试人员和开源社区让 CodeNova 成为可能。</p>
      </section>
    </div>
    
    <div class="footer">
      <p>&copy; 2024 CodeNova. All rights reserved.</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';

onMounted(() => {
  document.title = 'CodeNova - 随时随地写代码';
});
</script>

<style scoped>
.codenova-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #24292f;
  line-height: 1.6;
}

.header-section {
  margin-bottom: 2rem;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 2rem;
}

.tagline {
  font-size: 1.5rem;
  color: #57606a;
  margin-bottom: 1rem;
}

.badges {
  display: flex;
  justify-content: center;
  gap: 5px;
  margin-bottom: 1.5rem;
}

.download-area {
  margin-top: 2rem;
  padding: 1.5rem;
  background-color: #f6f8fa;
  border-radius: 6px;
  border: 1px solid #d0d7de;
}

.download-btn {
  display: inline-block;
  background-color: #2da44e;
  color: white;
  padding: 12px 24px;
  font-size: 1.1rem;
  font-weight: 600;
  border-radius: 6px;
  text-decoration: none;
  transition: background-color 0.2s;
}

.download-btn:hover {
  background-color: #2c974b;
}

.download-sub {
  margin-top: 10px;
  font-size: 0.9rem;
  color: #57606a;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

h2 {
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

h3 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

section {
  margin-bottom: 2rem;
}

.screenshots-row {
  display: flex;
  justify-content: center;
  gap: 2%;
  margin-bottom: 1rem;
}

.screenshots-row img {
  max-width: 100%;
  height: auto;
  width: 32%;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12);
}

.task-list {
  list-style-type: none;
  padding-left: 0;
}

.task-list-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 0.25rem;
}

.task-list-item input {
  margin-right: 0.5em;
  margin-top: 0.3em;
}

.alert {
  padding: 1rem;
  margin-bottom: 1rem;
  border-left: 0.25rem solid transparent;
  background-color: #fff;
  border: 1px solid #d0d7de;
  border-radius: 6px;
}

.alert-important {
  border-left-color: #8250df;
  background-color: #f6f8fa;
}

.alert-note {
  border-left-color: #0969da;
  background-color: #f6f8fa;
}

code {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(175, 184, 193, 0.2);
  border-radius: 6px;
  font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, Liberation Mono, monospace;
}

a {
  color: #0969da;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.footer {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 1px solid #eaecef;
  text-align: center;
  color: #57606a;
}
</style>