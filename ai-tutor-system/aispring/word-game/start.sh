#!/bin/bash

# ==============================================================================
# word-game 服务器启动脚本 (Linux/宝塔专用)
# 功能：自动安装依赖并使用 PM2 启动后端 API 服务
# ==============================================================================

# 确保脚本在当前目录下运行
cd "$(dirname "$0")"

echo ">>> [1/3] 检查 Node.js 环境..."
node -v || { echo "错误: 未找到 Node.js，请先安装。"; exit 1; }

echo ">>> [2/4] 安装依赖项..."
npm install

echo ">>> [3/4] 构建前端静态资源 (用于生产环境)..."
# 如果 dist 目录不存在，或者你想强制更新，运行 build
npm run build

echo ">>> [4/4] 启动后端服务 (PM2)..."
# 生产环境下，后端服务 (5201) 会自动托管 dist 目录下的前端页面
# 检查是否安装了 pm2，如果没有则尝试临时运行
if command -v pm2 &> /dev/null
then
    pm2 stop word-game 2>/dev/null || true
    pm2 start server/index.js --name word-game --watch
    pm2 save
    echo ">>> 服务已在 PM2 中启动，你可以通过 'pm2 logs word-game' 查看日志。"
else
    echo "警告: 未检测到 PM2，将直接使用 Node 启动 (按 Ctrl+C 可停止)..."
    npm run server
fi
