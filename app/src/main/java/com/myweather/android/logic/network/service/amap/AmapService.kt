package com.myweather.android.logic.network.service.amap

import com.myweather.android.logic.model.Location
import com.myweather.android.logic.model.Place
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// 高德文本搜索接口：城市关键词搜索
interface AmapTextSearchService {

    @GET("v5/place/text?key=${AmapApiKey.KEY}")
    suspend fun searchCities(
        @Query("keywords") keywords: String,
        @Query("types") types: String,
        @Query("region") region: String,
        @Query("offset") offset: Int,
        @Query("page") page: Int
    ): AmapResponse
}

// 高德逆地理编码接口：经纬度 → 地址
interface AmapReverseGeocodeService {

    @GET("v3/geocode/regeo?key=${AmapApiKey.KEY}")
    suspend fun reverseGeocode(
        @Query("location") location: String
    ): AmapResponse
}

// 高德 API Key
object AmapApiKey {
    const val KEY = "674cb69f17c648a6028a75c6bff8037c"
}

/**
 * 高德 API 通用响应。
 * regeocode 字段仅在逆地理编码响应中存在。
 */
data class AmapResponse(
    val status: String,
    val regeocode: Regeocode? = null,
    val pois: List<AmapPOI> = emptyList()
) {
    data class Regeocode(
        @SerializedName("formatted_address") val formattedAddress: String?,
        val addressComponent: AddressComponent?
    ) {
        data class AddressComponent(val city: String?, val district: String?, val township: String?)
    }
}

/** 高德 POI（搜索兴趣点） */
data class AmapPOI(
    val name: String?,
    val location: String?,
    val pname: String?,
    val cityname: String?,
    val adname: String?,
    val address: String?,
    @SerializedName("formatted_address") val formattedAddress: String?
) {
    private val coordinates: List<String>
        get() = location?.split(",")?.take(2) ?: listOf("0", "0")

    val lng: String get() = coordinates.getOrElse(0) { "0" }
    val lat: String get() = coordinates.getOrElse(1) { "0" }

    // 转为内部 Place 模型
    fun toPlace(): Place = Place(
        name = name ?: cityname ?: "",
        location = Location(lng, lat),
        address = address ?: formattedAddress ?: ""
    )
}

/** 逆地理编码结果的 Place 转换 */
fun AmapResponse.Regeocode.toPlace(lng: String, lat: String): Place {
    val comp = addressComponent
    val city = comp?.city?.takeIf { it.isNotEmpty() } ?: comp?.district ?: ""
    return Place(
        name = city,
        location = Location(lng, lat),
        address = formattedAddress ?: city
    )
}