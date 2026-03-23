# OCR Service - CRNN ONNX 轻量级部署

轻量级OCR服务，基于RapidOCR（PaddleOCR），支持中英文混合识别。

## 特性

- 🚀 **轻量级**: 内存占用 ~110MB，单核CPU即可运行
- 🔥 **高性能**: CPU推理，响应时间 <1秒
- 🌐 **REST API**: 简单易用的HTTP接口
- 🐳 **Docker支持**: 一键容器化部署
- 📝 **中英文识别**: 支持中英文混合文本识别
- 🖥️ **Linux一键部署**: 支持Ubuntu/Debian/CentOS等系统

## 快速开始

### 方式一：Linux服务器一键部署（推荐生产环境）

```bash
# 下载部署脚本
git clone https://github.com/your-repo/ai-tutor-system.git
cd ai-tutor-system/ocr-service

# 一键部署（需要root权限）
sudo bash deploy-linux.sh

# 查看服务状态
systemctl status ocr-service

# 查看日志
journalctl -u ocr-service -f
```

**部署完成后：**
- 服务地址: `http://服务器IP/`
- 健康检查: `http://服务器IP/health`
- 内存限制: 200MB
- CPU限制: 单核

**管理命令：**
```bash
# 启动服务
systemctl start ocr-service

# 停止服务
systemctl stop ocr-service

# 重启服务
systemctl restart ocr-service

# 查看状态
systemctl status ocr-service

# 查看日志
journalctl -u ocr-service -f

# 卸载服务
sudo bash deploy-linux.sh --uninstall
```

### 方式二：本地运行（开发测试）

```bash
# Windows
start.bat

# Linux/Mac
chmod +x start.sh
./start.sh
```

### 方式三：Docker部署

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
    
    /**
     * 识别图像中的文字
     */
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
| 内存占用 | ~110MB |
| CPU | 单核即可 |
| 模型大小 | 自动下载 |
| 推理延迟 | <1秒 |

## 目录结构

```
ocr-service/
├── app.py              # Flask主应用
├── ocr_engine.py       # OCR推理引擎
├── requirements.txt    # Python依赖
├── Dockerfile          # Docker配置
├── docker-compose.yml  # Docker Compose配置
├── deploy-linux.sh     # Linux一键部署脚本
├── start.bat           # Windows启动脚本
├── start.sh            # Linux/Mac启动脚本
├── test_ocr.py         # 测试脚本
└── README.md           # 说明文档
```

## Linux部署详情

### 系统要求

- 操作系统: Ubuntu 18.04+ / Debian 10+ / CentOS 7+ / Rocky Linux 8+
- Python: 3.8+
- 内存: 最低256MB
- 权限: root或sudo

### 部署脚本功能

1. **自动检测系统** - 支持Ubuntu/Debian/CentOS等主流发行版
2. **安装依赖** - 自动安装Python3、pip、Nginx等
3. **创建用户** - 创建专用服务用户`ocr`
4. **配置服务** - 创建systemd服务，开机自启
5. **配置Nginx** - 反向代理，支持外网访问
6. **资源限制** - 内存限制200MB，CPU单核

### 服务配置

服务安装位置: `/opt/ocr-service/`

systemd服务文件: `/etc/systemd/system/ocr-service.service`

Nginx配置: `/etc/nginx/sites-available/ocr-service`

## 测试

```bash
# 运行测试
python test_ocr.py

# 或使用识别测试
python test_ocr_recognition.py
```

## 注意事项

1. 首次运行会自动下载模型文件
2. 建议使用Python 3.8+
3. 内存限制200MB，可通过systemd配置调整
4. 支持的图像格式：PNG, JPG, JPEG, BMP

## 故障排查

### 服务无法启动

```bash
# 查看详细日志
journalctl -u ocr-service -n 100

# 检查服务状态
systemctl status ocr-service

# 检查端口占用
netstat -tlnp | grep 8089
```

### 内存不足

编辑 `/etc/systemd/system/ocr-service.service`，调整 `MemoryMax` 值：

```ini
MemoryMax=300M
```

然后重载配置：

```bash
systemctl daemon-reload
systemctl restart ocr-service
```

### Nginx 502错误

```bash
# 检查OCR服务是否运行
systemctl status ocr-service

# 检查Nginx配置
nginx -t

# 查看Nginx错误日志
tail -f /var/log/nginx/error.log
```

## License

MIT License
