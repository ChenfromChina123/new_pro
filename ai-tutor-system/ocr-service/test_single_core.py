#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
极限单核CPU测试
模拟低配服务器环境
"""

import time
import os
import requests
import json
import threading
import psutil
import cv2
import numpy as np

# 限制CPU使用
def set_cpu_affinity():
    """限制进程只使用一个CPU核心"""
    try:
        p = psutil.Process(os.getpid())
        p.cpu_affinity([0])  # 只使用第一个核心
        print(f"CPU亲和性已设置: {p.cpu_affinity()}")
    except Exception as e:
        print(f"设置CPU亲和性失败: {e}")

# 创建测试图像
def create_test_images():
    """创建多个测试图像"""
    images = []
    
    # 图像1: 简单文本
    img1 = np.ones((100, 400, 3), dtype=np.uint8) * 255
    cv2.putText(img1, "Hello World Test OCR", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    cv2.imwrite("test_single.png", img1)
    images.append(("test_single.png", "Hello World Test OCR"))
    
    # 图像2: 多行文本
    img2 = np.ones((200, 600, 3), dtype=np.uint8) * 255
    cv2.putText(img2, "Line 1: OCR Test", (30, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img2, "Line 2: Performance", (30, 100), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img2, "Line 3: Single Core", (30, 150), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.imwrite("test_multi.png", img2)
    images.append(("test_multi.png", "多行文本"))
    
    # 图像3: 原测试图像
    images.append(("test_image.png", "原测试图像"))
    
    return images

# 测试OCR服务
def test_ocr_service(image_path, base_url="http://localhost:8089"):
    """测试单个图像"""
    start = time.time()
    try:
        with open(image_path, 'rb') as f:
            resp = requests.post(f"{base_url}/ocr", files={'image': f}, timeout=30)
        elapsed = time.time() - start
        result = resp.json()
        return {
            "success": result.get("success", False),
            "elapsed": elapsed,
            "text": result.get("text", "")[:50],
            "error": result.get("error", "")
        }
    except Exception as e:
        return {
            "success": False,
            "elapsed": time.time() - start,
            "text": "",
            "error": str(e)
        }

# 主测试
def main():
    print("=" * 60)
    print("极限单核CPU测试")
    print("=" * 60)
    
    # 设置CPU亲和性
    set_cpu_affinity()
    
    # 创建测试图像
    images = create_test_images()
    
    # 测试服务健康状态
    print("\n[健康检查]")
    try:
        resp = requests.get("http://localhost:8089/health", timeout=5)
        print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
    except Exception as e:
        print(f"服务未启动: {e}")
        return
    
    # 单次测试
    print("\n[单次识别测试]")
    for img_path, desc in images:
        if os.path.exists(img_path):
            result = test_ocr_service(img_path)
            print(f"  {desc}: {result['elapsed']:.3f}秒 - {result['text']}")
    
    # 连续测试
    print("\n[连续10次测试 - test_single.png]")
    times = []
    for i in range(10):
        result = test_ocr_service("test_single.png")
        times.append(result['elapsed'])
        print(f"  第{i+1}次: {result['elapsed']:.3f}秒")
    
    print(f"\n[统计]")
    print(f"  平均: {sum(times)/len(times):.3f}秒")
    print(f"  最快: {min(times):.3f}秒")
    print(f"  最慢: {max(times):.3f}秒")
    print(f"  中位数: {sorted(times)[len(times)//2]:.3f}秒")
    
    # 并发测试
    print("\n[并发测试 - 5个并发请求]")
    start = time.time()
    threads = []
    results = []
    
    def worker(img_path):
        result = test_ocr_service(img_path)
        results.append(result)
    
    for i in range(5):
        t = threading.Thread(target=worker, args=("test_single.png",))
        threads.append(t)
        t.start()
    
    for t in threads:
        t.join()
    
    total_time = time.time() - start
    print(f"  总耗时: {total_time:.3f}秒")
    print(f"  平均每请求: {total_time/5:.3f}秒")
    print(f"  成功率: {sum(1 for r in results if r['success'])}/5")
    
    # 内存监控
    print("\n[内存使用]")
    process = psutil.Process(os.getpid())
    mem = process.memory_info()
    print(f"  当前进程: RSS={mem.rss/1024/1024:.1f}MB")
    
    print("\n" + "=" * 60)
    print("测试完成!")
    print("=" * 60)

if __name__ == "__main__":
    main()
