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
            <a-button size="large" @click="goInspiration">探索灵感</a-button>
          </div>
        </div>

        <div class="hero-visual">
          <img :src="illustrations.travelMode" alt="旅行插画" class="hero-illustration">
        </div>
      </div>
    </section>

    <section class="story-section">
      <div class="section-heading">
        <p class="eyebrow">三段式故事</p>
        <h2>规划、执行、沉淀，对应一条连续的产品体验。</h2>
      </div>

      <div class="story-grid">
        <article v-for="(step, index) in workflowSteps" :key="step.key" class="story-card">
          <div v-if="storyPhotos[index]" class="story-media">
            <img :src="photoSrc(storyPhotos[index])" :alt="step.title">
            <div class="story-credit">
              <PexelsCredit :image="storyPhotos[index]" compact />
            </div>
          </div>
          <div class="story-index">{{ step.index }}</div>
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
          <ul>
            <li v-for="point in step.points" :key="point">{{ point }}</li>
          </ul>
        </article>
      </div>
    </section>

    <section class="proof-section">
      <div class="proof-copy">
        <div class="section-heading">
          <p class="eyebrow">产品设计</p>
          <h2>不是堆更多页面，而是让每一步都更顺滑。</h2>
        </div>

        <div class="proof-grid">
          <div class="proof-card">
            <strong>产品结构</strong>
            <p>一级导航只保留首页、规划行程、我的行程、灵感广场和我的。</p>
          </div>
          <div class="proof-card">
            <strong>工程可靠</strong>
            <p>去掉伪数据、死路由和演示按钮，让每个入口都能真实执行。</p>
          </div>
          <div class="proof-card">
            <strong>内容沉淀</strong>
            <p>AI 不是单点聊天，而是贯穿行程生成、回忆图和内容沉淀全链路。</p>
          </div>
        </div>
      </div>

      <div class="proof-visual">
        <img :src="illustrations.aircraft" alt="aircraft illustration">
        <p>视觉上同时交代“真实旅行”和“数字工作流”，让整体体验更完整。</p>
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
const storyPhotos = ref<API.ContentImageVO[]>([])

const workflowSteps = [
  {
    key: 'plan',
    index: '规划',
    title: 'AI 生成候选方案',
    desc: '先用对话澄清需求，再生成结构化候选行程，支持 DIY 编辑后保存。',
    points: ['AI 助手澄清需求', '候选方案多版本生成', '保存为可执行行程'],
  },
  {
    key: 'execute',
    index: '执行',
    title: '围绕真实行程继续推进',
    desc: '行程详情页继续承接照片上传、状态推进和回忆图生成，不再把能力拆散。',
    points: ['行程状态统一', '照片集中管理', '回忆图与历史版本'],
  },
  {
    key: 'publish',
    index: '沉淀',
    title: '一键沉淀为灵感内容',
    desc: '用回忆图作为封面，生成可展示的游记/攻略，让旅程留下可分享的内容。',
    points: ['发布前校验回忆图', '自动归档到灵感广场', '形成可复用内容资产'],
  },
]

function photoSrc(image?: API.ContentImageVO | null) {
  return image?.landscapeUrl || image?.large2xUrl || image?.largeUrl || image?.mediumUrl || ''
}

async function loadVisuals() {
  const [hero, story] = await Promise.all([
    fetchCollection('travel planning desk', 2),
    fetchCollection('travel destination landscape', 3),
  ])
  heroPhotos.value = hero
  storyPhotos.value = story
}

function enterPrimary() {
  router.push(isLoggedIn.value ? '/workspace' : '/user/login')
}

function goInspiration() {
  router.push('/inspiration')
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

.hero-illustration {
  max-width: 100%;
  height: auto;
  max-height: 400px;
}

.story-section,
.proof-section {
  margin-top: 32px;
  padding: 30px;
  border-radius: 30px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(18, 52, 97, 0.06);
}

.section-heading {
  margin-bottom: 22px;

  h2 {
    margin: 8px 0 0;
    font-size: 32px;
    color: var(--color-text);
  }
}

.story-grid,
.proof-grid {
  display: grid;
  gap: 18px;
}

.story-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.story-card,
.proof-card {
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #f8fbff;
}

.story-media {
  position: relative;
  height: 180px;
  margin: -22px -22px 16px;
  overflow: hidden;
  border-radius: 24px 24px 18px 18px;
}

.story-credit {
  position: absolute;
  left: 14px;
  bottom: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
}

.story-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(59, 110, 220, 0.12);
  color: var(--primary-700);
  font-size: 12px;
  font-weight: 800;
}

.story-card h3,
.proof-card strong {
  display: block;
  margin: 16px 0 10px;
  font-size: 20px;
  color: var(--color-text);
}

.story-card p,
.proof-card p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.story-card ul {
  margin: 16px 0 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
  display: grid;
  gap: 8px;
}

.proof-section {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(280px, 0.9fr);
  gap: 24px;
  align-items: stretch;
}

.proof-copy {
  min-width: 0;
}

.proof-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.proof-visual {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #eef4ff 0%, #fffaf2 100%);
  border: 1px solid rgba(15, 28, 46, 0.08);

  img {
    width: 100%;
    max-width: 320px;
    display: block;
    margin: 0 auto 10px;
  }

  p {
    margin: 0;
    color: var(--color-muted);
    text-align: center;
    line-height: 1.7;
  }
}

@media (max-width: 1200px) {
  .hero,
  .proof-section,
  .story-grid,
  .proof-grid {
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

  .section-heading h2 {
    font-size: 26px;
  }

  .story-media {
    margin-left: -18px;
    margin-right: -18px;
  }
}
</style>
