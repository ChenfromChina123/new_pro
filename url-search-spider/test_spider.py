#!/usr/bin/env python3
"""
测试 URL 搜索爬虫
"""
import requests
import json

def test_spider(keywords):
    """
    测试运行爬虫
    """
    print(f"测试搜索关键词: {keywords}")
    
    # 启动爬虫
    url = "http://localhost:6800/schedule.json"
    data = {
        "project": "urlsearch",
        "spider": "urlsearchspider",
        "keywords": keywords
    }
    
    response = requests.post(url, data=data)
    result = response.json()
    
    if result.get("status") == "ok":
        job_id = result.get("jobid")
        print(f"爬虫启动成功！任务ID: {job_id}")
        print(f"可以在 http://localhost:6800/ 查看任务状态")
        return job_id
    else:
        print(f"爬虫启动失败: {result}")
        return None

def main():
    """
    主函数
    """
    print("URL 搜索爬虫测试工具")
    print("=" * 50)
    
    # 测试搜索
    test_keywords = ["人工智能发展趋势", "Python 爬虫教程"]
    
    for keyword in test_keywords:
        print(f"\n测试: {keyword}")
        test_spider(keyword)
        print("-" * 50)

if __name__ == "__main__":
    main()
