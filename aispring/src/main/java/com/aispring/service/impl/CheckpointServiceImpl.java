package com.aispring.service.impl;

import com.aispring.entity.checkpoint.ChatCheckpoint;
import com.aispring.entity.checkpoint.CheckpointType;
import com.aispring.repository.ChatCheckpointRepository;
import com.aispring.service.CheckpointService;
import com.aispring.service.TerminalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查点服务实现
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckpointServiceImpl implements CheckpointService {
    
    private final ChatCheckpointRepository checkpointRepository;
    private final TerminalService terminalService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public String createCheckpoint(
            String sessionId,
            Long userId,
            Integer messageOrder,
            CheckpointType type,
            Map<String, ChatCheckpoint.FileSnapshot> fileSnapshots,
            String description
    ) {
        String checkpointId = UUID.randomUUID().toString();
        
        ChatCheckpoint checkpoint = ChatCheckpoint.builder()
                .checkpointId(checkpointId)
                .sessionId(sessionId)
                .userId(userId)
                .checkpointType(type)
                .messageOrder(messageOrder)
                .fileSnapshots(fileSnapshots)
                .description(description)
                .createdAt(Instant.now())
                .build();
        
        checkpointRepository.save(checkpoint);
        
        log.info("创建检查点: checkpointId={}, sessionId={}, type={}, messageOrder={}", 
                checkpointId, sessionId, type, messageOrder);
        
        return checkpointId;
    }
    
    @Override
    public Optional<ChatCheckpoint> getCheckpoint(String checkpointId) {
        return checkpointRepository.findByCheckpointId(checkpointId);
    }
    
    @Override
    public List<ChatCheckpoint> getSessionCheckpoints(String sessionId, Long userId) {
        return checkpointRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId);
    }
    
    @Override
    public Optional<ChatCheckpoint> getCheckpointBeforeMessage(String sessionId, Integer messageOrder) {
        List<ChatCheckpoint> checkpoints = checkpointRepository
                .findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, null);
        
        // 查找消息顺序小于指定值的最近检查点
        return checkpoints.stream()
                .filter(cp -> cp.getMessageOrder() < messageOrder)
                .max(Comparator.comparing(ChatCheckpoint::getMessageOrder));
    }
    
    @Override
    @Transactional
    public List<String> jumpToCheckpoint(String checkpointId) {
        Optional<ChatCheckpoint> checkpointOpt = getCheckpoint(checkpointId);
        if (checkpointOpt.isEmpty()) {
            log.warn("检查点不存在: checkpointId={}", checkpointId);
            return Collections.emptyList();
        }
        
        ChatCheckpoint checkpoint = checkpointOpt.get();
        Map<String, ChatCheckpoint.FileSnapshot> fileSnapshots = checkpoint.getFileSnapshots();
        
        if (fileSnapshots == null || fileSnapshots.isEmpty()) {
            log.info("检查点没有文件快照: checkpointId={}", checkpointId);
            return Collections.emptyList();
        }
        
        List<String> restoredFiles = new ArrayList<>();
        
        // 恢复每个文件
        for (Map.Entry<String, ChatCheckpoint.FileSnapshot> entry : fileSnapshots.entrySet()) {
            String filePath = entry.getKey();
            ChatCheckpoint.FileSnapshot snapshot = entry.getValue();
            
            try {
                // 使用 TerminalService 写入文件
                terminalService.writeFile(
                        checkpoint.getUserId(),
                        filePath,
                        snapshot.getFileContent(),
                        null,  // 相对于当前工作目录
                        true   // 允许覆盖
                );
                restoredFiles.add(filePath);
                log.debug("恢复文件: path={}", filePath);
            } catch (Exception e) {
                log.error("恢复文件失败: path={}, error={}", filePath, e.getMessage(), e);
            }
        }
        
        log.info("跳转到检查点: checkpointId={}, 恢复文件数={}", checkpointId, restoredFiles.size());
        return restoredFiles;
    }
    
    @Override
    @Transactional
    public void updateUserModifications(
            String checkpointId,
            Map<String, ChatCheckpoint.FileSnapshot> userModifications
    ) {
        checkpointRepository.findByCheckpointId(checkpointId).ifPresent(checkpoint -> {
            checkpoint.setUserModifications(userModifications);
            checkpointRepository.save(checkpoint);
            log.info("更新用户修改快照: checkpointId={}", checkpointId);
        });
    }
    
    @Override
    @Transactional
    public void deleteCheckpoint(String checkpointId) {
        checkpointRepository.findByCheckpointId(checkpointId).ifPresent(checkpoint -> {
            checkpointRepository.delete(checkpoint);
            log.info("删除检查点: checkpointId={}", checkpointId);
        });
    }
    
    @Override
    @Transactional
    public void cleanupOldCheckpoints(String sessionId, int keepCount) {
        long totalCount = checkpointRepository.countBySessionId(sessionId);
        
        if (totalCount <= keepCount) {
            log.debug("检查点数量未超出限制: sessionId={}, count={}, limit={}", 
                    sessionId, totalCount, keepCount);
            return;
        }
        
        checkpointRepository.deleteOldCheckpoints(sessionId, keepCount);
        log.info("清理旧检查点: sessionId={}, 删除数量={}", sessionId, totalCount - keepCount);
    }
    
    @Override
    public long getCheckpointCount(String sessionId) {
        return checkpointRepository.countBySessionId(sessionId);
    }
    
    @Override
    public String exportCheckpoint(String checkpointId) {
        Optional<ChatCheckpoint> checkpointOpt = getCheckpoint(checkpointId);
        if (checkpointOpt.isEmpty()) {
            log.warn("检查点不存在: checkpointId={}", checkpointId);
            return null;
        }
        
        try {
            String json = objectMapper.writeValueAsString(checkpointOpt.get());
            log.info("导出检查点: checkpointId={}", checkpointId);
            return json;
        } catch (Exception e) {
            log.error("导出检查点失败: checkpointId={}, error={}", checkpointId, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    @Transactional
    public String importCheckpoint(String sessionId, Long userId, String checkpointJson) {
        try {
            ChatCheckpoint checkpoint = objectMapper.readValue(checkpointJson, ChatCheckpoint.class);
            
            // 重新生成ID和更新元数据
            String newCheckpointId = UUID.randomUUID().toString();
            checkpoint.setId(null);  // 清除原ID让数据库自动生成
            checkpoint.setCheckpointId(newCheckpointId);
            checkpoint.setSessionId(sessionId);
            checkpoint.setUserId(userId);
            checkpoint.setCreatedAt(Instant.now());
            
            checkpointRepository.save(checkpoint);
            
            log.info("导入检查点: newCheckpointId={}, sessionId={}", newCheckpointId, sessionId);
            return newCheckpointId;
        } catch (Exception e) {
            log.error("导入检查点失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return null;
        }
    }
}

