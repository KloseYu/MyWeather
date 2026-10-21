package com.myweather.android.logic.dao

import android.content.Context
import androidx.core.content.edit
import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.Place
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

    /** 保存单个城市到列表头部（去重：同经纬度只保留一条） */
    fun savePlace(place: Place) {
        val list = getSavedPlaceList().toMutableList()
        list.removeAll { it.location.lng == place.location.lng && it.location.lat == place.location.lat }
        list.add(0, place)
        val json = gson.toJson(list)
        sharedPreferences().edit { putString("saved_places", json) }
    }

    fun getSavedPlace(): Place {
        val json = sharedPreferences().getString("saved_places", "")
        val list: List<Place> = if (!json.isNullOrEmpty()) {
            gson.fromJson(json, object : TypeToken<List<Place>>(){}.type)
        } else emptyList()
        return list.first()
    }

    fun isPlaceSaved(): Boolean {
        val json = sharedPreferences().getString("saved_places", "")
        val list: List<Place> = if (!json.isNullOrEmpty()) {
            gson.fromJson(json, object : TypeToken<List<Place>>(){}.type)
        } else emptyList()
        return list.isNotEmpty()
    }

    fun getSavedPlaceList(): List<Place> {
        val json = sharedPreferences().getString("saved_places", "")
        return if (!json.isNullOrEmpty()) {
            gson.fromJson(json, object : TypeToken<List<Place>>(){}.type)
        } else emptyList()
    }

    /** 删除指定城市 */
    fun deletePlace(place: Place) {
        val list = getSavedPlaceList().toMutableList()
        list.removeAll { it.location.lng == place.location.lng && it.location.lat == place.location.lat }
        val json = gson.toJson(list)
        sharedPreferences().edit { putString("saved_places", json) }
    }
}