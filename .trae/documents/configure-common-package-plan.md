# 配置 Common 包用于存储项目常量计划

## 项目现状分析

### 现有结构
- 已存在 `constant` 包，包含 `DateTimeConstants.java`（日期时间常量）
- 项目中散落大量常量定义，分布在多个类中

### 散落的常量分类

| 类别 | 位置 | 常量示例 |
|------|------|----------|
| Redis 缓存 | RedisCacheService, VocabularyService, UrlFilterServiceImpl | `chat:messages:`, `chat:session:`, `public_word:` |
| 限流 | RateLimitService, PlaygroundController | `chat_limit:`, `playground_limit:` |
| 代码执行 | CodeExecutionServiceImpl | `MAX_OUTPUT_LENGTH`, 文件名映射 |
| PRD 生成 | PrdPipelineServiceImpl | `DEFAULT_MODEL`, `MAX_REVISIONS`, 提示词模板 |
| URL 处理 | UrlContentServiceImpl | `DEFAULT_MAX_CHARS`, `TIMEOUT_MS`, `USER_AGENT` |
| 聊天记录 | ChatRecordController | `IMAGE_META_PREFIX`, `MAX_IMAGE_COUNT` |
| 模型配置 | AgentServiceImpl | `DEFAULT_MODEL` |
| 正则模式 | SearchInstructionHandler | `SEARCH_PATTERN`, `URL_PATTERN` |
| **AI 提示词** | 多个服务类 | PRD、翻译、发音评测、语义搜索等提示词 |

### AI 提示词详细清单

| 服务类 | 提示词用途 | 提示词类型 |
|--------|-----------|-----------|
| PrdPipelineServiceImpl | PRD 大纲生成 | OUTLINE_PROMPT |
| PrdPipelineServiceImpl | PRD 初稿生成 | DRAFT_PROMPT |
| PrdPipelineServiceImpl | PRD 评审 | CRITIQUE_PROMPT |
| PrdPipelineServiceImpl | PRD 修订 | REVISE_PROMPT |
| AiVocabServiceImpl | 发音评测 | systemPrompt |
| AiVocabServiceImpl | 拼写评测 | systemPrompt |
| TranslationServiceImpl | 翻译 | systemPrompt |
| SemanticSearchServiceImpl | 语义搜索关键词扩展 | prompt |
| SessionMetadataService | 会话标题和建议生成 | buildSystemPrompt() |

## 实施计划

### 第一步：创建 common 包结构

在 `com.aispring` 下创建 `common` 包，包含以下常量类：

```
com.aispring.common/
├── CacheConstants.java         # Redis 缓存相关常量
├── RateLimitConstants.java     # 限流相关常量
├── CodeExecutionConstants.java # 代码执行相关常量
├── UrlConstants.java           # URL 处理相关常量
├── ChatConstants.java          # 聊天相关常量
├── ModelConstants.java         # 模型配置常量
├── RegexConstants.java         # 正则表达式常量
├── DateTimeConstants.java      # 从 constant 包迁移
└── prompt/                     # AI 提示词子包
    ├── PrdPromptConstants.java # PRD 生成提示词
    ├── VocabPromptConstants.java # 词汇评测提示词
    ├── TranslationPromptConstants.java # 翻译提示词
    ├── SearchPromptConstants.java # 搜索相关提示词
    └── SessionPromptConstants.java # 会话元数据提示词
```

### 第二步：创建各常量类

#### 1. CacheConstants.java
```java
package com.aispring.common;

/**
 * Redis 缓存相关常量
 */
public final class CacheConstants {
    private CacheConstants() {}
    
    // 聊天消息缓存
    public static final String MESSAGES_CACHE_PREFIX = "chat:messages:";
    public static final String SESSION_CACHE_PREFIX = "chat:session:";
    
    // URL 过滤规则缓存
    public static final String RULES_CACHE = "urlFilterRules";
    
    // 词汇缓存
    public static final String PUBLIC_WORD_CACHE_PREFIX = "public_word:";
    public static final String PUBLIC_WORDS_SEARCH_CACHE_PREFIX = "public_words_search:";
    
    // 缓存过期时间
    public static final long CACHE_DURATION_HOURS = 24;
}
```

