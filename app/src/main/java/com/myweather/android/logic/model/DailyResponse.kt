package com.myweather.android.logic.model

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * 文件名: DailyResponse
 * 作者: KloseYu
 * 日期: 2021/10/27
 * 描述: 最近天气模块
 */
data class DailyResponse(val status: String, val result: Result) {
    data class Result(val daily: Daily)

    data class Daily(
        val temperature: List<Temperature>,
        val skycon: List<Skycon>,
        @SerializedName("life_index") val lifeIndex: LifeIndex
    )

    data class Temperature(
        val max: Float,
        val min: Float)
    data class Skycon(
        val value: String,
        val date: Date)
    data class LifeIndex(
        val coldRisk: List<LifeDescription>,
        val carWashing: List<LifeDescription>,
        val ultraviolet: List<LifeDescription>,
        val dressing:List<LifeDescription>
    )
    data class LifeDescription(val desc:String)
}

