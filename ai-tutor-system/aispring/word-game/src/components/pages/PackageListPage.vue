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
        <div class="header-actions">
          <button
            type="button"
            class="btn btn-outline upload-btn"
            @click="$router.push('/upload')"
            title="分节上传，适合多节课程包"
          >
            <span class="btn-icon">📑</span>
            <span class="btn-text">分节上传</span>
          </button>
          <button
            class="btn btn-primary upload-btn"
            @click="showUploadModal = true"
            title="上传自定义课程包（单文件）"
          >
            <span class="btn-icon">📤</span>
            <span class="btn-text">上传课程包</span>
          </button>
          <button
            v-if="!isEmbedded"
            class="btn btn-ghost theme-btn"
            @click="toggleDarkMode"
            :title="isDark ? '切换浅色' : '切换暗色'"
          >
            {{ isDark ? "☀️" : "🌙" }}
          </button>
        </div>
      </div>
    </header>

    <!-- 加载中骨架 -->
    <main class="packages-main" v-if="loading">
      <div class="skeleton-card" v-for="i in 2" :key="i"></div>
    </main>

    <!-- 课程包列表 -->
    <main v-else class="packages-main">
      <!-- 主内容区工具栏：选项卡 + 搜索（与主体一致 max-width） -->
      <div class="main-toolbar">
        <div class="toolbar-top">
          <div class="tab-row">
            <button
              type="button"
              class="tab-btn"
              :class="{ active: activeTab === 'all' }"
              @click="activeTab = 'all'"
            >
              全部课程包
            </button>
            <button
              type="button"
              class="tab-btn"
              :class="{ active: activeTab === 'mine' }"
              @click="activeTab = 'mine'"
            >
              我的
            </button>
          </div>
          <p class="section-label">
            {{ sectionLabel }} · {{ displayPackages.length }} 个
            <span v-if="activeTab === 'all' && !searchKeyword" class="rank-hint">（按点击量排序）</span>
          </p>
        </div>
        <div class="search-row">
          <input
            v-model="searchKeyword"
            type="text"
            class="search-input"
            placeholder="搜索课程包名称或描述…"
            @input="onSearchInput"
          />
          <span v-if="searchKeyword" class="search-clear" @click="searchKeyword = ''; loadPackages()">✕</span>
        </div>
      </div>
      <!-- 我的选项卡空状态 -->
      <div v-if="activeTab === 'mine' && displayPackages.length === 0" class="empty-mine">
        <p class="empty-mine-text">您还没有上传课程包</p>
        <p class="empty-mine-hint">点击右上角「上传课程包」即可添加自定义课程</p>
        <button type="button" class="btn btn-primary empty-mine-btn" @click="showUploadModal = true">
          上传课程包
        </button>
      </div>
      <div v-else class="packages-grid">
        <div
          v-for="(pkg, index) in displayPackages"
          :key="pkg.id"
          class="package-card card"
          @click="onPackageClick(pkg.id)"
        >
          <!-- 排名序号 -->
          <div class="pkg-rank" :class="{ 'rank-top3': index < 3 }">{{ index + 1 }}</div>
          <!-- 左侧图标区（图片或 emoji） -->
          <div class="pkg-icon-wrap">
            <img v-if="isIconImage(pkg.icon)" :src="pkg.icon" alt="" class="pkg-icon-img" />
            <span v-else class="pkg-icon">{{ pkg.icon || "📦" }}</span>
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
              <span v-if="pkg.clickCount != null" class="stat-item stat-click">
                <span class="stat-icon">👆</span>
                {{ pkg.clickCount }} 次点击
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

    <!-- 上传课程包弹窗 -->
    <div v-if="showUploadModal" class="modal-overlay" @click.self="showUploadModal = false">
      <div class="modal-box upload-modal">
        <div class="modal-header">
          <h2>上传课程包</h2>
          <button type="button" class="modal-close" @click="showUploadModal = false">×</button>
        </div>
        <div class="modal-body">
          <p class="format-intro">请按固定格式准备数据后上传（JSON 或 CSV），每行/每条包含：英文、中文、音标（可选）。</p>
          <p class="format-tip">💡 测试上传可使用项目中的 <code>public/sample-upload.json</code> 示例文件。</p>
          <div class="format-help">
            <strong>JSON 格式示例：</strong>
            <pre>[{"english":"Hello world","chinese":"你好世界","soundmark":""},
{"english":"Good morning","chinese":"早上好","soundmark":"/ɡʊd ˈmɔːrnɪŋ/"}]</pre>
            <strong>CSV 格式：</strong> 第一行可为表头 <code>english,chinese,soundmark</code>，后续每行一条。含逗号时请用双引号包裹。
          </div>
          <div class="form-group">
            <label>课程包名称 <span class="required">*</span></label>
            <input v-model="uploadForm.name" type="text" placeholder="例如：我的考研词汇" maxlength="200" />
          </div>
          <div class="form-group">
            <label>简介</label>
            <input v-model="uploadForm.description" type="text" placeholder="简短描述，选填" maxlength="500" />
          </div>
          <div class="form-row">
            <div class="form-group form-group-icon">
              <label>图标（图片）</label>
              <div class="icon-upload-wrap">
                <div class="icon-preview" v-if="uploadForm.icon">
                  <img v-if="isIconImage(uploadForm.icon)" :src="uploadForm.icon" alt="图标预览" class="icon-preview-img" />
                  <span v-else class="icon-preview-emoji">{{ uploadForm.icon }}</span>
                </div>
                <label class="icon-upload-btn btn btn-outline">
                  <input
                    type="file"
                    accept="image/*"
                    class="icon-file-input"
                    @change="onIconFileSelect"
                  />
                  {{ uploadForm.icon ? "更换图片" : "选择图片" }}
                </label>
              </div>
              <p class="form-hint">建议尺寸 64×64 或正方形，支持 PNG/JPG</p>
            </div>
            <div class="form-group">
              <label>难度标签</label>
              <input v-model="uploadForm.level" type="text" placeholder="自定义" maxlength="50" class="form-input" />
            </div>
          </div>
          <div class="form-group checkbox-group">
            <label>
              <input v-model="uploadForm.isPublic" type="checkbox" />
              公开课程包（所有用户可见）
            </label>
          </div>
          <div class="form-group">
            <label>数据文件 <span class="required">*</span></label>
            <label class="file-upload-wrap">
              <input
                ref="fileInputRef"
                type="file"
                accept=".json,.csv,.txt"
                class="file-upload-input"
                @change="onFileSelect"
              />
              <span class="file-upload-btn btn btn-outline">选择文件</span>
              <span v-if="uploadForm.fileName" class="file-upload-name">{{ uploadForm.fileName }}</span>
            </label>
            <p v-if="uploadError" class="form-error">{{ uploadError }}</p>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost" @click="showUploadModal = false">取消</button>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="uploading || !uploadForm.name.trim() || !uploadForm.parsedStatements.length"
            @click="submitUpload"
          >
            {{ uploading ? "上传中…" : "上传" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchPackages, uploadPackage, recordPackageClick, type PackageMeta } from "@/services/wordGameApi";
import { coursePackages as staticPackages } from "@/data/courses";
import { useProgressStore } from "@/stores/progressStore";

const router = useRouter();
const isDark = ref(false);
const loading = ref(true);
const packages = ref<PackageMeta[]>([]);
const searchKeyword = ref("");
/** 当前选中的选项卡：all 全部课程包，mine 我的上传 */
const activeTab = ref<"all" | "mine">("all");
const progressStore = useProgressStore();

/** 当前页展示的课程包列表：全部或仅用户上传 */
const displayPackages = computed(() => {
  if (activeTab.value === "mine") {
    return packages.value.filter((p) => p.isUserPackage === true);
  }
  return packages.value;
});

/** 工具栏区域标题文案（根据选项卡与搜索状态） */
const sectionLabel = computed(() => {
  if (searchKeyword) return "搜索结果";
  return activeTab.value === "mine" ? "我的课程包" : "全部课程包";
});

const isEmbedded = window.self !== window.top;

/** 加载课程包列表，支持搜索 */
async function loadPackages(search?: string) {
  try {
    const data = await fetchPackages(search || undefined);
    console.log('[调试] API 返回的课程包数据:', data);
    console.log('[调试] 课程包数量:', data.length);
    packages.value = data;
    console.log('[调试] packages.value 已更新:', packages.value);
  } catch (error) {
    console.error('[错误] 加载课程包失败:', error);
    let list = staticPackages.map((p) => ({
      id: p.id,
      name: p.name,
      description: p.description,
      icon: p.icon,
      level: p.level,
      courseCount: p.courses.length,
      totalQuestions: p.courses.reduce((s, c) => s + c.count, 0),
    }));
    if (search?.trim()) {
      const term = search.trim().toLowerCase();
      list = list.filter(
        (p) =>
          p.name.toLowerCase().includes(term) ||
          p.description.toLowerCase().includes(term)
      );
    }
    packages.value = list;
  }
  console.log('[调试] 最终 packages.value:', packages.value);
  console.log('[调试] loading 状态:', loading.value);
  for (const pkg of packages.value) {
    progressStore.loadProgress(pkg.id);
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null;
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    loadPackages(searchKeyword.value);
    searchTimer = null;
  }, 300);
}

