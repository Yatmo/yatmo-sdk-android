package com.yatmo.sdk

import org.json.JSONArray
import org.json.JSONObject

/** Simple latitude / longitude pair used by the client. */
data class LatLng(val latitude: Double, val longitude: Double)

// ---- /Points ----------------------------------------------------------------------------------

/** One point of interest as returned by GET /Points (compact keys on the wire). */
data class YatmoPoi(
    val name: String,
    /** Translated type, for example "Preschool" or "Bus stop (Dansaert)". */
    val type: String,
    val latitude: Double,
    val longitude: Double,
    /** Category id, the value to pass in `poiTypeIds`. */
    val categoryId: String,
    /** Icon ids, comma separated when several POIs share the same position. */
    val icon: String,
    /** Sub-icon ids (transit lines), comma separated, may be empty. */
    val subIcon: String,
    val grouped: Boolean,
    val rpt: String?,
    /** Specific data as a JSON string (transit lines, brand...), "{}" when empty. */
    val specificData: String?,
    val filterId: String?
) {
    val id: String get() = "$latitude,$longitude,$name"
    val iconIds: List<String> get() = icon.split(',').filter { it.isNotEmpty() }
    val subIconIds: List<String> get() = subIcon.split(',').filter { it.isNotEmpty() }

    internal companion object {
        fun fromJson(o: JSONObject) = YatmoPoi(
            name = o.optString("n"), type = o.optString("t"),
            latitude = o.getDouble("la"), longitude = o.getDouble("ln"),
            categoryId = o.optString("p"), icon = o.optString("i"), subIcon = o.optString("si"),
            grouped = o.optBoolean("g"), rpt = o.optNullableString("rpt"),
            specificData = o.optNullableString("sd"), filterId = o.optNullableString("fid")
        )
    }
}

// ---- /Summary ---------------------------------------------------------------------------------

/** Travel information for one mode, as computed by the routing engine. */
data class YatmoTravelData(
    val hasTravelInformation: Boolean,
    /** 1 driving, 2 walking, 3 bicycling, 4 transit. See [travelMode]. */
    val travelModeCode: Int,
    val translatedTravelMode: String?,
    val distanceMeters: Int?,
    val distanceLongLabel: String?,
    val distanceShortLabel: String?,
    val travelTimeSeconds: Int?,
    val travelTimeLongLabel: String?,
    val travelTimeShortLabel: String?,
    val travelTimeExtraShortLabel: String?
) {
    val travelMode: TravelMode? get() = TravelMode.fromSummaryCode(travelModeCode)

    internal companion object {
        fun fromJson(o: JSONObject) = YatmoTravelData(
            hasTravelInformation = o.optBoolean("hti"), travelModeCode = o.optInt("tm"),
            translatedTravelMode = o.optNullableString("ttm"),
            distanceMeters = o.optNullableInt("ptdd"), distanceLongLabel = o.optNullableString("ptdll"), distanceShortLabel = o.optNullableString("ptdsl"),
            travelTimeSeconds = o.optNullableInt("tt"), travelTimeLongLabel = o.optNullableString("ttll"),
            travelTimeShortLabel = o.optNullableString("ttsl"), travelTimeExtraShortLabel = o.optNullableString("ttesl")
        )
    }
}

/** One place inside a Summary sub-category. */
data class YatmoSummaryPlace(
    val categoryId: Long,
    val icon: Int,
    val subIcon: Int,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val specificData: String?,
    val travelData: List<YatmoTravelData>
) {
    fun travel(mode: TravelMode): YatmoTravelData? = travelData.firstOrNull { it.travelModeCode == mode.summaryCode }

    internal companion object {
        fun fromJson(o: JSONObject) = YatmoSummaryPlace(
            categoryId = o.optLong("id"), icon = o.optInt("i"), subIcon = o.optInt("si"),
            latitude = o.getDouble("la"), longitude = o.getDouble("lo"), name = o.optString("n"),
            specificData = o.optNullableString("sd"),
            travelData = o.optJSONArray("td").toList(YatmoTravelData::fromJson)
        )
    }
}

data class YatmoSummarySubCategory(val subType: Int, val label: String, val singularLabel: String?, val places: List<YatmoSummaryPlace>) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoSummarySubCategory(
            subType = o.optInt("st"), label = o.optString("l"), singularLabel = o.optNullableString("lb"),
            places = o.optJSONArray("d").toList(YatmoSummaryPlace::fromJson)
        )
    }
}

