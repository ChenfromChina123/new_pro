import os
import json
import re
from typing import List, Dict, Optional
from datetime import datetime
from colorama import init, Fore, Style
from openai import OpenAI

# 初始化终端颜色显示
init(autoreset=True)

# ==========================================
# 1. 核心提示词定义 (保持分层架构)
# ==========================================

SYSTEM_PROMPT_TEXT = """You are an autonomous AI Agent.

GLOBAL RULES:
1. You must follow the Developer execution protocol.
2. Tool results must appear ONLY in Observation.
3. Use XML format for tool calls: <tool_call><name>name</name><params>{"k":"v"}</params></tool_call>
"""

DEVELOPER_PROMPT_TEXT = """ROLE: Execution-Oriented AI Agent
PROTOCOL: STEP 1: Plan | STEP 2: Execute (XML) | STEP 3: Verify

TOOLS:
<tools>
  <tool name="read_file">
    <description>Read local file content.</description>
    <input><path type="string" /></input>
  </tool>
  <tool name="write_memory">
    <description>Save facts to memory.</description>
    <input><key type="string" /><value type="string" /></input>
  </tool>
</tools>
"""

# ==========================================
# 2. 运行时组件
# ==========================================

class AgentMemory:
    def __init__(self):
        self.state = {"derived_facts": {}, "current_goal": "Idle"}

    def update_fact(self, key: str, value: str):
        self.state["derived_facts"][key] = value
        print(f"{Fore.CYAN}[Memory Update] {key} -> {value}")

    def get_injection(self) -> str:
        return f"\nCURRENT MEMORY:\n{json.dumps(self.state)}\n"

class ToolBox:
    @staticmethod
    def read_file(path: str) -> str:
        # 这里模拟文件内容
        mock_files = {"config.txt": "API_KEY=DEEPSEEK-999-XYZ\nSTATUS=ACTIVE"}
        content = mock_files.get(path)
        if content:
            return f"OBSERVATION: File {path} content follows:\n{content}"
        return f"OBSERVATION: Error - File {path} not found."

# ==========================================
# 3. 增强型 Agent 类
# ==========================================

class EngineeringAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model
        self.memory = AgentMemory()
        self.toolbox = ToolBox()
        self.history: List[Dict] = []

    def _parse_tool(self, text: str):
        """增强版正则：处理换行、空格和大小写"""
        # 使用 re.DOTALL 允许点号匹配换行，忽略空格
        pattern = r"<tool_call>.*?<name>(.*?)</name>.*?<params>(.*?)</params>.*?</tool_call>"
        match = re.search(pattern, text, re.DOTALL | re.IGNORECASE)
        if match:
            name = match.group(1).strip()
            params_str = match.group(2).strip()
            try:
                return name, json.loads(params_str)
            except:
                return name, None
        return None, None

    def chat(self, user_input: str):
        self.history.append({"role": "user", "content": user_input})
        
        # 显式循环，直到没有工具调用为止
        while True:
            print(f"\n{Fore.WHITE}{Style.DIM}Agent is thinking...")
            
            messages = [
                {"role": "system", "content": SYSTEM_PROMPT_TEXT},
                {"role": "system", "content": DEVELOPER_PROMPT_TEXT},
                {"role": "system", "content": self.memory.get_injection()},
                *self.history
            ]

            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0
            )

            answer = response.choices[0].message.content
            print(f"\n{Fore.GREEN}[Assistant]:\n{answer}")
            self.history.append({"role": "assistant", "content": answer})

            # 解析并执行
            tool_name, tool_params = self._parse_tool(answer)
            
            if tool_name:
                print(f"{Fore.YELLOW}[System] Executing tool: {tool_name}...")
                
                if tool_name == "read_file":
                    obs = self.toolbox.read_file(tool_params.get("path", ""))
                elif tool_name == "write_memory":
                    self.memory.update_fact(tool_params.get("key"), tool_params.get("value"))
                    obs = "OBSERVATION: Fact successfully committed to memory."
                else:
                    obs = "OBSERVATION: Unknown tool."

                print(f"{Fore.MAGENTA}[Observation]: {obs}")
                # 将观察结果存入历史，继续循环
                self.history.append({"role": "user", "content": obs})
            else:
                # 没有检测到工具调用，说明 Agent 认为任务完成了
                print(f"\n{Fore.BLUE}Task Complete.")
                break

# ==========================================
# 4. 执行
# ==========================================

if __name__ == "__main__":
    # 配置
    # 注意：DeepSeek 的 URL 通常不需要 /v1，除非是通过代理
    api_key = "sk-45021a61b84a4693a1db4deb72cec673" 
    base_url = "https://api.deepseek.com" 
    
    agent = EngineeringAgent(api_key, base_url, "deepseek-chat")
    
    # 开启对话
    agent.chat("Read config.txt and save the API_KEY value to your memory.")