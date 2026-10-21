package com.myweather.android.logic.network

import com.myweather.android.logic.model.*
import com.myweather.android.logic.network.service.amap.AmapServiceCreator
import com.myweather.android.logic.network.service.amap.toPlace
import com.myweather.android.logic.network.service.caiyun.WeatherService
import com.myweather.android.logic.network.service.caiyun.CaiYunServiceCreator

/**
 * 统一网络访问入口。
 * - 彩云天气：实时/每日/逐小时天气数据（suspend 直接调用）
 * - 高德地图：城市搜索 + 逆地理编码（suspend 直接调用）
 */
object MyWeatherNetWork {

    // 高德城市搜索接口
    private val amapTextSearchService = AmapServiceCreator.createTextSearchService()

    // 高德逆地理编码接口
    private val amapReverseGeocodeService = AmapServiceCreator.createReverseGeocodeService()

    // 彩云天气接口
    private val weatherService = CaiYunServiceCreator.create<WeatherService>()

    // 城市搜索：高德 POI 搜索 → Place 列表
    suspend fun searchPlaces(query: String): List<Place> {
        return try {
            val amapResult = amapTextSearchService.searchCities(query, "", "", 10, 1)
            if (amapResult.status == "1" && amapResult.pois.isNotEmpty()) {
                amapResult.pois.map { it.toPlace() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 逆地理编码：经纬度 → 城市名（高德 regeo）
    suspend fun reverseGeocode(lng: String, lat: String): Place? {
        return try {
            val response = amapReverseGeocodeService.reverseGeocode("${lng},${lat}")
            if (response.status == "1" && response.regeocode != null) {
                response.regeocode.toPlace(lng, lat)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 实时天气（彩云）
    suspend fun getRealtimeWeather(lng: String, lat: String) =
        weatherService.getRealtimeWeather(lng, lat)

    // 每日预报（彩云）
    suspend fun getDailyWeather(lng: String, lat: String) =
        weatherService.getDailyWeather(lng, lat)

    // 逐小时预报（彩云）
    suspend fun getHourlyWeather(lng: String, lat: String) =
        weatherService.getHourlyWeather(lng, lat)
}