package com.myweather.android.logic.network.service.amap

import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.Location
import com.myweather.android.logic.model.Place
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// 高德文本搜索接口：城市关键词搜索
interface AmapTextSearchService {

    @GET("v5/place/text?key=${MyWeatherApplication.AMAP_KEY}")
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

    @GET("v3/geocode/regeo?key=${MyWeatherApplication.AMAP_KEY}")
    suspend fun reverseGeocode(
        @Query("location") location: String
    ): AmapResponse
}

// 高德行政区划接口：按关键词一次取回全国所有同名行政区
interface AmapDistrictService {

    @GET("v3/config/district?key=${MyWeatherApplication.AMAP_KEY}")
    suspend fun searchDistricts(
        @Query("keywords") keywords: String,
        @Query("subdistrict") subdistrict: Int,
        @Query("extensions") extensions: String
    ): AmapDistrictResponse
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
        /**
         * 高德对直辖市的 city 字段返回空数组 []，直接声明为 String 会导致 Gson 解析异常，
         * 因此统一用 JsonElement 接收，再安全地取出文本。
         */
        data class AddressComponent(
            val country: JsonElement? = null,
            val province: JsonElement? = null,
            val city: JsonElement? = null,
            val district: JsonElement? = null,
            val township: JsonElement? = null
        ) {
            fun text(element: JsonElement?): String? =
                if (element != null && element.isJsonPrimitive) {
                    element.asString.takeIf { it.isNotBlank() }
                } else null
        }
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
    val typecode: String?,
    @SerializedName("formatted_address") val formattedAddress: String?
) {
    private val coordinates: List<String>
        get() = location?.split(",")?.take(2) ?: listOf("0", "0")

    val lng: String get() = coordinates.getOrElse(0) { "0" }
    val lat: String get() = coordinates.getOrElse(1) { "0" }

    /**
     * 转为内部 Place 模型。
     *
     * 名称一律取行政区名（省 + 市 + 区/县）：城市搜索的候选只能是行政区，
     * 而这类 POI 的 name 是"鹿城区人民政府"之类的机构名，对用户没有意义。
     *
     * 地址一律只到行政区一级：天气预报按行政区发布，门牌号、街道对天气没有意义。
     */
    fun toPlace(): Place {
        val provinceName = pname?.takeIf { it.isNotBlank() }
        val cityName = cityname?.takeIf { it.isNotBlank() }
        val districtName = adname?.takeIf { it.isNotBlank() }
        val poiName = name?.trim()?.takeIf { it.isNotEmpty() }
        val adminName = buildDisplayName(provinceName, cityName, districtName, poiName)
        val displayName = adminName.ifEmpty { poiName.orEmpty() }

        val rawAddress = address?.takeIf { it.isNotBlank() }
            ?: formattedAddress?.takeIf { it.isNotBlank() }
            ?: ""
        val adminAddress = joinAdminLevels(
            listOfNotNull(provinceName, cityName, districtName).mapNotNull { adminLevel(it) }
        )
        val displayAddress = adminAddress.ifEmpty { rawAddress }

        return Place(
            name = displayName,
            location = Location(lng, lat),
            address = displayAddress
        )
    }
}

/** 逆地理编码结果的 Place 转换 */
fun AmapResponse.Regeocode.toPlace(lng: String, lat: String): Place {
    val comp = addressComponent
    // 与搜索路径保持一致：优先区/县/县级市，直辖市 city 为空时也能拿到区级名称
    val provinceName = comp?.text(comp.province)
    val cityName = comp?.text(comp.city)
    val districtName = comp?.text(comp.district)
    val name = buildDisplayName(provinceName, cityName, districtName, null)
    // 与搜索路径同规则：地址截到行政区一级，formatted_address 里的街道门牌一律不用。
    // 只有在拿不到任何行政区字段时才退回原始地址。
    val adminAddress = joinAdminLevels(
        listOfNotNull(provinceName, cityName, districtName).mapNotNull { adminLevel(it) }
    )
    return Place(
        name = name,
        location = Location(lng, lat),
        address = adminAddress.ifEmpty {
            formattedAddress?.takeIf { it.isNotBlank() } ?: name
        }
    )
}

