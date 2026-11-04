package com.myweather.android.ui.place

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myweather.android.R
import com.myweather.android.logic.model.Place
import com.myweather.android.ui.place.components.PlaceItem
import com.myweather.android.ui.place.components.SavedPlaceItem
import com.myweather.android.ui.theme.LocateIcon
import com.myweather.android.ui.theme.placePalette

/**
 * 城市搜索与管理界面，作为侧边栏内容展示，
 * 对应传统版本的 PlaceFragment + fragment_place.xml。
 *
 * 只消费 [PlaceUiState]，不直接持有 ViewModel：
 * 搜索词、搜索结果、已保存城市都来自状态流，用户操作通过回调交回上层。
 *
 * 布局结构：
 * - 顶部搜索栏：包含城市名输入框和定位按钮；
 * - 已保存城市列表：显示所有已收藏城市，当前城市名前带 ● 标记，每项右侧有删除按钮；
 * - 搜索结果列表：输入搜索词后实时展示匹配城市。
 *
 * 配色来自 [placePalette]，随系统深色模式自动切换。
 *
 * @param uiState 侧边栏界面状态
 * @param currentLng 当前展示城市的经度，用于标记当前城市
 * @param currentLat 当前展示城市的纬度，用于标记当前城市
 * @param onSearchQueryChange 搜索词变化回调
 * @param onLocateClick 定位按钮点击回调
 * @param onPlaceClick 城市项点击回调，切换到该城市的天气信息
 * @param onDeletePlace 删除已保存城市回调
 * @param onSearchErrorShown 搜索失败提示已展示回调
 * @param onDrawerOpened 侧边栏打开回调，用于刷新已保存列表
 * @param isDrawerOpen 侧边栏是否打开
 */
@Composable
fun PlaceScreen(
    uiState: PlaceUiState,
    currentLng: String,
    currentLat: String,
    onSearchQueryChange: (String) -> Unit,
    onLocateClick: () -> Unit,
    onPlaceClick: (Place) -> Unit,
    onDeletePlace: (Place) -> Unit,
    onSearchErrorShown: () -> Unit,
    onDrawerOpened: () -> Unit,
    isDrawerOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = placePalette()
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) {
            onDrawerOpened()
            // 侧边栏只用于搜索与管理城市，打开即聚焦搜索框并弹出键盘，
            // 用户不必再点一次输入框才知道该做什么
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, "搜索失败: $message", Toast.LENGTH_LONG).show()
            onSearchErrorShown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(palette.actionBar)
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .focusRequester(searchFocusRequester)
                        .padding(start = 0.dp, end = 6.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = palette.searchText, fontSize = 14.sp),
                    cursorBrush = SolidColor(palette.searchText),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.searchBg)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text("搜索城市", color = palette.searchHint, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    }
                )
                IconButton(
                    onClick = onLocateClick,
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
                    if (uiState.savedPlaces.isNotEmpty()) {
                        Text(
                            text = "已保存城市",
                            fontSize = 13.sp,
                            color = palette.savedLabel,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.savedPlaces) { place ->
                                SavedPlaceItem(
                                    place = place,
                                    isCurrent = place.location.lng == currentLng &&
                                        place.location.lat == currentLat,
                                    palette = palette,
                                    onClick = { onPlaceClick(place) },
                                    onDelete = { onDeletePlace(place) }
                                )
                            }
                        }
                    }

                    if (uiState.searchQuery.isNotEmpty() && uiState.searchResults.isNotEmpty()) {
                        HorizontalDivider(color = palette.divider, thickness = 1.dp)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.searchResults) { place ->
                                PlaceItem(
                                    place = place,
                                    palette = palette,
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