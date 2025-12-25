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
# 1. 提示词：明确写入规范
# ==========================================

SYSTEM_PROMPT_TEXT = """You are an Autonomous Terminal Expert.
WORKSPACE: Current directory.

RULES:
1. To CREATE or UPDATE files, ALWAYS use 'write_file'. Do NOT use 'echo' via 'run_command'.
2. 'write_file' and 'read_file' are auto-executed. 'run_command' requires approval.
3. Use 'web_search' for real-time info.
"""

DEVELOPER_PROMPT_TEXT = """ROLE: Terminal & Research Assistant
TOOLS:
<tools>
  <tool name="write_file">
    <description>Write content to a file directly. Use this instead of echo.</description>
    <input>{"path": "string", "content": "string"}</input>
  </tool>
  <tool name="run_command"><description>Execute shell command (Requires approval).</description></tool>
  <tool name="read_file"><description>Read file content (Auto-exec).</description></tool>
  <tool name="web_search"><description>Search the web.</description></tool>
</tools>
"""

# ==========================================
# 2. 增强工具箱与日志
# ==========================================

class Logger:
    def __init__(self, filename="agent_trace.log"):
        self.filename = filename
    def log(self, role: str, content: str):
        with open(self.filename, "a", encoding="utf-8") as f:
            f.write(f"\n[{datetime.now().strftime('%H:%M:%S')}] {role.upper()}:\n{content}\n")

class ToolBox:
    @staticmethod
    def run_command(cmd: str) -> str:
        if os.name == 'nt' and cmd == 'ls': cmd = 'dir'
        res = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=15)
        return f"OBSERVATION:\n{res.stdout}{res.stderr}"

    @staticmethod
    def write_file(path: str, content: str) -> str:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        return f"OBSERVATION: Saved to {path}."

    @staticmethod
    def read_file(path: str) -> str:
        with open(path, 'r', encoding='utf-8') as f:
            return f"OBSERVATION: Content of {path}:\n{f.read()}"

    @staticmethod
    def web_search(query: str) -> str:
        return f"OBSERVATION: Results for {query}..."

# ==========================================
# 3. Agent 核心
# ==========================================

class UltimateAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model
        self.toolbox = ToolBox()
        self.logger = Logger()
        self.history: List[Dict] = []

    def _parse_tool(self, text: str):
        # 匹配标准 XML
        pattern = r"<tool_call>.*?<name>(.*?)</name>.*?<params>(.*?)</params>.*?</tool_call>"
        match = re.search(pattern, text, re.DOTALL | re.IGNORECASE)
        if match:
            try: return match.group(1).strip(), json.loads(match.group(2).strip())
            except: pass
        return None, None

    def chat(self, user_input: str):
        if user_input:
            self.history.append({"role": "user", "content": user_input})
            self.logger.log("User", user_input)
        
        while True:
            print(f"{Style.DIM}Thinking...")
            messages = [{"role": "system", "content": SYSTEM_PROMPT_TEXT},
                        {"role": "system", "content": DEVELOPER_PROMPT_TEXT}] + self.history
            
            response = self.client.chat.completions.create(model=self.model, messages=messages, temperature=0.1)
            answer = response.choices[0].message.content
            print(f"\n{Fore.GREEN}[Assistant]:\n{answer}")
            self.history.append({"role": "assistant", "content": answer})
            self.logger.log("Assistant", answer)

            tool_name, tool_params = self._parse_tool(answer)
            if not tool_name: break

            obs = ""
            if tool_name == "write_file":
                # 丝滑写入，无需批准
                obs = self.toolbox.write_file(tool_params['path'], tool_params['content'])
                print(f"{Fore.CYAN}[System]: File '{tool_params['path']}' written automatically.")
            
            elif tool_name == "run_command":
                print(f"\n{Fore.RED}⚠️  SEC-CHECK: {tool_params['command']}")
                if input("Approve? (y/n): ").lower() == 'y':
                    obs = self.toolbox.run_command(tool_params['command'])
                else: obs = "OBSERVATION: Denied."

            elif tool_name == "read_file":
                obs = self.toolbox.read_file(tool_params['path'])
            
            elif tool_name == "web_search":
                obs = self.toolbox.web_search(tool_params['query'])

            if obs:
                self.history.append({"role": "user", "content": obs})
                self.logger.log("Observation", obs)
                continue
            break

if __name__ == "__main__":
    agent = UltimateAgent(os.getenv("DEEPSEEK_KEY", "sk-45021a61b84a4693a1db4deb72cec673"), "https://api.deepseek.com", "deepseek-chat")
    while True:
        msg = input(f"\n{Fore.WHITE}You: ")
        if msg.lower() in ['exit', 'quit']: break
        agent.chat(msg)