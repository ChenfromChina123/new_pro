#!/usr/bin/env python3
"""
测试新添加的 API 端点
包括：检查点、批准、会话状态相关 API
"""

import requests
import json
import sys

BASE_URL = "http://localhost:5000/api/terminal"

# 测试用的用户ID和会话ID（需要根据实际情况修改）
TEST_USER_ID = 21
TEST_SESSION_ID = "test-session-" + str(hash("test") % 10000)
TEST_DECISION_ID = "test-decision-" + str(hash("test") % 10000)

# 测试用的Token（需要先登录获取）
TEST_TOKEN = None

def print_section(title):
    print("\n" + "="*60)
    print(f"  {title}")
    print("="*60)

def print_result(name, response):
    status = "✅ PASS" if response.status_code in [200, 201] else "❌ FAIL"
    print(f"{status} {name}")
    print(f"   Status: {response.status_code}")
    if response.status_code in [200, 201]:
        try:
            data = response.json()
            print(f"   Response: {json.dumps(data, indent=2, ensure_ascii=False)[:200]}...")
        except:
            print(f"   Response: {response.text[:200]}...")
    else:
        print(f"   Error: {response.text[:200]}...")
    print()

def test_checkpoint_apis():
    """测试检查点相关 API"""
    print_section("测试检查点相关 API")
    
    headers = {"Authorization": f"Bearer {TEST_TOKEN}"} if TEST_TOKEN else {}
    
    # 1. 获取会话检查点
    print("1. GET /checkpoints/{sessionId}")
    response = requests.get(
        f"{BASE_URL}/checkpoints/{TEST_SESSION_ID}",
        headers=headers
    )
    print_result("获取会话检查点", response)
    
    # 2. 创建手动检查点
    print("2. POST /checkpoints")
    checkpoint_data = {
        "sessionId": TEST_SESSION_ID,
        "messageOrder": 1,
        "description": "测试检查点",
        "fileSnapshots": {}
    }
    response = requests.post(
        f"{BASE_URL}/checkpoints",
        json=checkpoint_data,
        headers=headers
    )
    print_result("创建手动检查点", response)
    checkpoint_id = None
    if response.status_code == 200:
        try:
            data = response.json()
            if data.get("data"):
                checkpoint_id = data["data"]
        except:
            pass
    
    # 3. 导出检查点
    if checkpoint_id:
        print(f"3. GET /checkpoints/{checkpoint_id}/export")
        response = requests.get(
            f"{BASE_URL}/checkpoints/{checkpoint_id}/export",
            headers=headers
        )
        print_result("导出检查点", response)
    
    # 4. 跳转到检查点
    if checkpoint_id:
        print(f"4. POST /checkpoints/{checkpoint_id}/jump")
        response = requests.post(
            f"{BASE_URL}/checkpoints/{checkpoint_id}/jump",
            headers=headers
        )
        print_result("跳转到检查点", response)

def test_approval_apis():
    """测试批准相关 API"""
    print_section("测试批准相关 API")
    
    headers = {"Authorization": f"Bearer {TEST_TOKEN}"} if TEST_TOKEN else {}
    
    # 1. 获取待批准列表
    print("1. GET /approvals/pending/{sessionId}")
    response = requests.get(
        f"{BASE_URL}/approvals/pending/{TEST_SESSION_ID}",
        headers=headers
    )
    print_result("获取待批准列表", response)
    
    # 2. 获取用户批准设置
    print("2. GET /approvals/settings")
    response = requests.get(
        f"{BASE_URL}/approvals/settings",
        headers=headers
    )
    print_result("获取用户批准设置", response)
    
    # 3. 更新用户批准设置
    print("3. PUT /approvals/settings")
    settings_data = {
        "autoApproveDangerousTools": False,
        "autoApproveReadFile": True,
        "autoApproveFileEdits": False,
        "autoApproveMcpTools": False,
        "includeToolLintErrors": True,
        "maxCheckpointsPerSession": 50
    }
    response = requests.put(
        f"{BASE_URL}/approvals/settings",
        json=settings_data,
        headers=headers
    )
    print_result("更新用户批准设置", response)
    
    # 4. 批量批准（如果有待批准项）
    print("4. POST /approvals/approve-all/{sessionId}")
    response = requests.post(
        f"{BASE_URL}/approvals/approve-all/{TEST_SESSION_ID}",
        headers=headers
    )
    print_result("批量批准", response)

def test_session_state_apis():
    """测试会话状态相关 API"""
    print_section("测试会话状态相关 API")
    
    headers = {"Authorization": f"Bearer {TEST_TOKEN}"} if TEST_TOKEN else {}
    
    # 1. 获取会话状态
    print("1. GET /state/{sessionId}")
    response = requests.get(
        f"{BASE_URL}/state/{TEST_SESSION_ID}",
        headers=headers
    )
    print_result("获取会话状态", response)
    
    # 2. 请求中断
    print("2. POST /state/{sessionId}/interrupt")
    response = requests.post(
        f"{BASE_URL}/state/{TEST_SESSION_ID}/interrupt",
        headers=headers
    )
    print_result("请求中断", response)
    
    # 3. 清除中断
    print("3. POST /state/{sessionId}/clear-interrupt")
    response = requests.post(
        f"{BASE_URL}/state/{TEST_SESSION_ID}/clear-interrupt",
        headers=headers
    )
    print_result("清除中断", response)

def main():
    global TEST_TOKEN
    
    print("="*60)
    print("  AISpring AI Terminal API 测试")
    print("="*60)
    print(f"\n测试配置:")
    print(f"  Base URL: {BASE_URL}")
    print(f"  Session ID: {TEST_SESSION_ID}")
    print(f"  User ID: {TEST_USER_ID}")
    
    # 提示：如果需要认证，先登录获取Token
    print("\n提示: 如果API需要认证，请先登录获取Token并更新 TEST_TOKEN")
    
    try:
        # 测试检查点 API
        test_checkpoint_apis()
        
        # 测试批准 API
        test_approval_apis()
        
        # 测试会话状态 API
        test_session_state_apis()
        
        print("\n" + "="*60)
        print("  测试完成！")
        print("="*60)
        
    except requests.exceptions.ConnectionError:
        print("\n❌ 错误: 无法连接到后端服务器")
        print("   请确保后端服务已启动在 http://localhost:5000")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()

