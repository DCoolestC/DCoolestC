package com.isokovibe.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.isokovibe.musicplayer.data.RemoteBanner

/** The design ratio operators are told to supply artwork at (330x70). */
private const val BANNER_ASPECT_RATIO = 330f / 70f

/** Cap the width so the banner doesn't stretch absurdly on a tablet. */
private val BANNER_MAX_WIDTH = 480.dp

/**
 * A remotely-managed promo slot.
 *
 * Sized by aspect ratio rather than fixed pixels: 330dp is wider than the
 * usable width of a small phone, so a hard 330x70 would be clipped on
 * exactly the devices most people use. Fitting the width and holding the
 * 33:7 ratio keeps the artwork whole everywhere.
 *
 * When several banners target the same slot they take turns on each
 * recomposition of the rotation timer, so multiple clients share the space
 * rather than the first one winning permanently.
 */
@Composable
fun RemoteBannerView(
    banners: List<RemoteBanner>,
    onBannerClick: (RemoteBanner) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    var index by remember(banners.size) { mutableIntStateOf(0) }
    val banner = banners[index % banners.size]

    // Rotate through the banners targeting this slot.
    androidx.compose.runtime.LaunchedEffect(banners.size) {
        if (banners.size <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(BANNER_ROTATE_MS)
            index++
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = BANNER_MAX_WIDTH)
            .aspectRatio(BANNER_ASPECT_RATIO)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = banner.linkUrl.isNotBlank()) { onBannerClick(banner) }
    ) {
        AsyncImage(
            model = banner.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
        if (banner.sponsored) {
            Text(
                "Sponsored",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

private const val BANNER_ROTATE_MS = 8_000L
