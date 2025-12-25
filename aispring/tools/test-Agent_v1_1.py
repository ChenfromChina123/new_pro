import os
import json
import re
import subprocess
from typing import List, Dict, Optional
from colorama import init, Fore, Style
from openai import OpenAI

# 初始化颜色
init(autoreset=True)

# ==========================================
# 1. 提示词定义 (增加了严格的示例说明)
# ==========================================

SYSTEM_PROMPT_TEXT = """You are a Terminal Expert Assistant.
Your workspace is the current directory.

GLOBAL RULES:
1. Every tool call MUST follow the <tool_call> XML format.
2. Tool results appear ONLY in Observation.

EXAMPLE OF CORRECT TOOL CALL:
I will list the files in the directory.
<tool_call>
<name>run_command</name>
<params>{"command": "ls"}</params>
</tool_call>
"""

DEVELOPER_PROMPT_TEXT = """ROLE: Terminal Assistant
PROTOCOL: Plan -> Execute (XML) -> Verify

AVAILABLE TOOLS:
<tools>
  <tool name="run_command">
    <description>Execute a shell command.</description>
    <input>{"command": "string"}</input>
  </tool>
</tools>
"""

# ==========================================
# 2. 工具箱
# ==========================================

class ToolBox:
    @staticmethod
    def run_command(command: str) -> str:
        try:
            # 兼容 Windows 的命令处理 (例如 ls 转 dir)
            if os.name == 'nt' and command == 'ls':
                command = 'dir'
            
            result = subprocess.run(
                command, shell=True, capture_output=True, text=True, timeout=15
            )
            output = result.stdout if result.stdout else result.stderr
            return f"OBSERVATION (Exit Code {result.returncode}):\n{output}"
        except Exception as e:
            return f"OBSERVATION: Execution failed. Error: {str(e)}"

# ==========================================
# 3. Agent 核心 (带鲁棒性解析)
# ==========================================

class TerminalAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model
        self.toolbox = ToolBox()
        self.history: List[Dict] = []

    def _parse_tool(self, text: str):
        """增强版解析器：支持标准格式和模型可能生成的简化格式"""
        
        # 1. 匹配标准格式 <tool_call><name>...</name><params>...</params></tool_call>
        std_pattern = r"<tool_call>.*?<name>(.*?)</name>.*?<params>(.*?)</params>.*?</tool_call>"
        std_match = re.search(std_pattern, text, re.DOTALL | re.IGNORECASE)
        if std_match:
            try:
                name = std_match.group(1).strip()
                params = json.loads(std_match.group(2).strip())
                return name, params
            except: pass

        # 2. 保底匹配：如果模型直接写了 <run_command><command>...</command></run_command>
        simple_pattern = r"<run_command>.*?<command>(.*?)</command>.*?</run_command>"
        simple_match = re.search(simple_pattern, text, re.DOTALL | re.IGNORECASE)
        if simple_match:
            return "run_command", {"command": simple_match.group(1).strip()}

        return None, None

    def chat(self, user_input: str):
        if user_input:
            self.history.append({"role": "user", "content": user_input})
        
        while True:
            print(f"\n{Style.DIM}AI is analyzing...")
            
            messages = [
                {"role": "system", "content": SYSTEM_PROMPT_TEXT},
                {"role": "system", "content": DEVELOPER_PROMPT_TEXT},
                *self.history
            ]

            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.1
            )

            answer = response.choices[0].message.content
            print(f"\n{Fore.GREEN}[Assistant]:\n{answer}")
            self.history.append({"role": "assistant", "content": answer})

            # 解析解析
            tool_name, tool_params = self._parse_tool(answer)
            
            if tool_name == "run_command":
                cmd = tool_params.get("command", "")
                
                # --- 审批流 ---
                print(f"\n{Fore.RED}{Style.BRIGHT}⚠️  SECURITY CHECK")
                print(f"{Fore.YELLOW}AI wants to run: {Fore.WHITE}{cmd}")
                confirm = input(f"{Fore.CYAN}Allow this command? (y/n/exit): ").lower().strip()
                
                if confirm == 'y':
                    obs = self.toolbox.run_command(cmd)
                    print(f"{Fore.MAGENTA}[System]: Execution successful.")
                elif confirm == 'exit':
                    print("Exiting...")
                    exit()
                else:
                    obs = "OBSERVATION: User REJECTED this command. Suggest an alternative or explain why it was needed."
                    print(f"{Fore.YELLOW}[System]: Command rejected by user.")
                
                self.history.append({"role": "user", "content": obs})
                # 继续循环让 AI 处理 Observation
            else:
                # 没有工具调用，跳出循环等待用户输入
                break

# ==========================================
# 4. 程序入口
# ==========================================

if __name__ == "__main__":
    # 请确保环境变量已设置，或在此处填入真实 Key
    api_key = os.getenv("DEEPSEEK_KEY", "sk-45021a61b84a4693a1db4deb72cec673") 
    base_url = "https://api.deepseek.com"
    
    agent = TerminalAgent(api_key, base_url, "deepseek-chat")

    print(f"{Fore.CYAN}=== Terminal Agent v2 (With Auto-Correction) ===")
    print(f"{Fore.WHITE}Type 'exit' to quit.")

    while True:
        try:
            user_msg = input(f"\n{Fore.WHITE}You: ")
            if user_msg.lower() in ['exit', 'quit']:
                break
            agent.chat(user_msg)
        except KeyboardInterrupt:
            break