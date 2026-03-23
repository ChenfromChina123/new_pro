@echo off
chcp 65001 >nul
echo ============================================
echo OCR Service - CRNN ONNX 轻量级部署
echo ============================================
echo.

cd /d "%~dp0"

echo [1/3] 检查Python环境...
python --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Python，请先安装Python 3.8+
    pause
    exit /b 1
)

echo [2/3] 安装依赖...
pip install -r requirements.txt -q

echo [3/3] 下载模型文件...
if not exist "models\det_model.onnx" (
    echo 正在下载OCR模型，请稍候...
    python download_models.py
) else (
    echo 模型文件已存在，跳过下载
)

echo.
echo ============================================
echo 启动OCR服务...
echo 服务地址: http://localhost:8089
echo API文档: http://localhost:8089/
echo ============================================
echo.

python app.py

pause
