#!/bin/bash
# ==========================================
# 重新启动后端服务
# ==========================================

echo "=========================================="
echo "🔄 重新启动后端服务"
echo "=========================================="
echo ""

# 获取项目根目录（脚本所在目录）
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# 1. 停止现有进程
echo "【1/4】停止现有后端进程..."
BACKEND_PID=$(lsof -ti:5000 | head -n 1)
if [ -n "$BACKEND_PID" ]; then
    echo "    发现后端进程 (PID: $BACKEND_PID)，正在停止..."
    kill -9 $BACKEND_PID 2>/dev/null || true
    sleep 2
    if [ -n "$(lsof -ti:5000)" ]; then
        echo "    ⚠️  进程未完全停止，再次尝试..."
        lsof -ti:5000 | xargs -r kill -9 2>/dev/null || true
        sleep 1
    fi
    echo "    ✅ 后端进程已停止"
else
    echo "    ℹ️  后端未运行"
fi
echo ""

# 2. 清理日志
echo "【2/4】清理旧日志..."
if [ -f backend.log ]; then
    mv backend.log backend.log.backup.$(date +%Y%m%d_%H%M%S)
    echo "    ✅ 旧日志已备份"
fi
echo ""

# 3. 启动后端
echo "【3/4】启动后端..."
cd ai-tutor-system/aispring

# 查找最新的 JAR
JAR_FILE=$(ls -t target/*.jar 2>/dev/null | grep -v "original" | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ 未找到 JAR 文件，请先构建"
    echo ""
    echo "    请先执行："
    echo "    cd ai-tutor-system/aispring"
    echo "    mvn package -DskipTests"
    exit 1
fi

echo "    使用 JAR: $JAR_FILE"

# JVM 配置
JVM_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC -Dspring.profiles.active=prod -Dfile.encoding=UTF-8"

# 数据库环境变量
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-aispring}"
export DB_USERNAME="${DB_USERNAME:-aispring}"
export DB_PASSWORD="${DB_PASSWORD:-xGDswMCdHhsajfxF}"

# 启动
echo "    正在启动后端..."
nohup env DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_NAME="$DB_NAME" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" \
      java $JVM_OPTS -jar "$JAR_FILE" > ../backend.log 2>&1 &

BACKEND_PID=$!
cd ../..

echo "    ✅ 后端进程已启动 (PID: $BACKEND_PID)"
echo ""

# 4. 等待启动
echo "【4/4】等待后端启动..."
for i in {1..45}; do
    if [ -n "$(lsof -ti:5000)" ]; then
        echo "    ✅ 后端已就绪 (端口 5000)"
        echo ""
        echo "=========================================="
        echo "🎉 后端启动成功！"
        echo "=========================================="
        echo "进程 PID: $BACKEND_PID"
        echo "访问地址：http://localhost:5000"
        echo "API 文档：http://localhost:5000/swagger-ui.html"
        echo "日志文件：backend.log"
        echo ""
        echo "实时日志："
        tail -f backend.log
        exit 0
    fi
    sleep 2
    echo "    等待中... ${i}s"
done

echo ""
echo "❌ 后端在 90 秒内未启动，请查看日志："
echo "----------------------------------------"
tail -100 backend.log
exit 1
