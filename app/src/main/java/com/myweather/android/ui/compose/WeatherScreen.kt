package com.myweather.android.ui.compose

import android.content.res.Configuration
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.R
import com.myweather.android.logic.model.DailyResponse
import com.myweather.android.logic.model.HourlyResponse
import com.myweather.android.logic.model.RealtimeResponse
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.model.formatCelsius
import com.myweather.android.logic.model.formatFahrenheit
import com.myweather.android.logic.model.formatHumidityText
import com.myweather.android.logic.model.formatWindText
import com.myweather.android.logic.model.getDayIcon
import com.myweather.android.logic.model.getSky
import com.myweather.android.logic.model.getSkyDesc
import com.myweather.android.ui.theme.WeatherText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * 天气详情主界面，与传统版本 [WeatherPageFragment] 功能对齐。
 *
 * 整体布局为可垂直滚动的列，自上而下包含四个卡片区域：
 * 1. [NowSection] — 当前天气概况（温度、天气描述、AQI、风速、湿度）；
 * 2. [HourlySection] — 24 小时逐时预报图表；
 * 3. [DailySection] — 7 日逐日预报图表；
 * 4. [LifeIndexSection] — 生活指数（感冒、穿衣、实时紫外线、洗车），
 *    文字标签与数据绑定逻辑均与 [life_index.xml] 保持一致。
 *
 * 外层包裹 [PullToRefreshBox] 支持下拉刷新，
 * 天气数据为 null 时展示"加载中..."占位文字。
 *
 * @param placeName 当前城市名称
 * @param weather 天气数据，为 null 时显示加载状态
 * @param isFahrenheitCountry 是否为华氏度国家（同时显示摄氏与华氏）
 * @param lng 经度，用于判断中国/海外 AQI 标准
 * @param lat 纬度
 * @param onNavClick 导航按钮点击回调（打开侧边栏定位）
 * @param onAddClick 添加城市按钮点击回调（打开侧边栏搜索）
 * @param onSwipeLeft 左滑回调，切换到下一个城市
 * @param onSwipeRight 右滑回调，切换到上一个城市
 * @param onRefresh 下拉刷新回调
 * @param isRefreshing 是否正在刷新
 */
@Composable
fun WeatherScreen(
    placeName: String,
    weather: Weather?,
    isFahrenheitCountry: Boolean,
    lng: String,
    lat: String,
    onNavClick: () -> Unit,
    onAddClick: () -> Unit,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
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
            isRefreshing = isRefreshing,
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
                placeName = placeName,
                realtime = realtime,
                daily = daily,
                weather = weather,
                isFahrenheitCountry = isFahrenheitCountry,
                lng = lng,
                lat = lat,
                skyBg = skyBg,
                onNavClick = onNavClick,
                onAddClick = onAddClick,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight
            )

            if (hourly != null) {
                HourlySection(
                    hourly = hourly,
                    isFahrenheitCountry = isFahrenheitCountry
                )
            }

            DailySection(
                daily = daily,
                isFahrenheitCountry = isFahrenheitCountry
            )

            LifeIndexSection(
                lifeIndex = daily.lifeIndex
            )
            }
        }
    }
}

/**
 * 当前天气概况区域，对应传统版本的 [NowLayout] + now.xml。
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
private fun NowSection(
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

            Spacer(modifier = Modifier.height(50.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCelsius(realtime.temperature),
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
                            text = formatFahrenheit(realtime.temperature),
                            color = WeatherText,
                            fontSize = 50.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val dailySkycons = daily.skycon
                val todayDaySkycon = daily.skycon08h20h?.getOrNull(0)?.value
                    ?: dailySkycons.getOrNull(0)?.value
                    ?: realtime.skycon
                val todayNightSkycon = daily.skycon20h32h?.getOrNull(0)?.value
                    ?: dailySkycons.getOrNull(0)?.value
                    ?: realtime.skycon

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getSkyDesc(todayDaySkycon, todayNightSkycon),
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

/**
 * 24 小时逐时预报区域，对应传统版本的 [HourlyChartView] + hourly.xml。
 *
 * 以卡片形式包裹 [HourlyChart]，
 * 标题为"24小时预报"，图表可水平滚动。
 *
 * @param hourly 逐时预报数据
 * @param isFahrenheitCountry 是否使用华氏度
 */