/** 点击课程包：记录点击后跳转详情 */
async function onPackageClick(packageId: string) {
  try {
    await recordPackageClick(packageId);
  } catch {
    // 忽略记录失败，不影响跳转
  }
  router.push(`/package/${packageId}`);
}

// 上传弹窗
const showUploadModal = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);
const uploading = ref(false);
const uploadError = ref("");
const uploadForm = ref({
  name: "",
  description: "",
  icon: "" as string,
  level: "自定义",
  isPublic: false,
  fileName: "",
  parsedStatements: [] as Array<{ english: string; chinese: string; soundmark: string }>,
});

/** 判断 icon 是否为图片（data URL 或 http(s) URL） */
function isIconImage(icon: string): boolean {
  if (!icon || typeof icon !== "string") return false;
  return icon.startsWith("data:") || icon.startsWith("http://") || icon.startsWith("https://");
}

/**
 * 将图片压缩为小尺寸 base64，避免请求体过大（建议 < 100KB）
 * 最大边长 128px，JPEG 质量 0.82
 */
function compressImageToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    const url = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(url);
      const max = 128;
      let w = img.naturalWidth;
      let h = img.naturalHeight;
      if (w > max || h > max) {
        if (w > h) {
          h = Math.round((h * max) / w);
          w = max;
        } else {
          w = Math.round((w * max) / h);
          h = max;
        }
      }
      const canvas = document.createElement("canvas");
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext("2d");
      if (!ctx) {
        reject(new Error("无法创建画布"));
        return;
      }
      ctx.drawImage(img, 0, 0, w, h);
      try {
        resolve(canvas.toDataURL("image/jpeg", 0.82));
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("图片加载失败"));
    };
    img.src = url;
  });
}

