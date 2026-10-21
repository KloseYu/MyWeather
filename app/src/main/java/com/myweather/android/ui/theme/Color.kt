package com.myweather.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 应用配色常量。
 *
 * 包含 Material Design 3 主题色和自定义业务色。
 * 业务色按浅色/深色模式成对定义，在 Composable 中根据
 * [isSystemInDarkTheme] 选择对应色值。
 */

// Material Design 3 基础色
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)
val Teal700 = Color(0xFF018786)

// 城市搜索栏背景色
val ActionBarLight = Color(0xFF29B6F6)
val ActionBarDark = Color(0xFF172233)

// 侧边栏背景色
val DrawerBgLight = Color(0xFFE1F5FE)
val DrawerBgDark = Color(0xFF1B2635)

// 天气界面文字色（始终白色，叠加在天气背景图上）
val WeatherText = Color.White

// 预报卡片文字色
val ForecastTextLight = Color(0xFF37474F)
val ForecastTextDark = Color(0xFFE0E0E0)

// 搜索输入框背景色
val SearchBgLight = Color(0xFFF0F0F0)
val SearchBgDark = Color(0xFF2C2C2C)

// 搜索输入框提示文字色
val SearchHintLight = Color(0xFF999999)
val SearchHintDark = Color(0xFF888888)

// 搜索输入框文字色
val SearchTextLight = Color(0xFF333333)
val SearchTextDark = Color.White

// 分隔线色
val DividerLight = Color(0xFFE0E0E0)
val DividerDark = Color(0xFF3C3C3C)

// "已保存城市"标签色
val SavedLabelLight = Color(0xFF666666)
val SavedLabelDark = Color(0xB3FFFFFF)

// 搜索结果卡片蒙层色
val CardOverlayLight = Color(0xF0F2F4FA)
val CardOverlayDark = Color(0xD9404055)