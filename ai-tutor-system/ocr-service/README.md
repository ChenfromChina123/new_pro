# OCR Service - CRNN ONNX 轻量级部署

轻量级OCR服务，基于CRNN ONNX量化模型，支持中英文混合识别。

## 特性

- 🚀 **轻量级**: 内存占用 ~150MB，单核CPU即可运行
- 🔥 **高性能**: CPU推理，响应时间 <1秒
- 🌐 **REST API**: 简单易用的HTTP接口
- 🐳 **Docker支持**: 一键容器化部署
- 📝 **中英文识别**: 支持中英文混合文本识别

## 快速开始

### 方式一：本地运行（推荐开发测试）

```bash
# Windows
start.bat

# Linux/Mac
chmod +x start.sh
./start.sh
```

启动脚本会自动：
1. 检查Python环境
2. 安装依赖
3. 下载模型文件
4. 启动服务

### 方式二：Docker部署

```bash
# 构建并启动
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## API接口

### 1. 健康检查

```
GET /health
```

响应示例：
```json
{
  "status": "healthy",
  "service": "ocr-service",
  "version": "1.0.0",
  "uptime_seconds": 3600,
  "model_loaded": true
}
```

### 2. 文件上传识别

```
POST /ocr
Content-Type: multipart/form-data

参数:
- image: 图像文件
```

响应示例：
```json
{
  "success": true,
  "results": [
    {
      "text": "识别的文字",
      "confidence": 0.95,
      "box": [[x1, y1], [x2, y2], [x3, y3], [x4, y4]]
    }
  ],
  "full_text": "所有文字拼接",
  "count": 1
}
```

### 3. Base64识别

```
POST /ocr/base64
Content-Type: application/json

{
  "image": "base64编码的图像数据"
}
```

### 4. URL识别

```
POST /ocr/url
Content-Type: application/json

{
  "url": "图像URL"
}
```

## 使用示例

### Python

```python
import requests

# 文件上传
with open('image.png', 'rb') as f:
    response = requests.post('http://localhost:8089/ocr', files={'image': f})
    print(response.json())

# Base64
import base64
with open('image.png', 'rb') as f:
    image_base64 = base64.b64encode(f.read()).decode()
    response = requests.post(
        'http://localhost:8089/ocr/base64',
        json={'image': image_base64}
    )
    print(response.json())
```

### cURL

```bash
# 文件上传
curl -X POST -F "image=@test.png" http://localhost:8089/ocr

# 健康检查
curl http://localhost:8089/health
```

### Java (Spring Boot集成)

```java
/**
 * OCR服务客户端
 */
@Service
public class OcrServiceClient {
    
    private static final String OCR_URL = "http://localhost:8089/ocr";
    
    private final RestTemplate restTemplate;
    
    public OcrResult recognizeImage(MultipartFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", file.getResource());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        HttpEntity<MultiValueMap<String, Object>> request = 
            new HttpEntity<>(body, headers);
        
        return restTemplate.postForObject(OCR_URL, request, OcrResult.class);
    }
}
```

## 资源消耗

| 指标 | 数值 |
|------|------|
| 内存占用 | ~150MB |
| CPU | 单核即可 |
| 模型大小 | ~15MB |
| 推理延迟 | <1秒 |

## 目录结构

```
ocr-service/
├── app.py              # Flask主应用
├── ocr_engine.py       # OCR推理引擎
├── download_models.py  # 模型下载脚本
├── requirements.txt    # Python依赖
├── Dockerfile          # Docker配置
├── docker-compose.yml  # Docker Compose配置
├── start.bat           # Windows启动脚本
├── start.sh            # Linux/Mac启动脚本
├── test_ocr.py         # 测试脚本
├── README.md           # 说明文档
└── models/             # 模型文件目录
    ├── det_model.onnx  # 检测模型
    ├── rec_model.onnx  # 识别模型
    ├── cls_model.onnx  # 方向分类模型
    └── ppocr_keys_v1.txt  # 字典文件
```

## 测试

```bash
# 运行测试
python test_ocr.py
```

## 注意事项

1. 首次运行会自动下载模型文件（约15MB）
2. 建议使用Python 3.8+
3. 内存限制200MB，可通过Docker配置调整
4. 支持的图像格式：PNG, JPG, JPEG, BMP

## License

MIT License
