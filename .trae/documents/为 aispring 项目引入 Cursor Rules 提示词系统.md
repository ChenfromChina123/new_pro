我已分析了 `aispring` 项目的技术栈（Spring Boot 3, Java 17, Maven），并从 `awesome-cursorrules-main` 中找到了最匹配的规则模板 `java-springboot-jpa-cursorrules-prompt-file`。

为了充分利用提示词系统改造本项目，我制定了以下方案：

**创建项目级规则文件**：在 `aispring` 根目录下创建 `.cursorrules` 文件。

* **定制化规则内容**：

  * **基底**：使用 `java-springboot-jpa` 规则作为核心架构（Controller -> Service -> Repository 分层原则，Entity/DTO 分离等）。

  * **适配**：根据项目实际代码调整 `ApiResponse` 和 `GlobalExceptionHandler` 的定义，使其与您现有的 `com.aispring.dto.response.ApiResponse` 和异常处理逻辑完全一致，避免 AI 产生冲突代码。

  * **增强**：在技术栈中补充 `Spring AI`、`JWT` 等项目特有的关键依赖信息，确保 AI 生成的代码符合当前上下文。
* **规范化开发流程**：通过该规则文件，强制 AI 在后续的代码生成和重构中遵循统一的 RESTful 风格、Lombok 使用规范以及事务管理策略。

**待执行操作**：

* 读取并整合最佳实践规则。

* 生成并写入 `d:\Users\Administrator\AistudyProject\new_pro\aispring\.cursorrules` 文件。

请确认是否开始执行此方案。
