package com.myweather.android.logic.model

import com.google.gson.annotations.SerializedName

// 实时天气 API 响应体
data class RealtimeResponse(val status: String, val result: Result) {

    data class Result(val realtime: Realtime)

    data class Realtime(
        val skycon: String,
        val temperature: Double,
        val humidity: Double,
        val wind: Wind,
        @SerializedName("air_quality") val airQuality: AirQuality
    )

    data class Wind(val direction: Double, val speed: Double)

    data class AirQuality(val aqi: AQI)

    // 空气质量指数，中国标准(chn)和美国标准(usa)
    data class AQI(val chn: Double?, val usa: Double?)
}