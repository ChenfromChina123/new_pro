<template>
  <div class="author-page font-customFont">
    <div class="mx-auto max-w-screen-xl px-4 py-16 sm:px-6 lg:px-8">
      <!-- 页面标题 -->
      <div class="mb-12 text-center">
        <h1 class="text-4xl font-bold text-gray-900 dark:text-white sm:text-5xl">关于作者</h1>
        <p class="mt-4 text-lg text-gray-600 dark:text-gray-300">了解 Earthworm 背后的开发者</p>
      </div>

      <!-- 作者信息卡片 -->
      <div class="mx-auto max-w-4xl">
        <div class="overflow-hidden rounded-2xl bg-white shadow-xl dark:bg-gray-800">
          <div class="p-8 sm:p-12">
            <div class="flex flex-col items-center sm:flex-row sm:items-start">
              <!-- 头像 -->
              <div
                class="mb-6 flex h-32 w-32 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-purple-600 text-5xl font-bold text-white shadow-lg sm:mb-0 sm:mr-8"
              >
                林
              </div>

              <!-- 基本信息 -->
              <div class="text-center sm:text-left">
                <h2 class="text-3xl font-bold text-gray-900 dark:text-white">林晓枫</h2>
                <p class="mt-2 text-xl text-gray-600 dark:text-gray-300">Java 后端开发工程师</p>
                <p class="mt-1 text-gray-500 dark:text-gray-400">
                  本科在读 | 软件工程 | 专注后端开发与 AI 应用
                </p>

                <!-- 联系方式 -->
                <div class="mt-6 flex flex-wrap justify-center gap-4 sm:justify-start">
                  <div class="flex items-center text-gray-600 dark:text-gray-300">
                    <span class="mr-2">📧</span>
                    <a
                      href="mailto:3301767269@qq.com"
                      class="hover:text-blue-500"
                    >
                      3301767269@qq.com
                    </a>
                  </div>
                  <div class="flex items-center text-gray-600 dark:text-gray-300">
                    <span class="mr-2">🎓</span>
                    <span>桂林理工大学</span>
                  </div>
                  <div class="flex items-center text-gray-600 dark:text-gray-300">
                    <span class="mr-2">🌐</span>
                    <a
                      href="https://aistudy.icu"
                      target="_blank"
                      class="hover:text-blue-500"
                    >
                      aistudy.icu
                    </a>
                  </div>
                </div>
              </div>
            </div>

            <!-- 教育经历 -->
            <div class="mt-12">
              <h3 class="mb-4 text-2xl font-bold text-gray-900 dark:text-white">教育经历</h3>
              <div class="rounded-lg bg-gray-50 p-6 dark:bg-gray-700">
                <div class="flex flex-col sm:flex-row sm:justify-between">
                  <div>
                    <h4 class="text-xl font-semibold text-gray-900 dark:text-white">
                      桂林理工大学
                    </h4>
                    <p class="text-gray-600 dark:text-gray-300">
                      软件工程 · 本科 · 计算机科学与工程学院
                    </p>
                  </div>
                  <span class="mt-2 text-gray-500 dark:text-gray-400 sm:mt-0">
                    2023.09 - 2027.06
                  </span>
                </div>
              </div>
            </div>

            <!-- 专业技能 -->
            <div class="mt-12">
              <h3 class="mb-4 text-2xl font-bold text-gray-900 dark:text-white">专业技能</h3>
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="rounded-lg bg-gray-50 p-4 dark:bg-gray-700">
                  <h4 class="mb-2 font-semibold text-gray-900 dark:text-white">后端技术</h4>
                  <div class="flex flex-wrap gap-2">
                    <span
                      class="rounded-full bg-blue-100 px-3 py-1 text-sm text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                      >Java</span
                    >
                    <span
                      class="rounded-full bg-green-100 px-3 py-1 text-sm text-green-800 dark:bg-green-900 dark:text-green-200"
                      >Spring Boot</span
                    >
                    <span
                      class="rounded-full bg-purple-100 px-3 py-1 text-sm text-purple-800 dark:bg-purple-900 dark:text-purple-200"
                      >MySQL</span
                    >
                    <span
                      class="rounded-full bg-yellow-100 px-3 py-1 text-sm text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200"
                      >Redis</span
                    >
                  </div>
                </div>
                <div class="rounded-lg bg-gray-50 p-4 dark:bg-gray-700">
                  <h4 class="mb-2 font-semibold text-gray-900 dark:text-white">前端技术</h4>
                  <div class="flex flex-wrap gap-2">
                    <span
                      class="rounded-full bg-cyan-100 px-3 py-1 text-sm text-cyan-800 dark:bg-cyan-900 dark:text-cyan-200"
                      >Vue.js</span
                    >
                    <span
                      class="rounded-full bg-indigo-100 px-3 py-1 text-sm text-indigo-800 dark:bg-indigo-900 dark:text-indigo-200"
                      >React</span
                    >
                    <span
                      class="rounded-full bg-pink-100 px-3 py-1 text-sm text-pink-800 dark:bg-pink-900 dark:text-pink-200"
                      >TypeScript</span
                    >
                    <span
                      class="rounded-full bg-orange-100 px-3 py-1 text-sm text-orange-800 dark:bg-orange-900 dark:text-orange-200"
                      >Tailwind CSS</span
                    >
                  </div>
                </div>
              </div>
            </div>

            <!-- 荣誉奖项 -->
            <div class="mt-12">
              <h3 class="mb-4 text-2xl font-bold text-gray-900 dark:text-white">荣誉奖项</h3>
              <div class="space-y-3">
                <div class="flex items-start">
                  <span class="mr-3 mt-1 text-yellow-500">🏆</span>
                  <div>
                    <p class="font-medium text-gray-900 dark:text-white">
                      第16届蓝桥杯全国软件类国赛三等奖
                    </p>
                  </div>
                </div>
                <div class="flex items-start">
                  <span class="mr-3 mt-1 text-blue-500">⭐</span>
                  <div>
                    <p class="font-medium text-gray-900 dark:text-white">
                      第六届全国大学生算法设计与编程挑战赛优秀奖
                    </p>
                  </div>
                </div>
                <div class="flex items-start">
                  <span class="mr-3 mt-1 text-green-500">🎓</span>
                  <div>
                    <p class="font-medium text-gray-900 dark:text-white">
                      连续两年获得校级三等奖学金（2024、2025学年）
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 个人简介 -->
            <div class="mt-12">
              <h3 class="mb-4 text-2xl font-bold text-gray-900 dark:text-white">个人简介</h3>
              <div class="prose max-w-none text-gray-600 dark:text-gray-300">
                <p>
                  热爱编程，专注于后端开发和AI应用。作为Earthworm项目的主要开发者，致力于打造高效、易用的学习平台。
                  在校期间积极参与各类编程竞赛，不断提升自己的技术能力和解决问题的思维方式。
                </p>
                <p class="mt-4">
                  擅长使用Java进行后端开发，熟悉Spring
                  Boot、MySQL、Redis等技术栈。同时具备前端开发能力，
                  能够使用Vue.js、React等框架进行全栈开发。对AI技术有浓厚兴趣，正在探索将AI技术应用于教育领域。
                </p>
              </div>
            </div>

            <!-- 社交链接 -->
            <div class="mt-12 text-center">
              <h3 class="mb-4 text-2xl font-bold text-gray-900 dark:text-white">联系方式</h3>
              <div class="flex justify-center space-x-6">
                <a
                  href="mailto:3301767269@qq.com"
                  class="text-gray-600 hover:text-blue-500 dark:text-gray-300 dark:hover:text-blue-400"
                  title="邮箱"
                >
                  <span class="text-xl">📧</span>
                </a>
                <a
                  href="https://aistudy.icu"
                  target="_blank"
                  class="text-gray-600 hover:text-blue-500 dark:text-gray-300 dark:hover:text-blue-400"
                  title="个人网站"
                >
                  <span class="text-xl">🌐</span>
                </a>
                <a
                  href="https://cv.aistudy.icu"
                  target="_blank"
                  class="text-gray-600 hover:text-blue-500 dark:text-gray-300 dark:hover:text-blue-400"
                  title="完整简历"
                >
                  <span class="text-xl">📄</span>
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 页面元数据
useHead({
  title: "关于作者 - Earthworm",
  meta: [
    {
      name: "description",
      content: "了解Earthworm背后的开发者林晓枫，Java后端开发工程师，专注于后端开发与AI应用",
    },
  ],
});
</script>

<style scoped>
.author-page {
  min-height: calc(100vh - var(--header-height, 80px));
}
</style>
