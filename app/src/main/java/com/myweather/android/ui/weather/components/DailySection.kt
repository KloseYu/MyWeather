package com.myweather.android.ui.weather.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.logic.model.DailyResponse

/**
 * 7 日预报区域，对应传统版本的 DailyChartView + forecast.xml。
 *
 * 以卡片形式包裹 [DailyChart]，标题为"7日预报"，图表可水平滚动。
 * 日间/夜间天气代码缺失时退化为通用 skycon，与 [DailyChart] 的兜底保持一致。
 */
@Composable
fun DailySection(
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