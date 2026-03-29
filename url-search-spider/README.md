# URL 搜索服务项目

## 项目介绍

这是一个基于 Scrapy 和 Scrapyd 的 URL 搜索服务项目，用于通过百度和必应搜索引擎获取搜索结果。

## 项目结构

```
url-search-spider/
├── urlsearch/             # Scrapy 项目目录
│   ├── urlsearch/         # 项目代码
│   │   ├── spiders/       # 爬虫目录
│   │   │   └── urlsearchspider.py  # URL 搜索爬虫
│   │   ├── settings.py    # 项目设置
│   │   └── ...            # 其他配置文件
│   └── scrapy.cfg         # 部署配置
├── test_spider.py         # 测试脚本
└── README.md              # 项目说明
```

## 功能特性

- 使用百度和必应搜索引擎
- 支持自定义搜索关键词
- 提取搜索结果标题、URL 和摘要
- 自动爬取详情页内容
- 支持通过 Scrapyd 服务部署和管理

## 环境要求

- Python 3.7+
- Scrapy
- Scrapyd
- scrapyd-client

## 安装步骤

1. **安装依赖**
   ```bash
   pip install scrapy scrapyd scrapyd-client
   ```

2. **启动 Scrapyd 服务**
   ```bash
   scrapyd
   ```

## 部署步骤

1. **进入项目目录**
   ```bash
   cd url-search-spider/urlsearch
   ```

2. **部署到 Scrapyd**
   ```bash
   # 方法一：使用命令
   scrapyd-deploy
   
   # 方法二：使用 Python 模块
   python -m scrapyd_client.deploy
   ```

## 使用方法

### 1. 通过 API 启动爬虫

```bash
# 启动爬虫（替换关键词为你需要的搜索词）
curl http://localhost:6800/schedule.json -d project=urlsearch -d spider=urlsearchspider -d keywords=人工智能
```

### 2. 查看任务状态

访问 http://localhost:6800/ 查看任务状态和日志。

### 3. 停止爬虫

```bash
# 替换任务ID为实际的任务ID
curl http://localhost:6800/cancel.json -d project=urlsearch -d job=任务ID
```

## 测试脚本

运行测试脚本测试爬虫功能：

```bash
python test_spider.py
```

## 爬虫配置

### 爬虫参数

- `keywords`：搜索关键词（必填）

### 项目设置

- `USER_AGENT`：浏览器用户代理
- `ROBOTSTXT_OBEY`：是否遵守 robots.txt 规则
- `CONCURRENT_REQUESTS_PER_DOMAIN`：每个域名的并发请求数
- `DOWNLOAD_DELAY`：下载延迟（秒）

## 技术栈

- **Scrapy**：Python 爬虫框架
- **Scrapyd**：爬虫部署和管理服务
- **BeautifulSoup**：HTML 解析（Scrapy 内置）
- **Requests**：HTTP 请求（测试脚本使用）

## 注意事项

1. 请遵守相关网站的 robots.txt 规则
2. 合理设置爬取延迟，避免对目标网站造成压力
3. 爬虫仅用于学习和研究目的

## 扩展建议

1. 添加更多搜索引擎支持
2. 实现结果去重和过滤
3. 集成到现有项目中作为搜索服务
4. 添加定时任务功能

## 故障排查

- **部署失败**：检查 Scrapyd 服务是否正常运行
- **爬虫不启动**：检查关键词参数是否正确
- **抓取失败**：检查网络连接和目标网站是否可访问
- **结果为空**：检查选择器是否正确，可能网站结构已变化
