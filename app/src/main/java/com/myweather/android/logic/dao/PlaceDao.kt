package com.myweather.android.logic.dao

import android.content.Context
import androidx.core.content.edit
import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.Location
import com.myweather.android.logic.model.Place
import com.myweather.android.logic.model.normalizedAddress
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 城市数据持久化：通过 SharedPreferences 存储已保存的城市列表（JSON 序列化）。
 * 支持增删改查，key 为 saved_places。
 */
object PlaceDao {

    private fun sharedPreferences() =
        MyWeatherApplication.context.getSharedPreferences("sunny_weather", Context.MODE_PRIVATE)

    private val gson = Gson()

    /** 保存单个城市到列表头部（去重：同一城市只保留一条） */
    fun savePlace(place: Place) {
        val list = getSavedPlaceList().toMutableList()
        list.removeAll { it.isSamePlaceAs(place) }
        list.add(0, place)
        val json = gson.toJson(list)
        sharedPreferences().edit { putString("saved_places", json) }
    }

    fun getSavedPlace(): Place {
        val list = getSavedPlaceList()
        return list.firstOrNull() ?: Place("", Location("", ""), "")
    }

    fun isPlaceSaved(): Boolean = getSavedPlaceList().isNotEmpty()

    /**
     * 读取已保存城市列表，并清洗历史数据：
     * 过滤掉空元素与坐标缺失项，按城市去重，
     * 保证返回的数量就是实际可展示的城市数量，避免侧边栏与天气页数量不一致。
     * 同时把地址裁剪到行政区一级，这样以前存下的「街道门牌」地址
     * 无需重新搜索也会自动变干净。
     */
    fun getSavedPlaceList(): List<Place> {
        val json = sharedPreferences().getString("saved_places", "")
        if (json.isNullOrEmpty()) return emptyList()
        val raw: List<Place?> = gson.fromJson(json, object : TypeToken<List<Place?>>(){}.type) ?: return emptyList()
        val result = ArrayList<Place>()
        for (place in raw.filterNotNull()) {
            if (place.location.lng.isBlank() || place.location.lat.isBlank()) continue
            val cleaned = place.copy(address = place.normalizedAddress())
            if (result.none { it.isSamePlaceAs(cleaned) }) result.add(cleaned)
        }
        return result
    }

    /** 删除指定城市 */
    fun deletePlace(place: Place) {
        val list = getSavedPlaceList().toMutableList()
        list.removeAll { it.isSamePlaceAs(place) }
        val json = gson.toJson(list)
        sharedPreferences().edit { putString("saved_places", json) }
    }

    /**
     * 判定是否为同一城市，依据行政归属与位置本身，不看距离。
     * 天气预报按行政区发布，行政区的实际形状与距离无关：
     * - 飞地、跨山河的插花地很常见，离区中心几十公里仍属同一个区、同一个预报区
     * - 反过来，相邻的两个区中心点可能只差几公里，却是两份独立预报
     * 任何距离阈值都会在某一类地区上判错，而这类地区在全国范围内大量存在，
     * 不是个别特例，因此距离不参与判定。
     *
     * 两条独立依据，满足其一即为同一城市：
     * 1. 坐标完全相同：6 位小数的同一个点不可能属于两个地方，
     *    同时可吸收「同一位置先后被以不同名称保存」的历史数据；
     * 2. 展示名相同：展示名是从省到区县的完整层级路径，
     *    本身已能区分同名地区（北京市朝阳区 / 吉林省长春市朝阳区），故同名即同城。
     * 名称缺失的历史数据退化为按坐标完全相同判定。
     */
    private fun Place.isSamePlaceAs(other: Place): Boolean = isSamePlace(this, other)

    /**
     * 对外暴露同一套身份判定，供天气页与侧边栏共用。
     * 只要两边都用这一个函数定位城市，就不可能出现「同一城市被认成两条记录」。
     */
    fun isSamePlace(a: Place, b: Place): Boolean {
        if (a.location.lng == b.location.lng && a.location.lat == b.location.lat) return true
        return a.name.isNotBlank() && b.name.isNotBlank() && a.name == b.name
    }
}