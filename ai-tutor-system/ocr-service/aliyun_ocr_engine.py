#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
阿里云OCR引擎 - 基于阿里云通用文字识别服务
支持中英文混合识别，使用官方SDK
"""

import os
import json
from typing import List, Optional
from pathlib import Path


class AliyunOCREngine:
    """
    阿里云OCR推理引擎 - 基于阿里云通用文字识别服务
    """
    
    def __init__(self):
        """
        初始化阿里云OCR引擎
        """
        self._client = None
        self._init_aliyun_ocr()
    
    def _init_aliyun_ocr(self):
        """
        初始化阿里云OCR客户端
        """
        try:
            # 尝试导入阿里云SDK
            from alibabacloud_ocr_api20210707.client import Client as ocr_api20210707Client
            from alibabacloud_tea_openapi import models as open_api_models
            
            # 从环境变量获取AccessKey
            access_key_id = os.getenv('ALIBABA_CLOUD_ACCESS_KEY_ID')
            access_key_secret = os.getenv('ALIBABA_CLOUD_ACCESS_KEY_SECRET')
            
            if not access_key_id or not access_key_secret:
                print("阿里云OCR: 环境变量未配置，请设置 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET")
                self._client = None
                return
            
            # 配置
            config = open_api_models.Config(
                access_key_id=access_key_id,
                access_key_secret=access_key_secret
            )
            # 默认公网接入地址
            config.endpoint = 'ocr-api.cn-hangzhou.aliyuncs.com'
            
            # 创建客户端
            self._client = ocr_api20210707Client(config)
            
            print("阿里云OCR引擎初始化成功")
            
        except ImportError:
            print("阿里云OCR SDK未安装，请运行: pip install alibabacloud-ocr-api20210707")
            self._client = None
        except Exception as e:
            print(f"阿里云OCR初始化失败: {e}")
            self._client = None
    
    def ocr(self, image_path: str) -> List[dict]:
        """
        完整OCR流程
        
        Args:
            image_path: 图像文件路径
            
        Returns:
            识别结果列表 ["text": str, "confidence": float, "box": list]
        """
        if self._client is None:
            return self._fallback_ocr()
        
        try:
            from alibabacloud_ocr_api20210707.models import RecognizeAllTextRequest
            from alibabacloud_tea_util import models as util_models
            
            # 构建请求
            recognize_all_text_request = RecognizeAllTextRequest(
                url=f"file://{Path(image_path).absolute()}"
            )
            runtime = util_models.RuntimeOptions()
            
            # 发送请求
            resp = self._client.recognize_all_text_with_options(recognize_all_text_request, runtime)
            
            # 处理响应
            if not resp.body.data.words:
                return []
            
            results = []
            for word in resp.body.data.words:
                results.append({
                    "text": word.content,
                    "confidence": float(word.confidence),
                    "box": [
                        [int(word.position.x), int(word.position.y)],
                        [int(word.position.x + word.position.width), int(word.position.y)],
                        [int(word.position.x + word.position.width), int(word.position.y + word.position.height)],
                        [int(word.position.x), int(word.position.y + word.position.height)]
                    ]
                })
            
            # 按位置排序（从上到下，从左到右）
            if results:
                results.sort(key=lambda x: (x["box"][0][1], x["box"][0][0]))
            
            return results
            
        except Exception as e:
            print(f"阿里云OCR识别错误: {e}")
            import traceback
            traceback.print_exc()
            return self._fallback_ocr()
    
    def _fallback_ocr(self) -> List[dict]:
        """
        降级处理 - 当阿里云OCR不可用时返回空结果
        """
        return []
    
    def is_loaded(self) -> bool:
        """
        检查引擎是否已加载
        """
        return self._client is not None


# 全局实例
_aliyun_ocr_engine = None


def get_aliyun_ocr_engine() -> AliyunOCREngine:
    """
    获取阿里云OCR引擎单例
    """
    global _aliyun_ocr_engine
    if _aliyun_ocr_engine is None:
        _aliyun_ocr_engine = AliyunOCREngine()
    return _aliyun_ocr_engine
