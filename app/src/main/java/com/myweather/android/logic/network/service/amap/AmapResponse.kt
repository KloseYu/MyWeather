package com.myweather.android.logic.network.service.amap

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// 高德行政区划查询响应
data class AmapDistrictResponse(
    val status: String? = null,
    val info: String? = null,
    val infocode: String? = null,
    val districts: List<AmapDistrict>? = null
)

// 行政区划条目
data class AmapDistrict(
    val adcode: String? = null,
    val name: String? = null,
    val center: String? = null,
    val level: String? = null
)

// 高德逆地理编码响应
data class AmapRegeoResponse(
    val status: String? = null,
    val info: String? = null,
    val regeocode: AmapRegeocode? = null
)

// 逆地理编码结果
data class AmapRegeocode(
    @SerializedName("formatted_address") val formattedAddress: String? = null,
    @SerializedName("addressComponent") val addressComponent: AmapAddressComponent? = null
)

// 地址组件，city 可能是 String 或 List（直辖市时为数组）
data class AmapAddressComponent(
    val province: String? = null,
    val city: JsonElement? = null,
    val district: String? = null,
    val adcode: String? = null
)