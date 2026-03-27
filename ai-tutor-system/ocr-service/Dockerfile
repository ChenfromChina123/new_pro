# OCR Service - CRNN ONNX轻量级部署
# 内存限制: ~150MB, CPU推理

FROM python:3.10-slim

LABEL maintainer="ZhiXueYunJing"
LABEL description="智学云境 (AI LearnSphere) - Lightweight OCR Service with CRNN ONNX"

# 设置环境变量 - 优化内存使用
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    OMP_NUM_THREADS=1 \
    MKL_NUM_THREADS=1 \
    OPENBLAS_NUM_THREADS=1 \
    VECLIB_MAXIMUM_THREADS=1 \
    NUMEXPR_NUM_THREADS=1

# 设置工作目录
WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖 - 使用最小化安装
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 创建模型目录
RUN mkdir -p /app/models

# 暴露端口
EXPOSE 8089

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8089/health || exit 1

# 启动命令
CMD ["python", "app.py"]
