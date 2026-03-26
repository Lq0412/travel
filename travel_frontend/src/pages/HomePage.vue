<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">AI 旅行工作台</p>
        <h1>把 AI 旅行做成一条完整闭环，而不是功能拼盘。</h1>
        <p class="intro">
          从需求输入、候选行程生成、DIY 编辑，到照片沉淀、回忆图生成和游记发布，
          让一次旅行从灵感出现到内容沉淀都能在同一个产品里完成。
        </p>
        <div class="hero-actions">
          <a-button type="primary" size="large" @click="enterPrimary">
            {{ isLoggedIn ? '进入旅行工作台' : '登录后开始规划' }}
          </a-button>
          <a-button size="large" @click="goInspiration">查看灵感广场</a-button>
        </div>
      </div>

      <div class="hero-visuals">
        <article class="hero-illustration hero-illustration-stage">
          <span class="illustration-halo illustration-halo-one"></span>
          <span class="illustration-halo illustration-halo-two"></span>
          <img :src="illustrations.travelMode" alt="travel illustration">
          <div class="illustration-copy">
            <strong>AI 与旅行场景结合</strong>
            <p>用插画解释工作流，用真实照片补足旅途温度。</p>
          </div>
        </article>

        <article v-if="heroPhotos[0]" class="floating-photo floating-photo-primary">
          <img :src="photoSrc(heroPhotos[0])" :alt="heroPhotos[0].alt || '旅行视觉图'">
          <div class="floating-photo-caption">
            <strong>真实旅途氛围</strong>
            <PexelsCredit :image="heroPhotos[0]" compact />
          </div>
        </article>

        <article v-if="heroPhotos[1]" class="floating-photo floating-photo-secondary">
          <img :src="photoSrc(heroPhotos[1])" :alt="heroPhotos[1].alt || '规划场景图'">
          <div class="floating-photo-caption">
            <strong>规划现场</strong>
            <PexelsCredit :image="heroPhotos[1]" compact />
          </div>
        </article>
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
            <p>一级导航只保留首页、工作台、我的行程、灵感广场和我的。</p>
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
    desc: '用回忆图作为封面，生成可展示的游记/攻略，让作品具备完整闭环结果。',
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
  display: grid;
  grid-template-columns: minmax(0, 0.96fr) minmax(420px, 1.04fr);
  gap: 28px;
  align-items: stretch;
  padding: 38px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top left, rgba(47, 144, 240, 0.18), transparent 32%),
    radial-gradient(circle at bottom right, rgba(212, 139, 31, 0.12), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f4f8ff 58%, #fff8ee 100%);
  box-shadow: 0 18px 44px rgba(18, 52, 97, 0.05);

  h1 {
    margin: 8px 0 16px;
    font-size: 56px;
    line-height: 1.05;
    color: var(--color-text);
  }
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.hero-visuals {
  position: relative;
  min-height: 500px;
  padding: 0;
}

.floating-photo,
.hero-illustration {
  position: relative;
  border-radius: 26px;
}

.floating-photo {
  overflow: hidden;
  background: #ffffff;
  box-shadow:
    0 18px 42px rgba(18, 52, 97, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.72);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.story-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.hero-illustration-stage {
  z-index: 1;
  min-height: 500px;
  padding: 42px 36px 34px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.18) 36%, transparent 62%),
    radial-gradient(circle at bottom right, rgba(255, 244, 226, 0.88), rgba(255, 244, 226, 0.08) 34%, transparent 60%),
    linear-gradient(145deg, rgba(238, 244, 255, 0.96) 0%, rgba(255, 249, 239, 0.86) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);

  img {
    position: relative;
    z-index: 2;
    width: 100%;
    max-width: 410px;
    height: auto;
  }
}

.illustration-copy {
  position: relative;
  z-index: 2;
  text-align: center;

  strong {
    display: block;
    margin-bottom: 8px;
    font-size: 28px;
    color: var(--color-text);
  }

  p {
    max-width: 360px;
    margin: 0;
    color: var(--color-muted);
    font-size: 16px;
    line-height: 1.7;
  }
}

.illustration-halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(4px);
  opacity: 0.85;
}

.illustration-halo-one {
  top: 34px;
  left: 34px;
  width: 170px;
  height: 170px;
  background: radial-gradient(circle, rgba(47, 144, 240, 0.24) 0%, transparent 72%);
}

.illustration-halo-two {
  right: 30px;
  bottom: 24px;
  width: 210px;
  height: 210px;
  background: radial-gradient(circle, rgba(212, 139, 31, 0.2) 0%, transparent 72%);
}

.floating-photo {
  position: absolute;
  z-index: 3;
}

.floating-photo-primary {
  left: 10px;
  bottom: 14px;
  width: 240px;
  height: 164px;
  transform: translate(-6px, 8px) rotate(-4deg);
}

.floating-photo-secondary {
  top: 14px;
  right: 10px;
  width: 200px;
  height: 274px;
  transform: translate(6px, -4px) rotate(5deg);
}

.floating-photo-caption {
  position: absolute;
  inset: auto 0 0 0;
  padding: 14px;
  background: linear-gradient(180deg, rgba(15, 28, 46, 0.08) 0%, rgba(15, 28, 46, 0.82) 100%);

  strong {
    display: block;
    margin-bottom: 8px;
    color: #ffffff;
    font-size: 16px;
  }
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--primary-600);
}

.intro {
  max-width: 720px;
  margin: 0;
  font-size: 17px;
  color: var(--color-text-secondary);
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 28px;
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

  .hero-visuals {
    min-height: 0;
    padding: 0;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 20px 12px 48px;
  }

  .hero,
  .story-section,
  .proof-section {
    padding: 22px 18px;
  }

  .hero h1 {
    font-size: 38px;
  }

  .section-heading h2 {
    font-size: 26px;
  }

  .hero-visuals {
    display: grid;
    gap: 14px;
    min-height: 0;
    padding: 0;
  }

  .hero-illustration-stage {
    min-height: 320px;
    padding: 30px 20px 24px;

    img {
      max-width: 240px;
    }
  }

  .illustration-copy strong {
    font-size: 22px;
  }

  .floating-photo {
    position: static;
    width: 100%;
    height: 190px;
    transform: none;
  }

  .story-media {
    margin-left: -18px;
    margin-right: -18px;
  }
}
</style>
