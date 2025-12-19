package com.aispring.controller;

import com.aispring.entity.UserFile;
import com.aispring.entity.UserFolder;
import com.aispring.service.CloudDiskService;
import com.aispring.dto.response.ApiResponse;
import com.aispring.security.CustomUserDetails;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 云盘控制器
 * 对应Python: app.py中的/api/cloud_disk端点
 */
@RestController
@RequestMapping("/api/cloud_disk")
@RequiredArgsConstructor
public class CloudDiskController {
    
    private final CloudDiskService cloudDiskService;
    
    // DTO类
    @Data
    public static class CreateFolderRequest {
        @NotBlank
        @JsonAlias({"folderName","folder_name"})
        private String folderName;
        @JsonAlias({"folderPath","folder_path"})
        private String folderPath;
        @JsonAlias({"parentId","parent_id"})
        private Long parentId;
    }
    
    @Data
    public static class MoveFileRequest {
        private Long targetFolderId;
        private String targetPath;
    }
    
    @Data
    public static class RenameFolderRequest {
        @NotBlank
        @JsonAlias({"newName","new_name"})
        private String newName;
    }

    @Data
    public static class RenameFileRequest {
        @NotBlank
        @JsonAlias({"newName","new_name"})
        private String newName;
    }
    
    @Data
    public static class ResolveRenameRequest {
        @JsonAlias({"action"})
        private String action;
        @NotBlank
        @JsonAlias({"finalName","final_name"})
        private String finalName;
    }
    
    /**
     * 初始化用户文件夹结构
     * Python: POST /api/cloud_disk/init-folder-structure
     */
    @PostMapping("/init-folder-structure")
    public ResponseEntity<ApiResponse<Void>> initFolderStructure(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        cloudDiskService.initUserFolderStructure(userId);
        
        return ResponseEntity.ok(ApiResponse.success("文件夹结构初始化成功", null));
    }
    
    /**
     * 获取文件夹树
     * Python: GET /api/cloud_disk/folders
     */
    @GetMapping("/folders")
    public ResponseEntity<ApiResponse<List<UserFolder>>> getFolders(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        List<UserFolder> folders = cloudDiskService.getUserFolders(userId);
        
        return ResponseEntity.ok(ApiResponse.success("获取文件夹树成功", folders));
    }
    
