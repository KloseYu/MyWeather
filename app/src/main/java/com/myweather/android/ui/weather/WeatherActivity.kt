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
import com.myweather.android.logic.model.normalizedAddress
import com.myweather.android.ui.place.PlaceFragment

/**
 * 天气主页面。使用 ViewPager2 承载多个 WeatherPageFragment，
 * 通过侧边栏抽屉切换/添加/删除城市。
 */
class WeatherActivity : AppCompatActivity() {
    lateinit var binding: ActivityWeatherBinding

    private var placeList = listOf<Place>()
    // 整个生命周期只用一个 adapter，数据变化走 submit 而不是重建 adapter：
    // 重建 adapter 会把 currentItem 重置为 0 并触发一轮异步布局，
    // 与随后的 setCurrentItem 竞争，页面可能停在旧页而索引已指向新页
    private var pagerAdapter: WeatherPagerAdapter? = null
    private var viewPagerReady = false
    // 当前页索引的权威值：每次切换时立即写入，onPageSelected 再校准。
    // 不能只读 ViewPager.currentItem——ViewPager2 在完成布局前调用 setCurrentItem
    // 只是记下待生效页码，currentItem 仍返回旧值，侧边栏会因此高亮错条目。
    private var activeIndex = 0
    // 界面重建（旋转/进程恢复）时用于按经纬度还原当前城市
    private var restoreLng: String? = null
    private var restoreLat: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        restoreLng = savedInstanceState?.getString(KEY_CURRENT_LNG)
        restoreLat = savedInstanceState?.getString(KEY_CURRENT_LAT)

