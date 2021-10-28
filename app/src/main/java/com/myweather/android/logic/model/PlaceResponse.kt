package com.myweather.android.logic.model

import com.google.gson.annotations.SerializedName

/**
 * 文件名: PlaceResponse
 * 作者: KloseYu
 * 日期: 2021/10/24
 * 描述: 全球城市数据模型
 */
data class PlaceResponse(
    val status:String,
    val places:List<Place>,
){
    data class Place(
        val name:String,
        val location: Location,
        @SerializedName("formatted_address") val address:String,
    ){
        data class Location(
            val lng:String,
            val lat :String,
        )
    }
}