    /**
     * 创建文件夹
     * Python: POST /api/cloud_disk/create-folder
     */
    @PostMapping("/create-folder")
    public ResponseEntity<ApiResponse<UserFolder>> createFolder(
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false) String folderPath,
            @RequestParam(required = false) Long parentId,
            @RequestBody(required = false) CreateFolderRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getId();
        String name = (request != null && request.getFolderName() != null) ? request.getFolderName() : folderName;
        String path = (request != null && request.getFolderPath() != null) ? request.getFolderPath() : folderPath;
        Long pid = (request != null && request.getParentId() != null) ? request.getParentId() : parentId;
        try {
            UserFolder folder = cloudDiskService.createFolder(userId, name, path, pid);
            return ResponseEntity.ok(ApiResponse.success("文件夹创建成功", folder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "创建文件夹失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除文件夹
     * Python: POST /api/cloud_disk/delete-folder
     */
    @PostMapping("/delete-folder")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @RequestParam Long folderId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        
        Long userId = customUserDetails.getUser().getId();
        cloudDiskService.deleteFolder(userId, folderId);
        
        return ResponseEntity.ok(ApiResponse.success("文件夹已删除", null));
    }
    
    /**
     * 上传文件
     * Python: POST /api/cloud_disk/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<UserFile>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long folderId,
            @RequestParam String folderPath,
            @RequestParam(required = false, defaultValue = "RENAME") String conflictStrategy,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        
        Long userId = customUserDetails.getUser().getId();
        UserFile userFile = cloudDiskService.uploadFile(userId, folderId, folderPath, file, conflictStrategy);
        
        return ResponseEntity.ok(ApiResponse.success("文件上传成功", userFile));
    }

    @PostMapping("/upload-folder")
    public ResponseEntity<ApiResponse<Integer>> uploadFolder(
            @RequestParam("file") MultipartFile zipFile,
            @RequestParam String folderPath,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        Long userId = customUserDetails.getUser().getId();
        int count = cloudDiskService.uploadFolderZip(userId, folderPath, zipFile);
        return ResponseEntity.ok(ApiResponse.success("文件夹上传成功", count));
    }

    @PostMapping("/upload-folder-stream")
    public ResponseEntity<ApiResponse<Integer>> uploadFolderStream(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("paths") String[] paths,
            @RequestParam String folderPath,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        Long userId = customUserDetails.getUser().getId();
        int count = cloudDiskService.uploadFolderStream(userId, folderPath, files, paths);
        return ResponseEntity.ok(ApiResponse.success("文件夹上传成功", count));
    }
    
    /**
     * 获取文件列表
     * Python: GET /api/cloud_disk/files
     */
    @GetMapping("/files")
    public ResponseEntity<ApiResponse<List<UserFile>>> getFiles(
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String folderPath,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        List<UserFile> files = cloudDiskService.getUserFiles(userId, folderId, folderPath);
        
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @PutMapping("/rename-file")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> renameFile(
            @RequestParam Long fileId,
            @Valid @RequestBody RenameFileRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        try {
            Long userId = customUserDetails.getUser().getId();
            java.util.Map<String, Object> result = cloudDiskService.startRenameFile(userId, fileId, request.getNewName());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "重命名失败: " + e.getMessage()));
        }
    }

    @PostMapping("/resolve-rename-file")
    public ResponseEntity<ApiResponse<UserFile>> resolveRenameFile(
            @RequestParam Long fileId,
            @Valid @RequestBody ResolveRenameRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        try {
            Long userId = customUserDetails.getUser().getId();
            UserFile updated = cloudDiskService.resolveRenameFile(userId, fileId, request.getAction(), request.getFinalName());
            return ResponseEntity.ok(ApiResponse.success("重命名完成", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "重命名失败: " + e.getMessage()));
        }
    }
    
    /**
     * 下载文件
     * Python: GET /api/cloud_disk/download/{file_id}
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        try {
            Long userId = customUserDetails.getUser().getId();
            Path filePath = cloudDiskService.downloadFile(userId, fileId);
            
            // 再次检查文件是否存在
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "文件不存在或已被删除"));
            }
            
            Resource resource;
            try {
                resource = new UrlResource(filePath.toUri());
                // 检查资源是否存在和可读
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "文件不可访问"));
                }
            } catch (MalformedURLException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件路径无效: " + e.getMessage()));
            }
            
            String filename = filePath.getFileName().toString();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + encodedFilename + "\"")
                .body(resource);
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "文件不存在: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "下载文件失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/download-folder")
    public ResponseEntity<?> downloadFolder(
            @RequestParam(required = false) String folderPath,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        try {
            Long userId = customUserDetails.getUser().getId();
            Path zipPath = cloudDiskService.zipFolder(userId, folderPath);
            Resource resource;
            try {
                resource = new UrlResource(zipPath.toUri());
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "文件不可访问"));
                }
            } catch (MalformedURLException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件路径无效: " + e.getMessage()));
            }
            String name;
            if (folderPath == null || folderPath.trim().isEmpty()) {
                name = "download";
            } else {
                String s = folderPath.trim();
                while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
                int idx = s.lastIndexOf('/');
                name = idx >= 0 ? s.substring(idx + 1) : s;
            }
            String encodedFilename = URLEncoder.encode(name + ".zip", StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + encodedFilename + "\"")
                .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "文件不存在: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "下载文件夹失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除文件
     * Python: DELETE /api/cloud_disk/delete/{file_id}
     */
    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        
        Long userId = customUserDetails.getUser().getId();
        cloudDiskService.deleteFile(userId, fileId);
        
        return ResponseEntity.ok(ApiResponse.success("文件已删除", null));
    }
    
    /**
     * 移动文件
     * Python: PUT /api/cloud_disk/move-file
     */
    @PutMapping("/move-file")
    public ResponseEntity<ApiResponse<UserFile>> moveFile(
            @RequestParam Long fileId,
            @RequestBody MoveFileRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        
        Long userId = customUserDetails.getUser().getId();
        UserFile file = cloudDiskService.moveFile(
            userId, fileId, request.getTargetFolderId(), request.getTargetPath());
        
        return ResponseEntity.ok(ApiResponse.success("文件移动成功", file));
    }
    
    /**
     * 重命名文件夹
     * Python: PUT /api/cloud_disk/rename-folder
     */
    @PutMapping("/rename-folder")
    public ResponseEntity<?> renameFolder(
            @RequestParam Long folderId,
            @RequestParam(required = false) String newName,
            @RequestBody(required = false) RenameFolderRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        Long userId = customUserDetails.getUser().getId();
        String name = (request != null && request.getNewName() != null) ? request.getNewName() : newName;
        try {
            java.util.Map<String, Object> result = cloudDiskService.startRenameFolder(userId, folderId, name);
            if (Boolean.TRUE.equals(result.get("conflict"))) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.success("文件夹重名冲突", result));
            }
            return ResponseEntity.ok(ApiResponse.success("文件夹重命名成功", result.get("folder")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "重命名文件夹失败: " + e.getMessage()));
        }
    }

    @PutMapping("/resolve-rename-folder")
    public ResponseEntity<?> resolveRenameFolder(
            @RequestParam Long folderId,
            @RequestBody ResolveRenameRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws IOException {
        Long userId = customUserDetails.getUser().getId();
        try {
            UserFolder folder = cloudDiskService.resolveRenameFolder(userId, folderId, request.getAction(), request.getFinalName());
            return ResponseEntity.ok(ApiResponse.success("文件夹重命名成功", folder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "处理文件夹重命名失败: " + e.getMessage()));
        }
    }

    @PostMapping("/migrate")
    public ResponseEntity<ApiResponse<Void>> migrateAll() {
        cloudDiskService.migrateToUnifiedBase();
        return ResponseEntity.ok(ApiResponse.success("迁移完成", null));
    }
}

