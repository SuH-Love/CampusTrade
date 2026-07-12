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
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索你想要的宝贝..."
          size="large"
          clearable
          prefix-icon="Search"
          @keyup.enter="handleSearch"
          @focus="showSearchDropdown = true"
          @blur="hideSearchDropdown"
          class="search-input"
        />
        <div class="search-dropdown" v-if="showSearchDropdown && (searchHistory.length > 0 || hotKeywords.length > 0)">
          <div class="search-dropdown-section" v-if="searchHistory.length > 0">
            <div class="search-dropdown-header">
              <span>搜索历史</span>
              <el-button link type="info" size="small" @click="clearSearchHistory">清空</el-button>
            </div>
            <div class="search-tags">
              <el-tag v-for="(kw, idx) in searchHistory" :key="idx" size="small" round effect="plain" @mousedown.prevent="searchFromHistory(kw)" style="cursor: pointer">{{ kw }}</el-tag>
            </div>
          </div>
          <div class="search-dropdown-section" v-if="hotKeywords.length > 0">
            <div class="search-dropdown-header"><span>智能推荐</span></div>
            <div class="search-tags">
              <el-tag v-for="(kw, idx) in hotKeywords" :key="idx" size="small" round type="danger" effect="plain" @mousedown.prevent="searchFromHistory(kw)" style="cursor: pointer">{{ kw }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="category-bar">
        <div
          v-for="cat in categories" :key="cat.id"
          class="category-chip"
          :class="{ active: selectedCategoryId === cat.id }"
          @click="toggleCategory(cat.id)"
        >{{ cat.categoryName }}</div>
      </div>

      <div class="announcement-bar" v-if="announcements.length > 0">
        <el-icon style="color: var(--primary); font-size: 16px; flex-shrink: 0"><Bell /></el-icon>
        <div class="announcement-scroll">
          <span v-for="(a, idx) in announcements" :key="a.id" class="announcement-item">
            <strong>{{ a.title }}</strong>：{{ a.content }}<span v-if="idx < announcements.length - 1" class="announcement-divider">|</span>
          </span>
        </div>
      </div>

      <section style="margin-top: 32px">
        <h3 class="section-title">热门商品</h3>
        <el-row :gutter="16" v-if="hotGoods.length > 0">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in hotGoods" :key="item.id">
            <GoodsCard :goods="item" />
          </el-col>
        </el-row>
        <el-row :gutter="16" v-else-if="homeLoading">
          <el-col :xs="12" :sm="8" :md="6" v-for="i in 8" :key="'hs'+i">
            <GoodsCardSkeleton />
          </el-col>
        </el-row>
        <el-empty v-else description="暂无热门商品" />
      </section>

      <section style="margin-top: 40px">
        <h3 class="section-title">最新上架</h3>
        <el-row :gutter="16" v-if="recommendGoods.length > 0">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in recommendGoods" :key="item.id">
            <GoodsCard :goods="item" />
          </el-col>
        </el-row>
        <el-row :gutter="16" v-else-if="homeLoading">
          <el-col :xs="12" :sm="8" :md="6" v-for="i in 8" :key="'rs'+i">
            <GoodsCardSkeleton />
          </el-col>
        </el-row>
        <el-empty v-else description="暂无推荐商品" />
      </section>
    </div>
    <BackToTop />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHotGoods, getRecommendGoods, getHotKeywords } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import { getActiveBanners } from '@/api/banner'
import { getActiveAnnouncements, type AnnouncementVO } from '@/api/announcement'
import GoodsCard from '@/components/GoodsCard.vue'
import GoodsCardSkeleton from '@/components/GoodsCardSkeleton.vue'
import BackToTop from '@/components/BackToTop.vue'
import type { GoodsVO } from '@/api/goods'
import type { GoodsCategory } from '@/api/category'
import type { BannerVO } from '@/api/banner'

