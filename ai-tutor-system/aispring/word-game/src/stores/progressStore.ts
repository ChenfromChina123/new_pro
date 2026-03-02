/**
 * 学习进度 Store
 * 管理课程包内所有课程的学习进度（完成状态、当前题目、学习时长）
 * 支持游客（IP 标识）和已登录用户（JWT 标识）
 */
import { defineStore } from "pinia";
import { ref } from "vue";
import {
  fetchProgress,
  saveProgress,
  type PackageProgress,
} from "@/services/wordGameApi";

export const useProgressStore = defineStore("progress", () => {
  /** 当前课程包 ID */
  const currentPackageId = ref<string | null>(null);

  /** 进度映射：courseIndex → 进度信息 */
  const progress = ref<PackageProgress>({});

  /** 是否正在加载 */
  const loading = ref(false);

  /** 加载错误信息 */
  const error = ref<string | null>(null);

  /**
   * 加载指定课程包的学习进度
   * 先从后端请求，失败时降级到 localStorage 缓存
   */
  async function loadProgress(packageId: string): Promise<void> {
    currentPackageId.value = packageId;
    loading.value = true;
    error.value = null;
    try {
      const rawData = await fetchProgress(packageId);
      // JSON key 均为字符串，需转换为 number 以匹配 courseIndex
      const data: PackageProgress = {};
      for (const [k, v] of Object.entries(rawData)) {
        data[Number(k)] = v;
      }
      // 合并 localStorage 缓存（优先以服务端数据为准）
      const cached = loadLocalProgress(packageId);
      progress.value = { ...cached, ...data };
      saveLocalProgress(packageId, progress.value);
    } catch (e) {
      // 网络不可达时使用本地缓存
      console.warn("[进度] 后端不可达，使用本地缓存", e);
      progress.value = loadLocalProgress(packageId);
      error.value = "进度同步失败，已使用本地数据";
    } finally {
      loading.value = false;
    }
  }

  /**
   * 保存某课程的进度
   * 同时写入后端和本地缓存（保证离线可用）
   */
  async function updateProgress(
    courseIndex: number,
    currentQuestion: number,
    completed: boolean,
    studySeconds = 0
  ): Promise<void> {
    if (!currentPackageId.value) return;

    // 乐观更新本地状态
    progress.value[courseIndex] = {
      currentQuestion,
      completed,
      studySeconds,
      updatedAt: new Date().toISOString(),
    };
    saveLocalProgress(currentPackageId.value, progress.value);

    // 异步同步到后端
    try {
      await saveProgress({
        packageId: currentPackageId.value,
        courseIndex,
        currentQuestion,
        completed,
        studySeconds,
      });
    } catch (e) {
      console.warn("[进度] 后端同步失败，仅本地保存", e);
    }
  }

  /** 获取指定课程的进度（若无则返回初始值） */
  function getCourseProgress(courseIndex: number) {
    return (
      progress.value[courseIndex] ?? {
        currentQuestion: 0,
        completed: false,
        studySeconds: 0,
        updatedAt: "",
      }
    );
  }

  /** 某课程是否已完成 */
  function isCompleted(courseIndex: number): boolean {
    return progress.value[courseIndex]?.completed === true;
  }

  /** 某课程的当前进度百分比（0-100） */
  function getProgressPercent(courseIndex: number, totalCount: number): number {
    if (totalCount === 0) return 0;
    const p = progress.value[courseIndex];
    if (!p) return 0;
    if (p.completed) return 100;
    return Math.round((p.currentQuestion / totalCount) * 100);
  }

  /** 课程包内已完成课程数量 */
  function completedCount(): number {
    return Object.values(progress.value).filter((p) => p.completed).length;
  }

  // ── 本地缓存工具 ─────────────────────────────────────────────────────────────

  function localKey(packageId: string): string {
    return `wg_progress_${packageId}`;
  }

  function loadLocalProgress(packageId: string): PackageProgress {
    try {
      const raw = localStorage.getItem(localKey(packageId));
      if (!raw) return {};
      const parsed = JSON.parse(raw);
      // JSON key 为字符串，转换为 number
      const result: PackageProgress = {};
      for (const [k, v] of Object.entries(parsed)) {
        result[Number(k)] = v as PackageProgress[number];
      }
      return result;
    } catch {
      return {};
    }
  }

  function saveLocalProgress(packageId: string, data: PackageProgress): void {
    try {
      localStorage.setItem(localKey(packageId), JSON.stringify(data));
    } catch {
      // 忽略 storage 满的情况
    }
  }

  return {
    currentPackageId,
    progress,
    loading,
    error,
    loadProgress,
    updateProgress,
    getCourseProgress,
    isCompleted,
    getProgressPercent,
    completedCount,
  };
});
