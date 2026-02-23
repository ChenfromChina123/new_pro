#!/bin/bash
# ==========================================
# AI 智能学习助手 - Linux 全量启动脚本
# 功能：Git 拉取、构建、启动所有必要服务
# 生产环境：word-game 接口使用域名 earthworm.aistudy.icu
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# 项目根目录（脚本所在目录）
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# ---------- 端口（与 Nginx 反代一致）----------
# aistudy.icu         -> 3000  主站前端
# earthworm.aistudy.icu -> 5010  单词记忆（word-game 前端+API）
# 主站后端 aispring   -> 5000
BACKEND_PORT=5000
FRONTEND_PORT=3000
WORD_GAME_PORT=5010

# ---------- 数据库（与宝塔/MySQL 一致，可按需改为环境变量）----------
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-aispring}"
export DB_USERNAME="${DB_USERNAME:-aispring}"
export DB_PASSWORD="${DB_PASSWORD:-xGDswMCdHhsajfxF}"

# ---------- 辅助函数 ----------
# 获取占用指定端口的进程 PID（取第一个）
get_port_pid() { lsof -ti:"$1" 2>/dev/null | head -n 1; }
# 检测端口冲突：若端口被占用则终止该进程
kill_port_if_used() {
  local port="$1"
  local name="${2:-端口 $port}"
  local pid
  pid=$(get_port_pid "$port")
  if [ -n "$pid" ]; then
    echo "⚠️  $name 端口 $port 已被占用 (PID: $pid)，正在终止..."
    kill -9 "$pid" 2>/dev/null || true
    sleep 1
    if [ -n "$(get_port_pid "$port")" ]; then
      echo "❌ 无法释放端口 $port，请手动检查后重试"
      exit 1
    fi
    echo "✅ 已释放端口 $port"
  fi
}
# 等待端口就绪（用于确认后端/服务真正监听），超时则提示查日志
# 参数: 端口 服务名 超时秒数
wait_for_port() {
  local port="$1" name="${2:-端口 $port}" timeout="${3:-60}" t=0
  while [ $t -lt "$timeout" ]; do
    [ -n "$(get_port_pid "$port")" ] && return 0
    sleep 2
    t=$((t + 2))
    echo "    等待 $name 监听端口 $port ... ${t}s"
  done
  echo "❌ $name 在 ${timeout}s 内未就绪，请查看 backend.log 排查（如数据库、Redis 连接等）"
  return 1
}
ensure_dir() {
  [ -d "$1" ] || { echo "❌ 错误: 找不到目录 $1"; exit 1; }
}

echo "=========================================="
echo "🚀 AI 智能学习助手 - 全量启动（Git 拉取 + 构建 + 启动）"
echo "=========================================="

# ========== 1. Git 拉取最新代码 ==========
echo ""
echo "【1/5】Git 拉取最新代码"
git pull || echo "⚠️  git pull 失败，继续使用本地代码"

# ========== 2. 安装依赖（已有 node_modules 则跳过，避免每次拉取）==========
echo ""
echo "【2/5】安装依赖"

ensure_dir "vue-app"
cd vue-app
if [ -d "node_modules" ]; then
  echo "    vue-app 依赖已存在，跳过 npm install"
else
  npm install
fi
cd "$PROJECT_ROOT"

ensure_dir "word-game"
cd word-game
if [ -d "node_modules" ]; then
  echo "    word-game 依赖已存在，跳过 npm install"
else
  npm install
fi
cd "$PROJECT_ROOT"

ensure_dir "aispring"
cd "$PROJECT_ROOT"

# ========== 3. 构建 ==========
echo ""
echo "【3/5】构建前端与 word-game"

# 主站前端
cd vue-app
npm run build
cd "$PROJECT_ROOT"

# 单词记忆：生产环境 API 使用 earthworm.aistudy.icu
cd word-game
export VITE_WORD_GAME_API_BASE="https://earthworm.aistudy.icu/api"
npm run build
cd "$PROJECT_ROOT"

# 后端 JAR（生产模式，不 clean 以复用依赖与增量构建）
cd aispring
mvn package -DskipTests -q
cd "$PROJECT_ROOT"