data class YatmoSummaryCategory(val categoryType: Int, val label: String, val subCategories: List<YatmoSummarySubCategory>) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoSummaryCategory(
            categoryType = o.optInt("ct"), label = o.optString("l"),
            subCategories = o.optJSONArray("sc").toList(YatmoSummarySubCategory::fromJson)
        )
    }
}

data class YatmoCloseCity(
    /** City name per language code (EN, FR, NL...). */
    val names: Map<String, String>,
    val travelData: List<YatmoTravelData>,
    val center: LatLng?
) {
    fun name(language: Language): String? = names[language.name] ?: names["EN"] ?: names.values.firstOrNull()

    internal companion object {
        fun fromJson(o: JSONObject) = YatmoCloseCity(
            names = o.optJSONObject("n")?.let { n -> n.keys().asSequence().associateWith { n.optString(it) } } ?: emptyMap(),
            travelData = o.optJSONArray("td").toList(YatmoTravelData::fromJson),
            center = o.optJSONObject("c")?.let { LatLng(it.optDouble("Latitude"), it.optDouble("Longitude")) }
        )
    }
}

data class YatmoPlaceInformation(
    val streetName: String?,
    val isLocality: Boolean,
    val cityName: String?,
    val zipCode: String?,
    val localizedStreetNames: Map<String, String>?,
    val localizedCityNames: Map<String, String>?
) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoPlaceInformation(
            streetName = o.optNullableString("StreetName"), isLocality = o.optBoolean("IsLocality"),
            cityName = o.optNullableString("CityName"), zipCode = o.optNullableString("ZipCode"),
            localizedStreetNames = o.optJSONObject("LocalizedStreetNames")?.toStringMap(),
            localizedCityNames = o.optJSONObject("LocalizedCityNames")?.toStringMap()
        )
    }
}

/** GET /Summary: nearby places grouped by category, closest cities and reverse-geocoded place. */
data class YatmoSummary(
    val categories: List<YatmoSummaryCategory>,
    val closeCities: List<YatmoCloseCity>,
    val placeInformation: YatmoPlaceInformation?
) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoSummary(
            categories = o.optJSONArray("AvailableCategoriesAroundPosition").toList(YatmoSummaryCategory::fromJson),
            closeCities = o.optJSONArray("CloseCities").toList(YatmoCloseCity::fromJson),
            placeInformation = o.optJSONObject("PlaceInformation")?.let(YatmoPlaceInformation::fromJson)
        )
    }
}

// ---- /Summary/text ----------------------------------------------------------------------------

/** Generated neighbourhood paragraphs, resolved to the configured language by the client. */
data class YatmoSummaryText(val paragraphs: List<Paragraph>) {
    data class Paragraph(val sentences: List<String>) {
        val text: String get() = sentences.joinToString(" ")
    }

    val text: String get() = paragraphs.joinToString("\n\n") { it.text }

    internal companion object {
        fun fromJson(o: JSONObject, language: Language) = YatmoSummaryText(
            o.optJSONArray("Paragraphs").toList { p ->
                Paragraph(p.optJSONArray("Sentences").toList { s ->
                    s.optNullableString(language.name) ?: s.optNullableString("EN") ?: s.keys().asSequence().firstOrNull()?.let { s.optString(it) } ?: ""
                }.filter { it.isNotEmpty() })
            }
        )
    }
}

// ---- /Scores ----------------------------------------------------------------------------------

data class YatmoScore(
    /** Stable key: publicTransport, trains, motorways, nurseries, schools, supermarkets... */
    val key: String,
    val label: String,
    /** 0 to 10. */
    val value: Double,
    val iconId: Int?,
    val categoryType: Int?,
    val subTypes: List<Int>
) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoScore(
            key = o.optString("k"), label = o.optString("l"), value = o.optDouble("v"),
            iconId = o.optNullableInt("iconId"), categoryType = o.optNullableInt("pt"),
            subTypes = o.optJSONArray("st")?.let { a -> (0 until a.length()).map { a.getInt(it) } } ?: emptyList()
        )
    }
}

data class YatmoScores(val scores: List<YatmoScore>, val language: String?) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoScores(o.optJSONArray("scores").toList(YatmoScore::fromJson), o.optNullableString("language"))
    }
}

// ---- /Enrichment ------------------------------------------------------------------------------

