/**
 * word-game API 客户端
 * 开发：使用相对路径 /api，由 Vite 代理到 5201，避免跨域与 Failed to fetch
 * 生产：使用 VITE_WORD_GAME_API_BASE 或当前域名:端口 /api
 */
const BASE =
  import.meta.env.DEV
    ? "/api"
    : (import.meta.env.VITE_WORD_GAME_API_BASE ?? `${location.protocol}//${location.hostname}:${location.port}/api`);

/** 从 localStorage 读取 JWT Token（aispring 同步写入） */
function getToken(): string | null {
  return localStorage.getItem("wordGameAuthToken") || null;
}

/** 构建通用请求头 */
function buildHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;
  return headers;
}

/** 通用 fetch 包装，统一错误处理 */
async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: buildHeaders(),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  return res.json();
}

// ── 课程包 API ────────────────────────────────────────────────────────────────

export interface PackageMeta {
  id: string;
  name: string;
  description: string;
  icon: string;
  level: string;
  courseCount: number;
  totalQuestions: number;
  /** 用户上传的课程包 */
  isUserPackage?: boolean;
  /** 是否公开（所有用户可见） */
  isPublic?: boolean;
  /** 点击次数，用于排名（按点击量递减排序） */
  clickCount?: number;
}

export interface CourseMeta {
  index: number;
  title: string;
  count: number;
}

export interface Statement {
  chinese: string;
  english: string;
  soundmark: string;
}

/** 获取所有课程包列表，支持按名称/描述搜索；返回结果已按点击量降序排名 */
export async function fetchPackages(search?: string): Promise<PackageMeta[]> {
  const q = search?.trim() ? `?search=${encodeURIComponent(search.trim())}` : "";
  const res = await request<{ success: boolean; data: PackageMeta[] }>(`/packages${q}`);
  return res.data;
}

/** 记录课程包点击（用于排名），进入详情前调用 */
export async function recordPackageClick(packageId: string): Promise<void> {
  await request(`/packages/${encodeURIComponent(packageId)}/click`, { method: "POST" });
}

/**
 * 上传自定义课程包（固定格式，单次全量，内部会作为单节「第一课」）
 * statements 每项需包含 english, chinese, soundmark（音标可选）
 */
export async function uploadPackage(params: {
  name: string;
  description?: string;
  icon?: string;
  level?: string;
  isPublic: boolean;
  statements: Array<{ english: string; chinese: string; soundmark?: string }>;
}): Promise<PackageMeta> {
  const res = await request<{ success: boolean; data: PackageMeta; message?: string }>("/packages", {
    method: "POST",
    body: JSON.stringify({
      name: params.name,
      description: params.description ?? "",
      icon: params.icon ?? "📦",
      level: params.level ?? "自定义",
      isPublic: params.isPublic,
      statements: params.statements.map((s) => ({
        english: s.english,
        chinese: s.chinese,
        soundmark: s.soundmark ?? "",
      })),
    }),
  });
  return res.data;
}

/** 创建课程包（仅元数据），不包含任何节，后续用 addPackageSection 添加节 */
export async function createPackage(params: {
  name: string;
  description?: string;
  icon?: string;
  level?: string;
  isPublic: boolean;
}): Promise<PackageMeta> {
  const res = await request<{ success: boolean; data: PackageMeta; message?: string }>("/packages", {
    method: "POST",
    body: JSON.stringify({
      name: params.name,
      description: params.description ?? "",
      icon: params.icon ?? "📦",
      level: params.level ?? "自定义",
      isPublic: params.isPublic,
    }),
  });
  return res.data;
}

/** 为课程包追加一节（标题 + 题目列表），仅包所属用户可调用 */
export async function addPackageSection(
  packageId: string,
  params: {
    title?: string;
    statements: Array<{ english: string; chinese: string; soundmark?: string }>;
  }
): Promise<PackageMeta> {
  const res = await request<{ success: boolean; data: PackageMeta; message?: string }>(
    `/packages/${encodeURIComponent(packageId)}/sections`,
    {
      method: "POST",
      body: JSON.stringify({
        title: params.title ?? "",
        statements: params.statements.map((s) => ({
          english: s.english,
          chinese: s.chinese,
          soundmark: s.soundmark ?? "",
        })),
      }),
    }
  );
  return res.data;
}

/** 获取指定课程包内的课程列表 */
export async function fetchPackageCourses(packageId: string): Promise<CourseMeta[]> {
  const res = await request<{ success: boolean; data: CourseMeta[] }>(
    `/packages/${packageId}/courses`
  );
  return res.data;
}

/** 获取指定课程的题目列表；用户课程包需传 packageId（以 up- 开头） */
export async function fetchCourseQuestions(
  courseIndex: number,
  packageId?: string
): Promise<Statement[]> {
  const q = packageId ? `?packageId=${encodeURIComponent(packageId)}` : "";
  const res = await request<{ success: boolean; data: Statement[] }>(
    `/courses/${courseIndex}/questions${q}`
  );
  return res.data;
}

// ── 进度 API ──────────────────────────────────────────────────────────────────

export interface CourseProgressItem {
  currentQuestion: number;
  completed: boolean;
  studySeconds: number;
  updatedAt: string;
}

export type PackageProgress = Record<number, CourseProgressItem>;

/** 获取用户在某课程包内的全部进度（key 为 courseIndex） */
export async function fetchProgress(packageId: string): Promise<PackageProgress> {
  const res = await request<{ success: boolean; data: PackageProgress }>(
    `/progress?packageId=${encodeURIComponent(packageId)}`
  );
  return res.data;
}

/** 保存（或更新）某课程的学习进度 */
export async function saveProgress(params: {
  packageId: string;
  courseIndex: number;
  currentQuestion: number;
  completed: boolean;
  studySeconds?: number;
}): Promise<void> {
  await request("/progress", {
    method: "POST",
    body: JSON.stringify(params),
  });
}

/**
 * 登录后迁移游客进度到用户账号
 * @param userId - 登录用户的 userId
 * @param packageId - 可选，限制迁移范围
 */
export async function migrateGuestProgress(
  userId: number | string,
  packageId?: string
): Promise<{ migrated: number }> {
  const res = await request<{ success: boolean; migrated: number }>(
    "/progress/migrate",
    {
      method: "POST",
      body: JSON.stringify({ userId, packageId }),
    }
  );
  return { migrated: res.migrated };
}