const router = useRouter()
const banners = ref<BannerVO[]>([])
const hotGoods = ref<GoodsVO[]>([])
const recommendGoods = ref<GoodsVO[]>([])
const categories = ref<GoodsCategory[]>([])
const selectedCategoryId = ref<number | undefined>(undefined)
const searchKeyword = ref('')
const showSearchDropdown = ref(false)
const searchHistory = ref<string[]>([])
const hotKeywords = ref<string[]>([])
const announcements = ref<AnnouncementVO[]>([])
const homeLoading = ref(true)

const loadHotKeywords = async () => {
  try { hotKeywords.value = await getHotKeywords() || [] } catch (e) { console.error(e) }
}

const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  addSearchHistory(kw)
  showSearchDropdown.value = false
  router.push({ path: '/goods', query: { keyword: kw } })
}

const searchFromHistory = (kw: string) => {
  searchKeyword.value = kw
  handleSearch()
}

const addSearchHistory = (kw: string) => {
  const list = searchHistory.value.filter(k => k !== kw)
  list.unshift(kw)
  searchHistory.value = list.slice(0, 10)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

const hideSearchDropdown = () => {
  setTimeout(() => { showSearchDropdown.value = false }, 200)
}

const loadAnnouncements = async () => {
  try { announcements.value = await getActiveAnnouncements() || [] } catch (e) { console.error(e) }
}

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
  try { banners.value = await getActiveBanners() } catch (e) { console.error(e) }
}

const loadHotGoods = async () => {
  try {
    const res = await getHotGoods()
    let list = res.list || []
    if (selectedCategoryId.value) list = list.filter((g: GoodsVO) => g.categoryId === selectedCategoryId.value)
    hotGoods.value = list.slice(0, 8)
  } catch (e) { console.error(e) }
  finally { homeLoading.value = false }
}

const loadRecommendGoods = async () => {
  try { const res = await getRecommendGoods(); recommendGoods.value = (res.list || []).slice(0, 8) } catch (e) { console.error(e) }
}

const loadCategories = async () => {
  try { const res = await getCategoryList(); categories.value = res || [] } catch (e) { console.error(e) }
}

onMounted(() => {
  const saved = localStorage.getItem('searchHistory')
  if (saved) { try { searchHistory.value = JSON.parse(saved) } catch (e) { console.error(e) } }
  loadBanners(); loadHotGoods(); loadRecommendGoods(); loadCategories(); loadAnnouncements(); loadHotKeywords()
})
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
  background: rgba(255,255,255,0.95);
  border-radius: 20px 20px 0 0;
  border: 1px solid var(--border);
  border-bottom: none;
  box-shadow: var(--shadow-sm);
  margin: 24px 24px 0;
  max-width: 100%;
  padding: 20px;
}

.search-section {
  position: relative;
  max-width: 560px;
  margin: 0 auto;
}
.search-input {
  :deep(.el-input__wrapper) {
    border-radius: 24px;
    box-shadow: 0 2px 12px rgba(99,102,241,0.1);
    padding: 4px 20px;
  }
}
.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  z-index: 100;
  padding: 12px 16px;
  margin-top: 4px;
}
.search-dropdown-section { margin-bottom: 10px; &:last-child { margin-bottom: 0; } }
.search-dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
  font-weight: 600;
}
.search-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.category-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 16px;
}

.category-chip {
  padding: 6px 18px;
  border-radius: 20px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover { border-color: var(--primary); color: var(--primary); }
  &.active { background: var(--primary-gradient); color: #fff; border-color: transparent; box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3); }
}

.announcement-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  margin-top: 14px;
  background: linear-gradient(135deg, #f5f3ff, #ede9fe);
  border-radius: 10px;
  border: 1px solid #e0e7ff;
  overflow: hidden;
}
.announcement-scroll {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  font-size: 12px;
  color: var(--text-secondary);
}
.announcement-item { margin-right: 4px; }
.announcement-divider { margin: 0 8px; color: #c7d2fe; }

</style>
