package com.aispring.controller;

import com.aispring.dto.request.ResourceRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.entity.Resource;
import com.aispring.service.ResourceService;
import com.aispring.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * 资源控制器
 * 处理资源相关的API请求
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {
    
    private final ResourceService resourceService;
    private final StorageProperties storageProperties;
    
    /**
     * 添加资源
     * API文档：POST /api/resources
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Resource>> createResource(
            @RequestBody ResourceRequest request) {
        // 这里暂时使用固定的userId，实际应该从当前登录用户获取
        // 后续需要修改为从Authentication对象中正确获取用户ID
        Long userId = 21L;
        Resource resource = Resource.builder()
                .userId(userId)
                .title(request.getTitle())
                .url(request.getUrl())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .type(request.getType() != null ? request.getType() : "article")
                .version(request.getVersion())
                .build();
        
        Resource createdResource = resourceService.createResource(resource, request.getCategoryName());
        return ResponseEntity.ok(ApiResponse.success("资源添加成功", createdResource));
    }

    /**
     * 上传软件源文件
     * 仅限管理员使用 (实际项目中需添加 @PreAuthorize("hasRole('ADMIN')"))
     */
    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Resource>> uploadSoftwareFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        Resource resource = resourceService.getResourceById(id);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "上传文件不能为空"));
        }

        // 确保存储目录存在
        String uploadDir = storageProperties.getPublicFilesAbsolute();
        Path uploadPath = Paths.get(uploadDir).resolve("software");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex != -1) {
            extension = originalFilename.substring(lastDotIndex);
        }
        String fileName = UUID.randomUUID().toString() + extension;
        Path targetLocation = uploadPath.resolve(fileName);

        // 保存文件
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // 更新资源文件路径 (返回相对路径供前端下载)
        String relativePath = "/api/public-files/download/software/" + fileName;
        resource.setFilePath(relativePath);
        
        Resource updatedResource = resourceService.updateResource(id, resource, null);
        
        return ResponseEntity.ok(ApiResponse.success("文件上传成功", updatedResource));
    }
    
    /**
     * 获取资源列表
     * API文档：GET /api/resources
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Resource>>> getResources(
            @RequestParam(required = false) String categoryName) {
        List<Resource> resources;
        if (categoryName != null && !categoryName.isEmpty()) {
            resources = resourceService.getResourcesByCategory(categoryName);
        } else {
            resources = resourceService.getAllResources();
        }
        return ResponseEntity.ok(ApiResponse.success("获取资源列表成功", resources));
    }
    
    /**
     * 获取公开资源
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<Resource>>> getPublicResources() {
        List<Resource> resources = resourceService.getPublicResources();
        return ResponseEntity.ok(ApiResponse.success("获取公开资源列表成功", resources));
    }
    
    /**
     * 获取资源详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Resource>> getResource(@PathVariable Long id) {
        Resource resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success("获取资源详情成功", resource));
    }
    
    /**
     * 更新资源
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Resource>> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceRequest request) {
        Resource resource = Resource.builder()
                .title(request.getTitle())
                .url(request.getUrl())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .build();
        Resource updatedResource = resourceService.updateResource(id, resource, request.getCategoryName());
        return ResponseEntity.ok(ApiResponse.success("资源更新成功", updatedResource));
    }

    /**
     * 公开下载资源文件，并提供友好的文件名
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadResourceFile(@PathVariable Long id) {
        try {
            Resource resource = resourceService.getResourceById(id);
            if (resource.getFilePath() == null || resource.getFilePath().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "该资源没有可下载的文件"));
            }

            // 从相对路径解析出物理路径
            // 相对路径格式通常为: /api/public-files/download/software/uuid.apk
            String filePathStr = resource.getFilePath();
            String fileNameOnDisk = filePathStr.substring(filePathStr.lastIndexOf("/") + 1);
            
            Path path = Paths.get(storageProperties.getPublicFilesAbsolute()).resolve("software").resolve(fileNameOnDisk);
            org.springframework.core.io.Resource fileResource = new UrlResource(path.toUri());

            if (!fileResource.exists() || !fileResource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "文件不存在或不可读"));
            }

            // 构造友好的文件名: 标题 + 原后缀
            String extension = "";
            int dotIdx = fileNameOnDisk.lastIndexOf(".");
            if (dotIdx != -1) {
                extension = fileNameOnDisk.substring(dotIdx);
            }
            String friendlyName = resource.getTitle() + extension;
            String encodedFilename = URLEncoder.encode(friendlyName, StandardCharsets.UTF_8).replace("+", "%20");

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .body(fileResource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "下载失败: " + e.getMessage()));
        }
    }

    /**
     * 删除资源
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.success("删除资源成功", null));
    }
}