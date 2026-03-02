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
# cv.aistudy.icu      -> 3100  个人简历网站
# blog.aistudy.icu    -> 3200  博客系统
# 主站后端 aispring   -> 5000
BACKEND_PORT=5000
FRONTEND_PORT=3000
WORD_GAME_PORT=5010
CV_PORT=3100
BLOG_PORT=3200

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

# 博客系统：检查依赖
BLOG_DIR="$PROJECT_ROOT/个人博客/tailwind-nextjs-starter-blog-main"
if [ -d "$BLOG_DIR" ]; then
  ensure_dir "$BLOG_DIR"
  cd "$BLOG_DIR"
  if [ -d "node_modules" ]; then
    echo "    博客系统依赖已存在，跳过 npm install"
  else
    echo "    正在安装博客系统依赖..."
    npm install
  fi
  cd "$PROJECT_ROOT"
fi

ensure_dir "aispring"
cd "$PROJECT_ROOT"

# ========== 3. 构建 ==========
echo ""
echo "【3/5】构建前端与 word-game"

# 主站前端
cd vue-app
if [ -d "dist" ]; then
  echo "    主站前端已构建，跳过 build"
else
  npm run build
fi
cd "$PROJECT_ROOT"

# 单词记忆：生产环境 API 使用 earthworm.aistudy.icu
cd word-game
export VITE_WORD_GAME_API_BASE="https://earthworm.aistudy.icu/api"
if [ -d "dist" ]; then
  echo "    word-game 已构建，跳过 build"
else
  npm run build
fi
cd "$PROJECT_ROOT"

# 博客系统：如果有 .next 目录则跳过构建
if [ -d "$BLOG_DIR/.next" ]; then
  echo "    博客系统已构建，跳过 build"
else
  echo "    正在构建博客系统..."
  cd "$BLOG_DIR"
  npm run build
  cd "$PROJECT_ROOT"
fi

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
kill_port_if_used "$CV_PORT" "简历网站"
kill_port_if_used "$BLOG_PORT" "博客系统"

# ========== 5. 启动后端 aispring (500)0 ==========
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
  # 显式传入数据库环境变量，确保 JAR 内 Spring 能解析 datasource.url
  nohup env DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_NAME="$DB_NAME" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" java $JVM_OPTS -jar "$JAR_FILE" > ../backend.log 2>&1 &
  BACKEND_PID=$!
  cd "$PROJECT_ROOT"
  echo "✅ 后端进程已启动（PID: $BACKEND_PID），等待端口 $BACKEND_PORT 就绪（下方为实时后端日志）..."
  tail -f backend.log &
  TAIL_PID=$!
  wait_for_port "$BACKEND_PORT" "主站后端" 90
  WPR=$?
  kill $TAIL_PID 2>/dev/null || true
  if [ $WPR -eq 0 ]; then
    echo "✅ 后端已就绪（端口 $BACKEND_PORT）"
  else
    echo "⚠️ 后端可能启动失败，请执行: tail -100 backend.log"
  fi
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

# ========== 5b. 启动简历网站 (3100，cv.aistudy.icu 反代) ==========
CV_PID=$(get_port_pid "$CV_PORT")
if [ -n "$CV_PID" ]; then
  echo "✅ 简历网站已在运行（端口 $CV_PORT, PID: $CV_PID）"
else
  command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1 || { echo "❌ 未找到 Python 命令，无法启动简历网站，请先安装 Python"; exit 1; }
  CV_DIR="$PROJECT_ROOT/cv-site"
  if [ ! -d "$CV_DIR" ]; then
    echo "⚠️  简历网站目录不存在，跳过启动"
  else
    CV_LOG="$PROJECT_ROOT/cv-site.log"
    echo "    正在启动简历网站（端口 $CV_PORT, 日志：cv-site.log）..."
    # 使用 Python 简单 HTTP 服务器
    export PORT=$CV_PORT
    (cd "$CV_DIR" && nohup python3 -m http.server $CV_PORT >> "$CV_LOG" 2>&1 || python -m http.server $CV_PORT >> "$CV_LOG" 2>&1 &)
    sleep 2
    CV_PID=$(get_port_pid "$CV_PORT")
    if [ -n "$CV_PID" ]; then
      echo "✅ 简历网站已启动（端口 $CV_PORT, PID: $CV_PID）"
      echo "   访问地址：http://localhost:$CV_PORT (cv.aistudy.icu)"
      echo "   日志：$CV_LOG"
    else
      echo "⚠️  简历网站启动中，请等待片刻..."
    fi
    unset PORT
  fi
fi

# ========== 5c. 启动 word-game (5010，earthworm.aistudy.icu 反代) ==========
WORD_GAME_LOG="$PROJECT_ROOT/word-game.log"
WORD_PID=$(get_port_pid "$WORD_GAME_PORT")
if [ -n "$WORD_PID" ]; then
  echo "✅ 单词记忆(word-game)已在运行（端口 $WORD_GAME_PORT, PID: $WORD_PID）"
