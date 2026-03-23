#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR识别测试
"""

import requests
import json
import base64
import cv2
import numpy as np
from pathlib import Path

BASE_URL = "http://localhost:8089"

def create_test_image():
    """
    创建测试图像
    """
    # 创建白色背景
    img = np.ones((300, 800, 3), dtype=np.uint8) * 255
    
    # 添加英文文字
    cv2.putText(img, "Hello OCR Service!", (50, 60), 
                cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 0, 0), 2)
    
    # 添加更多英文
    cv2.putText(img, "This is a test image for OCR.", (50, 120), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    
    # 添加数字
    cv2.putText(img, "12345 ABCDE abcde", (50, 180), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    
    # 添加中文（使用OpenCV默认字体可能显示为方框，但OCR应该能识别）
    cv2.putText(img, "OCR Test 2024", (50, 250), 
                cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 0), 2)
    
    # 保存
    test_path = Path(__file__).parent / "test_image.png"
    cv2.imwrite(str(test_path), img)
    print(f"测试图像已创建: {test_path}")
    
    return str(test_path)


def test_ocr_file(image_path: str):
    """
    测试文件上传OCR
    """
    print("\n" + "=" * 60)
    print(f"测试: 文件上传OCR")
    print("=" * 60)
    
    with open(image_path, 'rb') as f:
        files = {'image': f}
        response = requests.post(f"{BASE_URL}/ocr", files=files)
    
    print(f"状态码: {response.status_code}")
    result = response.json()
    print(f"成功: {result.get('success')}")
    print(f"识别数量: {result.get('count')}")
    print(f"完整文本:\n{result.get('full_text')}")
    print(f"\n详细结果:")
    for i, item in enumerate(result.get('results', []), 1):
        print(f"  [{i}] {item['text']} (置信度: {item['confidence']})")
    
    return result.get('success', False)


def test_ocr_base64(image_path: str):
    """
    测试Base64 OCR
    """
    print("\n" + "=" * 60)
    print(f"测试: Base64 OCR")
    print("=" * 60)
    
    with open(image_path, 'rb') as f:
        image_data = base64.b64encode(f.read()).decode('utf-8')
    
    payload = {"image": image_data}
    response = requests.post(f"{BASE_URL}/ocr/base64", json=payload)
    
    print(f"状态码: {response.status_code}")
    result = response.json()
    print(f"成功: {result.get('success')}")
    print(f"识别数量: {result.get('count')}")
    print(f"完整文本:\n{result.get('full_text')}")
    
    return result.get('success', False)


if __name__ == "__main__":
    # 创建测试图像
    test_image = create_test_image()
    
    # 测试文件上传
    test_ocr_file(test_image)
    
    # 测试Base64
    test_ocr_base64(test_image)
    
    print("\n" + "=" * 60)
    print("测试完成!")
    print("=" * 60)
