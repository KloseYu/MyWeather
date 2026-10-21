package com.myweather.android.ui.compose

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.R
import com.myweather.android.logic.model.Place
import com.myweather.android.ui.place.PlaceViewModel

/**
 * 城市搜索与管理界面，作为侧边栏内容展示。
 *
 * 布局结构：
 * - 顶部搜索栏：包含城市名输入框和定位按钮；
 * - 已保存城市列表：显示所有已收藏城市，当前城市名前带 ● 标记，每项右侧有删除按钮；
 * - 搜索结果列表：输入搜索词后实时展示匹配城市。
 *
 * 深色/浅色模式通过 [isSystemInDarkTheme] 自动切换配色。
 * 搜索词变化时自动触发 [PlaceViewModel.searchPlaces]，
 * 侧边栏打开时自动刷新已保存城市列表。
 *
 * @param viewModel 城市搜索 ViewModel
 * @param currentLng 当前展示城市的经度，用于标记当前城市
 * @param currentLat 当前展示城市的纬度，用于标记当前城市
 * @param onPlaceClick 城市项点击回调，用于切换到该城市的天气信息
 * @param onLocateClick 定位按钮点击回调，用于触发定位流程
 * @param onPlaceDelete 城市删除回调，用于通知外部更新状态
 * @param isDrawerOpen 侧边栏是否打开，打开时刷新已保存列表
 */
@Composable
fun PlaceScreen(
    viewModel: PlaceViewModel,
    currentLng: String,
    currentLat: String,
    onPlaceClick: (Place) -> Unit,
    onLocateClick: () -> Unit,
    onPlaceDelete: (Place) -> Unit = {},
    isDrawerOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var searchQuery by remember { mutableStateOf("") }
    val searchResult by viewModel.placeLiveData.observeAsState()
    val savedPlaces = remember { mutableStateOf(viewModel.getSavedPlaceList()) }

    val searchPlaces = searchResult?.getOrNull() ?: emptyList()

    val actionBarColor = if (isDark) Color(0xFF172233) else Color(0xFF29B6F6)
    val searchBgColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)
    val searchHintColor = if (isDark) Color(0xFF888888) else Color(0xFF999999)
    val searchTextColor = if (isDark) Color.White else Color(0xFF333333)
    val savedLabelColor = if (isDark) Color(0xFFBDBDBD) else Color(0xFF757575)
    val dividerColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
    val savedNameColor = Color.White
    val savedAddressColor = if (isDark) Color(0xFFBDBDBD) else Color(0xFF757575)
    val cardOverlayColor = if (isDark) Color(0xD9404055) else Color(0xF0F2F4FA)
    val placeItemNameColor = if (isDark) Color.White else Color(0xFF333333)
    val placeItemAddressColor = if (isDark) Color(0xFFBDBDBD) else Color(0xFF757575)

    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) {
            savedPlaces.value = viewModel.getSavedPlaceList()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            viewModel.searchPlaces(searchQuery)
        }
    }

    LaunchedEffect(searchResult) {
        val result = searchResult
        if (result != null) {
            val places = result.getOrNull()
            if (places == null) {
                val msg = result.exceptionOrNull()?.message ?: "未知错误"
                Toast.makeText(context, "搜索失败: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(actionBarColor)
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .padding(start = 0.dp, end = 6.dp),
                    placeholder = { Text("搜索城市", color = searchHintColor, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = searchTextColor,
                        unfocusedTextColor = searchTextColor,
                        focusedContainerColor = searchBgColor,
                        unfocusedContainerColor = searchBgColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = searchTextColor
                    )
                )
                IconButton(
                    onClick = { onLocateClick() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = LocateIcon,
                        contentDescription = "定位",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Image(
                    painter = painterResource(id = R.drawable.bg_place),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    if (savedPlaces.value.isNotEmpty()) {
                        Text(
                            text = "已保存城市",
                            fontSize = 13.sp,
                            color = savedLabelColor,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(savedPlaces.value) { place ->
                                SavedPlaceItem(
                                    place = place,
                                    isCurrent = place.location.lng == currentLng && place.location.lat == currentLat,
                                    nameColor = savedNameColor,
                                    addressColor = savedAddressColor,
                                    onClick = { onPlaceClick(place) },
                                    onDelete = {
                                        viewModel.deletePlace(place)
                                        savedPlaces.value = viewModel.getSavedPlaceList()
                                        onPlaceDelete(place)
                                    }
                                )
                            }
                        }
                    }

                    if (searchQuery.isNotEmpty() && searchPlaces.isNotEmpty()) {
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(searchPlaces) { place ->
                                PlaceItem(
                                    place = place,
                                    cardColor = cardOverlayColor,
                                    nameColor = placeItemNameColor,
                                    addressColor = placeItemAddressColor,
                                    onClick = { onPlaceClick(place) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 搜索结果中的城市项组件。
 *
 * 以卡片形式展示城市名称和地址，点击后选中该城市。
 *
 * @param place 城市数据
 * @param cardColor 卡片背景色
 * @param nameColor 城市名文字颜色
 * @param addressColor 地址文字颜色
 * @param onClick 点击回调
 */
@Composable
private fun PlaceItem(
    place: Place,
    cardColor: Color,
    nameColor: Color,
    addressColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = place.name,
                fontSize = 20.sp,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = place.address,
                fontSize = 14.sp,
                color = addressColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 已保存城市列表项组件。
 *
 * 以行形式展示城市名称和地址，当前城市名前带 ● 标记并加粗。
 * 右侧提供圆形红色删除按钮。
 *
 * @param place 城市数据
 * @param isCurrent 是否为当前展示的城市
 * @param nameColor 城市名文字颜色
 * @param addressColor 地址文字颜色
 * @param onClick 点击回调，切换到该城市
 * @param onDelete 删除回调，移除该城市
 */
@Composable
private fun SavedPlaceItem(
    place: Place,
    isCurrent: Boolean,
    nameColor: Color,
    addressColor: Color,
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
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = place.address,
                fontSize = 12.sp,
                color = addressColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x22EF5350))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DeleteIcon,
                contentDescription = "删除",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp).padding(6.dp)
            )
        }
    }
}