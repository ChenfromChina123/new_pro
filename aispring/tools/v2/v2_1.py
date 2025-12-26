import os
import platform
import subprocess
import requests
from colorama import init, Fore, Style

init(autoreset=True)

class UltimateAgent:
    def __init__(self, api_key: str, base_url: str, model: str):
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.history = []
        self.chat_endpoint = f"{self.base_url}/chat/completions"
        self.operation_history = []
        self.backup_cache = {}

    def count_tokens_estimate(self, messages: list) -> int:
        total_chars = 0
        for msg in messages:
            total_chars += len(msg["role"]) + len(msg["content"]) + 8
        return int(total_chars / 3)

    def _get_system_context(self):
        cwd = os.getcwd()
        sep = os.sep
        os_name = platform.system()
        return f"""# AI TERMINAL AGENT - STRICT SINGLE TAG RULE
OPERATING SYSTEM: {os_name}
CURRENT DIRECTORY: {cwd}
PATH SEPARATOR: {sep}

## 🔴 NON-NEGOTIABLE RULES (VIOLATION = NO EXECUTION)
1. **ONE TASK = ONE TAG ONLY** — output MAX 1 closed XML tag per reply.
2. **NO TASK = NO TAGS** — reply with natural language only, NO tag examples.
3. Tags must be paired and closed: <tag>...</tag> (unclosed tags are ignored).
4. ALLOWED TAGS: <write_file>, <read_file>, <run_command> (no custom tags).

## 📋 TAG SYNTAX (EXACTLY AS SHOWN)
### 1. Write File (path + content required)
<write_file>
  <path>file_path</path>
  <content>file_content</content>
</write_file>

### 2. Read File (path required, start_line/end_line optional)
# 读取全部内容
<read_file>
  <path>file_path</path>
</read_file>
# 读取指定行（行号从1开始，包含起止行）
<read_file>
  <path>file_path</path>
  <start_line>10</start_line>
  <end_line>20</end_line>
</read_file>

### 3. Run Command (os-compatible only)
<run_command>
  <command>system_command</command>
</run_command>

## 🚫 FORBIDDEN
- Multiple tags in one reply
- Unclosed tags (e.g., <run_command> without </run_command>)
- Example tags when no task is needed
- Dangerous commands: rm -rf, format, del /f/s/q, mkfs"""

    def _stack_parser(self, text: str):
        tasks = []
        valid_tags = ["write_file", "read_file", "run_command"]
        text_lower = text.lower()
        for tag in valid_tags:
            start_tag = f"<{tag}>"
            end_tag = f"</{tag}>"
            if text_lower.count(start_tag) > 1 or text_lower.count(end_tag) > 1:
                return tasks
        
        for tag in valid_tags:
            start_tag = f"<{tag}>"
            end_tag = f"</{tag}>"
            s_idx = text.find(start_tag) if start_tag in text else text_lower.find(start_tag.lower())
            if s_idx == -1:
                continue
            e_idx = text.find(end_tag, s_idx + len(start_tag)) if end_tag in text else text_lower.find(end_tag.lower(), s_idx + len(start_tag))
            if e_idx == -1:
                continue
            
            inner = text[s_idx + len(start_tag) : e_idx].strip()
            task = {"type": tag}
            
            if tag == "write_file":
                task["path"] = self._find_sub(inner, "path")
                task["content"] = self._find_sub(inner, "content")
                if not task["path"] or not task["content"]:
                    continue
            elif tag == "read_file":
                task["path"] = self._find_sub(inner, "path")
                start_line_str = self._find_sub(inner, "start_line")
                end_line_str = self._find_sub(inner, "end_line")
                task["start_line"] = int(start_line_str) if start_line_str.strip() else 1
                task["end_line"] = int(end_line_str) if end_line_str.strip() else None
                if not task["path"]:
                    continue
            elif tag == "run_command":
                task["command"] = self._find_sub(inner, "command") or inner.strip()
                if not task["command"]:
                    continue
            
            tasks.append(task)
            break
        return tasks

    def _find_sub(self, source, sub):
        s, e = f"<{sub}>", f"</{sub}>"
        si = source.find(s)
        ei = source.find(e)
        if si != -1 and ei != -1:
            return source[si + len(s) : ei].strip()
        s_lower, e_lower = f"<{sub.lower()}>", f"</{sub.lower()}>"
        si_lower = source.lower().find(s_lower)
        ei_lower = source.lower().find(e_lower)
        if si_lower != -1 and ei_lower != -1:
            return source[si_lower + len(s_lower) : ei_lower].strip()
        return ""
    
    def _backup_file(self, file_path: str):
        if os.path.exists(file_path):
            with open(file_path, "r", encoding="utf-8") as f:
                self.backup_cache[file_path] = f.read()
    
    def rollback_last_operation(self):
        if not self.operation_history:
            print(f"{Fore.RED}无可用回退操作{Style.RESET_ALL}")
            return
        last_op = self.operation_history[-1]
        file_path = last_op[0]
        if file_path in self.backup_cache:
            try:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(self.backup_cache[file_path])
                print(f"{Fore.GREEN}已回退文件: {file_path}{Style.RESET_ALL}")
                self.operation_history.pop()
                del self.backup_cache[file_path]
            except Exception as e:
                print(f"{Fore.RED}回退失败: {str(e)}{Style.RESET_ALL}")
        else:
            print(f"{Fore.RED}无备份数据，无法回退{Style.RESET_ALL}")

    def _calculate_line_diff(self, file_path: str, new_content: str) -> tuple:
        old_lines = 0
        if os.path.exists(file_path):
            with open(file_path, "r", encoding="utf-8") as f:
                old_lines = len(f.readlines())
        new_lines = len(new_content.splitlines())
        added = max(0, new_lines - old_lines)
        deleted = max(0, old_lines - new_lines)
        return added, deleted

    def _print_file_modification_stats(self):
        if not self.operation_history:
            print(f"\n{Fore.GREEN}本次对话无文件修改操作{Style.RESET_ALL}")
            return
        print(f"\n{Style.BRIGHT}===== 文件修改统计 ====={Style.RESET_ALL}")
        for op in self.operation_history:
            file_path, added, deleted = op[0], op[1], op[2]
            print(f"文件: {file_path}")
            print(f"  {Fore.BLUE}+({added}){Style.RESET_ALL} | {Fore.RED}-({deleted}){Style.RESET_ALL}")
        print(f"{Style.BRIGHT}========================{Style.RESET_ALL}")

    def chat(self, user_input: str, max_cycles: int = 5):
        sys_msg = {"role": "system", "content": self._get_system_context()}
        if user_input:
            self.history.append({"role": "user", "content": user_input})
        
        cycle_count = 0
        while cycle_count < max_cycles:
            cycle_count += 1
            print(f"{Fore.YELLOW}[循环 {cycle_count}/{max_cycles}] 处理中...{Style.RESET_ALL}")
            
            messages = [sys_msg] + self.history[-10:]
            token_estimate = self.count_tokens_estimate(messages)
            print(f"{Fore.MAGENTA}[Token 估算] 本次请求约消耗 {token_estimate} tokens{Style.RESET_ALL}")

            headers = {
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": self.model,
                "messages": messages,
                "temperature": 0.1,
                "stream": True,
                "max_tokens": 8000
            }

            full_reply = ""
            print(f"{Fore.GREEN}[Assistant]: ", end="")
            try:
                response = requests.post(
                    self.chat_endpoint,
                    headers=headers,
                    json=payload,
                    stream=True,
                    timeout=30
                )
                response.raise_for_status()

                for line in response.iter_lines():
                    if not line:
                        continue
                    line = line.decode("utf-8").strip()
                    if line.startswith("data: "):
                        line = line[6:]
                    if line in ("", "[DONE]"):
                        continue
                    import json
                    chunk_data = json.loads(line)
                    token = chunk_data["choices"][0]["delta"].get("content", "")
                    if token:
                        full_reply += token
                        print(token, end="", flush=True)
            except requests.exceptions.RequestException as e:
                error_msg = f"{Fore.RED}[请求错误] {str(e)}{Style.RESET_ALL}"
                print(error_msg)
                full_reply = error_msg
                break
            finally:
                print("\n" + "-"*40)

            self.history.append({"role": "assistant", "content": full_reply})
            tasks = self._stack_parser(full_reply)

            # 核心修改1：只要解析不到任务，直接退出循环
            if not tasks:
                tool_keywords = ["write", "read", "run", "command", "file"]
                if any(kw in full_reply.lower() for kw in tool_keywords):
                    error_feedback = "ERROR: 无效格式！一次只能有1个闭合标签，无任务时禁止输出标签。"
                    self.history.append({"role": "user", "content": error_feedback})
                else:
                    # AI输出无命令内容，直接退出
                    break

            print(f"{Fore.YELLOW}[待执行任务] {tasks[0]}{Style.RESET_ALL}")
            confirm = input(f"{Style.BRIGHT}是否执行该任务？(y/n): ")
            if confirm.lower() != "y":
                print(f"{Fore.BLUE}用户取消执行{Style.RESET_ALL}")
                self.history.append({"role": "user", "content": "用户取消执行任务"})
                break

            observations = []
            t = tasks[0]
            if t["type"] == "write_file":
                path = os.path.abspath(t["path"])
                content = t["content"]
                print(f"{Fore.CYAN}[执行] 写入文件: {path}")
                try:
                    self._backup_file(path)
                    os.makedirs(os.path.dirname(path), exist_ok=True)
                    added, deleted = self._calculate_line_diff(path, content)
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(content)
                    self.operation_history.append([path, added, deleted])
                    observations.append(f"SUCCESS: 保存到 {path} | 新增{added}行 | 删除{deleted}行")
                except Exception as e:
                    observations.append(f"FAILURE: {str(e)}")
            elif t["type"] == "read_file":
                path = os.path.abspath(t["path"])
                start_line = t.get("start_line", 1)
                end_line = t.get("end_line")
                print(f"{Fore.CYAN}[执行] 读取文件: {path} (行 {start_line} 至 {end_line if end_line else '末尾'})")
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        all_lines = [line.rstrip('\n') for line in f.readlines()]
                    total_lines = len(all_lines)
                    if start_line < 1 or (end_line is not None and end_line < start_line):
                        observations.append(f"FAILURE: 无效行号！起始行必须≥1，结束行必须≥起始行")
                    else:
                        actual_end = end_line if end_line and end_line <= total_lines else total_lines
                        target_lines = all_lines[start_line-1 : actual_end]
                        content = "\n".join(target_lines)
                        observations.append(
                            f"SUCCESS: 读取文件 {path}\n"
                            f"总行数: {total_lines} | 读取范围: 行{start_line}至行{actual_end}\n"
                            f"内容:\n{content}"
                        )
                except FileNotFoundError:
                    observations.append(f"FAILURE: 文件不存在: {path}")
                except Exception as e:
                    observations.append(f"FAILURE: {str(e)}")
            elif t["type"] == "run_command":
                cmd = t["command"]
                dangerous_cmds = ["rm -rf", "format", "del /f/s/q", "mkfs", "rm -rf /"]
                if any(d in cmd.lower() for d in dangerous_cmds):
                    observations.append(f"FAILURE: 禁止执行高危命令: {cmd}")
                else:
                    print(f"{Fore.CYAN}[执行] 运行命令: {cmd}")
                    try:
                        result = subprocess.run(
                            cmd,
                            shell=True,
                            capture_output=True,
                            text=True,
                            encoding='utf-8'
                        )
                        output = f"标准输出:\n{result.stdout}\n标准错误:\n{result.stderr}"
                        observations.append(f"SUCCESS: 命令执行完成\n{output}")
                    except Exception as e:
                        observations.append(f"FAILURE: {str(e)}")

            # 核心修改2：移除强制continue，改为将执行结果加入历史后，让循环重新判断AI是否有新任务
            if observations:
                self.history.append({"role": "user", "content": "\n".join(observations)})
        
        if cycle_count >= max_cycles:
            print(f"{Fore.RED}[提示] 已达最大循环次数 {max_cycles} 次，退出对话{Style.RESET_ALL}")
        
        self._print_file_modification_stats()

if __name__ == "__main__":
    bot = UltimateAgent(
        api_key="sk-45021a61b84a4693a1db4deb72cec673",
        base_url="https://api.deepseek.com",
        model="deepseek-chat"
    )
    while True:
        try:
            user_input = input(f"\n{Style.BRIGHT}You: ")
            if user_input.strip().lower() == "rollback":
                bot.rollback_last_operation()
                continue
            bot.chat(user_input, max_cycles=5)
        except KeyboardInterrupt:
            print(f"\n{Fore.BLUE}程序已退出{Style.RESET_ALL}")
            break