package com.myweather.android.logic.model

/**
 * 温度格式化工具。
 * 支持摄氏度/华氏度，对美国/利比里亚/缅甸地区自动使用华氏度。
 */

// 判断经纬度是否在华氏度国家范围内
fun isFahrenheitCountry(lng: Double, lat: Double): Boolean {
    val inUS = lat in 24.0..50.0 && lng in -125.0..-66.0
    val inLiberia = lat in 4.0..9.0 && lng in -11.0..-7.0
    val inMyanmar = lat in 9.0..29.0 && lng in 92.0..102.0
    return inUS || inLiberia || inMyanmar
}

// 格式化温度数值，整数不显示小数位
fun formatTempValue(temp: Double): String {
    return if (temp == temp.toInt().toDouble()) {
        "${temp.toInt()}"
    } else {
        "%.1f".format(temp)
    }
}

fun formatCelsius(celsius: Double): String {
    return "${formatTempValue(celsius)} °C"
}

fun formatFahrenheit(celsius: Double): String {
    val f = celsius * 9.0 / 5.0 + 32.0
    return "${formatTempValue(f)} °F"
}

fun formatTemp(celsius: Double, useFahrenheit: Boolean): String {
    return if (useFahrenheit) formatFahrenheit(celsius) else formatCelsius(celsius)
}