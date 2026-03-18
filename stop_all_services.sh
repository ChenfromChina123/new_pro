#!/bin/bash
# ==========================================
# AI Study Project - 停止所有服务
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo "=========================================="
echo "🛑 AI Study Project - 停止所有服务"
echo "=========================================="

# 端口配置
BACKEND_PORT=5000
FRONTEND_PORT=3000
CV_PORT=3100
BLOG_PORT=3200

# 辅助函数
get_port_pid() { lsof -ti:"$1" 2>/dev/null | head -n 1; }

stop_port() {
  local port="$1" name="${2:-端口 $port}"
  local pid
  pid=$(get_port_pid "$port")
  if [ -n "$pid" ]; then
    echo "🛑 正在停止 $name (端口 $port, PID: $pid)..."
    kill -9 "$pid" 2>/dev/null || true
    sleep 1
    if [ -n "$(get_port_pid "$port")" ]; then
      lsof -ti:"$port" 2>/dev/null | xargs -r kill -9 2>/dev/null || true
    fi
    echo "✅ $name 已停止"
  else
    echo "ℹ️  $name 未运行（端口 $port 未被占用）"
  fi
}

# 停止所有服务
stop_port "$BACKEND_PORT" "后端服务"
stop_port "$FRONTEND_PORT" "主站前端"
stop_port "$CV_PORT" "简历网站"
stop_port "$BLOG_PORT" "博客系统"

echo ""
echo "=========================================="
echo "✅ 所有服务已停止"
echo "=========================================="
