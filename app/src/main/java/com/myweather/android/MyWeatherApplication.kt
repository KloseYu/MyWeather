package com.myweather.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * 文件名: MyWeatherApplication
 * 作者: KloseYu
 * 日期: 2021/10/24
 * 描述: Application
 */
class MyWeatherApplication : Application(){
    companion object{
        
        //caiyun CN
        const val TOKEN ="8yTp8hXFWAxo0n5C"

        //weather UN
        const val TOKEN1="666f0d45761147d98fa232047231311"

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context =applicationContext
    }
}
