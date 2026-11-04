package com.myweather.android.ui.navigation

import android.Manifest
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myweather.android.logic.model.Place
import com.myweather.android.ui.place.PlaceScreen
import com.myweather.android.ui.place.PlaceViewModel
import com.myweather.android.ui.theme.DrawerBgDark
import com.myweather.android.ui.theme.DrawerBgLight
import com.myweather.android.ui.weather.WeatherScreen
import com.myweather.android.ui.weather.WeatherViewModel
import kotlinx.coroutines.launch

/**
 * 应用主导航容器，替代传统版本的 DrawerLayout + ViewPager2 结构。
 *
 * 使用 [ModalNavigationDrawer] 实现侧边栏 + 主内容的经典布局：
 * - 侧边栏（320dp 宽）承载 [PlaceScreen] 城市搜索/管理界面；
 * - 主内容区承载 [WeatherScreen] 天气详情界面。
 *
 * 这里只做三件事：把两个 ViewModel 的状态收集成 Compose 状态、把用户操作转成 ViewModel 调用、
 * 处理需要 Activity 能力（权限申请、Toast、键盘）的副作用。
 * 具体界面渲染分别在 [PlaceScreen] 与 [WeatherScreen] 中完成。
 *
 * 状态管理：
 * - 已保存城市列表由 [PlaceViewModel.uiState] 单向驱动，保存/删除后 UI 自动重组；
 * - [selectedKey] 记录当前展示城市的经纬度，城市被删除时自动回落到列表中的第一个；
 * - 天气数据由 [WeatherViewModel.uiState] 驱动，切换城市时自动刷新。
 *
 * 定位流程：请求位置权限 → [PlaceViewModel.locate] 取坐标并逆地理编码 → 保存城市 → 切换展示。
 */
@Composable
fun MainNavigation(
    placeViewModel: PlaceViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val placeState by placeViewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val locatedPlace by placeViewModel.locatedPlace.collectAsStateWithLifecycle()
    val locateError by placeViewModel.locateError.collectAsStateWithLifecycle()

    var selectedKey by remember { mutableStateOf<String?>(null) }

    // 首次进入且已有已保存城市时直接展示第一个；
    // 当前城市被删除后自动回落到列表中的第一个，列表为空则回到引导页。
    LaunchedEffect(placeState.savedPlaces) {
        val key = selectedKey
        if (key != null && placeState.savedPlaces.none { it.placeKey == key }) {
            selectedKey = placeState.savedPlaces.firstOrNull()?.placeKey
        } else if (key == null && placeState.savedPlaces.isNotEmpty()) {
            selectedKey = placeState.savedPlaces.first().placeKey
        }
    }

    val currentPlace = selectedKey?.let { key ->
        placeState.savedPlaces.firstOrNull { it.placeKey == key }
    }

    if (currentPlace != null) {
        LaunchedEffect(currentPlace) {
            weatherViewModel.showPlace(currentPlace)
        }
    }

    LaunchedEffect(weatherState.errorMessage) {
        weatherState.errorMessage?.let {
            Toast.makeText(context, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
            weatherViewModel.onErrorShown()
        }
    }

    LaunchedEffect(locatedPlace) {
        val place = locatedPlace ?: return@LaunchedEffect
        selectedKey = place.placeKey
        scope.launch { drawerState.close() }
        placeViewModel.onLocatedPlaceConsumed()
    }

    LaunchedEffect(locateError) {
        locateError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            placeViewModel.onLocateErrorShown()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            placeViewModel.locate()
        } else {
            Toast.makeText(context, "定位权限未开启，请手动搜索城市", Toast.LENGTH_SHORT).show()
        }
    }

    // 已有定位权限直接定位，否则先申请权限
    fun requestLocate() {
        if (placeViewModel.hasLocationPermission()) {
            placeViewModel.locate()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(
                (context as? android.app.Activity)?.window?.decorView?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = if (isSystemInDarkTheme()) DrawerBgDark else DrawerBgLight
            ) {
                PlaceScreen(
                    uiState = placeState,
                    currentLng = currentPlace?.location?.lng ?: "",
                    currentLat = currentPlace?.location?.lat ?: "",
                    onSearchQueryChange = placeViewModel::onSearchQueryChange,
                    onLocateClick = { requestLocate() },
                    onPlaceClick = { place ->
                        placeViewModel.savePlace(place)
                        selectedKey = place.placeKey
                        scope.launch { drawerState.close() }
                    },
                    onDeletePlace = placeViewModel::deletePlace,
                    onSearchErrorShown = placeViewModel::onErrorShown,
                    onDrawerOpened = placeViewModel::refreshSavedPlaces,
                    isDrawerOpen = drawerState.isOpen,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        modifier = modifier
    ) {
        if (currentPlace != null) {
            WeatherScreen(
                uiState = weatherState,
                onNavClick = { requestLocate() },
                onAddClick = { scope.launch { drawerState.open() } },
                onSwipeLeft = {
                    val index = placeState.savedPlaces.indexOfFirst { it.placeKey == currentPlace.placeKey }
                    if (index >= 0 && index < placeState.savedPlaces.size - 1) {
                        selectedKey = placeState.savedPlaces[index + 1].placeKey
                    }
                },
                onSwipeRight = {
                    val index = placeState.savedPlaces.indexOfFirst { it.placeKey == currentPlace.placeKey }
                    if (index > 0) {
                        selectedKey = placeState.savedPlaces[index - 1].placeKey
                    }
                },
                onRefresh = weatherViewModel::refresh
            )
        } else {
            LaunchedEffect(Unit) {
                if (placeState.savedPlaces.isEmpty()) {
                    drawerState.open()
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("请搜索并选择城市", color = Color.Gray)
            }
        }
    }
}

/** 城市唯一标识：经纬度组合，用于在城市列表里定位当前城市 */
private val Place.placeKey: String
    get() = "${location.lng},${location.lat}"