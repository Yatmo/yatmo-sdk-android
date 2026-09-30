package com.yatmo.example

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yatmo.sdk.Country
import com.yatmo.sdk.Language
import com.yatmo.sdk.LatLng
import com.yatmo.sdk.TravelMode
import com.yatmo.sdk.Yatmo
import com.yatmo.sdk.YatmoConfiguration
import com.yatmo.sdk.YatmoMapStyle
import com.yatmo.sdk.YatmoMapView
import kotlinx.coroutines.launch

/**
 * Functional test of the Yatmo Android SDK: a map on a Brussels listing plus every client call,
 * each result logged on screen and in Logcat (tag YatmoTest) so an automated run can read it.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var map: YatmoMapView
    private lateinit var logView: TextView
    private val lines = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val yatmo = Yatmo(this, YatmoConfiguration(
            licenseKey = BuildConfig.YATMO_LICENSE_KEY,
            country = Country.BE,
            language = Language.FR
        ))
        setContentView(R.layout.activity_main)
        logView = findViewById(R.id.log)
        log("appId=${yatmo.configuration.appId} key=${BuildConfig.YATMO_LICENSE_KEY.take(6)}...")

        map = findViewById(R.id.yatmoMap)
        map.onCreate(savedInstanceState)
        map.onError = { e -> log("MAP ERROR: $e") }
        map.onPoiSelected = { poi -> log("POI TAP: ${poi.name} (${poi.type})") }
        map.attach(yatmo, YatmoMapStyle.LIBERTY)
        map.showProperty(latitude = LAT, longitude = LNG, zoom = 15.0)
        map.showIsochrones(TravelMode.WALKING)
        map.getMapAsync { m ->
            m.addOnCameraIdleListener {
                m.style?.let { style ->
                    val pois = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("yatmo-pois")
                    val sources = style.sources.map { it.id }
                    log("camera idle zoom=${"%.1f".format(m.cameraPosition.zoom)} sources=$sources poiSource=${pois != null}")
                }
            }
        }

        lifecycleScope.launch {
            val client = yatmo.client
            step("summary") {
                val s = client.summary(LAT, LNG)
                "${s.categories.size} categories, ${s.closeCities.size} cities, street=${s.placeInformation?.streetName}, first=${s.categories.firstOrNull()?.label}/${s.categories.firstOrNull()?.subCategories?.firstOrNull()?.places?.firstOrNull()?.name} walk=${s.categories.firstOrNull()?.subCategories?.firstOrNull()?.places?.firstOrNull()?.travel(TravelMode.WALKING)?.travelTimeShortLabel}"
            }
            step("summaryText") { val t = client.summaryText(LAT, LNG); "${t.paragraphs.size} paragraphs, ${t.text.length} chars: ${t.text.take(80)}" }
            step("scores") { val s = client.scores(LAT, LNG); s.scores.joinToString { "${it.key}=${it.value}" } }
            step("enrichment") { val e = client.enrichment(LAT, LNG); "${e.categories.size} categories, ${e.categories.firstOrNull()?.key} -> ${e.categories.firstOrNull()?.nearest?.name} ${e.categories.firstOrNull()?.nearest?.walking?.durationMinutes} min" }
            step("geocode") { val p = client.geocode("Rue Neuve", LAT, LNG); "${p.size} places, first=${p.firstOrNull()?.label}" }
            step("simplifiedCategories") { val g = client.simplifiedCategories(); g.joinToString { "${it.name}:${it.poiTypeIds.size}" } }
            step("points") { val p = client.points(LatLng(50.848, 4.338), LatLng(50.856, 4.350)); "${p.size} pois, first=${p.firstOrNull()?.name} icon=${p.firstOrNull()?.iconIds}" }
            step("isochrones") { val i = client.isochrones(TravelMode.WALKING, LAT, LNG); i.joinToString { "${it.label}:${it.geometryJson.length}b" } }
            step("pluginUrl") { client.pluginUrl(com.yatmo.sdk.YatmoWebViewOptions(LAT, LNG)) }
            log("ALL CLIENT CALLS DONE")
        }
    }

    private suspend fun step(name: String, block: suspend () -> String) {
        try {
            log("OK $name: ${block()}")
        } catch (e: Exception) {
            log("FAIL $name: $e")
        }
    }

    private fun log(line: String) {
        Log.i("YatmoTest", line)
        lines.append(line).append('\n')
        runOnUiThread { logView.text = lines.toString() }
    }

    override fun onStart() { super.onStart(); map.onStart() }
    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { map.onPause(); super.onPause() }
    override fun onStop() { map.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); map.onLowMemory() }
    override fun onDestroy() { map.onDestroy(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); map.onSaveInstanceState(outState) }

    private companion object {
        const val LAT = 50.8520525
        const val LNG = 4.3442926
    }
}