/** 上传表单：选择图标图片后压缩并转 base64，避免请求体过大 */
async function onIconFileSelect(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file || !file.type.startsWith("image/")) return;
  try {
    uploadForm.value.icon = await compressImageToDataUrl(file);
  } catch {
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      if (dataUrl) uploadForm.value.icon = dataUrl;
    };
    reader.readAsDataURL(file);
  }
  input.value = "";
}

function parseUploadFile(text: string, fileName: string): Array<{ english: string; chinese: string; soundmark: string }> {
  const trim = (s: string) => (s ?? "").trim();
  if (fileName.toLowerCase().endsWith(".json")) {
    const data = JSON.parse(text);
    if (!Array.isArray(data)) throw new Error("JSON 需为数组");
    return data.map((row: Record<string, unknown>) => ({
      english: trim(String(row.english ?? row.word ?? "")),
      chinese: trim(String(row.chinese ?? row.meaning ?? "")),
      soundmark: trim(String(row.soundmark ?? row.phonetic ?? "")),
    })).filter((s) => s.english || s.chinese);
  }
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  const out: Array<{ english: string; chinese: string; soundmark: string }> = [];
  const first = lines[0] || "";
  const isHeader = /^english,|^chinese,|^word,|^meaning,/i.test(first) || first.startsWith("english\t");
  const start = isHeader ? 1 : 0;
  for (let i = start; i < lines.length; i++) {
    const line = lines[i];
    let parts: string[];
    if (line.includes("\t")) {
      parts = line.split("\t").map(trim);
    } else {
      const match = line.match(/(?:"([^"]*)"|([^,]*))(?:,|$)/g);
      parts = match ? match.map((p) => trim(p.replace(/^"|"$/g, "").replace(/,$/, ""))) : line.split(",").map(trim);
    }
    const en = (parts[0] ?? "").trim();
    const zh = (parts[1] ?? "").trim();
    const sm = (parts[2] ?? "").trim();
    if (en || zh) out.push({ english: en, chinese: zh, soundmark: sm });
  }
  return out;
}

