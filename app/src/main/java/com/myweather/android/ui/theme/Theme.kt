package com.myweather.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 应用 Material Design 3 主题定义。
 *
 * 深色/浅色配色方案均基于 [Color.kt] 中定义的业务色，
 * 将侧边栏背景、卡片蒙层、预报文字等映射到 Material3 语义槽位，
 * 使所有 Composable 可通过 [MaterialTheme.colorScheme] 统一访问。
 */

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    secondary = Teal200,
    tertiary = Teal700,
    background = DrawerBgDark,
    surface = CardOverlayDark,
    onSurface = ForecastTextDark,
    onSurfaceVariant = SavedLabelDark
)

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    secondary = Teal200,
    tertiary = Teal700,
    background = DrawerBgLight,
    surface = CardOverlayLight,
    onSurface = ForecastTextLight,
    onSurfaceVariant = SavedLabelLight
)

/**
 * 应用顶层主题 Composable。
 *
 * 根据系统深色模式自动切换 [DarkColorScheme] / [LightColorScheme]，
 * 并应用 [Typography] 排版规范。
 *
 * @param darkTheme 是否启用深色模式，默认跟随系统
 * @param content 子组件树
 */
@Composable
fun MyWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}