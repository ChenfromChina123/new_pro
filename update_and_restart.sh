#!/bin/bash
# ==========================================
# AI Study Project - 自动更新并重启服务
# 功能：拉取最新代码、构建前端、重启所有服务
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo "=========================================="
echo "🚀 AI Study Project - 自动更新并重启"
echo "=========================================="
echo ""

# 1. 停止所有服务
echo "【1/4】停止所有服务..."
if [ -f "stop_all_services.sh" ]; then
    bash stop_all_services.sh
else
    echo "⚠️  未找到 stop_all_services.sh，手动停止..."
    kill -9 $(lsof -ti:5000) 2>/dev/null || true
    kill -9 $(lsof -ti:3000) 2>/dev/null || true
    kill -9 $(lsof -ti:3100) 2>/dev/null || true
    kill -9 $(lsof -ti:3200) 2>/dev/null || true
fi
sleep 2
echo "✅ 所有服务已停止"
echo ""

# 2. 拉取最新代码
echo "【2/4】拉取最新代码..."
cd ai-tutor-system

# 检查是否有未提交的修改
if [ -n "$(git status --porcelain)" ]; then
    echo "⚠️  检测到未提交的修改，正在暂存..."
    git stash
fi

# 拉取最新代码
echo ">>> 正在从远程仓库拉取最新代码..."
git fetch --all
git reset --hard origin/earthworm-deploy
git pull origin earthworm-deploy

echo "✅ 代码更新完成"
echo "最新提交："
git log -1 --oneline
cd "$PROJECT_ROOT"
echo ""

# 3. 构建项目
echo "【3/4】构建项目..."

# 构建后端
echo ">>> 构建后端..."
cd ai-tutor-system/aispring
mvn clean package -DskipTests
echo "✅ 后端构建完成"
cd "$PROJECT_ROOT"

# 构建前端
echo ">>> 构建前端..."
cd ai-tutor-system/vue-app
npm install
npm run build
echo "✅ 前端构建完成"
cd "$PROJECT_ROOT"
echo ""

# 4. 启动所有服务
echo "【4/4】启动所有服务..."
if [ -f "start_all_services.sh" ]; then
    bash start_all_services.sh
else
    echo "❌ 未找到 start_all_services.sh"
    exit 1
fi

echo ""
echo "=========================================="
echo "🎉 更新并重启完成！"
echo "=========================================="
echo ""