@Composable
private fun HourlySection(
    hourly: HourlyResponse.Hourly,
    isFahrenheitCountry: Boolean
) {
    val temps = hourly.temperature ?: emptyList()
    val skycons = hourly.skycon ?: emptyList()
    val minSize = minOf(temps.size, skycons.size)
    if (minSize == 0) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Text(
                text = "24小时预报",
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                HourlyChart(
                    temperatures = temps.map { it.value }.subList(0, minSize),
                    timestamps = temps.map { it.date }.subList(0, minSize),
                    skycons = skycons.map { it.value }.subList(0, minSize),
                    useFahrenheit = isFahrenheitCountry
                )
            }
        }
    }
}

/**
 * 7 日逐日预报区域，对应传统版本的 [DailyChartView] + forecast.xml。
 *
 * 以卡片形式包裹 [DailyChart]，
 * 标题为"7日预报"，图表可水平滚动。
 *
 * @param daily 逐日预报数据
 * @param isFahrenheitCountry 是否使用华氏度
 */
@Composable
private fun DailySection(
    daily: DailyResponse.Daily,
    isFahrenheitCountry: Boolean
) {
    val maxTemps = daily.temperature.map { it.max }
    val minTemps = daily.temperature.map { it.min }
    val times = daily.skycon.map { it.date }
    val daySkycons = daily.skycon08h20h?.map { it.value } ?: daily.skycon.map { it.value }
    val nightSkycons = daily.skycon20h32h?.map { it.value } ?: daily.skycon.map { it.value }
    val minSize = minOf(maxTemps.size, minTemps.size, times.size, daySkycons.size, nightSkycons.size)
    if (minSize == 0) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Text(
                text = "7日预报",
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                DailyChart(
                    maxTemps = maxTemps.subList(0, minSize),
                    minTemps = minTemps.subList(0, minSize),
                    timestamps = times.subList(0, minSize),
                    daySkycons = daySkycons.subList(0, minSize),
                    nightSkycons = nightSkycons.subList(0, minSize),
                    useFahrenheit = isFahrenheitCountry
                )
            }
        }
    }
}

/**
 * 生活指数区域，对应传统版本的 life_index.xml。
 *
 * 以 2×2 网格展示四项生活指数：
 * 感冒、穿衣、实时紫外线、洗车。
 * 文字标签与 [life_index.xml] 中的硬编码文本完全一致，
 * 数据绑定使用与传统版本相同的 [List.isNotEmpty] 守卫模式：
 * 仅当列表非空时取 [0].desc，否则显示空字符串。
 *
 * @param lifeIndex 生活指数数据
 */
