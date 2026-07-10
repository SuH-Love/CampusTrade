<template>
  <div class="home-page">
    <section class="hero">
      <el-carousel height="300px" :interval="5000" arrow="hover" indicator-position="outside">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="hero-slide" :style="{ background: banner.imageUrl ? 'transparent' : (banner.bgColor || 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)') }">
            <img v-if="banner.imageUrl" :src="banner.imageUrl" class="hero-bg-img" />
            <div class="hero-overlay" v-if="banner.imageUrl" />
            <div class="hero-content">
              <h1 v-if="banner.title">{{ banner.title }}</h1>
              <p v-if="banner.subtitle">{{ banner.subtitle }}</p>
              <el-button v-if="banner.buttonText && banner.linkUrl" size="large" round :style="bannerButtonStyle(banner)" @click="$router.push(banner.linkUrl)">{{ banner.buttonText }}</el-button>
            </div>
          </div>
        </el-carousel-item>
        <el-carousel-item v-if="banners.length === 0">
          <div class="hero-slide" style="background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%)">
            <div class="hero-content">
              <h1>校园二手交易平台</h1>
              <p>安全 · 便捷 · 值得信赖的校园闲置好物流转平台</p>
              <el-button type="primary" size="large" round @click="$router.push('/goods')">浏览商品</el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <div class="home-content">
      <section class="category-bar">
        <div
          v-for="cat in categories" :key="cat.id"
          class="category-chip"
          :class="{ active: selectedCategoryId === cat.id }"
          @click="toggleCategory(cat.id)"
        >{{ cat.categoryName }}</div>
      </section>

      <section style="margin-top: 32px">
        <h3 class="section-title">热门商品</h3>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in hotGoods" :key="item.id">
            <div class="goods-card" @click="$router.push(`/goods/${item.id}`)">
              <div class="goods-img-wrap">
                <img :src="item.coverImage || '/default-cover.svg'" class="goods-img" />
                <span class="goods-category-tag">{{ item.categoryName }}</span>
                <span v-if="item.condition" class="goods-condition-tag">{{ item.condition }}</span>
                <span v-if="item.originalPrice && item.originalPrice > item.price" class="goods-discount-tag">折扣</span>
                <el-avatar v-if="item.userAvatar" :size="28" :src="item.userAvatar" class="goods-seller-avatar" />
              </div>
              <div class="goods-info">
                <div class="goods-title">{{ item.title }}</div>
                <div class="goods-bottom">
                  <div class="goods-price-row">
                    <span class="price-text">¥{{ item.price }}</span>
                    <span v-if="item.originalPrice && item.originalPrice > item.price" class="original-price">¥{{ item.originalPrice }}</span>
                  </div>
                  <span class="goods-views">{{ item.viewCount }} 浏览</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
        <el-empty v-if="hotGoods.length === 0" description="暂无热门商品" />
      </section>

      <section style="margin-top: 40px">
        <h3 class="section-title">最新上架</h3>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in recommendGoods" :key="item.id">
            <div class="goods-card" @click="$router.push(`/goods/${item.id}`)">
              <div class="goods-img-wrap">
                <img :src="item.coverImage || '/default-cover.svg'" class="goods-img" />
                <span class="goods-category-tag">{{ item.categoryName }}</span>
                <span v-if="item.condition" class="goods-condition-tag">{{ item.condition }}</span>
                <span v-if="item.originalPrice && item.originalPrice > item.price" class="goods-discount-tag">折扣</span>
                <el-avatar v-if="item.userAvatar" :size="28" :src="item.userAvatar" class="goods-seller-avatar" />
              </div>
              <div class="goods-info">
                <div class="goods-title">{{ item.title }}</div>
                <div class="goods-bottom">
                  <div class="goods-price-row">
                    <span class="price-text">¥{{ item.price }}</span>
                    <span v-if="item.originalPrice && item.originalPrice > item.price" class="original-price">¥{{ item.originalPrice }}</span>
                  </div>
                  <span class="goods-views">{{ item.viewCount }} 浏览</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
        <el-empty v-if="recommendGoods.length === 0" description="暂无推荐商品" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHotGoods, getRecommendGoods } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import { getActiveBanners } from '@/api/banner'
