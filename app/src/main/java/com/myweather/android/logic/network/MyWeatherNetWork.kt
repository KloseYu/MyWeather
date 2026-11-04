package com.myweather.android.logic.network

import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.*
import com.myweather.android.logic.network.service.amap.AmapDistrict
import com.myweather.android.logic.network.service.amap.AmapPOI
import com.myweather.android.logic.network.service.amap.AmapServiceCreator
import com.myweather.android.logic.network.service.amap.CollapsedSearch
import com.myweather.android.logic.network.service.amap.GOVERNMENT_TYPES
import com.myweather.android.logic.network.service.amap.adminKey
import com.myweather.android.logic.network.service.amap.collapseByQueryGranularity
import com.myweather.android.logic.network.service.amap.districtLevelOf
import com.myweather.android.logic.network.service.amap.districtToPlace
import com.myweather.android.logic.network.service.amap.fullAdminNameOf
import com.myweather.android.logic.network.service.amap.isHmtRegion
import com.myweather.android.logic.network.service.amap.isMainlandProvince
import com.myweather.android.logic.network.service.amap.isWeatherAdminLevel
import com.myweather.android.logic.network.service.amap.shouldUseGoogle
import com.myweather.android.logic.network.service.amap.toPlace
import com.myweather.android.logic.network.service.caiyun.WeatherService
import com.myweather.android.logic.network.service.caiyun.CaiYunServiceCreator
import com.myweather.android.logic.network.service.google.GoogleService
import com.myweather.android.logic.network.service.google.GoogleServiceCreator
import com.myweather.android.logic.network.service.google.toPlace as googleResultToPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 统一网络访问入口。
 * - 彩云天气：实时/每日/逐小时天气数据（suspend 直接调用）
 * - 高德地图：城市搜索 + 逆地理编码（国内主路径）
 * - Google Geocoding：海外及港澳台地区的兜底路径
 */
object MyWeatherNetWork {

    // 高德城市搜索接口
    private val amapTextSearchService = AmapServiceCreator.createTextSearchService()

    /** 行政区名后缀，用来判断输入是否已经自带级别（"朝阳区"不必再展开） */
    private val ADMIN_SUFFIX_HINTS =
        listOf("自治区", "自治州", "特别行政区", "省", "市", "区", "县", "盟", "旗")

    /** 同名候选逐个补全省市名时的间隔，避免连续请求触发高德的 QPS 限流 */
    private const val SEARCH_GAP_MS = 400L

    // 高德逆地理编码接口
    private val amapReverseGeocodeService = AmapServiceCreator.createReverseGeocodeService()

    // 高德行政区划接口（同名行政区补全）
    private val amapDistrictService = AmapServiceCreator.createDistrictService()

    // Google 地理编码接口（海外兜底）
    private val googleService = GoogleServiceCreator.create<GoogleService>()

    // Google 返回结果的语言
    private const val GOOGLE_LANGUAGE = "zh-CN"

    // 彩云天气接口
    private val weatherService = CaiYunServiceCreator.create<WeatherService>()

    /** Google Key 是否可用；空白值代表尚未申请，兜底请求直接跳过，避免无效网络调用 */
    private fun isGoogleKeyAvailable(): Boolean = MyWeatherApplication.GOOGLE_KEY.isNotBlank()

