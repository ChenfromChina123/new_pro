#!/bin/bash
# ==========================================
# 博客系统启动脚本
# 功能：启动 blog.aistudy.icu 博客服务
# 端口：3200
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# 配置
BLOG_PORT=3200
BLOG_DIR="$(cd "$(dirname "$0")/个人博客/tailwind-nextjs-starter-blog-main" && pwd)"
BLOG_LOG="$(cd "$(dirname "$0")" && pwd)/blog.log"

# 辅助函数
get_port_pid() {
  lsof -ti:"$1" 2>/dev/null | head -n 1
}

wait_for_port() {
  local port="$1"
  local name="${2:-服务}"
  local timeout="${3:-30}"
  local count=0
  
  while [ $count -lt $timeout ]; do
    if [ -n "$(get_port_pid "$port")" ]; then
      return 0
    fi
    sleep 1
    count=$((count + 1))
  done
  
  return 1
}

# 主流程
echo "=========================================="
echo "📖 博客系统启动脚本"
echo "=========================================="
echo "端口：$BLOG_PORT"
echo "目录：$BLOG_DIR"
echo "日志：$BLOG_LOG"
echo ""

# 检查 node
command -v node >/dev/null 2>&1 || { echo "❌ 未找到 node 命令，请先安装 Node.js"; exit 1; }
echo "✅ Node.js 已找到：$(node --version)"

# 检查目录
if [ ! -d "$BLOG_DIR" ]; then
  echo "❌ 博客系统目录不存在：$BLOG_DIR"
  exit 1
fi
echo "✅ 博客系统目录已找到"

# 检查依赖
cd "$BLOG_DIR"
if [ ! -d "node_modules" ]; then
  echo "⚠️  未找到 node_modules，正在安装依赖..."
  npm install
fi

# 检查端口
BLOG_PID=$(get_port_pid "$BLOG_PORT")
if [ -n "$BLOG_PID" ]; then
  echo "✅ 博客系统已在运行（端口 $BLOG_PORT, PID: $BLOG_PID）"
  echo ""
  echo "访问地址：http://localhost:$BLOG_PORT"
  echo "=========================================="
  exit 0
fi

# 释放端口（如果有残留）
echo "🔍 检测端口 $BLOG_PORT..."
if [ -n "$BLOG_PID" ]; then
  echo "⚠️  端口 $BLOG_PORT 已被占用 (PID: $BLOG_PID)，正在终止..."
  kill -9 "$BLOG_PID" 2>/dev/null || true
  sleep 2
fi

# 启动服务
echo "🚀 正在启动博客系统..."
echo "💡 提示：首次启动可能需要 30-60 秒构建"

export PORT=$BLOG_PORT
nohup npm run start > "$BLOG_LOG" 2>&1 &
BLOG_LAUNCH_PID=$!
unset PORT

echo "📋 博客系统进程已启动 (PID: $BLOG_LAUNCH_PID)"
echo "⏳ 等待端口 $BLOG_PORT 就绪..."

if wait_for_port "$BLOG_PORT" "博客系统" 60; then
  BLOG_PID=$(get_port_pid "$BLOG_PORT")
  echo ""
  echo "=========================================="
  echo "✅ 博客系统启动成功！"
  echo "=========================================="
  echo "访问地址：http://localhost:$BLOG_PORT"
  echo "域名访问：https://blog.aistudy.icu"
  echo "进程 PID: $BLOG_PID"
  echo "日志文件：$BLOG_LOG"
  echo ""
  echo "🛑 停止命令：kill $BLOG_PID"
  echo "📝 查看日志：tail -f $BLOG_LOG"
  echo "=========================================="
else
  echo ""
  echo "❌ 博客系统启动失败"
  echo "--- blog.log 最后 30 行 ---"
  tail -30 "$BLOG_LOG" 2>/dev/null || echo "(无日志)"
  echo "---"
  echo ""
  echo "建议："
  echo "1. 检查 node 版本：node --version"
  echo "2. 检查依赖是否完整：cd 个人博客/tailwind-nextjs-starter-blog-main && npm install"
  echo "3. 查看详细日志：tail -f $BLOG_LOG"
  echo "4. 手动启动测试：cd 个人博客/tailwind-nextjs-starter-blog-main && npm run start"
  exit 1
fi
