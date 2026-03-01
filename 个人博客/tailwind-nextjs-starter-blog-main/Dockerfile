# blog.aistudy.icu Dockerfile
FROM node:20-alpine AS base

# 安装依赖阶段
FROM base AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

# 安装 yarn
RUN corepack enable && corepack prepare yarn@3.6.1 --activate

# 复制依赖文件
COPY package.json yarn.lock .yarnrc.yml ./
COPY .yarn ./.yarn

# 安装依赖
RUN yarn install --frozen-lockfile

# 构建阶段
FROM base AS builder
WORKDIR /app

RUN corepack enable && corepack prepare yarn@3.6.1 --activate

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# 设置环境变量
ENV NEXT_TELEMETRY_DISABLED 1

# 构建应用
RUN yarn build

# 运行阶段
FROM base AS runner
WORKDIR /app

ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1

# 创建非 root 用户
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# 复制构建产物
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

# 设置权限
RUN chown -R nextjs:nodejs /app

USER nextjs

EXPOSE 3000

ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]
