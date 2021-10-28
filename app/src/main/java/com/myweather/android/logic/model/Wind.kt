package com.myweather.android.logic.model



/**
 * 文件名: Wind
 * 作者: KloseYu
 * 日期: 2021/10/28
 * 描述: 风向与风力等级转换模块
 */

class WindOri(val ori: String)
class WindSpeed(val speed: String)

fun getWindOri(windOri: Double): WindOri {
    when (windOri) {
        in 11.26..56.25 -> {
            return WindOri("东北风")
        }
        in 56.26..78.75 -> {
            return WindOri("东风")
        }
        in 78.76..146.25 -> {
            return WindOri("东南风")
        }
        in 146.26..168.75 -> {
            return WindOri("南风")
        }
        in 168.76..236.25 -> {
            return WindOri("西南风")
        }
        in 236.26..258.75 -> {
            return WindOri("西风")
        }
        in 281.26..348.75 -> {
            return WindOri("西北风")
        }
        else -> return WindOri("北风")
    }
}

fun getWindSpeed(windSpeed: Double): WindSpeed {
    when (windSpeed) {
        in 1.0..5.0 -> {
            return WindSpeed(" 1级")
        }
        in 6.0..11.0 -> {
            return WindSpeed(" 2级")
        }
        in 12.0..19.0 -> {
            return WindSpeed(" 3级")
        }
        in 20.0..28.0 -> {
            return WindSpeed(" 4级")
        }
        in 29.0..38.0 -> {
            return WindSpeed(" 5级")
        }
        in 39.0..49.0 -> {
            return WindSpeed(" 6级")
        }
        in 50.0..61.0 -> {
            return WindSpeed(" 7级")
        }
        in 62.0..74.0 -> {
            return WindSpeed(" 8级")
        }
        in 75.0..88.0 -> {
            return WindSpeed(" 9级")
        }
        in 89.0..102.0 -> {
            return WindSpeed(" 10级")
        }
        in 103.0..117.0 -> {
            return WindSpeed(" 11级")
        }
        in 118.0..133.0 -> {
            return WindSpeed(" 12级")
        }
        in 134.0..149.0 -> {
            return WindSpeed(" 13级")
        }
        in 150.0..166.0 -> {
            return WindSpeed(" 14级")
        }
        in 167.0..183.0 -> {
            return WindSpeed(" 15级")
        }
        in 184.0..201.0 -> {
            return WindSpeed(" 16级")
        }
        in 202.0..220.0 -> {
            return WindSpeed(" 17级")
        }
        else -> return WindSpeed(" 0级")
    }

}