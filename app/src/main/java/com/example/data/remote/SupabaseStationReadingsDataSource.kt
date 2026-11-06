package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Read-only client for verified station observations stored in Supabase.
 *
 * The Android app uses only the publishable key. Ingestion credentials must stay on a
 * trusted server and must never be shipped in the APK.
 */
class SupabaseStationReadingsDataSource(
    private val httpClient: OkHttpClient = defaultHttpClient
) {
    suspend fun fetchLatestVerifiedReadings(): Result<Map<String, StationObservation>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
                require(baseUrl.startsWith("https://")) { "Supabase URL is not configured" }
                require(publishableKey.isNotBlank() &&
                    publishableKey != "YOUR_SUPABASE_PUBLISHABLE_KEY"
                ) { "Supabase publishable key is not configured" }

                val url = "$baseUrl/rest/v1/station_readings".toHttpUrl().newBuilder()
                    .addQueryParameter(
                        "select",
                        "station_id,observed_at,temperature_c,relative_humidity,pressure_hpa," +
                            "wind_speed_kmh,wind_direction_deg,precipitation_mm,source_name,source_reference"
                    )
                    .addQueryParameter("is_verified", "eq.true")
                    .addQueryParameter("order", "observed_at.desc")
                    .addQueryParameter("limit", "500")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("apikey", publishableKey)
                    .header("Authorization", "Bearer $publishableKey")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Supabase request failed (HTTP ${response.code})")
                    }
                    val payload = response.body?.string()
                        ?: throw IOException("Supabase returned an empty response")
                    parseLatestReadings(JSONArray(payload))
                }
            }
        }

    internal fun parseLatestReadings(rows: JSONArray): Map<String, StationObservation> {
        val latest = linkedMapOf<String, StationObservation>()
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val stationId = row.optString("station_id").takeIf { it.isNotBlank() } ?: continue
            if (latest.containsKey(stationId)) continue

            val observedAt = row.optString("observed_at").takeIf { it.isNotBlank() } ?: continue
            val temperature = row.optionalDouble("temperature_c")?.takeIf { it in -100.0..70.0 }
            val humidity = row.optionalDouble("relative_humidity")?.takeIf { it in 0.0..100.0 }
            val pressure = row.optionalDouble("pressure_hpa")?.takeIf { it in 800.0..1100.0 }
            val windSpeed = row.optionalDouble("wind_speed_kmh")?.takeIf { it in 0.0..300.0 }
            val windDirection = row.optionalDouble("wind_direction_deg")?.takeIf { it in 0.0..360.0 }
            val precipitation = row.optionalDouble("precipitation_mm")?.takeIf { it >= 0.0 }

            // A record with no usable meteorological measurement is not a reading.
            if (listOf(temperature, humidity, pressure, windSpeed, windDirection, precipitation).all { it == null }) {
                continue
            }

            latest[stationId] = StationObservation(
                stationId = stationId,
                observedAt = observedAt,
                temperatureCelsius = temperature,
                relativeHumidityPercent = humidity?.toInt(),
                pressureHpa = pressure,
                windSpeedKmh = windSpeed,
                windDirectionDegrees = windDirection,
                precipitationMm = precipitation,
                sourceName = row.optString("source_name").takeIf { it.isNotBlank() },
                sourceReference = row.optString("source_reference").takeIf { it.isNotBlank() }
            )
        }
        return latest
    }

    private fun org.json.JSONObject.optionalDouble(name: String): Double? {
        if (isNull(name) || !has(name)) return null
        val value = optDouble(name, Double.NaN)
        return value.takeIf { it.isFinite() }
    }

    companion object {
        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }
}

data class StationObservation(
    val stationId: String,
    val observedAt: String,
    val temperatureCelsius: Double?,
    val relativeHumidityPercent: Int?,
    val pressureHpa: Double?,
    val windSpeedKmh: Double?,
    val windDirectionDegrees: Double?,
    val precipitationMm: Double?,
    val sourceName: String?,
    val sourceReference: String?
)
