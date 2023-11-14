package com.myweather.android.logic.model

/*
 * 文件名: RealtimeResponse
 * 作者: KloseYu
 * 日期: 2021/10/27
 * 描述: 天气整合模块
 */

data class Weather(
    val realtime: RealtimeResponse.Result.Realtime,
    val hourly: HourlyResponse.Hourly,
    val daily: DailyResponse.Daily,
)