<template>
  <div v-if="image && (image.photographer || image.pexelsUrl)" class="credit" :class="{ compact }">
    <span>Photo</span>
    <template v-if="image.photographer">
      <span>by</span>
      <a
        v-if="image.photographerUrl"
        :href="image.photographerUrl"
        target="_blank"
        rel="noreferrer"
      >
        {{ image.photographer }}
      </a>
      <span v-else>{{ image.photographer }}</span>
    </template>
    <template v-if="image.pexelsUrl">
      <span>on</span>
      <a :href="image.pexelsUrl" target="_blank" rel="noreferrer">Pexels</a>
    </template>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  image?: API.ContentImageVO | null
  compact?: boolean
}>()
</script>

<style scoped lang="scss">
.credit {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);

  a {
    color: inherit;
    text-decoration: underline;
    text-underline-offset: 2px;
  }

  &.compact {
    color: var(--color-subtle);
  }
}
</style>
