package com.myweather.android.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.myweather.android.R
import com.myweather.android.databinding.FragmentWeatherPageBinding
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.model.formatCelsius
import com.myweather.android.logic.model.formatFahrenheit
import com.myweather.android.logic.model.formatHumidityText
import com.myweather.android.logic.model.formatWindText
import com.myweather.android.logic.model.isFahrenheitCountry
import com.myweather.android.ui.place.PlaceFragment
import com.myweather.android.logic.model.getSky
import com.myweather.android.logic.model.getSkyDesc

/**
 * 天气页面 Fragment，展示单个城市的实时天气、逐小时预报、逐日预报及生活指数。
 * 由 ViewPager2 承载，每个城市一个实例。
 */
class WeatherPageFragment : Fragment() {

    private val viewModel by viewModels<WeatherViewModel>()

    private var _binding: FragmentWeatherPageBinding? = null
    private val binding get() = _binding!!

    private var placeLng: String = ""
    private var placeLat: String = ""
    private var placeNameStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeLng = arguments?.getString(ARG_LNG) ?: ""
        placeLat = arguments?.getString(ARG_LAT) ?: ""
        placeNameStr = arguments?.getString(ARG_NAME) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置定位信息并判断温度单位
        viewModel.locationLng = placeLng
        viewModel.locationLat = placeLat
        viewModel.placeName = placeNameStr
        val lng = placeLng.toDoubleOrNull() ?: 0.0
        val lat = placeLat.toDoubleOrNull() ?: 0.0
        viewModel.isFahrenheitCountry = isFahrenheitCountry(lng, lat)

        // 导航按钮：定位当前城市
        binding.includeNow.navBtn.setOnClickListener {
            val act = activity as? WeatherActivity
            val placeFragment = act?.supportFragmentManager?.findFragmentById(R.id.placeFragment) as? PlaceFragment
            if (placeFragment != null) {
                placeFragment.tryLocateAndRefresh(act)
            } else {
                act?.binding?.drawerLayout?.openDrawer(androidx.core.view.GravityCompat.START)
            }
        }