else
  command -v node >/dev/null 2>&1 || { echo "❌ 未找到 node 命令，无法启动 word-game，请先安装 Node.js"; exit 1; }
  ensure_dir "word-game/dist"
  ensure_dir "word-game/server"
  echo "    正在启动 word-game（端口 $WORD_GAME_PORT，日志: word-game.log）..."
  echo "    node 路径: $(command -v node)"
  # 使用绝对路径写日志；在主 shell 中 export PORT 再启动，确保 node 收到环境变量
  export PORT=$WORD_GAME_PORT
  (cd "$PROJECT_ROOT/word-game" && nohup node server/index.js >> "$WORD_GAME_LOG" 2>&1 &)
  WORD_LAUNCH_PID=$!
  unset PORT
  echo "    word-game 进程已 fork (PID: $WORD_LAUNCH_PID)，等待端口 $WORD_GAME_PORT 就绪..."
  if wait_for_port "$WORD_GAME_PORT" "word-game" 20; then
    WORD_PID=$(get_port_pid "$WORD_GAME_PORT")
    echo "✅ 单词记忆(word-game)已就绪（端口 $WORD_GAME_PORT, PID: $WORD_PID）"
    echo "   生产接口域名: https://earthworm.aistudy.icu"
    echo "   日志: $WORD_GAME_LOG"
  else
    echo "❌ word-game 在 20s 内未监听端口 $WORD_GAME_PORT，请查看日志:"
    echo "--- word-game.log 最后 30 行 ---"
    tail -30 "$WORD_GAME_LOG" 2>/dev/null || echo "(无日志或文件不存在)"
    echo "---"
    WORD_PID=$(get_port_pid "$WORD_GAME_PORT")
    [ -z "$WORD_PID" ] && echo "   建议: 检查 node 版本、word-game/node_modules 是否已安装，或执行: node word-game/server/index.js 查看报错"
  fi
fi

# ========== 5d. 启动博客系统 (3200，blog.aistudy.icu 反代) ==========
BLOG_LOG="$PROJECT_ROOT/blog.log"
BLOG_PID=$(get_port_pid "$BLOG_PORT")
if [ -n "$BLOG_PID" ]; then
  echo "✅ 博客系统已在运行（端口 $BLOG_PORT, PID: $BLOG_PID）"
else
  command -v node >/dev/null 2>&1 || { echo "❌ 未找到 node 命令，无法启动博客系统，请先安装 Node.js"; exit 1; }
  BLOG_DIR="$PROJECT_ROOT/个人博客/tailwind-nextjs-starter-blog-main"
  if [ ! -d "$BLOG_DIR" ]; then
    echo "⚠️  博客系统目录不存在，跳过启动"
  else
    ensure_dir "$BLOG_DIR/.next"
    echo "    正在启动博客系统（端口 $BLOG_PORT, 日志：blog.log）..."
    echo "    node 路径：$(command -v node)"
    export PORT=$BLOG_PORT
    (cd "$BLOG_DIR" && nohup npm run start >> "$BLOG_LOG" 2>&1 &)
    BLOG_LAUNCH_PID=$!
    unset PORT
    echo "    博客系统进程已 fork (PID: $BLOG_LAUNCH_PID)，等待端口 $BLOG_PORT 就绪..."
    if wait_for_port "$BLOG_PORT" "博客系统" 30; then
      BLOG_PID=$(get_port_pid "$BLOG_PORT")
      echo "✅ 博客系统已就绪（端口 $BLOG_PORT, PID: $BLOG_PID）"
      echo "   访问地址：http://localhost:$BLOG_PORT (blog.aistudy.icu)"
      echo "   日志：$BLOG_LOG"
    else
      echo "❌ 博客系统在 30s 内未监听端口 $BLOG_PORT，请查看日志:"
      echo "--- blog.log 最后 30 行 ---"
      tail -30 "$BLOG_LOG" 2>/dev/null || echo "(无日志或文件不存在)"
      echo "---"
      BLOG_PID=$(get_port_pid "$BLOG_PORT")
      [ -z "$BLOG_PID" ] && echo "   建议：检查 node 版本、博客系统 node_modules 是否已安装，或执行：cd 个人博客/tailwind-nextjs-starter-blog-main && npm run start 查看报错"
    fi
  fi
fi

# ========== 汇总 ==========
echo ""
echo "=========================================="
echo "🎉 全量启动完成"
echo "=========================================="
echo "主站前端：   http://localhost:$FRONTEND_PORT  (aistudy.icu)"
echo "简历网站：   http://localhost:$CV_PORT        (cv.aistudy.icu)"
echo "主站后端：   http://localhost:$BACKEND_PORT   (API)"
echo "单词记忆：   http://localhost:$WORD_GAME_PORT (earthworm.aistudy.icu)"
echo "博客系统：   http://localhost:$BLOG_PORT      (blog.aistudy.icu)"
echo ""
echo "📋 进程：后端 $BACKEND_PID | 前端 $FRONTEND_PID | 简历 $CV_PID | word-game $WORD_PID | 博客 $BLOG_PID"
echo "📝 日志：backend.log | frontend.log | cv-site.log | word-game.log | blog.log"
echo "🛑 停止：kill $BACKEND_PID $FRONTEND_PID $CV_PID $WORD_PID $BLOG_PID"
echo "=========================================="
