package com.myweather.android.ui.weather.components

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myweather.android.logic.model.formatChartTemp
import com.myweather.android.logic.model.getSky
import com.myweather.android.ui.theme.hourlyChartColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * 24 小时预报图表。
 *
 * 使用 [Canvas] 在原生画布上绘制，每个时间刻度包含：
 * - 圆角温度柱状图，带水波纹填充效果；
 * - 柱顶天气图标（深色模式自动着白色）；
 * - 柱顶温度文字；
 * - 底部时间标签（HH:mm）。
 *
 * 柱体、波纹、文字颜色来自 [hourlyChartColors]，本文件只负责几何计算与绘制。
 */
@Composable
fun HourlyChart(
    temperatures: List<Double>,
    timestamps: List<Date>,
    skycons: List<String>,
    useFahrenheit: Boolean
) {
    val n = minOf(temperatures.size, timestamps.size, skycons.size)
    if (n == 0) return

    val context = LocalContext.current
    val colors = hourlyChartColors()

    val colWidthDp = 48
    val chartHeightDp = 230
    val totalWidthDp = (colWidthDp * n).dp

    val minT = temperatures.minOrNull() ?: 0.0
    val maxT = temperatures.maxOrNull() ?: 0.0
    val range = (maxT - minT).coerceAtLeast(1.0)
    val paddedMin = minT - range * 0.15
    val paddedMax = maxT + range * 0.15
    val paddedRange = paddedMax - paddedMin

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .width(totalWidthDp)
            .height(chartHeightDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val density = this.density

            val w = size.width
            val h = size.height
            val padL = 8.dp.toPx()
            val padR = 8.dp.toPx()
            val padT = 42.dp.toPx()
            val padB = 20.dp.toPx()
            val chartW = w - padL - padR
            val chartH = h - padT - padB

            val colW = chartW / n
            val barW = colW * 0.52f
            val barRadius = barW * 0.3f

            val iconPx = (14 * density).toInt()
            val iconPad = 2.dp.toPx()

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.bar.toArgb() }
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 11f * density
                color = colors.valueText.toArgb()
                textAlign = Paint.Align.CENTER
            }
            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 9f * density
                color = colors.timeText.toArgb()
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until n) {
                val cx = padL + colW * i + colW / 2f
                val temp = temperatures[i]
                val barH = ((temp - paddedMin) / paddedRange * chartH).toFloat()
                val barTop = padT + chartH - barH
                val barBottom = padT + chartH

                val barRect = RectF(cx - barW / 2, barTop, cx + barW / 2, barBottom)
                canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)

                if (barH > 4.dp.toPx()) {
                    val waterLevel = barTop + barH * 0.35f
                    val waveAmplitude = barW * 0.12f
                    val waveFreq = 2.5f

                    val clipPath = Path()
                    clipPath.addRoundRect(barRect, barRadius, barRadius, Path.Direction.CW)

                    val waveFill = Path()
                    waveFill.moveTo(barRect.left, barRect.bottom)
                    waveFill.lineTo(barRect.left, waterLevel)
                    val steps = 20
                    for (s in 0..steps) {
                        val ratio = s.toFloat() / steps
                        val px = barRect.left + barW * ratio
                        val py = waterLevel + waveAmplitude * sin(
                            ratio * waveFreq * PI.toFloat() + i * 0.8f
                        )
                        waveFill.lineTo(px, py)
                    }
                    waveFill.lineTo(barRect.right, barRect.bottom)
                    waveFill.close()

                    wavePaint.shader = LinearGradient(
                        0f, waterLevel - waveAmplitude, 0f, barRect.bottom,
                        colors.waveStart.toArgb(), colors.waveEnd.toArgb(), Shader.TileMode.CLAMP
                    )

                    canvas.save()
                    canvas.clipPath(clipPath)
                    canvas.drawPath(waveFill, wavePaint)
                    canvas.restore()
                }

                if (i < skycons.size) {
                    val iconRes = getSky(skycons[i]).icon
                    val iconDrawable = context.getDrawable(iconRes)
                    iconDrawable?.let {
                        if (colors.isNight) {
                            it.mutate().colorFilter = PorterDuffColorFilter(
                                android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN
                            )
                        }
                        val bgCy = barTop - iconPx / 2f - iconPad
                        val left = (cx - iconPx / 2).toInt()
                        val top = (bgCy - iconPx / 2).toInt()
                        it.setBounds(left, top, left + iconPx, top + iconPx)
                        it.draw(canvas)
                    }
                }

                canvas.drawText(
                    formatChartTemp(temperatures[i], useFahrenheit),
                    cx,
                    barTop - iconPx - iconPad * 2 - 2.dp.toPx(),
                    textPaint
                )
            }

            for (i in 0 until n) {
                val cx = padL + colW * i + colW / 2f
                val label = timeFormat.format(timestamps[i])
                canvas.drawText(label, cx, h - 4.dp.toPx(), timePaint)
            }
        }
    }
}