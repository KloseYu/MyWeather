package com.myweather.android.logic.network

import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.*
import com.myweather.android.logic.network.service.amap.AmapServiceCreator
import com.myweather.android.logic.network.service.amap.CollapsedSearch
import com.myweather.android.logic.network.service.amap.GOVERNMENT_TYPES
import com.myweather.android.logic.network.service.amap.collapseByQueryGranularity
import com.myweather.android.logic.network.service.amap.fullAdminNameOf
import com.myweather.android.logic.network.service.amap.isHmtRegion
import com.myweather.android.logic.network.service.amap.shouldUseGoogle
import com.myweather.android.logic.network.service.amap.toPlace
import com.myweather.android.logic.network.service.caiyun.WeatherService
import com.myweather.android.logic.network.service.caiyun.CaiYunServiceCreator
import com.myweather.android.logic.network.service.google.GoogleService
import com.myweather.android.logic.network.service.google.GoogleServiceCreator
import com.myweather.android.logic.network.service.google.toPlace as googleResultToPlace
import kotlinx.coroutines.Dispatchers
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

    // 高德逆地理编码接口
    private val amapReverseGeocodeService = AmapServiceCreator.createReverseGeocodeService()

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
                // 按输入粒度收敛：搜城市名只给一条城市级结果，不把区县铺给用户挑
                amapPlaces = anchorToCityCenter(collapseByQueryGranularity(query, amapResult.pois))
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