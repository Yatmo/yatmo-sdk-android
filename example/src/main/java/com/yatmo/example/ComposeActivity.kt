package com.yatmo.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yatmo.sdk.Country
import com.yatmo.sdk.Language
import com.yatmo.sdk.TravelMode
import com.yatmo.sdk.Yatmo
import com.yatmo.sdk.YatmoConfiguration
import com.yatmo.sdk.YatmoMap
import com.yatmo.sdk.YatmoMapStyle

/** Test of the Jetpack Compose wrapper: same listing, dark style, cycling isochrones. */
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val yatmo = Yatmo(this, YatmoConfiguration(BuildConfig.YATMO_LICENSE_KEY, Country.BE, Language.NL))
        Log.i("YatmoTest", "compose activity started")
        setContent {
            var status by remember { mutableStateOf("compose: waiting for a POI tap") }
            Column(Modifier.fillMaxSize()) {
                YatmoMap(
                    yatmo = yatmo,
                    latitude = 50.8520525,
                    longitude = 4.3442926,
                    zoom = 15.0,
                    style = YatmoMapStyle.DARK,
                    isochrones = TravelMode.BICYCLING,
                    onError = { e -> Log.i("YatmoTest", "compose MAP ERROR: $e"); status = "error: $e" },
                    onPoiSelected = { poi -> Log.i("YatmoTest", "compose POI TAP: ${poi.name}"); status = "tap: ${poi.name}" },
                    modifier = Modifier.fillMaxWidth().height(520.dp)
                )
                Text(status, Modifier.padding(8.dp))
            }
        }
    }
}