        // 添加按钮：打开侧边栏搜索新城市
        binding.includeNow.addBtn.setOnClickListener {
            val act = activity as? WeatherActivity
            act?.binding?.drawerLayout?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        // 下拉刷新
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_500)
        binding.swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }

        setupNowSwipe()

        // 观察天气数据变化
        viewModel.weatherLiveData.observe(viewLifecycleOwner) { result ->
            val weather = result.getOrNull()
            if (weather != null) {
                viewModel.putCachedWeather(viewModel.locationLng, viewModel.locationLat, weather)
                showWeatherInfo(weather)
            } else {
                Toast.makeText(activity, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
                if (binding.weatherLayout.visibility != View.VISIBLE) {
                    binding.weatherLayout.visibility = View.VISIBLE
                }
            }
            binding.swipeRefresh.isRefreshing = false
        }

        refreshWeather()
    }

    /**
     * 设置 NowLayout 的滑动手势回调：
     * 左滑 → 下一个城市，右滑 → 上一个城市。
     */
    private fun setupNowSwipe() {
        binding.includeNow.nowLayout.onSwipeLeft = {
            val act = activity as? WeatherActivity
            if (act != null) {
                val viewPager = act.binding.viewPager
                if (act.isViewPagerReady()) {
                    val next = viewPager.currentItem + 1
                    if (next < (viewPager.adapter?.itemCount ?: 0)) {
                        viewPager.setCurrentItem(next, true)
                    }
                }
            }
        }
        binding.includeNow.nowLayout.onSwipeRight = {
            val act = activity as? WeatherActivity
            if (act != null) {
                val viewPager = act.binding.viewPager
                if (act.isViewPagerReady()) {
                    val prev = viewPager.currentItem - 1
                    if (prev >= 0) {
                        viewPager.setCurrentItem(prev, true)
                    }
                }
            }
        }
    }

    // 优先显示缓存天气，然后发起网络请求刷新
    fun refreshWeather() {
        val cached = viewModel.getCachedWeather(viewModel.locationLng, viewModel.locationLat)
        if (cached != null) {
            showWeatherInfo(cached)
        }
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        if (cached == null) {
            binding.swipeRefresh.isRefreshing = true
        }
    }

    /**
     * 将 Weather 数据绑定到各个 UI 区域：
     * 实时天气（温度/天气/空气质量/风力/湿度）、逐小时图表、逐日图表、生活指数。
     */
    private fun showWeatherInfo(weather: Weather) {
        binding.includeNow.placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily

        // 温度显示：根据国家决定是否同时显示华氏度
        val celsiusStr = formatCelsius(realtime.temperature)
        if (viewModel.isFahrenheitCountry) {
            binding.includeNow.currentTemp.text = celsiusStr
            binding.includeNow.currentTempF.text = formatFahrenheit(realtime.temperature)
            binding.includeNow.currentTempF.visibility = View.VISIBLE
            binding.includeNow.tempDivider.visibility = View.VISIBLE
        } else {
            binding.includeNow.currentTemp.text = celsiusStr
            binding.includeNow.currentTempF.visibility = View.GONE
            binding.includeNow.tempDivider.visibility = View.GONE
        }

        // 天气描述（日间/夜间）
        val dailySkycons = daily.skycon
        val todayDaySkycon = daily.skycon08h20h?.getOrNull(0)?.value ?: dailySkycons.getOrNull(0)?.value ?: realtime.skycon
        val todayNightSkycon = daily.skycon20h32h?.getOrNull(0)?.value ?: dailySkycons.getOrNull(0)?.value ?: realtime.skycon
        binding.includeNow.currentSky.text = getSkyDesc(todayDaySkycon, todayNightSkycon)

        // 空气质量（中国用 CN 标准，海外用 US 标准）
        val isChina = weather.isChina(viewModel.locationLng, viewModel.locationLat)
        val aqiValue = weather.getAqiValue(isChina)
        val aqiLevel = weather.getAqiLevel(isChina)
        val aqiLabel = if (isChina) "AQI(CN)" else "AQI(US)"
        val currentAQIText = if (aqiValue != null) "$aqiLabel $aqiValue $aqiLevel" else "$aqiLabel N/A"
        binding.includeNow.currentAQI.text = currentAQIText

        binding.includeNow.currentWind.text = formatWindText(realtime.wind.direction, realtime.wind.speed)
        binding.includeNow.currentHumidity.text = formatHumidityText(realtime.humidity)

        // 根据实时天气设置背景图片
        binding.includeNow.nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)

        // 逐小时预报 → HourlyChartView
        val hourly = weather.hourly
        val hourlyTemps = hourly?.temperature?.map { it.value } ?: emptyList()
        val hourlyTimes = hourly?.temperature?.map { it.date } ?: emptyList()
        val hourlySkycons = hourly?.skycon?.map { it.value } ?: emptyList()
        val minHourlySize = minOf(hourlyTemps.size, hourlyTimes.size, hourlySkycons.size)
        if (minHourlySize > 0) {
            binding.includeHourly.hourlyChart.setData(
                hourlyTemps.subList(0, minHourlySize),
                hourlyTimes.subList(0, minHourlySize),
                hourlySkycons.subList(0, minHourlySize),
                viewModel.isFahrenheitCountry
            )
        }

        // 逐日预报 → DailyChartView
        val dailyMaxTemps = daily.temperature.map { it.max }
        val dailyMinTemps = daily.temperature.map { it.min }
        val dailyTimes = daily.skycon.map { it.date }
        val dailyDaySkycons = daily.skycon08h20h?.map { it.value } ?: daily.skycon.map { it.value }
        val dailyNightSkycons = daily.skycon20h32h?.map { it.value } ?: daily.skycon.map { it.value }
        val minDailySize = minOf(dailyMaxTemps.size, dailyMinTemps.size, dailyTimes.size, dailyDaySkycons.size, dailyNightSkycons.size)
        if (minDailySize > 0) {
            binding.includeForecast.dailyChart.setData(
                dailyMaxTemps.subList(0, minDailySize),
                dailyMinTemps.subList(0, minDailySize),
                dailyTimes.subList(0, minDailySize),
                dailyDaySkycons.subList(0, minDailySize),
                dailyNightSkycons.subList(0, minDailySize),
                viewModel.isFahrenheitCountry
            )
        }

        // 生活指数
        val lifeIndex = daily.lifeIndex
        if (lifeIndex.coldRisk.isNotEmpty()) {
            binding.includeLifeIndex.coldRiskText.text = lifeIndex.coldRisk[0].desc
        }
        if (lifeIndex.dressing.isNotEmpty()) {
            binding.includeLifeIndex.dressingText.text = lifeIndex.dressing[0].desc
        }
        if (lifeIndex.ultraviolet.isNotEmpty()) {
            binding.includeLifeIndex.ultravioletText.text = lifeIndex.ultraviolet[0].desc
        }
        if (lifeIndex.carWashing.isNotEmpty()) {
            binding.includeLifeIndex.carWashingText.text = lifeIndex.carWashing[0].desc
        }
        binding.weatherLayout.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_LNG = "arg_lng"
        const val ARG_LAT = "arg_lat"
        const val ARG_NAME = "arg_name"

        fun newInstance(lng: String, lat: String, name: String): WeatherPageFragment {
            return WeatherPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LNG, lng)
                    putString(ARG_LAT, lat)
                    putString(ARG_NAME, name)
                }
            }
        }
    }
}