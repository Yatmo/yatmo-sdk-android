package com.yatmo.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.LatLng as MlLatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * A MapLibre map preconfigured with a Yatmo style, the property pin, POI markers that follow the camera
 * (debounced, from [minZoomForPois]) and optional isochrones. The SDK draws no popup: handle [onPoiSelected]
 * with your own UI. Forward the Activity / Fragment lifecycle to [onStart], [onResume], [onPause], [onStop],
 * [onLowMemory], [onDestroy] and [onSaveInstanceState], or use the Compose wrapper [YatmoMap].
 */
class YatmoMapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    /** The underlying MapLibre view, for camera animations, gesture options or your own layers. */
    val mapView: MapView = MapView(context)

    /** Restrict the POIs to some categories (ids from `client.simplifiedCategories()`). Null = all. */
    var poiTypeIds: List<Long>? = null
        set(value) { field = value; reloadPois() }
    /** Marker size in dp, 24 or 32 (icons exist for both). */
    var poiIconSizeDp: Int = 24
    /** No POI request below this zoom. */
    var minZoomForPois: Double = 13.0
    /** Called when a POI marker is tapped. */
    var onPoiSelected: ((YatmoPoi) -> Unit)? = null
    /** Network or licence errors while loading POIs or isochrones. */
    var onError: ((Throwable) -> Unit)? = null
    /**
     * Like the web plugin: once the isochrones are drawn, the camera fits the largest area, and [hideIsochrones]
     * restores the previous camera.
     */
    var fitIsochrones: Boolean = true
    /** Padding around the isochrones when the camera fits them, in dp. */
    var isochronePaddingDp: Int = 40
    /**
     * Fill colours of the isochrones as ARGB ints, from the smallest area to the largest. Same palette as the web
     * plugin (5 min blue, 10 min orange, 20 min pink); the outline takes the same colour, opaque.
     */
    var isochroneColors: IntArray = intArrayOf(0x8070ADF0.toInt(), 0x66F5A623.toInt(), 0x4DEF427F.toInt())

    private var yatmo: Yatmo? = null
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var property: LatLng? = null
    private var propertyZoom: Double = 15.0
    private var isochroneMode: TravelMode? = null
    private var cameraBeforeIsochrones: CameraPosition? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var poiJob: Job? = null
    private var isochroneJob: Job? = null
    private val currentPois = LinkedHashMap<String, YatmoPoi>()
    private val loadedIcons = ConcurrentHashMap.newKeySet<String>()

    init {
        addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** Binds the view to a [Yatmo] instance and loads the style. Call once, after [onCreate]. */
    fun attach(yatmo: Yatmo, style: YatmoMapStyle = YatmoMapStyle.LIBERTY) {
        this.yatmo = yatmo
        mapView.getMapAsync { map ->
            this.map = map
            map.uiSettings.isAttributionEnabled = true
            map.uiSettings.isLogoEnabled = false
            // showProperty() is usually called before the map is ready: replay the camera now.
            property?.let { map.cameraPosition = CameraPosition.Builder().target(MlLatLng(it.latitude, it.longitude)).zoom(propertyZoom).build() }
            map.addOnCameraIdleListener { scheduleLoadPois(DEBOUNCE_MS) }
            map.addOnMapClickListener { point -> handleClick(map, point) }
            setStyle(style)
        }
    }

    /** Changes the basemap style. Markers and isochrones are re-added once the style is loaded. */
    fun setStyle(style: YatmoMapStyle) {
        val map = map ?: return
        map.setStyle(Style.Builder().fromUri(style.url)) { loaded ->
            this.style = loaded
            loadedIcons.clear()
            currentPois.clear()
            setupLayers(loaded)
            property?.let { renderProperty(it) }
            scheduleLoadPois(0)
            isochroneMode?.let { showIsochrones(it) }
        }
    }

    /** Centres the map on the property and drops the pin. */
    fun showProperty(latitude: Double, longitude: Double, zoom: Double = 15.0) {
        property = LatLng(latitude, longitude)
        propertyZoom = zoom
        map?.cameraPosition = CameraPosition.Builder().target(MlLatLng(latitude, longitude)).zoom(zoom).build()
        style?.let { renderProperty(property!!) }
        cameraBeforeIsochrones = null // the camera to restore is the one centred on the new property
        isochroneMode?.let { showIsochrones(it) }
    }

    /** Draws the 5, 10 and 20 minute areas around the property for the given mode. */
    fun showIsochrones(mode: TravelMode) {
        isochroneMode = mode
        val yatmo = yatmo ?: return
        val property = property ?: return
        isochroneJob?.cancel()
        isochroneJob = scope.launch {
            try {
                val isochrones = yatmo.client.isochrones(mode, property.latitude, property.longitude)
                val features = isochrones.reversed().mapIndexed { index, iso ->
                    Feature.fromJson("""{"type":"Feature","properties":{"rank":${isochrones.size - 1 - index},"label":"${iso.label}"},"geometry":${iso.geometryJson}}""")
                }
                style?.getSourceAs<GeoJsonSource>(SOURCE_ISOCHRONES)?.setGeoJson(FeatureCollection.fromFeatures(features))
                val map = map
                val bounds = isochrones.lastOrNull()?.let { boundsOf(it.geometryJson) }
                if (fitIsochrones && map != null && bounds != null) {
                    if (cameraBeforeIsochrones == null) cameraBeforeIsochrones = map.cameraPosition
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, (isochronePaddingDp * resources.displayMetrics.density).toInt()))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // superseded by a newer request, not an error
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }

    fun hideIsochrones() {
        isochroneMode = null
        isochroneJob?.cancel()
        cameraBeforeIsochrones?.let { camera ->
            cameraBeforeIsochrones = null
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
        }
        style?.getSourceAs<GeoJsonSource>(SOURCE_ISOCHRONES)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    }

    /** Forces a POI reload for the current viewport. */
    fun reloadPois() = scheduleLoadPois(0)

    /** Async access to the MapLibre map. */
    fun getMapAsync(callback: (MapLibreMap) -> Unit) = mapView.getMapAsync(callback)

    // ---- lifecycle ----
    fun onCreate(savedInstanceState: Bundle?) = mapView.onCreate(savedInstanceState)
    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onLowMemory() = mapView.onLowMemory()
    fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)
    fun onDestroy() {
        scope.cancel()
        mapView.onDestroy()
    }

    // ---- internals ----

    private fun setupLayers(style: Style) {
        style.addSource(GeoJsonSource(SOURCE_ISOCHRONES))
        style.addSource(GeoJsonSource(SOURCE_POIS))
        style.addSource(GeoJsonSource(SOURCE_PROPERTY))

        val colors = isochroneColors
        val fill = FillLayer(LAYER_ISOCHRONES_FILL, SOURCE_ISOCHRONES).withProperties(
            PropertyFactory.fillColor(
                Expression.step(
                    Expression.get("rank"), Expression.color(colors[0]),
                    *colors.drop(1).mapIndexed { i, c -> Expression.stop(i + 1, Expression.color(c)) }.toTypedArray()
                )
            )
        )
        val opaque = colors.map { it or 0xFF000000.toInt() }
        val line = LineLayer(LAYER_ISOCHRONES_LINE, SOURCE_ISOCHRONES).withProperties(
            PropertyFactory.lineColor(
                Expression.step(
                    Expression.get("rank"), Expression.color(opaque[0]),
                    *opaque.drop(1).mapIndexed { i, c -> Expression.stop(i + 1, Expression.color(c)) }.toTypedArray()
                )
            ),
            PropertyFactory.lineWidth(3f)
        )
        val firstSymbol = style.layers.firstOrNull { it is SymbolLayer }?.id
        if (firstSymbol != null) {
            style.addLayerBelow(fill, firstSymbol)
            style.addLayerBelow(line, firstSymbol)
        } else {
            style.addLayer(fill)
            style.addLayer(line)
        }

        style.addLayer(
            SymbolLayer(LAYER_POIS, SOURCE_POIS).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconSize(1f), // CDN @2x bitmaps registered with a 2x density: drawn at 24 or 32 dp
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
        style.addLayer(
            CircleLayer(LAYER_PROPERTY, SOURCE_PROPERTY).withProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(0xFF428BFF.toInt()),
                PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                PropertyFactory.circleStrokeWidth(3f)
            )
        )
    }

    private fun renderProperty(property: LatLng) {
        style?.getSourceAs<GeoJsonSource>(SOURCE_PROPERTY)
            ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(property.longitude, property.latitude)))
    }

    private fun scheduleLoadPois(delayMs: Long) {
        val yatmo = yatmo ?: return
        val map = map ?: return
        val style = style ?: return
        poiJob?.cancel()
        if (map.cameraPosition.zoom < minZoomForPois) {
            currentPois.clear()
            style.getSourceAs<GeoJsonSource>(SOURCE_POIS)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        val bounds = map.projection.visibleRegion.latLngBounds
        val ids = poiTypeIds
        poiJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            try {
                val pois = yatmo.client.points(
                    LatLng(bounds.latitudeSouth, bounds.longitudeWest),
                    LatLng(bounds.latitudeNorth, bounds.longitudeEast),
                    ids
                )
                ensureIcons(yatmo, style, pois)
                currentPois.clear()
                pois.forEach { currentPois[it.id] = it }
                val features = pois.mapNotNull { poi ->
                    val iconId = poi.iconIds.firstOrNull() ?: return@mapNotNull null
                    Feature.fromGeometry(Point.fromLngLat(poi.longitude, poi.latitude)).apply {
                        addStringProperty("id", poi.id)
                        addStringProperty("icon", iconKey(iconId))
                    }
                }
                style.getSourceAs<GeoJsonSource>(SOURCE_POIS)?.setGeoJson(FeatureCollection.fromFeatures(features))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }

    /** Downloads the CDN icons not yet registered on the style. */
    private suspend fun ensureIcons(yatmo: Yatmo, style: Style, pois: List<YatmoPoi>) {
        val missing = pois.mapNotNull { it.iconIds.firstOrNull() }.distinct().filter { loadedIcons.add(it) }
        if (missing.isEmpty()) return
        val size = if (poiIconSizeDp >= 32) 32 else 24
        val bitmaps = withContext(Dispatchers.IO) {
            missing.mapNotNull { iconId ->
                runCatching {
                    URL(YatmoCdn.iconUrl(yatmo.configuration.country, iconId, size)).openStream().use { BitmapFactory.decodeStream(it) }
                }.getOrNull()?.also { bitmap ->
                    // The CDN serves @2x PNGs. Without a density MapLibre draws them at their pixel size (about 9 dp on a
                    // 420 dpi phone); declaring 2x makes the style scale them to 24 or 32 dp on every screen.
                    bitmap.density = 2 * android.util.DisplayMetrics.DENSITY_DEFAULT
                }?.let { iconId to it }
            }
        }
        bitmaps.forEach { (iconId, bitmap: Bitmap) -> style.addImage(iconKey(iconId), bitmap) }
    }

    private fun iconKey(iconId: String) = "yatmo-icon-$iconId"

    /** Bounding box of a GeoJSON Polygon or MultiPolygon, whatever its nesting depth. */
    private fun boundsOf(geometryJson: String): LatLngBounds? {
        val builder = LatLngBounds.Builder()
        var count = 0
        fun walk(node: Any?) {
            if (node !is JSONArray) return
            if (node.length() >= 2 && node.opt(0) is Number && node.opt(1) is Number) {
                builder.include(MlLatLng(node.getDouble(1), node.getDouble(0))); count++
            } else {
                for (i in 0 until node.length()) walk(node.opt(i))
            }
        }
        walk(runCatching { JSONObject(geometryJson).optJSONArray("coordinates") }.getOrNull())
        return if (count >= 2) builder.build() else null
    }

    private fun handleClick(map: MapLibreMap, point: MlLatLng): Boolean {
        val callback = onPoiSelected ?: return false
        val screen = map.projection.toScreenLocation(point)
        val touch = poiIconSizeDp * resources.displayMetrics.density
        val rect = android.graphics.RectF(screen.x - touch, screen.y - touch, screen.x + touch, screen.y + touch)
        val feature = map.queryRenderedFeatures(rect, LAYER_POIS).firstOrNull() ?: return false
        val poi = currentPois[feature.getStringProperty("id")] ?: return false
        callback(poi)
        return true
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val SOURCE_ISOCHRONES = "yatmo-isochrones"
        const val SOURCE_POIS = "yatmo-pois"
        const val SOURCE_PROPERTY = "yatmo-property"
        const val LAYER_ISOCHRONES_FILL = "yatmo-isochrones-fill"
        const val LAYER_ISOCHRONES_LINE = "yatmo-isochrones-line"
        const val LAYER_POIS = "yatmo-pois"
        const val LAYER_PROPERTY = "yatmo-property"
    }
}
