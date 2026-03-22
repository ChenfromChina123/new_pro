#!/bin/bash
# ==========================================
# Java 环境变量配置脚本
# 用于在服务器上配置 JAVA_HOME
# ==========================================

echo "=========================================="
echo "🔧 正在配置 Java 环境变量..."
echo "=========================================="

# 常见的 Java 安装路径
JAVA_PATHS=(
    "/usr/lib/jvm/java-17-openjdk"
    "/usr/lib/jvm/java-11-openjdk"
    "/usr/lib/jvm/java-8-openjdk"
    "/usr/java/default"
    "/opt/java/openjdk"
    "/usr/lib/jvm/default-java"
)

# 查找 Java 安装路径
JAVA_HOME=""
for path in "${JAVA_PATHS[@]}"; do
    if [ -d "$path" ] && [ -f "$path/bin/java" ]; then
        JAVA_HOME="$path"
        break
    fi
done

# 如果没找到，尝试使用 which 命令
if [ -z "$JAVA_HOME" ]; then
    JAVA_BIN=$(which java 2>/dev/null)
    if [ -n "$JAVA_BIN" ]; then
        # 读取符号链接的真实路径
        JAVA_REAL=$(readlink -f "$JAVA_BIN")
        # 提取 JAVA_HOME（去掉 /bin/java）
        JAVA_HOME=$(dirname "$(dirname "$JAVA_REAL")")
    fi
fi

# 如果还是没找到，提示用户
if [ -z "$JAVA_HOME" ]; then
    echo "❌ 未找到 Java 安装路径"
    echo ""
    echo "请先安装 Java 17+："
    echo "  # CentOS/RHEL:"
    echo "  sudo yum install java-17-openjdk"
    echo ""
    echo "  # Ubuntu/Debian:"
    echo "  sudo apt install openjdk-17-jdk"
    echo ""
    exit 1
fi

echo "✅ 找到 Java 安装：$JAVA_HOME"
echo ""

# 显示 Java 版本
echo "Java 版本信息："
"$JAVA_HOME/bin/java" -version 2>&1 | head -3
echo ""

# 配置环境变量
echo "正在配置环境变量..."

# 添加到 /etc/profile.d/
cat > /etc/profile.d/java_env.sh << EOF
# Java 环境变量配置
export JAVA_HOME=$JAVA_HOME
export PATH=\$JAVA_HOME/bin:\$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
EOF

chmod +x /etc/profile.d/java_env.sh

# 同时添加到当前用户的 .bashrc
if ! grep -q "JAVA_HOME=$JAVA_HOME" ~/.bashrc 2>/dev/null; then
    cat >> ~/.bashrc << EOF

# Java 环境变量配置（$(date +%Y-%m-%d) 自动添加）
export JAVA_HOME=$JAVA_HOME
export PATH=\$JAVA_HOME/bin:\$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
EOF
fi

# 立即生效
export JAVA_HOME=$JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar

echo "✅ Java 环境变量配置完成！"
echo ""
echo "=========================================="
echo "配置摘要："
echo "  JAVA_HOME = $JAVA_HOME"
echo "  Java 路径 = $JAVA_HOME/bin/java"
echo "=========================================="
echo ""
echo "📝 注意："
echo "  - 当前终端已立即生效"
echo "  - 其他终端需要重新登录或执行：source ~/.bashrc"
echo "  - 系统级配置已添加到：/etc/profile.d/java_env.sh"
echo ""

# 验证配置
echo "验证 Java 是否可用："
java -version 2>&1 | head -1
echo ""
echo "✅ 配置完成！现在可以运行构建脚本了。"
