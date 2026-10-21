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