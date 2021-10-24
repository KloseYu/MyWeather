package com.myweather.android.logic.network

import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.PlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 文件名: ApiService
 * 作者: KloseYu
 * 日期: 2021/10/24
 * 描述: Api接口
 */
interface ApiService {
    //地区api
    @GET("2.5/place?${MyWeatherApplication.TOKEN}&lang=zh_CN")
    fun searchPlaces(@Query("query") query:String):Call<PlaceResponse>
}