/**
 * 高德用这些名称占位表示"没有这一级行政区"，不能当作城市名展示。
 */
private val PLACEHOLDER_LEVELS = setOf(
    "市辖区",
    "省直辖县级行政区划",
    "自治区直辖县级行政区划",
    "直辖县级行政区划"
)

/**
 * 构造展示用城市名，按国内"省 + 市 + 区/县"的书写习惯由粗到细拼接。
 * - 天气预报是按区县发布的，同一个市下的城区、郊区、各个区县都是独立区域，
 *   因此只要取到了区县级名称就保留到区县，不因为搜索词是城市名就退化成城市级
 * - 高德对直辖市与省直辖县级市不返回市级，拼接后自然得到"北京市朝阳区""湖北省仙桃市"
 * - 相邻层级重名只保留一次，避免出现"北京市北京市朝阳区"
 * - 港澳去掉"特别行政区"后缀，避免"香港特别行政区油尖旺区"这种过长名称
 * - 区县级取不到（为空、只是"市辖区"这类占位名、或只是镇/街道）时退到市级，再退到省级，最后用 POI 名
 */
private fun buildDisplayName(
    provinceName: String?,
    cityName: String?,
    districtName: String?,
    poiName: String?
): String {
    val province = adminLevel(provinceName)?.removeSuffix("特别行政区")
    val city = adminLevel(cityName)
    val district = adminLevel(districtName)
    return joinAdminLevels(listOfNotNull(province, city, district))
        .ifEmpty { poiName?.trim().orEmpty() }
}

/**
 * 清洗单级行政区名：去掉空白、占位名称，以及镇/街道这一级。
 *
 * 展示粒度以区县或县级市为下限，镇、街道不再往下展开：
 * 天气按区县发布，同一镇内不同地点拿到的是同一份预报，写出来只会撑长标题。
 *
 * 这一条同时解决了直筒子市的问题。东莞、中山、嘉峪关、儋州不设区县，
 * 高德返回的 adname 直接就是"南城街道"这类乡镇级名称，
 * 不过滤就会拼成"广东省东莞市南城街道"，凭空多出一级行政区。
 * 过滤后自然收敛成"广东省东莞市"，与当地实际的行政层级一致。
 */
private fun adminLevel(name: String?): String? =
    name?.trim()?.takeIf {
        it.isNotEmpty() && it !in PLACEHOLDER_LEVELS && !isTownshipLevel(it)
    }

/**
 * 乡镇级行政区判定：街道、镇、乡、苏木。
 * 只按后缀判断，不涉及任何具体地名，所以全国各地与港澳台都适用。
 */
private fun isTownshipLevel(name: String): Boolean =
    TOWNSHIP_SUFFIXES.any { name.endsWith(it) }

private val TOWNSHIP_SUFFIXES = listOf("街道", "镇", "乡", "苏木")

/**
 * 港澳台判定：这些地区国内地图的数据更新明显滞后，行政区划变动常常跟不上，
 * 而 Google 的本地数据更及时，因此这类查询应该主动改走 Google，
 * 而不是等国内地图返回空结果才兜底——它们往往"有数据，只是数据旧"。
 *
 * 只判断名称开头：行政区名由粗到细拼接，省级在最前，
 * 所以港澳台的记录必然以"香港""澳门""台湾"开头。
 * 不用 contains，避免把内地城市里的"香港路""澳门路"这类地名误判成港澳。
 */
fun isHmtRegion(vararg names: String?): Boolean =
    names.any { raw ->
        val name = raw?.trim().orEmpty()
        HMT_PREFIXES.any { name.startsWith(it) }
    }

