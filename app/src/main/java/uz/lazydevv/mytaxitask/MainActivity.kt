package uz.lazydevv.mytaxitask

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions.Companion.cameraAnimatorOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.permissionx.guolindev.PermissionX
import uz.lazydevv.mytaxitask.databinding.ActivityMainBinding
import uz.lazydevv.mytaxitask.services.LocationService

class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::bind)

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        with(binding.mapView) {
            gestures.focalPoint = getMapboxMap().pixelForCoordinate(it)

            getMapboxMap().flyTo(
                cameraOptions {
                    center(it)
                    zoom(DEFAULT_ZOOM_VALUE)
                },
                MapAnimationOptions.mapAnimationOptions {
                    duration(1000)
                }
            )
        }
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            disableUserTrackingInMap()
        }

        override fun onMove(detector: MoveGestureDetector) = false

        override fun onMoveEnd(detector: MoveGestureDetector) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkLocationPermissionAndDo {
            setUpMap()
            startUserLocationTracking()
        }

        with(binding) {
            btnSpark.setOnClickListener {
                if (isDarkModeEnabled()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }

            btnZoomIn.setOnClickListener {
                disableUserTrackingInMap()
                changeZoomWithAnimation(1)
            }

            btnZoomOut.setOnClickListener {
                disableUserTrackingInMap()
                changeZoomWithAnimation(-1)
            }

            btnTrackUser.setOnClickListener {
                checkLocationPermissionAndDo {
                    disableUserTrackingInMap()
                    enableUserTrackingInMap()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> binding.mapView.getMapboxMap().loadStyleUri(Style.DARK)
            Configuration.UI_MODE_NIGHT_NO -> binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
        }

        resetBackgroundColors()
    }

    private fun checkLocationPermissionAndDo(onGranted: () -> Unit) {
        PermissionX.init(this)
            .permissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "You need to allow necessary permissions in Settings manually",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) onGranted.invoke()
            }
    }

    private fun startUserLocationTracking() {
        Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            startService(this)
        }
    }

    private fun stopUserLocationTracking() {
        Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            startService(this)
        }
    }

    private fun setUpMap() {
        with(binding.mapView) {
            val style = if (isDarkModeEnabled()) Style.DARK else Style.MAPBOX_STREETS

            getMapboxMap().loadStyleUri(style) {
                scalebar.enabled = false
                compass.enabled = false
                logo.enabled = false
                attribution.enabled = false

                gestures.rotateEnabled = false
                gestures.pitchEnabled = false

                initLocationComponent()
                enableUserTrackingInMap()
            }
        }
    }

    private fun isDarkModeEnabled(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun changeZoomWithAnimation(byValue: Int) {
        with(binding.mapView) {
            val currentZoom = getMapboxMap().cameraState.zoom

            val zoom = camera.createZoomAnimator(
                cameraAnimatorOptions(currentZoom + byValue) {
                    startValue(currentZoom)
                }
            ) {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
            }

            camera.playAnimatorsSequentially(zoom)
        }
    }

    private fun initLocationComponent() {
        binding.mapView.location.updateSettings {
            enabled = true

            locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.car
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
    }

    private fun enableUserTrackingInMap() {
        with(binding.mapView) {
            location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.addOnMoveListener(onMoveListener)
        }
    }

    private fun disableUserTrackingInMap() {
        with(binding.mapView) {
            gestures.focalPoint = getMapboxMap().pixelForCoordinate(getMapboxMap().cameraState.center)
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
        }
    }

    private fun resetBackgroundColors() {
        with(binding) {
            btnMenu.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white_gray))
            btnNotifications.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white_gray))
            btnSpark.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white_gray))

            cvTab.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white_gray))

            ivOrders.setBackgroundResource(R.drawable.gradient_btn)
            ivBorders.setBackgroundResource(R.drawable.gradient_btn)
            ivTariffs.setBackgroundResource(R.drawable.gradient_btn)

            ivOrders.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.icon_color_white))
            ivBorders.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.icon_color_white))
            ivTariffs.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.icon_color_white))

            tvOrders.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
            tvBorders.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
            tvTariffs.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disableUserTrackingInMap()
        stopUserLocationTracking()
    }

    companion object {

        const val DEFAULT_ZOOM_VALUE = 15.0
    }
}