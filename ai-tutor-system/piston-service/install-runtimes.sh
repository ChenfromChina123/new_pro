#!/bin/bash

# 等待 Piston 启动
echo "等待 Piston 引擎启动..."
sleep 10

# 安装语言运行时
echo "开始安装语言运行时..."

# 可用的语言包列表
PACKAGES=(
    "python"
    "node"
    "java-jdk"
    "c-gcc"
    "c++-gcc"
)

for pkg in "${PACKAGES[@]}"; do
    echo "安装 $pkg..."
    docker exec piston-engine /piston/packages/install "$pkg"
done

echo "语言运行时安装完成！"
