#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试远程OCR服务性能
服务地址: https://ocr.aistudy.icu/
"""

import requests
import time
import json
import cv2
import numpy as np
import base64

# 服务地址
BASE_URL = "https://ocr.aistudy.icu"

# 创建测试图像
def create_test_images():
    """创建多个测试图像"""
    images = []
    
    # 图像1: 简单英文文本
    img1 = np.ones((100, 400, 3), dtype=np.uint8) * 255
    cv2.putText(img1, "Hello OCR Test", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    images.append((img1, "简单英文", "Hello OCR Test"))
    
    # 图像2: 多行文本
    img2 = np.ones((200, 600, 3), dtype=np.uint8) * 255
    cv2.putText(img2, "Line 1: OCR Test", (30, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img2, "Line 2: Performance Test", (30, 100), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img2, "Line 3: Multi Line Text", (30, 150), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    images.append((img2, "多行文本", "多行文本测试"))
    
    # 图像3: 数字和符号
    img3 = np.ones((100, 500, 3), dtype=np.uint8) * 255
    cv2.putText(img3, "12345 ABCDE abcde !@#$", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    images.append((img3, "数字符号", "12345 ABCDE abcde !@#$"))
    
    # 图像4: 混合文本
    img4 = np.ones((150, 600, 3), dtype=np.uint8) * 255
    cv2.putText(img4, "Hello World 123", (30, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    cv2.putText(img4, "Test OCR Service", (30, 100), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
    images.append((img4, "混合文本", "Hello World 123 Test OCR Service"))
    
    return images

# 图像转base64
def image_to_base64(image):
    """将图像转换为base64"""
    _, buffer = cv2.imencode('.png', image)
    return base64.b64encode(buffer).decode('utf-8')

# 测试健康检查
def test_health():
    """测试健康检查"""
    print("\n[健康检查]")
    try:
        start = time.time()
        resp = requests.get(f"{BASE_URL}/health", timeout=10)
        elapsed = time.time() - start
        print(f"  响应时间: {elapsed:.3f}秒")
        print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
        return True
    except Exception as e:
        print(f"  错误: {e}")
        return False

# 测试文件上传识别
def test_file_upload(image, desc):
    """测试文件上传识别"""
    start = time.time()
    try:
        # 保存图像
        cv2.imwrite("temp_test.png", image)
        
        with open("temp_test.png", 'rb') as f:
            resp = requests.post(f"{BASE_URL}/ocr", files={'image': f}, timeout=30)
        
        elapsed = time.time() - start
        result = resp.json()
        
        if result.get('success'):
            text = result.get('text', '').strip()
            print(f"  {desc}: {elapsed:.3f}秒 - 文本: {text[:50]}...")
        else:
            print(f"  {desc}: {elapsed:.3f}秒 - 错误: {result.get('error')}")
        
        return result.get('success'), elapsed, result.get('text', '')
        
    except Exception as e:
        print(f"  {desc}: 错误 - {e}")
        return False, time.time() - start, ""

# 测试Base64识别
def test_base64(image, desc):
    """测试Base64识别"""
    start = time.time()
    try:
        base64_data = image_to_base64(image)
        resp = requests.post(
            f"{BASE_URL}/ocr/base64",
            json={'image': base64_data},
            timeout=30
        )
        
        elapsed = time.time() - start
        result = resp.json()
        
        if result.get('success'):
            text = result.get('text', '').strip()
            print(f"  {desc}: {elapsed:.3f}秒 - 文本: {text[:50]}...")
        else:
            print(f"  {desc}: {elapsed:.3f}秒 - 错误: {result.get('error')}")
        
        return result.get('success'), elapsed, result.get('text', '')
        
    except Exception as e:
        print(f"  {desc}: 错误 - {e}")
        return False, time.time() - start, ""

# 并发测试
def test_concurrency(images, test_func):
    """并发测试"""
    import threading
    
    print("\n[并发测试 - 5个并发请求]")
    
    results = []
    
    def worker(image, desc):
        success, elapsed, text = test_func(image, desc)
        results.append({'success': success, 'elapsed': elapsed, 'desc': desc})
    
    start = time.time()
    threads = []
    
    # 取前5个图像进行测试
    for i, (image, desc, _) in enumerate(images[:5]):
        t = threading.Thread(target=worker, args=(image, f"并发{i+1}"))
        threads.append(t)
        t.start()
    
    for t in threads:
        t.join()
    
    total_time = time.time() - start
    success_count = sum(1 for r in results if r['success'])
    avg_elapsed = sum(r['elapsed'] for r in results) / len(results)
    
    print(f"  总耗时: {total_time:.3f}秒")
    print(f"  平均每请求: {avg_elapsed:.3f}秒")
    print(f"  成功率: {success_count}/{len(results)}")

# 主测试
def main():
    print("=" * 60)
    print("测试OCR服务性能")
    print(f"服务地址: {BASE_URL}")
    print("=" * 60)
    
    # 测试健康检查
    if not test_health():
        print("服务不可用，测试终止")
        return
    
    # 创建测试图像
    images = create_test_images()
    print(f"\n[创建了 {len(images)} 个测试图像]")
    
    # 测试文件上传
    print("\n[文件上传识别测试]")
    file_times = []
    for image, desc, expected in images:
        success, elapsed, text = test_file_upload(image, desc)
        if success:
            file_times.append(elapsed)
    
    if file_times:
        print(f"  平均识别时间: {sum(file_times)/len(file_times):.3f}秒")
    
    # 测试Base64
    print("\n[Base64识别测试]")
    base64_times = []
    for image, desc, expected in images:
        success, elapsed, text = test_base64(image, desc)
        if success:
            base64_times.append(elapsed)
    
    if base64_times:
        print(f"  平均识别时间: {sum(base64_times)/len(base64_times):.3f}秒")
    
    # 并发测试
    test_concurrency(images, test_file_upload)
    
    # 性能总结
    print("\n" + "=" * 60)
    print("性能测试总结")
    print("=" * 60)
    
    if file_times:
        print(f"文件上传:")
        print(f"  平均: {sum(file_times)/len(file_times):.3f}秒")
        print(f"  最快: {min(file_times):.3f}秒")
        print(f"  最慢: {max(file_times):.3f}秒")
    
    if base64_times:
        print(f"Base64:")
        print(f"  平均: {sum(base64_times)/len(base64_times):.3f}秒")
        print(f"  最快: {min(base64_times):.3f}秒")
        print(f"  最慢: {max(base64_times):.3f}秒")
    
    print("\n测试完成!")

if __name__ == "__main__":
    main()