private val HMT_PREFIXES = listOf("香港", "澳门", "澳門", "台湾", "台灣")

/**
 * 中国大陆省级行政区全称。
 *
 * 判断一条高德结果是否在国内，只能靠这个集合，不能靠名称后缀：
 * 海外的一级行政区中文名同样以"省""州"结尾，印尼的"东加里曼丹省"、
 * 加拿大的"安大略省"都会被后缀规则误判成国内。
 * 这正是努山塔拉这类新地名出问题的原因——高德若把新首都挂在旧的省级名称下，
 * 后缀判断会认为它在国内，于是永远轮不到 Google。
 *
 * 这份清单只包含国内自己的省级单位，属于固定的行政区划事实，
 * 不涉及任何海外地名的书写习惯，因此不随地区变化而失效。
 * 港澳台不在其中：它们的 country 字段同样是"中国"，
 * 但数据时效性差，按既定策略一律交给 Google，由 isHmtRegion 单独识别。
 */
private val DOMESTIC_PROVINCES = setOf(
    "北京市", "天津市", "河北省", "山西省", "内蒙古自治区", "辽宁省",
    "吉林省", "黑龙江省", "上海市", "江苏省", "浙江省", "安徽省",
    "福建省", "江西省", "山东省", "河南省", "湖北省", "湖南省",
    "广东省", "广西壮族自治区", "海南省", "重庆市", "四川省", "贵州省",
    "云南省", "西藏自治区", "陕西省", "甘肃省", "青海省",
    "宁夏回族自治区", "新疆维吾尔自治区"
)

/** 省级字段是否为国内省级行政区；空值与任何海外名称都返回 false */
private fun isDomesticProvince(name: String?): Boolean =
    name?.trim() in DOMESTIC_PROVINCES

/**
 * 省级名称是否属于中国大陆。
 *
 * 高德只负责大陆：港澳台与海外一律交给 Google，避免用高德滞后的境外数据当候选。
 * 判定只看省级名称，不看名称后缀——"台湾省"以"省"结尾但不是大陆省份，
 * 海外的"东加里曼丹省""安大略省"同理。
 * 省名为空时无法判定，按非大陆处理，宁可跳过该条也不展示归属不明的数据。
 */
fun isMainlandProvince(province: String?): Boolean {
    val name = province?.trim().orEmpty()
    if (name.isEmpty()) return false
    if (isHmtRegion(name)) return false
    return isDomesticProvince(name)
}

/** 一条高德搜索结果是否属于中国大陆，判定依据是它的省级字段 pname */
fun AmapPOI.isMainlandPoi(): Boolean = isMainlandProvince(pname)

/**
 * 高德搜索结果是否应交给 Google 处理（港澳台与海外）。
 *
 * 判定只用高德返回的省级字段 pname，不依赖查询文本：
 * - pname 为空 → 是。国内高德数据必定带省级字段，为空说明这条记录不在国内
 * - pname 命中港澳台 → 是
 * - pname 不是国内省级行政区 → 是，海外省级名称一律落在这里
 */
fun shouldUseGoogle(pois: List<AmapPOI>): Boolean {
    val pname = pois.firstOrNull()?.pname?.trim().orEmpty()
    if (pname.isEmpty()) return true
    if (isHmtRegion(pname)) return true
    return !isDomesticProvince(pname)
}

/**
 * 高德逆地理编码结果是否应交给 Google 处理（港澳台与海外）。
 *
 * 逆地理编码直接返回 country 字段，比搜索路径可靠：
 * - country 明确不是"中国" → 是
 * - country 是"中国"但省级命中港澳台 → 是
 * - country 缺失时用省级字段兜底，不在国内集合即视为境外
 */
fun shouldUseGoogle(regeocode: AmapResponse.Regeocode): Boolean {
    val component = regeocode.addressComponent ?: return true
    val country = component.text(component.country)
    val province = component.text(component.province)
    if (country != null && country != DOMESTIC_COUNTRY) return true
    if (isHmtRegion(province)) return true
    if (country == null && province != null) return !isDomesticProvince(province)
    return false
}