import type { GoodsVO } from '@/api/goods'
import type { GoodsCategory } from '@/api/category'
import type { BannerVO } from '@/api/banner'

const banners = ref<BannerVO[]>([])
const hotGoods = ref<GoodsVO[]>([])
const recommendGoods = ref<GoodsVO[]>([])
const categories = ref<GoodsCategory[]>([])
const selectedCategoryId = ref<number | undefined>(undefined)

const toggleCategory = (id: number) => {
  selectedCategoryId.value = selectedCategoryId.value === id ? undefined : id
  loadHotGoods()
}

const bannerButtonStyle = (banner: BannerVO) => {
  const color = banner.buttonColor || 'rgba(255,255,255,0.2)'
  const isGradient = color.includes('gradient')
  return {
    background: color,
    borderColor: isGradient ? 'transparent' : color,
    color: '#fff'
  }
}

const loadBanners = async () => {
  try { banners.value = await getActiveBanners() } catch { /* ignore */ }
}

const loadHotGoods = async () => {
  try {
    const res = await getHotGoods()
    let list = res.list || []
    if (selectedCategoryId.value) list = list.filter((g: GoodsVO) => g.categoryId === selectedCategoryId.value)
    hotGoods.value = list.slice(0, 8)
  } catch { /* ignore */ }
}

const loadRecommendGoods = async () => {
  try { const res = await getRecommendGoods(); recommendGoods.value = (res.list || []).slice(0, 8) } catch { /* ignore */ }
}

const loadCategories = async () => {
  try { const res = await getCategoryList(); categories.value = res || [] } catch { /* ignore */ }
}

onMounted(() => { loadBanners(); loadHotGoods(); loadRecommendGoods(); loadCategories() })
</script>

<style scoped lang="scss">
.hero {
  margin: 0 24px;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.hero-slide {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  position: relative;
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.35) 100%);
}

.hero-content {
  position: relative;
  z-index: 1;
  h1 { font-size: 36px; font-weight: 800; margin-bottom: 12px; letter-spacing: -0.5px; text-shadow: 0 2px 8px rgba(0,0,0,0.15); }
  p { font-size: 16px; opacity: 0.92; margin-bottom: 24px; text-shadow: 0 1px 4px rgba(0,0,0,0.1); }
  .el-button { font-size: 16px; padding: 12px 32px; background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); color: #fff; &:hover { background: rgba(255,255,255,0.35); transform: translateY(-2px); } }
}

.hero-bg-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 1;
}

.home-content {
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  margin: 24px 24px 0;
  max-width: 100%;
  padding: 24px;
}

.category-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.category-chip {
  padding: 7px 20px;
  border-radius: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
  &:hover { border-color: var(--primary); color: var(--primary); transform: translateY(-1px); box-shadow: var(--shadow-sm); }
  &.active { background: var(--primary-gradient); color: #fff; border-color: transparent; box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3); }
}

.goods-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border);
  margin-bottom: 16px;
  &:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); border-color: var(--primary-lighter); }
}

.goods-img-wrap {
  position: relative;
  padding-top: 75%;
  overflow: hidden;
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
}

.goods-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  .goods-card:hover & { transform: scale(1.08); }
}

.goods-category-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
  letter-spacing: 0.3px;
}

.goods-condition-tag {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(234, 179, 8, 0.85);
  backdrop-filter: blur(6px);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
}

.goods-discount-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(239, 68, 68, 0.85);
  backdrop-filter: blur(6px);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
}

.goods-seller-avatar {
  position: absolute;
  bottom: 10px;
  right: 10px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.goods-info { padding: 14px 14px 16px; }

.goods-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.goods-price-row { display: flex; align-items: baseline; gap: 6px; }
.original-price { font-size: 12px; color: var(--text-muted); text-decoration: line-through; }
.goods-views { font-size: 12px; color: var(--text-muted); font-weight: 500; }
</style>
