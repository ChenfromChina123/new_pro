/**
 * word-game API 服务器
 * 提供课程包数据、学习进度；已接入 aispring 登录，API 需携带有效 JWT
 * 端口: 5201
 */
import express from "express";
import cors from "cors";
import { createRequire } from "module";
import { readFileSync, readdirSync, existsSync } from "fs";
import { join, dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { randomUUID } from "crypto";
import Database from "better-sqlite3";

const require = createRequire(import.meta.url);
const jwt = require("jsonwebtoken");
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

  -- 用户上传的课程包（可私有或公开）
  CREATE TABLE IF NOT EXISTS user_packages (
    id          TEXT    PRIMARY KEY,
    user_key    TEXT    NOT NULL,
    name        TEXT    NOT NULL,
    description TEXT    NOT NULL DEFAULT '',
    icon        TEXT    NOT NULL DEFAULT '📦',
    level       TEXT    NOT NULL DEFAULT '自定义',
    is_public   INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
  );
  CREATE INDEX IF NOT EXISTS idx_user_packages_user ON user_packages(user_key);
  CREATE INDEX IF NOT EXISTS idx_user_packages_public ON user_packages(is_public);

  -- 用户课程包中的题目，按节（course_index 0-based）组织
  CREATE TABLE IF NOT EXISTS user_package_statements (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    package_id  TEXT    NOT NULL,
    course_index INTEGER NOT NULL DEFAULT 0,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    english     TEXT    NOT NULL,
    chinese     TEXT    NOT NULL,
    soundmark   TEXT    NOT NULL DEFAULT '',
    FOREIGN KEY (package_id) REFERENCES user_packages(id) ON DELETE CASCADE
  );
  CREATE INDEX IF NOT EXISTS idx_ups_package ON user_package_statements(package_id);
  CREATE INDEX IF NOT EXISTS idx_ups_package_course ON user_package_statements(package_id, course_index);

  -- 用户课程包中的“节”信息（节标题等，便于扩展）
  CREATE TABLE IF NOT EXISTS user_package_courses (
    package_id   TEXT    NOT NULL,
    course_index INTEGER NOT NULL,
    title        TEXT    NOT NULL DEFAULT '',
    created_at   TEXT    NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (package_id, course_index),
    FOREIGN KEY (package_id) REFERENCES user_packages(id) ON DELETE CASCADE
  );

  -- 兼容旧表：若无 course_index 列则添加（SQLite 3.35+ 支持 ADD COLUMN … DEFAULT）
  -- 通过 pragma 检查列是否存在，若不存在再 ALTER（better-sqlite3 无信息模式则用 try/catch）
  -- 此处建表已包含 course_index，仅对已有库做迁移
`);
try {
  db.prepare(`SELECT course_index FROM user_package_statements LIMIT 1`).get();
} catch {
  try {
    db.exec(`ALTER TABLE user_package_statements ADD COLUMN course_index INTEGER NOT NULL DEFAULT 0`);
  } catch (_) {}
}
// 为已有用户包回填一节（便于 GET courses 走统一逻辑）
try {
  const userPkgIds = db.prepare(`SELECT id FROM user_packages`).all();
  const ins = db.prepare(
    `INSERT OR IGNORE INTO user_package_courses (package_id, course_index, title) VALUES (?, 0, '第一课')`
  );
  for (const p of userPkgIds) {
    ins.run(p.id);
  }
} catch (_) {}

db.exec(`
  -- 课程包点击次数（内置包与用户包统一存储，用于排名）
  CREATE TABLE IF NOT EXISTS package_clicks (
    package_id TEXT PRIMARY KEY,
    click_count INTEGER NOT NULL DEFAULT 0
  );
`);

// 旧数据回填：为已有题目但无节信息的包插入默认一节「第一课」
try {
  const packagesWithStatements = db.prepare(`
    SELECT DISTINCT package_id FROM user_package_statements
  `).all();
  const insertCourse = db.prepare(`
    INSERT OR IGNORE INTO user_package_courses (package_id, course_index, title) VALUES (?, 0, '第一课')
  `);
  for (const { package_id } of packagesWithStatements) {
    insertCourse.run(package_id);
  }
} catch (_) {}

// ── 课程数据加载 ──────────────────────────────────────────────────────────────

/** 课程 JSON 文件所在目录（packages/xingrong-courses/data/courses） */
const COURSES_DIR = resolve(
  __dirname,
  "../packages/xingrong-courses/data/courses"
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
      // 兼容 aispring JWT：user_id（下划线）及常见字段
      const userId = payload.user_id ?? payload.userId ?? payload.sub ?? payload.id;
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

/**
 * 获取与 aispring 一致的 JWT 签名密钥（至少 32 字节，用于 HS256）
 * 生产环境请设置环境变量 JWT_SECRET_KEY 与 aispring 的 jwt.secret 一致
 */
function getJwtSigningKey() {
  const secret = process.env.JWT_SECRET_KEY || "default_development_key_change_in_production";
  let keyBytes = Buffer.from(secret, "utf8");
  if (keyBytes.length < 32) {
    const extended = Buffer.alloc(32, 0);
    keyBytes.copy(extended, 0);
    keyBytes = extended;
  }
  return keyBytes;
}

/**
 * aispring 登录验证中间件：/api 开头的请求必须携带有效 JWT，否则 401
 */
function requireAispringAuth(req, res, next) {
  try {
    const auth = req.headers["authorization"];
    if (!auth || !auth.startsWith("Bearer ")) {
      res.status(401).json({ success: false, message: "未登录或 token 缺失" });
      return;
    }
    const token = auth.slice(7);
    const key = getJwtSigningKey();
    jwt.verify(token, key, { algorithms: ["HS256"] });
    next();
  } catch (err) {
    if (res.headersSent) return next(err);
    const msg = err.name === "TokenExpiredError" ? "登录已过期" : (err.name === "JsonWebTokenError" ? "无效的登录凭证" : (err.message || "认证失败"));
    res.status(401).set("Content-Type", "application/json").json({ success: false, message: msg });
  }
}

// ── Express 应用 ──────────────────────────────────────────────────────────────

const app = express();

app.use(
  cors({
    origin: true,        // 允许所有来源（iframe 场景）
    credentials: true,
  })
);
// 课程包图标等可能为 base64，需放宽请求体限制（默认约 100kb）
app.use(express.json({ limit: "10mb" }));

// 健康检查（无需登录，用于确认 5201 服务与 JSON 响应正常）
app.get("/api/ping", (req, res) => {
  res.set("Content-Type", "application/json").json({ success: true, message: "pong" });
});

// 所有其它 /api 请求必须通过 aispring JWT 验证
app.use("/api", requireAispringAuth);

// ── 路由：课程包 ──────────────────────────────────────────────────────────────

/**
 * 获取用户上传的课程包列表（公开 + 当前用户自己的）
 * 支持 ?search= 按名称、描述模糊搜索
 */
function getUserPackagesList(userKey, search) {
  let sql = `
    SELECT id, user_key, name, description, icon, level, is_public, created_at
    FROM user_packages
    WHERE is_public = 1 OR user_key = ?
  `;
  const args = [userKey];
  if (search && String(search).trim()) {
    sql += ` AND (name LIKE ? OR description LIKE ?)`;
    const term = `%${String(search).trim()}%`;
    args.push(term, term);
  }
  sql += ` ORDER BY created_at DESC`;
  return db.prepare(sql).all(...args);
}

/**
 * 将用户包转为与 PACKAGES 一致的 meta 结构，并统计题目数、节数
 * 节数来自 user_package_courses，若无则回退为 statements 总数（兼容旧数据）
 */
function userPackageToMeta(row) {
  const totalStmt = db
    .prepare(`SELECT COUNT(*) AS cnt FROM user_package_statements WHERE package_id = ?`)
    .get(row.id);
  const total = (totalStmt && totalStmt.cnt) || 0;
  let courseCount = total > 0 ? 1 : 0;
  try {
    const courseRows = db
      .prepare(
        `SELECT course_index FROM user_package_courses WHERE package_id = ? ORDER BY course_index`
      )
      .all(row.id);
    if (courseRows.length > 0) courseCount = courseRows.length;
  } catch (_) {
    // 兼容旧库无 user_package_courses 表
  }
  return {
    id: row.id,
    name: row.name,
    description: row.description || "",
    icon: row.icon || "📦",
    level: row.level || "自定义",
    courseCount,
    totalQuestions: total,
    isUserPackage: true,
    isPublic: !!row.is_public,
  };
}

/** 获取某课程包的点击次数 */
function getPackageClickCount(packageId) {
  const row = db.prepare(`SELECT click_count FROM package_clicks WHERE package_id = ?`).get(packageId);
  return row ? (row.click_count || 0) : 0;
}

/** 增加课程包点击次数（内置包与用户包统一写入 package_clicks） */
function incrementPackageClick(packageId) {
  db.prepare(
    `INSERT INTO package_clicks (package_id, click_count) VALUES (?, 1)
     ON CONFLICT(package_id) DO UPDATE SET click_count = click_count + 1`
  ).run(packageId);
}

/** GET /api/packages — 返回内置 + 用户课程包列表，支持 ?search= 搜索，按点击量降序排名 */
app.get("/api/packages", (req, res) => {
  const { key } = resolveUserKey(req);
  const search = req.query.search ? String(req.query.search).trim() : "";
  let list = [];
  if (!search) {
    list = PACKAGES.map((p) => ({ ...p }));
  } else {
    const term = search.toLowerCase();
    list = PACKAGES.filter(
      (p) =>
        (p.name && p.name.toLowerCase().includes(term)) ||
        (p.description && p.description.toLowerCase().includes(term))
    ).map((p) => ({ ...p }));
  }
  const userRows = getUserPackagesList(key, search || null);
  for (const row of userRows) {
    list.push(userPackageToMeta(row));
  }
  // 合并点击次数并按点击量降序排序
  list = list.map((p) => ({
    ...p,
    clickCount: getPackageClickCount(p.id),
  }));
  list.sort((a, b) => (b.clickCount || 0) - (a.clickCount || 0));
  res.json({ success: true, data: list });
});

/** POST /api/packages/:packageId/click — 记录课程包点击（用于排名） */
app.post("/api/packages/:packageId/click", (req, res) => {
  const packageId = req.params.packageId;
  const exists =
    PACKAGES.some((p) => p.id === packageId) ||
    db.prepare(`SELECT 1 FROM user_packages WHERE id = ?`).get(packageId);
  if (!exists) {
    return res.status(404).json({ success: false, message: "课程包不存在" });
  }
  incrementPackageClick(packageId);
  res.json({ success: true });
});

/** GET /api/packages/:packageId/courses — 返回指定课程包内的课程列表（节列表，index 从 1 开始） */
app.get("/api/packages/:packageId/courses", (req, res) => {
  const packageId = req.params.packageId;
  const pkg = PACKAGES.find((p) => p.id === packageId);
  if (pkg) {
    return res.json({ success: true, data: courseMetas });
  }
  const userPkg = db.prepare(`SELECT id FROM user_packages WHERE id = ?`).get(packageId);
  if (userPkg) {
    const courses = db
      .prepare(
        `SELECT package_id, course_index, title FROM user_package_courses WHERE package_id = ? ORDER BY course_index`
      )
      .all(packageId);
    if (courses.length > 0) {
      const data = courses.map((c, i) => {
        const countRow = db
          .prepare(
            `SELECT COUNT(*) AS cnt FROM user_package_statements WHERE package_id = ? AND course_index = ?`
          )
          .get(packageId, c.course_index);
        return {
          index: i + 1,
          title: c.title || toChineseTitle(i + 1),
          count: (countRow && countRow.cnt) || 0,
        };
      });
      return res.json({ success: true, data });
    }
    // 兼容旧数据：无 user_package_courses 时按单节返回
    const count = db
      .prepare(`SELECT COUNT(*) AS cnt FROM user_package_statements WHERE package_id = ?`)
      .get(packageId);
    const total = (count && count.cnt) || 0;
    res.json({
      success: true,
      data: [{ index: 1, title: "第一课", count: total }],
    });
    return;
  }
  res.status(404).json({ success: false, message: "课程包不存在" });
});

/** GET /api/courses/:courseIndex/questions — 返回指定课程的题目列表；支持 ?packageId= 用户课程包；courseIndex 为 1-based */
app.get("/api/courses/:courseIndex/questions", (req, res) => {
  const packageId = req.query.packageId;
  const idx = parseInt(req.params.courseIndex, 10);
  if (isNaN(idx) || idx < 1) {
    return res.status(400).json({ success: false, message: "无效的课程索引" });
  }
  if (packageId && String(packageId).startsWith("up-")) {
    const courseIndex0 = idx - 1;
    const rows = db
      .prepare(
        `SELECT english, chinese, soundmark FROM user_package_statements WHERE package_id = ? AND course_index = ? ORDER BY sort_order, id`
      )
      .all(packageId, courseIndex0);
    const statements = rows.map((r) => ({
      english: r.english,
      chinese: r.chinese,
      soundmark: r.soundmark || "",
    }));
    return res.json({ success: true, data: statements });
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

/**
 * POST /api/packages — 创建自定义课程包（支持仅元数据、单次全量、分节）
 * Body: { name, description?, icon?, level?, isPublic [, statements ] [, sections: [{ title, statements }] ] }
 * - 仅 name + isPublic：创建空包，后续用 POST /packages/:id/sections 添加节
 * - statements（数组）：兼容旧版，视为单节「第一课」
 * - sections（数组）：多节，每节 { title, statements }
 */
app.post("/api/packages", (req, res) => {
  try {
    const { key } = resolveUserKey(req);
    const { name, description, icon, level, isPublic, statements, sections } = req.body || {};
    if (!name || typeof name !== "string" || !name.trim()) {
      return res.status(400).json({ success: false, message: "课程包名称不能为空" });
    }
    const packageId = "up-" + randomUUID().replace(/-/g, "").slice(0, 16);
    const insertPkg = db.prepare(`
      INSERT INTO user_packages (id, user_key, name, description, icon, level, is_public)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `);
    const insertCourse = db.prepare(`
      INSERT INTO user_package_courses (package_id, course_index, title) VALUES (?, ?, ?)
    `);
    const insertStmt = db.prepare(`
      INSERT INTO user_package_statements (package_id, course_index, sort_order, english, chinese, soundmark)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    insertPkg.run(
      packageId,
      key,
      String(name).trim().slice(0, 200),
      (description != null && description !== undefined ? String(description) : "").slice(0, 1000),
      (icon != null && icon !== undefined ? String(icon) : "📦").slice(0, 5000),
      (level != null && level !== undefined ? String(level) : "自定义").slice(0, 50),
      isPublic ? 1 : 0
    );
    const sectionList = Array.isArray(sections) && sections.length > 0
      ? sections
      : Array.isArray(statements) && statements.length > 0
        ? [{ title: "第一课", statements }]
        : [];
    let courseIndex = 0;
    for (const sec of sectionList) {
      const title = (sec.title != null && sec.title !== undefined ? String(sec.title).trim() : "") || toChineseTitle(courseIndex + 1);
      const list = Array.isArray(sec.statements) ? sec.statements : [];
      insertCourse.run(packageId, courseIndex, title.slice(0, 200));
      let order = 0;
      for (const s of list) {
        const en = (s.english != null ? String(s.english) : "").trim();
        const zh = (s.chinese != null ? String(s.chinese) : "").trim();
        if (!en && !zh) continue;
        insertStmt.run(
          packageId,
          courseIndex,
          order++,
          en.slice(0, 2000),
          zh.slice(0, 2000),
          (s.soundmark != null ? String(s.soundmark) : "").slice(0, 200)
        );
      }
      courseIndex++;
    }
    const meta = userPackageToMeta(
      db.prepare(`SELECT * FROM user_packages WHERE id = ?`).get(packageId)
    );
    res.status(201).json({ success: true, data: meta, message: sectionList.length > 0 ? "上传成功" : "课程包已创建，请添加节" });
  } catch (e) {
    console.error("[upload package]", e);
    const msg = (e && typeof e.message === "string" ? e.message : String(e && e.code || "保存失败"));
    res.status(500).set("Content-Type", "application/json").json({ success: false, message: msg });
  }
});

