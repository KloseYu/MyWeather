package com.myweather.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * 全局 Application，持有全局 Context。
 * 在 AndroidManifest.xml 中通过 android:name 注册。
 *
 * API Key 不再硬编码在源码里，改为由 app/build.gradle.kts 从 local.properties
 * 读取后注入 [BuildConfig]，对应字段为 [BuildConfig.CAIYUN_KEY]、
 * [BuildConfig.AMAP_KEY]、[BuildConfig.GOOGLE_KEY]。
 * local.properties 已被 .gitignore 忽略，Key 只留在本地，不会进入版本库。
 * 申请地址见 README。
 */
class MyWeatherApplication : Application() {
    companion object {
        @SuppressLint("staticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}