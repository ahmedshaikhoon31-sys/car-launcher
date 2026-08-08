package com.zarwa.launcher.weather

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zarwa.launcher.Prefs
import com.zarwa.launcher.databinding.ActivityWeatherBinding

class WeatherActivity : AppCompatActivity() {

    private lateinit var b: ActivityWeatherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(Prefs.isDark(this))
        super.onCreate(savedInstanceState)
        b = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(b.root)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        b.btnBack.setOnClickListener { finish() }
        b.cityDesc.text = Prefs.city(this)
        b.hoursList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val lat = Prefs.lat(this)
        val lon = Prefs.lon(this)

        WeatherRepo.fetch(lat, lon) { w ->
            if (w != null) {
                b.bigTemp.text = "${w.tempC}°"
                b.cityDesc.text = "${Prefs.city(this)} · ${w.desc}"
                b.bigIcon.setImageResource(w.iconRes)
            }
        }
        WeatherRepo.fetchHourly(lat, lon, 8) { hours ->
            b.hoursList.adapter = HourAdapter(hours)
        }
    }
}
