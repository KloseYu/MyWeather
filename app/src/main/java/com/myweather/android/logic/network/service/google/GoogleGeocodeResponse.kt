package com.myweather.android.logic.network.service.google

import com.google.gson.annotations.SerializedName
import com.myweather.android.logic.model.Location
import com.myweather.android.logic.model.Place

// Google Geocoding API 响应体
data class GoogleGeocodeResponse(
    val status: String? = null,
    val results: List<GoogleGeocodeResult>? = null
)

// 单个地理编码结果
data class GoogleGeocodeResult(
    @SerializedName("formatted_address") val formattedAddress: String? = null,
    @SerializedName("address_components") val addressComponents: List<GoogleAddressComponent>? = null,
    val geometry: GoogleGeometry? = null
)

// 地址组件（long_name + types 用于提取城市名）
data class GoogleAddressComponent(
    @SerializedName("long_name") val longName: String? = null,
    @SerializedName("short_name") val shortName: String? = null,
    val types: List<String>? = null
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

/**
 * Google 地理编码结果 → 内部 Place 模型。
 * - 逆地理编码时传入原始 lng/lat，保证与已保存城市的去重逻辑一致
 * - 关键字搜索时不传经纬度，改用结果中 geometry 的坐标
 * - 城市名不做"上属地级市 + 区县"拼接，因为各地行政区划差异很大
 */
fun GoogleGeocodeResult.toPlace(
    lng: String? = null,
    lat: String? = null,
    fallbackName: String = ""
): Place? {
    val geoLocation = geometry?.location
    val finalLng = lng ?: geoLocation?.lng?.let { "%.6f".format(it) } ?: return null
    val finalLat = lat ?: geoLocation?.lat?.let { "%.6f".format(it) } ?: return null

    val components = addressComponents.orEmpty()
    fun findLongName(type: String): String? = components
        .firstOrNull { component -> component.types?.contains(type) == true }
        ?.longName
        ?.takeIf { it.isNotBlank() }

    val countryName = findLongName("country")
    // Google 的行政区类型标注不是全球统一标准：各层级在不同国家由不同数据源维护，
    // 名称、层级深度、甚至某个层级是否出现都不一致，同一国家内部也可能混用。
    // 因此这里不按名称判断、也不硬编码任何国家的规则，只按"由细到粗"逐级降级，
    // 取第一个存在且不等于国家名的层级。
    // 代价是显示粒度会随地区浮动：数据齐全时得到市/镇，数据稀疏时可能退到州或省，
    // 这是数据源本身的差异，不是逻辑错误。
    // 同理也不凭空补上级：吉隆坡、华盛顿特区这类嵌在州内或州间的直辖区，
    // 若数据只给了外围州名，就如实显示该州名，而不是拼成"雪兰莪吉隆坡"。
    // 名称一律取 longName 全称而非 shortName 缩写，同名地区（如华盛顿市与华盛顿州）
    // 由列表里的完整地址行区分，不靠拼接上级来制造唯一名。
    val nameTypes = listOf(
        "locality",                     // 城市/城镇
        "postal_town",                  // 邮政城镇
        "administrative_area_level_3",  // 三级行政区
        "administrative_area_level_2",  // 二级行政区
        "administrative_area_level_1",  // 一级行政区
        "sublocality"                   // 次级行政区，仅作兜底
    )
    val name = nameTypes.asSequence()
        .mapNotNull { findLongName(it) }
        .firstOrNull { it != countryName }
        ?: fallbackName.takeIf { it.isNotBlank() }
        ?: formattedAddress?.substringBefore(",")?.takeIf { it.isNotBlank() }
        ?: return null

    // 地址同样只保留行政区层级，不用 formatted_address：
    // 后者会带上街道、门牌号，而天气是按行政区发布的，门牌号对天气没有意义。
    // 这里按行政区组件由粗到细拼接，既不依赖各国的地址书写顺序，
    // 也不会出现"1600 Amphitheatre Parkway"这类纯街道内容。
    val addressLevels = listOf(
        "administrative_area_level_1",
        "administrative_area_level_2",
        "administrative_area_level_3",
        "locality",
        "sublocality_level_1"
    )
    val adminAddress = addressLevels
        .mapNotNull { findLongName(it) }
        .fold(mutableListOf<String>()) { acc, level ->
            if (acc.lastOrNull() != level) acc.add(level)
            acc
        }
        .joinToString("")

    return Place(
        name = name,
        location = Location(finalLng, finalLat),
        address = adminAddress.ifEmpty { name }
    )
}