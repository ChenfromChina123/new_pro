<template>
  <div
    ref="selectRef"
    class="custom-select"
  >
    <div
      class="select-trigger"
      :class="{ active: isOpen }"
      @click="isOpen = !isOpen"
    >
      <span class="selected-text">{{ selectedLabel }}</span>
      <span
        class="chevron"
        :class="{ open: isOpen }"
      >▼</span>
    </div>
    <transition name="dropdown">
      <div
        v-if="isOpen"
        class="options-menu"
      >
        <div 
          v-for="option in options" 
          :key="option.value" 
          class="option-item"
          :class="{ selected: modelValue === option.value }"
          @click="selectOption(option)"
        >
          <div class="option-content">
            <span class="option-label">{{ option.label }}</span>
            <span
              v-if="option.description"
              class="option-desc"
            >{{ option.description }}</span>
          </div>
          <span
            v-if="modelValue === option.value"
            class="check-icon"
          >✓</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: String,
  options: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const selectRef = ref(null)

const selectedLabel = computed(() => {
  const option = props.options.find(opt => opt.value === props.modelValue)
  return option ? option.label : '请选择'
})

const selectOption = (option) => {
  emit('update:modelValue', option.value)
  isOpen.value = false
}

const handleClickOutside = (event) => {
  if (selectRef.value && !selectRef.value.contains(event.target)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.custom-select {
  position: relative;
  width: 180px;
  font-family: inherit;
}

.select-trigger {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-primary);
}

.select-trigger:hover {
  border-color: var(--primary-color);
  background: var(--bg-tertiary);
}

.select-trigger.active {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(6, 182, 212, 0.1);
}

.selected-text {
  font-size: 0.95rem;
  color: var(--text-primary);
}

.chevron {
  font-size: 0.8rem;
  color: var(--text-tertiary);
  transition: transform 0.2s;
}

.chevron.open {
  transform: rotate(180deg);
}

.options-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  overflow: hidden;
  padding: 4px;
}

.option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: var(--border-radius-md);
  transition: all 0.2s;
}

.option-item:hover {
  background: var(--bg-tertiary);
}

.option-item.selected {
  background: rgba(6, 182, 212, 0.1);
  color: var(--primary-color);
}

.option-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-label {
  font-size: 0.95rem;
  font-weight: 500;
}

.option-desc {
  font-size: 0.8rem;
  color: var(--text-tertiary);
}

.check-icon {
  font-size: 0.9rem;
  color: var(--primary-color);
}

/* Transitions */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
