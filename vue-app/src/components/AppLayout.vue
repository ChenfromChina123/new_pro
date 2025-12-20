<template>
  <div class="app-layout">
    <!-- 移除顶部 AppHeader，改为全屏侧边栏布局 -->
    <main class="main-content">
      <slot />
    </main>
  </div>
</template>

<script setup>
// AppHeader 已不再作为顶部导航使用
</script>

<style scoped>
.app-layout {
  height: 100vh;
  width: 100vw;
  display: flex;
  overflow: hidden;
  background-color: var(--bg-primary);
}

.main-content {
  flex: 1;
  height: 100%;
  overflow: hidden;
  position: relative;
}
</style>

