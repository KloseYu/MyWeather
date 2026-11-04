package com.myweather.android.ui.weather

import com.myweather.android.logic.model.Weather

/**
 * 天气页的完整界面状态。
 *
 * 城市信息与天气数据合并在同一个状态对象里，
 * 界面只消费这一份状态，不再从 ViewModel 的多个字段里各自拼装。
 *
 * @param placeName 当前城市名称
 * @param lng 当前城市经度，用于判断 AQI 标准
 * @param lat 当前城市纬度
 * @param isFahrenheitCountry 是否同时显示华氏度
 * @param weather 天气数据，为 null 表示尚未加载到
 * @param isRefreshing 是否正在请求网络
 * @param errorMessage 待展示的错误提示，展示后由界面回调清除
 */
data class WeatherUiState(
    val placeName: String = "",
    val lng: String = "",
    val lat: String = "",
    val isFahrenheitCountry: Boolean = false,
    val weather: Weather? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)