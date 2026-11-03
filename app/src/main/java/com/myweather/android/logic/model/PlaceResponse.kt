package com.myweather.android.logic.model

import com.google.gson.annotations.SerializedName

// 城市搜索结果
data class Place(
    val name: String,
    val location: Location,
    @SerializedName("formatted_address") val address: String
)

// 经纬度位置
data class Location(val lng: String, val lat: String)

/**
 * 把地址裁剪到行政区一级，去掉街道、门牌号等细节。
 *
 * 搜索与定位返回的地址常精确到街道门牌（"浙江省温州市鹿城区南汇街道xx路123号"），
 * 但天气是按行政区发布的，门牌号对天气毫无意义，显示出来只会显得怪，
 * 而且会撑长标题。城市名本身就是从省到区县的完整层级路径，地址里必然包含它，
 * 因此只要地址里出现过城市名，就截到城市名结束为止，后面全部丢弃。
 * 地址里找不到城市名时（海外地址书写顺序不同、或只剩门牌号），
 * 直接退回城市名，同样保证不会把门牌号显示出来。
 *
 * 这条规则只看「地址是否包含城市名」，不依赖任何国家/地区的地址格式，
 * 国内、海外、港澳台都适用。
 */
fun Place.normalizedAddress(): String {
    val name = name.trim()
    val address = address.trim()
    if (address.isEmpty()) return name
    if (name.isEmpty()) return address
    val index = address.indexOf(name)
    return if (index >= 0) address.substring(0, index + name.length) else name
}