        // 页面回调只注册一次：页码以 activeIndex 为准并由 onPageSelected 校准，
        // 避免侧边栏读到 ViewPager 尚未生效的页码，导致高亮与页面城市不一致
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activeIndex = position
                updatePageIndicator(position)
                // 立即校准一次，再等页面挂载后补一次：
                // 页面 Fragment 在布局阶段才创建，首次调用可能还取不到实例
                syncPageTitle(position)
                binding.viewPager.post { syncPageTitle(position) }
                refreshDrawer()
            }
        })

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
        val placeFragment = supportFragmentManager.findFragmentById(R.id.placeFragment) as? PlaceFragment
        placeFragment?.focusSearch()
    }

    fun isViewPagerReady(): Boolean = viewPagerReady

    /**
     * 创建 ViewPager2 适配器，禁用用户滑动，通过侧边栏切换城市。
     * 适配器全生命周期只创建一次，列表变化交给 submit 处理，
     * 避免重建 adapter 时 currentItem 被重置、与页面布局产生竞争。
     */
    private fun setupViewPager() {
        if (placeList.isEmpty()) return
        val adapter = pagerAdapter ?: WeatherPagerAdapter(this).also {
            pagerAdapter = it
            binding.viewPager.isUserInputEnabled = false
            binding.viewPager.adapter = it
        }
        adapter.submit(placeList)
        viewPagerReady = true
        val initialLng = restoreLng ?: intent.getStringExtra("location_lng") ?: ""
        val initialLat = restoreLat ?: intent.getStringExtra("location_lat") ?: ""
        val initialPos = if (initialLng.isNotEmpty() && initialLat.isNotEmpty()) {
            placeList.indexOfFirst { it.location.lng == initialLng && it.location.lat == initialLat }
                .takeIf { it >= 0 } ?: 0
        } else 0
        showPage(initialPos, false)
    }

    /**
     * 页面定位的唯一入口：索引、标题、侧边栏、页面本身四者在这里一次性对齐。
     *
     * 数据刚变更时 ViewPager2 还没完成布局，这时调用 setCurrentItem 只是记下
     * 「待生效页码」，真正生效要等它处理完本次数据变更。因此这里先断言一次，
     * 再等布局结束后补断言一次，最终一定停在 index 这一页。
     */
    private fun showPage(index: Int, animate: Boolean) {
        if (index !in placeList.indices) return
        activeIndex = index
        updatePageIndicator(index)
        binding.viewPager.setCurrentItem(index, animate)
        // 标题按城市身份校准，不依赖页面是否已经挂载到位
        syncPageTitle(index)
        binding.viewPager.post {
            if (binding.viewPager.currentItem != index) {
                binding.viewPager.setCurrentItem(index, false)
            }
            syncPageTitle(index)
            refreshDrawer()
        }
        refreshDrawer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        getCurrentPlace()?.let {
            outState.putString(KEY_CURRENT_LNG, it.location.lng)
            outState.putString(KEY_CURRENT_LAT, it.location.lat)
        }
    }

    // 页码指示器已隐藏，仅作为空实现保留
    private fun updatePageIndicator(position: Int) {
        binding.pageIndicatorLayout.visibility = View.GONE
    }

    /**
     * 重新加载已保存城市列表（删除城市后调用），尽量保持当前城市不变。
     * 全程只切换一次页面，避免多次 setCurrentItem 与页面布局竞争，
     * 导致「页面停在旧城市、索引已指向新城市」。
     */
    fun refreshPlaces() {
        val currentPlace = placeList.getOrNull(getCurrentIndex())
        val newList = PlaceDao.getSavedPlaceList()
        placeList = newList
        if (newList.isEmpty()) {
            // 城市被删空：同步清空 ViewPager，避免残留旧城市页面与侧边栏数量不一致
            binding.viewPager.adapter = null
            pagerAdapter = null
            viewPagerReady = false
            refreshDrawer()
            return
        }
        val adapter = pagerAdapter
        if (adapter == null) {
            setupViewPager()
            return
        }
        adapter.submit(newList)
        viewPagerReady = true
        val index = currentPlace
            ?.let { target -> newList.indexOfFirst { PlaceDao.isSamePlace(it, target) } }
            ?.takeIf { it >= 0 } ?: 0
        showPage(index, false)
    }

    /**
     * 打开指定城市：搜索结果选中、定位结果都走这里。
     * 先按同一套身份规则同步列表，再按身份定位唯一索引，只切换一次页面，
     * 保证「页面显示的城市」与「侧边栏高亮的条目」是同一条记录。
     */
    fun openPlace(place: Place) {
        placeList = PlaceDao.getSavedPlaceList().ifEmpty { listOf(place) }
        val adapter = pagerAdapter ?: WeatherPagerAdapter(this).also {
            pagerAdapter = it
            binding.viewPager.isUserInputEnabled = false
            binding.viewPager.adapter = it
        }
        adapter.submit(placeList)
        viewPagerReady = true
        val index = placeList.indexOfFirst { PlaceDao.isSamePlace(it, place) }.takeIf { it >= 0 } ?: 0
        showPage(index, false)
    }

    // 切换到第一个城市
    fun switchToMainPlace() {
        if (viewPagerReady && placeList.isNotEmpty()) {
            switchToIndex(0)
        }
    }

    // 获取当前显示的城市
    fun getCurrentPlace(): Place? = placeList.getOrNull(getCurrentIndex())

    // 获取当前 ViewPager 承载的城市列表，供侧边栏直接复用，保证两侧顺序严格一致
    fun getPlaceList(): List<Place> = placeList

    /**
     * 获取当前页面索引。
     * 用 activeIndex 而不是 ViewPager.currentItem：后者在页面尚未布局完成时
     * 仍是待生效的旧页码，会让侧边栏高亮到错误的城市。
     */
    fun getCurrentIndex(): Int {
        if (!viewPagerReady || placeList.isEmpty()) return -1
        return activeIndex.coerceIn(0, placeList.lastIndex)
    }

    // 按索引切换城市，侧边栏点击时使用，索引与列表顺序天然对应
    fun switchToIndex(index: Int) {
        if (!viewPagerReady || placeList.isEmpty()) return
        if (index in placeList.indices) {
            showPage(index, true)
        }
    }

    /**
     * 天气页标题文本：直接显示地区。
     *
     * 侧边栏每条是「城市名 + 灰色地区」两行，灰色那行（address）就是这个坐标
     * 真正所属的地区，天气也是按这个地区发布的。天气页标题因此直接取 address，
     * 与侧边栏灰色行是同一个字符串，一眼就能对上号。
     * 搜索时输入城市名，返回的本来就是区县一级的地点，所以这里显示地区才是
     * 与实际数据粒度一致的做法；地址为空的历史数据才退回城市名。
     */
    fun titleOf(place: Place): String {
        val address = place.normalizedAddress()
        if (address.isNotEmpty()) return address
        return place.name.trim()
    }

    /**
     * 用列表里的城市名校准指定页面的标题。
     * 页面标题的唯一数据来源是 placeList，Fragment 只是展示方，
     * 因此即使 Fragment 是旧列表创建的、或列表内名称被更新过，标题也会被纠正。
     */
    private fun syncPageTitle(position: Int) {
        val place = placeList.getOrNull(position) ?: return
        val adapter = pagerAdapter ?: return
        if (position !in 0 until adapter.itemCount) return
        // FragmentStateAdapter 以 "f"+itemId 作为 Fragment 的 tag，
        // 直接按 tag 取当前页实例，比遍历 FragmentManager 更准
        val fragment = supportFragmentManager.findFragmentByTag("f${adapter.getItemId(position)}")
            as? WeatherPageFragment
            ?: supportFragmentManager.fragments
                .filterIsInstance<WeatherPageFragment>()
                .firstOrNull { it.matchesPlace(place.location.lng, place.location.lat) }
        fragment?.applyPlaceTitle(titleOf(place))
    }

    /**
     * 无条件把最新列表与当前索引推给侧边栏。
     * 之前只在抽屉打开时刷新，而抽屉里的列表是打开那一刻的快照：
     * 若初始化或切换发生在抽屉关闭期间，再次打开时看到的仍是旧快照。
     * 这里不做打开判断，页面一旦变化就刷新，抽屉打开时数据必然已是最新。
     */
    private fun refreshDrawer() {
        val placeFragment = supportFragmentManager.findFragmentById(R.id.placeFragment) as? PlaceFragment
        placeFragment?.refreshSavedPlaces()
    }

    companion object {
        private const val KEY_CURRENT_LNG = "current_city_lng"
        private const val KEY_CURRENT_LAT = "current_city_lat"
    }

    /**
     * ViewPager2 适配器，每个城市对应一个 WeatherPageFragment。
     * 数据放在适配器内部并可原地更新，稳定 ID 由坐标决定：
     * 同一坐标永远对应同一个页面，列表增删时页面实例可复用。
     */
    class WeatherPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        private val places = ArrayList<Place>()

        /**
         * 按差异逐项通知，而不是 notifyDataSetChanged。
         *
         * 新城市是插在列表头部的，整个列表的下标都会平移。notifyDataSetChanged
         * 会让每一页的 itemId 都变，框架只能把「同一位置上的旧页面」拆掉重建，
         * 正在显示的页面因此可能残留上一个城市的内容；逐项通知则能让框架明确
         * 知道「第 0 位插入了一个新项、原来的项整体后移」，页面实例跟着自己
         * 的城市走，不会串页。
         */
        fun submit(newList: List<Place>) {
            // 先删掉不再存在的城市，从后往前避免下标位移
            for (i in places.indices.reversed()) {
                if (newList.none { PlaceDao.isSamePlace(it, places[i]) }) {
                    places.removeAt(i)
                    notifyItemRemoved(i)
                }
            }
            // 再按目标顺序把每个城市放到它该在的位置
            for (target in newList.indices) {
                val want = newList[target]
                val current = places.indexOfFirst { PlaceDao.isSamePlace(it, want) }
                when {
                    current < 0 -> {
                        places.add(target, want)
                        notifyItemInserted(target)
                    }
                    current != target -> {
                        val moved = places.removeAt(current)
                        places.add(target, moved)
                        notifyItemMoved(current, target)
                        if (moved != want) {
                            places[target] = want
                            notifyItemChanged(target)
                        }
                    }
                    places[target] != want -> {
                        places[target] = want
                        notifyItemChanged(target)
                    }
                }
            }
        }

        override fun getItemCount(): Int = places.size

        override fun createFragment(position: Int): WeatherPageFragment {
            val place = places[position]
            return WeatherPageFragment.newInstance(place.location.lng, place.location.lat, place.name)
        }

        private fun stableIdOf(place: Place): Long =
            place.location.lng.hashCode().toLong() * 31L + place.location.lat.hashCode().toLong()

        override fun getItemId(position: Int): Long = stableIdOf(places[position])

        override fun containsItem(itemId: Long): Boolean = places.any { stableIdOf(it) == itemId }
    }
}