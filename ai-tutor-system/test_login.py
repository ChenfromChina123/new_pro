#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
登录测试脚本
获取测试账号的 token
"""

import requests

# 登录 API 端点
LOGIN_URL = "http://localhost:5000/api/auth/login"

# 测试账号
TEST_EMAIL = "3301767269@qq.com"
TEST_PASSWORD = "123456"

def test_login():
    """测试登录接口"""
    print("开始测试登录...")
    
    # 准备登录数据
    login_data = {
        "email": TEST_EMAIL,
        "password": TEST_PASSWORD
    }
    
    try:
        # 发送登录请求
        print("发送登录请求...")
        response = requests.post(LOGIN_URL, json=login_data, timeout=10)
        
        print(f"响应状态码: {response.status_code}")
        
        # 解析响应
        result = response.json()
        print("响应结果:")
        print(result)
        
        if result.get('success'):
            token = result.get('token')
            print(f"\n登录成功!")
            print(f"获取到的 token: {token}")
            return token
        else:
            print(f"\n登录失败: {result.get('message')}")
            return None
            
    except Exception as e:
        print(f"测试失败: {e}")
        return None

if __name__ == "__main__":
    token = test_login()
    if token:
        print(f"\n请将以下 token 复制到 test_ocr.py 文件中:")
        print(token)
