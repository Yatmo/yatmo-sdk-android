# Yatmo SDK for Android

[![CI](https://github.com/yatmo/yatmo-sdk-android/actions/workflows/ci.yml/badge.svg)](https://github.com/yatmo/yatmo-sdk-android/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/Yatmo/yatmo-sdk-android.svg)](https://jitpack.io/#Yatmo/yatmo-sdk-android)
![minSdk 24](https://img.shields.io/badge/minSdk-24-blue.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Neighbourhood intelligence for real-estate apps: the [Yatmo](https://yatmo.com) map with the points of interest, travel times and neighbourhood summaries your listing pages need, native on Android. Built on [MapLibre Android](https://maplibre.org/), no Google Maps key required. Kotlin, XML views and Jetpack Compose.

<p align="center">
  <img src="docs/screenshot-android.png" width="260" alt="Yatmo map in the listing screen of a demo Android app">
</p>

## Requirements

- minSdk 24, Kotlin 1.9+, AndroidX
- A Yatmo licence: the **frontend key** and your **applicationId** registered as an [app id](https://documentation.yatmo.com/mobile/app-ids)

## Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.Yatmo:yatmo-sdk-android:v1.0.0")
}
```

## Quick start

```kotlin
val yatmo = Yatmo(applicationContext, YatmoConfiguration("YOUR_FRONTEND_KEY", Country.BE, Language.FR))

// XML view (forward the Activity lifecycle: onStart, onResume, onPause, onStop, onLowMemory, onDestroy, onSaveInstanceState)
val map = findViewById<YatmoMapView>(R.id.yatmoMap)
map.onCreate(savedInstanceState)
map.attach(yatmo, YatmoMapStyle.LIBERTY)
map.showProperty(50.8520525, 4.3442926, zoom = 15.0)
map.showIsochrones(TravelMode.WALKING)          // 5 / 10 / 20 minute areas, camera fitted like the web plugin
map.onPoiSelected = { poi -> Log.d("Yatmo", poi.name) }

// Jetpack Compose (lifecycle handled for you)
YatmoMap(yatmo, latitude = 50.8520525, longitude = 4.3442926, zoom = 15.0, onPoiSelected = { poi -> })

// Data (suspend functions)
lifecycleScope.launch {
    val summary = yatmo.client.summary(50.8520525, 4.3442926)
    val enrichment = yatmo.client.enrichment(50.8520525, 4.3442926)
}
```

The app id is read from `context.packageName` and sent with every request. Register it on your licence, otherwise the API answers 403.

## What is inside

| Type | Role |
|---|---|
| `Yatmo`, `YatmoConfiguration` | Licence key, country, language, app id; initialises MapLibre |
| `YatmoClient` | Suspend client: `summary`, `summaryText`, `scores`, `enrichment`, `points`, `isochrones`, `geocode`, `simplifiedCategories` |
| `YatmoMapView` (View), `YatmoMap` (Compose) | Yatmo map styles on MapLibre, property pin, POI markers following the camera, isochrones, tap callback |
| `YatmoWebView` | `WebView` preloaded with the Yatmo iframe plugin (map + summary panel) |

Full guide: https://documentation.yatmo.com/mobile/android

## Example app

`example/` shows the map and exercises every client call (plus a Compose screen, a WebView screen and the listing screen of the screenshot). Put your key in `local.properties` (ignored by git):

```
yatmo.licenseKey=YOUR_FRONTEND_KEY
```

then `./gradlew :example:assembleDebug`.

## Licence

The SDK is released under the [MIT licence](LICENSE). Using the Yatmo API requires a licence key: https://yatmo.com
