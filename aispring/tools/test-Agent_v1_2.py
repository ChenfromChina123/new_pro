import os
import json
import re
import subprocess
from datetime import datetime
from typing import List, Dict, Optional
from colorama import init, Fore, Style
from openai import OpenAI

init(autoreset=True)

# ==========================================
# 1. 强化版提示词定义
# ==========================================

SYSTEM_PROMPT_TEXT = """You are an Autonomous Terminal & Research Expert.
WORKSPACE: Current directory.

GLOBAL RULES:
1. Always PLAN before acting.
2. Use 'web_search' to get real-time info or verify facts.
3. Use 'read_file' to understand file contents in the workspace.
4. Tool format MUST be: <tool_call><name>name</name><params>{"k":"v"}</params></tool_call>
"""

DEVELOPER_PROMPT_TEXT = """ROLE: Terminal & Research Assistant
PROTOCOL: Plan -> Execute (XML) -> Observation -> Verify

AVAILABLE TOOLS:
<tools>
  <tool name="run_command"><description>Execute shell command (Requires user approval).</description></tool>
  <tool name="web_search"><description>Search the internet for real-time info.</description></tool>
  <tool name="read_file"><description>Read local file content (Auto-exec).</description></tool>
</tools>
"""

# ==========================================
# 2. 增强工具箱与日志记录器
# ==========================================

class Logger:
    """负责记录完整的 AI 思考路径和上下文"""
    def __init__(self, filename="agent_trace.log"):
        self.filename = filename

    def log_step(self, role: str, content: str):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with open(self.filename, "a", encoding="utf-8") as f:
            f.write(f"\n{'='*20} {timestamp} | {role.upper()} {'='*20}\n")
            f.write(content + "\n")

class ToolBox:
    @staticmethod
    def run_command(command: str) -> str:
        try:
            if os.name == 'nt' and command == 'ls': command = 'dir'
            result = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=15)
            return f"OBSERVATION (Code {result.returncode}):\n{result.stdout}{result.stderr}"
        except Exception as e:
            return f"OBSERVATION: Error: {str(e)}"

    @staticmethod
    def read_file(path: str) -> str:
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return f"OBSERVATION (File: {path}):\n{f.read()}"
        except Exception as e:
            return f"OBSERVATION: Cannot read {path}. Error: {str(e)}"

    @staticmethod
    def web_search(query: str) -> str:
        # 这里模拟搜索结果，实际可接入 Tavily 或 Google Search API
        print(f"{Fore.BLUE}[Searching Web] Query: {query}...")
        return f"OBSERVATION (Web Search Result): Found real-time data about '{query}'. Current status: Updated at 2025."

# ==========================================
# 3. 核心 Agent 类
# ==========================================

class UltimateAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model
        self.toolbox = ToolBox()
        self.logger = Logger()
        self.history: List[Dict] = []

    def _parse_tool(self, text: str):
        # 强大的多模式正则匹配
        patterns = [
            r"<tool_call>.*?<name>(.*?)</name>.*?<params>(.*?)</params>.*?</tool_call>",
            r"<(run_command|web_search|read_file)>.*?<(?:command|query|path)>(.*?)</(?:command|query|path)>.*?</\1>"
        ]
        
        for p in patterns:
            match = re.search(p, text, re.DOTALL | re.IGNORECASE)
            if match:
                if "params" in p: # 标准格式
                    try: return match.group(1).strip(), json.loads(match.group(2).strip())
                    except: continue
                else: # 简易格式保底
                    tool_name = match.group(1).strip()
                    param_val = match.group(2).strip()
                    param_key = "command" if "command" in p else ("query" if "query" in p else "path")
                    return tool_name, {param_key: param_val}
        return None, None

    def chat(self, user_input: str):
        if user_input:
            self.history.append({"role": "user", "content": user_input})
            self.logger.log_step("User", user_input)
        
        while True:
            print(f"\n{Style.DIM}Agent Thinking...")
            
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
            self.logger.log_step("Assistant", answer)

            tool_name, tool_params = self._parse_tool(answer)
            
            if tool_name:
                obs = ""
                # --- 分支 1: 需要批准的工具 ---
                if tool_name == "run_command":
                    cmd = tool_params.get("command", "")
                    print(f"\n{Fore.RED}⚠️  SECURITY CHECK: AI wants to run [{cmd}]")
                    if input(f"{Fore.CYAN}Approve? (y/n): ").lower() == 'y':
                        obs = self.toolbox.run_command(cmd)
                    else:
                        obs = "OBSERVATION: User denied command execution."

                # --- 分支 2: 自动执行的工具 (Search & Read) ---
                elif tool_name == "web_search":
                    obs = self.toolbox.web_search(tool_params.get("query", ""))
                
                elif tool_name == "read_file":
                    obs = self.toolbox.read_file(tool_params.get("path", ""))

                if obs:
                    print(f"{Fore.MAGENTA}[System]: Result updated to context.")
                    self.history.append({"role": "user", "content": obs})
                    self.logger.log_step("Observation", obs)
                    continue 
            else:
                break

# ==========================================
# 4. 执行
# ==========================================

if __name__ == "__main__":
    agent = UltimateAgent(
        api_key=os.getenv("DEEPSEEK_API_KEY", "sk-45021a61b84a4693a1db4deb72cec673"),
        base_url="https://api.deepseek.com",
        model="deepseek-chat"
    )

    print(f"{Fore.CYAN}--- Ultimate Agent v3 (Search/Read/Log) Active ---")

    while True:
        try:
            user_msg = input(f"\n{Fore.WHITE}You: ")
            if user_msg.lower() in ['exit', 'quit']: break
            agent.chat(user_msg)
        except KeyboardInterrupt: break