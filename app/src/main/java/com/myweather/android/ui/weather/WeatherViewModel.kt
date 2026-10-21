package com.myweather.android.ui.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.myweather.android.logic.Repository
import com.myweather.android.logic.model.Location
import com.myweather.android.logic.model.Weather

/**
 * 天气页 ViewModel。通过 switchMap 驱动天气数据请求，
 * 内置 LRU 缓存避免频繁网络请求。
 */
class WeatherViewModel : ViewModel() {

    // 触发网络请求的核心 LiveData
    private val locationLiveData = MutableLiveData<Location>()

    var locationLng = ""
    var locationLat = ""
    var placeName = ""

    // 是否使用华氏度（美国/利比里亚/缅甸）
    var isFahrenheitCountry = false

    // 天气数据 LRU 缓存，key 为 "lng,lat"，最多 10 条
    private val weatherCache = LinkedHashMap<String, Weather>(8, 0.75f, false)

    // 当 locationLiveData 变化时自动发起天气网络请求
    val weatherLiveData: LiveData<Result<Weather>> = locationLiveData.switchMap { location ->
        Repository.refreshWeather(location.lng, location.lat)
    }

    fun refreshWeather(lng: String, lat: String) {
        locationLiveData.value = Location(lng, lat)
    }

    fun getCachedWeather(lng: String, lat: String): Weather? {
        return weatherCache[cacheKey(lng, lat)]
    }

    fun putCachedWeather(lng: String, lat: String, weather: Weather) {
        val key = cacheKey(lng, lat)
        if (weatherCache.size >= 10 && !weatherCache.containsKey(key)) {
            weatherCache.remove(weatherCache.keys.first())
        }
        weatherCache[key] = weather
    }

    private fun cacheKey(lng: String, lat: String) = "$lng,$lat"
}