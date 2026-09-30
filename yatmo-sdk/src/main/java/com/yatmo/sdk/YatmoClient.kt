package com.yatmo.sdk

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Typed client for the Yatmo REST API. Every method is a suspend function running on Dispatchers.IO
 * and sends the licence and the mobile headers (LicenseKey, X-Yatmo-App-Id, X-Yatmo-Platform, X-Yatmo-SDK).
 */
class YatmoClient(val configuration: YatmoConfiguration) {

    /** GET /Summary: nearby places by category with travel times, closest cities, reverse-geocoded place. */
    suspend fun summary(latitude: Double, longitude: Double): YatmoSummary =
        YatmoSummary.fromJson(getObject("Summary", position(latitude, longitude)))

    /** GET /Summary/text: generated paragraphs, resolved to the configured language. */
    suspend fun summaryText(latitude: Double, longitude: Double): YatmoSummaryText =
        YatmoSummaryText.fromJson(getObject("Summary/text", position(latitude, longitude)), configuration.language)

    /** GET /Scores: one 0 to 10 score per category. */
    suspend fun scores(latitude: Double, longitude: Double): YatmoScores =
        YatmoScores.fromJson(getObject("Scores", position(latitude, longitude)))

    /** GET /Enrichment: nearest place of each category with distances and times for four travel modes. */
    suspend fun enrichment(latitude: Double, longitude: Double): YatmoEnrichment =
        YatmoEnrichment.fromJson(getObject("Enrichment", position(latitude, longitude)))

    /** GET /Points inside a bounding box. Only ask from zoom 13 upwards and debounce camera events. */
    suspend fun points(southWest: LatLng, northEast: LatLng, poiTypeIds: List<Long>? = null): List<YatmoPoi> {
        val query = mutableMapOf(
            "bound1" to "${fmt(southWest.latitude)},${fmt(southWest.longitude)}",
            "bound2" to "${fmt(northEast.latitude)},${fmt(northEast.longitude)}",
            "groupSamePositions" to "true",
            "caringForBigResponse" to "true"
        )
        if (!poiTypeIds.isNullOrEmpty()) query["poiTypesIds"] = poiTypeIds.joinToString(",")
        return getArray("Points", query).toList(YatmoPoi::fromJson)
    }

    /** GET /Isochrone/GetMultipleTimes: the 5, 10 and 20 minute areas, smallest first. */
    suspend fun isochrones(mode: TravelMode, latitude: Double, longitude: Double): List<YatmoIsochrone> {
        val array = getArray("Isochrone/GetMultipleTimes", position(latitude, longitude) + ("travelMode" to mode.apiName))
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            YatmoIsochrone(label = item.optString("label"), geometryJson = item.get("iso").toString())
        }
    }

    /** GET /Geolocation/GetClose: address autocomplete near a position, inside the configured country. */
    suspend fun geocode(query: String, latitude: Double, longitude: Double): List<YatmoPlace> {
        val root = getObject("Geolocation/GetClose", position(latitude, longitude) + ("address" to query))
        return root.optJSONArray("features").toList { it }.mapNotNull(YatmoPlace::fromFeature)
    }

    /** GET /SimplifiedCategories: category ids grouped by family, the values accepted by `poiTypeIds`. */
    suspend fun simplifiedCategories(): List<YatmoCategoryGroup> {
        val root = getObject("SimplifiedCategories", emptyMap())
        return root.keys().asSequence().map { name ->
            val ids = root.optJSONArray(name)
            YatmoCategoryGroup(name, if (ids == null) emptyList() else (0 until ids.length()).map { ids.getLong(it) })
        }.sortedBy { it.name }.toList()
    }

    /** URL of the iframe plugin for a WebView, see [YatmoWebViewOptions]. */
    fun pluginUrl(options: YatmoWebViewOptions): String = options.url(configuration)

    // ---- plumbing ----

    /** Builds the URL with the language appended, for callers that need an endpoint not wrapped above. */
    fun buildUrl(path: String, query: Map<String, String>): String {
        val builder = Uri.parse(configuration.resolvedBaseUrl() + path).buildUpon()
        query.forEach { (k, v) -> builder.appendQueryParameter(k, v) }
        builder.appendQueryParameter("language", configuration.language.name)
        return builder.build().toString()
    }

    private suspend fun getObject(path: String, query: Map<String, String>): JSONObject = JSONObject(getText(path, query))

    private suspend fun getArray(path: String, query: Map<String, String>): JSONArray = JSONArray(getText(path, query))

    private suspend fun getText(path: String, query: Map<String, String>): String = withContext(Dispatchers.IO) {
        val connection = (URL(buildUrl(path, query)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = configuration.timeoutMillis
            readTimeout = configuration.timeoutMillis
            setRequestProperty("LicenseKey", configuration.licenseKey)
            setRequestProperty("X-Yatmo-App-Id", configuration.appId ?: "")
            setRequestProperty("X-Yatmo-Platform", "android")
            setRequestProperty("X-Yatmo-SDK", "yatmo-android/${Yatmo.SDK_VERSION}")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (status !in 200..299) {
                val message = runCatching { JSONObject(body).optString("Error", body) }.getOrDefault(body)
                throw YatmoException(status, "Yatmo API $status: $message")
            }
            body
        } catch (e: YatmoException) {
            throw e
        } catch (e: Exception) {
            throw YatmoException(0, e.message ?: "network error", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun position(latitude: Double, longitude: Double) = mapOf("latitude" to fmt(latitude), "longitude" to fmt(longitude))

    private fun fmt(value: Double) = String.format(Locale.US, "%.7f", value)
}
