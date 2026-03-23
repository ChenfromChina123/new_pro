#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试日志功能
"""

import os
import time
import json
import requests
import cv2
import numpy as np

# 服务地址
BASE_URL = "http://localhost:8089"

# 创建测试图像
def create_test_image():
    """创建测试图像"""
    img = np.ones((100, 400, 3), dtype=np.uint8) * 255
    cv2.putText(img, "Test Logging", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    return img

# 测试健康检查
def test_health():
    """测试健康检查"""
    print("测试健康检查...")
    try:
        resp = requests.get(f"{BASE_URL}/health", timeout=10)
        print(f"状态码: {resp.status_code}")
        print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
        return True
    except Exception as e:
        print(f"错误: {e}")
        return False

# 测试OCR识别
def test_ocr():
    """测试OCR识别"""
    print("测试OCR识别...")
    try:
        img = create_test_image()
        cv2.imwrite("test_logging.png", img)
        
        with open("test_logging.png", 'rb') as f:
            resp = requests.post(f"{BASE_URL}/ocr", files={'image': f}, timeout=30)
        
        print(f"状态码: {resp.status_code}")
        print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
        return True
    except Exception as e:
        print(f"错误: {e}")
        return False

# 检查日志文件
def check_logs():
    """检查日志文件"""
    print("检查日志文件...")
    log_dir = "logs"
    if os.path.exists(log_dir):
        print(f"日志目录存在: {log_dir}")
        files = os.listdir(log_dir)
        print(f"日志文件: {files}")
        
        # 检查最新的日志文件
        for file in files:
            if file.endswith('.log'):
                log_path = os.path.join(log_dir, file)
                print(f"查看日志文件: {file}")
                try:
                    with open(log_path, 'r', encoding='utf-8', errors='replace') as f:
                        lines = f.readlines()
                        if lines:
                            print("最新日志:")
                            for line in lines[-5:]:  # 显示最后5行
                                print(line.strip())
                except Exception as e:
                    print(f"读取日志文件错误: {e}")
    else:
        print("日志目录不存在")

# 主测试
def main():
    print("=" * 60)
    print("测试日志功能")
    print("=" * 60)
    
    # 测试健康检查
    test_health()
    time.sleep(1)
    
    # 测试OCR识别
    test_ocr()
    time.sleep(1)
    
    # 检查日志
    check_logs()
    
    print("\n测试完成!")

if __name__ == "__main__":
    main()
