package uz.lazydevv.mytaxitask

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
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
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
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
                if (allGranted) {
                    setUpMap()
                    startUserLocationTracking()
                }
            }

        with(binding) {
            btnZoomIn.setOnClickListener {
                disableUserTrackingInMap()
                changeZoomWithAnimation(1)
            }

            btnZoomOut.setOnClickListener {
                disableUserTrackingInMap()
                changeZoomWithAnimation(-1)
            }

            btnTrackUser.setOnClickListener {
                disableUserTrackingInMap()
                enableUserTrackingInMap()
            }
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
            getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
                gestures.rotateEnabled = false
                gestures.pitchEnabled = false

                initLocationComponent()
                enableUserTrackingInMap()
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        disableUserTrackingInMap()
        stopUserLocationTracking()
    }

    companion object {

        const val DEFAULT_ZOOM_VALUE = 15.0
    }
}