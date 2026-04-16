<script setup lang="ts">
import { useRouter } from 'vue-router'
import HomeHero from '@/components/home/HomeHero.vue'
import { useHomeDestinations } from '@/composables/useHomeDestinations'

const router = useRouter()
const { destinations, activeDestination, setActiveDestination } = useHomeDestinations()

function handleStartPlanning(prompt: string) {
  router.push({
    path: '/workspace',
    query: prompt ? { prompt } : undefined,
  })
}
</script>

<template>
  <div class="home-page">
    <main class="home-page__main">
      <HomeHero
        :destinations="destinations"
        :active-destination="activeDestination"
        @select-destination="setActiveDestination"
        @start-planning="handleStartPlanning"
      />
    </main>
  </div>
</template>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: #0f172a;
}

.home-page__main {
  display: flex;
  flex-direction: column;
  gap: 0;
}
</style>
