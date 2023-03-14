package uz.lazydevv.mytaxitask.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uz.lazydevv.mytaxitask.R
import uz.lazydevv.mytaxitask.db.AppDb
import uz.lazydevv.mytaxitask.db.entity.LocationEntity
import uz.lazydevv.mytaxitask.locationclient.LocationClient
import uz.lazydevv.mytaxitask.locationclient.UserLocationClient
import uz.lazydevv.mytaxitask.utils.AppConstants

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        locationClient = UserLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        locationClient.getLocationUpdates(LOCATION_INTERVAL)
            .catch { it.printStackTrace() }
            .onEach {
                val newLocation = LocationEntity(latitude = it.latitude, longitude = it.longitude)
                AppDb.getInstance(this).locationDao().addLocation(newLocation)
            }
            .launchIn(serviceScope)

        startForeground(1, notification)
    }

    private fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {

        const val LOCATION_INTERVAL = 1000L
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
    }
}