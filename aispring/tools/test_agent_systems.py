#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AISpring Agent系统测试脚本

测试内容：
1. Agent状态管理 API
2. Agent会话流式接口
3. 任务计划提交接口
4. 工具结果反馈接口
5. 批准系统接口
6. 检查点系统接口
"""

import requests
import json
import time
import uuid

class AgentSystemTester:
    def __init__(self, base_url="http://localhost:5000"):
        self.base_url = base_url
        self.token = None
        self.user_id = None
        self.session_id = None
        self.decision_id = None
        
    def login(self, email, password):
        """登录获取JWT令牌"""
        print("\n=== 登录 ===")
        url = f"{self.base_url}/api/auth/login"
        data = {"email": email, "password": password}
        
        try:
            response = requests.post(url, json=data)
            if response.status_code == 200:
                result = response.json()
                print(f"响应: {json.dumps(result, ensure_ascii=False, indent=2)}")
                # 检查响应结构
                if "data" in result:
                    self.token = result["data"].get("access_token")
                    self.user_id = result["data"].get("user_id")
                    print(f"✅ 登录成功，用户ID: {self.user_id}, Token: {self.token[:50]}...")
                    return True
                else:
                    print(f"❌ 响应中缺少data字段")
                    return False
            else:
                print(f"❌ 登录失败: {response.text}")
                return False
        except Exception as e:
            print(f"❌ 登录请求异常: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def get_headers(self):
        """获取请求头"""
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }
    
    def test_endpoint(self, name, method, url, data=None, params=None, expected_status=200):
        """测试单个API端点"""
        print(f"\n=== 测试 {name} ===")
        print(f"请求: {method} {url}")
        
        try:
            if method == "GET":
                response = requests.get(url, headers=self.get_headers(), params=params)
            elif method == "POST":
                response = requests.post(url, headers=self.get_headers(), json=data, params=params)
            elif method == "PUT":
                response = requests.put(url, headers=self.get_headers(), json=data, params=params)
            elif method == "DELETE":
                response = requests.delete(url, headers=self.get_headers(), params=params)
            else:
                print(f"❌ 不支持的请求方法: {method}")
                return None
            
            print(f"状态码: {response.status_code} (期望: {expected_status})")
            
            if response.status_code == expected_status:
                print(f"✅ {name} 测试通过")
                try:
                    result = response.json()
                    print(f"响应: {json.dumps(result, ensure_ascii=False, indent=2)[:300]}...")
                    return result
                except:
                    print(f"响应: {response.text[:300]}...")
                    return response.text
            else:
                print(f"❌ {name} 测试失败")
                print(f"响应: {response.text}")
                return None
        except Exception as e:
            print(f"❌ {name} 请求异常: {e}")
            return None

    def test_create_terminal_session(self):
        """创建终端会话"""
        print("\n" + "="*60)
        print("创建终端会话")
        print("="*60)
        
        result = self.test_endpoint(
            "创建终端会话",
            "POST",
            f"{self.base_url}/api/terminal/new-session"
        )
        
        if result and "data" in result and result["data"] is not None:
            self.session_id = result["data"].get("sessionId")
            if self.session_id:
                print(f"✅ 会话创建成功，Session ID: {self.session_id}")
            else:
                # 使用默认会话ID
                self.session_id = f"test-session-{uuid.uuid4().hex[:8]}"
                print(f"⚠️ 响应中缺少sessionId，使用默认会话ID: {self.session_id}")
        else:
            # 使用默认会话ID
            self.session_id = f"test-session-{uuid.uuid4().hex[:8]}"
            print(f"⚠️ 使用默认会话ID: {self.session_id}")
        
        return self.session_id

    def test_agent_state_api(self):
        """测试Agent状态API"""
        print("\n" + "="*60)
        print("测试Agent状态API")
        print("="*60)
        
        if not self.session_id:
            print("❌ 没有会话ID，跳过Agent状态测试")
            return
        
        # 获取会话状态
        result = self.test_endpoint(
            "获取会话状态",
            "GET",
            f"{self.base_url}/api/terminal/state/{self.session_id}"
        )
        
        if result and "data" in result:
            state = result["data"]
            print(f"\n📊 Agent状态详情:")
            print(f"  状态: {state.get('status')}")
            print(f"  Loop ID: {state.get('currentLoopId')}")
            print(f"  流式状态: {state.get('streamState')}")

    def test_agent_chat_stream(self):
        """测试Agent聊天流式接口"""
        print("\n" + "="*60)
        print("测试Agent聊天流式接口")
        print("="*60)
        
        if not self.session_id:
            print("❌ 没有会话ID，跳过聊天测试")
            return
        
        print("\n⚠️ 注意: 流式接口使用SSE，测试复杂，仅记录端点信息")
        print(f"端点: POST {self.base_url}/api/terminal/chat-stream")
        print(f"请求参数:")
        print(f"  prompt: 用户提示词")
        print(f"  session_id: {self.session_id}")
        print(f"  model: 模型名称")
        print(f"响应: SSE流式数据")

    def test_submit_plan(self):
        """测试提交任务计划"""
        print("\n" + "="*60)
        print("测试提交任务计划")
        print("="*60)
        
        if not self.session_id:
            print("❌ 没有会话ID，跳过计划提交测试")
            return
        
        # 示例任务计划JSON
        plan_json = json.dumps([
            {
                "id": "task-1",
                "name": "创建项目",
                "goal": "初始化一个新的Vue项目",
                "status": "pending",
                "substeps": [
                    {
                        "id": "sub-1.1",
                        "name": "初始化项目",
                        "type": "COMMAND",
                        "command": "npm init"
                    }
                ]
            }
        ])
        
        result = self.test_endpoint(
            "提交任务计划",
            "POST",
            f"{self.base_url}/api/terminal/submit-plan",
            data={
                "session_id": self.session_id,
                "plan_json": plan_json
            }
        )

    def test_tool_approval_apis(self):
        """测试工具批准API"""
        print("\n" + "="*60)
        print("测试工具批准API")
        print("="*60)
        
        if not self.session_id:
            print("❌ 没有会话ID，跳过批准API测试")
            return
        
        # 获取待批准列表
        result = self.test_endpoint(
            "获取待批准列表",
            "GET",
            f"{self.base_url}/api/terminal/approvals/pending/{self.session_id}"
        )
        
        # 获取用户批准设置
        result = self.test_endpoint(
            "获取用户批准设置",
            "GET",
            f"{self.base_url}/api/terminal/approvals/settings"
        )
        
        if result and "data" in result:
            settings = result["data"]
            print(f"\n📋 用户批准设置:")
            print(f"  自动批准危险工具: {settings.get('autoApproveDangerousTools')}")
            print(f"  自动批准文件读取: {settings.get('autoApproveReadFile')}")
            print(f"  自动批准文件编辑: {settings.get('autoApproveFileEdits')}")
        
        # 更新批准设置
        self.test_endpoint(
            "更新用户批准设置",
            "PUT",
            f"{self.base_url}/api/terminal/approvals/settings",
            data={
                "autoApproveDangerousTools": False,
                "autoApproveReadFile": True,
                "autoApproveFileEdits": False,
                "autoApproveMcpTools": False
            }
        )

    def test_checkpoint_apis(self):
        """测试检查点API"""
        print("\n" + "="*60)
        print("测试检查点API")
        print("="*60)
        
        if not self.session_id:
            print("❌ 没有会话ID，跳过检查点API测试")
            return
        
        # 获取会话的检查点
        result = self.test_endpoint(
            "获取会话检查点",
            "GET",
            f"{self.base_url}/api/terminal/checkpoints/{self.session_id}"
        )
        
        if result and "data" in result:
            checkpoints = result["data"]
            print(f"\n📌 检查点列表:")
            for cp in checkpoints:
                print(f"  - ID: {cp.get('id')}, 描述: {cp.get('description')}")
        
        # 创建手动检查点
        result = self.test_endpoint(
            "创建手动检查点",
            "POST",
            f"{self.base_url}/api/terminal/checkpoints",
            data={
                "sessionId": self.session_id,
                "messageOrder": 1,
                "description": "测试检查点",
                "fileSnapshots": {}
            }
        )
        
        checkpoint_id = None
        if result and "data" in result:
            checkpoint_id = result["data"]
            print(f"✅ 检查点创建成功，ID: {checkpoint_id}")
            
            # 导出检查点
            self.test_endpoint(
                "导出检查点",
                "GET",
                f"{self.base_url}/api/terminal/checkpoints/{checkpoint_id}/export"
            )

    def test_session_control_apis(self):
        """测试会话控制API"""
        print("\n" + "="*60)
        print("测试会话控制API")
        print("="*60)
        
        if not self.session_id:
            print("❌ 没有会话ID，跳过会话控制测试")
            return
        
        # 请求中断
        self.test_endpoint(
            "请求中断Agent循环",
            "POST",
            f"{self.base_url}/api/terminal/state/{self.session_id}/interrupt"
        )
        
        # 清除中断
        self.test_endpoint(
            "清除中断标志",
            "POST",
            f"{self.base_url}/api/terminal/state/{self.session_id}/clear-interrupt"
        )

    def test_terminal_basic_apis(self):
        """测试终端基础API"""
        print("\n" + "="*60)
        print("测试终端基础API")
        print("="*60)
        
        if not self.session_id:
            print("❌ 没有会话ID，跳过终端基础API测试")
            return
        
        # 获取会话列表
        result = self.test_endpoint(
            "获取终端会话列表",
            "GET",
            f"{self.base_url}/api/terminal/sessions"
        )
        
        # 获取会话历史
        result = self.test_endpoint(
            "获取会话历史",
            "GET",
            f"{self.base_url}/api/terminal/history/{self.session_id}"
        )
        
        # 获取文件列表
        result = self.test_endpoint(
            "获取文件列表",
            "GET",
            f"{self.base_url}/api/terminal/files",
            params={"path": ""}
        )

    def analyze_agent_architecture(self):
        """分析Agent架构"""
        print("\n" + "="*80)
        print("AISpring Agent系统架构分析")
        print("="*80)
        
        print("\n1. 核心实体类:")
        print("   - AgentState: Agent完整状态（会话ID、元数据、世界状态、任务状态等）")
        print("   - AgentMeta: Agent元数据（agentId、version、mode）")
        print("   - AgentStatus: Agent状态枚举")
        print("     * IDLE - 空闲")
        print("     * PLANNING - 规划中")
        print("     * RUNNING - 运行中")
        print("     * WAITING_TOOL - 等待工具执行")
        print("     * AWAITING_APPROVAL - 等待用户批准")
        print("     * PAUSED - 已暂停")
        print("     * COMPLETED - 已完成")
        print("     * ERROR - 错误")
        
        print("\n2. 世界状态 (WorldState):")
        print("   - projectRoot: 项目根目录")
        print("   - fileSystem: 文件系统快照")
        print("   - trackedPaths: 已追踪路径集合")
        print("   - services: 服务状态")
        
        print("\n3. 任务状态 (TaskState):")
        print("   - pipelineId: 流水线ID")
        print("   - currentTaskId: 当前任务ID")
        print("   - tasks: 任务列表")
        
        print("\n4. 任务 (Task):")
        print("   - id: 任务ID")
        print("   - name: 任务名称")
        print("   - goal: 任务目标")
        print("   - status: 任务状态 (PENDING/IN_PROGRESS/COMPLETED)")
        print("   - substeps: 子步骤列表")
        
        print("\n5. 决策信封 (DecisionEnvelope):")
        print("   - decisionId: 决策ID")
        print("   - type: 决策类型 (TASK_COMPLETE, TOOL_CALL等)")
        print("   - action: 动作/工具名称")
        print("   - params: 工具参数")
        print("   - reasoning: 决策原因")
        print("   - requiresApproval: 是否需要批准")
        
        print("\n6. 核心服务:")
        print("   - AgentStateService: Agent状态管理")
        print("   - AgentPromptBuilder: 构建Agent提示词上下文")
        print("   - TaskCompiler: 编译AI输出的任务JSON")
        print("   - StateMutator: 应用工具结果到Agent状态")
        print("   - ToolsService: 工具调用服务")
        print("   - TerminalService: 终端服务")
        
        print("\n7. Agent工作流程:")
        print("   1) 用户输入 -> 意图分类 (PLAN/EXECUTE/CHAT)")
        print("   2) PLAN模式 -> AI生成任务计划")
        print("   3) EXECUTE模式 -> AI生成决策信封")
        print("   4) 工具调用 -> 检查是否需要批准")
        print("   5) 执行工具 -> 返回结果")
        print("   6) 应用结果 -> 更新Agent状态")
        print("   7) 循环继续或完成任务")
        
        print("\n8. API端点:")
        print("   - POST /api/terminal/chat-stream: Agent流式聊天")
        print("   - POST /api/terminal/submit-plan: 提交任务计划")
        print("   - POST /api/terminal/report-tool-result: 报告工具结果")
        print("   - GET /api/terminal/state/{sessionId}: 获取会话状态")
        print("   - POST /api/terminal/state/{sessionId}/interrupt: 中断Agent")
        print("   - GET /api/terminal/approvals/pending/{sessionId}: 获取待批准列表")
        print("   - POST /api/terminal/approvals/{decisionId}/approve: 批准工具")
        print("   - POST /api/terminal/approvals/{decisionId}/reject: 拒绝工具")
        print("   - GET /api/terminal/checkpoints/{sessionId}: 获取检查点")
        print("   - POST /api/terminal/checkpoints: 创建检查点")
        print("   - POST /api/terminal/checkpoints/{checkpointId}/jump: 跳转到检查点")

    def run_all_tests(self):
        """运行所有测试"""
        print("🚀 AISpring Agent系统测试")
        print(f"测试时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 1. 登录
        if not self.login("3301767269@qq.com", "123456"):
            print("\n❌ 登录失败，无法继续测试")
            return
        
        # 2. 架构分析
        self.analyze_agent_architecture()
        
        # 3. 创建会话
        self.test_create_terminal_session()
        
        # 4. 测试Agent状态API
        self.test_agent_state_api()
        
        # 5. 测试Agent聊天流式接口
        self.test_agent_chat_stream()
        
        # 6. 测试提交任务计划
        self.test_submit_plan()
        
        # 7. 测试工具批准API
        self.test_tool_approval_apis()
        
        # 8. 测试检查点API
        self.test_checkpoint_apis()
        
        # 9. 测试会话控制API
        self.test_session_control_apis()
        
        # 10. 测试终端基础API
        self.test_terminal_basic_apis()
        
        print("\n" + "="*80)
        print("🎉 Agent系统测试完成")
        print("="*80)

if __name__ == "__main__":
    tester = AgentSystemTester()
    tester.run_all_tests()
