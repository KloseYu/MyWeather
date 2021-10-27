package com.myweather.android.logic.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * 文件名: MyWeatherNetWork
 * 作者: KloseYu
 * 日期: 2021/10/24
 * 描述: 网络数据源访问入口
 */
object MyWeatherNetWork {
    private val apiService = ServiceCreator.create(ApiService::class.java)


    /**
     * 搜索全球地区的API接口，调用了自己定义的await函数来简化写法。
     */
    suspend fun searchPlaces(query:String) = apiService.searchPlaces(query).await()

    /**
     * 搜索实时天气的API
     */
    suspend fun getRealtimeWeather(lng:String,lat:String) = apiService.getRealWeather(lng, lat).await()

    /**
     * 搜索最近天气的API
     */
    suspend fun getDailyWeather(lng:String,lat:String) = apiService.getDailyWeather(lng,lat).await()

    /**
     * suspend fun挂起函数 这里对所有Call<T>定义了一个扩展方法await,里面对数据进行了处理和封装
     * 使用协程的写法来简化Retrofit回调
     */
    private suspend fun <T> Call<T>.await():T{
        return suspendCoroutine {
                 enqueue(object :Callback<T>{
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val body = response.body()
                if(body != null){
                    // 请求成功，继续
                    it.resume(body)
                }else{
                    // 请求成功但无结果，继续
                    it.resumeWithException(RuntimeException("response body is null"))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                // 请求失败，返回错误信息
                it.resumeWithException(t)
            }
        })
        }

    }
}
