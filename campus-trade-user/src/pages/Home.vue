<template>
  <div class="home-page">
    <section class="hero">
      <el-carousel height="260px" :interval="5000" arrow="hover" indicator-position="outside">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="hero-slide" :style="{ background: banner.bgColor || 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)' }">
            <img v-if="banner.imageUrl" :src="banner.imageUrl" class="hero-bg-img" />
            <h1>{{ banner.title }}</h1>
            <p>{{ banner.subtitle }}</p>
            <el-button v-if="banner.linkUrl" type="primary" size="large" round @click="$router.push(banner.linkUrl)">{{ banner.buttonText || '了解更多' }}</el-button>
          </div>
        </el-carousel-item>
        <el-carousel-item v-if="banners.length === 0">
          <div class="hero-slide" style="background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%)">
            <h1>校园二手交易平台</h1>
            <p>安全 · 便捷 · 值得信赖的校园闲置好物流转平台</p>
            <el-button type="primary" size="large" round @click="$router.push('/goods')">浏览商品</el-button>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <div class="page-container">
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
                <img :src="item.coverImage || '/placeholder.png'" class="goods-img" />
                <span class="goods-category-tag">{{ item.categoryName }}</span>
              </div>
              <div class="goods-info">
                <div class="goods-title">{{ item.title }}</div>
                <div class="goods-bottom">
                  <span class="price-text">¥{{ item.price }}</span>
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
                <img :src="item.coverImage || '/placeholder.png'" class="goods-img" />
                <span class="goods-category-tag">{{ item.categoryName }}</span>
              </div>
              <div class="goods-info">
                <div class="goods-title">{{ item.title }}</div>
                <div class="goods-bottom">
                  <span class="price-text">¥{{ item.price }}</span>
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
}

.hero-slide {
  height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  position: relative;
  h1 { font-size: 32px; font-weight: 800; margin-bottom: 12px; letter-spacing: -0.5px; position: relative; z-index: 1; }
  p { font-size: 16px; opacity: 0.9; margin-bottom: 24px; position: relative; z-index: 1; }
  .el-button { font-size: 16px; padding: 12px 32px; background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); &:hover { background: rgba(255,255,255,0.35); } position: relative; z-index: 1; }
}

.hero-bg-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.3;
}

.category-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.category-chip {
  padding: 6px 18px;
  border-radius: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
  &:hover { border-color: var(--primary); color: var(--primary); }
  &.active { background: var(--primary); color: #fff; border-color: var(--primary); }
}

.goods-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border);
  margin-bottom: 16px;
  &:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
}

.goods-img-wrap {
  position: relative;
  padding-top: 75%;
  overflow: hidden;
  background: #f1f5f9;
}

.goods-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
  .goods-card:hover & { transform: scale(1.05); }
}

.goods-category-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}

.goods-info { padding: 12px; }

.goods-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.goods-views { font-size: 12px; color: var(--text-muted); }
</style>
