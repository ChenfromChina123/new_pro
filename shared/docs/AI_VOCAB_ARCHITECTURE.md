# AI 单词记忆模块与对话交互结合架构设计 (RAG & 个性化进阶版)

## 1. 业务痛点与升级目标

在初始版本中，直接依赖大模型生成单词卡片暴露了以下痛点：

1. **词汇量少且发散**：AI 凭空捏造（Hallucination）的词汇往往不够系统，且每次生成的数量受限，缺乏权威词典的严谨性。
2. **缺乏用户长期记忆（UserProfile）**：AI 不知道用户之前背过哪些词，哪些词容易错，无法做到真正的“因材施教”。
3. **UI 交互体验不佳**：纵向堆叠的卡片导致聊天流过长（“上下滑动太难受”），且缺少基础的单词详细释义（Definition）。

**升级目标**：
引入 **RAG (检索增强生成)** 架构，结合 GitHub 高质量开源词库；建立用户单词记忆模型（类似艾宾浩斯记忆曲线追踪）；前端 UI 从“信息流”重构为“沉浸式轮播/弹窗”，彻底解决上述痛点。

***

## 2. 核心开源词库选型 (GitHub)

为了建立本地词库源，架构将集成以下高质量开源数据：

1. **[skywind3000/ECDICT](https://github.com/skywind3000/ECDICT)**
   - **定位**：底层大词典库（超 300 万条）。
   - **优势**：包含详尽的音标（英/美）、中文释义、英文释义、词频、以及考试标签（CET4/CET6/GRE/TOEFL 等）。
2. **[mahavivo/english-wordlists](https://github.com/mahavivo/english-wordlists)**
   - **定位**：学习大纲/词书库。
   - **优势**：结构化的高频词表、考纲词表，适合作为 AI 筛选单词的范围限定。

***

## 3. RAG 与个性化记忆架构设计

### 3.1 数据库设计 (MySQL)

需要新增或扩展两张核心表：

- `word_dict` (词典表)：导入 ECDICT 数据，包含字段 `id`, `word`, `phonetic`, `definition` (详尽释义), `translation`, `level_tags`。
- `user_word_progress` (用户记忆追踪表)：记录 `user_id`, `word_id`, `status` (未学/学习中/已掌握), `error_count` (发音/拼写错误次数), `next_review_time` (下次复习时间)。

### 3.2 改进后的 RAG 交互流 (Agentic RAG)

1. **意图识别与搜索触发**：用户发送“帮我复习咖啡相关的四级单词”。AI 不再直接生成单词，而是输出意图标签：`<query-vocab topic="coffee" level="CET4" limit="5" />`。
2. **后端拦截与本地检索 (Retrieval)**：后端解析到 `<query-vocab>`，查询本地 `word_dict`，并**关联当前用户的** **`user_word_progress`**，优先查出“该用户易错”或“待复习”的词汇。
3. **上下文注入 (Augmentation)**：后端将查出的词汇数据（含完整释义和 ID）注入回给 AI：`[系统检索到以下候选单词：[{"id": 1024, "word": "latte", "definition": "n. 拿铁...", "user_status": "易错"}]]`。
4. **生成卡片指令 (Generation)**：AI 根据检索结果，结合聊天语境生成最终的卡片结构，此时携带唯一的 `id` 和详尽的 `definition`。

***

## 4. 前端 UI 交互重构

为了解决纵向滑动难受的问题，摒弃将大型卡片直接铺在聊天流中的做法，改用 **“卡片组轮播 (Carousel)”** 或 **“专注模式弹窗 (Modal)”**。

### 4.1 UI 方案设计

- **消息流占位**：AI 在对话框中仅生成一个小巧的 **“任务概览面板”**，例如：`[ 🎯 你的专属咖啡词汇练习生成完毕 (共 5 词) | 点击开始练习 ]`。
- **沉浸式练习区 (Carousel / Swiper)**：
  用户点击后，展开一个**横向滑动**的卡片轮播（类似市面上主流的背单词 App）。每次只聚焦一个单词。
  - **上方**：大字号单词、英/美音标、详细的英/中双语 `definition`（释义）。
  - **中间**：AI 结合语境生成的定制例句 `sentence`。
  - **下方**：发音与拼写双模式的操作按钮区。

### 4.2 LLM 输出协议 (XML V2)

更新后的 XML 协议，强化了关联 ID 与详尽释义：

```xml
<vocab-practice>
  <vocab 
    id="1024" 
    word="ubiquitous" 
    phonetic="/juːˈbɪkwɪtəs/" 
    definition="adj. 似乎无处不在的，十分普遍的"
    sentence="Smartphones have become ubiquitous in modern life." 
    translation="智能手机在现代生活中已无处不在。"
    mode="pronunciation"
  />
  <!-- 更多单词... -->
</vocab-practice>
```

***

## 5. 真实 ASR 语音评测与防作弊机制

针对早期版本“未发音也给高分”的模拟评测痛点，进阶版架构必须接入真实的 ASR 引擎，并建立严格的防作弊和多维打分体系。

### 5.1 真实 ASR 引擎选型

必须废弃 `mockAsrTranscription` 模拟代码，为了降低 API 成本并保护用户隐私，推荐使用以下 **本地化开源 ASR 方案 (GitHub)**：

1. **[ggerganov/whisper.cpp](https://github.com/ggerganov/whisper.cpp)** **(极力推荐)**
   - **定位**：OpenAI Whisper 模型的 C/C++ 轻量级移植版。
   - **优势**：无需 GPU 也能在普通 CPU 上极速运行（支持 Apple Silicon / x86 AVX 等指令集加速）。支持将其封装为本地 HTTP 服务（内置 `server` 示例），后端 Spring Boot 直接向本地服务发送音频，完全免费且极速。
   - **资源要求 (CPU & 内存)**：
     - **Tiny 模型 (推荐用于单词/短句评测)**：内存占用仅约 **\~75 MB**，普通 1 核 CPU 即可实现远超实时的推理速度（如 1秒音频仅需 0.05秒处理），准确率对付单句发音绰绰有余。
     - **Base 模型**：内存占用约 **\~140 MB**，2 核 CPU 运行毫无压力，兼顾极高准确率和低延迟。
     - **Small 模型**：内存占用约 **\~480 MB**，适合需要极高上下文理解的长段落识别。
     - *结论：在我们的单词/单句记忆场景下，使用* *`tiny.en`* *或* *`base.en`* *模型，一台最基础的 1核/2G 便宜云服务器就能完美流畅运行。*
   - **部署与端口建议**：
     - 启动命令示例：`./whisper-server -m models/ggml-base.en.bin -l en --port 8090` (默认是 8080，建议修改以避免与 Tomcat 等冲突)。
     - **Nginx 反向代理**：为了在生产环境中供外部或微服务内部调用，建议在 Nginx 中配置代理：
       ```nginx
       location /api/asr/ {
           proxy_pass http://127.0.0.1:8090/;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           client_max_body_size 10M; # 允许上传音频文件
       }
       ```
2. **[m-bain/whisperX](https://github.com/m-bain/whisperX)**
   - **定位**：支持单词级时间戳（Word-level timestamps）的 Whisper 增强版。
   - **优势**：如果你想在未来实现类似“卡拉OK”那样，用户读到哪个词就高亮哪个词的进阶评测，这个项目是最好的本地化选择。
3. **[Vosk (alphacep/vosk-api)](https://github.com/alphacep/vosk-api)**
   - **定位**：离线语音识别工具包。
   - **优势**：模型体积积极小（仅需 50MB），支持多种语言，甚至可以直接跑在浏览器前端（WebAssembly），彻底省去后端的音频传输压力。
4. *(备选)* **OpenAI Whisper API / 阿里云一句话识别**：仅作为本地服务器算力不足时的云端降级方案。

### 5.2 静音检测与防作弊 (VAD)

1. **前端拦截 (Audio Context)**：在录音结束前，前端利用 Web Audio API (AnalyserNode) 计算录音期间的平均音量(Volume)。如果音量始终低于阈值，直接提示“检测到声音”，拦截未请求，避免浪费后端资源。
2. **后端空结果校验**：如果 ASR 引擎返回的结果为空字符串，或者与目标单词毫不相干（相似度极低，比如用户录了环境噪音），系统直接返回 `score: 0`，并给出反馈“未能识别到有效的英文发音”。

### 5.3 智能 LLM 打分策略 (基于 Levenshtein 距离与音素分析)

当获取到 ASR 的真实文本后：

1. **字符串相似度算法**：后端先计算 `目标单词` 与 `识别单词` 的莱文斯坦距离（编辑距离）。如果完全一致，基础分为 90+；如果差异较大，基础分为 50 以下。
2. **LLM 深度纠错**：将两个文本喂给大模型。Prompt 升级为：
   *“用户目标是读* *`ubiquitous`，但语音识别出来的结果是* *`you be quit us`。请根据这个识别误差，推测用户在哪个音节发音错误，并给出具体的口腔/舌位纠正建议，最后给出一个 0-100 的综合评分。”*

***

## 6. 后端记忆闭环

- 当用户完成 `ubiquitous` 的发音或拼写测验时，前端除了展示 AI 的点评反馈，还会将真实的评测分数和错误类型异步提交至 `/api/ai/vocab/progress`。
- 后端根据得分，更新 `user_word_progress` 表中的熟练度和下次复习时间（基于记忆曲线算法）。
- 在用户下一次请求复习时，这套数据将再次进入 RAG 检索池，实现真正的“AI 智能私教”。

