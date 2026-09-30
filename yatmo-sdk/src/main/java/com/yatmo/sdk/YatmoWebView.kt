package com.yatmo.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Locale

/** Options of the iframe plugin (https://documentation.yatmo.com/plugins/iframe). Every field maps to a URL parameter. */
data class YatmoWebViewOptions(
    val latitude: Double,
    val longitude: Double,
    /** overlay, overlay-scores, map-top, map, summary or summary-tabs. */
    val mode: String = "overlay",
    val zoom: Int = 15,
    /** Hex colour, for example "#428BFF". */
    val accentColor: String? = null,
    /** "pin" or "circle". */
    val marker: String? = null,
    val mapStyle: YatmoMapStyle? = null,
    /** Any extra iframe parameter, appended as is. */
    val extraParameters: Map<String, String> = emptyMap()
) {
    fun url(configuration: YatmoConfiguration): String {
        val builder = Uri.parse("https://map.yatmo.com/plugin.html").buildUpon()
            .appendQueryParameter("licenseKey", configuration.licenseKey)
            .appendQueryParameter("country", configuration.country.name)
            .appendQueryParameter("language", configuration.language.name)
            .appendQueryParameter("latitude", String.format(Locale.US, "%.7f", latitude))
            .appendQueryParameter("longitude", String.format(Locale.US, "%.7f", longitude))
            .appendQueryParameter("mode", mode)
            .appendQueryParameter("zoom", zoom.toString())
        accentColor?.let { builder.appendQueryParameter("accentColor", it) }
        marker?.let { builder.appendQueryParameter("marker", it) }
        mapStyle?.let { builder.appendQueryParameter("mapStyle", it.id.toString()) }
        extraParameters.toSortedMap().forEach { (k, v) -> builder.appendQueryParameter(k, v) }
        return builder.build().toString()
    }
}

/** A WebView preloaded with the Yatmo iframe plugin: the full web experience (map, summary panel, scores, tabs). */
class YatmoWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {

    init {
        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        webViewClient = WebViewClient() // keep navigation inside the WebView
    }

    fun load(yatmo: Yatmo, options: YatmoWebViewOptions) {
        loadUrl(options.url(yatmo.configuration))
    }
}
