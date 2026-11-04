package com.myweather.android.ui.place

import com.myweather.android.logic.model.Place

/**
 * 城市搜索侧边栏的完整界面状态。
 *
 * @param searchQuery 搜索输入框的当前内容
 * @param searchResults 搜索结果列表，输入被清空时为空
 * @param savedPlaces 已保存城市列表，保存/删除后自动更新
 * @param errorMessage 搜索失败的提示，界面展示后回调清除
 */
data class PlaceUiState(
    val searchQuery: String = "",
    val searchResults: List<Place> = emptyList(),
    val savedPlaces: List<Place> = emptyList(),
    val errorMessage: String? = null
)