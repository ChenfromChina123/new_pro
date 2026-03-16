#!/bin/bash

# ==============================================================================
# word-game 服务器全量修复启动脚本 (V3)
# 功能：解决 native 模块版本不匹配、配置 Vite 监听所有网卡、启动双路服务
# ==============================================================================

# 确保脚本在当前目录下运行
cd "$(dirname "$0")"

echo ">>> [1/4] 检查 Node.js 环境..."
node -v || { echo "错误: 未找到 Node.js，请先安装。"; exit 1; }

echo ">>> [2/4] 安装依赖并重新编译 native 模块 (解决 better-sqlite3 报错)..."
npm install
# 修复 ERR_DLOPEN_FAILED: 重新针对当前 Node 版本编译 native 扩展
npm rebuild better-sqlite3 || { echo "警告: npm rebuild 失败，尝试强制重装 better-sqlite3..."; npm install better-sqlite3 --build-from-source; }

echo ">>> [3/4] 准备清理旧的 PM2 进程..."
if command -v pm2 &> /dev/null
then
    echo ">>> [PM2] 执行 pm2 kill 以彻底清除后台残留进程 (解决 pidusage 报错)..."
    pm2 kill 2>/dev/null || true
    sleep 2  # 等待 PM2 守护进程退出

    # 删除旧的日志文件以避免混淆
    rm -f ~/.pm2/logs/word-game-*.log

    echo ">>> [3.5/4] 强制清理端口占用 (防止 PM2 假死)..."
    # 检查并清理端口 5201 (后端) 和 5200 (前端)
    for port in 5201 5200; do
        # 获取占用端口的 PID (Windows netstat)
        pid=$(netstat -ano | grep ":$port" | awk '{print $5}' | head -n 1)
        if [ -n "$pid" ] && [ "$pid" != "0" ]; then
             echo ">>> 端口 $port 被 PID $pid 占用，正在强制终止..."
             taskkill //F //PID $pid >/dev/null 2>&1 || true
        fi
    done

    echo ">>> [4/4] 正在通过 PM2 同时启动前后端服务..."

    # 1. 启动后端 API (5201 端口)
    pm2 start server/index.js --name word-game-backend --max-memory-restart 512M

    # 2. 启动前端开发服务器 (5200 端口，配置 --host 允许外部访问)
    # 我们已经在 vite.config.ts 中加了 host: true，这里确保启动
    pm2 start npm --name word-game-frontend -- run dev

    pm2 save

    echo ">>> [5/5] 执行健康检查..."
    sleep 5

    # 检查后端端口 5201
    if netstat -ano | grep ":5201" | grep LISTEN > /dev/null; then
        echo ">>> ✅ 后端服务 (5201) 启动成功！"
    else
        echo ">>> ⚠️  [警告] 后端服务似乎启动失败，端口 5201 未被监听。"
        echo ">>> 请检查 pm2 logs word-game-backend"
    fi

    # 检查前端端口 5200
    if netstat -ano | grep ":5200" | grep LISTEN > /dev/null; then
        echo ">>> ✅ 前端服务 (5200) 启动成功！"
    else
        echo ">>> ⚠️  [警告] 前端服务似乎启动失败，端口 5200 未被监听。"
        echo ">>> 请检查 pm2 logs word-game-frontend"
    fi

    echo "============================================================"
    echo ">>> 修复启动完成！"
    echo ">>> 后端 API: 5201 端口 (任务名: word-game-backend)"
    echo ">>> 前端 Dev: 5200 端口 (任务名: word-game-frontend)"
    echo ">>> 提示：如果你访问 http://域名:5200 仍然 502，请确认："
    echo ">>> 1. 服务器安全组放行了 5200 和 5201 端口。"
    echo ">>> 2. 宝塔 Nginx 的反向代理指向了 127.0.0.1:5200。"
    echo ">>> 3. 运行 'pm2 logs' 查看是否有新的报错。"
    echo "============================================================"
else
    echo "警告: 未检测到 PM2，正在尝试在前台启动..."
    npm run server &
    npm run dev
fi
