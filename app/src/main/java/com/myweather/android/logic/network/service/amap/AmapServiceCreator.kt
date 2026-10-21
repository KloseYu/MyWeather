package com.myweather.android.logic.network.service.amap

import retrofit2.Retrofit

// 高德地图 Retrofit 构建器
object AmapServiceCreator {
    private const val BASE_URL = "https://restapi.amap.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    fun createTextSearchService(): AmapTextSearchService {
        return retrofit.create(AmapTextSearchService::class.java)
    }

    fun createReverseGeocodeService(): AmapReverseGeocodeService {
        return retrofit.create(AmapReverseGeocodeService::class.java)
    }
}