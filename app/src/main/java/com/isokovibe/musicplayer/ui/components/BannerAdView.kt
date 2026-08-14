package com.isokovibe.musicplayer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * The sticky footer ad banner. Uses Google's official test ad unit by
 * default (Settings > Ads has the toggle + a field to swap in a real
 * AdMob unit ID once there's an AdMob account behind this).
 *
 * AdMob's AdView can only have its ad unit ID set once per instance, so
 * [key] forces a fresh AdView (rather than mutating the old one, which
 * would crash) whenever [adUnitId] changes.
 */
@Composable
fun BannerAdView(adUnitId: String, modifier: Modifier = Modifier) {
    key(adUnitId) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
