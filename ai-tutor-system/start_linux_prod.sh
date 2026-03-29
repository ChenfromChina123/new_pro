#!/bin/bash

# ==========================================
# AI Study Project - Linux 生产环境启动脚本（内存优化版）
# ==========================================

# 统一控制台与进程环境编码为 UTF-8
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# 设置 Java 环境变量（自动检测）
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
    elif [ -d "/usr/lib/jvm/java-11-openjdk" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
    elif [ -d "/usr/lib/jvm/java-1.8.0-openjdk" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
    else
        # 尝试自动查找
        JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java 2>/dev/null) 2>/dev/null) 2>/dev/null) 2>/dev/null)
        if [ -n "$JAVA_HOME" ] && [ "$JAVA_HOME" != "/" ]; then
            export JAVA_HOME
        fi
    fi
fi

if [ -n "$JAVA_HOME" ]; then
    export PATH=$JAVA_HOME/bin:$PATH
    echo "✅ JAVA_HOME: $JAVA_HOME"
else
    echo "⚠️  警告: 未找到 JAVA_HOME，尝试使用系统默认 Java"
fi

# 设置数据库凭据
export DB_USERNAME=aispring
export DB_PASSWORD=xGDswMCdHhsajfxF
export DB_NAME=aispring

# 定义端口
BACKEND_PORT=5000
FRONTEND_PORT=3000

# ==========================================
# 函数：获取端口占用 PID
# ==========================================
get_port_pid() {
    local port=$1
    lsof -ti:"$port" 2>/dev/null | head -n 1
}

# ==========================================
# 函数：检查目录是否存在
# ==========================================
ensure_dir_exists() {
    local dir=$1
    if [ ! -d "$dir" ]; then
        echo "❌ 错误: 找不到目录 $dir"
        exit 1
    fi
}

echo "=========================================="
echo "🚀 正在启动 AI Study Project (生产优化版)"
echo "=========================================="

# 1. 更新代码
echo "【代码更新】"
git pull
if [ $? -ne 0 ]; then
    echo "⚠️  Git pull 失败，尝试继续使用本地代码..."
fi

# 2. 构建并启动后端 (Spring Boot)
echo ""
echo "【后端服务】"
echo "🔍 检查端口 $BACKEND_PORT 是否被占用..."

BACKEND_EXISTING_PID=$(get_port_pid "$BACKEND_PORT")
if [ -n "$BACKEND_EXISTING_PID" ]; then
    echo "⚠️  检测到后端已在运行（PID: $BACKEND_EXISTING_PID，端口: $BACKEND_PORT），正在停止..."
    kill -9 "$BACKEND_EXISTING_PID" 2>/dev/null
    sleep 2
    echo "✅ 已停止旧的后端服务"
fi

echo "ℹ️  开始启动后端..."
ensure_dir_exists "aispring"
cd aispring

# 强制重新打包以应用最新的配置文件（如 API Key）
echo "   正在清理并重新打包..."
echo "   Java版本: $(java -version 2>&1 | head -n 1)"
echo "   JAVA_HOME: $JAVA_HOME"

# 确保 mvnw 有执行权限
if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
fi

# 直接 export JAVA_HOME 后执行 mvnw
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 打包失败"
    cd ..
    exit 1
fi

echo "   正在启动 Spring Boot 后端（生产模式）..."

# JVM 内存优化参数
JVM_OPTS="-Xms128m -Xmx256m"
JVM_OPTS="$JVM_OPTS -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops -XX:+UseCompressedClassPointers"
JVM_OPTS="$JVM_OPTS -Djava.security.egd=file:/dev/./urandom"
JVM_OPTS="$JVM_OPTS -Dspring.profiles.active=prod"

# 自动选择最新打包的 JAR，避免版本号变化导致启动失败
JAR_FILE=$(ls -t target/*.jar 2>/dev/null | grep -v "original" | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ 错误: 未找到可执行 JAR 文件"
    cd ..
    exit 1
fi

# 使用 JAR 直接运行（比 mvn spring-boot:run 更省内存）
nohup java $JVM_OPTS -jar "$JAR_FILE" > ../backend.log 2>&1 &
BACKEND_PID=$!

echo "✅ 后端服务已在后台启动（生产模式，内存限制: 256MB）"
echo "   PID: $BACKEND_PID"
echo "   端口: $BACKEND_PORT"
echo "   JAR: $JAR_FILE"
echo "   JVM参数: $JVM_OPTS"
echo "   日志文件: backend.log"
cd ..

echo "------------------------------------------"

# ==========================================
# 3. 启动前端 (Vue)
# ==========================================
echo ""
echo "【前端服务】"
echo "🔍 检查端口 $FRONTEND_PORT 是否被占用..."

FRONTEND_EXISTING_PID=$(get_port_pid "$FRONTEND_PORT")
if [ -n "$FRONTEND_EXISTING_PID" ]; then
    echo "⚠️  检测到前端已在运行（PID: $FRONTEND_EXISTING_PID，端口: $FRONTEND_PORT），正在停止..."
    kill -9 "$FRONTEND_EXISTING_PID" 2>/dev/null
    sleep 2
    echo "✅ 已停止旧的前端服务"
fi

echo "ℹ️  开始启动前端..."
ensure_dir_exists "vue-app"
cd vue-app

if [ ! -d "node_modules" ]; then
    echo "   正在安装前端依赖..."
    npm install
fi

echo "   正在启动 Vue 前端（内存优化）..."

# Node.js 内存限制（降低至 256MB）
export NODE_OPTIONS="--max-old-space-size=256"

nohup npm run dev -- --host > ../frontend.log 2>&1 &
FRONTEND_PID=$!

echo "✅ 前端服务已在后台启动（内存限制: 256MB）"
echo "   PID: $FRONTEND_PID"
echo "   端口: $FRONTEND_PORT"
echo "   日志文件: frontend.log"
cd ..

echo ""
echo "=========================================="
echo "🎉 服务启动完成！（生产优化版）"
echo "=========================================="
echo "后端地址: http://localhost:$BACKEND_PORT"
echo "前端地址: http://localhost:$FRONTEND_PORT"
echo ""
echo "💾 内存优化:"
echo "   后端 JVM Heap: 最大 256MB"
echo "   前端 Node: 最大 256MB"
echo "   预计总内存占用: ~400-500MB"
echo ""
echo "📋 进程信息:"
echo "   后端 PID: $BACKEND_PID"
echo "   前端 PID: $FRONTEND_PID"
echo ""
echo "📝 查看日志:"
echo "   后端: tail -f backend.log"
echo "   前端: tail -f frontend.log"
echo ""
echo "📊 查看内存使用:"
echo "   ps aux | grep java"
echo "   ps aux | grep node"
echo ""
echo "🛑 停止服务:"
echo "   kill $BACKEND_PID $FRONTEND_PID"
echo "=========================================="
