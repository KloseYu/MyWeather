package com.myweather.android.ui.weather.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.toColorInt
import com.myweather.android.logic.model.getDayIcon
import com.myweather.android.logic.model.getSkyDesc
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

// 逐日预报图表，柱形 + 水波纹风格，每根柱子代表一天的最高/最低温区间
class DailyChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 每日最高温列表
    private var maxTemps: List<Double> = emptyList()
    // 每日最低温列表
    private var minTemps: List<Double> = emptyList()
    // 日期时间戳列表
    private var timestamps: List<Date> = emptyList()
    // 白天天气状况列表
    private var daySkycons: List<String> = emptyList()
    // 夜间天气状况列表
    private var nightSkycons: List<String> = emptyList()
    // 是否使用华氏度
    private var useFahrenheit: Boolean = false

    // 柱形画笔
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // 水波纹画笔
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // 最高温文字画笔
    private val maxTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    // 最低温文字画笔
    private val minTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    // 日期文字画笔
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    // 图标背景画笔（保留字段，当前未使用）
    private val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    // 天气描述文字画笔
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // dp 转 px
    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
    // sp 转 px
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    // 设置图表数据并触发重绘
    fun setData(
        maxT: List<Double>, minT: List<Double>,
        times: List<Date>, daySkyconList: List<String>, nightSkyconList: List<String>, fahrenheit: Boolean
    ) {
        maxTemps = maxT
        minTemps = minT
        timestamps = times
        daySkycons = daySkyconList
        nightSkycons = nightSkyconList
        useFahrenheit = fahrenheit
        requestLayout()
        invalidate()
    }

    // 根据数据点数量计算宽度，确保在 HorizontalScrollView 中能正确展开
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 每个数据点占 56dp 宽度
        val minW = if (maxTemps.isNotEmpty()) {
            (maxTemps.size * 56f * resources.displayMetrics.density).toInt()
        } else 0
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val w = resolveSize(minW.coerceAtLeast(suggestedMinimumWidth), widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        if (maxTemps.isEmpty() || minTemps.isEmpty() || timestamps.isEmpty()) return

        // 判断深色/浅色模式
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 根据主题设置颜色
        val barColor: Int
        val waveColor1: Int
        val waveColor2: Int
        val iconBgColor: Int

        if (isNight) {
            barColor = Color.argb(55, 255, 138, 101)
            waveColor1 = Color.argb(170, 255, 112, 67)
            waveColor2 = Color.argb(70, 244, 81, 30)
            iconBgColor = Color.argb(40, 255, 255, 255)
            maxTextPaint.color = "#FFCDD2".toColorInt()
            minTextPaint.color = "#BBDEFB".toColorInt()
            timePaint.color = "#BDBDBD".toColorInt()
            descPaint.color = "#BDBDBD".toColorInt()
        } else {
            barColor = Color.argb(35, 255, 87, 34)
            waveColor1 = Color.argb(190, 255, 87, 34)
            waveColor2 = Color.argb(90, 230, 57, 0)
            iconBgColor = Color.argb(25, 0, 0, 0)
            maxTextPaint.color = "#C62828".toColorInt()
            minTextPaint.color = "#1565C0".toColorInt()
            timePaint.color = "#757575".toColorInt()
            descPaint.color = "#757575".toColorInt()
        }

        barPaint.color = barColor
        maxTextPaint.textSize = sp(10f)
        minTextPaint.textSize = sp(10f)
        timePaint.textSize = sp(9f)
        descPaint.textSize = sp(8f)
        iconBgPaint.color = iconBgColor

        val w = width.toFloat()
        val h = height.toFloat()
        // 内边距
        val padL = dp(8f)
        val padR = dp(8f)
        val padT = dp(42f)
        val padB = dp(20f)
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // 计算全局温度范围（最高温+最低温），上下各留 15% 余量
        val allTemps = maxTemps + minTemps
        val minT = allTemps.minOrNull() ?: return
        val maxT = allTemps.maxOrNull() ?: return
        val range = if (maxT - minT < 1.0) 2.0 else (maxT - minT)
        val paddedMin = minT - range * 0.15
        val paddedMax = maxT + range * 0.15
        val paddedRange = paddedMax - paddedMin

        val n = maxTemps.size
        // 每列宽度
        val colW = chartW / n
        // 柱子宽度为列宽的 48%
        val barW = colW * 0.48f
        // 圆角半径
        val barRadius = barW * 0.3f

        // 天气图标尺寸
        val iconPx = (14 * resources.displayMetrics.density).toInt()
        val iconPad = dp(2f)

        // 温度值到 Y 坐标的映射函数
        fun yPos(temp: Double) = padT + chartH - ((temp - paddedMin) / paddedRange * chartH).toFloat()

        for (i in 0 until n) {
            val cx = padL + colW * i + colW / 2f
            // 柱子从最高温位置画到最低温位置
            val yMax = yPos(maxTemps[i])
            val yMin = yPos(minTemps[i])
            val barTop = yMax
            val barBottom = yMin
            val barH = barBottom - barTop

            // 绘制圆角柱形
            val barRect = RectF(cx - barW / 2, barTop, cx + barW / 2, barBottom)
            canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)

            // 柱子足够高时绘制水波纹效果
            if (barH > dp(4f)) {
                // 柱形裁剪路径
                val clipPath = Path()
                clipPath.addRoundRect(barRect, barRadius, barRadius, Path.Direction.CW)

                // 水波纹填充路径：正弦曲线模拟水面
                val waveFill = Path()
                val waveAmplitude = barW * 0.12f
                val waveFreq = 2.5f
                val waterLevel = barTop + barH * 0.35f

                waveFill.moveTo(barRect.left, barRect.bottom)
                waveFill.lineTo(barRect.left, waterLevel)
                val steps = 20
                for (s in 0..steps) {
                    val ratio = s.toFloat() / steps
                    val px = barRect.left + barW * ratio
                    val py = waterLevel + waveAmplitude * sin(ratio * waveFreq * PI.toFloat() + i * 0.8f).toFloat()
                    waveFill.lineTo(px, py)
                }
                waveFill.lineTo(barRect.right, barRect.bottom)
                waveFill.close()

                // 渐变着色：从水面到底部
                wavePaint.shader = LinearGradient(
                    0f,
                    waterLevel - waveAmplitude,
                    0f,
                    barRect.bottom,
                    waveColor1,
                    waveColor2,
                    Shader.TileMode.CLAMP
                )
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawPath(waveFill, wavePaint)
                canvas.restore()
            }

            // 绘制天气图标
            val daySky = if (i < daySkycons.size) daySkycons[i] else "CLEAR_DAY"
            val nightSky = if (i < nightSkycons.size) nightSkycons[i] else daySky

            val iconRes = getDayIcon(daySky)
            val icon: Drawable? = context.getDrawable(iconRes)
            icon?.let {
                if (isNight) {
                    it.mutate().colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                }
                val bgCx = cx
                val bgCy = barTop - iconPx / 2f - iconPad
                val left = (bgCx - iconPx / 2).toInt()
                val top = (bgCy - iconPx / 2).toInt()
                it.setBounds(left, top, left + iconPx, top + iconPx)
                it.draw(canvas)
            }

            // 绘制天气描述文字（如"晴转多云"）
            val desc = getSkyDesc(daySky, nightSky)
            val descW = descPaint.measureText(desc)
            canvas.drawText(desc, cx - descW / 2, barTop - iconPx - iconPad * 2 - dp(1f), descPaint)

            // 绘制最高温/最低温数值
            val maxStr = formatTemp(maxTemps[i])
            val minStr = formatTemp(minTemps[i])
            val maxTw = maxTextPaint.measureText(maxStr)
            val minTw = minTextPaint.measureText(minStr)
            canvas.drawText(maxStr, cx - maxTw / 2, barTop - iconPx - iconPad * 2 - dp(11f), maxTextPaint)
            canvas.drawText(minStr, cx - minTw / 2, barBottom + dp(14f), minTextPaint)
        }

        // 绘制底部日期标签
        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
        for (i in 0 until n) {
            val cx = padL + colW * i + colW / 2f
            val label = sdf.format(timestamps[i])
            val tw = timePaint.measureText(label)
            canvas.drawText(label, cx - tw / 2, h - dp(4f), timePaint)
        }
    }

    // 格式化温度：摄氏度或华氏度
    private fun formatTemp(celsius: Double): String {
        return if (!useFahrenheit) {
            "${"%.0f".format(celsius)}°"
        } else {
            val f = celsius * 9.0 / 5.0 + 32.0
            "${"%.0f".format(f)}°"
        }
    }
}