package uz.lazydevv.mytaxitask.locationclient

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class UserLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            when {
                !context.hasLocationPermission() -> {
                    throw Exception("Missing location permission")
                }

                !context.isNetworkEnabled() -> {
                    throw Exception("Network is disabled")
                }

                !context.isGpsEnabled() -> {
                    throw Exception("GPS is disabled")
                }

                else -> {
                    val request = LocationRequest.Builder(interval).build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            super.onLocationResult(result)
                            result.locations.lastOrNull()?.let { location ->
                                launch { send(location) }
                            }
                        }
                    }

                    client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

                    awaitClose {
                        client.removeLocationUpdates(locationCallback)
                    }
                }
            }
        }
    }

    private fun Context.hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun Context.isNetworkEnabled(): Boolean {
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun Context.isGpsEnabled(): Boolean {
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}