package com.lq.travel.constant;

/**
 * 留言墙相关常量
 */
public class MessageWallConstants {

    // 留言状态
    public static final int MESSAGE_STATUS_PENDING = 0; // 待审核
    public static final int MESSAGE_STATUS_APPROVED = 1; // 已发布
    public static final int MESSAGE_STATUS_DELETED = 2; // 已删除

    // 留言类型
    public static final int MESSAGE_TYPE_NORMAL = 1; // 普通留言
    public static final int MESSAGE_TYPE_OFFICIAL = 2; // 官方公告

    // 背景类型
    public static final int BACKGROUND_TYPE_DAY = 1; // 白天
    public static final int BACKGROUND_TYPE_NIGHT = 2; // 夜晚
    public static final int BACKGROUND_TYPE_SEASON = 3; // 季节限定

    // 弹幕速度范围
    public static final int BARRAGE_SPEED_MIN = 1;
    public static final int BARRAGE_SPEED_MAX = 5;

    // 弹幕轨迹范围
    public static final int BARRAGE_TRAJECTORY_MIN = 1;
    public static final int BARRAGE_TRAJECTORY_MAX = 5;

    // 默认值
    public static final String DEFAULT_TEXT_COLOR = "#FFFFFF";
    public static final int DEFAULT_FONT_SIZE = 20;
    public static final String DEFAULT_BACKGROUND_COLOR = "rgba(0,0,0,0.3)";
    public static final int DEFAULT_BARRAGE_SPEED = 3;
    public static final int DEFAULT_BARRAGE_TRAJECTORY = 3;
}