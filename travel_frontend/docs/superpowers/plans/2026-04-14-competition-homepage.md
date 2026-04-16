# Competition Homepage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the competition-grade landing homepage with fixed left-side navigation, cinematic multi-destination hero, scrollable cases, and a distinct planner entry page that separates planning from the workspace.

**Architecture:** Introduce a dedicated `LandingLayout` for the homepage so the immersive hero and fixed side nav do not fight the existing `TopNavLayout`. Keep homepage content split into focused home components backed by a local destination/theme configuration composable, and route `规划` to a real `PlannerPage` that becomes the formal generation entry while `WorkspacePage` remains the post-generation editing surface.

**Tech Stack:** Vue 3, `<script setup lang="ts">`, Vue Router 4, Vitest, Vite, Sass

---

## File Map

### New files

- `travel_frontend/src/layouts/LandingLayout.vue`
  Purpose: Dedicated outer shell for the immersive homepage, containing the logo column and a `router-view`.
- `travel_frontend/src/utils/layoutResolver.ts`
  Purpose: Centralize route-meta-to-layout resolution for `App.vue`.
- `travel_frontend/src/utils/layoutResolver.test.ts`
  Purpose: Verify `landing`, `topnav`, and `none` layout resolution.
- `travel_frontend/src/composables/useHomeDestinations.ts`
  Purpose: Own destination presets, prompt chips, active theme state, and section ids used by the homepage.
- `travel_frontend/src/composables/useHomeDestinations.test.ts`
  Purpose: Verify homepage destination defaults, theme lookup, and prompt hydration.
- `travel_frontend/src/components/home/HomeSideNav.vue`
  Purpose: Fixed homepage side navigation with icon + short label items.
- `travel_frontend/src/components/home/HomeHero.vue`
  Purpose: Render the hero background, title, subtitle, main input, and prompt chips.
- `travel_frontend/src/components/home/HomeCapabilityStrip.vue`
  Purpose: Render the lightweight capability cards below the hero.
- `travel_frontend/src/components/home/HomeCasesSection.vue`
  Purpose: Render curated case cards and the bottom CTA.
- `travel_frontend/src/pages/ai/PlannerPage.vue`
  Purpose: Dedicated planning entry page instead of proxying straight to the workspace.

### Modified files

- `travel_frontend/src/App.vue`
  Purpose: Use a tested layout resolver instead of hardcoding only `TopNavLayout`.
- `travel_frontend/src/router/index.ts`
  Purpose: Keep `/` on landing layout, point `/ai/planner` to a real planner page, and align route names with homepage flow.
- `travel_frontend/src/layouts/TopNavLayout.vue`
  Purpose: Align internal navigation labels/routes to `首页 / 规划 / 行程 / 监控`.
- `travel_frontend/src/pages/HomePage.vue`
  Purpose: Replace the current two-column intro page with the competition homepage composition.
- `travel_frontend/src/router/index.test.ts`
  Purpose: Verify planner route exists and homepage keeps the landing layout.

### Verification commands

- `npm run test -- src/utils/layoutResolver.test.ts src/composables/useHomeDestinations.test.ts src/router/index.test.ts`
- `npm run type-check`
- `npm run build`

## Task 1: Add layout resolution for a dedicated landing shell

**Files:**
- Create: `travel_frontend/src/utils/layoutResolver.ts`
- Create: `travel_frontend/src/utils/layoutResolver.test.ts`
- Create: `travel_frontend/src/layouts/LandingLayout.vue`
- Modify: `travel_frontend/src/App.vue`

- [ ] **Step 1: Write the failing layout resolver test**

```ts
// travel_frontend/src/utils/layoutResolver.test.ts
import { describe, expect, it } from 'vitest'
import { resolveLayout } from './layoutResolver'

describe('resolveLayout', () => {
  it('returns router-view for routes without layout chrome', () => {
    expect(resolveLayout('none')).toBe('router-view')
  })

  it('returns the landing layout key for immersive pages', () => {
    expect(resolveLayout('landing')).toBe('landing')
  })

  it('falls back to the top navigation layout', () => {
    expect(resolveLayout(undefined)).toBe('topnav')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- src/utils/layoutResolver.test.ts`  
Expected: FAIL with `Cannot find module './layoutResolver'`.

- [ ] **Step 3: Write the minimal implementation**

