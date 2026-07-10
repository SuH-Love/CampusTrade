<template>
  <div class="not-found">
    <div class="not-found-content">
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" round @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.not-found {
  display: flex; align-items: center; justify-content: center; min-height: 80vh;
}
.not-found-content {
  text-align: center;
  h1 { font-size: 120px; font-weight: 800; color: var(--primary, #6366f1); opacity: 0.3; margin: 0; line-height: 1; }
  p { font-size: 18px; color: var(--text-muted, #94a3b8); margin: 16px 0 32px; }
}
</style>