function onFileSelect(e: Event) {
  uploadError.value = "";
  uploadForm.value.fileName = "";
  uploadForm.value.parsedStatements = [];
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  uploadForm.value.fileName = file.name;
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const text = String(reader.result ?? "");
      uploadForm.value.parsedStatements = parseUploadFile(text, file.name);
      if (uploadForm.value.parsedStatements.length === 0) {
        uploadError.value = "未解析到有效题目，请检查格式";
      }
    } catch (err) {
      uploadError.value = err instanceof Error ? err.message : "解析失败";
    }
  };
  reader.readAsText(file, "UTF-8");
}

async function submitUpload() {
  if (!uploadForm.value.name.trim() || uploadForm.value.parsedStatements.length === 0) return;
  uploading.value = true;
  uploadError.value = "";
  try {
    await uploadPackage({
      name: uploadForm.value.name.trim(),
      description: uploadForm.value.description.trim(),
      icon: uploadForm.value.icon.trim() || "📦",
      level: uploadForm.value.level.trim() || "自定义",
      isPublic: uploadForm.value.isPublic,
      statements: uploadForm.value.parsedStatements,
    });
    showUploadModal.value = false;
    uploadForm.value = { name: "", description: "", icon: "", level: "自定义", isPublic: false, fileName: "", parsedStatements: [] };
    fileInputRef.value && (fileInputRef.value.value = "");
    await loadPackages(searchKeyword.value);
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : "上传失败";
  } finally {
    uploading.value = false;
  }
}

onMounted(async () => {
  const saved = localStorage.getItem("wordGameDarkMode");
  if (saved !== null) {
    isDark.value = saved === "true";
    document.body.classList.toggle("dark-mode", isDark.value);
  } else {
    isDark.value = document.body.classList.contains("dark-mode");
  }
  loading.value = true;
  await loadPackages();
  loading.value = false;
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
.header-actions { display: flex; align-items: center; gap: 10px; }
.upload-btn { display: flex; align-items: center; gap: 6px; padding: 8px 14px; font-size: 0.9rem; }
.theme-btn { font-size: 1.3rem; padding: 6px 10px; border-radius: var(--border-radius-sm); }

/* 主内容区工具栏：与列表同宽，与主体一致 */
.main-toolbar {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.tab-row {
  display: flex;
  gap: 4px;
}
.tab-btn {
  padding: 6px 14px;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-tertiary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--border-radius-md);
  cursor: pointer;
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}
.tab-btn:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}
.tab-btn.active {
  color: var(--primary-color);
  background: var(--bg-secondary);
  border-color: var(--border-color);
}
.section-label {
  font-size: 0.82rem;
  color: var(--text-tertiary);
  font-weight: 500;
  letter-spacing: 0.04em;
}
.rank-hint {
  color: var(--text-tertiary);
  font-weight: 400;
  margin-left: 4px;
}

/* 我的选项卡空状态 */
.empty-mine {
  text-align: center;
  padding: 48px 24px;
  background: var(--bg-secondary);
  border-radius: var(--border-radius-lg);
  border: 1px dashed var(--border-color);
}
.empty-mine-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}
.empty-mine-hint {
  font-size: 0.9rem;
  color: var(--text-tertiary);
  margin: 0 0 20px;
}
.empty-mine-btn {
  padding: 10px 20px;
  font-size: 0.95rem;
}

