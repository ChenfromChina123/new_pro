import os
import platform
import subprocess
import requests
import glob
import fnmatch
import locale
import json
import datetime
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Any, Tuple
from colorama import init, Fore, Style

init(autoreset=True)

@dataclass
class Config:
    apiKey: str
    baseUrl: str
    modelName: str
    maxCycles: int = 30
    whitelistedTools: List[str] = field(default_factory=lambda: ["search_files", "read_file"])
    whitelistedCommands: List[str] = field(default_factory=lambda: ["ls", "dir", "pwd", "whoami", "echo", "cat", "type"])

class VoidAgent:
    def __init__(self, config: Config):
        self.config = config
        self.historyOfMessages: List[Dict[str, str]] = []
        self.endpointOfChat = f"{self.config.baseUrl.rstrip('/')}/chat/completions"
        self.historyOfOperations: List[Tuple[str, int, int]] = []
        self.cacheOfBackups: Dict[str, str] = {}

    def estimateTokensOfMessages(self, messages: List[Dict[str, str]]) -> int:
        totalChars = 0
        for msg in messages:
            totalChars += len(msg["role"]) + len(msg["content"]) + 8
        return int(totalChars / 3)

    def getContextOfSystem(self) -> str:
        cwd = os.getcwd()
        sep = os.sep
        osName = platform.system()
        
        # Load user rules from .voidrules (Void Feature: Customization)
        contentOfRules = ""
        pathOfRules = os.path.join(cwd, ".voidrules")
        if os.path.exists(pathOfRules):
            try:
                with open(pathOfRules, "r", encoding="utf-8") as f:
                    contentOfRules = f.read().strip()
            except Exception:
                pass
        
        sectionUserRules = ""
        if contentOfRules:
            sectionUserRules = f"\n## 👤 USER RULES (FROM .voidrules)\n{contentOfRules}\n"

        return f"""# VOID AGENT - STRICT LOGIC & PASSIVE VALIDATION
OPERATING SYSTEM: {osName}
CURRENT DIRECTORY: {cwd}
PATH SEPARATOR: {sep}
{sectionUserRules}
## 🔴 VOID RULES (STRICT ADHERENCE REQUIRED)
1. **PASSIVE VALIDATION**: Do NOT execute commands blindly. You must PROPOSE actions. The user acts as the validator.
2. **SEARCH FIRST**: You must SEARCH the codebase before modifying it to ensure you have the full context.
3. **ONE TASK = ONE TAG**: Output MAX 1 closed XML tag per reply.
4. **NO TASK = NO TAGS**: Reply with natural language only if no action is needed.
5. **NAMING CONVENTION**: Variable names in your code should follow 'bOfA' pattern (e.g., contentOfFile).

## 📋 TAG SYNTAX (EXACTLY AS SHOWN)

### 1. Search Files (glob pattern)
<search_files>
  <pattern>**/*.py</pattern>
</search_files>

### 2. Write File (path + content required)
<write_file>
  <path>path/to/file</path>
  <content>file_content</content>
</write_file>

### 3. Read File (path required, start_line/end_line optional)
<read_file>
  <path>path/to/file</path>
  <start_line>1</start_line>
  <end_line>100</end_line>
</read_file>

### 4. Run Command (Use for system commands, not for file editing)
<run_command>
  <command>ls -la</command>
</run_command>

## 🚫 FORBIDDEN
- Multiple tags in one reply
- Unclosed tags
- Dangerous commands (rm -rf, etc.) without explicit user request
"""

    def parseStackOfTags(self, text: str) -> List[Dict[str, Any]]:
        tasks = []
        validTags = ["write_file", "read_file", "run_command", "search_files"]
        textLower = text.lower()
        
        # Check for multiple tags
        for tag in validTags:
            startTag = f"<{tag}>"
            endTag = f"</{tag}>"
            if textLower.count(startTag) > 1 or textLower.count(endTag) > 1:
                return tasks
        
        for tag in validTags:
            startTag = f"<{tag}>"
            endTag = f"</{tag}>"
            sIdx = text.find(startTag) if startTag in text else textLower.find(startTag.lower())
            if sIdx == -1:
                continue
            eIdx = text.find(endTag, sIdx + len(startTag)) if endTag in text else textLower.find(endTag.lower(), sIdx + len(startTag))
            if eIdx == -1:
                continue
            
            inner = text[sIdx + len(startTag) : eIdx].strip()
            task = {"type": tag}
            
            if tag == "write_file":
                task["path"] = self.findSubstring(inner, "path")
                task["content"] = self.findSubstring(inner, "content")
                if not task["path"] or not task["content"]:
                    continue
            elif tag == "read_file":
                task["path"] = self.findSubstring(inner, "path")
                startLineStr = self.findSubstring(inner, "start_line")
                endLineStr = self.findSubstring(inner, "end_line")
                task["start_line"] = int(startLineStr) if startLineStr.strip() else 1
                task["end_line"] = int(endLineStr) if endLineStr.strip() else None
                if not task["path"]:
                    continue
            elif tag == "run_command":
                task["command"] = self.findSubstring(inner, "command") or inner.strip()
                if not task["command"]:
                    continue
            elif tag == "search_files":
                task["pattern"] = self.findSubstring(inner, "pattern") or "*"
            
            tasks.append(task)
            break
        return tasks

    def findSubstring(self, source: str, sub: str) -> str:
        s, e = f"<{sub}>", f"</{sub}>"
        si = source.find(s)
        ei = source.find(e)
        if si != -1 and ei != -1:
            return source[si + len(s) : ei].strip()
        sLower, eLower = f"<{sub.lower()}>", f"</{sub.lower()}>"
        siLower = source.lower().find(sLower)
        eiLower = source.lower().find(eLower)
        if siLower != -1 and eiLower != -1:
            return source[siLower + len(sLower) : eiLower].strip()
        return ""
    
    def backupFile(self, pathOfFile: str):
        if os.path.exists(pathOfFile):
            with open(pathOfFile, "r", encoding="utf-8") as f:
                self.cacheOfBackups[pathOfFile] = f.read()
    
    def rollbackLastOperation(self):
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

    def calculateDiffOfLines(self, pathOfFile: str, contentNew: str) -> Tuple[int, int]:
        linesOld = 0
        if os.path.exists(pathOfFile):
            with open(pathOfFile, "r", encoding="utf-8") as f:
                linesOld = len(f.readlines())
        linesNew = len(contentNew.splitlines())
        added = max(0, linesNew - linesOld)
        deleted = max(0, linesOld - linesNew)
        return added, deleted

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

    def searchFiles(self, pattern: str) -> List[str]:
        results = []
        for root, dirs, files in os.walk(os.getcwd()):
            # Skip hidden directories
            dirs[:] = [d for d in dirs if not d.startswith('.')]
            for name in files:
                if fnmatch.fnmatch(name, pattern):
                    results.append(os.path.join(root, name))
        return results[:50] # Limit results

    def _generateTreeStructure(self, paths: List[str]) -> str:
        if not paths:
            return ""
        
        # Sort paths to ensure tree structure is correct
        paths.sort()
        
        # Determine the common root directory
        if len(paths) == 1:
            commonRoot = os.path.dirname(paths[0])
        else:
            commonRoot = os.path.commonpath(paths)
            if not os.path.isdir(commonRoot):
                commonRoot = os.path.dirname(commonRoot)
        
        # If commonRoot is empty or just a drive letter, default to CWD
        if not commonRoot or commonRoot == os.path.splitdrive(os.getcwd())[0]:
             commonRoot = os.getcwd()

        treeLines = [f"{commonRoot}/"]
        
        # Helper to print tree
        class Node:
            def __init__(self, name):
                self.name = name
                self.children = {}
        
        rootNode = Node("")
        
        for path in paths:
            relPath = os.path.relpath(path, commonRoot)
            parts = relPath.split(os.sep)
            curr = rootNode
            for part in parts:
                if part not in curr.children:
                    curr.children[part] = Node(part)
                curr = curr.children[part]
        
        def buildTreeString(node, prefix="", isLast=True):
            lines = []
            if node.name:
                connector = "└── " if isLast else "├── "
                lines.append(f"{prefix}{connector}{node.name}")
                prefix += "    " if isLast else "│   "
            
            children = list(node.children.values())
            for i, child in enumerate(children):
                lines.extend(buildTreeString(child, prefix, i == len(children) - 1))
            return lines

        treeLines.extend(buildTreeString(rootNode))
        return "\n".join(treeLines)

    def logRequest(self, messages: List[Dict[str, str]]):
        logDir = "logs"
        os.makedirs(logDir, exist_ok=True)
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        logFile = os.path.join(logDir, f"void_chat_{timestamp}.json")
        
        # Create a copy and split lines for better readability
        messagesToLog = []
        for msg in messages:
            msgCopy = msg.copy()
            if "content" in msgCopy and isinstance(msgCopy["content"], str):
                # Void Enhancement: Add newlines to long system prompts in logs for readability
                if msgCopy["role"] == "system":
                     msgCopy["content"] = msgCopy["content"].replace("\\n", "\n").splitlines()
                else:
                    msgCopy["content"] = msgCopy["content"].splitlines()
            messagesToLog.append(msgCopy)

        try:
            with open(logFile, "w", encoding="utf-8") as f:
                json.dump(messagesToLog, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"{Fore.RED}[Log Error] Could not save log: {str(e)}{Style.RESET_ALL}")

    def chat(self, inputOfUser: str):
        msgSystem = {"role": "system", "content": self.getContextOfSystem()}
        if inputOfUser:
            self.historyOfMessages.append({"role": "user", "content": inputOfUser})
        
        countCycle = 0
        while countCycle < self.config.maxCycles:
            try:
                countCycle += 1
                print(f"{Fore.YELLOW}[Cycle {countCycle}/{self.config.maxCycles}] Processing...{Style.RESET_ALL}")
                
                messages = [msgSystem] + self.historyOfMessages[-10:]
                estimateTokens = self.estimateTokensOfMessages(messages)
                print(f"{Fore.MAGENTA}[Token Estimate] ~{estimateTokens} tokens{Style.RESET_ALL}")

                # Log the full request (Void Feature: Auditing)
                self.logRequest(messages)

                headers = {
                    "Authorization": f"Bearer {self.config.apiKey}",
                    "Content-Type": "application/json"
                }
                payload = {
                    "model": self.config.modelName,
                    "messages": messages,
                    "temperature": 0.1,
                    "stream": True,
                    "max_tokens": 8000
                }

                replyFull = ""
                print(f"{Fore.GREEN}[VoidAgent]: ", end="")
                try:
                    response = requests.post(
                        self.endpointOfChat,
                        headers=headers,
                        json=payload,
                        stream=True,
                        timeout=30
                    )
                    response.raise_for_status()

                    for line in response.iter_lines():
                        if not line: continue
                        line = line.decode("utf-8").strip()
                        if line.startswith("data: "): line = line[6:]
                        if line in ("", "[DONE]"): continue
                        import json
                        dataChunk = json.loads(line)
                        token = dataChunk["choices"][0]["delta"].get("content", "")
                        if token:
                            replyFull += token
                            print(token, end="", flush=True)
                except requests.exceptions.RequestException as e:
                    msgError = f"{Fore.RED}[Request Error] {str(e)}{Style.RESET_ALL}"
                    print(msgError)
                    replyFull = msgError
                    break
                finally:
                    print("\n" + "-"*40)

                self.historyOfMessages.append({"role": "assistant", "content": replyFull})
                tasks = self.parseStackOfTags(replyFull)

                if not tasks:
                    keywordsTool = ["write", "read", "run", "command", "file", "search"]
                    if any(kw in replyFull.lower() for kw in keywordsTool):
                        feedbackError = "ERROR: Invalid Format! Output MAX 1 closed tag. No tag if no task."
                        self.historyOfMessages.append({"role": "user", "content": feedbackError})
                    else:
                        break

                print(f"{Fore.YELLOW}[Pending Task] {tasks[0]}{Style.RESET_ALL}")
                
                # Check whitelist logic (Void Optimization)
                t = tasks[0]
                isWhitelisted = False
                
                if t["type"] in self.config.whitelistedTools:
                    isWhitelisted = True
                elif t["type"] == "run_command":
                    cmd = t["command"].strip()
                    # Simple check: command starts with whitelisted command
                    # (In a real scenario, this should be more robust)
                    baseCmd = cmd.split()[0] if cmd else ""
                    if baseCmd in self.config.whitelistedCommands:
                        isWhitelisted = True
                
                if isWhitelisted:
                    print(f"{Fore.GREEN}>> Auto-approved whitelisted task.{Style.RESET_ALL}")
                    confirm = "y"
                else:
                    confirm = input(f"{Style.BRIGHT}Execute this task? (y/n): ")
                
                if confirm.lower() != "y":
                    print(f"{Fore.BLUE}User cancelled execution{Style.RESET_ALL}")
                    self.historyOfMessages.append({"role": "user", "content": "User cancelled execution"})
                    break

                observations = []
                t = tasks[0]
                
                if t["type"] == "search_files":
                    pattern = t["pattern"]
                    print(f"{Fore.CYAN}[Exec] Searching files: {pattern}")
                    try:
                        results = self.searchFiles(pattern)
                        if results:
                            observations.append(f"SUCCESS: Found {len(results)} files:\n" + "\n".join(results))
                        else:
                            observations.append(f"SUCCESS: No files found matching {pattern}")
                    except Exception as e:
                        observations.append(f"FAILURE: {str(e)}")

                elif t["type"] == "write_file":
                    path = os.path.abspath(t["path"])
                    content = t["content"]
                    print(f"{Fore.CYAN}[Exec] Writing file: {path}")
                    try:
                        self.backupFile(path)
                        os.makedirs(os.path.dirname(path), exist_ok=True)
                        # Use utf-8 for writing, which is standard for code
                        added, deleted = self.calculateDiffOfLines(path, content)
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write(content)
                        self.historyOfOperations.append((path, added, deleted))
                        observations.append(f"SUCCESS: Saved to {path} | +{added} | -{deleted}")
                    except Exception as e:
                        observations.append(f"FAILURE: {str(e)}")

                elif t["type"] == "read_file":
                    path = os.path.abspath(t["path"])
                    startLine = t.get("start_line", 1)
                    endLine = t.get("end_line")
                    print(f"{Fore.CYAN}[Exec] Reading file: {path}")
                    try:
                        contentRaw = ""
                        try:
                            with open(path, 'r', encoding='utf-8') as f:
                                linesAll = [line.rstrip('\n') for line in f.readlines()]
                        except UnicodeDecodeError:
                            # Fallback to system encoding or binary
                            try:
                                with open(path, 'r', encoding=locale.getpreferredencoding()) as f:
                                    linesAll = [line.rstrip('\n') for line in f.readlines()]
                            except Exception:
                                # Final fallback: read as binary and replace errors
                                with open(path, 'r', encoding='utf-8', errors='replace') as f:
                                    linesAll = [line.rstrip('\n') for line in f.readlines()]
                                    
                        totalLines = len(linesAll)
                        actualEnd = endLine if endLine and endLine <= totalLines else totalLines
                        linesTarget = linesAll[startLine-1 : actualEnd]
                        content = "\n".join(linesTarget)
                        observations.append(
                            f"SUCCESS: Read {path}\n"
                            f"Lines: {totalLines} | Range: {startLine}-{actualEnd}\n"
                            f"Content:\n{content}"
                        )
                    except Exception as e:
                        observations.append(f"FAILURE: {str(e)}")

                elif t["type"] == "run_command":
                    cmd = t["command"]
                    dangerousCmds = ["rm -rf", "format", "del /f/s/q", "mkfs"]
                    if any(d in cmd.lower() for d in dangerousCmds):
                        observations.append(f"FAILURE: Dangerous command blocked: {cmd}")
                    else:
                        print(f"{Fore.CYAN}[Exec] Running command: {cmd}")
                        try:
                            # Capture bytes to handle potential encoding issues (Void Logic: Robustness)
                            result = subprocess.run(
                                cmd, shell=True, capture_output=True, text=False
                            )
                            
                            def decodeBytes(b: bytes) -> str:
                                try:
                                    return b.decode('utf-8')
                                except UnicodeDecodeError:
                                    try:
                                        return b.decode(locale.getpreferredencoding())
                                    except Exception:
                                        return b.decode('utf-8', errors='replace')
                            
                            stdoutStr = decodeBytes(result.stdout)
                            stderrStr = decodeBytes(result.stderr)
                            
                            output = f"Stdout:\n{stdoutStr}\nStderr:\n{stderrStr}"
                            observations.append(f"SUCCESS: Command executed\n{output}")
                        except Exception as e:
                            observations.append(f"FAILURE: {str(e)}")

                if observations:
                    self.historyOfMessages.append({"role": "user", "content": "\n".join(observations)})
            
            except Exception as e:
                print(f"{Fore.RED}[Cycle Error] {str(e)}{Style.RESET_ALL}")
                self.historyOfMessages.append({"role": "user", "content": f"ERROR: Agent crashed with error: {str(e)}"})
                break
        
        if countCycle >= self.config.maxCycles:
            print(f"{Fore.RED}[Tip] Max cycles reached.{Style.RESET_ALL}")
        
        self.printStatsOfModification()

if __name__ == "__main__":
    # Load config from env or use defaults (Theme 6: System Config)
    apiKey = os.environ.get("VOID_API_KEY", "sk-45021a61b84a4693a1db4deb72cec673")
    baseUrl = os.environ.get("VOID_BASE_URL", "https://api.deepseek.com")
    modelName = os.environ.get("VOID_MODEL", "deepseek-chat")
    
    config = Config(apiKey=apiKey, baseUrl=baseUrl, modelName=modelName)
    agent = VoidAgent(config)
    
    while True:
        try:
            inputOfUser = input(f"\n{Style.BRIGHT}VoidUser: ")
            if inputOfUser.strip().lower() == "rollback":
                agent.rollbackLastOperation()
                continue
            if inputOfUser.strip().lower() in ["exit", "quit"]:
                break
            agent.chat(inputOfUser)
        except KeyboardInterrupt:
            print(f"\n{Fore.BLUE}VoidAgent Exiting...{Style.RESET_ALL}")
            break
