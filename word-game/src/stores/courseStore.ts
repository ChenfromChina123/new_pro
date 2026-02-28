/**
 * 课程与游戏进度 Store
 * 支持从 API 加载课程数据，并在题目切换时自动保存学习进度
 */
import { defineStore } from "pinia";
import { computed, ref } from "vue";
import type { Course, Statement } from "@/types";
import { getCourseStatements, courseMetas } from "@/data/courses";
import { fetchCourseQuestions } from "@/services/wordGameApi";
import { useProgressStore } from "@/stores/progressStore";

export const useCourseStore = defineStore("course", () => {
  /** 当前加载的课程（含所有题目） */
  const currentCourse = ref<Course | null>(null);

  /** 当前题目索引（0-based） */
  const statementIndex = ref(0);

  /** 是否显示完成弹窗 */
  const showCompletion = ref(false);

  /** 正在加载课程数据 */
  const loadingCourse = ref(false);

  /** 当前课程所属包 ID */
  const currentPackageId = ref<string | null>(null);

  /** 学习开始时间戳（用于计算本次学习时长） */
  let _sessionStart = Date.now();

  /** 当前题目 */
  const currentStatement = computed<Statement | undefined>(
    () => currentCourse.value?.statements[statementIndex.value]
  );

  /** 当前题目的英文单词数组（按空格拆分） */
  const words = computed(() => currentStatement.value?.english.split(" ") ?? []);

  /** 总题目数 */
  const totalCount = computed(() => currentCourse.value?.statements.length ?? 0);

  /** 人类友好的题目编号（1-based） */
  const visibleIndex = computed(() => statementIndex.value + 1);

  /** 是否已完成所有题目 */
  const isAllDone = computed(
    () => statementIndex.value >= totalCount.value - 1
  );

  /**
   * 加载指定课程
   * 1. 优先从 API 获取题目（支持后端加载）
   * 2. API 失败时降级到静态文件
   * 3. 自动恢复上次学习位置
   */
  async function loadCourse(courseIndex: number, packageId = "xingrong-beginner") {
    loadingCourse.value = true;
    currentPackageId.value = packageId;
    _sessionStart = Date.now();

    try {
      // ① 尝试从后端 API 加载题目（用户课程包传 packageId）
      const statements = await fetchCourseQuestions(courseIndex, packageId);
      const meta =
        packageId?.startsWith("up-")
          ? { index: 1, title: "第一课", count: statements.length }
          : courseMetas.find((m) => m.index === courseIndex);
      if (!meta) throw new Error("元数据不存在");
      currentCourse.value = { ...meta, statements };
    } catch {
      // ② 降级到静态数据（仅内置包）
      if (packageId?.startsWith("up-")) {
        loadingCourse.value = false;
        return;
      }
      const meta = courseMetas.find((m) => m.index === courseIndex);
      if (!meta) { loadingCourse.value = false; return; }
      const statements = getCourseStatements(courseIndex);
      currentCourse.value = { ...meta, statements };
    }

    showCompletion.value = false;

    // ③ 恢复上次学习位置
    const progressStore = useProgressStore();
    if (progressStore.currentPackageId === packageId) {
      const saved = progressStore.getCourseProgress(courseIndex);
      // 若已完成则从头开始，否则续接上次
      statementIndex.value =
        saved.completed ? 0 : Math.min(saved.currentQuestion, totalCount.value - 1);
    } else {
      statementIndex.value = 0;
    }

    loadingCourse.value = false;
  }

  /** 进入下一题，并自动保存进度 */
  async function toNextStatement() {
    if (statementIndex.value < totalCount.value - 1) {
      statementIndex.value++;
      await _autoSaveProgress();
    }
  }

  /** 回到上一题 */
  function toPrevStatement() {
    if (statementIndex.value > 0) {
      statementIndex.value--;
    }
  }

  /** 重置当前课程（从第一题重新开始） */
  function resetCourse() {
    statementIndex.value = 0;
    showCompletion.value = false;
    _sessionStart = Date.now();
  }

  /** 标记课程完成，显示完成弹窗，并保存完成进度 */
  async function completeCourse() {
    showCompletion.value = true;
    await _saveProgress(true);
  }

  /** 关闭完成弹窗 */
  function hideCompletion() {
    showCompletion.value = false;
  }

  /** 计算本次学习秒数（距 sessionStart 的累计） */
  function _sessionSeconds(): number {
    return Math.round((Date.now() - _sessionStart) / 1000);
  }

  /** 自动保存当前进度（题目切换时调用） */
  async function _autoSaveProgress(): Promise<void> {
    if (!currentCourse.value || !currentPackageId.value) return;
    const progressStore = useProgressStore();
    const prev = progressStore.getCourseProgress(currentCourse.value.index);
    const totalSecs = (prev.studySeconds || 0) + _sessionSeconds();
    _sessionStart = Date.now();
    await progressStore.updateProgress(
      currentCourse.value.index,
      statementIndex.value,
      false,
      totalSecs
    );
  }

  /** 保存进度（支持标记完成状态） */
  async function _saveProgress(completed: boolean): Promise<void> {
    if (!currentCourse.value || !currentPackageId.value) return;
    const progressStore = useProgressStore();
    const prev = progressStore.getCourseProgress(currentCourse.value.index);
    const totalSecs = (prev.studySeconds || 0) + _sessionSeconds();
    _sessionStart = Date.now();
    await progressStore.updateProgress(
      currentCourse.value.index,
      statementIndex.value,
      completed,
      totalSecs
    );
  }

  return {
    currentCourse,
    statementIndex,
    currentStatement,
    words,
    totalCount,
    visibleIndex,
    isAllDone,
    showCompletion,
    loadingCourse,
    loadCourse,
    toNextStatement,
    toPrevStatement,
    resetCourse,
    completeCourse,
    hideCompletion,
  };
});
