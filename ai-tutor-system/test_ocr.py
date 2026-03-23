#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR 功能测试脚本
测试阿里云 OCR 接口是否正常工作
"""

import requests
import base64
import os

# 测试图片路径
TEST_IMAGE_PATH = "d:\\Users\\Administrator\\AistudyProject\\new_pro\\ai-tutor-system\\ocr-service\\test_image.png"

# API 端点
API_URL = "http://localhost:5000/api/ocr/recognize"

# 测试账号 token
# 从登录测试获取的 token
TEST_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJpc19hZG1pbiI6dHJ1ZSwidXNlcl9pZCI6MjEsInN1YiI6IjMzMDE3NjcyNjlAcXEuY29tIiwiaWF0IjoxNzc0Mjc1NzM4LCJleHAiOjE3NzQyODI5Mzh9.BDeJu00PqwkemyX79DmWw9lQRkPcRw4L7_U0XcxDuSo"

def test_ocr():
    """测试 OCR 接口"""
    print("开始测试 OCR 功能...")
    
    # 检查测试图片是否存在
    if not os.path.exists(TEST_IMAGE_PATH):
        print(f"错误：测试图片不存在: {TEST_IMAGE_PATH}")
        return
    
    print(f"使用测试图片: {TEST_IMAGE_PATH}")
    
    # 读取图片文件
    with open(TEST_IMAGE_PATH, 'rb') as f:
        image_data = f.read()
    
    # 准备请求数据
    files = {
        'image': ('test_image.png', image_data, 'image/png')
    }
    
    headers = {
        'Authorization': f'Bearer {TEST_TOKEN}'
    }
    
    try:
        # 发送请求
        print("发送 OCR 请求...")
        response = requests.post(API_URL, files=files, headers=headers, timeout=30)
        
        print(f"响应状态码: {response.status_code}")
        
        # 解析响应
        result = response.json()
        print("响应结果:")
        print(result)
        
        if result.get('success'):
            print("\nOCR 识别成功!")
            print(f"识别文本: {result.get('text')}")
        else:
            print(f"\nOCR 识别失败: {result.get('error')}")
            
    except Exception as e:
        print(f"测试失败: {e}")

if __name__ == "__main__":
    test_ocr()
