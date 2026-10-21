package com.myweather.android.logic.model

// 蒲福风级描述
data class BeaufortLevel(val level: Int, val name: String)

// 根据角度返回中文风向（北/东北/东/东南/南/西南/西/西北）
fun getWindDirection(degrees: Double): String {
    val dirs = arrayOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    val index = ((degrees + 22.5) % 360 / 45).toInt()
    return dirs[index]
}

// 根据风速(km/h)返回蒲福风级
fun getBeaufortLevel(speed: Double): BeaufortLevel {
    val levels = arrayOf(
        BeaufortLevel(0, "无风"), BeaufortLevel(1, "软风"), BeaufortLevel(2, "轻风"), BeaufortLevel(3, "微风"),
        BeaufortLevel(4, "和风"), BeaufortLevel(5, "清风"), BeaufortLevel(6, "强风"), BeaufortLevel(7, "疾风"),
        BeaufortLevel(8, "大风"), BeaufortLevel(9, "烈风"), BeaufortLevel(10, "狂风"), BeaufortLevel(11, "暴风"),
        BeaufortLevel(12, "热带气旋")
    )
    val thresholds = doubleArrayOf(1.0, 6.0, 12.0, 20.0, 29.0, 39.0, 50.0, 62.0, 75.0, 89.0, 103.0, 118.0)
    for (i in thresholds.indices) {
        if (speed < thresholds[i]) return levels[i]
    }
    return levels[12]
}

// 组合风向、风级和风速为显示文字
fun formatWindText(direction: Double, speed: Double): String {
    val dir = getWindDirection(direction)
    val level = getBeaufortLevel(speed)
    return "风力 $dir ${level.level}级 ${formatTempValue(speed)} km/h"
}