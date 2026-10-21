package com.myweather.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * 全局 Application，持有全局 Context 和 API 密钥常量。
 * 在 AndroidManifest.xml 中通过 android:name 注册。
 */
class MyWeatherApplication : Application() {
    companion object {
        const val CaiYun_Key = "8yTp8hXFWAxo0n5C"
        const val AMAP_KEY = "674cb69f17c648a6028a75c6bff8037c"
        const val GOOGLE_KEY = " " //如需使用google位置服务，需在google console中创建项目并获取api key

        @SuppressLint("staticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}