#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
直接测试OCR引擎
"""

import cv2
import numpy as np
import sys
import os

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from ocr_engine import OCREngine

# 创建测试图像
img = np.ones((100, 300, 3), dtype=np.uint8) * 255
cv2.putText(img, "Test OCR", (50, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)

# 测试OCR引擎
engine = OCREngine()
results = engine.ocr(img)

print(f"\n结果: {results}")