#### 2. RateLimitConstants.java
```java
package com.aispring.common;

/**
 * 限流相关常量
 */
public final class RateLimitConstants {
    private RateLimitConstants() {}
    
    // 聊天限流
    public static final String CHAT_LIMIT_PREFIX = "chat_limit:";
    public static final int CHAT_MAX_REQUESTS = 5;
    public static final long CHAT_EXPIRATION_HOURS = 24;
    
    // Playground 限流
    public static final String PLAYGROUND_LIMIT_PREFIX = "playground_limit:";
}
```

#### 3. CodeExecutionConstants.java
```java
package com.aispring.common;

import java.util.Map;

/**
 * 代码执行相关常量
 */
public final class CodeExecutionConstants {
    private CodeExecutionConstants() {}
    
    public static final int MAX_OUTPUT_LENGTH = 102400; // 100KB
    
    // 语言对应文件名映射
    public static final Map<String, String> FILE_NAME_MAP = Map.ofEntries(
        Map.entry("python", "main.py"),
        Map.entry("javascript", "main.js"),
        Map.entry("typescript", "main.ts"),
        Map.entry("java", "Main.java"),
        Map.entry("c", "main.c"),
        Map.entry("cpp", "main.cpp"),
        Map.entry("csharp", "main.cs"),
        Map.entry("go", "main.go"),
        Map.entry("rust", "main.rs"),
        Map.entry("ruby", "main.rb"),
        Map.entry("php", "main.php"),
        Map.entry("swift", "main.swift"),
        Map.entry("kotlin", "main.kt")
    );
}
```

#### 4. UrlConstants.java
```java
package com.aispring.common;

/**
 * URL 处理相关常量
 */
public final class UrlConstants {
    private UrlConstants() {}
    
    public static final int DEFAULT_MAX_CHARS = 5000;
    public static final int TIMEOUT_MS = 15000;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
}
```

#### 5. ChatConstants.java
```java
package com.aispring.common;

/**
 * 聊天相关常量
 */
public final class ChatConstants {
    private ChatConstants() {}
    
    // 图片相关
    public static final String IMAGE_META_PREFIX = "IMG_META_JSON:";
    public static final int MAX_IMAGE_COUNT = 3;
    public static final int MAX_IMAGE_ITEM_LENGTH = 14000;
    public static final int MAX_IMAGE_META_LENGTH = 50000;
}
```

#### 6. ModelConstants.java
```java
package com.aispring.common;

/**
 * AI 模型相关常量
 */
public final class ModelConstants {
    private ModelConstants() {}
    
    public static final String DEFAULT_MODEL = "deepseek-v3";
    public static final String DEEPSEEK_V3 = "deepseek-v3";
    public static final String DEEPSEEK_R1 = "deepseek-r1";
    public static final String DEEPSEEK_CODER = "deepseek-coder";
}
```

#### 7. RegexConstants.java
```java
package com.aispring.common;

import java.util.regex.Pattern;

/**
 * 正则表达式常量
 */
public final class RegexConstants {
    private RegexConstants() {}
    
    public static final Pattern SEARCH_PATTERN = Pattern.compile(
        "<search(?:\\s+site=\"([^\"]+)\")?>(.*?)</search>", Pattern.DOTALL);
    public static final Pattern URL_PATTERN = Pattern.compile(
        "<fetch-url>(.*?)</fetch-url>", Pattern.DOTALL);
    public static final Pattern VOCAB_PATTERN = Pattern.compile(
        "<query-vocab\\s+topic=\"([^\"]+)\"\\s+limit=\"(\\d+)\"\\s*/>", Pattern.DOTALL);
}
```

#### 8. PrdPromptConstants.java（AI 提示词）
```java
package com.aispring.common.prompt;

/**
 * PRD 生成相关提示词常量
 */
public final class PrdPromptConstants {
    private PrdPromptConstants() {}
    
    public static final int MAX_REVISIONS = 3;
    public static final String APPROVAL_PHRASE = "No issues found.";
    
    /**
     * PRD 大纲生成提示词
     */
    public static final String OUTLINE_PROMPT = """
        You are a world-class product manager. Your task is to create a structured
        outline for a Product Requirements Document (PRD) based on a given project idea.
        ...
        """;
    
    /**
     * PRD 初稿生成提示词
     */
    public static final String DRAFT_PROMPT = """
        You are a world-class product manager. Your task is to expand a given PRD outline into a full first draft.
        ...
        """;
    
    /**
     * PRD 评审提示词
     */
    public static final String CRITIQUE_PROMPT = """
        You are a meticulous and critical product manager. Your task is to review a draft of a Product Requirements Document (PRD) and provide constructive feedback.
        ...
        """;
    
    /**
     * PRD 修订提示词
     */
    public static final String REVISE_PROMPT = """
        You are a world-class product manager. Your task is to revise a Product Requirements Document (PRD) draft based on a set of critiques.
        ...
        """;
}
```

