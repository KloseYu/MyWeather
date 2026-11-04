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
import com.myweather.android.logic.model.HourlyResponse

/**
 * 24 小时逐时预报区域，对应传统版本的 HourlyChartView + hourly.xml。
 *
 * 以卡片形式包裹 [HourlyChart]，标题为"24小时预报"，图表可水平滚动。
 * 逐时数据在接口里是可为空的三段并行列表（温度、时间、天气代码），
 * 这里按最短长度截断，保证三者下标对齐。
 */
@Composable
fun HourlySection(
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