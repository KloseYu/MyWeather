package com.myweather.android.logic.network.service.caiyun

import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.RealtimeResponse
import com.myweather.android.logic.model.DailyResponse
import com.myweather.android.logic.model.HourlyResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 彩云天气 Retrofit 接口。
 * Token 直接引用 [MyWeatherApplication.CaiYun_Key]（const 常量，可用于注解）。
 * 文档：https://docs.caiyunapp.com/docs/tables/weather
 */
interface WeatherService {
    @GET("v2.6/${MyWeatherApplication.CaiYun_Key}/{lng},{lat}/realtime.json")
    suspend fun getRealtimeWeather(@Path("lng") lng: String, @Path("lat") lat: String): RealtimeResponse

    @GET("v2.6/${MyWeatherApplication.CaiYun_Key}/{lng},{lat}/daily.json?dailysteps=7")
    suspend fun getDailyWeather(@Path("lng") lng: String, @Path("lat") lat: String): DailyResponse

    @GET("v2.6/${MyWeatherApplication.CaiYun_Key}/{lng},{lat}/hourly.json")
    suspend fun getHourlyWeather(@Path("lng") lng: String, @Path("lat") lat: String): HourlyResponse
}