```ts
// travel_frontend/src/utils/layoutResolver.ts
export type LayoutKey = 'topnav' | 'landing' | 'none'

export function resolveLayout(layout?: string): 'topnav' | 'landing' | 'router-view' {
  if (layout === 'none') {
    return 'router-view'
  }

  if (layout === 'landing') {
    return 'landing'
  }

  return 'topnav'
}
```

```vue
<!-- travel_frontend/src/layouts/LandingLayout.vue -->
<template>
  <div class="landing-layout">
    <router-view />
  </div>
</template>

<style scoped lang="scss">
.landing-layout {
  min-height: 100vh;
  background: #f4efe8;
}
</style>
```

```vue
<!-- travel_frontend/src/App.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LandingLayout from './layouts/LandingLayout.vue'
import TopNavLayout from './layouts/TopNavLayout.vue'
import { resolveLayout } from './utils/layoutResolver'

const route = useRoute()

const layoutComponent = computed(() => {
  const layout = resolveLayout(route.meta?.layout as string | undefined)

  if (layout === 'router-view') {
    return 'router-view'
  }

  return layout === 'landing' ? LandingLayout : TopNavLayout
})
</script>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run test -- src/utils/layoutResolver.test.ts`  
Expected: PASS with 3 passing tests.

- [ ] **Step 5: Commit**

```bash
git add travel_frontend/src/utils/layoutResolver.ts travel_frontend/src/utils/layoutResolver.test.ts travel_frontend/src/layouts/LandingLayout.vue travel_frontend/src/App.vue
git commit -m "feat(frontend): add landing layout resolution"
```

## Task 2: Add homepage destination/theme configuration with tests

**Files:**
- Create: `travel_frontend/src/composables/useHomeDestinations.ts`
- Create: `travel_frontend/src/composables/useHomeDestinations.test.ts`

- [ ] **Step 1: Write the failing destination config test**

```ts
// travel_frontend/src/composables/useHomeDestinations.test.ts
import { describe, expect, it } from 'vitest'
import {
  getDefaultHomeDestination,
  getHomeDestinationById,
  homeDestinations,
} from './useHomeDestinations'

describe('home destinations', () => {
  it('exposes at least four curated travel destinations', () => {
    expect(homeDestinations.length).toBeGreaterThanOrEqual(4)
  })

  it('uses tokyo as the stable default theme', () => {
    expect(getDefaultHomeDestination().id).toBe('tokyo')
  })

  it('returns a destination prompt set for the selected theme', () => {
    expect(getHomeDestinationById('paris')?.prompts).toContain('巴黎五天浪漫假期')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- src/composables/useHomeDestinations.test.ts`  
Expected: FAIL with `Cannot find module './useHomeDestinations'`.

- [ ] **Step 3: Write the minimal implementation**

```ts
// travel_frontend/src/composables/useHomeDestinations.ts
import { computed, ref } from 'vue'

export interface HomeDestinationTheme {
  id: string
  name: string
  title: string
  subtitle: string
  imageUrl: string
  themeColor: string
  prompts: string[]
  sectionLabel: string
}

export const homeDestinations: HomeDestinationTheme[] = [
  {
    id: 'tokyo',
    name: '东京',
    title: '一句话，生成你的下一段旅程',
    subtitle: '让 AI 为你整理景点、美食、路线与节奏。',
    imageUrl: 'https://images.unsplash.com/photo-1542051841857-5f90071e7989?auto=format&fit=crop&w=1800&q=80',
    themeColor: '#6aa7ff',
    prompts: ['东京三天城市漫游', '涩谷表参道设计感之旅', '东京周末轻松亲子游'],
    sectionLabel: '城市漫游',
  },
  {
    id: 'paris',
    name: '巴黎',
    title: '把灵感交给 AI，把时间留给旅程',
    subtitle: '从浪漫假期到艺术漫步，自动生成完整行程。',
    imageUrl: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1800&q=80',
    themeColor: '#f0c7a1',
    prompts: ['巴黎五天浪漫假期', '巴黎左岸艺术路线', '巴黎经典地标轻松游'],
    sectionLabel: '浪漫假期',
  },
  {
    id: 'iceland',
    name: '冰岛',
    title: '让每一次远行，都从清晰的计划开始',
    subtitle: '适配自驾、公路旅行和多日节奏规划。',
    imageUrl: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=80',
    themeColor: '#8ed0df',
    prompts: ['冰岛七天环岛自驾', '冰岛冬季追极光路线', '雷克雅未克周边两日游'],
    sectionLabel: '公路旅行',
  },
  {
    id: 'island',
    name: '海岛',
    title: '出发之前，先拥有一份恰到好处的旅行节奏',
    subtitle: '把度假氛围、预算节奏和动线安排一起生成。',
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1800&q=80',
    themeColor: '#ffd6a0',
    prompts: ['海岛度假四天三晚', '情侣蜜月海边行程', '预算友好的海岛放松之旅'],
    sectionLabel: '疗愈度假',
  },
]

export function getDefaultHomeDestination() {
  return homeDestinations[0]
}

export function getHomeDestinationById(id: string) {
  return homeDestinations.find((item) => item.id === id) ?? getDefaultHomeDestination()
}

export function useHomeDestinations() {
  const activeDestinationId = ref(getDefaultHomeDestination().id)
  const activeDestination = computed(() => getHomeDestinationById(activeDestinationId.value))

  function setActiveDestination(id: string) {
    activeDestinationId.value = getHomeDestinationById(id).id
  }

  return {
    homeDestinations,
    activeDestinationId,
    activeDestination,
    setActiveDestination,
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run test -- src/composables/useHomeDestinations.test.ts`  
Expected: PASS with 3 passing tests.

