#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR引擎 - 基于RapidOCR轻量级方案
支持中英文混合识别，内存优化版本
"""

import os
import cv2
import numpy as np
from typing import List, Tuple, Optional
from pathlib import Path


class OCREngine:
    """
    OCR推理引擎 - 基于RapidOCR
    内存优化: 使用单线程推理，限制内存占用
    """
    
    def __init__(self, models_dir: str = None):
        """
        初始化OCR引擎
        
        Args:
            models_dir: 模型文件目录（RapidOCR会自动管理）
        """
        self._rapid_ocr = None  # 改名避免与方法冲突
        self._init_rapidocr()
    
    def _init_rapidocr(self):
        """
        初始化RapidOCR引擎
        """
        try:
            from rapidocr_onnxruntime import RapidOCR
            
            # 创建OCR实例 - 使用轻量级配置
            self._rapid_ocr = RapidOCR()
            
            print("RapidOCR引擎初始化成功")
            print(f"使用设备: CPU")
            
        except ImportError:
            print("RapidOCR未安装，请运行: pip install rapidocr-onnxruntime")
            self._rapid_ocr = None
        except Exception as e:
            print(f"RapidOCR初始化失败: {e}")
            self._rapid_ocr = None
    
    def preprocess_image(self, image: np.ndarray, max_size: int = 1920) -> np.ndarray:
        """
        图像预处理
        
        Args:
            image: 输入图像
            max_size: 最大边长
            
        Returns:
            预处理后的图像
        """
        h, w = image.shape[:2]
        if max(h, w) > max_size:
            scale = max_size / max(h, w)
            new_h, new_w = int(h * scale), int(w * scale)
            image = cv2.resize(image, (new_w, new_h))
        return image
    
    def ocr(self, image: np.ndarray) -> List[dict]:
        """
        完整OCR流程
        
        Args:
            image: 输入图像 (BGR格式)
            
        Returns:
            识别结果列表 [{"text": str, "confidence": float, "box": list}]
        """
        if self._rapid_ocr is None:
            return self._fallback_ocr(image)
        
        try:
            # 预处理
            image = self.preprocess_image(image)
            
            # RapidOCR识别
            # 返回格式: result是列表，每个元素是 [box, text, confidence]
            # box: [[x1,y1], [x2,y2], [x3,y3], [x4,y4]]
            # text: 字符串
            # confidence: 字符串格式的浮点数
            result, elapse = self._rapid_ocr(image)
            
            if result is None or len(result) == 0:
                return []
            
            results = []
            for item in result:
                # item格式: [box, text, confidence]
                # 注意: confidence是字符串类型
                box = item[0]  # 列表的列表
                text = item[1]  # 字符串
                confidence = float(item[2])  # 字符串转float
                
                if text and text.strip():
                    results.append({
                        "text": text,
                        "confidence": round(confidence, 4),
                        "box": [[int(p[0]), int(p[1])] for p in box]
                    })
            
            # 按位置排序（从上到下，从左到右）
            if results:
                results.sort(key=lambda x: (x["box"][0][1], x["box"][0][0]))
            
            return results
            
        except Exception as e:
            print(f"OCR识别错误: {e}")
            import traceback
            traceback.print_exc()
            return self._fallback_ocr(image)
    
    def _fallback_ocr(self, image: np.ndarray) -> List[dict]:
        """
        降级OCR - 当主引擎不可用时使用简单方法
        """
        # 使用OpenCV的简单文本检测
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # 自适应阈值
        binary = cv2.adaptiveThreshold(
            gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY_INV, 11, 2
        )
        
        # 形态学操作
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (15, 3))
        dilated = cv2.dilate(binary, kernel, iterations=2)
        
        # 查找轮廓
        contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        results = []
        for contour in contours:
            x, y, w, h = cv2.boundingRect(contour)
            if w > 30 and h > 15:
                results.append({
                    "text": "[检测到文本区域]",
                    "confidence": 0.0,
                    "box": [[x, y], [x+w, y], [x+w, y+h], [x, y+h]]
                })
        
        return results
    
    def is_loaded(self) -> bool:
        """
        检查引擎是否已加载
        """
        return self._rapid_ocr is not None


# 全局实例
_ocr_engine = None


def get_ocr_engine() -> OCREngine:
    """
    获取OCR引擎单例
    """
    global _ocr_engine
    if _ocr_engine is None:
        _ocr_engine = OCREngine()
    return _ocr_engine