private const val DOMESTIC_COUNTRY = "中国"

/** 由粗到细拼接各级行政区名，跳过与上一级重复的层级 */
private fun joinAdminLevels(levels: List<String>): String {
    val parts = mutableListOf<String>()
    levels.forEach { level ->
        if (parts.lastOrNull() != level) parts.add(level)
    }
    return parts.joinToString("")
}

/**
 * 行政区名后缀，用于把"温州市""鹿城区"还原成用户输入时的写法。
 * 必须包含省级后缀：省市同名的地方（吉林省 / 吉林市）去掉后缀后都是"吉林"，
 * 这样无论用户输入"吉林""吉林市"还是"吉林省"，都能落到同一条记录上。
 * 顺序按长度从长到短，避免"特别行政区"被"区"抢先截断成"特别行政"。
 */
private val ADMIN_SUFFIXES = listOf(
    "特别行政区", "自治区", "自治州", "自治县", "地区", "盟", "省", "市", "区", "县"
)

/** 去掉行政区后缀，得到用户可能输入的核心词；"温州市"与"温州"应视为同一个词 */
fun adminKey(name: String): String =
    ADMIN_SUFFIXES
        .firstOrNull { name.endsWith(it) && name.length > it.length }
        ?.let { name.removeSuffix(it) }
        ?: name

/**
 * 区划条目对应的行政级别。
 * 高德 district 接口的 level 只有 province / city / district 三种取值。
 */
enum class DistrictLevel { PROVINCE, CITY, DISTRICT }

fun districtLevelOf(level: String?): DistrictLevel = when (level) {
    "province" -> DistrictLevel.PROVINCE
    "city" -> DistrictLevel.CITY
    else -> DistrictLevel.DISTRICT
}

/**
 * 该区划条目是否属于天气预报的范围。
 *
 * 区划接口的 level 除省、市、区县外还有 street（街道、镇），
 * 天气按行政区发布，街道和镇没有独立的天气预报，不能作为候选展示。
 *
 * 判断只认 level 字段，不认名字里有没有"新区"：
 * 浦东新区、滨海新区、两江新区是正式的市辖区，区划里 level=district，照常放行；
 * 高新区、开发区、经开区只是经济功能区，不是行政区，区划里 level=street，
 * 正好被这里挡住。靠后缀判断会把它们混成区级，所以不要改成按名字匹配。
 */
fun isWeatherAdminLevel(level: String?): Boolean =
    level == "province" || level == "city" || level == "district"

/**
 * 由一条区划条目构造展示记录。
 *
 * 名称按区划自身的级别截取：省级只到省，市级到"省 + 市"，区县级到"省 + 市 + 区/县"。
 * 不能一律取到区县——朝阳市的中心点落在双塔区，全取会拼成"辽宁省朝阳市双塔区"，
 * 而用户搜的是朝阳市本身。
 *
 * 自身那一级一律取区划条目的名称，不用逆地理编码的对应字段：
 * 区划条目是本次匹配的依据，逆地理编码的市级字段在省直辖县级市等情况下是占位名。
 *
 * 坐标用区划中心点，它就是该行政区的行政中心，
 * 不会像文本搜索那样把首条结果落在飞地或郊区。
 */
fun districtToPlace(
    district: AmapDistrict,
    province: String?,
    city: String?,
    county: String?
): Place? {
    val center = district.center?.split(',') ?: return null
    if (center.size < 2) return null
    val lng = center[0].trim()
    val lat = center[1].trim()
    if (lng.isEmpty() || lat.isEmpty()) return null

    val provinceName = adminLevel(province)?.removeSuffix("特别行政区")
    val cityName = adminLevel(city)
    val ownName = adminLevel(district.name)
    val name = when (districtLevelOf(district.level)) {
        DistrictLevel.PROVINCE -> provinceName ?: ownName.orEmpty()
        DistrictLevel.CITY -> joinAdminLevels(listOfNotNull(provinceName, ownName ?: cityName))
        DistrictLevel.DISTRICT ->
            joinAdminLevels(listOfNotNull(provinceName, cityName, ownName ?: adminLevel(county)))
    }
    if (name.isEmpty()) return null
    return Place(name, Location(lng, lat), name)
}

