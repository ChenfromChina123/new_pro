<template>
  <div class="app-layout">
    <!-- 移动端遮罩层 -->
    <div 
      v-if="showSidebar && uiStore.isMobileSidebarOpen"
      class="mobile-overlay"
      @click="uiStore.closeMobileSidebar()"
    />

    <AppSidebar 
      v-if="showSidebar" 
      :class="{ 'mobile-open': uiStore.isMobileSidebarOpen }"
    />
    
    <main class="main-content">
      <!-- 移动端菜单按钮 -->
      <button 
        v-if="showSidebar"
        class="mobile-menu-btn"
        @click="uiStore.toggleMobileSidebar()"
      >
        <i class="fas fa-bars" />
      </button>

      <router-view v-slot="{ Component }">
        <transition
          name="fade-slide"
          mode="out-in"
        >
          <component
            :is="Component"
            :key="route.fullPath"
          />
        </transition>
      </router-view>
    </main>

    <!-- 全局提示 (Toast) -->
    <transition name="toast-fade">
      <div
        v-if="uiStore.showToastMsg"
        class="toast-container"
      >
        <div class="toast-content">
          <i class="fas fa-info-circle" />
          <span>{{ uiStore.toastMessage }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/Sidebar/AppSidebar.vue'
import { useUIStore } from '@/stores/ui'

const route = useRoute()
const uiStore = useUIStore()

const showSidebar = computed(() => {
  // 认证路由或允许游客访问的路由都显示侧边栏
  return route.meta.requiresAuth || route.meta.allowGuest
})
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
  min-height: 0; /* 关键：允许 flex 子项在需要时缩小，从而使内部滚动生效 */
}

/* Toast 样式 */
.toast-container {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  pointer-events: none;
}

.toast-content {
  background-color: rgba(31, 41, 55, 0.9);
  color: white;
  padding: 12px 24px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  white-space: nowrap;
}

.toast-content i {
  color: #3b82f6;
  font-size: 16px;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 页面切换动画：淡入淡出 + 轻微位移 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

/* 移动端适配 */
.mobile-menu-btn {
  display: none;
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 90;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: none;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.mobile-menu-btn:hover {
  background-color: var(--bg-tertiary);
}

.mobile-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 99;
  backdrop-filter: blur(2px);
}

@media (max-width: 768px) {
  .mobile-menu-btn {
    display: flex;
  }

  .mobile-overlay {
    display: block;
  }
}
</style>
