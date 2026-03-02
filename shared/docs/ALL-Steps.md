
---

🧠 AI-Project 全流程说明（工程级）

> 目标：
用户一句话 → AI 自动完成整个工程项目 → 可运行、可交付、可复用




---

总览：全流程分 7 个阶段

0. 用户意图解析
1. 项目规划（Plan）
2. Agent Runtime 初始化
3. 任务链执行（核心循环）
4. 世界状态持续对齐
5. 质量验证 & 收敛
6. 交付 & 冻结
7. 后续迭代（可选）

下面逐层拆。


---

0️⃣ 用户意图解析（Intent → Project）

输入

“帮我做一个投资组合风险分析平台”

内部流程

1. 意图分类

类型：engineering_project

复杂度：multi-stage

是否需要长期状态：true



2. 目标抽象



{
  "project_goal": "构建可运行的投资组合风险分析系统",
  "success_criteria": [
    "前端可访问",
    "能输入组合",
    "能输出风险指标和图表"
  ]
}

3. 生成 Project ID



portfolio_project_v1

📌 这一阶段不生成代码


---

1️⃣ 项目规划阶段（Planner Agent）

> 这是唯一一次允许 AI「发散思考」的地方



产物：Task Pipeline

{
  "pipeline": [
    "项目初始化",
    "数据源与API设计",
    "后端与模型逻辑",
    "前端与组合管理",
    "风险分析与可视化",
    "LLM 分析与报告",
    "测试与部署",
    "交付"
  ]
}

关键点

一旦 pipeline 确认，不允许模型自己加任务

规划完成后 → 写入 task_state

Planner Agent 退出舞台



---

2️⃣ Agent Runtime 初始化（系统接管）

初始化内容

① World State

{
  "filesystem": {},
  "services": {},
  "dependencies": {}
}

② Task State

{
  "current_task_id": 1,
  "tasks": [...]
}

③ Tool Registry

ensure_file

ensure_service

compute_xxx


📌 从这里开始：模型不再“想项目”，只“执行任务”


---

3️⃣ 核心循环：任务链执行（🔥 最重要）

> 这是你的产品真正的“心跳”



3.1 单任务生命周期

以「风险分析与可视化」为例：

输入给模型

SYSTEM PROMPT
AgentState（world + current_task）
Tools Schema

模型允许做的只有三件事：

1. 判断：目标是否已满足


2. 决定：最少需要哪一步


3. 调用：一个原子工具




---

3.2 工具执行（模型外）

Model → Intent
        ↓
   Tool Executor
        ↓
 Real World Change
        ↓
 World State Update

📌 模型永远不直接接触真实世界


---

3.3 状态写回（关键）

每一步完成后：

world_state.filesystem["risk_chart.tsx"] = { "exists": true }
task_state.substeps["charts"] = "done"


---

3.4 判断任务完成

当：

所有 substeps = done

或 goal 已满足


→ 模型输出：

{ "result": "TASK_COMPLETE" }

系统切换到下一个 task。


---

4️⃣ 世界状态持续对齐（State Reconciliation）

为什么要这一层？

因为：

文件可能被用户改

服务可能崩

依赖可能丢


对齐机制

非模型线程

定期：

hash 校验

service health check



更新 world_state，而不是让模型自己查


---

5️⃣ 质量验证 & 收敛（Validation）

> 这是 Devin / Manus 显得“靠谱”的原因



验证维度

1. 功能是否可运行


2. 是否满足 success criteria


3. 是否有未完成任务



验证工具示例

verify_project({
  "checks": ["frontend_up", "charts_render"]
})

❌ 验证失败 → 回退到对应 task
✅ 全通过 → 进入交付


---

6️⃣ 交付 & 冻结（Delivery）

冻结内容

{
  "project_status": "delivered",
  "world_state_frozen": true,
  "task_state_locked": true
}

输出给用户

可运行地址

使用说明

架构摘要

后续扩展建议


📌 冻结后，模型不再自动修改项目


---

7️⃣ 后续迭代（可选）

用户说：

> “再加一个最大回撤分析”



系统行为

1. 创建 新 pipeline v2


2. 继承旧 world_state


3. 新任务追加



📌 不是“继续聊”，是“新项目版本”


---

🧠 核心设计哲学（你一定要记住）

❌ 错误模式

> 让模型记住一切、推理一切、执行一切



✅ 正确模式

> 模型 = 决策函数
系统 = 世界管理者

---

