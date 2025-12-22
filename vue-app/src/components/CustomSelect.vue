<template>
  <div class="custom-select" ref="selectRef">
    <div class="select-trigger" @click="isOpen = !isOpen" :class="{ active: isOpen }">
      <span class="selected-text">{{ selectedLabel }}</span>
      <span class="chevron" :class="{ open: isOpen }">▼</span>
    </div>
    <transition name="dropdown">
      <div v-if="isOpen" class="options-menu">
        <div 
          v-for="option in options" 
          :key="option.value" 
          class="option-item"
          :class="{ selected: modelValue === option.value }"
          @click="selectOption(option)"
        >
          <div class="option-content">
            <span class="option-label">{{ option.label }}</span>
            <span v-if="option.description" class="option-desc">{{ option.description }}</span>
          </div>
          <span v-if="modelValue === option.value" class="check-icon">✓</span>
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
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
  color: #334155;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.select-trigger:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.select-trigger.active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.chevron {
  font-size: 0.7rem;
  color: #94a3b8;
  transition: transform 0.2s;
}

.chevron.open {
  transform: rotate(180deg);
}

.options-menu {
  position: absolute;
  top: calc(100% + 5px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  z-index: 100;
  overflow: hidden;
  min-width: 200px;
}

.option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.option-item:hover {
  background: #f1f5f9;
}

.option-item.selected {
  background: #eff6ff;
}

.option-content {
  display: flex;
  flex-direction: column;
}

.option-label {
  font-size: 0.9rem;
  font-weight: 500;
  color: #1e293b;
}

.option-desc {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 2px;
}

.check-icon {
  color: #3b82f6;
  font-weight: bold;
}

/* Transitions */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
