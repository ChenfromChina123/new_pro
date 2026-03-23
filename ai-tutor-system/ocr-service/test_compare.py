#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对比测试两个OCR引擎的速度
"""

import time
import cv2
import numpy as np

# 创建测试图像
img = np.ones((100, 300, 3), dtype=np.uint8) * 255
cv2.putText(img, "Hello OCR Test", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
cv2.imwrite("test_simple.png", img)

print("=" * 60)
print("OCR引擎速度对比测试")
print("=" * 60)

# 测试 RapidOCR
print("\n[RapidOCR 测试]")
print("首次加载模型可能较慢...")

start = time.time()
from rapidocr_onnxruntime import RapidOCR
ocr1 = RapidOCR()
print(f"模型加载耗时: {time.time() - start:.2f}秒")

# 多次测试
times = []
for i in range(5):
    start = time.time()
    result, _ = ocr1("test_simple.png")
    elapsed = time.time() - start
    times.append(elapsed)
    text = result[0][1] if result else ""
    print(f"  第{i+1}次: {elapsed:.3f}秒 - 文本: {text}")

print(f"平均: {sum(times)/len(times):.3f}秒")

# 测试 ddddocr
print("\n[ddddocr 测试]")

start = time.time()
import ddddocr
ocr2 = ddddocr.DdddOcr(show_ad=False)
print(f"模型加载耗时: {time.time() - start:.2f}秒")

# 多次测试
times = []
for i in range(5):
    start = time.time()
    with open("test_simple.png", "rb") as f:
        text = ocr2.classification(f.read())
    elapsed = time.time() - start
    times.append(elapsed)
    print(f"  第{i+1}次: {elapsed:.3f}秒 - 文本: {text}")

print(f"平均: {sum(times)/len(times):.3f}秒")

print("\n" + "=" * 60)
print("测试完成!")
print("=" * 60)
