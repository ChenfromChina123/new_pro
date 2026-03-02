import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./styles/global.css";
import { migrateGuestProgress } from "@/services/wordGameApi";

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount("#app");

// ─────────────────────────────────────────
//  学习统计追踪（向 aispring 侧边栏上报）
// ─────────────────────────────────────────
interface WordGameStats {
  completedCourses: number;
  totalSeconds: number;
  todaySessions: number;
  lastStudyDate: string;
}

function loadStats(): WordGameStats {
  try {
    const raw = localStorage.getItem("wordGameStats");
    if (raw) return JSON.parse(raw);
  } catch { /* ignore */ }
  return { completedCourses: 0, totalSeconds: 0, todaySessions: 0, lastStudyDate: "" };
}

function reportStats(): void {
  if (window.self === window.top) return;
  const stats = loadStats();
  window.parent.postMessage({ type: "WORD_GAME_STATS", stats }, "*");
}

let _sessionStart = Date.now();

function saveSessionTime(): void {
  const elapsed = Math.floor((Date.now() - _sessionStart) / 1000);
  if (elapsed < 5) return;
  const stats = loadStats();
  const today = new Date().toISOString().slice(0, 10);
  stats.totalSeconds += elapsed;
  if (stats.lastStudyDate !== today) {
    stats.todaySessions = 0;
    stats.lastStudyDate = today;
  }
  stats.todaySessions += 1;
  localStorage.setItem("wordGameStats", JSON.stringify(stats));
  reportStats();
  _sessionStart = Date.now();
}

document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "hidden") saveSessionTime();
  else _sessionStart = Date.now();
});
window.addEventListener("beforeunload", saveSessionTime);

// ─────────────────────────────────────────
//  父窗口消息处理
// ─────────────────────────────────────────
/**
 * 监听父窗口（aispring）发送的消息：
 *  - AISPRING_THEME   : 同步暗色/浅色主题
 *  - REQUEST_STATS    : 上报学习统计数据到侧边栏
 *  - AISPRING_LOGIN   : 用户登录，携带 JWT token；触发游客进度迁移并持久化 token
 *  - AISPRING_LOGOUT  : 用户退出，清除 token
 */
window.addEventListener("message", async (event) => {
  const data = event.data;
  if (!data || typeof data !== "object") return;

  switch (data.type) {
    // 主题同步
    case "AISPRING_THEME": {
      const isDark: boolean = !!data.isDark;
      document.body.classList.toggle("dark-mode", isDark);
      localStorage.setItem("wordGameDarkMode", String(isDark));
      break;
    }

    // 侧边栏请求统计数据
    case "REQUEST_STATS": {
      reportStats();
      break;
    }

    /**
     * 用户登录事件
     * data.token   : JWT 字符串
     * data.userId  : 用户 ID（number）
     *
     * 逻辑：
     * 1. 将 token 写入 localStorage，后续 API 请求自动携带
     * 2. 请求后端迁移游客进度（只迁移用户尚无记录的课程）
     */
    case "AISPRING_LOGIN": {
      if (data.token) {
        localStorage.setItem("wordGameAuthToken", data.token);
      }
      if (data.userId) {
        try {
          const result = await migrateGuestProgress(data.userId);
          console.log(`[word-game] 进度迁移完成，共迁移 ${result.migrated} 条记录`);
        } catch (e) {
          console.warn("[word-game] 进度迁移失败", e);
        }
      }
      break;
    }

    // 用户退出，清除 token（回到游客模式）
    case "AISPRING_LOGOUT": {
      localStorage.removeItem("wordGameAuthToken");
      break;
    }
  }
});