@Composable
private fun LifeIndexSection(
    lifeIndex: DailyResponse.LifeIndex
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Text(
                text = "生活指数",
                modifier = Modifier.padding(start = 15.dp, top = 20.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                LifeIndexItem(
                    iconRes = R.drawable.ic_coldrisk,
                    label = "感冒",
                    desc = if (lifeIndex.coldRisk.isNotEmpty()) lifeIndex.coldRisk[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
                LifeIndexItem(
                    iconRes = R.drawable.ic_dressing,
                    label = "穿衣",
                    desc = if (lifeIndex.dressing.isNotEmpty()) lifeIndex.dressing[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                LifeIndexItem(
                    iconRes = R.drawable.ic_ultraviolet,
                    label = "实时紫外线",
                    desc = if (lifeIndex.ultraviolet.isNotEmpty()) lifeIndex.ultraviolet[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
                LifeIndexItem(
                    iconRes = R.drawable.ic_carwashing,
                    label = "洗车",
                    desc = if (lifeIndex.carWashing.isNotEmpty()) lifeIndex.carWashing[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 单项生活指数组件，由图标、标签和描述文字水平排列组成。 */
@Composable
private fun LifeIndexItem(iconRes: Int, label: String, desc: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier
                .padding(start = 20.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier.padding(start = 20.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 24 小时逐时预报图表。
 *
 * 使用 [Canvas] 在原生画布上绘制，每个时间刻度包含：
 * - 圆角温度柱状图，带水波纹填充效果；
 * - 柱顶天气图标（夜间模式自动着白色）；
 * - 柱顶温度文字；
 * - 底部时间标签（HH:mm）。
 *
 * 深色/浅色模式自动切换柱体、波纹、文字颜色。
 *
 * @param temperatures 温度列表（摄氏度）
 * @param timestamps 时间戳列表
 * @param skycons 天气代码列表
 * @param useFahrenheit 是否使用华氏度显示
 */
@Composable
private fun HourlyChart(
    temperatures: List<Double>,
    timestamps: List<Date>,
    skycons: List<String>,
    useFahrenheit: Boolean
) {
    val n = minOf(temperatures.size, timestamps.size, skycons.size)
    if (n == 0) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isNight = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val colWidthDp = 48
    val chartHeightDp = 230
    val totalWidthDp = (colWidthDp * n).dp

    val minT = temperatures.minOrNull() ?: 0.0
    val maxT = temperatures.maxOrNull() ?: 0.0
    val range = (maxT - minT).coerceAtLeast(1.0)
    val paddedMin = minT - range * 0.15
    val paddedMax = maxT + range * 0.15
    val paddedRange = paddedMax - paddedMin

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .width(totalWidthDp)
            .height(chartHeightDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val density = this.density

            val w = size.width
            val h = size.height
            val padL = 8.dp.toPx()
            val padR = 8.dp.toPx()
            val padT = 42.dp.toPx()
            val padB = 20.dp.toPx()
            val chartW = w - padL - padR
            val chartH = h - padT - padB

            val colW = chartW / n
            val barW = colW * 0.52f
            val barRadius = barW * 0.3f

            val iconPx = (14 * density).toInt()
            val iconPad = 2.dp.toPx()

            val barColorInt = if (isNight) {
                android.graphics.Color.argb(60, 79, 195, 247)
            } else {
                android.graphics.Color.argb(40, 33, 150, 243)
            }
            val waveColor1Int = if (isNight) {
                android.graphics.Color.argb(180, 79, 195, 247)
            } else {
                android.graphics.Color.argb(200, 33, 150, 243)
            }
            val waveColor2Int = if (isNight) {
                android.graphics.Color.argb(80, 41, 121, 255)
            } else {
                android.graphics.Color.argb(100, 21, 101, 192)
            }
            val textColorInt = if (isNight) {
                android.graphics.Color.parseColor("#E0E0E0")
            } else {
                android.graphics.Color.parseColor("#424242")
            }
            val timeColorInt = if (isNight) {
                android.graphics.Color.parseColor("#BDBDBD")
            } else {
                android.graphics.Color.parseColor("#757575")
            }

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColorInt }
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 11f * density
                color = textColorInt
                textAlign = Paint.Align.CENTER
            }
            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 9f * density
                color = timeColorInt
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until n) {
                val cx = padL + colW * i + colW / 2f
                val temp = temperatures[i]
                val barH = ((temp - paddedMin) / paddedRange * chartH).toFloat()
                val barTop = padT + chartH - barH
                val barBottom = padT + chartH

                val barRect = RectF(cx - barW / 2, barTop, cx + barW / 2, barBottom)
                canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)

                if (barH > 4.dp.toPx()) {
                    val waterLevel = barTop + barH * 0.35f
                    val waveAmplitude = barW * 0.12f
                    val waveFreq = 2.5f

                    val clipPath = Path()
                    clipPath.addRoundRect(barRect, barRadius, barRadius, Path.Direction.CW)

                    val waveFill = Path()
                    waveFill.moveTo(barRect.left, barRect.bottom)
                    waveFill.lineTo(barRect.left, waterLevel)
                    val steps = 20
                    for (s in 0..steps) {
                        val ratio = s.toFloat() / steps
                        val px = barRect.left + barW * ratio
                        val py = waterLevel + waveAmplitude * sin(
                            ratio * waveFreq * PI.toFloat() + i * 0.8f
                        ).toFloat()
                        waveFill.lineTo(px, py)
                    }
                    waveFill.lineTo(barRect.right, barRect.bottom)
                    waveFill.close()

                    wavePaint.shader = LinearGradient(
                        0f, waterLevel - waveAmplitude, 0f, barRect.bottom,
                        waveColor1Int, waveColor2Int, Shader.TileMode.CLAMP
                    )

                    canvas.save()
                    canvas.clipPath(clipPath)
                    canvas.drawPath(waveFill, wavePaint)
                    canvas.restore()
                }

                if (i < skycons.size) {
                    val iconRes = getSky(skycons[i]).icon
                    val iconDrawable = context.getDrawable(iconRes)
                    iconDrawable?.let {
                        if (isNight) {
                            it.mutate().colorFilter = PorterDuffColorFilter(
                                android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN
                            )
                        }
                        val bgCy = barTop - iconPx / 2f - iconPad
                        val left = (cx - iconPx / 2).toInt()
                        val top = (bgCy - iconPx / 2).toInt()
                        it.setBounds(left, top, left + iconPx, top + iconPx)
                        it.draw(canvas)
                    }
                }

                val tempStr = if (!useFahrenheit) {
                    "${"%.0f".format(temperatures[i])}°"
                } else {
                    val f = temperatures[i] * 9.0 / 5.0 + 32.0
                    "${"%.0f".format(f)}°"
                }
                canvas.drawText(
                    tempStr, cx, barTop - iconPx - iconPad * 2 - 2.dp.toPx(), textPaint
                )
            }

            for (i in 0 until n) {
                val cx = padL + colW * i + colW / 2f
                val label = timeFormat.format(timestamps[i])
                canvas.drawText(label, cx, h - 4.dp.toPx(), timePaint)
            }
        }
    }
}

/**
 * 7 日逐日预报图表。
 *
 * 使用 [Canvas] 在原生画布上绘制，每个日期刻度包含：
 * - 圆角温度范围柱状图（最高温到最低温），带水波纹填充效果；
 * - 柱顶日间天气图标（夜间模式自动着白色）；
 * - 柱顶天气描述文字（如"多云转晴"）；
 * - 柱顶最高温文字（红色系）、柱底最低温文字（蓝色系）；
 * - 底部日期标签（MM/dd）。
 *
 * 深色/浅色模式自动切换所有颜色。
 *
 * @param maxTemps 每日最高温列表（摄氏度）
 * @param minTemps 每日最低温列表（摄氏度）
 * @param timestamps 日期列表
 * @param daySkycons 日间天气代码列表
 * @param nightSkycons 夜间天气代码列表
 * @param useFahrenheit 是否使用华氏度显示
 */
@Composable
private fun DailyChart(
    maxTemps: List<Double>,
    minTemps: List<Double>,
    timestamps: List<Date>,
    daySkycons: List<String>,
    nightSkycons: List<String>,
    useFahrenheit: Boolean
) {
    val count = minOf(maxTemps.size, minTemps.size, timestamps.size, daySkycons.size, nightSkycons.size)
    if (count == 0) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isNight = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val colWidthDp = 56
    val chartHeightDp = 250
    val totalWidthDp = (colWidthDp * count).dp

    val allTemps = maxTemps + minTemps
    val minT = allTemps.minOrNull() ?: 0.0
    val maxT = allTemps.maxOrNull() ?: 0.0
    val range = (maxT - minT).coerceAtLeast(1.0)
    val paddedMin = minT - range * 0.15
    val paddedMax = maxT + range * 0.15
    val paddedRange = paddedMax - paddedMin

    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .width(totalWidthDp)
            .height(chartHeightDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val density = this.density

            val w = size.width
            val h = size.height
            val padL = 8.dp.toPx()
            val padR = 8.dp.toPx()
            val padT = 42.dp.toPx()
            val padB = 20.dp.toPx()
            val chartW = w - padL - padR
            val chartH = h - padT - padB

            val colW = chartW / count
            val barW = colW * 0.48f
            val barRadius = barW * 0.3f

            val iconPx = (14 * density).toInt()
            val iconPad = 2.dp.toPx()

            val barColorInt = if (isNight) {
                android.graphics.Color.argb(55, 255, 138, 101)
            } else {
                android.graphics.Color.argb(35, 255, 87, 34)
            }
            val waveColor1Int = if (isNight) {
                android.graphics.Color.argb(170, 255, 112, 67)
            } else {
                android.graphics.Color.argb(190, 255, 87, 34)
            }
            val waveColor2Int = if (isNight) {
                android.graphics.Color.argb(70, 244, 81, 30)
            } else {
                android.graphics.Color.argb(90, 230, 57, 0)
            }
            val maxTextColorInt = if (isNight) {
                android.graphics.Color.parseColor("#FFCDD2")
            } else {
                android.graphics.Color.parseColor("#C62828")
            }
            val minTextColorInt = if (isNight) {
                android.graphics.Color.parseColor("#BBDEFB")
            } else {
                android.graphics.Color.parseColor("#1565C0")
            }
            val timeColorInt = if (isNight) {
                android.graphics.Color.parseColor("#BDBDBD")
            } else {
                android.graphics.Color.parseColor("#757575")
            }
            val descColorInt = if (isNight) {
                android.graphics.Color.parseColor("#BDBDBD")
            } else {
                android.graphics.Color.parseColor("#757575")
            }

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColorInt }
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val maxTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 10f * density
                color = maxTextColorInt
                textAlign = Paint.Align.CENTER
            }
            val minTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 10f * density
                color = minTextColorInt
                textAlign = Paint.Align.CENTER
            }
            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 9f * density
                color = timeColorInt
                textAlign = Paint.Align.CENTER
            }
            val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 8f * density
                color = descColorInt
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until count) {
                val cx = padL + colW * i + colW / 2f
                val yMax = padT + chartH - ((maxTemps[i] - paddedMin) / paddedRange * chartH).toFloat()
                val yMin = padT + chartH - ((minTemps[i] - paddedMin) / paddedRange * chartH).toFloat()
                val barTop = yMax
                val barBottom = yMin
                val barH = barBottom - barTop

                val barRect = RectF(cx - barW / 2, barTop, cx + barW / 2, barBottom)
                canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)

                if (barH > 4.dp.toPx()) {
                    val waterLevel = barTop + barH * 0.35f
                    val waveAmplitude = barW * 0.12f
                    val waveFreq = 2.5f

                    val clipPath = Path()
                    clipPath.addRoundRect(barRect, barRadius, barRadius, Path.Direction.CW)

                    val waveFill = Path()
                    waveFill.moveTo(barRect.left, barRect.bottom)
                    waveFill.lineTo(barRect.left, waterLevel)
                    val steps = 20
                    for (s in 0..steps) {
                        val ratio = s.toFloat() / steps
                        val px = barRect.left + barW * ratio
                        val py = waterLevel + waveAmplitude * sin(
                            ratio * waveFreq * PI.toFloat() + i * 0.8f
                        ).toFloat()
                        waveFill.lineTo(px, py)
                    }
                    waveFill.lineTo(barRect.right, barRect.bottom)
                    waveFill.close()

                    wavePaint.shader = LinearGradient(
                        0f, waterLevel - waveAmplitude, 0f, barRect.bottom,
                        waveColor1Int, waveColor2Int, Shader.TileMode.CLAMP
                    )

                    canvas.save()
                    canvas.clipPath(clipPath)
                    canvas.drawPath(waveFill, wavePaint)
                    canvas.restore()
                }

                val daySky = if (i < daySkycons.size) daySkycons[i] else "CLEAR_DAY"
                val nightSky = if (i < nightSkycons.size) nightSkycons[i] else daySky

                val iconRes = getDayIcon(daySky)
                val iconDrawable = context.getDrawable(iconRes)
                iconDrawable?.let {
                    if (isNight) {
                        it.mutate().colorFilter = PorterDuffColorFilter(
                            android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN
                        )
                    }
                    val bgCy = barTop - iconPx / 2f - iconPad
                    val left = (cx - iconPx / 2).toInt()
                    val top = (bgCy - iconPx / 2).toInt()
                    it.setBounds(left, top, left + iconPx, top + iconPx)
                    it.draw(canvas)
                }

                val desc = getSkyDesc(daySky, nightSky)
                canvas.drawText(
                    desc, cx, barTop - iconPx - iconPad * 2 - 1.dp.toPx(), descPaint
                )

                val maxStr = formatTemp(maxTemps[i], useFahrenheit)
                val minStr = formatTemp(minTemps[i], useFahrenheit)
                canvas.drawText(
                    maxStr, cx, barTop - iconPx - iconPad * 2 - 11.dp.toPx(), maxTextPaint
                )
                canvas.drawText(
                    minStr, cx, barBottom + 14.dp.toPx(), minTextPaint
                )
            }

            for (i in 0 until count) {
                val cx = padL + colW * i + colW / 2f
                val label = dateFormat.format(timestamps[i])
                canvas.drawText(label, cx, h - 4.dp.toPx(), timePaint)
            }
        }
    }
}

/** 将摄氏温度格式化为显示字符串，根据 [useFahrenheit] 决定输出华氏度或摄氏度。 */
private fun formatTemp(celsius: Double, useFahrenheit: Boolean): String {
    return if (!useFahrenheit) {
        "${"%.0f".format(celsius)}°"
    } else {
        val f = celsius * 9.0 / 5.0 + 32.0
        "${"%.0f".format(f)}°"
    }
}