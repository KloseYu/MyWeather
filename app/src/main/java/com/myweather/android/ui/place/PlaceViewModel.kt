package com.myweather.android.ui.place

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.myweather.android.logic.Repository
import com.myweather.android.logic.model.PlaceResponse

/**
 * 文件名: PlaceViewModel
 * 作者: KloseYu
 * 日期: 2021/10/25
 * 描述: place的ViewModel层
 */
class PlaceViewModel : ViewModel() {
    private val searchLiveData:MutableLiveData<String> = MutableLiveData<String>()

    val placeList:ArrayList<PlaceResponse.Place> = ArrayList<PlaceResponse.Place>()

    val placeLiveData =Transformations.switchMap(searchLiveData){
        Repository.searchPlaces(it)
    }

    fun searchPlaces(query:String){
        searchLiveData.value =query
    }

    fun savePlace(place: PlaceResponse.Place) {
        Repository.savePlace(place)
    }

    fun getSavedPlace(): PlaceResponse.Place {
        return Repository.getSavedPlace()
    }

    fun isPlaceSaved(): Boolean {
        return Repository.isPlaceSaved()
    }
}