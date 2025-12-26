from dataclasses import dataclass
from typing import Any, Dict, Optional


@dataclass
class CacheStats:
    """记录会话级的 prompt 缓存命中/未命中 token 统计与命中率。"""
    promptCacheHitTokens: int = 0
    promptCacheMissTokens: int = 0
    promptTokens: int = 0
    completionTokens: int = 0
    totalTokens: int = 0
    countedRequests: int = 0

    def updateFromUsage(self, usage: Dict[str, Any]) -> None:
        """从单次请求的 usage 字段中增量更新会话统计。"""
        hit = int(usage.get("prompt_cache_hit_tokens") or 0)
        miss = int(usage.get("prompt_cache_miss_tokens") or 0)
        prompt = int(usage.get("prompt_tokens") or (hit + miss))
        completion = int(usage.get("completion_tokens") or 0)
        total = int(usage.get("total_tokens") or (prompt + completion))

        if hit == 0 and miss == 0 and prompt == 0 and completion == 0 and total == 0:
            return

        self.promptCacheHitTokens += hit
        self.promptCacheMissTokens += miss
        self.promptTokens += prompt
        self.completionTokens += completion
        self.totalTokens += total
        self.countedRequests += 1

    def getSessionHitRate(self) -> Optional[float]:
        """返回会话维度命中率（hit/(hit+miss)），无数据时返回 None。"""
        denom = self.promptCacheHitTokens + self.promptCacheMissTokens
        if denom <= 0:
            return None
        return self.promptCacheHitTokens / denom

    @staticmethod
    def getHitRateOfUsage(usage: Dict[str, Any]) -> Optional[float]:
        """返回单次请求命中率（hit/(hit+miss)），无数据时返回 None。"""
        hit = int(usage.get("prompt_cache_hit_tokens") or 0)
        miss = int(usage.get("prompt_cache_miss_tokens") or 0)
        denom = hit + miss
        if denom <= 0:
            return None
        return hit / denom
