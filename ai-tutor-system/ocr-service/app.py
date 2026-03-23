#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR Service - Flask REST API
轻量级OCR服务，支持中英文混合识别
内存限制: ~150MB
"""

import os
import io
import base64
import logging
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS
import cv2
import numpy as np

from ocr_engine import get_ocr_engine, OCREngine

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
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 最大16MB
app.config['JSON_AS_ASCII'] = False  # 支持中文

# 全局变量
ocr_engine = None
start_time = datetime.now()


def init_ocr():
    """
    初始化OCR引擎
    """
    global ocr_engine
    try:
        logger.info("正在初始化OCR引擎...")
        ocr_engine = get_ocr_engine()
        logger.info("OCR引擎初始化完成")
    except Exception as e:
        logger.error(f"OCR引擎初始化失败: {e}")
        ocr_engine = None


def decode_image(image_data):
    """
    解码图像数据
    
    Args:
        image_data: 图像数据 (bytes, base64, 或 numpy array)
        
    Returns:
        numpy array (BGR格式)
    """
    if isinstance(image_data, np.ndarray):
        return image_data
    
    if isinstance(image_data, str):
        # Base64编码
        if image_data.startswith('data:image'):
            image_data = image_data.split(',')[1]
        image_bytes = base64.b64decode(image_data)
    else:
        image_bytes = image_data
    
    # 解码图像
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
        "service": "ocr-service",
        "version": "1.0.0",
        "uptime_seconds": int(uptime),
        "model_loaded": ocr_engine is not None and ocr_engine.is_loaded()
    })


@app.route('/', methods=['GET'])
def index():
    """
    首页
    """
    return jsonify({
        "service": "OCR Service",
        "version": "1.0.0",
        "description": "轻量级OCR服务 - CRNN ONNX",
        "endpoints": {
            "POST /ocr": "识别图像中的文字",
            "POST /ocr/base64": "识别Base64编码图像中的文字",
            "GET /health": "健康检查"
        },
        "memory_limit": "~150MB",
        "supported_languages": ["中文", "英文", "中英混合"]
    })


@app.route('/ocr', methods=['POST'])
def ocr_image():
    """
    OCR识别接口 - 文件上传
    
    请求:
        multipart/form-data: image文件
        
    返回:
        {
            "success": true,
            "results": [
                {
                    "text": "识别的文字",
                    "confidence": 0.95,
                    "box": [[x1,y1], [x2,y2], [x3,y3], [x4,y4]]
                }
            ],
            "full_text": "所有文字拼接"
        }
    """
    if ocr_engine is None:
        return jsonify({
            "success": False,
            "error": "OCR引擎未初始化"
        }), 500
    
    try:
        # 检查文件
        if 'image' not in request.files:
            return jsonify({
                "success": False,
                "error": "未找到图像文件，请使用 'image' 字段上传"
            }), 400
        
        file = request.files['image']
        if file.filename == '':
            return jsonify({
                "success": False,
                "error": "未选择文件"
            }), 400
        
        # 读取图像
        image_bytes = file.read()
        image = decode_image(image_bytes)
        
        if image is None:
            return jsonify({
                "success": False,
                "error": "无法解析图像文件"
            }), 400
        
        logger.info(f"处理图像: {image.shape}")
        
        # OCR识别
        results = ocr_engine.ocr(image)
        
        # 拼接完整文本
        full_text = '\n'.join([r['text'] for r in results])
        
        return jsonify({
            "success": True,
            "results": results,
            "full_text": full_text,
            "count": len(results)
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
    
    请求:
        {
            "image": "base64编码的图像数据",
            "options": {
                "preprocess": true
            }
        }
        
    返回:
        {
            "success": true,
            "results": [...],
            "full_text": "..."
        }
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
        
        # 解码图像
        image = decode_image(data['image'])
        
        if image is None:
            return jsonify({
                "success": False,
                "error": "无法解析图像数据"
            }), 400
        
        logger.info(f"处理Base64图像: {image.shape}")
        
        # OCR识别
        results = ocr_engine.ocr(image)
        
        # 拼接完整文本
        full_text = '\n'.join([r['text'] for r in results])
        
        return jsonify({
            "success": True,
            "results": results,
            "full_text": full_text,
            "count": len(results)
        })
        
    except Exception as e:
        logger.error(f"OCR处理错误: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/ocr/url', methods=['POST'])
def ocr_url():
    """
    OCR识别接口 - URL
    
    请求:
        {
            "url": "图像URL"
        }
    """
    if ocr_engine is None:
        return jsonify({
            "success": False,
            "error": "OCR引擎未初始化"
        }), 500
    
    try:
        import urllib.request
        
        data = request.get_json()
        
        if not data or 'url' not in data:
            return jsonify({
                "success": False,
                "error": "请求体需要包含 'url' 字段"
            }), 400
        
        # 下载图像
        with urllib.request.urlopen(data['url'], timeout=10) as response:
            image_bytes = response.read()
        
        image = decode_image(image_bytes)
        
        if image is None:
            return jsonify({
                "success": False,
                "error": "无法解析图像数据"
            }), 400
        
        logger.info(f"处理URL图像: {image.shape}")
        
        # OCR识别
        results = ocr_engine.ocr(image)
        full_text = '\n'.join([r['text'] for r in results])
        
        return jsonify({
            "success": True,
            "results": results,
            "full_text": full_text,
            "count": len(results)
        })
        
    except Exception as e:
        logger.error(f"OCR处理错误: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.errorhandler(413)
def request_entity_too_large(error):
    """
    文件过大错误处理
    """
    return jsonify({
        "success": False,
        "error": "文件过大，最大支持16MB"
    }), 413


@app.errorhandler(500)
def internal_error(error):
    """
    服务器错误处理
    """
    return jsonify({
        "success": False,
        "error": "服务器内部错误"
    }), 500


def print_memory_usage():
    """
    打印内存使用情况
    """
    try:
        import psutil
        import os
        process = psutil.Process(os.getpid())
        mem_info = process.memory_info()
        logger.info(f"内存使用: RSS={mem_info.rss/1024/1024:.1f}MB, VMS={mem_info.vms/1024/1024:.1f}MB")
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
    logger.info("API文档: http://localhost:8089/")
    
    # 开发模式
    app.run(
        host=host,
        port=port,
        debug=False,
        threaded=True
    )
