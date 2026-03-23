#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试RapidOCR优化配置
"""

import time
import cv2
import numpy as np

# 创建测试图像
img = np.ones((100, 300, 3), dtype=np.uint8) * 255
cv2.putText(img, "Hello OCR Test", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
cv2.imwrite("test_simple.png", img)

print("=" * 60)
print("RapidOCR 优化配置测试")
print("=" * 60)

# 测试不同配置
from rapidocr_onnxruntime import RapidOCR

# 默认配置
print("\n[默认配置]")
start = time.time()
ocr1 = RapidOCR()
print(f"模型加载: {time.time() - start:.2f}秒")

times = []
for i in range(5):
    start = time.time()
    result, _ = ocr1("test_simple.png")
    elapsed = time.time() - start
    times.append(elapsed)
    text = result[0][1] if result else ""
    print(f"  第{i+1}次: {elapsed:.3f}秒 - 文本: {text}")

print(f"平均: {sum(times)/len(times):.3f}秒")

# 优化配置 - 禁用方向分类器
print("\n[优化配置 - 禁用方向分类]")
start = time.time()
ocr2 = RapidOCR(
    use_det=True,      # 启用文本检测
    use_cls=False,     # 禁用方向分类（加速）
    use_rec=True,      # 启用文本识别
)
print(f"模型加载: {time.time() - start:.2f}秒")

times = []
for i in range(5):
    start = time.time()
    result, _ = ocr2("test_simple.png")
    elapsed = time.time() - start
    times.append(elapsed)
    text = result[0][1] if result else ""
    print(f"  第{i+1}次: {elapsed:.3f}秒 - 文本: {text}")

print(f"平均: {sum(times)/len(times):.3f}秒")

# 最轻量配置 - 只识别不检测
print("\n[最轻量配置 - 纯识别模式]")
# 直接识别整张图片
start = time.time()
ocr3 = RapidOCR(
    use_det=False,     # 禁用文本检测
    use_cls=False,     # 禁用方向分类
    use_rec=True,      # 只用识别
)
print(f"模型加载: {time.time() - start:.2f}秒")

times = []
for i in range(5):
    start = time.time()
    # 直接传入图像数组
    result, _ = ocr3(img)
    elapsed = time.time() - start
    times.append(elapsed)
    text = result[0][1] if result else ""
    print(f"  第{i+1}次: {elapsed:.3f}秒 - 文本: {text}")

print(f"平均: {sum(times)/len(times):.3f}秒")

print("\n" + "=" * 60)
print("测试完成!")
print("=" * 60)
