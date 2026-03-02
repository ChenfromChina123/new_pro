package com.aispring.service.impl;

import com.aispring.dto.PrdStateDto;
import com.aispring.service.AiChatService;
import com.aispring.service.PrdPipelineService;
import com.aispring.service.PrdStreamHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * PRD 流水线服务实现
 * 复刻 prd 项目 pipeline：Outline → Draft → Critique（最多 MAX_REVISIONS 轮修订）→ Complete
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PrdPipelineServiceImpl implements PrdPipelineService {

    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int MAX_REVISIONS = 3;
    private static final String APPROVAL_PHRASE = "No issues found.";

    private static final String OUTLINE_PROMPT = """
        You are a world-class product manager. Your task is to create a structured
        outline for a Product Requirements Document (PRD) based on a given project idea.

        The outline should cover all standard sections of a PRD, including:
        1.  Executive Summary
        2.  Problem Statement & User Personas
        3.  Goals & Success Metrics
        4.  Functional Requirements (Features)
        5.  Non-Functional Requirements (Performance, Security, etc.)
        6.  Out-of-Scope Items
        7.  Risks & Mitigations

        Please generate a Markdown-formatted outline for the following project idea:

        **Project Idea:** "%s"

        **Instructions:**
        - Use Markdown headings (#, ##, ###) to structure the document.
        - For each section, include a brief, one-sentence placeholder description of what it will contain.
        - Do NOT write the full content of the PRD yet. Just the outline.
        """;

    private static final String DRAFT_PROMPT = """
        You are a world-class product manager. Your task is to expand a given PRD outline into a full first draft.

        Use the provided outline and flesh out each section with detailed, clear, and concise content. Make reasonable assumptions where necessary, but clearly state them.

        **PRD Outline to Draft:**
        ```markdown
        %s
        ```

        **Instructions:**
        - Write comprehensive content for every section of the outline.
        - Use clear and professional language.
        - Format the output as a complete Markdown document.
        - Ensure the functional requirements are specific and actionable.
        - The draft should be complete enough for a stakeholder to understand the entire scope of the project.
        """;

    private static final String CRITIQUE_PROMPT = """
        You are a meticulous and critical product manager. Your task is to review a draft of a Product Requirements Document (PRD) and provide constructive feedback.

        Analyze the following PRD draft for clarity, completeness, coherence, and realism. Identify any ambiguities, contradictions, or missing information.

        **PRD Draft to Critique:**
        ```markdown
        %s
        ```

        **Instructions:**
        - Provide your critique as a list of bullet points.
        - For each point, specify the section of the PRD it refers to.
        - Focus on actionable feedback that can be used to improve the document.
        - Be ruthless but fair. The goal is to make the PRD as strong as possible.
        - **If the PRD is well-structured, clear, and comprehensive with no obvious issues, you MUST respond with the exact phrase "No issues found."**
        - Do not add any other text or formatting if you are approving the document.
        """;

    private static final String REVISE_PROMPT = """
        You are a world-class product manager. Your task is to revise a Product Requirements Document (PRD) draft based on a set of critiques.

        Carefully review the original draft and the provided feedback. Update the PRD to address all the points raised in the critique.

        **Original PRD Draft:**
        ```markdown
        %s
        ```

        **Critique to Address:**
        ```
        %s
        ```

        **Instructions:**
        - Produce a new, complete version of the PRD in Markdown format.
        - Incorporate all the suggested changes from the critique.
        - Ensure the revised document is coherent and consistent.
        - Do not include the critique in the final output. Only the revised PRD.
        """;

    private final AiChatService aiChatService;
    private final PrdStreamHolder streamHolder;

    @Override
    @Async
    public void runPipelineAsync(String runId, String idea, Long userId) {
        log.info("PRD pipeline started: runId={}, userId={}", runId, userId);
        String content = "# PRD for " + idea + "\n\n*Initial state.*";
        int revision = 0;

        try {
            // 推送初始状态（Outline）
            sendState(runId, "Outline", content, revision, null);

            // Step 1: Outline → 生成大纲
            String outlinePrompt = String.format(OUTLINE_PROMPT, idea);
            content = aiChatService.ask(outlinePrompt, null, DEFAULT_MODEL, userId);
            if (content == null || content.isBlank()) {
                content = "# PRD for " + idea + "\n\n*Outline step returned empty.*";
            }
            revision++;
            sendState(runId, "Draft", content, revision, null);

            // Step 2: Draft → 根据大纲写初稿
            String draftPrompt = String.format(DRAFT_PROMPT, content);
            content = aiChatService.ask(draftPrompt, null, DEFAULT_MODEL, userId);
            if (content == null || content.isBlank()) {
                content = "# PRD for " + idea + "\n\n*Draft step returned empty.*";
            }
            revision++;
            sendState(runId, "Critique", content, revision, null);

            // Step 3: Critique 循环（最多 MAX_REVISIONS 轮）
            for (int i = 0; i < MAX_REVISIONS; i++) {
                log.info("Running critique step (Revision {}/{}).", i + 1, MAX_REVISIONS);
                String critiquePrompt = String.format(CRITIQUE_PROMPT, content);
                String critique = aiChatService.ask(critiquePrompt, null, DEFAULT_MODEL, userId);
                if (critique == null) {
                    critique = "";
                }

                if (critique.contains(APPROVAL_PHRASE)) {
                    log.info("PRD approved. Exiting revision loop.");
                    break;
                }

                String contentWithCritique = content + "\n\n---\n\n## Critique\n\n" + critique;
                revision++;
                sendState(runId, "Critique", contentWithCritique, revision, null);

                log.info("Running revise step (Revision {}/{}).", i + 1, MAX_REVISIONS);
                String revisePrompt = String.format(REVISE_PROMPT, content, critique);
                content = aiChatService.ask(revisePrompt, null, DEFAULT_MODEL, userId);
                if (content == null || content.isBlank()) {
                    content = contentWithCritique;
                }
                revision++;
                sendState(runId, "Critique", content, revision, null);
            }

            // 完成
            sendState(runId, "Complete", content, revision, null);
            log.info("PRD pipeline complete: runId={}", runId);
        } catch (Exception e) {
            log.error("PRD pipeline error: runId=" + runId, e);
            String errorContent = content + "\n\n---\n\n**Pipeline Error:**\n`" + e.getMessage() + "`";
            sendState(runId, "Error", errorContent, revision + 1, null);
        } finally {
            streamHolder.complete(runId);
        }
    }

    /**
     * 构建状态并推送到 SSE
     */
    private void sendState(String runId, String step, String content, int revision, String diff) {
        PrdStateDto state = PrdStateDto.builder()
                .step(step)
                .content(content)
                .revision(revision)
                .diff(diff)
                .build();
        streamHolder.send(runId, state);
    }
}
