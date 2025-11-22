// @ts-ignore
/* eslint-disable */
// 适配层：提供 messageWallApi 对象，兼容现有页面与组合式函数的导入方式
import {
  addMessage,
  cancelLikeMessage,
  deleteMessage,
  likeMessage,
  listMessages,
  getAllScenicMessageWalls,
  createScenicMessageWall,
  updateScenicMessageWall,
  reviewMessage,
} from '@/api/messageWallController'

export const messageWallApi = {
  // 消息墙基础能力
  addMessage,
  listMessages,
  likeMessage,
  cancelLikeMessage,
  deleteMessage,
  reviewMessage,
  // 场景墙管理
  getAllScenicMessageWalls,
  createScenicMessageWall,
  updateScenicMessageWall,
}


