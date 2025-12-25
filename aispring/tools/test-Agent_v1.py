import os
import json
import re
import subprocess
from typing import List, Dict, Optional
from colorama import init, Fore, Style
from openai import OpenAI

init(autoreset=True)

# ==========================================
# 1. 提示词优化 (强化终端助手角色)
# ==========================================

SYSTEM_PROMPT_TEXT = """You are a Terminal Expert Assistant. 
Your workspace is the current directory. You can help users manage files, check system status, and run commands.

GLOBAL RULES:
1. Every action must be planned first.
2. If you need to know what's in a folder or run a script, use the 'run_command' tool.
3. NEVER assume a command succeeded without seeing the Observation.
"""

DEVELOPER_PROMPT_TEXT = """ROLE: Terminal Assistant
PROTOCOL: Plan -> Execute (XML) -> Verify Observation

AVAILABLE TOOLS:
<tools>
  <tool name="run_command">
    <description>Execute a shell command in the current workspace.</description>
    <input><command type="string" /></input>
  </tool>
</tools>
"""

# ==========================================
# 2. 工具与审批逻辑
# ==========================================

class ToolBox:
    @staticmethod
    def run_command(command: str) -> str:
        """在当前目录下执行真实的终端命令"""
        try:
            # 执行命令并捕获输出
            result = subprocess.run(
                command, shell=True, capture_output=True, text=True, timeout=30
            )
            output = result.stdout if result.stdout else result.stderr
            return f"OBSERVATION (Exit Code {result.returncode}):\n{output}"
        except Exception as e:
            return f"OBSERVATION: Execution failed. Error: {str(e)}"

# ==========================================
# 3. Agent 核心 (加入人工审核)
# ==========================================

class TerminalAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model
        self.toolbox = ToolBox()
        self.history: List[Dict] = []

    def _parse_tool(self, text: str):
        pattern = r"<tool_call>.*?<name>(.*?)</name>.*?<params>(.*?)</params>.*?</tool_call>"
        match = re.search(pattern, text, re.DOTALL | re.IGNORECASE)
        if match:
            try:
                return match.group(1).strip(), json.loads(match.group(2).strip())
            except: pass
        return None, None

    def chat(self, user_input: str):
        # 如果是首轮对话或新输入，加入历史
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
                model=self.model, messages=messages, temperature=0.1
            )
            answer = response.choices[0].message.content
            print(f"\n{Fore.GREEN}[Assistant]:\n{answer}")
            self.history.append({"role": "assistant", "content": answer})

            tool_name, tool_params = self._parse_tool(answer)
            
            if tool_name == "run_command":
                cmd = tool_params.get("command", "")
                
                # --- 人工审批流 (Human-in-the-loop) ---
                print(f"\n{Fore.RED}{Style.BRIGHT}⚠️  SECURITY CHECK")
                print(f"{Fore.YELLOW}AI wants to run: {Fore.WHITE}{cmd}")
                confirm = input(f"{Fore.CYAN}Allow this command? (y/n): ").lower().strip()
                
                if confirm == 'y':
                    obs = self.toolbox.run_command(cmd)
                else:
                    obs = "OBSERVATION: User REJECTED this command. DO NOT run it. Try another way or explain why you needed it."
                
                print(f"{Fore.MAGENTA}[System]: Result sent to AI.")
                self.history.append({"role": "user", "content": obs})
                continue 
            else:
                break

# ==========================================
# 4. 运行
# ==========================================

if __name__ == "__main__":
    # 配置你的 DeepSeek 或其他模型信息
    agent = TerminalAgent(
        api_key=os.getenv("DEEPSEEK_KEY", "sk-45021a61b84a4693a1db4deb72cec673"),
        base_url="https://api.deepseek.com",
        model="deepseek-chat"
    )

    print(f"{Fore.CYAN}--- Terminal Assistant Active ---")
    while True:
        prompt = input(f"\n{Fore.WHITE}You: ")
        if prompt.lower() in ['exit', 'quit']: break
        agent.chat(prompt)