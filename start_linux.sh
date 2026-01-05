#!/bin/bash

# ==========================================
# AI Study Project - Linux 启动脚本
# ==========================================

# 1. 启动后端 (Spring Boot)
echo "正在启动后端服务..."
if [ -d "aispring" ]; then
    cd aispring
    # 使用 nohup 后台运行，日志输出到 backend.log
    # 注意：确保服务器已安装 Maven (mvn) 和 Java
    nohup mvn spring-boot:run > ../backend.log 2>&1 &
    BACKEND_PID=$!
    echo "✅ 后端服务已在后台启动 (PID: $BACKEND_PID)"
    echo "   日志文件: backend.log"
    cd ..
else
    echo "❌ 错误: 找不到 aispring 目录"
fi

echo "------------------------------------------"

# 2. 启动前端 (Vue)
echo "正在启动前端服务..."
if [ -d "vue-app" ]; then
    cd vue-app
    # 确保依赖已安装
    if [ ! -d "node_modules" ]; then
        echo "   正在安装前端依赖..."
        npm install
    fi
    # 使用 nohup 后台运行，日志输出到 frontend.log
    # --host 参数允许外部访问
    nohup npm run dev -- --host > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo "✅ 前端服务已在后台启动 (PID: $FRONTEND_PID)"
    echo "   日志文件: frontend.log"
    cd ..
else
    echo "❌ 错误: 找不到 vue-app 目录"
fi

echo "=========================================="
echo "服务启动完成！"
echo "查看后端日志: tail -f backend.log"
echo "查看前端日志: tail -f frontend.log"
echo "停止服务命令: kill $BACKEND_PID $FRONTEND_PID"
echo "=========================================="
