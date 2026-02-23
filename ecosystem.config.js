module.exports = {
  apps: [
    {
      name: "earthworm-client",
      script: "/root/.nvm/versions/node/v20.20.0/bin/node",
      args: ".output/server/index.mjs",
      cwd: "/opt/earthworm/apps/client",
      env: {
        HOST: "0.0.0.0",
        PORT: 4000,
        NUXT_PUBLIC_API_BASE: "http://earthworm-api.aistudy.icu",
        NUXT_PUBLIC_ENDPOINT: "https://logto.aistudy.icu",
        NUXT_PUBLIC_APP_ID: "earthworm001",
        NUXT_PUBLIC_SIGN_IN_REDIRECT_URI: "https://earthworm.aistudy.icu/callback",
        NUXT_PUBLIC_SIGN_OUT_REDIRECT_URI: "https://earthworm.aistudy.icu",
        NUXT_PUBLIC_BACKEND_ENDPOINT: "http://earthworm-api.aistudy.icu",
      },
    },
    {
      // word-game API 服务器（学习进度 + 课程数据）
      name: "word-game-api",
      script: "node",
      args: "server/index.js",
      cwd: "d:\\Users\\Administrator\\AistudyProject\\new_pro\\word-game",
      watch: false,
      env: {
        NODE_ENV: "production",
      },
    },
  ],
};
