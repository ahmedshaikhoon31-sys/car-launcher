package com.zarwa.launcher.weather

import android.os.Handler
import android.os.Looper
import com.zarwa.launcher.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(val tempC: Int, val desc: String, val iconRes: Int)

/** Fetches current weather from Open-Meteo (free, no API key). */
object WeatherRepo {

    private val main = Handler(Looper.getMainLooper())

    fun fetch(lat: Double, lon: Double, cb: (Weather?) -> Unit) {
        Thread {
            val result = try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val current = JSONObject(body).getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weather_code")
                Weather(temp, descFor(code), iconFor(code))
            } catch (e: Exception) {
                null
            }
            main.post { cb(result) }
        }.start()
    }

    private fun iconFor(code: Int): Int = when (code) {
        0, 1 -> R.drawable.ic_sun
        in 51..67, in 80..82, in 95..99 -> R.drawable.ic_rain
        else -> R.drawable.ic_cloud
    }

    private fun descFor(code: Int): String = when (code) {
        0 -> "صافي"
        1, 2 -> "غائم جزئياً"
        3 -> "غائم"
        45, 48 -> "شبورة"
        in 51..67 -> "أمطار"
        in 71..77 -> "ثلوج"
        in 80..82 -> "زخات مطر"
        in 95..99 -> "عواصف رعدية"
        else -> "—"
    }
}
