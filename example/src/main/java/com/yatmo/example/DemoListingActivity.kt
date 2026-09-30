package com.yatmo.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatmo.sdk.Country
import com.yatmo.sdk.Language
import com.yatmo.sdk.TravelMode
import com.yatmo.sdk.Yatmo
import com.yatmo.sdk.YatmoConfiguration
import com.yatmo.sdk.YatmoEnrichedCategory
import com.yatmo.sdk.YatmoMap
import com.yatmo.sdk.YatmoMapStyle

/** Marketing screen: a listing page of a fictional real-estate app ("Nestly") with the Yatmo map and data. */
class DemoListingActivity : ComponentActivity() {
    private val accent = Color(0xFF428BFF)
    private val lat = 50.8520525
    private val lng = 4.3442926

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val yatmo = Yatmo(this, YatmoConfiguration(BuildConfig.YATMO_LICENSE_KEY, Country.BE, Language.FR))
        setContent {
            var nearby by remember { mutableStateOf<List<YatmoEnrichedCategory>>(emptyList()) }
            LaunchedEffect(Unit) {
                runCatching { yatmo.client.enrichment(lat, lng) }.onSuccess { e ->
                    val wanted = listOf("Children.PreSchool", "Transports.Metros", "Shopping.Hypermarket", "Transports.Trains")
                    var list = wanted.mapNotNull { key -> e.categories.firstOrNull { it.key == key && it.nearest != null } }
                    if (list.size < 3) list = e.categories.filter { it.nearest != null }.take(4)
                    nearby = list
                }
            }
            Column(Modifier.fillMaxSize().background(Color.White)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Header()
                    Listing()
                    Text("Le quartier", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp))
                    YatmoMap(
                        yatmo = yatmo, latitude = lat, longitude = lng, zoom = 15.0,
                        style = YatmoMapStyle.LIBERTY,
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(300.dp).clip(RoundedCornerShape(18.dp))
                    )
                    Text("À proximité", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 6.dp))
                    nearby.forEach { NearbyRow(it) }
                    Spacer(Modifier.height(24.dp))
                }
                TabBar()
            }
        }
    }

    @Composable
    private fun Header() {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.weight(1f))
            Text("Nestly", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.FavoriteBorder, contentDescription = null)
        }
    }

    @Composable
    private fun Listing() {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Box(
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFEDE6D9), Color(0xFFC7B8A3))))
            ) {
                Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(72.dp).align(Alignment.Center))
                Text(
                    "Nouveau", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).background(accent, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Text("425 000 €", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
            Text("Appartement 2 chambres, Dansaert", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            Text("Rue du Rempart des Moines, 1000 Bruxelles", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            Text("95 m²  ·  2 ch.  ·  Terrasse", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }
    }

    @Composable
    private fun NearbyRow(category: YatmoEnrichedCategory) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(iconFor(category.key), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.nearest?.name ?: "", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(category.label, fontSize = 13.sp, color = Color.Gray)
            }
            category.nearest?.walking?.let {
                Icon(Icons.Filled.DirectionsWalk, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text(" ${it.durationMinutes} min", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            }
        }
    }

    @Composable
    private fun TabBar() {
        Row(Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).padding(top = 10.dp, bottom = 8.dp)) {
            Tab(Icons.Filled.Search, "Recherche", false)
            Tab(Icons.Filled.Home, "Biens", true)
            Tab(Icons.Filled.Favorite, "Favoris", false)
            Tab(Icons.Filled.Person, "Profil", false)
        }
    }

    @Composable
    private fun androidx.compose.foundation.layout.RowScope.Tab(icon: ImageVector, label: String, selected: Boolean) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (selected) accent else Color.Gray, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 11.sp, color = if (selected) accent else Color.Gray)
        }
    }

    private fun iconFor(key: String): ImageVector = when {
        key.contains("School") -> Icons.Filled.School
        key.contains("Metro") || key.contains("Train") || key.contains("Tram") || key.contains("Bus") -> Icons.Filled.Train
        key.contains("market") || key.contains("Shop") -> Icons.Filled.ShoppingCart
        else -> Icons.Filled.Place
    }
}
