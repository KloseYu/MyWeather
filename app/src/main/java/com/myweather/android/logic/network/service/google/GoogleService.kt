package com.myweather.android.logic.network.service.google

import com.myweather.android.MyWeatherApplication.Companion.GOOGLE_KEY
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// Google Geocoding API 接口（海外逆地理编码备选方案）
interface GoogleService {

    @GET("maps/api/geocode/json")
    fun geocode(
        @Query("address") address: String,
        @Query("language") language: String = "zh-CN",
        @Query("key") key: String = GOOGLE_KEY
    ): Call<GoogleGeocodeResponse>

    @GET("maps/api/geocode/json")
    fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("language") language: String = "zh-CN",
        @Query("key") key: String = GOOGLE_KEY
    ): Call<GoogleGeocodeResponse>
}