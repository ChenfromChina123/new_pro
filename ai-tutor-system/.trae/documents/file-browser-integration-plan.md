# File Browser 集成计划

## 项目概述
将 GitHub 热门开源项目 File Browser (⭐ 22.3k+) 集成到当前的 AI 学习助手系统中，替换现有的 SFTP 文件管理器。

## File Browser 简介
- **技术栈**: Golang + Vue
- **核心特点**:
  - 可快速搭建私有网盘
  - 支持多用户权限控制（RBAC 角色管理）
  - 支持在线编辑文本/代码、Markdown 预览
  - 生成带密码/有效期的分享链接
  - 部署极简：单文件运行或 Docker 部署
  - 资源占用低

## 集成方案

### 方案选择：独立部署 + 前端嵌入
由于 File Browser 是一个完整的独立应用（Go 后端 + Vue 前端），我们采用以下集成策略：

1. **后端**: File Browser 作为独立服务运行在 8080 端口
2. **前端**: 通过 iframe 嵌入到现有 Vue 应用中
3. **认证**: 配置统一的用户认证

---

## 实施步骤

### 第一阶段：下载与部署 File Browser

#### 步骤 1: 下载 File Browser 二进制文件
```powershell
# Windows 下载命令
iwr -useb https://raw.githubusercontent.com/filebrowser/get/master/get.ps1 | iex
```

#### 步骤 2: 创建配置目录
```
ai-tutor-system/
├── filebrowser/
│   ├── filebrowser.exe    # 二进制文件
│   ├── database/          # 数据库目录
│   └── config/            # 配置目录
```

#### 步骤 3: 初始化配置
```bash
filebrowser config init
filebrowser config set --address 0.0.0.0
filebrowser config set --port 8080
filebrowser config set --root /path/to/files
```

#### 步骤 4: 创建启动脚本
创建 `start-filebrowser.bat` 用于启动服务

---

### 第二阶段：前端集成

#### 步骤 5: 创建 FileBrowserView.vue
创建新的 Vue 组件，通过 iframe 嵌入 File Browser：
```vue
<template>
  <div class="file-browser-container">
    <iframe 
      src="http://localhost:8080" 
      class="file-browser-iframe"
    />
  </div>
</template>
```

#### 步骤 6: 更新路由配置
在 `router/index.js` 中添加 File Browser 路由

#### 步骤 7: 更新导航菜单
在主导航中添加"文件管理"入口

---

### 第三阶段：用户认证集成

#### 步骤 8: 配置 File Browser 用户
- 创建与系统用户对应的 File Browser 用户
- 配置用户权限和目录访问

#### 步骤 9: 单点登录集成（可选）
- 研究 File Browser 的 JWT 认证机制
- 实现与现有系统的单点登录

---

### 第四阶段：清理旧代码

#### 步骤 10: 移除旧的 SFTP 管理器
- 删除 `SFTPManagerView.vue`
- 删除 `FilePanel.vue` 等相关组件
- 删除 `SFTPController.java` 等后端代码
- 删除 `sftpService.js` 等前端服务

---

## 文件变更清单

### 新增文件
1. `ai-tutor-system/filebrowser/` - File Browser 目录
2. `vue-app/src/views/FileBrowserView.vue` - 新的文件管理视图
3. `start-filebrowser.bat` - 启动脚本

### 修改文件
1. `vue-app/src/router/index.js` - 添加路由
2. `vue-app/src/components/Navbar.vue` - 添加导航入口

### 删除文件（可选）
1. `vue-app/src/views/SFTPManagerView.vue`
2. `vue-app/src/components/sftp/` 目录
3. `aispring/src/main/java/com/aispring/sftp/` 目录
4. `vue-app/src/services/sftpService.js`
5. `vue-app/src/stores/sftp.js`

---

## 预期效果

1. **更强大的文件管理**: 支持在线编辑、Markdown 预览、文件分享
2. **更好的用户体验**: 类似于主流网盘的操作界面
3. **更低的维护成本**: 使用成熟的开源项目，减少自定义代码
4. **更丰富的功能**: 多用户权限、分享链接、文件预览等

---

## 风险与注意事项

1. **端口冲突**: 确保 8080 端口未被占用
2. **用户同步**: 需要手动创建 File Browser 用户或实现自动同步
3. **跨域问题**: iframe 嵌入可能需要配置 CORS
4. **存储路径**: 需要配置正确的文件存储根目录
