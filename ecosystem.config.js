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
        NUXT_PUBLIC_APP_ID: "b4vtjjefuw6hh330518kp",
        NUXT_PUBLIC_SIGN_IN_REDIRECT_URI: "https://earthworm.aistudy.icu/callback",
        NUXT_PUBLIC_SIGN_OUT_REDIRECT_URI: "https://earthworm.aistudy.icu",
        NUXT_PUBLIC_BACKEND_ENDPOINT: "http://earthworm-api.aistudy.icu",
      },
    },
  ],
};
