import scrapy
import json
import requests
import urllib.parse


class UrlsearchspiderSpider(scrapy.Spider):
    """
    URL搜索爬虫 - 使用 SearXNG 搜索引擎 API 获取搜索结果
    相比直接爬取百度/必应，SearXNG 提供更稳定的 API 接口
    """
    name = "urlsearchspider"
    allowed_domains = ["search.aistudy.icu"]
    
    # 自定义设置
    custom_settings = {
        'FEEDS': {
            'results_%(time)s.json': {
                'format': 'json',
                'overwrite': True,
                'encoding': 'utf8',
                'indent': 2,
            },
            'results_%(time)s.csv': {
                'format': 'csv',
                'overwrite': True,
                'encoding': 'utf8',
            },
        },
        'LOG_LEVEL': 'INFO',
        'DOWNLOAD_DELAY': 1,
    }

    def __init__(self, keywords=None, **kwargs):
        """
        初始化爬虫
        
        Args:
            keywords: 搜索关键词
        """
        super(UrlsearchspiderSpider, self).__init__(**kwargs)
        self.keywords = keywords
        self.start_urls = []
        
        if keywords:
            # 构建 SearXNG API URL
            encoded_keywords = urllib.parse.quote(keywords)
            self.start_urls = [
                f"https://search.aistudy.icu/search?q={encoded_keywords}&format=json&language=zh-CN"
            ]
            self.logger.info(f"=" * 60)
            self.logger.info(f"搜索关键词: {keywords}")
            self.logger.info(f"SearXNG URL: {self.start_urls[0]}")
            self.logger.info(f"=" * 60)

    def parse(self, response):
        """
        解析 SearXNG API 返回的 JSON 数据
        """
        self.logger.info(f"\n{'='*60}")
        self.logger.info(f"正在解析: {response.url}")
        self.logger.info(f"状态码: {response.status}")
        self.logger.info(f"{'='*60}\n")
        
        try:
            # 解析 JSON 响应
            data = json.loads(response.text)
            results = data.get('results', [])
            
            self.logger.info(f"获取到 {len(results)} 条搜索结果")
            
            results_found = 0
            for i, result in enumerate(results[:10], 1):  # 只取前10个
                title = result.get('title', '')
                url = result.get('url', '')
                summary = result.get('content', '')
                source = result.get('engine', 'unknown')
                
                if title and url:
                    results_found += 1
                    item = {
                        'title': title.strip(),
                        'url': url.strip(),
                        'summary': summary.strip() if summary else '',
                        'source': source,
                        'search_keywords': self.keywords,
                        'rank': i
                    }
                    self.logger.info(f"✓ 结果 #{i}: {title[:50]}...")
                    yield item
                    
                    # 继续爬取详情页
                    yield scrapy.Request(
                        url, 
                        callback=self.parse_detail,
                        meta={'item': item},
                        errback=self.handle_error
                    )
            
            self.logger.info(f"\n总共提取到 {results_found} 条结果\n")
            
        except json.JSONDecodeError as e:
            self.logger.error(f"JSON 解析错误: {e}")
            self.logger.error(f"响应内容: {response.text[:500]}")

    def parse_detail(self, response):
        """
        解析详情页内容
        """
        parent_item = response.meta.get('item', {})
        
        # 提取页面标题
        page_title = response.css('title::text').get('').strip()
        
        # 提取页面主要内容
        # 尝试多种内容选择器
        content_selectors = [
            'article',
            'main',
            '[role="main"]',
            '.content',
            '.article',
            '#content',
            '#main-content',
        ]
        
        content = ''
        for selector in content_selectors:
            text = response.css(f'{selector} ::text').getall()
            if text:
                content = ' '.join(t.strip() for t in text if t.strip())
                if len(content) > 100:
                    break
        
        # 如果没有找到内容，提取所有段落
        if not content:
            paragraphs = response.css('p::text').getall()
            content = ' '.join(p.strip() for p in paragraphs if len(p.strip()) > 20)
        
        # 限制内容长度
        content = content[:1000] if content else ''
        
        self.logger.info(f"详情页: {response.url[:60]}... | 标题: {page_title[:40]}... | 内容长度: {len(content)}")
        
        yield {
            **parent_item,
            'detail_page_title': page_title,
            'detail_page_content': content,
            'detail_page_url': response.url
        }

    def handle_error(self, failure):
        """
        处理请求错误
        """
        self.logger.error(f"请求失败: {failure.getErrorMessage()}")
        self.logger.error(f"失败 URL: {failure.request.url}")

    def closed(self, reason):
        """
        爬虫关闭时的回调
        """
        self.logger.info(f"\n{'='*60}")
        self.logger.info(f"爬虫已关闭，原因: {reason}")
        self.logger.info(f"搜索结果已保存到 results_*.json 和 results_*.csv")
        self.logger.info(f"{'='*60}\n")
