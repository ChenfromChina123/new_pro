#!/bin/bash

# ==========================================
# AI Study Project - Linux 生产环境启动脚本（内存优化版）
# ==========================================

# 设置数据库凭据
export DB_USERNAME=aispring
export DB_PASSWORD=xGDswMCdHhsajfxF
export DB_NAME=aispring

# 定义端口
BACKEND_PORT=5000
FRONTEND_PORT=3000

# ==========================================
# 函数：检测并清理端口
# ==========================================
kill_port() {
    local port=$1
    local service_name=$2
    
    echo "🔍 检查端口 $port 是否被占用..."
    
    local pid=$(lsof -ti:$port 2>/dev/null)
    
    if [ -n "$pid" ]; then
        echo "⚠️  端口 $port 被进程 $pid 占用"
        echo "   正在停止 $service_name 进程..."
        kill -9 $pid 2>/dev/null
        sleep 2
        
        local check_pid=$(lsof -ti:$port 2>/dev/null)
        if [ -z "$check_pid" ]; then
            echo "✅ 端口 $port 已释放"
        else
            echo "❌ 警告: 端口 $port 仍然被占用"
            return 1
        fi
    else
        echo "✅ 端口 $port 未被占用"
    fi
    
    return 0
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

kill_port $BACKEND_PORT "后端服务"

if [ -d "aispring" ]; then
    cd aispring
    
    # 强制重新打包以应用最新的配置文件（如 API Key）
    echo "   正在清理并重新打包..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ 打包失败"
        exit 1
    fi
    
    echo "   正在启动 Spring Boot 后端（生产模式）..."
    
    # JVM内存优化参数（极致优化版）
    # -Xms: 初始堆大小 128MB
    # -Xmx: 最大堆大小 256MB
    # -XX:MetaspaceSize: 元空间初始大小 64MB
    # -XX:MaxMetaspaceSize: 元空间最大大小 128MB
    # -XX:+UseG1GC: 使用G1垃圾收集器
    # -XX:MaxGCPauseMillis: 最大GC暂停时间 200ms
    # -XX:+UseStringDeduplication: 字符串去重
    # -XX:+UseCompressedOops: 压缩对象指针
    # -XX:+UseCompressedClassPointers: 压缩类指针
    # -Djava.security.egd: 加快启动速度
    JVM_OPTS="-Xms128m -Xmx256m"
    JVM_OPTS="$JVM_OPTS -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m"
    JVM_OPTS="$JVM_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"
    JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops -XX:+UseCompressedClassPointers"
    JVM_OPTS="$JVM_OPTS -Djava.security.egd=file:/dev/./urandom"
    JVM_OPTS="$JVM_OPTS -Dspring.profiles.active=prod"
    
    # 使用jar文件直接运行（比mvn运行节省大量内存）
    nohup java $JVM_OPTS -jar target/ai-tutor-1.0.0.jar > ../backend.log 2>&1 &
    BACKEND_PID=$!
    
    echo "✅ 后端服务已在后台启动（生产模式，内存限制: 256MB）"
    echo "   PID: $BACKEND_PID"
    echo "   端口: $BACKEND_PORT"
    echo "   JVM参数: $JVM_OPTS"
    echo "   日志文件: backend.log"
    cd ..
else
    echo "❌ 错误: 找不到 aispring 目录"
    exit 1
fi

echo "------------------------------------------"

# ==========================================
# 2. 启动前端 (Vue)
# ==========================================
echo ""
echo "【前端服务】"

kill_port $FRONTEND_PORT "前端服务"

if [ -d "vue-app" ]; then
    cd vue-app
    
    if [ ! -d "node_modules" ]; then
        echo "   正在安装前端依赖..."
        npm install
    fi
    
    echo "   正在启动 Vue 前端（内存优化）..."
    
    # Node.js内存限制（降低至256MB）
    export NODE_OPTIONS="--max-old-space-size=256"
    
    nohup npm run dev -- --host > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    
    echo "✅ 前端服务已在后台启动（内存限制: 256MB）"
    echo "   PID: $FRONTEND_PID"
    echo "   端口: $FRONTEND_PORT"
    echo "   日志文件: frontend.log"
    cd ..
else
    echo "❌ 错误: 找不到 vue-app 目录"
    exit 1
fi

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
echo "   或者: lsof -ti:$BACKEND_PORT,$FRONTEND_PORT | xargs kill -9"
echo "=========================================="

