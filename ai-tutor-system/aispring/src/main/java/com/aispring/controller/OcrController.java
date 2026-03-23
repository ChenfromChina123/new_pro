package com.aispring.controller;

import com.aispring.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * OCR控制器
 * 处理文字识别相关的HTTP请求
 */
@RestController
@RequestMapping("/api/ocr")
public class OcrController {
    
    @Autowired
    private OcrService ocrService;
    
    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean isAvailable = ocrService.isAvailable();
        Map<String, Object> response = Map.of(
                "status", isAvailable ? "healthy" : "unavailable",
                "service", "ocr-service",
                "version", "1.0.0",
                "available", isAvailable,
                "engine", "阿里云OCR"
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * 识别图像中的文字
     * 
     * @param file 图像文件
     * @return 识别结果
     */
    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognizeText(@RequestParam("image") MultipartFile file) {
        try {
            Map<String, Object> result = ocrService.recognizeText(file);
            if ((boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (IOException e) {
            Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * 识别Base64编码的图像中的文字
     * 
     * @param request 请求体，包含Base64编码的图像数据
     * @return 识别结果
     */
    @PostMapping("/recognize/base64")
    public ResponseEntity<Map<String, Object>> recognizeTextFromBase64(@RequestBody Map<String, String> request) {
        try {
            String base64Image = request.get("image");
            if (base64Image == null) {
                Map<String, Object> errorResult = Map.of(
                        "success", false,
                        "error", "请求体需要包含 'image' 字段"
                );
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            Map<String, Object> result = ocrService.recognizeTextFromBase64(base64Image);
            if ((boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (IOException e) {
            Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
}
