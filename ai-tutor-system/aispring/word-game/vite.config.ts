import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";
import obfuscator from "rollup-plugin-obfuscator";

const isProd = process.env.NODE_ENV === "production";

/**
 * Vite 配置
 * 生产模式开启 JS 代码混淆（rollup-plugin-obfuscator）
 * 开发模式保持可读以便调试
 */
export default defineConfig({
  plugins: [vue()],
  base: isProd ? "/word-game/" : "/",
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
    },
  },
  server: {
    port: 5200,
    host: true,  // 允许外部访问
    cors: true,  // 允许跨域请求（支持 iframe 嵌入）
    fs: {
      allow: [resolve(__dirname, ".."), resolve(__dirname)],
    },
    proxy: {
      // 开发时将 /api 代理到 word-game API 服务器 (后端端口为 5201)
      "/api": {
        target: "http://127.0.0.1:5201",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
    // 生产构建开启混淆
    rollupOptions: {
      output: {
        manualChunks: {
          "vue-vendor": ["vue", "vue-router", "pinia"],
        },
      },
      plugins: isProd
        ? [
            obfuscator({
              // 仅混淆业务逻辑代码
              // 排除 data/courses.ts（含 import.meta.glob，需 Vite 提前处理）
              include: ["src/**/*.ts", "src/**/*.vue"],
              exclude: ["src/data/**", "**/node_modules/**"],
              options: {
                compact: true,
                controlFlowFlattening: false,      // 关闭控制流平铺（太慢）
                deadCodeInjection: false,           // 关闭死代码注入
                debugProtection: false,
                disableConsoleOutput: true,         // 生产环境去掉 console
                identifierNamesGenerator: "hexadecimal",
                rotateStringArray: true,
                selfDefending: false,               // 关闭自保护（可能影响 Vue3）
                stringArray: true,
                stringArrayThreshold: 0.75,
                transformObjectKeys: false,
                unicodeEscapeSequence: false,
              },
            }),
          ]
        : [],
    },
  },
});