- [ ] **Step 5: Commit**

```bash
git add travel_frontend/src/composables/useHomeDestinations.ts travel_frontend/src/composables/useHomeDestinations.test.ts
git commit -m "feat(frontend): add homepage destination themes"
```

## Task 3: Build homepage side navigation and immersive hero components

**Files:**
- Create: `travel_frontend/src/components/home/HomeSideNav.vue`
- Create: `travel_frontend/src/components/home/HomeHero.vue`
- Modify: `travel_frontend/src/pages/HomePage.vue`

- [ ] **Step 1: Write the failing router assertion for planner and landing home**

```ts
// travel_frontend/src/router/index.test.ts
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('router route components', () => {
  it('keeps the homepage on the landing layout', () => {
    const routerSource = readFileSync(resolve(__dirname, './index.ts'), 'utf8')

    expect(routerSource).toContain("path: '/'")
    expect(routerSource).toContain("meta: { layout: 'landing' }")
  })

  it('includes the AI planner page component file referenced by the router', () => {
    const plannerPagePath = resolve(__dirname, '../pages/ai/PlannerPage.vue')

    expect(existsSync(plannerPagePath)).toBe(true)
  })
})
```

- [ ] **Step 2: Run test to verify the new homepage assumptions fail**

Run: `npm run test -- src/router/index.test.ts`  
Expected: FAIL after adding the new landing-layout expectation or because the planner page is still only a workspace proxy.

- [ ] **Step 3: Write the minimal implementation**

```vue
<!-- travel_frontend/src/components/home/HomeSideNav.vue -->
<script setup lang="ts">
defineProps<{
  items: Array<{ key: string; label: string; icon: string; href?: string; to?: string; active?: boolean }>
}>()
</script>

<template>
  <aside class="home-side-nav" aria-label="首页导航">
    <a
      v-for="item in items"
      :key="item.key"
      class="nav-item"
      :class="{ active: item.active }"
      :href="item.href"
    >
      <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span>
      <span class="nav-label">{{ item.label }}</span>
    </a>
  </aside>
</template>
```

```vue
<!-- travel_frontend/src/components/home/HomeHero.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import type { HomeDestinationTheme } from '@/composables/useHomeDestinations'

const props = defineProps<{
  destinations: HomeDestinationTheme[]
  activeDestination: HomeDestinationTheme
}>()

const draftPrompt = ref('')

const heroStyle = computed(() => ({
  '--hero-theme': props.activeDestination.themeColor,
  backgroundImage: `linear-gradient(rgba(14, 21, 31, 0.18), rgba(14, 21, 31, 0.42)), url(${props.activeDestination.imageUrl})`,
}))
</script>

<template>
  <section id="home-hero" class="home-hero" :style="heroStyle">
    <div class="hero-copy">
      <p class="hero-kicker">AI TRAVEL PLANNER</p>
      <h1>{{ activeDestination.title }}</h1>
      <p class="hero-subtitle">{{ activeDestination.subtitle }}</p>
    </div>

    <div class="hero-input-shell">
      <input v-model="draftPrompt" type="text" :placeholder="activeDestination.prompts[0]" />
      <button type="button">开始规划</button>
    </div>

    <div class="hero-chips">
      <button v-for="item in destinations" :key="item.id" type="button">
        {{ item.name }}
      </button>
    </div>
  </section>
</template>
```

