package com.myweather.android.logic.model

import com.google.gson.annotations.SerializedName
import java.util.Date

// 逐小时天气预报 API 响应体，包含 24 小时数据
data class HourlyResponse(val status: String, val result: Result) {

    data class Result(val hourly: Hourly)

    data class Hourly(
        val temperature: List<Temperature>?,
        val skycon: List<Skycon>?,
        @SerializedName("wind") val wind: List<Wind>?
    )

    data class Temperature(val value: Double, @SerializedName("datetime") val date: Date)

    data class Skycon(val value: String, @SerializedName("datetime") val date: Date)

    data class Wind(val speed: Double, val direction: Double, @SerializedName("datetime") val date: Date)
}