    /**
     * 城市搜索：国内走高德 POI 文本搜索，港澳台与海外只交给 Google。
     *
     * 高德对港澳台及海外的数据更新滞后，经常"有结果但结果旧"，
     * 拿这些旧数据顶替，用户看到的行政区划是错的，比查不到更糟。
     * 所以海外结果只信任 Google：
     * - 查询文本是外文，或命中港澳台地名 → 直接走 Google
     * - 高德结果的省级字段不是国内行政区 → 丢弃高德结果，改用 Google
     * - Google 未配置或没有结果 → 返回空，不拿高德结果凑数
     */
    suspend fun searchPlaces(query: String): List<Place> {
        // 外文查询与港澳台地名不必先问高德，省去一次注定要丢弃的请求
        if (isHmtRegion(query) || isForeignScript(query)) {
            return searchPlacesByGoogle(query)
        }

        var amapPlaces = emptyList<Place>()
        var overseas = false
        try {
            val amapResult = amapTextSearchService.searchCities(query, "", "", 10, 1)
            if (amapResult.status == "1" && amapResult.pois.isNotEmpty()) {
                // 按输入粒度收敛，并把同名行政区全部并列：
                // 搜城市名只给城市级结果，不把区县铺给用户挑；
                // 但"朝阳""吉林"这类一名多指的地方，同名的每一条都不能少
                amapPlaces = resolveAdminSearch(query, amapResult.pois)
                // 归属判定要在收敛前做：收敛后的名称已改写成展示格式，
                // 原始 pname 才是判断"这条记录属于哪个国家"的依据
                overseas = shouldUseGoogle(amapResult.pois)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (amapPlaces.isEmpty()) return searchPlacesByGoogle(query)
        // 港澳台与海外一律交给 Google，高德的滞后数据不再作为兜底
        if (overseas) return searchPlacesByGoogle(query)
        return amapPlaces
    }

    /**
     * 查询文本是否不含中日韩文字，即外文输入。
     * 国内地名总以汉字书写，纯拉丁字母的查询几乎都是海外地名；
     * 拼音输入也会被送到 Google，而 Google 在 zh-CN 语言下同样返回中文名，不影响结果。
     */
    private fun isForeignScript(query: String): Boolean =
        query.none { it in '\u4e00'..'\u9fff' || it in '\u3040'..'\u30ff' }

    /**
     * 城市级结果改用市政府坐标。
     *
     * 用户只输入城市名时，高德返回的第一条 POI 可能落在城区边缘、开发区甚至飞地，
     * 天气就会按那个偏远区县发布，看起来就像"搜了温州市，天气却是山福的"。
     * 这里再查一次市政府（地级市查市人民政府，县级市查它自己的市人民政府），
     * 用行政中心的坐标作为这条城市记录的位置。
     *
     * 名称与地址保持收敛时算好的城市名，只替换坐标：
     * 用户搜的是城市，标题就该是城市名，不该变成市政府所在的那个区。
     *
     * 查不到市政府（数据缺失或网络异常）时保留原坐标，不影响搜索可用性。
     */
    private suspend fun anchorToCityCenter(collapsed: CollapsedSearch): List<Place> {
        val anchorQuery = collapsed.cityAnchorQuery ?: return collapsed.places
        val anchorPoi = try {
            val response = amapTextSearchService.searchCities(
                anchorQuery, GOVERNMENT_TYPES, collapsed.anchorRegion, 1, 1
            )
            if (response.status == "1") response.pois.firstOrNull() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return collapsed.places

        val original = collapsed.places.first()
        // 校验锚点确实落在同一个行政区：市政府查询可能命中别处的同名机构。
        // 城市名是"省+市"或"省+市+县级市"，锚点的完整层级必须包含它，
        // 否则宁可保留原坐标，也不把用户的城市换成另一个地方。
        val anchorName = fullAdminNameOf(anchorPoi)
        if (!anchorName.startsWith(original.name) && !original.name.startsWith(anchorName)) {
            return collapsed.places
        }
        return listOf(original.copy(location = Location(anchorPoi.lng, anchorPoi.lat)))
    }

    /**
     * 输入是否是一个"裸地名"：不带行政后缀的短词。
     * 这类词最容易一名多指（吉林、朝阳），也是唯一值得补搜的输入形态；
     * "朝阳区"这种自带后缀的输入已经足够精确，不必再展开。
     */
    private fun isBarePlaceName(query: String): Boolean =
        query.length in 2..4 && ADMIN_SUFFIX_HINTS.none { query.endsWith(it) }

    private fun needsSameNameExpansion(query: String): Boolean = isBarePlaceName(query)

    /**
     * 搜索结果的最终解析：收敛 + 同名补全。
     *
     * 裸地名（吉林、朝阳）交给 [expandSameNameByDistrict] 取回全国所有同名行政区，
     * 再与文本搜索的收敛结果合并去重。两套来源各有覆盖盲区：
     * 文本搜索按相关度排序，同名的省市县往往只回一部分；
     * 区划接口按行政区划索引，偶尔会漏掉个别条目。合并后一条不漏。
     *
     * 合并结果直接用区划中心点做坐标，不再查市政府：
     * 多条并列时每条代表不同城市，统一锚到一个点会把它们全挪到同一个地方。
     *
     * 没有同名冲突（只有一个候选）时仍走原来的收敛 + 市政府校准，行为保持不变。
     */
    private suspend fun resolveAdminSearch(query: String, pois: List<AmapPOI>): List<Place> {
        val collapsed = collapseByQueryGranularity(query, pois)
        if (!needsSameNameExpansion(query)) return anchorToCityCenter(collapsed)
        val expanded = expandSameNameByDistrict(query)
        if (expanded.isEmpty()) return anchorToCityCenter(collapsed)
        return (expanded + collapsed.places).distinctBy { it.name }
    }

    /**
     * 同名行政区补全，返回可并列展示的完整记录。
     *
     * 高德文本搜索把同一关键词的行政区压在一页里，同名的地方往往只回一部分：
     * 搜"朝阳"只出辽宁的朝阳市和朝阳县，北京的朝阳区、长春的朝阳区都拿不到；
     * 搜"吉林"只出吉林省，吉林市被省会挤掉。
     * 靠加后缀补搜也不可靠——"朝阳区"不带地域参数只回北京那一个。
     *
     * 行政区划接口能一次给出全国所有同名行政区（带 adcode、level 和中心点），
     * 再逐个逆地理编码补上省、市名，就能拼出彼此可区分的完整名称。
     *
     * 只有真的存在同名冲突（候选 ≥ 2）才走这条路；
     * 只有一个候选时仍用文本搜索 + 市政府校准，行为与之前保持一致。
     * 区划接口取不到同名结果（海外地名、具体地点名）时返回空，由调用方兜底。
     */
    private suspend fun expandSameNameByDistrict(query: String): List<Place> {
        val sameName = sameNameDistricts(query)
        if (sameName.size < 2) return emptyList()
        return districtsToPlaces(sameName)
    }

    /** 区划接口里与该输入核心词同名的行政区；接口异常或没有同名结果时返回空 */
    private suspend fun sameNameDistricts(query: String): List<AmapDistrict> = try {
        val response = amapDistrictService.searchDistricts(query, 0, "base")
        if (response.status == "1") {
            val key = adminKey(query)
            response.districts.orEmpty().filter { adminKey(it.name.orEmpty()) == key }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    /**
     * 把区划条目转成可展示记录。
     *
     * 只保留省、市、区县三级：街道与镇没有独立的天气预报，不能作为候选。
     * 由粗到细排列，用户先看到覆盖面大的那个。
     * 每条都要按中心点逆地理编码补全省、市名，否则"朝阳区"这类名字无法区分。
     *
     * 去重按 adcode，不按名称。adcode 是行政区划的唯一编号，
     * 而同名候选的展示名称依赖逆地理编码的结果——万一某条逆地理编码失败，
     * 它的名称会退化成只剩"朝阳区"，按名称去重就会把另一条真候选误删。
     * 按 adcode 去重能保证区划接口给出的每一条都如实并列展示。
     *
     * 只保留中国大陆的条目：港澳台与海外交给 Google，不用高德的滞后数据凑候选。
     * 判定依赖逆地理编码的省级字段，取不到省级字段时同样跳过——
     * 这种条目既无法确认归属，名称也无法与同名候选区分。
     */
    private suspend fun districtsToPlaces(districts: List<AmapDistrict>): List<Place> =
        districts
            .filter { isWeatherAdminLevel(it.level) }
            .distinctBy { it.adcode ?: it.name }
            .sortedBy { districtLevelOf(it.level).ordinal }
            .mapNotNull { district ->
                val center = district.center?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                delay(SEARCH_GAP_MS)
                val component = try {
                    val response = amapReverseGeocodeService.reverseGeocode(center)
                    if (response.status == "1") response.regeocode?.addressComponent else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                if (!isMainlandProvince(component?.text(component.province))) return@mapNotNull null
                districtToPlace(
                    district = district,
                    province = component?.text(component.province),
                    city = component?.text(component.city),
                    county = component?.text(component.district)
                )
            }

    // Google 关键字搜索（海外与港澳台主路径，国内仅作无结果兜底）
    private suspend fun searchPlacesByGoogle(query: String): List<Place> {
        if (!isGoogleKeyAvailable()) return emptyList()
        return try {
            val response = withContext(Dispatchers.IO) {
                googleService.geocode(query, GOOGLE_LANGUAGE, MyWeatherApplication.GOOGLE_KEY).execute()
            }
            val results = response.body()?.results
            if (response.isSuccessful && !results.isNullOrEmpty()) {
                results.mapNotNull { it.googleResultToPlace(fallbackName = query) }
                    // 天气按行政区发布，同一区域的多条结果天气完全相同，只保留一条
                    .distinctBy { it.address.ifEmpty { it.name } }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 逆地理编码：国内走高德 regeo，港澳台与海外只交给 Google。
     * 逆地理编码响应直接带 country 字段，比搜索路径的推断可靠得多。
     * 与搜索路径同一原则：Google 未配置或没有结果时返回 null，
     * 不拿高德的滞后区划顶替，避免显示错误的地名。
     */
    suspend fun reverseGeocode(lng: String, lat: String): Place? {
        var amapPlace: Place? = null
        var overseas = false
        try {
            val response = amapReverseGeocodeService.reverseGeocode("${lng},${lat}")
            val regeocode = response.regeocode
            if (response.status == "1" && regeocode != null) {
                amapPlace = regeocode.toPlace(lng, lat).takeIf { it.name.isNotBlank() }
                overseas = shouldUseGoogle(regeocode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 高德拿不到结果、或定位落在港澳台与海外，都交给 Google 处理
        if (amapPlace == null || overseas) return reverseGeocodeByGoogle(lng, lat)
        return amapPlace
    }

    // Google 逆地理编码兜底（参数顺序为 lat,lng）
    private suspend fun reverseGeocodeByGoogle(lng: String, lat: String): Place? {
        if (!isGoogleKeyAvailable()) return null
        return try {
            val response = withContext(Dispatchers.IO) {
                googleService.reverseGeocode("${lat},${lng}", GOOGLE_LANGUAGE, MyWeatherApplication.GOOGLE_KEY).execute()
            }
            val result = response.body()?.results?.firstOrNull()
            if (response.isSuccessful && result != null) {
                result.googleResultToPlace(lng = lng, lat = lat)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 实时天气（彩云）
    suspend fun getRealtimeWeather(lng: String, lat: String) =
        weatherService.getRealtimeWeather(lng, lat)

    // 每日预报（彩云）
    suspend fun getDailyWeather(lng: String, lat: String) =
        weatherService.getDailyWeather(lng, lat)

    // 24小时预报（彩云）
    suspend fun getHourlyWeather(lng: String, lat: String) =
        weatherService.getHourlyWeather(lng, lat)
}