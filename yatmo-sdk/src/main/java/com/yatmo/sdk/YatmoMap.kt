package com.yatmo.sdk

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Jetpack Compose wrapper around [YatmoMapView]. Forwards the lifecycle for you.
 *
 * ```kotlin
 * YatmoMap(yatmo, latitude = 50.85, longitude = 4.34, zoom = 15.0, isochrones = TravelMode.WALKING,
 *          onPoiSelected = { poi -> ... }, modifier = Modifier.fillMaxWidth().height(320.dp))
 * ```
 */
@Composable
fun YatmoMap(
    yatmo: Yatmo,
    latitude: Double,
    longitude: Double,
    zoom: Double = 15.0,
    style: YatmoMapStyle = YatmoMapStyle.LIBERTY,
    isochrones: TravelMode? = null,
    poiTypeIds: List<Long>? = null,
    poiIconSizeDp: Int = 24,
    onError: ((Throwable) -> Unit)? = null,
    onPoiSelected: ((YatmoPoi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPoiSelected = rememberUpdatedState(onPoiSelected)
    val currentOnError = rememberUpdatedState(onError)

    val mapView = remember {
        YatmoMapView(context).apply {
            this.poiIconSizeDp = poiIconSizeDp
            onCreate(Bundle())
            attach(yatmo, style)
        }
    }

    // Track what was applied so recompositions only push real changes.
    val applied = remember { Applied() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier) { view ->
        view.onPoiSelected = { poi -> currentOnPoiSelected.value?.invoke(poi) }
        view.onError = { error -> currentOnError.value?.invoke(error) }
        if (applied.latitude != latitude || applied.longitude != longitude || applied.zoom != zoom) {
            view.showProperty(latitude, longitude, zoom)
            applied.latitude = latitude; applied.longitude = longitude; applied.zoom = zoom
        }
        if (applied.style != style) {
            view.setStyle(style)
            applied.style = style
        }
        if (applied.poiTypeIds != poiTypeIds) {
            view.poiTypeIds = poiTypeIds
            applied.poiTypeIds = poiTypeIds
        }
        if (applied.isochrones != isochrones) {
            if (isochrones != null) view.showIsochrones(isochrones) else view.hideIsochrones()
            applied.isochrones = isochrones
        }
    }
}

private class Applied {
    var latitude: Double? = null
    var longitude: Double? = null
    var zoom: Double? = null
    var style: YatmoMapStyle? = null
    var poiTypeIds: List<Long>? = null
    var isochrones: TravelMode? = null
}
