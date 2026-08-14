package com.isokovibe.musicplayer.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.isokovibe.musicplayer.data.CustomAd
import kotlinx.coroutines.delay

/**
 * Rotating banner for the operator's own local/affiliate ads (managed via
 * the iSokoVibe Custom Ads WordPress plugin) — the sticky footer shows
 * this instead of the AdMob banner whenever there's at least one to show.
 */
@Composable
fun CustomAdCarousel(ads: List<CustomAd>, modifier: Modifier = Modifier) {
    if (ads.isEmpty()) return

    var index by remember(ads) { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(ads) {
        while (ads.size > 1) {
            delay(7000)
            index = (index + 1) % ads.size
        }
    }

    val ad = ads[index % ads.size]

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ad.clickUrl)))
                } catch (_: ActivityNotFoundException) {
                    // No browser available — nothing sensible to do, so just ignore.
                }
            },
        shape = RoundedCornerShape(10.dp)
    ) {
        Box {
            AsyncImage(
                model = ad.imageUrl,
                contentDescription = ad.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(6f)
            )
            if (ad.sponsored) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 10.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        "Sponsored",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
