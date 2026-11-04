package com.myweather.android.ui.weather

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.myweather.android.logic.model.getSky
import com.myweather.android.ui.weather.components.DailySection
import com.myweather.android.ui.weather.components.HourlySection
import com.myweather.android.ui.weather.components.LifeIndexSection
import com.myweather.android.ui.weather.components.NowSection

/**
 * 天气详情主界面，与传统版本 WeatherPageFragment 功能对齐。
 *
 * 只消费 [WeatherUiState]，不持有任何业务状态：
 * 城市名、天气数据、是否刷新、经纬度全部来自 ViewModel 的状态流，
 * 用户操作（定位、添加城市、左右滑动切换、下拉刷新）通过回调交回上层。
 *
 * 布局自上而下包含四个卡片区域：
 * 1. [NowSection] — 当前天气概况（温度、天气描述、AQI、风速、湿度）；
 * 2. [HourlySection] — 24 小时逐时预报图表；
 * 3. [DailySection] — 7 日逐日预报图表；
 * 4. [LifeIndexSection] — 生活指数（感冒、穿衣、实时紫外线、洗车）。
 *
 * 外层包裹 [PullToRefreshBox] 支持下拉刷新，
 * 天气数据为 null 时展示"加载中..."占位文字。
 */
@Composable
fun WeatherScreen(
    uiState: WeatherUiState,
    onNavClick: () -> Unit,
    onAddClick: () -> Unit,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val weather = uiState.weather
    if (weather == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("加载中...", color = Color.Gray)
        }
        return
    }

    val realtime = weather.realtime
    val daily = weather.daily
    val hourly = weather.hourly
    val skyBg = getSky(realtime.skycon).bg

    Box(modifier = modifier.fillMaxSize()) {
        val pullToRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                NowSection(
                    placeName = uiState.placeName,
                    realtime = realtime,
                    daily = daily,
                    weather = weather,
                    isFahrenheitCountry = uiState.isFahrenheitCountry,
                    lng = uiState.lng,
                    lat = uiState.lat,
                    skyBg = skyBg,
                    onNavClick = onNavClick,
                    onAddClick = onAddClick,
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight
                )

                if (hourly != null) {
                    HourlySection(
                        hourly = hourly,
                        isFahrenheitCountry = uiState.isFahrenheitCountry
                    )
                }

                DailySection(
                    daily = daily,
                    isFahrenheitCountry = uiState.isFahrenheitCountry
                )

                LifeIndexSection(
                    lifeIndex = daily.lifeIndex
                )
            }
        }
    }
}