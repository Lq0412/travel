package com.lq.travel.util;

import com.lq.travel.constant.MessageWallConstants;

/**
 * 留言墙工具类
 */
public class MessageWallUtil {

    /**
     * 验证弹幕速度是否合法
     */
    public static boolean isValidBarrageSpeed(Integer speed) {
        return speed != null &&
                speed >= MessageWallConstants.BARRAGE_SPEED_MIN &&
                speed <= MessageWallConstants.BARRAGE_SPEED_MAX;
    }

    /**
     * 验证弹幕轨迹是否合法
     */
    public static boolean isValidBarrageTrajectory(Integer trajectory) {
        return trajectory != null &&
                trajectory >= MessageWallConstants.BARRAGE_TRAJECTORY_MIN &&
                trajectory <= MessageWallConstants.BARRAGE_TRAJECTORY_MAX;
    }

    /**
     * 验证颜色格式
     */
    public static boolean isValidColor(String color) {
        if (color == null) return false;
        return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$") ||
                color.matches("^rgba?\\([^)]+\\)$");
    }

    /**
     * 获取背景类型名称
     */
    public static String getBackgroundTypeName(Integer type) {
        if (type == null) return "未知";

        switch (type) {
            case MessageWallConstants.BACKGROUND_TYPE_DAY:
                return "白天";
            case MessageWallConstants.BACKGROUND_TYPE_NIGHT:
                return "夜晚";
            case MessageWallConstants.BACKGROUND_TYPE_SEASON:
                return "季节限定";
            default:
                return "其他";
        }
    }
}