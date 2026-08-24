<template>
  <div class="home-page page-bg">
    <HomeSkeleton v-if="homeLoading && hotGoods.length === 0" />
    <template v-else>
      <section class="hero">
        <el-carousel :height="heroHeight" :interval="5000" arrow="hover">
          <el-carousel-item v-for="banner in allBanners" :key="banner.id || 'default'">
            <div class="hero-slide" :class="{ 'hero-default': !banner.id }" :style="banner.id ? { background: banner.imageUrl ? 'transparent' : (banner.bgColor || 'linear-gradient(135deg, #0EA5E9 0%, #14B8A6 100%)') } : {}">
              <img v-if="banner.imageUrl" :src="banner.imageUrl" class="hero-bg-img" alt="轮播图" />
              <div class="hero-overlay" v-if="banner.imageUrl" />
              <template v-if="!banner.id">
                <div class="hero-bg-particles">
                  <span v-for="i in 4" :key="i" class="hero-particle" :style="particleStyle(i)" />
                </div>
                <div class="hero-content">
                  <div class="hero-badge">校园闲置好物流转平台</div>
                  <h1>校园贸易<span class="gradient-text">新体验</span></h1>
                  <p>安全·便捷·值得信赖的校园闲置好物流转平台</p>
                  <el-button type="primary" size="large" round @click="$router.push('/goods')">立即探索</el-button>
                </div>
              </template>
              <div v-else class="hero-content">
                <h1 v-if="banner.title">{{ banner.title }}</h1>
                <p v-if="banner.subtitle">{{ banner.subtitle }}</p>
                <el-button v-if="banner.buttonText && banner.linkUrl" size="large" round :style="bannerButtonStyle(banner)" @click="$router.push(banner.linkUrl)">{{ banner.buttonText }}</el-button>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
        <div v-if="announcements.length > 0" class="hero-announce-bar">
          <el-icon><BellFilled /></el-icon>
          <div class="announce-marquee">
            <transition name="announce-slide" mode="out-in">
              <span :key="currentAnnouncementIdx" class="announce-marquee-text">{{ announcements[currentAnnouncementIdx]?.title }}：{{ announcements[currentAnnouncementIdx]?.content }}</span>
            </transition>
          </div>
        </div>
      </section>

      <div class="bento-container">
        <div class="bento-grid">
          <div class="bento-card bento-search-card">
            <div class="search-wrap">
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
              <div class="search-dropdown" v-if="showSearchDropdown">
                <!-- 实时联想 -->
                <div class="search-dropdown-section" v-if="suggestList.length > 0">
                  <div class="search-dropdown-header"><span>猜你想找</span></div>
                  <div class="search-tags">
                    <el-tag v-for="(kw, idx) in suggestList" :key="'s'+idx" size="small" round effect="plain" @mousedown.prevent="searchFromHistory(kw, $event)" class="search-tag-btn">{{ kw }}</el-tag>
                  </div>
                </div>
                <!-- 搜索历史分组 -->
                <div class="search-dropdown-section" v-if="todayHistory.length > 0">
                  <div class="search-dropdown-header"><span>今天</span><el-button link type="info" size="small" @click="clearSearchHistory">清空</el-button></div>
                  <div class="search-tags">
                    <el-tag v-for="(item, idx) in todayHistory" :key="'t'+idx" size="small" round effect="plain" @mousedown.prevent="searchFromHistory(item.kw, $event)" class="search-tag-btn" closable @close="removeHistoryItem(idx, 'today')">{{ item.kw }}</el-tag>
                  </div>
                </div>
                <div class="search-dropdown-section" v-if="yesterdayHistory.length > 0">
                  <div class="search-dropdown-header"><span>昨天</span></div>
                  <div class="search-tags">
                    <el-tag v-for="(item, idx) in yesterdayHistory" :key="'y'+idx" size="small" round effect="plain" @mousedown.prevent="searchFromHistory(item.kw, $event)" class="search-tag-btn" closable @close="removeHistoryItem(idx, 'yesterday')">{{ item.kw }}</el-tag>
                  </div>
                </div>
                <div class="search-dropdown-section" v-if="earlierHistory.length > 0">
                  <div class="search-dropdown-header"><span>更早</span></div>
                  <div class="search-tags">
                    <el-tag v-for="(item, idx) in earlierHistory" :key="'e'+idx" size="small" round effect="plain" @mousedown.prevent="searchFromHistory(item.kw, $event)" class="search-tag-btn" closable @close="removeHistoryItem(idx, 'earlier')">{{ item.kw }}</el-tag>
                  </div>
                </div>
                <!-- 智能推荐（带热门/新趋势标记） -->
                <div class="search-dropdown-section" v-if="hotKeywords.length > 0">
                  <div class="search-dropdown-header"><span>智能推荐</span></div>
                  <div class="search-tags">
                    <el-tag v-for="(item, idx) in hotKeywords" :key="'h'+idx" size="small" round :type="item.type === 'hot' ? 'danger' : 'warning'" effect="plain" @mousedown.prevent="searchFromHistory(item.keyword, $event)" class="search-tag-btn">
                      {{ item.keyword }}<span v-if="item.type === 'hot'" class="kw-type-text">hot</span><span v-else-if="item.type === 'new'" class="kw-type-text">new</span>
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>

            <div class="category-chips" v-if="!categoryExpanded">
              <div
                v-for="cat in categories.slice(0, 12)" :key="cat.id"
                class="category-chip"
                :class="{ active: selectedCategoryId === cat.id, 'cat-empty': !cat.goodsCount }"
                @click="toggleCategory(cat.id)"
              >{{ cat.categoryName }}<span v-if="cat.goodsCount" class="chip-count">{{ cat.goodsCount }}</span></div>
              <div class="category-expand-icon" @click="categoryExpanded = true">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
            <div class="category-expanded-strip" v-else>
              <div
                v-for="cat in categories" :key="cat.id"
                class="category-chip"
                :class="{ active: selectedCategoryId === cat.id, 'cat-empty': !cat.goodsCount }"
                @click="toggleCategory(cat.id)"
              >{{ cat.categoryName }}<span v-if="cat.goodsCount" class="chip-count">{{ cat.goodsCount }}</span></div>
              <div class="category-expand-icon collapse" @click="categoryExpanded = false">
                <el-icon><ArrowLeft /></el-icon>
              </div>
            </div>
          </div>

          <div class="bento-card bento-hot-card">
            <div class="bento-card-head">
              <h3><span class="title-emoji">🔥</span> <span class="bento-title-gradient">热门商品</span></h3>
              <el-button class="more-btn" size="small" @click="$router.push('/goods')">查看更多 <el-icon class="more-arrow"><ArrowRight /></el-icon></el-button>
            </div>
            <div class="goods-grid" v-if="hotGoods.length > 0">
              <div v-for="(item, idx) in hotGoods" :key="item.id" class="goods-grid-item" :style="idx < 8 ? { animationDelay: `${idx * 0.05}s` } : undefined">
                <GoodsCard :goods="item" />
              </div>
            </div>
            <EmptyState v-else icon="📦" title="暂无热门商品" description="去看看其他商品吧" action-text="浏览商品" @action="$router.push('/goods')" />
          </div>

          <div class="bento-card bento-new-card">
            <div class="bento-card-head">
              <h3><span class="title-emoji">✨</span> <span class="bento-title-gradient">最新上架</span></h3>
              <el-button class="more-btn" size="small" @click="$router.push('/goods')">查看更多 <el-icon class="more-arrow"><ArrowRight /></el-icon></el-button>
            </div>
            <div class="goods-grid" v-if="recommendGoods.length > 0">
              <div v-for="(item, idx) in recommendGoods" :key="item.id" class="goods-grid-item" :style="idx < 8 ? { animationDelay: `${idx * 0.05}s` } : undefined">
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
import { BellFilled, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import { getHotGoods, getRecommendGoods, getHotKeywords, getSuggest, type HotKeywordVO } from '@/api/goods'
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
const categoryExpanded = ref(false)
const searchHistory = ref<{ kw: string; ts: number }[]>([])
const hotKeywords = ref<HotKeywordVO[]>([])
const suggestList = ref<string[]>([])
const announcements = ref<AnnouncementVO[]>([])
const homeLoading = ref(true)
const currentAnnouncementIdx = ref(0)
let announcementTimer: ReturnType<typeof setInterval> | null = null
const windowWidth = ref(window.innerWidth)

const heroHeight = computed(() => windowWidth.value < 768 ? '220px' : '340px')

const allBanners = computed(() => {
  const defaultBanner = { id: 0, title: '', subtitle: '', buttonText: '', linkUrl: '' }
  return [defaultBanner, ...banners.value]
})

const onResize = () => { windowWidth.value = window.innerWidth }

const handleSearchInput = debounce(() => {
  const kw = searchKeyword.value.trim()
  if (kw.length >= 1) {
    getSuggest(kw).then(res => { suggestList.value = res || [] }).catch(() => {})
  } else {
    suggestList.value = []
  }
}, 300)

const todayHistory = computed(() => {
  const today = new Date().setHours(0,0,0,0)
  return searchHistory.value.filter(h => h.ts >= today)
})
const yesterdayHistory = computed(() => {
  const today = new Date().setHours(0,0,0,0)
  const yesterday = today - 86400000
  return searchHistory.value.filter(h => h.ts >= yesterday && h.ts < today)
})
const earlierHistory = computed(() => {
  const yesterday = new Date().setHours(0,0,0,0) - 86400000
  return searchHistory.value.filter(h => h.ts < yesterday)
})


const loadHotKeywords = async () => {
  try { hotKeywords.value = await getHotKeywords() || [] } catch (e) { console.error(e) }
}

const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  addSearchHistory(kw)
  showSearchDropdown.value = false
  sessionStorage.setItem('goodsSearchKeyword', kw)
  router.push('/goods')
}

