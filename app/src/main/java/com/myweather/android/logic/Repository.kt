package com.myweather.android.logic

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.myweather.android.logic.dao.PlaceDao
import com.myweather.android.logic.model.Place
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.network.MyWeatherNetWork
import kotlin.coroutines.CoroutineContext

/**
 * 数据仓库单例。封装网络层与本地存储的访问，
 * 提供给 ViewModel 统一的数据接口。
 */
object Repository {

    fun searchPlaces(query: String): LiveData<Result<List<Place>>> = fire(Dispatchers.IO) {
        val places = MyWeatherNetWork.searchPlaces(query)
        if (places.isNotEmpty()) {
            Result.success(places)
        } else {
            Result.failure(RuntimeException("未搜索到任何地点"))
        }
    }

    fun reverseGeocode(lng: String, lat: String): LiveData<Result<Place>> = fire(Dispatchers.IO) {
        val place = MyWeatherNetWork.reverseGeocode(lng, lat)
        if (place != null) {
            Result.success(place)
        } else {
            Result.failure(RuntimeException("海外及港澳台地区需配置Google API Key"))
        }
    }

    /**
     * 统一的网络请求入口函数。在 liveData 协程块中执行 block，
     * 自动捕获异常并包装为 Result，通过 emit 通知观察者。
     */
    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure(e)
            }
            emit(result)
        }

    fun savePlace(place: Place) = PlaceDao.savePlace(place)

    fun getSavedPlace() = PlaceDao.getSavedPlace()

    fun isPlaceSaved() = PlaceDao.isPlaceSaved()

    fun getSavedPlaceList() = PlaceDao.getSavedPlaceList()

    fun deletePlace(place: Place) = PlaceDao.deletePlace(place)

    /**
     * 并发请求实时天气、每日预报和逐小时预报（3 个 Retrofit 调用并行），
     * 全部成功后合并为 Weather 对象返回，否则返回失败 Result。
     */
    fun refreshWeather(lng: String, lat: String): LiveData<Result<Weather>> = fire(Dispatchers.IO) {
        coroutineScope {
            val deferredRealtime = async { MyWeatherNetWork.getRealtimeWeather(lng, lat) }
            val deferredDaily = async { MyWeatherNetWork.getDailyWeather(lng, lat) }
            val deferredHourly = async { MyWeatherNetWork.getHourlyWeather(lng, lat) }
            val realtimeResponse = deferredRealtime.await()
            val dailyResponse = deferredDaily.await()
            val hourlyResponse = deferredHourly.await()
            if (realtimeResponse.status == "ok" && dailyResponse.status == "ok" && hourlyResponse.status == "ok") {
                val weather = Weather(realtimeResponse.result.realtime, dailyResponse.result.daily, hourlyResponse.result.hourly)
                Result.success(weather)
            } else {
                Result.failure(RuntimeException(
                    "realtime response status is ${realtimeResponse.status}, " +
                        "daily response status is ${dailyResponse.status}, " +
                        "hourly response status is ${hourlyResponse.status}"
                ))
            }
        }
    }
}