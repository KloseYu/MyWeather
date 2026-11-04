package com.myweather.android.ui.weather.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.logic.model.DailyResponse
import com.myweather.android.logic.model.RealtimeResponse
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.model.formatCelsius
import com.myweather.android.logic.model.formatFahrenheit
import com.myweather.android.logic.model.formatHumidityText
import com.myweather.android.logic.model.formatWindText
import com.myweather.android.logic.model.getSkyDesc
import com.myweather.android.ui.theme.AddCityIcon
import com.myweather.android.ui.theme.HomeIcon
import com.myweather.android.ui.theme.WeatherText

/**
 * 当前天气概况区域，对应传统版本的 NowLayout + now.xml。
 *
 * 顶部以天气背景图铺满，覆盖以下信息：
 * - 导航栏：左侧主页按钮、中间城市名、右侧添加城市按钮；
 * - 温度：摄氏温度，华氏度国家额外显示华氏温度；
 * - 天气描述 + AQI：日间/夜间天气描述组合，AQI 根据中国/海外标准选择；
 * - 风速 + 湿度：格式化后的风向风速和相对湿度。
 *
 * 支持左右滑动手势切换城市，滑动阈值 80px。
 */
@Composable
fun NowSection(
    placeName: String,
    realtime: RealtimeResponse.Realtime,
    daily: DailyResponse.Daily,
    weather: Weather,
    isFahrenheitCountry: Boolean,
    lng: String,
    lat: String,
    skyBg: Int,
    onNavClick: () -> Unit,
    onAddClick: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val swipeThreshold = 80f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    do {
                        val event = awaitPointerEvent()
                        val move = event.changes.firstOrNull()
                        if (move != null) {
                            totalX = move.position.x - down.position.x
                            move.consume()
                        }
                    } while (event.changes.any { it.pressed })
                    if (totalX < -swipeThreshold) onSwipeLeft()
                    else if (totalX > swipeThreshold) onSwipeRight()
                }
            }
    ) {
        Image(
            painter = painterResource(id = skyBg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            NowTopBar(
                placeName = placeName,
                onNavClick = onNavClick,
                onAddClick = onAddClick
            )

            Spacer(modifier = Modifier.height(50.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TemperatureRow(
                    temperature = realtime.temperature,
                    isFahrenheitCountry = isFahrenheitCountry
                )

                Spacer(modifier = Modifier.height(20.dp))

                val dailySkycons = daily.skycon
                val todayDaySkycon = daily.skycon08h20h?.getOrNull(0)?.value
                    ?: dailySkycons.getOrNull(0)?.value
                    ?: realtime.skycon
                val todayNightSkycon = daily.skycon20h32h?.getOrNull(0)?.value
                    ?: dailySkycons.getOrNull(0)?.value
                    ?: realtime.skycon

                SkyAndAqiRow(
                    daySkycon = todayDaySkycon,
                    nightSkycon = todayNightSkycon,
                    weather = weather,
                    lng = lng,
                    lat = lat
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatWindText(realtime.wind.direction, realtime.wind.speed),
                        color = WeatherText,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(13.dp))
                    Text(text = "|", color = WeatherText, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(13.dp))
                    Text(
                        text = formatHumidityText(realtime.humidity),
                        color = WeatherText,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

/** 顶部导航栏：左侧定位/主页按钮、中间城市名、右侧添加城市按钮。 */
@Composable
private fun NowTopBar(
    placeName: String,
    onNavClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onNavClick, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = HomeIcon,
                contentDescription = "定位",
                tint = WeatherText,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = placeName,
            color = WeatherText,
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = AddCityIcon,
                contentDescription = "添加",
                tint = WeatherText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** 温度行：摄氏度恒显示，华氏度国家额外以竖线分隔显示华氏度。 */
@Composable
private fun TemperatureRow(
    temperature: Double,
    isFahrenheitCountry: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatCelsius(temperature),
            color = WeatherText,
            fontSize = 50.sp
        )
        if (isFahrenheitCountry) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(WeatherText.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatFahrenheit(temperature),
                color = WeatherText,
                fontSize = 50.sp
            )
        }
    }
}

/** 天气描述与 AQI 行：描述由日间/夜间天气代码组合而成，AQI 按中国/海外标准取值。 */
@Composable
private fun SkyAndAqiRow(
    daySkycon: String,
    nightSkycon: String,
    weather: Weather,
    lng: String,
    lat: String
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = getSkyDesc(daySkycon, nightSkycon),
            color = WeatherText,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(13.dp))
        Text(text = "|", color = WeatherText, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(13.dp))

        val isChina = weather.isChina(lng, lat)
        val aqiValue = weather.getAqiValue(isChina)
        val aqiLevel = weather.getAqiLevel(isChina)
        val aqiLabel = if (isChina) "AQI(CN)" else "AQI(US)"
        val currentAQIText = if (aqiValue != null) "$aqiLabel $aqiValue $aqiLevel" else "$aqiLabel N/A"
        Text(
            text = currentAQIText,
            color = WeatherText,
            fontSize = 18.sp
        )
    }
}