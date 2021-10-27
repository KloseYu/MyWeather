package com.myweather.android.ui.weather

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.myweather.android.logic.Repository
import com.myweather.android.logic.model.PlaceResponse

/**
 * 文件名:  WeatherActivity
 * 作者: KloseYu
 * 日期: 2021/10/27
 * 描述:  Weather的ViewModel层
 */
class WeatherViewModel: ViewModel() {

    private val locationLiveData = MutableLiveData<PlaceResponse.Place.Location>()

    var locationLng = ""

    var locationLat = ""

    var placeName = ""

    val weatherLiveData = Transformations.switchMap(locationLiveData){
        Repository.refreshWeather(it.lng,it.lat)
    }

    fun refreshWeather (lng:String,lat:String){
        locationLiveData.value = PlaceResponse.Place.Location(lng, lat)
    }
}