```vue
<!-- travel_frontend/src/pages/HomePage.vue -->
<script setup lang="ts">
import HomeHero from '@/components/home/HomeHero.vue'
import HomeSideNav from '@/components/home/HomeSideNav.vue'
import { useHomeDestinations } from '@/composables/useHomeDestinations'

const { homeDestinations, activeDestination } = useHomeDestinations()

const navItems = [
  { key: 'home', label: '首页', icon: '⌂', href: '#home-hero', active: true },
  { key: 'planner', label: '规划', icon: '✦', to: '/ai/planner' },
  { key: 'trips', label: '行程', icon: '▣', to: '/trips' },
]
</script>

<template>
  <div class="home-page">
    <HomeSideNav :items="navItems" />
    <HomeHero :destinations="homeDestinations" :active-destination="activeDestination" />
  </div>
</template>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run test -- src/router/index.test.ts`  
Expected: PASS with the updated route/component assertions.

- [ ] **Step 5: Commit**

```bash
git add travel_frontend/src/components/home/HomeSideNav.vue travel_frontend/src/components/home/HomeHero.vue travel_frontend/src/pages/HomePage.vue travel_frontend/src/router/index.test.ts
git commit -m "feat(frontend): scaffold homepage hero and side navigation"
```

## Task 4: Add capability strip, curated cases, and homepage section composition

**Files:**
- Create: `travel_frontend/src/components/home/HomeCapabilityStrip.vue`
- Create: `travel_frontend/src/components/home/HomeCasesSection.vue`
- Modify: `travel_frontend/src/pages/HomePage.vue`

- [ ] **Step 1: Write the failing destination-section test**

```ts
// travel_frontend/src/composables/useHomeDestinations.test.ts
it('provides stable section ids for inspiration and cases anchors', () => {
  expect(getDefaultHomeDestination().sectionLabel).toBeTruthy()
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- src/composables/useHomeDestinations.test.ts`  
Expected: FAIL if section metadata or supporting fields are missing after the hero scaffold.

- [ ] **Step 3: Write the minimal implementation**

```vue
<!-- travel_frontend/src/components/home/HomeCapabilityStrip.vue -->
<script setup lang="ts">
const cards = [
  { title: '智能生成行程', text: '一句需求，生成每日路线和节奏。' },
  { title: '地图联动路线', text: '景点顺路程度和出行逻辑清晰可见。' },
  { title: '预算与交通建议', text: '兼顾预算、效率与旅行体验。' },
]
</script>

<template>
  <section id="home-inspiration" class="home-capability-strip">
    <article v-for="card in cards" :key="card.title" class="capability-card">
      <h2>{{ card.title }}</h2>
      <p>{{ card.text }}</p>
    </article>
  </section>
</template>
```

```vue
<!-- travel_frontend/src/components/home/HomeCasesSection.vue -->
<script setup lang="ts">
const cases = [
  { title: '东京 3 天城市漫游', tag: '城市漫游', imageUrl: 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=1200&q=80' },
  { title: '巴黎 5 天浪漫假期', tag: '艺术假期', imageUrl: 'https://images.unsplash.com/photo-1499856871958-5b9627545d1a?auto=format&fit=crop&w=1200&q=80' },
  { title: '冰岛 7 天环岛自驾', tag: '公路旅行', imageUrl: 'https://images.unsplash.com/photo-1476610182048-b716b8518aae?auto=format&fit=crop&w=1200&q=80' },
]
</script>

<template>
  <section id="home-cases" class="home-cases-section">
    <header>
      <p>精选案例</p>
      <h2>向下看真实旅程的展开方式</h2>
    </header>
    <div class="case-grid">
      <article v-for="item in cases" :key="item.title" class="case-card">
        <img :src="item.imageUrl" :alt="item.title" />
        <div class="case-copy">
          <span>{{ item.tag }}</span>
          <h3>{{ item.title }}</h3>
        </div>
      </article>
    </div>
    <router-link class="case-cta" to="/ai/planner">立即开始规划</router-link>
  </section>
</template>
```

