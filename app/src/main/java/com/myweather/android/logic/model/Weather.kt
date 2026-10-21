package com.myweather.android.logic.model

/**
 * 聚合天气数据。将实时、每日、逐小时三种响应合并为一个对象。
 */
data class Weather(
    val realtime: RealtimeResponse.Realtime,
    val daily: DailyResponse.Daily,
    val hourly: HourlyResponse.Hourly?
) {

    // 根据经纬度判断是否在中国范围内（用于选择 AQI 标准）
    fun isChina(lng: String, lat: String): Boolean {
        val lngVal = lng.toDoubleOrNull() ?: return false
        val latVal = lat.toDoubleOrNull() ?: return false
        return lngVal in 73.0..135.0 && latVal in 3.0..54.0
    }

    // 获取 AQI 数值（中国用 chn 值，海外用 usa 值）
    fun getAqiValue(isChina: Boolean): Int? {
        val aqi = realtime.airQuality.aqi
        return if (isChina) aqi.chn?.toInt() else aqi.usa?.toInt()
    }

    // 获取 AQI 等级描述文字
    fun getAqiLevel(isChina: Boolean): String {
        val aqiValue = getAqiValue(isChina) ?: return ""
        return if (isChina) {
            when {
                aqiValue <= 50 -> "优"
                aqiValue <= 100 -> "良"
                aqiValue <= 150 -> "轻度污染"
                aqiValue <= 200 -> "中度污染"
                aqiValue <= 300 -> "重度污染"
                else -> "严重污染"
            }
        } else {
            when {
                aqiValue <= 50 -> "Good"
                aqiValue <= 100 -> "Moderate"
                aqiValue <= 150 -> "Unhealthy(S)"
                aqiValue <= 200 -> "Unhealthy"
                aqiValue <= 300 -> "Very Unhealthy"
                else -> "Hazardous"
            }
        }
    }
}