/**
 * 高德 POI 类型码：政府机构。
 * 只取政府类 POI，避免"温州市人民政府"命中同名的酒店、广场或小区。
 */
const val GOVERNMENT_TYPES = "130100"

/**
 * 高德 POI 类型码：行政区划。
 *
 * 天气是按行政区发布的，所以搜索结果的候选范围只能是行政区，
 * 商场、饭店、车站、桥梁这些 POI 即使名字里带城市名，也不是能查天气的地方。
 *
 * - 190102 省级（直辖市也归在这一级）
 * - 190103 城市
 * - 190104 市辖区、地级市
 * - 190105 县级市、县
 */

/** 单条 POI 是否属于行政区划，判断依据是类型码而不是名字 */
fun AmapPOI.isAdminDivision(): Boolean =
    typecode?.split('|')?.any { it.trim() in ADMIN_TYPECODES } == true

private val ADMIN_TYPECODES = setOf("190102", "190103", "190104", "190105")

/**
 * 搜索收敛结果。
 *
 * places 是最终展示给用户的列表。
 * cityAnchorQuery 非空时表示这次是一条城市级记录，还需要用市政府坐标校准：
 * 用户只输入城市名时，高德返回的第一条 POI 可能落在城区边缘、开发区甚至飞地，
 * 天气就会按那个偏远区县发布。改用市政府位置后，坐标落在该市行政中心，
 * 既符合"搜城市"的预期，也避免出现山福那种飞地郊区压过主城区的情况。
 */
data class CollapsedSearch(
    val places: List<Place>,
    val cityAnchorQuery: String? = null,
    val anchorRegion: String = ""
)

/**
 * 按输入收敛搜索结果，同名行政区并列保留。
 *
 * 高德文本搜索对"温州"这类城市名会返回十几条区县、街道级 POI，
 * 让用户在一堆鹿城区、瓯海区里挑一个，这不是搜城市该有的体验。
 * 因此这里按输入本身的粒度收敛：
 * - 输入是城市名 → 只留城市级记录，并给出市政府校准线索
 * - 输入是区县名 → 只留区县级记录
 * - 输入是具体地点（如"温州南站"）→ 不收敛，保持 POI 原貌
 *
 * 但同名的地方必须一条不漏地并列出来。吉林既是省名也是市名，
 * 朝阳既是辽宁的市、也是北京的区、还是长春的区，这类同名行政区
 * 在全国有很多。收敛成一条就等于替用户做了选择，而他真正要的那个
 * 往往就被这一下吞掉了，所以同名的每一条都要保留，
 * 并靠省名把它与其他同名地方区分开。
 *
 * 名称与地址保持同值：天气页标题显示地址，侧边栏灰色行也显示地址，
 * 两边同源同值；侧边栏上方显示名称，用户看到的仍是自己搜的那个地方。
 */
