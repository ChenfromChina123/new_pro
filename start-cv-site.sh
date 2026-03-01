#!/bin/bash
# ==========================================
# 简历网站启动脚本
# 功能：启动 cv.aistudy.icu 个人简历网站
# 端口：3100
# ==========================================

set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# 配置
CV_PORT=3100
CV_DIR="$(cd "$(dirname "$0")/cv-site" && pwd)"
CV_LOG="$(cd "$(dirname "$0")" && pwd)/cv-site.log"

# 辅助函数
get_port_pid() {
  lsof -ti:"$1" 2>/dev/null | head -n 1
}

# 主流程
echo "=========================================="
echo "📄 简历网站启动脚本"
echo "=========================================="
echo "端口：$CV_PORT"
echo "目录：$CV_DIR"
echo "日志：$CV_LOG"
echo ""

# 检查 Python
command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1 || { echo "❌ 未找到 Python 命令，请先安装 Python"; exit 1; }
echo "✅ Python 已找到：$(python3 --version 2>/dev/null || python --version)"

# 检查目录
if [ ! -d "$CV_DIR" ]; then
  echo "❌ 简历网站目录不存在：$CV_DIR"
  exit 1
fi
echo "✅ 简历网站目录已找到"

# 检查端口
CV_PID=$(get_port_pid "$CV_PORT")
if [ -n "$CV_PID" ]; then
  echo "✅ 简历网站已在运行（端口 $CV_PORT, PID: $CV_PID）"
  echo ""
  echo "访问地址：http://localhost:$CV_PORT"
  echo "=========================================="
  exit 0
fi

# 释放端口（如果有残留）
echo "🔍 检测端口 $CV_PORT..."
if [ -n "$CV_PID" ]; then
  echo "⚠️  端口 $CV_PORT 已被占用 (PID: $CV_PID)，正在终止..."
  kill -9 "$CV_PID" 2>/dev/null || true
  sleep 2
fi

# 启动服务
echo "🚀 正在启动简历网站..."
echo "💡 提示：静态 HTML 网站，启动速度很快"

export PORT=$CV_PORT
nohup python3 -m http.server $CV_PORT > "$CV_LOG" 2>&1 &
if [ $? -ne 0 ]; then
  # 如果 python3 失败，尝试 python
  nohup python -m http.server $CV_PORT > "$CV_LOG" 2>&1 &
fi
CV_LAUNCH_PID=$!
unset PORT

echo "📋 简历网站进程已启动 (PID: $CV_LAUNCH_PID)"
echo "⏳ 等待端口 $CV_PORT 就绪..."

sleep 3

# 检查端口是否监听
CV_PID=$(get_port_pid "$CV_PORT")
if [ -n "$CV_PID" ]; then
  echo ""
  echo "=========================================="
  echo "✅ 简历网站启动成功！"
  echo "=========================================="
  echo "访问地址：http://localhost:$CV_PORT"
  echo "域名访问：https://cv.aistudy.icu"
  echo "进程 PID: $CV_PID"
  echo "日志文件：$CV_LOG"
  echo ""
  echo "🛑 停止命令：kill $CV_PID"
  echo "📝 查看日志：tail -f $CV_LOG"
  echo "=========================================="
else
  echo ""
  echo "❌ 简历网站启动失败"
  echo "--- cv-site.log 最后 30 行 ---"
  tail -30 "$CV_LOG" 2>/dev/null || echo "(无日志)"
  echo "---"
  echo ""
  echo "建议："
  echo "1. 检查 Python 版本：python3 --version"
  echo "2. 检查目录是否存在：ls -la $CV_DIR"
  echo "3. 查看详细日志：tail -f $CV_LOG"
  echo "4. 手动启动测试：cd $CV_DIR && python3 -m http.server $CV_PORT"
  exit 1
fi
