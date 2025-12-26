#!/usr/bin/env python3
# -*- coding: utf-8 -*-

def main():
    """主函数"""
    print("Hello from Python!")
    print("这是一个简单的Python脚本。")
    
    # 一些基本操作
    numbers = [1, 2, 3, 4, 5]
    total = sum(numbers)
    
    print(f"数字列表: {numbers}")
    print(f"总和: {total}")
    
    # 读取刚才创建的文本文件
    try:
        with open('example.txt', 'r', encoding='utf-8') as f:
            content = f.read()
        print("\n读取example.txt文件内容:")
        print(content)
    except FileNotFoundError:
        print("文件未找到")

if __name__ == "__main__":
    main()