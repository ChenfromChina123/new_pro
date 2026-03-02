<template>
  <div class="course-list-page">
    <!-- 顶部标题栏 -->
    <header class="page-header">
      <div class="header-inner">
        <button class="back-btn" @click="router.push('/')" title="返回课程包">
          <span class="back-arrow">←</span>
          <span class="back-text">课程包</span>
        </button>

        <div class="brand" v-if="pkgMeta">
          <div class="brand-icon-wrap">
            <img v-if="isIconImage(pkgMeta.icon)" :src="pkgMeta.icon" alt="" class="brand-icon-img" />
            <span v-else class="brand-icon">{{ pkgMeta.icon || "📦" }}</span>
          </div>
          <div>
            <h1 class="brand-title">{{ pkgMeta.name }}</h1>
            <p class="brand-sub">
              共 {{ courses.length }} 课
              <template v-if="progressStore.completedCount() > 0">
                · 已完成 {{ progressStore.completedCount() }} 课
              </template>
            </p>
          </div>
        </div>

        <button
          v-if="!isEmbedded"
          class="btn btn-ghost theme-btn"
          @click="toggleDarkMode"
          :title="isDark ? '切换浅色' : '切换暗色'"
        >
          {{ isDark ? "☀️" : "🌙" }}
        </button>
      </div>
    </header>

    <!-- 加载中骨架 -->
    <main class="courses-main" v-if="loading">
      <div class="courses-grid">
        <div class="skeleton-card" v-for="i in 12" :key="i"></div>
      </div>
    </main>

    <!-- 课程不存在提示 -->
    <div v-else-if="!pkgMeta" class="not-found">
      <p>找不到该课程包，请返回重新选择。</p>
      <button class="btn btn-primary" @click="router.push('/')">返回首页</button>
    </div>

    <!-- 课程卡片网格 -->
    <main v-else class="courses-main">
      <div class="courses-grid">
        <div
          v-for="course in courses"
          :key="course.index"
          class="course-card card"
          :class="{
            'is-completed': progressStore.isCompleted(course.index),
            'in-progress': !progressStore.isCompleted(course.index) && progressStore.getProgressPercent(course.index, course.count) > 0
          }"
          @click="handleStartCourse(course.index)"
        >
          <!-- 完成勾选图标 -->
          <div
            v-if="progressStore.isCompleted(course.index)"
            class="course-done-badge"
            title="已完成"
          >✓</div>

          <!-- 课程编号徽章 -->
          <div class="course-badge">{{ String(course.index).padStart(2, "0") }}</div>

          <!-- 课程信息 -->
          <div class="course-info">
            <h3 class="course-title">{{ course.title }}</h3>
            <p class="course-count">{{ course.count }} 道练习</p>
            <!-- 进度条（有学习记录时显示） -->
            <div
              v-if="progressStore.getProgressPercent(course.index, course.count) > 0 && !progressStore.isCompleted(course.index)"
              class="course-progress-bar"
            >
              <div
                class="course-progress-fill"
                :style="{ width: progressStore.getProgressPercent(course.index, course.count) + '%' }"
              ></div>
            </div>
          </div>

          <!-- 右侧箭头 -->
          <div class="course-arrow">
            {{ progressStore.isCompleted(course.index) ? '✓' : '→' }}
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchPackages, fetchPackageCourses, type PackageMeta, type CourseMeta } from "@/services/wordGameApi";
import { coursePackages as staticPackages } from "@/data/courses";
import { useCourseStore } from "@/stores/courseStore";
import { useProgressStore } from "@/stores/progressStore";

const props = defineProps<{ packageId: string }>();
const router = useRouter();
const courseStore = useCourseStore();
const progressStore = useProgressStore();

const isDark = ref(false);
const loading = ref(true);
const pkgMeta = ref<PackageMeta | null>(null);
const courses = ref<CourseMeta[]>([]);

const isEmbedded = window.self !== window.top;

/** 判断图标是否为图片（data URL 或 http(s)），避免 base64 当作文本显示在标题区 */
function isIconImage(icon: string): boolean {
  if (!icon || typeof icon !== "string") return false;
  return icon.startsWith("data:") || icon.startsWith("http://") || icon.startsWith("https://");
}

onMounted(async () => {
  // 主题初始化
  const saved = localStorage.getItem("wordGameDarkMode");
  if (saved !== null) {
    isDark.value = saved === "true";
    document.body.classList.toggle("dark-mode", isDark.value);
  } else {
    isDark.value = document.body.classList.contains("dark-mode");
  }

  // 加载课程包元信息
  try {
    const allPkgs = await fetchPackages();
    pkgMeta.value = allPkgs.find((p) => p.id === props.packageId) ?? null;
  } catch {
    const staticPkg = staticPackages.find((p) => p.id === props.packageId);
    if (staticPkg) {
      pkgMeta.value = {
        id: staticPkg.id,
        name: staticPkg.name,
        description: staticPkg.description,
        icon: staticPkg.icon,
        level: staticPkg.level,
        courseCount: staticPkg.courses.length,
        totalQuestions: staticPkg.courses.reduce((s, c) => s + c.count, 0),
      };
    }
  }

  // 加载课程列表
  try {
    courses.value = await fetchPackageCourses(props.packageId);
  } catch {
    const staticPkg = staticPackages.find((p) => p.id === props.packageId);
    courses.value = staticPkg?.courses ?? [];
  }

  // 加载此课程包的学习进度
  await progressStore.loadProgress(props.packageId);

  loading.value = false;
});