# ========== 4. 检测端口冲突并释放 ==========
echo ""
echo "【4/5】检测端口冲突"
kill_port_if_used "$BACKEND_PORT" "主站后端"
kill_port_if_used "$FRONTEND_PORT" "主站前端"
kill_port_if_used "$WORD_GAME_PORT" "单词记忆"

# ========== 5. 启动后端 aispring (5000) ==========
echo ""
echo "【5/5】启动服务"

BACKEND_PID=$(get_port_pid "$BACKEND_PORT")
if [ -n "$BACKEND_PID" ]; then
  echo "✅ 后端已在运行（端口 $BACKEND_PORT, PID: $BACKEND_PID）"
else
  ensure_dir "aispring"
  cd aispring
  JAR_FILE=$(ls -t target/*.jar 2>/dev/null | grep -v "original" | head -n 1)
  [ -z "$JAR_FILE" ] && { echo "❌ 未找到 aispring JAR"; exit 1; }
  JVM_OPTS="-Xms128m -Xmx256m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m"
  JVM_OPTS="$JVM_OPTS -XX:+UseG1GC -Dspring.profiles.active=prod"
  nohup java $JVM_OPTS -jar "$JAR_FILE" > ../backend.log 2>&1 &
  BACKEND_PID=$!
  echo "✅ 后端进程已启动（PID: $BACKEND_PID），等待端口 $BACKEND_PORT 就绪..."
  if wait_for_port "$BACKEND_PORT" "主站后端" 90; then
    echo "✅ 后端已就绪（端口 $BACKEND_PORT）"
  else
    echo "⚠️ 后端可能启动失败，请执行: tail -100 backend.log"
  fi
  cd "$PROJECT_ROOT"
fi

# ========== 5a. 启动主站前端 (3000) ==========
FRONTEND_PID=$(get_port_pid "$FRONTEND_PORT")
if [ -n "$FRONTEND_PID" ]; then
  echo "✅ 主站前端已在运行（端口 $FRONTEND_PORT, PID: $FRONTEND_PID）"
else
  ensure_dir "vue-app/dist"
  (cd vue-app && nohup npx --yes serve -s dist -l $FRONTEND_PORT > ../frontend.log 2>&1 &)
  sleep 1
  FRONTEND_PID=$(get_port_pid "$FRONTEND_PORT")
  echo "✅ 主站前端已启动（端口 $FRONTEND_PORT, PID: $FRONTEND_PID）"
fi

# ========== 5b. 启动 word-game (5010，earthworm.aistudy.icu 反代) ==========
WORD_PID=$(get_port_pid "$WORD_GAME_PORT")
if [ -n "$WORD_PID" ]; then
  echo "✅ 单词记忆(word-game)已在运行（端口 $WORD_GAME_PORT, PID: $WORD_PID）"
else
  ensure_dir "word-game/dist"
  ensure_dir "word-game/server"
  (cd word-game && PORT=$WORD_GAME_PORT nohup node server/index.js >> ../word-game.log 2>&1 &)
  sleep 1
  WORD_PID=$(get_port_pid "$WORD_GAME_PORT")
  echo "✅ 单词记忆(word-game)已启动（端口 $WORD_GAME_PORT, PID: $WORD_PID）"
  echo "   生产接口域名: https://earthworm.aistudy.icu"
fi

# ========== 汇总 ==========
echo ""
echo "=========================================="
echo "🎉 全量启动完成"
echo "=========================================="
echo "主站前端:    http://localhost:$FRONTEND_PORT  (aistudy.icu)"
echo "主站后端:    http://localhost:$BACKEND_PORT   (API)"
echo "单词记忆:    http://localhost:$WORD_GAME_PORT (earthworm.aistudy.icu)"
echo ""
echo "📋 进程: 后端 $BACKEND_PID | 前端 $FRONTEND_PID | word-game $WORD_PID"
echo "📝 日志: backend.log | frontend.log | word-game.log"
echo "🛑 停止: kill $BACKEND_PID $FRONTEND_PID $WORD_PID"
echo "=========================================="
