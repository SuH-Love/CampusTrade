<template>
  <div class="skeleton-base" :class="`skeleton--${variant}`" :style="customStyle" />
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  variant?: 'text' | 'rect' | 'circle' | 'card' | 'avatar' | 'button' | 'image'
  width?: string | number
  height?: string | number
  radius?: string | number
  lines?: number
}>(), {
  variant: 'rect',
  width: '100%',
  height: 'auto',
  radius: 8,
  lines: 1
})

const customStyle = computed(() => {
  const w = typeof props.width === 'number' ? `${props.width}px` : props.width
  const h = typeof props.height === 'number' ? `${props.height}px` : props.height
  const r = typeof props.radius === 'number' ? `${props.radius}px` : props.radius
  return {
    width: w,
    height: props.variant === 'text' ? `${14 * props.lines}px` : h,
    borderRadius: props.variant === 'circle' ? '50%' : (props.variant === 'text' ? '4px' : r)
  }
})
</script>

<style scoped lang="scss">
.skeleton-base {
  background: linear-gradient(90deg,
    var(--color-img-placeholder-from) 25%,
    var(--color-img-placeholder-to) 50%,
    var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.5s ease-in-out infinite;
  display: block;
}

.skeleton--image { aspect-ratio: 4/3; border-radius: var(--radius-md); }
.skeleton--card { border-radius: var(--radius-lg); }
.skeleton--avatar { border-radius: 50%; }
.skeleton--button { height: 36px; border-radius: var(--radius-sm); }

@keyframes skeletonShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>