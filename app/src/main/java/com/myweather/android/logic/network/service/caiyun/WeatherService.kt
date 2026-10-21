package com.myweather.android.logic.network.service.caiyun

import com.myweather.android.logic.model.RealtimeResponse
import com.myweather.android.logic.model.DailyResponse
import com.myweather.android.logic.model.HourlyResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 彩云天气 Retrofit 接口。
 * 通过动态 baseUrl（含 token）访问。
 * 文档：https://docs.caiyunapp.com/docs/tables/weather
 */
interface WeatherService {
    @GET("v2.6/${CaiYunToken.token}/{lng},{lat}/realtime.json")
    suspend fun getRealtimeWeather(@Path("lng") lng: String, @Path("lat") lat: String): RealtimeResponse

    @GET("v2.6/${CaiYunToken.token}/{lng},{lat}/daily.json?dailysteps=7")
    suspend fun getDailyWeather(@Path("lng") lng: String, @Path("lat") lat: String): DailyResponse

    @GET("v2.6/${CaiYunToken.token}/{lng},{lat}/hourly.json")
    suspend fun getHourlyWeather(@Path("lng") lng: String, @Path("lat") lat: String): HourlyResponse
}

// 彩云天气 API token
object CaiYunToken {
    const val token = "8yTp8hXFWAxo0n5C"
}