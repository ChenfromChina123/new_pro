import os
import platform
import subprocess
from colorama import init, Fore, Style
from openai import OpenAI

init(autoreset=True)

class UltimateAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model
        self.history = []

    def _get_system_context(self):
        """生成实时环境上下文，强制 AI 意识到自己在 Windows 还是 Linux"""
        cwd = os.getcwd()
        sep = os.sep
        os_name = platform.system()
        return f"""You are an AI Terminal Agent operating on {os_name}.
CURRENT DIRECTORY: {cwd}
PATH SEPARATOR: {sep}

STRICT RULE: To perform ANY file action, you MUST use:
<write_file><path>PATH</path><content>CONTENT</content></write_file>
DO NOT just show code blocks. If you don't use XML tags, the file will NOT be created."""

    def _stack_parser(self, text: str):
        """增强的堆栈解析，能够处理各种嵌套和异常换行"""
        tasks = []
        # 寻找顶层标签
        for tag in ["write_file", "read_file", "run_command"]:
            start_tag = f"<{tag}>"
            end_tag = f"</{tag}>"
            
            ptr = 0
            while True:
                s_idx = text.find(start_tag, ptr)
                if s_idx == -1: break
                e_idx = text.find(end_tag, s_idx)
                if e_idx == -1: break
                
                inner = text[s_idx + len(start_tag) : e_idx].strip()
                
                # 进一步剥离内部标签
                task = {"type": tag}
                if tag == "write_file":
                    # 即使 AI 把 path 写在属性里也能通过子标签找回
                    task["path"] = self._find_sub(inner, "path")
                    task["content"] = self._find_sub(inner, "content")
                elif tag == "read_file":
                    task["path"] = self._find_sub(inner, "path")
                elif tag == "run_command":
                    task["command"] = self._find_sub(inner, "command") or inner
                
                tasks.append(task)
                ptr = e_idx + len(end_tag)
        return tasks

    def _find_sub(self, source, sub):
        """在堆栈内容中寻找子项"""
        s, e = f"<{sub}>", f"</{sub}>"
        si = source.find(s)
        ei = source.find(e)
        if si != -1 and ei != -1:
            return source[si + len(s) : ei].strip()
        return ""

    def chat(self, user_input: str):
        # 每次对话都刷新系统上下文（因为目录可能变化）
        sys_msg = {"role": "system", "content": self._get_system_context()}
        
        if user_input:
            self.history.append({"role": "user", "content": user_input})

        while True:
            print(f"{Fore.YELLOW}感知路径并思考中...{Style.RESET_ALL}")
            
            # 组合消息：系统背景 + 历史
            messages = [sys_msg] + self.history[-10:] # 取最近10轮防止超过 token

            stream = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.1,
                stream=True
            )

            full_reply = ""
            print(f"{Fore.GREEN}[Assistant]: ", end="")
            for chunk in stream:
                token = chunk.choices[0].delta.content
                if token:
                    full_reply += token
                    print(token, end="", flush=True)
            print("\n" + "-"*40)

            self.history.append({"role": "assistant", "content": full_reply})
            
            # 解析
            tasks = self._stack_parser(full_reply)
            
            if not tasks:
                # 检查 AI 是否在忽悠（说了要写文件但没给标签）
                if "write" in full_reply.lower() and "<write_file>" not in full_reply.lower():
                    error_feedback = "ERROR: You mentioned writing a file but didn't use <write_file> tags. Please use XML tags to actually execute the task."
                    self.history.append({"role": "user", "content": error_feedback})
                    continue
                break

            observations = []
            for t in tasks:
                if t["type"] == "write_file":
                    path = t["path"]
                    # 路径感知纠错：如果是相对路径，补全为绝对路径
                    if not os.path.isabs(path):
                        path = os.path.join(os.getcwd(), path)
                    
                    print(f"{Fore.CYAN}[执行] 正在写入文件到: {path}")
                    try:
                        os.makedirs(os.path.dirname(path), exist_ok=True)
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write(t["content"])
                        observations.append(f"SUCCESS: File saved at {path}")
                    except Exception as e:
                        observations.append(f"FAILURE: {str(e)}")
                
                # 其他工具逻辑... (省略同前)

            if observations:
                self.history.append({"role": "user", "content": "\n".join(observations)})
                continue
            break

if __name__ == "__main__":
    bot = UltimateAgent("sk-45021a61b84a4693a1db4deb72cec673", "https://api.deepseek.com", "deepseek-chat")
    while True:
        try:
            bot.chat(input(f"\n{Style.BRIGHT}You: "))
        except KeyboardInterrupt: break