const searchFromHistory = (kw: string, e?: MouseEvent) => {
  if (e && (e.target as HTMLElement).closest('.el-tag__close')) return
  searchKeyword.value = kw
  handleSearch()
}

const addSearchHistory = (kw: string) => {
  const list = searchHistory.value.filter(k => k.kw !== kw)
  list.unshift({ kw, ts: Date.now() })
  searchHistory.value = list.slice(0, 20)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

const removeHistoryItem = (idx: number, group: string) => {
  const arr = group === 'today' ? todayHistory.value : group === 'yesterday' ? yesterdayHistory.value : earlierHistory.value
  const item = arr[idx]
  if (item) {
    searchHistory.value = searchHistory.value.filter(h => h.kw !== item.kw)
    localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
  }
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
      }, 6000)
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
  try {
    const res = await getCategoryList()
    const list = res || []
    const withCount = list.filter(c => c.goodsCount).sort((a, b) => (b.goodsCount || 0) - (a.goodsCount || 0))
    const withoutCount = list.filter(c => !c.goodsCount).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    categories.value = [...withCount, ...withoutCount]
  } catch (e) { console.error(e) }
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  const saved = localStorage.getItem('searchHistory')
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      searchHistory.value = parsed.map((item: any) => typeof item === 'string' ? { kw: item, ts: 0 } : item)
    } catch (e) { console.error(e) }
  }
  loadBanners(); loadHotGoods(); loadRecommendGoods(); loadCategories(); loadAnnouncements(); loadHotKeywords()
})

