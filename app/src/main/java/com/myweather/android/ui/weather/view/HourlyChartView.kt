package com.myweather.android.ui.weather.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.myweather.android.logic.model.getSky
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin
import kotlin.math.PI

// 逐小时预报图表，柱形 + 水波纹风格，每根柱子代表一小时的温度
class HourlyChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 温度列表（摄氏度）
    private var temperatures: List<Double> = emptyList()
    // 时间戳列表
    private var timestamps: List<Date> = emptyList()
    // 天气状况列表
    private var skycons: List<String> = emptyList()
    // 是否使用华氏度
    private var useFahrenheit: Boolean = false

    // 柱形画笔
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // 水波纹画笔
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // 温度文字画笔
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    // 时间文字画笔
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    // 图标背景画笔（保留字段，当前未使用）
    private val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // dp 转 px
    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
    // sp 转 px
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    // 设置图表数据并触发重绘
    fun setData(temps: List<Double>, times: List<Date>, skyconList: List<String>, fahrenheit: Boolean) {
        temperatures = temps
        timestamps = times
        skycons = skyconList
        useFahrenheit = fahrenheit
        requestLayout()
        invalidate()
    }

    // 根据数据点数量计算宽度，确保在 HorizontalScrollView 中能正确展开
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 每个数据点占 48dp 宽度
        val minW = if (temperatures.isNotEmpty()) {
            (temperatures.size * 48f * resources.displayMetrics.density).toInt()
        } else 0
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val w = resolveSize(minW.coerceAtLeast(suggestedMinimumWidth), widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        if (temperatures.isEmpty() || timestamps.isEmpty()) return

        // 判断深色/浅色模式
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 根据主题设置颜色
        val barColor: Int
        val waveColor1: Int
        val waveColor2: Int
        val iconBgColor: Int

        if (isNight) {
            barColor = Color.argb(60, 79, 195, 247)
            waveColor1 = Color.argb(180, 79, 195, 247)
            waveColor2 = Color.argb(80, 41, 121, 255)
            iconBgColor = Color.argb(40, 255, 255, 255)
            textPaint.color = Color.parseColor("#E0E0E0")
            timePaint.color = Color.parseColor("#BDBDBD")
        } else {
            barColor = Color.argb(40, 33, 150, 243)
            waveColor1 = Color.argb(200, 33, 150, 243)
            waveColor2 = Color.argb(100, 21, 101, 192)
            iconBgColor = Color.argb(25, 0, 0, 0)
            textPaint.color = Color.parseColor("#424242")
            timePaint.color = Color.parseColor("#757575")
        }

        barPaint.color = barColor
        textPaint.textSize = sp(11f)
        timePaint.textSize = sp(9f)
        iconBgPaint.color = iconBgColor

        val w = width.toFloat()
        val h = height.toFloat()
        // 内边距：左、右、上（留给图标+温度文字）、下（留给时间文字）
        val padL = dp(8f)
        val padR = dp(8f)
        val padT = dp(42f)
        val padB = dp(20f)
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // 计算温度范围，上下各留 15% 余量
        val minT = temperatures.minOrNull() ?: return
        val maxT = temperatures.maxOrNull() ?: return
        val range = if (maxT - minT < 1.0) 2.0 else (maxT - minT)
        val paddedMin = minT - range * 0.15
        val paddedMax = maxT + range * 0.15
        val paddedRange = paddedMax - paddedMin

        val n = temperatures.size
        // 每列宽度
        val colW = chartW / n
        // 柱子宽度为列宽的 52%
        val barW = colW * 0.52f
        // 圆角半径
        val barRadius = barW * 0.3f

        // 天气图标尺寸
        val iconPx = (14 * resources.displayMetrics.density).toInt()
        val iconPad = dp(2f)

        for (i in 0 until n) {
            val cx = padL + colW * i + colW / 2f
            // 柱子高度按温度比例映射
            val barH = ((temperatures[i] - paddedMin) / paddedRange * chartH).toFloat()
            val barTop = padT + chartH - barH
            val barBottom = padT + chartH

            // 绘制圆角柱形
            val barRect = RectF(cx - barW / 2, barTop, cx + barW / 2, barBottom)
            canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)

            // 柱子足够高时绘制水波纹效果
            if (barH > dp(4f)) {
                // 柱形裁剪路径
                val wavePath = Path()
                wavePath.addRoundRect(barRect, barRadius, barRadius, Path.Direction.CW)

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
                wavePaint.shader = LinearGradient(0f, waterLevel - waveAmplitude, 0f, barRect.bottom, waveColor1, waveColor2, Shader.TileMode.CLAMP)
                canvas.save()
                canvas.clipPath(wavePath)
                canvas.drawPath(waveFill, wavePaint)
                canvas.restore()
            }

            // 绘制天气图标
            if (i < skycons.size) {
                val icon: Drawable? = context.getDrawable(getSky(skycons[i]).icon)
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
            }

            // 绘制温度数值
            val tempStr = if (!useFahrenheit) {
                "${"%.0f".format(temperatures[i])}°"
            } else {
                val f = temperatures[i] * 9.0 / 5.0 + 32.0
                "${"%.0f".format(f)}°"
            }
            val tw = textPaint.measureText(tempStr)
            canvas.drawText(tempStr, cx - tw / 2, barTop - iconPx - iconPad * 2 - dp(2f), textPaint)
        }

        // 绘制底部时间标签
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        for (i in 0 until n) {
            val cx = padL + colW * i + colW / 2f
            val label = sdf.format(timestamps[i])
            val tw = timePaint.measureText(label)
            canvas.drawText(label, cx - tw / 2, h - dp(4f), timePaint)
        }
    }
}