package com.isokovibe.musicplayer.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val DEFAULT_CUSTOM_ADS_FEED_URL = "https://isokovibe.com.ng/wp-json/isokovibe/v1/ads"

/**
 * Fetches the operator's own local/affiliate ad banners from the
 * iSokoVibe Custom Ads WordPress plugin's REST endpoint. Deliberately
 * uses plain [HttpURLConnection] rather than adding an HTTP library
 * dependency — it's one unauthenticated GET request.
 */
class CustomAdsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /** Throws on network/parse failure — callers decide how to fall back. */
    suspend fun fetchAds(feedUrl: String): List<CustomAd> = withContext(Dispatchers.IO) {
        val connection = URL(feedUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<List<CustomAd>>(body).sortedBy { it.priority }
        } finally {
            connection.disconnect()
        }
    }
}