const particleStyle = (i: number) => {
  const colors = ['#38BDF8', '#2DD4BF', '#5EEAD4', '#38BDF8', '#7DD3FC']
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
  window.removeEventListener('resize', onResize)
  if (announcementTimer) clearInterval(announcementTimer)
})
</script>

<style scoped lang="scss">
.hero {
  margin: 0 var(--spacing-lg);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  position: relative;
  transform: translateZ(0);
  clip-path: inset(0 0 0 0 round 0 0 var(--radius-lg) var(--radius-lg));
  :deep(.el-carousel) { overflow: hidden; }
  :deep(.el-carousel__container) { overflow: hidden; }
  :deep(.el-carousel__item) { overflow: hidden; width: 100%; }
  :deep(.el-carousel__track) { overflow: hidden; }
}

.hero-announce-bar {
  display: flex; align-items: center; gap: 4px;
  padding: 10px 24px;
  background: linear-gradient(90deg, rgba(14, 165, 233, 0.08), rgba(20, 184, 166, 0.06), rgba(14, 165, 233, 0.08));

  border-top: 1px solid var(--border-light);
  color: var(--text-secondary);
  font-size: 14px;
  .el-icon { color: var(--primary); flex-shrink: 0; }
}
.announce-marquee {
  flex: 1; overflow: hidden; white-space: nowrap; padding: 0 8px;
}
.announce-marquee-text {
  display: inline-block;
  font-weight: 500;
}
.announce-slide-enter-active, .announce-slide-leave-active {
  transition: all 0.4s ease;
}
.announce-slide-enter-from { opacity: 0; transform: translateX(20px); }
.announce-slide-leave-to { opacity: 0; transform: translateX(-20px); }

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
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 20%, #0EA5E9 40%, #14B8A6 60%, #0D9488 80%, #0284C7 100%);
  background-size: 300% 300%;
  animation: heroBgShift 16s ease-in-out infinite;
}

