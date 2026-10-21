package com.myweather.android.ui.weather

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.myweather.android.R
import com.myweather.android.databinding.ActivityWeatherBinding
import com.myweather.android.logic.dao.PlaceDao
import com.myweather.android.logic.model.Location
import com.myweather.android.logic.model.Place
import com.myweather.android.ui.place.PlaceFragment

/**
 * 天气主页面。使用 ViewPager2 承载多个 WeatherPageFragment，
 * 通过侧边栏抽屉切换/添加/删除城市。
 */
class WeatherActivity : AppCompatActivity() {
    lateinit var binding: ActivityWeatherBinding

    private var placeList = listOf<Place>()
    private var viewPagerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // 加载已保存城市列表并初始化 ViewPager
        loadPlacesAndSetup()

        // 侧边栏监听：打开时刷新已保存列表，关闭时隐藏键盘
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                val placeFragment = supportFragmentManager.findFragmentById(R.id.placeFragment) as? PlaceFragment
                placeFragment?.refreshSavedPlaces()
            }
            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        })
    }

    /**
     * 加载已保存城市：有城市则创建 ViewPager，无城市则尝试自动定位。
     */
    private fun loadPlacesAndSetup() {
        placeList = PlaceDao.getSavedPlaceList()
        if (placeList.isEmpty()) {
            val lng = intent.getStringExtra("location_lng") ?: ""
            val lat = intent.getStringExtra("location_lat") ?: ""
            val name = intent.getStringExtra("place_name") ?: ""
            if (lng.isNotEmpty() && lat.isNotEmpty()) {
                placeList = listOf(Place(name, Location(lng, lat), ""))
                setupViewPager()
            } else {
                tryAutoLocate()
            }
        } else {
            setupViewPager()
        }
    }

    // 无权/无定位时，尝试 PlaceFragment 内自动定位
    private fun tryAutoLocate() {
        val placeFragment = supportFragmentManager.findFragmentById(R.id.placeFragment) as? PlaceFragment
        if (placeFragment != null) {
            placeFragment.tryLocateForInit(this)
        } else {
            openDrawerForSearch()
        }
    }

    // 从外部打开侧边栏供用户手动搜索城市
    fun openDrawerForSearch() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun isViewPagerReady(): Boolean = viewPagerReady

    // 首次设置（无已保存城市时由定位结果调用）
    fun setupWithPlace(place: Place) {
        placeList = listOf(place)
        setupViewPager()
    }

    /**
     * 创建 ViewPager2 适配器，禁用用户滑动，通过侧边栏切换城市。
     */
    private fun setupViewPager() {
        if (placeList.isEmpty()) return
        val adapter = WeatherPagerAdapter(this, placeList)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.adapter = adapter
        viewPagerReady = true
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
            }
        })
        val initialLng = intent.getStringExtra("location_lng") ?: ""
        val initialLat = intent.getStringExtra("location_lat") ?: ""
        val initialPos = if (initialLng.isNotEmpty() && initialLat.isNotEmpty()) {
            placeList.indexOfFirst { it.location.lng == initialLng && it.location.lat == initialLat }.coerceAtLeast(0)
        } else 0
        binding.viewPager.setCurrentItem(initialPos, false)
        updatePageIndicator(initialPos)
    }

    // 页码指示器已隐藏，仅作为空实现保留
    private fun updatePageIndicator(position: Int) {
        binding.pageIndicatorLayout.visibility = View.GONE
    }

    /**
     * 重新加载已保存城市列表（增删城市后调用），保持当前城市不变。
     */
    fun refreshPlaces() {
        val oldList = placeList
        placeList = PlaceDao.getSavedPlaceList()
        if (placeList.isEmpty()) return
        val currentPlace = if (viewPagerReady) {
            val pos = binding.viewPager.currentItem
            if (pos < oldList.size) oldList[pos] else null
        } else null
        val adapter = WeatherPagerAdapter(this, placeList)
        binding.viewPager.adapter = adapter
        viewPagerReady = true
        val targetPos = if (currentPlace != null) {
            placeList.indexOfFirst { it.location.lng == currentPlace.location.lng && it.location.lat == currentPlace.location.lat }
                .coerceAtLeast(0)
        } else 0
        binding.viewPager.setCurrentItem(targetPos, false)
        updatePageIndicator(targetPos)
    }

    // 切换到指定城市（侧边栏已保存城市点击时调用）
    fun switchToPlace(place: Place) {
        val pos = placeList.indexOfFirst { it.location.lng == place.location.lng && it.location.lat == place.location.lat }
        if (pos >= 0) {
            binding.viewPager.setCurrentItem(pos, true)
        } else {
            refreshPlaces()
            val newPos = placeList.indexOfFirst { it.location.lng == place.location.lng && it.location.lat == place.location.lat }
            if (newPos >= 0) {
                binding.viewPager.setCurrentItem(newPos, true)
            }
        }
    }

    // 切换到第一个城市
    fun switchToMainPlace() {
        if (viewPagerReady && placeList.isNotEmpty()) {
            binding.viewPager.setCurrentItem(0, true)
        }
    }

    // 获取当前显示的城市
    fun getCurrentPlace(): Place? {
        if (!viewPagerReady || placeList.isEmpty()) return null
        val pos = binding.viewPager.currentItem
        return if (pos < placeList.size) placeList[pos] else null
    }

    /**
     * ViewPager2 适配器，每个城市对应一个 WeatherPageFragment。
     * 通过经纬度生成稳定 ID，避免列表变更时的闪退。
     */
    class WeatherPagerAdapter(
        fragmentActivity: FragmentActivity,
        private val places: List<Place>
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = places.size

        override fun createFragment(position: Int): WeatherPageFragment {
            val place = places[position]
            return WeatherPageFragment.newInstance(place.location.lng, place.location.lat, place.name)
        }

        override fun getItemId(position: Int): Long {
            val place = places[position]
            return (place.location.lng.hashCode().toLong() * 31L + place.location.lat.hashCode().toLong())
        }

        override fun containsItem(itemId: Long): Boolean {
            return places.any { place ->
                (place.location.lng.hashCode().toLong() * 31L + place.location.lat.hashCode().toLong()) == itemId
            }
        }
    }
}