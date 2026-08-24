<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="auth-brand">
        <div class="brand-icon">C</div>
        <h1>CampusTrade</h1>
        <p>{{ subtitle }}</p>
      </div>
      <div class="auth-features" v-if="features && features.length > 0">
        <div class="feature-item" v-for="f in features" :key="f.title">
          <div class="feature-icon">{{ f.icon }}</div>
          <div><div class="feature-title">{{ f.title }}</div><div class="feature-desc">{{ f.desc }}</div></div>
        </div>
      </div>
    </div>
    <div class="auth-right">
      <div class="auth-form-wrap">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface FeatureItem {
  icon: string
  title: string
  desc: string
}

defineProps<{
  subtitle: string
  features?: FeatureItem[]
}>()
</script>

<style scoped lang="scss">
.auth-page { display: flex; min-height: 100vh; }

.auth-left {
  flex: 1;
  background: linear-gradient(135deg, #0EA5E9, #14B8A6, #2DD4BF);
  padding: 60px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    width: 400px; height: 400px;
    background: rgba(255,255,255,0.08);
    border-radius: 50%;
    top: -100px; right: -100px;
  }
  &::after {
    content: '';
    position: absolute;
    width: 300px; height: 300px;
    background: rgba(255,255,255,0.05);
    border-radius: 50%;
    bottom: -80px; left: -60px;
  }
  @media (max-width: 768px) { display: none; }
}

.brand-icon {
  width: 56px; height: 56px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 800; margin-bottom: 20px;
}

.auth-brand h1 { font-size: 36px; font-weight: 800; margin-bottom: 8px; letter-spacing: -0.5px; }
.auth-brand p { font-size: 16px; opacity: 0.85; }

.auth-features { margin-top: 48px; display: flex; flex-direction: column; gap: 24px; }
.feature-item { display: flex; gap: 14px; align-items: flex-start; }
.feature-icon { font-size: 24px; flex-shrink: 0; margin-top: 2px; }
.feature-title { font-size: 15px; font-weight: 600; }
.feature-desc { font-size: 13px; opacity: 0.75; margin-top: 2px; }

.auth-right {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  padding: 40px;
  background: var(--bg-page);
}

.auth-form-wrap {
  width: 100%; max-width: 400px;
}
</style>