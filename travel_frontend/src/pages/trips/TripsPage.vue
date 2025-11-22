<template>
  <div class="page">
    <h2>我的行程</h2>
    <div class="list">
      <div v-for="t in trips" :key="t.id" class="item" @click="goDetail(t.id)">
        <div class="row">
          <div class="title">{{ t.destination }} · {{ t.days }}天</div>
          <span class="status">{{ t.status }}</span>
        </div>
        <div class="meta">{{ t.theme || '通用主题' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMyTrips } from '@/api/tripController'
import { useRouter } from 'vue-router'

const router = useRouter()
const trips = ref<any[]>([])

onMounted(load)
async function load() {
  try {
    const resp = await getMyTrips()
    trips.value = resp?.data?.data || []
  } catch (e) {
    alert('加载行程失败')
  }
}
function goDetail(id: number) {
  router.push(`/trips/${id}`)
}
</script>

<style scoped>
.page { max-width: 800px; margin: 0 auto; padding: 16px; }
.list { display: grid; gap: 12px; }
.item { border: 1px solid var(--color-border); border-radius: 8px; padding: 12px; cursor: pointer; background: var(--color-bg-secondary); transition: background-color 0.15s ease, border-color 0.15s ease; }
.row { display: flex; align-items: center; justify-content: space-between; }
.title { font-weight: 600; color: var(--color-text); }
.status { font-size: 12px; color: var(--color-muted); }
.meta { color: var(--color-text-secondary); margin-top: 6px; }
.item:hover { background: var(--color-bg); border-color: var(--color-border-strong); }
</style>


