package com.myweather.android.logic.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 经纬度坐标，保留两位小数。
 *
 * 天气接口按行政区返回数据，坐标精度到 0.01° 已足够，
 * 更高精度只会让同一城市反复产生新的缓存键。
 */
data class Coordinate(val lng: String, val lat: String)

/**
 * 设备定位客户端。
 *
 * 把 [LocationManager] 的回调式接口包装成挂起函数，
 * ViewModel 只拿一个 [Result] 结果，不必关心定位提供者、监听器与线程切换。
 */
object LocationClient {

    /** 是否已获得粗略或精确定位权限 */
    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * 获取一次当前位置。调用方需已获得定位权限，
     * 缺少权限时返回失败结果而不是抛出异常。
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinate(context: Context): Result<Coordinate> {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.failure(IllegalStateException("定位服务不可用，请手动搜索城市"))

        val providers = manager.getProviders(true)
        if (providers.isEmpty()) {
            return Result.failure(IllegalStateException("无可用的定位提供者"))
        }

        val criteria = Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE
            isCostAllowed = true
        }
        val provider = manager.getBestProvider(criteria, true) ?: providers.first()

        return try {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) {
                            continuation.resume(Result.success(location.toCoordinate()))
                        }
                    }

                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {}
                }
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("定位权限不足"))
        }
    }

    private fun Location.toCoordinate() = Coordinate(
        lng = String.format(Locale.US, "%.2f", longitude),
        lat = String.format(Locale.US, "%.2f", latitude)
    )
}