fun collapseByQueryGranularity(query: String, pois: List<AmapPOI>): CollapsedSearch {
    val raw = query.trim()
    if (raw.isEmpty()) return CollapsedSearch(emptyList())
    val key = adminKey(raw)

    // 与输入同名的行政区全部留下，按完整层级去重
    val sameName = pois.filter { it.matchesAdminKey(key) }
        .distinctBy { fullAdminNameOf(it) }

    if (sameName.isEmpty()) {
        // 省名本身不是 POI：搜"海南"高德只返回三亚市和它的几个区，
        // 但它们的省级字段都写着"海南省"，据此把结果收回到省
        val provinceName = pois.mapNotNull { adminLevel(it.pname) }.distinct().singleOrNull()
        if (provinceName != null && adminKey(provinceName) == key) {
            val name = provinceName.removeSuffix("特别行政区")
            return CollapsedSearch(
                places = listOf(Place(name, Location(pois.first().lng, pois.first().lat), name)),
                cityAnchorQuery = "${provinceName}人民政府"
            )
        }
        // 命中的都不是与输入同名的行政区，说明用户搜的是具体地点（车站、商场、小区）。
        // 天气按行政区发布，这类地点本身不是天气预报的范围，不能作为候选展示，
        // 所以直接判定为没有结果，而不是拿它们所属的区县凑数。
        return CollapsedSearch(emptyList())
    }

    // 只有一条同名结果时才做市政府校准。
    // 多条并列时每条代表不同城市，统一锚到一个点会把它们全挪到同一个地方
    if (sameName.size == 1) {
        val only = sameName.first()
        val place = adminPlaceOf(only)
        // 县级记录（区、县、县级市）的坐标本身就是区县政府位置，不需要再校准。
        // 拿所属地级市的市政府去锚，会把凌源市、朝阳县这类地方挪到地级市中心
        if (only.selfAdminLevel() == AdminLevel.COUNTY) {
            return CollapsedSearch(places = listOf(place))
        }
        val cityPart = cityPartOf(only)
        return CollapsedSearch(
            places = listOf(place),
            cityAnchorQuery = cityPart?.let { "${it}人民政府" },
            anchorRegion = cityPart.orEmpty()
        )
    }

    return CollapsedSearch(sameName.map { adminPlaceOf(it) })
}

/**
 * 这条记录是否与输入同名。
 *
 * 只看记录自身的名称。省市县三级字段都不能用：
 * - "凌源市"的 cityname 写着"朝阳市"，用 cityname 判断会把朝阳市下辖的每个县市
 *   都当成"朝阳"的同名结果，用户搜朝阳就会看到一堆自己不认识的县
 * - "吉林省"的 cityname 写着"长春市"，用 cityname 判断又会把省名整条漏掉
 *
 * 高德返回的行政区 POI，名称就是它自己的行政区名（"吉林省""朝阳区""朝阳市"），
 * 所以拿名称去掉后缀与输入比对，既不会漏也不会多。
 */
private fun AmapPOI.matchesAdminKey(key: String): Boolean =
    adminKey(adminLevel(name).orEmpty()) == key

/** 记录自身所处的行政层级，决定按哪一级拼接展示名 */
private enum class AdminLevel { PROVINCE, CITY, COUNTY }

/**
 * 判断这条记录自己属于哪一级。
 *
 * 判据是"记录自身的名称出现在哪一级字段里"，而不是"名称是否以市结尾"。
 * 县、县级市、市辖区在行政区划上就是同一级，只是叫法不同：
 * 凌源市、朝阳县、朝阳区都属县级。若按后缀判断，凌源市会被当成地级市，
 * 拼出"辽宁省朝阳市"，既吞掉了它所属的地级市，也让它和朝阳县撞成同一个名字。
 *
 * 按由粗到细的顺序匹配：省级字段、市级字段、区县字段。
 * 直辖市（北京市）的省级字段就是它自己，因此归到省级，展示为"北京市"。
 */
private fun AmapPOI.selfAdminLevel(): AdminLevel {
    val self = adminLevel(name)
    if (self != null) {
        if (adminLevel(pname) == self) return AdminLevel.PROVINCE
        if (adminLevel(cityname) == self) return AdminLevel.CITY
        if (adminLevel(adname) == self) return AdminLevel.COUNTY
    }
    // 字段对不上时退回类型码，只用来兜底识别省级，其余一律按县级
    return if (typecode?.split('|')?.any { it.trim() == "190102" } == true) {
        AdminLevel.PROVINCE
    } else {
        AdminLevel.COUNTY
    }
}

