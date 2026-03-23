#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试RapidOCR返回格式
"""

import cv2
import numpy as np

# 创建测试图像
img = np.ones((100, 300, 3), dtype=np.uint8) * 255
cv2.putText(img, "Test OCR", (50, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)

# 测试RapidOCR
from rapidocr_onnxruntime import RapidOCR

ocr = RapidOCR()
result, elapse = ocr(img)

print(f"Result type: {type(result)}")
print(f"Result: {result}")
print(f"Elapse: {elapse}")

if result:
    for i, item in enumerate(result):
        print(f"\nItem {i}:")
        print(f"  Type: {type(item)}")
        print(f"  Content: {item}")
        if hasattr(item, '__len__'):
            for j, sub in enumerate(item):
                print(f"    Sub {j}: type={type(sub)}, value={sub}")
