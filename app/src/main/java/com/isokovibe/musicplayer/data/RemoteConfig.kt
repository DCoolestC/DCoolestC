package com.isokovibe.musicplayer.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Default endpoint; overridable in Settings so the site can move. */
const val DEFAULT_APP_CONFIG_URL = "https://isokovibe.com.ng/wp-json/isokovibe/v1/app-config"

/**
 * Everything the site tells the app, fetched in one request.
 *
 * All fields are nullable or defaulted and the parser ignores unknown keys,
 * so a plugin that gains fields later — or one older than the app — still
 * produces a usable config rather than failing the whole fetch.
 */
@Serializable
data class RemoteConfig(
    val announcement: RemoteAnnouncement? = null,
    @SerialName("app_version") val appVersion: RemoteAppVersion? = null,
    val banners: List<RemoteBanner> = emptyList()
)

/**
 * A message the operator chose to send. Only the newest one is served: the
 * app tracks the last id it showed, so a second announcement supersedes the
 * first rather than a week offline producing a burst of notifications.
 */
@Serializable
data class RemoteAnnouncement(
    val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val url: String = "",
    @SerialName("published_at") val publishedAt: Long = 0L
)

@Serializable
data class RemoteAppVersion(
    @SerialName("version_code") val versionCode: Int = 0,
    @SerialName("version_name") val versionName: String = "",
    @SerialName("download_url") val downloadUrl: String = "",
    val message: String = "",
    val required: Boolean = false
)

@Serializable
data class RemoteBanner(
    val id: Long = 0L,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("link_url") val linkUrl: String = "",
    /** "header", "footer", or "both". */
    val placement: String = "footer",
    val sponsored: Boolean = false
) {
    fun showsInHeader() = placement == "header" || placement == "both"
    fun showsInFooter() = placement == "footer" || placement == "both"
}
