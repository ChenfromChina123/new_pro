<template>
  <div class="package-upload-page">
    <header class="page-header">
      <div class="header-inner">
        <div class="brand">
          <span class="brand-icon">📚</span>
          <div>
            <h1 class="brand-title">分节上传课程包</h1>
            <p class="brand-sub">先创建课程包，再按节依次上传，支持后续扩展更多节</p>
          </div>
        </div>
        <div class="header-actions">
          <button type="button" class="btn btn-ghost" @click="goBack">← 返回</button>
        </div>
      </div>
    </header>

    <main class="upload-main">
      <!-- 步骤 1：创建课程包元数据 -->
      <section v-if="step === 1" class="upload-step step-create">
        <h2 class="step-title">第一步：创建课程包信息</h2>
        <p class="step-desc">填写名称、简介等，创建后再添加各节内容。</p>
        <div class="form-card">
          <div class="form-group">
            <label>课程包名称 <span class="required">*</span></label>
            <input v-model="metaForm.name" type="text" placeholder="例如：我的考研词汇" maxlength="200" />
          </div>
          <div class="form-group">
            <label>简介</label>
            <input v-model="metaForm.description" type="text" placeholder="简短描述，选填" maxlength="500" />
          </div>
          <div class="form-row">
            <div class="form-group form-group-icon">
              <label>图标（图片）</label>
              <div class="icon-upload-wrap">
                <div class="icon-preview" v-if="metaForm.icon">
                  <img v-if="isIconImage(metaForm.icon)" :src="metaForm.icon" alt="" class="icon-preview-img" />
                  <span v-else class="icon-preview-emoji">{{ metaForm.icon }}</span>
                </div>
                <label class="icon-upload-btn btn btn-outline">
                  <input type="file" accept="image/*" class="icon-file-input" @change="onIconSelect" />
                  {{ metaForm.icon ? "更换" : "选择图片" }}
                </label>
              </div>
            </div>
            <div class="form-group">
              <label>难度标签</label>
              <input v-model="metaForm.level" type="text" placeholder="自定义" maxlength="50" />
            </div>
          </div>
          <div class="form-group checkbox-group">
            <label>
              <input v-model="metaForm.isPublic" type="checkbox" />
              公开课程包（所有用户可见）
            </label>
          </div>
          <div class="form-actions">
            <button
              type="button"
              class="btn btn-primary"
              :disabled="creating || !metaForm.name.trim()"
              @click="createPackage"
            >
              {{ creating ? "创建中…" : "创建课程包" }}
            </button>
          </div>
          <p v-if="createError" class="form-error">{{ createError }}</p>
        </div>
      </section>

      <!-- 步骤 2：添加节 -->
      <section v-else class="upload-step step-sections">
        <h2 class="step-title">第二步：添加节</h2>
        <p class="step-desc">每节可上传一个 JSON/CSV 文件，格式：英文、中文、音标（可选）。</p>

        <div v-if="sections.length > 0" class="sections-summary">
          <p class="summary-label">已添加 {{ sections.length }} 节</p>
          <ul class="sections-list">
            <li v-for="(s, i) in sections" :key="i" class="section-item">
              <span class="section-num">{{ i + 1 }}</span>
              <span class="section-title">{{ s.title || `第${i + 1}课` }}</span>
              <span class="section-count">{{ s.count }} 道题</span>
            </li>
          </ul>
        </div>

        <div class="form-card section-form">
          <div class="form-group">
            <label>本节标题</label>
            <input v-model="sectionForm.title" type="text" placeholder="例如：第一课、Unit 1" maxlength="200" />
          </div>
          <div class="form-group">
            <label>本节数据文件 <span class="required">*</span></label>
            <label class="file-upload-wrap">
              <input
                ref="sectionFileRef"
                type="file"
                accept=".json,.csv,.txt"
                class="file-upload-input"
                @change="onSectionFileSelect"
              />
              <span class="file-upload-btn btn btn-outline">选择文件</span>
              <span v-if="sectionForm.fileName" class="file-upload-name">{{ sectionForm.fileName }}</span>
            </label>
            <p class="format-tip">JSON 数组或 CSV（english, chinese, soundmark），可用 public/sample-upload.json 测试。</p>
            <p v-if="sectionError" class="form-error">{{ sectionError }}</p>
          </div>
          <div class="form-actions">
            <button
              type="button"
              class="btn btn-primary"
              :disabled="adding || sectionForm.parsed.length === 0"
              @click="addSection"
            >
              {{ adding ? "添加中…" : "添加本节" }}
            </button>
            <button type="button" class="btn btn-ghost" @click="finishAndGo">
              完成，前往课程包
            </button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { createPackage as apiCreatePackage, addPackageSection, fetchPackageCourses, type PackageMeta, type CourseMeta } from "@/services/wordGameApi";

