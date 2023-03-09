package uz.lazydevv.mytaxitask

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import by.kirich1409.viewbindingdelegate.viewBinding
import com.mapbox.maps.Style
import uz.lazydevv.mytaxitask.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(binding) {
            mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
        }
    }
}