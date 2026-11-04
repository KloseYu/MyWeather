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
import com.myweather.android.logic.network.service.amap.isAdminDivision
import com.myweather.android.logic.network.service.amap.isHmtRegion
import com.myweather.android.logic.network.service.amap.isMainlandPoi
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
     * 城市搜索：只返回能查天气的行政区，港澳台与海外交给 Google。
     *
     * 搜索结果的第一原则是"这一行必须是一个行政区"：
     * 天气按行政区发布，所以商场、饭店、车站这类 POI 即使名字里带城市名，
     * 也不能作为候选展示——用户搜"shanghai"时高德返回的正是十几家英文招牌的店，
     * 把它们列出来既查不了天气，也让搜索结果看起来全是外文店名。
     * 因此候选一律按 POI 类型码筛选，只保留省级、市、区县这几级行政区。
     *
     * 高德对港澳台及海外的数据更新滞后，经常"有结果但结果旧"，
     * 拿这些旧数据顶替，用户看到的行政区划是错的，比查不到更糟。
     * 所以海外结果只信任 Google：
     * - 高德结果的省级字段不是国内行政区 → 丢弃高德结果，改用 Google
     * - Google 未配置或没有结果 → 返回空，不拿高德结果凑数
     *
     * 查询文本的语言不决定"是否海外"：拼音、英文输入一样先问高德，
     * 因为高德自己能识别拼音（"shanghai" 同样命中上海市），
     * 而"输入是外文"并不等于"用户要查的是海外城市"。
     *
     * 同理，"车站""商场"这类具体地点不是天气的范围，也不该被搜出来：
     * 天气预报按行政区发布，同一个区里的车站和商场拿到的是完全相同的天气，
     * 用户真正要的永远是一个能查天气的行政区。所以搜索只认行政区，
     * 命不中行政区的输入一律当作没有结果。
     */
    suspend fun searchPlaces(query: String): List<Place> {
        // 港澳台地名在国内地图上的行政区划明显滞后，Google 可用时直接改走 Google
        if (isGoogleKeyAvailable() && isHmtRegion(query)) {
            return searchPlacesByGoogle(query)
        }

        // 纯拉丁字母输入一律交给 Google。
        // 高德是中文地图，拼音和英文地名都命不中行政区：
        // beijing 能被高德直接认成北京市，shanghai 却只回一堆同名店名，
        // 靠"加行政区类型再搜一次"硬兜才勉强拿到上海市，同类输入行为完全不一致。
        // 国内用户查天气不会用拼音，海外地名本来也该走 Google，
        // 所以这里统一交出去，不再保留拼音补搜这条不确定的路径。
        if (isLatinQuery(query)) {
            return searchPlacesByGoogle(query)
        }

        // 主路径：不限类型搜一遍，城市名、区县名都能命中
        val pois = amapSearch(query, types = "")
        // 高德给出的是港澳台或海外记录时，国内数据不可信，改由 Google 兜底
        if (pois.isNotEmpty() && shouldUseGoogle(pois)) {
            return searchPlacesByGoogle(query)
        }

        // 行政区优先：命中行政区就只按行政区收敛，POI 与店名一律不参与展示
        val adminPois = pois.filter { it.isAdminDivision() && it.isMainlandPoi() }
        if (adminPois.isNotEmpty()) {
            return resolveAdminSearch(query, adminPois)
        }

        // 文本搜索彻底命不中行政区时，区划接口还能兜住一部分。
        // 宁波江北区这类区名，全量搜索只回同名的商业街、农贸市场、街道办事处，
        // 按行政区类型再搜更是零结果，但区划索引里它是有的。
        if (needsSameNameExpansion(query)) {
            val districtPlaces = districtsToPlaces(sameNameDistricts(query))
            if (districtPlaces.isNotEmpty()) return districtPlaces
        }

        // 高德始终命不中行政区，说明用户输入的是车站、商场这类具体地点。
        // 天气按行政区发布，这类地点本身不是天气预报的范围，搜出来也没有对应的天气，
        // 所以不拿它们所属的区县凑数，直接判定为没有结果，改由 Google 兜底海外地名。
        return searchPlacesByGoogle(query)
    }

    /**
     * 是否需要补全同名候选。
     *
     * 只有不带行政后缀的裸地名才会一名多指（吉林、朝阳），需要展开；
     * "朝阳区"这种自带后缀的输入已经足够精确，不必再展开。
     */
    private fun needsSameNameExpansion(query: String): Boolean = isBarePlaceName(query)

    /**
     * 行政区搜索的最终解析：收敛 + 同名补全。
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

    /**
     * 输入是否为纯拉丁字母（不含中日韩字符）。
     *
     * 这类输入在高德这种中文地图上不可靠：同为拼音，beijing 能直接命中北京市，
     * shanghai 却只能拿到一堆同名店名。所以不参与国内行政区匹配，统一交给 Google。
     * 纯数字或符号（没有字母）不算拉丁输入，仍按原路径处理。
     */
    private fun isLatinQuery(query: String): Boolean {
        val letters = query.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        return letters.none { ch ->
            ch in '\u4E00'..'\u9FFF' ||
                ch in '\u3400'..'\u4DBF' ||
                ch in '\u3040'..'\u30FF' ||
                ch in '\uAC00'..'\uD7AF'
        }
    }

    /**
     * 输入是否是一个"裸地名"：不带行政后缀的短词。
     * 这类词最容易一名多指（吉林、朝阳），也是唯一值得补搜的输入形态；
     * "朝阳区"这种自带后缀的输入已经足够精确，不必再展开。
     */
    private fun isBarePlaceName(query: String): Boolean =
        query.length in 2..4 && ADMIN_SUFFIX_HINTS.none { query.endsWith(it) }

    /** 高德文本搜索，失败或接口返回异常状态时按"没有结果"处理 */
    private suspend fun amapSearch(query: String, types: String): List<AmapPOI> = try {
        val response = amapTextSearchService.searchCities(query, types, "", 10, 1)
        if (response.status == "1") response.pois else emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

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
     *
     * 坐标直接透传，不做任何转换：高德与彩云同属 GCJ-02，
     * 搜索拿到的坐标可以直接存下来查天气，定位拿到的坐标同样原样使用。
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