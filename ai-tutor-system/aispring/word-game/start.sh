#!/bin/bash

# ==============================================================================
# word-game 服务器全量启动脚本 (Linux/宝塔专用)
# 功能：强制安装所有依赖 (含 devDependencies) 并同时启动前后端服务
# ==============================================================================

# 确保脚本在当前目录下运行
cd "$(dirname "$0")"

echo ">>> [1/3] 检查 Node.js 环境..."
node -v || { echo "错误: 未找到 Node.js，请先安装。"; exit 1; }

echo ">>> [2/3] 正在全量安装依赖 (包含 Vite 等开发工具)..."
# 注意：不能使用 --production，因为前端开发模式 (5200) 依赖 devDependencies 中的 vite
npm install

echo ">>> [3/3] 正在通过 PM2 同时启动前后端服务..."
# 检查是否安装了 pm2
if command -v pm2 &> /dev/null
then
    # 1. 停止旧任务
    pm2 stop word-game-backend 2>/dev/null || true
    pm2 stop word-game-frontend 2>/dev/null || true

    # 2. 启动后端 API (5201 端口)
    pm2 start server/index.js --name word-game-backend

    # 3. 启动前端开发服务器 (5200 端口)
    # 使用 -- 传递参数给 npm run dev
    pm2 start npm --name word-game-frontend -- run dev

    pm2 save
    echo "============================================================"
    echo ">>> 服务已在 PM2 中双路启动！"
    echo ">>> 后端 API: 5201 端口 (任务名: word-game-backend)"
    echo ">>> 前端 Dev: 5200 端口 (任务名: word-game-frontend)"
    echo ">>> 你可以使用 'pm2 logs' 查看实时运行日志。"
    echo "============================================================"
else
    echo "警告: 未检测到 PM2，正在尝试在前台同步启动 (建议安装 pm2)..."
    echo "正在后台启动后端..."
    npm run server &
    echo "正在前台启动前端..."
    npm run dev
fi
