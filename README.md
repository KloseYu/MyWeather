# MyWeather

一款基于彩云天气 API 和高德地图 POI 搜索的 Android 天气应用，支持多城市管理、逐小时/逐日预报图表展示，以及左右滑动切换城市。

## 功能特性

- **城市搜索**：基于高德地图 POI 文本搜索，输入关键词即可搜索国内外城市
- **多城市管理**：保存多个城市，在天气页左右滑动切换
- **实时天气**：温度、体感温度、风速、湿度、气压、能见度等
- **逐小时预报**：柱形 + 水波纹图表，横向滑动浏览 24 小时数据
- **7 日预报**：柱形 + 水波纹图表，展示每日最高/最低温区间
- **深色模式**：自动适配系统深色/浅色主题
- **华氏度切换**：支持摄氏度与华氏度显示
- **下拉刷新**：SwipeRefreshLayout 刷新当前城市天气数据

## 技术架构

```
MVVM (ViewModel + LiveData + View Binding)
```

| 层级 | 关键类 | 说明 |
|------|--------|------|
| UI | `WeatherActivity` / `WeatherPageFragment` | ViewPager2 承载多城市天气页 |
| UI | `PlaceFragment` / `PlaceAdapter` | 城市搜索与结果列表 |
| ViewModel | `WeatherViewModel` / `PlaceViewModel` | LiveData 驱动 UI 更新 |
| Logic | `Repository` | 统一数据仓库，协调网络与本地 |
| Network | `MyWeatherNetWork` | Retrofit 3.0 协程调用入口 |
| DAO | `PlaceDao` | SharedPreferences JSON 持久化 |
| View | `HourlyChartView` / `DailyChartView` | 自定义 Canvas 图表 |
| View | `NowLayout` | 手势识别：左滑/右滑切城市，竖滑驱动滚动 |

## 项目结构

```
com.myweather.android/
├── MainActivity.kt                    // 入口 Activity，判断是否有已保存城市
├── MyWeatherApplication.kt            // Application 初始化
├── logic/
│   ├── Repository.kt                  // 数据仓库单例
│   ├── dao/
│   │   └── PlaceDao.kt               // 城市本地持久化（SharedPreferences）
│   ├── model/
│   │   ├── Weather.kt                 // 天气数据模型
│   │   ├── Sky.kt                     // 天气图标与描述映射
│   │   ├── PlaceResponse.kt           // 高德 POI 响应模型
│   │   ├── RealtimeResponse.kt        // 彩云实时天气响应
│   │   ├── DailyResponse.kt           // 彩云逐日预报响应
│   │   ├── HourlyResponse.kt          // 彩云逐小时预报响应
│   │   ├── TempFormatter.kt           // 温度格式化工具
│   │   ├── WindFormatter.kt           // 风速格式化工具
│   │   └── WeatherFormatter.kt        // 天气数据格式化工具
│   └── network/
│       ├── MyWeatherNetWork.kt        // 网络访问统一入口
│       └── service/
│           ├── amap/                  // 高德地图 POI 搜索服务
│           ├── caiyun/                // 彩云天气 API 服务
│           └── google/                // Google Geocode 服务
└── ui/
    ├── place/
    │   ├── PlaceFragment.kt           // 城市搜索页
    │   ├── PlaceViewModel.kt          // 城市搜索 ViewModel
    │   ├── PlaceAdapter.kt            // 搜索结果适配器
    │   └── SavedPlaceAdapter.kt       // 已保存城市适配器
    ├── weather/
    │   ├── WeatherActivity.kt         // 天气主页（ViewPager2）
    │   ├── WeatherPageFragment.kt     // 单城市天气页
    │   ├── WeatherViewModel.kt        // 天气 ViewModel
    │   └── view/
    │       ├── NowLayout.kt           // 实时天气布局（手势识别）
    │       ├── NestedScrollView.kt    // 竖向滚动容器
    │       ├── NestedHorizontalScrollView.kt // 横向滚动容器
    │       ├── HourlyChartView.kt     // 逐小时图表（柱形+水波纹）
    │       └── DailyChartView.kt      // 逐日图表（柱形+水波纹）
```

## 依赖

| 库 | 版本 | 用途 |
|----|------|------|
| Retrofit | 3.0.0 | HTTP 网络请求 |
| converter-gson | 3.0.0 | JSON 解析 |
| kotlinx-coroutines | 1.11.0 | 协程异步 |
| lifecycle-livedata-ktx | 2.11.0 | LiveData 生命周期感知 |
| lifecycle-viewmodel-ktx | 2.11.0 | ViewModel 协程扩展 |
| ViewPager2 | 1.1.0 | 多城市滑动切换 |
| Material Components | 1.12.0 | Material Design 卡片等 |
| SwipeRefreshLayout | 1.2.0 | 下拉刷新 |

## API 说明

- **高德地图 POI 搜索**：用于城市搜索，需配置有效的高德 Web API Key
- **彩云天气 API**：用于获取实时天气、逐小时预报、7 日预报数据，需配置有效的彩云 Token
- **Google Geocode API**：非中国大陆地区使用的地理编码服务，需配置有效的 Google API Key

## 构建与运行

1. 使用 Android Studio 打开项目
2. 在 `AmapServiceCreator.kt` 中填入高德 API Key
3. 在 `CaiYunServiceCreator.kt` 中填入彩云天气 Token
4. 在 `GoogleServiceCreator.kt` 中填入 Google Geocode API Key
44. Sync Gradle → Run

## 环境要求

- minSdk 24 / targetSdk 36
- Kotlin + Java 11
- AndroidX
- View Binding