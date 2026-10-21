package com.myweather.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.myweather.android.databinding.ActivityMainBinding

/**
 * 应用入口 Activity，包含侧边栏抽屉 + ViewPager 占位。
 * 实际由 PlaceFragment 接管城市搜索与列表展示。
 */
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}