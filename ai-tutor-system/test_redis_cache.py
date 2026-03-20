import requests
import time
import json

# API 基础 URL
BASE_URL = "http://localhost:5000"

# 测试单词列表
TEST_WORDS = ["test", "hello", "world", "example", "python", "java", "spring", "redis", "database", "performance"]

# 测试函数
def test_word_search(keyword, language="en", page=1, size=10):
    """测试单词搜索 API 的响应时间"""
    url = f"{BASE_URL}/api/vocabulary/public/search"
    params = {"keyword": keyword, "language": language, "page": page, "size": size}

    # 记录开始时间
    start_time = time.time()

    # 发送请求
    response = requests.get(url, params=params)

    # 记录结束时间
    end_time = time.time()

    # 计算响应时间（毫秒）
    response_time = (end_time - start_time) * 1000

    # 解析响应
    if response.status_code == 200:
        data = response.json()
        words = data.get("words", [])
        print(f"  响应状态: 成功, 单词数量: {len(words)}")
        return response_time, len(words) > 0
    else:
        print(f"  响应状态: 失败, 状态码: {response.status_code}")
        return response_time, False

def main():
    """主测试函数"""
    print("开始测试 Redis 缓存性能提升效果...\n")

    # 测试结果存储
    results = {
        "word_search": []
    }

    # 测试单词搜索
    print("=== 测试单词搜索 API ===")
    search_keywords = ["test", "hello", "java", "spring", "redis", "database"]
    for keyword in search_keywords:
        # 首次搜索
        first_time, first_success = test_word_search(keyword)

        # 第二次搜索
        second_time, second_success = test_word_search(keyword)

        # 计算性能提升
        if first_success and second_success and first_time > 0:
            improvement = ((first_time - second_time) / first_time) * 100
        else:
            improvement = 0

        results["word_search"].append({
            "keyword": keyword,
            "first_time": first_time,
            "second_time": second_time,
            "improvement": improvement
        })

        print(f"关键词: {keyword}")
        print(f"  首次搜索: {first_time:.2f} ms")
        print(f"  缓存搜索: {second_time:.2f} ms")
        print(f"  性能提升: {improvement:.2f}%\n")

    # 计算平均性能提升
    print("=== 性能提升统计 ===")

    # 单词搜索平均提升
    word_search_improvements = [r["improvement"] for r in results["word_search"] if r["improvement"] > 0]
    if word_search_improvements:
        avg_word_search_improvement = sum(word_search_improvements) / len(word_search_improvements)
        print(f"单词搜索平均性能提升: {avg_word_search_improvement:.2f}%")

    # 保存测试结果
    with open("redis_cache_test_results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print("\n测试结果已保存到 redis_cache_test_results.json")

if __name__ == "__main__":
    main()