data class YatmoTravelInfo(val distanceMeters: Int, val durationSeconds: Int, val durationMinutes: Int) {
    internal companion object {
        fun fromJson(o: JSONObject?) = o?.let { YatmoTravelInfo(it.optInt("distanceMeters"), it.optInt("durationSeconds"), it.optInt("durationMinutes")) }
    }
}

data class YatmoNearestPoi(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val straightLineDistanceMeters: Int,
    val walking: YatmoTravelInfo?,
    val bicycling: YatmoTravelInfo?,
    val driving: YatmoTravelInfo?,
    val transit: YatmoTravelInfo?
) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoNearestPoi(
            name = o.optString("name"), latitude = o.getDouble("latitude"), longitude = o.getDouble("longitude"),
            straightLineDistanceMeters = o.optInt("straightLineDistanceMeters"),
            walking = YatmoTravelInfo.fromJson(o.optJSONObject("walking")), bicycling = YatmoTravelInfo.fromJson(o.optJSONObject("bicycling")),
            driving = YatmoTravelInfo.fromJson(o.optJSONObject("driving")), transit = YatmoTravelInfo.fromJson(o.optJSONObject("transit"))
        )
    }
}

data class YatmoEnrichedCategory(val id: Long, val key: String, val group: String, val label: String, val nearest: YatmoNearestPoi?) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoEnrichedCategory(
            id = o.optLong("id"), key = o.optString("key"), group = o.optString("group"), label = o.optString("label"),
            nearest = o.optJSONObject("nearest")?.let(YatmoNearestPoi::fromJson)
        )
    }
}

/** GET /Enrichment: the nearest place of each category with routed distances and times. */
data class YatmoEnrichment(
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val language: String,
    val searchRadiusMeters: Int,
    val categories: List<YatmoEnrichedCategory>
) {
    internal companion object {
        fun fromJson(o: JSONObject) = YatmoEnrichment(
            latitude = o.getDouble("latitude"), longitude = o.getDouble("longitude"),
            country = o.optString("country"), language = o.optString("language"), searchRadiusMeters = o.optInt("searchRadiusMeters"),
            categories = o.optJSONArray("categories").toList(YatmoEnrichedCategory::fromJson)
        )
    }
}

// ---- /Isochrone/GetMultipleTimes --------------------------------------------------------------

/** One reachable area. [geometryJson] is the GeoJSON geometry (Polygon or MultiPolygon) as returned by the API. */
data class YatmoIsochrone(val label: String, val geometryJson: String)

// ---- /Geolocation/GetClose (Photon) -----------------------------------------------------------

data class YatmoPlace(
    val name: String?,
    val street: String?,
    val houseNumber: String?,
    val postcode: String?,
    val city: String?,
    val country: String?,
    val countryCode: String?,
    val type: String?,
    val latitude: Double,
    val longitude: Double
) {
    /** "Rue Neuve, 1000 Brussels" style single line. */
    val label: String
        get() = listOfNotNull(name ?: street, listOfNotNull(postcode, city).joinToString(" ").ifEmpty { null }).joinToString(", ")

    internal companion object {
        fun fromFeature(f: JSONObject): YatmoPlace? {
            val coordinates = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return null
            if (coordinates.length() < 2) return null
            val p = f.optJSONObject("properties") ?: JSONObject()
            return YatmoPlace(
                name = p.optNullableString("name"), street = p.optNullableString("street"), houseNumber = p.optNullableString("housenumber"),
                postcode = p.optNullableString("postcode"), city = p.optNullableString("city"), country = p.optNullableString("country"),
                countryCode = p.optNullableString("countrycode"), type = p.optNullableString("type"),
                latitude = coordinates.getDouble(1), longitude = coordinates.getDouble(0)
            )
        }
    }
}

// ---- /SimplifiedCategories --------------------------------------------------------------------

/** Category ids grouped by family (Education, Transports, Motorways, Shopping, Tourism...). */
data class YatmoCategoryGroup(val name: String, val poiTypeIds: List<Long>)

// ---- org.json helpers -------------------------------------------------------------------------

internal fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else optString(key)
internal fun JSONObject.optNullableInt(key: String): Int? = if (isNull(key)) null else optInt(key)
internal fun JSONObject.toStringMap(): Map<String, String> = keys().asSequence().associateWith { optString(it) }
internal fun <T> JSONArray?.toList(map: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }.map(map)
