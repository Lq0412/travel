package com.lq.travel.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间格式化工具类
 * 统一处理时间格式化，避免重复代码
 */
public class TimeFormatUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 格式化时间为字符串
     * @param date 日期对象
     * @return 格式化后的时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_TIME_FORMATTER);
    }

    /**
     * 格式化时间为日期字符串
     * @param date 日期对象
     * @return 格式化后的日期字符串 (yyyy-MM-dd)
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_FORMATTER);
    }

    /**
     * 格式化时间为时间字符串
     * @param date 日期对象
     * @return 格式化后的时间字符串 (HH:mm:ss)
     */
    public static String formatTimeOnly(Date date) {
        if (date == null) {
            return "";
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(TIME_FORMATTER);
    }

    /**
     * 格式化LocalDateTime为字符串
     * @param dateTime LocalDateTime对象
     * @return 格式化后的时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 获取相对时间描述（如：2小时前、3天前等）
     * @param date 日期对象
     * @return 相对时间描述
     */
    public static String getRelativeTime(Date date) {
        if (date == null) {
            return "";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        long seconds = java.time.Duration.between(targetTime, now).getSeconds();

        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时前";
        } else if (seconds < 2592000) {
            return (seconds / 86400) + "天前";
        } else if (seconds < 31536000) {
            return (seconds / 2592000) + "个月前";
        } else {
            return (seconds / 31536000) + "年前";
        }
    }

    /**
     * 获取当前时间字符串
     * @return 当前时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String getCurrentTimeString() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * 获取当前日期字符串
     * @return 当前日期字符串 (yyyy-MM-dd)
     */
    public static String getCurrentDateString() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
}
