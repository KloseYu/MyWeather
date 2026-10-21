package com.myweather.android.logic.model

// 格式化湿度显示文字
fun formatHumidityText(humidity: Double): String {
    val percent = (humidity * 100).toInt()
    return "湿度 $percent%"
}