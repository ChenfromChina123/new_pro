# 搜索服务部署完成总结

## ✅ 已完成的功能

### 1. 远程 SearXNG 服务集成
- **服务地址**：`https://search.aistudy.icu/search`
- **配置位置**：[SearchServiceImpl.java](file://d:\Users\Administrator\AistudyProject\new_pro\ai-tutor-system\aispring\src\main\java\com\aispring\service\impl\SearchServiceImpl.java#L41)
- **超时时间**：30 秒
- **返回结果数**：最多 5 条

### 2. API Key 认证模块
- **配置类**：[ApiKeyConfig.java](file://d:\Users\Administrator\AistudyProject\new_pro\ai-tutor-system\aispring\src\main\java\com\aispring\config\ApiKeyConfig.java)
- **控制器**：[SearchController.java](file://d:\Users\Administrator\AistudyProject\new_pro\ai-tutor-system\aispring\src\main\java\com\aispring\controller\SearchController.java)（已更新）
- **配置文件**：[application.yml](file://d:\Users\Administrator\AistudyProject\new_pro\ai-tutor-system\aispring\src\main\resources\application.yml#L215-L220)

### 3. 支持的认证方式
- **请求参数**：`?apiKey=YOUR_API_KEY`
- **请求头**：`X-API-Key: YOUR_API_KEY`

---

## 📋 部署清单

### 服务器端配置

#### 1. 配置 API Key（可选但推荐）

**方法 A：环境变量（推荐）**
```bash
# 生成随机 API Key
API_KEY="sk-aispring-2026-$(openssl rand -hex 16)"
echo $API_KEY

# 添加到环境变量
echo "export SEARCH_API_KEY=\"$API_KEY\"" >> ~/.bashrc
source ~/.bashrc

# 验证
echo $SEARCH_API_KEY
```

**方法 B：直接修改配置文件**
```yaml
# application.yml
search:
  api:
    key: "your-secret-api-key-here"
```

#### 2. 重启后端服务
```bash
# 停止服务
# （找到进程 ID 后 kill）

# 启动服务
cd /www/project/new_pro/ai-tutor-system/aispring
java -jar target/ai-tutor-1.0.0.jar &

# 或使用 nohup
nohup java -jar target/ai-tutor-1.0.0.jar > logs/backend.log 2>&1 &
```

#### 3. 验证服务
```bash
# 检查端口监听
netstat -tuln | grep 5000

# 测试本地访问（不带 API Key）
curl "http://localhost:5000/api/search?q=test"

# 测试带 API Key 访问
curl "http://localhost:5000/api/search?q=test&apiKey=YOUR_API_KEY"
```

### 客户端调用配置

#### 前端调用示例

**JavaScript (axios)**
```javascript
// 方式 1：请求参数
const response = await axios.get('/api/search', {
  params: {
    q: '人工智能',
    apiKey: 'YOUR_API_KEY'  // 如果配置了 API Key
  }
});

// 方式 2：请求头
const response = await axios.get('/api/search', {
  params: {
    q: '人工智能'
  },
  headers: {
    'X-API-Key': 'YOUR_API_KEY'
  }
});
```

**Python (requests)**
```python
import requests

# 方式 1：请求参数
response = requests.get(
    'http://localhost:5000/api/search',
    params={'q': '人工智能', 'apiKey': 'YOUR_API_KEY'}
)

# 方式 2：请求头
response = requests.get(
    'http://localhost:5000/api/search',
    params={'q': '人工智能'},
    headers={'X-API-Key': 'YOUR_API_KEY'}
)
```

---

## 🔧 配置说明

### 1. API Key 配置选项

| 配置方式 | 优点 | 缺点 | 推荐场景 |
|----------|------|------|----------|
| 环境变量 | 安全，不提交代码 | 需要手动设置 | 生产环境 ✅ |
| application.yml | 简单直接 | 可能提交到代码库 | 开发环境 |
| 启动参数 | 灵活 | 每次启动都要指定 | 临时测试 |

### 2. 不启用 API Key

如果 `search.api.key` 未配置或为空，则：
- ✅ 不需要提供 API Key 即可访问
- ✅ 适合开发环境或内部网络
- ❌ 任何人都可以访问（不安全）

### 3. 启用 API Key

如果配置了 `search.api.key`，则：
- ✅ 必须提供正确的 API Key
- ✅ 返回 401 错误给未授权请求
- ✅ 适合生产环境

---

## 📝 使用示例

### 示例 1：不启用 API Key（开发环境）

**配置**：
```yaml
# application.yml
search:
  api:
    key:  # 留空或不配置
```

**调用**：
```bash
curl "http://localhost:5000/api/search?q=人工智能"
```

### 示例 2：启用 API Key（生产环境）

**配置**：
```bash
# 服务器环境变量
export SEARCH_API_KEY="sk-aispring-2026-secure-key-xyz789"
```

**调用**：
```bash
# 方式 1：参数传递
curl "http://localhost:5000/api/search?q=人工智能&apiKey=sk-aispring-2026-secure-key-xyz789"

# 方式 2：请求头
curl -H "X-API-Key: sk-aispring-2026-secure-key-xyz789" "http://localhost:5000/api/search?q=人工智能"
```

### 示例 3：前端集成

**Vue/React 项目**
```javascript
// src/utils/request.js 或 api.js
const API_KEY = process.env.VUE_APP_SEARCH_API_KEY || 'your-api-key';

export function searchKnowledge(keyword) {
  return request({
    url: '/api/search',
    method: 'get',
    params: {
      q: keyword,
      apiKey: API_KEY
    }
  });
}
```

---

## 🔍 故障排查

### 问题 1：返回 401 错误

**现象**：
```json
{
  "code": 401,
  "message": "无效的 API Key",
  "data": null
}
```

**原因**：
- 配置了 API Key 但未提供
- 提供的 API Key 不正确

**解决**：
```bash
# 检查配置
grep "search.api.key" application.yml

# 检查环境变量
echo $SEARCH_API_KEY

# 使用正确的 API Key 调用
curl "http://localhost:5000/api/search?q=test&apiKey=正确的 API_KEY"
```

### 问题 2：搜索返回空结果

**现象**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "result": "针对关键词\"xxx\"的搜索没有找到相关结果。"
  }
}
```

**原因**：
- SearXNG 服务不可用
- 网络连接问题
- 搜索词过于冷门

**解决**：
```bash
# 检查 SearXNG 服务
curl "https://search.aistudy.icu/search?q=test&format=json"

# 查看后端日志
tail -f logs/application.log | grep "SearXNG"

# 检查网络连接
ping search.aistudy.icu
```

### 问题 3：SSL 证书错误

**现象**：
```
javax.net.ssl.SSLHandshakeException
```

**解决**：
代码中已配置忽略 SSL 证书错误，如果仍有问题，检查：
- 服务器时间是否同步
- SSL 证书是否过期

---

## 📊 性能优化

### 1. 超时时间配置

当前配置：30 秒
```java
.timeout(30000)  // SearchServiceImpl.java 第 54 行
```

可根据实际情况调整：
- 网络好：15-20 秒
- 网络差：30-45 秒

### 2. 结果数量配置

当前配置：5 条
```java
if (count >= 5) break;  // SearchServiceImpl.java 第 91 行
```

可根据需求调整：
- 快速响应：3 条
- 完整结果：10 条

### 3. 缓存优化（未来优化）

可以添加 Redis 缓存搜索结果：
```java
// 伪代码
String cacheKey = "search:" + MD5(keywords);
String cached = redisTemplate.opsForValue().get(cacheKey);
if (cached != null) {
    return cached;
}
String result = searchFromSearXNG(keywords);
redisTemplate.opsForValue().set(cacheKey, result, 3600);
return result;
```

---

## 🔐 安全建议

### 1. API Key 管理

- ✅ 使用强随机密钥（至少 32 位）
- ✅ 定期更换（建议每 3 个月）
- ✅ 不要在代码中硬编码
- ✅ 使用环境变量或密钥管理服务

### 2. 访问控制

- ✅ 配置防火墙限制访问 IP
- ✅ 使用 HTTPS 加密传输
- ✅ 记录所有访问日志
- ✅ 监控异常访问

### 3. 日志审计

```bash
# 查看 API Key 验证失败记录
grep "Invalid API Key" logs/application.log

# 查看搜索请求统计
grep "Searching web for query" logs/application.log | wc -l

# 查看错误请求
grep "ERROR" logs/application.log | grep search
```

---

## 📚 相关文档

- [API Key 配置与使用指南](./API-KEY-GUIDE.md)
- [SearXNG 部署配置文档](./DEPLOYMENT-GUIDE.md)
- [Linux 一键启动脚本](./LINUX-STARTUP-GUIDE.md)

---

## 🎯 下一步操作

### 立即执行

1. **配置 API Key**（如果启用）
   ```bash
   export SEARCH_API_KEY="your-secret-key"
   ```

2. **重启后端服务**
   ```bash
   # 停止旧服务
   # 启动新服务
   java -jar target/ai-tutor-1.0.0.jar &
   ```

3. **测试验证**
   ```bash
   curl "http://localhost:5000/api/search?q=test&apiKey=YOUR_API_KEY"
   ```

### 后续优化

- [ ] 添加搜索结果缓存
- [ ] 实现 API Key 速率限制
- [ ] 配置监控告警
- [ ] 添加访问统计

---

**部署版本**：v1.0  
**更新时间**：2026-03-22  
**服务地址**：https://search.aistudy.icu  
**后端端口**：5000
