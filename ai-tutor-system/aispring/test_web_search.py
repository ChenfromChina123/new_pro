#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试联网搜索功能
"""

import requests
import json

# 配置
BASE_URL = "http://localhost:5000"
TEST_EMAIL = "3301767269@qq.com"
TEST_PASSWORD = "123456"


def get_auth_token():
    """获取认证 token"""
    url = f"{BASE_URL}/api/auth/login"
    data = {"email": TEST_EMAIL, "password": TEST_PASSWORD}
    
    response = requests.post(url, json=data, timeout=10)
    if response.status_code == 200:
        result = response.json()
        if 'data' in result:
            return result['data'].get('access_token')
    return None


def test_search():
    """测试搜索接口"""
    print("="*90)
    print("测试联网搜索 API")
    print("="*90)
    
    token = get_auth_token()
    if not token:
        print("❌ 获取 token 失败")
        return
    
    print(f"✅ 获取 Token 成功：{token[:50]}...")
    
    url = f"{BASE_URL}/api/search"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    keyword = "科技英语单词"
    print(f"\n搜索关键词：{keyword}")
    print(f"请求 URL: {url}")
    print(f"请求头：Authorization: Bearer {token[:30]}...")
    
    try:
        response = requests.get(url, headers=headers, params={"q": keyword}, timeout=30)
        print(f"\n响应状态码：{response.status_code}")
        print(f"\n完整响应内容:")
        print(json.dumps(response.json(), ensure_ascii=False, indent=2))
    except Exception as e:
        print(f"\n❌ 请求失败：{e}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    test_search()
