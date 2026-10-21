package com.myweather.android.ui.weather.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

// 竖向滚动容器，扩展 ScrollView 用于天气页面整体竖向滚动
class NestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr)