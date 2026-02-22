#!/bin/bash
export NVM_DIR="$HOME/.nvm"
source "$NVM_DIR/nvm.sh"
export HOST=0.0.0.0
export PORT=4000
export NUXT_PUBLIC_API_BASE=http://earthworm-api.aistudy.icu
export NUXT_PUBLIC_LOGTO_ENDPOINT=https://logto.aistudy.icu
export NUXT_PUBLIC_LOGTO_APP_ID=b4vtjjefuw6hh330518kp
export NUXT_PUBLIC_LOGTO_SIGN_IN_REDIRECT_URI=http://earthworm.aistudy.icu/callback
export NUXT_PUBLIC_LOGTO_SIGN_OUT_REDIRECT_URI=http://earthworm.aistudy.icu
export NUXT_PUBLIC_BACKEND_ENDPOINT=http://earthworm-api.aistudy.icu
cd /opt/earthworm/apps/client
node .output/server/index.mjs
