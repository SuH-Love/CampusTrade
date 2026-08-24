<template>
  <div ref="pageRoot" class="page-root">
    <router-view v-slot="{ Component }">
      <transition :css="false"
        @before-enter="onBeforeEnter"
        @enter="onEnter"
        @leave="onLeave">
        <component :is="Component" :key="$route.path" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()
const $route = useRoute()
const pageRoot = ref<HTMLElement>()

let isFirstNav = true

function easeInOutCubic(t: number): number {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}

function createRipple(x: number, y: number) {
  const ripple = document.createElement('div')
  ripple.style.cssText = `
    position: fixed; left: ${x}px; top: ${y}px;
    width: 10px; height: 10px; border-radius: 50%;
    border: 2px solid rgba(14, 165, 233, 0.7);
    pointer-events: none; z-index: 99998;
    transform: translate(-50%, -50%);
    transition: all 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  `
  document.body.appendChild(ripple)
  requestAnimationFrame(() => {
    ripple.style.width = '80px'
    ripple.style.height = '80px'
    ripple.style.opacity = '0'
    ripple.style.borderWidth = '1px'
  })
  setTimeout(() => ripple.remove(), 650)
}

function createCursorGlow() {
  const glow = document.createElement('div')
  glow.id = 'cursor-glow'
  glow.style.cssText = `
    position: fixed; width: 24px; height: 24px;
    border-radius: 50%; pointer-events: none; z-index: 99998;
    background: radial-gradient(circle, rgba(14,165,233,0.7) 0%, rgba(20,184,166,0.3) 40%, transparent 70%);
    transform: translate(-50%, -50%);
    transition: transform 0.12s ease-out, opacity 0.3s;
    mix-blend-mode: screen;
    box-shadow: 0 0 20px rgba(14, 165, 233, 0.4);
    opacity: 0;
  `
  document.body.appendChild(glow)

  let visible = false
  document.addEventListener('mousemove', (e) => {
    glow.style.left = e.clientX + 'px'
    glow.style.top = e.clientY + 'px'
    if (!visible) {
      glow.style.opacity = '1'
      visible = true
    }
  })
  document.addEventListener('mouseleave', () => {
    glow.style.opacity = '0'
    visible = false
  })
}

function onBeforeEnter(el: Element) {
  const e = el as HTMLElement
  e.style.cssText += `
    position: absolute; top: 0; left: 0; right: 0; z-index: 2;
    clip-path: inset(0 100% 0 0);
  `
  const beam = document.createElement('div')
  beam.className = 'light-beam'
  beam.style.cssText = `
    position: fixed; top: 0; bottom: 0; width: 160px; left: 0;
    background: linear-gradient(to right,
      transparent 0%,
      rgba(14, 165, 233, 0.2) 20%,
      rgba(14, 165, 233, 0.7) 42%,
      rgba(255, 255, 255, 0.95) 50%,
      rgba(14, 165, 233, 0.7) 58%,
      rgba(14, 165, 233, 0.2) 80%,
      transparent 100%);
    z-index: 99999; pointer-events: none;
    mix-blend-mode: screen;
    box-shadow: 0 0 60px rgba(14, 165, 233, 0.6), 0 0 120px rgba(14, 165, 233, 0.3);
    transform: translateX(-80px);
  `
  e.appendChild(beam)

  const beam2 = document.createElement('div')
  beam2.className = 'light-beam2'
  beam2.style.cssText = `
    position: fixed; top: 0; bottom: 0; width: 80px; left: 0;
    background: linear-gradient(to right,
      transparent, rgba(255,255,255,0.5), transparent);
    z-index: 99999; pointer-events: none;
    mix-blend-mode: screen;
    transform: translateX(-40px); opacity: 0.6;
  `
  e.appendChild(beam2)
}

function onEnter(el: Element, done: () => void) {
  const e = el as HTMLElement
  const beam = e.querySelector('.light-beam') as HTMLElement
  const beam2 = e.querySelector('.light-beam2') as HTMLElement
  const duration = 550
  const start = performance.now()


  function frame(now: number) {
    const t = Math.min((now - start) / duration, 1)
    const eased = easeInOutCubic(t)
    const reveal = eased * 100

    e.style.clipPath = `inset(0 ${100 - reveal}% 0 0)`
    if (beam) {
      beam.style.left = `${reveal}%`
      beam.style.transform = `translateX(-80px)`
      beam.style.opacity = t < 0.92 ? '1' : `${Math.max(0, (1 - t) / 0.08)}`
    }
    if (beam2) {
      beam2.style.left = `${reveal * 1.02}%`
      beam2.style.transform = `translateX(-40px)`
      beam2.style.opacity = t < 0.9 ? '0.6' : '0'
    }

    if (t < 1) {
      requestAnimationFrame(frame)
    } else {
      e.style.clipPath = ''
      e.style.position = ''
      e.style.zIndex = ''
      beam?.remove()
      beam2?.remove()

      done()
    }
  }
  requestAnimationFrame(frame)
}

function onLeave(el: Element, done: () => void) {
  const e = el as HTMLElement
  e.style.cssText += `
    position: absolute; top: 0; left: 0; right: 0; z-index: 1;
    transition: opacity 0.45s ease-in, filter 0.45s ease-in;
    opacity: 0; filter: brightness(1.3);
  `
  e.addEventListener('transitionend', done, { once: true })
}

router.afterEach(() => {
  if (isFirstNav) isFirstNav = false
})

onMounted(async () => {
  createCursorGlow()
  document.addEventListener('click', (e) => {
    createRipple(e.clientX, e.clientY)
  })
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.clearAuth()
    }
  }
})
</script>

<style>
.page-root {
  position: relative;
  min-height: 100vh;

}

</style>