const router = useRouter();
const step = ref<1 | 2>(1);
const createdPackageId = ref<string | null>(null);
const createdPackageMeta = ref<PackageMeta | null>(null);

const metaForm = reactive({
  name: "",
  description: "",
  icon: "" as string,
  level: "自定义",
  isPublic: false,
});
const creating = ref(false);
const createError = ref("");

/** 已添加的节（用于展示），从 fetchPackageCourses 同步 */
const sections = ref<CourseMeta[]>([]);
const sectionForm = reactive({
  title: "",
  fileName: "",
  parsed: [] as Array<{ english: string; chinese: string; soundmark: string }>,
});
const sectionFileRef = ref<HTMLInputElement | null>(null);
const adding = ref(false);
const sectionError = ref("");

/** 判断 icon 是否为图片 URL */
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
        const dataUrl = canvas.toDataURL("image/jpeg", 0.82);
        resolve(dataUrl);
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

/** 选择图标文件：压缩后转 base64，避免请求体过大 */
async function onIconSelect(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file?.type.startsWith("image/")) return;
  try {
    metaForm.icon = await compressImageToDataUrl(file);
  } catch {
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      if (dataUrl) metaForm.icon = dataUrl;
    };
    reader.readAsDataURL(file);
  }
  input.value = "";
}

/**
 * 解析上传文件为题目列表（与 PackageListPage 一致）
 */
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

function onSectionFileSelect(e: Event) {
  sectionError.value = "";
  sectionForm.fileName = "";
  sectionForm.parsed = [];
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  sectionForm.fileName = file.name;
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const text = String(reader.result ?? "");
      sectionForm.parsed = parseUploadFile(text, file.name);
      if (sectionForm.parsed.length === 0) sectionError.value = "未解析到有效题目";
    } catch (err) {
      sectionError.value = err instanceof Error ? err.message : "解析失败";
    }
  };
  reader.readAsText(file, "UTF-8");
}

/** 创建课程包（仅元数据） */
async function createPackage() {
  if (!metaForm.name.trim()) return;
  creating.value = true;
  createError.value = "";
  try {
    const meta = await apiCreatePackage({
      name: metaForm.name.trim(),
      description: metaForm.description.trim(),
      icon: metaForm.icon.trim() || "📦",
      level: metaForm.level.trim() || "自定义",
      isPublic: metaForm.isPublic,
    });
    createdPackageId.value = meta.id;
    createdPackageMeta.value = meta;
    await loadSections();
    step.value = 2;
  } catch (err) {
    createError.value = err instanceof Error ? err.message : "创建失败";
  } finally {
    creating.value = false;
  }
}

/** 拉取当前包下的节列表（用于展示） */
async function loadSections() {
  if (!createdPackageId.value) return;
  try {
    const list = await fetchPackageCourses(createdPackageId.value);
    sections.value = list;
  } catch {
    sections.value = [];
  }
}

