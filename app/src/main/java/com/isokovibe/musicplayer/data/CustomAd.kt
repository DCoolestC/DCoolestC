package com.isokovibe.musicplayer.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matches the JSON shape returned by the iSokoVibe Custom Ads WordPress plugin. */
@Serializable
data class CustomAd(
    val id: Long,
    val title: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("click_url") val clickUrl: String,
    val sponsored: Boolean = false,
    val priority: Int = 10
)
