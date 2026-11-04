package com.myweather.android.ui.place

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myweather.android.logic.Repository
import com.myweather.android.logic.location.LocationClient
import com.myweather.android.logic.model.Place
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 城市搜索与管理的 ViewModel。
 *
 * 界面状态集中在 [uiState] 一个 [StateFlow] 里：搜索、保存、删除都在 ViewModel 内完成，
 * 界面只负责把状态画出来、把用户操作转发进来。
 *
 * - 搜索：输入变化即写入状态，并取消上一次未完成的请求（与原先 switchMap 的取消语义一致）；
 * - 保存/删除：操作后自动从本地存储重新读取列表，界面无需手动刷新；
 * - 定位：获取坐标 → 逆地理编码 → 保存城市，成功与失败分别通过 [locatedPlace]、[locateError] 通知界面。
 *
 * 定位结果单独成流，是因为定位可能由天气页顶部按钮触发，
 * 此时侧边栏尚未打开，不适合复用侧边栏内的搜索错误状态。
 */
class PlaceViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        PlaceUiState(savedPlaces = Repository.getSavedPlaceList())
    )
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()

    private val _locatedPlace = MutableStateFlow<Place?>(null)

    /** 定位成功的城市，界面消费后调用 [onLocatedPlaceConsumed] 清除 */
    val locatedPlace: StateFlow<Place?> = _locatedPlace.asStateFlow()

    private val _locateError = MutableStateFlow<String?>(null)

    /** 定位失败的提示，界面展示后调用 [onLocateErrorShown] 清除 */
    val locateError: StateFlow<String?> = _locateError.asStateFlow()

    private var searchJob: Job? = null
    private var locateJob: Job? = null

    /**
     * 搜索词变化时调用。空白输入直接清空结果，不再发起请求；
     * 非空输入取消上一次请求后延迟发起新请求。
     *
     * 这里的延迟就是输入防抖：连续键入会不断取消上一次任务，
     * 只有停顿之后的那一次才真正打到网络。城市搜索在补全同名行政区时
     * 要连发几次请求，没有防抖时每敲一个字都会触发一整轮搜索，
     * 很容易撞上高德的 QPS 限流，反而搜不出结果。
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val result = Repository.searchPlaces(query)
            val places = result.getOrNull()
            _uiState.update {
                it.copy(
                    searchResults = places ?: emptyList(),
                    errorMessage = if (places == null) {
                        result.exceptionOrNull()?.message ?: "未搜索到任何地点"
                    } else {
                        it.errorMessage
                    }
                )
            }
        }
    }

    /** 重新从本地存储读取已保存城市，保证列表与实际存储一致 */
    fun refreshSavedPlaces() {
        _uiState.update { it.copy(savedPlaces = Repository.getSavedPlaceList()) }
    }

    fun savePlace(place: Place) {
        Repository.savePlace(place)
        refreshSavedPlaces()
    }

    fun deletePlace(place: Place) {
        Repository.deletePlace(place)
        refreshSavedPlaces()
    }

    /** 是否已获得粗略或精确定位权限 */
    fun hasLocationPermission(): Boolean = LocationClient.hasPermission(getApplication())

    /**
     * 定位当前城市：取坐标 → 逆地理编码 → 保存 → 通过 [locatedPlace] 通知界面切换。
     * 调用前需确保已获得定位权限，缺少权限时由 [LocationClient] 返回失败结果。
     */
    fun locate() {
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            val coordinate = LocationClient.getCurrentCoordinate(getApplication()).getOrElse { error ->
                _locateError.value = error.message ?: "定位失败，请手动搜索城市"
                return@launch
            }
            val result = Repository.reverseGeocode(coordinate.lng, coordinate.lat)
            val place = result.getOrNull()
            if (place == null) {
                _locateError.value = result.exceptionOrNull()?.message ?: "定位失败，请手动搜索城市"
                return@launch
            }
            savePlace(place)
            _locatedPlace.value = place
        }
    }

    fun onLocatedPlaceConsumed() {
        _locatedPlace.value = null
    }

    fun onLocateErrorShown() {
        _locateError.value = null
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private companion object {
        /** 输入防抖时长：停顿超过这段时间才真正发起搜索 */
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}