package com.myweather.android.ui.place

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.myweather.android.MainActivity
import com.myweather.android.databinding.FragmentPlaceBinding
import com.myweather.android.logic.model.Place
import com.myweather.android.ui.weather.WeatherActivity

/**
 * 城市搜索与已保存城市列表 Fragment。
 * 支持文字搜索、GPS 定位、已保存城市切换/删除。
 */
class PlaceFragment : Fragment() {

    val viewModel by viewModels<PlaceViewModel>()

    private var _binding: FragmentPlaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaceAdapter
    private var savedAdapter: SavedPlaceAdapter? = null

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var locationRequested = false

    // 定位权限请求回调
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            if (pendingWeatherActivity != null) {
                locationRequested = false
                requestLocation()
            } else {
                requestLocation()
            }
        } else {
            val act = pendingWeatherActivity
            if (act != null) {
                pendingWeatherActivity = null
                act.openDrawerForSearch()
            } else {
                Toast.makeText(activity, "定位权限未开启，请手动搜索城市", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPlaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager

        // 搜索结果列表
        val layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = layoutManager
        adapter = PlaceAdapter(this, viewModel.placeList)
        binding.recyclerView.adapter = adapter

        // 已保存城市列表
        val savedLayoutManager = LinearLayoutManager(activity)
        binding.savedPlacesRecyclerView.layoutManager = savedLayoutManager

        refreshSavedPlaces()

        // 搜索框输入监听：有内容则搜索，空则隐藏结果
        binding.searchPlaceEdit.addTextChangedListener { editable ->
            val content = editable.toString()
            if (content.isNotEmpty()) {
                viewModel.searchPlaces(content)
            } else {
                binding.recyclerView.visibility = View.GONE
                updateDividerVisibility()
                viewModel.placeList.clear()
                adapter.notifyDataSetChanged()
            }
        }

        // 观察搜索结果
        viewModel.placeLiveData.observe(viewLifecycleOwner) { result ->
            val places = result.getOrNull()
            if (places != null) {
                binding.recyclerView.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
                viewModel.placeList.clear()
                viewModel.placeList.addAll(places)
                adapter.notifyDataSetChanged()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "未知错误"
                Toast.makeText(activity, "搜索失败: $msg", Toast.LENGTH_LONG).show()
            }
        }

        // 观察逆地理编码结果（定位 → 城市名）
        viewModel.reverseGeocodeResult.observe(viewLifecycleOwner) { result ->
            val place = result.getOrNull()
            if (place != null) {
                viewModel.savePlace(place)
                val act = pendingWeatherActivity
                if (act is WeatherActivity) {
                    pendingWeatherActivity = null
                    if (!act.isViewPagerReady()) {
                        act.setupWithPlace(place)
                    } else {
                        act.refreshPlaces()
                        act.switchToPlace(place)
                    }
                } else if (activity is WeatherActivity) {
                    (activity as WeatherActivity).refreshPlaces()
                    (activity as WeatherActivity).switchToPlace(place)
                } else {
                    navigateToWeather(place)
                }
            } else {
                val act = pendingWeatherActivity
                if (act != null) {
                    pendingWeatherActivity = null
                    Toast.makeText(act, "定位失败，请手动搜索城市", Toast.LENGTH_SHORT).show()
                    act.openDrawerForSearch()
                } else if (activity != null) {
                    val msg = result.exceptionOrNull()?.message ?: "未知错误"
                    Toast.makeText(activity, "定位失败: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.locateBtn.setOnClickListener {
            checkAndRequestLocation()
        }

        if (!viewModel.isPlaceSaved()) {
            tryAutoLocate()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    private fun tryAutoLocate() {
        if (hasLocationPermission()) {
            requestLocation()
        }
    }

    private fun checkAndRequestLocation() {
        if (hasLocationPermission()) {
            requestLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @Suppress("MissingPermission")
    private fun requestLocation() {
        if (locationRequested) return
        val lm = locationManager ?: run {
            val act = pendingWeatherActivity
            if (act != null) {
                act.openDrawerForSearch()
            } else if (activity != null) {
                Toast.makeText(activity, "定位服务不可用，请手动搜索城市", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val providers = lm.getProviders(true)
        if (providers.isEmpty()) {
            val act = pendingWeatherActivity
            if (act != null) {
                act.openDrawerForSearch()
            } else if (activity != null) {
                Toast.makeText(activity, "未启用定位服务，请在系统设置中开启", Toast.LENGTH_LONG).show()
            }
            return
        }

        // 优先尝试缓存定位
        for (provider in providers) {
            val lastKnown = lm.getLastKnownLocation(provider)
            if (lastKnown != null) {
                onLocationResult(lastKnown.latitude, lastKnown.longitude)
                return
            }
        }

        // 缓存不可用，发起实时定位请求（15 秒超时）
        locationRequested = true
        val criteria = Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE
            isCostAllowed = false
        }
        val bestProvider = lm.getBestProvider(criteria, true) ?: providers.firstOrNull()
        if (bestProvider != null) {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    onLocationResult(location.latitude, location.longitude)
                    stopLocationUpdates()
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    stopLocationUpdates()
                    val act = pendingWeatherActivity
                    if (act != null) {
                        pendingWeatherActivity = null
                        act.openDrawerForSearch()
                    } else if (activity != null) {
                        Toast.makeText(activity, "定位服务已关闭，请手动搜索城市", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            lm.requestLocationUpdates(bestProvider, 0L, 0f, locationListener!!)
        }

        view?.postDelayed({
            if (locationRequested) {
                stopLocationUpdates()
                val act = pendingWeatherActivity
                if (act != null) {
                    pendingWeatherActivity = null
                    Toast.makeText(act, "定位失败，请手动搜索城市", Toast.LENGTH_SHORT).show()
                    act.openDrawerForSearch()
                } else if (activity != null) {
                    Toast.makeText(activity, "定位超时，请手动搜索城市", Toast.LENGTH_SHORT).show()
                }
            }
        }, 15000L)
    }

    private fun onLocationResult(latitude: Double, longitude: Double) {
        locationRequested = false
        val lng = "%.6f".format(longitude)
        val lat = "%.6f".format(latitude)
        viewModel.reverseGeocode(lng, lat)
    }

    private fun stopLocationUpdates() {
        locationRequested = false
        val lm = locationManager
        val ll = locationListener
        if (lm != null && ll != null) {
            lm.removeUpdates(ll)
        }
        locationListener = null
    }

    // 跳转到天气页面
    private fun navigateToWeather(place: Place) {
        val intent = Intent(context, WeatherActivity::class.java).apply {
            putExtra("location_lng", place.location.lng)
            putExtra("location_lat", place.location.lat)
            putExtra("place_name", place.name)
        }
        startActivity(intent)
        activity?.finish()
    }

    // 搜索结果点击：保存城市并跳转/切换
    fun onPlaceClicked(place: Place) {
        viewModel.savePlace(place)
        val act = activity
        if (act is WeatherActivity) {
            act.binding.drawerLayout.closeDrawers()
            if (!act.isViewPagerReady()) {
                act.setupWithPlace(place)
            } else {
                act.refreshPlaces()
                act.switchToPlace(place)
            }
        } else {
            navigateToWeather(place)
        }
    }

    // 刷新已保存城市列表（含 UI 颜色适配）
    fun refreshSavedPlaces() {
        viewModel.getSavedPlaceList()
        val isInDrawer = activity is WeatherActivity
        if (isInDrawer) {
            binding.savedLabel.setTextColor(Color.parseColor("#BDBDBD"))
            binding.divider.setBackgroundColor(Color.parseColor("#33FFFFFF"))
        } else {
            binding.savedLabel.setTextColor(Color.parseColor("#757575"))
            binding.divider.setBackgroundColor(Color.parseColor("#1F000000"))
        }
        if (viewModel.savedPlaceList.isNotEmpty()) {
            binding.savedLabel.visibility = View.VISIBLE
            val currentLng = getCurrentLng()
            val currentLat = getCurrentLat()
            savedAdapter = SavedPlaceAdapter(
                viewModel.savedPlaceList,
                currentLng,
                currentLat,
                isInDrawer = isInDrawer,
                showDeleteButton = isInDrawer,
                onPlaceClick = { place ->
                    val act = activity
                    if (act is WeatherActivity) {
                        act.binding.drawerLayout.closeDrawers()
                        act.switchToPlace(place)
                    } else {
                        navigateToWeather(place)
                    }
                },
                onPlaceDelete = { place ->
                    viewModel.deletePlace(place)
                    val act = activity
                    if (act is WeatherActivity) {
                        act.refreshPlaces()
                    }
                    refreshSavedPlaces()
                }
            )
            binding.savedPlacesRecyclerView.adapter = savedAdapter
            binding.savedPlacesRecyclerView.visibility = View.VISIBLE
            updateDividerVisibility()
        } else {
            binding.savedLabel.visibility = View.GONE
            binding.savedPlacesRecyclerView.visibility = View.GONE
            binding.divider.visibility = View.GONE
        }
    }

    // 搜索框有内容 + 有已保存城市时显示分隔线
    private fun updateDividerVisibility() {
        binding.divider.visibility = if (
            viewModel.savedPlaceList.isNotEmpty() &&
            binding.searchPlaceEdit.text.isEmpty()
        ) View.VISIBLE else View.GONE
    }

    private fun getCurrentLng(): String {
        val act = activity
        return if (act is WeatherActivity) act.getCurrentPlace()?.location?.lng ?: "" else ""
    }

    private fun getCurrentLat(): String {
        val act = activity
        return if (act is WeatherActivity) act.getCurrentPlace()?.location?.lat ?: "" else ""
    }

    // 从 WeatherActivity 初始化时调用的定位入口
    fun tryLocateForInit(weatherActivity: WeatherActivity) {
        pendingWeatherActivity = weatherActivity
        stopLocationUpdates()
        if (hasLocationPermission()) {
            requestLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // "定位当前城市"按钮调用的定位入口
    fun tryLocateAndRefresh(weatherActivity: WeatherActivity) {
        pendingWeatherActivity = weatherActivity
        stopLocationUpdates()
        if (hasLocationPermission()) {
            requestLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private var pendingWeatherActivity: WeatherActivity? = null

    override fun onDestroyView() {
        pendingWeatherActivity = null
        stopLocationUpdates()
        super.onDestroyView()
        _binding = null
    }
}