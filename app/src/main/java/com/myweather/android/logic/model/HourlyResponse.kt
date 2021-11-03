package com.myweather.android.logic.model


/**
 * 文件名: HourlyResponse
 * 作者: KloseYu
 * 日期: 2021/11/03
 * 描述: 24小时天气模块
 */
data class HourlyResponse(
    val status:String,
    val result:Result,
){

    data class Result(val hourly:Hourly)

    data class Hourly(
        val temperature: List<Temperature>,
        val skycon: List<Skycon>,
    )

    data class Temperature(
        val datetime: String,
        val value: Float,
    )

    data class  Skycon(
        val datetime: String,
        val value: String
    )



}

