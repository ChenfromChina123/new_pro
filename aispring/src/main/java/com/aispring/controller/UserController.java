package com.aispring.controller;

import com.aispring.config.StorageProperties;
import com.aispring.dto.response.ApiResponse;
import com.aispring.entity.User;
import com.aispring.security.CustomUserDetails;
import com.aispring.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final StorageProperties storageProperties;

    /**
     * 上传用户头像
     */
    @PostMapping("/avatar/upload")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        try {
            if (customUserDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, "请先登录"));
            }
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "文件不能为空"));
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "只能上传图片文件"));
            }

            Long userId = customUserDetails.getUser().getId();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".jpg";
            
            // 生成唯一文件名: userId_uuid.ext
            String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = Paths.get(storageProperties.getAvatarsAbsolute()).resolve(filename);
            
            // 确保父目录存在 (虽然 StorageProperties 已经尝试创建，但这里再双重检查一次更安全)
            Files.createDirectories(filePath.getParent());
            
            // 保存文件，使用 REPLACE_EXISTING 覆盖同名文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 更新数据库中的头像路径
            String avatarPath = "/api/users/avatar/" + filename;
            userService.updateAvatar(userId, avatarPath);
            
            log.info("用户 {} 上传头像成功: {}", userId, filename);
            return ResponseEntity.ok(ApiResponse.success("头像上传成功", avatarPath));
            
        } catch (Exception e) {
            log.error("上传头像失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "头像上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<?> getAvatar(@PathVariable String filename) throws IOException {
        Path primary = Paths.get(storageProperties.getAvatarsAbsolute(), filename);
        Path path = Files.exists(primary) ? primary : Paths.get("../../old_pro/py/avatars", filename);
        if (!Files.exists(path)) {
            String fallback;
            Long uid = null;
            int idx = filename.indexOf('_');
            if (idx > 0) {
                try { uid = Long.parseLong(filename.substring(0, idx)); } catch (Exception ignore) {}
            }
            if (uid != null) {
                try {
                    String name = userService.getUserById(uid).getUsername().replace(" ", "+");
                    fallback = String.format("https://ui-avatars.com/api/?name=%s&background=random&color=fff&size=128", name);
                } catch (Exception e) {
                    fallback = "https://ui-avatars.com/api/?name=User&background=random&color=fff&size=128";
                }
            } else {
                fallback = "https://ui-avatars.com/api/?name=User&background=random&color=fff&size=128";
            }
            return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, fallback).build();
        }
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
