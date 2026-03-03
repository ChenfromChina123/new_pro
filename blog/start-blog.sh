#!/bin/bash
# ==========================================
# 博客系统启动脚本
# 功能：启动 blog.aistudy.icu 博客服务
# 端口：3200
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# 配置 - 使用脚本所在目录（不跟随软链接）
BLOG_PORT=3200
# 获取脚本的绝对路径，不跟随软链接
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
BLOG_DIR="$SCRIPT_DIR"
BLOG_LOG="$BLOG_DIR/blog.log"

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

# 调试信息
echo "🔍 检测当前目录..."
echo "   PWD: $(pwd)"
echo "   脚本路径：$0"
echo "   脚本目录：$(dirname "$0")"
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
if [ -d "node_modules/next" ]; then
  echo "✅ 博客系统依赖已存在"
else
  echo "⚠️  未找到 node_modules 或 next 依赖，正在安装依赖..."
  rm -rf node_modules package-lock.json 2>/dev/null || true
  npm install
fi

# 检查构建
if [ -d ".next" ]; then
  echo "✅ 博客系统已构建"
else
  echo "⚠️  未找到 .next 构建目录，正在构建..."
  npm run build
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
  # 再次检查并尝试强制释放
  if [ -n "$(get_port_pid "$BLOG_PORT")" ]; then
    echo "    尝试强制终止所有占用进程..."
    lsof -ti:"$BLOG_PORT" 2>/dev/null | xargs -r kill -9 2>/dev/null || true
    sleep 1
  fi
  if [ -n "$(get_port_pid "$BLOG_PORT")" ]; then
    echo "❌ 无法释放端口 $BLOG_PORT，但将继续尝试启动..."
  else
    echo "✅ 已释放端口 $BLOG_PORT"
  fi
fi

# 启动服务
echo "🚀 正在启动博客系统..."
echo "💡 提示：首次启动可能需要 30-60 秒构建"

nohup npx next start -p $BLOG_PORT > "$BLOG_LOG" 2>&1 &
BLOG_LAUNCH_PID=$!

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
  echo "2. 检查依赖是否完整：cd tailwind-nextjs-starter-blog-main && npm install"
  echo "3. 查看详细日志：tail -f $BLOG_LOG"
  echo "4. 手动启动测试：cd tailwind-nextjs-starter-blog-main && npm run start"
  exit 1
fi
