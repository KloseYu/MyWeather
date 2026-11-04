package com.myweather.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 城市搜索侧边栏的配色集合。
 *
 * [com.myweather.android.ui.place.PlaceScreen] 及其列表项组件原先各自在 Composable 里
 * 写 `if (isDark) ... else ...` 选择颜色，同一套判断在多处重复；
 * 集中到这里后，页面只关心布局，配色随系统深色模式自动切换。
 */
@Immutable
data class PlacePalette(
    val actionBar: Color,
    val searchBg: Color,
    val searchHint: Color,
    val searchText: Color,
    val savedLabel: Color,
    val divider: Color,
    val savedName: Color,
    val savedAddress: Color,
    val cardOverlay: Color,
    val placeName: Color,
    val placeAddress: Color,
    val deleteButtonBg: Color,
    val deleteIcon: Color
)

private val DividerOnLight = Color(0x1F000000)
private val DividerOnDark = Color(0x33FFFFFF)

/** 浅色侧边栏底色是浅蓝（#E1F5FE），次要文字必须比中灰更深才够对比度 */
private val LabelOnLight = Color(0xFF546E7A)
private val LabelOnDark = Color(0xFFBDBDBD)

/** 已保存城市名：浅色模式必须用深色字，白色字压在浅蓝底上等于隐形 */
private val SavedNameOnLight = Color(0xFF263238)
private val SavedNameOnDark = Color.White

private val DeleteButtonBg = Color(0x22EF5350)
private val DeleteIconOnLight = Color(0xFFD32F2F)
private val DeleteIconOnDark = Color(0xFFEF5350)

@Composable
fun placePalette(isDark: Boolean = isSystemInDarkTheme()): PlacePalette {
    return if (isDark) {
        PlacePalette(
            actionBar = ActionBarDark,
            searchBg = SearchBgDark,
            searchHint = SearchHintDark,
            searchText = SearchTextDark,
            savedLabel = LabelOnDark,
            divider = DividerOnDark,
            savedName = SavedNameOnDark,
            savedAddress = LabelOnDark,
            cardOverlay = CardOverlayDark,
            placeName = SearchTextDark,
            placeAddress = LabelOnDark,
            deleteButtonBg = DeleteButtonBg,
            deleteIcon = DeleteIconOnDark
        )
    } else {
        PlacePalette(
            actionBar = ActionBarLight,
            searchBg = SearchBgLight,
            searchHint = SearchHintLight,
            searchText = SearchTextLight,
            savedLabel = LabelOnLight,
            divider = DividerOnLight,
            savedName = SavedNameOnLight,
            savedAddress = LabelOnLight,
            cardOverlay = CardOverlayLight,
            placeName = SearchTextLight,
            placeAddress = LabelOnLight,
            deleteButtonBg = DeleteButtonBg,
            deleteIcon = DeleteIconOnLight
        )
    }
}