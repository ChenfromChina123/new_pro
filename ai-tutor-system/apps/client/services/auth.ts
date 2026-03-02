import { useLogto } from "@logto/vue";
import { useRuntimeConfig } from "nuxt/app";

let logto: ReturnType<typeof useLogto>;
let runtimeConfig: ReturnType<typeof useRuntimeConfig>;

/**
 * 初始化 Logto 认证服务，在 Nuxt 插件中调用
 * 必须在 app.use(createLogto) 之后执行
 */
export async function setupAuth() {
  try {
    logto = useLogto();
    runtimeConfig = useRuntimeConfig();
    console.log("[Auth] Logto 初始化成功, endpoint:", runtimeConfig.public.endpoint, "appId:", runtimeConfig.public.appId);
  } catch (e) {
    console.error("[Auth] Logto 初始化失败:", e);
  }
}

/**
 * 触发 Logto 登录跳转
 * @param callback 登录成功后的回调路径
 */
export async function signIn(callback?: string) {
  try {
    if (!logto) {
      console.error("[Auth] signIn 失败: logto 未初始化");
      return;
    }
    const redirectURI = runtimeConfig.public.signInRedirectURI;
    if (!redirectURI) {
      console.error("[Auth] signIn 失败: signInRedirectURI 未配置");
      return;
    }
    console.log("[Auth] 正在跳转登录, redirectURI:", redirectURI);
    callback && setSignInCallback(callback);
    await logto.signIn(redirectURI);
  } catch (e) {
    console.error("[Auth] signIn 发生错误:", e);
  }
}

export function signOut() {
  return logto.signOut(runtimeConfig.public.signOutRedirectURI);
}

export function isAuthenticated() {
  return logto.isAuthenticated.value;
}

export async function getToken() {
  const accessToken = await logto.getAccessToken(runtimeConfig.public.backendEndpoint);

  return accessToken;
}

export function fetchUserInfo() {
  return logto.fetchUserInfo();
}

export function getSignInCallback() {
  let callback = sessionStorage.getItem("callback");
  if (callback) {
    sessionStorage.removeItem("callback");
    return callback;
  } else {
    return "/";
  }
}

function setSignInCallback(callback: string) {
  sessionStorage.setItem("callback", callback);
}