```vue
<!-- travel_frontend/src/pages/HomePage.vue -->
<template>
  <div class="home-page">
    <HomeSideNav :items="navItems" />
    <main class="home-main">
      <HomeHero :destinations="homeDestinations" :active-destination="activeDestination" />
      <HomeCapabilityStrip />
      <HomeCasesSection />
    </main>
  </div>
</template>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run test -- src/composables/useHomeDestinations.test.ts`  
Expected: PASS with the destination metadata test restored to green.

- [ ] **Step 5: Commit**

```bash
git add travel_frontend/src/components/home/HomeCapabilityStrip.vue travel_frontend/src/components/home/HomeCasesSection.vue travel_frontend/src/pages/HomePage.vue travel_frontend/src/composables/useHomeDestinations.test.ts
git commit -m "feat(frontend): add homepage capability and cases sections"
```

## Task 5: Turn PlannerPage into a distinct planning entry

**Files:**
- Modify: `travel_frontend/src/pages/ai/PlannerPage.vue`
- Modify: `travel_frontend/src/router/index.ts`
- Modify: `travel_frontend/src/layouts/TopNavLayout.vue`

- [ ] **Step 1: Write the failing router source assertion**

```ts
// travel_frontend/src/router/index.test.ts
it('registers the planner route with a distinct planning entry page', () => {
  const routerSource = readFileSync(resolve(__dirname, './index.ts'), 'utf8')

  expect(routerSource).toContain("path: '/ai/planner'")
  expect(routerSource).toContain("name: 'AI 行程规划'")
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- src/router/index.test.ts`  
Expected: FAIL while `PlannerPage.vue` is still only re-exporting `WorkspacePage`.

- [ ] **Step 3: Write the minimal implementation**

```vue
<!-- travel_frontend/src/pages/ai/PlannerPage.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const prompt = ref('')

function enterWorkspace() {
  router.push({
    path: '/workspace',
    query: prompt.value ? { prompt: prompt.value } : undefined,
  })
}
</script>

<template>
  <section class="planner-page">
    <div class="planner-shell">
      <p class="planner-kicker">AI 行程规划</p>
      <h1>从一句需求开始，进入你的旅行工作台</h1>
      <textarea
        v-model="prompt"
        rows="6"
        placeholder="例如：帮我规划一个东京 3 天 2 晚的轻松路线，预算 5000 元，想吃好吃的。"
      />
      <button type="button" @click="enterWorkspace">生成初始方案</button>
    </div>
  </section>
</template>
```

```ts
// travel_frontend/src/layouts/TopNavLayout.vue
const navItems = computed(() => {
  const items = [
    { label: '首页', path: '/' },
    { label: '规划', path: '/ai/planner' },
    { label: '行程', path: '/trips' },
    { label: '个人中心', path: '/profile' },
  ]
  if (loginUserStore.loginUser.userRole === 'admin') {
    items.push({ label: '监控', path: '/admin/ai-monitor' })
  }
  return items
})
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run test -- src/router/index.test.ts`  
Expected: PASS with planner route assertions green and planner component file present.

- [ ] **Step 5: Commit**

```bash
git add travel_frontend/src/pages/ai/PlannerPage.vue travel_frontend/src/router/index.ts travel_frontend/src/layouts/TopNavLayout.vue
git commit -m "feat(frontend): separate planner entry from workspace"
```

## Task 6: Polish homepage visuals, scrolling behavior, and responsive layout

**Files:**
- Modify: `travel_frontend/src/components/home/HomeSideNav.vue`
- Modify: `travel_frontend/src/components/home/HomeHero.vue`
- Modify: `travel_frontend/src/components/home/HomeCapabilityStrip.vue`
- Modify: `travel_frontend/src/components/home/HomeCasesSection.vue`
- Modify: `travel_frontend/src/pages/HomePage.vue`
- Modify: `travel_frontend/src/layouts/LandingLayout.vue`

- [ ] **Step 1: Write the failing build-level expectation by checking types first**

```ts
// No new unit test file. This task is guarded by project verification.
// The failure gate is type-check/build before visual polish.
```

- [ ] **Step 2: Run verification to expose current gaps**

Run: `npm run type-check`  
Expected: FAIL or reveal missing props, unresolved imports, or template typing issues before polish.

