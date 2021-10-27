package com.myweather.android.logic.model

import com.google.gson.annotations.SerializedName

/**
 * 文件名: RealtimeResponse
 * 作者: KloseYu
 * 日期: 2021/10/27
 * 描述: 实时天气模块
 */
data class RealtimeResponse (
    val status:String,
    val result:Result
) { data class Result(
    val realtime:Realtime
) { data class Realtime(
    val temperature:Float,
    val skycon:String,
    @SerializedName("air_quality") val airQuality:AirQuality,
){ data class AirQuality(
        val aqi:AQI
        )
    { data class AQI(
            val chn:Float
            )
    }
       }
    }
}