package com.myweather.android.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myweather.android.logic.dao.PlaceDao
import com.myweather.android.logic.model.Location
import com.myweather.android.logic.model.Place
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.model.isFahrenheitCountry
import com.myweather.android.ui.place.PlaceViewModel
import com.myweather.android.ui.weather.WeatherViewModel
import kotlinx.coroutines.launch

/**
 * 应用主导航容器。
 *
 * 使用 [ModalNavigationDrawer] 实现侧边栏 + 主内容的经典布局：
 * - 侧边栏（320dp 宽）承载 [PlaceScreen] 城市搜索/管理界面；
 * - 主内容区承载 [WeatherScreen] 天气详情界面。
 *
 * 状态管理：
 * - [placeList] 已保存城市列表，增删后同步刷新；
 * - [currentPlaceIndex] 当前展示的城市索引，支持左右滑动切换；
 * - [viewPagerReady] 标记是否已有城市被选中，用于决定展示天气还是引导页。
 *
 * 天气数据通过 [WeatherViewModel] 获取，切换城市时自动触发 [LaunchedEffect] 刷新，
 * 并优先展示缓存数据，网络请求成功后更新缓存。
 *
 * 定位流程：请求位置权限 → 获取经纬度 → 反向地理编码 → 保存城市 → 切换展示。
 */
