package com.yatmo.sdk

/** POI icons are served from one CDN endpoint per country (same list as the web plugin's getCdnUrl). */
object YatmoCdn {
    private val hosts = mapOf(
        Country.BE to "https://beprod-chh6e6e9hhg8bwha.z01.azurefd.net/",
        Country.FR to "https://frprod-atf6hrhkezbhdqem.z01.azurefd.net/",
        Country.NL to "https://nlprod-euexhcaedwekb9g9.z01.azurefd.net/",
        Country.LU to "https://luprod-d3azhthcdgc5d7e2.z01.azurefd.net/",
        Country.CH to "https://chprod-ebercaa7bddnbvbz.z01.azurefd.net/",
        Country.DE to "https://deprod.azureedge.net/",
        Country.IT to "https://itprod2.azureedge.net/",
        Country.ES to "https://esprod.azureedge.net/",
        Country.PT to "https://ptprod.azureedge.net/",
        Country.IE to "https://ieprod.azureedge.net/",
        Country.UK to "https://ukprod.azureedge.net/",
        Country.AT to "https://atprod.azureedge.net/",
        Country.CA to "https://caprod-hba9fuctcmdqcfea.z01.azurefd.net/",
        Country.GR to "https://yatmogrprod-egf4hqdje7a8f4hd.z01.azurefd.net/",
        Country.MA to "https://yatmomaprod-ecesacfgcmc3dugj.z01.azurefd.net/",
        Country.HR to "https://yatmohrprod-aebpfxd3crhcfeh6.z01.azurefd.net/",
        Country.MT to "https://yatmomtprod-bveegpf9csapdcdp.z01.azurefd.net/",
        Country.SI to "https://yatmosiprod-cxftg7d7g5bua8hv.z01.azurefd.net/",
        Country.RS to "https://yatmorsprod-cdguargcaqcxhcgg.z01.azurefd.net/",
        Country.CY to "https://yatmocyprod-bkg4g7f2b6hwahav.z01.azurefd.net/",
        Country.BA to "https://yatmobaprod-d8duf5bffkdvg5b0.z01.azurefd.net/",
        Country.ME to "https://yatmomeprod-eafne6d5g3g0h3g2.z01.azurefd.net/",
        Country.BG to "https://yatmobgprod-dqbzcte0aeg3c2b7.z01.azurefd.net/",
        Country.AL to "https://yatmoalprod-b4cpebdzd2cnerfu.z01.azurefd.net/",
        Country.AU to "https://yatmoauprod-g5gfbkbjhhduewdk.z01.azurefd.net/"
    )

    fun baseUrl(country: Country): String = hosts[country] ?: hosts.getValue(Country.BE)

    /** `{cdn}/icons{size}/{iconId}@2x.png`, size 24 or 32. */
    fun iconUrl(country: Country, iconId: String, size: Int = 24): String = "${baseUrl(country)}icons$size/$iconId@2x.png"

    /** `{cdn}/subicons24/{subIconId}@2x.png`: transit line badges. */
    fun subIconUrl(country: Country, subIconId: String): String = "${baseUrl(country)}subicons24/$subIconId@2x.png"
}
