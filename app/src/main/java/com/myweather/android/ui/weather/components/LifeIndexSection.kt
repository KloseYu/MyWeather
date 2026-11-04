package com.myweather.android.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.R
import com.myweather.android.logic.model.DailyResponse

/**
 * 生活指数区域，对应传统版本的 life_index.xml。
 *
 * 以 2×2 网格展示四项生活指数：感冒、穿衣、实时紫外线、洗车。
 * 文字标签与 life_index.xml 中的硬编码文本完全一致，
 * 数据绑定沿用传统版本的 [List.isNotEmpty] 守卫模式：
 * 仅当列表非空时取 [0].desc，否则显示空字符串。
 */
@Composable
fun LifeIndexSection(lifeIndex: DailyResponse.LifeIndex) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Text(
                text = "生活指数",
                modifier = Modifier.padding(start = 15.dp, top = 20.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                LifeIndexItem(
                    iconRes = R.drawable.ic_coldrisk,
                    label = "感冒",
                    desc = if (lifeIndex.coldRisk.isNotEmpty()) lifeIndex.coldRisk[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
                LifeIndexItem(
                    iconRes = R.drawable.ic_dressing,
                    label = "穿衣",
                    desc = if (lifeIndex.dressing.isNotEmpty()) lifeIndex.dressing[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                LifeIndexItem(
                    iconRes = R.drawable.ic_ultraviolet,
                    label = "实时紫外线",
                    desc = if (lifeIndex.ultraviolet.isNotEmpty()) lifeIndex.ultraviolet[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
                LifeIndexItem(
                    iconRes = R.drawable.ic_carwashing,
                    label = "洗车",
                    desc = if (lifeIndex.carWashing.isNotEmpty()) lifeIndex.carWashing[0].desc else "",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 单项生活指数组件，由图标、标签和描述文字水平排列组成。 */
@Composable
private fun LifeIndexItem(iconRes: Int, label: String, desc: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier
                .padding(start = 20.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier.padding(start = 20.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}