<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-content">
        <div class="hero-text">
          <h1>让每段旅程<br>都值得刻下</h1>
          <p class="hero-desc">
            从灵感到回忆，AI 助你规划行程、记录旅途、沉淀故事。
            一次旅行，完整体验。
          </p>
          <div class="hero-actions">
            <a-button type="primary" size="large" @click="enterPrimary">
              {{ isLoggedIn ? '开始规划' : '立即体验' }}
            </a-button>
          </div>
        </div>

        <div class="hero-visual">
          <div v-if="heroPhotos[0]" class="hero-image-wrapper">
            <img :src="photoSrc(heroPhotos[0])" alt="旅行图片" class="hero-image">
            <div class="hero-credit">
              <PexelsCredit :image="heroPhotos[0]" compact />
            </div>
          </div>
          <img v-else :src="illustrations.travelMode" alt="旅行插画" class="hero-illustration">
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PexelsCredit from '@/components/content/PexelsCredit.vue'
import { useVisualContent } from '@/composables/useVisualContent'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const { fetchCollection, illustrations } = useVisualContent()

const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))
const heroPhotos = ref<API.ContentImageVO[]>([])

function photoSrc(image?: API.ContentImageVO | null) {
  return image?.landscapeUrl || image?.large2xUrl || image?.largeUrl || image?.mediumUrl || ''
}

async function loadVisuals() {
  const hero = await fetchCollection('beautiful travel vacation', 2)
  heroPhotos.value = hero
}

function enterPrimary() {
  router.push(isLoggedIn.value ? '/workspace' : '/user/login')
}

onMounted(loadVisuals)
</script>

<style scoped lang="scss">
.home-page {
  max-width: 1320px;
  margin: 0 auto;
  padding: 32px 20px 72px;
}

.hero {
  padding: 60px 48px;
  border-radius: 32px;
  background: linear-gradient(135deg, #f0f6ff 0%, #fff9f2 100%);
  box-shadow: 0 20px 60px rgba(18, 52, 97, 0.08);
}

.hero-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: center;
}

.hero-text {
  h1 {
    margin: 0 0 20px;
    font-size: 52px;
    font-weight: 700;
    line-height: 1.15;
    color: var(--color-text);
  }
}

.hero-desc {
  margin: 0 0 32px;
  font-size: 18px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.hero-visual {
  display: flex;
  justify-content: center;
  align-items: center;
}

.hero-image-wrapper {
  position: relative;
  width: 100%;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.hero-image {
  width: 100%;
  height: 400px;
  object-fit: cover;
  display: block;
}

.hero-credit {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-sm);
}

.hero-illustration {
  max-width: 100%;
  height: auto;
  max-height: 400px;
}

@media (max-width: 1200px) {
  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 20px 12px 48px;
  }

  .hero {
    padding: 32px 20px;
  }

  .hero-content {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .hero-text h1 {
    font-size: 36px;
  }

  .hero-desc {
    font-size: 16px;
  }
}
</style>
