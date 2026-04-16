<script setup lang="ts">
const featuredCases = [
  {
    title: '东京 3 天城市漫游',
    tag: '城市漫游',
    summary: '把街区密度、夜晚节奏和设计感路线压缩进一个周末。',
  },
  {
    title: '巴黎 5 天浪漫假期',
    tag: '艺术假期',
    summary: '从晨间咖啡到博物馆动线，适合比赛展示的高完成度案例。',
  },
  {
    title: '冰岛 7 天环岛自驾',
    tag: '公路旅行',
    summary: '展示多日行程、跨城市路线和天气节奏规划能力。',
  },
] as const
</script>

<template>
  <section id="home-cases" class="home-cases-section">
    <header class="home-cases-section__header">
      <p class="home-cases-section__eyebrow">Featured Journeys</p>
      <h2 class="home-cases-section__title">向下看真实旅程如何被展开</h2>
      <p class="home-cases-section__description">
        首页不是一张海报，而是一个能延展到规划、工作台和结果页的完整作品入口。
      </p>
    </header>

    <div class="home-cases-section__grid">
      <article v-for="item in featuredCases" :key="item.title" class="home-cases-section__card">
        <span class="home-cases-section__tag">{{ item.tag }}</span>
        <h3 class="home-cases-section__card-title">{{ item.title }}</h3>
        <p class="home-cases-section__summary">{{ item.summary }}</p>
      </article>
    </div>

    <router-link class="home-cases-section__cta" to="/ai/planner">立即开始规划</router-link>
  </section>
</template>

<style scoped lang="scss">
.home-cases-section {
  display: grid;
  gap: 28px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 64px 32px 96px;
  color: #ffffff;
}

.home-cases-section__header {
  max-width: 720px;
}

.home-cases-section__eyebrow {
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.home-cases-section__title {
  margin-bottom: 14px;
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1.08;
}

.home-cases-section__description {
  max-width: 640px;
  color: rgba(255, 255, 255, 0.76);
  line-height: 1.8;
}

.home-cases-section__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.home-cases-section__card {
  min-height: 280px;
  padding: 28px;
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08)),
    radial-gradient(circle at top left, rgba(125, 211, 252, 0.25), transparent 46%),
    rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(18px);
}

.home-cases-section__tag {
  display: inline-flex;
  margin-bottom: 16px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.home-cases-section__card-title {
  margin-bottom: 10px;
  font-size: 28px;
  line-height: 1.2;
}

.home-cases-section__summary {
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.8;
}

.home-cases-section__cta {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: fit-content;
  min-width: 180px;
  min-height: 54px;
  padding: 0 24px;
  border-radius: 999px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

@media (max-width: 1100px) {
  .home-cases-section {
    padding: 56px 16px 120px;
  }

  .home-cases-section__grid {
    grid-template-columns: 1fr;
  }
}
</style>
