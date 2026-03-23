#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试ddddocr识别
"""

import ddddocr
import cv2
import numpy as np

# 创建测试图像
img = np.ones((100, 300, 3), dtype=np.uint8) * 255
cv2.putText(img, "Hello OCR Test", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)

# 保存
cv2.imwrite("test_simple.png", img)

# 测试识别
ocr = ddddocr.DdddOcr(show_ad=False)

# 方法1: 直接读取文件
with open("test_simple.png", "rb") as f:
    result = ocr.classification(f.read())
    print(f"方法1结果: {result}")

# 方法2: 读取原来的测试图像
with open("test_image.png", "rb") as f:
    result = ocr.classification(f.read())
    print(f"方法2结果: {result}")

print("\n测试完成!")
