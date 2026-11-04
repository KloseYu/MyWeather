package com.myweather.android.ui.place.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.logic.model.Place
import com.myweather.android.ui.theme.PlacePalette

/**
 * 搜索结果中的城市项组件。
 *
 * 以卡片形式展示城市名称和地址，点击后选中该城市。
 * 配色由 [PlacePalette] 提供，深浅色模式切换不再在此重复判断。
 *
 * @param place 城市数据
 * @param palette 侧边栏配色
 * @param onClick 点击回调
 */
@Composable
fun PlaceItem(
    place: Place,
    palette: PlacePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardOverlay)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = place.name,
                fontSize = 20.sp,
                color = palette.placeName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = place.address,
                fontSize = 14.sp,
                color = palette.placeAddress,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}