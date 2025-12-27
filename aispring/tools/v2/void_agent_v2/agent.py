import os
import platform
import subprocess
from typing import Any, Dict, List, Optional, Tuple

import requests
import urllib3

from .config import Config
from .console import Fore, Style
from .files import (
    calculate_diff_of_lines,
    edit_lines,
    ensure_parent_dir,
    generate_dir_tree,
    generate_tree_structure,
    generate_match_tree,
    read_range,
    read_range_numbered,
    search_files,
    search_in_files,
)
from .logs import append_edit_history, append_usage_history, log_request, rollback_last_edit
from .metrics import CacheStats
from .tags import parse_stack_of_tags
from .terminal import TerminalManager


class VoidAgent:
    def __init__(self, config: Config):
        self.config = config
        if not self.config.verifySsl:
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        self.historyOfMessages: List[Dict[str, str]] = []
        self.endpointOfChat = f"{self.config.baseUrl.rstrip('/')}/chat/completions"
        self.historyOfOperations: List[Tuple[str, int, int]] = []
        self.cacheOfBackups: Dict[str, str] = {}
        self.cacheOfSystemMessage: Optional[Dict[str, str]] = None
        self.statsOfCache = CacheStats()
        self.terminalManager = TerminalManager()
        self.cacheOfProjectTree: Optional[str] = None
        self.cacheOfUserRules: Optional[str] = None

    def estimateTokensOfMessages(self, messages: List[Dict[str, str]]) -> int:
        totalChars = 0
        for msg in messages:
            totalChars += len(msg["role"]) + len(msg["content"]) + 8
        return int(totalChars / 3)

    def getContextOfSystem(self) -> str:
        """返回包含静态逻辑和用户规则的 System Prompt。"""
        rulesBlock = self.getUserRulesCached() or ""
        return f"""# VOID AGENT - STRICT LOGIC & PASSIVE VALIDATION
## 🔴 VOID RULES (STRICT ADHERENCE REQUIRED)
1. **PASSIVE VALIDATION**: Do NOT execute commands blindly. You must PROPOSE actions. The user acts as the validator.
2. **SEARCH FIRST (REGEX FIRST)**: When looking for problem code, prefer REGEX searching in file contents.
3. **MULTI TASKS ALLOWED**: You may output multiple closed tags per reply; they will be executed in order.
4. **NO TASK = NO TAGS**: Reply with natural language only if no action is needed.
5. **NAMING CONVENTION**: Variable names in your code should follow 'bOfA' pattern (e.g., contentOfFile).
6. **EDIT EXISTING CODE BY LINE**: If a file already exists, prefer editing by lines instead of rewriting the whole file.
7. **FOCUS FIRST**: The user's current request has the highest priority. Only do what the user explicitly asked.
8. **NO OVER-EXECUTION**: Do NOT search/read extra files, do NOT propose unrelated improvements, and do NOT create README unless asked.
9. **STOP AFTER TASK**: After completing the requested task(s), respond briefly and do NOT continue with additional tasks.
10. **NO REPETITION**: Do NOT repeat or paraphrase the user's rules, project tree, or any provided context unless explicitly asked.
11. **NO BOILERPLATE**: Do NOT output greetings, capability lists, or generic prompts. Answer directly.

## 📋 USER RULES (FROM .VOIDRULES)
```text
{rulesBlock}
```

## 📋 TAG SYNTAX (EXACTLY AS SHOWN)

### 1. Search Files (glob pattern)
<search_files>
  <pattern>**/*.py</pattern>
</search_files>

### 1b. Search In Files (regex + glob + root)
<search_in_files>
  <regex>VoidAgent\\b</regex>
  <glob>**/*.py</glob>
  <root>.</root>
  <max_matches>200</max_matches>
</search_in_files>

### 2. Write File (path + content required)
<write_file>
  <path>path/to/file</path>
  <content>file_content</content>
</write_file>

### 2b. Edit Lines (delete range, then insert at line)
<edit_lines>
  <path>path/to/file.py</path>
  <delete_start>10</delete_start>
  <delete_end>20</delete_end>
  <insert_at>10</insert_at>
  <content>new lines...</content>
</edit_lines>

### 3. Read File (path required, start_line/end_line optional)
<read_file>
  <path>path/to/file</path>
  <start_line>1</start_line>
  <end_line>100</end_line>
</read_file>

### 4. Run Command (Execute system commands)
<run_command>
  <command>python app.py</command>
  <is_long_running>false</is_long_running>
</run_command>

- **Short-running**: Set `<is_long_running>false</is_long_running>` (default). Use for quick commands like `ls`, `git status`, `pip install`.
- **Long-running**: Set `<is_long_running>true</is_long_running>`. Use for web servers, watchers, or any process that stays active. You will receive a Terminal ID to track it.

## 🚫 FORBIDDEN
- Unclosed tags
- Dangerous commands (rm -rf, etc.) without explicit user request
"""

    def invalidateProjectTreeCache(self) -> None:
        """使项目树缓存失效（下次构造 user 上下文时会重新生成）。"""
        self.cacheOfProjectTree = None

    def invalidateUserRulesCache(self) -> None:
        """使 .voidrules 缓存失效（下次构造 user 上下文时会重新读取）。"""
        self.cacheOfUserRules = None

    def getUserRulesCached(self) -> str:
        """读取并缓存 .voidrules 内容；仅在缓存失效后才重新读取。"""
        if self.cacheOfUserRules is not None:
            return self.cacheOfUserRules

        contentOfRules = ""
        cwd = os.getcwd()
        pathOfRules = os.path.join(cwd, ".voidrules")
        if os.path.exists(pathOfRules):
            try:
                with open(pathOfRules, "r", encoding="utf-8") as f:
                    contentOfRules = f.read().strip()
            except Exception:
                contentOfRules = ""
        self.cacheOfUserRules = contentOfRules
        return self.cacheOfUserRules

    def getProjectTreeCached(self) -> str:
        """生成并缓存项目树；仅在缓存失效（例如写入/编辑文件）后才重新生成。"""
        if self.cacheOfProjectTree is not None:
            return self.cacheOfProjectTree

        cwd = os.getcwd()
        treeOfCwd = ""
        try:
            treeOfCwd = generate_dir_tree(cwd, max_depth=3, max_entries=300)
        except Exception:
            treeOfCwd = f"{cwd}/"

        self.cacheOfProjectTree = treeOfCwd
        return self.cacheOfProjectTree

    def printToolResult(self, text: str, maxChars: int = 8000) -> None:
        return

    def getContextOfCurrentUser(self, inputOfUser: str) -> str:
        """构造当前轮的 user 动态上下文（环境信息）并拼接用户问题。"""
        sep = os.sep
        osName = platform.system()
        cwd = os.getcwd()

        return (
            "IMPORTANT: The following CONTEXT is input-only. Do NOT quote, repeat, or summarize it unless the user asks.\n"
            "IMPORTANT: Answer only the user's question. No greetings. No capability lists.\n"
            "\n"
            "BEGIN_CONTEXT\n"
            f"OPERATING SYSTEM: {osName}\n"
            f"PATH SEPARATOR: {sep}\n"
            f"CURRENT DIRECTORY: {cwd}\n"
            "END_CONTEXT\n"
            "\n"
            f"{inputOfUser}"
        )

    def getSystemMessage(self) -> Dict[str, str]:
        if self.cacheOfSystemMessage is None:
            self.cacheOfSystemMessage = {"role": "system", "content": self.getContextOfSystem()}
        return self.cacheOfSystemMessage

    def invalidateSystemMessageCache(self) -> None:
        self.cacheOfSystemMessage = None

    def backupFile(self, pathOfFile: str):
        if os.path.exists(pathOfFile):
            with open(pathOfFile, "r", encoding="utf-8") as f:
                self.cacheOfBackups[pathOfFile] = f.read()

    def rollbackLastOperation(self):
        ok, msg = rollback_last_edit()
        if ok:
            print(f"{Fore.GREEN}{msg}{Style.RESET_ALL}")
            if self.historyOfOperations:
                self.historyOfOperations.pop()
            return

        if not self.historyOfOperations:
            print(f"{Fore.RED}No operation to rollback{Style.RESET_ALL}")
            return
        lastOp = self.historyOfOperations[-1]
        pathOfFile = lastOp[0]
        if pathOfFile in self.cacheOfBackups:
            try:
                with open(pathOfFile, "w", encoding="utf-8") as f:
                    f.write(self.cacheOfBackups[pathOfFile])
                print(f"{Fore.GREEN}Rolled back file: {pathOfFile}{Style.RESET_ALL}")
                self.historyOfOperations.pop()
                del self.cacheOfBackups[pathOfFile]
            except Exception as e:
                print(f"{Fore.RED}Rollback failed: {str(e)}{Style.RESET_ALL}")
        else:
            print(f"{Fore.RED}No backup data found for rollback{Style.RESET_ALL}")

    def printStatsOfModification(self):
        if not self.historyOfOperations:
            print(f"\n{Fore.GREEN}No file modifications in this session{Style.RESET_ALL}")
            return
        print(f"\n{Style.BRIGHT}===== File Modification Stats ====={Style.RESET_ALL}")
        for op in self.historyOfOperations:
            pathOfFile, added, deleted = op[0], op[1], op[2]
            print(f"File: {pathOfFile}")
            print(f"  {Fore.BLUE}+({added}){Style.RESET_ALL} | {Fore.RED}-({deleted}){Style.RESET_ALL}")
        print(f"{Style.BRIGHT}==================================={Style.RESET_ALL}")

    def chat(self, inputOfUser: str):
        """
        处理用户输入并启动 AI 代理的多轮任务执行循环。
        
        Args:
            inputOfUser: 用户在控制台输入的原始文本。
        """
        if not inputOfUser.strip() and not self.historyOfMessages:
            print(f"{Fore.YELLOW}Empty input and no history. Waiting for command...{Style.RESET_ALL}")
            return

        msgSystem = self.getSystemMessage()
        baseHistoryLen = len(self.historyOfMessages)
        historyWorking: List[Dict[str, str]] = list(self.historyOfMessages)
        insertedUserContext = False
        if inputOfUser:
            historyWorking.append({"role": "user", "content": self.getContextOfCurrentUser(inputOfUser)})
            insertedUserContext = True

        countCycle = 0
        while countCycle < self.config.maxCycles:
            try:
                countCycle += 1
                print(f"{Fore.YELLOW}[Cycle {countCycle}/{self.config.maxCycles}] Processing...{Style.RESET_ALL}")

                messages = [msgSystem] + historyWorking
                estimateTokens = self.estimateTokensOfMessages(messages)
                if estimateTokens > 115000 and len(historyWorking) > 60:
                    head = historyWorking[:6]
                    tail = historyWorking[-54:]
                    messages = [msgSystem] + head + tail
                    estimateTokens = self.estimateTokensOfMessages(messages)
                print(f"{Fore.MAGENTA}[Token Estimate] ~{estimateTokens} tokens{Style.RESET_ALL}")

                try:
                    log_request(messages)
                except Exception as e:
                    print(f"{Fore.RED}[Log Error] Could not save log: {str(e)}{Style.RESET_ALL}")

                headers = {"Authorization": f"Bearer {self.config.apiKey}", "Content-Type": "application/json"}
                payload = {
                    "model": self.config.modelName,
                    "messages": messages,
                    "temperature": 0.1,
                    "stream": True,
                    "stream_options": {"include_usage": True},
                    "max_tokens": 8000,
                }

                replyFull = ""
                fullReasoning = ""
                hasReasoned = False
                usageOfRequest: Optional[Dict[str, Any]] = None
                print(f"{Fore.GREEN}[VoidAgent]: ", end="")
                try:
                    response = requests.post(
                        self.endpointOfChat, 
                        headers=headers, 
                        json=payload, 
                        stream=True, 
                        timeout=60,
                        verify=self.config.verifySsl
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

                        dataChunk = json.loads(line)
                        if isinstance(dataChunk, dict) and "usage" in dataChunk and isinstance(dataChunk.get("usage"), dict):
                            usageOfRequest = dataChunk.get("usage")
                        choices = dataChunk.get("choices") if isinstance(dataChunk, dict) else None
                        if isinstance(choices, list) and choices:
                            delta = choices[0].get("delta") if isinstance(choices[0], dict) else None
                            if not delta:
                                continue
                            
                            # 提取推理内容 (reasoning_content)
                            reasoning = delta.get("reasoning_content", "")
                            if reasoning:
                                if not hasReasoned:
                                    hasReasoned = True
                                fullReasoning += reasoning
                                print(f"{Fore.CYAN}{reasoning}{Style.RESET_ALL}", end="", flush=True)

                            # 提取正文内容
                            token = delta.get("content", "")
                            if token:
                                if hasReasoned:
                                    # 如果之前有推理内容，且现在开始输出正文，先换行
                                    print("\n")
                                    hasReasoned = False # 只换行一次
                                replyFull += token
                                print(token, end="", flush=True)
                except requests.exceptions.RequestException as e:
                    msgError = f"{Fore.RED}[Request Error] {str(e)}{Style.RESET_ALL}"
                    print(msgError)
                    replyFull = msgError
                    break
                finally:
                    print("\n" + "-" * 40)
                    if usageOfRequest:
                        self.statsOfCache.updateFromUsage(usageOfRequest)
                        
                        # 从单次请求中提取命中和未命中
                        hit = int(usageOfRequest.get("prompt_cache_hit_tokens") or 0)
                        if hit == 0:
                            details = usageOfRequest.get("prompt_tokens_details")
                            if isinstance(details, dict):
                                hit = int(details.get("cached_tokens") or 0)
                        
                        prompt = int(usageOfRequest.get("prompt_tokens") or 0)
                        miss = int(usageOfRequest.get("prompt_cache_miss_tokens") or 0)
                        if miss == 0 and prompt > hit:
                            miss = prompt - hit

                        rateReq = CacheStats.getHitRateOfUsage(usageOfRequest)
                        rateSession = self.statsOfCache.getSessionHitRate()
                        rateReqStr = f"{rateReq*100:.1f}%" if rateReq is not None else "N/A"
                        rateSessionStr = f"{rateSession*100:.1f}%" if rateSession is not None else "N/A"
                        print(
                            f"{Fore.CYAN}[Cache] hit={hit} miss={miss} rate={rateReqStr} | session_rate={rateSessionStr}{Style.RESET_ALL}"
                        )
                        try:
                            append_usage_history(
                                usage=usageOfRequest,
                                cache={
                                    "hit": hit,
                                    "miss": miss,
                                    "rate_request": rateReq,
                                    "rate_session": rateSession,
                                    "session": {
                                        "counted_requests": self.statsOfCache.countedRequests,
                                        "hit_tokens": self.statsOfCache.promptCacheHitTokens,
                                        "miss_tokens": self.statsOfCache.promptCacheMissTokens,
                                        "prompt_tokens": self.statsOfCache.promptTokens,
                                        "completion_tokens": self.statsOfCache.completionTokens,
                                        "total_tokens": self.statsOfCache.totalTokens,
                                    },
                                },
                            )
                        except Exception:
                            pass

                historyWorking.append({"role": "assistant", "content": replyFull})
                tasks = parse_stack_of_tags(replyFull)

                if not tasks:
                    replyLower = replyFull.lower()
                    suspiciousTagTokens = [
                        "<write_file",
                        "<read_file",
                        "<run_command",
                        "<search_files",
                        "<search_in_files",
                        "<edit_lines",
                        "</write_file",
                        "</read_file",
                        "</run_command",
                        "</search_files",
                        "</search_in_files",
                        "</edit_lines",
                    ]
                    if any(tok in replyLower for tok in suspiciousTagTokens):
                        feedbackError = "ERROR: Invalid Format! Use one or more closed tags. No tag if no task."
                        historyWorking.append({"role": "user", "content": feedbackError})
                    else:
                        break

                observations: List[str] = []
                isCancelled = False
                didExecuteAnyTask = False
                for t in tasks:
                    isWhitelisted = False
                    if t["type"] in self.config.whitelistedTools:
                        isWhitelisted = True
                    elif t["type"] == "run_command":
                        cmd_first = str(t.get("command", "")).strip().splitlines()[:1]
                        baseCmd = cmd_first[0].split()[0] if cmd_first and cmd_first[0] else ""
                        if baseCmd in self.config.whitelistedCommands:
                            isWhitelisted = True

                    if isWhitelisted:
                        confirm = "y"
                    else:
                        confirm = input(f"{Style.BRIGHT}Execute this task? (y/n): ")

                    if confirm.lower() != "y":
                        historyWorking.append({"role": "user", "content": "User cancelled execution"})
                        isCancelled = True
                        break
                    didExecuteAnyTask = True

                    if t["type"] == "search_files":
                        pattern = t["pattern"]
                        try:
                            results = search_files(pattern, os.getcwd())
                            if results:
                                treeOutput = generate_tree_structure(results, os.getcwd())
                                obs = f"SUCCESS: Found {len(results)} files:\n{treeOutput}"
                                observations.append(obs)
                                self.printToolResult(obs)
                            else:
                                obs = f"SUCCESS: No files found matching {pattern}"
                                observations.append(obs)
                                self.printToolResult(obs)
                        except Exception as e:
                            obs = f"FAILURE: {str(e)}"
                            observations.append(obs)
                            self.printToolResult(obs)

                    elif t["type"] == "search_in_files":
                        regex = t["regex"]
                        glob_pattern = t.get("glob") or "**/*"
                        root = os.path.abspath(t.get("root") or ".")
                        max_matches = int(t.get("max_matches") or 200)
                        try:
                            matches_by_path, error = search_in_files(
                                regex=regex,
                                root_dir=root,
                                glob_pattern=glob_pattern,
                                max_matches=max_matches,
                            )
                            if error:
                                obs = f"FAILURE: Invalid regex: {error}"
                                observations.append(obs)
                                self.printToolResult(obs)
                            elif matches_by_path:
                                treeOutput = generate_match_tree(matches_by_path, root)
                                totalMatches = sum(len(v) for v in matches_by_path.values())
                                obs = (
                                    "SUCCESS: Regex matches found\n"
                                    f"Regex: {regex}\n"
                                    f"Glob: {glob_pattern}\n"
                                    f"Matches: {totalMatches} (files: {len(matches_by_path)})\n"
                                    f"{treeOutput}"
                                )
                                observations.append(obs)
                                self.printToolResult(obs)
                            else:
                                obs = (
                                    "SUCCESS: No regex matches found\n"
                                    f"Regex: {regex}\n"
                                    f"Glob: {glob_pattern}\n"
                                    f"Root: {root}"
                                )
                                observations.append(obs)
                                self.printToolResult(obs)
                        except Exception as e:
                            obs = f"FAILURE: {str(e)}"
                            observations.append(obs)
                            self.printToolResult(obs)

                    elif t["type"] == "write_file":
                        path = os.path.abspath(t["path"])
                        content = t["content"]
                        try:
                            existedBefore = os.path.exists(path)
                            self.backupFile(path)
                            ensure_parent_dir(path)
                            before_content = self.cacheOfBackups.get(path, "")
                            added, deleted = calculate_diff_of_lines(path, content)
                            with open(path, "w", encoding="utf-8") as f:
                                f.write(content)
                            self.historyOfOperations.append((path, added, deleted))
                            append_edit_history(
                                path_of_file=path,
                                before_content=before_content,
                                after_content=content,
                                meta={"type": "write_file"},
                            )
                            self.invalidateProjectTreeCache()
                            if os.path.basename(path).lower() == ".voidrules":
                                self.invalidateUserRulesCache()
                                self.invalidateSystemMessageCache()
                            obs = f"SUCCESS: Saved to {path} | +{added} | -{deleted}"
                            observations.append(obs)
                            self.printToolResult(obs)
                        except Exception as e:
                            obs = f"FAILURE: {str(e)}"
                            observations.append(obs)
                            self.printToolResult(obs)

                    elif t["type"] == "edit_lines":
                        path = os.path.abspath(t["path"])
                        delete_start = t.get("delete_start")
                        delete_end = t.get("delete_end")
                        insert_at = t.get("insert_at")
                        content = t.get("content", "")
                        try:
                            existedBefore = os.path.exists(path)
                            self.backupFile(path)
                            ensure_parent_dir(path)
                            before_content, after_content = edit_lines(
                                path_of_file=path,
                                delete_start=delete_start,
                                delete_end=delete_end,
                                insert_at=insert_at,
                                content=content,
                            )
                            added, deleted = calculate_diff_of_lines(path, after_content)
                            with open(path, "w", encoding="utf-8") as f:
                                f.write(after_content)
                            self.historyOfOperations.append((path, added, deleted))
                            append_edit_history(
                                path_of_file=path,
                                before_content=before_content,
                                after_content=after_content,
                                meta={
                                    "type": "edit_lines",
                                    "delete_start": delete_start,
                                    "delete_end": delete_end,
                                    "insert_at": insert_at,
                                },
                            )
                            self.invalidateProjectTreeCache()
                            if os.path.basename(path).lower() == ".voidrules":
                                self.invalidateUserRulesCache()
                                self.invalidateSystemMessageCache()
                            obs = f"SUCCESS: Edited {path} | +{added} | -{deleted}"
                            observations.append(obs)
                            self.printToolResult(obs)
                        except Exception as e:
                            obs = f"FAILURE: {str(e)}"
                            observations.append(obs)
                            self.printToolResult(obs)

                    elif t["type"] == "read_file":
                        path = os.path.abspath(t["path"])
                        startLine = t.get("start_line", 1)
                        endLine = t.get("end_line")
                        try:
                            totalLines, actualEnd, content = read_range_numbered(path, startLine, endLine)
                            obs = (
                                f"SUCCESS: Read {path}\n"
                                f"Lines: {totalLines} | Range: {startLine}-{actualEnd}\n"
                                f"Content:\n{content}"
                            )
                            observations.append(obs)
                            self.printToolResult(obs)
                        except Exception as e:
                            obs = f"FAILURE: {str(e)}"
                            observations.append(obs)
                            self.printToolResult(obs)

                    elif t["type"] == "run_command":
                        cmd_text = str(t["command"])
                        is_long = str(t.get("is_long_running", "false")).lower() == "true"
                        
                        commands = [c.strip() for c in cmd_text.splitlines() if c.strip()]
                        if not commands:
                            obs = "FAILURE: Empty command"
                            observations.append(obs)
                            self.printToolResult(obs)
                        else:
                            for cmd in commands:
                                dangerousCmds = ["rm -rf", "format", "del /f/s/q", "mkfs"]
                                if any(d in cmd.lower() for d in dangerousCmds):
                                    obs = f"FAILURE: Dangerous command blocked: {cmd}"
                                    observations.append(obs)
                                    self.printToolResult(obs)
                                    continue
                                
                                try:
                                    success, output, error = self.terminalManager.run_command(cmd, is_long_running=is_long)
                                    if success:
                                        if is_long:
                                            obs = f"SUCCESS: Long-running command started. Terminal ID: {output}\nCommand: {cmd}"
                                        else:
                                            obs = f"SUCCESS: Command executed: {cmd}\n{output}"
                                    else:
                                        obs = f"FAILURE: {error}\n{output}"
                                    
                                    observations.append(obs)
                                    self.printToolResult(obs)
                                except Exception as e:
                                    obs = f"FAILURE: {str(e)}"
                                    observations.append(obs)
                                    self.printToolResult(obs)

                if observations:
                    historyWorking.append({"role": "user", "content": "\n".join(observations)})
                if isCancelled:
                    break
                if didExecuteAnyTask and self.config.stopAfterFirstToolExecution:
                    break

            except Exception as e:
                print(f"{Fore.RED}[Cycle Error] {str(e)}{Style.RESET_ALL}")
                historyWorking.append({"role": "user", "content": f"ERROR: Agent crashed with error: {str(e)}"})
                break

        if countCycle >= self.config.maxCycles:
            print(f"{Fore.RED}[Tip] Max cycles reached.{Style.RESET_ALL}")

        self.historyOfMessages = historyWorking

        self.printStatsOfModification()
