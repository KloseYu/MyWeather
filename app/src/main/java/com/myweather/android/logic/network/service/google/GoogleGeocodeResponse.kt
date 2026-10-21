package com.myweather.android.logic.network.service.google

// Google Geocoding API 响应体
data class GoogleGeocodeResponse(
    val status: String? = null,
    val results: List<GoogleGeocodeResult>? = null
)

// 单个逆地理编码结果
data class GoogleGeocodeResult(
    val formatted_address: String? = null,
    val geometry: GoogleGeometry? = null
)

// 几何信息（经纬度）
data class GoogleGeometry(
    val location: GoogleLocation? = null
)

// 经纬度坐标
data class GoogleLocation(
    val lat: Double? = null,
    val lng: Double? = null
)