/** 添加一节 */
async function addSection() {
  if (!createdPackageId.value || sectionForm.parsed.length === 0) return;
  adding.value = true;
  sectionError.value = "";
  try {
    await addPackageSection(createdPackageId.value, {
      title: sectionForm.title.trim() || undefined,
      statements: sectionForm.parsed,
    });
    sectionForm.title = "";
    sectionForm.fileName = "";
    sectionForm.parsed = [];
    if (sectionFileRef.value) sectionFileRef.value.value = "";
    await loadSections();
  } catch (err) {
    sectionError.value = err instanceof Error ? err.message : "添加失败";
  } finally {
    adding.value = false;
  }
}

/** 完成并跳转到课程包详情 */
function finishAndGo() {
  if (createdPackageId.value) {
    router.push(`/package/${createdPackageId.value}`);
  } else {
    router.push("/");
  }
}

function goBack() {
  if (step.value === 2 && sections.value.length > 0) {
    if (confirm("当前已添加的节会保留，确定返回列表吗？")) router.push("/");
  } else {
    router.push("/");
  }
}

onMounted(() => {
  // 若从外部传入 packageId（扩展：编辑已有包追加节）可在此读取 query
});
</script>

<style scoped>
.package-upload-page {
  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
}
.page-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}
.header-inner {
  max-width: 720px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand { display: flex; align-items: center; gap: 14px; }
.brand-icon { font-size: 2rem; }
.brand-title { font-size: 1.25rem; font-weight: 700; color: var(--text-primary); }
.brand-sub { font-size: 0.8rem; color: var(--text-tertiary); margin-top: 2px; }

.upload-main {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
}
.step-title { font-size: 1.15rem; font-weight: 600; color: var(--text-primary); margin: 0 0 8px; }
.step-desc { font-size: 0.9rem; color: var(--text-tertiary); margin: 0 0 20px; }
.form-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  padding: 24px;
}
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 0.9rem; font-weight: 500; color: var(--text-secondary); margin-bottom: 6px; }
.form-group input[type="text"] {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 0.95rem;
}
.required { color: var(--danger-color, #dc3545); }
.form-row { display: flex; gap: 16px; flex-wrap: wrap; }
.form-group-icon { flex: 0 0 auto; }
.icon-upload-wrap { display: flex; align-items: center; gap: 12px; }
.icon-preview { width: 64px; height: 64px; border-radius: var(--border-radius-md); overflow: hidden; background: var(--bg-primary); }
.icon-preview-img { width: 100%; height: 100%; object-fit: cover; }
.icon-preview-emoji { font-size: 2rem; display: flex; align-items: center; justify-content: center; height: 100%; }
.icon-file-input { display: none; }
.checkbox-group label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.form-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 20px; }
.form-error { color: var(--danger-color, #dc3545); font-size: 0.85rem; margin-top: 8px; }
.format-tip { font-size: 0.8rem; color: var(--text-tertiary); margin-top: 6px; }
.file-upload-wrap { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.file-upload-input { display: none; }
.file-upload-name { font-size: 0.9rem; color: var(--text-secondary); }
.btn { padding: 10px 18px; border-radius: var(--border-radius-md); font-size: 0.95rem; cursor: pointer; border: 1px solid transparent; }
.btn-primary { background: var(--primary-color); color: #fff; }
.btn-ghost { background: transparent; color: var(--text-secondary); border-color: var(--border-color); }
.btn-outline { background: transparent; border: 1px solid var(--border-color); color: var(--text-primary); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.sections-summary { margin-bottom: 24px; padding: 16px; background: var(--bg-secondary); border-radius: var(--border-radius-md); border: 1px solid var(--border-color); }
.summary-label { font-size: 0.9rem; font-weight: 600; color: var(--text-primary); margin: 0 0 12px; }
.sections-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.section-item { display: flex; align-items: center; gap: 12px; font-size: 0.9rem; }
.section-num { width: 28px; height: 28px; border-radius: 50%; background: var(--primary-color); color: #fff; display: inline-flex; align-items: center; justify-content: center; font-weight: 600; }
.section-title { flex: 1; color: var(--text-primary); }
.section-count { color: var(--text-tertiary); }
.section-form { margin-top: 16px; }
</style>
