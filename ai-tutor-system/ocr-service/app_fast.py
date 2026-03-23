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
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS
import cv2
import numpy as np

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
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
start_time = datetime.now()


def init_ocr():
    """
    初始化OCR引擎 - 优化配置
    """
    global ocr_engine
    try:
        logger.info("正在初始化OCR引擎...")
        from rapidocr_onnxruntime import RapidOCR
        
        # 优化配置：禁用方向分类器加速
        ocr_engine = RapidOCR(
            use_det=True,      # 文本检测
            use_cls=False,     # 禁用方向分类（加速）
            use_rec=True,      # 文本识别
        )
        
        # 预热模型
        logger.info("预热模型...")
        dummy_img = np.ones((100, 300, 3), dtype=np.uint8) * 255
        ocr_engine(dummy_img)
        
        logger.info("OCR引擎初始化完成（优化配置）")
    except Exception as e:
        logger.error(f"OCR引擎初始化失败: {e}")
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
        "engine": "RapidOCR (optimized)"
    })


@app.route('/', methods=['GET'])
def index():
    """
    首页
    """
    return jsonify({
        "service": "OCR Service Fast",
        "version": "1.0.0",
        "description": "优化版OCR服务 - RapidOCR",
        "engine": "RapidOCR (optimized)",
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
        image_bytes = file.read()
        
        # 解码图像
        image = decode_image(image_bytes)
        
        # OCR识别
        result, _ = ocr_engine(image)
        
        # 提取文本
        if result:
            text = '\n'.join([item[1] for item in result])
        else:
            text = ""
        
        return jsonify({
            "success": True,
            "text": text
        })
        
    except Exception as e:
        logger.error(f"OCR处理错误: {e}")
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
        image = decode_image(image_bytes)
        
        # OCR识别
        result, _ = ocr_engine(image)
        
        # 提取文本
        if result:
            text = '\n'.join([item[1] for item in result])
        else:
            text = ""
        
        return jsonify({
            "success": True,
            "text": text
        })
        
    except Exception as e:
        logger.error(f"OCR处理错误: {e}")
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


if __name__ == '__main__':
    # 初始化OCR引擎
    init_ocr()
    
    # 打印内存使用
    print_memory_usage()
    
    # 启动服务
    port = int(os.environ.get('PORT', 8089))
    host = os.environ.get('HOST', '0.0.0.0')
    
    logger.info(f"启动OCR服务: http://{host}:{port}")
    logger.info("引擎: RapidOCR (优化配置)")
    
    app.run(
        host=host,
        port=port,
        debug=False,
        threaded=True
    )