#### 9. VocabPromptConstants.java（AI 提示词）
```java
package com.aispring.common.prompt;

/**
 * 词汇评测相关提示词常量
 */
public final class VocabPromptConstants {
    private VocabPromptConstants() {}
    
    /**
     * 发音评测系统提示词
     */
    public static final String SPEECH_EVALUATION_PROMPT = """
        你是一位专业的英语发音教练。请根据用户的【实际发音识别结果】和【目标文本】进行对比分析。
        请严格输出 JSON 格式，不要输出其他 Markdown 标记，包含以下字段：
        score: 发音得分 (0-100，可参考基础分，酌情上下浮动)
        aiFeedback: 对发音的简短评价（用 1 到 2 句话指出主要问题或给予鼓励，必须简明扼要）
        weakWords: 读得不准的单词数组
        """;
    
    /**
     * 拼写评测系统提示词
     */
    public static final String SPELLING_EVALUATION_PROMPT = """
        你是一位词汇记忆专家。用户拼写单词出现了错误。
        请分析用户的拼写错误，找出典型错误原因，并提供简短的记忆方法。
        请严格输出 JSON 格式，不要输出其他 Markdown 标记，包含以下字段：
        aiFeedback: 对拼写错误的简明分析和记忆建议（用1到2句话说明即可，必须简短）
        tags: 错误类型标签数组（如 ["spelling_error", "vowel_confusion"]）
        """;
}
```

#### 10. TranslationPromptConstants.java（AI 提示词）
```java
package com.aispring.common.prompt;

/**
 * 翻译相关提示词常量
 */
public final class TranslationPromptConstants {
    private TranslationPromptConstants() {}
    
    /**
     * 翻译系统提示词
     */
    public static final String TRANSLATION_SYSTEM_PROMPT = 
        "You are a professional translator. Your task is to translate text accurately while maintaining the original meaning and tone.";
    
    /**
     * 翻译用户提示词模板
     */
    public static final String TRANSLATION_USER_PROMPT_TEMPLATE = 
        "Please translate the following text%s to %s.\n" +
        "Only provide the translated text, no explanations or additional content.\n\n" +
        "Text to translate:\n%s";
}
```

#### 11. SearchPromptConstants.java（AI 提示词）
```java
package com.aispring.common.prompt;

/**
 * 搜索相关提示词常量
 */
public final class SearchPromptConstants {
    private SearchPromptConstants() {}
    
    /**
     * 语义搜索关键词扩展提示词模板
     */
    public static final String SEMANTIC_SEARCH_PROMPT_TEMPLATE = """
        你是一个英语教育专家。用户想学习与"%s"相关的英语单词。
        请列出 %d 个最相关、最常用的英语单词（名词、动词、形容词等）。
        要求：
        1. 只返回英语单词，每行一个
        2. 不要包含中文、标点符号或其他解释
        3. 单词应该是基础到中等难度的常用词
        4. 确保单词与主题高度相关
        
        示例格式：
        technology
        computer
        software
        internet
        """;
}
```

#### 12. SessionPromptConstants.java（AI 提示词）
```java
package com.aispring.common.prompt;

/**
 * 会话元数据相关提示词常量
 */
public final class SessionPromptConstants {
    private SessionPromptConstants() {}
    
    /**
     * 会话标题和建议生成基础提示词
     */
    public static final String SESSION_METADATA_BASE_PROMPT = 
        "你是一个中文助手，需要基于【当前用户询问】（最重要）以及【历史用户询问】（仅供参考）生成结果。\n" +
        "仅输出 JSON，不要输出任何额外文字（包括 Markdown/代码块）。\n" +
        "请生成 3 个\"用户视角\"的下一步追问（用户对助手说的话），要求：\n" +
        "1) 每个都是完整问题，优先更具体、更可执行；\n" +
        "2) 不要以 AI 口吻表达（如\"我可以为你.../我还能...\"），不要自称\"AI/助手\"；\n" +
        "3) 不要复述历史问题，不要照抄历史原句；\n" +
        "4) 每个问题 8~25 个汉字，末尾使用\"？\"。\n";
    
    /**
     * 标题生成附加提示词
     */
    public static final String TITLE_GENERATION_SUFFIX = 
        "由于这是会话的第一条消息，请同时生成一个简短的标题（不超过15个字）。\n";
    
    /**
     * 默认建议问题
     */
    public static final String DEFAULT_SUGGESTION_1 = "我下一步应该先做什么？";
    public static final String DEFAULT_SUGGESTION_2 = "你能给我一个可执行的步骤清单吗？";
    public static final String DEFAULT_SUGGESTION_3 = "有哪些常见坑需要我提前避免？";
}
```

