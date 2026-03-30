<template>
  <div id="app">
    <!-- 根据路由 meta 决定是否使用布局 -->
    <component :is="layoutComponent" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TopNavLayout from './layouts/TopNavLayout.vue'

const route = useRoute()

// 根据路由 meta 动态选择布局
const layoutComponent = computed(() => {
  const layout = route.meta?.layout as string | undefined

  if (layout === 'none') {
    return 'router-view' // 直接渲染页面，无布局
  }
  return TopNavLayout // 默认使用顶部导航布局
})
</script>

<style>
/* 全局样式重置 */
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
  -webkit-text-size-adjust: 100%;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: var(--color-bg);
  color: var(--color-text);
  line-height: 1.6;
  overflow-x: hidden;
}

body.workspace-no-scroll {
  height: 100dvh;
  overflow: hidden;
}

/* ?????????????? + ????? */
/* Unified coastal-blue palette */
:root {
  /* Base neutrals */
  --color-bg: #f4f7fb;
  --color-bg-secondary: #ffffff;
  --color-bg-muted: #e8eef6;
  --color-surface-warm: #f7f1eb;
  --color-text: #0f1c2e;
  --color-text-secondary: #223046;
  --color-muted: #5b6475;
  --color-subtle: #7a8899;
  --color-border: #d5dde6;
  --color-border-strong: #bcc8d8;
  --color-shadow: rgba(18, 52, 97, 0.12);

  /* Coastal blue primary */
  --primary-50: #eaf1ff;
  --primary-100: #d6e4ff;
  --primary-200: #adc4ff;
  --primary-300: #84a5ff;
  --primary-400: #5f87f5;
  --primary-500: #3b6edc;
  --primary-600: #2f57b5;
  --primary-700: #264397;
  --primary-800: #1f3476;
  --primary-900: #162652;

  /* Cool sky accent */
  --accent-50: #e6f7ff;
  --accent-100: #c6eaff;
  --accent-200: #a6dcff;
  --accent-300: #7bc4ff;
  --accent-400: #4fa9ff;
  --accent-500: #2f90f0;
  --accent-600: #2373c2;
  --accent-700: #1c5ba0;
  --accent-800: #15447d;
  --accent-900: #0f2f59;
  --accent-border: #a6dcff;

  /* Coral support */
  --support-50: #fff0ed;
  --support-100: #fcd9d4;
  --support-200: #f7b5ac;
  --support-300: #f18d81;
  --support-400: #e56a63;
  --support-500: #d65050;
  --support-600: #bb4044;
  --support-700: #9c3337;
  --support-800: #7d282c;
  --support-900: #4c1619;

  /* Success / warning */
  --success-50: #e6f6f1;
  --success-100: #c4eada;
  --success-200: #95d9bf;
  --success-300: #68c8a4;
  --success-400: #3fb88c;
  --success-500: #26a174;
  --success-600: #1f835f;
  --success-700: #196a4e;
  --success-800: #13513d;
  --success-900: #0c3428;

  --warning-50: #fff7e6;
  --warning-100: #fee7c2;
  --warning-200: #ffd48a;
  --warning-300: #f7bb53;
  --warning-400: #e9a53c;
  --warning-500: #d48b1f;

  /* Secondary wash */
  --secondary-50: #f0f4ff;

  /* Gradients */
  --gradient-primary: linear-gradient(135deg, #3b6edc 0%, #2f90f0 100%);
  --gradient-secondary: linear-gradient(135deg, #2f90f0 0%, #7bc4ff 100%);
  --gradient-tertiary: linear-gradient(135deg, #264397 0%, #2f90f0 100%);
  --gradient-ocean: linear-gradient(135deg, #162652 0%, #3b6edc 100%);
  --gradient-sunrise: linear-gradient(135deg, #3b6edc 0%, #4fa9ff 50%, #a6dcff 100%);
  --gradient-sunset: linear-gradient(135deg, #162652 0%, #2f6fda 50%, #2f90f0 100%);
  --gradient-success: linear-gradient(135deg, #26a174 0%, #68c8a4 100%);
  --gradient-warning: linear-gradient(135deg, #d48b1f 0%, #f6c977 100%);

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(18, 52, 97, 0.08);
  --shadow-md: 0 4px 10px rgba(18, 52, 97, 0.1);
  --shadow-lg: 0 10px 25px rgba(18, 52, 97, 0.14);
  --shadow-xl: 0 20px 45px rgba(18, 52, 97, 0.16);
  --shadow-2xl: 0 30px 65px rgba(18, 52, 97, 0.22);

  --focus-ring: 0 0 0 3px rgba(59, 110, 220, 0.32);
  --shadow-accent: 0 12px 35px rgba(47, 144, 240, 0.2);
  --shadow-secondary: 0 12px 35px rgba(47, 144, 240, 0.12);

  /* ???? */
  --duration-fast: 150ms;
  --duration-normal: 250ms;
  --duration-slow: 350ms;
  
  /* ???? */
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  
  /* ?? */
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-xl: 24px;
  --radius-full: 9999px;
  
  /* ?? */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --spacing-2xl: 48px;

  /* Ant Design Vue ?????? */
  --ant-color-primary: var(--primary-500);
  --ant-color-success: var(--success-500);
  --ant-color-warning: var(--warning-500);
  --ant-color-error: var(--support-500);
  --ant-color-info: var(--accent-500);

  /* ??????? */
  --ant-color-primary-bg: var(--primary-50);
  --ant-color-success-bg: var(--success-50);
  --ant-color-warning-bg: var(--warning-50);
  --ant-color-error-bg: var(--support-50);
  --ant-color-info-bg: var(--accent-50);

  /* ?????? */
  --status-pending: var(--accent-500);
  --status-success: var(--success-500);
  --status-error: var(--support-500);
  --status-info: var(--primary-500);
  --status-warning: var(--warning-500);

  --status-pending-bg: var(--accent-50);
  --status-success-bg: var(--success-50);
  --status-error-bg: var(--support-50);
  --status-info-bg: var(--primary-50);
  --status-warning-bg: var(--warning-50);

  /* ?????? */
  --text-primary: var(--color-text);
  --text-secondary: var(--color-text-secondary);
  --text-muted: var(--color-subtle);
  --text-inverse: #ffffff;

  /* ????????? */
  --color-gray-999: #999999;
  --color-gray-ccc: #cccccc;
  --color-gray-eee: #eeeeee;
  --color-white: #ffffff;

  /* Material Design ?????? */
  --material-orange: var(--warning-500);
  --material-blue: var(--primary-500);
  --material-green: var(--success-500);
  --material-red: var(--support-500);
  --material-indigo: var(--primary-700);
}

/* 图片优化 */
img {
  max-width: 100%;
  height: auto;
  display: block;
}

/* 按钮重置 */
button {
  font-family: inherit;
  cursor: pointer;
}

/* 链接样式 */
a {
  color: inherit;
  text-decoration: none;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  transition: background var(--duration-normal) var(--ease-in-out);
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* 选中文本样式 - 旅游主题橙色 */
::selection {
  background: var(--accent-200);
  color: var(--accent-900);
}

::-moz-selection {
  background: var(--accent-200);
  color: var(--accent-900);
}

/* 全局动画类 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-normal) var(--ease-in-out);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all var(--duration-normal) var(--ease-in-out);
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 性能优化 */
.will-change-transform {
  will-change: transform;
}

.will-change-opacity {
  will-change: opacity;
}

/* 减少动画（尊重用户偏好） */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
</style>
