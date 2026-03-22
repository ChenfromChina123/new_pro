package com.aispring.controller;

import com.aispring.dto.response.ApiResponse;
import com.aispring.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 执行网络搜索
     * @param q 搜索关键词
     * @param site 可选，指定搜索站点
     * @return 搜索结果
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "site", required = false) String site) {
        
        String result = searchService.searchIndustryInfo(q, site);
        
        Map<String, Object> data = new HashMap<>();
        data.put("query", q);
        data.put("site", site);
        data.put("result", result);
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
