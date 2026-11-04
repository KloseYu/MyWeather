package com.myweather.android.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myweather.android.logic.Repository
import com.myweather.android.logic.model.Place
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.model.isFahrenheitCountry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 天气页 ViewModel。
 *
 * 城市信息、天气数据、刷新与错误状态统一收敛到 [uiState]，
 * 界面拿到一个状态对象就能渲染整页。
 *
 * 内置 LRU 缓存避免重复请求：切换城市时先展示缓存，网络结果到达后覆盖；
 * 切换城市会取消上一个城市的请求，避免旧结果盖住新城市。
 */
class WeatherViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    // 天气数据 LRU 缓存，key 为 "lng,lat"，最多 10 条
    private val weatherCache = LinkedHashMap<String, Weather>(8, 0.75f, false)

    private var requestJob: Job? = null

    /** 切换展示城市：写入城市信息 → 先展示缓存 → 再请求网络覆盖 */
    fun showPlace(place: Place) {
        val lng = place.location.lng
        val lat = place.location.lat
        _uiState.update {
            it.copy(
                placeName = place.name,
                lng = lng,
                lat = lat,
                isFahrenheitCountry = isFahrenheitCountry(
                    lng.toDoubleOrNull() ?: 0.0,
                    lat.toDoubleOrNull() ?: 0.0
                ),
                weather = getCachedWeather(lng, lat),
                errorMessage = null
            )
        }
        loadWeather(lng, lat)
    }

    /** 下拉刷新当前城市 */
    fun refresh() {
        val state = _uiState.value
        if (state.lng.isEmpty() || state.lat.isEmpty()) return
        loadWeather(state.lng, state.lat)
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadWeather(lng: String, lat: String) {
        requestJob?.cancel()
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        requestJob = viewModelScope.launch {
            val result = Repository.refreshWeather(lng, lat)
            val weather = result.getOrNull()
            if (weather != null) {
                putCachedWeather(lng, lat, weather)
                _uiState.update { it.copy(weather = weather, isRefreshing = false) }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "无法成功获取天气信息",
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private fun getCachedWeather(lng: String, lat: String): Weather? {
        return weatherCache[cacheKey(lng, lat)]
    }

    private fun putCachedWeather(lng: String, lat: String, weather: Weather) {
        val key = cacheKey(lng, lat)
        if (weatherCache.size >= 10 && !weatherCache.containsKey(key)) {
            weatherCache.remove(weatherCache.keys.first())
        }
        weatherCache[key] = weather
    }

    private fun cacheKey(lng: String, lat: String) = "$lng,$lat"
}