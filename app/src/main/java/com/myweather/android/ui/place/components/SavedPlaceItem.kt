package com.myweather.android.ui.place.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.logic.model.Place
import com.myweather.android.ui.theme.DeleteIcon
import com.myweather.android.ui.theme.PlacePalette

/**
 * 已保存城市列表项组件。
 *
 * 以行形式展示城市名称和地址，当前城市名前带 ● 标记并加粗。
 * 右侧提供圆形红色删除按钮。
 *
 * @param place 城市数据
 * @param isCurrent 是否为当前展示的城市
 * @param palette 侧边栏配色
 * @param onClick 点击回调，切换到该城市
 * @param onDelete 删除回调，移除该城市
 */
@Composable
fun SavedPlaceItem(
    place: Place,
    isCurrent: Boolean,
    palette: PlacePalette,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrent) "● ${place.name}" else place.name,
                fontSize = 16.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = palette.savedName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = place.address,
                fontSize = 12.sp,
                color = palette.savedAddress,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.deleteButtonBg)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DeleteIcon,
                contentDescription = "删除",
                tint = palette.deleteIcon,
                modifier = Modifier
                    .size(24.dp)
                    .padding(6.dp)
            )
        }
    }
}