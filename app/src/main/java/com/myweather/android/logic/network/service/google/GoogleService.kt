package com.myweather.android.logic.network.service.google

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Geocoding API 接口（海外及港澳台地区的逆地理编码/城市搜索备选方案）。
 * 注意：Retrofit 接口方法不使用默认参数，Key 与语言由调用方显式传入。
 */
interface GoogleService {

    @GET("maps/api/geocode/json")
    fun geocode(
        @Query("address") address: String,
        @Query("language") language: String,
        @Query("key") key: String
    ): Call<GoogleGeocodeResponse>

    @GET("maps/api/geocode/json")
    fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("language") language: String,
        @Query("key") key: String
    ): Call<GoogleGeocodeResponse>
}