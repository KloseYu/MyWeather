package com.myweather.android.logic.network.service.caiyun

import com.myweather.android.BuildConfig
import com.myweather.android.logic.model.RealtimeResponse
import com.myweather.android.logic.model.DailyResponse
import com.myweather.android.logic.model.HourlyResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 彩云天气 Retrofit 接口。
 * Token 引用 [BuildConfig.CAIYUN_KEY]，由 local.properties 注入，源码中不含真实 Key。
 * 文档：https://docs.caiyunapp.com/docs/tables/weather
 */
interface WeatherService {
    @GET("v2.6/${BuildConfig.CAIYUN_KEY}/{lng},{lat}/realtime.json")
    suspend fun getRealtimeWeather(@Path("lng") lng: String, @Path("lat") lat: String): RealtimeResponse

    @GET("v2.6/${BuildConfig.CAIYUN_KEY}/{lng},{lat}/daily.json?dailysteps=7")
    suspend fun getDailyWeather(@Path("lng") lng: String, @Path("lat") lat: String): DailyResponse

    @GET("v2.6/${BuildConfig.CAIYUN_KEY}/{lng},{lat}/hourly.json")
    suspend fun getHourlyWeather(@Path("lng") lng: String, @Path("lat") lat: String): HourlyResponse
}