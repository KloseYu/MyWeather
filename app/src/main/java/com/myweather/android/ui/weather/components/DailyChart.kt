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
import com.myweather.android.logic.model.getDayIcon
import com.myweather.android.logic.model.getSkyDesc
import com.myweather.android.ui.theme.dailyChartColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * 7 日预报图表。
 *
 * 使用 [Canvas] 在原生画布上绘制，每个日期刻度包含：
 * - 圆角温度范围柱状图（最高温到最低温），带水波纹填充效果；
 * - 柱顶日间天气图标（深色模式自动着白色）；
 * - 柱顶天气描述文字（如"多云转晴"）；
 * - 柱顶最高温文字（暖色系）、柱底最低温文字（冷色系）；
 * - 底部日期标签（MM/dd）。
 *
 * 柱体、波纹、文字颜色来自 [dailyChartColors]，本文件只负责几何计算与绘制。
 */
@Composable
fun DailyChart(
    maxTemps: List<Double>,
    minTemps: List<Double>,
    timestamps: List<Date>,
    daySkycons: List<String>,
    nightSkycons: List<String>,
    useFahrenheit: Boolean
) {
    val count = minOf(maxTemps.size, minTemps.size, timestamps.size, daySkycons.size, nightSkycons.size)
    if (count == 0) return

    val context = LocalContext.current
    val colors = dailyChartColors()

    val colWidthDp = 56
    val chartHeightDp = 250
    val totalWidthDp = (colWidthDp * count).dp

    val allTemps = maxTemps + minTemps
    val minT = allTemps.minOrNull() ?: 0.0
    val maxT = allTemps.maxOrNull() ?: 0.0
    val range = (maxT - minT).coerceAtLeast(1.0)
    val paddedMin = minT - range * 0.15
    val paddedMax = maxT + range * 0.15
    val paddedRange = paddedMax - paddedMin

    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

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

            val colW = chartW / count
            val barW = colW * 0.48f
            val barRadius = barW * 0.3f

            val iconPx = (14 * density).toInt()
            val iconPad = 2.dp.toPx()

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.bar.toArgb() }
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val maxTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 10f * density
                color = colors.maxText.toArgb()
                textAlign = Paint.Align.CENTER
            }
            val minTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 10f * density
                color = colors.minText.toArgb()
                textAlign = Paint.Align.CENTER
            }
            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 9f * density
                color = colors.timeText.toArgb()
                textAlign = Paint.Align.CENTER
            }
            val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textSize = 8f * density
                color = colors.descText.toArgb()
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until count) {
                val cx = padL + colW * i + colW / 2f
                val yMax = padT + chartH - ((maxTemps[i] - paddedMin) / paddedRange * chartH).toFloat()
                val yMin = padT + chartH - ((minTemps[i] - paddedMin) / paddedRange * chartH).toFloat()
                val barTop = yMax
                val barBottom = yMin
                val barH = barBottom - barTop

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

                val daySky = if (i < daySkycons.size) daySkycons[i] else "CLEAR_DAY"
                val nightSky = if (i < nightSkycons.size) nightSkycons[i] else daySky

                val iconRes = getDayIcon(daySky)
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

                val desc = getSkyDesc(daySky, nightSky)
                canvas.drawText(
                    desc, cx, barTop - iconPx - iconPad * 2 - 1.dp.toPx(), descPaint
                )

                canvas.drawText(
                    formatChartTemp(maxTemps[i], useFahrenheit),
                    cx,
                    barTop - iconPx - iconPad * 2 - 11.dp.toPx(),
                    maxTextPaint
                )
                canvas.drawText(
                    formatChartTemp(minTemps[i], useFahrenheit),
                    cx,
                    barBottom + 14.dp.toPx(),
                    minTextPaint
                )
            }

            for (i in 0 until count) {
                val cx = padL + colW * i + colW / 2f
                val label = dateFormat.format(timestamps[i])
                canvas.drawText(label, cx, h - 4.dp.toPx(), timePaint)
            }
        }
    }
}