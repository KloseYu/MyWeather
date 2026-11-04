package com.myweather.android.logic

import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.dao.PlaceDao
import com.myweather.android.logic.model.Place
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.network.MyWeatherNetWork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 数据仓库单例。封装网络层与本地存储的访问，
 * 提供给 ViewModel 统一的数据接口。
 *
 * 网络请求统一为挂起函数，由调用方的 viewModelScope 调度；
 * 请求被取消时重新抛出 [CancellationException]，
 * 避免「切换城市 / 改写搜索词」把旧请求的取消当成一次失败结果。
 */
object Repository {

    suspend fun searchPlaces(query: String): Result<List<Place>> = withContext(Dispatchers.IO) {
        try {
            val places = MyWeatherNetWork.searchPlaces(query)
            if (places.isNotEmpty()) {
                Result.success(places)
            } else {
                Result.failure(RuntimeException("未搜索到任何地点"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(lng: String, lat: String): Result<Place> = withContext(Dispatchers.IO) {
        try {
            val place = MyWeatherNetWork.reverseGeocode(lng, lat)
            if (place != null) {
                Result.success(place)
            } else {
                Result.failure(RuntimeException(geocodeFailureMessage()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 逆地理编码失败提示：区分「未配置 Google Key」与「已配置但仍失败」
    private fun geocodeFailureMessage(): String {
        return if (MyWeatherApplication.GOOGLE_KEY.isNotBlank()) {
            "定位失败，请检查网络后重试"
        } else {
            "海外及港澳台地区需配置 Google API Key"
        }
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
    suspend fun refreshWeather(lng: String, lat: String): Result<Weather> = withContext(Dispatchers.IO) {
        try {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}