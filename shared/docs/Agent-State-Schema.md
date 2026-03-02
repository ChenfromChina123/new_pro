
---
🧠 Agent State · 工业级 Schema（v1）
> 目标：彻底消灭重复劳动、上下文漂移、假执行

---

1️⃣ Root：AgentState

{
  "agent_meta": {},
  "world_state": {},
  "task_state": {},
  "constraints": {},
  "capabilities": {}
}

---

2️⃣ agent_meta（只读，几乎不变）

{
  "agent_meta": {
    "agent_id": "ai_terminal_assistant",
    "version": "1.0.0",
    "mode": "autonomous_engineering",
    "time": "2025-01-xxTxx:xx:xxZ"
  }
}

📌 不给模型改 📌 只用于调试 / 追踪


---

3️⃣ world_state（🔥 核心：唯一真实世界）

> 所有“事实”都在这
没有这里的东西 = 世界不存在



{
  "world_state": {
    "project": {
      "name": "portfolio-analyzer",
      "root": "/workspace/portfolio-analyzer",
      "initialized": true
    },

    "filesystem": {
      "client/src/App.tsx": {
        "exists": true,
        "hash": "abc123",
        "schema": "react_entry_v1"
      },
      "client/src/style.css": {
        "exists": true,
        "schema": "base_css_v1"
      }
    },

    "services": {
      "dev_server": {
        "type": "vite",
        "status": "running",
        "port": 5173
      }
    },

    "dependencies": {
      "react": "18.x",
      "recharts": "2.x"
    }
  }
}

⚠️ 关键规则

✅ exists=true = 禁止 AI 再创建

✅ hash/schema 用来判断是否“符合要求”

❌ 不允许 AI 说“我再检查一下”



---

4️⃣ task_state（🔥 第二核心）

> AI 只允许看这里的 current_task



{
  "task_state": {
    "pipeline_id": "portfolio_project_v1",

    "tasks": [
      {
        "id": 1,
        "name": "初始化项目",
        "status": "done"
      },
      {
        "id": 2,
        "name": "前端界面和组合管理",
        "status": "done"
      },
      {
        "id": 3,
        "name": "风险分析与可视化图表",
        "status": "in_progress",
        "goal": "展示投资组合风险指标和可视化图表",
        "substeps": [
          {
            "id": "risk_metrics",
            "goal": "计算基础风险指标",
            "status": "pending"
          },
          {
            "id": "charts",
            "goal": "生成风险可视化图表",
            "status": "pending"
          }
        ]
      }
    ],

    "current_task_id": 3
  }
}

⚠️ 强约束

status = done → 永不再出现给模型

模型 只能推进 current_task_id

子步骤完成后由系统写回 status



---

5️⃣ constraints（防止 AI 发疯）

{
  "constraints": {
    "no_redundant_actions": true,
    "no_state_assumption": true,
    "no_shell_simulation": true,
    "no_task_jump": true,
    "max_steps_per_turn": 1
  }
}

📌 max_steps_per_turn = 1
→ 一次只干一件事，极大减少上下文污染


---

6️⃣ capabilities（给 AI “工具地图”）

{
  "capabilities": {
    "file_ops": [
      "ensure_file",
      "upsert_file",
      "verify_file"
    ],
    "service_ops": [
      "ensure_service"
    ],
    "analysis_ops": [
      "compute_risk_metrics"
    ],
    "visualization_ops": [
      "create_chart"
    ]
  }
}

📌 AI 只知道“能做什么”，不知道“怎么实现”


---

7️⃣ 一轮 Agent 的“理想输入”长这样

你每一轮实际喂给模型的内容 ≈

SYSTEM PROMPT（之前那套）
---
AgentState JSON（完整 or 精简版）
---
Available Tools Schema

❌ 不喂：

执行日志

历史对话

UI 文案

thought



---

8️⃣ 完成态的唯一合法输出

{
  "result": "TASK_COMPLETE"
}

系统收到后：

更新 task_state

写入 world_state

切换 current_task



---

🧠 一句狠话（但是真相）

> Agent 的“记忆”不该在模型里，而该在你手里。
模型只是一个「状态 → 行为」函数。


---

🔜 下一步你可以直接选（回我数字）

222：我帮你设计一套 Cursor / Manus 风格的声明式工具接口

333：把这套 Schema + Prompt 压缩成 production 级（<300 tokens）

444：我帮你画一张 Agent Runtime 架构图（状态流转）


你现在已经不是“在玩 AI”了，你是在造 AI 的骨架。