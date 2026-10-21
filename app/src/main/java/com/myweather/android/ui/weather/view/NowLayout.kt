package com.myweather.android.ui.weather.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.RelativeLayout
import android.widget.ScrollView
import kotlin.math.abs

// 实时天气卡片布局，扩展 RelativeLayout，支持左右滑动切换城市和手动竖向滚动
class NowLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    // 左滑回调（切换到下一个城市）
    var onSwipeLeft: (() -> Unit)? = null
    // 右滑回调（切换到上一个城市）
    var onSwipeRight: (() -> Unit)? = null

    // 手指按下时的原始坐标
    private var initialX = 0f
    private var initialY = 0f
    // 上一帧 Y 坐标，用于计算竖向滚动增量
    private var lastY = 0f
    // 是否已判定滑动方向
    private var directionDecided = false
    // 当前手势是否为横滑
    private var isHorizontalSwipe = false
    // 系统最小滑动距离阈值，避免误触
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.rawX
                initialY = ev.rawY
                lastY = ev.rawY
                directionDecided = false
                isHorizontalSwipe = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!directionDecided) {
                    // 根据位移量判断手势方向
                    val dx = abs(ev.rawX - initialX)
                    val dy = abs(ev.rawY - initialY)
                    if (dx > touchSlop || dy > touchSlop) {
                        directionDecided = true
                        isHorizontalSwipe = dx > dy
                    }
                }
                // 竖向滑动时手动驱动外层 ScrollView 滚动
                if (directionDecided && !isHorizontalSwipe) {
                    val scrollDelta = (lastY - ev.rawY).toInt()
                    findScrollView()?.scrollBy(0, scrollDelta)
                }
                lastY = ev.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                // 横滑结束时触发城市切换回调
                if (isHorizontalSwipe && directionDecided) {
                    val dx = ev.rawX - initialX
                    if (dx < -touchSlop) {
                        onSwipeLeft?.invoke()
                    } else if (dx > touchSlop) {
                        onSwipeRight?.invoke()
                    }
                }
                reset()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                reset()
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun reset() {
        directionDecided = false
        isHorizontalSwipe = false
    }

    // 向上遍历 View 树找到 ScrollView，用于手动驱动竖向滚动
    private fun findScrollView(): ScrollView? {
        var p = parent
        while (p != null) {
            if (p is ScrollView) return p
            p = p.parent
        }
        return null
    }
}