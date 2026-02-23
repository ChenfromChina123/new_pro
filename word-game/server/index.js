/**
 * word-game API 服务器
 * 提供课程包数据、学习进度（支持游客 IP 标识 + 登录用户 JWT）
 * 端口: 5201
 */
import express from "express";
import cors from "cors";
import { createRequire } from "module";
import { readFileSync, readdirSync, existsSync } from "fs";
import { join, dirname, resolve } from "path";
import { fileURLToPath } from "url";
import Database from "better-sqlite3";

const require = createRequire(import.meta.url);
const __dirname = dirname(fileURLToPath(import.meta.url));
/** 端口：生产环境 Nginx 反代 earthworm.aistudy.icu 到 5010，此处通过环境变量 PORT 配置 */
const PORT = parseInt(process.env.PORT || "5201", 10);

// ── 数据库初始化 ──────────────────────────────────────────────────────────────

const db = new Database(join(__dirname, "progress.db"));
db.pragma("journal_mode = WAL");

db.exec(`
  CREATE TABLE IF NOT EXISTS course_progress (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_key    TEXT    NOT NULL,
    user_type   TEXT    NOT NULL DEFAULT 'guest',
    package_id  TEXT    NOT NULL,
    course_index INTEGER NOT NULL,
    current_q   INTEGER NOT NULL DEFAULT 0,
    completed   INTEGER NOT NULL DEFAULT 0,
    study_secs  INTEGER NOT NULL DEFAULT 0,
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    UNIQUE(user_key, package_id, course_index)
  );
  CREATE INDEX IF NOT EXISTS idx_progress_user ON course_progress(user_key);
`);

// ── 课程数据加载 ──────────────────────────────────────────────────────────────

/** 课程 JSON 文件所在目录（monorepo 中的 packages/xingrong-courses/data/courses） */
const COURSES_DIR = resolve(
  __dirname,
  "../../packages/xingrong-courses/data/courses"
);

/** 将数字转为中文课程标题，如 1 → "第一课" */
function toChineseTitle(num) {
  const nums = ["零","一","二","三","四","五","六","七","八","九","十"];
  if (num <= 10) return `第${nums[num]}课`;
  const tens = Math.floor(num / 10);
  const ones = num % 10;
  const tensStr = tens === 1 ? "" : nums[tens];
  const onesStr = ones === 0 ? "" : nums[ones];
  return `第${tensStr}十${onesStr}课`;
}

/** 读取并缓存所有课程元数据 */
function buildCourseMetas() {
  if (!existsSync(COURSES_DIR)) {
    console.warn(`[警告] 课程目录不存在: ${COURSES_DIR}`);
    return [];
  }
  const files = readdirSync(COURSES_DIR)
    .filter((f) => f.endsWith(".json"))
    .sort((a, b) => {
      const na = parseInt(a, 10);
      const nb = parseInt(b, 10);
      return na - nb;
    });

  return files.map((f) => {
    const idx = parseInt(f, 10);
    const raw = readFileSync(join(COURSES_DIR, f), "utf-8");
    const statements = JSON.parse(raw);
    return {
      index: idx,
      title: toChineseTitle(idx),
      count: statements.length,
    };
  });
}

const courseMetas = buildCourseMetas();
console.log(`[启动] 已加载 ${courseMetas.length} 门课程元数据`);

/** 课程包定义（可扩展） */
const PACKAGES = [
  {
    id: "xingrong-beginner",
    name: "星荣零基础学英语",
    description: "从零开始，系统掌握日常英语词汇与基础句型，适合完全零基础的学习者。",
    icon: "🌟",
    level: "零基础",
    courseCount: courseMetas.length,
    totalQuestions: courseMetas.reduce((s, c) => s + c.count, 0),
  },
];

// ── 工具函数 ──────────────────────────────────────────────────────────────────

/**
 * 从请求中解析用户标识
 * - 已登录：从 Authorization 头解析 userId（简单 Base64 解码 JWT payload）
 * - 未登录：使用 IP 地址作为游客标识
 */
function resolveUserKey(req) {
  const auth = req.headers["authorization"];
  if (auth && auth.startsWith("Bearer ")) {
    try {
      const token = auth.slice(7);
      const payloadB64 = token.split(".")[1];
      if (!payloadB64) throw new Error("invalid token");
      const payload = JSON.parse(
        Buffer.from(payloadB64.replace(/-/g, "+").replace(/_/g, "/"), "base64").toString("utf-8")
      );
      const userId = payload.userId || payload.sub || payload.id;
      if (userId) return { key: String(userId), type: "user" };
    } catch {
      // token 无效，回退到游客模式
    }
  }
  // 获取真实 IP（支持 nginx 代理）
  const ip =
    req.headers["x-forwarded-for"]?.split(",")[0]?.trim() ||
    req.socket?.remoteAddress ||
    "unknown";
  return { key: `guest_${ip}`, type: "guest" };
}

// ── Express 应用 ──────────────────────────────────────────────────────────────

const app = express();

app.use(
  cors({
    origin: true,        // 允许所有来源（iframe 场景）
    credentials: true,
  })
);
app.use(express.json());

// ── 路由：课程包 ──────────────────────────────────────────────────────────────

/** GET /api/packages — 返回所有课程包列表 */
app.get("/api/packages", (req, res) => {
  res.json({ success: true, data: PACKAGES });
});

/** GET /api/packages/:packageId/courses — 返回指定课程包内的课程列表 */
app.get("/api/packages/:packageId/courses", (req, res) => {
  const pkg = PACKAGES.find((p) => p.id === req.params.packageId);
  if (!pkg) return res.status(404).json({ success: false, message: "课程包不存在" });
  // 只返回元数据，不含题目内容
  res.json({ success: true, data: courseMetas });
});

