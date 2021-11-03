package com.myweather.android.logic

import androidx.lifecycle.liveData
import com.myweather.android.logic.dao.PlaceDao
import com.myweather.android.logic.model.PlaceResponse
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.network.MyWeatherNetWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.coroutines.CoroutineContext

/**
 * 文件名: Repository
 * 作者: KloseYu
 * 日期: 2021/10/24
 * 描述: 仓库层的统一封装入口
 */
object Repository {

    // liveData是ktx库提供的一个功能，可以自动构建并返回LiveData对象
    // 可以在代码块提供一个挂起函数的上下文，并将线程设置成了IO，所有的代码块都在子线程中了。
    //
    fun searchPlaces(query: String) = fire(Dispatchers.IO) {
        val placeResponse = MyWeatherNetWork.searchPlaces(query)
        if (placeResponse.status == "ok") {
            // 请求成功，读取数据并返回
            val places = placeResponse.places
            Result.success(places)
        } else {
            // 请求失败，数据为空，把错误信息包装抛上层
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }

    fun refreshWeather(lng: String, lat: String) = fire(Dispatchers.IO) {
        coroutineScope {
            val deferredRealtime = async {
                MyWeatherNetWork.getRealtimeWeather(lng, lat)
            }
            val deferredHourly =async {
                MyWeatherNetWork.getHourlyWeather(lng,lat)
            }
            val deferredDaily = async {
                MyWeatherNetWork.getDailyWeather(lng, lat)
            }
            val realtimeResponse = deferredRealtime.await()
            val hourlyResponse = deferredHourly.await()
            val dailyResponse = deferredDaily.await()
            if (realtimeResponse.status == "ok" &&hourlyResponse.status =="ok"&& dailyResponse.status == "ok") {
                val weather = Weather(realtimeResponse.result.realtime,hourlyResponse.result.hourly,dailyResponse.result.daily)
                Result.success(weather)
            } else {
                Result.failure(
                    RuntimeException(
                        "realtime response status is ${realtimeResponse.status}" +
                                "hourly response status is ${hourlyResponse.status}"+
                                "daily response status is ${dailyResponse.status}"
                    )
                )
            }
        }
    }

    // 定义了一个符合liveData()函数的参数的接收标准定义的高阶函数。
    // suspend关键字，表示传入的Lambda表达式也拥有挂起函数的上下文
    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData<Result<T>>(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }

    // placeDao的相关函数
    fun savePlace(place: PlaceResponse.Place) {
        PlaceDao.savePlace(place)
    }

    fun getSavedPlace(): PlaceResponse.Place {
        return PlaceDao.getSavedPlace()
    }

    fun isPlaceSaved(): Boolean {
        return PlaceDao.isPlaceSaved()
    }
}