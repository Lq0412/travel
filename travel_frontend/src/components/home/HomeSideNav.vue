<script setup lang="ts">
import { RouterLink } from 'vue-router'

interface HomeNavItem {
  key: string
  label: string
  icon: string
  to?: string
  href?: string
  active?: boolean
}

defineProps<{
  items: HomeNavItem[]
}>()
</script>

<template>
  <aside class="home-side-nav" aria-label="首页导航">
    <component
      :is="item.to ? RouterLink : 'a'"
      v-for="item in items"
      :key="item.key"
      class="home-side-nav__item"
      :class="{ 'is-active': item.active }"
      :to="item.to"
      :href="item.href"
    >
      <span class="home-side-nav__icon" aria-hidden="true">{{ item.icon }}</span>
      <span class="home-side-nav__label">{{ item.label }}</span>
    </component>
  </aside>
</template>

<style scoped lang="scss">
.home-side-nav {
  position: fixed;
  top: 144px;
  left: 24px;
  z-index: 12;
  display: grid;
  gap: 12px;
  padding: 14px 10px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
}

.home-side-nav__item {
  display: grid;
  justify-items: center;
  gap: 6px;
  min-width: 64px;
  padding: 10px 8px;
  color: rgba(255, 255, 255, 0.76);
  border-radius: 18px;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.home-side-nav__item:hover,
.home-side-nav__item.is-active {
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  transform: translateY(-1px);
}

.home-side-nav__icon {
  font-size: 20px;
  line-height: 1;
}

.home-side-nav__label {
  font-size: 13px;
  letter-spacing: 0.04em;
}

@media (max-width: 900px) {
  .home-side-nav {
    left: 12px;
    right: 12px;
    top: auto;
    bottom: 14px;
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
  }

  .home-side-nav__item {
    min-width: 0;
  }
}
</style>
