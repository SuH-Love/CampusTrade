<template>
  <div class="home-page">
    <HomeSkeleton v-if="homeLoading && hotGoods.length === 0" />
    <template v-else>
      <section class="hero">
        <el-carousel :height="heroHeight" :interval="5000" arrow="hover" indicator-position="outside">
          <el-carousel-item v-for="banner in banners" :key="banner.id">
            <div class="hero-slide" :style="{ background: banner.imageUrl ? 'transparent' : (banner.bgColor || 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)') }">
              <img v-if="banner.imageUrl" :src="banner.imageUrl" class="hero-bg-img" alt="轮播图" />
              <div class="hero-overlay" v-if="banner.imageUrl" />
              <div class="hero-content">
                <h1 v-if="banner.title">{{ banner.title }}</h1>
                <p v-if="banner.subtitle">{{ banner.subtitle }}</p>
                <el-button v-if="banner.buttonText && banner.linkUrl" size="large" round :style="bannerButtonStyle(banner)" @click="$router.push(banner.linkUrl)">{{ banner.buttonText }}</el-button>
              </div>
            </div>
          </el-carousel-item>
          <el-carousel-item v-if="banners.length === 0">
            <div class="hero-slide hero-default">
              <div class="hero-bg-particles">
                <span v-for="i in 8" :key="i" class="hero-particle" :style="particleStyle(i)" />
              </div>
              <div class="hero-content">
                <div class="hero-badge">校园闲置好物流转平台</div>
                <h1>校园贸易<span class="gradient-text">新体验</span></h1>
                <p>安全·便捷·值得信赖的校园闲置好物流转平台</p>
                <el-button type="primary" size="large" round @click="$router.push('/goods')">立即探索</el-button>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </section>

      <div class="bento-container">
        <div class="bento-grid">
          <div class="bento-card bento-search-card">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索你想要的宝贝..."
              size="large"
              clearable
              prefix-icon="Search"
              @keyup.enter="handleSearch"
              @input="handleSearchInput"
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
                  <el-tag v-for="(kw, idx) in searchHistory" :key="idx" size="small" round effect="plain" @mousedown.prevent="searchFromHistory(kw)" class="search-tag-btn">{{ kw }}</el-tag>
                </div>
              </div>
              <div class="search-dropdown-section" v-if="hotKeywords.length > 0">
                <div class="search-dropdown-header"><span>智能推荐</span></div>
                <div class="search-tags">
                  <el-tag v-for="(kw, idx) in hotKeywords" :key="idx" size="small" round type="danger" effect="plain" @mousedown.prevent="searchFromHistory(kw)" class="search-tag-btn">{{ kw }}</el-tag>
                </div>
              </div>
            </div>
          </div>

          <div class="bento-card bento-category-card">
            <div class="bento-card-head">
              <h3>快速分类</h3>
              <el-button text type="primary" size="small" @click="$router.push('/goods')">全部分类 →</el-button>
            </div>
            <div class="category-chips">
              <div
                v-for="cat in categories.slice(0, 8)" :key="cat.id"
                class="category-chip"
                :class="{ active: selectedCategoryId === cat.id }"
                @click="toggleCategory(cat.id)"
              >{{ cat.categoryName }}</div>
            </div>
          </div>

          <div class="bento-card bento-announce-card" v-if="announcements.length > 0">
            <div class="bento-card-head">
              <h3>📢 平台公告</h3>
            </div>
            <div class="announce-list">
              <div v-for="(ann, idx) in announcements.slice(0, 3)" :key="ann.id" class="announce-item" :style="{ animationDelay: `${idx * 0.1}s` }">
                <div class="announce-title">{{ ann.title }}</div>
                <div class="announce-content">{{ ann.content }}</div>
              </div>
            </div>
          </div>

          <div class="bento-card bento-hot-card">
            <div class="bento-card-head">
              <h3 class="bento-title-gradient">🔥 热门商品</h3>
              <el-button text type="primary" @click="$router.push('/goods')">查看更多 →</el-button>
            </div>
            <div class="goods-grid" v-if="hotGoods.length > 0">
              <div v-for="(item, idx) in hotGoods" :key="item.id" class="goods-grid-item" :style="{ animationDelay: `${idx * 0.05}s` }">
                <GoodsCard :goods="item" />
              </div>
            </div>
            <EmptyState v-else icon="📦" title="暂无热门商品" description="去看看其他商品吧" action-text="浏览商品" @action="$router.push('/goods')" />
          </div>

          <div class="bento-card bento-new-card">
            <div class="bento-card-head">
              <h3 class="bento-title-gradient">✨ 最新上架</h3>
              <el-button text type="primary" @click="$router.push('/goods')">查看更多 →</el-button>
            </div>
            <div class="goods-grid" v-if="recommendGoods.length > 0">
              <div v-for="(item, idx) in recommendGoods" :key="item.id" class="goods-grid-item" :style="{ animationDelay: `${idx * 0.05}s` }">
                <GoodsCard :goods="item" />
              </div>
            </div>
            <EmptyState v-else icon="🆕" title="暂无推荐商品" description="成为第一个发布商品的人" action-text="去发布" @action="$router.push('/goods/publish')" />
          </div>
        </div>
      </div>
    </template>
    <BackToTop />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHotGoods, getRecommendGoods, getHotKeywords } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import { getActiveBanners } from '@/api/banner'
