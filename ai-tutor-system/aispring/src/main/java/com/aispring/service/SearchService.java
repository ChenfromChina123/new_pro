package com.aispring.service;

/**
 * 实时网络搜索服务接口
 */
public interface SearchService {

    /**
     * 根据关键词搜索行业相关信息
     * @param keywords 搜索关键词
     * @return 搜索结果摘要
     */
    String searchIndustryInfo(String keywords);
}
