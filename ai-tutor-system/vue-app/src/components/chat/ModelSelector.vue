<template>
  <div ref="menuRef" class="model-selector">
    <div
      class="model-selector-trigger"
      :class="{ active: isOpen }"
      @click="toggleMenu"
    >
      <span class="brand-name">{{ currentBrand.name }}</span>
      <i
        class="fas fa-chevron-up toggle-arrow"
        :class="{ rotate: isOpen }"
      />
    </div>

    <transition name="menu-fade">
      <div v-if="isOpen" class="model-dropdown-menu">
        <div
          v-for="brand in brands"
          :key="brand.id"
          class="model-menu-item"
          :class="{ active: currentBrand.id === brand.id }"
          @click="selectBrand(brand)"
        >
          <div class="item-info">
            <span class="item-name">{{ brand.name }}</span>
            <span class="item-desc">{{ brand.description }}</span>
          </div>
          <i
            v-if="currentBrand.id === brand.id"
            class="fas fa-check check-icon"
          />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const isOpen = ref(false)
const menuRef = ref(null)

const brands = [
  {
    id: 'deepseek',
    name: 'DeepSeek',
    description: 'DeepSeek-V3.2',
    standard: 'deepseek-chat',
    reasoner: 'deepseek-reasoner'
  }
]

const currentBrand = computed(() => {
  const model = chatStore.selectedModel
  return brands.find(b => model === b.standard || model === b.reasoner) || brands[0]
})

const toggleMenu = () => {
  isOpen.value = !isOpen.value
}

const selectBrand = (brand) => {
  const isReasoning = chatStore.selectedModel.includes('reasoner')
  const newModel = isReasoning ? brand.reasoner : brand.standard
  chatStore.setModel(newModel)
  isOpen.value = false
}

const handleClickOutside = (event) => {
  if (menuRef.value && !menuRef.value.contains(event.target)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

defineExpose({
  currentBrand,
  brands
})
</script>

<style scoped>
.model-selector {
  position: relative;
}

.model-selector-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.model-selector-trigger:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
}

.model-selector-trigger.active {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
}

.brand-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.toggle-arrow {
  font-size: 10px;
  color: var(--text-secondary);
  transition: transform 0.2s ease;
}

.toggle-arrow.rotate {
  transform: rotate(180deg);
}

.model-dropdown-menu {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 8px;
  min-width: 200px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 100;
}

.model-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.model-menu-item:hover {
  background-color: var(--bg-secondary);
}

.model-menu-item.active {
  background-color: rgba(59, 130, 246, 0.1);
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.item-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.check-icon {
  font-size: 12px;
  color: var(--primary-color);
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.2s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
