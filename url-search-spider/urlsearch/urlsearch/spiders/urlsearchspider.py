import scrapy


class UrlsearchspiderSpider(scrapy.Spider):
    name = "urlsearchspider"
    allowed_domains = ["baidu.com", "bing.com"]
    start_urls = []

    def __init__(self, keywords=None, *args, **kwargs):
        super(UrlsearchspiderSpider, self).__init__(*args, **kwargs)
        if keywords:
            # 使用百度和必应搜索
            self.start_urls = [
                f"https://www.baidu.com/s?wd={keywords}",
                f"https://www.bing.com/search?q={keywords}"
            ]

    def parse(self, response):
        # 提取搜索结果
        if "baidu.com" in response.url:
            # 百度搜索结果提取
            for result in response.css('div.result.c-container'):
                title = result.css('h3.t a::text').get()
                url = result.css('h3.t a::attr(href)').get()
                summary = result.css('div.c-abstract::text').get()
                
                if title and url:
                    yield {
                        'title': title,
                        'url': url,
                        'summary': summary,
                        'source': 'baidu'
                    }
                    # 继续爬取详情页
                    yield scrapy.Request(url, callback=self.parse_detail)
        elif "bing.com" in response.url:
            # 必应搜索结果提取
            for result in response.css('li.b_algo'):
                title = result.css('h2 a::text').get()
                url = result.css('h2 a::attr(href)').get()
                summary = result.css('div.b_caption p::text').get()
                
                if title and url:
                    yield {
                        'title': title,
                        'url': url,
                        'summary': summary,
                        'source': 'bing'
                    }
                    # 继续爬取详情页
                    yield scrapy.Request(url, callback=self.parse_detail)

    def parse_detail(self, response):
        # 提取详情页内容
        title = response.css('title::text').get() or "无标题"
        content = ' '.join(response.css('p::text').getall())
        
        yield {
            'detail_title': title,
            'detail_url': response.url,
            'detail_content': content[:500]  # 限制内容长度
        }
