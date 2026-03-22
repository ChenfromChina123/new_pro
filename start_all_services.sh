#!/bin/bash
# ==========================================
# AI Study Project - Linux 一键启动所有服务
# 功能：启动主应用、单词记忆、简历网站
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# ========== 端口配置 ==========
BACKEND_PORT=5000
FRONTEND_PORT=3000
CV_PORT=3100
BLOG_PORT=3200
WHISPER_PORT=8090

# ---------- Whisper Server 配置 ----------
WHISPER_DIR="/opt/whisper-server"
WHISPER_MODEL="${WHISPER_DIR}/models/ggml-base.en.bin"
WHISPER_THREADS=4

# ---------- 数据库配置 ----------
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-aispring}"
export DB_USERNAME="${DB_USERNAME:-aispring}"
export DB_PASSWORD="${DB_PASSWORD:-xGDswMCdHhsajfxF}"

# ---------- 辅助函数 ----------
get_port_pid() { lsof -ti:"$1" 2>/dev/null | head -n 1; }

kill_port_if_used() {
  local port="$1"
  local name="${2:-端口 $port}"
  local pid
  pid=$(get_port_pid "$port")
  if [ -n "$pid" ]; then
    echo "⚠️  $name 端口 $port 已被占用 (PID: $pid)，正在终止..."
    kill -9 "$pid" 2>/dev/null || true
    sleep 2
    # 再次检查，如果仍被占用则尝试强制终止所有相关进程
    if [ -n "$(get_port_pid "$port")" ]; then
      echo "    尝试强制终止所有占用端口 $port 的进程..."
      lsof -ti:"$port" 2>/dev/null | xargs -r kill -9 2>/dev/null || true
      sleep 1
    fi
    if [ -n "$(get_port_pid "$port")" ]; then
      echo "❌ 无法释放端口 $port，跳过并继续..."
    else
      echo "✅ 已释放端口 $port"
    fi
  fi
}

wait_for_port() {
  local port="$1" name="${2:-端口 $port}" timeout="${3:-60}" t=0
  while [ $t -lt "$timeout" ]; do
    [ -n "$(get_port_pid "$port")" ] && return 0
    sleep 2
    t=$((t + 2))
    echo "    等待 $name 监听端口 $port ... ${t}s"
  done
  echo "❌ $name 在 ${timeout}s 内未就绪，请查看日志排查"
  return 1
}

ensure_dir() {
  [ -d "$1" ] || { echo "❌ 错误：找不到目录 $1"; exit 1; }
}

echo "=========================================="
echo "🚀 AI Study Project - 一键启动所有服务"
echo "=========================================="

# ========== 1. 检测环境 ==========
echo ""
echo "【1/6】检测环境依赖..."

command -v java >/dev/null 2>&1 || { echo "❌ 未找到 Java，请先安装 Java"; exit 1; }
echo "✅ Java: $(java -version 2>&1 | head -n 1)"

command -v node >/dev/null 2>&1 || { echo "❌ 未找到 Node.js，请先安装 Node.js"; exit 1; }
echo "✅ Node.js: $(node -v)"

command -v npm >/dev/null 2>&1 || { echo "❌ 未找到 npm，请先安装 npm"; exit 1; }
echo "✅ npm: $(npm -v)"

command -v mvn >/dev/null 2>&1 || { echo "❌ 未找到 Maven，请先安装 Maven"; exit 1; }
echo "✅ Maven: $(mvn -version 2>&1 | head -n 1)"

command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1 || { echo "❌ 未找到 Python，请先安装 Python"; exit 1; }
echo "✅ Python: $(command -v python3 >/dev/null 2>&1 && python3 --version || python --version)"

# ========== 2. 安装依赖 ==========
echo ""
echo "【2/6】安装依赖..."

# 主站前端
if [ -d "ai-tutor-system/vue-app" ]; then
  cd ai-tutor-system/vue-app
  if [ -d "node_modules/vite" ]; then
    echo "    ✅ 主站前端依赖已存在"
  else
    echo "    正在安装主站前端依赖..."
    npm install
  fi
  cd "$PROJECT_ROOT"
fi

# 后端
if [ -d "ai-tutor-system/aispring" ]; then
  echo "    ✅ 后端目录已存在"
fi

# 单词记忆（已合并到 aispring）
# if [ -d "ai-tutor-system/aispring/word-game" ]; then
#   cd ai-tutor-system/aispring/word-game
#   if [ -d "node_modules/vite" ]; then
#     echo "    ✅ 单词记忆依赖已存在"
#   else
#     echo "    正在安装单词记忆依赖..."
#     rm -rf node_modules package-lock.json
#     npm install
#   fi
#   cd "$PROJECT_ROOT"
# fi

# 简历网站
if [ -d "cv-site" ]; then
  echo "    ✅ 简历网站目录已存在（静态 HTML，无需依赖）"
fi

# 博客系统
if [ -d "blog/tailwind-nextjs-starter-blog-main" ]; then
  cd blog/tailwind-nextjs-starter-blog-main
  if [ -d "node_modules/next" ]; then
    echo "    ✅ 博客系统依赖已存在"
  else
    echo "    正在安装博客系统依赖..."
    npm install
  fi
  cd "$PROJECT_ROOT"
