#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR引擎工厂 - 用于管理不同的OCR引擎实现
"""

import os
from typing import Optional
from ocr_engine import OCREngine, get_ocr_engine
from aliyun_ocr_engine import AliyunOCREngine, get_aliyun_ocr_engine


class OCREngineFactory:
    """
    OCR引擎工厂类
    负责创建和管理不同的OCR引擎实例
    """
    
    @staticmethod
    def get_engine(engine_type: str = None) -> Optional[OCREngine]:
        """
        获取OCR引擎实例
        
        Args:
            engine_type: 引擎类型，可选值: "local", "aliyun"
                        如果为None，将根据环境变量OCR_ENGINE_TYPE决定
        
        Returns:
            OCR引擎实例
        """
        # 确定引擎类型
        if engine_type is None:
            engine_type = os.getenv('OCR_ENGINE_TYPE', 'local')
        
        # 根据引擎类型返回对应的引擎实例
        if engine_type == 'aliyun':
            return get_aliyun_ocr_engine()
        else:  # 默认使用本地引擎
            return get_ocr_engine()
    
    @staticmethod
    def is_aliyun_available() -> bool:
        """
        检查阿里云OCR是否可用
        
        Returns:
            bool: 阿里云OCR是否可用
        """
        aliyun_engine = get_aliyun_ocr_engine()
        return aliyun_engine.is_loaded()
    
    @staticmethod
    def is_local_available() -> bool:
        """
        检查本地OCR是否可用
        
        Returns:
            bool: 本地OCR是否可用
        """
        local_engine = get_ocr_engine()
        return local_engine.is_loaded()
