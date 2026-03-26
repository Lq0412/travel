import { createApp } from 'vue'
import { createPinia } from 'pinia'

import Antd from 'ant-design-vue';
import App from './App.vue'
import 'ant-design-vue/dist/reset.css';
import router from './router'
import './access'

const app = createApp(App)

// 配置 Ant Design Vue 主题，统一远航蓝视觉系统
app.use(Antd as any, {
  theme: {
    token: {
      // 主色系 - Ocean teal
      colorPrimary: '#3b6edc',
      colorPrimaryBg: '#eaf1ff',
      colorPrimaryBorder: '#3b6edc',
      colorPrimaryHover: '#2f57b5',
      colorPrimaryActive: '#264397',

      // 强调色 - Sunlit gold (用于重要操作和按钮)
      colorInfo: '#2f90f0',
      colorInfoBg: '#e6f7ff',
      colorInfoBorder: '#2f90f0',
      colorInfoHover: '#2373c2',
      colorInfoActive: '#1c5ba0',

      // 成功色 - Teal green
      colorSuccess: '#26a174',
      colorSuccessBg: '#e6f6f1',
      colorSuccessBorder: '#26a174',
      colorSuccessHover: '#1f835f',
      colorSuccessActive: '#196a4e',

      // 警告色 - Sunlit gold warning
      colorWarning: '#d48b1f',
      colorWarningBg: '#fff7e6',
      colorWarningBorder: '#d48b1f',
      colorWarningHover: '#b67419',
      colorWarningActive: '#965e14',

      // 错误色 - Coral
      colorError: '#d65050',
      colorErrorBg: '#fff0ed',
      colorErrorBorder: '#d65050',
      colorErrorHover: '#bb4044',
      colorErrorActive: '#9c3337',

      // 文本色系
      colorText: '#0f1c2e',
      colorTextSecondary: '#223046',
      colorTextTertiary: '#7a8899',
      colorTextQuaternary: '#bcc8d8',

      // 背景色系
      colorBgContainer: '#ffffff',
      colorBgElevated: '#ffffff',
      colorBgLayout: '#f4f7fb',
      colorBgSpotlight: 'rgba(255, 255, 255, 0.9)',

      // 边框色系
      colorBorder: '#d5dde6',
      colorBorderSecondary: '#e8eef6',

      // 圆角系统
      borderRadius: 10,
      borderRadiusLG: 16,
      borderRadiusSM: 6,

      // 字体系统
      fontSize: 14,
      fontSizeLG: 16,
      fontSizeSM: 12,
      lineHeight: 1.6,

      // 间距系统
      marginXS: 4,
      marginSM: 8,
      marginMD: 16,
      marginLG: 24,
      marginXL: 32,
      paddingXS: 4,
      paddingSM: 8,
      paddingMD: 16,
      paddingLG: 24,
      paddingXL: 32,

      // 阴影系统
      boxShadow: '0 4px 10px rgba(18, 52, 97, 0.1)',
      boxShadowSecondary: '0 1px 2px rgba(18, 52, 97, 0.08)',
      boxShadowTertiary: '0 10px 25px rgba(18, 52, 97, 0.14)',

      // 动画系统
      motionDurationSlow: '350ms',
      motionDurationMid: '250ms',
      motionDurationFast: '150ms'
    }
  }
});

app.use(createPinia())
app.use(router)
app.mount('#app');

