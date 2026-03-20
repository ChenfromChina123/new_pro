## 🦞 OpenClaw 视角下的 Agent Skills 定义与结构
在 OpenClaw 体系中，**Skill** 是 Agent 的**可插拔能力单元**，以**目录为最小分发单位**，包含元数据、触发规则、执行代码和文档，让 Agent 能按需发现、加载并调用具体功能。

---

### 一、OpenClaw Skills 的核心定义
OpenClaw Skill 是一个**标准化的功能包**，它将特定任务（如日志分析、内存检查）封装为可被 Agent 调用的独立模块，遵循「高内聚、低耦合」原则，支持动态扩展和组合调用。

---

### 二、标准目录结构（结合你的文件示例）
一个合法的 OpenClaw Skill 目录结构如下（与你提供的文件完全对应）：
```
your-skill-name/
├── .clawhub/          # ClawHub 平台配置（可选，用于技能发布与管理）
├── _meta.json         # 补充元数据（兼容旧版框架或额外配置）
├── SKILL.md           # ✅ 必需：技能核心定义文件（YAML + Markdown）
├── skill.json         # 技能配置（触发条件、参数、版本等，部分框架依赖）
├── check_memory.py    # 技能实现代码（Python 能力单元）
├── log_best_practice.py
├── log_correction.py
├── log_error.py
├── log.js             # 其他语言实现的技能逻辑（可选）
└── assets/            # 可选：静态资源（模板、图标、配置文件等）
```

---

### 三、核心文件格式详解

#### 1. ✅ SKILL.md（技能灵魂文件）
采用 **YAML frontmatter + Markdown 正文** 格式，是 OpenClaw 识别技能的唯一入口：
```markdown
---
name: log_analysis_skill          # 技能唯一标识（英文小写，下划线分隔）
description: 日志分析与纠错工具集，包含内存检查、最佳实践校验、错误定位等能力
version: 1.0.0
homepage: https://clawhub.dev/your-skill  # 可选：技能主页
metadata:
  {
    "openclaw": {
      "emoji": "📝",              # 技能图标（用于 UI 展示）
      "os": ["windows", "linux"], # 支持的操作系统
      "requires": ["python>=3.9"],# 依赖环境
      "user-invocable": true      # 是否允许用户直接调用
    }
  }
---

### 触发场景
当用户输入包含「日志」「内存」「错误排查」「日志优化」等关键词时，自动激活该技能。

### 功能说明
- `check_memory.py`：检查系统内存占用，定位内存泄漏风险
- `log_best_practice.py`：校验日志格式是否符合最佳实践
- `log_correction.py`：自动修正日志中的格式错误与冗余内容
- `log_error.py`：从日志中提取错误栈，定位异常根因

### 使用示例
```
用户：帮我检查一下这段日志的内存问题
Agent：调用 check_memory.py 分析内存占用，返回泄漏点与优化建议
```
```

#### 2. 📄 skill.json（技能配置文件）
用于定义**触发规则、输入输出参数和执行逻辑**，是 Agent 决策的核心依据：
```json
{
  "name": "log_analysis_skill",
  "version": "1.0.0",
  "description": "日志分析与纠错工具集",
  "trigger": {
    "keywords": ["日志", "内存", "错误", "优化", "排查"],
    "intents": ["log_analysis", "memory_check", "error_diagnosis"]
  },
  "parameters": {
    "input": {
      "type": "object",
      "properties": {
        "log_content": {"type": "string", "description": "待分析的日志内容"},
        "check_type": {"type": "string", "enum": ["memory", "best_practice", "error", "correction"], "default": "error"}
      },
      "required": ["log_content"]
    },
    "output": {
      "type": "object",
      "properties": {
        "status": {"type": "string", "enum": ["success", "failed"]},
        "data": {"type": "object", "description": "分析结果"},
        "error": {"type": "string", "description": "失败时的错误信息"}
      }
    }
  },
  "execution": {
    "type": "python",
    "entry": "check_memory.py",  # 默认执行入口
    "fallback": ["log_error.py", "log_correction.py"]  # 备选执行脚本
  }
}
```

#### 3. 🧠 技能实现代码（如 check_memory.py）
遵循 OpenClaw 规范的 Python 函数格式，输入输出为标准化字典，包含完整异常处理：
```python
def check_memory_skill(log_content: str, **kwargs) -> dict:
    """
    OpenClaw Skill：检查日志中的内存泄漏风险
    :param log_content: 待分析的日志内容
    :param kwargs: 额外参数（如 check_type 等）
    :return: 标准化输出字典
    """
    try:
        # 核心业务逻辑：解析日志，提取内存占用数据，分析泄漏点
        memory_leaks = []
        for line in log_content.split("\n"):
            if "memory leak" in line.lower():
                memory_leaks.append(line.strip())
        
        return {
            "status": "success",
            "data": {
                "leak_count": len(memory_leaks),
                "leak_details": memory_leaks,
                "suggestion": "建议检查相关代码的内存分配与释放逻辑"
            },
            "message": f"发现 {len(memory_leaks)} 处内存泄漏风险"
        }
    except Exception as e:
        return {
            "status": "failed",
            "error": f"内存检查失败：{str(e)}"
        }
```

#### 4. 📦 _meta.json 与 .clawhub/
- `_meta.json`：补充元数据（如作者、发布时间、依赖版本），兼容旧版 OpenClaw 框架。
- `.clawhub/`：ClawHub 平台的发布配置（如技能标签、权限、版本控制），用于将技能上传到 ClawHub 市场。

---

### 四、OpenClaw Skills 的生命周期
1. **发现**：OpenClaw 扫描 `skills/` 目录，解析 `SKILL.md` 识别可用技能。
2. **加载**：读取 `skill.json` 配置，验证依赖环境，注册技能到 Agent 技能库。
3. **触发**：用户输入匹配 `trigger` 规则时，Agent 选择对应技能。
4. **执行**：调用技能实现代码，传入参数，执行核心逻辑。
5. **返回**：将执行结果格式化为标准化字典，返回给 Agent 进行后续处理。

---

### 五、与通用 Agent Skills 的区别
| 维度                | 通用 Agent Skills                | OpenClaw Skills                          |
|---------------------|----------------------------------|-----------------------------------------|
| **组织形式**        | 函数/类级别                     | 目录为单位，包含完整文档与配置          |
| **核心文件**        | 无强制规范                      | 必须包含 `SKILL.md`，格式严格固定       |
| **平台集成**        | 依赖框架自行实现                | 原生支持 ClawHub 发布、版本管理与协作   |
| **触发规则**        | 自定义逻辑                      | 标准化 `trigger` 配置，支持关键词/意图  |
| **可移植性**        | 依赖框架实现                    | 目录打包即可跨环境部署                  |

---
