package com.aispring.controller;

import com.aispring.dto.request.ResourceRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.entity.Resource;
import com.aispring.service.CategoryService;
import com.aispring.service.ResourceService;
import com.aispring.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源控制器
 * 处理资源相关的API请求
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {
    
    private final ResourceService resourceService;
    private final CategoryService categoryService;
    private final UserFavoriteService userFavoriteService;
    
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
                .type("article")
                .build();
        
        Resource createdResource = resourceService.createResource(resource, request.getCategoryName());
        return ResponseEntity.ok(ApiResponse.success("资源添加成功", createdResource));
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
        return ResponseEntity.ok(ApiResponse.success("更新资源成功", updatedResource));
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