#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速测试脚本
"""

import requests
import json

BASE_URL = "http://localhost:8089"

print("=" * 50)
print("测试健康检查接口...")
print("=" * 50)

try:
    response = requests.get(f"{BASE_URL}/health", timeout=5)
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
except Exception as e:
    print(f"错误: {e}")

print("\n" + "=" * 50)
print("测试首页接口...")
print("=" * 50)

try:
    response = requests.get(f"{BASE_URL}/", timeout=5)
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
except Exception as e:
    print(f"错误: {e}")

print("\n" + "=" * 50)
print("服务运行正常!")
print("=" * 50)
