import type { LogtoConfig } from "@logto/vue";

import { createLogto, UserScope } from "@logto/vue";
import { defineNuxtPlugin, useRuntimeConfig } from "nuxt/app";

import { setupAuth } from "~/services/auth";

export default defineNuxtPlugin((nuxtApp) => {
  const runtimeConfig = useRuntimeConfig();

  // 校验必要的配置项，缺失时在控制台提示
  const endpoint = runtimeConfig.public.endpoint as string;
  const appId = runtimeConfig.public.appId as string;
  const backendEndpoint = runtimeConfig.public.backendEndpoint as string;

  if (!endpoint || !appId) {
    console.error("[Logto Plugin] 缺少必要配置: endpoint=", endpoint, "appId=", appId);
  }

  const config: LogtoConfig = {
    endpoint,
    appId,
    scopes: [
      UserScope.Email,
      UserScope.Phone,
      UserScope.CustomData,
      UserScope.Identities,
      UserScope.Organizations,
    ],
    resources: backendEndpoint ? [backendEndpoint] : [],
  };

  nuxtApp.vueApp.use(createLogto, config);
  setupAuth();
});
