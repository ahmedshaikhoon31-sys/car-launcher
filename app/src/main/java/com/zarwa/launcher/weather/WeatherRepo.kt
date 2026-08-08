package com.zarwa.launcher.weather

import android.os.Handler
import android.os.Looper
import com.zarwa.launcher.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(val tempC: Int, val desc: String, val iconRes: Int)
data class HourWeather(val label: String, val tempC: Int, val iconRes: Int)

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

    /** Next hours forecast (up to [count]). */
    fun fetchHourly(lat: Double, lon: Double, count: Int, cb: (List<HourWeather>) -> Unit) {
        Thread {
            val result = try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon&hourly=temperature_2m,weather_code" +
                        "&forecast_days=2&timezone=auto"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000; requestMethod = "GET"
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val hourly = JSONObject(body).getJSONObject("hourly")
                val times = hourly.getJSONArray("time")
                val temps = hourly.getJSONArray("temperature_2m")
                val codes = hourly.getJSONArray("weather_code")

                val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH':00'", java.util.Locale.US)
                    .format(java.util.Date())
                var start = 0
                for (i in 0 until times.length()) {
                    if (times.getString(i) >= now) { start = i; break }
                }
                val list = ArrayList<HourWeather>()
                var i = start
                while (i < times.length() && list.size < count) {
                    val t = times.getString(i) // yyyy-MM-ddTHH:00
                    val hour = t.substring(11, 13).toInt()
                    val label = if (list.isEmpty()) "الآن" else hourLabel(hour)
                    list.add(HourWeather(label, temps.getDouble(i).toInt(), iconFor(codes.getInt(i))))
                    i++
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
            main.post { cb(result) }
        }.start()
    }

    private fun hourLabel(hour24: Int): String {
        val h12 = when { hour24 == 0 -> 12; hour24 > 12 -> hour24 - 12; else -> hour24 }
        val ampm = if (hour24 < 12) "ص" else "م"
        return "$h12 $ampm"
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
