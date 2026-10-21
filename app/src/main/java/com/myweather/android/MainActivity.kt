package com.myweather.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.myweather.android.ui.compose.MainNavigation
import com.myweather.android.ui.theme.MyWeatherTheme

/**
 * 应用主 Activity。
 *
 * 使用 Jetpack Compose 的 [setContent] 声明式 UI 方式，
 * 启用边到边（edge-to-edge）显示，在 [MyWeatherTheme] 主题下
 * 渲染 [MainNavigation] 作为整个应用的入口 Composable。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWeatherTheme {
                MainNavigation()
            }
        }
    }
}