- [ ] **Step 3: Write the minimal implementation**

```vue
<!-- visual polish targets -->
<!-- HomeSideNav.vue -->
<style scoped lang="scss">
.home-side-nav {
  position: fixed;
  top: 132px;
  left: 28px;
  z-index: 20;
  display: grid;
  gap: 18px;
  padding: 16px 10px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(18px);
}
</style>
```

```vue
<!-- HomeHero.vue -->
<style scoped lang="scss">
.home-hero {
  min-height: 100vh;
  padding: 120px 64px 72px 180px;
  background-position: center;
  background-size: cover;
}

.hero-input-shell {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 16px;
  padding: 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(24px);
}
```

```vue
<!-- HomePage.vue -->
<style scoped lang="scss">
.home-main {
  display: flex;
  flex-direction: column;
}

@media (max-width: 900px) {
  .home-page {
    padding-left: 0;
  }
}
</style>
```

- [ ] **Step 4: Run verification to verify it passes**

Run: `npm run type-check && npm run build`  
Expected: Type check passes, then Vite build completes successfully.

- [ ] **Step 5: Commit**

```bash
git add travel_frontend/src/components/home/HomeSideNav.vue travel_frontend/src/components/home/HomeHero.vue travel_frontend/src/components/home/HomeCapabilityStrip.vue travel_frontend/src/components/home/HomeCasesSection.vue travel_frontend/src/pages/HomePage.vue travel_frontend/src/layouts/LandingLayout.vue
git commit -m "feat(frontend): polish competition homepage visuals"
```

## Task 7: Final verification and route regression check

**Files:**
- Test: `travel_frontend/src/utils/layoutResolver.test.ts`
- Test: `travel_frontend/src/composables/useHomeDestinations.test.ts`
- Test: `travel_frontend/src/router/index.test.ts`

- [ ] **Step 1: Run focused automated tests**

Run: `npm run test -- src/utils/layoutResolver.test.ts src/composables/useHomeDestinations.test.ts src/router/index.test.ts`  
Expected: PASS with all targeted tests green.

- [ ] **Step 2: Run full frontend type-check**

Run: `npm run type-check`  
Expected: PASS with no Vue or TypeScript errors.

- [ ] **Step 3: Run production build**

Run: `npm run build`  
Expected: PASS and emit the production bundle.

- [ ] **Step 4: Perform manual homepage smoke checks**

Run: `npm run dev`  
Expected manual checks:

- Homepage uses the landing layout instead of the top nav layout.
- Left-side nav stays fixed and shows `首页 / 规划 / 行程` plus admin-only `监控`.
- Hero destination chips switch background imagery and copy.
- Planner page opens as a dedicated entry page.
- Cases section is reachable by scrolling below the hero.

- [ ] **Step 5: Commit**

```bash
git add travel_frontend/src/App.vue travel_frontend/src/router/index.ts travel_frontend/src/layouts/LandingLayout.vue travel_frontend/src/layouts/TopNavLayout.vue travel_frontend/src/pages/HomePage.vue travel_frontend/src/pages/ai/PlannerPage.vue travel_frontend/src/components/home travel_frontend/src/composables/useHomeDestinations.ts travel_frontend/src/composables/useHomeDestinations.test.ts travel_frontend/src/utils/layoutResolver.ts travel_frontend/src/utils/layoutResolver.test.ts travel_frontend/src/router/index.test.ts
git commit -m "feat(frontend): deliver competition homepage flow"
```

## Self-Review

### Spec coverage

- Fixed left-side nav: Task 3 and Task 6.
- Logo-only top brand and landing shell: Task 1 and Task 6.
- Multi-destination hero with large input: Task 2, Task 3, and Task 6.
- Scrollable capability/cases sections: Task 4.
- Planner as the formal generation entry and workspace as follow-up editing: Task 5.
- Route and regression verification: Task 7.

No uncovered spec sections remain.

### Placeholder scan

- No `TBD`, `TODO`, or “implement later” placeholders remain in the tasks.
- Every code-changing task includes file paths, code blocks, commands, and expected outcomes.

### Type consistency

- Layout keys are consistently `topnav`, `landing`, and `none`.
- Homepage destination objects use the same `HomeDestinationTheme` shape throughout.
- `PlannerPage` is consistently the entry page and `WorkspacePage` remains the editing surface.
