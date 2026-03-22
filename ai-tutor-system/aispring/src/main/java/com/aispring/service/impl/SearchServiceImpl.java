package com.aispring.service.impl;

import com.aispring.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/**
 * 实时网络搜索服务实现类
 */
@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    /**
     * 根据关键词搜索相关信息
     * @param keywords 搜索关键词
     * @return 搜索结果摘要
     */
    @Override
    public String searchIndustryInfo(String keywords) {
        return searchIndustryInfo(keywords, null);
    }

    /**
     * 根据关键词在指定网站搜索相关信息
     * @param keywords 搜索关键词
     * @param site 指定网站域名，可为空
     * @return 搜索结果摘要
     */
    @Override
    public String searchIndustryInfo(String keywords, String site) {
        String searchQuery = keywords;
        if (site != null && !site.trim().isEmpty()) {
            searchQuery = keywords + " site:" + site.trim();
        }
        
        log.info("Searching web for query: {}", searchQuery);
        try {
            // 使用 SSL 忽略来解决 Jsoup 证书问题
            org.jsoup.Connection connection = Jsoup.connect("https://html.duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(searchQuery, "UTF-8"))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
                    .timeout(15000);
            
            // 忽略 SSL 证书错误
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            connection.sslSocketFactory(sc.getSocketFactory());

            Document doc = connection.get();

            Elements results = doc.select(".result__body");
            if (results.isEmpty()) {
                return "针对关键词\"" + keywords + "\"的搜索没有找到相关结果。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("针对关键词\"").append(keywords).append("\"的实时搜索结果：\n\n");
            
            int count = 0;
            for (Element result : results) {
                if (count >= 5) break; // 取前5条结果
                
                Element titleElem = result.selectFirst(".result__title .result__a");
                Element snippetElem = result.selectFirst(".result__snippet");
                Element linkElem = result.selectFirst(".result__url");
                
                if (titleElem != null && snippetElem != null) {
                    sb.append(count + 1).append(". ").append(titleElem.text()).append("\n");
                    sb.append("   摘要: ").append(snippetElem.text()).append("\n");
                    if (linkElem != null) {
                        sb.append("   链接: ").append(linkElem.text().trim()).append("\n");
                    }
                    sb.append("\n");
                    count++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Web search failed for keywords: {}", keywords, e);
            return "针对关键词\"" + keywords + "\"的搜索失败，请稍后重试。";
        }
    }
}