import { getActiveAnnouncements, type AnnouncementVO } from '@/api/announcement'
import GoodsCard from '@/components/GoodsCard.vue'
import HomeSkeleton from '@/components/HomeSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'
import BackToTop from '@/components/BackToTop.vue'
import { debounce } from '@/utils/labels'
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
const currentAnnouncementIdx = ref(0)
let announcementTimer: ReturnType<typeof setInterval> | null = null

const heroHeight = computed(() => window.innerWidth < 768 ? '220px' : '340px')

const handleSearchInput = debounce(() => {}, 300)

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
  try {
    announcements.value = await getActiveAnnouncements() || []
    if (announcements.value.length > 1) {
      announcementTimer = setInterval(() => {
        currentAnnouncementIdx.value = (currentAnnouncementIdx.value + 1) % announcements.value.length
      }, 4000)
    }
  } catch (e) { console.error(e) }
}

const toggleCategory = (id: number) => {
  selectedCategoryId.value = selectedCategoryId.value === id ? undefined : id
  loadHotGoods()
}

const bannerButtonStyle = (banner: BannerVO) => {
  const color = banner.buttonColor || 'rgba(255,255,255,0.2)'
  const isGradient = color.includes('gradient')
  return { background: color, borderColor: isGradient ? 'transparent' : color, color: '#fff' }
}

const loadBanners = async () => {
  try { banners.value = await getActiveBanners() } catch (e) { console.error(e) }
}

const loadHotGoods = async () => {
  try {
    const res = await getHotGoods()
    let list = res.list || []
    if (selectedCategoryId.value) list = list.filter((g: GoodsVO) => g.categoryId === selectedCategoryId.value)
    hotGoods.value = list.slice(0, 10)
  } catch (e) { console.error(e) }
  finally { homeLoading.value = false }
}

const loadRecommendGoods = async () => {
  try { const res = await getRecommendGoods(); recommendGoods.value = (res.list || []).slice(0, 10) } catch (e) { console.error(e) }
}

const loadCategories = async () => {
  try { const res = await getCategoryList(); categories.value = res || [] } catch (e) { console.error(e) }
}

onMounted(() => {
  const saved = localStorage.getItem('searchHistory')
  if (saved) { try { searchHistory.value = JSON.parse(saved) } catch (e) { console.error(e) } }
  loadBanners(); loadHotGoods(); loadRecommendGoods(); loadCategories(); loadAnnouncements(); loadHotKeywords()
})

const particleStyle = (i: number) => {
  const colors = ['#818cf8', '#a78bfa', '#c4b5fd', '#60a5fa', '#93c5fd']
  const sizes = [4, 6, 8, 10, 12]
  return {
    left: `${(i * 13 + 7) % 100}%`,
    top: `${(i * 17 + 11) % 100}%`,
    width: `${sizes[i % sizes.length]}px`,
    height: `${sizes[i % sizes.length]}px`,
    background: colors[i % colors.length],
    animationDelay: `${i * 0.7}s`,
    animationDuration: `${3 + i * 0.5}s`,
  }
}

onUnmounted(() => {
  if (announcementTimer) clearInterval(announcementTimer)
})
</script>

