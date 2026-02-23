import { createRouter, createWebHashHistory } from "vue-router";
import PackageListPage from "@/components/pages/PackageListPage.vue";
import CourseListPage from "@/components/pages/CourseListPage.vue";
import GamePage from "@/components/pages/GamePage.vue";

/**
 * 应用路由配置
 * 使用 hash 模式，部署时无需服务端配置
 *
 * 路由层级：
 *   /                        → 课程包列表（PackageListPage）
 *   /package/:packageId      → 包内课程列表（CourseListPage）
 *   /game/:courseIndex       → 答题游戏页（GamePage）
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      name: "package-list",
      component: PackageListPage,
    },
    {
      path: "/package/:packageId",
      name: "course-list",
      component: CourseListPage,
      props: true,
    },
    {
      path: "/game/:courseIndex",
      name: "game",
      component: GamePage,
      props: true,
    },
    {
      // 未知路由重定向到课程包列表
      path: "/:pathMatch(.*)*",
      redirect: "/",
    },
  ],
});

export default router;