@Composable
fun MainNavigation(
    placeViewModel: PlaceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var placeList by remember { mutableStateOf(PlaceDao.getSavedPlaceList()) }
    var currentPlaceIndex by remember { mutableStateOf(0) }
    var viewPagerReady by remember { mutableStateOf(false) }

    val currentPlace = if (placeList.isNotEmpty() && currentPlaceIndex < placeList.size) {
        placeList[currentPlaceIndex]
    } else null

    val weatherViewModel: WeatherViewModel = viewModel()

    if (currentPlace != null) {
        LaunchedEffect(currentPlace) {
            weatherViewModel.locationLng = currentPlace.location.lng
            weatherViewModel.locationLat = currentPlace.location.lat
            weatherViewModel.placeName = currentPlace.name
            val lng = currentPlace.location.lng.toDoubleOrNull() ?: 0.0
            val lat = currentPlace.location.lat.toDoubleOrNull() ?: 0.0
            weatherViewModel.isFahrenheitCountry = isFahrenheitCountry(lng, lat)

            val cached = weatherViewModel.getCachedWeather(
                currentPlace.location.lng,
                currentPlace.location.lat
            )
            if (cached != null) {
                weatherViewModel.putCachedWeather(
                    currentPlace.location.lng,
                    currentPlace.location.lat,
                    cached
                )
            }
            weatherViewModel.refreshWeather(
                currentPlace.location.lng,
                currentPlace.location.lat
            )
        }
    }

    val weatherResult by weatherViewModel.weatherLiveData.observeAsState()
    var currentWeather by remember { mutableStateOf<Weather?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(weatherResult) {
        val result = weatherResult
        if (result != null) {
            val weather = result.getOrNull()
            if (weather != null) {
                weatherViewModel.putCachedWeather(
                    weatherViewModel.locationLng,
                    weatherViewModel.locationLat,
                    weather
                )
                currentWeather = weather
            } else {
                val cached = weatherViewModel.getCachedWeather(
                    weatherViewModel.locationLng,
                    weatherViewModel.locationLat
                )
                if (cached != null) {
                    currentWeather = cached
                }
            }
            isRefreshing = false
        }
    }

    val reverseGeocodeResult by placeViewModel.reverseGeocodeResult.observeAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestLocation(context, placeViewModel)
        } else {
            Toast.makeText(context, "定位权限未开启，请手动搜索城市", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(reverseGeocodeResult) {
        val result = reverseGeocodeResult
        if (result != null) {
            val place = result.getOrNull()
            if (place != null) {
                placeViewModel.savePlace(place)
                placeList = PlaceDao.getSavedPlaceList()
                currentPlaceIndex = placeList.indexOfFirst {
                    it.location.lng == place.location.lng && it.location.lat == place.location.lat
                }.coerceAtLeast(0)
                viewPagerReady = true
                scope.launch { drawerState.close() }
            } else {
                Toast.makeText(context, "定位失败，请手动搜索城市", Toast.LENGTH_SHORT).show()
            }
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
                drawerContainerColor = if (isSystemInDarkTheme()) Color(0xFF1B2635) else Color(0xFFE1F5FE)
            ) {
                PlaceScreen(
                    viewModel = placeViewModel,
                    currentLng = currentPlace?.location?.lng ?: "",
                    currentLat = currentPlace?.location?.lat ?: "",
                    isDrawerOpen = drawerState.isOpen,
                    onPlaceClick = { place ->
                        if (!placeList.any { it.location.lng == place.location.lng && it.location.lat == place.location.lat }) {
                            placeViewModel.savePlace(place)
                        }
                        placeList = PlaceDao.getSavedPlaceList()
                        currentPlaceIndex = placeList.indexOfFirst {
                            it.location.lng == place.location.lng && it.location.lat == place.location.lat
                        }.coerceAtLeast(0)
                        viewPagerReady = true
                        scope.launch { drawerState.close() }
                    },
                    onLocateClick = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            requestLocation(context, placeViewModel)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onPlaceDelete = { place ->
                        placeViewModel.deletePlace(place)
                        placeList = PlaceDao.getSavedPlaceList()
                        if (placeList.isEmpty()) {
                            viewPagerReady = false
                        } else {
                            currentPlaceIndex = currentPlaceIndex.coerceAtMost(placeList.size - 1)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        modifier = modifier
    ) {
        if (currentPlace != null && viewPagerReady) {
            WeatherScreen(
                placeName = currentPlace.name,
                weather = currentWeather,
                isFahrenheitCountry = weatherViewModel.isFahrenheitCountry,
                lng = currentPlace.location.lng,
                lat = currentPlace.location.lat,
                onNavClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        requestLocation(context, placeViewModel)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onAddClick = {
                    scope.launch { drawerState.open() }
                },
                onSwipeLeft = {
                    if (currentPlaceIndex < placeList.size - 1) {
                        currentPlaceIndex++
                    }
                },
                onSwipeRight = {
                    if (currentPlaceIndex > 0) {
                        currentPlaceIndex--
                    }
                },
                onRefresh = {
                    isRefreshing = true
                    weatherViewModel.refreshWeather(
                        currentPlace.location.lng,
                        currentPlace.location.lat
                    )
                },
                isRefreshing = isRefreshing
            )
        } else {
            LaunchedEffect(Unit) {
                if (placeList.isEmpty()) {
                    drawerState.open()
                }
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("请搜索并选择城市", color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}

/**
 * 请求设备当前位置并通过反向地理编码获取城市信息。
 *
 * 使用 [LocationManager] 获取最佳定位提供者的单次位置更新，
 * 经纬度保留两位小数后调用 [PlaceViewModel.reverseGeocode] 进行逆地理编码。
 *
 * 需要调用方已获取 ACCESS_FINE_LOCATION 或 ACCESS_COARSE_LOCATION 权限，
 * 缺少权限时会抛出 [SecurityException] 并提示用户。
 */
@Suppress("MissingPermission")
private fun requestLocation(
    context: android.content.Context,
    placeViewModel: PlaceViewModel
) {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        Toast.makeText(context, "定位服务不可用，请手动搜索城市", Toast.LENGTH_SHORT).show()
        return
    }

    val providers = locationManager.getProviders(true)
    if (providers.isEmpty()) {
        Toast.makeText(context, "无可用的定位提供者", Toast.LENGTH_SHORT).show()
        return
    }

    val criteria = Criteria().apply {
        accuracy = Criteria.ACCURACY_FINE
        isCostAllowed = true
    }
    val bestProvider = locationManager.getBestProvider(criteria, true) ?: providers[0]

    try {
        locationManager.requestSingleUpdate(bestProvider, object : LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                val lng = String.format("%.2f", location.longitude)
                val lat = String.format("%.2f", location.latitude)
                placeViewModel.reverseGeocode(lng, lat)
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String, status: Int, extras: android.os.Bundle?) {}
        }, null)
    } catch (e: SecurityException) {
        Toast.makeText(context, "定位权限不足", Toast.LENGTH_SHORT).show()
    }
}