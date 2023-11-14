package com.myweather.android.logic.dao

import android.content.Context
import com.google.gson.Gson
import androidx.core.content.edit
import com.myweather.android.MyWeatherApplication
import com.myweather.android.logic.model.PlaceResponse

/*
 * 文件名: PlaceDao
 * 作者: KloseYu
 * 日期: 2021/10/27
 * 描述: 存储sharedpreferences
 */

object PlaceDao {
    fun savePlace(place: PlaceResponse.Place){
        sharedPreferences().edit(){
            putString("place", Gson().toJson(place))
        }
    }
    fun getSavedPlace(): PlaceResponse.Place {
        val placeJson = sharedPreferences().getString("place","")
        return Gson().fromJson(placeJson, PlaceResponse.Place::class.java)
    }
    fun isPlaceSaved() = sharedPreferences().contains("place")
    private fun sharedPreferences() = MyWeatherApplication.context.getSharedPreferences("my_weather",
        Context.MODE_PRIVATE)
}