@keyframes heroBgShift {
  0%, 100% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
}

:deep(.dark) .hero-default, .dark .hero-default {
  background: linear-gradient(135deg, #082F49 0%, #0C4A6E 20%, #0C4A6E 40%, #134E4A 60%, #0C4A6E 80%, #082F49 100%);
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
  max-width: 1480px;
  margin: 0 auto;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.bento-card {
  background: var(--bg-glass);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
  &:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
}

.bento-search-card {
  grid-column: span 4;
  padding: 16px 20px;
  background: var(--bg-glass);
  position: relative;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.search-wrap { position: relative; }

.search-input { :deep(.el-input__wrapper) { border-radius: 24px; box-shadow: 0 2px 16px rgba(14, 165, 233, 0.12); padding: 6px 20px; transition: var(--transition); } :deep(.el-input__wrapper:hover) { box-shadow: 0 4px 20px rgba(14, 165, 233, 0.2); } }
.search-dropdown {
  position: absolute; top: 100%; left: 0; right: 0;
  background: var(--color-dropdown-bg); border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  z-index: 200; padding: 12px 16px; margin-top: 4px;
  border: 1px solid var(--border);
}
.search-dropdown-section { margin-bottom: 10px; &:last-child { margin-bottom: 0; } }
.search-dropdown-header { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-muted); margin-bottom: 6px; font-weight: 600; }
.search-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.search-tag-btn { cursor: pointer; }
.kw-type-text { font-size: 10px; font-weight: 700; margin-left: 4px; padding: 0 4px; border-radius: 4px; opacity: 0.85; }

.bento-hot-card, .bento-new-card { grid-column: span 4; }

.bento-card-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
  h3 { font-size: 18px; font-weight: 700; color: var(--text-primary); }
}
.bento-title-gradient {
  background: var(--primary-gradient-gloss);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.title-emoji { font-size: 20px; }
.more-arrow { margin-left: 2px; transition: transform 0.2s ease; }
:deep(.el-button:hover .more-arrow) { transform: translateX(3px); }

.more-btn {
  background: var(--primary-gradient) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-weight: 600 !important;
  border-radius: 20px !important;
  padding: 6px 16px !important;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.25);
  transition: var(--transition) !important;
  &:hover {
    opacity: 0.92;
    transform: translateY(-1px);
    box-shadow: 0 4px 14px rgba(14, 165, 233, 0.35);
  }
}

.category-chips { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; align-items: center; }
.category-expanded-strip {
  display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px;
  align-items: center;
  &::-webkit-scrollbar { height: 6px; }
  &::-webkit-scrollbar-track { background: var(--bg-hover); border-radius: 3px; }
  &::-webkit-scrollbar-thumb { background: var(--primary); border-radius: 3px; }
  .category-expand-icon { position: sticky; right: 0; flex-shrink: 0; z-index: 2; }
}
.category-chip {
  padding: 8px 18px; border-radius: 20px; background: var(--color-chip-bg);
  border: 1px solid var(--color-chip-border); font-size: 13px; font-weight: 500;
  color: var(--text-secondary); cursor: pointer; transition: all 0.2s ease;
  white-space: nowrap; display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0;
  &:hover { border-color: var(--primary); color: var(--primary); transform: translateY(-1px); }
  &.active { background: var(--primary-gradient); color: #fff; border-color: transparent; box-shadow: 0 2px 12px rgba(14, 165, 233, 0.3); .chip-count { background: rgba(255,255,255,0.3); color: #fff; } }
  &.cat-empty { opacity: 0.5; }
}
.chip-count { font-size: 11px; background: var(--bg-hover); color: var(--text-muted); padding: 1px 6px; border-radius: 10px; font-weight: 600; }

.category-expand-icon {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border-radius: 50%;
  background: var(--color-chip-bg); border: 1px solid var(--color-chip-border);
  color: var(--text-secondary); cursor: pointer; flex-shrink: 0;
  transition: all 0.3s ease;
  &:hover { background: var(--primary); color: #fff; border-color: var(--primary); transform: translateX(6px); box-shadow: 0 2px 12px rgba(14, 165, 233, 0.3); }
  &.collapse { &:hover { transform: translateX(-6px); } }
}

.goods-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
.goods-grid-item { min-width: 0; animation: fadeInUp 0.4s ease-out backwards; contain: layout style; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .goods-grid { grid-template-columns: repeat(4, 1fr); }
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