/**
 * 由一条行政区 POI 构造记录，名称一律带完整层级。
 *
 * 同名行政区只能靠省名区分：北京的朝阳区和长春的朝阳区都叫"朝阳区"，
 * 写成"北京市朝阳区"和"吉林省长春市朝阳区"才能让用户分清。
 */
private fun adminPlaceOf(poi: AmapPOI): Place = when (poi.selfAdminLevel()) {
    AdminLevel.PROVINCE -> {
        val name = adminLevel(poi.pname)?.removeSuffix("特别行政区")
            ?: adminLevel(poi.name).orEmpty()
        Place(name, Location(poi.lng, poi.lat), name)
    }
    AdminLevel.CITY -> buildCityLevelPlace(poi)
    AdminLevel.COUNTY -> buildCountyLevelCityPlace(poi)
}

/** 取市级名称：省直辖县级市的 cityname 是占位名，已被过滤，此时退到区县或 POI 名 */
private fun cityPartOf(poi: AmapPOI): String? =
    adminLevel(poi.cityname) ?: adminLevel(poi.adname) ?: adminLevel(poi.name)

/**
 * 由一条城市级 POI 构造记录。
 *
 * 名称与地址都只到城市级。用户只输入城市名时，他关心的是"这个城市今天什么天气"，
 * 而不是市政府所在哪个区，因此展示地址直接取城市名：
 * - 天气页标题取地址，于是显示城市名，与用户输入的城市一致
 * - 侧边栏灰色行也取地址，与城市名相同，不会出现"上面城市、下面区县"的两行错位
 *
 * 省市同名（吉林省 / 吉林市）在这里不会出问题：joinAdminLevels 只跳过
 * "完整名称相同"的相邻层级，所以"北京市"+"北京市"会合并成"北京市"，
 * 而"吉林省"+"吉林市"会原样拼成"吉林省吉林市"。
 */
fun buildCityLevelPlace(poi: AmapPOI): Place {
    val province = adminLevel(poi.pname)?.removeSuffix("特别行政区")
    val cityPart = cityPartOf(poi)
    val cityName = joinAdminLevels(listOfNotNull(province, cityPart))
        .ifEmpty { poi.name?.trim().orEmpty() }
    return Place(cityName, Location(poi.lng, poi.lat), cityName)
}

/**
 * 由一条区县 / 县级市 POI 构造记录。
 *
 * 名称与地址都取完整层级：省 + 地级市 + 区县。
 * 同名区县靠省名区分（北京市朝阳区 / 吉林省长春市朝阳区），
 * 县级市也要保留中间的地级市，否则不同地级市下的同名县级市无法区分，
 * 也会丢掉用户熟悉的"省市区县"读法。
 */
fun buildCountyLevelCityPlace(poi: AmapPOI): Place {
    val province = adminLevel(poi.pname)?.removeSuffix("特别行政区")
    val city = adminLevel(poi.cityname)
    // 区县名取不到时退回 POI 名：县级市记录偶尔不给 adname，
    // 而它的 name 就是"凌源市"，漏掉这一级就只剩"辽宁省朝阳市"，看不出是哪个县级市
    val county = adminLevel(poi.adname) ?: adminLevel(poi.name)
    val name = joinAdminLevels(listOfNotNull(province, city, county))
        .ifEmpty { poi.name?.trim().orEmpty() }
    return Place(name, Location(poi.lng, poi.lat), name)
}

/**
 * 由 POI 拼出完整行政区层级，只用于校验锚点是否落在同一个行政区。
 */
fun fullAdminNameOf(poi: AmapPOI): String {
    val province = adminLevel(poi.pname)?.removeSuffix("特别行政区")
    return joinAdminLevels(
        listOfNotNull(province, adminLevel(poi.cityname), adminLevel(poi.adname))
    )
}