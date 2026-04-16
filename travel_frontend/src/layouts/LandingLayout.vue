<template>
  <div class="landing-layout">
    <router-link to="/" class="landing-layout__brand">
      <span class="landing-layout__mark">旅</span>
      <span class="landing-layout__copy">
        <strong>旅刻</strong>
        <small>AI Travel Planner</small>
      </span>
    </router-link>
    <RouterView />
  </div>
</template>

<style scoped lang="scss">
.landing-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.14), transparent 32%),
    linear-gradient(180deg, #0f172a 0%, #111827 100%);
}

.landing-layout__brand {
  position: fixed;
  top: 26px;
  left: 28px;
  z-index: 18;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #ffffff;
}

.landing-layout__mark {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(16px);
  font-size: 18px;
  font-weight: 700;
}

.landing-layout__copy {
  display: grid;
}

.landing-layout__copy small {
  color: rgba(255, 255, 255, 0.72);
}

@media (max-width: 900px) {
  .landing-layout__brand {
    top: 16px;
    left: 16px;
  }
}
</style>