function toggleDarkMode() {
  isDark.value = !isDark.value;
  document.body.classList.toggle("dark-mode", isDark.value);
  localStorage.setItem("wordGameDarkMode", String(isDark.value));
}

/** 点击课程卡片，加载课程（自动恢复进度）并跳转到游戏页；用户课程包需把 packageId 带入 URL */
async function handleStartCourse(courseIndex: number) {
  await courseStore.loadCourse(courseIndex, props.packageId);
  const q =
    props.packageId?.startsWith("up-")
      ? `?packageId=${encodeURIComponent(props.packageId)}`
      : "";
  router.push(`/game/${courseIndex}${q}`);
}
</script>

<style scoped>
.course-list-page {
  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.page-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  position: sticky; top: 0; z-index: 10;
}

.header-inner {
  max-width: 1100px; margin: 0 auto;
  padding: 14px 24px; display: flex; align-items: center; gap: 16px;
}

.back-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 12px; border-radius: var(--border-radius-sm);
  border: 1px solid var(--border-color); background: transparent;
  color: var(--text-secondary); cursor: pointer; font-size: 0.85rem; font-weight: 500;
  transition: all 0.15s ease; flex-shrink: 0; white-space: nowrap;
}
.back-btn:hover { background: var(--bg-tertiary); color: var(--primary-color); border-color: var(--primary-color); }
.back-arrow { font-size: 1rem; }

.brand { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.brand-icon-wrap { width: 48px; height: 48px; flex-shrink: 0; border-radius: var(--border-radius-md); overflow: hidden; display: flex; align-items: center; justify-content: center; background: var(--bg-tertiary); }
.brand-icon-img { width: 100%; height: 100%; object-fit: cover; }
.brand-icon { font-size: 1.8rem; flex-shrink: 0; }
.brand-title { font-size: 1.2rem; font-weight: 700; color: var(--text-primary); line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.brand-sub { font-size: 0.78rem; color: var(--text-tertiary); margin-top: 2px; }
.theme-btn { font-size: 1.3rem; padding: 6px 10px; border-radius: var(--border-radius-sm); margin-left: auto; }

.not-found {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 16px;
  color: var(--text-secondary); padding: 48px;
}

.courses-main {
  flex: 1; max-width: 1100px; margin: 0 auto;
  padding: 32px 24px 48px; width: 100%;
}

/* 骨架屏 */
.skeleton-card {
  height: 80px; border-radius: var(--border-radius);
  background: var(--bg-secondary);
  animation: shimmer 1.2s infinite ease-in-out;
}
@keyframes shimmer {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.course-card {
  padding: 20px; cursor: pointer; display: flex;
  align-items: center; gap: 14px; position: relative;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: rgba(59, 130, 246, 0.3);
}
.course-card:active { transform: translateY(0); }

/* 已完成状态：绿色边框 */
.course-card.is-completed {
  border-color: rgba(34, 197, 94, 0.5) !important;
  background: var(--bg-secondary);
}
.course-card.is-completed::before {
  content: '';
  position: absolute; inset: 0; border-radius: inherit;
  background: rgba(34, 197, 94, 0.04);
  pointer-events: none;
}

/* 学习中状态：蓝色 */
.course-card.in-progress { border-color: rgba(59, 130, 246, 0.4) !important; }

/* 完成勾选角标 */
.course-done-badge {
  position: absolute; top: 8px; right: 8px;
  width: 20px; height: 20px; border-radius: 50%;
  background: #22c55e; color: #fff;
  font-size: 0.7rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}

.course-badge {
  width: 44px; height: 44px; border-radius: 50%;
  background: var(--chip-bg); color: var(--chip-color);
  font-size: 0.9rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.course-card.is-completed .course-badge { background: rgba(34, 197, 94, 0.15); color: #16a34a; }

.course-info { flex: 1; min-width: 0; }
.course-title { font-size: 1rem; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.course-count { font-size: 0.78rem; color: var(--text-tertiary); margin-top: 2px; }

/* 课程内进度条 */
.course-progress-bar {
  height: 3px; border-radius: 2px;
  background: var(--border-color); overflow: hidden; margin-top: 6px;
}
.course-progress-fill {
  height: 100%; border-radius: 2px;
  background: var(--primary-color);
  transition: width 0.3s ease;
}

.course-arrow {
  font-size: 1.1rem; color: var(--text-tertiary);
  transition: color 0.15s ease, transform 0.15s ease; flex-shrink: 0;
}
.course-card:hover .course-arrow { color: var(--primary-color); transform: translateX(3px); }
.course-card.is-completed .course-arrow { color: #22c55e; }

@media (max-width: 480px) {
  .courses-grid { grid-template-columns: 1fr 1fr; gap: 12px; }
  .courses-main { padding: 20px 16px 32px; }
  .back-text { display: none; }
}
</style>
