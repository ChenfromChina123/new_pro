#!/bin/bash

# ==========================================
# AI Study Project - Linux 启动脚本
# ==========================================

# 设置数据库凭据 (来自截图信息)
# 使用环境变量覆盖 application.yml 中的默认配置
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
    
    # 查找占用端口的进程ID
    local pid=$(lsof -ti:$port 2>/dev/null)
    
    if [ -n "$pid" ]; then
        echo "⚠️  端口 $port 被进程 $pid 占用"
        echo "   正在停止 $service_name 进程..."
        kill -9 $pid 2>/dev/null
        sleep 2
        
        # 再次检查是否成功停止
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
echo "🚀 正在启动 AI Study Project"
echo "=========================================="

# ==========================================
# 1. 启动后端 (Spring Boot)
# ==========================================
echo ""
echo "【后端服务】"

# 清理后端端口
kill_port $BACKEND_PORT "后端服务"

if [ -d "aispring" ]; then
    cd aispring
    
    # 查找 JAR 文件
    JAR_FILE=$(ls -t target/*.jar 2>/dev/null | grep -v "original" | head -n 1)
    
    if [ -z "$JAR_FILE" ]; then
        echo "   ⚠️  未找到 JAR 文件，正在构建..."
        mvn package -DskipTests -q
        JAR_FILE=$(ls -t target/*.jar 2>/dev/null | grep -v "original" | head -n 1)
    fi
    
    if [ -z "$JAR_FILE" ]; then
        echo "❌ 错误: 构建失败，找不到 JAR 文件"
        exit 1
    fi
    
    echo "   正在启动 Spring Boot 后端..."
    echo "   JAR 文件: $JAR_FILE"
    
    # 设置JVM内存限制参数（大幅降低内存占用）
    JVM_OPTS="-Xms128m -Xmx256m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Dspring.profiles.active=prod -Dfile.encoding=UTF-8"
    
    # 使用 nohup 后台运行，日志输出到 backend.log
    nohup java $JVM_OPTS -jar "$JAR_FILE" > ../backend.log 2>&1 &
    BACKEND_PID=$!
    echo "✅ 后端服务已在后台启动（内存限制: 256MB）"
    echo "   PID: $BACKEND_PID"
    echo "   端口: $BACKEND_PORT"
    echo "   数据库用户: $DB_USERNAME"
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

# 清理前端端口
kill_port $FRONTEND_PORT "前端服务"

if [ -d "vue-app" ]; then
    cd vue-app
    
    # 确保依赖已安装
    if [ ! -d "node_modules" ]; then
        echo "   正在安装前端依赖..."
        npm install
    fi
    
    echo "   正在启动 Vue 前端..."
    # 设置Node.js内存限制（降低内存占用）
    # --max-old-space-size=256: 限制老生代内存为256MB
    export NODE_OPTIONS="--max-old-space-size=256"
    
    # 使用 nohup 后台运行，日志输出到 frontend.log
    # --host 参数允许外部访问
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
echo "🎉 服务启动完成！"
echo "=========================================="
echo "后端地址: http://localhost:$BACKEND_PORT"
echo "前端地址: http://localhost:$FRONTEND_PORT"
echo ""
echo "📋 进程信息:"
echo "   后端 PID: $BACKEND_PID"
echo "   前端 PID: $FRONTEND_PID"
echo ""
echo "📝 查看日志:"
echo "   后端: tail -f backend.log"
echo "   前端: tail -f frontend.log"
echo ""
echo "🛑 停止服务:"
echo "   kill $BACKEND_PID $FRONTEND_PID"
echo "   或者: lsof -ti:$BACKEND_PORT,$FRONTEND_PORT | xargs kill -9"
echo "=========================================="
