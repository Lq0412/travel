<script setup lang="ts">
const capabilityCards = [
  {
    title: '智能生成行程',
    text: '一句需求，生成每天的节奏、景点组合和旅行重点。',
  },
  {
    title: '地图联动路线',
    text: '把时间轴和地图位置联在一起，路线顺不顺一眼可见。',
  },
  {
    title: '预算与交通建议',
    text: '兼顾成本、体验和移动效率，减少临时决策压力。',
  },
] as const
</script>

<template>
  <section id="home-inspiration" class="home-capability-strip">
    <article
      v-for="card in capabilityCards"
      :key="card.title"
      class="home-capability-strip__card"
    >
      <span class="home-capability-strip__eyebrow">AI Capability</span>
      <h2 class="home-capability-strip__title">{{ card.title }}</h2>
      <p class="home-capability-strip__text">{{ card.text }}</p>
    </article>
  </section>
</template>

<style scoped lang="scss">
.home-capability-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  max-width: 1200px;
  width: 100%;
  margin: -40px auto 0;
  padding: 24px 32px 0;
  position: relative;
  z-index: 10;
}

.home-capability-strip__card {
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 24px 55px rgba(15, 23, 42, 0.1);
}

.home-capability-strip__eyebrow {
  display: inline-flex;
  margin-bottom: 14px;
  color: var(--primary-600);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.home-capability-strip__title {
  margin-bottom: 10px;
  color: var(--color-text);
  font-size: 24px;
}

.home-capability-strip__text {
  color: var(--color-text-secondary);
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .home-capability-strip {
    grid-template-columns: 1fr;
    padding: 24px 16px 0;
    margin-top: 0;
  }
}
</style>
