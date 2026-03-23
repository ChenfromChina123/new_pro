#!/bin/bash
# OCR Service 启动脚本

echo "============================================"
echo "OCR Service - CRNN ONNX 轻量级部署"
echo "============================================"
echo ""

cd "$(dirname "$0")"

echo "[1/3] 检查Python环境..."
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到Python，请先安装Python 3.8+"
    exit 1
fi

echo "[2/3] 安装依赖..."
pip3 install -r requirements.txt -q

echo "[3/3] 下载模型文件..."
if [ ! -f "models/det_model.onnx" ]; then
    echo "正在下载OCR模型，请稍候..."
    python3 download_models.py
else
    echo "模型文件已存在，跳过下载"
fi

echo ""
echo "============================================"
echo "启动OCR服务..."
echo "服务地址: http://localhost:8089"
echo "API文档: http://localhost:8089/"
echo "============================================"
echo ""

python3 app.py
