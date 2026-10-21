package com.myweather.android.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自定义矢量图标集。
 *
 * 提供 Home、AddCity、Locate、Delete 四个图标，
 * 用于天气界面的顶部导航栏和城市列表的删除按钮。
 * 所有图标均通过 [ImageVector.Builder] 手动定义路径数据，
 * 避免引入额外的图标依赖库。
 */

/** 主页/导航图标，白色房屋形状，用于天气界面左上角返回城市列表。 */
val HomeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(10f, 20f)
            lineTo(10f, 14f)
            lineTo(14f, 14f)
            lineTo(14f, 20f)
            lineTo(19f, 20f)
            lineTo(19f, 12f)
            lineTo(22f, 12f)
            lineTo(12f, 3f)
            lineTo(2f, 12f)
            lineTo(5f, 12f)
            lineTo(5f, 20f)
            close()
        }
    }.build()
}

val AddCityIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AddCity",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(19f, 13f)
            lineTo(13f, 13f)
            lineTo(13f, 19f)
            lineTo(11f, 19f)
            lineTo(11f, 13f)
            lineTo(5f, 13f)
            lineTo(5f, 11f)
            lineTo(11f, 11f)
            lineTo(11f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 11f)
            lineTo(19f, 11f)
            close()
        }
    }.build()
}

val LocateIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Locate",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 8f)
            curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
            reflectiveCurveTo(9.79f, 16f, 12f, 16f)
            reflectiveCurveTo(16f, 14.21f, 16f, 12f)
            reflectiveCurveTo(14.21f, 8f, 12f, 8f)
            close()
            moveTo(20.94f, 11f)
            curveTo(20.48f, 6.83f, 17.17f, 3.52f, 13f, 3.06f)
            lineTo(13f, 1f)
            lineTo(11f, 1f)
            lineTo(11f, 3.06f)
            curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11f)
            lineTo(1f, 11f)
            lineTo(1f, 13f)
            lineTo(3.06f, 13f)
            curveTo(3.52f, 17.17f, 6.83f, 20.48f, 11f, 20.94f)
            lineTo(11f, 23f)
            lineTo(13f, 23f)
            lineTo(13f, 20.94f)
            curveTo(17.17f, 20.48f, 20.48f, 17.17f, 20.94f, 13f)
            lineTo(23f, 13f)
            lineTo(23f, 11f)
            lineTo(20.94f, 11f)
            close()
            moveTo(12f, 19f)
            curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
            reflectiveCurveTo(8.13f, 5f, 12f, 5f)
            reflectiveCurveTo(19f, 8.13f, 19f, 12f)
            reflectiveCurveTo(15.87f, 19f, 12f, 19f)
            close()
        }
    }.build()
}

/** 删除图标，红色叉号形状，用于已保存城市列表项的删除按钮。 */
val DeleteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Delete",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFEF5350)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(19f, 6.41f)
            lineTo(17.59f, 5f)
            lineTo(12f, 10.59f)
            lineTo(6.41f, 5f)
            lineTo(5f, 6.41f)
            lineTo(10.59f, 12f)
            lineTo(5f, 17.59f)
            lineTo(6.41f, 19f)
            lineTo(12f, 13.41f)
            lineTo(17.59f, 19f)
            lineTo(19f, 17.59f)
            lineTo(13.41f, 12f)
            close()
        }
    }.build()
}