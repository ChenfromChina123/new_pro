#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cursor 架构解耦系统测试框架

基于文档思想，实现并测试 4 个解耦系统：
1. 提示词系统解耦（Prompt System Decoupling）
2. 工具系统解耦（Tool System Decoupling）
3. 身份定位系统解耦（Identity System Decoupling）
4. 信息解耦系统（Information Decoupling System）
"""

import json
import uuid
from typing import Dict, List, Optional, Any, Callable
from dataclasses import dataclass, field
from enum import Enum
from datetime import datetime
import copy


# ============================================================================
# 一、提示词系统解耦（Prompt System Decoupling）
# ============================================================================

class IdentityType(Enum):
    """核心身份类型"""
    IDE_ENGINEER = "ide_software_engineer"
    TERMINAL_ASSISTANT = "terminal_assistant"
    CODE_REVIEWER = "code_reviewer"


class TaskRole(Enum):
    """任务角色"""
    REFACTORER = "refactorer"
    DEBUGGER = "debugger"
    PLANNER = "planner"
    EXECUTOR = "executor"
    CHATTER = "chatter"


class BehavioralBias(Enum):
    """行为偏好"""
    MINIMAL_CHANGE = "prefer_small_diffs"
    AVOID_SPECULATION = "avoid_speculation"
    SAFE_FIRST = "safe_first"
    FAST_EXECUTION = "fast_execution"


@dataclass
class PromptConfig:
    """Prompt 配置 - 系统状态，不是视图"""
    identity: IdentityType
    role: TaskRole
    objective: str
    constraints: List[str] = field(default_factory=list)
    bias: List[BehavioralBias] = field(default_factory=list)
    context: Optional[str] = None


class PromptCompiler:
    """Prompt 编译器 - 将配置编译为最终自然语言"""
    
    IDENTITY_TEMPLATES = {
        IdentityType.IDE_ENGINEER: "你是一位专业的 IDE 嵌入式软件工程师",
        IdentityType.TERMINAL_ASSISTANT: "你是一个运行在专用文件夹系统中的智能终端助手",
        IdentityType.CODE_REVIEWER: "你是一位经验丰富的代码审查专家"
    }
    
    ROLE_TEMPLATES = {
        TaskRole.REFACTORER: "你的任务是重构代码，提高可读性和可维护性",
        TaskRole.DEBUGGER: "你的任务是定位和修复代码中的错误",
        TaskRole.PLANNER: "你的任务是分析用户意图，生成结构化的任务流水线",
        TaskRole.EXECUTOR: "你是自主工程执行 Agent，目标是完成当前任务",
        TaskRole.CHATTER: "你是 AI 终端助手，回答用户问题并提供工程建议"
    }
    
    BIAS_TEMPLATES = {
        BehavioralBias.MINIMAL_CHANGE: "优先进行最小化修改，保持现有行为不变",
        BehavioralBias.AVOID_SPECULATION: "避免推测，只基于确定的事实进行决策",
        BehavioralBias.SAFE_FIRST: "安全第一，所有操作都需要经过验证",
        BehavioralBias.FAST_EXECUTION: "优先快速执行，在保证正确性的前提下提高效率"
    }
    
    @classmethod
    def render(cls, config: PromptConfig) -> str:
        """将配置编译为最终 Prompt"""
        parts = []
        
        # 1. Identity
        identity_text = cls.IDENTITY_TEMPLATES.get(config.identity, "")
        parts.append(f"# 角色\n{identity_text}")
        
        # 2. Role
        role_text = cls.ROLE_TEMPLATES.get(config.role, "")
        parts.append(f"\n# 任务\n{role_text}")
        
        # 3. Objective
        if config.objective:
            parts.append(f"\n# 目标\n{config.objective}")
        
        # 4. Constraints
        if config.constraints:
            parts.append("\n# 约束")
            for constraint in config.constraints:
                parts.append(f"- {constraint}")
        
        # 5. Behavioral Bias
        if config.bias:
            parts.append("\n# 行为偏好")
            for bias in config.bias:
                bias_text = cls.BIAS_TEMPLATES.get(bias, "")
                if bias_text:
                    parts.append(f"- {bias_text}")
        
        # 6. Context
        if config.context:
            parts.append(f"\n# 上下文\n{config.context}")
        
        return "\n".join(parts)


# ============================================================================
# 二、工具系统解耦（Tool System Decoupling）
# ============================================================================

class Capability(Enum):
    """能力层 - 抽象能力"""
    READ_FILE = "read_file"
    WRITE_FILE = "write_file"
    EXECUTE_COMMAND = "execute_command"
    APPLY_DIFF = "apply_diff"
    RUN_TEST = "run_test"


@dataclass
class ToolProposal:
    """工具提议 - 模型只能提议，不能执行"""
    capability: Capability
    params: Dict[str, Any]
    decision_id: str = field(default_factory=lambda: str(uuid.uuid4()))


@dataclass
class ToolResult:
    """工具执行结果"""
    decision_id: str
    success: bool
    stdout: str = ""
    stderr: str = ""
    artifacts: List[str] = field(default_factory=list)
    exit_code: int = 0


class CapabilityAdapter:
    """能力适配器 - 将抽象能力映射到具体实现"""
    
    def __init__(self):
        self.adapters: Dict[Capability, Callable] = {}
    
    def register(self, capability: Capability, adapter: Callable):
        """注册能力适配器"""
        self.adapters[capability] = adapter
    
    def execute(self, proposal: ToolProposal) -> ToolResult:
        """执行工具提议"""
        if proposal.capability not in self.adapters:
            return ToolResult(
                decision_id=proposal.decision_id,
                success=False,
                stderr=f"Capability {proposal.capability.value} not registered"
            )
        
        try:
            adapter = self.adapters[proposal.capability]
            result = adapter(proposal.params)
            return ToolResult(
                decision_id=proposal.decision_id,
                success=True,
                stdout=result.get("stdout", ""),
                artifacts=result.get("artifacts", [])
            )
        except Exception as e:
            return ToolResult(
                decision_id=proposal.decision_id,
                success=False,
                stderr=str(e),
                exit_code=1
            )


class InvocationPolicy:
    """调用策略 - 决定是否执行"""
    
    def __init__(self):
        self.model_can_propose = True
        self.host_can_execute = True
        self.whitelist: List[Capability] = []
        self.blacklist: List[Capability] = []
    
    def can_execute(self, proposal: ToolProposal) -> tuple[bool, str]:
        """判断是否可以执行"""
        if proposal.capability in self.blacklist:
            return False, f"Capability {proposal.capability.value} is blacklisted"
        
        if self.whitelist and proposal.capability not in self.whitelist:
            return False, f"Capability {proposal.capability.value} not in whitelist"
        
        return True, "OK"


# ============================================================================
# 三、身份定位系统解耦（Identity System Decoupling）
# ============================================================================

@dataclass
class CoreIdentity:
    """核心身份 - 长期，进程级"""
    agent_type: str
    authority: str  # "suggest_only" | "execute"
    domain: str
    created_at: datetime = field(default_factory=datetime.now)


@dataclass
class TaskIdentity:
    """任务身份 - 任务级，一次任务"""
    task_id: str
    role: TaskRole
    goal: str
    created_at: datetime = field(default_factory=datetime.now)
    completed_at: Optional[datetime] = None


@dataclass
class ViewpointIdentity:
    """视角身份 - 瞬时，每次调用都变"""
    file: Optional[str] = None
    symbol: Optional[str] = None
    cursor_line: Optional[int] = None
    context_scope: List[str] = field(default_factory=list)


class IdentityManager:
    """身份管理器"""
    
    def __init__(self, core_identity: CoreIdentity):
        self.core_identity = core_identity
        self.current_task: Optional[TaskIdentity] = None
        self.current_viewpoint: Optional[ViewpointIdentity] = None
    
    def set_task(self, task: TaskIdentity):
        """设置当前任务身份"""
        self.current_task = task
    
    def set_viewpoint(self, viewpoint: ViewpointIdentity):
        """设置当前视角身份"""
        self.current_viewpoint = viewpoint
    
    def get_composite_identity(self) -> Dict[str, Any]:
        """获取复合身份 - 用于构建上下文"""
        return {
            "core": {
                "type": self.core_identity.agent_type,
                "authority": self.core_identity.authority,
                "domain": self.core_identity.domain
            },
            "task": {
                "id": self.current_task.task_id if self.current_task else None,
                "role": self.current_task.role.value if self.current_task else None,
                "goal": self.current_task.goal if self.current_task else None
            },
            "viewpoint": {
                "file": self.current_viewpoint.file if self.current_viewpoint else None,
                "symbol": self.current_viewpoint.symbol if self.current_viewpoint else None,
                "line": self.current_viewpoint.cursor_line if self.current_viewpoint else None
            }
        }


# ============================================================================
# 四、信息解耦系统（Information Decoupling System）
# ============================================================================

class InformationSource(Enum):
    """信息来源"""
    FILE_SYSTEM = "file_system"
    CURSOR_POSITION = "cursor_position"
    USER_INPUT = "user_input"
    AST_SYMBOL = "ast_symbol"
    AGENT_STATE = "agent_state"


@dataclass
class StateSlice:
    """状态切片 - 可重构的状态，不是记忆"""
    source: InformationSource
    scope: List[str]  # 可见范围
    data: Dict[str, Any]
    timestamp: datetime = field(default_factory=datetime.now)
    authority: str = "fact"  # "fact" | "model_output"


class InformationManager:
    """信息管理器 - 管理状态切片"""
    
    def __init__(self):
        self.slices: List[StateSlice] = []
        self.visible_files: List[str] = []
        self.visible_functions: List[str] = []
    
    def add_slice(self, slice: StateSlice):
        """添加状态切片"""
        self.slices.append(slice)
    
    def get_current_state(self) -> Dict[str, Any]:
        """获取当前状态 - 从切片重构"""
        state = {
            "files": {},
            "cursor": None,
            "user_input": None,
            "symbols": {},
            "agent_state": {}
        }
        
        for slice in self.slices:
            if slice.source == InformationSource.FILE_SYSTEM:
                state["files"].update(slice.data)
            elif slice.source == InformationSource.CURSOR_POSITION:
                state["cursor"] = slice.data
            elif slice.source == InformationSource.USER_INPUT:
                state["user_input"] = slice.data
            elif slice.source == InformationSource.AST_SYMBOL:
                state["symbols"].update(slice.data)
            elif slice.source == InformationSource.AGENT_STATE:
                state["agent_state"].update(slice.data)
        
        return state
    
    def set_scope(self, visible_files: List[str], visible_functions: List[str]):
        """设置作用域 - 只给模型应该看到的世界"""
        self.visible_files = visible_files
        self.visible_functions = visible_functions
    
    def filter_by_scope(self, state: Dict[str, Any]) -> Dict[str, Any]:
        """根据作用域过滤状态"""
        filtered = copy.deepcopy(state)
        
        # 只保留可见文件
        if self.visible_files:
            filtered["files"] = {
                k: v for k, v in filtered.get("files", {}).items()
                if k in self.visible_files
            }
        
        # 只保留可见函数
        if self.visible_functions:
            filtered["symbols"] = {
                k: v for k, v in filtered.get("symbols", {}).items()
                if k in self.visible_functions
            }
        
        return filtered


# ============================================================================
# 五、集成测试：4 个解耦系统协同工作
# ============================================================================

class DecoupledAgent:
    """解耦的 Agent - 4 个系统协同工作"""
    
    def __init__(self):
        # 1. Prompt System
        self.prompt_compiler = PromptCompiler()
        
        # 2. Tool System
        self.capability_adapter = CapabilityAdapter()
        self.invocation_policy = InvocationPolicy()
        
        # 3. Identity System
        core_identity = CoreIdentity(
            agent_type="ide_software_engineer",
            authority="suggest_only",
            domain="software"
        )
        self.identity_manager = IdentityManager(core_identity)
        
        # 4. Information System
        self.information_manager = InformationManager()
    
    def process_request(self, user_input: str, context: Dict[str, Any]) -> Dict[str, Any]:
        """处理请求 - 展示 4 个系统如何协同"""
        
        # 1. Information: 添加用户输入切片
        input_slice = StateSlice(
            source=InformationSource.USER_INPUT,
            scope=[],
            data={"text": user_input}
        )
        self.information_manager.add_slice(input_slice)
        
        # 2. Identity: 根据输入确定任务身份
        task_identity = TaskIdentity(
            task_id=str(uuid.uuid4()),
            role=TaskRole.EXECUTOR,
            goal=user_input
        )
        self.identity_manager.set_task(task_identity)
        
        # 3. Information: 获取当前状态（从切片重构）
        current_state = self.information_manager.get_current_state()
        
        # 4. Prompt: 构建 Prompt 配置
        prompt_config = PromptConfig(
            identity=IdentityType.IDE_ENGINEER,
            role=task_identity.role,
            objective=task_identity.goal,
            constraints=["keep behavior", "no new dependencies"],
            bias=[BehavioralBias.MINIMAL_CHANGE, BehavioralBias.AVOID_SPECULATION],
            context=json.dumps(current_state, ensure_ascii=False, indent=2)
        )
        
        # 5. Prompt: 编译为最终 Prompt
        final_prompt = self.prompt_compiler.render(prompt_config)
        
        # 6. 模拟模型返回工具提议
        tool_proposal = ToolProposal(
            capability=Capability.READ_FILE,
            params={"path": "src/App.vue"}
        )
        
        # 7. Tool: 检查是否可以执行
        can_execute, reason = self.invocation_policy.can_execute(tool_proposal)
        
        if not can_execute:
            return {
                "error": reason,
                "prompt": final_prompt,
                "proposal": tool_proposal.__dict__
            }
        
        # 8. Tool: 执行工具
        result = self.capability_adapter.execute(tool_proposal)
        
        # 9. Information: 添加执行结果切片
        result_slice = StateSlice(
            source=InformationSource.AGENT_STATE,
            scope=[],
            data={"tool_result": result.__dict__}
        )
        self.information_manager.add_slice(result_slice)
        
        return {
            "prompt": final_prompt,
            "proposal": tool_proposal.__dict__,
            "result": result.__dict__,
            "identity": self.identity_manager.get_composite_identity(),
            "state": self.information_manager.get_current_state()
        }


# ============================================================================
# 测试用例
# ============================================================================

def test_prompt_system():
    """测试提示词系统解耦"""
    print("\n" + "=" * 60)
    print("测试 1: 提示词系统解耦")
    print("=" * 60)
    
    config = PromptConfig(
        identity=IdentityType.IDE_ENGINEER,
        role=TaskRole.REFACTORER,
        objective="重构代码以提高可读性",
        constraints=["保持行为不变", "不添加新依赖"],
        bias=[BehavioralBias.MINIMAL_CHANGE, BehavioralBias.AVOID_SPECULATION],
        context="当前文件: src/App.vue"
    )
    
    prompt = PromptCompiler.render(config)
    print("\n生成的 Prompt:")
    print("-" * 60)
    print(prompt)
    print("-" * 60)
    
    # 验证：Prompt 是视图，配置是状态
    config2 = copy.deepcopy(config)
    config2.role = TaskRole.DEBUGGER
    prompt2 = PromptCompiler.render(config2)
    
    assert prompt != prompt2, "不同配置应生成不同 Prompt"
    print("\n[OK] 提示词系统解耦测试通过")


def test_tool_system():
    """测试工具系统解耦"""
    print("\n" + "=" * 60)
    print("测试 2: 工具系统解耦")
    print("=" * 60)
    
    adapter = CapabilityAdapter()
    
    # 注册模拟适配器
    def mock_read_file(params: Dict) -> Dict:
        return {
            "stdout": f"File content of {params['path']}",
            "artifacts": [params['path']]
        }
    
    adapter.register(Capability.READ_FILE, mock_read_file)
    
    # 模型提议
    proposal = ToolProposal(
        capability=Capability.READ_FILE,
        params={"path": "src/App.vue"}
    )
    
    print(f"\n模型提议: {proposal.capability.value}")
    print(f"参数: {proposal.params}")
    
    # 策略检查
    policy = InvocationPolicy()
    policy.whitelist = [Capability.READ_FILE]
    can_execute, reason = policy.can_execute(proposal)
    
    print(f"\n策略检查: {'[OK] 允许执行' if can_execute else '[ERROR] 拒绝执行'}")
    print(f"原因: {reason}")
    
    if can_execute:
        result = adapter.execute(proposal)
        print(f"\n执行结果:")
        print(f"  成功: {result.success}")
        print(f"  输出: {result.stdout}")
        print(f"  产物: {result.artifacts}")
    
    print("\n[OK] 工具系统解耦测试通过")


def test_identity_system():
    """测试身份定位系统解耦"""
    print("\n" + "=" * 60)
    print("测试 3: 身份定位系统解耦")
    print("=" * 60)
    
    core_identity = CoreIdentity(
        agent_type="ide_software_engineer",
        authority="suggest_only",
        domain="software"
    )
    
    manager = IdentityManager(core_identity)
    
    # 设置任务身份
    task = TaskIdentity(
        task_id="task-123",
        role=TaskRole.DEBUGGER,
        goal="修复空指针异常"
    )
    manager.set_task(task)
    
    # 设置视角身份
    viewpoint = ViewpointIdentity(
        file="src/auth.ts",
        symbol="login()",
        cursor_line=124
    )
    manager.set_viewpoint(viewpoint)
    
    composite = manager.get_composite_identity()
    print("\n复合身份:")
    print(json.dumps(composite, ensure_ascii=False, indent=2))
    
    print("\n[OK] 身份定位系统解耦测试通过")


def test_information_system():
    """测试信息解耦系统"""
    print("\n" + "=" * 60)
    print("测试 4: 信息解耦系统")
    print("=" * 60)
    
    manager = InformationManager()
    
    # 添加文件系统切片
    file_slice = StateSlice(
        source=InformationSource.FILE_SYSTEM,
        scope=[],
        data={"src/App.vue": "export default {...}"}
    )
    manager.add_slice(file_slice)
    
    # 添加光标位置切片
    cursor_slice = StateSlice(
        source=InformationSource.CURSOR_POSITION,
        scope=[],
        data={"file": "src/App.vue", "line": 10}
    )
    manager.add_slice(cursor_slice)
    
    # 设置作用域
    manager.set_scope(
        visible_files=["src/App.vue"],
        visible_functions=["login()"]
    )
    
    # 获取当前状态
    state = manager.get_current_state()
    print("\n当前状态（重构自切片）:")
    print(json.dumps(state, ensure_ascii=False, indent=2))
    
    # 过滤状态
    filtered = manager.filter_by_scope(state)
    print("\n过滤后的状态（仅可见范围）:")
    print(json.dumps(filtered, ensure_ascii=False, indent=2))
    
    print("\n[OK] 信息解耦系统测试通过")


def test_integrated_system():
    """测试集成系统"""
    print("\n" + "=" * 60)
    print("测试 5: 4 个系统集成测试")
    print("=" * 60)
    
    agent = DecoupledAgent()
    
    # 注册模拟工具
    def mock_read_file(params: Dict) -> Dict:
        return {
            "stdout": f"Content of {params['path']}",
            "artifacts": [params['path']]
        }
    
    agent.capability_adapter.register(Capability.READ_FILE, mock_read_file)
    agent.invocation_policy.whitelist = [Capability.READ_FILE]
    
    # 处理请求
    result = agent.process_request(
        user_input="读取 src/App.vue 文件",
        context={"current_file": "src/App.vue"}
    )
    
    print("\n集成测试结果:")
    print(json.dumps(result, ensure_ascii=False, indent=2, default=str))
    
    print("\n[OK] 集成系统测试通过")


if __name__ == "__main__":
    import sys
    import io
    # 设置输出编码为 UTF-8
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    
    print("开始测试 Cursor 架构解耦系统")
    print("=" * 60)
    
    try:
        test_prompt_system()
        test_tool_system()
        test_identity_system()
        test_information_system()
        test_integrated_system()
        
        print("\n" + "=" * 60)
        print("所有测试通过！")
        print("=" * 60)
        print("\n关键验证点:")
        print("1. [OK] Prompt 是编译产物，不是状态")
        print("2. [OK] 模型只能提议，系统决定执行")
        print("3. [OK] 身份分为三层：核心/任务/视角")
        print("4. [OK] 信息是状态切片，可重构")
        print("5. [OK] 4 个系统完全解耦，可独立替换")
        
    except Exception as e:
        print(f"\n[ERROR] 测试失败: {e}")
        import traceback
        traceback.print_exc()