else
  echo "    ⚠️  博客系统目录不存在，跳过"
fi

# ========== 3. 构建 ==========
echo ""
echo "【3/6】构建项目..."

# 清理根目录可能干扰 vite 的 node_modules 和缓存
# 根目录不应该有 node_modules，它会干扰子项目的依赖解析
if [ -d "node_modules" ]; then
  echo "    清理根目录多余的 node_modules..."
  rm -rf node_modules
fi
rm -rf .vite .vite-temp 2>/dev/null || true

# 主站前端
if [ -d "ai-tutor-system/vue-app" ]; then
  cd ai-tutor-system/vue-app
  if [ -d "dist" ]; then
    echo "    ✅ 主站前端已构建"
  else
    echo "    正在构建主站前端..."
    npm run build
  fi
  cd "$PROJECT_ROOT"
fi

# 单词记忆（已合并到 aispring）
# if [ -d "ai-tutor-system/aispring/word-game" ]; then
#   cd ai-tutor-system/aispring/word-game
#   if [ -d "dist" ]; then
#     echo "    ✅ 单词记忆已构建"
#   else
#     echo "    正在构建单词记忆..."
#     rm -rf node_modules/.vite node_modules/.vite-temp .vite .vite-temp
#     npm run build
#   fi
#   cd "$PROJECT_ROOT"
# fi

# 后端
if [ -d "ai-tutor-system/aispring" ]; then
  echo "    正在构建后端..."
  cd ai-tutor-system/aispring
  mvn package -DskipTests -q
  cd "$PROJECT_ROOT"
  echo "    ✅ 后端构建完成"
fi

# 博客系统：构建
if [ -d "blog/tailwind-nextjs-starter-blog-main" ]; then
  cd blog/tailwind-nextjs-starter-blog-main
  if [ -d ".next" ]; then
    echo "    ✅ 博客系统已构建"
  else
    echo "    正在构建博客系统..."
    npm run build
  fi
  cd "$PROJECT_ROOT"
fi

# ========== 4. 检测端口冲突 ==========
echo ""
echo "【4/6】检测端口冲突..."
kill_port_if_used "$BACKEND_PORT" "后端"
kill_port_if_used "$FRONTEND_PORT" "主站前端"
kill_port_if_used "$CV_PORT" "简历网站"
kill_port_if_used "$BLOG_PORT" "博客系统"
kill_port_if_used "$WHISPER_PORT" "Whisper语音识别"

# ========== 5. 启动服务 ==========
echo ""
echo "【5/6】启动服务..."

# 5a. 启动后端
BACKEND_PID=$(get_port_pid "$BACKEND_PORT")
if [ -n "$BACKEND_PID" ]; then
  echo "✅ 后端已在运行（端口 $BACKEND_PORT, PID: $BACKEND_PID）"
