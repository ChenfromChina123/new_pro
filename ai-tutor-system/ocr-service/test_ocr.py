#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR服务测试脚本
测试各个API接口
"""

import requests
import base64
import json
import os
from pathlib import Path

# 服务地址
BASE_URL = "http://localhost:8089"


def test_health():
    """
    测试健康检查接口
    """
    print("\n" + "=" * 50)
    print("测试: 健康检查")
    print("=" * 50)
    
    response = requests.get(f"{BASE_URL}/health")
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
    
    return response.status_code == 200


def test_index():
    """
    测试首页接口
    """
    print("\n" + "=" * 50)
    print("测试: 首页")
    print("=" * 50)
    
    response = requests.get(f"{BASE_URL}/")
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
    
    return response.status_code == 200


def test_ocr_file(image_path: str):
    """
    测试文件上传OCR
    """
    print("\n" + "=" * 50)
    print(f"测试: 文件上传OCR - {image_path}")
    print("=" * 50)
    
    if not os.path.exists(image_path):
        print(f"文件不存在: {image_path}")
        return False
    
    with open(image_path, 'rb') as f:
        files = {'image': f}
        response = requests.post(f"{BASE_URL}/ocr", files=files)
    
    print(f"状态码: {response.status_code}")
    result = response.json()
    print(f"成功: {result.get('success')}")
    print(f"识别数量: {result.get('count')}")
    print(f"完整文本:\n{result.get('full_text')}")
    
    return result.get('success', False)


def test_ocr_base64(image_path: str):
    """
    测试Base64 OCR
    """
    print("\n" + "=" * 50)
    print(f"测试: Base64 OCR - {image_path}")
    print("=" * 50)
    
    if not os.path.exists(image_path):
        print(f"文件不存在: {image_path}")
        return False
    
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


def create_test_image():
    """
    创建测试图像
    """
    import cv2
    import numpy as np
    
    # 创建白色背景
    img = np.ones((200, 600, 3), dtype=np.uint8) * 255
    
    # 添加文字
    cv2.putText(img, "Hello OCR Test!", (50, 50), 
                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    cv2.putText(img, "OCR Service Demo", (50, 120), 
                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    
    # 保存
    test_path = Path(__file__).parent / "test_image.png"
    cv2.imwrite(str(test_path), img)
    print(f"测试图像已创建: {test_path}")
    
    return str(test_path)


def main():
    """
    运行所有测试
    """
    print("=" * 50)
    print("OCR Service 测试")
    print("=" * 50)
    
    # 测试健康检查
    try:
        if not test_health():
            print("\n服务未启动，请先运行 start.bat 启动服务")
            return
    except requests.exceptions.ConnectionError:
        print("\n无法连接服务，请先运行 start.bat 启动服务")
        return
    
    # 测试首页
    test_index()
    
    # 创建测试图像
    test_image = create_test_image()
    
    # 测试文件上传
    test_ocr_file(test_image)
    
    # 测试Base64
    test_ocr_base64(test_image)
    
    print("\n" + "=" * 50)
    print("测试完成!")
    print("=" * 50)


if __name__ == "__main__":
    main()
