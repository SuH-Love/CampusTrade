<template>
  <div class="goods-card-skeleton">
    <div class="skeleton-img"></div>
    <div class="skeleton-info">
      <div class="skeleton-title"></div>
      <div class="skeleton-bottom">
        <div class="skeleton-price"></div>
        <div class="skeleton-views"></div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.goods-card-skeleton {
  background: var(--bg-card);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border);
  margin-bottom: 16px;
}

.skeleton-img {
  padding-top: 75%;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info { padding: 14px 14px 16px; }

.skeleton-title {
  height: 16px;
  width: 70%;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.skeleton-price {
  height: 18px;
  width: 60px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-views {
  height: 14px;
  width: 50px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>