#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR Service - 优化版本
使用RapidOCR优化配置，速度更快
"""

import os
import base64
import logging
import time
import gzip
import shutil
from datetime import datetime, timedelta
from flask import Flask, request, jsonify
from flask_cors import CORS
import cv2
import numpy as np

# 日志配置
LOG_DIR = 'logs'
LOG_FILE = os.path.join(LOG_DIR, 'ocr-service.log')

# 创建日志目录
if not os.path.exists(LOG_DIR):
    os.makedirs(LOG_DIR)

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(LOG_FILE),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# 创建Flask应用
app = Flask(__name__)
CORS(app)

# 配置
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
app.config['JSON_AS_ASCII'] = False

# 全局变量
ocr_engine = None
ocr_engine_type = None
start_time = datetime.now()


def compress_old_logs():
    """
    压缩旧日志文件
    压缩7天前的日志文件
    """
    try:
        current_time = datetime.now()
        for file in os.listdir(LOG_DIR):
            if file.endswith('.log') and file != os.path.basename(LOG_FILE):
                file_path = os.path.join(LOG_DIR, file)
                file_mtime = datetime.fromtimestamp(os.path.getmtime(file_path))
                
                # 压缩7天前的日志
                if current_time - file_mtime > timedelta(days=7):
                    gz_path = file_path + '.gz'
                    with open(file_path, 'rb') as f_in:
                        with gzip.open(gz_path, 'wb') as f_out:
                            shutil.copyfileobj(f_in, f_out)
                    # 删除原文件
                    os.remove(file_path)
                    logger.info(f"压缩并删除旧日志文件: {file}")
    except Exception as e:
        logger.error(f"压缩日志文件错误: {e}")


def init_ocr():
    """
    初始化OCR引擎 - 支持本地和阿里云OCR
    """
    global ocr_engine, ocr_engine_type
    try:
        logger.info("正在初始化OCR引擎...")
        from ocr_engine_factory import OCREngineFactory
        
        # 获取引擎类型
        ocr_engine_type = os.getenv('OCR_ENGINE_TYPE', 'local')
        logger.info(f"使用OCR引擎类型: {ocr_engine_type}")
        
        # 使用工厂创建引擎
        ocr_engine = OCREngineFactory.get_engine(ocr_engine_type)
        
        # 预热模型（仅本地引擎需要）
        if ocr_engine_type == 'local' and ocr_engine:
            logger.info("预热本地模型...")
            dummy_img = np.ones((100, 300, 3), dtype=np.uint8) * 255
            ocr_engine.ocr(dummy_img)
        
        logger.info(f"OCR引擎初始化完成: {ocr_engine_type}")
    except Exception as e:
        logger.error(f"OCR引擎初始化失败: {e}")
        import traceback
        traceback.print_exc()
        ocr_engine = None


def decode_image(image_data):
    """
    解码图像数据
    """
    if isinstance(image_data, np.ndarray):
        return image_data
    
    if isinstance(image_data, str):
        if image_data.startswith('data:image'):
            image_data = image_data.split(',')[1]
        image_bytes = base64.b64decode(image_data)
    else:
        image_bytes = image_data
    
    image_array = np.frombuffer(image_bytes, dtype=np.uint8)
    image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
    
    return image


@app.route('/health', methods=['GET'])
def health_check():
    """
    健康检查接口
    """
    uptime = (datetime.now() - start_time).total_seconds()
    
    return jsonify({
        "status": "healthy",
        "service": "ocr-service-fast",
        "version": "1.0.0",
        "uptime_seconds": int(uptime),
        "model_loaded": ocr_engine is not None,
        "engine": ocr_engine_type or "RapidOCR (optimized)"
    })


@app.route('/', methods=['GET'])
def index():
    """
    首页
    """
    return jsonify({
        "service": "OCR Service Fast",
        "version": "1.0.0",
        "description": "优化版OCR服务 - 支持本地和阿里云OCR",
        "engine": ocr_engine_type or "RapidOCR (optimized)",
        "endpoints": {
            "POST /ocr": "识别图像中的文字（返回纯文本）",
            "POST /ocr/base64": "识别Base64编码图像中的文字",
            "GET /health": "健康检查"
        }
    })


@app.route('/ocr', methods=['POST'])
def ocr_image():
    """
    OCR识别接口 - 文件上传，返回纯文本
    """
    if ocr_engine is None:
        return jsonify({
            "success": False,
            "error": "OCR引擎未初始化"
        }), 500
    
    try:
        if 'image' not in request.files:
            return jsonify({
                "success": False,
                "error": "未找到图像文件"
            }), 400
        
        file = request.files['image']
        
        # 根据引擎类型处理
        if ocr_engine_type == 'aliyun':
            # 保存文件到临时目录
            import tempfile
            with tempfile.NamedTemporaryFile(suffix='.jpg', delete=False) as temp_file:
                file.save(temp_file.name)
                temp_path = temp_file.name
            
            try:
                # 使用阿里云OCR
                result = ocr_engine.ocr(temp_path)
                # 提取文本
                if result:
                    text = '\n'.join([item['text'] for item in result])
                else:
                    text = ""
            finally:
                # 删除临时文件
                import os
                if os.path.exists(temp_path):
                    os.remove(temp_path)
        else:
            # 本地OCR处理
            image_bytes = file.read()
            image = decode_image(image_bytes)
            result = ocr_engine.ocr(image)
            # 提取文本
            if result:
                text = '\n'.join([item['text'] for item in result])
            else:
                text = ""
        
        return jsonify({
            "success": True,
            "text": text
        }), 200
        
    except Exception as e:
        logger.error(f"OCR处理错误: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/ocr/base64', methods=['POST'])
def ocr_base64():
    """
    OCR识别接口 - Base64编码
    """
    if ocr_engine is None:
        return jsonify({
            "success": False,
            "error": "OCR引擎未初始化"
        }), 500
    
    try:
        data = request.get_json()
        
        if not data or 'image' not in data:
            return jsonify({
                "success": False,
                "error": "请求体需要包含 'image' 字段"
            }), 400
        
        image_data = data['image']
        
        if image_data.startswith('data:image'):
            image_data = image_data.split(',')[1]
        
        image_bytes = base64.b64decode(image_data)
        
        # 根据引擎类型处理
        if ocr_engine_type == 'aliyun':
            # 保存文件到临时目录
            import tempfile
            with tempfile.NamedTemporaryFile(suffix='.jpg', delete=False) as temp_file:
                temp_file.write(image_bytes)
                temp_path = temp_file.name
            
            try:
                # 使用阿里云OCR
                result = ocr_engine.ocr(temp_path)
                # 提取文本
                if result:
                    text = '\n'.join([item['text'] for item in result])
                else:
                    text = ""
            finally:
                # 删除临时文件
                import os
                if os.path.exists(temp_path):
                    os.remove(temp_path)
        else:
            # 本地OCR处理
            image = decode_image(image_bytes)
            result = ocr_engine.ocr(image)
            # 提取文本
            if result:
                text = '\n'.join([item['text'] for item in result])
            else:
                text = ""
        
        return jsonify({
            "success": True,
            "text": text
        }), 200
        
    except Exception as e:
        logger.error(f"OCR处理错误: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


def print_memory_usage():
    """
    打印内存使用情况
    """
    try:
        import psutil
        process = psutil.Process(os.getpid())
        mem_info = process.memory_info()
        logger.info(f"内存使用: RSS={mem_info.rss/1024/1024:.1f}MB")
    except ImportError:
        pass


@app.before_request
def before_request():
    """
    请求前处理
    """
    request.start_time = time.time()
    logger.info("收到请求: %s %s", request.method, request.path)


@app.after_request
def after_request(response):
    """
    请求后处理
    """
    elapsed = time.time() - request.start_time
    logger.info("请求完成: %s %s - %d - %.3f秒", request.method, request.path, response.status_code, elapsed)
    return response


if __name__ == '__main__':
    # 压缩旧日志
    compress_old_logs()
    
    # 初始化OCR引擎
    init_ocr()
    
    # 打印内存使用
    print_memory_usage()
    
    # 启动服务
    port = int(os.environ.get('PORT', 8089))
    host = os.environ.get('HOST', '0.0.0.0')
    
    logger.info(f"启动OCR服务: http://{host}:{port}")
    logger.info(f"引擎: {ocr_engine_type or 'RapidOCR (优化配置)'}")
    
    app.run(
        host=host,
        port=port,
        debug=False,
        threaded=True,   # 启用多线程，充分利用两核CPU
        processes=1      # 单进程
    )
