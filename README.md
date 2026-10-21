# MyWeather — 彩云天气客户端

一款基于 **彩云天气 API** 的 Android 天气应用，使用 **Kotlin + Jetpack Compose** 构建，支持全球城市搜索、实时定位、逐时/逐日预报、生活指数展示，以及深色模式。

> 本项目为 `compose` 分支，采用 Jetpack Compose 完全重写 UI 层，替代了 `main` 分支中基于 View + ViewPager2 的传统实现。两个分支共享相同的 `logic` 层（ViewModel / Repository / Model / Network），仅 UI 层实现不同。

## 功能特性

| 功能 | 说明 |
|------|------|
| 城市搜索 | 支持全球城市关键词搜索，结果实时展示 |
| 一键定位 | 基于 GPS + 高德/Google 逆地理编码自动获取当前城市 |
| 多城市管理 | 侧边栏管理已保存城市，支持增删和切换 |
| 实时天气 | 温度、天气描述、AQI（中国/海外双标准）、风速、湿度 |
| 逐时预报 | 24 小时温度柱状图 + 水波纹效果，可水平滚动 |
| 逐日预报 | 7 日温度范围柱状图 + 水波纹效果，可水平滚动 |
| 生活指数 | 感冒、穿衣、实时紫外线、洗车（文字与传统版本完全同步） |
| 下拉刷新 | PullToRefreshBox 支持下拉刷新天气数据 |
| 左右滑动 | 天气界面左右滑动切换城市 |
| 深色模式 | 完整适配系统深色模式，所有界面自动切换配色 |
| 华氏度支持 | 美国等华氏度国家自动同时显示摄氏与华氏温度 |
| 数据缓存 | 网络请求失败时自动回退到上次缓存数据 |

## 技术栈

- **UI 框架**: Jetpack Compose + Material Design 3
- **架构**: MVVM（ViewModel + LiveData + Repository）
- **网络**: Retrofit 2 + Gson
- **异步**: Kotlin Coroutines + Flow
- **定位**: Android LocationManager + 高德/Google 逆地理编码
- **天气数据**: 彩云天气 API（realtime / daily / hourly）
- **最低 SDK**: 24（Android 7.0）
- **目标 SDK**: 36
- **JVM**: 21

## 项目结构

```
app/src/main/java/com/myweather/android/
├── MainActivity.kt                  # Compose 入口 Activity
├── MyWeatherApplication.kt          # 全局 Application，持有 API Key
├── logic/                           # ← 与 main 分支共享，无差异
│   ├── Repository.kt                # 数据仓库，封装网络 + 本地存储
│   ├── dao/
│   │   └── PlaceDao.kt              # SharedPreferences 城市持久化
│   ├── model/
│   │   ├── Weather.kt               # 聚合天气数据 + AQI 工具方法
│   │   ├── Sky.kt                   # 天气代码 → 图标/背景映射
│   │   ├── RealtimeResponse.kt      # 实时天气响应模型
│   │   ├── DailyResponse.kt         # 7日预报响应模型（含 LifeIndex）
│   │   ├── HourlyResponse.kt        # 24小时时预报响应模型
│   │   ├── PlaceResponse.kt         # 城市搜索响应模型
│   │   └── ...                     # 格式化工具类
│   └── network/
│       ├── MyWeatherNetWork.kt      # 统一网络访问入口
│       └── service/
│           ├── caiyun/              # 彩云天气 Retrofit Service
│           ├── amap/                # 高德逆地理编码 Service
│           └── google/              # Google 逆地理编码 Service
└── ui/
    ├── compose/                     # ← compose 分支新增，替代 weather/ 下的 View 实现
    │   ├── MainNavigation.kt        # 主导航（ModalNavigationDrawer 侧边栏 + 天气内容）
    │   ├── PlaceScreen.kt           # 城市搜索/管理界面（对应 PlaceFragment）
    │   ├── WeatherScreen.kt         # 天气详情界面（对应 WeatherPageFragment，含 Canvas 图表）
    │   └── WeatherIcons.kt          # 自定义矢量图标（Home / AddCity / Locate / Delete）
    ├── theme/
    │   ├── Color.kt                 # 配色常量（浅色/深色成对定义）
    │   ├── Theme.kt                 # Material3 主题定义
    │   └── Type.kt                  # 排版规范
    ├── place/
    │   └── PlaceViewModel.kt        # 城市搜索 ViewModel
    └── weather/
        ├── WeatherViewModel.kt      # 天气数据 ViewModel
        ├── WeatherActivity.kt       # 传统版本 Activity（main 分支使用）
        ├── WeatherPageFragment.kt   # 传统版本 Fragment（main 分支使用）
        └── view/                    # 传统版本自定义 View（DailyChartView 等）
```

## compose 分支与 main 分支的差异

| 方面 | main 分支 | compose 分支 |
|------|-----------|-------------|
| UI 框架 | View + DataBinding + ViewPager2 | Jetpack Compose + Material3 |
| Activity | AppCompatActivity + Fragment | ComponentActivity + setContent |
| 导航 | DrawerLayout + Fragment 切换 | ModalNavigationDrawer |
| 城市列表 | RecyclerView + Adapter | LazyColumn |
| 图表 | 自定义 View（DailyChartView 等） | Canvas Composable |
| 刷新 | SwipeRefreshLayout | PullToRefreshBox |
| 主题 | XML styles/themes | Compose Theme + ColorScheme |
| 边到边 | 手动配置 | enableEdgeToEdge() |
| 生活指数 | life_index.xml 硬编码标签 | LifeIndexSection Composable，文字与 XML版本 完全同步 |

### 生活指数文字同步说明

compose 分支的 `LifeIndexSection` 严格保持与传统版本 `life_index.xml` 一致：

| 项目 | XML 标签 | Compose label | 数据绑定 |
|------|---------|---------------|---------|
| 感冒 | `android:text="感冒"` | `"感冒"` | `isNotEmpty()` 守卫 |
| 穿衣 | `android:text="穿衣"` | `"穿衣"` | `isNotEmpty()` 守卫 |
| 实时紫外线 | `android:text="实时紫外线"` | `"实时紫外线"` | `isNotEmpty()` 守卫 |
| 洗车 | `android:text="洗车"` | `"洗车"` | `isNotEmpty()` 守卫 |

## 构建与运行

1. 在 Android Studio 中打开项目，同步 Gradle。
2. 配置 API Key（如需使用 Google 逆地理编码）：在 `MyWeatherApplication.kt` 中填写 `GOOGLE_KEY`。
3. 连接 Android 设备或模拟器，点击 Run。

## API 说明

| API | 用途 | Key 配置 |
|-----|------|----------|
| 彩云天气 | 实时/逐日/逐时天气数据 | `CaiYun_Key`（已内置） |
| 高德地图 | 国内逆地理编码 | `AMAP_KEY`（已内置） |
| Google Geocoding | 海外逆地理编码 | `GOOGLE_KEY`（需自行申请） |

> 国内定位优先使用高德 API，海外及港澳台地区需配置 Google API Key。

## 许可证

本项目仅供学习交流使用，请勿用于商业用途。天气数据版权归彩云天气所有。