.search-row {
  position: relative;
  width: 100%;
  max-width: 320px;
  flex-shrink: 0;
}
.search-input {
  width: 100%;
  padding: 10px 36px 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 0.9rem;
}
.search-input::placeholder { color: var(--text-tertiary); }
.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
}
.search-clear {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--text-tertiary);
  font-size: 0.9rem;
}
.search-clear:hover { color: var(--text-primary); }

/* 上传弹窗：柔和圆角与阴影，避免尖锐感 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}
.modal-box {
  background: var(--bg-secondary);
  border-radius: 20px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12), 0 8px 24px rgba(0, 0, 0, 0.08);
  max-width: 540px;
  width: 100%;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-color);
}
.upload-modal .modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}
.upload-modal .modal-header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.02em;
}
.modal-close {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  border: none;
  font-size: 1.25rem;
  color: var(--text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: background 0.2s ease, color 0.2s ease;
}
.modal-close:hover {
  color: var(--text-primary);
  background: var(--border-color);
}
.upload-modal .modal-body {
  padding: 24px;
  overflow-y: auto;
}
.format-intro {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 8px;
  line-height: 1.5;
}
.format-tip {
  font-size: 0.82rem;
  color: var(--text-tertiary);
  margin-bottom: 14px;
  line-height: 1.5;
}
.format-tip code {
  background: var(--bg-tertiary);
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 0.8rem;
}
.format-help {
  font-size: 0.82rem;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 14px 16px;
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid var(--border-color);
}
.format-help pre {
  margin: 10px 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 0.78rem;
  line-height: 1.5;
}
.format-help code {
  background: var(--bg-primary);
  padding: 3px 8px;
  border-radius: 6px;
}
.form-group {
  margin-bottom: 18px;
}
.form-group label {
  display: block;
  font-size: 0.88rem;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.form-group input[type="text"],
.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 0.9rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.form-group input[type="text"]:focus,
.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-light);
}
/* 数据文件选择：圆角按钮样式 */
.file-upload-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.file-upload-input { display: none; }
.file-upload-btn {
  border-radius: 12px;
  padding: 10px 18px;
  cursor: pointer;
  margin: 0;
}
.file-upload-name {
  font-size: 0.88rem;
  color: var(--text-secondary);
}
.required { color: var(--primary-color); }
.form-row { display: flex; gap: 16px; }
.form-row .form-group { flex: 1; }
.form-group-icon { min-width: 0; }
/* 图标上传：与主体一致的卡片风格 */
.icon-upload-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.icon-preview {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.icon-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.icon-preview-emoji { font-size: 1.8rem; }
.icon-upload-btn {
  cursor: pointer;
  margin: 0;
  border-radius: 12px;
  padding: 10px 16px;
}
.icon-file-input { display: none; }
.form-hint { font-size: 0.75rem; color: var(--text-tertiary); margin-top: 6px; }
.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 0;
}
.checkbox-group input[type="checkbox"] {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  accent-color: var(--primary-color);
}
.file-name { font-size: 0.82rem; color: var(--text-tertiary); margin-top: 6px; }
.form-error { font-size: 0.82rem; color: var(--danger-color); margin-top: 6px; }
.upload-modal .modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-primary);
}
.upload-modal .modal-footer .btn {
  border-radius: 12px;
  padding: 10px 20px;
  font-weight: 500;
}
.upload-modal .modal-footer .btn-primary {
  box-shadow: 0 2px 8px rgba(29, 78, 216, 0.25);
}

.packages-main {
  flex: 1;
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  width: 100%;
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

/* 排名序号 */
.pkg-rank {
  width: 28px;
  height: 28px;
  border-radius: var(--border-radius-sm);
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  font-size: 0.8rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.pkg-rank.rank-top3 {
  background: var(--primary-light);
  color: var(--primary-color);
}
.pkg-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: var(--border-radius-lg);
  background: var(--chip-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.pkg-icon { font-size: 2rem; line-height: 1; }
.pkg-icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.stat-click { color: var(--text-tertiary); }

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
