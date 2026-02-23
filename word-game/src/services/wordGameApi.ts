/**
 * word-game API 客户端
 * 统一封装对后端 API Server（端口 5201）的请求
 */

/** API 基础地址：开发时用 localhost:5201，生产时同域端口 5201 */
const BASE =
  import.meta.env.DEV
    ? "http://localhost:5201/api"
    : `${location.protocol}//${location.hostname}:5201/api`;

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

/** 获取所有课程包列表 */
export async function fetchPackages(): Promise<PackageMeta[]> {
  const res = await request<{ success: boolean; data: PackageMeta[] }>("/packages");
  return res.data;
}

/** 获取指定课程包内的课程列表 */
export async function fetchPackageCourses(packageId: string): Promise<CourseMeta[]> {
  const res = await request<{ success: boolean; data: CourseMeta[] }>(
    `/packages/${packageId}/courses`
  );
  return res.data;
}

/** 获取指定课程的题目列表 */
export async function fetchCourseQuestions(courseIndex: number): Promise<Statement[]> {
  const res = await request<{ success: boolean; data: Statement[] }>(
    `/courses/${courseIndex}/questions`
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
