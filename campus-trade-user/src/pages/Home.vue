<template>
  <div class="home-page">
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

      <div class="category-bar">
        <div class="category-scroll">
          <div
            v-for="cat in categories" :key="cat.id"
            class="category-chip"
            :class="{ active: selectedCategoryId === cat.id }"
            @click="toggleCategory(cat.id)"
          >{{ cat.categoryName }}</div>
        </div>
      </div>

      <div class="announcement-bar" v-if="announcements.length > 0">
        <el-icon class="announcement-icon"><Bell /></el-icon>
        <div class="announcement-scroll">
          <transition name="announcement-slide" mode="out-in">
            <div :key="currentAnnouncementIdx" class="announcement-text">
              <strong>{{ announcements[currentAnnouncementIdx]?.title }}</strong>：{{ announcements[currentAnnouncementIdx]?.content }}
            </div>
          </transition>
        </div>
      </div>

      <section class="mt-lg">
        <div class="flex-between mb-md">
          <h3 class="section-title mb-0">热门商品</h3>
          <el-button text type="primary" @click="$router.push('/goods')">查看更多 →</el-button>
        </div>
        <div class="goods-grid" v-if="hotGoods.length > 0">
          <div v-for="item in hotGoods" :key="item.id" class="goods-grid-item">
            <GoodsCard :goods="item" />
          </div>
        </div>
        <div class="goods-grid" v-else-if="homeLoading">
          <div v-for="i in 10" :key="'hs'+i" class="goods-grid-item">
            <GoodsCardSkeleton />
          </div>
        </div>
        <EmptyState v-else icon="📦" title="暂无热门商品" description="去看看其他商品吧" action-text="浏览商品" @action="$router.push('/goods')" />
      </section>

      <section class="mt-lg">
        <div class="flex-between mb-md">
          <h3 class="section-title mb-0">最新上架</h3>
          <el-button text type="primary" @click="$router.push('/goods')">查看更多 →</el-button>
        </div>
        <div class="goods-grid" v-if="recommendGoods.length > 0">
          <div v-for="item in recommendGoods" :key="item.id" class="goods-grid-item">
            <GoodsCard :goods="item" />
          </div>
        </div>
        <div class="goods-grid" v-else-if="homeLoading">
          <div v-for="i in 10" :key="'rs'+i" class="goods-grid-item">
            <GoodsCardSkeleton />
          </div>
        </div>
        <EmptyState v-else icon="🆕" title="暂无推荐商品" description="成为第一个发布商品的人" action-text="去发布" @action="$router.push('/goods/publish')" />
      </section>
    </div>
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
import GoodsCardSkeleton from '@/components/GoodsCardSkeleton.vue'
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

const heroHeight = computed(() => window.innerWidth < 768 ? '200px' : '300px')

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
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  position: relative;
}

.hero-default { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%); }
:deep(.dark) .hero-default, .dark .hero-default { background: linear-gradient(135deg, #4338ca 0%, #6d28d9 50%, #7c3aed 100%); }

.hero-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.35) 100%);
}
.dark .hero-overlay {
  background: linear-gradient(180deg, rgba(0,0,0,0.3) 0%, rgba(0,0,0,0.55) 100%);
}

.hero-content {
  position: relative; z-index: 1;
  h1 { font-size: 36px; font-weight: 800; margin-bottom: 12px; letter-spacing: -0.5px; text-shadow: 0 2px 8px rgba(0,0,0,0.15); }
  p { font-size: 16px; opacity: 0.92; margin-bottom: 24px; text-shadow: 0 1px 4px rgba(0,0,0,0.1); }
  .el-button { font-size: 16px; padding: 12px 32px; background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); color: #fff; &:hover { background: rgba(255,255,255,0.35); transform: translateY(-2px); } }
}

.hero-bg-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; filter: brightness(var(--img-brightness)); }

.home-content {
  background: var(--bg-glass);
  border-radius: 20px 20px 0 0;
  border: 1px solid var(--border);
  border-bottom: none;
  box-shadow: var(--shadow-sm);
  margin: var(--spacing-lg) var(--spacing-lg) 0;
  padding: 20px;
}

.search-section { position: relative; max-width: 560px; margin: 0 auto; }
.search-input { :deep(.el-input__wrapper) { border-radius: 24px; box-shadow: 0 2px 12px rgba(99,102,241,0.1); padding: 4px 20px; } }
.search-dropdown {
  position: absolute; top: 100%; left: 0; right: 0;
  background: var(--color-dropdown-bg); border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  z-index: 100; padding: 12px 16px; margin-top: 4px;
}
.search-dropdown-section { margin-bottom: 10px; &:last-child { margin-bottom: 0; } }
.search-dropdown-header { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-muted); margin-bottom: 6px; font-weight: 600; }
.search-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.search-tag-btn { cursor: pointer; }

.category-bar { margin-top: var(--spacing-md); overflow: hidden; }
.category-scroll {
  display: flex; gap: 8px; overflow-x: auto; justify-content: center;
  padding-bottom: 4px; scrollbar-width: none; &::-webkit-scrollbar { display: none; }
}

.category-chip {
  padding: 6px 18px; border-radius: 20px; background: var(--color-chip-bg);
  border: 1px solid var(--color-chip-border); font-size: 13px; font-weight: 500;
  color: var(--text-secondary); cursor: pointer; transition: all 0.2s ease;
  white-space: nowrap; flex-shrink: 0;
  &:hover { border-color: var(--primary); color: var(--primary); }
  &.active { background: var(--primary-gradient); color: #fff; border-color: transparent; box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3); }
}

.announcement-bar {
  display: flex; align-items: center; gap: 8px; padding: 8px 16px;
  margin-top: 14px; background: linear-gradient(135deg, var(--color-announcement-from), var(--color-announcement-to));
  border-radius: 10px; border: 1px solid var(--color-announcement-border); overflow: hidden;
}
.announcement-scroll { flex: 1; overflow: hidden; white-space: nowrap; font-size: 12px; color: var(--text-secondary); }
.announcement-icon { color: var(--primary); font-size: 16px; flex-shrink: 0; }
.announcement-text { display: inline; }

.announcement-slide-enter-active { transition: all 0.4s ease; }
.announcement-slide-leave-active { transition: all 0.3s ease; }
.announcement-slide-enter-from { opacity: 0; transform: translateY(10px); }
.announcement-slide-leave-to { opacity: 0; transform: translateY(-10px); }

.goods-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
.goods-grid-item { min-width: 0; }

@media (max-width: 1200px) { .goods-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 900px) { .goods-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 600px) { .goods-grid { grid-template-columns: repeat(2, 1fr); } }


@media (max-width: 768px) {
  .hero-slide { height: 200px; }
  .hero-content h1 { font-size: 24px; }
  .hero-content p { font-size: 14px; }
  .home-content { margin: var(--spacing-md) var(--spacing-md) 0; padding: var(--spacing-md); }
}
</style>
