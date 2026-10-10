package com.yatmo.sdk

/** POI icons are served from one CDN for every country: cdn/<iso code>/ (same rule as the web plugin's getCdnUrl). */
object YatmoCdn {
    const val HOST = "https://beprod-chh6e6e9hhg8bwha.z01.azurefd.net/"

    fun baseUrl(country: Country): String = HOST + "cdn/" + country.name.lowercase() + "/"

    /** `{cdn}/icons{size}/{iconId}@2x.png`, size 24 or 32. */
    fun iconUrl(country: Country, iconId: String, size: Int = 24): String = "${baseUrl(country)}icons$size/$iconId@2x.png"

    /** `{cdn}/subicons24/{subIconId}@2x.png`: transit line badges. */
    fun subIconUrl(country: Country, subIconId: String): String = "${baseUrl(country)}subicons24/$subIconId@2x.png"
}