/**
 * POST /api/packages/:packageId/sections — 为已有课程包追加一节
 * Body: { title?, statements: [{ english, chinese, soundmark? }] }
 * 仅允许包所属用户操作
 */
app.post("/api/packages/:packageId/sections", (req, res) => {
  const { key } = resolveUserKey(req);
  const packageId = req.params.packageId;
  if (!packageId || !String(packageId).startsWith("up-")) {
    return res.status(400).json({ success: false, message: "无效的课程包 ID" });
  }
  const row = db.prepare(`SELECT id, user_key FROM user_packages WHERE id = ?`).get(packageId);
  if (!row || row.user_key !== key) {
    return res.status(404).json({ success: false, message: "课程包不存在或无权操作" });
  }
  const { title, statements } = req.body || {};
  const list = Array.isArray(statements) ? statements : [];
  if (list.length === 0) {
    return res.status(400).json({ success: false, message: "至少需要一条题目" });
  }
  const nextIndexRow = db.prepare(
    `SELECT COALESCE(MAX(course_index), -1) + 1 AS next FROM user_package_courses WHERE package_id = ?`
  ).get(packageId);
  const courseIndex = (nextIndexRow && nextIndexRow.next != null) ? nextIndexRow.next : 0;
  const insertCourse = db.prepare(`
    INSERT INTO user_package_courses (package_id, course_index, title) VALUES (?, ?, ?)
  `);
  const insertStmt = db.prepare(`
    INSERT INTO user_package_statements (package_id, course_index, sort_order, english, chinese, soundmark)
    VALUES (?, ?, ?, ?, ?, ?)
  `);
  try {
    const sectionTitle = (title != null && title !== undefined ? String(title).trim() : "") || toChineseTitle(courseIndex + 1);
    insertCourse.run(packageId, courseIndex, sectionTitle.slice(0, 200));
    let order = 0;
    for (const s of list) {
      const en = (s.english != null ? String(s.english) : "").trim();
      const zh = (s.chinese != null ? String(s.chinese) : "").trim();
      if (!en && !zh) continue;
      insertStmt.run(
        packageId,
        courseIndex,
        order++,
        en.slice(0, 2000),
        zh.slice(0, 2000),
        (s.soundmark != null ? String(s.soundmark) : "").slice(0, 200)
      );
    }
    const meta = userPackageToMeta(db.prepare(`SELECT * FROM user_packages WHERE id = ?`).get(packageId));
    res.status(201).json({ success: true, data: meta, message: "节已添加" });
  } catch (e) {
    console.error("[add section]", e);
    res.status(500).json({ success: false, message: "添加节失败" });
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

// 全局错误处理：未捕获异常时统一返回 JSON 500
app.use((err, req, res, next) => {
  console.error("[express error]", err);
  if (res.headersSent) return next(err);
  res.status(500).set("Content-Type", "application/json").json({
    success: false,
    message: err.message || "服务器内部错误",
  });
});

// ── 启动 ──────────────────────────────────────────────────────────────────────

app.listen(PORT, () => {
  console.log(`[word-game API] 服务已启动: http://localhost:${PORT}`);
  console.log(`[word-game API] 进度数据库: ${join(__dirname, "progress.db")}`);
});