/** GET /api/courses/:courseIndex/questions — 返回指定课程的题目列表 */
app.get("/api/courses/:courseIndex/questions", (req, res) => {
  const idx = parseInt(req.params.courseIndex, 10);
  if (isNaN(idx) || idx < 1) {
    return res.status(400).json({ success: false, message: "无效的课程索引" });
  }
  const filePath = join(COURSES_DIR, `${String(idx).padStart(2, "0")}.json`);
  if (!existsSync(filePath)) {
    return res.status(404).json({ success: false, message: "课程数据不存在" });
  }
  try {
    const raw = readFileSync(filePath, "utf-8");
    const statements = JSON.parse(raw);
    res.json({ success: true, data: statements });
  } catch (e) {
    res.status(500).json({ success: false, message: "课程数据读取失败" });
  }
});

// ── 路由：学习进度 ────────────────────────────────────────────────────────────

/** GET /api/progress?packageId=xxx — 获取当前用户在某课程包内所有课程的进度 */
app.get("/api/progress", (req, res) => {
  const { key, type } = resolveUserKey(req);
  const { packageId } = req.query;
  if (!packageId) {
    return res.status(400).json({ success: false, message: "packageId 不能为空" });
  }
  const rows = db
    .prepare(
      `SELECT course_index, current_q, completed, study_secs, updated_at
       FROM course_progress
       WHERE user_key = ? AND package_id = ?`
    )
    .all(key, packageId);

  const progress = {};
  for (const row of rows) {
    progress[row.course_index] = {
      currentQuestion: row.current_q,
      completed: row.completed === 1,
      studySeconds: row.study_secs,
      updatedAt: row.updated_at,
    };
  }
  res.json({ success: true, userType: type, data: progress });
});

/** POST /api/progress — 保存（更新）某课程的学习进度 */
app.post("/api/progress", (req, res) => {
  const { key, type } = resolveUserKey(req);
  const { packageId, courseIndex, currentQuestion, completed, studySeconds } = req.body;
  if (!packageId || courseIndex == null) {
    return res.status(400).json({ success: false, message: "packageId 和 courseIndex 必填" });
  }
  db.prepare(
    `INSERT INTO course_progress (user_key, user_type, package_id, course_index, current_q, completed, study_secs, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
     ON CONFLICT(user_key, package_id, course_index) DO UPDATE SET
       current_q   = excluded.current_q,
       completed   = excluded.completed,
       study_secs  = excluded.study_secs,
       updated_at  = excluded.updated_at`
  ).run(
    key,
    type,
    packageId,
    Number(courseIndex),
    Number(currentQuestion ?? 0),
    completed ? 1 : 0,
    Number(studySeconds ?? 0)
  );
  res.json({ success: true });
});

/**
 * POST /api/progress/migrate
 * 用户登录后，将游客进度合并到用户账号
 * 仅在用户账号尚无进度时才迁移（首次登录逻辑）
 * Body: { userId, packageId? }
 */
app.post("/api/progress/migrate", (req, res) => {
  const { key: guestKey } = resolveUserKey(req);
  const { userId, packageId } = req.body;
  if (!userId) {
    return res.status(400).json({ success: false, message: "userId 必填" });
  }
  const userKey = String(userId);

  // 查找此游客的进度记录
  const guestQuery = packageId
    ? db.prepare(`SELECT * FROM course_progress WHERE user_key = ? AND package_id = ?`).all(guestKey, packageId)
    : db.prepare(`SELECT * FROM course_progress WHERE user_key = ?`).all(guestKey);

  if (guestQuery.length === 0) {
    return res.json({ success: true, migrated: 0, message: "游客无进度记录" });
  }

  let migrated = 0;
  const insertOrSkip = db.prepare(
    `INSERT OR IGNORE INTO course_progress
       (user_key, user_type, package_id, course_index, current_q, completed, study_secs, updated_at)
     VALUES (?, 'user', ?, ?, ?, ?, ?, ?)`
  );

  const migrate = db.transaction(() => {
    for (const row of guestQuery) {
      // 已存在用户进度的课程不覆盖
      const existing = db
        .prepare(`SELECT id FROM course_progress WHERE user_key = ? AND package_id = ? AND course_index = ?`)
        .get(userKey, row.package_id, row.course_index);
      if (!existing) {
        insertOrSkip.run(
          userKey,
          row.package_id,
          row.course_index,
          row.current_q,
          row.completed,
          row.study_secs,
          row.updated_at
        );
        migrated++;
      }
    }
  });

  migrate();
  res.json({ success: true, migrated, message: `已迁移 ${migrated} 条游客进度` });
});

// ── 生产环境：托管前端静态资源（Nginx 反代 earthworm.aistudy.icu 到本服务时使用）────
// 构建时 base 为 /word-game/，故静态资源挂在 /word-game
// 注意：Express 5 / path-to-regexp 不支持匿名 "/word-game/*"，改用正则避免 PathError
const distPath = join(__dirname, "..", "dist");
if (existsSync(distPath)) {
  app.get("/", (req, res) => res.redirect(302, "/word-game/"));
  app.use("/word-game", express.static(distPath));
  app.get("/word-game", (req, res) => res.redirect(301, "/word-game/"));
  app.get(/^\/word-game\/.+/, (req, res) => res.sendFile(join(distPath, "index.html")));
}

// ── 启动 ──────────────────────────────────────────────────────────────────────

app.listen(PORT, () => {
  console.log(`[word-game API] 服务已启动: http://localhost:${PORT}`);
  console.log(`[word-game API] 进度数据库: ${join(__dirname, "progress.db")}`);
});
