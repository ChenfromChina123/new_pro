package com.aispring.service.impl;

import com.aispring.service.OcrService;
import com.aliyun.ocr_api20210707.models.RecognizeAllTextRequest;
import com.aliyun.ocr_api20210707.models.RecognizeAllTextResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aliyun.ocr_api20210707.Client;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OCR服务实现类
 * 使用阿里云OCR服务进行文字识别
 */
@Service
public class OcrServiceImpl implements OcrService {
    
    private Client ocrClient;
    
    /**
     * 初始化阿里云OCR客户端
     */
    public OcrServiceImpl() {
        try {
            // 从环境变量获取AccessKey
            String accessKeyId = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
            String accessKeySecret = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
            
            if (accessKeyId == null || accessKeySecret == null) {
                System.err.println("阿里云OCR: 环境变量未配置，请设置 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET");
                return;
            }
            
            // 配置
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            // 默认公网接入地址
            config.endpoint = "ocr-api.cn-hangzhou.aliyuncs.com";
            
            // 创建客户端
            ocrClient = new Client(config);
            System.out.println("阿里云OCR引擎初始化成功");
            
        } catch (Exception e) {
            System.err.println("阿里云OCR初始化失败: " + e.getMessage());
            ocrClient = null;
        }
    }
    
    @Override
    public Map<String, Object> recognizeText(MultipartFile file) throws IOException {
        if (ocrClient == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "OCR引擎未初始化");
            return errorResult;
        }
        
        try {
            // 保存文件到临时目录
            File tempFile = File.createTempFile("ocr", ".jpg");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }
            
            try {
                // 构建请求
                RecognizeAllTextRequest request = new RecognizeAllTextRequest()
                        .setUrl("file://" + tempFile.getAbsolutePath());
                RuntimeOptions runtime = new RuntimeOptions();
                
                // 发送请求
                RecognizeAllTextResponse response = ocrClient.recognizeAllTextWithOptions(request, runtime);
                
                // 处理响应
                StringBuilder textBuilder = new StringBuilder();
                if (response.getBody().getData().getWords() != null) {
                    response.getBody().getData().getWords().forEach(word -> {
                        textBuilder.append(word.getContent()).append("\n");
                    });
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("text", textBuilder.toString().trim());
                return result;
                
            } finally {
                // 删除临时文件
                tempFile.delete();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
    
    @Override
    public Map<String, Object> recognizeTextFromBase64(String base64Image) throws IOException {
        if (ocrClient == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "OCR引擎未初始化");
            return errorResult;
        }
        
        try {
            // 解码Base64图像数据
            byte[] imageBytes;
            if (base64Image.startsWith("data:image")) {
                base64Image = base64Image.split(",")[1];
            }
            imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(base64Image);
            
            // 保存到临时文件
            File tempFile = File.createTempFile("ocr", ".jpg");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(imageBytes);
            }
            
            try {
                // 构建请求
                RecognizeAllTextRequest request = new RecognizeAllTextRequest()
                        .setUrl("file://" + tempFile.getAbsolutePath());
                RuntimeOptions runtime = new RuntimeOptions();
                
                // 发送请求
                RecognizeAllTextResponse response = ocrClient.recognizeAllTextWithOptions(request, runtime);
                
                // 处理响应
                StringBuilder textBuilder = new StringBuilder();
                if (response.getBody().getData().getWords() != null) {
                    response.getBody().getData().getWords().forEach(word -> {
                        textBuilder.append(word.getContent()).append("\n");
                    });
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("text", textBuilder.toString().trim());
                return result;
                
            } finally {
                // 删除临时文件
                tempFile.delete();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return ocrClient != null;
    }
}
