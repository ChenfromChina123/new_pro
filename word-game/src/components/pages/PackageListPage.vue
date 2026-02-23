<template>
  <div class="package-list-page">
    <!-- 顶部标题栏 -->
    <header class="page-header">
      <div class="header-inner">
        <div class="brand">
          <span class="brand-icon">📚</span>
          <div>
            <h1 class="brand-title">单词记忆</h1>
            <p class="brand-sub">选择课程包，开始学习之旅</p>
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
    <main class="packages-main" v-if="loading">
      <div class="skeleton-card" v-for="i in 2" :key="i"></div>
    </main>

    <!-- 课程包列表 -->
    <main v-else class="packages-main">
      <p class="section-label">全部课程包 · {{ packages.length }} 个</p>
      <div class="packages-grid">
        <div
          v-for="pkg in packages"
          :key="pkg.id"
          class="package-card card"
          @click="router.push(`/package/${pkg.id}`)"
        >
          <!-- 左侧图标区 -->
          <div class="pkg-icon-wrap">
            <span class="pkg-icon">{{ pkg.icon }}</span>
          </div>

          <!-- 主体信息 -->
          <div class="pkg-body">
            <div class="pkg-header-row">
              <h2 class="pkg-name">{{ pkg.name }}</h2>
              <span class="pkg-level-badge">{{ pkg.level }}</span>
            </div>
            <p class="pkg-desc">{{ pkg.description }}</p>
            <!-- 统计信息行 -->
            <div class="pkg-stats">
              <span class="stat-item">
                <span class="stat-icon">📖</span>
                {{ pkg.courseCount }} 课
              </span>
              <span class="stat-item">
                <span class="stat-icon">✏️</span>
                {{ pkg.totalQuestions }} 道练习
              </span>
            </div>

            <!-- 学习进度条 -->
            <div class="pkg-progress" v-if="getPackageCompleted(pkg.id) > 0">
              <div class="pkg-progress-bar">
                <div
                  class="pkg-progress-fill"
                  :style="{ width: getPackagePercent(pkg.id, pkg.courseCount) + '%' }"
                ></div>
              </div>
              <span class="pkg-progress-text">
                已完成 {{ getPackageCompleted(pkg.id) }}/{{ pkg.courseCount }} 课
              </span>
            </div>
          </div>

          <!-- 右侧进入箭头 -->
          <div class="pkg-arrow">→</div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchPackages, type PackageMeta } from "@/services/wordGameApi";
import { coursePackages as staticPackages } from "@/data/courses";
import { useProgressStore } from "@/stores/progressStore";

const router = useRouter();
const isDark = ref(false);
const loading = ref(true);
const packages = ref<PackageMeta[]>([]);
const progressStore = useProgressStore();

const isEmbedded = window.self !== window.top;

onMounted(async () => {
  // 主题初始化
  const saved = localStorage.getItem("wordGameDarkMode");
  if (saved !== null) {
    isDark.value = saved === "true";
    document.body.classList.toggle("dark-mode", isDark.value);
  } else {
    isDark.value = document.body.classList.contains("dark-mode");
  }

  // 加载课程包：优先 API，失败降级静态数据
  try {
    packages.value = await fetchPackages();
  } catch {
    packages.value = staticPackages.map((p) => ({
      id: p.id,
      name: p.name,
      description: p.description,
      icon: p.icon,
      level: p.level,
      courseCount: p.courses.length,
      totalQuestions: p.courses.reduce((s, c) => s + c.count, 0),
    }));
  } finally {
    loading.value = false;
  }

  // 加载各课程包进度
  for (const pkg of packages.value) {
    progressStore.loadProgress(pkg.id);
  }
});

function toggleDarkMode() {
  isDark.value = !isDark.value;
  document.body.classList.toggle("dark-mode", isDark.value);
  localStorage.setItem("wordGameDarkMode", String(isDark.value));
}

/** 获取课程包已完成课程数 */
function getPackageCompleted(packageId: string): number {
  if (progressStore.currentPackageId !== packageId) return 0;
  return progressStore.completedCount();
}

/** 获取课程包整体完成百分比 */
function getPackagePercent(packageId: string, total: number): number {
  if (total === 0) return 0;
  return Math.round((getPackageCompleted(packageId) / total) * 100);
}
</script>

<style scoped>
.package-list-page {
  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.page-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-inner {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand { display: flex; align-items: center; gap: 14px; }
.brand-icon { font-size: 2rem; }
.brand-title { font-size: 1.4rem; font-weight: 700; color: var(--text-primary); line-height: 1.2; }
.brand-sub { font-size: 0.8rem; color: var(--text-tertiary); margin-top: 2px; }
.theme-btn { font-size: 1.3rem; padding: 6px 10px; border-radius: var(--border-radius-sm); }

.packages-main {
  flex: 1;
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  width: 100%;
}

.section-label {
  font-size: 0.82rem;
  color: var(--text-tertiary);
  font-weight: 500;
  letter-spacing: 0.04em;
  margin-bottom: 16px;
}

/* 骨架屏 */
.skeleton-card {
  height: 120px;
  border-radius: var(--border-radius);
  background: var(--bg-secondary);
  margin-bottom: 16px;
  animation: shimmer 1.2s infinite ease-in-out;
}
@keyframes shimmer {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.packages-grid { display: flex; flex-direction: column; gap: 16px; }

.package-card {
  padding: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 20px;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.package-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: rgba(59, 130, 246, 0.35);
}
.package-card:active { transform: translateY(0); }

.pkg-icon-wrap {
  width: 64px; height: 64px; border-radius: 16px;
  background: var(--chip-bg); display: flex; align-items: center;
  justify-content: center; flex-shrink: 0;
}
.pkg-icon { font-size: 2rem; line-height: 1; }

.pkg-body { flex: 1; min-width: 0; }

.pkg-header-row {
  display: flex; align-items: center; gap: 10px;
  flex-wrap: wrap; margin-bottom: 6px;
}
.pkg-name { font-size: 1.15rem; font-weight: 700; color: var(--text-primary); }
.pkg-level-badge {
  padding: 2px 10px; border-radius: 20px;
  background: var(--chip-bg); color: var(--chip-color);
  font-size: 0.72rem; font-weight: 600; white-space: nowrap;
}
.pkg-desc { font-size: 0.85rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 12px; }

.pkg-stats { display: flex; gap: 16px; margin-bottom: 10px; }
.stat-item { display: flex; align-items: center; gap: 4px; font-size: 0.8rem; color: var(--text-tertiary); font-weight: 500; }
.stat-icon { font-size: 0.9rem; }

/* 进度条 */
.pkg-progress { margin-top: 6px; }
.pkg-progress-bar {
  height: 5px; border-radius: 3px;
  background: var(--border-color); overflow: hidden; margin-bottom: 4px;
}
.pkg-progress-fill {
  height: 100%; border-radius: 3px;
  background: var(--primary-color);
  transition: width 0.4s ease;
}
.pkg-progress-text { font-size: 0.75rem; color: var(--primary-color); font-weight: 600; }

.pkg-arrow {
  font-size: 1.3rem; color: var(--text-tertiary); flex-shrink: 0;
  transition: color 0.15s ease, transform 0.15s ease;
}
.package-card:hover .pkg-arrow { color: var(--primary-color); transform: translateX(4px); }

@media (max-width: 480px) {
  .package-card { padding: 18px 16px; gap: 14px; }
  .pkg-icon-wrap { width: 52px; height: 52px; border-radius: 12px; }
  .pkg-icon { font-size: 1.6rem; }
  .packages-main { padding: 20px 16px 32px; }
}
</style>
