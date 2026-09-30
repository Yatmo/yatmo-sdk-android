package com.yatmo.sdk

import android.content.Context
import org.maplibre.android.MapLibre

/** Countries served by the Yatmo API. The name is the subdomain: `https://{country}.yatmo.com/`. */
enum class Country {
    BE, FR, NL, LU, CH, DE, IT, ES, PT, IE, UK, AT, CA, GR, MA, AU, HR, MT, SI, RS, CY, BA, ME, BG, AL
}

/** Languages accepted by the `language` parameter of the API. */
enum class Language {
    EN, FR, NL, DE, IT, ES, PT, CA, ZH, HI, AR, RU, JA, EL, HR, MT, SL, SR, TR, BS, SQ, BG, CNR
}

/** The seven map styles of the web plugin. [id] is the style id used by the plugin. */
enum class YatmoMapStyle(val id: Int, private val fileName: String) {
    LIBERTY(1, "osm_liberty"),
    BASIC(2, "osm_basic"),
    BRIGHT(3, "osm_bright"),
    THREE_D(4, "osm_3d"),
    POSITRON(5, "osm_positron"),
    DARK(6, "osm_dark"),
    LIBERTY_STONEHEDGE(7, "osm_liberty_stonehedge");

    val url: String get() = "https://map.yatmo.com/$fileName.json"
}

/** Travel modes of the routing endpoints. [apiName] is the API spelling, [summaryCode] the code inside Summary payloads. */
enum class TravelMode(val apiName: String, val summaryCode: Int) {
    DRIVING("Driving", 1),
    WALKING("Walking", 2),
    BICYCLING("Bicycling", 3),
    TRANSIT("Transit", 4);

    companion object {
        fun fromSummaryCode(code: Int): TravelMode? = values().firstOrNull { it.summaryCode == code }
    }
}

/**
 * Everything the SDK needs to talk to the API. One instance per app.
 *
 * @param licenseKey your frontend key (the one used by the web plugins), never the backend key
 * @param appId the Android application id sent in X-Yatmo-App-Id, read from `context.packageName` when null
 * @param apiBaseUrl override for staging environments, defaults to `https://{country}.yatmo.com/`
 * @param timeoutMillis request timeout
 */
data class YatmoConfiguration(
    val licenseKey: String,
    val country: Country,
    val language: Language,
    val appId: String? = null,
    val apiBaseUrl: String? = null,
    val timeoutMillis: Int = 15_000
) {
    internal fun resolvedBaseUrl(): String = apiBaseUrl ?: "https://${country.name.lowercase()}.yatmo.com/"
}

/** Thrown by [YatmoClient]. 401 = key missing or unknown, 403 = app id or country not allowed, 429 = quota. */
class YatmoException(val statusCode: Int, message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Entry point: holds the configuration and the client, and initialises MapLibre.
 * Create it in your Application (or DI graph) before inflating any map.
 */
class Yatmo(context: Context, configuration: YatmoConfiguration) {
    val configuration: YatmoConfiguration = configuration.copy(appId = configuration.appId ?: context.applicationContext.packageName)
    val client: YatmoClient = YatmoClient(this.configuration)

    init {
        MapLibre.getInstance(context.applicationContext)
    }

    /** Base URL of the POI icons for the configured country. */
    val cdnBaseUrl: String get() = YatmoCdn.baseUrl(configuration.country)

    companion object {
        /** Version sent in the X-Yatmo-SDK header. */
        const val SDK_VERSION: String = BuildConfig.SDK_VERSION
    }
}
