package com.aispring.controller;

import com.aispring.service.UserService;
import com.aispring.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final StorageProperties storageProperties;

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
