<script setup lang="ts">
import { computed, shallowRef } from 'vue'
import type { HomeDestination } from '@/composables/useHomeDestinations'

const props = defineProps<{
  destinations: readonly HomeDestination[]
  activeDestination: HomeDestination
}>()

const emit = defineEmits<{
  selectDestination: [id: HomeDestination['id']]
  startPlanning: [prompt: string]
}>()

const draftPrompt = shallowRef('')

const heroStyle = computed(() => ({
  backgroundImage: `url(https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80)`,
  backgroundSize: 'cover',
  backgroundPosition: 'center',
}))

const suggestions = [
  '潮汕 2 天美食打卡',
  '汕头周末轻松游',
  '潮州古城一日慢逛',
  '南澳岛看海放空 2 天',
]

function usePrompt(prompt: string) {
  draftPrompt.value = prompt
  submitPlanning()
}

function submitPlanning() {
  if (draftPrompt.value.trim()) {
    emit('startPlanning', draftPrompt.value.trim())
  }
}
</script>

<template>
  <section id="home-hero" class="home-hero" :style="heroStyle">
    <div class="home-hero__content">
      <h1 class="home-hero__title">AI 旅行规划，从一句话开始</h1>
      <p class="home-hero__subtitle">
        告诉我们你想去哪里、想怎么玩，我们会为你生成专属行程方案
      </p>

      <div class="home-hero__input-shell">
        <input
          v-model="draftPrompt"
          class="home-hero__input"
          type="text"
          placeholder="输入你的旅行想法，例如：潮汕 2 天想吃牛肉火锅和逛老街"
          @keydown.enter="submitPlanning"
        />
        <button class="home-hero__button" type="button" @click="submitPlanning" title="开始规划">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L13.8 9.2L21 11L13.8 12.8L12 20L10.2 12.8L3 11L10.2 9.2L12 2Z" fill="#CFA061"/>
          </svg>
        </button>
      </div>

      <div class="home-hero__chips" aria-label="推荐提示词">
        <button
          v-for="dest in suggestions"
          :key="dest"
          class="home-hero__chip"
          type="button"
          @click="usePrompt(dest)"
        >
          {{ dest }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.home-hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  overflow: hidden;
}

.home-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.2);
  pointer-events: none;
}

.home-hero__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 24px;
  max-width: 860px;
  width: 100%;
}

.home-hero__title {
  font-size: clamp(36px, 5vw, 64px);
  font-weight: 600;
  color: #ffffff;
  line-height: 1.1;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  margin: 0;
}

.home-hero__subtitle {
  font-size: clamp(16px, 2.5vw, 22px);
  color: #ffffff;
  opacity: 0.95;
  margin-bottom: 24px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.home-hero__input-shell {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 720px;
  padding: 8px 12px 8px 32px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), inset 0 0 0 1px rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.home-hero__input-shell:focus-within {
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15), inset 0 0 0 1px rgba(255, 255, 255, 0.3);
}

.home-hero__input {
  flex: 1;
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  background: transparent;
  border: 0;
  outline: none;
  min-width: 0;
}

.home-hero__input::placeholder {
  color: rgba(51, 51, 51, 0.8);
}

.home-hero__button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 0;
  border-radius: 50%;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 4px 12px rgba(207, 160, 97, 0.3);
  flex-shrink: 0;
  
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 8px 24px rgba(207, 160, 97, 0.4);
  }

  svg {
    width: 28px;
    height: 28px;
  }
}

.home-hero__chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.home-hero__chip {
  padding: 12px 24px;
  border-radius: 999px;
  color: #333333;
  font-weight: 500;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  
  &:hover {
    background: rgba(255, 255, 255, 0.7);
    transform: translateY(-1px);
  }
}

@media (max-width: 768px) {
  .home-hero {
    padding: 100px 20px;
  }

  .home-hero__input-shell {
    padding: 6px 8px 6px 24px;
  }

  .home-hero__input {
    font-size: 16px;
  }

  .home-hero__button {
    width: 48px;
    height: 48px;
  }

  .home-hero__chip {
    padding: 10px 20px;
    font-size: 14px;
  }
}
</style>
