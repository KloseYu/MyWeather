# MyWeather — 彩云天气客户端

一款基于 **彩云天气 API** 和 **高德地图 POI 搜索** 的 Android 天气应用，采用 Kotlin + View + DataBinding 构建，支持多城市管理、逐小时/逐日预报图表展示、左右滑动切换城市，以及深色模式。

> 本项目为 `main` 分支，采用传统 View 体系实现 UI。`compose` 分支使用 Jetpack Compose 重写了 UI 层，两个分支共享相同的 `logic` 层。

## 功能特性

| 功能 | 说明 |
|------|------|
| 城市搜索 | 基于高德地图 POI 文本搜索，输入关键词即可搜索国内外城市 |
| 一键定位 | 基于 GPS + 高德/Google 逆地理编码自动获取当前城市 |
| 多城市管理 | 保存多个城市，在天气页左右滑动切换 |
| 实时天气 | 温度、体感温度、风速、湿度、气压、能见度等 |
| 逐时预报 | 柱形 + 水波纹图表，横向滑动浏览 24 小时数据 |
| 逐日预报 | 柱形 + 水波纹图表，展示每日最高/最低温区间 |
| 生活指数 | 感冒、穿衣、实时紫外线、洗车 |
| 下拉刷新 | SwipeRefreshLayout 刷新当前城市天气数据 |
| 左右滑动 | 天气界面左右滑动切换城市 |
| 深色模式 | 自动适配系统深色/浅色主题 |
| 华氏度支持 | 美国等华氏度国家自动同时显示摄氏与华氏温度 |
| 数据缓存 | 网络请求失败时自动回退到上次缓存数据 |

## 技术栈

- **UI 框架**: View + DataBinding + ViewPager2
- **架构**: MVVM（ViewModel + LiveData + Repository）
- **网络**: Retrofit 3.0 + Gson
- **异步**: Kotlin Coroutines
- **定位**: Android LocationManager + 高德/Google 逆地理编码
- **天气数据**: 彩云天气 API（realtime / daily / hourly）
- **最低 SDK**: 24（Android 7.0）
- **目标 SDK**: 36

## 项目结构

```
app/src/main/java/com/myweather/android/
├── MainActivity.kt                    # 入口 Activity，判断是否有已保存城市
├── MyWeatherApplication.kt            # 全局 Application，API Key 集中配置
├── logic/                             # ← 与 compose 分支共享，无差异
│   ├── Repository.kt                  # 数据仓库单例
│   ├── dao/
│   │   └── PlaceDao.kt               # 城市本地持久化（SharedPreferences）
│   ├── model/
│   │   ├── Weather.kt                 # 天气数据模型
│   │   ├── Sky.kt                     # 天气图标与描述映射
│   │   ├── PlaceResponse.kt           # 高德 POI 响应模型
│   │   ├── RealtimeResponse.kt        # 彩云实时天气响应
│   │   ├── DailyResponse.kt           # 彩云逐日预报响应（含 LifeIndex）
│   │   ├── HourlyResponse.kt          # 彩云逐小时预报响应
│   │   └── ...                       # 格式化工具类
│   └── network/
│       ├── MyWeatherNetWork.kt        # 网络访问统一入口
│       └── service/
│           ├── amap/                  # 高德地图 POI 搜索服务
│           ├── caiyun/                # 彩云天气 API 服务
│           └── google/                # Google Geocode 服务
└── ui/
    ├── place/
    │   ├── PlaceFragment.kt           # 城市搜索页
    │   ├── PlaceViewModel.kt          # 城市搜索 ViewModel
    │   ├── PlaceAdapter.kt            # 搜索结果适配器
    │   └── SavedPlaceAdapter.kt       # 已保存城市适配器
    └── weather/
        ├── WeatherActivity.kt         # 天气主页（ViewPager2）
        ├── WeatherPageFragment.kt     # 单城市天气页
        ├── WeatherViewModel.kt        # 天气 ViewModel
        └── view/
            ├── NowLayout.kt           # 实时天气布局（手势识别）
            ├── NestedScrollView.kt    # 竖向滚动容器
            ├── NestedHorizontalScrollView.kt # 横向滚动容器
            ├── HourlyChartView.kt     # 逐小时图表（柱形+水波纹）
            └── DailyChartView.kt      # 逐日图表（柱形+水波纹）
```

## 构建与运行

1. 克隆仓库：
   ```bash
   git clone https://github.com/KloseYu/MyWeather.git
   ```
2. 在 Android Studio 中打开项目，同步 Gradle。
3. **配置 API Key**（见下方说明）。
4. 连接 Android 设备或模拟器，点击 Run。

## API Key 配置

项目不内置任何 API Key，所有 Key 均以占位符形式存在，需自行申请并填入 `MyWeatherApplication.kt`：

```kotlin
companion object {
    const val CaiYun_Key = "YOUR_CAIYUN_KEY"    // 彩云天气
    const val AMAP_KEY   = "YOUR_AMAP_KEY"      // 高德地图
    const val GOOGLE_KEY = " "                   // Google Geocoding（可选）
}
```

同时需要将对应 Key 填入以下位置（保持一致）：

| 文件 | 常量 | 对应 Key |
|------|------|---------|
| `WeatherService.kt` → `CaiYunToken.token` | `"YOUR_CAIYUN_KEY"` | 彩云天气 |
| `AmapService.kt` → `AmapApiKey.KEY` | `"YOUR_AMAP_KEY"` | 高德地图 |

### 申请地址

| API | 用途 | 申请地址 |
|-----|------|---------|
| 彩云天气 | 实时/7日/24小时天气数据 | https://www.caiyunapp.com/h5/ |
| 高德地图 | 国内逆地理编码 | https://lbs.amap.com/ |
| Google Geocoding | 海外及港澳台逆地理编码（可选） | Google Cloud Console |

> 国内定位优先使用高德 API，海外及港澳台地区需额外配置 Google API Key。

## 许可证

本项目仅供学习交流使用，请勿用于商业用途。天气数据版权归彩云天气所有。