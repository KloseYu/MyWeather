package com.myweather.android.logic.network

import com.myweather.android.MyWeatherApplication.Companion.TOKEN
import com.myweather.android.logic.model.DailyResponse
import com.myweather.android.logic.model.HourlyResponse
import com.myweather.android.logic.model.PlaceResponse
import com.myweather.android.logic.model.RealtimeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/*
 * 文件名: ApiService
 * 作者: KloseYu
 * 日期: 2021/10/24
 * 描述: Api接口
 * Api更新：2023/11/13
 */

interface ApiService {
    //全球地区api
    @GET("v2.6/place?${TOKEN}&lang=zh_CN")
    fun searchPlaces(@Query("query") query:String):Call<PlaceResponse>

    //实时天气api
    @GET("v2.6/${TOKEN}/{lng},{lat}/realtime.json")
    fun getRealWeather(@Path("lng") lng:String,@Path("lat") lat:String):Call<RealtimeResponse>


    //24小时天气api
    @GET("v2.6/${TOKEN}/{lng},{lat}/hourly.json?hourlysteps=24")
    fun getHourlyWeather(@Path("lng") lng:String,@Path("lat") lat:String):Call<HourlyResponse>

    //最近天气api
   @GET("v2.6/${TOKEN}/{lng},{lat}/daily.json")
    fun getDailyWeather(@Path("lng") lng:String,@Path("lat") lat:String):Call<DailyResponse>
}