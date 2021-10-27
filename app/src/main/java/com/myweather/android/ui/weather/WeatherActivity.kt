package com.myweather.android.ui.weather

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.myweather.android.R
import com.myweather.android.logic.model.Weather
import com.myweather.android.logic.model.getSky
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {
    val viewModel by lazy { ViewModelProvider(this).get(WeatherViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val decorView = window.decorView
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_weather)
        if (viewModel.locationLng.isEmpty()) {
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        if (viewModel.locationLat.isEmpty()) {
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        if (viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }

        viewModel.weatherLiveData.observe(this, Observer {
            val weather = it.getOrNull()
            if (weather != null) {
                showWeatherInfo(weather)
            } else {
                Toast.makeText(this, "无法成功获取到天气信息!", Toast.LENGTH_SHORT).show()
                it.exceptionOrNull()?.printStackTrace()
            }
        })
        viewModel.refreshWeather(viewModel.locationLng,viewModel.locationLat)
    }

    private fun showWeatherInfo(weather: Weather) {
        val placeName:TextView = findViewById(R.id.placeName)
        placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily

        // 填充now.xml的布局内容
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        val currentTemp:TextView = findViewById(R.id.currentTemp)
        currentTemp.text = currentTempText
        val currentSky:TextView = findViewById(R.id.currentSky)
        currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        val currentAQI:TextView = findViewById(R.id.currentAQI)
        currentAQI.text = currentPM25Text
        //设置背景
        val nowLayout: RelativeLayout = findViewById(R.id.nowLayout)
        nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)

        // 填充forecast.xml的布局内容
        val forecastLayout:LinearLayout = findViewById(R.id.forecastLayout)
        forecastLayout.removeAllViews()
        val days = daily.skycon.size
        // 循环,动态设置view布局
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout,false)
            val dateInfo = view.findViewById(R.id.dateInfo) as TextView
            val skyIcon = view.findViewById(R.id.skyIcon) as ImageView
            val skyInfo = view.findViewById(R.id.skyInfo) as TextView
            val temperatureInfo = view.findViewById(R.id.temperatureInfo) as TextView
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInfo.text = simpleDateFormat.format(skycon.date)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ℃"
            temperatureInfo.text = tempText
            forecastLayout.addView(view)
        }

        // 填充life_index.xml布局中的数据
        val lifeIndex = daily.lifeIndex
        val coldRiskText:TextView = findViewById(R.id.coldRiskText)
        val dressingText:TextView = findViewById(R.id.dressingText)
        val ultravioletText:TextView = findViewById(R.id.ultravioletText)
        val carWashingText:TextView = findViewById(R.id.carWashingText)
        coldRiskText.text = lifeIndex.coldRisk[0].desc
        dressingText.text = lifeIndex.dressing[0].desc
        ultravioletText.text = lifeIndex.carWashing[0].desc
        carWashingText.text = lifeIndex.carWashing[0].desc
        val weatherLayout:ScrollView = findViewById(R.id.weatherLayout)
        weatherLayout.visibility = View.VISIBLE
    }

}