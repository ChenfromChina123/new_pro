package com.aispring.controller;

import com.aispring.entity.RequirementDoc;
import com.aispring.entity.RequirementDocHistory;
import com.aispring.repository.RequirementDocHistoryRepository;
import com.aispring.repository.RequirementDocRepository;
import com.aispring.security.CustomUserDetails;
import com.aispring.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementDocRepository docRepository;
    private final RequirementDocHistoryRepository historyRepository;
    private final AiChatService aiChatService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<RequirementDoc> listDocs(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return docRepository.findByUserIdOrderByUpdatedAtDesc(currentUser.getUser().getId());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public RequirementDoc createDoc(@AuthenticationPrincipal CustomUserDetails currentUser, @RequestBody RequirementDoc doc) {
        doc.setUserId(currentUser.getUser().getId());
        doc.setVersion(1);
        RequirementDoc saved = docRepository.save(doc);
        
        // Save initial history
        historyRepository.save(RequirementDocHistory.builder()
                .docId(saved.getId())
                .content(saved.getContent())
                .version(1)
                .build());
        
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public RequirementDoc updateDoc(@AuthenticationPrincipal CustomUserDetails currentUser, @PathVariable Long id, @RequestBody RequirementDoc update) {
        RequirementDoc doc = docRepository.findById(id).orElseThrow(() -> new RuntimeException("Doc not found"));
        if (!doc.getUserId().equals(currentUser.getUser().getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Save current version to history before updating? No, we saved version 1 on create.
        // We are creating version N+1.
        
        doc.setContent(update.getContent());
        doc.setTitle(update.getTitle());
        doc.setVersion(doc.getVersion() + 1);
        RequirementDoc saved = docRepository.save(doc);

        historyRepository.save(RequirementDocHistory.builder()
                .docId(saved.getId())
                .content(saved.getContent())
                .version(saved.getVersion())
                .build());

        return saved;
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public List<RequirementDocHistory> getHistory(@AuthenticationPrincipal CustomUserDetails currentUser, @PathVariable Long id) {
        RequirementDoc doc = docRepository.findById(id).orElseThrow(() -> new RuntimeException("Doc not found"));
        if (!doc.getUserId().equals(currentUser.getUser().getId())) {
            throw new RuntimeException("Unauthorized");
        }
        return historyRepository.findByDocIdOrderByVersionDesc(id);
    }

    @PostMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter generateDocStream(@AuthenticationPrincipal CustomUserDetails currentUser, @RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String fullPrompt = "请根据以下需求，生成一份详细的需求文档（Markdown格式）：\n" + prompt;
        // Use a generic model or specific one. Assuming 'deepseek-chat' as default.
        return aiChatService.askAgentStream(fullPrompt, null, "deepseek-chat", currentUser.getUser().getId().toString(), "You are a professional Product Manager.");
    }
}
