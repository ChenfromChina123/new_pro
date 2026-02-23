# Linux/macOS 启动脚本使用说明

## 权限设置

首次使用前，请给脚本添加执行权限：

```bash
chmod +x full_stack_launcher.sh
```

## 使用方法

```bash
./full_stack_launcher.sh
```

## 功能特性

- **环境检测**：自动检查Git、Java、Node.js、npm、Maven等必要工具
- **自动更新**：自动拉取最新的代码并合并
- **端口管理**：自动检测并清理占用的5000和3000端口
- **服务启动**：一键启动前后端服务
- **后台运行**：服务在后台运行，不影响终端使用
- **日志记录**：详细的操作日志和错误提示
- **进程管理**：显示服务进程ID，方便管理

## 系统要求

- Linux 或 macOS 操作系统
- Git
- Java 8+
- Maven
- Node.js
- npm

## 停止服务

如需停止服务，可使用以下命令：

```bash
# 查找进程ID
ps aux | grep -E "(spring-boot:run|npm run dev)"

# 或者根据启动时显示的PID停止服务
kill <PID>
```