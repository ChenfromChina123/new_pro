#!/bin/bash
# ==========================================
# 博客系统启动脚本
# 功能：启动 blog.aistudy.icu 博客服务
# 端口：3200
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# 配置 - 使用脚本所在目录
BLOG_PORT=3200
# 获取脚本所在目录的绝对路径（不跟随软链接）
if [[ "$0" = /* ]]; then
  # 已经是绝对路径
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
else
  # 相对路径，转换为绝对路径
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
fi

# 检查是否有 tailwind-nextjs-starter-blog-main 子目录
if [ -d "$SCRIPT_DIR/tailwind-nextjs-starter-blog-main" ]; then
  BLOG_DIR="$SCRIPT_DIR/tailwind-nextjs-starter-blog-main"
else
  BLOG_DIR="$SCRIPT_DIR"
fi

BLOG_LOG="$BLOG_DIR/blog.log"

# 辅助函数 - 获取端口占用的 PID
get_port_pid() {
  get_port_pids "$1" | head -n 1
}

get_port_pids() {
  local port="$1"
  local pids=""

  if command -v ss >/dev/null 2>&1; then
    pids=$(ss -H -ltnp 2>/dev/null | awk -v p=":${port}" '$4 ~ p {print $NF}' | grep -oE 'pid=[0-9]+' | cut -d= -f2)
  fi

  if [ -z "$pids" ] && command -v lsof >/dev/null 2>&1; then
    pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null)
  fi

  if [ -z "$pids" ] && command -v netstat >/dev/null 2>&1; then
    pids=$(netstat -ltnp 2>/dev/null | awk -v p=":${port}" '$4 ~ p && $7 != "-" {print $7}' | cut -d/ -f1)
  fi

  if [ -z "$pids" ] && command -v fuser >/dev/null 2>&1; then
    pids=$(fuser -n tcp "$port" 2>/dev/null | tr ' ' '\n')
  fi

  if [ -n "$pids" ]; then
    echo "$pids" | tr ' ' '\n' | sed '/^$/d' | sort -u
  fi
}

is_pid_running() {
  local pid="$1"
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

is_pid_in_blog_dir() {
  local pid="$1"
  local cwd=""

  if ! is_pid_running "$pid"; then
    return 1
  fi

  if [ -d "/proc/$pid" ] && command -v readlink >/dev/null 2>&1; then
    cwd=$(readlink -f "/proc/$pid/cwd" 2>/dev/null || true)
    case "$cwd" in
      "$BLOG_DIR"|"$BLOG_DIR"/*) return 0 ;;
      *) return 1 ;;
    esac
  fi

  return 1
}

print_port_owners() {
  local port="$1"
  local pids=""
  pids=$(get_port_pids "$port")

  if [ -z "$pids" ]; then
    echo "   无监听进程"
    return 0
  fi

  while IFS= read -r pid; do
    [ -z "$pid" ] && continue
    if command -v ps >/dev/null 2>&1; then
      echo "   PID $pid: $(ps -p "$pid" -o args= 2>/dev/null || echo '未知命令')"
    else
      echo "   PID $pid"
    fi
  done <<< "$pids"
}

# 检查端口是否被占用
is_port_used() {
  local port="$1"
  [ -n "$(get_port_pids "$port")" ]
}

# 等待端口就绪
wait_for_port() {
  local port="$1"
  local name="${2:-服务}"
  local timeout="${3:-30}"
  local count=0

  while [ $count -lt $timeout ]; do
    if is_port_used "$port"; then
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

# 检查并释放端口
echo "🔍 检测端口 $BLOG_PORT..."
BLOG_PID=$(get_port_pid "$BLOG_PORT")
if [ -n "$BLOG_PID" ]; then
  echo "⚠️  端口 $BLOG_PORT 已被占用 (PID: $BLOG_PID)"
  echo " 正在停止旧进程..."

  # 停止进程
  kill -15 "$BLOG_PID" 2>/dev/null || true

  # 等待进程完全停止
  echo "⏳ 等待进程停止..."
  for i in {1..10}; do
    if [ -z "$(get_port_pid "$BLOG_PORT")" ]; then
      break
    fi
    sleep 1
  done

  # 如果还在运行，强制停止
  if [ -n "$(get_port_pid "$BLOG_PORT")" ]; then
    echo "⚠️  进程未响应，强制终止..."
    kill -9 "$BLOG_PID" 2>/dev/null || true
    sleep 2
  fi

  # 检查是否还有其他进程占用
  OTHER_PIDS=$(get_port_pids "$BLOG_PORT")
  if [ -n "$OTHER_PIDS" ]; then
    echo "⚠️  发现其他占用进程，一并终止..."
    echo "$OTHER_PIDS" | xargs -r kill -9 2>/dev/null || true
    sleep 2
  fi

  # 最终检查
  if [ -n "$(get_port_pid "$BLOG_PORT")" ]; then
    echo "❌ 无法释放端口 $BLOG_PORT，请手动处理"
    echo "   当前占用进程：$(get_port_pid "$BLOG_PORT")"
    exit 1
  else
    echo "✅ 端口 $BLOG_PORT 已释放"
  fi
else
  echo "✅ 端口 $BLOG_PORT 空闲"
fi

# 确认端口已释放
sleep 1
if [ -n "$(get_port_pid "$BLOG_PORT")" ]; then
  echo "❌ 端口 $BLOG_PORT 仍然被占用，无法启动"
  exit 1
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
  if ! is_pid_in_blog_dir "$BLOG_PID"; then
    echo ""
    echo "❌ 端口 $BLOG_PORT 已监听，但不是博客目录进程"
    echo "当前监听进程："
    print_port_owners "$BLOG_PORT"
    echo "日志文件：$BLOG_LOG"
    exit 1
  fi
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
