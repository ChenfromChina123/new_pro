package com.aispring.controller;

import com.aispring.controller.dto.UrlFilterRuleRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.entity.UrlFilterRule;
import com.aispring.service.UrlFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * URL过滤规则控制器
 */
@RestController
@RequestMapping("/api/url-filter")
@RequiredArgsConstructor
@Slf4j
public class UrlFilterController {

    private final UrlFilterService urlFilterService;

    /**
     * 创建过滤规则
     * @param request 规则请求
     * @return 创建的规则
     */
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<UrlFilterRule>> createRule(@Valid @RequestBody UrlFilterRuleRequest request) {
        UrlFilterRule rule = buildRuleFromRequest(request);
        UrlFilterRule created = urlFilterService.createRule(rule);
        log.info("Created URL filter rule: {}", created.getName());
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /**
     * 更新过滤规则
     * @param id 规则ID
     * @param request 规则请求
     * @return 更新后的规则
     */
    @PutMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<UrlFilterRule>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody UrlFilterRuleRequest request) {
        UrlFilterRule rule = buildRuleFromRequest(request);
        UrlFilterRule updated = urlFilterService.updateRule(id, rule);
        log.info("Updated URL filter rule: {}", updated.getName());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * 删除过滤规则
     * @param id 规则ID
     * @return 操作结果
     */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        urlFilterService.deleteRule(id);
        log.info("Deleted URL filter rule: {}", id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取规则详情
     * @param id 规则ID
     * @return 规则详情
     */
    @GetMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<UrlFilterRule>> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(urlFilterService.getRuleById(id)));
    }

    /**
     * 获取所有规则
     * @return 规则列表
     */
    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<UrlFilterRule>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success(urlFilterService.getAllRules()));
    }

    /**
     * 获取启用的规则
     * @return 启用的规则列表
     */
    @GetMapping("/rules/enabled")
    public ResponseEntity<ApiResponse<List<UrlFilterRule>>> getEnabledRules() {
        return ResponseEntity.ok(ApiResponse.success(urlFilterService.getEnabledRules()));
    }

    /**
     * 根据分类获取规则
     * @param category 分类名称
     * @return 规则列表
     */
    @GetMapping("/rules/category/{category}")
    public ResponseEntity<ApiResponse<List<UrlFilterRule>>> getRulesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(urlFilterService.getRulesByCategory(category)));
    }

    /**
     * 启用/禁用规则
     * @param id 规则ID
     * @param enabled 是否启用
     * @return 更新后的规则
     */
    @PatchMapping("/rules/{id}/enabled")
    public ResponseEntity<ApiResponse<UrlFilterRule>> setRuleEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        UrlFilterRule rule = urlFilterService.setRuleEnabled(id, enabled);
        log.info("Set rule {} enabled: {}", id, enabled);
        return ResponseEntity.ok(ApiResponse.success(rule));
    }

    /**
     * 检查单个URL
     * @param url 待检查的URL
     * @return 检查结果
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUrl(@RequestParam String url) {
        UrlFilterService.FilterResult result = urlFilterService.checkUrl(url);
        Map<String, Object> data = Map.of(
                "url", url,
                "shouldFilter", result.shouldFilter(),
                "filterType", result.filterType() != null ? result.filterType().name() : null,
                "redirectUrl", result.redirectUrl(),
                "matchedRule", result.matchedRule(),
                "matchedPattern", result.matchedPattern()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 批量过滤URL
     * @param urls URL列表
     * @return 过滤结果
     */
    @PostMapping("/filter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> filterUrls(@RequestBody List<String> urls) {
        UrlFilterService.FilteredUrls result = urlFilterService.filterUrls(urls);
        Map<String, Object> data = Map.of(
                "totalUrls", urls.size(),
                "allowedCount", result.allowedUrls().size(),
                "blockedCount", result.blockedUrls().size(),
                "allowedUrls", result.allowedUrls(),
                "blockedUrls", result.blockedUrls()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 从请求构建规则实体
     * @param request 请求DTO
     * @return 规则实体
     */
    private UrlFilterRule buildRuleFromRequest(UrlFilterRuleRequest request) {
        return UrlFilterRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .filterType(request.getFilterType())
                .matchType(request.getMatchType())
                .pattern(request.getPattern())
                .redirectUrl(request.getRedirectUrl())
                .priority(request.getPriority())
                .enabled(request.getEnabled())
                .category(request.getCategory())
                .build();
    }
}