else
  ensure_dir "ai-tutor-system/aispring"
  cd ai-tutor-system/aispring
  JAR_FILE=$(ls -t target/*.jar 2>/dev/null | grep -v "original" | head -n 1)
  [ -z "$JAR_FILE" ] && { echo "❌ 未找到后端 JAR 文件"; exit 1; }
  JVM_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
  JVM_OPTS="$JVM_OPTS -XX:+UseG1GC -Dspring.profiles.active=prod -Dfile.encoding=UTF-8"
  nohup env DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_NAME="$DB_NAME" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" java $JVM_OPTS -jar "$JAR_FILE" > ../backend.log 2>&1 &
  BACKEND_PID=$!
  cd "$PROJECT_ROOT"
  echo "    后端进程已启动（PID: $BACKEND_PID）"
  wait_for_port "$BACKEND_PORT" "后端" 90
  echo "✅ 后端已就绪（端口 $BACKEND_PORT）"
fi

# 5b. 启动主站前端
FRONTEND_PID=$(get_port_pid "$FRONTEND_PORT")
if [ -n "$FRONTEND_PID" ]; then
  echo "✅ 主站前端已在运行（端口 $FRONTEND_PORT, PID: $FRONTEND_PID）"
else
  ensure_dir "ai-tutor-system/vue-app/dist"
  (cd ai-tutor-system/vue-app && nohup npx --yes serve -s dist -l $FRONTEND_PORT > ../frontend.log 2>&1 &)
  sleep 2
  FRONTEND_PID=$(get_port_pid "$FRONTEND_PORT")
  echo "✅ 主站前端已启动（端口 $FRONTEND_PORT, PID: $FRONTEND_PID）"
fi

# 5c. 启动单词记忆（已合并到 aispring）
# WORD_PID=$(get_port_pid "$WORD_GAME_PORT")
# if [ -n "$WORD_PID" ]; then
#   echo "✅ 单词记忆已在运行（端口 $WORD_GAME_PORT, PID: $WORD_PID）"
# else
#   ensure_dir "ai-tutor-system/aispring/word-game/dist"
#   ensure_dir "ai-tutor-system/aispring/word-game/server"
#   export PORT=$WORD_GAME_PORT
#   (cd ai-tutor-system/aispring/word-game && nohup node server/index.js > ../word-game.log 2>&1 &)
#   unset PORT
#   sleep 2
#   WORD_PID=$(get_port_pid "$WORD_GAME_PORT")
#   echo "✅ 单词记忆已启动（端口 $WORD_GAME_PORT, PID: $WORD_PID）"
# fi

# 5d. 启动简历网站
CV_PID=$(get_port_pid "$CV_PORT")
if [ -n "$CV_PID" ]; then
  echo "✅ 简历网站已在运行（端口 $CV_PORT, PID: $CV_PID）"
else
  CV_DIR="$PROJECT_ROOT/cv-site"
  if [ ! -d "$CV_DIR" ]; then
    echo "⚠️  简历网站目录不存在，跳过启动"
  else
    export PORT=$CV_PORT
    (cd "$CV_DIR" && nohup python3 -m http.server $CV_PORT > ../cv-site.log 2>&1 &)
    unset PORT
    sleep 2
    CV_PID=$(get_port_pid "$CV_PORT")
    echo "✅ 简历网站已启动（端口 $CV_PORT, PID: $CV_PID）"
  fi
fi

# 5e. 启动博客系统
BLOG_PID=$(get_port_pid "$BLOG_PORT")
if [ -n "$BLOG_PID" ]; then
  echo "✅ 博客系统已在运行（端口 $BLOG_PORT, PID: $BLOG_PID）"
else
  BLOG_DIR="$PROJECT_ROOT/blog/tailwind-nextjs-starter-blog-main"
  if [ ! -d "$BLOG_DIR" ]; then
    echo "⚠️  博客系统目录不存在，跳过启动"
  else
    echo "    正在启动博客系统（端口 $BLOG_PORT, 日志：blog.log）..."
    (cd "$BLOG_DIR" && nohup npx next start -p $BLOG_PORT > ../blog.log 2>&1 &)
    sleep 3
    BLOG_PID=$(get_port_pid "$BLOG_PORT")
    echo "✅ 博客系统已启动（端口 $BLOG_PORT, PID: $BLOG_PID）"
  fi
fi

# 5f. 启动 Whisper Server (语音识别服务)
WHISPER_PID=$(get_port_pid "$WHISPER_PORT")
if [ -n "$WHISPER_PID" ]; then
  echo "✅ Whisper Server 已在运行（端口 $WHISPER_PORT, PID: $WHISPER_PID）"
else
  if [ ! -f "$WHISPER_DIR/whisper-server" ]; then
    echo "⚠️  Whisper Server 未安装，跳过启动"
    echo "    请运行 deploy/whisper-server/deploy-whisper-linux.sh 进行安装"
  elif [ ! -f "$WHISPER_MODEL" ]; then
    echo "⚠️  Whisper 模型文件不存在: $WHISPER_MODEL"
    echo "    请先下载模型或修改 WHISPER_MODEL 配置"
  else
    echo "    正在启动 Whisper Server（端口 $WHISPER_PORT）..."
    (cd "$WHISPER_DIR" && nohup ./whisper-server -m "$WHISPER_MODEL" --port $WHISPER_PORT --host 0.0.0.0 -t $WHISPER_THREADS > whisper-server.log 2>&1 &)
    sleep 3
    WHISPER_PID=$(get_port_pid "$WHISPER_PORT")
    if [ -n "$WHISPER_PID" ]; then
      echo "✅ Whisper Server 已启动（端口 $WHISPER_PORT, PID: $WHISPER_PID）"
    else
      echo "⚠️  Whisper Server 启动失败，请检查日志: $WHISPER_DIR/whisper-server.log"
    fi
  fi
fi

# ========== 6. 汇总 ==========
echo ""
echo "=========================================="
echo "🎉 所有服务启动完成！"
echo "=========================================="
echo ""
echo "📍 访问地址："
echo "  主站前端：   http://localhost:$FRONTEND_PORT"
echo "  后端 API:    http://localhost:$BACKEND_PORT/swagger-ui.html"
echo "  简历网站：   http://localhost:$CV_PORT"
echo "  博客系统：   http://localhost:$BLOG_PORT"
echo "  Whisper API: http://localhost:$WHISPER_PORT/inference"
echo ""
echo "📋 进程信息："
echo "  后端：$BACKEND_PID"
echo "  前端：$FRONTEND_PID"
echo "  简历网站：$CV_PID"
echo "  博客系统：$BLOG_PID"
echo "  Whisper: $WHISPER_PID"
echo ""
echo "📝 日志文件："
echo "  后端：backend.log"
echo "  前端：frontend.log"
echo "  简历网站：cv-site.log"
echo "  博客系统：blog.log"
echo "  Whisper:  $WHISPER_DIR/whisper-server.log"
echo ""
echo "🛑 停止所有服务："
echo "  kill $BACKEND_PID $FRONTEND_PID $CV_PID $BLOG_PID $WHISPER_PID"
echo ""
echo "=========================================="
