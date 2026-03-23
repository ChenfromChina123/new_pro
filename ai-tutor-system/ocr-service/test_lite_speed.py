#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试轻量级OCR服务速度
"""

import requests
import time
import json

BASE_URL = "http://localhost:8089"

# 测试图像
test_image = "test_image.png"

print("=" * 50)
print("测试轻量级OCR服务 (ddddocr)")
print("=" * 50)

# 健康检查
print("\n[健康检查]")
resp = requests.get(f"{BASE_URL}/health")
print(json.dumps(resp.json(), indent=2, ensure_ascii=False))

# 测试识别速度 - 多次测试取平均
print("\n[速度测试] 5次识别测试:")
times = []

for i in range(5):
    start = time.time()
    with open(test_image, 'rb') as f:
        resp = requests.post(f"{BASE_URL}/ocr", files={'image': f})
    elapsed = time.time() - start
    times.append(elapsed)
    
    result = resp.json()
    print(f"  第{i+1}次: {elapsed:.3f}秒 - 文本: {result.get('text', '')[:50]}...")

print(f"\n[统计]")
print(f"  平均耗时: {sum(times)/len(times):.3f}秒")
print(f"  最快: {min(times):.3f}秒")
print(f"  最慢: {max(times):.3f}秒")

print("\n" + "=" * 50)
print("测试完成!")
print("=" * 50)
