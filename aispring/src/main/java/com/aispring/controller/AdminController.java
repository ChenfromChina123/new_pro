package com.aispring.controller;

import com.aispring.dto.response.ApiResponse;
import com.aispring.entity.User;
import com.aispring.entity.UserFile;
import com.aispring.repository.ChatRecordRepository;
import com.aispring.repository.UserFileRepository;
import com.aispring.repository.UserRepository;
import com.aispring.security.CustomUserDetails;
import com.aispring.service.CloudDiskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ChatRecordRepository chatRecordRepository;
    private final UserFileRepository userFileRepository;
    private final CloudDiskService cloudDiskService;

    @Data
    public static class AdminStatistics {
        private long totalUsers;
        private long totalChats;
        private long totalFiles;
        private long totalStorage;
    }

    @Data
    public static class AdminUserDTO {
        private Long id;
        private String email;
        private java.time.LocalDateTime createdAt;
        private boolean active;
    }

    @Data
    public static class AdminFileDTO {
        private Long id;
        private String filename;
        private String userEmail;
        private Long fileSize;
        private java.time.LocalDateTime uploadTime;
    }

    @Data
    public static class FileContentRequest {
        @NotBlank
        private String content;
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<AdminStatistics>> getStatistics() {
        AdminStatistics stats = new AdminStatistics();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalChats(chatRecordRepository.count());
        stats.setTotalFiles(userFileRepository.count());
        Long storage = userFileRepository.sumAllFileSizes();
        stats.setTotalStorage(storage != null ? storage : 0L);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取文件内容
     */
    @GetMapping("/files/content/{fileId}")
    public ResponseEntity<ApiResponse<String>> getFileContent(@PathVariable Long fileId) {
        try {
            String content = cloudDiskService.getFileContentAdmin(fileId);
            return ResponseEntity.ok(ApiResponse.success("获取文件内容成功", content));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "读取文件失败: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取文件内容失败: " + e.getMessage()));
        }
    }

    /**
     * 更新文件内容
     */
    @PutMapping("/files/content/{fileId}")
    public ResponseEntity<ApiResponse<Void>> updateFileContent(
            @PathVariable Long fileId,
            @Valid @RequestBody FileContentRequest request) {
        try {
            cloudDiskService.updateFileContentAdmin(fileId, request.getContent());
            return ResponseEntity.ok(ApiResponse.success("文件内容更新成功", null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "更新文件失败: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "更新文件内容失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers() {
        List<User> allUsers = userRepository.findAll();
        List<AdminUserDTO> userDTOs = allUsers.stream().map(u -> {
            AdminUserDTO dto = new AdminUserDTO();
            dto.setId(u.getId());
            dto.setEmail(u.getEmail());
            dto.setCreatedAt(u.getCreatedAt());
            dto.setActive(u.getIsActive() != null && u.getIsActive());
            return dto;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("users", userDTOs);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有文件列表
     */
    @GetMapping("/files")
    public ResponseEntity<ApiResponse<List<AdminFileDTO>>> getFiles() {
        List<UserFile> allFiles = userFileRepository.findAll();
        List<AdminFileDTO> fileDTOs = allFiles.stream().map(f -> {
            AdminFileDTO dto = new AdminFileDTO();
            dto.setId(f.getId());
            dto.setFilename(f.getFilename());
            dto.setUserEmail(f.getUser().getEmail());
            dto.setFileSize(f.getFileSize());
            dto.setUploadTime(f.getUploadTime());
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(fileDTOs));
    }
}
