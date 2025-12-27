import subprocess
import threading
import time
import os
import uuid
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, field
from .console import Fore, Style

@dataclass
class TerminalProcess:
    id: str
    command: str
    process: subprocess.Popen
    is_long_running: bool
    output: List[str] = field(default_factory=list)
    start_time: float = field(default_factory=time.time)
    exit_code: Optional[int] = None
    thread: Optional[threading.Thread] = None

class TerminalManager:
    """
    管理多个终端进程，支持长期停留（非阻塞）和短期停留（阻塞）任务。
    """
    def __init__(self):
        self.terminals: Dict[str, TerminalProcess] = {}

    def run_command(self, command: str, is_long_running: bool = False, cwd: Optional[str] = None) -> Tuple[bool, str, str]:
        """
        执行指令。
        :param command: 要执行的命令
        :param is_long_running: 是否为长期停留任务（如 web 服务）
        :param cwd: 工作目录
        :return: (是否成功启动/执行, 终端ID/输出结果, 错误信息)
        """
        tid = str(uuid.uuid4())[:8]
        
        try:
            # 统一使用 shell 执行
            proc = subprocess.Popen(
                command,
                shell=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                cwd=cwd or os.getcwd(),
                bufsize=1,
                universal_newlines=True
            )

            term = TerminalProcess(
                id=tid,
                command=command,
                process=proc,
                is_long_running=is_long_running
            )
            self.terminals[tid] = term

            if is_long_running:
                # 长期任务：启动背景线程读取输出，立即返回 TID
                term.thread = threading.Thread(target=self._read_output, args=(term,), daemon=True)
                term.thread.start()
                return True, tid, ""
            else:
                # 短期任务：等待执行完毕并捕获输出
                stdout, stderr = proc.communicate(timeout=120)
                term.exit_code = proc.returncode
                output = f"Stdout:\n{stdout}\nStderr:\n{stderr}"
                # 短期任务执行完后从字典移除，避免内存堆积
                del self.terminals[tid]
                if proc.returncode == 0:
                    return True, output, ""
                else:
                    return False, output, f"Exit Code: {proc.returncode}"

        except Exception as e:
            return False, "", str(e)

    def _read_output(self, term: TerminalProcess):
        """背景线程读取长期任务的输出。"""
        try:
            for line in iter(term.process.stdout.readline, ''):
                if line:
                    term.output.append(line)
                    # 限制输出缓存大小，避免内存溢出
                    if len(term.output) > 1000:
                        term.output.pop(0)
            
            term.process.wait()
            term.exit_code = term.process.returncode
        except Exception:
            pass

    def get_terminal_status(self, tid: str) -> Optional[Dict[str, Any]]:
        """获取指定终端的状态和最新输出。"""
        if tid not in self.terminals:
            return None
        
        term = self.terminals[tid]
        return {
            "id": term.id,
            "command": term.command,
            "is_running": term.process.poll() is None,
            "exit_code": term.exit_code,
            "output": "".join(term.output[-50:]) # 返回最后50行输出
        }

    def stop_terminal(self, tid: str) -> bool:
        """停止并移除指定终端。"""
        if tid not in self.terminals:
            return False
        
        term = self.terminals[tid]
        try:
            if term.process.poll() is None:
                if os.name == 'nt':
                    subprocess.run(f"taskkill /F /T /PID {term.process.pid}", shell=True, capture_output=True)
                else:
                    term.process.terminate()
            del self.terminals[tid]
            return True
        except Exception:
            return False

    def list_terminals(self) -> List[Dict[str, Any]]:
        """列出所有正在运行的长期任务终端。"""
        return [
            {
                "id": t.id,
                "command": t.command,
                "uptime": time.time() - t.start_time,
                "is_running": t.process.poll() is None
            }
            for t in self.terminals.values() if t.is_long_running
        ]
