package com.myweather.android.ui.place

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.myweather.android.logic.Repository
import com.myweather.android.logic.model.Place

/**
 * 城市搜索 ViewModel。
 * 通过 switchMap 驱动搜索和逆地理编码请求，
 * 提供城市保存/删除/查询等本地存储操作。
 */
class PlaceViewModel : ViewModel() {

    private val searchLiveData = MutableLiveData<String>()

    val placeList = ArrayList<Place>()

    val savedPlaceList = ArrayList<Place>()

    // 城市搜索：查询词变化时自动触发
    val placeLiveData: LiveData<Result<List<Place>>> = searchLiveData.switchMap { query ->
        Repository.searchPlaces(query)
    }

    private val regeoLiveData = MutableLiveData<Pair<String, String>>()

    // 逆地理编码：经纬度变化时自动触发
    val reverseGeocodeResult: LiveData<Result<Place>> = regeoLiveData.switchMap { (lng, lat) ->
        Repository.reverseGeocode(lng, lat)
    }

    val reverseGeocodeLoading = MutableLiveData<Boolean>()

    fun setReverseGeocodeLoading() {
        reverseGeocodeLoading.value = true
    }

    fun searchPlaces(query: String) {
        searchLiveData.value = query
    }

    fun reverseGeocode(lng: String, lat: String) {
        regeoLiveData.value = Pair(lng, lat)
    }

    fun savePlace(place: Place) = Repository.savePlace(place)

    fun getSavedPlace() = Repository.getSavedPlace()

    fun isPlaceSaved() = Repository.isPlaceSaved()

    fun getSavedPlaceList(): List<Place> {
        savedPlaceList.clear()
        savedPlaceList.addAll(Repository.getSavedPlaceList())
        return savedPlaceList
    }

    fun deletePlace(place: Place) = Repository.deletePlace(place)
}