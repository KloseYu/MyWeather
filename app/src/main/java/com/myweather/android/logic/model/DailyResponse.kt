package com.myweather.android.logic.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 每日天气预报 API 响应体。
 * 包含 7 天温度/天气图标/生活指数数据。
 */
data class DailyResponse(val status: String, val result: Result) {

    data class Result(val daily: Daily)

    data class Daily(
        val temperature: List<Temperature>,
        val skycon: List<Skycon>,
        @SerializedName("skycon_08h_20h") val skycon08h20h: List<Skycon>? = null,
        @SerializedName("skycon_20h_32h") val skycon20h32h: List<Skycon>? = null,
        @SerializedName("life_index") val lifeIndex: LifeIndex
    )

    data class Temperature(val max: Double, val min: Double)

    data class Skycon(val value: String, val date: Date)

    data class LifeIndex(
        val coldRisk: List<LifeDescription>,
        val carWashing: List<LifeDescription>,
        val ultraviolet: List<LifeDescription>,
        val dressing: List<LifeDescription>
    )

    data class LifeDescription(val desc: String)
}