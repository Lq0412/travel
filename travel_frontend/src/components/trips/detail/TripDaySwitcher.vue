<script setup lang="ts">
const props = defineProps<{
  days: number[]
  selectedDay: number | 'all'
}>()

const emit = defineEmits<{
  select: [value: number | 'all']
}>()

function selectDay(value: number | 'all') {
  if (value === props.selectedDay) {
    return
  }
  emit('select', value)
}
</script>

<template>
  <section v-if="days.length" class="day-switcher">
    <button
      type="button"
      class="day-chip"
      :class="{ active: selectedDay === 'all' }"
      @click="selectDay('all')"
    >
      全部行程
    </button>
    <button
      v-for="day in days"
      :key="day"
      type="button"
      class="day-chip"
      :class="{ active: selectedDay === day }"
      @click="selectDay(day)"
    >
      Day {{ day }}
    </button>
  </section>
</template>

<style scoped lang="scss">
.day-switcher {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.day-chip {
  flex: 0 0 auto;
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &.active {
    border-color: rgba(22, 119, 255, 0.28);
    background: #eaf3ff;
    color: var(--primary-600);
    font-weight: 600;
  }
}
</style>
