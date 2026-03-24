<template>
  <div class="image-preview-area">
    <div class="image-preview-list">
      <div
        v-for="image in images"
        :key="image.id"
        class="image-preview-item"
      >
        <img
          :src="image.url"
          class="preview-image"
          alt="预览图片"
        >
        <button
          class="remove-image-btn"
          title="删除图片"
          @click="$emit('remove', image.id)"
        >
          <i class="fas fa-times" />
        </button>
        <div
          v-if="image.isProcessing"
          class="ocr-processing-overlay"
        >
          <i class="fas fa-spinner fa-spin" />
        </div>
      </div>
    </div>
    <button
      v-if="images.length > 0"
      class="explain-images-btn"
      @click="$emit('explain')"
    >
      <span>解释图片</span>
      <i class="fas fa-arrow-right" />
    </button>
  </div>
</template>

<script setup>
defineProps({
  images: {
    type: Array,
    default: () => []
  }
})

defineEmits(['remove', 'explain'])
</script>

<style scoped>
.image-preview-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: var(--bg-secondary);
  border-radius: 12px;
  margin-bottom: 8px;
}

.image-preview-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.image-preview-item {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.6);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.image-preview-item:hover .remove-image-btn {
  opacity: 1;
}

.remove-image-btn:hover {
  background-color: rgba(239, 68, 68, 0.9);
}

.ocr-processing-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.explain-images-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background-color: var(--primary-color);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.explain-images-btn:hover {
  background-color: var(--primary-color-dark);
  transform: translateY(-1px);
}

.explain-images-btn i {
  font-size: 12px;
}
</style>
