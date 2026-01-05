#!/bin/bash

echo "正在停止 AI Study Project 服务..."

# 查找并杀掉 Spring Boot 进程
# 注意：这会杀掉所有包含 spring-boot:run 的进程，请根据实际情况调整
pkill -f "spring-boot:run"
if [ $? -eq 0 ]; then
    echo "✅ 后端服务已停止"
else
    echo "⚠️  未找到运行中的后端服务"
fi

# 查找并杀掉 Vue (Vite) 进程
pkill -f "vite"
if [ $? -eq 0 ]; then
    echo "✅ 前端服务已停止"
else
    echo "⚠️  未找到运行中的前端服务"
fi

echo "操作完成。"
