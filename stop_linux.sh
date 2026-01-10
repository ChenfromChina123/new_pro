#!/bin/bash

echo "正在停止 AI Study Project 服务..."

# 定义端口
BACKEND_PORT=5000
FRONTEND_PORT=3000

# 杀掉占用端口的进程
echo "🔍 正在停止后端服务 (端口 $BACKEND_PORT)..."
fuser -k $BACKEND_PORT/tcp 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ 后端服务已停止"
else
    # 尝试使用 pkill 作为备选方案
    pkill -f "ai-tutor"
    pkill -f "spring-boot:run"
    echo "✅ 后端停止指令已发送"
fi

echo "🔍 正在停止前端服务 (端口 $FRONTEND_PORT)..."
fuser -k $FRONTEND_PORT/tcp 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ 前端服务已停止"
else
    pkill -f "vite"
    echo "✅ 前端停止指令已发送"
fi

echo "操作完成。"
