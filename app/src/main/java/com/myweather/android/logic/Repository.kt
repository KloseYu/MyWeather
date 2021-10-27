package com.myweather.android.logic

import androidx.lifecycle.liveData
import com.myweather.android.logic.model.PlaceResponse
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
    fun searchPlaces(query:String) = liveData(Dispatchers.IO) {
        val result = try {
            val placeResponse = MyWeatherNetWork.searchPlaces(query)
            if (placeResponse.status == "ok") {
                // 请求成功，读取数据并返回
                val places = placeResponse.places
                Result.success(places)
            } else {
                // 请求失败，数据为空，把错误信息包装抛上层
                Result.failure(RuntimeException("response status is ${placeResponse.status}"))
            }
        } catch (e: Exception) {
            Result.failure<List<PlaceResponse.Place>>(e)
        }
        emit(result)
    }
}