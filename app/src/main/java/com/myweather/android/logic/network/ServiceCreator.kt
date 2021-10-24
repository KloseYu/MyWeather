package com.myweather.android.logic.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 文件名:ServiceCreator
 * 作者: KloseYu
 * 日期: 2021/10/24
 * 描述: Retrofit构建器
 */
object ServiceCreator {

    private  const  val BASE_URL ="http://api.caiyunapp.com"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> create(serviceClass: Class<T>):T = retrofit.create(serviceClass)

    inline fun <reified T> create():T = create(T::class.java)

}