<style scoped lang="scss">
.hero {
  margin: 0 var(--spacing-lg);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.hero-slide {
  height: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.hero-default {
  background: linear-gradient(135deg, #4338ca 0%, #6366f1 20%, #3b82f6 40%, #8b5cf6 60%, #7c3aed 80%, #4f46e5 100%);
  background-size: 300% 300%;
  animation: heroBgShift 8s ease-in-out infinite;
}

@keyframes heroBgShift {
  0%, 100% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
}

:deep(.dark) .hero-default, .dark .hero-default {
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 20%, #1e3a5f 40%, #4c1d95 60%, #312e81 80%, #1e1b4b 100%);
  background-size: 300% 300%;
}

.hero-bg-particles {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; z-index: 0;
}

.hero-particle {
  position: absolute;
  border-radius: 50%;
  opacity: 0;
  animation: particleFloat 4s ease-in-out infinite;
}

@keyframes particleFloat {
  0%, 100% { opacity: 0; transform: translateY(0) scale(0.5); }
  20% { opacity: 0.6; }
  50% { opacity: 0.3; transform: translateY(-20px) scale(1.2); }
  80% { opacity: 0.6; }
}

.hero-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: linear-gradient(180deg, rgba(0,0,0,0.05) 0%, rgba(0,0,0,0.3) 100%);
}
.dark .hero-overlay {
  background: linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(0,0,0,0.5) 100%);
}

.hero-content {
  position: relative; z-index: 1;
  h1 { font-size: 42px; font-weight: 800; margin-bottom: 8px; letter-spacing: -0.5px; text-shadow: 0 2px 12px rgba(0,0,0,0.2); line-height: 1.2; }
  .gradient-text { background: linear-gradient(90deg, #fbbf24, #f59e0b); background-size: 200% auto; -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
  p { font-size: 16px; opacity: 0.9; margin-bottom: 24px; text-shadow: 0 1px 4px rgba(0,0,0,0.1); }
  .el-button {
    font-size: 16px; padding: 14px 36px; height: auto;
    background: rgba(255,255,255,0.2) !important; border: 1px solid rgba(255,255,255,0.4) !important; color: #fff !important;
    backdrop-filter: blur(8px); font-weight: 600;
    &:hover { background: rgba(255,255,255,0.35) !important; transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.2); }
  }
}

.hero-badge {
  display: inline-block;
  padding: 4px 16px;
  border-radius: 20px;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
  backdrop-filter: blur(8px);
  letter-spacing: 0.5px;
}

.hero-bg-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; filter: brightness(var(--img-brightness)); }


.bento-container {
  padding: var(--spacing-lg);
  max-width: 1280px;
  margin: 0 auto;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.bento-card {
  background: var(--bg-glass);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  transition: var(--transition-slow);
  animation: fadeInUp 0.5s ease-out backwards;
  &:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
}

.bento-search-card {
  grid-column: span 4;
  padding: 16px 20px;
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
}
.search-input { :deep(.el-input__wrapper) { border-radius: 24px; box-shadow: 0 2px 16px rgba(99,102,241,0.12); padding: 6px 20px; transition: var(--transition); } :deep(.el-input__wrapper:hover) { box-shadow: 0 4px 20px rgba(99,102,241,0.2); } }
.search-dropdown {
  position: absolute; top: 100%; left: 0; right: 0;
  background: var(--color-dropdown-bg); border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  z-index: 100; padding: 12px 16px; margin-top: 4px;
}
.search-dropdown-section { margin-bottom: 10px; &:last-child { margin-bottom: 0; } }
.search-dropdown-header { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-muted); margin-bottom: 6px; font-weight: 600; }
.search-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.search-tag-btn { cursor: pointer; }

.bento-category-card { grid-column: span 3; }
.bento-announce-card { grid-column: span 1; }
.bento-hot-card, .bento-new-card { grid-column: span 4; }

.bento-card-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
  h3 { font-size: 18px; font-weight: 700; color: var(--text-primary); }
}
.bento-title-gradient {
  background: var(--primary-gradient-gloss);
  background-size: 200% auto;
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}

.category-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.category-chip {
  padding: 8px 18px; border-radius: 20px; background: var(--color-chip-bg);
  border: 1px solid var(--color-chip-border); font-size: 13px; font-weight: 500;
  color: var(--text-secondary); cursor: pointer; transition: all 0.2s ease;
  white-space: nowrap;
  &:hover { border-color: var(--primary); color: var(--primary); transform: translateY(-1px); }
  &.active { background: var(--primary-gradient); color: #fff; border-color: transparent; box-shadow: 0 2px 12px rgba(99, 102, 241, 0.3); }
}

.announce-list { display: flex; flex-direction: column; gap: 12px; }
.announce-item {
  padding: 10px 12px;
  background: linear-gradient(135deg, var(--color-announcement-from), var(--color-announcement-to));
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-announcement-border);
  animation: fadeInUp 0.4s ease-out backwards;
  .announce-title { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
  .announce-content { font-size: 12px; color: var(--text-secondary); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
}

.goods-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
.goods-grid-item { min-width: 0; animation: fadeInUp 0.4s ease-out backwards; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .goods-grid { grid-template-columns: repeat(4, 1fr); }
  .bento-category-card { grid-column: span 4; }
  .bento-announce-card { grid-column: span 4; }
}
@media (max-width: 900px) {
  .goods-grid { grid-template-columns: repeat(3, 1fr); }
  .hero-slide { height: 280px; }
  .hero-content h1 { font-size: 32px; }
}
@media (max-width: 600px) {
  .goods-grid { grid-template-columns: repeat(2, 1fr); }
  .bento-container { padding: var(--spacing-md); }
  .hero { margin: 0 var(--spacing-md); }
  .hero-slide { height: 220px; }
  .hero-content h1 { font-size: 24px; }
  .hero-content p { font-size: 14px; }
}
</style>
