import os
import json
import re
import subprocess
from datetime import datetime
from typing import List, Dict, Optional
from colorama import init, Fore, Style
from openai import OpenAI

# 初始化终端颜色
init(autoreset=True)

# ==========================================
# 1. 提示词：明确写入规范与角色
# ==========================================

SYSTEM_PROMPT_TEXT = """You are an Autonomous Terminal & Research Expert.
WORKSPACE: Current directory.

STRICT RULES:
1. To CREATE or UPDATE files, ALWAYS use 'write_file'. Do NOT use 'echo'.
2. 'write_file' and 'read_file' are auto-executed (silent).
3. 'run_command' ALWAYS requires user approval.
4. Use 'web_search' for real-time info.
"""

DEVELOPER_PROMPT_TEXT = """ROLE: Terminal Assistant
TOOL_FORMAT_PREFERENCE:
<tool_call>
  <name>tool_name</name>
  <params>{"param": "value"}</params>
</tool_call>

AVAILABLE TOOLS:
- write_file: {"path": "str", "content": "str"}
- read_file: {"path": "str"}
- run_command: {"command": "str"}
- web_search: {"query": "str"}
"""

# ==========================================
# 2. 工具箱与日志持久化
# ==========================================

class Logger:
    def __init__(self, filename="agent_trace.log"):
        self.filename = filename
    def log(self, role: str, content: str):
        with open(self.filename, "a", encoding="utf-8") as f:
            f.write(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {role.upper()}:\n{content}\n")

class ToolBox:
    @staticmethod
    def run_command(cmd: str) -> str:
        # 针对 Windows 的常用命令转换
        if os.name == 'nt' and cmd.startswith('ls'):
            cmd = cmd.replace('ls', 'dir', 1)
        
        try:
            res = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=20)
            output = res.stdout + res.stderr
            return f"OBSERVATION: (Exit Code {res.returncode})\n{output}"
        except Exception as e:
            return f"OBSERVATION: Command failed. Error: {str(e)}"

    @staticmethod
    def write_file(path: str, content: str) -> str:
        try:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)
            return f"OBSERVATION: Successfully saved to {path}."
        except Exception as e:
            return f"OBSERVATION: Write failed. Error: {str(e)}"

    @staticmethod
    def read_file(path: str) -> str:
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return f"OBSERVATION: Content of {path}:\n{f.read()}"
        except Exception as e:
            return f"OBSERVATION: Read failed. Error: {str(e)}"

    @staticmethod
    def web_search(query: str) -> str:
        # 这里为模拟搜索结果，可接入搜索API
        return f"OBSERVATION: Found online data about '{query}'. (Simulated Result)"

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
        """核心增强：多重模式匹配，确保不漏掉 AI 的任何指令"""
        
        # 模式 1: 匹配 AI 刚才生成的子标签格式 <write_file><path>...</path><content>...</content></write_file>
        write_xml = r"<write_file>.*?<path>(.*?)</path>.*?<content>(.*?)</content>.*?</write_file>"
        m1 = re.search(write_xml, text, re.DOTALL | re.IGNORECASE)
        if m1:
            return "write_file", {"path": m1.group(1).strip(), "content": m1.group(2).strip()}

        # 模式 2: 匹配标准 <tool_call> 格式 (带 JSON 参数)
        std_xml = r"<tool_call>.*?<name>(.*?)</name>.*?<params>(.*?)</params>.*?</tool_call>"
        m2 = re.search(std_xml, text, re.DOTALL | re.IGNORECASE)
        if m2:
            try: return m2.group(1).strip(), json.loads(m2.group(2).strip())
            except: pass

        # 模式 3: 匹配简化命令标签 <run_command><command>...</command></run_command>
        cmd_xml = r"<run_command>.*?<command>(.*?)</command>.*?</run_command>"
        m3 = re.search(cmd_xml, text, re.DOTALL | re.IGNORECASE)
        if m3:
            return "run_command", {"command": m3.group(1).strip()}

        return None, None

    def chat(self, user_input: str):
        if user_input:
            self.history.append({"role": "user", "content": user_input})
            self.logger.log("User", user_input)
        
        while True:
            print(f"\n{Fore.CYAN}[System]: AI 正在思考/搜索...{Style.RESET_ALL}")
            
            messages = [
                {"role": "system", "content": SYSTEM_PROMPT_TEXT},
                {"role": "system", "content": DEVELOPER_PROMPT_TEXT},
                *self.history
            ]

            # 启用 Streaming 流式输出
            stream = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.1,
                stream=True
            )

            full_response = ""
            print(f"{Fore.GREEN}[Assistant]: ", end="")
            for chunk in stream:
                token = chunk.choices[0].delta.content
                if token:
                    full_response += token
                    print(token, end="", flush=True)
            print("\n")

            self.history.append({"role": "assistant", "content": full_response})
            self.logger.log("Assistant", full_response)

            # 解析工具指令
            name, params = self._parse_tool(full_response)
            if not name: break # 无工具调用，结束对话轮次

            observation = ""
            # --- 自动执行类工具 ---
            if name == "write_file":
                observation = self.toolbox.write_file(params.get('path'), params.get('content'))
                print(f"{Fore.BLUE}✨ 自动执行: 文件 '{params.get('path')}' 已写入。")
            
            elif name == "read_file":
                observation = self.toolbox.read_file(params.get('path'))
                print(f"{Fore.BLUE}📖 自动执行: 已读取文件 '{params.get('path')}'。")
            
            elif name == "web_search":
                observation = self.toolbox.web_search(params.get('query'))
                print(f"{Fore.BLUE}🌐 自动执行: 网络搜索完成。")

            # --- 审批类工具 ---
            elif name == "run_command":
                cmd = params.get('command')
                print(f"\n{Fore.RED}{Style.BRIGHT}⚠️  安全确认: AI 请求执行指令 -> {Fore.WHITE}{cmd}")
                if input(f"{Fore.YELLOW}批准执行? (y/n): ").lower().strip() == 'y':
                    observation = self.toolbox.run_command(cmd)
                else:
                    observation = "OBSERVATION: User rejected command. Plan another approach."
                    print(f"{Fore.YELLOW}[System]: 指令已被拦截。")

            # 将观察结果喂给 AI 继续思考
            if observation:
                self.history.append({"role": "user", "content": observation})
                self.logger.log("Observation", observation)
                continue # 进入下一轮 AI 分析
            break

# ==========================================
# 4. 运行入口
# ==========================================

if __name__ == "__main__":
    # 配置 API 信息
    KEY = os.getenv("DEEPSEEK_KEY", "sk-45021a61b84a4693a1db4deb72cec673")
    URL = "https://api.deepseek.com"
    
    agent = UltimateAgent(KEY, URL, "deepseek-chat")

    print(f"{Fore.CYAN}{Style.BRIGHT}=== 终端助手 v6 运行中 ===")
    print(f"{Style.DIM}工作路径: {os.getcwd()}")
    print(f"{Style.DIM}输入 'exit' 退出程序。")

    while True:
        try:
            u_input = input(f"\n{Fore.WHITE}{Style.BRIGHT}You: ")
            if u_input.lower() in ['exit', 'quit']: break
            agent.chat(u_input)
        except KeyboardInterrupt:
            break