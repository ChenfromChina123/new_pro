package com.aispring.service.impl;

import com.aispring.entity.approval.ApprovalStatus;
import com.aispring.entity.approval.ToolApproval;
import com.aispring.entity.approval.UserApprovalSettings;
import com.aispring.repository.ToolApprovalRepository;
import com.aispring.repository.UserApprovalSettingsRepository;
import com.aispring.service.ToolApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 工具批准服务实现
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolApprovalServiceImpl implements ToolApprovalService {
    
    private final ToolApprovalRepository approvalRepository;
    private final UserApprovalSettingsRepository settingsRepository;
    
    // 危险工具列表
    private static final Set<String> DANGEROUS_TOOLS = Set.of(
            "delete_file_or_folder",
            "run_command",
            "run_persistent_command",
            "kill_persistent_terminal"
    );
    
    // 读文件工具
    private static final Set<String> READ_FILE_TOOLS = Set.of(
            "read_file",
            "ls_dir",
            "get_dir_tree",
            "search_pathnames_only",
            "search_for_files",
            "search_in_file",
            "read_lint_errors"
    );
    
    // 文件编辑工具
    private static final Set<String> FILE_EDIT_TOOLS = Set.of(
            "edit_file",
            "rewrite_file",
            "create_file_or_folder"
    );
    
    @Override
    @Transactional
    public Long createApprovalRequest(
            String sessionId,
            Long userId,
            String toolName,
            Map<String, Object> toolParams,
            String decisionId
    ) {
        ToolApproval approval = ToolApproval.builder()
                .sessionId(sessionId)
                .userId(userId)
                .toolName(toolName)
                .toolParams(toolParams)
                .decisionId(decisionId)
                .approvalStatus(ApprovalStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        
        ToolApproval saved = approvalRepository.save(approval);
        
        log.info("创建工具批准请求: id={}, sessionId={}, tool={}, decisionId={}", 
                saved.getId(), sessionId, toolName, decisionId);
        
        return saved.getId();
    }
    
    @Override
    public boolean requiresApproval(Long userId, String toolName) {
        UserApprovalSettings settings = getUserSettings(userId);
        
        // 检查危险工具
        if (DANGEROUS_TOOLS.contains(toolName)) {
            return !settings.getAutoApproveDangerousTools();
        }
        
        // 检查读文件工具
        if (READ_FILE_TOOLS.contains(toolName)) {
            return !settings.getAutoApproveReadFile();
        }
        
        // 检查文件编辑工具
        if (FILE_EDIT_TOOLS.contains(toolName)) {
            return !settings.getAutoApproveFileEdits();
        }
        
        // MCP 工具（以 "mcp_" 开头）
        if (toolName.startsWith("mcp_")) {
            return !settings.getAutoApproveMcpTools();
        }
        
        // 默认不需要批准（未知工具）
        return false;
    }
    
    @Override
    @Transactional
    public boolean approveToolCall(String decisionId, String reason) {
        Optional<ToolApproval> approvalOpt = approvalRepository.findByDecisionId(decisionId);
        
        if (approvalOpt.isEmpty()) {
            log.warn("批准记录不存在: decisionId={}", decisionId);
            return false;
        }
        
        ToolApproval approval = approvalOpt.get();
        
        if (approval.getApprovalStatus() != ApprovalStatus.PENDING) {
            log.warn("批准记录已处理: decisionId={}, status={}", decisionId, approval.getApprovalStatus());
            return false;
        }
        
        approval.setApprovalStatus(ApprovalStatus.APPROVED);
        approval.setApprovalReason(reason);
        approval.setApprovedAt(Instant.now());
        
        approvalRepository.save(approval);
        
        log.info("批准工具调用: decisionId={}, tool={}", decisionId, approval.getToolName());
        return true;
    }
    
    @Override
    @Transactional
    public boolean rejectToolCall(String decisionId, String reason) {
        Optional<ToolApproval> approvalOpt = approvalRepository.findByDecisionId(decisionId);
        
        if (approvalOpt.isEmpty()) {
            log.warn("批准记录不存在: decisionId={}", decisionId);
            return false;
        }
        
        ToolApproval approval = approvalOpt.get();
        
        if (approval.getApprovalStatus() != ApprovalStatus.PENDING) {
            log.warn("批准记录已处理: decisionId={}, status={}", decisionId, approval.getApprovalStatus());
            return false;
        }
        
        approval.setApprovalStatus(ApprovalStatus.REJECTED);
        approval.setApprovalReason(reason);
        approval.setApprovedAt(Instant.now());
        
        approvalRepository.save(approval);
        
        log.info("拒绝工具调用: decisionId={}, tool={}, reason={}", 
                decisionId, approval.getToolName(), reason);
        return true;
    }
    
    @Override
    public Optional<ToolApproval> getApproval(String decisionId) {
        return approvalRepository.findByDecisionId(decisionId);
    }
    
    @Override
    public List<ToolApproval> getPendingApprovals(String sessionId) {
        return approvalRepository.findBySessionIdAndApprovalStatus(sessionId, ApprovalStatus.PENDING);
    }
    
    @Override
    public UserApprovalSettings getUserSettings(Long userId) {
        return settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }
    
    /**
     * 创建默认设置
     */
    private UserApprovalSettings createDefaultSettings(Long userId) {
        UserApprovalSettings settings = UserApprovalSettings.builder()
                .userId(userId)
                .autoApproveDangerousTools(false)
                .autoApproveReadFile(true)
                .autoApproveFileEdits(false)
                .autoApproveMcpTools(false)
                .includeToolLintErrors(true)
                .maxCheckpointsPerSession(50)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        settingsRepository.save(settings);
        
        log.info("创建默认批准设置: userId={}", userId);
        return settings;
    }
    
    @Override
    @Transactional
    public void updateUserSettings(Long userId, UserApprovalSettings settings) {
        UserApprovalSettings existing = getUserSettings(userId);
        
        // 更新设置
        existing.setAutoApproveDangerousTools(settings.getAutoApproveDangerousTools());
        existing.setAutoApproveReadFile(settings.getAutoApproveReadFile());
        existing.setAutoApproveFileEdits(settings.getAutoApproveFileEdits());
        existing.setAutoApproveMcpTools(settings.getAutoApproveMcpTools());
        existing.setIncludeToolLintErrors(settings.getIncludeToolLintErrors());
        existing.setMaxCheckpointsPerSession(settings.getMaxCheckpointsPerSession());
        existing.setUpdatedAt(Instant.now());
        
        settingsRepository.save(existing);
        
        log.info("更新批准设置: userId={}", userId);
    }
    
    @Override
    @Transactional
    public int approveAllPending(String sessionId) {
        List<ToolApproval> pendingList = getPendingApprovals(sessionId);
        
        for (ToolApproval approval : pendingList) {
            approval.setApprovalStatus(ApprovalStatus.APPROVED);
            approval.setApprovalReason("批量批准");
            approval.setApprovedAt(Instant.now());
            approvalRepository.save(approval);
        }
        
        log.info("批量批准: sessionId={}, count={}", sessionId, pendingList.size());
        return pendingList.size();
    }
    
    @Override
    @Transactional
    public int rejectAllPending(String sessionId, String reason) {
        List<ToolApproval> pendingList = getPendingApprovals(sessionId);
        
        for (ToolApproval approval : pendingList) {
            approval.setApprovalStatus(ApprovalStatus.REJECTED);
            approval.setApprovalReason(reason != null ? reason : "批量拒绝");
            approval.setApprovedAt(Instant.now());
            approvalRepository.save(approval);
        }
        
        log.info("批量拒绝: sessionId={}, count={}", sessionId, pendingList.size());
        return pendingList.size();
    }
    
    @Override
    @Transactional
    public int cleanupExpiredApprovals(int daysToKeep) {
        Instant cutoffTime = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
        
        List<ToolApproval> toDelete = approvalRepository.findAll().stream()
                .filter(approval -> approval.getCreatedAt().isBefore(cutoffTime))
                .toList();
        
        approvalRepository.deleteAll(toDelete);
        
        log.info("清理过期批准记录: count={}, daysToKeep={}", toDelete.size(), daysToKeep);
        return toDelete.size();
    }
}