### 第三步：迁移 DateTimeConstants

将 `com.aispring.constant.DateTimeConstants` 迁移到 `com.aispring.common.DateTimeConstants`

### 第四步：更新所有引用

更新以下文件中的常量引用：

1. **RedisCacheService.java** - 使用 `CacheConstants`
2. **VocabularyService.java** - 使用 `CacheConstants`
3. **UrlFilterServiceImpl.java** - 使用 `CacheConstants`
4. **RateLimitService.java** - 使用 `RateLimitConstants`
5. **PlaygroundController.java** - 使用 `RateLimitConstants`
6. **CodeExecutionServiceImpl.java** - 使用 `CodeExecutionConstants`
7. **PrdPipelineServiceImpl.java** - 使用 `PrdPromptConstants`, `ModelConstants`
8. **UrlContentServiceImpl.java** - 使用 `UrlConstants`
9. **ChatRecordController.java** - 使用 `ChatConstants`
10. **AgentServiceImpl.java** - 使用 `ModelConstants`
11. **SearchInstructionHandler.java** - 使用 `RegexConstants`
12. **ChatRecordService.java** - 使用 `DateTimeConstants`
13. **ChatRecord.java** - 使用 `DateTimeConstants`
14. **AiVocabServiceImpl.java** - 使用 `VocabPromptConstants`, `ModelConstants`
15. **TranslationServiceImpl.java** - 使用 `TranslationPromptConstants`
16. **SemanticSearchServiceImpl.java** - 使用 `SearchPromptConstants`
17. **SessionMetadataService.java** - 使用 `SessionPromptConstants`, `ModelConstants`

### 第五步：删除旧的 constant 包

确认所有引用更新后，删除 `com.aispring.constant` 包

### 第六步：验证

- 运行 Maven 编译确保无错误
- 提交代码到 Git

## 文件变更清单

### 新增文件
- `src/main/java/com/aispring/common/CacheConstants.java`
- `src/main/java/com/aispring/common/RateLimitConstants.java`
- `src/main/java/com/aispring/common/CodeExecutionConstants.java`
- `src/main/java/com/aispring/common/UrlConstants.java`
- `src/main/java/com/aispring/common/ChatConstants.java`
- `src/main/java/com/aispring/common/ModelConstants.java`
- `src/main/java/com/aispring/common/RegexConstants.java`
- `src/main/java/com/aispring/common/DateTimeConstants.java`
- `src/main/java/com/aispring/common/prompt/PrdPromptConstants.java`
- `src/main/java/com/aispring/common/prompt/VocabPromptConstants.java`
- `src/main/java/com/aispring/common/prompt/TranslationPromptConstants.java`
- `src/main/java/com/aispring/common/prompt/SearchPromptConstants.java`
- `src/main/java/com/aispring/common/prompt/SessionPromptConstants.java`

### 修改文件
- `RedisCacheService.java`
- `VocabularyService.java`
- `UrlFilterServiceImpl.java`
- `RateLimitService.java`
- `PlaygroundController.java`
- `CodeExecutionServiceImpl.java`
- `PrdPipelineServiceImpl.java`
- `UrlContentServiceImpl.java`
- `ChatRecordController.java`
- `AgentServiceImpl.java`
- `SearchInstructionHandler.java`
- `ChatRecordService.java`
- `ChatRecord.java`
- `AiVocabServiceImpl.java`
- `TranslationServiceImpl.java`
- `SemanticSearchServiceImpl.java`
- `SessionMetadataService.java`

### 删除文件
- `src/main/java/com/aispring/constant/DateTimeConstants.java`
- `src/main/java/com/aispring/constant/` 目录

## 设计原则遵循

1. **单一职责原则**：每个常量类只负责一类常量
2. **DRY 原则**：消除重复的常量定义
3. **KISS 原则**：保持常量类简洁，仅包含静态常量
4. **开闭原则**：提示词独立为 prompt 子包，便于扩展新的提示词类型
