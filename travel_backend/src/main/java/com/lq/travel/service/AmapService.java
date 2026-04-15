package com.lq.travel.service;

public interface AmapService {

    record GeoPoint(Double longitude, Double latitude, String formattedAddress) {}

    /**
     * 获取指定城市的实时和预报天气
     * @param city 城市名称（如：成都市）
     * @return 格式化后的天气文本
     */
    String getWeatherInfo(String city);

    GeoPoint geocode(String city, String address);
}
