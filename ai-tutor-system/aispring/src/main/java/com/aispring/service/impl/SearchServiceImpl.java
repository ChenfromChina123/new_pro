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

        // 重试机制，最多尝试 3 次
        int maxRetries = 3;
        java.util.List<String> usedInstances = new java.util.ArrayList<>(); // 记录已使用过的实例
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 优先使用本地 SearXNG 实例
                String[] allInstances = {
                    "http://localhost:8080/search",  // 本地 SearXNG 实例（优先）
                    "https://searx.tiekoetter.com/search",
                    "https://paulgo.io/search",
                    "https://search.mdosch.de/search",
                    "https://search.ononoki.org/search",
                    "https://priv.au/search",
                    "https://searx.org/search",
                    "https://searx.work/search",
                    "https://searx.be/search"
                };
                
                // 从未使用的实例中随机选择一个
                java.util.List<String> availableInstances = new java.util.ArrayList<>();
                for (String inst : allInstances) {
                    if (!usedInstances.contains(inst)) {
                        availableInstances.add(inst);
                    }
                }
                
                if (availableInstances.isEmpty()) {
                    // 所有实例都已尝试过，重置列表
                    usedInstances.clear();
                    availableInstances.addAll(java.util.Arrays.asList(allInstances));
                }
                
                String instanceUrl = availableInstances.get((int)(Math.random() * availableInstances.size()));
                usedInstances.add(instanceUrl);

                log.info("Trying SearXNG instance: {} (attempt {}/{})", instanceUrl, attempt, maxRetries);

                org.jsoup.Connection connection = Jsoup.connect(instanceUrl)
                        .data("q", searchQuery)
                        .data("format", "json") // 请求 JSON 格式
                        .data("language", "zh-CN")
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true) // 忽略 429 等 HTTP 错误，手动处理
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .header("Accept", "application/json") // 明确要求 JSON
                        .timeout(15000); // 增加超时时间

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

                org.jsoup.Connection.Response response = connection.execute();

                if (response.statusCode() != 200) {
                    log.warn("SearXNG instance {} returned status code: {}", instanceUrl, response.statusCode());
                    continue; // 尝试下一个实例
                }

                String jsonBody = response.body();

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonBody);
                com.fasterxml.jackson.databind.JsonNode resultsNode = rootNode.path("results");

                if (resultsNode.isMissingNode() || !resultsNode.isArray() || resultsNode.isEmpty()) {
                    log.warn("SearXNG returned empty results for keyword: {} on instance: {}", keywords, instanceUrl);
                    continue; // 尝试下一个实例或重试
                }

                StringBuilder sb = new StringBuilder();
                sb.append("针对关键词\"").append(keywords).append("\"的实时搜索结果：\n\n");

                int count = 0;
                for (com.fasterxml.jackson.databind.JsonNode result : resultsNode) {
                    if (count >= 5) break;

                    String title = result.path("title").asText("");
                    String snippet = result.path("content").asText("");
                    String link = result.path("url").asText("");

                    if (!title.isEmpty() && !snippet.isEmpty()) {
                        sb.append(count + 1).append(". ").append(title).append("\n");
                        sb.append("   摘要: ").append(snippet).append("\n");
                        if (!link.isEmpty()) {
                            sb.append("   链接: ").append(link).append("\n");
                        }
                        sb.append("\n");
                        count++;
                    }
                }

                if (count > 0) {
                    return sb.toString();
                } else {
                    log.warn("SearXNG returned empty results for keyword: {} on instance: {}", keywords, instanceUrl);
                    continue; // 数据解析为空，尝试下一个实例
                }

            } catch (Exception e) {
                log.error("Web search failed (attempt {}/{}): {}", attempt, maxRetries, keywords, e);
                if (attempt == maxRetries) {
                    log.error("All search attempts failed for keywords: {}", keywords, e);
                    return "针对关键词\"" + keywords + "\"的搜索失败，请稍后重试。";
                }
                // 等待一段时间后重试
                try {
                    Thread.sleep(2000 * attempt); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "针对关键词\"" + keywords + "\"的搜索失败，请稍后重试。";
                }
            }
        }
        return "针对关键词\"" + keywords + "\"的搜索没有找到相关结果。这可能是由于网络限制或搜索词过于冷门。";
    }
}
