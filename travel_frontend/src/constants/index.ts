/**
 * 应用常量配置
 */

/**
 * API相关常量
 */
export const API_CONSTANTS = {
  /** API基础URL */
  BASE_URL: import.meta.env.VITE_API_BASE_URL || '',
  
  /** 请求超时时间（毫秒） */
  TIMEOUT: 30000,
  
  /** 响应成功码 */
  SUCCESS_CODE: 0,
  
  /** 响应成功码（备用） */
  SUCCESS_CODE_ALT: 200,
} as const

/**
 * HTTP状态码
 */
export const HTTP_STATUS = {
  /** 成功 */
  OK: 200,
  /** 未授权 */
  UNAUTHORIZED: 401,
  /** 禁止访问 */
  FORBIDDEN: 403,
  /** 未找到 */
  NOT_FOUND: 404,
  /** 服务器错误 */
  INTERNAL_SERVER_ERROR: 500,
} as const

/**
 * 业务错误码
 */
export const BUSINESS_CODE = {
  /** 成功 */
  SUCCESS: 0,
  /** 成功（备用） */
  SUCCESS_ALT: 200,
  /** 请求参数错误 */
  PARAMS_ERROR: 40000,
  /** 未登录 */
  NOT_LOGIN: 40100,
  /** 无权限 */
  NO_AUTH: 40101,
  /** 禁止访问 */
  FORBIDDEN: 40300,
  /** 请求数据不存在 */
  NOT_FOUND: 40400,
  /** 系统内部异常 */
  SYSTEM_ERROR: 50000,
  /** 操作失败 */
  OPERATION_ERROR: 50001,
} as const

/**
 * 用户角色
 */
export const USER_ROLE = {
  /** 普通用户 */
  USER: 'user',
  /** 商家 */
  MERCHANT: 'merchant',
  /** 管理员 */
  ADMIN: 'admin',
} as const

/**
 * 本地存储键名
 */
export const STORAGE_KEYS = {
  /** 用户token */
  TOKEN: 'token',
  /** 用户信息 */
  USER_INFO: 'userInfo',
  /** 主题设置 */
  THEME: 'theme',
  /** 语言设置 */
  LANGUAGE: 'language',
} as const

/**
 * 路由路径
 */
export const ROUTES = {
  /** 首页 */
  HOME: '/',
  /** 登录（统一入口） */
  LOGIN: '/user/login',
  /** 用户登录 */
  USER_LOGIN: '/user/login',
  /** 用户注册 */
  USER_REGISTER: '/user/register',
} as const

/**
 * 默认图片URL
 */
export const DEFAULT_IMAGES = {
  /** 默认背景图 */
  BACKGROUND: 'https://open.saintic.com/api/bingPic',
  /** 默认用户头像 */
  USER_AVATAR: '/src/assets/user-avatar.jpg',
} as const

/**
 * 分页配置
 */
export const PAGINATION = {
  /** 默认页码 */
  DEFAULT_CURRENT: 1,
  /** 默认每页大小 */
  DEFAULT_PAGE_SIZE: 10,
  /** 每页大小选项 */
  PAGE_SIZE_OPTIONS: ['10', '20', '50', '100'],
  /** 是否显示总数 */
  SHOW_TOTAL: true,
  /** 是否显示每页大小选择器 */
  SHOW_SIZE_CHANGER: true,
} as const

/**
 * 表单验证规则
 */
export const VALIDATION = {
  /** 用户名最小长度 */
  USERNAME_MIN_LENGTH: 4,
  /** 用户名最大长度 */
  USERNAME_MAX_LENGTH: 20,
  /** 密码最小长度 */
  PASSWORD_MIN_LENGTH: 8,
  /** 密码最大长度 */
  PASSWORD_MAX_LENGTH: 20,
  /** 手机号正则 */
  PHONE_REGEX: /^1[3-9]\d{9}$/,
  /** 邮箱正则 */
  EMAIL_REGEX: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
} as const

/**
 * 时间格式
 */
export const DATE_FORMAT = {
  /** 日期时间格式 */
  DATETIME: 'YYYY-MM-DD HH:mm:ss',
  /** 日期格式 */
  DATE: 'YYYY-MM-DD',
  /** 时间格式 */
  TIME: 'HH:mm:ss',
  /** 月日格式 */
  MONTH_DAY: 'MM-DD',
  /** 时分格式 */
  HOUR_MINUTE: 'HH:mm',
} as const

/**
 * 消息提示持续时间（秒）
 */
export const MESSAGE_DURATION = {
  /** 短提示 */
  SHORT: 2,
  /** 默认提示 */
  DEFAULT: 3,
  /** 长提示 */
  LONG: 5,
} as const

/**
 * 订单状态
 */
export const ORDER_STATUS = {
  /** 待支付 */
  PENDING_PAYMENT: 0,
  /** 已支付 */
  PAID: 1,
  /** 已发货 */
  SHIPPED: 2,
  /** 已完成 */
  COMPLETED: 3,
  /** 已取消 */
  CANCELLED: 4,
  /** 已退款 */
  REFUNDED: 5,
} as const

/**
 * 商品状态
 */
export const PRODUCT_STATUS = {
  /** 下架 */
  OFF_SHELF: 0,
  /** 上架 */
  ON_SHELF: 1,
} as const

/**
 * 环境变量
 */
export const ENV = {
  /** 当前环境 */
  MODE: import.meta.env.MODE,
  /** 是否开发环境 */
  IS_DEV: import.meta.env.MODE === 'development',
  /** 是否生产环境 */
  IS_PROD: import.meta.env.MODE === 'production',
} as const

