import os
import subprocess
import json
from colorama import init, Fore, Style
from openai import OpenAI

init(autoreset=True)

class UltimateAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model
        self.history = []

    def _extract_tasks_by_stack(self, text: str):
        """
        纯堆栈解析逻辑：不使用正则，通过寻找标签位置提取内容
        支持嵌套结构如 <write_file><path>...</path><content>...</content></write_file>
        """
        tasks = []
        
        # 定义我们需要寻找的顶层标签
        tags = ["read_file", "write_file", "run_command"]
        
        for tag in tags:
            start_tag = f"<{tag}>"
            end_tag = f"</{tag}>"
            
            cursor = 0
            while True:
                # 寻找开始标签
                start_idx = text.find(start_tag, cursor)
                if start_idx == -1: break
                
                # 寻找结束标签
                end_idx = text.find(end_tag, start_idx)
                if end_idx == -1: break
                
                # 提取两个标签中间的原始荷载
                raw_payload = text[start_idx + len(start_tag) : end_idx].strip()
                
                # 根据类型进一步解析内部字段 (如 path, content, command)
                task = {"type": tag}
                if tag == "read_file":
                    task["path"] = self._get_sub_content(raw_payload, "path")
                elif tag == "write_file":
                    task["path"] = self._get_sub_content(raw_payload, "path")
                    task["content"] = self._get_sub_content(raw_payload, "content")
                elif tag == "run_command":
                    task["command"] = self._get_sub_content(raw_payload, "command")
                    if not task["command"]: task["command"] = raw_payload # 兼容无子标签情况
                
                tasks.append(task)
                cursor = end_idx + len(end_tag)
        
        return tasks

    def _get_sub_content(self, source: str, sub_tag: str):
        """辅助方法：从原始荷载中提取子标签内容"""
        s_tag = f"<{sub_tag}>"
        e_tag = f"</{sub_tag}>"
        s_idx = source.find(s_tag)
        e_idx = source.find(e_tag)
        if s_idx != -1 and e_idx != -1:
            return source[s_idx + len(s_tag) : e_idx].strip()
        return ""

    def chat(self, user_input: str):
        if user_input:
            self.history.append({"role": "user", "content": user_input})
        
        while True:
            print(f"\n{Fore.YELLOW}AI 正在思考...{Style.RESET_ALL}")
            
            stream = self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "system", "content": "You are a terminal assistant. Use XML tags for tools."}] + self.history,
                temperature=0.1,
                stream=True
            )

            full_content = ""
            active_tag = None
            print(f"{Fore.GREEN}[Assistant]: ", end="")
            
            for chunk in stream:
                token = chunk.choices[0].delta.content
                if token:
                    full_content += token
                    print(token, end="", flush=True)

                    # 实时识别：只要流中出现了开始标签，立刻在终端反馈提示
                    if not active_tag:
                        for t in ["read_file", "write_file", "run_command"]:
                            if f"<{t}>" in full_content and f"<{t}>" not in full_content[:-len(token)-5]:
                                active_tag = t
                                print(f"\n{Fore.MAGENTA}[检测到任务: {t}]{Fore.GREEN}", end="")
                    # 检测结束标签，重置提示状态
                    if active_tag and f"</{active_tag}>" in full_content:
                        active_tag = None

            print("\n" + "-"*30)
            self.history.append({"role": "assistant", "content": full_content})

            # --- 任务执行阶段 ---
            tasks = self._extract_tasks_by_stack(full_content)
            if not tasks: break

            observations = []
            for task in tasks:
                t_type = task["type"]
                
                if t_type == "read_file":
                    path = task.get("path")
                    print(f"{Fore.CYAN}[执行] 读取: {path}")
                    try:
                        with open(path, 'r', encoding='utf-8') as f:
                            observations.append(f"File {path} content:\n{f.read()}")
                    except Exception as e:
                        observations.append(f"Read error: {str(e)}")

                elif t_type == "write_file":
                    path, content = task.get("path"), task.get("content")
                    print(f"{Fore.CYAN}[执行] 写入: {path}")
                    try:
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write(content)
                        observations.append(f"Successfully wrote to {path}")
                    except Exception as e:
                        observations.append(f"Write error: {str(e)}")

                elif t_type == "run_command":
                    cmd = task.get("command")
                    print(f"{Fore.RED}[执行] 运行: {cmd}")
                    if input("确认执行? (y/n): ").lower() == 'y':
                        res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
                        observations.append(f"Command output:\n{res.stdout}{res.stderr}")

            if observations:
                self.history.append({"role": "user", "content": "\n".join(observations)})
                continue
            break

if __name__ == "__main__":
    # 配置
    KEY = "sk-45021a61b84a4693a1db4deb72cec673"
    URL = "https://api.deepseek.com"
    
    bot = UltimateAgent(KEY, URL, "deepseek-chat")
    
    print(f"{Fore.CYAN}=== Agent v7.0 纯堆栈解析版 ===")
    while True:
        try:
            bot.chat(input(f"\n{Style.BRIGHT}You: "))
        except KeyboardInterrupt: break