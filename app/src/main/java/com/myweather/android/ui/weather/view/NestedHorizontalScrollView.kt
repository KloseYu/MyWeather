package com.myweather.android.ui.weather.view

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView

// 横向滚动容器，扩展 HorizontalScrollView 用于图表区域横向滑动浏览
class NestedHorizontalScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr)