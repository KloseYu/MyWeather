package com.myweather.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 预报图表配色。
 *
 * 逐时图与逐日图都在原生 Canvas 上用 [android.graphics.Paint] 绘制，
 * 颜色需要是 ARGB 整数；这里统一以 Compose [Color] 声明，
 * 由图表在使用时调用 `toArgb()` 转换。
 * 深色/浅色的分支判断收敛在此处，图表文件只负责几何与绘制。
 */
@Immutable
data class HourlyChartColors(
    val isNight: Boolean,
    val bar: Color,
    val waveStart: Color,
    val waveEnd: Color,
    val valueText: Color,
    val timeText: Color
)

@Immutable
data class DailyChartColors(
    val isNight: Boolean,
    val bar: Color,
    val waveStart: Color,
    val waveEnd: Color,
    val maxText: Color,
    val minText: Color,
    val timeText: Color,
    val descText: Color
)

@Composable
fun hourlyChartColors(isDark: Boolean = isSystemInDarkTheme()): HourlyChartColors {
    return if (isDark) {
        HourlyChartColors(
            isNight = true,
            bar = Color(0x3C4FC3F7),
            waveStart = Color(0xB44FC3F7),
            waveEnd = Color(0x50297FFF),
            valueText = Color(0xFFE0E0E0),
            timeText = Color(0xFFBDBDBD)
        )
    } else {
        HourlyChartColors(
            isNight = false,
            bar = Color(0x282196F3),
            waveStart = Color(0xC82196F3),
            waveEnd = Color(0x641565C0),
            valueText = Color(0xFF424242),
            timeText = Color(0xFF757575)
        )
    }
}

@Composable
fun dailyChartColors(isDark: Boolean = isSystemInDarkTheme()): DailyChartColors {
    return if (isDark) {
        DailyChartColors(
            isNight = true,
            bar = Color(0x37FF8A65),
            waveStart = Color(0xAAFF7043),
            waveEnd = Color(0x46F4511E),
            maxText = Color(0xFFFFCDD2),
            minText = Color(0xFFBBDEFB),
            timeText = Color(0xFFBDBDBD),
            descText = Color(0xFFBDBDBD)
        )
    } else {
        DailyChartColors(
            isNight = false,
            bar = Color(0x23FF5722),
            waveStart = Color(0xBEFF5722),
            waveEnd = Color(0x5AE63900),
            maxText = Color(0xFFC62828),
            minText = Color(0xFF1565C0),
            timeText = Color(0xFF757575),
            descText = Color(0xFF757575)
        )
    }
}