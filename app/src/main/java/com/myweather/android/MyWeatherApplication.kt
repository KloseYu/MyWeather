package com.myweather.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * 全局 Application，持有全局 Context 和 API 密钥常量。
 * 在 AndroidManifest.xml 中通过 android:name 注册。
 *
 * API Key 配置说明：
 * - [CaiYun_Key]：彩云天气 API Key，需在 https://www.caiyunapp.com/h5/ 申请
 * - [AMAP_KEY]：高德逆地理编码 Key，需在 https://lbs.amap.com/ 申请
 * - [GOOGLE_KEY]：Google Geocoding API Key，需在 Google Cloud Console 申请
 */
class MyWeatherApplication : Application() {
    companion object {
        const val CaiYun_Key = "YOUR_CAIYUN_KEY"
        const val AMAP_KEY = "YOUR_AMAP_KEY"
        const val GOOGLE_KEY = " " //如需使用google位置服务，需在google console中创建项目并获取api key

        @SuppressLint("staticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}