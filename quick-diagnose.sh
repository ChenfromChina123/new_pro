#!/bin/bash
# ==========================================
# 快速诊断后端启动问题
# ==========================================

echo "=========================================="
echo "🔍 快速诊断后端启动问题"
echo "=========================================="
echo ""

# 获取项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

# 1. 检查 Java 进程
echo "【1/5】检查 Java 进程："
echo "----------------------------------------"
ps aux | grep "ai-tutor" | grep -v grep || echo "❌ 未找到后端进程"
echo ""

# 2. 检查日志文件位置
echo "【2/5】查找日志文件："
echo "----------------------------------------"
find "$PROJECT_ROOT" -name "backend.log*" -type f 2>/dev/null | head -5
echo ""

# 3. 查看 ai-tutor-system 目录下的日志
echo "【3/5】查看 ai-tutor-system 目录："
echo "----------------------------------------"
if [ -f "$PROJECT_ROOT/ai-tutor-system/backend.log" ]; then
    echo "找到日志文件，显示最后 50 行："
    tail -50 "$PROJECT_ROOT/ai-tutor-system/backend.log"
else
    echo "❌ 日志文件不存在于 ai-tutor-system 目录"
fi
echo ""

# 4. 检查 JAR 文件
echo "【4/5】检查 JAR 文件："
echo "----------------------------------------"
if [ -f "$PROJECT_ROOT/ai-tutor-system/aispring/target/ai-tutor-1.0.0.jar" ]; then
    echo "✅ JAR 文件存在"
    ls -lh "$PROJECT_ROOT/ai-tutor-system/aispring/target/ai-tutor-1.0.0.jar"
else
    echo "❌ JAR 文件不存在，需要重新构建"
fi
echo ""

# 5. 尝试手动启动并查看输出
echo "【5/5】手动启动测试（显示详细输出）："
echo "----------------------------------------"
cd "$PROJECT_ROOT/ai-tutor-system/aispring"

if [ ! -f "target/ai-tutor-1.0.0.jar" ]; then
    echo "❌ JAR 文件不存在，无法启动测试"
    echo ""
    echo "请先构建项目："
    echo "  cd $PROJECT_ROOT/ai-tutor-system/aispring"
    echo "  mvn package -DskipTests"
else
    echo "正在启动..."
    timeout 30 java -Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m \
         -XX:+UseG1GC -Dspring.profiles.active=prod \
         -jar target/ai-tutor-1.0.0.jar 2>&1 | head -100
fi

echo ""
echo "=========================================="
echo "诊断完成！"
echo "=========================================="
